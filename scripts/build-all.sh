#!/bin/bash
# Rebuild every Home from the current sources, install nothing, and say where the builds differ.
#   scripts/build-all.sh            webOS page and Samsung package
#   ANDROID=1 scripts/build-all.sh  also the Android APKs (slow)
set -eu
cd "$(dirname "$0")/.."
python3 webos/build.py
python3 tizen/build.py
# shellcheck disable=SC2012  # newest by modification time
(cd tizen && ~/tizen-studio/tools/ide/bin/tizen package -t wgt -s home -o "$PWD" -- build >/dev/null &&
  mv -f "$(ls -t ./*.wgt | head -1)" home.wgt 2>/dev/null || true)
if [ "${ANDROID:-}" = 1 ]; then (cd android && ./gradlew assembleStandardRelease assembleGuardRelease -q); fi
echo
echo "Android $(grep -o 'versionName = "[^"]*"' android/app/build.gradle.kts)"
for f in android/app/build/outputs/apk/standard/release/*.apk webos/index.html tizen/home.wgt; do
  [ -f "$f" ] && echo "$f  built $(date -r "$f" '+%F %T')"
done
echo
python3 scripts/check-parity.py
python3 scripts/check-parity.py --open | grep -v '^  '
