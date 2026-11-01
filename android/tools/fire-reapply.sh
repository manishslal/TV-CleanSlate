#!/usr/bin/env bash
# Put Home's setup back on a Fire OS 6 stick, for example after a Fire OS update. Safe to run any
# time: it changes only what has drifted, and reports what it could not restore.
#
#   TV=<tv-ip> tools/fire-reapply.sh          # check, repair, report
#   TV=<tv-ip> tools/fire-reapply.sh check    # report only
set -euo pipefail
cd "$(dirname "$0")/.."
: "${TV:?set TV to the stick IP address}"
MODE="${1:-repair}"
case "$MODE" in repair|check) ;; *) echo "use repair or check" >&2; exit 1 ;; esac
case "$TV" in *:*) export ANDROID_SERIAL="$TV" ;; *) export ANDROID_SERIAL="$TV:5555" ;; esac
PKG="${PKG:-io.manish.home}"
NS=app.orchard.tv
LISTS=../devices/fire-tv-stick-4k-gen1
adb connect "$ANDROID_SERIAL" >/dev/null
sh() { adb shell "$@" | tr -d '\r'; }
[ "$(sh getprop ro.product.manufacturer)" = Amazon ] || { echo "Not a Fire TV." >&2; exit 1; }
sh pm path "$PKG" | grep -q package: || { echo "Home is not installed: run tools/install.sh first." >&2; exit 1; }

drift=0 bad=0
fix() { # what, then the command that repairs it
  local what="$1"; shift
  drift=$((drift + 1))
  if [ "$MODE" = check ]; then echo "drifted: $what"; return; fi
  sh "$@" >/dev/null 2>&1 || true
  echo "restored: $what"
}
want() { # namespace, key, value
  [ "$(sh settings get "$1" "$2")" = "$3" ] || fix "$2=$3" settings put "$1" "$2" "$3"
}

for perm in WRITE_SECURE_SETTINGS READ_LOGS; do
  sh dumpsys package "$PKG" | grep -q "android.permission.$perm: granted=true" || fix "$perm" pm grant "$PKG" "android.permission.$perm"
done
[ "$(sh appops get "$PKG" GET_USAGE_STATS | grep -c allow)" != 0 ] || fix "usage stats" appops set "$PKG" GET_USAGE_STATS allow
want secure enabled_notification_listeners "$PKG/$NS.guard.RevivalListener"
want secure screensaver_components "$PKG/$NS.dream.AerialDream"
want secure screensaver_enabled 1
want secure limit_ad_tracking 1
want secure amz_limit_ad_tracking 1
want secure USAGE_METRICS_UPLOAD_ENABLED 0
want secure usage_metrics_marketing_enabled 0
for key in window_animation_scale transition_animation_scale animator_duration_scale; do want global "$key" 0.5; done

disabled=$(sh pm list packages -d)
for p in $(grep -v '^#' "$LISTS/debloat-disable.txt"); do
  grep -q "package:$p\$" <<<"$disabled" || fix "disable $p" pm disable-user --user 0 "$p"
done
for p in $(grep -v '^#' "$LISTS/debloat-background.txt"); do
  sh appops get "$p" RUN_IN_BACKGROUND | grep -q ignore || fix "background block $p" appops set "$p" RUN_IN_BACKGROUND ignore
done

if [ "$MODE" = repair ]; then
  # Tells Home the check ran against this firmware; it clears the note in its menu.
  sh settings put global home_fire_build "$(sh getprop ro.build.fingerprint)"
  sh am force-stop "$PKG"
  sh am start -n "$PKG/$NS.MainActivity" >/dev/null
  sleep 3
  disabled=$(sh pm list packages -d)
  for p in $(grep -v '^#' "$LISTS/debloat-disable.txt"); do
    grep -q "package:$p\$" <<<"$disabled" || { echo "Fire OS refused: $p"; bad=$((bad + 1)); }
  done
fi
echo "$(sh getprop ro.build.version.name | cut -c1-40): $drift drifted, $bad refused."
[ "$MODE" = check ] || sh dumpsys window | grep -m1 mCurrentFocus | grep -q "$PKG" || { echo "Home is not in front."; exit 1; }
