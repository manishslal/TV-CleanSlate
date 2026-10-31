#!/usr/bin/env bash
# Re-enables everything the SHIELD cleanup disabled. Usage: restore-shield-cleanup.sh [ip:port]
TV=${1:?usage: restore-cleanup.sh <tv-ip:5555>}
adb connect "$TV"
for p in com.google.android.videos com.android.printspooler com.nvidia.developerwidget com.nvidia.shield.beta com.nvidia.shieldbeta \
         com.google.android.feedback com.nvidia.feedback com.nvidia.stats com.google.android.tvrecommendations com.google.android.tvlauncher; do
  adb -s "$TV" shell pm enable "$p"
done
