#!/bin/bash
# Runs the built mod on real dedicated servers, as players install it:
#   tools/servertest.sh [neoforge] [fabric]
# For each loader: installs the server (NeoForge installer / Fabric launcher, cached in run/), copies the mod jar,
# puts the preset datapack (tools/preset/reterraforged-preset.zip, exported from the in-game preset screen) into a fresh
# world, starts the server, runs "rtf selftest" (the mod's test command, enabled by -Drtf.selftest=true) and stops it.
# Prints the result line and every error / exception the log contains.
# eula=true: the Minecraft EULA is accepted for these local test servers.
cd "$(dirname "$0")/.."
LOADERS=${*:-"neoforge fabric"}
MC=1.21.1
NEOFORGE=21.1.201
FABRIC_LOADER=0.16.3
FABRIC_API=0.102.0+1.21.1
JAVA=${JAVA:-/opt/homebrew/opt/openjdk@21/bin/java}
PRESET=tools/preset/reterraforged-preset.zip
FAILS=0

for loader in $LOADERS; do
    dir=run/server-$loader
    mkdir -p "$dir/mods"
    case $loader in
        neoforge)
            if [ ! -f "$dir/run.sh" ]; then
                curl -fsSL -o run/neoforge-installer.jar "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEOFORGE/neoforge-$NEOFORGE-installer.jar"
                (cd "$dir" && "$JAVA" -jar ../neoforge-installer.jar --installServer > installer.log 2>&1)
            fi
            jar=$(ls neoforge/build/libs/reterraforged-*.jar | grep -v "sources\|shadow" | head -1)
            start=("$JAVA" -Drtf.selftest=true -Xmx3G @libraries/net/neoforged/neoforge/$NEOFORGE/unix_args.txt nogui) ;;
        fabric)
            if [ ! -f "$dir/fabric-server-launch.jar" ]; then
                curl -fsSL -o "$dir/fabric-server-launch.jar" "https://meta.fabricmc.net/v2/versions/loader/$MC/$FABRIC_LOADER/1.0.1/server/jar"
                curl -fsSL -o "$dir/mods/fabric-api.jar" "https://cdn.modrinth.com/data/P7dR8mSH/versions/$(curl -fsSL "https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%22$MC%22%5D&loaders=%5B%22fabric%22%5D" | python3 -c 'import json,sys; v=[x for x in json.load(sys.stdin) if x["version_number"].startswith("'$FABRIC_API'".split("+")[0])]; print(v[0]["id"]+"/"+v[0]["files"][0]["filename"])')"
            fi
            jar=$(ls fabric/build/libs/reterraforged-*-fabric-*.jar | head -1)
            start=("$JAVA" -Drtf.selftest=true -Xmx3G -jar fabric-server-launch.jar nogui) ;;
    esac
    rm -f "$dir"/mods/reterraforged-*.jar
    cp "$jar" "$dir/mods/"
    rm -rf "$dir/world"
    mkdir -p "$dir/world/datapacks"
    cp "$PRESET" "$dir/world/datapacks/"
    echo "eula=true" > "$dir/eula.txt"
    printf 'online-mode=false\nserver-port=25599\nlevel-seed=20261013\nspawn-protection=0\nmax-players=5\nmax-tick-time=-1\n' > "$dir/server.properties"
    : > "$dir/console.in"; : > "$dir/console.log"
    (cd "$dir" && tail -f console.in | "${start[@]}" > console.log 2>&1) &
    pid=$!; disown
    for i in $(seq 1 600); do grep -q 'Done (' "$dir/console.log" && break; grep -q 'Crash report\|Failed to start' "$dir/console.log" && break; sleep 1; done
    if ! grep -q 'Done (' "$dir/console.log"; then
        echo "$loader: server did not start"; grep -m5 -E "Exception|Error" "$dir/console.log"; FAILS=$((FAILS+1)); kill $pid 2>/dev/null; pkill -f "tail -f console.in"; continue
    fi
    echo "rtf selftest" >> "$dir/console.in"
    for i in $(seq 1 600); do grep -q 'RTF selftest\] \(PASS all\|FAIL [0-9]*/\)' "$dir/console.log" && break; sleep 1; done
    line=$(grep -o 'RTF selftest\] \(PASS all\|FAIL [0-9]*/\).*' "$dir/console.log" | tail -1)
    echo "stop" >> "$dir/console.in"
    for i in $(seq 1 60); do kill -0 $pid 2>/dev/null || break; sleep 1; done
    kill $pid 2>/dev/null; pkill -f "tail -f console.in" 2>/dev/null; wait $pid 2>/dev/null
    errors=$(grep -cE '/ERROR\]|Exception|Caused by' "$dir/console.log")
    echo "$loader: ${line:-no result}; errors in log: $errors"
    grep -E '/ERROR\]|Exception' "$dir/console.log" | sed 's/^\[[0-9:]*\] //' | cut -c1-160 | sort | uniq -c | sort -rn | head -8
    [[ "$line" == *"PASS all"* && "$errors" == 0 ]] || FAILS=$((FAILS+1))
done
echo; [ $FAILS = 0 ] && echo "ALL PASS" || { echo "$FAILS FAILED"; exit 1; }
