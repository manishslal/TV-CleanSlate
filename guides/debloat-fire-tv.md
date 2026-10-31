> **How to use this guide:** this is a prompt for an AI coding agent with ADB access. Fill in the device details, then paste the whole file into your agent. Read [safety.md](safety.md) first.

# Fire TV Debloat — Master Prompt (v2, updated 2026-09-06 after Fire TV Stick)

Goal: Remove Amazon's newer bloat (Luna, Fire TV Channels, Amazon Live
shopping, and whatever "Music Videos / News / Sports / En Español" turn
out to actually be), disable unused factory apps, and speed the stick up
— WITHOUT breaking the Home button, HDMI-CEC input switching, Alexa
remote, Prime Video, or DRM playback.

RULES (follow strictly, same spirit as the Android TV version):

0. PRE-FLIGHT, BEFORE TOUCHING ANY PACKAGE. Confirm ADB survives a clean
   reboot on THIS device before assuming it's a safety net:
     adb connect <IP>:5555
     adb shell reboot
     (wait ~90 seconds)
     adb connect <IP>:5555
   If ADB does not reconnect within a couple of minutes on a stick with
   ZERO changes made, stop. Also check Settings > My Fire TV > About for
   the exact model name and note it in the changelog.
   - If `adb connect <IP>:5555` returns "Connection refused," sanity-check
     the IP before assuming it's wrong-device — a `.1` address is very
     likely the router/gateway, not the stick. Ask the user to confirm.
   - If it returns "failed to authenticate," have the user accept the
     on-screen RSA key popup ("Always allow from this computer") on the
     TV, then reconnect.

1. NEVER UNINSTALL. Prefer:
     pm disable-user --user 0 <package>
   Everything must be reversible via:
     pm enable <package>
   Do NOT use `pm uninstall` on system packages, even with -k or --user 0.
   VERSION FALLBACK: on older Fire OS, `disable-user` may return "Unknown
   command." If that happens, fall back to plain `pm disable <package>`
   (reversed with `pm enable`), or `pm hide <package>` / `pm unhide
   <package>` — check which form the device accepts.
   - **Two distinct failure modes, don't conflate them:**
     - `SecurityException: Cannot disable a protected package` = the OS
       itself has hard-blocked this package at the framework level. This
       is NOT a version-compatibility issue and `pm hide` will not work
       around it either — see next point.
     - `SecurityException: Neither user 2000 nor current process has
       android.permission.MANAGE_USERS` on a `pm hide` attempt = `pm hide`
       requires a permission unprivileged ADB shell doesn't have without
       root. Since Rule 2 forbids root, **`pm hide` is not a viable
       fallback for OS-protected packages on stock Fire OS** — if
       `disable-user` says "protected package," stop, leave it enabled,
       document it, do not attempt further workarounds.

2. NO ROOT, NO CUSTOM ROMS, NO BOOTLOADER UNLOCK. Stock, non-rooted
   debloat only. Rooting can trip Widevine down to L3 and is not
   reversible the same way pm disable is.

3. MEASURE BEFORE & AFTER, per device. Save to a file named after the
   device:
     adb shell dumpsys meminfo | grep -E "Total RAM|Free RAM|Used RAM"
     adb shell pm list packages -d
     adb shell pm list packages -f
   Repeat all three at the end.
   - Don't over-index on "Free RAM" going up — Android's memory manager
     keeps unused RAM filled with reclaimable cache by design. A few
     thousand K of "Free RAM" moving in either direction after disabling
     ~20 packages is normal and not a signal of success or failure. The
     meaningful outcome is fewer active packages and no bloat
     auto-restarting in the background, not a raw free-RAM increase.

4. BATCHES OF 10, WITH A REBOOT AFTER EVERY BATCH.
     a) adb shell reboot, wait ~90 seconds, confirm ADB reconnects —
        every batch, not just at the end.
     b) If it doesn't come back within a couple of minutes, see "if
        something breaks."
     c) If it reboots fine, ask for physical TV test: Home button,
        HDMI input switching, Alexa voice remote (media + search
        command), Prime Video playback, Netflix/3rd-party DRM/HD,
        on-screen keyboard, phone casting.
   Do not proceed to next batch until confirmed.
   - **Execution lesson: never issue multiple `pm disable-user`/`pm
     enable` calls via a single shell for-loop** (e.g. `for pkg in
     $PKGS; do adb shell pm disable-user --user 0 "$pkg"; done`). This
     was observed to silently execute only the first iteration and skip
     the rest with no error, in this tool environment. **Issue each
     package as its own separate command**, and always verify the final
     state afterward with `pm list packages -d` (count should match what
     you intended to disable).
   - If a mid-batch ADB command returns `error: closed`, reconnect
     (`adb connect <IP>:5555`) and verify the connection is genuinely
     alive with a trivial `adb shell echo "test"` before retrying the
     real command — don't assume a single dropped command means the
     device rebooted or crashed.

5. NEVER DISABLE (translate patterns, protect every match):
     com.amazon.tv.launcher, com.amazon.device.controllermanager,
     com.amazon.tv.settings, *routing*/*hdmi*/*cec*, *firebat*/*avod*,
     *drm*/*widevine*/*securemedia*/*playready*, *keyboard*/*ime*,
     com.android.shell, com.amazon.ssdpservice/*dial*,
     com.google.android.gms/.gsf/com.android.vending (only if present),
     ANY package not explicitly approved in a batch. If unsure, ASK.

6. DISCOVERY BEFORE DISABLING ANYTHING NEW OR AMBIGUOUS.
     a) Open the app/tile on the TV so it's foreground.
     b) adb shell dumpsys window | grep mCurrentFocus
     c) Use that real package/activity name.
     d) Show the user before adding to a batch.
   For Music Videos/News/Sports/En Español: if these are tabs inside one
   aggregator app rather than separate apps, say so plainly.
   - **Real Fire OS 8.x package-name findings (confirmed via this
     method, do NOT trust common/community-assumed names — they were
     wrong for every one of these):**
     - `com.amazon.spiderpork` = **Luna** cloud gaming (NOT
       `com.amazon.luna`, `gamehub`, or `cloud9`)
     - `com.amazon.hedwig` = **Fire TV Channels** aggregator — "Music
       Videos / News / Sports / En Español" are confirmed **tabs inside
       this one package**, not separate apps
     - `com.amazon.shoptv.firetv.client` (+ paired `com.amazon.
       shoptv.client`) = **Amazon Live** shopping (name changed from the
       2017-era `com.amazon.shoptv.client` alone)
     - `com.amazon.tahoe` = **Amazon Kids+**
     - `com.amazon.bueller.photos` / `com.amazon.bueller.music` =
       **Amazon Photos / Amazon Music**
     - Tiles that route through `com.amazon.firebat` (the protected
       Prime Video engine) — e.g. "The Great Courses Signature
       Collection" — are not separate packages at all; leave untouched,
       there is nothing to disable.
   - **Faster pre-discovery triage (new this session):** before
     requiring the user to physically open an app on the TV, run
     `adb shell dumpsys package <pkg>` and check its intent filters:
     - `android.intent.action.MAIN` + category `LEANBACK_LAUNCHER` = a
       real home-screen tile — worth full discovery before touching.
     - `MAIN` + `LAUNCHER`/`DEFAULT` only = reachable but not necessarily
       tiled on the home screen.
     - No `MAIN` activity at all = background-only service/library, not
       a user-facing app — usually safe to defer live discovery on
       these and reason from the name/dependency context instead.
   - **Stub-vs-installed distinction (new this session):** a package
     appearing in `pm list packages` does NOT mean it's "installed" in
     the App Store sense. Before assuming disabling a package will
     remove its home-screen tile, open it once and check whether the tile
     shows an **"Open"** button (genuinely installed/used) or only a
     **"Download"** button (dormant stub, never activated). Disabling a
     dormant stub cannot remove a home-screen tile that's rendered by
     Amazon's server-side promotional content feed — that's a launcher
     UI decision, independent of local package state. The only
     (untested) lever for that is long-pressing the tile on the remote
     to check for a native "Remove from row" / "Hide" option — not an
     ADB/pm action.

7. CANDIDATE BLOAT LIST (confirm each with step 6 before disabling):
   Tier A: com.amazon.precog, com.amazon.videoads.app,
   com.amazon.advertisingidsettings, com.amazon.alexashopping,
   com.amazon.vizzini (only if user doesn't use voice to control playback
   — if the user has Echo devices that voice-control the TV, ask first;
   there's no reliable public documentation separating "remote-only" vs.
   "Echo-device-targeting-TV" control paths for this package, so default
   to leaving it enabled unless confirmed safe).
   Tier B (needs discovery): Luna (`com.amazon.spiderpork`), Amazon
   Live/shopping (`com.amazon.shoptv.firetv.client` +
   `com.amazon.shoptv.client`), Fire TV Channels (`com.amazon.hedwig`),
   Music Videos/News/Sports/En Español (tabs inside hedwig, not separate
   packages).
   Tier C (this session's additions, low-risk once discovery-confirmed
   dormant/stub): `com.amazon.dummy.*` placeholder stubs (alarmclock,
   calendar, contacts, gallery, music, settings, soundpicker),
   `com.amazon.tv.easyupgrade`, `com.amazon.tv.releasenotes`,
   `com.amazon.tmm.tutorial`, `com.amazon.storm.lightning.tutorial`,
   `com.amazon.perfcollection`, `com.amazon.media.recommendations`,
   `com.amazon.hybridadidservice`, `com.amazon.bueller.photos`,
   `com.amazon.bueller.music` (confirm dormant-stub status per Rule 6
   before assuming any home-screen tile will disappear).
   - **Known OS-protected package (do not attempt):**
     `com.amazon.ftvads.deeplinking` — blocked by `pm disable-user` with
     `SecurityException: Cannot disable a protected package`, and `pm
     hide` blocked by missing `MANAGE_USERS` permission. Leave enabled,
     document, move on — do not chase further workarounds without root.
   - **Held back / no corroborating evidence either way (left untouched
     in Fire TV Stick):** `com.amazon.diode` (found explicitly excluded in
     one community debloat script for an older Fire OS with no
     explanation given), `com.amazon.tigris`, `com.amazon.systemnotices`,
     `com.amazon.tv.earlyaccess`, `com.amazon.aca`, `com.amazon.gamehub`,
     `com.amazon.cloud9` (unconfirmed function).

8. SPEED TWEAKS (safe):
     settings put global window_animation_scale 0.5
     settings put global transition_animation_scale 0.5
     settings put global animator_duration_scale 0.5
     pm trim-caches 1G

9. OPTIONAL/ADVANCED/SEPARATE APPROVAL REQUIRED — do NOT do this in the
   same pass, only if explicitly asked:
     adb shell pm disable-user --user 0 com.amazon.device.software.ota
   On some builds only `pm hide`/`pm unhide` works. Freezes Fire OS
   updates (stops silent bloat/ad re-adds) but: (a) stops security
   patches, (b) a factory reset silently re-enables updates and wipes
   this freeze + all other disables. Ask separately, explain both
   tradeoffs, only do if confirmed.

10. LOGGING, PER DEVICE. Separate changelog per stick:
      changelog-<room>-<model>.md
    listing every package disabled, reason, date. Do not merge logs
    across devices.
    - When a post-disable regression is reported (something behaves
      differently after a batch), don't assume it's caused by the batch.
      Root-cause it first: `adb shell dumpsys activity activities | grep
      -A3 ResumedActivity` to see what's actually in the foreground, and
      `adb logcat -c` (clear) then reproduce the issue live and `adb
      logcat -d | grep -i <keyword>` to capture the real error. A crash
      signature inside the OEM's own unmodified launcher/framework code
      (e.g. `ActivityTaskManager IllegalStateException`) is evidence of a
      pre-existing platform bug, not necessarily something your batch
      caused — bisect the most likely single candidate by re-enabling it
      and retesting before concluding a package is responsible, and
      document a negative result (bisection ruled a candidate out) just
      as clearly as a positive one.
    - `uiautomator dump` is useful to confirm whether a UI element is
      genuinely internally mislabeled vs. correctly labeled but routed
      to the wrong destination by app logic.

11. MULTI-DEVICE WORKFLOW. One Fire TV stick start-to-finish before
    starting the next. Do not assume a package found on one stick exists
    on another.

STEPS TO EXECUTE (per device): (a) confirm/install adb, connect, pre-flight;
(b) gather/save initial metrics; (c) list packages, group into table
(Tier A confirmed/Tier B needs discovery/unknown), before disabling
anything; (d) run discovery on every Tier B/ambiguous candidate; (e)
disable in batches of 10, stopping for test after each; (f) apply speed
tweaks; (g) reboot; (h) re-run metrics, before/after comparison; (i)
changelog + one-line revert command; (j) ask to move to next stick.

FINISHING UP (after all devices): summary table — RAM before/after,
packages disabled, anything skipped because discovery couldn't confirm.

---

## Changelog of prompt changes (v1 → v2, from Fire TV Stick session, 2026-09-04/06)
- Added pre-flight IP/auth troubleshooting notes (Rule 0)
- Split the "protected package" `SecurityException` out as its own
  documented failure mode, distinct from the "Unknown command"
  version-fallback case (Rule 1)
- Documented that `pm hide` is not a viable fallback for OS-protected
  packages without root — `MANAGE_USERS` permission required (Rule 1)
- Added the for-loop execution bug and its fix: always issue disable/
  enable commands individually, verify with `pm list packages -d` (Rule 4)
- Added the `error: closed` reconnect-and-verify pattern (Rule 4)
- Added confirmed real package names for Fire OS 8.x Tier B candidates
  (Rule 6) — none matched common/assumed names
- Added `dumpsys package <pkg>` intent-filter triage as a pre-discovery
  step before requiring live on-device navigation (Rule 6)
- Added the stub-vs-installed distinction and the promotional-tile
  caveat — `pm disable-user` cannot remove a server-rendered home-screen
  tile for a never-activated stub package (Rule 6)
- Added Tier C (stub/tutorial/telemetry low-risk candidates) and the
  known-protected-package list to Rule 7, based on this session's finds
- Added root-cause-before-blaming-a-batch guidance using
  `dumpsys activity activities` and `logcat`, plus bisection-by-
  re-enabling methodology, to Rule 10
