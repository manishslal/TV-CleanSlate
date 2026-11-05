# TV logo pack

Logos for dark tiles, shared by every Home launcher. Each brand has one file, and `manifest.json` maps it to that app's ID on each platform.

- `dark/` holds trimmed masters: light logos on transparent backgrounds, kept at full size.
- To add a logo: add its brand to `manifest.json` with the app IDs, then either put a file in `dark/` or name a source and run `build_pack.py`.
- `commons` names a Wikimedia Commons SVG, flattened to white. Check each result: a logo built from a filled shape turns into a solid block.
- `simple` names a [Simple Icons](https://simpleicons.org) SVG. `trace` names a picture to trace with [VTracer](https://github.com/visioncortex/vtracer). Rendering uses [resvg](https://github.com/linebender/resvg).
- `plate` is the tile color in Color. `ink` recolors a one-color logo there. Leave `ink` out to keep a logo's own colors.
- `size` scales a logo that reads small at equal area. For example, `1.25` suits a thin script or a round mark. Android only.
- LG webOS: `webos/build.py` exports each logo at 504 x 276 at most, as `logo-<brand>.webp`.
- Android/Google TV: `android/app/build.gradle.kts` copies the PNG and WebP masters and `manifest.json` into the build, only into `build/`. SVGs must be rendered to PNG first.

## Logos we still need

Apps seen on a TV with no logo in the pack. Add to this list whenever a tile falls back to plain text or the app's own icon.

| App | Seen on | IDs |
|---|---|---|
| IPTV Smarters Pro | Samsung UN50MU6070 | Tizen `rKpKZvmSog.IPTVSmartersPro` |

## Making your own logo

In Photoshop or any editor, export the mark white on transparent, cropped tight, at least 1024 px on its long side, as `dark/<brand>.png`. Keep it flat, with no shadow or plate. Add the brand to `manifest.json` with no source field, so the build doesn't replace your file. Clear turns any color in it into a gray shade automatically.

These are trademarks, for personal use on the owner's TVs. Don't publish them with a public fork.

Sources: the ten PNG and WebP logos were supplied by the owner on 2026-10-30. `dark/spotify.svg` is from Simple Icons (CC0).
