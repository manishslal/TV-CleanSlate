# Contributing

Device reports and fixes are welcome. Please follow the [code of conduct](CODE_OF_CONDUCT.md).

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

## Checks

Pull requests run [`checks.yml`](.github/workflows/checks.yml). Each one runs on your computer too, from the repository root:

| Check | Run it | Needs |
|---|---|---|
| Relative links in Markdown | `lychee --offline --config lychee.toml './**/*.md'` | [lychee](https://github.com/lycheeverse/lychee) |
| Shell scripts | `scripts/check-shell.sh` | [ShellCheck](https://www.shellcheck.net) |
| Python | `ruff check` and `python3 -m py_compile <file>` | [ruff](https://docs.astral.sh/ruff/) |
| Web Home (webOS, Tizen) | `python3 webos/build.py && python3 tizen/build.py && scripts/check-web.py` | Python with Pillow, Node |
| Paths named in docs | `scripts/check-doc-refs.py` | Python |

The web builds work without the private logos (`logos/dark/`) and the baked frost: they warn and go on. `scripts/check-web.py` fails if the page script does not parse or uses ES6 (`=>`, `let`, `const`, template literals, `class`), because the TVs run Chromium 47 and up. `scripts/check-doc-refs.py` fails when a Markdown file names a repo file in backticks that does not exist; a name that is not meant to be one (a file on a TV, in another project) goes in `scripts/doc-refs.ignore`. The weekly external-link pass reports dead web links but never fails a pull request.

## Adding a device

Add `devices/<device>/CHANGELOG.md`, then update `devices/devices.json` and `docs/SUPPORTED_DEVICES.md`.
