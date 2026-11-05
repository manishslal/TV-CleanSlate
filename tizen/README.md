# Samsung Tizen Home

Home runs on the two TVs below and opens apps with the real remote. See [supported devices](../docs/SUPPORTED_DEVICES.md). Community reports below are leads, not proof.

Samsung TVs run Tizen, not Android, so Android Home can't be installed. Tizen installs signed `.wgt` web apps, so this port starts from the [webOS Home](../webos/README.md) page.

## Tested TVs

| | First target | Second TV |
|---|---|---|
| Model | UN50MU6070 (2017, 4K) | UN55NU6900 (2018, 4K) |
| Platform | `17_KANTM_UHD_BASIC` | `18_KANTSU_UHD_BASIC` |
| Tizen | 3.0 | 4.0 |
| RAM | 1.36 GB | 1 GB |
| Build | Full | `LEAN=1`: still wallpaper, no frost |

The 2017 engine is Chromium M47, with no CSS variables, grid or unprefixed `filter`.

An app can't register the Home button, so Smart Hub always opens first. See [The Home button](#the-home-button).

| File | Purpose |
|---|---|
| `build.py` | Builds `build/` from webOS Home, compiling away CSS this engine lacks; needs Pillow and your own logos in `logos/dark/` |
| `adapter.js` | Tizen versions of the calls webOS Home makes to its local services |
| `deploy.sh` | Packages `build/`, installs it and opens it |
| `debug.js` | With `DEBUG_HOST=<computer-ip>`, reports errors and page state to a local receiver |
| `aerial.js` | Plays Apple's `.mov` files through Media Source |
| `homekey/` | A test package carrying a Home key rule, after [samsung-tizen-homekey](https://github.com/gtalusan/samsung-tizen-homekey). It did nothing on Tizen 3.0 |
| `tvkey.py` | Sends remote keys to the whole TV, after you press Allow on it once |
| `preflight.sh` | Reads the TV's details and changes nothing |
| `tvshot.py` | Screenshots Home, runs JavaScript in it and sends it keys, through the web inspector |

```bash
python3 tizen/build.py
TV=<tv-ip> tizen/deploy.sh
```

Every Samsung build starts on Efficient (still pictures, no focus glow), and a TV that had Home before switches to it once; Full can still be chosen in Control Center. `LEAN=1 python3 tizen/build.py` makes a smaller build for a TV short on memory and storage as well: still wallpaper from `python3 webos/bake_stills.py` and no frost.

`deploy.sh` needs Tizen Studio's CLI, a `home` security profile, and Rosetta on Apple Silicon. A free author certificate was accepted on this 2017 TV, so no Samsung account was needed.

## What the TV allows

| Goal | No root | Needs a firmware exploit or root |
|---|---|---|
| Install Home | Yes, through Developer Mode | |
| Open Home at power-on | Reported, with Autorun Last App | Reported, with `db/appfw/startup_appid` |
| Open apps and HDMI inputs | Yes, with `tizen.application.launch` and `tizen.tvwindow` | |
| Stop Smart Hub at power-on | Yes, with Autorun Smart Hub off | |
| Block ads and telemetry | At the router or DNS | Reported, on specific firmware |
| Send the Home button to Home | Partly: Home reopens over Smart Hub, on Tizen 4.0 so far | Reported, with `db/appfw/home_appid` |
| Remove downloaded apps | Yes, with the numeric ID | |
| Disable Samsung's own apps | No | Reported, on specific firmware |

Samsung's documentation doesn't offer a way to replace Smart Hub.

On the first target, `vd_appuninstall` refused AdSuite, Sponsored Video and Ad Player with error 132. It removed apps by the numeric ID from `vd_applist`, even after refusing the package ID. See the [device log](../devices/samsung-un50mu6070/CHANGELOG.md).

## Aerial video

On the first target, 113 of 113 of Apple's videos started through Home's own clip change, with a median of 1.4 seconds. `aerial.js` fetches each file with `XMLHttpRequest` and rebuilds it for Media Source, because the TV's media player can't reliably reach hosts with IPv6 records and its `<video>` player stalls on 10 Mbps 1080p. Apple's host fails over HTTPS from the browser side, so `build.py` uses its HTTP address. See [the research](../docs/research/samsung-older-tvs.md).

## The Home button

Smart Hub takes focus without hiding Home's page, so Home notices and reopens itself, about a second later on both TVs. This only works while Home is on screen. Inside another app, Home's page is frozen and Smart Hub stays. There, press Home, then reach Home's icon in the Recent row above Apps. [The research](../docs/research/samsung-older-tvs.md#the-home-button) covers a key-rule package that failed on Tizen 3.0.

The setting is **Home Button Returns Here**, under Screensaver, and it's on by default. Home skips this for 3 seconds after it opens an app, and stops after 3 reopens in 30 seconds so it can't loop.

Menu and Source also take focus, so Home takes those keys. Menu opens normally. No app can open the Source panel, so pressing Source once shows "Press Source again", and the second press reaches the TV. For that reason, there's no Inputs tile.

Home opens Netflix as `org.tizen.netflix-app`, because the store ID opened a black screen on the UN50MU6070.

## Remote keys

```bash
TV=<tv-ip> python3 tizen/tvkey.py KEY_RIGHT KEY_ENTER
```

The first run shows a prompt on the TV. Press Allow within 30 seconds, while the computer is connected; otherwise the TV closes the connection with `notack`. The token stays in `~/.cache/tv-cleanslate/`, one for each TV. Never commit it.

## Plan

1. **Check the TV.** Record the model, firmware and year, then run `preflight.sh`. It only reads.
2. **Build the no-root version.** Add a Tizen adapter, package a `.wgt`, and install it through Developer Mode.
3. **Set power-on.** Turn off Autorun Smart Hub, turn on Autorun Last App, and reboot to check.
4. **Block ads at the router.** This needs no change on the TV and survives a reset.
5. **Decide on the Home button.** The no-root reopen is built. An exploit-based route is optional and needs the owner's separate approval.

Later: walk the owner through blocking AdSuite, Sponsored Video and Ad Player at the router or DNS.

## Before the first run

The owner does these steps:

1. Note the exact model from the label or Settings, and the software version.
2. Open Apps, go to App Settings, and enter `12345`.
3. Turn Developer Mode on, enter the computer's IP address, and reboot the TV.
4. Create a free Samsung account for the signing certificate. Don't share the password; sign in yourself.
5. Install Tizen Studio with its TV extension and Certificate Manager. The certificate is tied to this TV's device ID.

```bash
TV=<tv-ip> tizen/preflight.sh
```

Screenshots show Home's page but not the video behind it:

```bash
TV=<tv-ip> python3 tizen/tvshot.py shot home.png
```

`tvshot.py` opens Home in debug mode, which leaves an unprotected port open on the local network until Home restarts. Close it with `DELETE`, then `POST`, on `http://<tv-ip>:8001/api/v2/applications/CSHome00Te.Home`. Tizen 3.0 needs a timeout argument on `sdb shell 0 debug` and Tizen 4.0 refuses one, so the tool tries both. Turn Developer Mode off when you aren't working on the TV. A reported flaw (SVE-2025-50109) is reachable on the local network while it's on.

## Porting from webOS

`webos/index.src.html` reaches the TV through `api()` and `svc()`, which call local services. On Tizen, these become direct calls in the page:

| webOS | Tizen |
|---|---|
| `/api/launch` | `tizen.application.launch` |
| App list from CleanYourTV | `tizen.application.getAppsInfo` |
| Inputs from CleanYourTV | `tizen.tvwindow` and `tizen.tvaudiocontrol` |
| `home-svc.js` volume, network and power target | `tizen.tvaudiocontrol`, `webapis.network`, Autorun Last App |
| `home-svc.js` news | Direct requests, if the app's access policy allows them |
| `webOSRelaunch` | `visibilitychange` and app control events |
| Remote keys | `tizen.tvinputdevice.registerKey` |
| Baked frost | Check whether Tizen video has the same limit |

`logos/manifest.json` needs Tizen app IDs for each brand, taken from the real TV, because IDs vary by model and region.

## Risks to weigh

- Rooting can void support, can leave the TV unusable, and depends on one model and firmware.
- A reported root on a 2026 Tizen 9 TV is lost at reboot, and a computer has to run it again.
- Blocking updates to keep root would break this project's rule and stop security fixes. Update packages stay untouched.
- A factory or Smart Hub reset restores Samsung's apps and menu settings.

## Leads

- [Samsung: connecting a TV](https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/tv-device.html)
- [cleanyourtv-samsung](https://github.com/smartjaganrao/cleanyourtv-samsung): no-root web app, Autorun Last App and DNS blocking
- [samsung-tv-launcher](https://github.com/ferdj40/samsung-tv-launcher): a plain web app home screen
- [Replacing Smart Hub](https://llmgateway.io/blog/hack-samsung-tv-home-screen-with-ai): Home button keys and why a helper app is needed
- [tizen-tv-control-center](https://github.com/woo-jobradovic/tizen-tv-control-center): a rooted Tizen 9 setup
- [samsung-tv-tizen-root](https://github.com/DemonKingSwarn/samsung-tv-tizen-root): root research and its risks
