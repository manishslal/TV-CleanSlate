# Safety

Follow this before changing any TV. Every step here came from real testing.

## Rules

1. **Back up first.** Run `scripts/01-preflight-backup.sh <tv-ip>`.
2. **Reboot once with no changes.** This proves the TV and ADB come back normally.
3. **Disable, don't uninstall.** Use `pm disable-user --user 0`.
4. **Leave these alone:** input, remote, keyboard, Wi-Fi, HDMI and update packages.
5. **Work in small batches.** Five packages at most, then check Home, inputs, the remote, Wi-Fi and playback.
6. **Install the new Home before disabling the stock one.**
7. **Keep a restore command ready** before each batch.
8. **Write it down.** Record what changed and how to undo it.
9. **Treat each model separately.** Never copy a list from another model.
10. **Turn off network debugging when you finish.** It isn't encrypted.

## Known traps

| Device | Trap |
|---|---|
| Google TV | The setup wizard (`setupwraith`) may own your saved Wi-Fi networks. Disabling it can erase them. |
| Google TV | Some disabled packages come back at the next boot. |
| Hisense | Disabling packages didn't survive reboots, and one batch broke input switching. Don't debloat this model. |
| TCL | `com.tcl.appstatecontroller` re-enables some apps at each boot. Removing them for the user with `pm uninstall -k --user 0` was the one logged exception to rule 3. |
| LG | Don't install LG's Developer Mode app while rooted. |
| Any TV | Don't run a command just to see what it does. A screen-off test blanked the LG. |

## If something breaks

```bash
scripts/99-restore-stock.sh <tv-ip>
```

On Android TV 9 to 11, run it with `STOCK_PKG=com.google.android.tvlauncher`. Each [device log](../devices/README.md) also lists the exact undo steps for that TV.

Everything here is at your own risk.
