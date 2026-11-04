#!/usr/bin/env python3
"""Home's cut-out service: the subject of a wallpaper as a mask, so Home can draw it over the clock.

Runs on a home server (Docker, see compose.yml and README.md) and answers the TVs on the home network:

  GET  /health                 {"ok": true, "model": ...}
  POST /cutout                 body: a JPEG or PNG; answers the subject's mask
  GET  /cutout?url=<picture>   the same for a picture Home's sources serve (Bing, NASA, Wikimedia, Earth View)

The mask is an 8-bit PNG, 960x540: the picture cropped to 16:9 from its middle, as Home fills the
screen with it, white where the subject is. Header X-Subject gives how much of the frame it covers, X-Solid how firmly (0 to 1).
Masks are kept on disk by the picture's hash, so every TV in a home asks once for the same picture.

The model is ISNet (general use, DIS), run with onnxruntime on the CPU, one picture at a time.
"""
import hashlib
import io
import json
import os
import threading
import time
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import numpy as np
import onnxruntime as ort
from PIL import Image

MODEL = os.environ.get('CUTOUT_MODEL', '/models/isnet-general-use.onnx')
CACHE = os.environ.get('CUTOUT_CACHE', '/cache')
PORT = int(os.environ.get('CUTOUT_PORT', '8890'))
THREADS = int(os.environ.get('CUTOUT_THREADS', '2'))
OUT_W, OUT_H = 960, 540
SIDE = 1024
MAX_BYTES = 20 << 20
# Only Home's own picture sources: the service fetches nothing else, so it is no open proxy.
HOSTS = {'www.bing.com', 'cn.bing.com', 'assets.science.nasa.gov', 'upload.wikimedia.org', 'thumb.wikimedia.org', 'www.gstatic.com'}
AGENT = 'HomeTVLauncher/1.67 (personal TV launcher)'

os.makedirs(CACHE, exist_ok=True)
opts = ort.SessionOptions()
opts.intra_op_num_threads = THREADS
opts.inter_op_num_threads = 1
session = ort.InferenceSession(MODEL, opts, providers=['CPUExecutionProvider'])
input_name = session.get_inputs()[0].name
busy = threading.Lock()
# At most two pictures decoded and worked on at once: a 20 MB PNG decodes to hundreds of MB.
working = threading.BoundedSemaphore(2)


def cover(img):
    """The middle 16:9 of the picture, as Home's wallpaper shows it."""
    w, h = img.size
    if w / h > 16 / 9:
        cw = round(h * 16 / 9)
        return img.crop(((w - cw) // 2, 0, (w - cw) // 2 + cw, h))
    ch = round(w * 9 / 16)
    return img.crop((0, (h - ch) // 2, w, (h - ch) // 2 + ch))


def mask_of(data):
    img = cover(Image.open(io.BytesIO(data)).convert('RGB'))
    x = np.asarray(img.resize((SIDE, SIDE), Image.BILINEAR), dtype=np.float32) / 255.0
    x = (x - 0.5) / 1.0
    x = np.transpose(x, (2, 0, 1))[None]
    with busy:
        start = time.time()
        out = session.run(None, {input_name: x})[0][0][0]
        spent = time.time() - start
    lo, hi = float(out.min()), float(out.max())
    m = (out - lo) / (hi - lo + 1e-6)
    # Soft edges kept, the haze around them dropped: below 0.2 is background.
    m = np.clip((m - 0.2) / 0.6, 0, 1)
    mask = Image.fromarray((m * 255).astype(np.uint8), 'L').resize((OUT_W, OUT_H), Image.BILINEAR)
    a = np.asarray(mask, dtype=np.float32) / 255.0
    cover_share = float(a.mean())
    # How sure the model is: of what it marked at all, the share it marked firmly. A real subject (a
    # fish, a tower) is near 1; a haze over a river or a sky is low, and Home leaves those alone.
    marked = a > 0.2
    solid = float((a > 0.8).sum() / marked.sum()) if marked.any() else 0.0
    buf = io.BytesIO()
    mask.save(buf, 'PNG', optimize=True)
    return buf.getvalue(), cover_share, solid, spent


def write_atomic(path, data):
    """Whole or not at all: a reader never sees half a file while a second request writes it."""
    tmp = '%s.%d.tmp' % (path, threading.get_ident())
    with open(tmp, 'wb') as f:
        f.write(data)
    os.replace(tmp, path)


def cached(data):
    key = hashlib.sha256(data).hexdigest()[:32]
    png, meta = os.path.join(CACHE, key + '.png'), os.path.join(CACHE, key + '.json')
    if os.path.exists(png) and os.path.exists(meta):
        with open(png, 'rb') as f, open(meta) as g:
            m = json.load(g)
            return f.read(), m['subject'], m.get('solid', 0.0), 0.0, True
    out, share, solid, spent = mask_of(data)
    # The picture first, then what it says: a meta file always has its picture.
    write_atomic(png, out)
    write_atomic(meta, json.dumps({'subject': share, 'solid': solid, 'seconds': round(spent, 2), 'at': int(time.time())}).encode())
    return out, share, solid, spent, False


def allowed(url):
    u = urllib.parse.urlparse(url)
    return u.scheme == 'https' and u.hostname in HOSTS


class Redirects(urllib.request.HTTPRedirectHandler):
    """Follows a redirect only to another of Home's picture sources, never into the home network."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        if not allowed(newurl):
            raise PermissionError('a redirect away from Home\'s picture sources')
        return super().redirect_request(req, fp, code, msg, headers, newurl)


opener = urllib.request.build_opener(Redirects)


def fetch(url):
    if not allowed(url):
        raise PermissionError('not one of Home\'s picture sources')
    req = urllib.request.Request(url, headers={'User-Agent': AGENT})
    with opener.open(req, timeout=30) as r:
        return r.read(MAX_BYTES + 1)


class Handler(BaseHTTPRequestHandler):
    server_version = 'HomeCutout/1'

    def log_message(self, fmt, *args):
        print('%s %s' % (self.address_string(), fmt % args), flush=True)

    def send(self, code, body, kind='application/json', headers=None):
        self.send_response(code)
        self.send_header('Content-Type', kind)
        self.send_header('Content-Length', str(len(body)))
        # The web Home on LG and Samsung asks from its own page.
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Expose-Headers', 'X-Subject, X-Solid, X-Seconds, X-Cached')
        for k, v in (headers or {}).items():
            self.send_header(k, v)
        self.end_headers()
        self.wfile.write(body)

    def answer(self, data):
        if not data or len(data) > MAX_BYTES:
            return self.send(413, b'{"error":"picture missing or over 20 MB"}')
        # Under the TVs' own 60 s wait, so a queued TV hears "busy" rather than timing out.
        if not working.acquire(timeout=45):
            return self.send(503, b'{"error":"busy"}')
        try:
            png, share, solid, spent, hit = cached(data)
        except Exception as e:  # not a picture Pillow can read
            return self.send(422, json.dumps({'error': type(e).__name__}).encode())
        finally:
            working.release()
        self.send(200, png, 'image/png', {'X-Subject': '%.4f' % share, 'X-Solid': '%.3f' % solid, 'X-Seconds': '%.2f' % spent, 'X-Cached': '1' if hit else '0', 'Cache-Control': 'max-age=31536000'})

    def do_OPTIONS(self):
        self.send(204, b'', headers={'Access-Control-Allow-Methods': 'GET, POST', 'Access-Control-Allow-Headers': 'Content-Type'})

    def do_GET(self):
        q = urllib.parse.urlparse(self.path)
        if q.path == '/health':
            return self.send(200, json.dumps({'ok': True, 'model': os.path.basename(MODEL), 'size': [OUT_W, OUT_H]}).encode())
        if q.path == '/cutout':
            url = urllib.parse.parse_qs(q.query).get('url', [''])[0]
            try:
                data = fetch(url)
            except PermissionError as e:
                return self.send(403, json.dumps({'error': str(e)}).encode())
            except Exception as e:
                return self.send(502, json.dumps({'error': type(e).__name__}).encode())
            return self.answer(data)
        self.send(404, b'{"error":"not found"}')

    def do_POST(self):
        if urllib.parse.urlparse(self.path).path != '/cutout':
            return self.send(404, b'{"error":"not found"}')
        try:
            n = int(self.headers.get('Content-Length') or 0)
        except ValueError:
            return self.send(400, b'{"error":"bad Content-Length"}')
        self.answer(self.rfile.read(n) if 0 < n <= MAX_BYTES else b'')


if __name__ == '__main__':
    print('cut-out service on :%d, model %s, %d threads' % (PORT, os.path.basename(MODEL), THREADS), flush=True)
    ThreadingHTTPServer(('0.0.0.0', PORT), Handler).serve_forever()
