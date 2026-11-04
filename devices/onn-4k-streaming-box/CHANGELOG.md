# Changelog — onn. 4K Streaming Box (Google TV)

**Device:** onn. 4K Streaming Box (`YOC`, product `onn_4k_gtv`), 2 GB RAM (MemTotal 1,971,588 kB)
**OS:** Android TV 14 (SDK 34), build URO4.260304.011.B1, patch 2026-03-01, bootloader locked
**IP:** <tv-ip>. ADB: wireless debugging (pairing) on first connect. After reboot, ADB is reachable on **5555** (the wireless-debugging port changes each boot).
**Latest build tested:** 1.67-mh17
**Rooted:** No

## Home mh37 — 2026-11-04

- `1.67-mh37` installed over mh35 and compiled with `-m speed` while YouTube played; YouTube was not interrupted. Sounds are Home's own. Not looked at on screen.
- Later mh37 build: the cut-out service answers here too (fish: solid 0.95). Put back on the daily photo, Large at the top left. Not looked at on screen.
- Review-fix build from 20d82c3 installed and compiled with `-m speed`. Checked on screen: Home, Dark with Clear tiles, the Large clock at the top left and the daily photo. The collections loaded (NASA 6, Wikimedia 5, Earth View 6), and the cut-out service answered: today's Bing photo is haze (solid 0.42), so no subject over the clock. 0 crash entries.

## Home mh35 — 2026-11-04

- `1.67-mh35` installed and compiled with `-m speed` while the TV slept. Not looked at on screen: smaller pill, Turn Off TV row (Home Guard is bound here), Aerial categories.

## 2026-10-28
### Phase 1: backup + reboot test (read-only)
- Backup: `backups//`. 0 packages disabled at baseline. Stock Home = `com.google.android.apps.tv.launcherx/.home.HomeActivity`.
- Home candidates at baseline: launcherx (priority 2), `setupwraith/.RecoveryActivity` (priority 1), `tv.settings/.system.FallbackHome`.
- Rebooted with no changes (uptime was ~18 days). Came back in under 2 minutes and ADB reconnected on 5555. PASS.

### Phase 2: install XMB TV Home
- Prebuilt `xmb-launcher.apk` (targetSdk 30) was blocked by **Google Play Protect** ("built for an older version of Android"). The user tapped "Install anyway", and it ran fine.
- Rebuilt from source (`sanangoyal/XBM-TV-Home` @ 4a43965) with targetSdk 34. Two-line patch to `android/build.sh` + `AndroidManifest.xml`. The build needs JDK 17: JDK 23 crashes D8 8.2.2. Signed with a local debug key, `kit/keys/xmb-dbg.jks` (keep it: future updates must use the same key).
- Replaced the prebuilt (uninstall, then install). Different signature, so an in-place update is impossible. `apks/xmb-launcher-sdk34.apk`, sha256 `fef8e1cf…747b5`. It installed without a Play Protect prompt.
- **No extra permissions granted** (no notification listener, usage stats or all-files storage). Agent/MCP server left off.
- Launches and renders the crossbar (Video/Music/Files/Sources, etc.). No crashes in logcat.

### Phase 3: make it Home (partial)
- `cmd package set-home-activity com.sanan.xmb/.MainActivity` returns Success, but `resolve-activity` still picks launcherx. Google TV's system launcher outranks it. **The Home button still opens Google TV.**
- Nothing is disabled. Stock is untouched.

### Phase 2b: install Free My TV (user didn't like the XMB look; XMB kept as a customizable backup)
- Source reviewed (`simbasjungles/free-my-tv` @ fa47183). `HomeMode.java` explicitly lists this box ("onn 4K Streaming Box, Google TV 14") and protects `com.onn.*`, `com.gretzky.*` and `com.droidlogic.*` (remote pairing, GlobalKey buttons, settings, mic).
- Installed the release APK v1.2 (`io.freemytv`, targetSdk 33). Hash verified. Home preview opened and rendered fine (clock, weather, crypto, app shelf).
- **Not switched yet.** Its "Home Screen → Free My TV" switch uses the box's own ADB (127.0.0.1:5555, approved on the TV once) to `pm disable-user`: launcherx, setupwraith, the stock screensaver (dreamx), Play Games, YouTube Music. "Original" re-enables them, and it auto-reverts after 3 crashes in 5 min.
- The release APK checks weekly for a signed update from the repo's releases and asks before installing.

- First switch attempt was refused by Free My TV: the saved Wi-Fi networks belonged to the setup wizard (setupwraith), so disabling it would erase them. With user approval, forgot the out-of-range networks over ADB (`cmd wifi forget-network 1 2 3`: <wifi-name>, <wifi-name>, <wifi-name>). The user forgets and rejoins <wifi-name> on the TV.

- The user forgot and rejoined <wifi-name> in Settings. The network owner changed from `setupwraith` to `android.uid.system` (net ID 4).
- The user flipped Home Screen → Free My TV and allowed the app's on-device USB debugging. **Switch succeeded.** Disabled (`pm disable-user`): `launcherx`, `setupwraith`, `dreamx`, `play.games`, `youtube.tvmusic`. Home = `io.freemytv/.HomeActivity`. Screensaver = `io.freemytv/.Screensaver`.
- Settings still opens. Reboot test: back in ~60 s, Home = Free My TV, 5 packages still disabled, Wi-Fi reconnected. PASS.

### Undo
- On the TV: Free My TV → Home Screen → **Original** (re-enables all of the above).
- If the app is broken: `scripts/99-restore-stock.sh <tv-ip>:5555`, then `adb shell pm enable` for dreamx, play.games and youtube.tvmusic.

### Phase 2c: Aerial (preview only)
- Installed `app.orchard.tv` v1.67 (targetSdk 34, hash verified). **No grants** and not Home. It opens and shows aerial footage, a clock, weather and an app shelf, plus its own "Make Aerial Your Home Screen" panel.
- Its full installer (`tools/install.sh`) would grant WRITE_SECURE_SETTINGS, usage stats, overlay, install-packages and location; enable an accessibility "Home Guard"; add a notification listener; keep ADB on; and take the HOME role. `tools/restore.sh` undoes it. Not run.
- Conflict: Free My TV's boot check keeps Free My TV as Home, so running Aerial as Home means setting Free My TV to Original first.

### Phase 3c: Aerial as Home (2026-10-28 ~17:00)
- The user set Free My TV → Original. Verified 0 disabled packages and Home = launcherx.
- Ran `TV=<tv-ip>:5555 APK_FILE=apks/aerial-standard.apk tools/install.sh` from `repos/aerial-tv` (no KEEP_ADB). The installer:
  - granted WRITE_SECURE_SETTINGS, location, overlay, write settings, install packages and usage stats
  - turned on the notification listener (RevivalListener)
  - set the screensaver to `AerialDream`
  - firmware kept Home on launcherx, so it enabled the **Home Guard** accessibility service
- **Nothing disabled.** Google TV's launcher is still enabled; Home Guard intercepts the Home key.
- Home key → Aerial. Reboot test: back in ~70 s, Wi-Fi OK, Home Guard still enabled, Aerial in front after boot and on Home. PASS.
- Free My TV is still installed (Original mode). XMB is still installed.
- **Undo:** `TV=<tv-ip>:5555 bash repos/aerial-tv/tools/restore.sh` (revokes grants, removes Home Guard, restores the screensaver and HOME role).

### Cleanup (2026-10-28 ~18:17)
- Backup: `backups//`.
- `pm trim-caches 8G`: free space went from **1.50 GB to 2.09 GB**. After a reboot: **2.13 GB**. This persists.
- Disabled play.games, feedback, dreams.basic and dreamx (`backups/`). **All 4 were re-enabled at the next boot** (`enabled=1`, an explicit enable, not a default reset). Neither Free My TV (Original mode exits early) nor Aerial re-enables arbitrary packages, so the Onn system does it. Free My TV's disables only "persisted" earlier because its boot check re-applies them. **Lesson: on this Onn, a disable only sticks if something re-applies it at boot.** Disabling system apps frees almost no space anyway (APKs live on /system).
- Side effect: disabling dreamx reset the screensaver to dreamx. Restored `screensaver_components` to `app.orchard.tv/.dream.AerialDream`.

### Fork "Home" (io.manish.home, v1.67-mh1) installed beside Aerial (2026-10-28 ~18:40)
- Built from `android` branch `fork/v1-features` and signed with `android/local/home-release.jks`. `cmd package compile -m speed` run.
- Verified on screen: 12-hour clock ("6:39 PM"); °F ("61°"); dark tiles across the grid (Disney+ needed two fixes plus a tile-cache bump); Google News headline over the screensaver, bottom right, clear of the place caption.
- Not yet verified: Plex linking and playback (needs the owner's plex.tv/link step and the Drone libraries); a reboot test with the fork as Home.
- Home is still upstream Aerial (Home Guard). The fork is opened from the app grid.

### Fork: Plex linked, phone page fixed (2026-10-28 ~20:30)
- First link showed "No Plex server answered": the app blocks cleartext, so `http://<tv-ip>:32400` was refused. Now uses the server's HTTPS `*.plex.direct` address, which resolves to <tv-ip> on the Onn. Relinked: libraries listed, **Drone Photos** chosen.
- "Set up from a phone or computer" showed the offline icon: upstream Aerial (uid 10039) already listens on 8790. The fork now uses **8791** (verified listening, uid 10064; answers 404 without its token, same as upstream).
- The video-library row overflowed (7 libraries) and hid its label and the chosen library: those rows now scroll.
- **Blocker for testing the fork:** upstream's Home Guard treats `io.manish.home` as a stock home and pulls Aerial back each time it opens (`AerialGuard: stock home io.manish.home came to the front; bringing Aerial back`). Fix = move Home to the fork (upstream `tools/restore.sh`, then the fork's installer). Not done yet; waiting for the owner.
- Not yet verified: drone videos/photos playing in the fork's screensaver.

### Home switched to the fork (2026-10-28 ~21:12, owner approved)
- Snapshot: `backups//` (accessibility, screensaver, listeners, HOME role = app.orchard.tv).
- `TV=… tools/restore.sh` (upstream, kept installed) → Home back to launcherx, guard off, screensaver dreamx.
- `PKG=io.manish.home APK_FILE=… TV=… tools/install.sh` (scripts now take PKG; components use the `app.orchard.tv` class namespace). Google TV kept HOME on launcherx, so the fork's **Home Guard** took over. Screensaver = `io.manish.home/app.orchard.tv.dream.AerialDream`; RevivalListener on. No KEEP_ADB.
- Reboot test: real reboot (uptime 64 s at check), fork in front, guard bound, Wi-Fi back on <wifi-name>. PASS.
- Phone setup page: QR + `<tv-ip>:8791/<token>/` shown; answers on the LAN.
- Plex: My Videos lists the NAS clips . Screensaver style set to **Videos**: 38 Plex films, `c2.amlogic.hevc.decoder` at 3840×2160 60 fps, first frames logged for 7 clips in a minute, no player errors. Picture not checked by eye (the video layer is not in screencaps).
- **Undo:** `PKG=io.manish.home TV=<tv-ip>:5555 tools/restore.sh` (from `android`), then optionally `TV=… APK_FILE=../apks/aerial-standard.apk tools/install.sh` to give Home back to upstream Aerial.

### Fork: look pass (2026-10-28 ~23:45)
- Built from `fork/v1-features`, installed over the fork (same key), `compile -m speed`.
- Verified on screen: shade behind the pill in the scrolled grid; single-line focus frame; large clock flips 43→44 (screenrecord frames) and sits in place at rest after a baseline fix; no edge sound (code path removed).
- Dark tiles checked with `export_tiles … --es tiles_look dark`: Aerial, HBO Max, Free My TV, VLC now seamless; Peacock keeps a faint edge from its own art. The owner's Appearance is still Light (not changed).
- Not checked on screen: Dark's wallpaper dim (needs Appearance → Dark).

## 2026-10-30
### Home 1.67-mh4 installed
- Box moved to another network. Replaced 1.67-mh1.  uncommitted, versionCode 84, APK sha256 `975aa589c422…`, with the logo pack, `compile -m speed`.
- Checked on the device with simulated keys: Control Center, Personalize list, Appearance, Wallpaper, Layout and Tiles, Clock and Weather, Screensaver, its Style list, Back returning focus to the row. No crash logs.
- Testing mishaps, all put back: an early key opened Netflix; test keys changed Appearance and Tile Style. Appearance is Light and Tile Style is Full Color again.
- Not checked: the real remote, Hidden Apps, Headlines, Plex, Passcode, Phone. Later cards in the Wallpaper grid had blank thumbnails about 1.5 s after opening; not known whether they fill in.
- Undo: no quick undo; mh1 and mh3 APKs were not kept. Stock Home: see "Undo (current state)" and `scripts/99-restore-stock.sh`.

### Old apps removed (user asked)
- `adb uninstall`, both reported Success: `app.orchard.tv` (Aerial 1.67) and `com.sanan.xmb` (XMB 1.0). Their app data went with them. Neither held Home Guard, the screensaver or the Home role; those stay on `io.manish.home`.
- Undo: reinstall from the APKs. Their earlier settings are not recoverable.

### Home 1.67-mh5 installed
- versionCode 85, APK sha256 `40a558875322…`, uncommitted. Build and unit tests passed.
- Tile Style moved to Appearance with System, Dark Glass and Full Color. Clock cards follow Mode (white with black type in Light). Real Liquid Glass on the dock, Control Center's round buttons and sliders, and the status pill's focus; lit rims on switches and chips. Light glass strengthened at the rim.
- Checked on screen in Dark and Light with simulated keys. Not checked with the real remote.
- Frames over one test run with panels and a Mode change, right after install: 202 frames, 39.6% janky, 50th 38 ms, 90th 109 ms. No baseline, so this does not show what the glass costs. Memory 156 MB PSS.
- Testing mishaps: keys opened TiviMate once, and the keys after it went there. Back starts the screensaver here. Mode (Auto), Tile Style (Dark Glass) and Focus Style (Standard) are as the user set them.

### Home 1.67-mh8 installed
- versionCode 88, APK sha256 `b357301bbdbf…`, uncommitted. Build and unit tests passed. Adds Home Mode and the design standard; see `android/docs/PERFORMANCE.md` and `android/DESIGN.md`.
- Mode tested by remote keys: Custom to Efficient and back restored every setting. Full, Efficient and Custom measured three times each. The Efficient and Custom numbers are from mh7; mh8 only moved screen reader code.
- Testing mishaps: test keys opened YouTube once and TiviMate once. No settings changed; left on Custom.
- Screen reader names read correctly in the accessibility tree on tiles, the status pill, Control Center buttons and the Home Mode row. Not heard through TalkBack.
- Control Center showed "Not the default home screen" once just after a test force-stopped Home; it read Home Guard on again afterward.

## Notes
- XMB's "Sources" row lists HDMI 1–3/AV/USB/Live TV. That's TV-set UI and meaningless on a streaming box. Hide it later via fork.
- XMB's own `restore-launcher.sh` targets the wrong stock package for Google TV. Use `scripts/99-restore-stock.sh`.
- Trending artwork needs a TMDB key in `/sdcard/xmb-tmdb.txt`, which XMB can only read if granted storage access.

## Undo (current state)
```bash
adb -s <tv-ip>:5555 shell settings put secure enabled_accessibility_services null
adb -s <tv-ip>:5555 uninstall io.manish.home
```
Then use `scripts/99-restore-stock.sh` if anything still points away from the stock launcher.

## 1.67-mh10 (2026-10-31)
- Installed from `android` with `tools/install.sh`. Settings carried over: mode Custom, Home Guard bound, Home in front.
- Exported Dark and Full Color tile sheets: logos match in size and position on all 20 tiles.
- Home idle in Custom: 126, 130 and 134 MB across three runs.
- Not checked yet: Clock page on screen with a real remote.
- Undo: reinstall an earlier build. No settings changed.

## Stock launcher test (2026-10-31)
- Disabled `launcherx` and read the resolver without pressing Home: it chose `setupwraith/.RecoveryActivity`, priority 1, over Home at priority 0.
- Re-enabled `launcherx` at once. The recovery screen never opened and Home stayed in front.

## 1.67-mh11 (2026-10-31)
- Installed. Clear tiles checked on screen in Light and Dark, and Full Color on the exported sheet. No crashes logged.
- Left on Clear tiles for review. Not checked with a real remote.

## 1.67-mh12 (2026-10-31)
- Installed. Clear logos are one ink and one shade. Checked on the exported sheet and Home screen. Folders and input tiles were not checked because this box has none.

## Stock launcher disabled (2026-10-31; reboot verified)
- Shell cannot disable only `setupwraith/.RecoveryActivity`: SecurityException.
- Wi-Fi saved by the system (`cuid=1000`), not the wizard, so the wizard package is safe to disable.
- Disabled `launcherx`, `setupwraith` and `io.freemytv` with `pm disable-user --user 0`, then `set-home-activity`.
- Two earlier tries came back after a reboot, because the reboot came within seconds. Waiting 40 s first made it stick.
- After reboot: Home is the default, Wi-Fi connected, Home Guard bound, Settings opens, Home key works.
- At 2 minutes after boot, launcher and wizard processes are gone: 79 MB of app memory in the baseline. Their share grew to 141 MB after long uptime.
- Total Used RAM at 2 minutes barely moved, because Android fills free memory with cache.
- Undo: `scripts/99-restore-stock.sh <tv-ip>` and `adb shell pm enable io.freemytv`.

## 1.67-mh16 (2026-10-31)
- Installed. Plex and Peacock colors, VLC cone, Netflix red and smoother edges checked on exported sheets. Clear checked on the Home grid.

## 1.67-mh17 (2026-10-31)
- Installed. Every tile style, Invert, Focus Glow, Wallpaper Tint and Control Center captured in Light and Dark, with independent QA on all but the last panel text change.
- 73 unit tests pass and no crashes are logged. Left on Clear tiles, Light, Liquid glass, as before.

### Home 1.67-mh31 installed (2026-11-03)
- Updated from 1.67-mh17 with `adb install -r` of the standard release APK, then `cmd package compile -m speed -f io.manish.home`. Home Guard stayed enabled and Home was in front.
- Control Center shows Wi-Fi, Bluetooth, Settings and Appearance. No VPN button, because no VPN has been seen up on this box, and no Inputs or Brightness, as on any box. Bluetooth opened Google TV's Connected Devices page (`com.android.tv.settings.CONNECTED_DEVICES`), listing the remote, and Back returned to Home.
- Android 14 draws Liquid Glass with the lens shader. The video wallpaper plays on a SurfaceView that `screencap` shows as black, so screenshots of Home look black behind the glass.
- Undo: install the earlier APK the same way.

### Home 1.67-mh32 installed (2026-11-03)
- After the owner found the glass flicking between liquid and flat, and the moving wallpaper stopping dead, when Control Center opened. Installed while TiviMate was playing; it kept playing. Not yet looked at: the owner was using the box.

### Home 1.67-mh34 installed (2026-11-03)
- Recorded Control Center opening over the moving Aerial wallpaper: the panel came in about 0.5 s after OK and its glass picture faded in over about 0.8 s, with no skipped frames. The first opening after an install took a second (42 frames skipped while it was composed for the first time). The video plane doesn't show in `screencap` or `screenrecord`, so the film's slow stop was checked by its log only.
- Focus glow checked: Windscribe blue, HBO Max purple, Apple TV white, TiviMate blue. The status pill shows the Wi-Fi name.
- Left on Daily Photo, as before the test, and on Home.

