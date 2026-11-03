#!/usr/bin/env python3
"""Bake a few frames of the aerial clips, for Efficient's still wallpaper. Needs ffmpeg."""
import json
import re
import subprocess
from pathlib import Path

here = Path(__file__).parent
PICK = {'landscapes': 5, 'cities': 4, 'space': 2, 'underwater': 1}
out = here / 'stills'
out.mkdir(exist_ok=True)
page = (here / 'index.html').read_text()
videos = json.loads(re.search(r'var VIDEOS = (\[.*?\]);\n', page, flags=re.S).group(1))
n = 0
for kind, count in PICK.items():
    clips = [v for v in videos if v[2] == kind]
    for clip in clips[::max(1, len(clips) // count)][:count]:
        path = out / f'still-{n}.jpg'
        subprocess.run(['ffmpeg', '-nostdin', '-v', 'error', '-y', '-ss', '40', '-i', clip[0], '-frames:v', '1',
                        '-vf', 'scale=1920:1080', '-q:v', '5', str(path)], check=True, timeout=120)
        print(path.name, clip[1], path.stat().st_size // 1024, 'KB', flush=True)
        n += 1
