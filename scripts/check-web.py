#!/usr/bin/env python3
"""Check the built web Home: its inline scripts parse (node --check) and use no ES6, since the
TVs run Chromium 47 and up. Usage: scripts/check-web.py [built.html ...] (default: webos/index.html and tizen/build/index.html).
Also runs node --check on webos/home-svc.js, which runs under the TV's own Node and may use modern syntax."""
import re
import subprocess
import sys
import tempfile
from pathlib import Path

root = Path(__file__).resolve().parent.parent
ES6 = [
    (r'=>', 'arrow function'),
    (r'(?<![\w$.])let\s+[\w$\[{]', 'let'),
    (r'(?<![\w$.])const\s+[\w$\[{]', 'const'),
    (r'`', 'template literal'),
    (r'(?<![\w$.])class\s+[\w$]+\s*(?:extends\s+[\w$.]+\s*)?\{', 'class'),
]
failed = False


def fail(msg):
    global failed
    failed = True
    print('FAIL', msg)


def node_check(path, label):
    r = subprocess.run(['node', '--check', str(path)], capture_output=True, text=True)
    if r.returncode:
        fail(f'{label} does not parse:\n{r.stderr.strip()}')
    else:
        print('ok  ', label, 'parses')


def es6_scan(code, label):
    for n, line in enumerate(code.splitlines(), 1):
        for pattern, name in ES6:
            if re.search(pattern, line):
                fail(f'{label} line {n}: {name}: {line.strip()[:100]}')


pages = [Path(a) for a in sys.argv[1:]] or [root / 'webos/index.html', root / 'tizen/build/index.html']
for page in pages:
    label = str(page.resolve().relative_to(root)) if page.resolve().is_relative_to(root) else str(page)
    if not page.exists():
        fail(f'{label} is missing (run webos/build.py, and tizen/build.py for the Samsung page)')
        continue
    blocks = re.findall(r'<script(?![^>]*\bsrc=)[^>]*>(.*?)</script>', page.read_text(), re.S)
    if not blocks:
        fail(f'{label} has no inline script')
        continue
    code = '\n'.join(blocks)
    with tempfile.TemporaryDirectory() as tmp:
        js = Path(tmp) / 'page.js'
        js.write_text(code)
        node_check(js, f'{label} script')
    es6_scan(code, label)
    if not failed:
        print('ok  ', label, 'has no ES6')

svc = root / 'webos/home-svc.js'
node_check(svc, 'webos/home-svc.js')
sys.exit(1 if failed else 0)
