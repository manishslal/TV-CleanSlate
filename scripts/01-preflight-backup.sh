#!/usr/bin/env bash
# READ-ONLY. Snapshots device state into backups/<model>-<date>/. Changes nothing on the TV.
. "$(dirname "$0")/common.sh"; need_tv "$1"
M=$(A getprop ro.product.model | tr ' /' '__'); OUT="$(dirname "$0")/../backups/$M-$(date +%Y%m%d-%H%M%S)"; mkdir -p "$OUT"
{ for k in ro.product.manufacturer ro.product.model ro.product.device ro.build.version.release ro.build.version.sdk ro.build.fingerprint ro.build.version.security_patch ro.boot.flash.locked; do echo "$k=$(A getprop $k)"; done; } > "$OUT/props.txt"
A pm list packages -f   > "$OUT/packages-full.txt"
A pm list packages -d   > "$OUT/packages-disabled.txt"
A pm list packages -3   > "$OUT/packages-3rdparty.txt"
A pm list packages -s   > "$OUT/packages-system.txt"
A cmd package query-activities --brief $HOME_INTENT > "$OUT/home-candidates.txt"
A cmd package resolve-activity --brief $HOME_INTENT > "$OUT/home-owner.txt"
for ns in global secure system; do A settings list $ns > "$OUT/settings-$ns.txt"; done
A "grep -E 'MemTotal|MemAvailable' /proc/meminfo" > "$OUT/meminfo.txt"
A uptime > "$OUT/uptime.txt"
A dumpsys package $STOCK_PKG 2>/dev/null | grep -E 'versionName|enabled=' | head -5 > "$OUT/stock-launcher.txt"
adb -s "$TV" exec-out screencap -p > "$OUT/screen.png" 2>/dev/null
for f in "$OUT"/*.txt; do [ -s "$f" ] || echo "WARNING: empty $(basename "$f")"; done
echo "backup: $OUT"; cat "$OUT/props.txt"; echo "home owner:"; cat "$OUT/home-owner.txt"; echo "candidates:"; cat "$OUT/home-candidates.txt"
