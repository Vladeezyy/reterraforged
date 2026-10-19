"""Compares two "rtf heights" files (RTFWorldChecks.heights): tools/compareheights.py <a> <b> [heights|ground|all|world]

heights: the generator's ground height at the grid (the world's shape), ground: the top of the ground in the generated
chunks, all: both and the biomes, world: like all, but the generator's height may be 1 block off (it is an estimate:
WorldLookup takes the eroded cell of a tile already in the cache and the raw one otherwise, so it depends on which
chunks were generated before; the blocks are not affected). Prints "PASS ..." or "FAIL ..." with the first differences.
"""
import sys


def read(path):
    base, ground = {}, {}
    for line in open(path, encoding='utf-8'):
        p = line.split()
        if not p or p[0] == '#':
            continue
        if p[0] == 'base':
            base[(p[1], p[2])] = (int(p[3]), p[4])
        elif p[0] == 'ground':
            ground[(p[1], p[2])] = int(p[3])
    return base, ground


a_base, a_ground = read(sys.argv[1])
b_base, b_ground = read(sys.argv[2])
mode = sys.argv[3] if len(sys.argv) > 3 else 'all'
diffs, near = [], 0
if mode in ('heights', 'all', 'world'):
    for k, v in a_base.items():
        w = b_base.get(k)
        if mode == 'world' and w is not None and abs(w[0] - v[0]) == 1:
            near += 1
        elif w is None or w[0] != v[0]:
            diffs.append(f'height at {k[0]},{k[1]}: {v[0]} / {w[0] if w else "-"}')
if mode in ('all', 'world'):
    for k, v in a_base.items():
        w = b_base.get(k)
        if w is not None and w[1] != v[1]:
            diffs.append(f'biome at {k[0]},{k[1]}: {v[1]} / {w[1]}')
if mode in ('ground', 'all', 'world'):
    for k, v in a_ground.items():
        w = b_ground.get(k)
        if w != v:
            diffs.append(f'ground at {k[0]},{k[1]}: {v} / {w}')
n = (len(a_base) if mode != 'ground' else 0) + (len(a_ground) if mode != 'heights' else 0)
if not a_base or (mode != 'heights' and not a_ground):
    print('FAIL empty sample file')
elif diffs:
    print(f'FAIL {len(diffs)} of {n} samples differ: ' + '; '.join(diffs[:6]))
else:
    print(f'PASS {n} samples identical ({mode})' + (f', {near} height estimates 1 block off (tile cache)' if near else ''))
