# Changelog

## Unreleased

- Android 1.67-mh25: Glass Behind Apps is now called Dock; clock text has darker shadows over bright video; a wallpaper that never starts gives way to the next after 30 seconds
- webOS: Aerial Quality offers 1080p or 4K; Samsung keeps 1080p
- `scripts/build-all.sh` rebuilds every Home and lists what each is missing
- Samsung Tizen: a web Home built from webOS Home, running on one 2017 TV
- Samsung Tizen: aerial videos now start reliably, through Media Source
- Samsung Tizen: the Home button returns to Home after Smart Hub opens, from Home's own screen, with a setting to turn it off
- Samsung Tizen: Home's Netflix tile no longer opens a black screen
- Samsung Tizen: the Menu key opens Settings, Source takes two presses, and the Inputs tile is gone
- Samsung Tizen: `tvshot.py` takes screenshots of Home and `tvkey.py` sends remote keys, with no root
- webOS: Dark shows one ink on every logo, as Android does
- webOS: the clock has light cards in Light, and the screensaver keeps dark cards
- webOS: the focused tile's name clears the focus ring
- webOS: progress bars in focused menu rows are easier to see
- webOS: Clear logos use one ink, so Netflix stays readable
- webOS: menus hide the status pill and no longer show tiles through the panel
- webOS: the date, caption and news have a stronger shadow, and the screensaver dims the scene slightly
- webOS: a stalled wallpaper moves on to the next one
- webOS: the focus glow surrounds the tile, and the background glow shows only on Dark and Clear, as on Android
- LG: Miracast stays on, and LG's cloud link is fully stopped
- webOS: new Focus Style, Focus Glow, Wallpaper Tint, Apps per Row, One or Two Rows and Dock
- webOS: Home Mode replaces Effects, Flip is gone from Clock Style, and the passcode can be four arrows
- webOS: logos use their own sizes, and Color plates deepen so white logos reach 4.5:1
- Home Mode's Efficient shows still aerial pictures instead of moving wallpaper
- `docs/parity.json` tracks each feature on every build, and CI fails a UI change that doesn't update it

## 1.67-mh23 (2026-11-01)

- Fire OS: Home opens before Amazon's launcher after a restart, and the Home button returns to it
- Fire OS: Home's screensaver replaces Amazon's, and the menu no longer says "Not the default home screen"
- `tools/fire-reapply.sh` restores the setup after a Fire OS update, and Home's menu reminds you to run it
- New Downloader, Send Files and Appstore logos, and larger VLC, Disney+ and Paramount+ logos
- Windscribe shows its W mark, and logos can set their own size in `logos/manifest.json`
- The Wi-Fi icon and the menu's status line are larger, and "More Apps" clears the focus ring

## 1.67-mh17 (2026-10-31)

- Tile Style is now System, Clear, Dark and Color
- Dark shows one ink on every logo: white with a grey shade, no brand color
- New installs start on Color tiles and Frosted glass, and installed TVs keep their choices
- Glass is clearer in the middle, in Light most of all, and Light's secondary text is darker to match
- The focus glow is centered on the tile, and stays outside a Clear tile, off the logo
- The sheen on a Clear tile goes under the logo
- Focus Style has Invert, which swaps a tile's logo and plate colors
- New Focus Glow (Small, Medium, Large) and Wallpaper Tint (Off, Dim, Warm, Cool)
- Input tiles follow the tile style, so Clear no longer shows them in color
- Clear tiles no longer sit on the glass plate behind the top row
- Home Mode is Auto, Full and Efficient, with Custom once something is set by hand; the Effects row is gone
- Removed the upstream update check and installer, the icon library download, and the permissions only they used
- Clock card and destructive colors moved into the palette; Control Center spacing tidied

## 1.67-mh16 (2026-10-31)

- Colored logos keep their colors: Plex's x and Peacock's dots in Full Color
- VLC and Play Store have clean redrawn art for the private pack, and Netflix is red on Dark Glass
- Logos shrink in steps and icons draw at double size, for smooth edges
- `build_pack.py` no longer adds an ink to art that has its own colors

## 1.67-mh15 (2026-10-31)

- Settings tile uses an Android-style gear with a lit hub
- Clear tiles use a real glass layer on Android 13 and newer when Glass is set to Liquid
- Full Color plates are deepened automatically so white logos reach 4.5:1
- `scripts/04-disable-stock-launcher.sh` disables Google TV's launcher safely
- Fixes from UI/UX audits: focused Clear tiles, panel contrast, slider height, labels

## 1.67-mh12 (2026-10-31)

- Clear logos are now strictly one ink and one shade, with no brand color
- Tile style rules written down in `android/DESIGN.md`
- Logo list expanded with ESPN, FOX, Tubi, Pluto TV, Sling, YouTube TV, The Roku Channel and more
- Neutral plates for Play Store and Settings, and baked diagonal shadows removed

## 1.67-mh11 (2026-10-31)

- New Clear tile style, in Light and Dark
- Full Color and Dark Glass now show the same logo shape
- Tall logos are capped so they match wide ones
- The focused tile glows in the app's color
- webOS has the same tile styles, logo sizing and glow in its source

## 1.67-mh10 (2026-10-31)

- Logos stay the same size and position in Dark Glass and Full Color
- Logo pack now works in Full Color, with brand plates and ink colors
- `logos/build_pack.py` builds a private pack from Simple Icons or traced art
- Dark tiles use an app's monochrome icon on Android 13 and newer
- Clock styles are now Cards and Plain
- Lower memory: tiles share plates, and modern Android uses the normal heap

## 1.67-mh9 (2026-10-31)

- The app and phone page now call themselves Home, except for Aerial film names
- `install.sh` needs `BUILD=1` or `APK_FILE`, and no longer downloads upstream Aerial
- Install, restore, report and selftest tools default to this app
- Bug reports point to this repository
- Backup and launcher scripts work without a private APK folder
- New getting started, safety and troubleshooting guides
- Device support table corrected: Hisense debloating doesn't last

## 1.67-mh8 (2026-10-31)

First public layout of TV CleanSlate.

- Android Home based on Aerial 1.67
- Personalize panel regrouped, with new type scale and motion
- Liquid glass on the dock, buttons and sliders
- Tile Style with a System option
- Home Mode: Full, Custom and Efficient, measured on real hardware
- Spoken labels for screen readers
- webOS Home, device kit, debloat guides and device logs

Detailed history: [android/FORK.md](android/FORK.md).
