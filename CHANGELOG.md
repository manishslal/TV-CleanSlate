# Changelog

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
