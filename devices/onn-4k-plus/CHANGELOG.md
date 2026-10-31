# Changelog — onn. 4K Plus Streaming (Google TV)

**Device:** onn. 4K Plus Streaming (device/product `coffey`), 2 GB RAM (MemTotal 1,971,788 kB), /data 11 GB (7.0 GB free at baseline)
**OS:** Android TV 14 (SDK 34), build URO4.260304.011.B1, fingerprint `onn/coffey/coffey:14/URO4.260304.011.B1/15051976`, patch 2026-03-01, bootloader locked
**IP:** <tv-ip> on Wi-Fi "<wifi-name>" (5 GHz). Cast name `onn.-4K-Plus-Streami…`, ADB mDNS name `<adb-name>-…`.
**ADB:** plaintext **5555** was already open (`service.adb.tcp.port=5555`), so no pairing code was needed. The TV only asked "Allow debugging?" once. 5555 survived both reboots.
**Rooted:** No

## 2026-10-29

### Connecting
- Found by mDNS (`_adb-tls-pairing._tcp`, `_googlecast._tcp`) → <tv-ip>.
- `adb pair` failed three times with `protocol fault (couldn't read status message)`, and `adb connect` returned `No route to host`, while `nc -z <tv-ip> 5555` succeeded. **Cause: the Mac's adb server (Homebrew adb 37.0.1), not the codes.** The running server had lost Local Network access. `adb kill-server && adb start-server`, then `adb connect <tv-ip>:5555` → "unauthorized" → owner tapped Allow (Always) on the TV → `device`. PASS.
- **Lesson:** on the Mac, if adb says "No route to host" but `nc` reaches the port, restart the adb server before retrying pairing.

### Phase 1: backup + reboot test (read-only)
- `scripts/01-preflight-backup.sh <tv-ip>:5555` → `backups//`. 0 packages disabled at baseline, so `packages-disabled.txt` is empty on purpose.
- Stock Home = `com.google.android.apps.tv.launcherx/.home.HomeActivity`. Home candidates: launcherx (priority 2), `setupwraith/.RecoveryActivity` (1), `tv.settings/.system.FallbackHome` (-1000). Same as the home box.
- Baseline: accessibility services `null`, screensaver_components `null` (stock default), HOME role = launcherx. No Aerial, Free My TV or XMB installed (no upstream `app.orchard.tv`, so there's no Home Guard conflict).
- No-change reboot (uptime was 14 days): back in ~57 s, ADB on 5555, Wi-Fi reconnected, Home = launcherx. PASS.

### Phase 2: build + install the fork "Home" (io.manish.home)
- Built `android` at `fork/v1-features` tip **da890d4** with `./gradlew assembleStandardRelease` (JDK 17). Gradle reported it up to date, matching the home box's build of the same commit.
- APK: `io.manish.home` versionCode 81, versionName 1.67-mh1, sha256 `df4d6c40…99d50`. Signer `CN=manishslal home launcher`, cert SHA-256 `3b544c34…c75f93`, which matches `local/home-release.jks`.
- From `android`:
  ```bash
  PKG=io.manish.home APK_FILE=app/build/outputs/apk/standard/release/app-standard-release.apk TV=<tv-ip>:5555 tools/install.sh
  adb -s <tv-ip>:5555 shell cmd package compile -m speed -f io.manish.home
  ```
  Installer output: screensaver set to the fork; "firmware still sends Home to launcherx; turning on Aerial Home Guard"; "Home Guard is running". No KEEP_ADB. **Nothing disabled or uninstalled.**
- Grants (from install.sh): fine location, WRITE_SECURE_SETTINGS, overlay, write settings, install packages, usage stats, notification listener `RevivalListener`.
- Compile: `dumpsys package dexopt` reports `status=speed-profile` (reason cmdline), not `speed`. The firmware compiles "speed" as speed-profile, as the installer's comment warns. Not a problem; noted.

### Phase 3: verification on the device
| Check | Before reboot | After real reboot (uptime 1 min) |
|---|---|---|
| Fork is Home (Home key from Settings → `io.manish.home/app.orchard.tv.MainActivity`) | PASS | PASS (fork in front after boot, and again after Home key from Settings) |
| Home Guard bound (`label=Aerial Home Guard`); HOME role still on launcherx | PASS | PASS |
| Wi-Fi back on <wifi-name> | PASS | PASS |
| Screensaver = `io.manish.home/app.orchard.tv.dream.AerialDream` | PASS | PASS: started it for real (see below) |
| Phone setup page: port 8791 listening, answers on the LAN (`HTTP 404` without its token, same as home) | PASS | PASS |
| Disabled packages | 0 | 0 |

- Home screen on screen: 12-hour clock, weather, Wi-Fi/VPN/controls icons, dark tiles (Netflix, YouTube, Apple TV, Disney+, Prime Video, Spotify). Screenshot saved in this session only.
- Screensaver: `cmd dreams` needs root on this build and Somnambulator doesn't exist, so the screensaver delay (`system screen_off_timeout`) was set to 20 s, then **put back to 600000 (10 min)**. `dumpsys dreams` showed `mCurrentDream = io.manish.home/app.orchard.tv.dream.AerialDream`, not preview mode. On screen: clock, place caption (Kunětická hora), and the fork's news headline at the bottom right.
- Not verified: the phone-page QR shown on screen (only the port was checked); Plex linking (owner does plex.tv/link); video-style screensaver (video layers aren't in screencaps).

### Device-specific fixes needed
- None found. The fork behaves the same as on the onn. 4K Streaming Box. No app code changed in this session.

### Cleanup (2026-10-29 ~16:43, owner asked for the Onn 4K cleanup)
- `adb shell pm trim-caches 8G`: free space on /data went from **7.46 GB to 8.10 GB** (~0.6 GB, df shows 7.1 → 7.7 G). After a real reboot it's still **7.7 G**. PASS.
- **Built-in bloat not disabled or uninstalled, on purpose.** The home box's cleanup showed the onn re-enables disabled system apps at boot (play.games, feedback, dreams.basic, dreamx all came back). Measured here: play.games 18 MB, youtube.tvmusic 5 MB, dreamx 34 MB + 14 MB data, feedback and dreams.basic ~0. About 70 MB in total, so not worth the risk.
- After the reboot: fork in front, Home Guard bound, screensaver = fork's AerialDream, Wi-Fi back, 0 disabled packages. PASS.

## Undo
Cache trim: nothing to undo (apps rebuild caches as needed).

From `android`:
```bash
PKG=io.manish.home TV=<tv-ip>:5555 tools/restore.sh
```
This revokes grants, removes Home Guard and the listener, and puts back the stock screensaver and HOME role. Then optionally `adb -s <tv-ip>:5555 shell pm uninstall io.manish.home`.
Baseline for comparison: `backups//` (screensaver_components and enabled_accessibility_services were `null`).
Last resort: `scripts/99-restore-stock.sh <tv-ip>:5555`.

## Notes
- ADB 5555 stays open in plaintext on this box (it already was before this session). Turn it off in Developer options when you no longer need it. The fork wasn't given KEEP_ADB.

### Fork look pass 2 (2026-10-29 ~16:50, from branch `fork/look-2`)
- Installed over the fork (same key), `compile -m speed`.
- Verified on screen: flap-card clock at rest (after spacing fix) and turning 4:49 → 4:50 (screenrecord frames); light frosted Control Center with dark type and a dark focus plate (Appearance here is Light); Control Center footer.
- Not checked on screen: Dark glow, Auto switching at sunset, launch wave, screensaver clock cards, the scrolling Screensaver row. The owner was using Personalize on the TV at the time, so remote input stopped.

### Board clock + location picker (2026-10-29 ~17:30, `fork/v1-features`)
- Installed over the fork, `compile -m speed`.
- Verified on screen: board clock (even Barlow Condensed cards, colon between, PM on small cards, THU 29 OCT and weather as board lines); flip 5:26 → 5:27 in screenrecord frames, PM steady while the card turns.
- Not checked on screen: the Location picker itself (needs the remote and keyboard) and the flap sound (off by default).
