# shellcheck shell=bash
# Sourced by the other scripts. Usage in each: . "$(dirname "$0")/common.sh"; need_tv "$1"
need_tv() {
  TV=${1:?usage: $(basename "$0") <tv-ip[:port]>}
  case $TV in *:*) ;; *) TV=$TV:5555 ;; esac
  adb connect "$TV" >/dev/null 2>&1
  adb -s "$TV" get-state >/dev/null 2>&1 || { echo "cannot reach $TV (accept the RSA prompt on the TV?)"; exit 1; }
  # shellcheck disable=SC2329  # called by the scripts that source this file
  A() { adb -s "$TV" shell "$@" | tr -d '\r'; }
}
# Says what is about to change on the TV and waits for a yes. YES=1 skips the question (for a script you've read).
confirm() {
  echo "On $TV this will: $*"
  [ "${YES:-}" = 1 ] && return 0
  [ -t 0 ] || { echo "Not asking without a terminal: run again with YES=1 if you mean it."; exit 1; }
  printf 'Go ahead? [y/N] '; read -r ok
  case $ok in y|Y|yes) ;; *) echo "Nothing changed."; exit 1 ;; esac
}
# shellcheck disable=SC2034  # used by the scripts that source this file
HOME_INTENT="-a android.intent.action.MAIN -c android.intent.category.HOME"
STOCK_PKG=${STOCK_PKG:-com.google.android.apps.tv.launcherx}   # Google TV. Android TV 9-11: STOCK_PKG=com.google.android.tvlauncher
