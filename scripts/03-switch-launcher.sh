#!/usr/bin/env bash
# Make a launcher the Home app. Usage: 03-switch-launcher.sh <tv> <name|stock>
# Lets you rotate launchers. Stock stays installed and enabled, so this is fully reversible.
. "$(dirname "$0")/common.sh"; need_tv "$1"; W=${2:?name or stock}
[ "$W" = stock ] && PKG=$STOCK_PKG || PKG=$(grep "^$W|" "$(dirname "$0")/launchers.conf" | cut -d'|' -f2)
[ -n "$PKG" ] || { echo "unknown launcher $W"; exit 1; }
CMP=$(A cmd package query-activities --brief $HOME_INTENT | grep -oE "$PKG/[^ ]+" | head -1)
[ -n "$CMP" ] || { echo "$PKG has no HOME activity (installed? enabled?)"; A cmd package query-activities --brief $HOME_INTENT; exit 1; }
A cmd package set-home-activity "$CMP"; A input keyevent KEYCODE_HOME
echo "home -> $CMP"; A cmd package resolve-activity --brief $HOME_INTENT | tail -1
