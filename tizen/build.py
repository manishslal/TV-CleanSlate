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
sys.path.insert(0, str(root / 'logos'))
from plates import readable  # noqa: E402
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
    if m.get('plate') or m.get('size'):
        plates.update({i: [readable(m.get('plate'), m.get('ink')), m.get('ink'), m.get('size')] for i in m.get('tizen', [])})
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
# Chromium M47 has no CSS variables: the screensaver clock drifts by an inline transform instead,
# cleared when the screensaver ends.
sub("""  r.setProperty('--driftx', driftSide() * Math.round(Math.random() * 760) * window.innerWidth / 1920 + 'px');
  r.setProperty('--drifty', Math.round(Math.random() * 520) * window.innerWidth / 1920 + 'px');""",
    """  var dx = driftSide() * Math.round(Math.random() * 760) * window.innerWidth / 1920, dy = Math.round(Math.random() * 520) * window.innerWidth / 1920;
  if (document.body.classList.contains('idle')) $('top').style.webkitTransform = 'translate(' + dx + 'px,' + dy + 'px) scale(' + saverScale() + ')';""")
sub("c.toggle('idle', on);", "c.toggle('idle', on); if (!on) $('top').style.webkitTransform = '';")
sub("el.style.setProperty('--halo', halo(t.id));", '')
sub("if (t && zone === 'grid' && !down) $('glow')", "if (t) haloStyle(halo(t.id));\n  if (t && zone === 'grid' && !down) $('glow')")
sub("t.el.style.setProperty('--dx'", "(function () {})('--dx'")
sub("t.el.style.setProperty('--dy'", "(function () {})('--dy'")
sub("icon: SVC + '/icon?id=' + encodeURIComponent(a.id), fallback: API + '/icon/' + encodeURIComponent(a.id)", 'icon: a.icon')
sub("if (t.id !== 'com.cleanyourtv.manager' && t.id !== 'org.webosbrew.hbchannel') rows.push", 'if (false) rows.push')
sub(",\n      { label: 'Turn Off TV', icon: 'POWER', ok: function () { api('/api/power/off', {}).catch(function () {}); } } ]", ' ]')
sub('backSaver: true,', 'backSaver: true, takeHome: true,')
sub("toggle('Back Starts Screensaver', 'backSaver'),", "toggle('Back Starts Screensaver', 'backSaver'), toggle('Home Button Returns Here', 'takeHome'),")
sub('https://sylvan.apple.com/', 'http://sylvan.apple.com/')
# The 2017 and 2018 TVs stall on anything above 1080p H.264, so the choice goes.
sub("    rows.push(choice('Aerial Quality', [['hd', '1080p'], ['uhd', '4K']], function () { return S.quality; }, function (v) { S.quality = v; queue = []; restartWall(); }));\n", '')
sub('v.src = item[0];', "if (/sylvan\\.apple\\.com/.test(item[0])) aerialPlay(v, item[0], next); else v.src = item[0];")
sub('v.onplaying = v.ontimeupdate = v.onerror = null; v.pause();', 'aerialStop(v); v.onplaying = v.ontimeupdate = v.onerror = null; v.pause();')
sub('clearTimeout(wallGuard); v.onerror = v.onplaying', 'clearTimeout(wallGuard); aerialStop(v); v.onerror = v.onplaying')
sub('old.pause(); old.removeAttribute', 'aerialStop(old); old.pause(); old.removeAttribute')
sub('v.play().catch(function () {});', 'v.play();')
sub('WALL_WAIT = 20000', 'WALL_WAIT = 10000')
sub('function readInputs(j) {', 'function readInputs() { return []; }\nfunction readWebOSInputs(j) {')
# Rows a Samsung web app cannot act on: brightness is Samsung's own picture menu (the Settings row
# opens it), no input list reaches a web app, and nothing can be opened when the TV turns on.
sub("      { label: 'Brightness', icon: 'LIGHT_MODE', value: 'Picture Settings', ok: function () { closeAll(); open({ id: 'com.palm.app.settings' }); } },\n", '')
sub("      { label: 'Inputs', icon: 'INPUT', to: pageInputs },\n", '')
sub(",\n      { label: 'When TV Turns On', icon: 'POWER', value: function () { var t = allInputs.concat(allApps).filter(function (x) { return x.id === S.bootTarget; })[0]; return t ? t.title : 'Home'; }, to: pageBoot } ]", ' ]')
sub("'X-Plex-Platform': 'webOS', 'X-Plex-Device': 'LG TV', 'X-Plex-Device-Name': 'LG TV'", "'X-Plex-Platform': 'Tizen', 'X-Plex-Device': 'Samsung TV', 'X-Plex-Device-Name': 'Samsung TV'")
sub("'com.palm.app.settings'", "'org.tizen.menu'")
page = re.sub(r"'calc\(' \+ (.+?) \+ ' \* var\(--u\)\)'", r"(\1) + 'px'", page)
sub('/* ---- settings, the same names', (here / 'adapter.js').read_text() + (here / 'aerial.js').read_text() + '\n/* ---- settings, the same names', 1)
if debug_host:
    sub('</head>', f'<script>var DEBUG_HOST = "{debug_host}";</script><script src="debug.js"></script></head>', 1)
sub('<head>', '<head><script src="$WEBAPIS/webapis/webapis.js"></script>', 1)
sub("api('/api/frontend-ok', { version: VERSION }).catch(function () {});", """document.addEventListener('visibilitychange', function () {
  if (document.hidden) stopVideo();
  else document.dispatchEvent(new Event('webOSRelaunch'));
});""")

# LEAN=1, for a TV short on memory and storage: starts on Efficient, with still wallpaper and no frost
lean = os.environ.get('LEAN')
if lean:
    sub("effects: 'auto'", "effects: 'lite'")

shutil.rmtree(out, ignore_errors=True)
out.mkdir()
(out / 'index.html').write_text(page)
shutil.copy(here / 'config.xml', out)
shutil.copy(web / 'barlow_condensed_semibold.ttf', out)
if debug_host:
    shutil.copy(here / 'debug.js', out)
for picture in (web / 'stills').glob('still-*.jpg'):
    shutil.copy(picture, out)
for sheet in () if lean else (web / 'frost').glob('sheet-*.jpg'):
    shutil.copy(sheet, out)
for logo in (web / 'logo').iterdir():
    shutil.copy(logo, out)
# Home's own interface sounds (tools/make-sounds.py), the same files Android plays
for wav in (root / 'android/app/src/main/res/raw').glob('ui_*.wav'):
    shutil.copy(wav, out)
left = re.findall(r'var\(--|display: grid', style)
print(len(page), 'bytes', len(logos), 'logos', len(ids), 'ordered', len(left), 'unsupported CSS left')
