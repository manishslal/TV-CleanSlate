#!/usr/bin/env python3
"""Fill index.src.html with the wallpaper list, icons and news topics from the Android launcher's sources."""
import json
import re
import sys
from pathlib import Path

here = Path(__file__).parent
sys.path.insert(0, str(here / '../logos'))
from plates import readable  # noqa: E402
android = here / '../android/app/src/main'
kt = android / 'java/app/orchard/tv'

videos = [[v['url'], v.get('label', ''), v['category'], v.get('uhd', '')]
          for v in json.loads((android / 'assets/aerials.json').read_text())['videos']
          if 'AVC' in v['url'].upper() and 'SDR' in v['url']]
videos = [v[:3] + [i, v[3]] for i, v in enumerate(videos)]  # the number names the clip's baked frost, frost/frost-<n>.jpg; the last is the 4K file

paths = dict(re.findall(r'const val (\w+) = "([^"]+)"', (kt / 'ui/theme/GlyphPaths.kt').read_text()))
wanted = 'WIFI LAN WIFI_OFF SETTINGS INPUT VOLUME_UP VOLUME_OFF VISIBILITY_OFF FOLDER OPEN_WITH DELETE CHEVRON_RIGHT HDMI LIGHT_MODE IMAGE PLAY_CIRCLE PARTLY_CLOUDY_DAY CALENDAR_MONTH VERIFIED_USER LANGUAGE BEDTIME CLOUD RAINY WEATHER_SNOWY FOGGY THUNDERSTORM POWER CONTRAST EDIT'.split()
icons = {k: paths[k] for k in wanted}

labels = {'tech': 'Tech', 'ai': 'AI', 'us': 'US', 'world': 'World', 'business': 'Business', 'science': 'Science', 'sports': 'Sports', 'gaming': 'Gaming', 'entertainment': 'Entertainment', 'health': 'Health'}
base = 'https://news.google.com/rss/'
tail = '&hl=en-US&gl=US&ceid=US:en'
topics = []
src = (kt / 'news/Headlines.kt').read_text()
for key, section, query in re.findall(r'\w+\("(\w+)", R\.string\.fk_news_\w+, (?:section = "(\w+)"|query = "((?:[^"\\]|\\.)*)")', src):
    if key not in labels:
        continue
    if section:
        topics.append([key, labels[key], f'{base}headlines/section/topic/{section}?{tail[1:]}'])
    else:
        from urllib.parse import quote_plus
        q = query.replace('\\"', '"') + ' when:1d'
        topics.append([key, labels[key], f'{base}search?q={quote_plus(q)}{tail}'])

# the shared logo pack, exported small enough for this TV
from PIL import Image  # noqa: E402
pack = here / '../logos'
out = here / 'logo'
out.mkdir(exist_ok=True)
logos = {}
plates = {}
for brand, m in json.loads((pack / 'manifest.json').read_text()).items():
    src = pack / m['file']
    if not src.exists():
        continue
    if src.suffix == '.svg':
        name = f'logo-{brand}.svg'
        (out / name).write_bytes(src.read_bytes())
    else:
        name = f'logo-{brand}.webp'
        im = Image.open(src)
        im.thumbnail((504, 276), Image.LANCZOS)
        im.save(out / name, 'WEBP', quality=92, alpha_quality=100, method=6)
    logos.update({i: name for i in m.get('webos', [])})
    plates.update({i: [readable(m.get('plate'), m.get('ink')), m.get('ink'), m.get('size'), m.get('glow')] for i in m.get('webos', []) if m.get('plate') or m.get('size') or m.get('glow')})

# CI has neither the private logo art (logos/dark/ is gitignored) nor the baked frost and stills, which
# need ffmpeg and the network. Those are skipped with a warning, so the page and its scripts still build.
missing_logos = [b for b, m in json.loads((pack / 'manifest.json').read_text()).items() if not (pack / m['file']).exists()]
if missing_logos:
    print(f'warning: {len(missing_logos)} logo files not found (logos/dark/ is private), built without them', file=sys.stderr)
frost_file = here / 'frost.json'
if not frost_file.exists():
    print('warning: webos/frost.json not found (run bake_frost.py), built without frost', file=sys.stderr)

s = (here / 'index.src.html').read_text()
for name, data in (('__VIDEOS__', videos), ('__ICONS__', icons), ('__TOPICS__', topics), ('__LOGOS__', logos), ('__PLATES__', plates), ('__STILLS__', [p.name for p in sorted((here / 'stills').glob('still-*.jpg'))]), ('__FROST__', (json.loads(frost_file.read_text()) if frost_file.exists() else {})), ('__EARTH__', json.loads((android / 'assets/earthview.json').read_text())['items'])):
    s = s.replace(name, json.dumps(data, separators=(',', ':')))
(here / 'index.html').write_text(s)

# Home's own interface sounds (tools/make-sounds.py), the same files Android plays; deployed beside index.html
import shutil  # noqa: E402
for wav in (android / 'res/raw').glob('ui_*.wav'):
    shutil.copy(wav, here / wav.name)
print(len(videos), 'videos', len(icons), 'icons', len(topics), 'topics', len(s), 'bytes')
