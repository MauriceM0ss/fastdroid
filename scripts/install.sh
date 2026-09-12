#!/usr/bin/env bash
# Build the debug APK and install it on the connected device over ADB.
set -euo pipefail

repo="$(cd "$(dirname "$0")/.." && pwd)"

"$repo/scripts/gradle.sh" assembleDebug
adb install -r "$repo/app/build/outputs/apk/debug/app-debug.apk"
adb shell am start -n io.github.mauricem0ss.fastdroid/.MainActivity
