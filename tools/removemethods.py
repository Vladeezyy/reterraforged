"""Deletes methods listed by tools/deadcode.py: tools/removemethods.py <list file>

Each line "method <class>.<name><descriptor>" (as deadcode.py prints it; <init> for a constructor). Finds the
declaration in the class's source by its name and parameter types (overloads are told apart), and deletes it with its
javadoc and annotations. Prints what it could not find. Compile and check the generation afterwards.
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
PRIM = {'Z': 'boolean', 'B': 'byte', 'C': 'char', 'S': 'short', 'I': 'int', 'J': 'long', 'F': 'float', 'D': 'double'}


def param_types(desc):
    inner = desc[1:desc.index(')')]
    out, i = [], 0
    while i < len(inner):
        dims = 0
        while inner[i] == '[':
            dims += 1
            i += 1
        if inner[i] == 'L':
            j = inner.index(';', i)
            name = re.split(r'[/$]', inner[i + 1:j])[-1]
            i = j + 1
        else:
            name = PRIM[inner[i]]
            i += 1
        out.append(name + '[]' * dims)
    return out


def declared_types(params):
    depth, cur, parts = 0, '', []
    for ch in params:
        if ch == '<':
            depth += 1
        elif ch == '>':
            depth -= 1
        elif ch == ',' and depth == 0:
            parts.append(cur)
            cur = ''
            continue
        if depth == 0 and ch not in '<>':
            cur += ch
    if cur.strip():
        parts.append(cur)
    types = []
    for p in parts:
        p = re.sub(r'@\w+(\([^)]*\))?', '', p).replace('final ', '').strip()
        varargs = '...' in p
        p = p.replace('...', '[]')
        t = ' '.join(p.split()[:-1]) if len(p.split()) > 1 else p
        t = t.replace(' ', '')
        t = t.split('.')[-1]
        types.append(t)
    return types


def find_source(cls):
    for m in ('common', 'neoforge', 'fabric'):
        path = os.path.join(ROOT, m, 'src', 'main', 'java', cls.split('$')[0] + '.java')
        if os.path.exists(path):
            return path
    return None


def remove(src, name, types):
    for mo in re.finditer(r'(?<![\w.])' + re.escape(name) + r'\s*\(', src):
        # the parameter list
        start = mo.end()
        depth, i = 1, start
        while depth:
            if src[i] == '(':
                depth += 1
            elif src[i] == ')':
                depth -= 1
            i += 1
        params = src[start:i - 1]
        if declared_types(params) != types:
            continue
        # a declaration: the line starts with modifiers / a type, and a body or ';' follows
        line_start = src.rfind('\n', 0, mo.start()) + 1
        head = src[line_start:mo.start()]
        if re.search(r'[=.(!]|return|new\s', head) or not re.match(r'\s*[\w<>\[\],.? @]*$', head):
            continue
        j = i
        while src[j] in ' \t\n\r' or src.startswith('throws', j):
            if src.startswith('throws', j):
                j = src.index('{', j)
                break
            j += 1
        if src[j] == ';':
            end = j + 1
        elif src[j] == '{':
            depth, k = 1, j + 1
            while depth:
                if src[k] == '{':
                    depth += 1
                elif src[k] == '}':
                    depth -= 1
                k += 1
            end = k
        else:
            continue
        # back over annotations and javadoc / line comments directly above
        begin = line_start
        while True:
            prev_end = begin - 1
            prev_start = src.rfind('\n', 0, prev_end) + 1
            prev = src[prev_start:prev_end].strip()
            if prev.startswith('@') or prev.startswith('//') or prev.startswith('*') or prev.startswith('/**') or prev.endswith('*/'):
                begin = prev_start
                continue
            break
        # and the blank line after it
        if src[end:end + 1] == '\n':
            end += 1
        while src[end:end + 1] in ('\n',) and src[begin - 2:begin] == '\n\n':
            end += 1
        return src[:begin] + src[end:], True
    return src, False


lines = [l.split()[1] for l in open(sys.argv[1]) if l.startswith('method ')]
missing = []
for entry in lines:
    cls, rest = entry.rsplit('.', 1) if '(' not in entry.rsplit('.', 1)[0] else (None, None)
    m = re.match(r'(.+)\.([<>\w$]+)(\(.*)$', entry)
    cls, name, desc = m.group(1), m.group(2), m.group(3)
    if name == '<init>':
        name = re.split(r'[/$]', cls)[-1]
    path = find_source(cls)
    if not path:
        missing.append(entry + ' (no source)')
        continue
    src = open(path, encoding='utf-8').read()
    src, ok = remove(src, name, param_types(desc))
    if ok:
        open(path, 'w', encoding='utf-8').write(src)
        print('removed ' + entry)
    else:
        missing.append(entry)
for m in missing:
    print('NOT FOUND ' + m)
