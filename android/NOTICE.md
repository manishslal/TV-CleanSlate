# Notice and credits

This repository is a fork of **[Aerial](https://github.com/pengchujin/aerial-tv)** by **pengchujin** and the Aerial contributors.
Almost all of the code, design and device-compatibility work here is theirs. The fork exists as the Android part of [TV CleanSlate](../README.md).

- Upstream: https://github.com/pengchujin/aerial-tv
- Full project credits: [CREDITS.md](../CREDITS.md)
- Forked at upstream commit `c3c0511` (v1.67, 2026-10-28)
- Upstream license: [MIT](LICENSE). Copyright (c) 2026 pengchujin. The original license text and copyright notice are kept unchanged in `LICENSE`, as the MIT license requires.
- Changes made in this fork are listed in [FORK.md](FORK.md) and in the git history. They are released under the same MIT license.

If you want Aerial itself, use and support the upstream project. Bug reports for anything not listed in FORK.md belong upstream only after checking that they reproduce on an unmodified upstream build.

## Third-party material (carried over from upstream)

| Material | Source | License |
|---|---|---|
| Inter font (`app/src/main/res/font/inter.ttf`) | The Inter Project Authors, https://github.com/rsms/inter | SIL Open Font License 1.1, see [FONT-LICENSE-OFL.txt](FONT-LICENSE-OFL.txt) |
| Barlow Condensed SemiBold (`app/src/main/res/font/barlow_condensed_semibold.ttf`), the fork's flap-card clock | The Barlow Project Authors, https://github.com/jpt/barlow | SIL Open Font License 1.1, see [FONT-LICENSE-OFL-Barlow.txt](FONT-LICENSE-OFL-Barlow.txt) |
| Icon path data in `GlyphPaths.kt` | Google Material Design Icons, https://github.com/google/material-design-icons | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| Chinese city list (`cities-cn.tsv`) | GeoNames, https://www.geonames.org | CC BY 4.0 |
| Inter, four static weights (`app/src/main/res/font/inter_static_*.ttf`), cut from the variable font for Android 7 | The Inter Project Authors, https://github.com/rsms/inter | SIL Open Font License 1.1, see [FONT-LICENSE-OFL.txt](FONT-LICENSE-OFL.txt) |
| Google Earth View picture numbers and places (`app/src/main/assets/earthview.json`) | Collected by https://github.com/limhenry/earthview | No license stated; only numbers and place names, as facts. The pictures are Google's, fetched at runtime |
| Interface sounds (`ui_move.wav`, `ui_select.wav`, `ui_back.wav`, `ui_limit.wav` in `app/src/main/res/raw/`) | Generated for the fork by `tools/make-sounds.py` | This project's license |
| Clock flap sound (`app/src/main/res/raw/ui_flap.wav`) | Made for the fork (see [FORK.md](FORK.md)) | This project's license |
| Aerial film list (`app/src/main/assets/aerials.json`) | Carried over from upstream; links to Apple's films only | Apple's terms for the films |
| Apple Root CA (`app/src/main/res/raw/apple_root_ca.pem`) | Apple's public root certificate, trusted for `sylvan.apple.com` only | Apple's public certificate |
| `SourceWallpaper.kt` (a TV input behind Home) | Adapted from @chikwok's tv-preview branch, which came into upstream Aerial | MIT, as upstream |
| Glass numbers (`Glisse` in `LiquidGlass.kt`) | Measured values from [glisse](https://github.com/0kzh/glisse) by Kelvin Zhang; numbers only, no code | MIT |
| NASA Earth Observatory, Wikimedia Commons (credited to author and license on screen), Google Earth View (links only) (runtime) | Fetched at runtime, never stored in this repository | NASA imagery is generally public domain; each Commons file's own license; Google's terms |
| Headlines (runtime) | Google News RSS | Google's terms |
| Plex videos and photos (runtime) | plex.tv and your own Plex server | Plex's terms |
| Weather data (runtime) | Open-Meteo, https://open-meteo.com | Open-Meteo terms (CC BY 4.0 data) |
| Aerial films (runtime) | Apple; streamed from Apple's servers, not included in this repository | Apple's terms |
| Photo of the day (runtime) | Microsoft Bing | Bing's terms |
| Location lookup (runtime) | ipinfo.io | ipinfo's terms |

Not affiliated with Apple, Microsoft, Google, pengchujin or any TV maker. "Aerial" is the upstream project's name. This fork uses its own name and app ID so it is never mistaken for the original.
