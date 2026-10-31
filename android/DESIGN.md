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
- Keep the bend narrow: 26 dp on panels, 16 on the top row, 8 to 10 on controls.
- Lite draws flat panels.

## Focus
- Plate in panels: a solid light or dark plate.
- Ring on tiles.
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

## Tiles
- Tile Style: System follows Mode, Dark Glass, Full Color.
- A Dark Glass logo comes from the pack when there is one. Logos are personal use and stay out of public builds.

## Motion
- Durations come from `Motion`: Fast 120, Quick 160, Enter 220, Panel 300 ms. Exits are quicker than entrances.
- Nothing runs while idle.

## Home Mode
- Full, Custom, Efficient, in Control Center under Personalize.
- A setting belongs to a mode only if measuring shows it matters.
