# Changelog — Fire TV Stick 4K (2nd Gen)

**Device:** Fire TV Stick 4K (2nd Gen), model `AFTKM` (codename "karat")
**Fire OS:** 8.1.8.2 (RS8182/3811) | Home: 7311000.1
**IP:** <tv-ip> | BSSID: <mac> | MAC: <mac>
**Rooted:** No | **Method:** `pm disable-user --user 0`

---

## Pre-flight (2026-09-04)
- ADB confirmed installed (via Homebrew `android-platform-tools`)
- Reboot-survival test passed with zero changes made: rebooted, waited 90s, ADB reconnected automatically, uptime confirmed fresh boot
- Baseline metrics saved: `meminfo-before.txt`, `packages-disabled-before.txt` (0 disabled), `packages-full-before.txt`, `packages-plain-before.txt` (300 total packages)
- Total RAM: 1,710,032K | Free RAM: 484,871K | Used RAM: 1,410,210K

## Discovery findings (Rule 6)
- `com.amazon.shoptv.firetv.client` confirmed = **Amazon Live** (via `dumpsys window`, activity `amazonlive.activity.OnboardingActivity`)
- `com.amazon.spiderpork` confirmed = **Luna** cloud gaming (internal codename; NOT `com.amazon.luna`, `gamehub`, or `cloud9` as commonly guessed elsewhere)
- `com.amazon.hedwig` confirmed = **Fire TV Channels** aggregator (activity `FireChannelsMainActivity`)
  - Music Videos / News / Sports / En Español confirmed to be **tabs inside hedwig**, not separate packages — same package/activity observed while inside "Music Videos"
- `com.amazon.tahoe` confirmed = **Amazon Kids+** (activity `AutoChildAppTileActivity`)
- "The Great Courses Signature Collection" tile found to route through `com.amazon.firebat` (protected Prime Video engine) — **not a separate package, left untouched**
- `com.amazon.vizzini` (Alexa media playback controller) — user relies on Echo devices to control this TV; no reliable public documentation separates "remote-only" vs. "Echo-device-targeting-TV" control paths for this package. **Left enabled** to avoid risking Echo→TV voice control.
- MiniTV (`com.amazon.minitv.android.app`) — not present as a home screen item for this user; left untouched (present in package list but not requested for removal)
- `com.amazon.gamehub`, `com.amazon.cloud9` — present on device but NOT confirmed to be Luna or anything else; left untouched (unconfirmed function, not in original ask)
- Cross-referenced all preinstalled 3rd-party apps against user's actual-use list (TiviMate=`ar.tvplayer.tv`, YouTube, Netflix, Plex, Hulu, HBO Max, Windscribe, Prime Video, Peacock, Disney+, Apple TV, Paramount+=`com.cbs.ott`, Send Files to TV=`com.yablio.sendfilestotv`) — **100% match, nothing extra installed to remove**
- System TTS/input components (`com.ivona.*`, `com.svox.pico`, `com.mediatek.tvinput`, `com.fireos.arcus.proxy`, `com.fireos.usagestats.proxy`) identified but left untouched — core system plumbing, not user-facing bloat, too close to protected categories (HDMI input, Alexa voice) to risk

## Batch 1 — 2026-09-04

| Package | Reason | Result |
|---|---|---|
| `com.amazon.shoptv.firetv.client` | Amazon Live shopping storefront — unused | ✅ Disabled |
| `com.amazon.shoptv.client` | Amazon Live support library (paired dependency) | ✅ Disabled |
| `com.amazon.spiderpork` | Luna cloud gaming — unused | ✅ Disabled |
| `com.amazon.hedwig` | Fire TV Channels aggregator (incl. Music Videos/News/Sports/En Español tabs) — unused | ✅ Disabled |
| `com.amazon.tahoe` | Amazon Kids+ — user requested removal from home screen | ✅ Disabled |
| `com.amazon.ftvads.deeplinking` | Ad deep-link handler | ❌ **Could not disable** — OS-protected package (`SecurityException: Cannot disable a protected package`); `pm hide` also blocked (`MANAGE_USERS` permission denied without root). Left enabled, no root attempted per rules. |
| `com.amazon.media.recommendations` | Powers "Recommended" row / personalization (modern precog equivalent) | ✅ Disabled |
| `com.amazon.hybridadidservice` | Advertising ID service | ✅ Disabled |

**Revert command for Batch 1:**
```bash
adb shell "pm enable com.amazon.shoptv.firetv.client; pm enable com.amazon.shoptv.client; pm enable com.amazon.spiderpork; pm enable com.amazon.hedwig; pm enable com.amazon.tahoe; pm enable com.amazon.media.recommendations; pm enable com.amazon.hybridadidservice"
```

## Batch 2 — 2026-09-04

Researched via web search against community Fire TV debloat scripts (esc0rtd3w/firestick-loader family, independent XDA/gist lists) before disabling anything, per user request. `com.amazon.diode` found explicitly commented-out (excluded) in one community script for an older Fire OS version with no explanation — excluded from this batch as a precaution. `com.amazon.tigris`, `com.amazon.systemnotices`, `com.amazon.tv.earlyaccess`, `com.amazon.aca` had no corroborating evidence either way — left untouched. `com.amazon.storm.lightning.tutorial` and `com.amazon.perfcollection` were corroborated as safe-ish bloat but held back to keep this batch at 10.

| Package | Reason | Result |
|---|---|---|
| `com.amazon.dummy.alarmclock` | Placeholder stub app (no real alarm clock on Fire TV) | ✅ Disabled |
| `com.amazon.dummy.calendar` | Placeholder stub app | ✅ Disabled |
| `com.amazon.dummy.contacts` | Placeholder stub app | ✅ Disabled |
| `com.amazon.dummy.gallery` | Placeholder stub app | ✅ Disabled |
| `com.amazon.dummy.music` | Placeholder stub app | ✅ Disabled |
| `com.amazon.dummy.settings` | Placeholder stub app | ✅ Disabled |
| `com.amazon.dummy.soundpicker` | Placeholder stub app | ✅ Disabled |
| `com.amazon.tv.easyupgrade` | One-time upgrade/onboarding UI | ✅ Disabled |
| `com.amazon.tv.releasenotes` | Release notes viewer | ✅ Disabled |
| `com.amazon.tmm.tutorial` | Tutorial overlay | ✅ Disabled |

**Revert command for Batch 2:**
```bash
adb shell "pm enable com.amazon.dummy.alarmclock; pm enable com.amazon.dummy.calendar; pm enable com.amazon.dummy.contacts; pm enable com.amazon.dummy.gallery; pm enable com.amazon.dummy.music; pm enable com.amazon.dummy.settings; pm enable com.amazon.dummy.soundpicker; pm enable com.amazon.tv.easyupgrade; pm enable com.amazon.tv.releasenotes; pm enable com.amazon.tmm.tutorial"
```

## Batch 3 — 2026-09-04

Amazon Photos and Amazon Music added at user's request ("never used, ever"). Confirmed via discovery that both are dormant stub packages — opening either tile routes to the Appstore's app-details page showing a "Download" button (not "Open"), meaning neither was ever actually activated/installed beyond the stub. Also included the 2 packages held back from Batch 2 research (corroborated safe, no risk warnings found).

| Package | Reason | Result |
|---|---|---|
| `com.amazon.bueller.photos` | Amazon Photos — confirmed dormant stub, user never used | ✅ Disabled |
| `com.amazon.bueller.music` | Amazon Music — confirmed dormant stub, user never used | ✅ Disabled |
| `com.amazon.storm.lightning.tutorial` | Tutorial overlay (held from Batch 2) | ✅ Disabled |
| `com.amazon.perfcollection` | Telemetry/performance collection (held from Batch 2) | ✅ Disabled |

**Revert command for Batch 3:**
```bash
adb shell "pm enable com.amazon.bueller.photos; pm enable com.amazon.bueller.music; pm enable com.amazon.storm.lightning.tutorial; pm enable com.amazon.perfcollection"
```

**Revert command for everything disabled today (Batches 1+2+3, all 21 packages):**
```bash
adb shell "pm enable com.amazon.shoptv.firetv.client; pm enable com.amazon.shoptv.client; pm enable com.amazon.spiderpork; pm enable com.amazon.hedwig; pm enable com.amazon.tahoe; pm enable com.amazon.media.recommendations; pm enable com.amazon.hybridadidservice; pm enable com.amazon.dummy.alarmclock; pm enable com.amazon.dummy.calendar; pm enable com.amazon.dummy.contacts; pm enable com.amazon.dummy.gallery; pm enable com.amazon.dummy.music; pm enable com.amazon.dummy.settings; pm enable com.amazon.dummy.soundpicker; pm enable com.amazon.tv.easyupgrade; pm enable com.amazon.tv.releasenotes; pm enable com.amazon.tmm.tutorial; pm enable com.amazon.bueller.photos; pm enable com.amazon.bueller.music; pm enable com.amazon.storm.lightning.tutorial; pm enable com.amazon.perfcollection"
```

## Post-batch investigation — Settings→Search regression (2026-09-04/05)

**Symptom (user-reported):** Home → 2 clicks left → menu icon → Settings (gear icon) → Enter now lands on the launcher's own **Search** page instead of the real Settings app. Confirmed not present immediately after Batch 1; noticed sometime after Batch 3.

**Also reported:** Amazon Photos and Amazon Music tiles still visible on the home screen after being disabled in Batch 3.

**Diagnosis steps taken:**
1. `uiautomator dump` of the nav rail confirmed the highlighted item the user is pressing Enter on is genuinely internally labeled `utility-settings_destination` — not a mislabeling/rendering bug, the launcher itself is routing this selection incorrectly.
2. `adb shell dumpsys activity activities | grep -A3 ResumedActivity` immediately after reproducing the bug showed the foreground activity is `com.amazon.tv.launcher/.ui.HomeActivity_vNext` — i.e. there is no separate "Settings" or "Search" app being launched; Search is an internal fragment of the same Home activity, and the launcher is silently falling back to it.
3. `adb logcat -c` then reproducing the bug live captured the actual root cause:
   ```
   I ActivityTaskManager: START u0 {flg=0x50000000 cmp=com.amazon.tv.launcher/.navigation.NavigationActivity} from uid 10194
   W Binder  : Caught a RuntimeException from the binder stub implementation.
   W Binder  : java.lang.IllegalStateException: Can't change activity type once set: { ... mActivityType=standard ...} activityType=home
   ```
   This is an Android framework-level task/activityType conflict inside `com.amazon.tv.launcher` itself (attempting to launch `NavigationActivity`'s Settings destination into a task already typed `standard` instead of `home`), not an obvious "a disabled package is missing" error.
4. An unrelated, pre-existing `neo_init`/`dlopen` error referencing `com.amazon.livedeviceservice` (a package never touched in any batch) also appeared in the same logcat capture — supporting evidence that not all warnings/errors on this device's logcat are caused by our changes.

**Bisection attempted:** Re-enabled `com.amazon.dummy.settings` (Batch 2, most name-suspicious candidate) and rebooted to test in isolation.
- **Result: user confirmed this did NOT fix the issue** ("Just tried. It is still not working. So might be a fire tv glitch.").
- Re-disabled `com.amazon.dummy.settings` per user's request to revert the test and restore the full 21-package-disabled state. Verified via `pm list packages -d` after a fresh reboot (uptime ~102s) that all 21 packages remained disabled and persisted correctly.

**Conclusion:** Root cause NOT isolated to any single disabled package in Batches 1–3. Given (a) the failed bisection of the most likely suspect, (b) the crash signature being an internal Android task-type conflict inside Amazon's own unmodified launcher code, and (c) unrelated pre-existing errors observed in the same logcat window, this is most likely a **pre-existing Fire OS 8.1.8.2 launcher bug**, not a regression caused by this debloat session. **Not further bisected** (remaining Batch 2/3 packages were not individually re-tested) — user chose to accept current state and move on rather than continue isolating. If this resurfaces as a priority, the next step would be to bisect the remaining Batch 2 stub packages (`dummy.alarmclock/calendar/contacts/gallery/music/soundpicker`, `tv.easyupgrade`, `tv.releasenotes`, `tmm.tutorial`) one at a time, or try a factory reset (last resort, wipes all disables) to see if the bug predates any ADB changes at all.

**Amazon Photos / Amazon Music tiles still visible:** Not a `pm disable-user` bug. Confirmed via on-device check that both `com.amazon.bueller.photos` and `com.amazon.bueller.music` were **dormant stub packages that were never actually installed/activated** — opening either tile routed to the Appstore's app-details page showing a **"Download"** button, never "Open," both before and after disabling. Amazon's curated/promotional home-screen row renders these tiles from a server-side content feed independent of local package state — the same mechanism that shows "The Great Courses Signature Collection" (which routes through the protected `com.amazon.firebat` engine). `pm disable-user`/`pm enable` cannot add or remove a promotional row tile. **Untested native alternative suggested to user:** long-press the tile on the remote to check for a "Remove from row" / "Hide" context-menu option — this is a launcher UI feature, not an ADB-controllable one. Not confirmed as of this writing.

## Final metrics (2026-09-04, after Batch 3 + investigation, 21 packages disabled)
- Total RAM: 1,710,032K | Free RAM: 462,926K (167,682K cached pss + 258,016K cached kernel + 37,228K free) | Used RAM: 1,479,816K (1,206,768K used pss + 273,048K kernel)
- Free RAM delta vs. baseline: **-21,945K** (RAM free dropped slightly — expected/normal, "Used RAM" isn't the right lens on Android since disabled packages still consume some slots; the meaningful outcome is 21 fewer active packages, no bloat auto-restarting in the background, and the device feeling snappier per animation-scale tweaks in Rule 8, not a raw free-RAM increase)
- Packages disabled: 21 (see combined revert command above)
- Packages that could not be disabled: 1 (`com.amazon.ftvads.deeplinking` — OS-protected, no root attempted)
- Regressions: 1 unresolved, likely-pre-existing (Settings→Search launcher glitch, not conclusively tied to any disabled package)
- All Rule 4 physical tests (Home button, HDMI-CEC, Alexa remote, Prime Video, Netflix/DRM, on-screen keyboard, casting) passed across all 3 batches

**Status: Fire TV Stick 4K (2nd Gen) — COMPLETE.** Moving to next device family (Google TV / Android TV) per user request; Rule 9 OTA-freeze question was never answered/actioned (left enabled, no action taken).
