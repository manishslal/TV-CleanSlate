# Device kit

ADB scripts for Android TV and Google TV. All take the TV's address.

| Script | What it does | Changes the TV? |
|---|---|---|
| `01-preflight-backup.sh <tv-ip>` | Saves device info, packages and settings to `backups/` | No |
| `02-install-launchers.sh` | Installs launchers listed in `launchers.conf` | Yes |
| `03-switch-launcher.sh` | Switches the active launcher | Yes |
| `99-restore-stock.sh` | Puts the stock launcher back | Yes |

`common.sh` holds shared helpers. `launchers.conf` lists launcher names, packages and APK paths. Put APKs in `apks/` at the repo root, which git ignores.

The stock launcher default in `common.sh` is Google TV's `launcherx`. Android TV 9 to 11 uses `com.google.android.tvlauncher`.

Read the [safety guide](../guides/safety-and-logistics.md) first.
