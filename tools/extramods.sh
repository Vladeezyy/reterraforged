#!/bin/bash
# Other mods for the test runs: tools/extramods.sh <loader> <mods folder> <modrinth slug>...
# Downloads the newest version of each project for this Minecraft version and loader from Modrinth, with its required
# dependencies (Fabric API excepted: servertest.sh installs the pinned one), into the mods folder. Earlier extra mods
# (listed in <mods folder>/.extramods) are removed first, so no slugs = no extra mods. Jars are cached in run/modcache.
# Used by servertest.sh and clienttest.sh through MODS="biomes-o-plenty ...".
cd "$(dirname "$0")/.."
LOADER=$1; DIR=$2; shift 2
MC=$(grep "^minecraft_version=" gradle.properties | cut -d= -f2 | tr -d '\r')
PY=$(command -v python3 >/dev/null && python3 -c '' 2>/dev/null && echo python3 || echo python)
mkdir -p "$DIR" run/modcache
if [ -f "$DIR/.extramods" ]; then
    while read -r f; do rm -f "$DIR/$f"; done < "$DIR/.extramods"
    rm -f "$DIR/.extramods"
fi
[ $# = 0 ] && exit 0

# newest version of a project (slug or id) for this Minecraft version and loader -> "url filename dep_id dep_id ..."
resolve() {
    curl -fsSL "https://api.modrinth.com/v2/project/$1/version?game_versions=%5B%22$MC%22%5D&loaders=%5B%22$LOADER%22%5D" | $PY -c '
import json, sys
v = json.load(sys.stdin)
if not v: sys.exit(1)
v = v[0]; f = next((f for f in v["files"] if f["primary"]), v["files"][0])
deps = [d["project_id"] for d in v["dependencies"] if d["dependency_type"] == "required" and d["project_id"]]
print(f["url"], f["filename"], *deps)' | tr -d '\r'
}

todo=("$@"); seen=" P7dR8mSH fabric-api "  # Fabric API: the pinned version from servertest.sh
while [ ${#todo[@]} -gt 0 ]; do
    p=${todo[0]}; todo=("${todo[@]:1}")
    [[ "$seen" == *" $p "* ]] && continue
    seen="$seen$p "
    read -r url file deps < <(resolve "$p") || { echo "extramods: no $p for $MC $LOADER"; exit 1; }
    [ -n "$url" ] || { echo "extramods: no $p for $MC $LOADER"; exit 1; }
    [ -f "run/modcache/$file" ] || curl -fsSL -o "run/modcache/$file" "$url" || exit 1
    cp "run/modcache/$file" "$DIR/"
    echo "$file" >> "$DIR/.extramods"
    echo "extra mod: $file"
    for d in $deps; do todo+=("$d"); done
done
exit 0
