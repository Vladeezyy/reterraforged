#!/bin/bash
# Screenshots for the README with shaders: tools/showcase.sh (Fabric dev client, after tools/servertest.sh fabric).
# Downloads Iris + Sodium for this Minecraft version and the Complementary Reimagined shader pack from Modrinth into
# fabric/run (not committed), runs the client check (tools/clienttest.sh) with them, then takes the mods out again so
# the normal client check stays without shaders. Screenshots: run/showcase/.
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

for mod in iris sodium; do
    read -r url file < <(latest $mod fabric)
    [ -f "$RUN/mods/$file" ] || curl -fsSL -o "$RUN/mods/$file" "$url"
    echo "$mod: $file"
done
read -r url pack < <(curl -fsSL "https://api.modrinth.com/v2/project/complementary-reimagined/version" \
    | $PY -c 'import json,sys; v=json.load(sys.stdin)[0]["files"][0]; print(v["url"], v["filename"])' | tr -d '\r')
[ -f "$RUN/shaderpacks/$pack" ] || curl -fsSL -o "$RUN/shaderpacks/$pack" "$url"
echo "shaders: $pack"
printf 'enableShaders=true\nshaderPack=%s\n' "$pack" > "$RUN/config/iris.properties"

bash tools/clienttest.sh fabric
code=$?
cp run/clienttest-fabric/rtf-*.png "$OUT/" 2>/dev/null
rm -f "$RUN"/mods/iris-*.jar "$RUN"/mods/sodium-*.jar
printf 'enableShaders=false\n' > "$RUN/config/iris.properties"
echo "showcase screenshots: $(ls "$OUT"/*.png 2>/dev/null | wc -l) in $OUT"
exit $code
}
