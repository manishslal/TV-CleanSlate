#!/usr/bin/env bash
# List a TV's launchable apps that have no logo in logos/manifest.json. Run it before a new install:
# the same app often has a different package ID on another platform.
#
#   TV=<tv-ip> tools/logo-audit.sh
set -euo pipefail
cd "$(dirname "$0")/.."
: "${TV:?set TV to the TV IP address}"
case "$TV" in *:*) SERIAL="$TV" ;; *) SERIAL="$TV:5555" ;; esac
adb connect "$SERIAL" >/dev/null
adb -s "$SERIAL" shell 'for p in $(pm list packages | sed s/package://); do for c in LEANBACK_LAUNCHER LAUNCHER; do
  r=$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.$c $p 2>/dev/null | tail -1)
  case "$r" in */*) echo "${r%%/*}"; break ;; esac; done; done' | tr -d '\r' | sort -u | python3 -c '
import json, re, sys
known = {i for b in json.load(open("../logos/manifest.json")).values() for i in b.get("android", [])}
# Apps Home never lists on Fire TV, Home itself and Amazon launcher need no logo.
apps = open("app/src/main/java/app/orchard/tv/apps/Apps.kt").read()
known |= set(re.findall(r"\"([\w.]+)\"", apps[apps.index("FIRE_UNLISTED = setOf("):].split(")", 1)[0])) | {"io.manish.home", "com.amazon.tv.launcher"}
missing = [p.strip() for p in sys.stdin if re.fullmatch(r"[A-Za-z][\w.]+", p.strip()) and p.strip() not in known]
print("\n".join(missing) or "Every launchable app has a logo.")'
