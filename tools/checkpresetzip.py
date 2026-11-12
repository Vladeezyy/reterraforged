"""Checks an exported preset datapack: tools/checkpresetzip.py <reterraforged-preset.zip>

Both loaders' biome modifier folders are there (NeoForge reads data/<ns>/neoforge/biome_modifier, Fabric
data/<ns>/forge/biome_modifier, each skips the other's) with the same files and the same bytes: a pack with one folder
gave a world without the mod's trees, erosion and swamp surface on the other loader. Prints "PASS ..." or "FAIL ...".
"""
import sys
import zipfile

FOLDERS = ('neoforge/biome_modifier/', 'forge/biome_modifier/')

with zipfile.ZipFile(sys.argv[1]) as pack:
    found = {folder: {} for folder in FOLDERS}
    for name in pack.namelist():
        parts = name.split('/', 2)
        if len(parts) < 3 or parts[0] != 'data' or name.endswith('/'):
            continue
        for folder in FOLDERS:
            if parts[2].startswith(folder):
                found[folder][parts[1] + ':' + parts[2][len(folder):]] = pack.read(name)
neo, fabric = (found[folder] for folder in FOLDERS)
bad = []
if not neo:
    bad.append('no ' + FOLDERS[0])
if not fabric:
    bad.append('no ' + FOLDERS[1])
if set(neo) != set(fabric):
    bad.append('only in %s: %s; only in %s: %s' % (FOLDERS[0], sorted(set(neo) - set(fabric)) or '-', FOLDERS[1], sorted(set(fabric) - set(neo)) or '-'))
changed = sorted(k for k in set(neo) & set(fabric) if neo[k] != fabric[k])
if changed:
    bad.append('different content: %s' % changed)
# the file the self test looks for to say the pack's biome modifiers reached the world
if not any(k.endswith('/add_swamp_surface.json') or k.endswith(':add_swamp_surface.json') for k in neo):
    bad.append('no add_swamp_surface.json')
print('FAIL %s' % '; '.join(bad) if bad else 'PASS %d biome modifiers in both folders, identical' % len(neo))
sys.exit(1 if bad else 0)
