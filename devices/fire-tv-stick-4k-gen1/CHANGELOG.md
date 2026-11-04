# Changelog — Fire TV Stick 4K (1st Gen)

**Device:** Fire TV Stick 4K (1st Gen), model `AFTMM` (codename "mantis")
**Fire OS:** 6.7.1.1 (NS6711/5908), Android 7.1.2, SDK 25, 32-bit (`armeabi-v7a`)
**RAM:** 1.3 GB | **Rooted:** No | **Debloat:** Yes (20 disabled, 10 background-blocked)

---

## Home mh37 — 2026-11-04

- `1.67-mh37-guard` installed over mh36 and compiled with `-m speed`. Sounds switched from the TV's to Home's own (they had been chosen as TV).
- Checked: Top Center at Small, Large and Huge, and Huge at the right; the screensaver's small credit, news and larger clock over a Photos picture; the pill's clear focus glass; Control Center in Light with white button and slider cards (Lite's flat panel). Put back to Large at the top left. 0 crash entries.
- Later mh37 build: NASA Earth Observatory, Wikimedia and Google Earth View load here (6, 5 and 6 pictures) and show in the picker. Auto put the clock right on Earth View, left on NASA, middle on Wikimedia. Put back on the daily photo, Large at the top left. 0 crash entries.
- Review fixes: checked Earth View with Auto (clock right, shade now deep enough for the weather line), the focused pill's rim, Control Center's one card, the screensaver's news shade, and the fish in front of a centred Large clock (14% of the time, rising 36% of it). Put back on the daily photo, Large at the top left. 0 crash entries.

## Home mh36 — 2026-11-04

- `1.67-mh36-guard` installed over mh35 and compiled with `-m speed`.
- Measured what each effect costs here, two runs each of 28 moves across two rows of apps (`dumpsys gfxinfo`):

  | Wallpaper | Effects | Glass | Janky frames | 90th pct | Home memory |
  |---|---|---|---|---|---|
  | Still | Lite | Frosted | 0-1.2% | 12 ms | 150 MB |
  | Still | Full | Frosted | 1.8-2.1% | 13-14 ms | 151-155 MB |
  | Still | Full or Lite | Liquid | 0.8-3.2% | 13 ms | 150-152 MB |
  | Moving | Lite | Frosted | 26-32% | 24-27 ms | 222 MB |
  | Moving | Full | Frosted | 32-40% | 25-27 ms | 225-230 MB |
  | Moving | Full or Lite | Liquid | 37-65% | 27-32 ms | 222-233 MB |

- The moving wallpaper is what this stick cannot keep up with (about 80 MB more and a third or more of frames late). The focus effects cost almost nothing on a still picture.
- So Auto here now keeps the glow, tilt and sheen while the wallpaper is still, and drops them while it moves. Checked: the focused tile glows in its color, `focusLite=false` on the daily picture and `true` on the moving one; 1.6-2.5% janky frames on mh36. The Focus Glow row in Appearance follows the same switch but was not opened on screen.
- The look was put back as it was after measuring (daily picture, frosted glass, Auto effects, Photos screensaver).

## Home mh35 — 2026-11-04

- `1.67-mh35-guard` installed over mh34 and compiled with `-m speed`.
- Text size: Fire OS ships with the system text size at 1.0, while the onn and SHIELD ship at 0.85, which is what Home's type was sized on. Every sp size drew 18% larger here, most visibly the Wi-Fi name in the pill. Home now reads Fire OS's text size against 1.0, so its default matches the others. A larger Fire OS text size still enlarges Home.
- Checked: the pill, date, clock and weather line are smaller and match the onn's proportions. 0 crash entries.
- Later mh35 build, from the parity audit: the pill is smaller (14 sp name, 18 dp icon), the type is Inter (four static weights, since Android 7 cannot vary a font), and Focus Glow is hidden in Appearance because Lite draws none. Checked: pill, Control Center, Personalize and Appearance; there is no Turn Off TV row, because Fire OS binds no accessibility service. 0 crash entries.
- The focus glow stays off here: Auto effects choose Lite on this 1.3 GB stick, and Lite leaves the glow out by the owner's choice.

## Home mh34 — 2026-11-03

- `1.67-mh34-guard` installed over mh23 through `tools/install.sh guard`, then compiled with `-m speed`. `fire-reapply.sh`: 0 drifted, 0 refused.
- ADB showed the stick as "offline" until ADB Debugging was switched off and on in Developer Options. Its address had changed since mh23.
- Home: Wi-Fi name in the pill, colored tiles, the dock and clock all show. Netflix opened, and the Home key brought Home back.
- Control Center: Wi-Fi, Bluetooth, Settings and Auto buttons, volume, Personalize, Home Mode, "Home Guard is on". No VPN button, since no VPN has ever connected here. No Inputs or brightness rows.
- 0 crash entries; 124 MB PSS on Home.
- Not verified: real remote, video playback, a reboot on mh34, and the Bluetooth page.

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
