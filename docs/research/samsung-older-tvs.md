# Older Samsung Tizen TVs

What else TV CleanSlate Home can do on older Samsung TVs, tested on a UN50MU6070 (2017, Tizen 3.0, Chromium M47, 1.3 GB RAM). Results marked "this TV" were measured there. Everything else is a report from the linked source.

## Summary

| Question | Answer | Status |
|---|---|---|
| Why did aerials start one try in six? | Two faults: the media player can't reliably reach hosts with IPv6 records, and `<video>` often won't start 10 Mbps 1080p | Fixed in `tizen/aerial.js` |
| Can we screenshot Home without root? | Yes, using the web inspector and an in-page render. The video plane isn't included | Built: `tizen/tvshot.py` |
| Can a Mac send remote keys? | Yes. Into Home with no pairing, and to the whole TV after pressing Allow once | Built: `tizen/tvkey.py` |
| Can Home open when Smart Hub opens? | Yes on a 2018 Tizen 4.0 TV: Home reopens itself and Smart Hub closes. The button itself still goes to Smart Hub first | Built, and only from Home's own screen. The key-rule package failed on Tizen 3.0 |

## What works where

| Technique | Tizen 3.0 / 2017 | Newer TVs only | Needs root or an exploit | Source |
|---|---|---|---|---|
| Sideload a `.wgt` with Developer Mode and a free author certificate | Proven, this TV | Tizen 7 and later need a Samsung certificate | | [TizenBrew](https://github.com/reisxd/TizenBrew/blob/main/docs/README.md) |
| Web inspector with `sdb shell 0 debug <app> <timeout>` | Proven, this TV | Works on 2018 and 2020 TVs without the timeout | | [forum](https://forum.developer.samsung.com/t/force-debug-port/10309) |
| AVPlay | Proven, this TV | | | [Samsung](https://developer.samsung.com/tv/develop/api-references/samsung-product-api-references/avplay-api) |
| Media Source with `<video>` | Proven, this TV | | | This repo |
| `GET /api/v2/applications/<id>` on port 8001 | Proven, this TV, read only | | | This repo |
| Autorun Last App | Proven, this TV | | | [samsung-tv-launcher](https://github.com/ferdj40/samsung-tv-launcher) |
| Removing downloaded apps by numeric ID | Proven, this TV | | | This repo |
| TizenBrew and its Node service app | Reported from Tizen 3.0 up, not run here | | | [TizenBrew](https://github.com/reisxd/TizenBrew) |
| TizenTube ad-free YouTube | Reported through TizenBrew, not run here | | | [control center](https://github.com/woo-jobradovic/tizen-tv-control-center) |
| Remote keys over `samsung.remote.control` | Proven, this TV, on port 8002 after Allow; port 8001 refused | Reported from 2016 | | [samsung-tv-ws-api](https://github.com/xchwarze/samsung-tv-ws-api) |
| Node.js launcher replacing Smart Hub | | Tizen 6 test only | | [samsung-tv-launcher](https://github.com/ferdj40/samsung-tv-launcher) |
| Writing `db/appfw/home_appid` | No report on this TV | About 2020 onward | Yes: command injection in the installer | [write-up](https://llmgateway.io/blog/hack-samsung-tv-home-screen-with-ai) |
| Removing Samsung's own packages | No report on this TV | Tizen 9 | Yes | [tizen-tv-control-center](https://github.com/woo-jobradovic/tizen-tv-control-center), [root notes](https://github.com/DemonKingSwarn/samsung-tv-tizen-root) |
| SamyGO root | Nothing found for M series or `T-KTM` firmware | | Covers C to K series | [K root thread](https://forum.samygo.tv/viewtopic.php?p=103712), [M/Q firmware list](https://forum.samygo.tv/viewtopic.php?p=110545) |

`cleanyourtv-samsung` claims Tizen 2.4 to 9 without root but shows no test results, so treat those version ranges as untested. It uses the same Developer Mode, Autorun Last App and DNS blocking described here.

The installer injection, SVE-2025-50109, is an exploit. It wasn't tried here and needs the owner's approval.

## Screenshots

| Route | Result on this TV |
|---|---|
| `sdb shell 0 debug <app>` | Answers `closed`, as [reported for Tizen 3.0](https://github.com/headspinio/appium-tizen-tv-driver/issues/584) |
| `sdb shell 0 debug <app> 30` | Works. The extra argument makes it print a debug port |
| `Page.captureScreenshot`, `Page.startScreencast` | Connect, then never return a frame, even with video stopped |
| html2canvas injected through the inspector | Works, in a few seconds for a 1920×1080 PNG |
| Drawing `<video>` to a canvas | All black, so video frames can't be read |

`tizen/tvshot.py` uses the working route:

```bash
TV=<tv-ip> python3 tizen/tvshot.py shot home.png
TV=<tv-ip> python3 tizen/tvshot.py eval 'document.title'
```

A capture shows layout, text, focus, logos and theme. It doesn't show the video, or any Samsung screen, because it's the page redrawing itself in JavaScript. html2canvas also doesn't draw everything as the engine does: blur, some box shadows and clipped layers can look wrong, and the dock's frost shows as a flat band. Check those on the real screen.

Asking for a debug port while Home is already in debug mode hangs, so the tool keeps the port. The debug port is also open on the local network while Home runs in debug mode, with no password, so restart Home normally afterwards and keep Developer Mode off when you aren't using it.

## Aerial video

The old result of one start in six came from two faults that multiplied.

### The media player and IPv6

| Test, using AVPlay | Starts |
|---|---|
| Host with only IPv4 records | 8 of 8 |
| Local server by IP address | 11 of 12 in 12 seconds; the 12th was still buffering |
| Apple through a relay on the Mac | Reached "prepared" 12 of 12 |
| Host with IPv4 and IPv6 records | 0 of 8 |
| Apple's host directly | 3 of 11 over HTTP, 2 of 8 over HTTPS |

When a try fails, the TV reports `PLAYER_ERROR_CONNECTION_FAILED` after 30 seconds, and the relay shows no request. The TV's browser side, which handles `XMLHttpRequest`, reached Apple on 16 of 16 requests at about 80 Mbps.

This TV has an IPv6 address on the home network but no IPv6 route to the internet. The Mac on the same network also has only a local IPv6 address. Apple's host publishes two IPv4 and two IPv6 addresses. The likeliest explanation is that the media player sometimes takes an IPv6 address and doesn't fall back, while the browser does. We didn't see the player's chosen address, so that last step is inferred. The pattern matched hosts, not Apple: no sign of it showed on an IPv4-only host.

Good and bad spells seem to last about as long as the DNS answer does, so quick retries don't help. AVPlay with a 2.5-second retry took from 5 to 45 tries per clip, up to nearly 2 minutes.

Local Thread or Matter hubs and some routers hand out addresses like this, so other homes can hit this even with no IPv6 service. Turning off IPv6 on the router may fix it for one home, but Home can't rely on that.

### `<video>` and 10 Mbps 1080p

Served from the Mac by IP address, so with no IPv6 involved:

| File | `<video>` starts in 12 seconds |
|---|---|
| Apple's file, 1080p 10 Mbps, `moov` at the end | 1 of 5 |
| Same file, faststart | 2 of 5 |
| First 60 seconds, faststart | 3 of 5 |
| 720p 3 Mbps, faststart | 5 of 5 |

These failures reach `loadedmetadata`, read up to 10 MB, and never reach `canplay`. The player read at about 1 MB/s, close to the file's own bitrate. Why it never starts is unknown. AVPlay and Media Source both play the same 1080p content.

### What didn't matter

- Network speed, signal and Apple's server: 16 of 16 range requests returned 206, including mid-file.
- `moov` at the end: the player fetches it with a range request in under a second, and faststart didn't fix it.
- Storage: playback streams to memory, and the fix changed nothing on disk. Free space stayed at 149 MiB through these tests.
- Apple's response headers: passing them through the relay unchanged made no difference.
- Encoding: Apple has no HEVC copies at these URLs, and the same H.264 stream plays under Media Source.

HTTPS is a separate limit. `XMLHttpRequest` and `<video>` to Apple failed over HTTPS, while AVPlay reached it. Samsung's forum [reports the same on older TVs](https://forum.developer.samsung.com/t/unable-to-play-video-url-in-samsung-tizen-tv-2015-and-2016-model/5325). Home stays on HTTP, so these public videos aren't encrypted in transit.

### The fix

`tizen/aerial.js` avoids the media player's network code. It reads the `.mov` index with range requests, cuts the file at keyframes into MP4 fragments in JavaScript, and appends them to the normal `<video>` through Media Source. Nothing is re-encoded. It keeps about 15 seconds ahead and removes what has played, so a clip uses little memory.

The first frames from the rebuilt stream matched the source frame for frame in `ffmpeg`. Apple's QuickTime sample entry needed two changes, or the TV rejected it with decode error 3: clearing its version fields, and removing a 4-byte zero terminator.

| Test on this TV | Result |
|---|---|
| Every video in the list, through Home's own clip change | 113 of 113 started |
| Time to the new clip | Median 1.4 seconds, slowest 2.4 |
| Free memory during that pass | 97 to 272 MB, no downward trend |
| A clip played to its natural end | Played 524 seconds, then the next clip started |
| Leave for another app and return | 3 of 3 played on, after a fix: the engine pauses video as the app returns, so the player resumes it |

The owner confirmed by eye that video plays behind Home. The other results come from the player's state and its playing position.

The resume fix came after the 113-video pass, and it only adds a one-second check to the player.

Other routes we rejected:

| Route | Why not |
|---|---|
| AVPlay | Fixes the bitrate fault but not the IPv6 one, and has no crossfade |
| Apple's old `a1.phobos.apple.com` set | IPv4 only, but a much older, smaller set that Apple could remove |
| Smaller or re-encoded videos | Means hosting Apple's work ourselves |
| A relay on the TV in a Node service | Heavy, and unnecessary now |

Plex wallpapers still use the plain `<video>` path, so high-bitrate files may stall there. Files that aren't H.264 High 4.0 or are on Tizen 2.4 are untested.

## Remote keys from a Mac

| Route | Reach | Result on this TV |
|---|---|---|
| Inspector `Input.dispatchKeyEvent` | Home only | Works with no pairing: `tizen/tvshot.py key Right Enter` |
| `POST` or `DELETE /api/v2/applications/<id>` on port 8001 | Opens and closes apps | Works with no pairing. Other devices on the network can do this too |
| `ws://<tv-ip>:8001/api/v2/channels/samsung.remote.control` | Whole TV | Closes at once with 1005 |
| `wss://<tv-ip>:8002/.../samsung.remote.control` | Whole TV | Works after Allow, with `tizen/tvkey.py` |

```bash
TV=<tv-ip> python3 tizen/tvkey.py KEY_RIGHT KEY_ENTER
```

The TV waits 30 seconds for Allow, then closes with 1005 `notack`, which explains the earlier failures. Allow has to be pressed while the computer is connected, and it returns a token. An Allow given with no token saved didn't carry over to the next connection, so connect under a new client name if that happens. The token is saved in `~/.cache/tv-cleanslate/` and reconnects with no prompt. On this TV, `KEY_LEFT` and `KEY_RIGHT` moved Home's focus as expected, and the token reconnected in under a second.

After `KEY_HOME`, Home's page stayed unhidden but lost focus: `document.hidden` was false and `document.hasFocus()` was false. So Smart Hub opens over Home without the `visibilitychange` event, and option 3 below would have to watch focus instead.

`tizen.tvinputdevice.getSupportedKeys()` on this TV lists Menu, Tools, Info, Exit, Guide, Source and the colour keys, but not Home.

## The Home button

### What Home does today

An app can't register the Home key, but Smart Hub takes focus without hiding Home's page, so Home reopens itself. This needs Home to be on screen. Inside another app, Home's page is frozen, so nothing happens.

| Test | UN55NU6900 (Tizen 4.0) | UN50MU6070 (Tizen 3.0) |
|---|---|---|
| Home pressed while Home is on screen | Back in about 1 second, seen by the owner | Back in about 0.6 seconds, seen by the owner |
| Home pressed inside an app | Not tested | Home can't see it |
| Volume, Mute, Info | Not tested | Don't set it off |
| Menu | Not tested | Stays open, because Home takes the key and opens it |
| Source | Not tested | Works, seen by the owner: Home asks for a second press |

Nothing else tells these apart. `getAppsContext` didn't change after Home, Source or Menu, and the REST API returns 404 for Samsung's own apps. Home can't press Source for you: `org.tizen.SMSourceAPP` shows nothing, secure WebSockets to `127.0.0.1` fail from the page, and the local port dropped even after Allow.

### What others have done

| Project | Method | Proven on | Needs |
|---|---|---|---|
| [samsung-tizen-homekey](https://github.com/gtalusan/samsung-tizen-homekey) | A small `.tpk` with an `XF86Home` key rule that launches your app. Works inside apps. Smart Hub flashes first. Uninstalling removes it | Tizen 5.5, 2020 Q90T | Native package install; no injection described |
| [samsung-tizen-home-remap](https://github.com/chutovkatest/samsung-tizen-home-remap) | Replaces FirstScreen's Home rule through a package manifest, with no Smart Hub. Has apply and restore packages. Warns not to uninstall before restoring | Tizen 5.0, 2019 RU7172; the generic sample is unproven | Native package, stock baseline report |
| [samsung-tv-unlock](https://github.com/DavidNgugi/samsung-tv-unlock) | Writes a row in `.app-context.db` | Tizen 6.0; 4.x and 5.x untested | File access we don't have |
| [fresh-home](https://github.com/KSWORK2001/fresh-home), [write-up](https://llmgateway.io/blog/hack-samsung-tv-home-screen-with-ai) | `home_appid` and a helper app | About 2020 onward | Installer injection on most firmware |
| [quiet-tizen](https://github.com/jss8649/quiet-tizen/blob/main/docs/04-launcher.md), [control center](https://github.com/woo-jobradovic/tizen-tv-control-center) | Replaces a system app, or watches keys | Tizen 9 | Root |

Nobody reports any of these on Tizen 3.0 or 4.0. One [report](https://github.com/schadogamer127-sys/Flix-Home/issues/1) lists the Home key as unsolved on a Tizen 4.0 TV. The key-rule route is the best fit: it's the only one that needs no flaw or root and works inside apps. It isn't proven on our TVs, the free author certificate may be refused for a native package, and a key rule touches how the remote is routed, so it needs the owner's approval first.

### Key-rule test on the UN50MU6070

This TV's `vd_appinstall` only installs web packages. A true `.tpk` fails with error 110, and `sdb install` and `0 appinstall` answer `closed`. A web package carrying the same `tizen-manifest.xml` installed, but its native service wasn't created. With YouTube open, Home opened Smart Hub and the rule didn't fire, so the package was removed. The key-rule route is closed on this TV unless a `.tpk` can be installed, which needs a method we don't have. `tizen/homekey/` keeps the test package for other TVs.

### The way back from inside an app

| Route | Result |
|---|---|
| Home inside YouTube, then Up, then OK | Home opened. The cursor started on Apps, with Home in Recent above it |
| Home inside Netflix | The bar opens on Netflix and takes a moment. Reaching Recent takes more presses |
| Exit or Back | Depends on the app |

Home stays usable, but no no-root route is one press from inside an app on this TV.

### Netflix tile

On the UN50MU6070, launching `RN1MCdNq8t.Netflix` opened a black screen. Launching `org.tizen.netflix-app`, which Home now tries first, opened Netflix normally, as seen on camera.

### Testing without the owner

The MacBook camera can watch the TV. Command-line capture hung on macOS's camera permission, and Photo Booth pauses when idle. QuickTime's Movie Recording window stays live and can be read as a window, with nothing recorded. `tvkey.py` then presses keys and each step is checked by picture. Look after every key: a blind sequence picked a Netflix profile by mistake.

## Root on 2017 and 2018 TVs

Only the delay test below was run on a TV.

### Delay test on the UN50MU6070

With the owner's approval, harmless `sleep` commands were tried on eight `sdb shell 0` forms. None ran. Most forms answer in under a second with `closed` or a normal error. `0 appinstall` answers `closed`. In `0 debug <app> <arg>`, the argument reaches `app_launcher`, but a delay of 2 or 8 seconds changed nothing: `;` cuts the argument off, and backticks, `$()`, `|` and `&&` answer `closed`. Steps 2 and 3 weren't run, so `home_appid` is unread and unchanged.

The installer flaw isn't reachable through these forms on firmware 1410.1. The advisory also describes injection through the developer IP setting, which wasn't tried. That would mean putting a crafted value into a TV menu, and it needs its own approval.

| Route | Tizen 3.0 / 2017 | Tizen 4.0 / 2018 | Stays after reboot | Undo |
|---|---|---|---|---|
| [SamyGO](https://forum.samygo.tv/viewtopic.php?t=8274) | Donor-only test root for early `T-KTM` firmware. Firmware 1410.1 isn't reported, and a [developer said](https://forum.samygo.tv/viewtopic.php?t=13166) updated TVs miss out | Nothing found | Yes | None reliable. Downgrading needs root first |
| [samsung-tv-root](https://github.com/chris-ritsen/samsung-tv-root) and [similar](https://github.com/DemonKingSwarn/samsung-tv-tizen-root) | No report. The chain uses `dotnet`, which we expect is absent on 3.0 | No report | No, a computer re-runs it | Reboot |
| `home_appid` write through the installer flaw, [no root described](https://llmgateway.io/blog/hack-samsung-tv-home-screen-with-ai) | No report. `0 appinstall` answers `closed` here | No report | Reported yes | Write the old value back |

No public, proven root exists for either TV, and no restore image is available. A factory reset reinstalls what's on the TV and doesn't undo a broken system, and [downgrades are refused](https://forum.samygo.tv/viewtopic.php?t=13166) without root. SamyGO's advice to block updates breaks this project's rule, and the injection itself is a [security flaw](https://bishopfox.com/blog/samsung-tizen-os-version-through-9-0) that gives only the `sdk` user.

The flaw is confirmed on Tizen 5.5, 7.0, 8.0 and 9.0 only. LG was different: its root route was proven on that exact model before we ran it.

The safest order, if the owner approves each step:

1. Prove code runs, with a delay only and no change on the TV.
2. Read `home_appid` and `startup_appid`, and save them.
3. Write `home_appid` for a helper app, and test.
4. Stop there. Temporary root needs its own review.

Reported limits: `home_appid` [didn't drive the Home button](https://github.com/NelsonEU/arn0-tv/issues/10) on one Tizen 6.0 TV.

## Other options

Ranked by value for risk.

| Rank | Option | Value | Risk | Needs |
|---|---|---|---|---|
| 1 | Keep aerials working on more TVs: test the player on another older Samsung | High | None | A second TV |
| 2 | Return to Home after an app exits, using `background-support` or a service app that checks `tizen.application.getAppsContext` | High | Low; can fight the user if wrong | Testing |
| 2a | Key-rule package for the Home button | Very high | Failed on Tizen 3.0: the TV won't install a native package. Tizen 4.0 untested | Test on the UN55NU6900 only |
| 3 | Reopen Home when the Home button opens Smart Hub | Medium: only works from Home | Low now that Menu and Source are handled | Built |
| 4 | Ship a release build with an install guide for 2017 to 2019 TVs | High | Low | More TV reports |
| 5 | TizenTube through TizenBrew | Medium: ad-free YouTube | Low; third-party code in a privileged app | Owner's choice |
| 6 | Turn off IPv6 on the router | Low now that Home has its fix; may help Samsung's own apps | Low, can affect local smart-home devices | Owner |
| 7 | Exploit-based Home button or package removal | High | High: no report for this firmware, and it depends on a security flaw | Owner's approval, and not recommended |

Ad blocking at the router is deferred. Update packages stay untouched in every option.

## Sources

- [SamyGO forum](https://forum.samygo.tv/)
- [TizenBrew](https://github.com/reisxd/TizenBrew)
- [cleanyourtv-samsung](https://github.com/smartjaganrao/cleanyourtv-samsung)
- [samsung-tv-launcher](https://github.com/ferdj40/samsung-tv-launcher)
- [tizen-tv-control-center](https://github.com/woo-jobradovic/tizen-tv-control-center)
- [samsung-tv-tizen-root](https://github.com/DemonKingSwarn/samsung-tv-tizen-root)
- [Replacing Smart Hub](https://llmgateway.io/blog/hack-samsung-tv-home-screen-with-ai)
- [samsung-tv-ws-api](https://github.com/xchwarze/samsung-tv-ws-api)
- [appium-tizen-tv-driver issue 584](https://github.com/headspinio/appium-tizen-tv-driver/issues/584)
- [Samsung debug gist](https://gist.github.com/laat/2b7107298593dca650062b7678279386)
- [TV debug MCP screenshot notes](https://glama.ai/mcp/servers/Ediand11/tv-debug-mcp/tools/tv_screenshot)
