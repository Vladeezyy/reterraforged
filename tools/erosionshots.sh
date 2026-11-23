#!/bin/bash
# PROTOTYPE (proto/erosion-filter): the same cameras in one world with different erosion, for comparison pictures.
#   tools/erosionshots.sh <world folder> <out dir> [variant ...]
# The world: a server test world, e.g. PRESET="preset:Towering Peaks" SKIP_SELFTEST=1 SKIP_STRUCTURES=1
# tools/servertest.sh fabric (its chunks at the cameras must not be generated yet: every variant generates them itself).
# Variants: nodrops (-Drtf.erosionfilter.drops=0), drops (as released), f05 (filter x0.5 + drops), f10 (filter x1 + drops),
# s035 / s040 (filter x0.35 / x0.4 with the first settings + drops), subtle / bold (ridges: ErosionFilterProto.SUBTLE / BOLD + drops).
# CAMS="name:x,y,z,yaw,pitch;..." (RTFClientTest), RENDER=<chunks> (24), TIME=<ticks> (9000), SHADERS=1 Iris + Sodium +
# Complementary Reimagined (as tools/showcase.sh), SODIUM=1 Sodium only. Screenshots: <out dir>/<variant>/<cam>.png.
{
cd "$(dirname "$0")/.."
WORLD=$1; OUTDIR=$2; shift 2
VARIANTS=${*:-"nodrops drops f05 f10"}
RUN=fabric/run
MC=$(grep "^minecraft_version=" gradle.properties | cut -d= -f2 | tr -d '\r')
PY=$(command -v python3 >/dev/null && python3 -c '' 2>/dev/null && echo python3 || echo python)
mkdir -p "$RUN/mods" "$RUN/shaderpacks" "$RUN/config" "$OUTDIR"
latest() {
    curl -fsSL "https://api.modrinth.com/v2/project/$1/version?game_versions=%5B%22$MC%22%5D&loaders=%5B%22$2%22%5D" \
        | $PY -c 'import json,sys; v=json.load(sys.stdin)[0]["files"][0]; print(v["url"], v["filename"])' | tr -d '\r'
}
rm -f "$RUN"/mods/iris-*.jar "$RUN"/mods/sodium-*.jar
if [ -n "$SHADERS$SODIUM" ]; then
    mods=sodium; [ -n "$SHADERS" ] && mods="iris sodium"
    for mod in $mods; do
        read -r url file < <(latest $mod fabric)
        mkdir -p run/modcache-shots
        [ -f "run/modcache-shots/$file" ] || curl -fsSL -o "run/modcache-shots/$file" "$url"
        cp "run/modcache-shots/$file" "$RUN/mods/"
        echo "$mod: $file"
    done
fi
if [ -n "$SHADERS" ]; then
    read -r url pack < <(curl -fsSL "https://api.modrinth.com/v2/project/complementary-reimagined/version" \
        | $PY -c 'import json,sys; v=json.load(sys.stdin)[0]["files"][0]; print(v["url"], v["filename"])' | tr -d '\r')
    [ -f "$RUN/shaderpacks/$pack" ] || curl -fsSL -o "$RUN/shaderpacks/$pack" "$url"
    printf 'enableShaders=true\nshaderPack=%s\n' "$pack" > "$RUN/config/iris.properties"
    # the pack's own clouds (the vanilla cloud option does not turn them off) and its haze off, the far mountains clear
    printf 'CLOUD_STYLE_DEFINE=0\nATM_FOG_MULT=0.50\nATM_FOG_DISTANCE=300\nBORDER_FOG=false\n' > "$RUN/shaderpacks/$pack.txt"
    echo "shaders: $pack"
fi
# no vanilla clouds either
touch "$RUN/options.txt"
if grep -q '^renderClouds:' "$RUN/options.txt"; then sed -i 's/^renderClouds:.*/renderClouds:"false"/' "$RUN/options.txt"; else echo 'renderClouds:"false"' >> "$RUN/options.txt"; fi
base="-Drtf.clientcams=$CAMS -Drtf.clientrender=${RENDER:-24} -Drtf.clienttime=${TIME:-9000} -Drtf.clientsettle=${SETTLE:-300}"
for v in $VARIANTS; do
    case $v in
        nodrops) props="-Drtf.erosionfilter.drops=0" ;;
        drops) props="" ;;
        f05) props="-Drtf.erosionfilter=0.5" ;;
        f10) props="-Drtf.erosionfilter=1.0" ;;
        s035) props="-Drtf.erosionfilter=0.35" ;;
        s040) props="-Drtf.erosionfilter=0.4" ;;
        subtle) props="-Drtf.erosionfilter.preset=subtle" ;;
        bold) props="-Drtf.erosionfilter.preset=bold" ;;
        *) echo "unknown variant $v"; continue ;;
    esac
    echo "== $v ($props) $(date +%T)"
    start=$SECONDS
    JDK_JAVA_OPTIONS="$base $props" WORLD="$WORLD" STALL=${STALL:-300} bash tools/clienttest.sh fabric
    echo "   $v: exit $?, $((SECONDS - start)) s"
    mkdir -p "$OUTDIR/$v"
    for f in run/clienttest-fabric/rtf-*.png; do [ -f "$f" ] && cp "$f" "$OUTDIR/$v/$(basename "$f" .png | sed 's/^rtf-//').png"; done
    cp run/clienttest-fabric/client.log "$OUTDIR/$v/client.log"
    grep -h "chunks around the camera.*settling\|PROTOTYPE" run/clienttest-fabric/client.log | sed 's/^.*\] //' | sed 's/^/   /'
done
rm -f "$RUN"/mods/iris-*.jar "$RUN"/mods/sodium-*.jar
printf 'enableShaders=false\n' > "$RUN/config/iris.properties"
}
