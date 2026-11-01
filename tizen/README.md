# Samsung Tizen Home

Planned. Nothing here has been checked on a Samsung TV yet, so Samsung isn't listed in [supported devices](../docs/SUPPORTED_DEVICES.md). Community reports below are leads, not proof.

Samsung TVs run Tizen, not Android, so Android Home can't be installed. Tizen installs signed `.wgt` web apps, so this port starts from the [webOS Home](../webos/README.md) page.

## What the TV allows

| Goal | No root | Needs a firmware exploit or root |
|---|---|---|
| Install Home | Yes, through Developer Mode | |
| Open Home at power-on | Reported, with Autorun Last App | Reported, with `db/appfw/startup_appid` |
| Open apps and HDMI inputs | Yes, with `tizen.application.launch` and `tizen.tvwindow` | |
| Stop Smart Hub at power-on | Yes, with Autorun Smart Hub off | |
| Block ads and telemetry | At the router or DNS | Reported, on specific firmware |
| Send the Home button to Home | No | Reported, with `db/appfw/home_appid` |
| Disable Samsung's own apps | No | Reported, on specific firmware |

Samsung's documentation doesn't offer a way to replace Smart Hub. The Home button is the main limit: without an exploit, it still opens Smart Hub.

## Plan

1. **Check the TV.** Record the model, firmware and year, then run `preflight.sh`. It only reads.
2. **Build the no-root version.** Add a Tizen adapter, package a `.wgt`, and install it through Developer Mode.
3. **Set power-on.** Turn off Autorun Smart Hub, turn on Autorun Last App, and reboot to check.
4. **Block ads at the router.** This needs no change on the TV and survives a reset.
5. **Decide on the Home button.** This step is optional and needs the owner's separate approval.

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

Turn Developer Mode off when you aren't working on the TV. A reported flaw (SVE-2025-50109) is reachable on the local network while it's on.

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
