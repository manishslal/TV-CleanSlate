# Changelog

## Unreleased

- Credits: CREDITS.md and NOTICE.md now cover every bundled font copy, sound, data file and certificate, and credit glisse (numbers only) and the tv-preview branch `SourceWallpaper.kt` is adapted from; Inter's license file carries the font's own 2016 copyright line. The README gains status and workflow badges, an FAQ, and NASA, Wikimedia and Google Earth View in the Privacy table
- Checks: a `checks.yml` workflow runs on every pull request and weekly: relative Markdown links (lychee), ShellCheck over every shell script, ruff and `py_compile` over the Python, the webOS and Tizen page builds (which now work without the private logos) with a check that their scripts parse and use no ES6, and a check that every repo path named in the docs exists. It fixed the findings: quoting and unused-variable notes in the ADB scripts and TV init scripts (behaviour unchanged), a chained exception in `tizen/tvkey.py`, and stale paths and one dead link in the docs. All third-party actions are pinned by commit SHA at their current majors
- Sharing: the README opens with "Before you start" (what each path risks and how to undo it, who this is for and not for), device logs say they record one owner's choices rather than give instructions, and the agent debloat prompts say to watch the agent and approve the list first. The scripts that change a TV now say what they will do and wait for a yes (`YES=1` skips it); the restore script still never asks
- GitHub: bug and idea forms, a pull request template, a code of conduct, Dependabot for the workflows and the Android build, secret scanning with push protection, and a rule that keeps `main` from being force-pushed or deleted. Aerial's copied READMEs link to the right files again (80 links), and a compiled Python file is no longer in the repository
- Web: the dock is the SHIELD's frosted plate in Android's own numbers, measured side by side on the same Bing picture: the frost fully over a still picture and at 55% over a moving film (FILM_FROST), the frost x0.84 + 16 (PlateTint) with the lift dimmed along with the wallpaper, corners of 32 dp (64 px, were 36), glisse's 1.22 dp rim at 60% of its shine with its 0.5 dp edge, and no shadow under it (there was one). Before, the dock was lifted with a grey veil and let the sharp picture through, as the onn's lens does, and read paler and less warm than the SHIELD's. The frost itself was already as soft as Android's
- LG: the TV's own screensaver (a clock on black) no longer comes on over Home's. The helper answers the TV's screensaver request with no while Home is in front and Home's screensaver is on; over another app, or with Home's set to Never, the TV's starts as before
- Web: the wallpaper's dim (and Warm or Cool) lies over its frost as well, so the dock is as dim as the picture around it, as on Android; the app's glow stays above it
- Web: a still wallpaper (Bing, NASA, Wikimedia, Earth View, the built-in stills, Plex photos) is frosted under the dock and behind the app grid, as on Android; only the aerial films had frost, so the dock showed the sharp picture and the grid a near-solid grey sheet. Clear tiles keep the frost at half strength, as Android's dock
- Web: the app grid behaves as Android's: the focused row rests second from the top with the row above in view, the clock leaves and the time joins the pill, and "More Apps" titles the first row under the shelf
- Samsung: starts on Efficient (still pictures, no focus glow), and a TV that had Home before switches once; Full can still be chosen
- LG: LG's voice app (the remote's mic button) is taken out by `17-lg-slim`, at the owner's request
- Web: a logo on a black or grey plate (Netflix, Peacock) glows in its icon's color, as on Android, not white; Netflix and Plex have their glow colors in `logos/manifest.json`. Control Center's Network row shows the Wi-Fi name, as the pill does, instead of the TV's address
- Android 1.67-mh37 and web, fixes from a code review: the subject over the clock no longer stays from the last picture (on Follow App, or while a new picture saved); a cut-out is kept by its picture alone, so moving the clock or updating does not ask again; the cut-out service is reached only at an address that resolves to the home network, and its answer must be whole; Every Source no longer counts Bing several times, and the web Home has Bing (Bing Daily, and in Every Source in Android's order); Bing's new photo no longer waits for the other collections; a picture that fails on the web gives way to the next. The cut-out service follows redirects only to Home's picture sources, works on two pictures at most, writes its files whole and runs as its own user
- Android 1.67-mh37 and web, fixes from a design review: the shade behind the clock is as deep as white type needs on that picture (4.5:1), across the clock's width; the screensaver's news has its own soft shade; the focused status pill has a lit rim and a soft shadow; Control Center's rows share one card; a centred clock puts its hours and minutes on the axis with AM/PM to the side; the subject is drawn over the clock only when it rises at least a fifth into the time, with a soft shadow on the cards; the picker's names are 14 sp, Every Source shows its sources, and the grid fades at its foot
- Android 1.67-mh37 and a home server: the wallpaper's subject can stand in front of the large clock, as on a phone's lock screen. A small cut-out service (`server/cutout`, Docker, ISNet on the CPU) answers each picture with its subject's mask; Home draws the subject over the time only when the cut-out is firm (no haze), reaches the time, covers 3-35% of it and hides no digit by more than half. The date and weather stay in front. Off unless a TV is given the service's address
- Android 1.67-mh37 and web: three free picture collections beside Bing's photo of the day, each a new picture daily: NASA Earth Observatory's Image of the Day, Wikimedia Commons' Picture of the Day (credited to its author and licence) and Google Earth View (one of its 1,520 landscapes a day, the same on every TV). "Every Source, Daily" takes a different one each day, and the photo screensaver moves through them. NASA's own Picture of the Day API is not used: in 2026 it answers every day with a placeholder logo
- Android 1.67-mh37 and web: Clock Position has Auto, which reads each wallpaper and puts the clock where the picture is calmest (least detail and contrast, not bright), and deepens the shade behind the clock wherever the picture is bright there. On the web, Earth View's pictures cannot be read, so Auto keeps the clock at the left on them
- Android 1.67-mh37 and web: Home's own calm sounds, made by `tools/make-sounds.py`: soft mallet tones in D major pentatonic, eased in so nothing clicks. The error is a low, muted two-note fall instead of a buzz. They are the default on Android (the TV's own sounds stay a choice), and Samsung now ships them (it had none)
- Android 1.67-mh37 and web: Clock Size has Huge (the time 1.5 times the large clock) and Position has Top Center. The wallpaper's shade behind the clock moves with it and deepens for Huge; before, it stayed on the left when the clock went right
- Android 1.67-mh37 and web: the screensaver's credit is one small line at the bottom left, the news headline sits on the same line with its source above it, and the clock is larger (it follows the clock size) with a soft dark pool and shadow behind it
- Android 1.67-mh37 and web: light menus are grey see-through glass with white section cards. The sections were drawn at 5% white, which vanished on light glass, and the light glass was a fixed 52% white that hid the picture. On Android the white added now follows the picture's brightness
- Android 1.67-mh37 and web: the status pill's focus is the dock's clear glass with white ink, not a panel's solid tint
- Android 1.67-mh36: a TV short only of memory (Fire TV Stick 4K: 1.3 GB, four cores) keeps the focused tile's glow, tilt and sheen on Auto while the wallpaper is still, and the Focus Glow row with them. Measured there: 1.5-2.5% janky frames moving across the apps on a still picture with these on, against 25-40% with a moving wallpaper whatever the effects. A moving wallpaper, Efficient, Lite, and weaker TVs (low-RAM flag, Android 6 or older, two cores) still drop them
- Android 1.67-mh35: the status pill is smaller (14 pt name, 18 dp icon), below the time beside it, and says "Wi-Fi" when Android withholds the network's name
- Android 1.67-mh35: Turn Off TV in Control Center wherever Home Guard runs (standby on Android 9+, else the TV's power menu)
- Android 1.67-mh35: Aerial categories (Landscapes, Cities, Space, Underwater), as the web Home has them
- Android 1.67-mh35: Android 7 TVs (Fire TV Stick 4K) get Inter instead of Roboto; Focus Glow is hidden where Lite draws none
- Web: rename a folder, and hold OK on an app in a folder to move it out or hide it; offline reads "Not Connected" as on Android
- Samsung: Inputs, Brightness and When TV Turns On are gone from Control Center, where they did nothing; the screensaver clock drifts again; Plex sees a Samsung TV
- Parity ledger: corrected Turn Off TV and Peek, and added 13 features it did not list
- Android 1.67-mh35: on Fire TV, Home's text is the same size as on Google TV and SHIELD. Fire OS ships its text size at 1.0 where they ship 0.85, so everything (most visibly the Wi-Fi name) drew 18% larger
- Android 1.67-mh34 and web: every app's focused tile glows. Black-and-white and grey apps glow white (Apple TV), and dark brand colors are lifted to a bright one of the same hue (Windscribe's navy to blue). A logo can set its glow color in `logos/manifest.json` (HBO Max: purple)
- Android 1.67-mh32: a moving wallpaper slows to a stop when a menu opens and gathers pace when it closes, instead of freezing and jumping
- Android 13 and later: the liquid glass stays liquid while its picture changes, fading from the old picture to the new over 0.9 s, where it used to drop to flat glass and pop back
- Android: the status pill shows the Wi-Fi name, as the web Home and Lite do
- Android 1.67-mh31 and web: fixes from a glass review: the dock no longer stays half-visible in the grid and screensaver with Clear tiles, the rim fades cleanly at its corners and follows the appearance on an input's plate, Dark tiles keep their dark rim in Light, and Clear tiles in Light read better over dark pictures
- Android 1.67-mh30: the VPN button shows with the other buttons instead of a second or two later, and only once a VPN has been connected on the device
- Android 1.67-mh29 and web: Clear tiles have a dock again, at half strength, and the Dock switch is in Appearance for every tile style
- Android and web: Wallpaper Tint is kept for each appearance; Dark starts on Dim instead of dimming on its own, so the two no longer stack, and Dim can be turned off or changed
- Android 1.67-mh28: the VPN button turns blue while the VPN is on and shows the app's own mark like the other icons; Inputs is hidden on boxes and sticks
- Android: Bluetooth opens Remotes & Accessories on Google TV and Android 12 TVs, and the pairing search on older ones such as the SHIELD
- Android and web: glass follows glisse's numbers (tint, saturation, rim, outline, press springs)
- Android and web: Light is clearer and less bright, and dims the wallpaper slightly as Dark does
- Android 1.67-mh26: Control Center's round buttons are now Wi-Fi, Bluetooth, Settings, your VPN app with its own logo, and an Auto, Dark or Light button; Inputs moved to its own row
- Android: the brightness row is hidden on boxes and sticks, where it moved nothing
- webOS and Samsung: Control Center has an Appearance row; the dock and panels have a lit rim
- `docs/research/liquid-glass.md`: what Home can take from glisse without a heavy GPU
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
- `android/tools/fire-reapply.sh` restores the setup after a Fire OS update, and Home's menu reminds you to run it
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
