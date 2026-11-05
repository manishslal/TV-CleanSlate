#!/bin/bash
# Package tizen/build, install it on the TV in $TV, and open it.
set -eu
cd "$(dirname "$0")"
T="$HOME/tizen-studio/tools"
S="$TV:26101"
"$T/sdb" connect "$S" >/dev/null
"$T/ide/bin/tizen" package -t wgt -s home -o "$PWD" -- build >/dev/null
# shellcheck disable=SC2012  # newest by modification time
WGT=$(ls -t ./*.wgt | head -1)
mv "$WGT" home.wgt
"$T/sdb" -s "$S" push home.wgt /home/owner/share/tmp/sdk_tools/home.wgt >/dev/null
"$T/sdb" -s "$S" shell 0 vd_appinstall CSHome00Te.Home /home/owner/share/tmp/sdk_tools/home.wgt | tail -2
"$T/sdb" -s "$S" shell 0 was_execute CSHome00Te.Home | tail -2
