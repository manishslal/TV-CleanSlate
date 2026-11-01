#!/usr/bin/env python3
"""Build the Tizen 3.0 page from webOS Home. This engine has no CSS variables or grid, so both are compiled away."""
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

here = Path(__file__).parent
root = here / '..'
web = root / 'webos'
out = here / 'build'
debug_host = os.environ.get('DEBUG_HOST', '')

subprocess.run([sys.executable, str(web / 'build.py')], check=True)
page = (web / 'index.html').read_text()
match = re.search(r'<style>(.*?)</style>', page, re.S)
css = match.group(1)


def declarations(text):
    return dict(re.findall(r'(--[\w-]+)\s*:\s*([^;}]+)', text))


base = declarations(re.search(r':root\s*\{(.*?)\}', css, re.S).group(1))
light = declarations(re.search(r'body\.light\s*\{(.*?)\}', css, re.S).group(1))
card = declarations(re.search(r'(?m)^\.card\s*\{(.*?)\}', css, re.S).group(1))
small = declarations(re.search(r'\.card\.sm\s*\{(.*?)\}', css, re.S).group(1))
base.pop('--u')


def closing(text, start):
    depth = 0
    for i in range(start, len(text)):
        depth += text[i] == '('
        depth -= text[i] == ')'
        if depth == 0:
            return i
    raise ValueError(text)


def resolve(value, env):
    value = re.sub(r'calc\(([\d.-]+) \* var\(--u\)\)', r'\1px', value).replace('var(--u)', '1px')
    while 'var(' in value:
        start = value.index('var(')
        end = closing(value, start + 3)
        name, _, fallback = value[start + 4:end].partition(',')
        value = value[:start] + str(env.get(name.strip(), fallback.strip())) + value[end + 1:]
        value = re.sub(r'calc\(([\d.-]+) \* var\(--u\)\)', r'\1px', value)
    return value


def in_body(selectors, cls):
    parts = []
    for s in selectors.split(','):
        s = s.strip()
        if s.startswith('body'):
            parts.append(s if cls in s else 'body' + cls + s[4:])
        else:
            parts.append('body' + cls + ' ' + s)
    return ', '.join(parts)


def in_small(selectors):
    parts = []
    for s in selectors.split(','):
        s = s.strip()
        parts.append(s.replace('.card', '.card.sm', 1) if '.card' in s else '.card.sm ' + s)
    return ', '.join(parts)


def body_of(block, env):
    kept = []
    for d in (x.strip() for x in block.split(';')):
        if not d or d.startswith('--'):
            continue
        d = resolve(d, env)
        for name in ('filter', 'clip-path'):
            d = re.sub(r'^' + name + r'\s*:', '-webkit-' + name + ':', d)
        kept.append(d.replace('transition: clip-path', 'transition: -webkit-clip-path'))
    return '; '.join(kept)


def rules(text):
    i = 0
    while i < len(text):
        brace = text.find('{', i)
        if brace < 0:
            return
        depth, j = 0, brace
        while True:
            depth += text[j] == '{'
            depth -= text[j] == '}'
            if depth == 0:
                break
            j += 1
        yield text[i:brace].strip(), text[brace + 1:j]
        i = j + 1


def compile_css(text):
    text = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    done = []
    for selector, block in rules(text):
        if selector == ':root' or selector == 'body.light' and '--' in block.split(';')[0]:
            continue
        if selector.startswith('@font-face'):
            done.append(f'{selector} {{ {block} }}')
            continue
        if selector.startswith('@'):
            done.append(f'{selector} {{ {compile_css(block)} }}')
            continue
        env = dict(base)
        env.update(card)
        plain = body_of(block, env)
        if plain:
            done.append(f'{selector} {{ {plain} }}')
        used = set(re.findall(r'var\((--[\w-]+)', block))
        if used & set(light) and '.light' not in selector:
            bright = dict(env)
            bright.update(light)
            only = '; '.join(d for d in (x.strip() for x in block.split(';'))
                             if set(re.findall(r'var\((--[\w-]+)', d)) & set(light))
            done.append(f"{in_body(selector, '.light')} {{ {body_of(only, bright)} }}")
        if used & set(small):
            tiny = dict(env)
            tiny.update(small)
            done.append(f'{in_small(selector)} {{ {body_of(block, tiny)} }}')
    return '\n'.join(done)


style = compile_css(css) + '''
#grid { display: flex; flex-wrap: wrap; width: 2000px; padding: 0 89px; box-sizing: content-box; }
.tile { width: 252px; height: 186px; margin: 0 46px 14px 0; flex: none; }
.minis { display: flex; flex-wrap: wrap; width: 174px; }
.minis img { width: 84px; height: 58px; margin: 0 6px 4px 0; }
#fgrid { display: flex; flex-wrap: wrap; justify-content: center; align-content: center; }
.tile.f .plate { box-shadow: 0 0 0 5px rgba(0,0,0,.75), 0 0 0 13px #f5f5f7, 0 0 30px rgba(0,0,0,.45); }
body.idle #top { transform: none; }
@keyframes push { 40% { transform: scale(1.15); } }
'''
page = page[:match.start(1)] + style + page[match.end(1):]

# Tizen app IDs for the tile order and logos
manifest = json.loads((root / 'logos/manifest.json').read_text())
order = ['netflix', 'youtube', 'prime-video', 'disney-plus', 'hbo-max', 'hulu', 'apple-tv', 'plex', 'peacock', 'paramount-plus', 'spotify']
logos, plates = {}, {}
for brand, m in manifest.items():
    source = root / 'logos' / m['file']
    name = f"logo-{brand}{'.svg' if source.suffix == '.svg' else '.webp'}"
    if source.exists() and (web / 'logo' / name).exists():
        logos.update({i: name for i in m.get('tizen', [])})
    if m.get('plate'):
        plates.update({i: [m.get('plate'), m.get('ink')] for i in m.get('tizen', [])})
ids = [manifest[b]['tizen'][0] for b in order if manifest.get(b, {}).get('tizen')]


def sub(old, new, count=-1):
    global page
    if old not in page:
        raise SystemExit(f'webOS page changed: {old[:60]}')
    page = page.replace(old, new) if count < 0 else page.replace(old, new, count)


page = re.sub(r"var ORDER = \[.*?\];", 'var ORDER = ' + json.dumps(ids) + ';', page, count=1, flags=re.S)
page = re.sub(r"var HIDDEN = \[.*?\];", 'var HIDDEN = [];', page, count=1, flags=re.S)
page = re.sub(r'var LOGOS = \{.*?\};\n', 'var LOGOS = ' + json.dumps(logos) + ';\n', page, count=1, flags=re.S)
page = re.sub(r'var PLATES = \{.*?\};\n', 'var PLATES = ' + json.dumps(plates) + ';\n', page, count=1, flags=re.S)
sub('461', '10009')
sub("r.setProperty('--driftx'", "(function () {})('--driftx'")
sub("r.setProperty('--drifty'", "(function () {})('--drifty'")
sub("el.style.setProperty('--halo', tint(t.id));", '')
sub("t.el.style.setProperty('--dx'", "(function () {})('--dx'")
sub("t.el.style.setProperty('--dy'", "(function () {})('--dy'")
sub("icon: SVC + '/icon?id=' + encodeURIComponent(a.id), fallback: API + '/icon/' + encodeURIComponent(a.id)", 'icon: a.icon')
sub("if (t.id !== 'com.cleanyourtv.manager' && t.id !== 'org.webosbrew.hbchannel') rows.push", 'if (false) rows.push')
sub(",\n      { label: 'Turn Off TV', icon: 'POWER', ok: function () { api('/api/power/off', {}).catch(function () {}); } } ]", ' ]')
sub('https://sylvan.apple.com/', 'http://sylvan.apple.com/')
sub('v.play().catch(function () {});', 'v.play();')
sub('WALL_WAIT = 20000', 'WALL_WAIT = 6000')
sub('function readInputs(j) {', "function readInputs() { return [{ id: 'org.tizen.SMSourceAPP', title: 'Inputs', short: 'Inputs', glyph: 'HDMI' }]; }\nfunction readWebOSInputs(j) {")
sub("'com.palm.app.settings'", "'org.tizen.menu'")
page = re.sub(r"'calc\(' \+ (.+?) \+ ' \* var\(--u\)\)'", r"(\1) + 'px'", page)
sub('/* ---- settings, the same names', (here / 'adapter.js').read_text() + '\n/* ---- settings, the same names', 1)
if debug_host:
    sub('</head>', f'<script>var DEBUG_HOST = "{debug_host}";</script><script src="debug.js"></script></head>', 1)
sub('<head>', '<head><script src="$WEBAPIS/webapis/webapis.js"></script>', 1)
sub("api('/api/frontend-ok', { version: VERSION }).catch(function () {});", """document.addEventListener('visibilitychange', function () {
  if (document.hidden) stopVideo();
  else document.dispatchEvent(new Event('webOSRelaunch'));
});""")

shutil.rmtree(out, ignore_errors=True)
out.mkdir()
(out / 'index.html').write_text(page)
shutil.copy(here / 'config.xml', out)
shutil.copy(web / 'barlow_condensed_semibold.ttf', out)
if debug_host:
    shutil.copy(here / 'debug.js', out)
for logo in (web / 'logo').iterdir():
    shutil.copy(logo, out)
left = re.findall(r'var\(--|display: grid', style)
print(len(page), 'bytes', len(logos), 'logos', len(ids), 'ordered', len(left), 'unsupported CSS left')
