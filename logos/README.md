# TV logo pack

Logos for dark tiles, shared by every Home launcher. Each brand has one file, and `manifest.json` maps it to that app's ID on each platform.

- `dark/` holds trimmed masters: light logos on transparent backgrounds, kept at full size.
- To add a logo: put the file in `dark/`, add its brand to `manifest.json` with the app IDs, and rebuild.
- LG webOS: `webos/build.py` exports each logo at 504 x 276 at most, as `logo-<brand>.webp`.
- Android/Google TV: `android/app/build.gradle.kts` copies the PNG and WebP masters and `manifest.json` into the build, only into `build/`. Android has no SVG support here, so Spotify keeps its processed icon.

These are trademarks, for personal use on the owner's TVs. Don't publish them with a public fork.

Sources: the ten PNG and WebP logos were supplied by the owner on 2026-10-30. `spotify.svg` is from Simple Icons (CC0).
