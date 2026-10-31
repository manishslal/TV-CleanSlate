#!/usr/bin/env python3
"""Bake a timeline of softly blurred frames for each wallpaper clip: one frame every 10 seconds, on one sheet per clip.
This TV draws video on a hardware plane that a page cannot read or blur, so Home shows these in step with the video."""
import concurrent.futures as cf
import io
import json
import math
import subprocess
from pathlib import Path

from PIL import Image, ImageFilter

STEP, COLS, MAX, W, H = 10, 6, 36, 192, 108
here = Path(__file__).parent
out = here / 'frost'
out.mkdir(exist_ok=True)
aerials = json.loads((here / '../android/app/src/main/assets/aerials.json').read_text())['videos']
urls = [v['url'] for v in aerials if 'AVC' in v['url'].upper() and 'SDR' in v['url']]


def duration(url):
    r = subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', url], capture_output=True, text=True, timeout=60)
    return float(r.stdout.strip())


def frame(url, t):
    r = subprocess.run(['ffmpeg', '-nostdin', '-v', 'error', '-ss', str(t), '-i', url, '-frames:v', '1', '-vf', f'scale={W}:{H}', '-f', 'image2pipe', '-c:v', 'png', '-'], capture_output=True, timeout=60)
    return Image.open(io.BytesIO(r.stdout)).convert('RGB')


def bake(item):
    i, url = item
    path = out / f'sheet-{i}.jpg'
    if path.exists():
        return i, -1
    n = min(MAX, max(1, math.ceil(duration(url) / STEP)))
    sheet = Image.new('RGB', (W * COLS, H * math.ceil(n / COLS)))
    last = None
    for k in range(n):
        try:
            last = frame(url, k * STEP + 1)
        except Exception:
            pass
        if last:
            sheet.paste(last, (k % COLS * W, k // COLS * H))
    sheet.filter(ImageFilter.GaussianBlur(1.6)).save(path, 'JPEG', quality=72, optimize=True)
    return i, n


with cf.ThreadPoolExecutor(10) as pool:
    done = list(pool.map(bake, enumerate(urls)))
counts = json.loads((here / 'frost.json').read_text()) if (here / 'frost.json').exists() else {}
counts.update({str(i): n for i, n in done if n > 0})
(here / 'frost.json').write_text(json.dumps(counts))
print(len(counts), 'clips', sum(counts.values()), 'frames', sum(f.stat().st_size for f in out.glob('sheet-*.jpg')) // 1024, 'KB')
