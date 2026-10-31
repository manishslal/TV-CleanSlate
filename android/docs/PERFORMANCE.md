# What Home costs

Measured on the onn 4K Streaming Box (2 GB RAM, Android 14, 1080p UI), 2026-10-30, release builds compiled with `compile -m speed`. Each run: force-stop, start Home, 22 s to settle, 10 focus moves, Control Center opened twice. Memory is `dumpsys meminfo` TOTAL PSS, frames are `dumpsys gfxinfo`, CPU is `top` for the Home process with 400% as all four cores. Runs on the same settings differ by about ±5 MB. The SHIELD and TCL have not been measured.

## Home Mode, three runs each

| | Full | Custom (still photo, Liquid, Videos saver) | Efficient |
|---|---|---|---|
| Home, idle | 227 MB | 158 MB | 141 MB |
| Control Center open | 274 MB | 167 MB | 149 MB |
| Screensaver running | 236 MB | 186 MB | 146 MB |
| CPU, Home idle | 57% | 0% | 0% |
| CPU, screensaver | 53% | 81% | 0% |
| Typical frame, focus moving | 82 ms | 25 ms | 14 ms |
| Slow frames | 77% | 9% | 3% |

Against Full, Efficient saves 86 MB at idle and 125 MB with a panel open. Full plays a film behind Home, so most of its cost is the film.

## One setting at a time

| Setting | Effect |
|---|---|
| Moving wallpaper: Aerial film | +100 MB, about 60% CPU, most frames slow |
| Moving wallpaper: owner's videos | +185 MB, about 40% CPU, 42% slow frames |
| Effects: Lite | -14 MB, flat panels; also turns off the Dark Glass glow |
| Dark Glass glow | About doubles frame time (25 ms against 14 ms). No memory cost |
| Liquid against Frosted | No memory saved. Frosted is smoother with a panel opening: 8% slow frames against 5%, 90th percentile 47 ms against 28 ms |
| Top row glass off | -6 MB. Not used by Efficient, because it changes the look of Home |
| Clock style | No measurable effect |
| Headlines | No effect on Home; they show over the screensaver |
| Tile Style | No memory effect once the glow is off |
| Screensaver: Clock | 144 MB, 0% CPU |
| Screensaver: Videos | 183 MB, 78% CPU |
| Screensaver: Aerial | 199 MB, 35% CPU |
| Screensaver: Photos | 199 MB, 64% CPU |

## What each mode sets

- **Full:** Liquid glass, Effects Full, Aerial film wallpaper, Aerial screensaver.
- **Efficient:** Frosted glass, Effects Lite, a moving wallpaper replaced by the daily photo (a still picture stays), Clock screensaver.
- **Custom:** what the owner set. Changing any of those four by hand makes the mode Custom.

## Repeating it

ADB, needs no app permission beyond the shell's:

```bash
adb shell am broadcast -n io.manish.home/app.orchard.tv.guard.ConfigReceiver -a app.orchard.tv.CONFIG --ez show_look true
adb shell am broadcast -n io.manish.home/app.orchard.tv.guard.ConfigReceiver -a app.orchard.tv.CONFIG --es home_mode efficient
```

Press OK only after reading that focus is where you expect (`uiautomator dump`): a stray OK opens an app, and Back on Home can start the screensaver.

## Limits to keep

On a 2 GB TV, in Efficient: Home under 150 MB idle and 160 MB with a panel open, 0% CPU idle, 5% slow frames or fewer. Measure again after any change to glass, tiles or wallpapers.
