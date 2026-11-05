# Aerial Lite (frozen)

Home for Android 4.0 to 4.4 TVs, which the Compose app cannot run on. Plain Views, no AndroidX. Package `app.orchard.tv.lite`, minSdk 14. Design notes: `../docs/LITE.md` (Chinese).

**Status: frozen.** Lite last changed with Home 1.67 (2026-10-28) and has not followed the app since:

- It compiles: the Effects choice it still offers has its own strings (`lite_effects_strings.xml`), since the app's Home Mode replaced them, and CI builds it with the app so it stays that way.
- It is not tested on any TV after 1.6 and is not part of any release.

To unfreeze it, test `./gradlew :lite:assembleRelease` on a 4.x device or emulator before shipping it.
