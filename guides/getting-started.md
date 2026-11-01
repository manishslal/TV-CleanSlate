# Getting started

This guide installs Android Home on an Android TV or Google TV device. For LG, see [webos/](../webos/README.md).

## What you need

- A TV or box on Android 5.0 or newer, on the same network as your computer
- [ADB](https://developer.android.com/tools/adb)
- JDK 17 and the Android SDK

## 1. Turn on network debugging

1. Open Settings, then About, and press Build number seven times.
2. Open Developer options and turn on Network debugging or Wireless debugging.
3. Note the TV's IP address.
4. Connect, then accept the prompt on the TV:

```bash
adb connect <tv-ip>:5555
```

Some brands differ. See [adb-by-brand.md](../android/docs/devices/adb-by-brand.md).

## 2. Back up the TV

```bash
scripts/01-preflight-backup.sh <tv-ip>
```

Then reboot once and confirm the TV comes back. Read [safety.md](safety.md).

## 3. Build and install

```bash
cd android
BUILD=1 TV=<tv-ip>:5555 tools/install.sh
adb -s <tv-ip>:5555 shell cmd package compile -m speed -f io.manish.home
```

Firmware that refuses a launcher needs `tools/install.sh guard` instead.

The installer grants the permissions Home needs and tries to make it the default. Where the firmware keeps sending Home to its own launcher, **Home Guard** takes over. Nothing is disabled or uninstalled by this step.

## 4. Check it

- Press Home from another app and confirm you land on the new Home.
- Reboot and confirm it still starts.
- Open Settings, inputs and a streaming app.

```bash
TV=<tv-ip> tools/selftest.sh
```

## 5. Set it up

Move up to the Wi-Fi button at the top right and press OK to open Control Center, then choose Personalize. Pick a **Home Mode**: Full looks best, and Efficient suits 2 GB devices. Hold OK on a tile to move, hide or group it.

## 6. Optional: clean up the TV

## Home on Fire TV

Fire OS keeps the Home button for Amazon, and the HOME role has no effect. `tools/install.sh` detects Fire OS 6 and installs the guard build with `FireKeeper`. It needs `BUILD=1`.

- Fire OS 6 never binds a third-party accessibility service, so Home Guard can't run there. `FireKeeper` reads usage stats and moves Home's task to the front.
- Amazon's launcher stays enabled. Disabling it can leave a stick with no home screen.
- Hold Home for Amazon's quick menu. Home's Amazon tile and Settings pause `FireKeeper` until the next Home press or app.
- After a reboot, the notification listener starts Home before Amazon's launcher. On a Fire TV Stick 4K (1st gen), Amazon's home shows for about a second. Don't rely on `BOOT_COMPLETED`, which can take 2 minutes there.
- Home's screensaver replaces Amazon's. The setup sets it as the system screensaver, and `FireKeeper` restores it if Fire OS changes it.
- After a Fire OS update, run `TV=<tv-ip> tools/fire-reapply.sh`. It restores the debloat, privacy settings, permissions and screensaver. Home's menu shows a reminder until you run it. Add `check` to report without changing anything.
- Before installing, run `TV=<tv-ip> tools/logo-audit.sh`. It lists apps with no logo, usually because the platform uses a different package ID.

Fire OS 7 and 8 use Home Guard, but this project hasn't tested Home on them.

Debloating is separate from Home. Follow the [Android and Google TV guide](debloat-android-google-tv.md), or the [Fire TV guide](debloat-fire-tv.md), one small batch at a time.

## 7. Optional: disable the stock launcher on Google TV

This is where most of the memory saving comes from.

```bash
scripts/04-disable-stock-launcher.sh <tv-ip> io.manish.home/app.orchard.tv.MainActivity
```

The script stops if your Wi-Fi depends on the setup wizard, and undoes the change if Android won't accept your Home. Reboot afterwards, then check Home, Settings, Wi-Fi and the remote. On some devices the YouTube button on the remote may stop working.

## 8. Turn off network debugging

Do this in Developer options when you finish.

## Undo

```bash
TV=<tv-ip> tools/restore.sh --uninstall
```

If you disabled the stock launcher, enable it first with `adb shell pm enable <package>`.

Having trouble? See [troubleshooting.md](troubleshooting.md).
