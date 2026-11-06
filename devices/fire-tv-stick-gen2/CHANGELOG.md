# Fire TV Stick (2nd gen) changelog

*A record of one TV's changes, not a guide. Read [safety.md](../../guides/safety.md) and [Before you start](../../README.md#before-you-start) before changing your own.*

**Device:** Fire TV Stick, 2nd generation (2016), model `AFTT`, product `full_tank`, 874 MB RAM, 256 MB swap
**OS:** Fire OS 5.2.9.5 (Android 5.1, SDK 22), build `LVY48F`
**Network:** Wi-Fi at <tv-ip>, ADB over the network on 5555 (turned on by the owner)
**Latest build tested:** 1.67-mh37-guard
**Rooted:** No

## 2026-11-06: Home, cleanup and a health check before and after

- Pre-flight: `scripts/01-preflight-backup.sh` saved `backups/<device>/` (two of its commands don't exist on Android 5). A restart with no changes came back in 70 s with ADB, so the stick was safe to work on.
- **Fire OS 5 refuses every way to switch off a built-in app from ADB.** `pm disable-user`, `pm disable` and `pm disable-until-used` answer "Permission Denial"; `pm hide` reports `hidden state: false`; `pm uninstall -k --user 0` fails with `DELETE_FAILED_INTERNAL_ERROR`. Switching off a component of a downloaded app is refused too. Root would get round it and was not used.
- **What does work: removing an app's downloaded update.** Amazon's own apps ship in `/system` as stubs or old versions and download their real version into user storage. `pm uninstall <package>` (no `--user`) removes that update, freeing its storage and leaving the stub, which is reversible from the Appstore or with the APK saved first. Done for nine unused apps (Silk, Amazon Live and its library, Amazon Photos, `hedwig`, Amazon Kids, the games hub, Special Offers `kso.blackbird`, `ceviche`), 143 MB in all; list in [debloat-revert.txt](debloat-revert.txt). After a restart none had come back. `fire-reapply.sh` removes an update again if the Appstore restores one.
- Removed at the owner's request: Downloader and Send Files to TV.
- **Why apps started by themselves:** eleven downloaded apps register for the boot broadcast and eight for every network change, mostly to feed Amazon's launcher rows (recommendations, continue watching) and Amazon's push messages; Amazon's launcher also stays loaded as the system's Home under ours. Their receivers can't be switched off here, but an app that has been force-stopped gets no boot or network broadcast until it is opened, and that state survives a restart. Netflix, Disney+, Hulu, Peacock, Paramount+, HBO Max, Plex, VLC and YouTube were force-stopped; each wakes normally again once it's used. TiviMate and Windscribe were left to start at boot (auto-start and VPN auto-connect).
- Home installed with `android/tools/install.sh` (guard build, FireKeeper). Fire OS 5 has no `cmd`, so the installer's notification-listener check misreports; the listener is set. `fire-reapply.sh` now picks lists by model, so the 4K stick's lists are never applied here, and reads Android 5's permission format. Home set to Efficient: still daily picture, clock screensaver (Home chose the clock over photos on this low-memory stick), no moving wallpaper.
- Privacy and speed settings from `fire-reapply.sh`: ad tracking limited, usage metrics off, animations at 0.5.

### Health, measured 3 minutes after a restart each time

| | Before | After |
|---|---:|---:|
| Memory available | 136 MB | 160 MB |
| Free RAM (Android's count) | 242 MB | 304 MB |
| Swap in use (of 256 MB) | 221 MB | 202 MB |
| Load average, 1 min / 5 min | 24.0 / 12.9 | 14.7 / 9.8 |
| CPU busy | 99% | 36% |
| Processes | 189 | 186 |
| User storage free | 2.4 GB | 2.7 GB |
| Downloaded apps | 13 | 12 (Home added, two removed) |
| Largest in memory | Netflix 95 MB (never opened), system 85, Amazon launcher 69 | Home 87 MB, system 84, compositor 39 |

At the end, Home was in front after every restart, Netflix and the other stopped apps stayed asleep, and no app crashed. Not checked by eye: playback, the remote's voice button and Alexa control from an Echo.

Undo: `TV=<tv-ip> android/tools/restore.sh --uninstall`; reinstall removed updates from the Appstore (or `adb install` the saved APKs); reinstall Downloader and Send Files from the Appstore.
