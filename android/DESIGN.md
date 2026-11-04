# Home design standard

For Android Home on Android and Google TV. tvOS is the reference; it deliberately does not follow Material. Cost figures are in `docs/PERFORMANCE.md`.

## Canvas and type
- 960×540 dp canvas. Text sizes are 12, 14, 16, 20 and 26, from `Type`. Nothing smaller than 12 except the VPN badge.
- A setting's name is 16, its value 14, a note 12.
- Title Case for names and values.

## Color
- Colors come from `Palette`. Words reach 4.5:1 on the panel behind them.
- Light and Dark are designed together: a change is checked in both.

## Glass
- Liquid Glass needs Android 13. Older TVs get frosted glass with a lit edge.
- Real lens: panels, the top row, Control Center's round buttons and sliders, the status pill's focus.
- Lit rim only (`glassRim`): anything too small for bending to show, such as switches.
- The middle of a panel shows the frosted picture with 30% of the sharp one on Liquid. Light's floor is 56% grey, so dark text holds on a black wallpaper.
- Keep the bend narrow: 26 dp on panels, 16 on the top row, 8 to 10 on controls.
- Lite draws flat panels.
- Light glass shows 58% of the picture; the white added brings its average to 204, between 96 and 150 (`lightPanelTint`). Groups and controls on a light panel are white cards (`Palette.Section`), so dark text sits on white, not on the glass.
- Home's own controls on the wallpaper (the status pill's focus) use the dock's clear glass (`liquidGlass(clear = true)`), with white ink.

## Tiles
- A logo keeps one size and position on every tile style. Only the plate and sometimes the ink change.
- Logos share one area, inside a box 70% of the tile's width and 50% of its height.

| Style | Plate | Logo |
|---|---|---|
| Clear | Glass like the dock, bright in Light and dim in Dark | One ink plus one shade. Color becomes the shade, so no brand color shows |
| Dark | Dark graphite | One ink plus one shade: white and grey, no brand color |
| Color | The brand's color | White or the brand's ink, whichever reads |

- Order of logo sources: the pack, the app's monochrome icon on Android 13+, then a logo cut from its icon.
- Banners and baked app art are not used, because their logos have their own size.
- Pack art is at least 1024 px on its long side and is shrunk in steps. A logo that matches its plate gets a neutral plate.
- Firmware apps with no usable logo use Home's own glyph.
- A focused tile glows evenly around it in the app's own color. Focus Glow sets how far; Small is the ring alone.
- On Clear the glow stays outside the tile, and the sheen goes under the logo, so nothing covers the logo.
- Clear tiles sit directly on the wallpaper: no glass plate under glass tiles.

## Focus
- Plate in panels: a solid light or dark plate.
- Ring on tiles, or Invert: a solid plate with the logo's colors swapped.
- Glass on Home's own controls, where it has the wallpaper to bend.

## Controls
- One line and one focus stop per setting, 44 dp tall.
- On or off: a switch. OK flips it.
- Two to four values: a value row. Left and Right change it live. OK does nothing with a remote.
- Five or more, or open-ended: a row that opens a list.
- Every control has a role and a name for screen readers.
- To check a name, read the nodes inside the focused one in `uiautomator dump`: the focused node itself shows empty.

## Personalize
- Three groups: Look, Home Screen, Apps and Access. Each row shows its current value.
- Notes sit at the panel's foot.

## Clock
- Clock cards follow Mode on Home: white with black type in Light.
- The screensaver's cards stay dark in both modes.
- Sizes: Small (one line), Large (104 sp time), Huge (the time 1.5x, date and weather 1.25x). Positions: Auto, Top Left, Top Center, Top Right. Auto takes the calmest of the three on each picture (`Images.clockSpot`); a bright spot there deepens the shade by a third.
- The wallpaper's shade lies behind the clock wherever it stands (`Images.clockShade`), and is part of the baked backdrop's key.
- Depth: with a cut-out service (`AerialSettings.depthServer`), a still wallpaper's subject is drawn over the time cards only, never the date or weather, when the cut-out is firm (0.7), covers 3-35% of the time and no digit-wide slice by more than half (`DepthOverClock`).
- The screensaver's time is 44, 60 or 80 sp by clock size, over a soft dark pool. Its credit is one 12 sp line at the bottom left; the headline ends on the same line.

- Tile Style: System follows Mode, Clear, Dark, Color. New installs start on Color and Frosted.
- A Dark logo comes from the pack when there is one. Logos are personal use and stay out of public builds.

## Motion
- Durations come from `Motion`: Fast 120, Quick 160, Enter 220, Panel 300 ms. Exits are quicker than entrances.
- Nothing runs while idle.

## Home Mode
- Full, Custom, Efficient, in Control Center under Personalize.
- A setting belongs to a mode only if measuring shows it matters.
