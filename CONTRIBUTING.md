# Contributing

Device reports and fixes are welcome.

## Device reports

Include the model, OS version, RAM, what worked, what failed and how to undo it. Remove IP addresses, MAC addresses, Wi-Fi names and serial numbers.

## Code

1. Read [AGENTS.md](AGENTS.md).
2. Keep each change small.
3. Run `./gradlew assembleStandardRelease testStandardReleaseUnitTest` in `android/`.
4. Test on a real TV and say which one.
5. Add strings in all five languages.
6. Add any new third-party material to [CREDITS.md](CREDITS.md).

A bug that also appears in unmodified Aerial belongs [upstream](https://github.com/pengchujin/aerial-tv).

## Adding a device

Add `devices/<device>/CHANGELOG.md`, then update `devices/devices.json` and `docs/SUPPORTED_DEVICES.md`.
