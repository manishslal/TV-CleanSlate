# Device kit

ADB scripts for Android TV and Google TV. All take the TV's address. Each script that changes the TV says what it will do and waits for a yes; `YES=1` before the command skips the question. `99-restore-stock.sh` never asks, so it works in a hurry.

| Script | What it does | Changes the TV? |
|---|---|---|
| `01-preflight-backup.sh <tv-ip>` | Saves device info, packages and settings to `backups/`, which git ignores | No |
| `02-install-launchers.sh <tv-ip> [name]` | Installs launchers listed in `launchers.conf` | Yes |
| `03-switch-launcher.sh <tv-ip> <name or stock>` | Switches the active launcher | Yes |
| `04-disable-stock-launcher.sh <tv-ip> <home component>` | Disables Google TV's launcher and setup wizard, with safety checks | Yes |
| `99-restore-stock.sh <tv-ip>` | Puts the stock launcher back | Yes |

`common.sh` holds shared helpers. `launchers.conf` lists launcher names, packages and APK paths. Put APKs in `apks/` at the repo root, which git ignores.

The stock launcher defaults to Google TV's `launcherx`. On Android TV 9 to 11, put `STOCK_PKG=com.google.android.tvlauncher` before the command.

`launchers.conf` includes other projects' launchers for comparison. Get those APKs from their own projects.

Read the [safety guide](../guides/safety.md) first.
