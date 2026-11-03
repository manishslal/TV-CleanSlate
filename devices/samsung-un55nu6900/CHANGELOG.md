# Changelog: Samsung UN55NU6900

**Device:** Samsung 6 Series UN55NU6900 (2018, 4K), `18_KANTSU_UHD_BASIC`, armv7, 1 GB RAM
**OS:** Tizen 4.0
**Access:** Developer Mode and `sdb`, with a free author certificate
**Rooted:** No

## 2026-11-01

### Install: Home (`CSHome00Te.Home`)

- Built with `LEAN=1`: Efficient, still wallpaper, no frost. Undo: `sdb shell 0 vd_appuninstall CSHome00Te.Home`.

### Removed

| Package | How | Undo |
|---|---|---|
| e-Manual (`20182100009`) | `vd_appuninstall` with the numeric ID | Samsung's app store, or a Smart Hub reset |
| Store demo (`JGHCDI7ctc`) | `vd_appuninstall` with the package ID | A Smart Hub reset may restore it |
| Ad player (`adplatform`) | `vd_appuninstall` with the package ID | A Smart Hub reset may restore it |

`org.tizen.ADPlayer` is a system package and returned error 132. Streaming apps stay, by the owner's choice.

### Power-on

- The owner turned Autorun Smart Hub off and Autorun Last App on.

### Inputs

- `org.tizen.SMSourceAPP` launches but shows nothing here. The local Source key route kept prompting for approval and returned unauthorized.
- The Inputs tile is hidden. Undo: Personalize > Hidden Apps. The remote's Source button works.

### Measured

| | Start | Now |
|---|---|---|
| Free storage | 93 of 816 MiB | 157 MiB |
| Free RAM with Home open | 213 MB with video | 293 MB with stills |
| Tiles | 47 | 15 |

Wi-Fi signal read 36%.

### Home button

- A temporary script made Home reopen itself after the real Home button. Home had focus again in about 1 second, and the owner saw Smart Hub close. That script ended when Home restarted.
- A later build added this to Home, with **Home Button Returns Here** on by default. It's installed here but hasn't been tested with that build. This build also reopens over Menu and Source, which the fix on the UN50MU6070 stops; reinstall to get it. The volume bar, Source and Menu are unchecked, and Home's debug port may still be open until Home restarts. Turn the setting off, or restart Home, if anything closes by itself.
- Undo: set the setting to Off, or reinstall an older build.
