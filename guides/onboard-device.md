# Onboard a TV

Use this when someone points at a TV and says install Home on it, set it up, or get it ready. It is the whole path in one place, so nobody has to ask for each step. One command covers every TV we support; it finds out which kind is at the address:

| TV | Found by | What `--yes` does |
|---|---|---|
| Android TV, Google TV, Fire TV (onn, SHIELD, TCL, Fire sticks) | ADB on port 5555 | Back up, install, debloat the model's tested list, verify |
| Samsung (Tizen) | `http://<tv-ip>:8001/api/v2/` answers | Build and install through `tizen/deploy.sh` once the requirements are met |
| LG (webOS, rooted) | ports 3000 or 3001 | Nothing: it prints the plan and the owner's part. Rooting is not automated |
| Hisense | ADB | Back up only. Home isn't installed there and its debloat didn't persist |

Read [safety.md](safety.md) once. The rules in `AGENTS.md` apply to every step.

## The short version

```bash
scripts/onboard.sh <tv-ip>          # reads the TV, prints the plan, changes nothing
scripts/onboard.sh <tv-ip> --yes    # runs the plan
```

The first command is the one approval: it shows what the TV is, which build goes on it, which packages will be disabled, and what is known to go wrong on that kind of TV. Read it, then run the second.

Android options: add `--keys` only when nobody is using the remote (keys from the script mix with theirs). Add `--reboot` to prove Home survives a restart. Add `--close-adb` when you are done. `EXPECT=<model>` stops the run if the address belongs to another device. `--platform=android|tizen|webos` skips the detection.

Samsung options: `--shot` takes a screenshot of Home afterwards (it opens a debug port until Home restarts), and `--close-debug` proves the sdb port is closed after the owner switches Developer Mode off.

## What it does on Android, Google TV and Fire TV

| Step | By | Changes the TV? |
|---|---|---|
| Identify: maker, model, firmware, SDK, RAM, text size | `onboard.sh` | No |
| Pick the profile from `devices/profiles.json` | `scripts/device-profile.py` | No |
| Back up packages, settings, Home owner, a screenshot | `scripts/01-preflight-backup.sh` | No |
| Install the build the profile names | `android/tools/install.sh` | Yes |
| Verify: Home installed, Home key answered, Settings stays open, nothing protected disabled | `scripts/verify-device.sh` | Briefly flips the screen |
| Disable this model's tested list, five at a time, verify after each | `scripts/05-disable-list.sh` | Yes |
| Optionally switch ADB off and prove it | `scripts/adb-closed.py` | Yes |

It stops at the first failure, says what to undo, and writes a report under `backups/`.

## What it does on a Samsung

`scripts/onboard-web.sh` reads the TV through `tizen/preflight.sh` (model, platform code, Developer Mode, sdb port), looks the model up in `devices/profiles.json` under `platforms` (a 1 GB model builds with `LEAN=1`; an unknown one starts lean), and checks the requirements: Tizen Studio's `sdb` and `tizen` CLI with a `home` security profile, Pillow, your own logos in `logos/dark/`, and Developer Mode on. Those need the owner: Developer Mode is switched on at the TV (Apps, 1 2 3 4 5, enter this computer's IP, restart). With everything met, `--yes` runs `tizen/build.py` and `tizen/deploy.sh`. The Home button always opens Smart Hub first. Finish by switching Developer Mode off and proving it with `--close-debug`.

## What it does on an LG

Nothing automatic. Home on LG needs a rooted TV, which cannot be undone, and this project does not root TVs. The plan points at `webos/README.md` ("Risks and undo") and the LG device log for the exact steps, then lists what to record.

## What it will not do for you

- **Debloat a model nobody has tested.** The profile says `review` or `none` and the run stops after verification. For those, read `docs/CLEANYOURTV-REVIEW.md` and the debloat guides, find out what each package is (open it, read the focused window, never guess names), show the owner the batch, and add the list as `devices/<device>/debloat-disable.txt`.
- **Copy a list between models.** Lists are per model, under `devices/`.
- **Disable input, remote, keyboard, Wi-Fi or update packages.** `scripts/protected.sh` refuses them, and `verify-device.sh` fails if any is off.
- **Decide for the owner.** Anything that is a judgement call (a browser, smart-home, a purchase service, Live TV) goes in front of them first.

## A new kind of TV

If the plan says "first run for it", the profile is `unknown`: it installs, verifies, and does not debloat. Then:

1. Run `scripts/verify-device.sh <tv-ip>`, then again with `--keys` if nobody is using the remote. A FAIL names what to look at.
2. Find out what Settings, Home and the text size do (`font_scale`; Home's type is sized for 0.85).
3. Fix what failed in the app, as `SystemState.settingsIntent` was for Fire OS 8, and check it on the TV.
4. Add the model under `models` in `devices/profiles.json`, and a profile if it behaves differently, with the quirks you found.

## Record it

The run ends with the list. In short: `devices/<device>/CHANGELOG.md` (what was done and how to undo it), `devices/devices.json` and `docs/SUPPORTED_DEVICES.md` together, `docs/LESSONS.md`, `logos/README.md` for logo gaps, `docs/parity.json` if a feature changed, and the profile. Use `<tv-ip>` in anything committed, never the address.

## Finish

Switch debugging off and prove it: `--close-adb`, or by hand then `python3 scripts/adb-closed.py <tv-ip>`. On a Fire TV that means turning ADB debugging back on in Settings before the next visit.
