# Aerial Lite (frozen)

Home for Android 4.0 to 4.4 TVs, which the Compose app cannot run on. Plain Views, no AndroidX. Package `app.orchard.tv.lite`, minSdk 14. Design notes: `../docs/LITE.md` (Chinese).

**Status: frozen.** Lite last changed with Home 1.67 (2026-10-28) and has not followed the app since:

- It does not compile at this commit. The app removed the `st_effects*` strings that `Panels.kt` still reads, and `SaverView.kt` uses a nullable `File` from the shared screensaver-video code without a null check. Both come from copying the app's strings and code at build time.
- It is listed in `settings.gradle.kts`, so Gradle configures it, but nothing builds it: CI and `./gradlew assembleStandardRelease testStandardReleaseUnitTest lintStandardRelease` only run `:app`.
- It is not tested on any TV after 1.6 and is not part of the release.

To unfreeze it, fix those two errors, run `./gradlew :lite:assembleRelease`, and test on a 4.x device or emulator before shipping.
