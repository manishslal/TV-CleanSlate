#!/usr/bin/env bash
# Take a TV from "new" to "ready": identify it, plan, back up, install Home, debloat in batches,
# verify, and say what is left to record. One approval covers the plan; every step is a script
# that already exists, so what it does is what its own header says.
#
# Usage: onboard.sh <tv-ip[:port]> [--yes] [--keys] [--reboot] [--skip-debloat] [--close-adb] [--platform=android|tizen|webos]
#   Android TV, Google TV and Fire TV are handled here. A Samsung (Tizen) or LG (webOS) TV is found by its
#   ports and handed to onboard-web.sh, which has its own options (--shot, --close-debug).
#   (no --yes)      read the TV and print the plan. Changes nothing on the TV.
#   --yes           run the plan. The plan was shown by the run before, or is printed first.
#   --keys          verification also presses Home like the remote: only if nobody is using the TV.
#   --reboot        verification reboots the TV and checks that Home comes back first.
#   --skip-debloat  install and verify only.
#   --close-adb     last step: switch network ADB off and prove it closed (adb-closed.py). The TV
#                   cannot be reached again until debugging is switched on by hand.
# EXPECT=<model> refuses to continue if the address belongs to some other device.
# BUILD=0 APK_FILE=<apk> installs a file instead of building from the checkout.
#
# What it never does: copy a list between models, touch input, remote, keyboard, Wi-Fi or update
# packages (protected.sh), run more than five packages at a time, or decide a debloat the project
# has not tested for the model (it says so and stops there). The profile is devices/profiles.json.
HERE=$(cd "$(dirname "$0")" && pwd); ROOT=$(dirname "$HERE")
# Which kind of TV is at this address? Android answers ADB on 5555; a Samsung answers on 8001; an LG on 3000 or 3001.
PLATFORM=auto; PASS=()
for a in "${@:2}"; do case $a in --platform=*) PLATFORM=${a#--platform=} ;; *) PASS+=("$a") ;; esac; done
if [ "$PLATFORM" = auto ]; then
  H=${1:?usage: $(basename "$0") <tv-ip[:port]> [--yes]}; H=${H%%:*}
  if nc -z -G 2 "$H" 5555 2>/dev/null; then PLATFORM=android
  elif curl -s --max-time 4 "http://$H:8001/api/v2/" 2>/dev/null | grep -q '"device"'; then PLATFORM=tizen
  elif nc -z -G 2 "$H" 3001 2>/dev/null || nc -z -G 2 "$H" 3000 2>/dev/null; then PLATFORM=webos
  else PLATFORM=android; fi
fi
case $PLATFORM in
  tizen|webos) exec bash "$HERE/onboard-web.sh" "$PLATFORM" "$1" "${PASS[@]+"${PASS[@]}"}" ;;
  android) ;;
  *) echo "--platform must be android, tizen or webos"; exit 2 ;;
esac
# shellcheck source=common.sh
. "$HERE/common.sh"; need_tv "$1"; set -- "${PASS[@]+"${PASS[@]}"}"
YES_RUN=${YES:-0} KEYS="" REBOOT="" SKIP_DEBLOAT=0 CLOSE=0
for a in "$@"; do case $a in
  --yes) YES_RUN=1 ;; --keys) KEYS=--keys ;; --reboot) REBOOT=--reboot ;;
  --skip-debloat) SKIP_DEBLOAT=1 ;; --close-adb) CLOSE=1 ;;
  *) echo "unknown option $a"; exit 2 ;; esac; done

# ---- 1. Identify -------------------------------------------------------------------------------
MANU=$(A getprop ro.product.manufacturer); MODEL=$(A getprop ro.product.model)
DEVICE=$(A getprop ro.product.device); PRODUCT=$(A getprop ro.product.name)
SDK=$(A getprop ro.build.version.sdk); REL=$(A getprop ro.build.version.release)
FIRMWARE=$(A getprop ro.build.version.name); [ -n "$FIRMWARE" ] || FIRMWARE="Android $REL"
FONT=$(A settings get system font_scale)
RAM=$(A "grep MemTotal /proc/meminfo" | awk '{printf "%.1f GB", $2/1048576}')
if [ -n "${EXPECT:-}" ] && [ "$EXPECT" != "$MODEL" ] && [ "$EXPECT" != "$DEVICE" ] && [ "$EXPECT" != "$PRODUCT" ]; then
  echo "This address is a $MODEL ($DEVICE), not the $EXPECT you expected. Nothing was changed."; exit 1
fi
eval "$(python3 -I "$HERE/device-profile.py" "$MANU" "$SDK" "$MODEL" "$DEVICE" "$PRODUCT")" || { echo "devices/profiles.json has no profile for this device."; exit 1; }
KNOWN=0; [ -n "$MODEL_DIR" ] && KNOWN=1

# ---- 2. Debloat plan: this model's own tested list, still enabled, five a batch ------------------
N_LEFT=0; N_DONE=0; LIST=""
if [ "$SKIP_DEBLOAT" = 0 ] && [ "$DEBLOAT_MODE" = list ] && [ -n "$MODEL_DIR" ]; then
  LIST="$ROOT/devices/$MODEL_DIR/debloat-disable.txt"
  if [ -r "$LIST" ]; then
    ENABLED=$(A pm list packages -e | sed 's/package://')
    WANT=$(sed -E 's/[[:space:]]*#.*$//' "$LIST" | awk 'NF{print $1}')
    N_DONE=$(printf '%s\n' "$WANT" | grep -vFx "$ENABLED" | grep -c . || true)
    TODO=$(printf '%s\n' "$WANT" | grep -Fx "$ENABLED" || true)
    N_LEFT=$(printf '%s\n' "$TODO" | grep -c . || true)
  fi
fi

# ---- 3. The plan -------------------------------------------------------------------------------
echo "=== $MANU $MODEL ($DEVICE), $FIRMWARE, SDK $SDK, $RAM RAM, text size $FONT"
echo "Address $TV. Profile: $PROFILE_LABEL ($STATUS)."
[ "$KNOWN" = 1 ] && echo "Known model: devices/$MODEL_DIR/" || echo "Model not in devices/profiles.json: this is a first run for it."
echo
echo "Plan:"
echo "  1. Back up (01-preflight-backup.sh, read-only): packages, settings, Home owner, a screenshot."
if [ "$FLAVOR" = none ]; then
  echo "  2. Install: none. $MECHANISM."
  echo "  3. Verify: nothing to verify; Home is not installed."
else
  echo "  2. Install the $FLAVOR build (android/tools/install.sh $FLAVOR). Mechanism: $MECHANISM."
  echo "  3. Verify (verify-device.sh ${KEYS:-without keys}${REBOOT:+ $REBOOT}): Home installed, Home key answered, Settings stays open, nothing protected disabled."
fi
case $DEBLOAT_MODE in
  list)
    if [ "$SKIP_DEBLOAT" = 1 ]; then echo "  4. Debloat: skipped (--skip-debloat)."
    elif [ -z "$LIST" ] || [ ! -r "$LIST" ]; then echo "  4. Debloat: no tested list for this model (devices/$MODEL_DIR/debloat-disable.txt). Stops after verifying."
    else echo "  4. Debloat with devices/$MODEL_DIR/debloat-disable.txt: $N_LEFT still enabled in batches of five (05-disable-list.sh), verified after each; $N_DONE already off."; fi ;;
  fire-reapply) echo "  4. Debloat with android/tools/fire-reapply.sh (this Fire OS refuses plain disables); install.sh runs it." ;;
  review) echo "  4. Debloat: NOT automatic. $DEBLOAT_WHY." ;;
  *) echo "  4. Debloat: none. $DEBLOAT_WHY." ;;
esac
echo "  5. Record: changelog section, devices.json, SUPPORTED_DEVICES.md, LESSONS.md, parity (list below)."
if [ "$CLOSE" = 1 ]; then echo "  6. Switch network ADB off and prove it (adb-closed.py)."; else echo "  6. Leave ADB as it is (add --close-adb to switch it off; rules want that when you are done)."; fi
if [ -n "$QUIRKS" ]; then echo; echo "Known on this kind of device:"; printf '%s\n' "$QUIRKS" | sed 's/^/  - /'; fi
if [ -z "$KEYS" ]; then echo; echo "No remote key will be sent. Verification starts Settings and Home by intent, which flips the screen for a few seconds."; fi

if [ "$YES_RUN" != 1 ]; then echo; echo "Nothing was changed. To run this plan: $(basename "$0") ${TV%%:*} --yes${KEYS:+ $KEYS}${REBOOT:+ $REBOOT}"; exit 0; fi

# ---- 4. Run ------------------------------------------------------------------------------------
STAMP=$(date +%Y%m%d-%H%M%S); REPORT_DIR="$ROOT/backups/onboard-$MODEL-$STAMP"; mkdir -p "$REPORT_DIR"
REPORT="$REPORT_DIR/report.txt"
log() { echo "$*" | tee -a "$REPORT"; }
log "onboard $MODEL ($DEVICE) $FIRMWARE, profile $PROFILE_ID, $(date)"
stop() { log "STOPPED: $*"; log "Report: $REPORT"; exit 1; }
verify() { bash "$HERE/verify-device.sh" "$TV" "$@" 2>&1 | tee -a "$REPORT"; return "${PIPESTATUS[0]}"; }

log "--- 1. backup"; bash "$HERE/01-preflight-backup.sh" "$TV" >>"$REPORT" 2>&1 || stop "the backup failed"
log "backup written under backups/ (git ignores it)"
if [ "$FLAVOR" = none ]; then
  log "This profile installs nothing ($MECHANISM). The backup is the whole run."
  log "Next: $DEBLOAT_WHY."
  log "Report: $REPORT"; exit 0
fi

log "--- 2. install ($FLAVOR)"
FLAVOR_ARG=""; [ "$FLAVOR" = standard ] || FLAVOR_ARG=$FLAVOR
# shellcheck disable=SC2086  # FLAVOR_ARG is one word or empty
( cd "$ROOT/android" && BUILD="${BUILD:-1}" TV="$TV" tools/install.sh $FLAVOR_ARG ) 2>&1 | tee -a "$REPORT"
[ "${PIPESTATUS[0]}" = 0 ] || stop "install.sh failed; Home is not installed. Undo: android/tools/restore.sh"

log "--- 3. verify before any debloat"
verify || stop "verification failed before debloating. Fix that first; nothing was disabled."

log "--- 4. debloat"
if [ "$SKIP_DEBLOAT" = 1 ]; then log "skipped"
elif [ "$DEBLOAT_MODE" = fire-reapply ]; then log "done by install.sh (fire-reapply.sh)"
elif [ "$DEBLOAT_MODE" = list ] && [ "$N_LEFT" -gt 0 ]; then
  TMP=$(mktemp -d); printf '%s\n' "$TODO" | split -l 5 - "$TMP/batch-"
  for b in "$TMP"/batch-*; do
    log "batch: $(tr '\n' ' ' < "$b")"
    YES=1 bash "$HERE/05-disable-list.sh" "$TV" "$b" 2>&1 | tee -a "$REPORT"
    if [ "${PIPESTATUS[0]}" != 0 ]; then stop "05-disable-list.sh refused or failed on that batch. Undo it: for each line, adb shell pm enable <package>"; fi
    verify || stop "verification failed after that batch. Undo it: for each line, adb shell pm enable <package>"
  done
  rm -rf "$TMP"
else log "nothing automatic to do ($DEBLOAT_MODE)"; fi

log "--- 5. final verification"
# shellcheck disable=SC2086  # KEYS and REBOOT are options or empty
verify $KEYS $REBOOT || stop "the final verification failed"

if [ "$CLOSE" = 1 ]; then
  log "--- 6. close ADB"
  A settings put global adb_enabled 0; sleep 3
  python3 -I "$HERE/adb-closed.py" "${TV%%:*}" 2>&1 | tee -a "$REPORT" || log "ADB is still open or could not be checked; see adb-closed.py"
fi

log
log "READY: $MODEL is installed and verified. Report: $REPORT"
log "Still to record (AGENTS.md rules): devices/${MODEL_DIR:-<new-device>}/CHANGELOG.md with what was done and its undo,"
log "  devices/devices.json and docs/SUPPORTED_DEVICES.md together, docs/LESSONS.md for what this TV taught us,"
log "  logos/README.md for logo gaps, docs/parity.json if a feature changed, and a profile in devices/profiles.json"
[ "$KNOWN" = 1 ] || log "  (new model: add it under \"models\" in devices/profiles.json, or a new profile if it behaves differently)"
[ "$CLOSE" = 1 ] || log "  Debugging is still on: switch it off when you are done and prove it with scripts/adb-closed.py."
