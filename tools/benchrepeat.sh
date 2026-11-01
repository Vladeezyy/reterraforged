#!/bin/bash
# The generation speed of ReTerraForged, repeated: tools/benchrepeat.sh [neoforge|fabric] [runs] (default 3).
# Runs tools/benchmark.sh with MODES=reterraforged that many times and prints each run and the median ms per chunk.
# One run varies by a few percent (the JIT, other programs): compare medians of builds, not single runs.
cd "$(dirname "$0")/.."
LOADER=${1:-neoforge}
RUNS=${2:-3}
: > run/benchrepeat-$LOADER.txt
for i in $(seq 1 "$RUNS"); do
    MODES=reterraforged bash tools/benchmark.sh "$LOADER" > run/benchrepeat-$LOADER-$i.log 2>&1
    grep -o 'RTF bench\] ReTerraForged:.*' run/benchrepeat-$LOADER-$i.log | head -1 | tee -a run/benchrepeat-$LOADER.txt
done
sort -t, -k3 run/benchrepeat-$LOADER.txt | grep -o '[0-9.]* ms per chunk' | sort -n | awk '{a[NR]=$1} END {print "median " a[int((NR+1)/2)] " ms per chunk (" NR " runs)"}'
