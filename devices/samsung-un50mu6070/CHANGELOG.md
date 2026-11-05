# Changelog: Samsung UN50MU6070

*A record of one TV's changes, not a guide. Read [safety.md](../../guides/safety.md) and [Before you start](../../README.md#before-you-start) before changing your own.*

**Device:** Samsung 6 Series UN50MU6070 (2017, 4K), `17_KANTM_UHD_BASIC`, armv7, 1.36 GB RAM
**OS:** Tizen 3.0, software T-KTMAKUC-1410.1
**Access:** Developer Mode and `sdb`, with a free author certificate
**Rooted:** No

## 2026-11-01

### Install: Home (`CSHome00Te.Home`)

- Installed with `tizen/deploy.sh`. The owner set Autorun Last App on the TV.
- Undo: `sdb shell 0 vd_appuninstall CSHome00Te.Home`.

### Removed

| App | How | Undo |
|---|---|---|
| Spotify, YouTube TV | Owner removed them | Samsung's app store |
| ESPN (`3201708014618`) | `vd_appuninstall` with the numeric ID | Samsung's app store |
| Tubi (`3201504001965`) | `vd_appuninstall` with the numeric ID | Samsung's app store |
| e-Manual (`20172100005`) | `vd_appuninstall` with the numeric ID | Check Samsung's app store; a Smart Hub reset may restore it |

Removing these took free space from 109 MB to 150 MB of 816 MB.

### Refused

- AdSuite, Sponsored Video and Ad Player returned error 132 by package ID. They have no numeric ID in `vd_applist`, so block ads at the router or DNS.
- PrivacyChoices stays, because it may hold the ad-tracking opt-out.

### Storage

- The largest packages are Samsung's own, led by ConnectionGuideApp (369 MB), firstscreen (272 MB) and voice-app (179 MB).
- `tizen.package` reports only a few KB for each downloaded app, and there's no API to clear another app's cache.

### Wallpaper

- HTTPS requests to Apple's video host failed from the browser side, so this build uses HTTP.
- `<video>` started about one try in six: the media player can't reliably reach hosts with IPv6 records, and `<video>` stalls on 10 Mbps 1080p from any host. See the [research](../../docs/research/samsung-older-tvs.md).
- `tizen/aerial.js` now fetches files with `XMLHttpRequest` and plays them through Media Source. 113 of 113 videos started through Home's clip change, with a median of 1.4 seconds and a slowest of 2.4.
- A 524-second clip ran to its end and the next one started. Leaving for another app and returning restarted the wallpaper.
- The owner confirmed by eye that video plays behind Home.

### Home button

- Home reopens itself about 0.6 seconds after the Home button, from Home's own screen only. **Home Button Returns Here** turns it off.
- Home takes Menu and Source. Menu opens normally, and Source asks for a second press. The Inputs tile is gone. All of this was seen by the owner, except that the tile's removal was installed afterwards.

### Known problems

- ESPN, Tubi, Spotify and YouTube TV came back on 2026-11-01 at 19:27, without anyone reinstalling them, and show as tiles. The removal above didn't last.

### Tiles

- Fixed Netflix: the store ID opened a black screen, so Home opens `org.tizen.netflix-app`.
- Seen opening on camera: YouTube, Netflix, Disney+ (about 30 seconds), HBO Max, Plex, Hulu, Prime Video, Apple TV, Paramount+, Tubi, YouTube TV. Not tested: ESPN, Spotify. Peacock's tile showed Prime Video's profile screen once, which may be a failed launch or a test slip, so it's unconfirmed.

### Delay test

- The owner approved a delay-only check of `sdb shell 0` commands. No command ran, and nothing changed on the TV. See the [research](../../docs/research/samsung-older-tvs.md#root-on-2017-and-2018-tvs).

### Home key package

- `CSHomeKey1.Web` was installed for a test and removed the same day with `vd_appuninstall`. The key rule didn't fire.
- A client named CleanSlateKeys is allowed on this TV. Undo: remove it in Device Connect Manager.

### Tools

- `sdb shell 0 debug CSHome00Te.Home 30` opens the web inspector. Without the last argument, the TV answers `closed`.
- The owner allowed a client named CleanSlateMac on the TV, and `tizen/tvkey.py` now sends remote keys. Undo: remove it in Device Connect Manager.
- `tizen/tvshot.py` takes screenshots of Home's page, runs JavaScript in it and sends it keys.

### Verified

- The real remote moves focus in Home, and OK opens apps.
- Focused menu text is white on dark in Light. Clock cards are light in Light.
- Not checked by eye: this log comes from the page's state, and nobody here can see the screen.
