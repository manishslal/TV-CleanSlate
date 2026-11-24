# Device kit

ADB scripts for Android TV and Google TV. All take the TV's address. Each script that changes the TV says what it will do and waits for a yes; `YES=1` before the command skips the question. `99-restore-stock.sh` never asks, so it works in a hurry.

| Script | What it does | Changes the TV? |
|---|---|---|
| `01-preflight-backup.sh <tv-ip>` | Saves device info, packages and settings to `backups/`, which git ignores | No |
| `02-install-launchers.sh <tv-ip> [name]` | Installs launchers listed in `launchers.conf` | Yes |
| `03-switch-launcher.sh <tv-ip> <name or stock>` | Switches the active launcher | Yes |
| `04-disable-stock-launcher.sh <tv-ip> <home component>` | Disables Google TV's launcher and setup wizard, with safety checks | Yes |
| `onboard.sh <tv-ip> [--yes]` | Whole path for a new TV: identify, plan, back up, install, debloat in batches, verify, list what to record. Without `--yes` it only prints the plan. See [onboard-device.md](../guides/onboard-device.md) | With `--yes` |
| `onboard-web.sh <tizen\|webos> <tv-ip>` | What `onboard.sh` hands a Samsung or LG to. Samsung: reads the TV, checks Tizen Studio and Developer Mode, then builds and installs with `tizen/deploy.sh`. LG: prints the plan only, since Home there needs a rooted TV | Samsung with `--yes` |
| `verify-device.sh <tv-ip> [--keys] [--reboot]` | PASS or FAIL: Home installed, Home key answered, Settings stays open, nothing protected disabled | Flips the screen briefly |
| `device-profile.py` | Picks a device's profile from `devices/profiles.json` for the two scripts above | No |
| `99-restore-stock.sh <tv-ip>` | Puts the stock launcher back | Yes |

`common.sh` holds shared helpers. `launchers.conf` lists launcher names, packages and APK paths. Put APKs in `apks/` at the repo root, which git ignores.

The stock launcher defaults to Google TV's `launcherx`. On Android TV 9 to 11, put `STOCK_PKG=com.google.android.tvlauncher` before the command.

`launchers.conf` includes other projects' launchers for comparison. Get those APKs from their own projects.

Read the [safety guide](../guides/safety.md) first.
