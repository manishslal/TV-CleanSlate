# Fork spec v1 (2026-10-28)

The first build of the fork. Target device: onn. 4K Streaming Box (Google TV 14, 2 GB RAM, ~1.5 GB free before cleanup).
Rules: follow upstream `AGENTS.md` (Motion curves, `Pressable`, 960×540dp canvas, strings in all 5 locales). Keep `system/`, `guard/`, `dream/` and `update/` untouched except where noted.

## 0. Make it its own app
- `applicationId` → `io.manish.home` (placeholder). App label "Home" (placeholder). Upstream `app.orchard.tv` can stay installed side by side.
- Release signing key generated into `local/` (gitignored). `local/signing.properties` as upstream's build expects.
- Remove calls to the upstream author's server:
  - Update checks/downloads (`update/Updates.kt`): disabled in our builds.
  - Icon library (`apps/IconLibrary.kt`): off; local icon art only.
  - Control Center update/setup pages: hidden.

## 1. Units, clock, city
- Settings: Temperature °F / °C (default °F). Converted at display time; the stored reading stays Celsius.
- Settings: Clock 12-hour / 24-hour (default 12-hour, shown as "5:39 PM") on Home, the screensaver and Control Center.
- Settings: City, either Automatic (IP lookup, today's behavior) or a chosen city via Open-Meteo geocoding search. The choice drives weather lat/lon and the label.

## 2. Dark tiles
- Personalize: Tile style Color (upstream) / Dark (default). Dark = elevated dark glass plate, logo extracted and drawn white using TileArt's existing `placeLogo`/`whiten`. Full-picture icons stay as pictures, inset on the dark plate.
- The tile cache key includes the style, so switching redraws.

## 3. Drone media from Plex (NAS <lan-ip>:32400)
- Settings → Plex: **Link with plex.tv PIN**. The TV shows a 4-character code, and the owner enters it at plex.tv/link on a phone. The app polls `plex.tv/api/v2/pins/{id}` and stores the token in app-private prefs. No password ever passes through the TV or the developer.
- Server: chosen from `plex.tv/api/v2/resources`; prefer the local connection.
- Choose a **video library** and a **photo library** (or a folder inside one).
- Uses:
  - Screensaver = Plex videos (direct play of the part file over HTTP; Media3 handles range/seek; no transcode)
  - Wallpaper = Plex video (existing "moves after 10 s still" behavior)
  - Screensaver/wallpaper = Plex photos (Plex photo transcoder at 1920×1080)
- Offline/unreachable: fall back to the last good mode (photos → aerial films), never a black screen.
- Upload-to-Onn (upstream phone page) stays. Caps become configurable, defaulting to 70% of free space at install, max 1.5 GB. Mac-side helper `tools/shrink-video.sh` (ffmpeg → 4K HEVC ~15 Mb/s, faststart).

## 4. Tech headlines on the screensaver
- Source: Google News Technology RSS (no key), refreshed every 30 min, cached last 10. Off switch in Settings.
- Card: bottom-left glass card, 3 headlines + source + age, cross-fading every 30 s using `Motion` curves. Hidden for the first 20 s of the screensaver. No continuous animation while static; one full-screen draw per frame max.
- Design pass with design-taste-frontend + ui-ux-pro-max (type scale, contrast over moving video, safe area).

## On-device acceptance (Onn)
- [ ] Installs side by side with upstream Aerial; Home Guard can be pointed at the new ID.
- [ ] °F, 5:39 PM format, and a chosen city are shown.
- [ ] Dark tiles for every shelf app; screenshot reviewed.
- [ ] Plex link via PIN; a 4K HEVC drone clip plays as the screensaver without stutter; photos rotate.
- [ ] Headlines card appears and rotates; screensaver still exits on any key.
- [ ] Reboot test passes; no crash in logcat.
