# Generates common/.../server/stress/PresetStressTable.java and data/worldgen/preset/settings/PresetLimitTable.java
# from the preset screen pages: every setting the
# preset screen can change (float / int sliders with their ranges, toggles, cycle buttons, random seed buttons) as
# a typed setter on a Preset, for the stress test (RTFStressTest). Run it again when the pages change:
#   python tools/stresstable.py
import os
import re

ROOT = os.path.join(os.path.dirname(__file__), '..')
PAGES = os.path.join(ROOT, 'common/src/main/java/raccoonman/reterraforged/client/gui/screen/presetconfig')
OUT = os.path.join(ROOT, 'common/src/main/java/raccoonman/reterraforged/server/stress/PresetStressTable.java')
LIMITS_OUT = os.path.join(ROOT, 'common/src/main/java/raccoonman/reterraforged/data/worldgen/preset/settings/PresetLimitTable.java')

# limits wider than the slider: values built-in presets use (Legacy Lite and Legacy Huge Biomes have -1 for the
# island control points), so holding presets to the limits never changes a built-in one
LIMIT_OVERRIDES = {
    'world().controlPoints.islandInland': ('-1.0F', None),
    'world().controlPoints.islandCoast': ('-1.0F', None),
}

CALL = re.compile(r'PresetWidgets\.(createFloatSlider|createIntSlider|createToggle|createCycle|createRandomButton)\(')


def split_args(text, start):
    # arguments of the call whose '(' is at text[start - 1], up to its ')'
    depth, args, cur, i = 0, [], '', start
    in_str = False
    while i < len(text):
        c = text[i]
        if in_str:
            cur += c
            if c == '"' and text[i - 1] != '\\':
                in_str = False
        elif c == '"':
            in_str = True
            cur += c
        elif c in '([{':
            depth += 1
            cur += c
        elif c in ')]}':
            if depth == 0:
                args.append(cur.strip())
                return args, i
            depth -= 1
            cur += c
        elif c == ',' and depth == 0:
            args.append(cur.strip())
            cur = ''
        else:
            cur += c
        i += 1
    raise ValueError('unbalanced call')


def bindings(text):
    # local variable -> expression on the preset 'p'
    found = {'preset': 'p'}
    for m in re.finditer(r'^\s*[A-Z][\w.<>]* (\w+) = (\w+)((?:\.\w+(?:\(\))?)+);', text, re.M):
        name, base, rest = m.groups()
        if base in found:
            found[name] = found[base] + rest
    return found


def target(lambda_text, names):
    # the first field of the preset the lambda assigns to
    for m in re.finditer(r'(?:^|[{;]\s*)([a-z]\w*(?:\.\w+)+)\s*=[^=]', lambda_text, re.S):
        parts = m.group(1).split('.')
        if parts[0] in names:
            return names[parts[0]] + ''.join('.' + p for p in parts[1:])
    return None


def number(expr, suffix):
    expr = expr.strip()
    if re.fullmatch(r'-?[\d.]+[FfDd]?', expr):
        v = expr.rstrip('FfDd')
        return v + suffix if suffix == 'F' and '.' in v else (v + '.0F' if suffix == 'F' else v)
    return expr


def main():
    entries, imports, limits = [], set(), []
    for name in sorted(os.listdir(PAGES)):
        if not name.endswith('Page.java'):
            continue
        text = open(os.path.join(PAGES, name), encoding='utf-8').read()
        names = bindings(text)
        page_imports = dict((imp.split('.')[-1], imp) for imp in re.findall(r'^import ([\w.]+);', text, re.M))
        for m in CALL.finditer(text):
            kind = m.group(1)
            args, _ = split_args(text, m.end())
            lam = next((a for a in args if '->' in a), None)
            path = target(lam, names) if lam else None
            if path is None:
                continue
            label = path[2:] if path.startswith('p.') else path
            if kind in ('createFloatSlider', 'createIntSlider'):
                # a range bound can be a constant of a settings class (RiverSettings.NO_SLOPE_LIMIT)
                for type_name in set(re.findall(r'\b([A-Z]\w*)\.', args[1] + ' ' + args[2])):
                    if type_name in page_imports:
                        imports.add(page_imports[type_name])
            setter = '(p, v) -> ' + path + ' = v'
            getter = 'p -> ' + path
            if kind == 'createFloatSlider':
                entries.append('Setting.ofFloat("%s", %s, %s, %s)' % (label, number(args[1], 'F'), number(args[2], 'F'), setter))
                low, high = LIMIT_OVERRIDES.get(label, (None, None))
                limits.append('Limit.ofFloat("%s", %s, %s, %s, %s)' % (label, low or number(args[1], 'F'), high or number(args[2], 'F'), getter, setter))
            elif kind == 'createIntSlider':
                low, high = number(args[1], ''), number(args[2], '')
                step = re.search(r'getNearestMultiple\([^,]+,[^,]+,\s*(\d+)\)', lam)
                if step:
                    # the page snaps the value to a multiple (world height also to at least one multiple)
                    if re.search(r'Math\.max\(getNearestMultiple', lam):
                        low = str(max(int(low), int(step.group(1))))
                    entries.append('Setting.ofInt("%s", %s, %s, %s).step(%s)' % (label, low, high, setter, step.group(1)))
                    limits.append('Limit.ofInt("%s", %s, %s, %s, %s, %s)' % (label, low, high, step.group(1), getter, setter))
                else:
                    entries.append('Setting.ofInt("%s", %s, %s, %s)' % (label, low, high, setter))
                    limits.append('Limit.ofInt("%s", %s, %s, 1, %s, %s)' % (label, low, high, getter, setter))
            elif kind == 'createToggle':
                entries.append('Setting.ofBoolean("%s", %s)' % (label, setter))
            elif kind == 'createRandomButton':
                entries.append('Setting.ofSeed("%s", %s)' % (label, setter))
            elif kind == 'createCycle':
                values = args[0]
                for type_name in set(re.findall(r'\b([A-Z]\w*)\.', values)):
                    if type_name in page_imports:
                        imports.add(page_imports[type_name])
                entries.append('Setting.ofEnum("%s", %s, %s)' % (label, values, setter))
    body = ',\n\t\t'.join(entries)
    imports_text = ''.join('import %s;\n' % i for i in sorted(imports))
    java = ('package raccoonman.reterraforged.server.stress;\n\n'
            'import java.util.List;\n\n'
            + imports_text +
            '\n// Generated by tools/stresstable.py from the preset screen pages (client/gui/screen/presetconfig): every\n'
            '// setting the preset screen can change, with the range of its slider.\n'
            'final class PresetStressTable {\n'
            '\tstatic final List<Setting<?>> SETTINGS = List.of(\n\t\t' + body + '\n\t);\n\n'
            '\tprivate PresetStressTable() {\n\t}\n}\n')
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, 'w', encoding='utf-8', newline='\n').write(java)
    print(len(entries), 'settings ->', os.path.relpath(OUT, ROOT))
    java = ('package raccoonman.reterraforged.data.worldgen.preset.settings;\n\n'
            'import java.util.List;\n\n'
            '// Generated by tools/stresstable.py from the preset screen pages (client/gui/screen/presetconfig): the range\n'
            '// of every number slider, which presets read from files are held to (PresetLimits).\n'
            'final class PresetLimitTable {\n'
            '\tstatic final List<PresetLimits.Limit<?>> LIMITS = List.of(\n\t\t' + ',\n\t\t'.join('PresetLimits.' + l for l in limits) + '\n\t);\n\n'
            '\tprivate PresetLimitTable() {\n\t}\n}\n')
    open(LIMITS_OUT, 'w', encoding='utf-8', newline='\n').write(java)
    print(len(limits), 'limits ->', os.path.relpath(LIMITS_OUT, ROOT))


main()
