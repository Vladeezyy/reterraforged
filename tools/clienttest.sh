#!/bin/bash
# Client check after tools/servertest.sh: tools/clienttest.sh [neoforge|fabric]
# Copies the server test world (made with the "Default" preset) into the dev client's saves, starts the dev client
# with -Drtf.clienttest (RTFClientTest): screenshot of the preset screen with its live preview, then the world is
# loaded and screenshots are taken at the highest mountain, a desert, a river and from above the spawn. The game
# quits by itself; screenshots are copied to run/clienttest-<loader>/.
{ # parsed as a whole before running, so editing this file during a run is safe
cd "$(dirname "$0")/.."
LOADER=${1:-neoforge}
WORLD=run/server-$LOADER/world
RUN=$LOADER/run
OUT=run/clienttest-$LOADER
if [ ! -f "$WORLD/level.dat" ]; then echo "no $WORLD, run tools/servertest.sh $LOADER first"; exit 1; fi

mkdir -p "$RUN/saves" "$OUT"
rm -rf "$RUN/saves/rtf-test" "$RUN"/screenshots/rtf-*.png "$OUT"/*.png
cp -r "$WORLD" "$RUN/saves/rtf-test"
rm -f "$RUN/saves/rtf-test/session.lock"
# no accessibility onboarding screen on the first start, and no pause when the window loses focus
if [ ! -f "$RUN/options.txt" ]; then
    printf 'onboardAccessibility:false\ntutorialStep:none\npauseOnLostFocus:false\nguiScale:2\n' > "$RUN/options.txt"
fi

./gradlew ":$LOADER:runClient" -PrtfClientTest=rtf-test --no-daemon > "$OUT/client.log" 2>&1
cp "$RUN"/screenshots/rtf-*.png "$OUT/" 2>/dev/null
grep -h "RTF clienttest" "$OUT/client.log" | sed 's/^.*\[RTF clienttest\]/ /'
# vanilla large dripstones can reach two chunks away and log "setBlock in a far chunk"; counted apart.
# Not the mod: Realms without an account in the dev client, NeoForge's note about Loom's dev-only class processor,
# OSHI system info on Windows with broken performance counters (26.2+)
noise='far chunk.*large_dripstone|Realms|ClassTransformStatistics|oshi.|Win32Exception|Perflib'
dripstone=$(grep -c 'far chunk.*large_dripstone' "$OUT/client.log")
errors=$(grep -E 'ERROR|Exception' "$OUT/client.log" | grep -vE "$noise" | grep -c .)
echo "screenshots: $(ls "$OUT"/*.png 2>/dev/null | wc -l) in $OUT; errors in log: $errors (+ $dripstone vanilla large_dripstone far-chunk lines)"
grep -E 'ERROR|Exception' "$OUT/client.log" | grep -vE "$noise" | cut -c1-200 | sort | uniq -c | sort -rn | head -8
grep -q "RTF clienttest\] done" "$OUT/client.log"
exit $?
}
