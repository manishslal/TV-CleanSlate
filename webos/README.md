# webOS Home

The TV CleanSlate look as a web app for a **rooted** LG TV. Tested on an LG 50UP8000PUA with webOS 6.5.3. See its [device log](../devices/lg-50up8000pua/CHANGELOG.md).

webOS doesn't let apps replace Home, so this runs on [CleanYourTV](https://github.com/Gathin23/cleanyourtv)'s Home takeover. The TV was rooted with [SlopBro](https://github.com/throwaway96/slopbro) and [Homebrew Channel](https://github.com/webosbrew/webos-homebrew-channel). Follow those projects' own instructions. Rooting is at your own risk.

## Risks and undo

Read this before rooting a TV for it. It records what was done on one LG; it isn't a recommendation to root yours.

- **The root can't be taken back.** It uses an exploit in LG's software. LG may refuse support for a rooted TV, and installing LG's own Developer Mode app on a rooted TV breaks the root.
- **Blocking updates is a trade-off.** The tested TV blocks LG's updates in Homebrew Channel so an update can't remove the root. That also stops LG's security fixes. Turn it off in Homebrew Channel's settings to get updates again.
- **Home itself comes out cleanly:**
  - Switch off CleanYourTV as Home in its app, or run `sh /media/developer/cleanyourtv/undo-home.sh`, to get LG's Home back.
  - Each start-up hook below is one file: `rm /var/lib/webosbrew/init.d/<name>`, then restart the TV. `12-home-svc` also needs `/media/developer/cleanyourtv/home-svc.js` deleted.
  - CleanYourTV's own debloat is undone in its app (Debloat), and the app removes itself fully from there.
- Every step taken on the tested TV, with its undo, is in the [device log](../devices/lg-50up8000pua/CHANGELOG.md).

| File | Purpose |
|---|---|
| `index.src.html` | The app, with placeholders filled at build time |
| `build.py` | Builds `index.html` from the Android app's wallpaper list, icons and topics |
| `bake_frost.py` | Bakes blurred frames, because webOS video can't be read or blurred by a page |
| `home-svc.js`, `12-home-svc` | Local service for volume, network, news, Bing's photo list, power-on target, and holding LG's own screensaver off while Home's is on |
| `16-lg-us-ads` | Blocks LG's US ad and telemetry hosts |
| `17-lg-slim` | Stops services this TV doesn't need |
| `20-home-on-power` | Opens Home on reboot and wake |

## Build

```bash
python3 webos/bake_frost.py   # writes frost/ and frost.json; needs ffmpeg and Pillow
python3 webos/build.py        # needs Pillow and your own logos in logos/dark/
```

`build.py` also copies the interface sounds (`ui_*.wav`, made by `tools/make-sounds.py`) next to `index.html`; deploy them with it. `index.html`, `frost/`, `frost.json`, `logo/` and the sounds are generated and not committed. Deployment steps and how to undo them are in the device log.

The daily pictures (Bing, NASA Earth Observatory, Wikimedia, Google Earth View) are fetched by the page. Bing's list does not allow web pages, so on LG the page asks `home-svc.js` (`/bing`); deploy the updated `home-svc.js` along with the page.
