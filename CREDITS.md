# Credits

TV CleanSlate is built on other people's open source work. This page says what each project is, how it was used here, and what license it carries.

## Forked

| Project | Author | License | How it was used |
|---|---|---|---|
| [Aerial](https://github.com/pengchujin/aerial-tv) | pengchujin and contributors | MIT | The base of `android/`, forked at upstream commit `c3c0511` (v1.67, 2026-10-28). Almost all of the app's system, compatibility and device work is theirs. |

The Aerial commit history is preserved here with its original authors. Commit hashes differ from upstream because the code moved into `android/`, and private network details were scrubbed from this project's own commits. Changes made here are listed in [android/FORK.md](android/FORK.md). Issue numbers in old commit messages refer to the upstream Aerial repository.

Copyright notice, as MIT requires: `Copyright (c) 2026 pengchujin`. Full text in [android/LICENSE](android/LICENSE).

## Adapted

| Project | Author | License | How it was used |
|---|---|---|---|
| [dumbtv](https://github.com/humblemuzzu/dumbtv) | dumbtv contributors | MIT | `FireKeeper` follows its Keeper method for Fire OS 6. Its tested AFTMM package lists were a reference for the Fire TV guide. |
| tv-preview branch of Aerial | @chikwok | Contributed to Aerial, which is MIT | `SourceWallpaper.kt` (a TV input's picture behind Home) is adapted from it. It reached this repository through Aerial's history. |

## Used directly on devices

These tools were run on our TVs. Their code is not included in this repository.

| Project | Author | License | How it was used |
|---|---|---|---|
| [CleanYourTV](https://github.com/Gathin23/cleanyourtv) | Gathin | MIT | LG Home takeover and debloat. `webos/` deploys on top of it, and its ad-host list prompted our extra US list. |
| [SlopBro](https://github.com/throwaway96/slopbro) | throwaway96 | AGPL-3.0 | Rooted the LG TV. Run as a separate tool, not linked or copied. |
| [Homebrew Channel](https://github.com/webosbrew/webos-homebrew-channel) | webOS Homebrew Project | MIT | Root services, SSH and update blocking on the LG TV. |
| [Free My TV](https://github.com/simbasjungles/free-my-tv) | Free My TV contributors | MIT | Tested as a launcher on the onn box. The dark tile treatment in Android Home follows its approach. |
| [XMB TV Home](https://github.com/sanangoyal/XBM-TV-Home) | Sanan Goyal | MIT | Tested as a launcher on the onn box, rebuilt from source for a newer target SDK. |

## Inspiration and research

No code from these projects is included.

| Project | Author | License | What it contributed |
|---|---|---|---|
| [XMB TV Home](https://github.com/sanangoyal/XBM-TV-Home) | Sanan Goyal | MIT | The idea of a kit with backup, debloat, restore and agent prompts. |
| [CleanYourTV](https://github.com/Gathin23/cleanyourtv) | Gathin | MIT | Disable-only debloating, and recording state before any change. |
| [xmb-home](https://github.com/amachador/xmb-home) | amachador | MIT | Reviewed as a launcher option. |
| [glisse](https://github.com/0kzh/glisse) | Kelvin Zhang | MIT | The measured numbers behind the glass tint, rim and press springs (the `Glisse` object in `LiquidGlass.kt`). Only numbers were taken, no code. See [docs/research/liquid-glass.md](docs/research/liquid-glass.md). |
| [tv-home](https://github.com/linuxliam/tv-home) | linuxliam | None stated | Reviewed only. It has no license, so nothing from it may be reused. |

## Design tools

Used while redesigning the interface. Their files are not included.

| Project | Author | License |
|---|---|---|
| [impeccable](https://github.com/pbakaus/impeccable) | Paul Bakaus | Apache-2.0 |
| [taste-skill](https://github.com/Leonxlnx/taste-skill) | Leonxlnx | MIT |
| [UI UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) | nextlevelbuilder | MIT |

## Bundled material

| Material | Source | License |
|---|---|---|
| Inter font, variable and four static weights cut from it (`android/app/src/main/res/font/`) | [The Inter Project Authors](https://github.com/rsms/inter) | [SIL OFL 1.1](android/FONT-LICENSE-OFL.txt) |
| Barlow Condensed SemiBold (`android/app/src/main/res/font/` and `webos/`) | [The Barlow Project Authors](https://github.com/jpt/barlow) | SIL OFL 1.1: [Android copy](android/FONT-LICENSE-OFL-Barlow.txt), [webOS copy](webos/FONT-LICENSE-OFL-Barlow.txt) |
| Icon path data (`GlyphPaths.kt`) | [Google Material Symbols](https://github.com/google/material-design-icons) | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| Aerial film list (`android/app/src/main/assets/aerials.json`) | Carried over from Aerial. Links to and names of Apple's films only | The films are Apple's and are streamed from Apple, never stored here |
| Apple Root CA certificate (`android/app/src/main/res/raw/apple_root_ca.pem`) | Apple's public root certificate, trusted for `sylvan.apple.com` only (see `network_security_config.xml`) | Apple's public certificate |
| City list | [GeoNames](https://www.geonames.org) | CC BY 4.0 |
| Google Earth View picture numbers and places (`android/app/src/main/assets/earthview.json`) | Collected by [limhenry/earthview](https://github.com/limhenry/earthview) | No license stated; only the numbers and place names are kept, as facts. No picture is included: they are Google's, fetched at runtime and credited "Google Earth View" on screen |
| Interface sounds `ui_move`, `ui_select`, `ui_back`, `ui_limit` | Generated here by `tools/make-sounds.py` from sine tones, no samples | This project's license |
| Clock flap sound `ui_flap.wav` | Made for the fork, as [FORK.md](android/FORK.md) records. Not made by `make-sounds.py` | This project's license |
| Screenshots in `docs/images` and the icon and banner in `android/docs/brand` | Made for this project. The screenshots show Apple's aerial films, Bing, NASA and similar pictures from the sources below | This project's license for the interface; the pictures and logos in them stay with their owners |

## Libraries and services

| Item | Use | License or terms |
|---|---|---|
| [Jetpack Compose and AndroidX](https://developer.android.com/jetpack) | Android interface | Apache-2.0 |
| [html2canvas](https://github.com/niklasvh/html2canvas) | `tizen/tvshot.py` downloads it to the computer to screenshot Home. It isn't included here or in Home | MIT |
| [Media3 ExoPlayer](https://github.com/androidx/media) | Film playback | Apache-2.0 |
| [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | Phone setup page | BSD-3-Clause |
| [ZXing](https://github.com/zxing/zxing) | QR codes | Apache-2.0 |
| [Open-Meteo](https://open-meteo.com) | Weather at runtime | CC BY 4.0 data |
| [ipinfo.io](https://ipinfo.io) | Location lookup at runtime | ipinfo's terms |
| Apple aerial films | Streamed from Apple at runtime, never stored here | Apple's terms |
| Bing photo of the day | Fetched at runtime | Microsoft's terms |
| [NASA Earth Observatory](https://science.nasa.gov/earth/earth-observatory/) Image of the Day | Wallpaper fetched at runtime, credited on screen | NASA imagery is generally public domain |
| [Wikimedia Commons](https://commons.wikimedia.org) Picture of the Day | Wallpaper fetched at runtime; its author and license are shown with it | Each file's own license (CC BY, CC BY-SA and others) |
| [Google Earth View](https://earthview.withgoogle.com) | Wallpaper fetched at runtime from Google's picture host; only links are kept here | Google's terms |
| Google News RSS | Headlines at runtime | Google's terms |

## Cut-out service

`server/cutout` builds a Docker image on a home server. Nothing below is included in this repository.

| Item | Author | License | Use |
|---|---|---|---|
| [ISNet (DIS)](https://github.com/xuebinqin/DIS) general-use model | Xuebin Qin and contributors | Apache-2.0 | Finds a wallpaper's subject |
| [rembg](https://github.com/danielgatis/rembg) | Daniel Gatis | MIT | Its release hosts the ONNX export of the model, downloaded when the image is built |
| [ONNX Runtime](https://github.com/microsoft/onnxruntime) | Microsoft | MIT | Runs the model |
| [Pillow](https://github.com/python-pillow/Pillow) and [NumPy](https://github.com/numpy/numpy) | Their contributors | MIT-CMU, BSD-3-Clause | Pictures and arrays |

## Logo tools

Used by `logos/build_pack.py`. Their files are not included.

| Project | License | Use |
|---|---|---|
| [Simple Icons](https://github.com/simple-icons/simple-icons) | CC0-1.0 (trademarks still belong to their owners) | Source SVGs |
| [resvg](https://github.com/linebender/resvg) | Apache-2.0 or MIT | SVG rendering |
| [VTracer](https://github.com/visioncortex/vtracer) | MIT | Raster to SVG tracing |
| [Wikimedia Commons](https://commons.wikimedia.org) | Varies by file | Source SVGs not in Simple Icons |

## Trademarks

Logos and film frames visible in the screenshots belong to their owners and are shown only to illustrate the interface. The logo artwork is not distributed in this repository.

Missing or wrong credit? Please [open an issue](https://github.com/manishslal/TV-CleanSlate/issues).
