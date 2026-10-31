# Bringing the LG work to Android Home

One Home design across all devices. Android Home (`io.manish.home`, in `android`) and the LG webOS version (`webos`) should look and behave the same wherever the platform allows.

## Move from LG to Android Home

Android and Google TV keep their own design. The LG work is inspiration, not a spec (user decision, 2026-10-30). Taken across: Personalize in a panel on the right, the logo pack for dark tiles with equal logo weight, and App Tiles separate from Appearance. Android's Control Center, pill, tile layout, spacing and colours stay as they are. The table below is the original list, kept for reference.

| Item | LG reference | Android work |
|---|---|---|
| Right-side settings panel opened from a top-right pill | `.panel`, `#pill`, `pageControl`, `pagePersonalize` | Move Control Center and Personalize to match |
| Logo pack for dark tiles | `logos/manifest.json` (Android IDs included) | Use pack logos instead of processed icons, and keep the pack out of public builds |
| Equal logo weight | `art.onload` in `tile()`: 176 x 84 box, equal area | Size pack logos the same way |
| Appearance and App Tiles are independent | `tileGlass()`, `body.glasstiles` | Confirm Dark tiles work in Light |
| Colours follow the surface behind them | `--onwall`, `--onsheet`, `--ring`, `--lift`, `--cleartile` | White over wallpaper and dock in both themes, dark on the pale sheet in Light, white focus ring on tiles |
| Rearranging inserts instead of swapping | `shift()` | Confirm others slide along |
| Hold OK opens the tile menu with Move, Hide, folders, Uninstall | `tileMenu()` | Confirm it matches |
| Tiles centred in the dock, focused tiles clear their neighbours | Tile 252 x 142, gap 46, 1.15 lift | Compare spacing |
| Frost shows the wallpaper softly blurred in the dock and sheet | `#fclip`, `bake_frost.py` | Android bakes frost already, so compare the look and tint |

The LG-specific parts stay on LG: `home-svc.js`, the startup hooks, `bake_frost.py` timelines and everything to do with root.

## Rules learned

- Never run a command on a TV to find out what it does. A screen-off test blanked the LG.
- Disable only, never uninstall, on Android. Never touch input, remote, keyboard, Wi-Fi or update packages. Full list: [LOGISTICS-AND-SAFETY.md](LOGISTICS-AND-SAFETY.md).
- Measure on the device. Blur cost 150 ms a tile move on the LG, against 25 ms without it. CPU was idle, so background apps were not the cause.
- Video on webOS sits on a hardware plane that a page cannot read or blur.
- Check `luna-send` needs `</dev/null`, or it can hang with no output.
- Test on the real remote as well as with simulated keys.
- Log every change and how to undo it, per device.
- `android/local/` is never committed, and nothing is pushed.
