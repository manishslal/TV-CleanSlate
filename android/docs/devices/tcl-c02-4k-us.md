# TCL C02_4K_US (US Android TV 9, Realtek RTD2851)

> Not the China-market TCL notes ([tcl-mt5879-cn.md](tcl-mt5879-cn.md), [tcl-m7221-cn.md](tcl-m7221-cn.md), [tcl-mt9655-cn.md](tcl-mt9655-cn.md)). This is a US TCL on Google's Android TV with no TCL launcher or install verifier, so none of the 多屏互动 / guard-flavor steps apply.

- `ro.product.model` = `Smart TV`, `ro.product.device` = `BeyondTV`, `ro.product.name` = `C02_4K_US`, client type `TCL-NA-RT2851-S1`.
- Realtek `rtd285o` (RTD2851), 32-bit ARM, 2 GB RAM. Android 9 (SDK 28), build PPR1.180610.011 (AP12), patch 2021-09-05.
- 1920×1080 UI at 320 dpi (960×540 dp: Aerial's canvas without scaling).
- Stock Home: Google's `com.google.android.tvlauncher/.MainActivity` (priority 2), TCL-customized through `com.tcl.partnercustomizer`. Fallbacks: `com.tcl.keycustomfunctionservice/.FallbackHome` (-999), Settings FallbackHome (-1000).
- Flavor: **standard**. `adb install -r` works (`persist.tcl.installapk.enable=1`, Play is the verifier). ADB is plaintext 5555 with the usual "Allow USB debugging?" prompt.
- Liquid glass is off (API 33+); the frosted panels show instead. Verified 2026-10-29.

## Home

`cmd package set-home-activity` reports Success, but Home still resolves to tvlauncher. On Android 9, when the top HOME candidate has a higher priority than the next, the resolver picks it without looking at the preferred activity. Two ways to handle it:

1. Leave tvlauncher: Home Guard brings Aerial back about 5 s after a Home press (Android's background-start delay).
2. Owner-approved: `pm disable-user --user 0 com.google.android.tvlauncher`. Home then resolves to Aerial, the Home key is instant, and it frees ~140 MB PSS. Undo with `pm enable`. Re-enable it before uninstalling Aerial, or the TV falls back to TCL's blank FallbackHome.

Boot: Aerial was in front ~25 s after boot_completed in both of the above setups (two reboots each). Before unlock, `MainActivity` hands over to TCL's FallbackHome (`handOverToFirmwareFallbackHome`) as designed. The system logs `prevent third party app from boot complete broadcast` for Aerial, but HOME and the guard bring it up anyway.

## TCL app-state controller

`com.tcl.appstatecontroller` downloads an enable/disable list from TCL at every boot (log tag `AppStateUtils`) and applies it. On this TV it re-enables `com.tcl.smartalexa` and `com.tcl.usercenter` after `pm disable-user`. Use `pm uninstall -k --user 0` for those (undo `cmd package install-existing`). Its enable call is a no-op on a package that isn't installed for the user.

## Screensaver

`screensaver_enabled` was 0 out of the box. With it set to 1 and `screensaver_components` = `<pkg>/app.orchard.tv.dream.AerialDream`, `am start -n com.android.systemui/.Somnambulator` runs AerialDream as the real dream (`mCurrentDreamIsTest=false`), and Up wakes to Aerial.

## Memory

With tvlauncher disabled, Aerial is ~87 MB PSS on Home and MemAvailable is ~600 MB of 2 GB. With tvlauncher still running, Aerial was 124 MB and tvlauncher 142 MB.
