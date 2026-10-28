# About this fork

A personal fork of [pengchujin/aerial-tv](https://github.com/pengchujin/aerial-tv) (MIT) for customizing the home screen on my own Android TVs. Credits and third-party licenses: [NOTICE.md](NOTICE.md).

## Ground rules
- `LICENSE` stays exactly as upstream wrote it. New files carry no claim over upstream code.
- Customizations live in the UI layer (`ui/`, `wallpaper/`, theme config). `system/`, `guard/`, `dream/` and `update/` stay as close to upstream as possible, so upstream fixes merge cleanly.
- The fork gets its own app name, icon and application ID. It must not be presented as Aerial.
- Runtime calls to the upstream author's server (`aerial.qust.me`: update checks, APK downloads, the icon library and the phone setup page) are repointed or switched off in our builds. Our builds are signed with our own key, so upstream updates can't install over them, and we shouldn't load the author's server.

## Branches
- `main`: tracks upstream plus our merged changes.
- `fork/*`: our work branches.

## Syncing with upstream
```bash
git fetch upstream
git checkout main
git merge upstream/main
```

## Changes from upstream
| Date | Change |
|---|---|
| 2026-10-28 | Forked at `c3c0511` (v1.67). Added NOTICE.md and FORK.md, plus a fork notice at the top of the READMEs. No code changes yet. |

## Planned
- [ ] New application ID and app name (side-by-side install with upstream Aerial)
- [ ] Repoint or disable `Updates.kt`, `IconLibrary.kt` and the Control Center update/setup URLs
- [ ] Theme layer: colors, fonts, tile shape, focus effect, motion timings
