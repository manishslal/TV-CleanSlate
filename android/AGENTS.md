# Android Home agent guide

Android Home is a fork of Aerial. Package namespace `app.orchard.tv`, application ID `io.manish.home`.

Read first:
1. `FORK.md`: what changed from upstream, and the fork's ground rules
2. `DESIGN.md`: the current design standard
3. `docs/ARCHITECTURE.md` and `docs/HANDOFF.md`: upstream's structure and rules (Chinese)
4. `docs/devices/*.md`: limits and fixes for each TV
5. `docs/upstream/AGENTS.zh.md`: upstream's original agent guide

## Commands

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleStandardRelease testStandardReleaseUnitTest
BUILD=1 TV=<tv-ip>:5555 tools/install.sh
adb -s <tv-ip>:5555 shell cmd package compile -m speed -f io.manish.home
```

Always pass `PKG` and `APK_FILE` to `tools/install.sh`. Without them it fetches upstream Aerial's release.

## Conventions

- English comments and identifiers.
- Interface strings go in all five languages: `res/values`, `values-zh`, `values-ru`, `values-uk`, `values-kk`.
- Design on the 960×540dp canvas, not real dpi.
- Use `Motion` from `ui/theme/Theme.kt`, and springs for animation that can be interrupted.
- Remote-pressable controls use `ui/components/Pressable`.
- Vendor behaviour goes behind feature detection, with the verified model in a comment.
- Test release builds on TVs, not debug builds.
- Bump `versionCode` for each build sent to a TV.
- Keep changes in the UI layer where possible, so upstream fixes merge cleanly.

## Don't

- Don't load upstream's server. `UPSTREAM_SERVER` stays false.
- Don't present this app as Aerial.
- Don't add code that downloads, installs or recommends other apps, or third-party SDKs.
- Don't overwrite other entries in `enabled_accessibility_services`.
- Don't commit `local/`.
- Don't commit logo artwork.
- `docs/README.md` says which docs are in Chinese and which came from upstream.
