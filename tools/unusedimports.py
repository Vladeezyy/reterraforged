"""Removes imports a source file does not use: tools/unusedimports.py <file.java>... (all of common, neoforge and
fabric when no file is given). An import is unused when its simple name appears nowhere else in the file (code,
javadoc links and comments count as uses, so nothing that is named survives wrongly removed; wildcards are kept).
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
files = sys.argv[1:]
if not files:
    for m in ('common', 'neoforge', 'fabric'):
        for dirpath, _, names in os.walk(os.path.join(ROOT, m, 'src')):
            files += [os.path.join(dirpath, n) for n in names if n.endswith('.java')]

total = 0
for path in files:
    src = open(path, encoding='utf-8').read()
    lines = src.split('\n')
    body = '\n'.join(l for l in lines if not l.startswith('import '))
    keep = []
    removed = 0
    for l in lines:
        m = re.match(r'import (static )?([\w.]+)\.(\w+);\s*$', l)
        if m and m.group(3) != '*':
            name = m.group(3)
            if not re.search(r'(?<![\w$])' + re.escape(name) + r'(?![\w$])', body):
                removed += 1
                continue
        keep.append(l)
    if removed:
        # no run of blank lines left where the imports were (the header only: up to the last import)
        last = max((i for i, l in enumerate(keep) if l.startswith('import ')), default=0)
        head, tail = keep[:last + 2], keep[last + 2:]
        collapsed = []
        for l in head:
            if l.strip() == '' and collapsed and collapsed[-1].strip() == '':
                continue
            collapsed.append(l)
        open(path, 'w', encoding='utf-8').write('\n'.join(collapsed + tail))
        total += removed
        print('%3d %s' % (removed, os.path.relpath(path, ROOT)))
print('removed %d imports' % total)
