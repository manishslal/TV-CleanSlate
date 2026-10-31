# webOS Home

The TV CleanSlate look as a web app for a **rooted** LG TV. Tested on an LG 50UP8000PUA with webOS 6.5.3. See its [device log](../devices/lg-50up8000pua/CHANGELOG.md).

webOS doesn't let apps replace Home, so this runs on [CleanYourTV](https://github.com/Gathin23/cleanyourtv)'s Home takeover. The TV was rooted with [SlopBro](https://github.com/throwaway96/slopbro) and [Homebrew Channel](https://github.com/webosbrew/webos-homebrew-channel). Follow those projects' own instructions. Rooting is at your own risk.

| File | Purpose |
|---|---|
| `index.src.html` | The app, with placeholders filled at build time |
| `build.py` | Builds `index.html` from the Android app's wallpaper list, icons and topics |
| `bake_frost.py` | Bakes blurred frames, because webOS video can't be read or blurred by a page |
| `home-svc.js`, `12-home-svc` | Local service for volume, network, news and power-on target |
| `16-lg-us-ads` | Blocks LG's US ad and telemetry hosts |
| `17-lg-slim` | Stops services this TV doesn't need |
| `20-home-on-power` | Opens Home on reboot and wake |

## Build

```bash
python3 webos/bake_frost.py   # writes frost/ and frost.json; needs ffmpeg and Pillow
python3 webos/build.py        # needs Pillow and your own logos in logos/dark/
```

`index.html`, `frost/`, `frost.json` and `logo/` are generated and not committed. Deployment steps and how to undo them are in the device log.
