# PS3/XMB-Style Launcher for Android/Google TV — Logistics & Safety

Researched 2026-10-28. Sources are listed at the bottom. Claims marked **(unverified)** came from one secondary source or conflict between sources.

## 1. What the reel is
Kevin Adams (@ikevinadams) says he "vibecoded" his Android TV home screen into a PS3 look using Claude. The reel names no repo.
The closest open-source match is **XMB TV Home** (`sanangoyal/XBM-TV-Home`): MIT license, native APK with a WebView UI, Android 9+, 8 stars (young, low traction). It ships backup, debloat, restore and check scripts, plus a `PROMPT.md` and `skill/SKILL.md` for AI agents.
Only one device is listed as tested (OnePlus TV, Android 11). Treat it as a starting point to audit and fork, not a finished product.

## 2. Projects with traction
| Project | What it is | Notes |
|---|---|---|
| `Gathin23/cleanyourtv` | Debloat lists per TV model, `disable.sh` / `restore.sh` / `check.py`, own launcher | Very popular. Star count conflicts across sources (5.4K vs 43.4K) **(unverified)**. Debloat uses `pm disable-user`, nothing uninstalled. Records state in `~/.cleanyourtv/` before changing anything. |
| `seun-novodev/android-tv-debloat-toolkit` | Windows GUI (Python) | Bundles FLauncher. Antivirus flags it, so skip it. We are on a Mac. |
| FLauncher / LTvLauncher / Projectivy | Safe fallback launchers | Install one of these as the safety-net launcher. |
| Wave Launcher (WaveLauncher/WaveLauncher) | Android XMB launcher | Built for retro handhelds; Android TV support not stated. |
| freya.cat guide | Manual procedure | Source of the "wrong order bricks it" warning. |

Fire TV: Fire OS will not let a third-party launcher own Home. That matches our earlier Fire Stick project. This plan is for **Android TV / Google TV only**.

## 3. Mistakes others made, and our own
1. **Disabling the stock launcher before a replacement is installed and set as Home.** This gives a black screen or soft brick. Install, verify, then disable. Never the reverse.
2. **Google TV `setupwraith` re-takes Home** after a reboot and can crash-loop. It may be disabled too, but check Wi-Fi first because disabling it may delete the saved network (cleanyourtv README).
3. **Disabling Gboard before a replacement keyboard is installed.**
4. **Disabling input and tuner packages**, for example `com.mediatek.wwtv.tvcenter` or similar HDMI/TV-input packages. This breaks the Inputs menu. Our Hisense run hit this: input switching broke, and only a full rollback plus reboot fixed it.
5. **Disabling the fused location package.** One source reports a boot loop.
6. **Firmware updates re-enable packages and reset Home.** Re-run the check script after every update.
7. **Root or bootloader unlock.** It factory-resets the TV and can drop Widevine L1 to L3, which caps Netflix at SD. We keep the no-root rule.
8. **Some devices reset package state on reboot.** Our Hisense reverted all `disable-user` changes on reboot. Test reboot persistence before any real changes.
9. **Some devices cut USB power while debugging is on**, and plaintext network ADB on port 5555 stays open. Turn it off after use.
10. **Running unvetted scripts.** PCWorld warns about boot loops from copied lists. Read every script and use model-specific lists only as hints.

## 4. Backup: what is and isn't possible
A true full-disk image is **not** possible without unlocking the bootloader or rooting. Both are out of scope and are the riskiest things on this list. What we can do:

| Layer | How | Restores |
|---|---|---|
| Package state | `pm list packages -f`, `-d`, `-e`, `-s`, `-3` saved to files | Which packages exist and were disabled |
| Home owner | `cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME` | Which launcher owned Home |
| Settings | `settings list global`, `secure` and `system` saved to files | Reference only. Restore selectively, never blindly. |
| Properties | `getprop` | Model, build and Android version, to check compatibility |
| Third-party APKs | `pm path <pkg>` and `adb pull` | Re-installing sideloaded apps (splits need all parts) |
| Launcher config | Exported JSON or theme files from our launcher | Rebuilding the look |
| Restore script | Auto-generated `restore.sh` that runs `pm enable` and re-sets Home | Undoes disables |

`adb backup` is deprecated and ignored by most apps, so we don't rely on it.
**Last-resort recovery** is a factory reset from Settings, or the hardware reset button on Onn boxes (hold at boot for recovery). The Onn 2026 stick reportedly does not do ADB over USB, so recovery depends on the device reaching the OS **(unverified)**.

## 5. Cloning the setup to another device
A "config bundle" is a folder, not a disk image:
```
bundle/
  apks/            launcher APK, fallback launcher APK, keyboard, sideloaded apps
  launcher/        theme, layout, wallpaper and sound assets, config JSON
  models/<model>/  debloat.txt, tested on that exact model
  settings.txt     allow-listed settings (animation scales and similar)
  apply.sh         ordered: install, set home, verify, then disable
  restore.sh
  check.sh         run after firmware updates
```
- **Same model (Onn to Onn):** reuse the model's `debloat.txt` as-is, still behind the safety gates below.
- **Different model or brand:** reuse the APKs and launcher config only. Re-run discovery for the package list. Never copy a disable list between models. Our Hisense run showed vendor packages differ completely.
- Google accounts and app logins are not portable. Sign in again.

## 6. Safety protocol (gates in order)
0. Pre-flight: record model, build and Android version. Confirm ADB works. Reboot once with **no changes** and confirm the device comes back and ADB reconnects.
1. Run the backup (Section 4). Don't proceed unless the files exist and are non-empty.
2. Install the fallback launcher (FLauncher). Confirm it opens.
3. Install the XMB launcher. Confirm it opens and Back and Home work. Take a screenshot.
4. Set the XMB launcher as Home. Reboot. Confirm it still owns Home.
5. Only then disable the stock launcher, in a batch of one. Reboot. Confirm Home, Inputs menu, remote and Wi-Fi.
6. Disable bloat in batches of 5 (the Onn RAM tier). Check after each batch. Keep the restore script updated before each one.
7. Test recovery on purpose: run `restore.sh` once and confirm the stock Home returns.
8. Close network ADB. Save a changelog in the project like the earlier ones.

Hard rules carried over from the earlier prompts: disable only, no uninstall, no root, no bootloader unlock, and never touch input, remote, keyboard, Wi-Fi or update packages.

## 7. Open questions for you
1. Which TV(s) first? Model and Android TV or Google TV version decide the plan. The Hisense unit may revert changes on reboot (see Section 3, item 8), so an Onn box is a better first target.
2. Do you want to **audit and fork XMB TV Home**, or have Claude build a new XMB launcher? Building a new one is a bigger job. Forking an 8-star repo means reviewing all of its code first.
3. Is a TMDB API key OK for the trending artwork?

## Sources
- https://github.com/Gathin23/cleanyourtv
- https://github.com/sanangoyal/XBM-TV-Home
- https://freya.cat/blog/post/2025-03-11
- https://www.pcworld.com/article/3266473/please-dont-use-ai-hacks-to-debloat-your-tv.html
- https://tv.cobanov.dev/en/
- https://xdaforums.com/t/how-to-disable-google-tv-launcher-install-3rd-party-launcher.4601397/ (blocked, 403; recovery commands taken from search summary)
- https://retrohandhelds.gg/wave-launcher-drops-v1-0/
- https://www.instagram.com/reel/De7550oyBjx/
- Local history: `GoogleTV-Debloat/Living-Room-TV/changelog-*.md`, `FireTV-Debloat/`
