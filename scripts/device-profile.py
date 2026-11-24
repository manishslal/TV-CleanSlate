#!/usr/bin/env python3
"""Pick a device's profile from devices/profiles.json and print it as shell assignments.

    eval "$(device-profile.py <manufacturer> <sdk> <model> [<device> <product>...])"

The first profile whose match fits wins (manufacturer, sdk_min, sdk_max). Any of the identifiers
(model, device, product name) found under "models" gives the device's folder under devices/.
Prints PROFILE_ID, PROFILE_LABEL, FLAVOR, MECHANISM, DEBLOAT_MODE, DEBLOAT_WHY, SETTINGS_COMPONENT,
STATUS, MODEL_DIR and QUIRKS (one per line). Exit 2 if the file has no profile that fits.
"""
import json
import os
import shlex
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROFILES = os.path.join(HERE, "..", "devices", "profiles.json")


def fits(match, manufacturer, sdk):
    if "manufacturer" in match and match["manufacturer"].lower() != manufacturer.lower():
        return False
    if "sdk_min" in match and sdk < match["sdk_min"]:
        return False
    if "sdk_max" in match and sdk > match["sdk_max"]:
        return False
    return True


def main(argv):
    if len(argv) < 4:
        print(__doc__)
        return 2
    manufacturer, sdk_text, *ids = argv[1:]
    sdk = int(sdk_text) if sdk_text.isdigit() else 0
    with open(PROFILES) as f:
        data = json.load(f)
    profile = next((p for p in data["profiles"] if fits(p["match"], manufacturer, sdk)), None)
    if profile is None:
        return 2
    model_dir = next((data["models"][i]["dir"] for i in ids if i in data["models"]), "")
    debloat = profile.get("debloat", {})
    values = {
        "PROFILE_ID": profile["id"],
        "PROFILE_LABEL": profile["label"],
        "FLAVOR": profile["flavor"],
        "MECHANISM": profile.get("mechanism", ""),
        "DEBLOAT_MODE": debloat.get("mode", "none"),
        "DEBLOAT_WHY": debloat.get("why", ""),
        "SETTINGS_COMPONENT": profile.get("settings", {}).get("component", ""),
        "STATUS": profile.get("status", "untested"),
        "MODEL_DIR": model_dir,
        "QUIRKS": "\n".join(profile.get("quirks", [])),
    }
    for key, value in values.items():
        print(f"{key}={shlex.quote(str(value))}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
