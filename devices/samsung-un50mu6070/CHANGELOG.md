# Changelog: Samsung UN50MU6070

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

- HTTPS requests to Apple's video host failed, so this build uses HTTP.
- Starts are intermittent: 18 of 113 files on one pass, 2 of those 18 on the next, and 1 of 12 with a 3-second pause. Working files start within 4 seconds, and encoding and file layout matched between sampled files that played and stalled.
- Home retries after 6 seconds. The cause is unknown.

### Verified

- The real remote moves focus in Home, and OK opens apps.
- Focused menu text is white on dark in Light. Clock cards are light in Light.
- Not checked by eye: this log comes from the page's state, and nobody here can see the screen.
