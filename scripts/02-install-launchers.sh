#!/usr/bin/env bash
# Installs launcher APKs ALONGSIDE the stock launcher. Does not change Home, does not disable anything.
# Usage: 02-install-launchers.sh <tv> [name ...]   (default: xmb)
. "$(dirname "$0")/common.sh"; need_tv "$1"; shift; K="$(dirname "$0")/.."
( cd "$K/apks" && shasum -a 256 -c SHA256SUMS ) || { echo "APK hash mismatch, aborting"; exit 1; }
for n in ${@:-xmb}; do
  l=$(grep "^$n|" "$(dirname "$0")/launchers.conf") || { echo "unknown launcher $n"; continue; }
  apk=$(echo "$l" | cut -d'|' -f3)
  [ -f "$K/$apk" ] || { echo "$n: no APK ($apk)"; continue; }
  echo "installing $n"; adb -s "$TV" install -r "$K/$apk"
done
echo "Now open each from the Apps row and confirm it launches. Permissions are NOT granted by this script."
