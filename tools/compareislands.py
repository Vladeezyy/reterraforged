"""Compares two "rtf islands" files (RTFIslandTest) of the same seed: tools/compareislands.py <a> <b>

Prints what differs on the land of the islands: the points of the grid (terrain, height, climate, the biome at the
surface and at y 0) and the real chunks around the middle and on the rims of islands (their hash, the blocks and biomes
on top).
"""
import sys


def read(path):
    points, chunks = {}, {}
    for line in open(path, encoding='utf-8'):
        p = line.split()
        if not p:
            continue
        if p[0] == 'point':
            points[(p[1], p[2])] = {'terrain': p[3], 'y': p[4], 'edge': p[5], 'continentalness': p[6], 'erosion': p[7], 'weirdness': p[8], 'surface': p[9], 'y0': p[10]}
        elif p[0] in ('chunk', 'rimchunk'):
            chunks[(p[0], p[1], p[2])] = (p[3], p[4], p[5])
    return points, chunks


pa, ca = read(sys.argv[1])
pb, cb = read(sys.argv[2])
common = set(pa) & set(pb)
parts = ['%d points, %d only in one' % (len(common), len(set(pa) ^ set(pb)))]
for field in ('y', 'terrain', 'edge', 'continentalness', 'erosion', 'weirdness', 'surface', 'y0'):
    differ = sum(1 for k in common if pa[k][field] != pb[k][field])
    parts.append('%s %d (%.1f%%)' % (field, differ, 100.0 * differ / max(1, len(common))))
chunks = set(ca) & set(cb)
parts.append('chunks hash %d of %d' % (sum(1 for k in chunks if ca[k][0] != cb[k][0]), len(chunks)))
parts.append('blocks on top %d' % sum(1 for k in chunks if ca[k][1] != cb[k][1]))
parts.append('biomes on top %d' % sum(1 for k in chunks if ca[k][2] != cb[k][2]))
print('differ: ' + ', '.join(parts))
