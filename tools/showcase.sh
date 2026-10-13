#!/bin/bash
# Screenshots for the README with shaders and a far view: tools/showcase.sh (Fabric dev client, after
# PRESET=showcase tools/servertest.sh fabric). Downloads Iris + Sodium + Distant Horizons for this Minecraft version and
# the Complementary Reimagined shader pack from Modrinth into fabric/run (not committed), runs the client check
# (tools/clienttest.sh) with them, a 256 chunk Distant Horizons view and a long wait at every place for the far
# terrain to generate, then takes the mods out again so the normal client check stays plain. Screenshots: run/showcase/.
{ # parsed as a whole before running, so editing this file during a run is safe
cd "$(dirname "$0")/.."
MC=$(grep "^minecraft_version=" gradle.properties | cut -d= -f2 | tr -d '\r')
RUN=fabric/run
OUT=run/showcase
PY=$(command -v python3 >/dev/null && python3 -c '' 2>/dev/null && echo python3 || echo python)
mkdir -p "$RUN/mods" "$RUN/shaderpacks" "$RUN/config" "$OUT"

# latest version file of a Modrinth project for this Minecraft version and loader -> "url filename"
latest() {
    curl -fsSL "https://api.modrinth.com/v2/project/$1/version?game_versions=%5B%22$MC%22%5D&loaders=%5B%22$2%22%5D" \
        | $PY -c 'import json,sys; v=json.load(sys.stdin)[0]["files"][0]; print(v["url"], v["filename"])' | tr -d '\r'  # Windows python: \r\n
}

for mod in iris sodium distanthorizons; do
    read -r url file < <(latest $mod fabric)
    [ -f "$RUN/mods/$file" ] || curl -fsSL -o "$RUN/mods/$file" "$url"
    echo "$mod: $file"
done
read -r url pack < <(curl -fsSL "https://api.modrinth.com/v2/project/complementary-reimagined/version" \
    | $PY -c 'import json,sys; v=json.load(sys.stdin)[0]["files"][0]; print(v["url"], v["filename"])' | tr -d '\r')
[ -f "$RUN/shaderpacks/$pack" ] || curl -fsSL -o "$RUN/shaderpacks/$pack" "$url"
echo "shaders: $pack"
printf 'enableShaders=true\nshaderPack=%s\n' "$pack" > "$RUN/config/iris.properties"
# shader options: no clouds (they hid the far terrain behind a layer at mountain height), the faintest atmospheric fog
# starting far away, so the Distant Horizons view reaches the horizon
printf 'CLOUD_STYLE_DEFINE=0\nATM_FOG_MULT=0.50\nATM_FOG_DISTANCE=300\n' > "$RUN/shaderpacks/$pack.txt"
# Distant Horizons: the farthest view it can fill in a few minutes, every core generating it
cat > "$RUN/config/DistantHorizons.toml" <<'DH'
[client]
	quickEnableRendering = true
	qualityPresetSetting = "CUSTOM"
	threadPresetSetting = "I_PAID_FOR_THE_WHOLE_CPU"
[client.advanced.graphics.quality]
	lodChunkRenderDistanceRadius = 256
	horizontalQuality = "EXTREME"
	verticalQuality = "EXTREME"
	maxHorizontalResolution = "BLOCK"
	lodBiomeBlending = 3
[client.advanced.graphics.fog]
	enableDhFog = false
[common.worldGenerator]
	generatorPlan = "SURFACE_THEN_CHUNKS"
	chunkGeneratorMode = "FEATURES"
	pauseIfMovingQuickly = false
	limitIfServerUnhealthy = false
DH
# the near view: 16 chunks
OPT="$RUN/options.txt"
if [ -f "$OPT" ]; then
    sed -i 's/^renderDistance:.*/renderDistance:16/; s/^simulationDistance:.*/simulationDistance:12/' "$OPT"
fi

# evening sun and lower cameras (RTFClientTest), passed to the client through the java launcher
JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS:+$JDK_JAVA_OPTIONS }-Drtf.showcase=true -Drtf.showcase.wait=${WAIT:-3000}" bash tools/clienttest.sh fabric
code=$?
cp run/clienttest-fabric/rtf-*.png "$OUT/" 2>/dev/null
rm -f "$RUN"/mods/iris-*.jar "$RUN"/mods/sodium-*.jar "$RUN"/mods/DistantHorizons-*.jar
printf 'enableShaders=false\n' > "$RUN/config/iris.properties"
echo "showcase screenshots: $(ls "$OUT"/*.png 2>/dev/null | wc -l) in $OUT"
exit $code
}
