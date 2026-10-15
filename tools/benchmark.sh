#!/bin/bash
# World generation speed and where the time goes: tools/benchmark.sh [neoforge|fabric] (after tools/servertest.sh for
# that loader, which installs the server and exports the "Default" preset datapack).
# Two fresh worlds on the same seed: one with the preset datapack (ReTerraForged), one without it (vanilla generation,
# the baseline). In each, "rtf bench" generates 32 x 32 chunks far from the spawn to the full status and reports chunks
# per second; the server runs with spark ("spark tps" during the generation: TPS, tick times, CPU; printed in the
# console, nothing is uploaded) and a Java Flight Recorder recording,
# whose hottest methods are printed. Both are downloaded/kept in run/ and taken out of the server's mods afterwards.
# SIZE=n changes the square (n x n chunks); MODES="reterraforged" measures one world only.
{ # parsed as a whole before running, so editing this file during a run is safe
cd "$(dirname "$0")/.."
LOADER=${1:-neoforge}
SIZE=${SIZE:-32}
prop() { grep "^$1=" gradle.properties | cut -d= -f2- | tr -d '\r'; }
MC=$(prop minecraft_version)
NEOFORGE=$(prop neoforge_version)
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) WIN=1; ARGS_FILE=win_args.txt ;; *) WIN=; ARGS_FILE=unix_args.txt ;; esac
JAVA=${JAVA:-${JAVA_HOME:+$JAVA_HOME/bin/java}}; JAVA=${JAVA:-java}
JFR=$(dirname "$JAVA")/jfr
PY=$(command -v python3 >/dev/null && python3 -c '' 2>/dev/null && echo python3 || echo python)
PORT=${PORT:-25598}
TAG=$(pwd | cksum | cut -d' ' -f1)
dir=run/server-$LOADER
OUT=run/benchmark-$LOADER
[ -f "$dir/reterraforged-preset.zip" ] || { echo "no $dir/reterraforged-preset.zip, run tools/servertest.sh $LOADER first"; exit 1; }
mkdir -p "$OUT"
: > "$OUT/summary.txt"

start_server() {
    : > "$dir/console.in"; : > "$dir/$1"
    (cd "$dir" && { tail -f console.in & echo $! > tail.pid; wait; } | "${start[@]}" > "$1" 2>&1) &
    pid=$!
}
kill_servers() {
    if [ -n "$WIN" ]; then
        powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"CommandLine like '%rtf.run=$TAG %'\" | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null
    else
        pkill -f "rtf.run=$TAG " 2>/dev/null
    fi
}
wait_for() {
    for i in $(seq 1 "$3"); do grep -q "$2" "$dir/$1" && return 0; grep -q 'Crash report\|Failed to start\|Exception in server tick loop' "$dir/$1" && return 1; sleep 1; done
    return 1
}

# spark for this Minecraft version and loader (Modrinth)
read -r url file < <(curl -fsSL "https://api.modrinth.com/v2/project/spark/version?game_versions=%5B%22$MC%22%5D&loaders=%5B%22$LOADER%22%5D" \
    | $PY -c 'import json,sys; v=json.load(sys.stdin)[0]["files"][0]; print(v["url"], v["filename"])' | tr -d '\r')
[ -f "run/$file" ] || curl -fsSL -o "run/$file" "$url"
cp "run/$file" "$dir/mods/"
echo "spark: $file"

for mode in ${MODES:-reterraforged vanilla}; do
    world=world-bench-$mode
    rm -rf "$dir/$world" "$OUT/$mode.jfr"
    mkdir -p "$dir/$world/datapacks"
    [ $mode = reterraforged ] && cp "$dir/reterraforged-preset.zip" "$dir/$world/datapacks/"
    printf "online-mode=false\nserver-port=$PORT\nlevel-seed=20261013\nspawn-protection=0\nmax-tick-time=-1\nlevel-name=$world\n" > "$dir/server.properties"
    jfr="-XX:StartFlightRecording=filename=../../$OUT/$mode.jfr,settings=profile,dumponexit=true"
    case $LOADER in
        neoforge) start=("$JAVA" -Drtf.selftest=true "-Drtf.run=$TAG" -Xmx4G "$jfr" @libraries/net/neoforged/neoforge/$NEOFORGE/$ARGS_FILE nogui) ;;
        fabric) start=("$JAVA" -Drtf.selftest=true "-Drtf.run=$TAG" -Xmx4G "$jfr" -jar fabric-server-launch.jar nogui) ;;
    esac
    start_server bench.log
    if ! wait_for bench.log 'Done (' 600; then echo "$mode: server did not start"; kill_servers; continue; fi
    echo "rtf bench $SIZE" >> "$dir/console.in"
    # spark while the chunks generate, 8 s into the measured part: TPS, tick times, CPU
    wait_for bench.log '\[RTF bench\] warmed up' 600
    sleep 8
    echo "spark tps" >> "$dir/console.in"
    wait_for bench.log '\[RTF bench\] [a-zA-Z]*:' 1800
    echo "stop" >> "$dir/console.in"
    wait_for bench.log 'All dimensions are saved' 120; sleep 5
    kill "$(cat "$dir/tail.pid")" 2>/dev/null; kill_servers; wait $pid 2>/dev/null
    cp "$dir/bench.log" "$OUT/$mode.log"
    echo "== $(grep -o '\[RTF bench\].*' "$OUT/$mode.log" | tail -1)"
    # spark's reply, from its worker thread's log lines ("[?] ..." on 1.21.x, "System chat: [?] ..." on 26.x)
    grep 'spark-worker' "$OUT/$mode.log" | sed -E 's/\x1b\[[0-9;]*m//g; s/^[^]]*\] \[[^]]*\] \[[^]]*\]: //; s/^System chat: //; s/^\[\?\] ?//; s/^> //' \
        | sed -n '/TPS from last/,$p' | head -10 | sed -E 's/^([A-Z])/> \1/' > "$OUT/$mode-spark.txt"
    cat "$OUT/$mode-spark.txt"
    grep -o '\[RTF bench\].*' "$OUT/$mode.log" | tail -1 >> "$OUT/summary.txt"
    if [ -f "$OUT/$mode.jfr" ] && [ -x "$JFR" -o -x "$JFR.exe" ]; then
        "$JFR" view --width 160 hot-methods "$OUT/$mode.jfr" > "$OUT/$mode-hot-methods.txt" 2>&1
        head -32 "$OUT/$mode-hot-methods.txt"
    fi
done
rm -f "$dir/mods/$file"
}
