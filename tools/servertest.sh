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
# Works in bash on macOS / Linux and in Git Bash on Windows. JAVA=... overrides the java binary (JDK 21).
# eula=true: the Minecraft EULA is accepted for these local test servers.
{ # parsed as a whole before running, so editing this file during a run is safe
cd "$(dirname "$0")/.."
LOADERS=${*:-"neoforge fabric"}
prop() { grep "^$1=" gradle.properties | cut -d= -f2- | tr -d '
'; }
MC=$(prop minecraft_version)
NEOFORGE=$(prop neoforge_version)
FABRIC_LOADER=$(prop fabric_loader_version)
FABRIC_API=$(prop fabric_api_version)
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

# wait_for <file> <regex> <seconds>
wait_for() {
    for i in $(seq 1 "$3"); do grep -q "$2" "$1" && return 0; grep -q 'Crash report\|Failed to start\|Exception in server tick loop\|Error executing task\|Mixin apply.*failed' "$1" && return 1; sleep 1; done
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
            if [ ! -f "$dir/fabric-server-launch.jar" ]; then
                curl -fsSL -o "$dir/fabric-server-launch.jar" "https://meta.fabricmc.net/v2/versions/loader/$MC/$FABRIC_LOADER/1.0.1/server/jar"
                curl -fsSL -o "$dir/mods/fabric-api.jar" "https://cdn.modrinth.com/data/P7dR8mSH/versions/$(curl -fsSL "https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%22$MC%22%5D&loaders=%5B%22fabric%22%5D" | $PY -c 'import json,sys; v=[x for x in json.load(sys.stdin) if x["version_number"]=="'$FABRIC_API'"]; print(v[0]["id"]+"/"+v[0]["files"][0]["filename"])')"
            fi
            jar=$(ls -t fabric/build/libs/reterraforged-*-fabric-*.jar | head -1)
            start=("$JAVA" -Drtf.selftest=true "-Drtf.run=$TAG" -Xmx$HEAP -jar fabric-server-launch.jar nogui) ;;
    esac
    rm -f "$dir"/mods/reterraforged-*.jar
    cp "$jar" "$dir/mods/"
    bash tools/extramods.sh $loader "$dir/mods" $MODS || { FAILS=$((FAILS+1)); continue; }
    echo "eula=true" > "$dir/eula.txt"
    props="online-mode=false\nserver-port=$PORT\nlevel-seed=20261013\nspawn-protection=0\nmax-players=5\nmax-tick-time=-1\n"

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

    # pass 2: a new world with the preset, self-test
    rm -rf "$dir/world"
    mkdir -p "$dir/world/datapacks"
    cp "$dir/reterraforged-preset.zip" "$dir/world/datapacks/"
    printf "${props}level-name=world\n" > "$dir/server.properties"
    start_server "$dir" console.log
    if ! wait_for "$dir/console.log" 'Done (' 600; then
        echo "$loader: server did not start"; grep -m5 -E "Exception|Error" "$dir/console.log"; FAILS=$((FAILS+1)); stop_server "$dir" console.log; continue
    fi
    echo "rtf selftest" >> "$dir/console.in"
    wait_for "$dir/console.log" 'RTF selftest\] \(PASS all\|FAIL [0-9]*/\)' 900
    line=$(grep -o 'RTF selftest\] \(PASS all\|FAIL [0-9]*/\).*' "$dir/console.log" | tail -1)
    # every overworld structure near the spawn, built and checked (RTFStructureTest)
    echo "rtf structures" >> "$dir/console.in"
    wait_for "$dir/console.log" 'RTF structures\] \(PASS\|FAIL\)' 1200
    structures=$(grep -o 'RTF structures\] \(PASS\|FAIL\).*' "$dir/console.log" | tail -1 | cut -c1-400)
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
    [[ "$line" == *"PASS all"* && "$errors" == 0 && "$structures" == *"PASS"* && "$biomemods" == PASS ]] || FAILS=$((FAILS+1))
done
echo; [ $FAILS = 0 ] && echo "ALL PASS" || { echo "$FAILS FAILED"; exit 1; }
exit 0
}
