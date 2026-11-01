# Changelog — Fire TV Stick 4K (1st Gen)

**Device:** Fire TV Stick 4K (1st Gen), model `AFTMM` (codename "mantis")
**Fire OS:** 6.7.1.1 (NS6711/5908), Android 7.1.2, SDK 25, 32-bit (`armeabi-v7a`)
**RAM:** 1.3 GB | **Rooted:** No | **Debloat:** Yes (20 disabled, 10 background-blocked)

---

## Home mh23 — 2026-11-01

- `1.67-mh23-guard` installed through `tools/install.sh guard`.
- The menu shows "Home Guard is on": `FireKeeper` now counts as guarded.
- Screensaver: `AerialDream` is the system screensaver. Home's own started at 5 idle minutes on Home, and the dream started at 5 idle minutes in Settings. `FireKeeper` restores it if Fire OS changes it.
- Update recovery: `tools/fire-reapply.sh` checks and restores permissions, listener, screensaver, privacy settings, animation scales, `debloat-disable.txt` and `debloat-background.txt`. Simulated drift was found and restored. Home's menu shows a reminder when the Fire OS build differs from the last run.
- Update packages stay untouched, so updates are not blocked.
- Logos: Downloader, Send Files and Appstore have new marks. VLC, Disney+, Paramount+ and the Windscribe mark use `size`. `tools/logo-audit.sh` reports every launchable app covered.
- 18 launches across 16 apps returned to Home within 4 s by ADB key events, with 0 crash entries. One earlier run showed `com.amazon.stillwatching.activity` 6 s after Home from Netflix; 3 repeats did not.
- After the final reboot: Home in focus, 20 packages disabled, screensaver set, 0 crash entries. On mh22, Amazon's launcher resumed about 0.5 s before Home.
- Real remote and video playback are not verified on mh23.
- Wi-Fi moves between bands and drops for about a minute. Wi-Fi packages stay untouched, so the fix is to lock this stick to 2.4 GHz on the router.

## Home — 2026-11-01

- `1.67-mh20-guard` installed. Fire OS 6.7.1.1 leaves accessibility `services:{}`, so Home Guard cannot run.
- `FireKeeper` reads usage stats and moves Home's task to the front. `RevivalListener` is enabled through `enabled_notification_listeners`, which starts it before Amazon's launcher.
- Boot: Home resumed before Amazon's launcher on 3 of 3 measured boots, about 29 s in. Amazon's launcher then showed for 0.4–1.4 s.
- Settings: pressing Home from Settings reads the log with `READ_LOGS`, because Fire OS keeps the `homekey` broadcast for Amazon-signed apps.
- Tested by ADB key events: 16 apps open and return to Home; gear, house and app tiles, reboot. Real remote and video playback are not verified here.
- Device owner is unavailable: Amazon parental controls is Profile Owner and accounts exist. `set-home-activity` was rejected for the guard build.
- Wi-Fi moves between 2.4 GHz (-54 dBm) and 5 GHz (-75 dBm), with drops of about a minute.

## Debloat — 2026-11-01

Reference: dumbtv's tested AFTMM list. Left out per owner's rules: Wi-Fi (`com.amazon.wifilocker`, `com.amazon.whisperjoin.wss.wifiprovisioner`), update (`com.amazon.tv.forcedotaupdater.v2`, `com.amazon.tv.easyupgrade`), plus `com.amazon.tv.ffsprovisioneeclient`. Alexa and input packages were not touched.

Disabled, surviving reboot (20): `com.amazon.ceviche`, `com.amazon.connectivitydiag`, `com.amazon.dp.logger`, `com.amazon.hybridadidservice`, `com.amazon.kso.blackbird`, `com.amazon.logan`, `com.amazon.privacypassservice`, `com.amazon.shoptv.client`, `com.amazon.shoptv.firetv.client`, `com.amazon.sneakpeek`, `com.amazon.storm.lightning.tutorial`, `com.amazon.systemnotices`, `com.amazon.tmm.tutorial`, `com.amazon.tv.fw.metrics`, `com.amazon.tv.releasenotes`, `com.amazon.tv.website_launcher`, `com.amazon.wirelessmetrics.service`, `com.amznfuse.operatorredirection`, `com.android.carrierconfig`, `com.mediatek.atci.service`.

Re-enabled by Fire OS at boot, so `RUN_IN_BACKGROUND` is set to `ignore` instead: `com.amazon.hedwig`, `com.amazon.perfcollection`, `com.amazon.tigris`, `com.amazon.tv.developer.dataservice`, `com.amazon.ale`, `com.amazon.katoch`. Protected, same block: `com.amazon.client.metrics`, `com.amazon.device.metrics`, `com.amazon.device.crashmanager`, `com.amazon.device.logmanager`.

Settings: ad tracking limited, usage metrics upload and marketing off, window and transition animations 0.5.

Revert with `pm enable <package>` and `appops set <package> RUN_IN_BACKGROUND allow`.

Available memory: 290–300 MB before; 240–410 MB after, with apps cached, so no clear gain is proven. Scrolling Home after compile: 2.3% janky frames, 90th percentile 9 ms.

## Home — 2026-10-31

- Installed `1.67-mh17-guard` as a regular app. The guard build declares no HOME filter, so Amazon's launcher keeps the Home button.
- Home Guard is not enabled, and no packages were disabled.
- Set to Color tiles and Frosted glass. Liquid glass needs Android 13.
- First-run measurements: 164–197 MB PSS while in use, 86 MB after returning from Amazon's launcher; 20% janky frames, 90th percentile 23 ms; no crash entries.
- Wi-Fi: RSSI -75 dBm on 5 GHz. The stick dropped off the network three times for about a minute, the first before Home was installed. Large ADB transfers stalled.
- Not verified: full-grid review and launching apps from Home's tiles.
