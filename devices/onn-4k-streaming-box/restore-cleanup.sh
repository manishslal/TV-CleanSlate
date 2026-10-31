#!/bin/sh
D=${1:?usage: restore-cleanup.sh <tv-ip:5555>}
adb -s $D shell pm enable com.google.android.play.games
adb -s $D shell pm enable com.google.android.feedback
adb -s $D shell pm enable com.android.dreams.basic
adb -s $D shell pm enable com.google.android.apps.tv.dreamx
