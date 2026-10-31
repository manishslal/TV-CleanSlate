#!/usr/bin/env python3
"""Fill index.src.html with the wallpaper list, icons and news topics from the Android launcher's sources."""
import json
import re
from pathlib import Path

here = Path(__file__).parent
android = here / '../android/app/src/main'
kt = android / 'java/app/orchard/tv'

videos = [[v['url'], v.get('label', ''), v['category']]
          for v in json.loads((android / 'assets/aerials.json').read_text())['videos']
          if 'AVC' in v['url'].upper() and 'SDR' in v['url']]
videos = [v + [i] for i, v in enumerate(videos)]  # the number names the clip's baked frost, frost/frost-<n>.jpg

paths = dict(re.findall(r'const val (\w+) = "([^"]+)"', (kt / 'ui/theme/GlyphPaths.kt').read_text()))
wanted = 'WIFI LAN WIFI_OFF SETTINGS INPUT VOLUME_UP VOLUME_OFF VISIBILITY_OFF FOLDER OPEN_WITH DELETE CHEVRON_RIGHT HDMI LIGHT_MODE IMAGE PLAY_CIRCLE PARTLY_CLOUDY_DAY CALENDAR_MONTH VERIFIED_USER LANGUAGE BEDTIME CLOUD RAINY WEATHER_SNOWY FOGGY THUNDERSTORM POWER'.split()
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
from PIL import Image
pack = here / '../logos'
out = here / 'logo'
out.mkdir(exist_ok=True)
logos = {}
for brand, m in json.loads((pack / 'manifest.json').read_text()).items():
    src = pack / m['file']
    if src.suffix == '.svg':
        name = f'logo-{brand}.svg'
        (out / name).write_bytes(src.read_bytes())
    else:
        name = f'logo-{brand}.webp'
        im = Image.open(src)
        im.thumbnail((504, 276), Image.LANCZOS)
        im.save(out / name, 'WEBP', quality=92, alpha_quality=100, method=6)
    logos.update({i: name for i in m['webos']})

s = (here / 'index.src.html').read_text()
for name, data in (('__VIDEOS__', videos), ('__ICONS__', icons), ('__TOPICS__', topics), ('__LOGOS__', logos), ('__FROST__', json.loads((here / 'frost.json').read_text()))):
    s = s.replace(name, json.dumps(data, separators=(',', ':')))
(here / 'index.html').write_text(s)
print(len(videos), 'videos', len(icons), 'icons', len(topics), 'topics', len(s), 'bytes')
