#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT_DIR/scripts/load_env.sh"
load_project_env "$ROOT_DIR"
setup_project_java

APK_DIR="$ROOT_DIR/apps/metronome/build/outputs/apk/debug"
APK=""
if [[ -d "$APK_DIR" ]]; then
  APK="$(find "$APK_DIR" -maxdepth 1 -type f -name '*debug.apk' | sort | tail -1)"
fi

if [[ -z "$APK" || ! -f "$APK" ]]; then
  "$ROOT_DIR/scripts/build_app.sh" metronome debug
  APK="$(find "$APK_DIR" -maxdepth 1 -type f -name '*debug.apk' | sort | tail -1)"
fi

if [[ -z "$APK" || ! -f "$APK" ]]; then
  echo "Debug APK was not found in $APK_DIR." >&2
  exit 1
fi

command -v adb >/dev/null 2>&1 || {
  echo "adb is not available. Set ANDROID_HOME or ANDROID_SDK_ROOT in .env." >&2
  exit 1
}

adb install -r "$APK"
