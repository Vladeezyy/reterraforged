"""Code nothing uses: tools/deadcode.py (after ./gradlew build)

Reads the constant pools of the compiled classes of common, neoforge and fabric (javap -v): a method is a candidate when
no class refers to its name and descriptor (calls, method references, lambdas) and its source does not mark it
@Override (an override is called by Minecraft or the JDK); a class when no other class refers to it and it is not a
mixin, an entry point or named in a resource (mixin configs, service files). Candidates, not proof: reflection and
codecs built from strings are not seen, so check each before deleting it.
"""
import os
import re
import subprocess
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
MODULES = ['common', 'neoforge', 'fabric']
PKG = 'raccoonman/reterraforged'
JAVAP = os.path.join(os.environ.get('JAVA_HOME', ''), 'bin', 'javap') if os.environ.get('JAVA_HOME') else 'javap'

classes = []
for m in MODULES:
    base = os.path.join(ROOT, m, 'build', 'classes', 'java', 'main')
    for dirpath, _, files in os.walk(base):
        for f in files:
            if f.endswith('.class'):
                classes.append((m, os.path.join(dirpath, f), os.path.relpath(os.path.join(dirpath, f), base)[:-6]))

declared = []          # (class, name, descriptor, flags)
refs = set()           # (name, descriptor)
ctor_refs = set()      # (class, descriptor) of constructors called
field_refs = set()     # (class, name) of fields read or written
fields = []            # (class, name, flags)
records = set()
class_refs = {}        # class -> set of classes referring to it
for i in range(0, len(classes), 200):
    batch = classes[i:i + 200]
    out = subprocess.run([JAVAP, '-v', '-p'] + [c[1] for c in batch], capture_output=True, text=True).stdout
    current = None
    pending = None
    for line in out.splitlines():
        mc = re.match(r'^(?:public |protected |private |final |abstract |static |sealed |non-sealed |strictfp |synchronized )*(?:class|interface|enum|record|@interface) ([\w.$]+)', line)
        if mc and not line.startswith(' '):
            current = mc.group(1).replace('.', '/')
            if 'extends java.lang.Record' in line:
                records.add(current)
            continue
        # constant pool entries: Methodref, InterfaceMethodref (calls and method handles), Class
        mr = re.search(r'= (?:Methodref|InterfaceMethodref)\s+#\d+\.#\d+\s+// ([\w/$]+)\.("?[<>\w$]+"?):(\S+)', line)
        if mr:
            refs.add((mr.group(2).strip('"'), mr.group(3)))
            if mr.group(2).strip('"') == '<init>':
                ctor_refs.add((mr.group(1), mr.group(3)))
            continue
        fr = re.search(r'= Fieldref\s+#\d+\.#\d+\s+// ([\w/$]+)\.("?[\w$]+"?):', line)
        if fr:
            field_refs.add((fr.group(1), fr.group(2).strip('"')))
            continue
        fd = re.match(r'^  (?! )((?:public |protected |private |static |final |volatile |transient )*)[\w.$<>\[\], ?]+ ([\w$]+);$', line)
        if fd and current and not any(c in line for c in '#=:/('):
            fields.append((current, fd.group(2), fd.group(1)))
            continue
        cr = re.search(r'= Class\s+#\d+\s+// "?\[*L?([\w/$]+)', line)
        if cr and current:
            target = cr.group(1).split('$')[0]
            if target != current.split('$')[0]:
                class_refs.setdefault(target, set()).add(current.split('$')[0])
            continue
        md = re.match(r'^  ((?:public |protected |private |static |final |abstract |synchronized |native |default |strictfp )*)[\w.$<>\[\], ?]*?([\w$<>]+)\(.*\);$', line)
        if md and current:
            pending = (current, md.group(2), md.group(1))
            continue
        dd = re.match(r'^    descriptor: (\S+)', line)
        if dd and pending:
            declared.append((pending[0], pending[1], dd.group(1), pending[2]))
            pending = None
        fl = re.match(r'^    flags: (.*)', line)
        if fl and declared and 'ACC_SYNTHETIC' in fl.group(1) and declared[-1][3] is not None:
            declared[-1] = declared[-1][:3] + (None,)

# sources: which methods are @Override, which classes are mixins
overrides = set()
mixins = set()
for m in MODULES:
    base = os.path.join(ROOT, m, 'src', 'main', 'java')
    for dirpath, _, files in os.walk(base):
        for f in files:
            if not f.endswith('.java'):
                continue
            path = os.path.join(dirpath, f)
            cls = os.path.relpath(path, base)[:-5]
            src = open(path, encoding='utf-8').read()
            if '@Mixin(' in src:
                mixins.add(cls)
            for mo in re.finditer(r'@Override\s+(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|protected|private|static|final|synchronized|default)\s+)*[\w.<>\[\], ?]+?\s+(\w+)\s*\(', src):
                overrides.add((cls, mo.group(1)))

resources = ''
for m in MODULES:
    base = os.path.join(ROOT, m, 'src', 'main', 'resources')
    for dirpath, _, files in os.walk(base):
        for f in files:
            if f.endswith(('.json', '.toml', '.mcmeta')) or 'services' in dirpath:
                try:
                    resources += open(os.path.join(dirpath, f), encoding='utf-8').read()
                except UnicodeDecodeError:
                    pass

dead_methods = []
for cls, name, desc, flags in declared:
    if flags is None or name in ('<clinit>', 'main', 'values', 'valueOf') or name.startswith('lambda$'):
        continue
    if cls.split('$')[0] in mixins:
        continue
    simple = re.split(r'[/$]', cls)[-1]
    if name == simple or name == cls.replace('/', '.') or name == cls.split('/')[-1]:
        if desc == '()V' or '$' in cls:
            # implicit constructors of utility classes; inner, anonymous and enum constant classes
            continue
        # a constructor (javap prints it under the class name): used when some class calls it; records' canonical
        # constructors are called by their codecs through method references, which also show up as calls
        if (cls, desc) in ctor_refs or cls in records or 'private' in flags:
            continue
        dead_methods.append('%s.<init>%s' % (cls, desc))
        continue
    if cls in records and desc.startswith('()'):
        continue
    outer = cls.split('$')[0]
    if outer in mixins or (outer, name) in overrides or (name, desc) in refs:
        continue
    if not cls.startswith(PKG):
        continue
    dead_methods.append('%s.%s%s' % (cls, name, desc))

dead_classes = []
for _, _, cls in classes:
    if '$' in cls or cls in mixins or not cls.startswith(PKG):
        continue
    users = class_refs.get(cls, set()) - {cls}
    if not users and cls.replace('/', '.') not in resources and cls.split('/')[-1] not in resources:
        dead_classes.append(cls)

print('# %d classes, %d methods declared' % (len(classes), len(declared)))
print('# classes nothing refers to: %d' % len(dead_classes))
for c in sorted(dead_classes):
    print('class ' + c)
print('# methods nothing calls: %d' % len(dead_methods))
for d in sorted(dead_methods):
    print('method ' + d)

# fields nothing reads or writes outside their declaration (a field written only counts as used: assignments are
# Fieldrefs too; constant fields inlined by javac have no Fieldref, so static final primitives and strings are left out)
field_names = {(c, n) for c, n in field_refs}
by_name = {n for _, n in field_refs}
dead_fields = []
for cls, name, flags in fields:
    if not cls.startswith(PKG) or cls.split('$')[0] in mixins or name.startswith('$') or name in ('CODEC', 'MAP_CODEC'):
        continue
    if 'static' in flags and 'final' in flags:
        continue
    if name in by_name:
        continue
    dead_fields.append('%s.%s' % (cls, name))
print('# fields nothing refers to: %d' % len(dead_fields))
for f in sorted(dead_fields):
    print('field ' + f)
