# Onn 4K — Launcher Rotation Plan (prepared 2026-10-28, nothing run on the TV yet)

Companion to [launcher-logistics.md](launcher-logistics.md). 

## What I need from you
1. **Which Onn?** The 2021 4K Streaming Box (2 GB RAM, 8 GB storage, Android TV 10→12 Google TV) or an Onn 4K Pro (2022/2024/2026; 3 GB RAM, Ethernet, 32 GB)? Settings → System → About shows the model. The steps differ for pairing and recovery.
2. TV IP address (Settings → Network).
3. A phone remote app or a USB mouse as a spare input, in case a launcher breaks navigation.

## Candidate launchers
| Name | Look | Stars / license | Package | Prebuilt APK | Verdict |
|---|---|---|---|---|---|
| `xmb` — XMB TV Home | PS3 crossbar, wave video | 8 / MIT | `com.sanan.xmb` | yes (hashed) | **First.** Has the most tooling. |
| `psp-waves` — xmb-home | PS3 crossbar + PSP waves, no permissions | 2 / MIT | `com.oso.xmbhome` | no, build needed | **Your PSP option.** Needs JDK 17 + Android SDK 34. We have JDK 23 and no SDK. |
| `tvhome` — linuxliam/tv-home | XMB with themes, live TV, multiview | 0 / no license stated | `com.liam.homescreen` | yes, not downloaded | Hisense-specific code in it. Review before use. |
| `aerial` — aerial-tv | Apple aerial wallpaper and screensaver | ~7.4k / MIT | `app.orchard.tv` | yes (hashed) | Uses accessibility service + overlay permission. |
| `freemytv` — free-my-tv | App shelf, trending picks | ~5.2k / MIT | `io.freemytv` | yes (hashed) | Has "Original" revert and crash auto-revert. |

No Android SDK exists on this Mac, so building anything (including the PSP one) needs `brew install openjdk@17` plus the SDK command-line tools. I haven't installed those.

## Rotating launchers
All of them can be installed side by side. `03-switch-launcher.sh <tv> <name|stock>` sets which one owns Home using `cmd package set-home-activity`. Stock stays installed and enabled, so switching is instant and reversible.
Known catch: on Google TV the Home button can snap back to the stock launcher after a reboot, and `setupwraith` can reclaim Home. If that happens, phase 4 below (disabling stock) is the real fix, and then switching works only between the custom launchers plus `stock` via `99-restore-stock.sh`.

## Phases, with gates
**Phase 0, on the TV (you):** developer options → USB debugging, and wireless debugging if offered. On Android 12+ you pair with a code: `adb pair <ip>:<pair-port>`, then `adb connect <ip>:<port>`.
**Phase 1, read-only:** `scripts/01-preflight-backup.sh <tv>`. Gate: all backup files non-empty and `props.txt` shows the model. Then do one reboot with no changes and confirm ADB reconnects.
**Phase 2, install only:** `scripts/02-install-launchers.sh <tv> xmb`. Verifies hashes first. Open it from the app row, test Back and Home. Grants nothing.
**Phase 3, make it Home:** `scripts/03-switch-launcher.sh <tv> xmb`, reboot, check it still owns Home. Switch to `stock` and back once to prove rotation works.
**Phase 4, only if Home keeps reverting:** disable `launcherx` and `setupwraith` one at a time with a reboot after each, and confirm Inputs menu, Wi-Fi and remote still work.
**Phase 5, debloat (optional, later):** model-specific discovery on the Onn itself, batches of 5, restore script updated before each batch. The XMB repo's bundled list is for a OnePlus TV and must not be used.
**Phase 6:** add `psp-waves` (after building), then `aerial` and `freemytv`, one at a time. Save the changelog and a config bundle for cloning to another Onn.

## Problems found in the repos (so we don't repeat them)
- XMB's own `restore-launcher.sh` re-enables `com.google.android.tvlauncher`. On Google TV the stock launcher is `com.google.android.apps.tv.launcherx`, so its restore would not bring Home back. Our `99-restore-stock.sh` uses the right package.
- XMB's `install.sh` also grants notification-listener, usage-stats and all-files storage access. Our install step grants nothing. Grant individually if a feature needs it.
- XMB's trending artwork needs a TMDB key and the repo reports one tested TV. Treat it as lightly tested.
- aerial-tv wants ADB to stay on, and it uses an accessibility service. That conflicts with the "close network ADB" rule, so we decide per launcher.
- free-my-tv and aerial-tv are big, active repos (256 open issues on aerial). Pinned commits are in `repos/`.
- I did not read the full source of any launcher yet. APK hashes prove the files match what I downloaded, not that they are safe. Reading XMB's code is the next step before Phase 2.

## Other TV OS (for later, in memory)
LG webOS and Samsung Tizen cannot swap Home. Only free-my-tv supports them (`FreeMyTV-webos.ipk`, `FreeMyTV-tizen.wgt`), via Developer Mode. Details are in the memory notes `tv-os-capabilities` and `tv-launcher-repos-shared`.
