# Sourced by the other scripts. Usage in each: . "$(dirname "$0")/common.sh"; need_tv "$1"
need_tv() {
  TV=${1:?usage: $(basename "$0") <tv-ip[:port]>}
  case $TV in *:*) ;; *) TV=$TV:5555 ;; esac
  adb connect "$TV" >/dev/null 2>&1
  adb -s "$TV" get-state >/dev/null 2>&1 || { echo "cannot reach $TV (accept the RSA prompt on the TV?)"; exit 1; }
  A() { adb -s "$TV" shell "$@" | tr -d '\r'; }
}
HOME_INTENT="-a android.intent.action.MAIN -c android.intent.category.HOME"
STOCK_PKG=${STOCK_PKG:-com.google.android.apps.tv.launcherx}   # Google TV. Android TV 9-11: STOCK_PKG=com.google.android.tvlauncher
