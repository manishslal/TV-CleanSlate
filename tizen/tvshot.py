#!/usr/bin/env python3
"""Screenshot Home or run JavaScript in it, through the TV's web inspector. Needs Developer Mode and websocket-client.

The inspector's own capture hangs on this TV, so the page draws itself with html2canvas. Video isn't in the image.

TV=<tv-ip> tizen/tvshot.py shot out.png
TV=<tv-ip> tizen/tvshot.py eval 'document.title'
TV=<tv-ip> tizen/tvshot.py key Right Right Enter
"""
import base64
import hashlib
import json
import os
import re
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

import websocket

APP = 'CSHome00Te.Home'
KEYS = {'Left': 37, 'Up': 38, 'Right': 39, 'Down': 40, 'Enter': 13, 'Back': 10009}
RENDERER = 'https://cdn.jsdelivr.net/npm/html2canvas@1.4.1/dist/html2canvas.min.js'
RENDERER_SHA256 = 'e87e550794322e574a1fda0c1549a3c70dae5a93d9113417a429016838eab8cb'
SDB = str(Path.home() / 'tizen-studio/tools/sdb')
TARGET = os.environ['TV'] + ':26101'


def sdb(*args):
    return subprocess.run([SDB, '-s', TARGET, *args], capture_output=True, text=True).stdout


def debug_port():
    # Asking twice hangs, so keep the port. Tizen 3.0 needs the extra argument and Tizen 4.0 answers "closed" with it.
    saved = Path.home() / f""".cache/tv-cleanslate/debug-port-{os.environ['TV']}"""
    saved.parent.mkdir(parents=True, exist_ok=True)
    if saved.exists() and alive(saved.read_text()):
        return saved.read_text()
    subprocess.run([SDB, 'connect', TARGET], capture_output=True)
    for extra in (['30'], []):
        said = subprocess.run([SDB, '-s', TARGET, 'shell', '0', 'debug', APP, *extra], capture_output=True, text=True, timeout=30)
        found = re.search(r'port: (\d+)', said.stdout + said.stderr)
        if found:
            break
    else:
        raise SystemExit('No debug port')
    saved.write_text(found.group(1))
    return found.group(1)


def alive(port):
    sdb('forward', f'tcp:{port}', f'tcp:{port}')
    try:
        return bool(urllib.request.urlopen(f'http://127.0.0.1:{port}/json', timeout=3).read())
    except OSError:
        return False


def page(port):
    sdb('forward', f'tcp:{port}', f'tcp:{port}')
    pages = json.load(urllib.request.urlopen(f'http://127.0.0.1:{port}/json', timeout=10))
    return websocket.create_connection(pages[0]['webSocketDebuggerUrl'], timeout=30, suppress_origin=True)


def call(ws, method, **params):
    ws.send(json.dumps({'id': 1, 'method': method, 'params': params}))
    while True:
        reply = json.loads(ws.recv())
        if reply.get('id') == 1:
            if 'error' in reply:
                raise SystemExit(str(reply['error']))
            return reply['result']


def renderer():
    path = Path.home() / '.cache/tv-cleanslate/html2canvas-1.4.1.min.js'
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(['curl', '-fsSL', '-o', str(path), RENDERER], check=True)
    if hashlib.sha256(path.read_bytes()).hexdigest() != RENDERER_SHA256:
        raise SystemExit(f'{path} does not match the expected checksum')
    return path.read_text()


def evaluate(ws, code):
    return call(ws, 'Runtime.evaluate', expression=code, returnByValue=True)['result']


def shot(ws, out):
    evaluate(ws, renderer())
    evaluate(ws, '''window.shot = 0; html2canvas(document.body, { backgroundColor: '#000', width: 1920, height: 1080, scale: 1, logging: false })
      .then(function (c) { window.shot = c.toDataURL('image/png'); }, function (e) { window.shot = 'failed ' + e; }); 0''')
    for _ in range(60):
        time.sleep(1)
        value = evaluate(ws, 'window.shot').get('value')
        if value:
            if not value.startswith('data:'):
                raise SystemExit(value)
            Path(out).write_bytes(base64.b64decode(value.split(',')[1]))
            return
    raise SystemExit('The page did not finish drawing')


def main():
    ws = page(os.environ.get('PORT') or debug_port())
    if sys.argv[1] == 'shot':
        shot(ws, sys.argv[2])
    elif sys.argv[1] == 'key':
        for name in sys.argv[2:]:
            for kind in ('rawKeyDown', 'keyUp'):
                call(ws, 'Input.dispatchKeyEvent', type=kind, windowsVirtualKeyCode=KEYS[name], nativeVirtualKeyCode=KEYS[name])
            time.sleep(0.4)
    else:
        print(json.dumps(evaluate(ws, sys.argv[2])))


main()
