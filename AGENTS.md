# Agent guide

TV CleanSlate is a monorepo: a Home app for Android and Google TV, a web version for rooted LG webOS, and scripts and guides for safely debloating TVs.

## Map

| Path | Contents |
|---|---|
| `android/` | Kotlin and Jetpack Compose app, a fork of Aerial. Read `android/AGENTS.md` before changing it. |
| `webos/` | Web Home for rooted LG TVs. Read `webos/README.md`. |
| `tizen/` | Web Home for Samsung TVs, built from `webos/`. Running on two TVs. |
| `scripts/` | ADB scripts for backup, launcher switching and restore. |
| `guides/` | Setup, safety, troubleshooting and debloat prompts. Follow `safety.md` on any real TV. |
| `devices/` | One folder per tested device, plus `devices.json`. |
| `logos/` | `manifest.json` only. Artwork stays private. |
| `docs/` | `SUPPORTED_DEVICES.md`, `ARCHITECTURE.md`, `LESSONS.md`, `parity.json`, images and research. |

## Commands

```bash
cd android
export JAVA_HOME=/opt/homebrew/opt/openjdk@17   # any JDK 17
./gradlew assembleStandardRelease testStandardReleaseUnitTest
BUILD=1 TV=<tv-ip>:5555 tools/install.sh        # install on a TV
python3 webos/build.py                          # from the repo root; needs Pillow and logos/dark/
python3 tizen/build.py                          # LEAN=1 for a TV short on memory
TV=<tv-ip> tizen/deploy.sh                      # install on a Samsung
TV=<tv-ip> python3 tizen/tvshot.py shot out.png # screenshot; also eval and key
TV=<tv-ip> python3 tizen/tvkey.py KEY_RIGHT     # first run needs Allow on the TV
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
- On a Samsung, close debug mode when you finish, because its port is open to the network with no password. See `tizen/README.md`.
- On a video test, check that playback time moves. A frozen first frame reports as ready.
- After each TV, add what it taught us to `docs/LESSONS.md`, list new logo gaps in `logos/README.md`, and bring every fix to the other Home versions.
- Finish any change to one Home by running `scripts/build-all.sh` (add `ANDROID=1` for the APKs). It rebuilds the web and Samsung builds, shows when each was built, and lists what each is missing. For every "missing" that you could fix, fix it in the same change, or write why not in the ledger's note.
- A feature goes in `docs/parity.json` when it is added, including settings, wallpaper sources, the phone page, weather, news and Plex. The checker watches those folders and the build scripts as well as the screens.
- Every UI change updates `docs/parity.json`, with a status for each build. `python3 scripts/check-parity.py --open` lists what each build is missing. Run `git config core.hooksPath scripts/hooks` once to check at commit time.

## Rules for the repository

- Never commit `local/`, `android/local/`, `backups/`, `apks/`, `keys/`, `logos/dark/`, signing keys or generated webOS output.
- Never commit IP addresses, MAC addresses, Wi-Fi names, cast or device names, serial numbers, ADB mDNS names, home locations or tokens. Use `<tv-ip>` and similar placeholders.
- Keep `android/LICENSE` and upstream copyright notices unchanged.
- Add new third-party material to `CREDITS.md` with its license.
- Don't say something works until it has been checked on a real device.
- Update `devices/devices.json` and `docs/SUPPORTED_DEVICES.md` together.
