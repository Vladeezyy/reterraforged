#!/bin/bash
# render.sh <dump dir> [scale=160] [strengthMul=1.0] [gradblur=48]: applies the erosion filter prototype to every
# ReTerraForged height dump (.npy/.json from `rtf heightdump`) in <dump dir> and writes comparison sheets to
# <dump dir>/render. Offline tool, not part of the mod; the filter files are MPL-2.0 (see their headers).
# gradblur: blocks of blur for the gully directions; 0 takes the raw per-pixel gradient, which ReTerraForged's
# small ripples scramble into a crumpled-paper look.
HERE=$(cd "$(dirname "$0")" && pwd)
J=${JAVA:-java}; JC=${JAVAC:-$(dirname "$J")/javac}; [ "$J" = java ] && JC=javac
DUMP=${1:?dump dir}; SCALE=${2:-160}; MUL=${3:-1.0}; GB=${4:-48}
OUT="$DUMP/render"; CLS="$HERE/build"; mkdir -p "$OUT" "$CLS"
"$JC" -d "$CLS" "$HERE/ErosionProto.java" "$HERE/RtfErosion.java" || exit 1
for f in "$DUMP"/*.npy; do
  [ -e "$f" ] || { echo "no .npy in $DUMP"; exit 1; }
  echo "== $f"
  "$J" -Dgradblur="$GB" -Xmx4g -cp "$CLS" RtfErosion rtf "$f" "$OUT" "$SCALE" "$MUL"
  "$J" -Dgradblur="$GB" -Xmx4g -cp "$CLS" RtfErosion tune "$f" "$OUT"
done
