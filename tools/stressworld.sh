#!/bin/bash
# Real worlds with stress test presets: tools/stressworld.sh [neoforge|fabric] [presets...]
# Presets (server/stress/RTFStressTest.makePreset): min (every preset screen setting at its minimum), max (at its
# maximum), random:N (the stress test's random combination N); default: min max random:0 .. random:5.
# For each one tools/servertest.sh exports it as a datapack, makes a new world with it and runs "rtf selftest"
# (thousands of chunks in the three dimensions, the spawn, a village). Checks may fail with extreme settings (no sea
# with the sea level at 0); what matters is that worlds generate without errors, crashes or hangs. The server logs
# are kept in run/stressworld-<loader>-<preset>.log.
{ # parsed as a whole before running, so editing this file during a run is safe
cd "$(dirname "$0")/.."
LOADER=${1:-neoforge}
shift
PRESETS=${*:-"min max random:0 random:1 random:2 random:3 random:4 random:5"}
for preset in $PRESETS; do
    echo "== $preset"
    PRESET=$preset bash tools/servertest.sh "$LOADER" | grep -v '^$' | grep -v 'ALL PASS\|FAILED$'
    cp "run/server-$LOADER/console.log" "run/stressworld-$LOADER-${preset/:/-}.log" 2>/dev/null
    grep -h 'RTF selftest\] FAIL [a-z]' "run/server-$LOADER/console.log" | sed 's/^.*RTF selftest\]/   /' | head -12
done
exit 0
}
