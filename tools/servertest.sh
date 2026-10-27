#!/bin/bash
# Runs the built mod on real dedicated servers, as players install it:
#   tools/servertest.sh [neoforge] [fabric]
# For each loader: installs the server (NeoForge installer / Fabric launcher, cached in run/) and copies the mod jar.
# Pass 1: a throwaway world, "rtf exportpreset" writes the "Default" preset datapack (the same pack the in-game preset
# screen exports). Pass 2: a fresh world with that datapack in world/datapacks, "rtf selftest" (the mod's test command,
# enabled by -Drtf.selftest=true), then "rtf structures" (every overworld structure near the spawn, built and checked).
# Prints the result lines and every error / exception the log contains.
# PRESET=min|max|random:N exports a stress test preset instead of "Default" (tools/stressworld.sh).
# MODS="biomes-o-plenty ..." also installs these Modrinth projects with their dependencies (tools/extramods.sh) and runs
# "rtf biomemods" in both worlds: the vanilla export world is the reference for the other mods' biomes.
# After the self test "rtf heights" writes the world's shape (run/heights-<loader>.txt); with both loaders the two must
# be identical. More (the FreeTerraForged release test plan): SEED=<number or text> another seed; PRESET=set:...seaLevel=
# ...;...lavaLevel=... with EXTREMES=1 checks such a world ("rtf extremes"); HEIGHTS_REF=<file> [HEIGHTS_MODE=heights|
# ground|all] the same shape as another run (MODS=biomes-o-plenty against a run without); LEGACY=<old version> a world
# of that release opened with this build has the same ground as a new one (no seams); NOPRESET=1 a vanilla reference
# world; SKIP_STRUCTURES=1 leaves the structure test out, SKIP_SELFTEST=1 the self test (quick runs that only need the
# world's shape: with both about 1 minute a loader instead of 6; compare such heights only with another SKIP_SELFTEST
# run: a few "base" samples come out 1-2 blocks off when the self test has generated the chunks around them). tools/uiparity.sh: the preset screen's world.
# Works in bash on macOS / Linux and in Git Bash on Windows. JAVA=... overrides the java binary (JDK 21).
# eula=true: the Minecraft EULA is accepted for these local test servers.
{ # parsed as a whole before running, so editing this file during a run is safe
cd "$(dirname "$0")/.."
LOADERS=${*:-"neoforge fabric"}
prop() { grep "^$1=" gradle.properties | cut -d= -f2- | tr -d '
'; }
MC=$(prop minecraft_version)
NEOFORGE=$(prop neoforge_version)
FABRIC_LOADER=${FABRIC_LOADER:-$(prop fabric_loader_version)}
FABRIC_API=${FABRIC_API:-$(prop fabric_api_version)}
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) WIN=1; ARGS_FILE=win_args.txt ;; *) WIN=; ARGS_FILE=unix_args.txt ;; esac
if [ -z "$JAVA" ]; then
    if [ -n "$JAVA_HOME" ]; then JAVA="$JAVA_HOME/bin/java"
    elif [ -x /opt/homebrew/opt/openjdk@21/bin/java ]; then JAVA=/opt/homebrew/opt/openjdk@21/bin/java
    else JAVA=java; fi
fi
PY=$(command -v python3 >/dev/null && python3 -c '' 2>/dev/null && echo python3 || echo python)
# a modpack with Biomes O' Plenty and the like gets 4 GB (the biome mod test after the structure test filled 3 GB on
# Fabric), the mod alone 3 GB
HEAP=$([ -n "$MODS" ] && echo 4G || echo 3G)
PORT=${PORT:-25599}  # another port lets two checkouts test at the same time
TAG=$(pwd | cksum | cut -d' ' -f1)  # marks this checkout's server processes (a number: MSYS rewrites path-like args)
FAILS=0

# start_server <dir> <log>: starts "${start[@]}" in <dir>, console input from <dir>/console.in
start_server() {
    : > "$1/console.in"; : > "$1/$2"
    (cd "$1" && { tail -f console.in & echo $! > tail.pid; wait; } | "${start[@]}" > "$2" 2>&1) &
    pid=$!
}

# kills this checkout's test servers still running (also ones left over from an interrupted run)
kill_servers() {
    if [ -n "$WIN" ]; then
        powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"CommandLine like '%rtf.run=$TAG %'\" | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null
    else
        pkill -f "rtf.run=$TAG " 2>/dev/null
    fi
}

# stop_server <dir> <log>: "stop", waits until the worlds are saved, cleans up
stop_server() {
    echo "stop" >> "$1/console.in"
    wait_for "$1/$2" 'All dimensions are saved' 90 && sleep 3
    kill "$(cat "$1/tail.pid")" 2>/dev/null
    kill_servers
    wait $pid 2>/dev/null
}

# wait_for <file> <regex> <seconds>; hang watchdog (modding-kb testing/hang-watchdog.md): before the server is up
# ("Done ("), a log that stops growing for $STALL seconds (default 90) once the server runs is a hang -> HUNG, fail.
# After that silence is normal (a server waiting for commands, the self test's long forest search)
wait_for() {
    local last=-1 quiet=0 size
    for i in $(seq 1 "$3"); do
        grep -q "$2" "$1" && return 0
        grep -q 'Crash report\|Failed to start\|Exception in server tick loop\|Error executing task\|Mixin apply.*failed' "$1" && return 1
        if ! grep -q 'Done (' "$1"; then
            size=$(wc -c < "$1" 2>/dev/null || echo 0)
            if [ "$size" != "$last" ]; then last=$size; quiet=0
            elif [ $((quiet += 1)) -ge "${STALL:-90}" ] && grep -qE 'Server thread|main/' "$1"; then
                echo "HUNG: $(basename "$1") wrote nothing for ${STALL:-90}s before the server was up. Last lines:" | tee -a "$1"
                grep -vE '^\s*$' "$1" | tail -4 | cut -c1-300
                return 1
            fi
        fi
        sleep 1
    done
    return 1
}

# one run per checkout: a second run would start its servers in the same folders
mkdir -p run
if ! mkdir run/.servertest.lock 2>/dev/null; then
    if kill -0 "$(cat run/.servertest.lock/pid 2>/dev/null)" 2>/dev/null; then echo "servertest.sh is already running in this checkout"; exit 1; fi
fi
echo $$ > run/.servertest.lock/pid
trap 'rm -rf run/.servertest.lock' EXIT

kill_servers
for loader in $LOADERS; do
    dir=run/server-$loader
    mkdir -p "$dir/mods"
    case $loader in
        neoforge)
            if [ ! -f "$dir/libraries/net/neoforged/neoforge/$NEOFORGE/$ARGS_FILE" ]; then
                curl -fsSL -o run/neoforge-installer.jar "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEOFORGE/neoforge-$NEOFORGE-installer.jar"
                (cd "$dir" && "$JAVA" -jar ../neoforge-installer.jar --installServer > installer.log 2>&1)
            fi
            jar=$(ls -t neoforge/build/libs/reterraforged-*-neoforge-*.jar | head -1)
            start=("$JAVA" -Drtf.selftest=true "-Drtf.run=$TAG" -Xmx$HEAP @libraries/net/neoforged/neoforge/$NEOFORGE/$ARGS_FILE nogui) ;;
        fabric)
            # FABRIC_LOADER= / FABRIC_API= other versions than gradle.properties' (mods that need newer ones): the
            # installed pair is remembered and replaced when it differs
            if [ ! -f "$dir/fabric-server-launch.jar" ] || [ "$(cat "$dir/fabric-versions.txt" 2>/dev/null)" != "$FABRIC_LOADER $FABRIC_API" ]; then
                rm -f "$dir/fabric-server-launch.jar" "$dir/mods/fabric-api.jar"; rm -rf "$dir/.fabric"
                curl -fsSL -o "$dir/fabric-server-launch.jar" "https://meta.fabricmc.net/v2/versions/loader/$MC/$FABRIC_LOADER/1.0.1/server/jar"
                curl -fsSL -o "$dir/mods/fabric-api.jar" "https://cdn.modrinth.com/data/P7dR8mSH/versions/$(curl -fsSL "https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%22$MC%22%5D&loaders=%5B%22fabric%22%5D" | $PY -c 'import json,sys; v=[x for x in json.load(sys.stdin) if x["version_number"]=="'$FABRIC_API'"]; print(v[0]["id"]+"/"+v[0]["files"][0]["filename"])')"
                echo "$FABRIC_LOADER $FABRIC_API" > "$dir/fabric-versions.txt"
            fi
            jar=$(ls -t fabric/build/libs/reterraforged-*-fabric-*.jar | head -1)
            start=("$JAVA" -Drtf.selftest=true "-Drtf.run=$TAG" -Xmx$HEAP -jar fabric-server-launch.jar nogui) ;;
    esac
    rm -f "$dir"/mods/reterraforged-*.jar
    cp "$jar" "$dir/mods/"
    bash tools/extramods.sh $loader "$dir/mods" $MODS || { FAILS=$((FAILS+1)); continue; }
    echo "eula=true" > "$dir/eula.txt"
    props="online-mode=false\nserver-port=$PORT\nlevel-seed=${SEED:-20261013}\nspawn-protection=0\nmax-players=5\nmax-tick-time=-1\n"

    # pass 1: export the preset datapack
    rm -rf "$dir/world-export" "$dir/reterraforged-preset.zip"
    printf "${props}level-name=world-export\n" > "$dir/server.properties"
    start_server "$dir" export.log
    if wait_for "$dir/export.log" 'Done (' 600; then
        echo "rtf exportpreset${PRESET:+ $PRESET}" >> "$dir/console.in"
        wait_for "$dir/export.log" '\[RTF\] \(exported\|preset export failed\)' 300
        if [ -n "$MODS" ]; then
            echo "rtf biomemods" >> "$dir/console.in"
            wait_for "$dir/export.log" 'RTF biomemods\] \(vanilla\|FAIL\)' 1200
        fi
    fi
    stop_server "$dir" export.log
    if [ ! -f "$dir/reterraforged-preset.zip" ]; then
        echo "$loader: preset export failed"; grep -m8 -E "Exception|ERROR|RTF" "$dir/export.log"; FAILS=$((FAILS+1)); continue
    fi

    # LEGACY=<old version> (e.g. 0.0.9): a world made by that release of the mod with its own preset datapack, then the
    # same world opened with this build, and pass 2 makes a new world with that old preset on this build: the chunks
    # the old version generated and the ones this build generates must have the same ground (no seams in old worlds)
    legacy=PASS
    if [ -n "$LEGACY" ]; then
        old="run/legacy/$LEGACY"; mkdir -p "$old"
        rel="https://github.com/Vladeezyy/reterraforged/releases/download/v$LEGACY-$MC"
        oldjar="$old/reterraforged-$LEGACY-$loader-$MC.jar"
        [ -s "$oldjar" ] || curl -fsSL -o "$oldjar" "$rel/reterraforged-$LEGACY-$loader-$MC.jar"
        [ -s "$old/preset.zip" ] || curl -fsSL -o "$old/preset.zip" "$rel/reterraforged-preset-$MC.zip"
        rm -rf "$dir/world-legacy"; mkdir -p "$dir/world-legacy/datapacks"; cp "$old/preset.zip" "$dir/world-legacy/datapacks/"
        printf "${props}level-name=world-legacy\n" > "$dir/server.properties"
        rm -f "$dir"/mods/reterraforged-*.jar; cp "$oldjar" "$dir/mods/"
        start_server "$dir" legacy-old.log
        if wait_for "$dir/legacy-old.log" 'Done (' 600; then
            echo "rtf selftest" >> "$dir/console.in"
            wait_for "$dir/legacy-old.log" 'RTF selftest\] \(PASS all\|FAIL [0-9]*/\)' 900
        fi
        stop_server "$dir" legacy-old.log
        rm -f "$dir"/mods/reterraforged-*.jar "$dir/rtf-heights-legacy.txt"; cp "$jar" "$dir/mods/"
        start_server "$dir" legacy-new.log
        if wait_for "$dir/legacy-new.log" 'Done (' 600; then
            echo "rtf heights legacy" >> "$dir/console.in"
            wait_for "$dir/legacy-new.log" 'RTF heights\]' 300
        fi
        stop_server "$dir" legacy-new.log
        cp "$dir/rtf-heights-legacy.txt" "run/heights-legacy-$loader.txt" 2>/dev/null || legacy="FAIL no heights from the old world"
        cp "$old/preset.zip" "$dir/reterraforged-preset.zip"
    fi
    # PRESET_ZIP=<file>: pass 2 uses this preset datapack (for example the one a GUI world saved) instead of the export
    [ -z "$PRESET_ZIP" ] || cp "$PRESET_ZIP" "$dir/reterraforged-preset.zip"

    # pass 2: a new world with the preset, self-test
    rm -rf "$dir/world"
    mkdir -p "$dir/world/datapacks"
    # NOPRESET=1: the second world without the preset (vanilla generation), as a reference
    [ -n "$NOPRESET" ] || cp "$dir/reterraforged-preset.zip" "$dir/world/datapacks/"
    printf "${props}level-name=world\n" > "$dir/server.properties"
    start_server "$dir" console.log
    if ! wait_for "$dir/console.log" 'Done (' 600; then
        echo "$loader: server did not start"; grep -m5 -E "Exception|Error" "$dir/console.log"; FAILS=$((FAILS+1)); stop_server "$dir" console.log; continue
    fi
    if [ -n "$SKIP_SELFTEST" ]; then
        line="RTF selftest] skipped (SKIP_SELFTEST=1), PASS all not checked"
    else
        echo "rtf selftest" >> "$dir/console.in"
        wait_for "$dir/console.log" 'RTF selftest\] \(PASS all\|FAIL [0-9]*/\)' 900
        line=$(grep -o 'RTF selftest\] \(PASS all\|FAIL [0-9]*/\).*' "$dir/console.log" | tail -1)
    fi
    # the world's shape as a file (RTFWorldChecks.heights): compared between the loaders below, and with HEIGHTS_REF
    rm -f "$dir/rtf-heights.txt" "run/heights-$loader.txt"
    echo "rtf heights" >> "$dir/console.in"
    wait_for "$dir/console.log" 'RTF heights\]' 300 && cp "$dir/rtf-heights.txt" "run/heights-$loader.txt" 2>/dev/null
    # WEATHER=1: how much of the land snows instead of raining because of its height (RTFWorldChecks.weather)
    if [ -n "$WEATHER" ]; then
        echo "rtf weather" >> "$dir/console.in"
        wait_for "$dir/console.log" 'RTF weather\]' 600
        grep -o 'RTF weather\].*' "$dir/console.log" | tail -1 | tee "run/weather-$loader.txt"
    fi
    # EXTREMES=1 (with PRESET=set:...seaLevel=...;...lavaLevel=...): bedrock and the levels of such a world
    extremes=PASS
    if [ -n "$EXTREMES" ]; then
        echo "rtf extremes" >> "$dir/console.in"
        wait_for "$dir/console.log" 'RTF extremes\] \(PASS\|FAIL\)' 600
        extremes=$(grep -o 'RTF extremes\] \(PASS\|FAIL\).*' "$dir/console.log" | tail -1)
    fi
    # DUNGEONS=<N>: the dungeon spawners over N x N chunks (compare with NOPRESET=1)
    if [ -n "$DUNGEONS" ]; then
        echo "rtf dungeons $DUNGEONS" >> "$dir/console.in"
        wait_for "$dir/console.log" 'RTF dungeons\]' 1800
        grep -o 'RTF dungeons\].*' "$dir/console.log" | tail -1 | sed 's/^/  /'
    fi
    # every overworld structure near the spawn, built and checked (RTFStructureTest); SKIP_STRUCTURES=1 for quick runs
    if [ -n "$SKIP_STRUCTURES" ]; then
        structures="RTF structures] PASS (skipped)"
    else
        echo "rtf structures" >> "$dir/console.in"
        wait_for "$dir/console.log" 'RTF structures\] \(PASS\|FAIL\)' 1200
        structures=$(grep -o 'RTF structures\] \(PASS\|FAIL\).*' "$dir/console.log" | tail -1 | cut -c1-400)
    fi
    if [ -n "$MODS" ]; then
        echo "rtf biomemods" >> "$dir/console.in"
        wait_for "$dir/console.log" 'RTF biomemods\] \(rtf\|FAIL\)' 1200
    fi
    stop_server "$dir" console.log
    # not the mod: NeoForge 21.11+ on Windows logs failed netty epoll/kqueue native probes when the network starts;
    # Biomes O' Plenty on a Fabric server: a client class of Fabric API it names, its boats without a data fixer;
    # 26.2+ reads system info with OSHI, which fails on Windows with broken performance counters (Perflib)
    noise='Error loading class: net/fabricmc/fabric/api/client|No data fixer registered for|Yggdrasil|api\.minecraftservices\.com/publickeys|SocketTimeoutException: Connect timed out|io\.netty\.channel\.(epoll|kqueue)|Only supported on (Linux|OSX/BSD)|oshi\.|Win32Exception|Perflib'
    errors=$(grep -E '/ERROR\]|Exception|Caused by' "$dir/console.log" | grep -vE "$noise" | grep -c .)
    echo "$loader: ${line:-no result}; errors in log: $errors"
    echo "  ${structures:-RTF structures] no result}"
    [ -n "$EXTREMES" ] && echo "  ${extremes:-RTF extremes] no result}"
    # HEIGHTS_REF=<file> [HEIGHTS_MODE=heights|ground|all]: the same shape as another run (a world without other mods,
    # another version of the mod)
    if [ -n "$LEGACY" ] && [[ "$legacy" == PASS ]]; then
        legacy=$($PY tools/compareheights.py "run/heights-legacy-$loader.txt" "run/heights-$loader.txt" ground)
        echo "  a world of $LEGACY opened with this build, against a new one: $legacy"
    fi
    shape=PASS
    if [ -n "$HEIGHTS_REF" ]; then
        shape=$($PY tools/compareheights.py "$HEIGHTS_REF" "run/heights-$loader.txt" "${HEIGHTS_MODE:-heights}")
        echo "  world shape against $HEIGHTS_REF: $shape"
    fi
    grep -E '/ERROR\]|Exception' "$dir/console.log" | grep -vE "$noise" | sed 's/^\[[0-9:]*\] //' | cut -c1-160 | sort | uniq -c | sort -rn | head -8
    biomemods=PASS
    if [ -n "$MODS" ]; then
        # the other mods' biomes as in a vanilla world with the same mods: at least 80 % of the biome kinds found
        # there, a share of the surface at least half as large, not more often under water, and their own surface
        # blocks in as many biomes (less 10 %)
        ref=$(grep -o 'RTF biomemods\] vanilla.*' "$dir/export.log" | tail -1)
        got=$(grep -o 'RTF biomemods\] rtf.*' "$dir/console.log" | tail -1)
        echo "  ${ref:-RTF biomemods] no vanilla result}"
        echo "  ${got:-RTF biomemods] no result}"
        biomemods=$($PY -c '
import re, sys
def p(s):
    m = re.search(r"found=(\d+)/(\d+) share=([\d.]+) underwater=([\d.]+) surface=(\d+)/(\d+)", s)
    return [float(x) for x in m.groups()] if m else None
r, g = p(sys.argv[1]), p(sys.argv[2])
if not r or not g: print("FAIL no result"); sys.exit()
bad = []
if g[0] < 0.8 * r[0]: bad.append("fewer biome kinds")
if g[2] < 0.5 * r[2]: bad.append("smaller share")
if g[3] > r[3] + 10: bad.append("more under water")
if g[4] / max(1, g[0]) < r[4] / max(1, r[0]) - 0.1: bad.append("fewer own surfaces")
print("PASS" if not bad else "FAIL " + ", ".join(bad))' "$ref" "$got")
        echo "  biomes of other mods: $biomemods (tables: $dir/rtf-biomemods-*.txt)"
    fi
    # LEGACY: the self test judges the old preset (no bee nest tree before 0.0.12, forge: biome modifiers before 0.0.10), only the ground is compared
    [[ ( -n "$LEGACY" || -n "$SKIP_SELFTEST" || "$line" == *"PASS all"* ) && "$errors" == 0 && "$structures" == *"PASS"* && "$biomemods" == PASS && "$extremes" == *PASS* && "$shape" == PASS* && "$legacy" == PASS* ]] || FAILS=$((FAILS+1))
done
# both loaders on the same seed and preset: the same world (heights, ground and biomes)
if [[ " $LOADERS " == *" neoforge "* && " $LOADERS " == *" fabric "* && -f run/heights-neoforge.txt && -f run/heights-fabric.txt ]]; then
    parity=$($PY tools/compareheights.py run/heights-neoforge.txt run/heights-fabric.txt all)
    echo "NeoForge and Fabric: $parity"
    [[ "$parity" == PASS* ]] || FAILS=$((FAILS+1))
fi
echo; [ $FAILS = 0 ] && echo "ALL PASS" || { echo "$FAILS FAILED"; exit 1; }
exit 0
}
