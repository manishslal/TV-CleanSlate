# Android Home

The TV CleanSlate Home app for Android TV and Google TV. It is a fork of [Aerial](https://github.com/pengchujin/aerial-tv) by pengchujin and contributors, who wrote almost all of the system, compatibility and device code. Please support the original project.

- Changes from upstream: [FORK.md](FORK.md)
- Design standard: [DESIGN.md](DESIGN.md)
- Performance results: [docs/PERFORMANCE.md](docs/PERFORMANCE.md)
- Credits: [NOTICE.md](NOTICE.md)
- Docs index: [docs/README.md](docs/README.md)
- Upstream's own READMEs: [English](docs/upstream/README.en.md), [Chinese](docs/upstream/README.zh.md)

## Build

Requires JDK 17 and the Android SDK.

```bash
./gradlew assembleStandardRelease testStandardReleaseUnitTest
```

Without `local/signing.properties` the release build uses the debug key. To sign your own builds, add a keystore and properties file to `local/`, which git ignores.

## Install

```bash
BUILD=1 TV=<tv-ip>:5555 tools/install.sh
adb -s <tv-ip>:5555 shell cmd package compile -m speed -f io.manish.home
```

To install an APK you already built, use `APK_FILE=<apk>` instead of `BUILD=1`. Full steps: [Getting started](../guides/getting-started.md).

## Logo pack (optional)

If `../logos/dark/` holds your own logos, the build includes them for Dark Glass tiles. They are not part of this repository. See [logos/README.md](../logos/README.md).

## Undo

```bash
TV=<tv-ip>:5555 tools/restore.sh --uninstall
```

If you disabled the stock launcher, enable it first with `adb shell pm enable <package>`.
