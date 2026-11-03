#!/usr/bin/env python3
"""Keeps docs/parity.json valid, and fails a UI change that doesn't update it.

python3 scripts/check-parity.py                  checks staged changes
python3 scripts/check-parity.py <base> <head>    checks a range, for CI
python3 scripts/check-parity.py --open           lists what each build is missing

A change with nothing to record says so with "[parity: none]" in its commit message.
"""
import json
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
LEDGER = 'docs/parity.json'
UI = ('android/app/src/main/java/app/orchard/tv/ui/', 'android/app/src/main/java/app/orchard/tv/apps/TileArt.kt',
      'android/lite/src/', 'webos/index.src.html', 'tizen/adapter.js',
      # Behavior a person can see even though it isn't layout:
      'android/app/src/main/java/app/orchard/tv/settings/', 'android/app/src/main/java/app/orchard/tv/wallpaper/',
      'android/app/src/main/java/app/orchard/tv/news/', 'android/app/src/main/java/app/orchard/tv/weather/',
      'android/app/src/main/java/app/orchard/tv/plex/', 'android/app/src/main/java/app/orchard/tv/apps/',
      'webos/build.py', 'webos/home-svc.js', 'tizen/build.py', 'tizen/aerial.js')


def git(*args):
    return subprocess.run(['git', *args], cwd=root, capture_output=True, text=True).stdout


def problems(ledger):
    found = []
    known = set(ledger['statuses'])
    seen = set()
    for f in ledger['features']:
        if f['id'] in seen:
            found.append(f"{f['id']}: duplicate id")
        seen.add(f['id'])
        for build in ledger['builds']:
            status = f.get(build)
            if status not in known:
                found.append(f"{f['id']}: {build} has status {status!r}")
            if status in ('partial', 'not-possible') and not f.get('note'):
                found.append(f"{f['id']}: {build} is {status} and needs a note")
    return found


def main():
    ledger = json.loads((root / LEDGER).read_text())
    if sys.argv[1:] == ['--open']:
        for build in ledger['builds']:
            missing = [f['name'] for f in ledger['features'] if f[build] == 'missing']
            print(f'{build}: {len(missing)} missing')
            for name in missing:
                print(f'  {name}')
        return 0
    found = problems(ledger)
    if len(sys.argv) == 3:
        changed = git('diff', '--name-only', sys.argv[1], sys.argv[2]).split()
        message = git('log', '--format=%B', f'{sys.argv[1]}..{sys.argv[2]}')
    else:
        changed = git('diff', '--cached', '--name-only').split()
        message = ''
    ui = [p for p in changed if p.startswith(UI)]
    if ui and LEDGER not in changed and '[parity: none]' not in message:
        found.append(f'UI changed without updating {LEDGER}: {", ".join(ui)}')
        found.append('Update the ledger, or put [parity: none] in the commit message.')
    print('\n'.join(found) or 'Parity ledger OK')
    return 1 if found else 0


sys.exit(main())
