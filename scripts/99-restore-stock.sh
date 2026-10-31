#!/usr/bin/env bash
# Emergency: put Google TV's stock launcher back. Safe to run any time. Does not uninstall custom launchers.
. "$(dirname "$0")/common.sh"; need_tv "$1"
for p in $STOCK_PKG com.google.android.tungsten.setupwraith com.google.android.katniss; do A pm enable --user 0 $p; done
CMP=$(A cmd package query-activities --brief $HOME_INTENT | grep -oE "$STOCK_PKG/[^ ]+" | head -1)
[ -n "$CMP" ] && A cmd package set-home-activity "$CMP"
A input keyevent KEYCODE_HOME
A cmd package resolve-activity --brief $HOME_INTENT | tail -1
