# Notice and credits

This repository is a fork of **[Aerial](https://github.com/pengchujin/aerial-tv)** by **pengchujin** and the Aerial contributors.
Almost all of the code, design and device-compatibility work here is theirs. The fork exists to make personal UI changes on top of it.

- Upstream: https://github.com/pengchujin/aerial-tv
- Forked at upstream commit `c3c0511` (v1.67, 2026-10-28)
- Upstream license: [MIT](LICENSE). Copyright (c) 2026 pengchujin. The original license text and copyright notice are kept unchanged in `LICENSE`, as the MIT license requires.
- Changes made in this fork are listed in [FORK.md](FORK.md) and in the git history. They are released under the same MIT license.

If you want Aerial itself, use and support the upstream project. Bug reports for anything not listed in FORK.md belong upstream only after checking that they reproduce on an unmodified upstream build.

## Third-party material (carried over from upstream)

| Material | Source | License |
|---|---|---|
| Inter font (`app/src/main/res/font/inter.ttf`) | The Inter Project Authors, https://github.com/rsms/inter | SIL Open Font License 1.1, see [FONT-LICENSE-OFL.txt](FONT-LICENSE-OFL.txt) |
| Icon path data in `GlyphPaths.kt` | Google Material Design Icons, https://github.com/google/material-design-icons | Apache License 2.0 |
| Chinese city list (`cities-cn.tsv`) | GeoNames, https://www.geonames.org | CC BY 4.0 |
| Weather data (runtime) | Open-Meteo, https://open-meteo.com | Open-Meteo terms (CC BY 4.0 data) |
| Aerial films (runtime) | Apple; streamed from Apple's servers, not included in this repository | Apple's terms |
| Photo of the day (runtime) | Microsoft Bing | Bing's terms |
| Location lookup (runtime) | ipinfo.io | ipinfo's terms |

Not affiliated with Apple, Microsoft, Google, pengchujin or any TV maker. "Aerial" is the upstream project's name. This fork will ship under its own name and app ID so it is never mistaken for the original.
