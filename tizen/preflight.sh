#!/bin/bash
# Reads a Samsung TV's public info. Changes nothing, and leaves out its IP, MAC, name and IDs.
set -u
TV="${TV:-${1:-}}"
[ -n "$TV" ] || { echo "Usage: TV=<tv-ip> tizen/preflight.sh" >&2; exit 2; }
command -v jq >/dev/null || { echo "jq is required." >&2; exit 2; }

if ! info=$(curl -s --max-time 5 "http://$TV:8001/api/v2/"); then
  echo "No answer on port 8001. Check the address, and that the TV is on and on this network." >&2
  exit 1
fi
if ! jq -e '.device' >/dev/null 2>&1 <<<"$info"; then
  echo "Port 8001 answered, but this doesn't look like a Samsung TV." >&2
  exit 1
fi

jq -r '.device | "Model:          \(.modelName // "unknown")
Platform code:  \(.model // "unknown")
OS:             \(.OS // "unknown")
Firmware:       \(.firmwareVersion // "unknown")
Resolution:     \(.resolution // "unknown")
Token auth:     \(.TokenAuthSupport // "unknown")
Developer Mode: \(if .developerMode == "1" then "on" else "off" end)"' <<<"$info"

if nc -z -G 3 "$TV" 26101 2>/dev/null; then
  echo "sdb port:       open"
else
  echo "sdb port:       closed"
fi
