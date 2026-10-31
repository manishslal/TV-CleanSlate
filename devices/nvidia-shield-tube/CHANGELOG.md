# NVIDIA SHIELD changelog

**Device:** NVIDIA SHIELD Android TV (`sif`, the 2019 tube), Tegra, 32-bit userland (armeabi-v7a), 2 GB RAM (MemTotal 1,987,624 kB), /data 5 GB (2.2 GB free), 4K output with a 1920x1080 UI at 320 dpi
**OS:** Android TV 11 (SDK 30), SHIELD Experience 9.2.4 (33.2.0.400), build `RQ1A.210105.003.7825199_4387.0822`, patch 2026-01-05, verified boot green
**Network:** Wi-Fi at <tv-ip>, MAC <mac>, on the LG's HDMI 2
**ADB:** 5555, turned on by the user in Developer options. The user accepted the prompt on the TV.
**Rooted:** No

## Baseline (2026-10-30, read-only)
- `scripts/01-preflight-backup.sh <tv-ip>` saved `backups//`. Its `stock-launcher.txt` is empty because the script looks for `launcherx`, and this device's stock launcher is `tvlauncher`.
- Stock Home: `com.google.android.tvlauncher/.MainActivity` (priority 2), the same launcher as the TCL, not the Onn's `launcherx`. The other candidate is `com.android.tv.settings/.system.FallbackHome` (-1000).
- `com.google.android.tungsten.setupwraith` is present, but unlike the Onn it is not a Home candidate.
- Already disabled: `com.android.gallery3d`, `com.google.android.tv`, `com.nvidia.benchmarkblocker`, `com.nvidia.tegrazone3`.
- `enabled_accessibility_services` null, screensaver `com.google.android.backdrop/.Backdrop`, screen off after 300000 ms, uptime 3 days.
- No-change reboot: back in 73 s with ADB on 5555, Wi-Fi at the same address, `tvlauncher` as Home, and the same four packages disabled. PASS.

## Changes
Each entry gives what changed and how to undo it.

### 1. Home 1.67-mh3 installed (2026-10-30; reboot verified)
- versionCode 83, standard flavor, release build, APK sha256 `035fa85e7897…`, with the logo pack.
- Keeps Android's design. Added from the LG work: Personalize in a right-side panel, logo pack on Dark Glass tiles, and App Tiles separate from Appearance.
- `PKG=io.manish.home APK_FILE=app/build/outputs/apk/standard/release/app-standard-release.apk TV=<tv-ip>:5555 tools/install.sh`, then `cmd package compile -m speed -f io.manish.home`.
- Set by the installer: Home Guard in `enabled_accessibility_services`, screensaver `io.manish.home/app.orchard.tv.dream.AerialDream`, plus the usual grants (fine location, WRITE_SECURE_SETTINGS, overlay, write settings, install packages, usage stats, notification listener).
- Android still resolves Home to `tvlauncher`, as on the TCL, so Home Guard brings Home back until the stock launcher is disabled.
- Checked: Home key from another app returned to Home; a reboot took 85 s and landed on Home with Home Guard bound, Wi-Fi at the same address and the same 4 packages disabled. The exported tile sheet shows all 10 pack logos on the installed apps.
- Memory: Home about 250 to 280 MB PSS, about 765 MB available after boot.
- Undo: `adb shell settings put secure enabled_accessibility_services null`, `adb shell settings put secure screensaver_components com.google.android.backdrop/.Backdrop`, then `adb uninstall io.manish.home`. Stock Home takes over at once.

### 2. Cleanup (2026-10-30; user approved the list; reboot verified)
All with `pm disable-user --user 0`. Undo one with `adb shell pm enable <package>`, or all with `nvidia-shield-tube/restore-shield-cleanup.sh`. The script has not been run.

| Batch | Package | What | Checked after |
|---|---|---|---|
| 1 | `com.google.android.tvlauncher` | Stock launcher. Home is now Android's real default | Home key, then reboot (129 s) |
| 2 | `com.google.android.tvrecommendations` | Stock launcher's recommendation rows | Home key |
| 2 | `com.nvidia.stats` | NVIDIA usage statistics | Home key |
| 2 | `com.nvidia.feedback` | NVIDIA feedback | Home key |
| 2 | `com.google.android.feedback` | Google feedback | Home key |
| 3 | `com.nvidia.shieldbeta` | SHIELD Beta app | Home key |
| 3 | `com.nvidia.shield.beta` | SHIELD Beta stub | Home key |
| 3 | `com.nvidia.developerwidget` | Developer widget | Home key |
| 3 | `com.android.printspooler` | Printing | Home key |
| 3 | `com.google.android.videos` | Play Movies & TV | Home key |

- Final reboot: Home in front and Android's default, Home Guard bound, Wi-Fi at the same address, Bluetooth on, NVIDIA SHIELD Remote connected, Settings opens, 14 packages disabled (10 here plus the 4 from baseline), no crashes logged.
- Available memory right after that boot: about 630 MB. It was about 594 MB at baseline after 3 days of uptime, so this is not a direct comparison.
- Boot times varied (35, 73, 85, 129 s by when ADB answered) with no pattern from the changes.
- Left alone: remote, Bluetooth, keyboards, Wi-Fi, HDMI, audio, updates, Assistant, Cast, SHIELD file sharing, `setupwraith`, Input Viewer and System upgrade.
- TiviMate 5.3.3 was already installed; nothing changed. Its backups are in `/sdcard/Download`.
- Network ADB stays on for now: Developer options → Network debugging. It is plaintext, so turn it off when finished.

### 3. Home 1.67-mh4 installed (2026-10-30; not yet checked on screen)
- versionCode 84, standard flavor, release build, APK sha256 `b61bba54a092…`. Build and unit tests passed. Lint not rerun.
- Personalize regrouped into Look, Home Screen, and Apps and Access, with each row showing its current value. Weather merged into Clock and Weather; tile settings moved to Layout and Tiles.
- Each setting is one line and one focus stop: switches, Left and Right values, and lists for more than four choices or open-ended ones. Notes sit at the panel's foot at higher contrast.
- Text scale 12/14/16/20/26. Wallpaper is a two-column grid. Hidden Apps is a page inside the panel. Home shows "More Apps" above the grid. The tile menu has three groups.
- Installed while the SHIELD was asleep. It was not woken, because HDMI control is on and another LG input was the active source, so waking it could switch the TV.
- Later mh4 build `975aa589c422…` (on the Onn) right-aligns row values; this SHIELD still has the first build and needs it.
- Undo: no quick undo. mh3 was never committed and its APK was not kept, so going back means reversing these edits in the source. No preference keys changed, so settings carry over.

## Health check (2026-10-30, 24 minutes after reboot, read-only)
- Home in front and default, Home Guard bound, 14 packages disabled, 1.67-mh3, patch 2026-01-05, 2.0 GB storage free.
- Memory: about 670 MB available, 190 MB of 512 MB swap used, Home about 218 MB. `lowmemorykiller` logged warnings at 16:27 while reclaiming background apps; no crash logs.
- Wi-Fi: "<wifi-name>", 5 GHz, -42 dBm at last poll. Router 3 ms, internet 7 ms, no loss.
- Temperatures: CPU 73 °C, GPU 71 °C, thermal status 0 (no throttling).
- TiviMate used about one core out of four in a background job with Home in front, for at least 20 seconds of sampling. Likely a playlist or guide refresh, not confirmed.
- System log: repeated `system_server_wtf` entries from 16:18 to 16:21, mostly JobScheduler "Can't cancel all jobs for system uid", plus firmware noise. The timing overlaps the cleanup. No visible effect.
