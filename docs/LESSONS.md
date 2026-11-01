# Lessons from each TV

What one TV taught us that applies to the next. Add to this after every device, and copy any fix into the other Home versions in the same change.

## Keeping versions the same

| Feature | Android | webOS | Samsung |
|---|---|---|---|
| Dark tiles: one ink on every logo | `TileArt.monoInk` | `index.src.html` | From webOS |
| Light clock cards; dark on the screensaver | `FlipText` | `index.src.html` | From webOS |
| Tile name clears the focus ring | `AppTile` | `index.src.html` | From webOS |

- Samsung Home is built from `webos/index.src.html`. Fix shared behavior there, then rebuild both.
- Android Lite has no card clock.
- Check the changelog before calling a feature new: Android's Dark logos shipped in 1.67-mh17, and the web versions missed them.

## Porting to an older web engine

- Compiling CSS variables away can change which rule wins. `body.light .row .v` beat `.row.f .v` after compiling, so focused menu values went dark on dark. Check focused states in both themes after any compile step.
- Don't assume `play()` returns a Promise or that `playing` fires. On Tizen 3.0, `timeupdate` was the reliable signal.

## Removing apps

- Try the ID from the TV's own app list, not only the package ID. On the Samsung, `vd_appuninstall` refused `XtU8fQk2gN.ESPN` with error 132 and accepted the numeric ID from `vd_applist`.
- The Samsung's Delete menu can refuse an app that the numeric ID removes.
- Check `applist` afterwards. Package-level removal also takes an app's background services.

## Debugging a TV you can't see

- Add a debug build that reports keys, errors and page state to the computer. It separated "the remote never reached Home" from "Home ignored it".
- Cache-bust any polling request, or commands can silently stop arriving.
- Test a failing URL directly from the TV. Its failing HTTPS request to Apple's video host looked like a playback stall.
- Repeat a media test before acting on it. On the Samsung, 18 of 113 videos started, but only 2 of those 18 started on a second pass, so a list of "good" files would have been false.

## Logos

See [Logos we still need](../logos/README.md#logos-we-still-need).
