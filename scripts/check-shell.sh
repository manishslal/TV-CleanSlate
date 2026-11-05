#!/usr/bin/env bash
# Run shellcheck on every shell script in the repo (CI runs this too). Needs shellcheck on PATH.
cd "$(dirname "$0")/.." || exit 1
files=(
  scripts/*.sh scripts/hooks/*
  webos/12-home-svc webos/16-lg-us-ads webos/17-lg-slim webos/20-home-on-power
  tizen/*.sh android/tools/*.sh devices/*/*.sh
)
exec shellcheck -f gcc "${files[@]}"
