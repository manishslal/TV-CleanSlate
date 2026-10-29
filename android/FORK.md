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
| 2026-10-28 | Forked at `c3c0511` (v1.67). Added NOTICE.md and FORK.md, plus a fork notice at the top of the READMEs. |
| 2026-10-28 | v1.67-mh1: app ID `io.manish.home` (label "Home"), own signing key, no calls to aerial.qust.me (`BuildConfig.UPSTREAM_SERVER`); revival account type follows the ID. |
| 2026-10-28 | °F/°C (default °F), 12/24-hour clock (default "5:39 PM"); city is still chosen from the phone page. |
| 2026-10-28 | Dark tiles (default): white logos on dark glass; many-colored logos keep their colors without their plate; picture icons give up a light logo. Tile cache `ART` +103. |
| 2026-10-28 | Plex: PIN link (plex.tv/link), local server discovery, video library → My Videos (direct play), photo library → My Photos (1080p copies, plex-*.jpg). Screensaver buffer raised to 48 MB. |
| 2026-10-28 | Tech headlines over the screensaver (Google News Technology RSS): one at a time, bottom right, no glass. Place caption narrows to 460dp while it is on. |
| 2026-10-28 | Plex finds the server by its HTTPS plex.direct address (cleartext to the LAN is blocked). Phone setup page on port 8791 so it can run beside upstream Aerial (8790). Plex library rows scroll instead of pushing the label out. |

## Planned
- [ ] Final app name and icon; rename the strings that still say "Aerial" for the app (setup page, Home Guard)
- [ ] Theme layer: colors, fonts, tile shape, focus effect, motion timings
