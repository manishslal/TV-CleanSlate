#!/usr/bin/env bash
# Check that Home works on a TV. Prints PASS or FAIL for each check and exits with the number of FAILs.
# Usage: verify-device.sh <tv-ip[:port]> [--keys] [--reboot]
#
# By default it sends no remote key. It starts Settings and then Home by intent, which flips the
# screen for a few seconds, so say so first if someone is watching.
#   --keys    also presses Home from an app and from Settings, as the remote would. Keys from this
#             script mix with keys from a remote someone is holding: only with the owner's say-so.
#   --reboot  waits 40 s (so Android has saved package changes), reboots, waits for the TV and checks
#             that Home is in front again. The TV goes off for a few minutes.
# What it needs from the profile (devices/profiles.json) it reads through device-profile.py.
# shellcheck source=common.sh
. "$(dirname "$0")/common.sh"; need_tv "$1"; shift
# shellcheck source=protected.sh
. "$(dirname "$0")/protected.sh"
KEYS=0 REBOOT=0
for a in "$@"; do case $a in --keys) KEYS=1 ;; --reboot) REBOOT=1 ;; *) echo "unknown option $a"; exit 2 ;; esac; done
PKG=${PKG:-io.manish.home}
HOME_ACT="$PKG/app.orchard.tv.MainActivity"
fails=0
pass() { echo "PASS  $*"; }
fail() { echo "FAIL  $*"; fails=$((fails + 1)); }
info() { echo "info  $*"; }
focus() { A dumpsys window | grep -m1 mCurrentFocus | sed 's/.*u0 //; s/}.*//'; }
# Waits up to $2 seconds for the focused window to begin with $1 (a package or package/activity).
wait_focus() { local i=0; while [ "$i" -lt "$2" ]; do case "$(focus)" in "$1"*) return 0 ;; esac; sleep 1; i=$((i + 1)); done; return 1; }

MANU=$(A getprop ro.product.manufacturer); SDK=$(A getprop ro.build.version.sdk)
eval "$(python3 -I "$(dirname "$0")/device-profile.py" "$MANU" "$SDK" "$(A getprop ro.product.model)" "$(A getprop ro.product.device)" "$(A getprop ro.product.name)")"
echo "$(A getprop ro.product.model) ($MANU, SDK $SDK): profile $PROFILE_ID, $FLAVOR build, $STATUS"

if [ "$REBOOT" = 1 ]; then
  echo "Waiting 40 s so Android saves the state, then rebooting."
  sleep 40; adb -s "$TV" reboot; sleep 20
  i=0; until [ "$(adb -s "$TV" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do
    adb connect "$TV" >/dev/null 2>&1; sleep 5; i=$((i + 1))
    [ "$i" -gt 60 ] && { fail "the TV did not come back within 5 minutes"; exit "$fails"; }
  done
  info "booted; giving it 45 s to settle"; sleep 45
  if wait_focus "$PKG" 30; then pass "Home is in front after a reboot"; else fail "after a reboot the front is $(focus), not Home"; fi
fi

# 1. Installed
VER=$(A dumpsys package "$PKG" | sed -n 's/.*versionName=//p' | head -1)
if [ -n "$VER" ]; then pass "Home $VER is installed"; else fail "Home ($PKG) is not installed"; exit "$fails"; fi

# 2. Something answers the Home key: the HOME role, or Home Guard bound, or FireKeeper's process on Fire OS 5 and 6
HOME_OWNER=$(A cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)
GUARD_ON=$(A settings get secure enabled_accessibility_services)
if A dumpsys accessibility | grep -q "Bound services:.*Home.*Guard"; then pass "Home Guard is bound"
elif [ "$MANU" = Amazon ] && [ "$SDK" -lt 26 ]; then
  if A ps -A 2>/dev/null | grep -q "$PKG"; then pass "Fire OS 5 or 6: Home's process runs, FireKeeper does the Home key"; else fail "Home's process is not running"; fi
elif case $HOME_OWNER in "$PKG"*) true ;; *) false ;; esac; then pass "Home holds the HOME role"
else fail "nothing sends the Home key to Home (HOME is $HOME_OWNER, accessibility: $GUARD_ON)"; fi

# 3. Settings opens and stays, and Home can be brought back
if [ -n "$SETTINGS_COMPONENT" ]; then A am start -n "$SETTINGS_COMPONENT" >/dev/null 2>&1; else A am start -a android.settings.SETTINGS >/dev/null 2>&1; fi
sleep 3; F=$(focus)
case $F in
  "$PKG"*|"") fail "Settings did not stay in front (focus: ${F:-none}): the guard may be pulling Home back, or no settings screen answers" ;;
  *) pass "Settings stays in front ($F)" ;;
esac
if [ "$KEYS" = 1 ]; then
  A input keyevent KEYCODE_HOME
  if wait_focus "$PKG" 6; then pass "Home key from Settings returns to Home"; else fail "Home key from Settings left $(focus) in front"; fi
else
  A am start -n "$HOME_ACT" >/dev/null 2>&1
  if wait_focus "$PKG" 4; then pass "Home starts again by intent"; else fail "Home did not come back by intent (focus $(focus))"; fi
fi

# 4. Home key from another app (keys only)
if [ "$KEYS" = 1 ]; then
  THIRD=$(A pm list packages -3 | sed 's/package://')
  APP=$(for c in LAUNCHER LEANBACK_LAUNCHER; do A cmd package query-activities --brief -a android.intent.action.MAIN -c "android.intent.category.$c"; done |
    grep -E '^ +[a-z]' | tr -d ' ' | cut -d/ -f1 | sort -u | grep -v "^$PKG$" | grep -Fx "$THIRD" | head -1)
  if [ -n "$APP" ]; then
    A monkey -p "$APP" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 6
    case "$(focus)" in "$PKG"*) fail "could not open $APP to test Home from an app" ;; *)
      A input keyevent KEYCODE_HOME
      if wait_focus "$PKG" 6; then pass "Home key from $APP returns to Home"; else fail "Home key from $APP left $(focus) in front"; fi ;;
    esac
  else info "no third-party app to open: Home key from an app not tested"; fi
fi

# 5. Nothing protected is disabled
BAD=""
for p in $(A pm list packages -d | sed 's/package://'); do is_protected "$p" && BAD="$BAD $p"; done
if [ -z "$BAD" ]; then pass "no protected package is disabled ($(A pm list packages -d | wc -l | tr -d ' ') disabled)"; else fail "protected packages disabled:$BAD"; fi

# 6. Read-only facts worth a look
FS=$(A settings get system font_scale)
info "text size font_scale=$FS (Home's type is sized for 0.85; Amazon and TCL are read against 1.0 in canvasFontScale)"
info "display $(A wm size | tail -1 | sed 's/.*: //') $(A wm density | tail -1 | sed 's/.*: //') dpi, SDK $SDK"
if python3 -I "$(dirname "$0")/adb-closed.py" "${TV%%:*}" >/dev/null 2>&1; then info "network ADB is closed"; else info "network ADB is open: switch debugging off when you are done (scripts/adb-closed.py proves it)"; fi
echo "$fails check(s) failed."
exit "$fails"
