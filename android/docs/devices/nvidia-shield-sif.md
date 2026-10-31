# NVIDIA SHIELD Android TV (`sif`, 2019 tube)

Android TV 11 (SDK 30), SHIELD Experience 9.2.4, Tegra, 32-bit userland, 2 GB. Verified 2026-10-30 with Home 1.67-mh3.

- Stock Home is `com.google.android.tvlauncher`, priority 2. Android resolves Home to it while it is enabled, so Home Guard does the work until it is disabled. Disabling it makes the fork the real default. No model rule is needed.
- The standard flavor installs with plain `adb install`; there is no launcher install block.
- `setupwraith` is installed but is not a Home candidate.
- Liquid Glass needs API 33, so panels use frosted glass here.
- The UI is 1920x1080 at 320 dpi over a 4K display mode.
- No device-specific code was needed.
