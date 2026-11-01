"""Compares two "rtf fingerprint" files (RTFFingerprint): tools/comparefingerprint.py <a> <b>

surface: chunks generated to the surface (noise, aquifers, surface rules), full: fully generated chunks (features too),
base: the generator's own height and biome on a grid. Prints "PASS ..." or "FAIL ..." with the first differences.
"""
import sys


def read(path):
    rows = {}
    for line in open(path, encoding='utf-8'):
        p = line.split()
        if p and p[0] != '#' and p[0] != 'fullblocks':
            rows[(p[0], p[1], p[2])] = ' '.join(p[3:])
    return rows


a, b = read(sys.argv[1]), read(sys.argv[2])
diffs = {}
for k in sorted(set(a) | set(b)):
    if a.get(k) != b.get(k):
        diffs.setdefault(k[0], []).append(k)
if not diffs:
    print('PASS %d lines identical' % len(a))
else:
    counts = ', '.join('%s %d of %d' % (kind, len(v), sum(1 for k in a if k[0] == kind)) for kind, v in sorted(diffs.items()))
    first = '; '.join('%s %s %s: %s -> %s' % (k[0], k[1], k[2], a.get(k), b.get(k)) for v in diffs.values() for k in v[:3])
    print('FAIL differ: %s (%s)' % (counts, first))
    # what the differing full chunks have differently (block@section counts)
    blocks_a, blocks_b = {}, {}
    for path, out in ((sys.argv[1], blocks_a), (sys.argv[2], blocks_b)):
        for line in open(path, encoding='utf-8'):
            p = line.split()
            if p and p[0] == 'fullblocks':
                out[(p[1], p[2])] = dict(kv.split('=') for kv in p[3:])
    for k in diffs.get('full', [])[:5]:
        ca, cb = blocks_a.get((k[1], k[2]), {}), blocks_b.get((k[1], k[2]), {})
        d = {n: int(cb.get(n, 0)) - int(ca.get(n, 0)) for n in set(ca) | set(cb) if ca.get(n) != cb.get(n)}
        print('  full %s %s: %s' % (k[1], k[2], ' '.join('%s%+d' % (n, v) for n, v in sorted(d.items()))))
