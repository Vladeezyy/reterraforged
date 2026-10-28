#!/bin/bash
# Client check after tools/servertest.sh: tools/clienttest.sh [neoforge|fabric] [gui|widgets]
# Copies the server test world (made with the "Default" preset) into the dev client's saves, starts the dev client
# with -Drtf.clienttest (RTFClientTest): screenshot of the preset screen with its live preview, then the world is
# loaded and screenshots are taken at the highest mountain, a desert, a river and from above the spawn. The game
# quits by itself; screenshots are copied to run/clienttest-<loader>/.
# "gui" instead makes a new world through the screens with mouse clicks, as a player does (RTFGuiTest): the preset
# list, the settings pages, Done twice, Create New World; checks the world is built with the edited preset.
# "widgets" uses every slider, switch and button of the settings pages (RTFWidgetTest), table in rtf-widgets.txt.
# MODS="biomes-o-plenty ..." installs these Modrinth projects into the dev client (tools/extramods.sh; the same as for
# servertest.sh, whose world is loaded), BIOMES="biomesoplenty:lavender_field,..." adds a screenshot in each biome.
{ # parsed as a whole before running, so editing this file during a run is safe
cd "$(dirname "$0")/.."
LOADER=${1:-neoforge}
MODE=${2:-world}
WORLD=${WORLD:-run/server-$LOADER/world}  # WORLD=<folder>: another world of the test server
RUN=$LOADER/run
OUT=run/clienttest-$LOADER
TEST=rtf-test
if [ "$MODE" = gui ] || [ "$MODE" = widgets ]; then
    OUT=run/${MODE}test-$LOADER
    TEST=$MODE
elif [ ! -f "$WORLD/level.dat" ]; then echo "no $WORLD, run tools/servertest.sh $LOADER first"; exit 1; fi

mkdir -p "$RUN/saves" "$OUT"
bash tools/extramods.sh $LOADER "$RUN/mods" $MODS || exit 1
[ -n "$BIOMES" ] && export JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS:+$JDK_JAVA_OPTIONS }-Drtf.clientbiomes=$BIOMES"
# SPOTS="x,z;x,z": a screenshot looking down at each place (the pits "rtf holes" lists); "x,z,distance,pitch" sets the camera
[ -n "$SPOTS" ] && export JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS:+$JDK_JAVA_OPTIONS }-Drtf.clientspots=$SPOTS"
# gui with RAIN=1: the new world is checked in the rain in the nearest plains (RTFRainProbe: rain sounds, the biome the
# rain is decided by, Ecliptic Seasons' surface biome when it is installed, #22); RAIN=vanilla makes the world with vanilla
# generation for comparison. The released jar does the same in a production client with -Drtf.clienttest=gui -Drtf.clientrain=true
[ -n "$RAIN" ] && export JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS:+$JDK_JAVA_OPTIONS }-Drtf.clientrain=true"
[ "$RAIN" = vanilla ] && export JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS -Drtf.clientrainvanilla=true"
rm -rf "$RUN/saves/rtf-test" "$RUN"/screenshots/rtf-*.png "$OUT"/*.png
if [ "$MODE" = gui ] || [ "$MODE" = widgets ]; then rm -rf "$RUN/saves/New World"* "$RUN/rtf-widgets.txt"; else
    cp -r "$WORLD" "$RUN/saves/rtf-test"
    rm -f "$RUN/saves/rtf-test/session.lock"
fi
# no accessibility onboarding screen on the first start, and no pause when the window loses focus
if [ ! -f "$RUN/options.txt" ]; then
    printf 'onboardAccessibility:false\ntutorialStep:none\npauseOnLostFocus:false\nguiScale:2\n' > "$RUN/options.txt"
fi

./gradlew ":$LOADER:runClient" -PrtfClientTest=$TEST --no-daemon > "$OUT/client.log" 2>&1 &
gradle=$!
# in dev runs Architectury's TransformerRuntime leaves non-daemon thread pools behind (seen with 26.2; not in the
# released jars), and the client then does not exit after "done" (NeoForge: never, Fabric: shutdown watchdog crash):
# a minute after the test is done, this checkout's client process is stopped
kill_client() {
    if [ "$(uname -s | cut -c1-5)" = MINGW ] || [ "$(uname -s | cut -c1-4)" = MSYS ]; then
        powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*rtf.clienttest=*' -and \$_.CommandLine.Replace([char]92, '/') -like '*$(pwd -W)/$LOADER/*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null
    else
        pkill -f "rtf.clienttest=.*$(pwd)/$LOADER/" 2>/dev/null
    fi
}
# hang watchdog (modding-kb testing/hang-watchdog.md): once the game runs, a log that stops growing for $STALL seconds
# (default 90; a modal screen, a deadlock) stops the client: HUNG, the run fails
done_at=; hung=; last_size=-1; last_change=$SECONDS
while kill -0 $gradle 2>/dev/null; do
    if [ -z "$done_at" ] && grep -q "RTF clienttest\] done" "$OUT/client.log"; then done_at=$SECONDS; fi
    if [ -n "$done_at" ] && [ $((SECONDS - done_at)) -gt 60 ]; then
        kill_client
        echo "client did not exit after the test, stopped" >> "$OUT/client.log"
        break
    fi
    # Fabric Loader's "Incompatible mods found!" window waits for a click before the game (and the log) starts
    if grep -qE 'Incompatible mods found|loader\.impl\.FormattedException' "$OUT/client.log"; then
        echo "FAILED: Fabric Loader refused the mods (window closed). Details:" | tee -a "$OUT/client.log"
        grep -E "requires|Replace mod|Remove mod" "$OUT/client.log" | head -6 | cut -c1-300
        hung=1; kill_client
        break
    fi
    size=$(wc -c < "$OUT/client.log" 2>/dev/null || echo 0)
    if [ "$size" != "$last_size" ]; then last_size=$size; last_change=$SECONDS
    elif [ -z "$done_at" ] && [ $((SECONDS - last_change)) -ge "${STALL:-90}" ] && grep -q 'Render thread' "$OUT/client.log"; then
        echo "HUNG: no client log output for ${STALL:-90}s, client stopped. Last lines:" | tee -a "$OUT/client.log"
        grep -vE '^\s*$' "$OUT/client.log" | tail -4 | cut -c1-300
        hung=1; kill_client
        break
    fi
    sleep 2
done
wait $gradle 2>/dev/null
cp "$RUN"/screenshots/rtf-*.png "$OUT/" 2>/dev/null
cp "$RUN/rtf-widgets.txt" "$OUT/" 2>/dev/null
grep -h "RTF clienttest" "$OUT/client.log" | sed 's/^.*\[RTF clienttest\]/ /'
# vanilla large dripstones can reach two chunks away and log "setBlock in a far chunk"; counted apart.
# Not the mod: Realms without an account in the dev client, NeoForge's note about Loom's dev-only class processor,
# OSHI system info on Windows with broken performance counters (26.2+), player certificates without an account,
# the client shutdown watchdog of a client that did not exit (see above; it fails to load its class after FML closed)
noise='far chunk.*large_dripstone|Realms|ClassTransformStatistics|oshi.|Win32Exception|Perflib|player/certificates|Failed to retrieve profile key pair|MinecraftClientHttpException|authlib.minecraft.client|ServerWatchdog|Client shutdown watchdog|Module minecraft has been closed'
dripstone=$(grep -c 'far chunk.*large_dripstone' "$OUT/client.log")
errors=$(grep -E 'ERROR|Exception' "$OUT/client.log" | grep -vE "$noise" | grep -c .)
echo "screenshots: $(ls "$OUT"/*.png 2>/dev/null | wc -l) in $OUT; errors in log: $errors (+ $dripstone vanilla large_dripstone far-chunk lines)"
grep -E 'ERROR|Exception' "$OUT/client.log" | grep -vE "$noise" | cut -c1-200 | sort | uniq -c | sort -rn | head -8
[ -z "$hung" ] || exit 124
grep -q "RTF clienttest\] done" "$OUT/client.log"
exit $?
}
