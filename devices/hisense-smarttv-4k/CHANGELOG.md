# Changelog — Hisense SmartTV 4K (Android TV)

**Device:** Hisense SmartTV 4K
**Android TV:** 12 | Build: STT2.230929.001.092
**IP:** <tv-ip> | MAC: <mac>
**Rooted:** No | **Method attempted:** `pm disable-user --user 0` (persistence failed — see below), `pm uninstall --user 0` (persistence also failed)

---

## Pre-flight (2026-09-06)
- ADB connect required accepting an on-screen RSA authorization popup on the TV.
- Model/build/Android version confirmed via `getprop` — matches user-provided info exactly.
- Reboot-survival test passed with zero changes made: rebooted, waited ~90s, ADB reconnected, uptime confirmed fresh boot (~101s).
- Baseline metrics saved: `meminfo-before.txt`, `packages-disabled-before.txt` (7 already disabled at factory default), `packages-full-before.txt`, `packages-plain-before.txt` (165 total packages).
- Total RAM: 1,833,488K (~1.75GB physical — same RAM tier as the Onn boxes in the original ruleset, prompting batches-of-5 instead of 10).
- `dumpsys meminfo` times out on this device before reaching the Total/Free/Used RAM summary section (confirmed repeatedly, 10000ms timeout) — used `/proc/meminfo` (MemTotal/MemFree/MemAvailable) as the equivalent metric for both before and after.
- Already-disabled at baseline (factory default, never touched by this session): `com.mediatek.tis`, `com.android.providers.calendar`, `com.mediatek.dtv.tvinput.isdbtuner`, `net.vidaatv.hisense.android.tv`, `com.google.android.videos`, `com.hisense.odinbrowser`, `com.google.android.play.games`.

## Discovery findings (Rule 6)
- This is a Hisense Android TV build with the VIDAA vendor layer present underneath (all `com.vt.*` packages are VIDAA components — "vt" = VIDAA), distinct from a "pure" Google Android TV reference build. This explains why the candidate list looked nothing like the Shield/Onn examples in the original ruleset.
- User's confirmed streaming/utility apps present and untouched, 100% match: Netflix, Disney+, Hulu, Max (`com.wbd.stream`), Paramount+ (`com.cbs.ott`), Peacock, Plex, Prime Video (`com.amazon.amazonvideo.livingroom`), TiviMate (`ar.tvplayer.tv`), Apple TV (`com.apple.atve.androidtv.appletv`), Windscribe, Send Files to TV, YouTube, plus confirmed-in-use YouTube Music/VLC/File Manager+ tiles.
- Confirmed via `dumpsys package <pkg>` intent-filter triage (LEANBACK_LAUNCHER presence = real home tile vs. background-only service) before requiring any live on-device navigation — efficient for ~50 ambiguous packages without physically opening each one.
- `com.vt.tv.videocolor` confirmed = core picture/video-color-processing service (`/system_ext/app/VideoColor`) — protected, not a debloat candidate despite ambiguous name.
- `com.vt.tv.protector` confirmed = Hisense's own storage-cleaner/"Turbo Mode" optimizer utility (NetworkMenu, TurboMode, SpaceProtection, Storage activities), has its own home tile. Also holds `DELETE_PACKAGES`/`CLEAR_APP_USER_DATA`/`RESTART_PACKAGES`/`MASTER_CLEAR` permissions and a `BOOT_COMPLETED` receiver — investigated as a possible package-state watchdog (see Persistence Investigation below); theory disproven.
- `com.esaba.downloader` confirmed = a genuine third-party sideloading/browser utility (VIEW/BROWSABLE intent filters), not OEM bloat — left untouched pending clarification, never disabled.
- User confirmed use of: AirPlay/mirroring, DLNA/casting, Alexa TV control, Google Assistant/voice search, YouTube Music, VLC, File Manager+ — all protected/left enabled.
- User confirmed NON-use of Hisense's VIDAA "smart" features (Smart Sense, AIoT, Single Listen, Media Center, TV Center) — these were the primary debloat targets.

## Persistence investigation — `pm disable-user` does not survive reboot on this device

This is the headline finding for this device and differs fundamentally from the Fire TV Stick session.

1. Disabled a batch of 5 unused VIDAA packages, verified disabled via `pm list packages -d`, rebooted (confirmed fresh uptime ~101s) — **all 5 were silently re-enabled by the reboot.** `pm list packages -d` afterward showed only the original 7.
2. Hypothesized `com.vt.tv.protector` (has `BOOT_COMPLETED` receiver + `RESTART_PACKAGES`/`DELETE_PACKAGES`/`MASTER_CLEAR` permissions) as a package-state watchdog. Tested by disabling `protector` itself alongside the same 5 packages, rebooting — **all 6, including `protector`, were reset.** Theory disproven; `protector` is not the mechanism (or isn't the only one).
3. Tested a diagnostic batch mixing a non-"smart" VIDAA package (`com.vt.eula`) with plain AOSP/Google packages (`com.android.printspooler`, `com.google.android.feedback`, `com.android.dynsystem`, `com.android.hotspot2.osulogin`) to determine if the reset was VIDAA-specific — **all 5 were reset**, including the plain AOSP ones. Confirmed: this is not namespace-specific. **No `pm disable-user --user 0` change survives a reboot on this device, for any package.**
4. Tested Rule 1's documented fallbacks: plain `pm disable` failed with `SecurityException: Shell cannot change component state` (Android 12 restricts unprivileged shell to `disable-user` only — expected, not new information). `pm hide` failed with the same `MANAGE_USERS` permission wall seen on the Fire TV (root required, forbidden by Rule 2).
5. Researched online — no documented fix specific to this issue for Hisense/VIDAA Android TV builds. One plausible lead investigated: a hidden VIDAA service menu (`Settings > Sound > Balance > 1969`) that on some Hisense models exposes a "Fastboot"/Quick-Start toggle whose behavior (suspend-and-snapshot-restore rather than true reboot) could explain the reset. On this device, the `1969` code instead surfaced a "reboot into factory mode" prompt — assessed as too risky to pursue (real factory/service diagnostic mode, not a simple settings toggle) and the user correctly cancelled out without proceeding.
6. With explicit user override of Rule 1 ("I want to keep trying ways to permanently disable/uninstall"), tested `pm uninstall --user 0` (user-scoped uninstall, reversible via `adb shell cmd package install-existing <package>` or a factory reset) on the same throwaway package (`com.vt.eula`) as an alternate mechanism. **Also reset by reboot** — `dumpsys package com.vt.eula` after reboot showed `installed=true` for User 0, fully restored. Confirms the reset happens at a level below both `pm disable-user` and `pm uninstall --user 0`.

**Conclusion:** This device's firmware restores a package-state snapshot on every true reboot (`adb shell reboot`), wiping any unprivileged-ADB-applied change and keeping only whatever's baked into the factory image (the original 7). No workaround was found that doesn't require root, which Rule 2 explicitly forbids (root risks a permanent Widevine L3 DRM downgrade, capping streaming apps at SD).

**User confirmed their TV is normally powered off via "Quick Start" standby, not a true reboot**, and true reboots are rare in normal use. Quick Start is a low-power suspend, not a full OS restart, and was not tested directly (no safe way to trigger it from ADB) — it may not trigger the same reset `adb shell reboot` does. Proceeded on this basis: disable for the current boot session, accept it won't survive an actual reboot, skip Rule 4's "reboot after every batch" (would only erase progress with no stability information gained, since we've already proven reboots wipe everything), and live-test stability instead.

## Batch attempt — 2026-09-06 (25 packages, later fully rolled back)

Disabled in 3 groups (live, no reboot between, per the adapted plan above):
- **Group A (7):** `com.vt.tv.smartsense`, `com.vt.tv.smartsense.ui`, `com.vt.tv.smartservice`, `com.vt.tv.aiot`, `com.vt.tv.singlelisten`, `com.vt.tv.mediacenter`, `com.vt.tv.tvcenter` — unused VIDAA smart features (user-confirmed non-use).
- **Group B (8):** `com.vt.eula`, `com.vt.factory`, `com.vt.fte`, `com.vt.storemode`, `com.vt.hishow`, `com.vt.tv.mobiledeviceconnectiontips`, `com.vt.udpserver`, `com.vt.tv.evservice` — one-time setup/legal/misc VIDAA stubs.
- **Group C (10):** `com.google.android.feedback`, `com.google.android.tungsten.setupwraith`, `com.google.android.apps.tv.dreamx`, `com.android.dreams.basic`, `com.android.printspooler`, `com.android.managedprovisioning`, `com.android.dynsystem`, `com.android.hotspot2.osulogin`, `tv.mopa.ginga`, `com.at.factory.androidtvaidlservice`.

All 25 confirmed disabled via `pm list packages -d` (32 total including the 7 factory baseline). Mid-batch, one ADB connection drop occurred (`adb: device offline`) — reconnected and verified state intact before continuing, no data lost.

**Unrelated event during this window:** the TV's saved WiFi password was lost, requiring re-entry. Investigated: device uptime was continuous (no reboot/crash), disabled-package count was unchanged before/after, and a logcat search for network-removal/disconnect signatures turned up nothing. Concluded this was an ordinary WiFi/router-side hiccup, not caused by the ADB session or package changes (package enable/disable state has no code path to saved network credentials).

**Regression found during live stability testing:** TV input switching stopped working. Two packages were suspected based on name (`com.vt.tv.evservice` — possibly "External Video" service; `com.at.factory.androidtvaidlservice` — a low-level factory hardware-bridge AIDL service, also the package whose disable coincided with the WiFi drop) and re-enabled live — **this did not restore input switching.** Given input switching is a Rule-5-protected core function, did not continue bisecting further live (risk of re-breaking); instead rolled back all 25 packages to `enabled`, verified via `pm list packages -d` (back to the original 7), and rebooted. **Input switching confirmed restored by the user after the reboot.**

Root cause not conclusively isolated to a single package — could be any of the 25, or a stuck hardware/CEC negotiation state unrelated to package changes and fixed simply by the reboot itself. **Not re-attempted or re-bisected given the persistence findings above make the reward too low to justify the risk** (any repeat disabling would be wiped by the next true reboot anyway, and re-triggers the same regression risk for no lasting benefit).

**Revert command (documented for completeness — not needed, full rollback already performed and verified):**
```bash
adb shell "pm enable com.vt.tv.smartsense; pm enable com.vt.tv.smartsense.ui; pm enable com.vt.tv.smartservice; pm enable com.vt.tv.aiot; pm enable com.vt.tv.singlelisten; pm enable com.vt.tv.mediacenter; pm enable com.vt.tv.tvcenter; pm enable com.vt.eula; pm enable com.vt.factory; pm enable com.vt.fte; pm enable com.vt.storemode; pm enable com.vt.hishow; pm enable com.vt.tv.mobiledeviceconnectiontips; pm enable com.vt.udpserver; pm enable com.vt.tv.evservice; pm enable com.google.android.feedback; pm enable com.google.android.tungsten.setupwraith; pm enable com.google.android.apps.tv.dreamx; pm enable com.android.dreams.basic; pm enable com.android.printspooler; pm enable com.android.managedprovisioning; pm enable com.android.dynsystem; pm enable com.android.hotspot2.osulogin; pm enable tv.mopa.ginga; pm enable com.at.factory.androidtvaidlservice"
```

## Speed tweaks applied — 2026-09-06 (Rule 8, kept)

```bash
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5
adb shell pm trim-caches 1G
```
All confirmed applied (`settings get` echoed back `0.5` for all three). Not tested for reboot persistence — given the `pm`-level findings above, these `settings put` values may or may not survive a true reboot either, but the downside of them resetting is negligible (worst case, animations return to normal speed) so no further reboot cycle was spent verifying this.

## Final state (2026-09-06)
- **Packages disabled: 7** — identical to the factory baseline. No net package-level change from this session (25-package batch was fully reverted after the input-switching regression).
- Speed tweaks (Rule 8): applied and confirmed live.
- Total RAM: 1,833,488K (unchanged, expected). MemFree: 78,228K vs. baseline 54,976K / 80,812K (two baseline readings taken at different points — free RAM fluctuates normally with background activity and isn't a meaningful before/after signal at this scale).
- Regressions: 1 (TV input switching, root cause not isolated) — **fully resolved via full rollback + reboot**, confirmed working by user.
- WiFi password loss: 1 occurrence, investigated, concluded unrelated to this session.

**Status: Hisense ( SmartTV 4K, Android TV 12) — CLOSED, no lasting package changes.** This device's firmware does not support persistent ADB-level debloating without root, which is out of scope per Rule 2. Recommended not to revisit package disabling on this device unless the user is willing to reconsider the no-root constraint (with the DRM/Widevine trade-off clearly understood), or unless a future firmware update changes this behavior. Speed tweaks are the only lasting change from this session, and even those are unconfirmed for reboot persistence.
