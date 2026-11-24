# Onboard a TV

Use this when someone points at a TV and says install Home on it, set it up, or get it ready. It is the whole path in one place, so nobody has to ask for each step. It covers Android TV, Google TV and Fire TV. Samsung and LG have their own scripts (`tizen/deploy.sh`, `webos/`).

Read [safety.md](safety.md) once. The rules in `AGENTS.md` apply to every step.

## The short version

```bash
scripts/onboard.sh <tv-ip>          # reads the TV, prints the plan, changes nothing
scripts/onboard.sh <tv-ip> --yes    # runs the plan
```

The first command is the one approval: it shows what the TV is, which build goes on it, which packages will be disabled, and what is known to go wrong on that kind of TV. Read it, then run the second.

Add `--keys` only when nobody is using the remote (keys from the script mix with theirs). Add `--reboot` to prove Home survives a restart. Add `--close-adb` when you are done. `EXPECT=<model>` stops the run if the address belongs to another device.

## What it does

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
