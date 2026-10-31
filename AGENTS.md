# Agent guide

TV CleanSlate is a monorepo: a Home app for Android and Google TV, a web version for rooted LG webOS, and scripts and guides for safely debloating TVs.

## Map

| Path | Contents |
|---|---|
| `android/` | Kotlin and Jetpack Compose app, a fork of Aerial. Read `android/AGENTS.md` before changing it. |
| `webos/` | Web Home for rooted LG TVs. Read `webos/README.md`. |
| `scripts/` | ADB scripts for backup, launcher switching and restore. |
| `guides/` | Setup, safety, troubleshooting and debloat prompts. Follow `safety.md` on any real TV. |
| `devices/` | One folder per tested device, plus `devices.json`. |
| `logos/` | `manifest.json` only. Artwork stays private. |
| `docs/` | `SUPPORTED_DEVICES.md`, `ARCHITECTURE.md`, images and research. |

## Commands

```bash
cd android
export JAVA_HOME=/opt/homebrew/opt/openjdk@17   # any JDK 17
./gradlew assembleStandardRelease testStandardReleaseUnitTest
BUILD=1 TV=<tv-ip>:5555 tools/install.sh        # install on a TV
python3 webos/build.py                          # from the repo root; needs Pillow and logos/dark/
```

## Rules for real TVs

- Disable with `pm disable-user`. Don't uninstall system packages without the owner's approval.
- Never touch input, remote, keyboard, Wi-Fi or update packages.
- Never copy a disable list between models.
- Don't run a command on a TV just to see what it does.
- Check which device an ADB address belongs to before changing it.
- Read the focused element before pressing OK when driving a TV remotely.
- Change a few things, reboot and check, then log the change and its undo in `devices/<device>/CHANGELOG.md`.
- Don't enter pairing codes or passwords. The owner does that.

## Rules for the repository

- Never commit `local/`, `android/local/`, `backups/`, `apks/`, `keys/`, `logos/dark/`, signing keys or generated webOS output.
- Never commit IP addresses, MAC addresses, Wi-Fi names, cast or device names, serial numbers, ADB mDNS names, home locations or tokens. Use `<tv-ip>` and similar placeholders.
- Keep `android/LICENSE` and upstream copyright notices unchanged.
- Add new third-party material to `CREDITS.md` with its license.
- Don't say something works until it has been checked on a real device.
- Update `devices/devices.json` and `docs/SUPPORTED_DEVICES.md` together.
