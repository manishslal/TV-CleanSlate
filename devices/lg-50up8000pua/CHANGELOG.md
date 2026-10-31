# LG 50UP8000PUA changelog

## Baseline (2026-10-30)
- Model: 50UP8000PUA (UP8000PUA.ACC), webOS 6.5.3, machine lm21a, otaId HE_DTV_W21A_AFADATAA
- Firmware: 03.53.45
- Network: Ethernet at <tv-ip>, MAC <mac>. Wi-Fi also answered at <tv-ip> (same device UUID).
- User decisions: skip all OS updates, root, install a version of our launcher, clean out the TV, block future updates.

## Changes
Each entry gives what changed and how to undo it.

### 1. Root with SlopBro (done; uid 0 confirmed, survives reboot)
- Tool: github.com/throwaway96/slopbro, commit 21023d56, run from the Mac with `--debug --webos-version 6`
- What it did: paired as `lol.slopbro`, created the `/var/luna/preferences/devmode_enabled` directory, installed Homebrew Channel 0.7.3 and elevated it.
- Undo: not verified. Do not install LG's Developer Mode app while rooted.

### 2. Update blocking (done; survives reboot)
- `/var/luna/preferences/webosbrew_block_updates` exists; LG's update hosts point to localhost in `/etc/hosts` at boot.
- Undo: turn off Block system updates in Homebrew Channel settings, then restart.

### 3. Remote access (done)
- Telnet off: `/var/luna/preferences/webosbrew_telnet_disabled`. Port 23 confirmed closed after reboot.
- SSH on, key only: private key `~/.ssh/lg_tv` on the Mac, public key in `/home/root/.ssh/authorized_keys`.
- Undo: SSH Server toggle in Homebrew Channel; delete `webosbrew_telnet_disabled` to bring telnet back (not recommended).

### 4. Home takeover with CleanYourTV 1.6.10 (done; survives reboot)
- Installed `com.cleanyourtv.manager`, then `bootstrap.sh install --no-debloat --no-input-key`. LG's Home is replaced in RAM by a bind mount at boot.
- Undo: switch off CleanYourTV as Home in its app, or `sh /media/developer/cleanyourtv/undo-home.sh`. Full removal is in the CleanYourTV app.

### 5. Our launcher, webOS version (done)
- Source: `webos/`. Deployed to `/media/developer/cleanyourtv/app/` with launcher version 99.0.0 so CleanYourTV's stock launcher does not overwrite it.
- Ported: Aerial video wallpaper, departure-board clock, date and weather, dark glass tiles, focus glow, idle mode after 5 minutes.
- Not ported: Android settings panels, headlines, Plex integration, Liquid Glass, flap sounds.
- Hidden in the launcher only, nothing uninstalled: Amazon Alexa, 8 Ball Pool, Home Dashboard, LG Channels, Who.Where.What?, Room to Room Share, Sports Alert.
- Undo: copy `/media/developer/cleanyourtv/backup/app.cyt-original/*` over `app/`, delete `app/barlow_condensed_semibold.ttf`, then run `stage-home.sh` and `reload-home.sh` in that folder.

### 6. Debloat: Tracking, Logs, Suggestions on; Ads off (done; survives reboot)
- Stops `remotelogger`, `contentminer`, `nudge`; stubs `acr2` and `sportsalarm`. `service-logger` does not exist on this TV.
- Ads stays off on 1.6.10 because it also stubs `admanager`, which can hang HBO Max and Disney+ (fixed in 1.6.11 dev).
- Undo: CleanYourTV app → Debloat, or `sh /media/developer/cleanyourtv/debloat-undo.sh`, then delete `/var/lib/webosbrew/init.d/15-cleanyourtv-debloat`.

### 7. Open Home on power-on (installed; wake from standby not yet tested)
- `/var/lib/webosbrew/init.d/20-home-on-power` (source in `webos/`) opens Home after a reboot and on each wake, instead of the last HDMI input.
- Undo: delete that file and restart the TV.

### 8. Faster power-on and fewer ads (done; user confirmed instant Home after sleep and wake)
- Quick Start+ on: `option.quickStartMode` from `off` to `on`, so power-off sleeps instead of cold booting.
- `general.homeAutoLaunch` from `on` to `off`, `general.adCookie` from `on` to `off`.
- `option.livePromotion` and `option.webOSPromotionVideo` from `on` to `off`.
- `20-home-on-power` now waits for the takeover instead of a fixed 75 seconds.
- Measured cold boot: Home takeover done about 79 seconds in. LG screens can still show before that, so cold boots remain slow.
- Undo: set the values back with `luna://com.webos.settingsservice/setSystemSettings`. Quick Start+ is also in the TV's General settings.

### 9. Ads and bloat pass (done; reboot verified)
- Ads debloat group on: `debloatOff` is now empty. `debloat-stop.sh` on the TV was replaced with CleanYourTV's newer dev version, which leaves `admanager` alone, so HBO Max and Disney+ should not hang. Undo: copy `scripts/debloat-stop.sh` from the `com.cleanyourtv.manager.service` folder back over it, or switch the group off in the CleanYourTV app.
- `/var/lib/webosbrew/init.d/16-lg-us-ads` (source in `webos/`) blocks `us.ad.lgsmartad.com`, `us.info.lgsmartad.com` and `us.rdx2.lgtvsdp.com`, and stops `rdxd` and `uploadd`. CleanYourTV's list only covers the NL ad servers. Undo: delete the file and restart.
- Uninstalled 8 Ball Pool (2 MB) through webOS. Undo: reinstall from the LG store.
- LG's built-in apps live on the read-only system partition and cannot be removed; they stay hidden from Home.
- The user deleted the two store-demo videos in `/media/preload/storedemo` (about 600 MB), used only by `com.webos.app.store-demo`. App storage went from 881 MB to 281 MB used, 1.8 GB free. Not recoverable. Factory reset behaviour without them is untested.

### 10. Speed pass (done; reboot verified)
- Launcher: removed backdrop blur, focus glow, tile animations and shadows. Measured on the TV over 20 focus changes: median 150 ms with blur, 25 ms without. The video wallpaper made no measurable difference, so it stays.
- `/var/lib/webosbrew/init.d/17-lg-slim` (source in `webos/`), about 80 seconds after boot: stops `preload-manager`, `miracast`, `broadcast-conf-downloader`, `rec-info-adapter`, `rec-listmgr`, `tv-reservation-agent`, `lgchannelurl`; closes preloaded Netflix and YouTube; stubs `homekit` and `mqtt-client`. Result: available memory about 310 MB to about 375 MB, swap used about 350 MB to 150 MB.
- Side effects: Netflix and YouTube cold start instead of being preloaded; Miracast, HomeKit and scheduled recordings are off.
- Undo: delete the file and restart. To pause it, create `/media/developer/cleanyourtv/DISABLE-SLIM`.

### 11. Launcher UI pass (done)
- Rounded focus ring with a dark gap so it shows on white tiles, focus scaling, icons filling the tile using the icon's corner colour, spaced clock cards.
- Input tiles show LG's input icons, copied to `app/input-*.png` from `/usr/palm/plugins/inputapps/assets/2k/medium/` (the page cannot read that system folder directly).
- Rearranging: hold OK on a tile, use the arrows, press OK or Back to place. The order is saved in the launcher's localStorage (`home-order`).
- Measured: tile move 32 ms median. A simulated long-press move worked and survived a launcher reload. The user confirmed rearranging works on the real remote. Keeping the order across a full reboot is untested.
- Undo: as entry 5.

### 12. Android Home polish (done)
- Two states as on Android Home: first six tiles in a glass dock; Down opens a full-screen sheet with all apps and a small clock; Back returns to the dock.
- Added: tile drop shadow on focus, 1.15 lift, light across tile tops, top-left shade behind the clock, card shadows and hinge pins, date text shadow, name shadow, edge nudge, load-in wave.
- Single dock blur only. Measured: tile move 13 ms in the dock, 18 ms in the grid (17 with blur); 61 fps with the wallpaper playing, with or without it.
- Undo: as entry 5.

### 13. Full feature pass (done; reboot verified)
- Rearranging now inserts the tile and moves the others along instead of swapping. Hold OK opens a tile menu: Move, Hide, folders, Uninstall.
- Tiles 252 x 142 with a 46 px gap, so a focused tile at 1.15x clears its neighbours.
- Dock blur removed, because the blur over the video plane was the likeliest cause of the black block. Fixed `inset` CSS, which Chromium 79 ignores.
- Added: app colour glow, weather icons, flap and navigation sounds, Cards/Flip/Plain clock, clock position and size, 24-hour time, Dark/Light/Auto, tile style, Lite effects, Control Center (volume, inputs, network, When TV Turns On, power), Personalize, weather location search, wallpaper categories, 113 aerial clips, screensaver with drifting clock, place caption and headlines, hidden apps, folders, passcode, Plex link with My Videos and My Photos as wallpaper sources.
- New files: `/media/developer/cleanyourtv/home-svc.js` (local service on 127.0.0.1:8766 for volume, network, news and power-on target), `/var/lib/webosbrew/init.d/12-home-svc`, `app/ui_*.wav`, `boot-target`. `20-home-on-power` now opens `boot-target`, or Home when it is empty.
- Measured: tile move 20 ms in the dock, 24 ms in the grid.
- Not ported: Liquid Glass (needs shaders), Home Guard, update installer and phone setup page (Android only).
- Undo: as entry 5, plus delete `12-home-svc` and `home-svc.js`, and restart.
- Incident: during testing a `turnOffScreen` call blanked the screen for under a minute. `turnOnScreen` restored it.

### 14. Baked dock frost (done)
- `frost/frost-<n>.jpg`: one 96x54 blurred frame per clip, baked on the Mac with ffmpeg (no live blur) and copied into `app/`. Shown through the dock, lined up with the picture, under a milky tint. Plex wallpapers fall back to the plain tint.
- Undo: as entry 5.

## Final health check (2026-10-30, 34 minutes after reboot)
- Firmware 03.53.45, root on, failsafe off, updates blocked, telnet closed, Quick Start+ on. Open to the network: SSH (key only) and LG's own ports 3000 and 3001. The launcher helpers listen on 127.0.0.1 only.
- About 340 MB memory available, 275 MB swap used, 1.8 GB app storage free, no out-of-memory kills, no crash reports.
- All six startup hooks present; ad and update host blocks in place; debloat and slim services stopped as intended.
- Failed units: `nudge` is from the debloat. `accountmanager`, `bugreportd`, `crashreportd`, `dropbear` and `kadpd` are not traced; the TV works without them. Homebrew Channel starts its own SSH server.
- Open: passcode, folders and sounds are not exercised on the real remote.

### 15. CleanYourTV 1.6.11 (done; reboot verified)
- Updated from 1.6.10 with its checksum-verified installer. The launcher (99.0.0), debloat options and all hooks carried over. Its own `debloat-stop.sh` now has the `admanager` fix, so the hand-applied copy is gone.
- After a full reboot: Disney+ reached its home screen, HBO Max reached the profile screen, Home returned, and tile order and settings persisted. All five stubs and the ad and update blocks came back; telnet stayed closed.
- Backups on the TV: `backup/app.home-before-1.6.11`, `backup/debloat-stop.before-1.6.11.sh`, `backup/config.before-1.6.11.json`.

### 16. Dock centring, logo parity, dark mode (done)
- Tile row centred in the dock (849 to 991 in a dock from 800 to 1040).
- Each logo's bounds are measured once from its icon and every logo is scaled into the same 168 x 92 box. Results are cached in `home-art-1`.
- `home-svc.js` serves each store app's 192 px icon at `/icon`, falling back to the 130 px one for LG system apps.
- Dark Glass tiles remove each icon's plate and turn near-black type white. They apply only in dark appearance, and also dim the wallpaper 30%, as Android's Dark does. In light appearance tiles stay Full Color.
- Measured: tile move 20 ms in the dock, 21 ms in the grid.
- Undo: as entry 5.

### 17. Logo pack for dark tiles (done)
- New shared folder `logos/`: trimmed masters in `dark/` and `manifest.json` mapping each brand to its webOS and Android app IDs. Ten logos came from the user; Spotify is a Simple Icons vector.
- `build.py` exports `logo-<brand>.webp` (about 3 to 18 KB each) into `app/`. Dark Glass shows pack logos and only processes icons for apps without one. Logos are scaled to equal visual area.
- Spotify is not installed on this TV now, so its logo is unused here.
- Undo: as entry 5.

### 18. Focus ring, flip cards, baked blur (done)
- Tile focus ring stays white in light mode, through its own `--ring` colour.
- Clock cards rebuilt as split-flaps with Android Home's timing: 460 ms flip, 60 ms stagger, 140 ms run through digits, shading and a bounce on landing.
- The video plane cannot be blurred: `drawImage` of the playing video returned black pixels (avg 0), and CSS blur cannot reach it. `bake_frost.py` bakes one softly blurred frame every 10 seconds per clip into `sheet-<n>.jpg` (about 7 MB in all). Home shows them in step with `video.currentTime`, clipped to the dock and opening to the full sheet, under a translucent tint in both themes. Plex wallpapers fall back to the plain tint.
- Measured when settled: tile move 20 ms in the dock, about 20 to 35 ms in the sheet. Opening or closing the sheet takes about 130 to 200 ms.
- Undo: as entry 5.

### 19. Theme colours by surface (done)
- Text and shadows now follow the surface behind them: white with a dark shadow over the wallpaper and dock in both themes, dark on the pale all-apps sheet in Light, white on the sheet in Dark. Tile and dock shadows are softer in Light. See-through icons keep a dark plate in Light so white icons stay visible.
- Auto appearance now rechecks every 5 seconds instead of every 30 minutes.
- Check on the TV: computed colours, backgrounds and shadows of 14 elements compared across Dark, Light, Dark, Light, Dark in both tile styles. No mismatches.
- Undo: as entry 5.

### 20. App Tiles independent of Appearance (done)
- Dark Glass now applies in Light too, with its own dark plate, light logos and light labels. The 30% wallpaper dim stays tied to Dark Glass in dark appearance.
- Check on the TV: all four Appearance and App Tiles combinations, twice each, no mismatches.
- Undo: as entry 5.

## Checks on the TV (2026-10-30)
- After a forced reboot: Home takeover, update block, debloat stubs and closed telnet all came back.
- Launching YouTube and returning Home both worked.
- Screen capture shows the launcher. It does not capture the video plane, so the wallpaper needs checking by eye.
