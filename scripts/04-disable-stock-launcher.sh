#!/usr/bin/env bash
# Google TV: disable the stock launcher and setup wizard, so your Home is Android's real default.
# Usage: 04-disable-stock-launcher.sh <tv> <home-package/activity>
#   e.g. 04-disable-stock-launcher.sh <tv-ip> io.manish.home/app.orchard.tv.MainActivity
# Stops if the wizard owns a saved Wi-Fi network, because disabling it could erase that network.
# Undo: 99-restore-stock.sh <tv>
. "$(dirname "$0")/common.sh"; need_tv "$1"; NEW=${2:?home component}
WIZARD=com.google.android.tungsten.setupwraith
H="$HOME_INTENT -c android.intent.category.DEFAULT"
UID_W=$(A pm list packages -U "$WIZARD" | sed -n 's/.*uid://p' | head -1)
if [ -n "$UID_W" ] && A dumpsys wifi | grep -q "cuid=$UID_W"; then
  echo "The setup wizard owns a saved Wi-Fi network. In Settings, forget and rejoin it, then run this again."; exit 1
fi
A pm list packages "${NEW%%/*}" | grep -q . || { echo "${NEW%%/*} is not installed"; exit 1; }
A pm disable-user --user 0 "$WIZARD" | tail -1
A pm disable-user --user 0 "$STOCK_PKG" | tail -1
sleep 2; A cmd package set-home-activity "$NEW" | tail -1; sleep 1
R=$(A cmd package resolve-activity --brief $H | tail -1)
case "$R" in
  "${NEW%%/*}"/*) ;;
  *) echo "Android chose $R, so undoing."; A pm enable --user 0 "$STOCK_PKG"; A pm enable --user 0 "$WIZARD"; exit 1 ;;
esac
# Android writes this state about 10 s later. A reboot before that silently loses the change.
echo "Home is $R. Waiting 40 s so Android saves it."; sleep 40
echo "Done. Reboot, then check Home, Settings, Wi-Fi and the remote."
