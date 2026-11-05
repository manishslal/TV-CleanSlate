# Lessons from each TV

What one TV taught us that applies to the next. Add to this after every device, and copy any fix into the other Home versions in the same change.

## Keeping versions the same

`docs/parity.json` is the full feature list, and CI fails a UI change that doesn't update it. The table below records where shared features live.

| Feature | Android | webOS | Samsung |
|---|---|---|---|
| Dark tiles: one ink on every logo | `TileArt.monoInk` | `index.src.html` | From webOS |
| Light clock cards; dark on the screensaver | `FlipText` | `index.src.html` | From webOS |
| Tile name clears the focus ring | `AppTile` | `index.src.html` | From webOS |
| Focus glow surrounds the tile; background glow on Dark and Clear only | `AppTile`, `HomeScreen` | `index.src.html` | From webOS |
| A stalled wallpaper gives way to the next | `ExoPlayer` not reviewed | `index.src.html` | From webOS, 6 seconds |

- Samsung Home is built from `webos/index.src.html`. Fix shared behavior there, then rebuild both.
- Android Lite has no card clock.
- Check the changelog before calling a feature new: Android's Dark logos shipped in 1.67-mh17, and the web versions missed them.

## Porting to an older web engine

- Compiling CSS variables away can change which rule wins. `body.light .row .v` beat `.row.f .v` after compiling, so focused menu values went dark on dark. Check focused states in both themes after any compile step.
- Don't assume `play()` returns a Promise or that `playing` fires. On Tizen 3.0, `timeupdate` was the reliable signal.

- Hand-built MP4 fragments can pass `ffmpeg` and still fail on the TV. Tizen 3.0 rejected Apple's QuickTime sample entry until its version fields and 4-byte terminator were removed. Bisect by swapping one box at a time into a file that works.

## Video on older TVs

- A TV's browser and media player can use different network code. On the Samsung, the media player couldn't reliably reach hosts with IPv6 records while `XMLHttpRequest` always could, so one in six looked random.
- When a media test looks random, rule out the network by host: try an IPv4-only host, a dual-stack host and a relay that logs requests. A failure on one host type is a clue a list of "good" files will hide.
- Feeding Media Source from `XMLHttpRequest` avoids the media player's network code. It took Apple's videos from about one in six to 113 of 113 on the Samsung.
- Test leaving and returning, and check that playback time moves. On the Samsung, the engine paused video as Home came back, leaving the first frame on screen with `readyState` 4.
- Check that a test can't pass by accident: a stale player event reported a 22 ms start until the numbers looked wrong.

## Removing apps

- Try the ID from the TV's own app list, not only the package ID. On the Samsung, `vd_appuninstall` refused `XtU8fQk2gN.ESPN` with error 132 and accepted the numeric ID from `vd_applist`.
- The Samsung's Delete menu can refuse an app that the numeric ID removes.
- Try the package ID too, the part before the dot. `vd_appuninstall adplatform` and the store demo's package ID worked after their app IDs returned error 132.
- Every Tizen version labels system parts differently. Tizen 4.0 marks `com.samsung.tv.*` system apps as visible.
- Video wallpaper cost about 200 MB of RAM on a 2018 Samsung with 1 GB. Stills are the fast option on such TVs.
- Check `applist` afterwards. Package-level removal also takes an app's background services.

## Web ports

- `org.tizen.SMSourceAPP` launches but shows nothing on a 2018 Samsung. Pressing the Source key from the page over `wss://127.0.0.1:8002` opened, but the TV kept asking for approval and returned `ms.channel.unauthorized`, so this isn't solved yet.
- Samsung's on-screen keyboard sends 65376 for Done and 65385 for Cancel, not Enter.
- When a port stubs a platform service, return every field the page reads. Samsung's network stub left out the IP address, so Control Center said Offline on a connected TV.

## Debugging a TV you can't see

- On rooted LG TVs, `com.webos.service.capture/executeOneShot` with method `DISPLAY` captures the real screen, video included. `captureCompositorOutput` leaves the video out.

- Try undocumented arguments before accepting a reported limit. Samsung's `sdb shell 0 debug <app>` answers `closed` on Tizen 3.0, but adding a timeout, as in `sdb shell 0 debug <app> 30`, opens the web inspector.
- The Samsung's inspector can't capture frames, but it can run html2canvas in the page. That shows layout and focus, not the video plane, and it isn't an exact render.
- On the Samsung, remote close 1005 `notack` means nobody pressed Allow within 30 seconds. Keep the connection open until the token arrives, and try a new client name if the prompt stops working.
- A Samsung app can't register the Home key, but Smart Hub takes focus without hiding the page. An app calling `tizen.application.launch` on itself at that moment closed Smart Hub on both Tizen 3.0 and 4.0. Menu and Source take focus the same way, so test every system pop-up before shipping a focus rule.
- A web app is frozen behind other apps, so test any Home button idea from inside an app, not only from Home.
- Set up a camera and scripted keys before a remote-control test. On a Mac, QuickTime's Movie Recording window stays live, and Photo Booth pauses. Look after every key, because a blind sequence can act inside the wrong app.
- A Samsung can offer two IDs for one app. Netflix's store ID opened black, and `org.tizen.netflix-app` worked.
- A remote Allow only counts while the computer stays connected. If a stale prompt stays up, close it and connect under a new client name.
- A timeout isn't proof that code ran. On the Samsung it was the known hang from asking for debug twice. Change the delay length and confirm the time changes with it.
- Check removed apps again later. The Samsung reinstalled ESPN, Tubi, Spotify and YouTube TV by itself.
- `sdb shell 0 debug <app>` needs a timeout argument on Tizen 3.0 and fails with one on Tizen 4.0, so try both.
- Inspector key events reach the page with no pairing, which is enough to test Home's focus and menus.
- Add a debug build that reports keys, errors and page state to the computer. It separated "the remote never reached Home" from "Home ignored it".
- Cache-bust any polling request, or commands can silently stop arriving.
- Test a failing URL directly from the TV. Its failing HTTPS request to Apple's video host looked like a playback stall.
- Repeat a media test before acting on it. On the Samsung, 18 of 113 videos started, but only 2 of those 18 started on a second pass, so a list of "good" files would have been false.

## Logos

- A focus glow taken from an app's icon is no glow at all for a grey or black-and-white icon (Apple TV) and nearly none for a dark one (HBO Max, Windscribe). Lift dark colors, glow white for greys, and let a logo name its glow color.

See [Logos we still need](../logos/README.md#logos-we-still-need).

## Settings pages on Android TVs

- No TV here answers the standard `android.settings.BLUETOOTH_SETTINGS`. Check `cmd package resolve-activity --brief -a <action>` on each device before relying on one.
- Google TV (onn 4K, Android 14) opens Remotes & Accessories with `com.android.tv.settings.CONNECTED_DEVICES`.
- Android 12 TV settings (Hisense) show the same page as a slice: `android.settings.SLICE_SETTINGS` with the extra `slice_uri` set to `content://com.android.tv.settings.accessories.sliceprovider/general`.
- Android TV 11 (SHIELD) can't open that page alone. Its `SubFragmentActivity` only takes power options. The nearest is the pairing search, `com.google.android.intent.action.CONNECT_INPUT`.
- Apps can list themselves as TV inputs for their preview channels. On the SHIELD, Peacock and VLC did, with no hardware behind them. Count only inputs with hardware before deciding a device has inputs.

## Control Center

- Anything a panel needs should be looked up before it opens. Reading 30 days of usage stats, an app's icon and a logo when Control Center opened made the VPN button appear a second or two after the rest on the SHIELD.

## Moving wallpaper under glass

- When a panel opens over a moving film, the film stops and its frame becomes the glass's picture. On Android 13 and later, the glass used to drop from the lens to flat frosting while the new picture faded in, then pop back: the "liquid glass turns on and off" the owner saw on the onn. With Daily Photo the picture never changes, so it never showed. Fade through the lens instead, and ease the film's speed down rather than pausing on the spot.

## Testing on a TV someone is using

- Look at the screen after every key, and never send a run of keys blind. A blind run that assumed Control Center had opened launched TiviMate's live TV on the SHIELD instead.
- Change appearance with Home's config broadcast (`--es appearance light`) rather than keys, and set it back when done.
- Don't open pages on a TV that is in use or asleep just to look. A box asleep can wake the TV over HDMI-CEC.


## Type size across devices

- Google TV and SHIELD ship with the system text size (`settings get system font_scale`) at 0.85, while Fire OS ships at 1.0. Home's type was sized on the first two, so every sp size drew 18% larger on the Fire TV. Check `font_scale` on each device before judging text size from a screenshot; `canvasFontScale` in `Theme.kt` now evens it out.
- Look at a new element on every device class it ships to, not only the one in front of you. The Wi-Fi name was checked on the onn only.

## Talking to a home server

- Android 9 and later refuse plain HTTP to any host the network security config does not name, and a home server's address is different in every home. The SHIELD and onn failed where the Fire TV (Android 7) worked. Home writes its request to the cut-out service on a socket, and only to an address that resolves to a private range; HTTPS rules stay as they are everywhere else.
- A Compose state object made for the first time on a background thread, while a frame is being composed, stops the app ("reading a state that was created after the snapshot was taken"). Touch such objects on the main thread at start-up (`Depth.clear()` in `Wallpapers.init`).

## Testing over ADB from zsh

- In zsh, `$id:latest` is not "the value, then :latest": `:l` is the lowercase modifier, so the shell sent `earthatest`. Write `"${id}:latest"`. Home was right to fall back to its default picture.

## Picture sources

- NASA's own Picture of the Day API answered every day of 2026 with a placeholder NASA logo. Earth Observatory's feed (science.nasa.gov) works and is mostly pictures of Earth from orbit; its resizer gives a 1920-wide copy of files up to 6 MB.
- Wikimedia's thumbnail server allows 1920 wide but refuses 2560.
- A cut-out model also marks haze (a river, a sky). Judge a cut-out by how firmly it marks what it marks, not by its size: a fish scored 0.93, river haze 0.44.
- Read a 16:9 picture's calmest spot from the part the screen shows: squashing the whole 3:2 picture into the sample picked a different side than the cropped picture did.

## LG webOS

- LG's own screensaver starts over any page, even a home page with its own. It asks the subscribers of `luna://com.webos.service.tvpower/power/registerScreenSaverRequest` first; answering `responseScreenSaverRequest` with `ack: false` and the request's `timestamp` holds it off. A root helper can do this; a web page cannot.
- Ask which device is the reference before matching a look: the onn (Android 14) draws its dock with a lens that lets the sharp picture through, the SHIELD (Android 11) a plain frosted plate. Matching the onn made the LG less like the SHIELD the owner had in mind.
- To compare docks, put the same picture on both TVs and measure the same rows; a Python model of Android's plate came within 2 levels of the SHIELD, which then predicts the web's numbers before deploying.
- The LG's DISPLAY capture includes an HDMI input: a capture showing another device's apps means the owner is watching that input. Check `getForegroundAppInfo` before reloading Home.
- On webOS 6 a CSS `filter: blur()` is re-run on every frame something on screen moves, and a `clip-path` that animates is redrawn every frame, so the two together cost the app grid a third of its frames and quarter-second stalls. Blur once into a canvas (`ctx.filter`, or shrink and grow on Chromium 47), fade with opacity, never animate a clip, and stop drawing what an opaque layer covers.
- Measure on the TV before arguing about speed: an idle CPU reading showed nothing wrong, because the cost is only paid while something moves. A test page that sends the same key presses and records frame times, triggered through the helper, settled it in minutes.

