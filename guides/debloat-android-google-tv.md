# Android TV / Google TV Debloat — Master Prompt (v2, updated 2026-09-06 after Hisense)

I want you to debloat my Android TV / Google TV device via ADB.
Goal: remove OEM/Google bloat, speed it up, without
breaking the Home button, HDMI-CEC input switching, remote pairing, or
DRM playback.

MY DEVICE (fill in per box, one at a time):
  Label:
  Model:
  Interface: Android TV / Google TV
  Android TV OS version:
  Android TV OS build:
  IP Address:
  Mac Address:

"Developer options" and "USB debugging" are enabled. My computer and the
device are on the same Wi-Fi.

RULES (follow strictly):

1. NEVER UNINSTALL. Only ever use:
     pm disable-user --user 0 <package>
   Everything must be reversible via:
     pm enable <package>
   Do NOT use `pm uninstall` on system packages, even with -k or --user 0
   — WITHOUT the user explicitly overriding this rule in the moment, in
   writing, for that specific device. Even then, treat it as a genuine
   exception, not a new default: re-confirm before every use, and log it
   clearly as an override each time.
   - **New finding (Hisense/VIDAA Android TV 12, this session):** on some
     OEM builds, NEITHER `pm disable-user --user 0` NOR `pm uninstall
     --user 0` survives a true reboot — both get silently reset back to
     enabled/installed, while only packages baked into the factory image
     stay disabled. This looks like the device restoring a package-state
     snapshot on boot rather than a normal AOSP reboot. See Rule 3a
     (persistence test) — run it BEFORE planning any batches, not after.

2. NO ROOT, NO CUSTOM ROMS. Shield TV is historically very rootable —
   don't. We only disable packages; this stays fully stock and reversible.
   - If persistence testing (Rule 3a) shows the device resets all
     ADB-applied package changes on reboot, root is very likely the ONLY
     way to make changes stick. This is a real, distinct decision from
     "should we debloat at all" — surface it explicitly and get a clear
     yes/no on accepting the DRM/Widevine trade-off before ever touching
     it. Default to NOT rooting; most users should stop at "session-only
     debloat" instead (see Rule 3a).

3. MEASURE BEFORE & AFTER, per device. Save to a file named after that
   device:
     adb shell dumpsys meminfo | grep -E "Total RAM|Free RAM|Used RAM"
     adb shell pm list packages -d
   Repeat both at the end.
   - **If `dumpsys meminfo` times out** (`SERVICE 'meminfo' DUMP TIMEOUT`
     — seen on underpowered Hisense/MediaTek hardware before reaching the
     RAM summary section), fall back to `adb shell cat /proc/meminfo`
     (MemTotal/MemFree/MemAvailable) as the equivalent metric, both
     before and after, and note the substitution.
   - Don't over-index on "Free RAM" moving up — Android keeps unused RAM
     filled with reclaimable cache by design; a few thousand K moving
     either direction after a debloat pass is normal, not a signal.

   **3a. TEST PERSISTENCE BEFORE PLANNING ANY BATCHES (new step).** Right
   after the Rule 0-equivalent reboot-survival check, run one more test:
   disable 1-2 low-risk, already-discovered packages, verify disabled via
   `pm list packages -d`, reboot, and check again. If they're still
   disabled after reboot, `pm disable-user` works normally on this
   device — proceed as normal. **If they've been silently re-enabled**,
   stop and tell the user before doing anything else: explain that
   changes will only last until the next true reboot, ask whether their
   device is normally power-cycled via a true reboot or a low-power
   standby/"Quick Start" (which may not trigger the same reset — untested
   from ADB, no safe way to simulate it remotely), and let them decide
   whether session-only debloating is still worth doing. Do NOT run a
   large batch under Rule 4 without this answered first — see the Living
   Room TV incident in the Rule 4 changes below for why.

4. BATCH SIZE. Shield TV: batches of 10, as usual. Onn 4K / 4K Plus, and
   any device with ~2GB total RAM (check via Rule 3/3a's meminfo output
   before assuming Shield-class): use batches of 5 instead, and reboot to
   confirm stability after every batch, not just at the end of a run.
   Stop after each batch and ask me to test:
     - Home button
     - HDMI input switching / HDMI-CEC (TV remote control of the box, if used)
     - Remote pairing/Bluetooth reconnect after a reboot
     - A streaming app I actually use (confirm HD/DRM playback still works)
     - Voice search / Assistant button on the remote
     - On-screen keyboard
   Do not proceed to the next batch until I confirm all of the above.
   - **If Rule 3a found that changes don't survive reboot:** rebooting
     after every batch to "confirm stability" is pointless AND
     destructive — it just erases the batch you're trying to test, with
     no stability information gained (we already know it resets). Adapt
     to: disable the batch, live-test the full checklist above WITHOUT
     rebooting, and treat any live regression exactly as seriously as a
     post-reboot one. Only reboot when you actually need one (to recover
     from a regression, or because the user is done for the session).
   - **Large batches carry real regression risk even without root/
     reboot involved.** On the Hisense TV, disabling 25 packages live in
     one session (grouped as 3 sub-batches with no reboot between) broke
     TV input switching. Two name-plausible suspects were re-enabled live
     and did NOT fix it — only a full rollback of all 25 packages plus a
     reboot restored it. Root cause was never isolated to a single
     package. **Lesson: even on a "changes don't persist anyway" device,
     don't take that as license to disable everything in one sweep** —
     the batch-of-5-with-testing discipline exists to catch exactly this
     kind of regression while it's still cheap to bisect. If a regression
     is found on a Rule 5-protected function and re-enabling the most
     likely suspects doesn't fix it quickly, don't keep bisecting live —
     roll back the whole batch and reboot to get to a known-good state
     first, THEN decide whether isolating the exact cause is worth the
     risk of reproducing it.
   - Mid-batch ADB disconnects (`adb: device offline`) happen; reconnect
     and verify state with `pm list packages -d` before continuing, same
     as the Fire TV lesson — don't assume a dropped command means
     anything else went wrong.
   - **Unrelated events can coincide with a batch and look causally
     linked but aren't.** A saved WiFi password was lost during this
     session's batch work; investigated via uptime (continuous, no
     reboot/crash) and logcat (no network-removal signatures) before
     concluding it was an ordinary WiFi/router hiccup, not caused by
     package changes. Don't skip this kind of check, but don't assume
     causation from timing alone either.

5. NEVER DISABLE THE FOLLOWING (grep for these substrings in
   `pm list packages` output on THIS device and protect every match —
   do not assume exact names, confirm what's actually present):
     *tvlauncher* / *launcherx* / *tv.launcher*  -> Home screen itself
     *hotkey* / *keydispatch*                    -> remote's Home/Input
                                                     button routing
                                                     (chipset-specific;
                                                     don't assume MediaTek
                                                     — confirm per device.
                                                     On the Hisense TV
                                                     this was
                                                     com.mediatek.android.
                                                     tv.globalkeyoverlay)
     *hdmi* / *cec* / *routing* / *input* / *source*
                                                  -> HDMI-CEC input
                                                     switching (widened
                                                     this session — see
                                                     below)
     *tv.remote.service* / *btservice* / *bluetooth* / *autopair*
                                                  -> remote pairing
     com.google.android.gms / .gsf / com.android.vending
                                                  -> Play Services / Store
     *drm* / *widevine* / *securemedia* / *playready*
                                                  -> streaming DRM, breaks
                                                     HD/4K permanently if
                                                     disabled
     *keyboard* / *ime* / *inputmethod*          -> on-screen keyboard
     com.android.shell                           -> this ADB connection
     com.google.android.permissioncontroller     -> permission/role management
     com.android.systemui                        -> system UI shell
   If unsure whether something matches, ASK ME before including it.
   - **Widened this session:** the original pattern list didn't catch
     `*input*`/`*source*`, and a Hisense TV input-switching regression
     happened after disabling packages that didn't obviously match any
     protected pattern by name. Package names that are vague, internal,
     or tied to hardware/factory services (`*aidlservice*`, `*factory*`,
     `*evservice*`, anything under a chipset vendor's namespace with an
     unclear one-or-two-word suffix) deserve the same caution as an
     explicit HDMI/input match, even without a clean substring hit —
     when in doubt, treat "sounds like it could touch hardware" as
     protected until Rule 6 discovery proves otherwise.
   - A package can match a protected pattern for reasons that aren't
     obvious from a quick read — e.g. `com.vt.tv.customerlauncher` on the
     Hisense TV had ZERO `LEANBACK_LAUNCHER` activities (not the active
     home screen) but was still left alone purely because "launcher" is
     in the name. That was the right call — don't let a negative
     discovery result (no home tile) override a protected-by-name match.

6. DISCOVERY BEFORE DISABLING ANYTHING OEM-SPECIFIC OR AMBIGUOUS. Same
   method regardless of device: open the app/tile in question with the
   remote so it's foreground, then from the computer run:
     adb shell dumpsys window | grep mCurrentFocus
   That prints the real package/activity on screen. Use that name, not a
   guessed one — especially for anything NVIDIA-branded on the Shield or
   any Onn/Walmart-branded tile.
   - **Faster pre-discovery triage for OEMs with a large ambiguous
     package list (e.g. Hisense's ~50 `com.vt.*`/`com.mediatek.*`
     packages):** before requiring the user to physically navigate to
     each one, batch-check intent filters via a single remote loop:
       adb shell "for p in pkg1 pkg2 ...; do echo === \$p ===; dumpsys package \$p | grep -c LEANBACK_LAUNCHER; done"
     A count > 0 means it's a real home-screen tile — do full discovery
     (or ask the user directly) before touching it. A count of 0 with no
     `android.intent.action.MAIN` at all means background-only — lower
     priority for live discovery, reason from name/permissions/receivers
     instead. This resolved ~50 ambiguous packages in 2 batched calls
     instead of requiring ~50 live navigations.
   - **Use `dumpsys package <pkg>` for more than intent filters** — it
     also shows granted permissions and registered receivers
     (`BOOT_COMPLETED`, etc.), which can reveal an app's real function
     even with zero home-screen presence. On the Hisense TV, this is how
     `com.vt.tv.videocolor` was confirmed as core picture-processing
     (`/system_ext/app/VideoColor`, dedicated service) and how
     `com.vt.tv.protector` was confirmed as a storage-cleaner/optimizer
     utility with a `BOOT_COMPLETED` receiver and unusually broad
     permissions (`DELETE_PACKAGES`, `RESTART_PACKAGES`, `MASTER_CLEAR`)
     — worth checking BOOT_COMPLETED receivers specifically on any
     package suspected of being a state-restoring watchdog.
   - When cross-referencing a user's confirmed-in-use app list against a
     batch of related-but-different packages (e.g. one categorical "do
     you use the OEM smart-features hub?" question covering 6-7
     packages), that's a reasonable substitute for individually opening
     each one live — document it as such in the changelog for
     transparency, same standard as the Fire TV session's app-list
     cross-reference.
   - **Do not follow a device's own hidden service-menu code (e.g.
     Hisense/VIDAA's `1969` Settings > Sound > Balance sequence) if it
     leads to a "reboot into factory mode" prompt rather than a plain
     settings toggle.** This is categorically different from checking a
     setting — it's a full diagnostic/factory-reset-adjacent mode. Cancel
     out and don't proceed; if stuck in the resulting menu, Back/Menu/
     Home should navigate out, and a plain power cycle is safe since no
     changes were made.

7. CANDIDATE BLOAT LIST — confirm each with step 6 on THIS device before
   disabling, group into a table for me, then disable in batches:

   Shared across both families (Google's own, present on any certified
   Android TV / Google TV device):
     com.google.android.tvrecommendations  - powers the "For You" /
                                              Watch Next / sponsored rows
                                              (not present on the Hisense
                                              build tested this session —
                                              don't assume every "shared"
                                              package exists on every OEM)
     com.google.android.katniss            - search/Assistant overlay
                                              feeding the launcher's search
                                              bar; disabling reduces
                                              Assistant-driven suggestions
                                              but also weakens voice search
                                              results — ask before disabling
     com.google.android.backdrop           - ambient/screensaver with
                                              promotional photo content
                                              (also not present on the
                                              Hisense build; that device
                                              uses its own dream/backdrop
                                              stack — check per device);
                                              also run:
                                                settings put secure screensaver_enabled 0
     com.google.android.videos             - "Google TV" movies/shows
                                              storefront tile, if unused

   Shield TV only (verify each via step 6 — NVIDIA package names have
   shifted across firmware years):
     com.nvidia.ota            - OTA updater (protect unless you
                                  deliberately want to freeze updates —
                                  see rule 9-equivalent tradeoff)
     com.nvidia.ocs             - NVIDIA cloud/telemetry service
     com.nvidia.diagtools       - diagnostics, safe to disable
     com.nvidia.feedback        - feedback/crash reporting
     com.nvidia.shield.registration - device registration nag
     NVIDIA Games / GeForce NOW - KEEP if you use GameStream, Steam Link,
                                  or GeForce NOW; only disable if you've
                                  never used Shield's gaming features

   Onn 4K / 4K Plus only (Google TV's own, more ad-dense home screen —
   verify each via step 6, exact strings vary by Android 12 vs 14 build):
     Any package whose foreground activity shows sponsored/promotional
     content in the home row when opened via step 6
     Onn/Walmart-branded setup or registration app, if present

   Hisense (VIDAA vendor layer under Android TV, `com.vt.*` namespace —
   new this session, verify per device/firmware since these are
   internal codenames, not documented anywhere publicly):
     com.vt.tv.smartsense / .smartsense.ui / .smartservice / .aiot /
       .singlelisten / .mediacenter / .tvcenter
                                - VIDAA "smart" features hub (ask
                                  categorically: does the user use any
                                  Hisense-native smart features, or only
                                  the Google TV interface + streaming
                                  apps?)
     com.vt.eula / .factory / .fte / .storemode / .hishow /
       .tv.mobiledeviceconnectiontips / .udpserver / .tv.evservice
                                - one-time setup/legal/demo stubs,
                                  generally low-risk, but confirm each
                                  isn't secretly hardware-adjacent (see
                                  Rule 5's widened caution) — evservice
                                  in particular was a Batch suspect in an
                                  unresolved input-switching regression,
                                  treat with more caution than its "stub"
                                  appearance suggests until proven safe
                                  on a fresh device
     com.vt.tv.protector        - storage-cleaner/optimizer utility with
                                  broad permissions; ask the user if they
                                  use the "Protector"/cleaner tile before
                                  touching, independent of any watchdog
                                  theory (which was tested and disproven
                                  this session)
     com.vt.tv.videocolor, com.vt.tv.commonui, com.vt.tvservice,
     com.vt.avmiddleware, com.vt.source.external, com.vt.source.tuner,
     com.vt.tv.input, com.vt.tv.customerlauncher
                                - PROTECT, confirmed core/hardware/
                                  launcher-adjacent this session
     com.vt.alexa               - same category as Fire TV's `vizzini`:
                                  ask if the user controls this TV via
                                  Echo devices before touching
     com.mediatek.AirplayAPK / .airplaydaemon / .partner.airplay_sys,
     com.vt.tv.airplaykeycheck  - AirPlay/mirroring — ask if used
     com.vt.tv.dlna              - DLNA/local casting — ask if used
     com.mediatek.android.tv.globalkeyoverlay, com.mediatek.autopair
                                - PROTECT: remote key routing and
                                  Bluetooth/remote pairing respectively,
                                  confirmed this session

8. SPEED TWEAKS (safe on both):
     settings put global window_animation_scale 0.5
     settings put global transition_animation_scale 0.5
     settings put global animator_duration_scale 0.5
     pm trim-caches 1G
   - These are `settings put` calls, a different subsystem than `pm`.
     Not yet confirmed whether they survive a reboot on a device where
     `pm`-level changes don't (untested this session) — apply them
     regardless, since the downside of them resetting is negligible
     (animations just return to normal speed), unlike a reset package
     disable which silently un-does the whole point of debloating.

9. LOGGING, PER DEVICE. Separate changelog per box:
     changelog-<room>-<model>.md
   listing every package disabled, a one-sentence reason, and the date.
   - When persistence testing (Rule 3a) fails, say so plainly and early
     in the changelog, before the batch tables — a reader shouldn't have
     to infer from a "final state: 0 net changes" line that the whole
     session's package-level work didn't stick.
   - Document negative/disproven theories, not just what worked — e.g.
     "suspected X as the watchdog, tested, disproven" is valuable
     information for the next session on the same device, exactly like
     documenting a successful discovery.

10. MULTI-DEVICE WORKFLOW. Do one device start-to-finish (all steps, all
    batches, final report) before starting the next. Do the Onn boxes
    with extra caution given the 2GB RAM ceiling — a bad batch has less
    headroom to recover gracefully; keep a factory-reset-via-recovery
    procedure noted as a last resort, not something to reach for.
    - Same extra caution now applies to any device that fails Rule 3a's
      persistence test — not because of RAM, but because there's no
      "let it ride until next reboot" safety net in the same way; a
      regression found mid-session has to be fixed live or via a
      deliberate rollback+reboot, not just "wait, it'll reset itself
      eventually anyway" (that's true for the disables, but not for a
      broken protected function in the meantime).

STEPS TO EXECUTE (per device):
  a) Confirm adb is installed; connect: adb connect <IP>:5555 — accept
     the on-screen prompt on that TV.
  b) Gather and save initial metrics (rule 3), including the persistence
     test (rule 3a) — do this before any planning conversation about
     batches, since it changes the whole approach.
  c) Run `pm list packages`, cross-reference against rule 7's candidate
     table for THIS device's family, mark what's actually present.
  d) Run discovery (rule 6) on every OEM-specific / ambiguous candidate.
  e) Disable in batches per rule 4, stopping for my test after each
     (live-test only, no reboot between batches, if rule 3a found no
     persistence).
  f) Apply speed tweaks (rule 8).
  g) Reboot (to return to a clean, known state if rule 3a found no
     persistence — understand this will erase the batch work; that's
     expected and fine, not a failure).
  h) Re-run metrics from (b), give before/after comparison.
  i) Give me the changelog and a one-line revert command:
       adb shell "$(for p in <list>; do echo pm enable $p\;; done)"
  j) Ask if I want to move to the next device.

FINISHING UP (after all devices): summary table — one row per device —
of RAM before/after, packages disabled, and interface confirmed
(Android TV vs Google TV) so I have a record for next time.

---

## Changelog of prompt changes (v1 → v2, from Hisense session, 2026-09-06)
- Added Rule 3a: test reboot-persistence of `pm disable-user` BEFORE
  planning any batches, not as an afterthought — this fundamentally
  changes the approach on devices where it fails.
- Documented that on some OEM builds, both `pm disable-user --user 0`
  AND `pm uninstall --user 0` (the latter only with explicit user
  override of Rule 1) fail to survive a true reboot — likely a
  boot-time package-state snapshot restore, not fixable without root.
- Softened Rule 1's uninstall prohibition to allow an explicit,
  re-confirmed-each-time user override, rather than an absolute never —
  while keeping the default firmly "don't."
- Widened Rule 5's protected-pattern list to include `*input*`/`*source*`
  and added guidance to treat vague/hardware-adjacent names as
  provisionally protected even without a clean substring match, after
  an unresolved TV-input-switching regression from a 25-package batch.
- Added guidance to roll back a whole batch + reboot rather than keep
  live-bisecting, once a regression hits a Rule-5-protected function and
  the obvious suspects don't fix it quickly.
- Added the batched `dumpsys package` intent-filter triage technique for
  large ambiguous package lists (resolved ~50 Hisense packages in 2
  calls instead of ~50 live navigations).
- Added guidance to check `BOOT_COMPLETED` receivers and permissions via
  `dumpsys package` when a package is suspected of being a state-
  restoring watchdog (tested and disproven for `com.vt.tv.protector`
  this session, but the technique itself is reusable).
- Added explicit caution against following a hidden OEM service-menu
  code into a "reboot into factory mode" prompt — cancel out, don't
  proceed, that's categorically different from a settings toggle.
- Added a Hisense/VIDAA-specific candidate list to Rule 7 based on this
  session's findings, clearly marked as internal/undocumented codenames
  to be reverified per device rather than trusted as universal.
- Added guidance to investigate (uptime + logcat) rather than assume
  causation when an unrelated-seeming issue (lost WiFi password) occurs
  during a batch — and to report the investigation either way.
- Noted Rule 8's speed tweaks are a different subsystem than `pm` and
  worth applying regardless of the Rule 3a persistence finding, since
  their failure mode is low-stakes.
