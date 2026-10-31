# Troubleshooting

| Problem | What to try |
|---|---|
| `adb connect` fails | Check the IP and network debugging. If `nc` reaches the port but adb says "No route to host", restart the adb server. |
| ADB shows `unauthorized` | Accept the prompt on the TV and choose Always allow. |
| Android 11+ asks for a pairing code | Use Wireless debugging. `install.sh` walks you through pairing. |
| Play Protect blocks the install | Choose Install anyway on the TV, or build with a current target SDK. |
| Pressing Home opens the old launcher | Run `TV=<tv-ip> tools/install.sh repair` to restart Home Guard. |
| Home Guard stops after an update or reboot | Run `repair`. Then check that `enabled_accessibility_services` still lists it. |
| Home feels slow | Choose Efficient in Control Center. Moving wallpapers cost the most. Run the `compile -m speed` step again after updates. |
| Glass looks flat | Liquid Glass needs Android 13. Older TVs get frosted glass. |
| Tiles show plain icons | That's normal without a logo pack. See [logos/](../logos/README.md). |
| Weather shows the wrong city | The location comes from your network. Set it in Personalize, under Clock and Weather. |
| Plex can't find a server | Link with the PIN at plex.tv/link. Home connects over Plex's HTTPS address. |
| Back starts the screensaver | This is current behaviour on Home. |
| A setting says the device doesn't support it | Some features depend on the OS or firmware. Check [supported devices](../docs/SUPPORTED_DEVICES.md). |

## Collect details for a bug report

```bash
cd android
TV=<tv-ip> tools/report.sh
TV=<tv-ip> tools/selftest.sh
```

Read the output, remove anything private, then open a [device report](https://github.com/manishslal/TV-CleanSlate/issues/new?template=device-report.yml).
