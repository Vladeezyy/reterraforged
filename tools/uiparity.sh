#!/bin/bash
# The world made through the preset screen against the world made from the exported datapack:
#   tools/uiparity.sh [neoforge|fabric]
# Runs the GUI test (tools/clienttest.sh <loader> gui: Default, the continent scale slider moved, Create World), which
# writes the world's shape (rtf heights) with its seed, then a real server world with that seed and the same preset
# ("Default" with that continent scale, exported as the datapack the server needs): ground and biomes must be
# identical, the generator's height estimate within 1 block (tools/compareheights.py world).
{ # parsed as a whole before running, so editing this file during a run is safe
cd "$(dirname "$0")/.."
LOADER=${1:-neoforge}
rm -f "$LOADER"/run/rtf-heights-gui-*.txt
bash tools/clienttest.sh "$LOADER" gui || { echo "GUI test failed"; exit 1; }
gui=$(ls -t "$LOADER"/run/rtf-heights-gui-*.txt 2>/dev/null | head -1)
[ -n "$gui" ] || { echo "FAIL the GUI test wrote no rtf-heights-gui file"; exit 1; }
seed=$(sed -n 's/^# seed //p' "$gui" | tr -d '\r')
scale=$(basename "$gui" .txt); scale=${scale#rtf-heights-gui-}
echo "GUI world: seed $seed, continent scale $scale ($gui)"
SEED=$seed PRESET="set:world().continent.continentScale=$scale" SKIP_STRUCTURES=1 HEIGHTS_REF="$gui" HEIGHTS_MODE=world \
    bash tools/servertest.sh "$LOADER"
}
