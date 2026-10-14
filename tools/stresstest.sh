#!/bin/bash
# Stress test of the presets on a real dedicated server: tools/stresstest.sh [neoforge|fabric] [random variants]
# (after tools/servertest.sh for that loader, which installs the server). Starts the server with a throwaway world
# and runs "rtf stress" (server/stress/RTFStressTest): every preset screen setting at both ends of its slider, every
# built-in preset, all at minimum, all at maximum and random combinations, each through the preset codec, the
# datapack registries of a new world, the preview and world tiles with every filter, with a time limit. Prints the
# summary, the failures and the hangs. Settings table: tools/stresstable.py.
{ # parsed as a whole before running, so editing this file during a run is safe
cd "$(dirname "$0")/.."
LOADER=${1:-neoforge}
RANDOM_VARIANTS=${2:-40}
prop() { grep "^$1=" gradle.properties | cut -d= -f2- | tr -d '\r'; }
NEOFORGE=$(prop neoforge_version)
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) WIN=1; ARGS_FILE=win_args.txt ;; *) WIN=; ARGS_FILE=unix_args.txt ;; esac
if [ -z "$JAVA" ]; then
    if [ -n "$JAVA_HOME" ]; then JAVA="$JAVA_HOME/bin/java"; else JAVA=java; fi
fi
PORT=${PORT:-25599}
TAG=$(pwd | cksum | cut -d' ' -f1)
dir=run/server-$LOADER
if [ ! -d "$dir/mods" ]; then echo "no $dir, run tools/servertest.sh $LOADER first"; exit 1; fi

kill_servers() {
    if [ -n "$WIN" ]; then
        powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"CommandLine like '%rtf.run=$TAG %'\" | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null
    else
        pkill -f "rtf.run=$TAG " 2>/dev/null
    fi
}
wait_for() {
    # also stops when the server process is gone (it can die without a crash report, e.g. out of memory)
    for i in $(seq 1 "$3"); do grep -q "$2" "$1" && return 0; grep -q 'Crash report\|Failed to start' "$1" && return 1; kill -0 "$pid" 2>/dev/null || return 1; sleep 1; done
    return 1
}

mkdir -p run
if ! mkdir run/.servertest.lock 2>/dev/null; then
    if kill -0 "$(cat run/.servertest.lock/pid 2>/dev/null)" 2>/dev/null; then echo "a server test is already running in this checkout"; exit 1; fi
fi
echo $$ > run/.servertest.lock/pid
trap 'rm -rf run/.servertest.lock' EXIT
kill_servers

case $LOADER in
    neoforge) jar=$(ls -t neoforge/build/libs/reterraforged-*-neoforge-*.jar | head -1)
              start=("$JAVA" -Drtf.selftest=true "-Drtf.run=$TAG" -Xmx6G @libraries/net/neoforged/neoforge/$NEOFORGE/$ARGS_FILE nogui) ;;
    fabric)   jar=$(ls -t fabric/build/libs/reterraforged-*-fabric-*.jar | head -1)
              start=("$JAVA" -Drtf.selftest=true "-Drtf.run=$TAG" -Xmx6G -jar fabric-server-launch.jar nogui) ;;
esac
rm -f "$dir"/mods/reterraforged-*.jar
cp "$jar" "$dir/mods/"
rm -rf "$dir/world-stress"
printf "online-mode=false\nserver-port=$PORT\nlevel-seed=20261013\nmax-tick-time=-1\nlevel-name=world-stress\n" > "$dir/server.properties"
log=stress.log
: > "$dir/console.in"; : > "$dir/$log"
(cd "$dir" && { tail -f console.in & echo $! > tail.pid; wait; } | "${start[@]}" > "$log" 2>&1) &
pid=$!
if wait_for "$dir/$log" 'Done (' 600; then
    echo "rtf stress $RANDOM_VARIANTS" >> "$dir/console.in"
    # about two seconds per variant, hangs take the full time limit
    wait_for "$dir/$log" '\[RTF stress\] [0-9]* variants in' 14400
fi
echo "stop" >> "$dir/console.in"
wait_for "$dir/$log" 'All dimensions are saved' 120
sleep 3
kill "$(cat "$dir/tail.pid")" 2>/dev/null
kill_servers
wait $pid 2>/dev/null
grep -h '\[RTF stress\] [0-9]* variants in' "$dir/$log" | sed 's/^.*\[RTF stress\]/stress:/' || echo "stress: no result"
grep -hE '\[RTF stress\] ([0-9]+x |HANG )' "$dir/$log" | sed 's/^.*\[RTF stress\] /  /' | cut -c1-240
grep -m3 -hE 'OutOfMemoryError|Crash report|Exception in server tick loop' "$dir/$log" | sed 's/^/  server: /' | cut -c1-240
grep -q '\[RTF stress\] [0-9]* variants in .* 0 failed, 0 hung' "$dir/$log"
exit $?
}
