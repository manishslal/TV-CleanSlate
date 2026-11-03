#!/usr/bin/env python3
"""Send remote keys to the whole TV. Needs websocket-client. The first run needs Allow pressed on the TV within 30 seconds.

TV=<tv-ip> tizen/tvkey.py KEY_RIGHT KEY_ENTER
"""
import base64
import json
import os
import ssl
import sys
import time
from pathlib import Path

import websocket

NAME = base64.b64encode(b'CleanSlateKeys').decode()
SAVED = Path.home() / f""".cache/tv-cleanslate/remote-token-{os.environ['TV']}"""


def main():
    token = f'&token={SAVED.read_text()}' if SAVED.exists() else ''
    url = f"wss://{os.environ['TV']}:8002/api/v2/channels/samsung.remote.control?name={NAME}{token}"
    ws = websocket.create_connection(url, timeout=35, sslopt={'cert_reqs': ssl.CERT_NONE}, suppress_origin=True)
    while True:
        try:
            reply = json.loads(ws.recv())
        except websocket.WebSocketProtocolException:
            raise SystemExit('The TV closed the connection. Press Allow within 30 seconds of connecting.')
        if reply.get('event') == 'ms.channel.connect':
            break
    if reply['data'].get('token'):
        SAVED.parent.mkdir(parents=True, exist_ok=True)
        SAVED.write_text(str(reply['data']['token']))
        SAVED.chmod(0o600)
    for key in sys.argv[1:]:
        ws.send(json.dumps({'method': 'ms.remote.control', 'params': {'Cmd': 'Click', 'DataOfCmd': key, 'Option': 'false', 'TypeOfRemote': 'SendRemoteKey'}}))
        time.sleep(0.5)


main()
