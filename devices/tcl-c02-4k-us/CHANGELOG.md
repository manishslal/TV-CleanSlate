# Changelog — TCL Android TV ("<device-name>")

**Device:** TCL `Smart TV`, product `C02_4K_US`, device `BeyondTV`, Realtek RTD2851 (`rtd285o`), 32-bit ARM (armeabi-v7a), 2 GB RAM (MemTotal 2,097,152 kB), 1920×1080 UI at 320 dpi, /data 10 GB (6.5 GB free)
**OS:** Android TV 9 (SDK 28), build PPR1.180610.011 (TCL AP12), fingerprint `TCL/C02_4K_US/BeyondTV:9/PPR1.180610.011/AP12:user/release-keys`, security patch 2021-09-05, bootloader locked, TCL client type `TCL-NA-RT2851-S1`
**IP:** <tv-ip> on Wi-Fi "<wifi-name>", mDNS `Android-4.local`, Cast name "<device-name>", ADB mDNS `<adb-name>`
**ADB:** plaintext 5555 already open. The owner accepted "Allow USB debugging?" once on the TV. No pairing code.
**Rooted:** No

## 2026-10-29

### Baseline (read-only)
- `scripts/01-preflight-backup.sh <tv-ip>:5555` → `backups//`.
- Stock Home: **Google's `com.google.android.tvlauncher/.MainActivity`** (priority 2), TCL-customized through `com.tcl.partnercustomizer` (Search / Home / Live / Shop / Apps tabs). Other HOME candidates: `com.tcl.keycustomfunctionservice/.FallbackHome` (-999), `com.android.tv.settings/.system.FallbackHome` (-1000).
- No TCL launcher guard: `persist.sys.launcher.component` is empty, and there's no `boot_app_componentname` or "boot to TV source" setting. `adb install` isn't blocked: `persist.tcl.installapk.enable=1`, and the verifier is Play's.
- Already disabled at baseline: `com.tcl.bootadservice`, `com.google.android.youtube.tvmusic`, `com.google.android.videos`, `com.google.android.play.games`.
- Baseline settings: `enabled_accessibility_services` empty, `accessibility_enabled=0`, **`screensaver_enabled=0`**, `screensaver_components` empty (default `com.google.android.backdrop/.Backdrop`), `screen_off_timeout=900000`, `sleep_timeout=14339000`.
- Full package lists: `packages-*.txt` in the backup folder (119 total, 109 system, 10 third-party).
- `com.tcl.appstatecontroller` fetches an app on/off list from TCL's servers at every boot and applies it (log tag `AppStateUtils`). It **re-enables `com.tcl.smartalexa` and `com.tcl.usercenter`** after `pm disable-user`. Its other entries are UK/AU apps that aren't on this TV.

### Install: Home (`io.manish.home`)
- Fork `android`, branch `main` (from `fork/v1-features` 2b754b4). `versionCode` 81 → **82**, `versionName` 1.67-mh2. Standard flavor. Lite isn't needed (SDK 28 ≥ minSdk 21).
- APK sha256 `16bc0140…036ee30`.
  ```bash
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
  ./gradlew assembleStandardRelease -q
  PKG=io.manish.home APK_FILE=app/build/outputs/apk/standard/release/app-standard-release.apk TV=<tv-ip>:5555 tools/install.sh
  ```
- Plain `adb install -r` worked. The script granted the usual permissions and ran `compile -m speed`. No KEEP_ADB.
- `set-home-activity` returns Success, but Android 9 ignores it here. `tvlauncher` is a higher-priority system HOME candidate, and the resolver picks the top candidate without looking at the preferred-activity setting when one outranks the rest. So Home Guard (accessibility) did the work until cleanup: Home key from Settings → tvlauncher showed for ~5 s → Home came back.
- `install.sh` falsely said "Home Guard is not running". The fork labels the service "Home Home Guard", and the script's check only matched `Aerial`/`app.orchard.tv`. Fixed in `tools/install.sh` and `tools/selftest.sh` on `main`.
- Liquid glass needs API 33. On API 28 it falls back to the frosted panels; the Control Center screenshot showed the light frosted panel.

### Cleanup (owner approved items 1–10 + screensaver, 2026-10-29 ~20:38)
| Package | What | Method | Undo |
|---|---|---|---|
| `com.google.android.tvlauncher` | Stock launcher (142 MB PSS). Disabling it makes Home the real default | disable-user | `adb shell pm enable com.google.android.tvlauncher` |
| `com.google.android.tvrecommendations` | Launcher recommendation rows | disable-user | `adb shell pm enable com.google.android.tvrecommendations` |
| `com.tcl.bi` | TCL telemetry | disable-user | `adb shell pm enable com.tcl.bi` |
| `com.tcl.messagebox` | TCL promo messages | disable-user | `adb shell pm enable com.tcl.messagebox` |
| `com.tcl.esticker` | In-store demo sticker | disable-user | `adb shell pm enable com.tcl.esticker` |
| `com.tcl.showmode` | Store demo mode | disable-user | `adb shell pm enable com.tcl.showmode` |
| `com.tcl.waterfall.overseas` | TCL Channel | disable-user | `adb shell pm enable com.tcl.waterfall.overseas` |
| `com.google.android.feedback` | Feedback sender | disable-user | `adb shell pm enable com.google.android.feedback` |
| `com.google.android.tv.bugreportsender` | Bug-report sender | disable-user | `adb shell pm enable com.google.android.tv.bugreportsender` |
| `com.tcl.appmarket2` | TCL app store | disable-user | `adb shell pm enable com.tcl.appmarket2` |
| `com.tcl.tvweishi` | TCL "TV Guard" cleaner | disable-user | `adb shell pm enable com.tcl.tvweishi` |
| `com.tcl.usercenter` | TCL account | disable-user, re-enabled by TCL at boot → **`pm uninstall -k --user 0`** | `adb shell cmd package install-existing com.tcl.usercenter` |
| `com.tcl.smartalexa` | Alexa link | disable-user, re-enabled by TCL at boot → **`pm uninstall -k --user 0`** | `adb shell cmd package install-existing com.tcl.smartalexa` |
| Screensaver | Was off | `settings put secure screensaver_enabled 1`; `screensaver_components io.manish.home/app.orchard.tv.dream.AerialDream`; ConfigReceiver `own_screensaver true` | `adb shell settings put secure screensaver_enabled 0; adb shell settings delete secure screensaver_components` |

Not touched: TV inputs (`com.tcl.tv`, `com.tcl.tvinput`), `com.tcl.keycustomfunctionservice` (remote keys + fallback Home), `com.tcl.partnercustomizer`, `setupwraith`, katniss (voice), `com.tcl.assistant`, GMS/Play, `mediashell` (Cast), `com.tcl.xian.StartandroidService` (device authentication), firmware updaters (`versionUpdateApp`, `rc.ota`), `appstatecontroller`, Netflix, YouTube, Prime Video, and the owner's apps.

### Verification on the TV
| Check | Result |
|---|---|
| HOME resolves to `io.manish.home/app.orchard.tv.MainActivity` (Android default, not just the guard) | PASS (after cleanup, and after 2 reboots) |
| Boot to Home | PASS: two real reboots, Home in front ~25 s after boot_completed, guard bound |
| Home key from Settings | PASS: Home in front within 1 s (was ~5 s via the guard before cleanup) |
| App launch + Home key | PASS: Plex opened, Home key → Home in 1.5 s. Earlier someone at the TV opened YouTube from Home and pressed Home: system Home start → Home |
| Disables survive reboot | PASS: 15 disabled after 2nd reboot. usercenter/smartalexa stay uninstalled for user 0 (TCL's re-enable is a no-op on them) |
| Screensaver | PASS: Somnambulator → `mCurrentDreamName=io.manish.home/app.orchard.tv.dream.AerialDream`, `IsTest=false`; screenshot shows the flip clock + photo caption; Up key woke it to Home |
| Control Center | PASS: frosted light panel (glass fallback); footer "Set as your home screen" green |
| Wi-Fi after reboots | PASS: <wifi-name> |
| Memory | `dumpsys meminfo io.manish.home` TOTAL PSS 88,979 kB (Native 59 MB, Java 7.6 MB); system MemAvailable 612 MB of 2 GB. Before cleanup: Home 124 MB, tvlauncher 142 MB |

Screenshots were taken with `adb exec-out screencap -p` (this session only).

Not verified: the system screensaver starting on its own after the 15 min `screen_off_timeout` (Somnambulator starts the same dream), HDMI input switching from Control Center, voice search, and a firmware update re-enabling packages (re-run the check below after any TCL update).

## Undo
Single items: see the table above. Everything, from `android`:
```bash
for p in com.google.android.tvlauncher com.google.android.tvrecommendations com.tcl.bi com.tcl.messagebox com.tcl.esticker com.tcl.showmode com.tcl.waterfall.overseas com.google.android.feedback com.google.android.tv.bugreportsender com.tcl.appmarket2 com.tcl.tvweishi; do adb -s <tv-ip>:5555 shell pm enable $p; done
for p in com.tcl.usercenter com.tcl.smartalexa; do adb -s <tv-ip>:5555 shell cmd package install-existing $p; done
PKG=io.manish.home TV=<tv-ip>:5555 tools/restore.sh          # guard, grants, screensaver, HOME
adb -s <tv-ip>:5555 shell settings put secure screensaver_enabled 0
adb -s <tv-ip>:5555 shell pm uninstall io.manish.home         # optional
```
**Re-enable tvlauncher before uninstalling Home.** Otherwise the TV falls back to TCL's blank FallbackHome.
Last resort: `scripts/99-restore-stock.sh <tv-ip>:5555`.

## Check after a TCL firmware update
```bash
adb -s <tv-ip>:5555 shell pm list packages -d | sort
adb -s <tv-ip>:5555 shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME -c android.intent.category.DEFAULT
```
Expect 15 disabled and `io.manish.home/app.orchard.tv.MainActivity`.

## Notes
- ADB 5555 stays open in plaintext (it already was). Turn off USB debugging in Developer options when it's no longer needed.
