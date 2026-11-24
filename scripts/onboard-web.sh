#!/usr/bin/env bash
# Samsung Tizen and LG webOS: the web Home. onboard.sh calls this when the TV is not an Android device.
# Usage: onboard-web.sh <tizen|webos> <tv-ip> [--yes] [--shot] [--close-debug]
#   (no --yes)     read the TV and print the plan. Changes nothing on the TV.
#   --yes          Tizen: build and install Home through tizen/deploy.sh, if every requirement is met.
#                  webOS: refused. This project does not root TVs, and Home on LG needs a rooted one.
#   --shot         Tizen: screenshot Home afterwards (tvshot.py opens a debug port until Home restarts).
#   --close-debug  Tizen: prove the sdb port (26101) is closed, once the owner has switched Developer Mode off.
# What a Samsung allows is in tizen/README.md; what LG needs is in webos/README.md.
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd); ROOT=$(dirname "$HERE")
PLATFORM=${1:?tizen or webos}; TV=${2:?tv-ip}; shift 2; TV=${TV%%:*}
YES_RUN=${YES:-0} SHOT=0 CLOSE=0
for a in "$@"; do case $a in --yes) YES_RUN=1 ;; --shot) SHOT=1 ;; --close-debug) CLOSE=1 ;; *) echo "unknown option $a"; exit 2 ;; esac; done

# One lookup in devices/profiles.json: prints shell assignments for the platform and, if given, the model.
lookup() { python3 -I - "$ROOT/devices/profiles.json" "$PLATFORM" "${1:-}" <<'PY'
import json, shlex, sys
f, platform, code = sys.argv[1:4]
p = json.load(open(f))["platforms"][platform]
m = p.get("models", {}).get(code)
def out(k, v): print(f"{k}={shlex.quote(str(v))}")
out("P_LABEL", p["label"]); out("P_INSTALL", p["install"]); out("P_OWNER", p["owner_steps"]); out("P_STATUS", p["status"])
out("P_QUIRKS", "\n".join(p.get("quirks", [])))
out("M_KNOWN", 1 if m else 0); out("M_DIR", (m or {}).get("dir", ""))
out("M_LEAN", 1 if (m["lean"] if m and "lean" in m else p.get("unknown_lean", False)) else 0)
PY
}
quirks() { [ -n "$P_QUIRKS" ] && { echo; echo "Known on this kind of TV:"; printf '%s\n' "$P_QUIRKS" | sed 's/^/  - /'; }; }
record() {
  echo
  echo "Record it (AGENTS.md rules): devices/${M_DIR:-<new-device>}/CHANGELOG.md with what was done and how to undo it,"
  echo "  devices/devices.json and docs/SUPPORTED_DEVICES.md together, docs/LESSONS.md, logos/README.md for logo gaps,"
  echo "  docs/parity.json if a feature changed, and the model under \"platforms\" in devices/profiles.json. Use <tv-ip> in anything committed."
}

case $PLATFORM in
tizen)
  command -v jq >/dev/null || { echo "jq is required (brew install jq)."; exit 2; }
  INFO=$(bash "$ROOT/tizen/preflight.sh" "$TV" 2>&1) || { echo "$INFO"; echo "Nothing was changed."; exit 1; }
  CODE=$(printf '%s\n' "$INFO" | sed -n 's/^Platform code: *//p')
  eval "$(lookup "$CODE")"
  SDB_OPEN=0; printf '%s\n' "$INFO" | grep -q 'sdb port: *open' && SDB_OPEN=1
  DEVMODE=off; printf '%s\n' "$INFO" | grep -q 'Developer Mode: *on' && DEVMODE=on
  T="$HOME/tizen-studio/tools"
  MISSING=""
  [ -x "$T/sdb" ] || MISSING="$MISSING\n  - Tizen Studio's sdb ($T/sdb): install Tizen Studio's CLI tools"
  [ -x "$T/ide/bin/tizen" ] || MISSING="$MISSING\n  - Tizen Studio's tizen CLI, and a security profile named home (tizen/README.md step 3)"
  python3 -c "import PIL" 2>/dev/null || MISSING="$MISSING\n  - Pillow for python3 (pip install Pillow)"
  [ -d "$ROOT/logos/dark" ] && [ -n "$(ls -A "$ROOT/logos/dark" 2>/dev/null)" ] || MISSING="$MISSING\n  - your own logos in logos/dark/ (artwork stays private)"
  [ "$SDB_OPEN" = 1 ] || MISSING="$MISSING\n  - the TV's sdb port is closed: $P_OWNER"
  echo "=== Samsung Tizen: $(printf '%s\n' "$INFO" | sed -n 's/^Model: *//p') ($CODE), Tizen $(printf '%s\n' "$INFO" | sed -n 's/^Firmware: *//p')"
  # shellcheck disable=SC2001  # indenting each line, not a substitution
  echo "$INFO" | sed 's/^/  /'
  [ "$M_KNOWN" = 1 ] && echo "Known model: devices/$M_DIR/ ($P_STATUS)" || echo "Model not in devices/profiles.json: a first run for it."
  echo
  echo "Plan:"
  echo "  1. Check the requirements below."
  echo "  2. Build: $([ "$M_LEAN" = 1 ] && echo 'LEAN=1 python3 tizen/build.py (still wallpaper, no frost)' || echo 'python3 tizen/build.py (full)')."
  echo "  3. Install and open Home: TV=<tv-ip> tizen/deploy.sh."
  echo "  4. Verify$([ "$SHOT" = 1 ] && echo ': screenshot Home (opens a debug port until Home restarts)' || echo ': by eye on the TV; add --shot for a screenshot'). The Home button opens Smart Hub first on any Samsung."
  echo "  5. Finish: Developer Mode off on the TV, then prove the port closed (--close-debug runs adb-closed.py on 26101)."
  echo "Developer Mode: $DEVMODE."
  if [ -n "$MISSING" ]; then echo; echo "Missing before it can run:"; printf '%b\n' "${MISSING#\\n}"; fi
  quirks
  if [ "$CLOSE" = 1 ]; then
    echo; python3 -I "$HERE/adb-closed.py" "$TV" 26101 && echo "sdb port 26101 is closed." || echo "sdb port is open or could not be checked: switch Developer Mode off and run it again."
    exit 0
  fi
  [ "$YES_RUN" = 1 ] || { echo; echo "Nothing was changed. To run this plan: onboard.sh <tv-ip> --yes"; exit 0; }
  [ -z "$MISSING" ] || { echo; echo "Not running: the requirements above are not met."; exit 1; }
  echo; echo "--- build"
  if [ "$M_LEAN" = 1 ]; then LEAN=1 python3 "$ROOT/tizen/build.py" || exit 1; else python3 "$ROOT/tizen/build.py" || exit 1; fi
  echo "--- install"
  TV="$TV" bash "$ROOT/tizen/deploy.sh" || { echo "deploy.sh failed. Undo: uninstall Home from Apps on the TV."; exit 1; }
  if [ "$SHOT" = 1 ]; then
    OUT="$ROOT/backups/tizen-shot-$(date +%Y%m%d-%H%M%S).png"; mkdir -p "$ROOT/backups"
    TV="$TV" python3 "$ROOT/tizen/tvshot.py" shot "$OUT" && echo "screenshot: $OUT (look at it: a frozen or blank first frame is not success)"
  fi
  record
  echo "Finish: switch Developer Mode off on the TV, then: onboard.sh <tv-ip> --close-debug"
  ;;
webos)
  eval "$(lookup "")"
  echo "=== LG webOS (identified by address only: webOS answers no public model query)"
  echo "$P_LABEL: $P_STATUS"
  echo
  echo "Plan: none to run. $P_INSTALL"
  echo "  Owner's part: $P_OWNER"
  echo "  Once the TV is rooted, Home on LG is the files in webos/ (build.py, bake_frost.py, home-svc.js and the init.d hooks)."
  echo "  Read webos/README.md 'Risks and undo' first, and the LG device log, devices/lg-50up8000pua/CHANGELOG.md, for the exact steps and their undo."
  quirks
  record
  if [ "$YES_RUN" = 1 ]; then echo; echo "Refused: there is no automated webOS install. Rooting cannot be taken back and is the owner's decision."; exit 1; fi
  ;;
*) echo "platform must be tizen or webos"; exit 2 ;;
esac
