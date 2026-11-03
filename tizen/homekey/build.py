#!/usr/bin/env python3
"""Build the package that carries a Home key rule, after samsung-tizen-homekey (MIT). The stub only exits and never runs."""
import shutil
import struct
import subprocess
from pathlib import Path

from PIL import Image

here = Path(__file__).parent
out = here / 'build'
PROFILE = 'home'


def stub():
    code = struct.pack('<3I', 0xe3a00000, 0xe3a07001, 0xef000000)  # mov r0, #0; mov r7, #1; svc 0
    start = 52 + 32
    entry = 0x10000 + start
    ident = b'\x7fELF\x01\x01\x01' + bytes(9)
    head = ident + struct.pack('<2H5I6H', 2, 40, 1, entry, 52, 0, 0x05000400, 52, 32, 1, 0, 0, 0)
    size = start + len(code)
    return head + struct.pack('<8I', 1, 0, 0x10000, 0x10000, size, size, 5, 0x1000) + code


shutil.rmtree(out, ignore_errors=True)
(out / 'bin').mkdir(parents=True)
(out / 'shared/res').mkdir(parents=True)
(out / 'bin/homekey-stub').write_bytes(stub())
(out / 'bin/homekey-stub').chmod(0o755)
shutil.copy(here / 'tizen-manifest.xml', out)
Image.new('RGB', (117, 117), (20, 20, 24)).save(out / 'shared/res/homekey.png')
# The TV's installer only takes web packages, so this is a hybrid: a web app that also carries the native manifest.
(out / 'config.xml').write_text('<?xml version="1.0" encoding="UTF-8"?><widget xmlns="http://www.w3.org/ns/widgets" xmlns:tizen="http://tizen.org/ns/widgets" id="http://cleanslate/homekey" version="1.0.0"><tizen:application id="CSHomeKey1.Web" package="CSHomeKey1" required_version="3.0"/><content src="index.html"/><name>HomeKey</name><tizen:profile name="tv"/><tizen:privilege name="http://tizen.org/privilege/application.launch"/></widget>')
(out / 'index.html').write_text('<!doctype html><script>tizen.application.getCurrentApplication().exit();</script>')
tizen = Path.home() / 'tizen-studio/tools/ide/bin/tizen'
subprocess.run([tizen, 'package', '-t', 'wgt', '-s', PROFILE, '-o', str(here), '--', str(out)], check=True, stdout=subprocess.DEVNULL)
print(*here.glob('*.wgt'))
