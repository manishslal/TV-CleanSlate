# Android Home

The TV CleanSlate Home app for Android TV and Google TV. It is a fork of [Aerial](https://github.com/pengchujin/aerial-tv) by pengchujin and contributors, who wrote almost all of the system, compatibility and device code. Please support the original project.

- Changes from upstream: [FORK.md](FORK.md)
- Design standard: [DESIGN.md](DESIGN.md)
- Performance results: [docs/PERFORMANCE.md](docs/PERFORMANCE.md)
- Credits: [NOTICE.md](NOTICE.md)
- Upstream's own READMEs: [English](docs/upstream/README.en.md), [Chinese](docs/upstream/README.zh.md)

## Build

Requires JDK 17 and the Android SDK.

```bash
./gradlew assembleStandardRelease testStandardReleaseUnitTest
```

Without `local/signing.properties` the release build uses the debug key. To sign your own builds, add a keystore and properties file to `local/`, which git ignores.

## Install

```bash
PKG=io.manish.home APK_FILE=app/build/outputs/apk/standard/release/app-standard-release.apk TV=<tv-ip>:5555 tools/install.sh
adb -s <tv-ip>:5555 shell cmd package compile -m speed -f io.manish.home
```

Always pass `PKG` and `APK_FILE`. Without them, `install.sh` fetches upstream Aerial.

## Logo pack (optional)

If `../logos/dark/` holds your own logos, the build includes them for Dark Glass tiles. They are not part of this repository. See [logos/README.md](../logos/README.md).

## Undo

```bash
adb shell settings put secure enabled_accessibility_services null
adb shell settings delete secure screensaver_components
adb uninstall io.manish.home
```

If you disabled the stock launcher, enable it first with `adb shell pm enable <package>`.
