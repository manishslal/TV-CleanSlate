# About this fork

A personal fork of [pengchujin/aerial-tv](https://github.com/pengchujin/aerial-tv) (MIT) for customizing the home screen on my own Android TVs. Credits and third-party licenses: [NOTICE.md](NOTICE.md).

## Ground rules
- `LICENSE` stays exactly as upstream wrote it. New files carry no claim over upstream code.
- Customizations live in the UI layer (`ui/`, `wallpaper/`, theme config). `system/`, `guard/`, `dream/` and `update/` stay as close to upstream as possible, so upstream fixes merge cleanly.
- The fork gets its own app name, icon and application ID. It must not be presented as Aerial.
- Runtime calls to the upstream author's server (`aerial.qust.me`: update checks, APK downloads, the icon library and the phone setup page) are repointed or switched off in our builds. Our builds are signed with our own key, so upstream updates can't install over them, and we shouldn't load the author's server.

## Branches
- `main`: tracks upstream plus our merged changes.
- `fork/*`: our work branches.

## Syncing with upstream
```bash
git fetch upstream
git checkout main
git merge upstream/main
```

## Changes from upstream
| Date | Change |
|---|---|
| 2026-10-28 | Forked at `c3c0511` (v1.67). Added NOTICE.md and FORK.md, plus a fork notice at the top of the READMEs. |
| 2026-10-28 | v1.67-mh1: app ID `io.manish.home` (label "Home"), own signing key, no calls to aerial.qust.me (`BuildConfig.UPSTREAM_SERVER`); revival account type follows the ID. |
| 2026-10-28 | °F/°C (default °F), 12/24-hour clock (default "5:39 PM"); city is still chosen from the phone page. |
| 2026-10-28 | Dark tiles (default): white logos on dark glass; many-colored logos keep their colors without their plate; picture icons give up a light logo. Tile cache `ART` +103. |
| 2026-10-28 | Plex: PIN link (plex.tv/link), local server discovery, video library → My Videos (direct play), photo library → My Photos (1080p copies, plex-*.jpg). Screensaver buffer raised to 48 MB. |
| 2026-10-28 | Tech headlines over the screensaver (Google News Technology RSS): one at a time, bottom right, no glass. Place caption narrows to 460dp while it is on. |
| 2026-10-28 | Plex finds the server by its HTTPS plex.direct address (cleartext to the LAN is blocked). Phone setup page on port 8791 so it can run beside upstream Aerial (8790). Plex library rows scroll instead of pushing the label out. |
| 2026-10-28 | `tools/install.sh` / `restore.sh`: `PKG=io.manish.home` installs or removes the fork; component names use the class namespace (`NS=app.orchard.tv`) instead of the package. |
| 2026-10-28 | Appearance (was "App tiles"): Dark also dims the wallpaper 30%. In Dark, adaptive icons drop their background layer, picture icons lose their plate, near-black in a logo goes; Free My TV recipe. Tile cache `ART` +108. `export_tiles` takes `tiles_look dark|light`. |
| 2026-10-28 | Flip clock (default on, Personalize → Clock): the Home clocks turn digit by digit like a departure board (`ui/home/FlipText.kt`); plain under reduced motion. |
| 2026-10-28 | A dark shade behind the status pill once the grid scrolls; a single-line focus frame with a soft shadow (no dark edge or inner line); no sound at the grid's edge. |
| 2026-10-29 | Clock: Cards (default) / Flip / Plain. Cards put each letter and digit of the large clock and the screensaver clock on a departure board's flap card that turns with it. |
| 2026-10-29 | Appearance: Auto, dark from today's sunset to sunrise (Open-Meteo `sunrise,sunset`). In Light the panels are light frosted glass with dark type (`LocalLightPanel`; Palette and Type read it). |
| 2026-10-29 | Dark: a glow in the focused app's brand color behind the grid (`TileArt.tint` from the icon; cache `ART` +109). Tiles rise into place in a diagonal wave. Screensaver row scrolls ("Styl" fix). Control Center footer and Home Guard say "Home". |
| 2026-10-29 | Board clock: Cards are set as a departure board, in Barlow Condensed SemiBold (OFL, NOTICE.md) on cards of one width, warm-white ink, hinge pins, a split gap, smaller AM/PM cards; flaps fall and bounce. Date and weather as board lines. Optional flap sound (`res/raw/ui_flap.wav`, made for the fork). |
| 2026-10-29 | Location chosen on the TV: Personalize → Weather → Change searches Open-Meteo's geocoder in the TV's language and lists each place with its region and country; Automatic goes back to the network's guess. |
| 2026-10-29 | Headlines by topic: Tech (a Google News search for AI, open source, models, chips, startups and IPOs, games left out), AI, US, World, Business, Science, Sports, Gaming, Entertainment, Health; several at once, taking turns or shuffled. Each headline says its topic. |
| 2026-10-29 | Liquid Glass (Android 13+, AGSL, `ui/components/LiquidLens.kt`): panels bend the sharp wallpaper at their rim, part red and blue there, and catch light; frosted inside so type reads. Personalize → Glass: Liquid / Frosted; frosted elsewhere and in Lite. |

## Planned
- [ ] Final app name and icon; rename the strings that still say "Aerial" for the app (setup page, Home Guard)
- [ ] Theme layer: colors, fonts, tile shape, focus effect, motion timings
