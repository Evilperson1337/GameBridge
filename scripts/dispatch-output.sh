#!/usr/bin/env bash
# Hands one Bridge Shortcut from the Output Folder to the debug build's BridgeActivity, the way LaunchBox would,
# and prints the Dispatch from logcat. Debug build only: it reads the Output Folder from the debug build's
# preferences with run-as and relies on the debug build's own persisted grant.
#
#   scripts/dispatch-output.sh             list the files in the Output Folder
#   scripts/dispatch-output.sh "<name>"    Dispatch that file (exact name, as listed)
#
# Pick the device with ANDROID_SERIAL (adb's own variable), not -s. Prints a Dispatch, never a Launch: a person
# confirms the game or stream started.
set -euo pipefail

PKG=com.allourmedia.gamebridge.debug
ACTION=com.allourmedia.gamebridge.action.DISPATCH

prefs="$(adb shell run-as "$PKG" cat shared_prefs/scan_folders.xml 2>/dev/null)" || {
    echo "Can't read the debug build's Scan folders. Is $PKG installed, and has Scan been run with an Output Folder?" >&2
    exit 1
}
tree="$(sed -n 's/.*name="Output">\([^<]*\)<.*/\1/p' <<<"$prefs")"
[[ -n "$tree" ]] || { echo "No Output Folder chosen in the debug build. Choose one on the Scan screen." >&2; exit 1; }

# The tree's document ID ("primary:Games/pc" or "XXXX-XXXX:GameNative") names the folder on shared storage.
doc_id="$(python3 -I -c 'import sys, urllib.parse; print(urllib.parse.unquote(sys.argv[1].rsplit("/tree/", 1)[1]))' "$tree")"
volume="${doc_id%%:*}"
relative="${doc_id#*:}"
if [[ "$volume" == primary ]]; then dir="/storage/emulated/0/$relative"; else dir="/storage/$volume/$relative"; fi

names="$(adb shell "ls -1 '$dir'" | tr -d '\r' | grep -E '\.gamebridge$' || true)"
if [[ $# -eq 0 ]]; then
    printf '%s\n' "${names:-(no .gamebridge files)}"
    exit 0
fi

name="$1"
grep -qxF -- "$name" <<<"$names" || {
    echo "No file named exactly \"$name\" in the Output Folder. These exist:" >&2
    printf '%s\n' "${names:-(none)}" >&2
    exit 1
}

document="$(python3 -I -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1] + "/" + sys.argv[2], safe=""))' "$doc_id" "$name")"
uri="$tree/document/$document"

adb logcat -c
adb shell "am start -W -a $ACTION -n $PKG/com.allourmedia.gamebridge.BridgeActivity -d '$uri'" | grep -E '^(Status|Error)'
sleep 3
adb logcat -d -s GameBridge ActivityTaskManager | grep -E 'GameBridge:|START u0' | grep -v 'action.DISPATCH' || true
