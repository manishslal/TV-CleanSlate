# Supported devices

Only devices tested on real hardware are listed. The same data is in [devices.json](../devices/devices.json).

| Device | Codename | OS | RAM | Home | Latest build tested | Debloat | Liquid glass |
|---|---|---|---|---|---|---|---|
| [onn 4K Streaming Box](../devices/onn-4k-streaming-box/CHANGELOG.md) | `onn_4k_gtv` | Android TV 14 (SDK 34) | 2 GB | Android | 1.67-mh8 | Yes | Yes |
| [onn 4K Plus](../devices/onn-4k-plus/CHANGELOG.md) | `coffey` | Android TV 14 (SDK 34) | 2 GB | Android | 1.67-mh1 | Yes | Yes |
| [NVIDIA SHIELD TV, 2019 tube](../devices/nvidia-shield-tube/CHANGELOG.md) | `sif` | Android TV 11 (SDK 30) | 2 GB | Android | 1.67-mh4 | Yes | Lit edge |
| [TCL C02_4K_US](../devices/tcl-c02-4k-us/CHANGELOG.md) | `C02_4K_US` | Android TV 9 (SDK 28) | 2 GB | Android | 1.67-mh2 | Yes | Lit edge |
| [LG 50UP8000PUA](../devices/lg-50up8000pua/CHANGELOG.md) | `lm21a` | webOS 6.5.3, rooted | 1.3 GB | webOS | Web version | Yes | No |
| [Hisense SmartTV 4K](../devices/hisense-smarttv-4k/CHANGELOG.md) | | Android TV 12 | 1.8 GB | No | | No | |
| [Fire TV Stick 4K, 2nd gen](../devices/fire-tv-stick-4k-gen2/CHANGELOG.md) | `AFTKM` | Fire OS 8.1.8.2 | 1.7 GB | No | | Yes | |

## Limits

- **Liquid glass** needs Android 13 or newer. Older TVs get a lit edge and frost.
- **Google TV** can take Home back after a reboot. See the [Android and Google TV guide](../guides/debloat-android-google-tv.md).
- **LG** needs root, and only this model and firmware were tested.
- **Hisense** firmware doesn't keep package changes across reboots, and a debloat batch broke input switching. The batch was fully rolled back, so debloating isn't recommended on this model.
- **Newer builds** haven't been checked on real TVs until they appear in this table. 1.67-mh9 changes only wording and tools.

## Other TVs

Aerial supports many more TVs, mostly China-market models. Its notes are in [android/docs/devices/](../android/docs/devices/), but those models haven't been tested with this fork.

## Adding a device

Open a [device report](https://github.com/manishslal/TV-CleanSlate/issues/new?template=device-report.yml), or follow [CONTRIBUTING.md](../CONTRIBUTING.md).
