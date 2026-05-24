#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${1:-metronome}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

source "$ROOT_DIR/scripts/load_env.sh"
load_project_env "$ROOT_DIR"
setup_project_java

missing=0
for name in ANDROID_SIGNING_STORE_FILE ANDROID_SIGNING_STORE_PASSWORD ANDROID_SIGNING_KEY_ALIAS ANDROID_SIGNING_KEY_PASSWORD; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing $name in .env. Release APKs must be signed." >&2
    missing=1
  fi
done

if [[ "$missing" -ne 0 ]]; then
  exit 1
fi

if [[ ! -f "$ROOT_DIR/$ANDROID_SIGNING_STORE_FILE" && ! -f "$ANDROID_SIGNING_STORE_FILE" ]]; then
  echo "Signing store not found: $ANDROID_SIGNING_STORE_FILE" >&2
  exit 1
fi

cd "$ROOT_DIR"
./gradlew ":apps:${APP_NAME}:assembleRelease"

APK_PATH="$(find "$ROOT_DIR/apps/$APP_NAME/build/outputs/apk/release" -maxdepth 1 -type f -name '*release*.apk' ! -name '*unsigned*' | sort | tail -1)"
if [[ -z "$APK_PATH" ]]; then
  echo "No signed release APK found." >&2
  exit 1
fi

version_var="$(printf '%s_VERSION_NAME' "$APP_NAME" | tr '[:lower:]-' '[:upper:]_')"
version_name="${!version_var:-dev}"
release_dir="$ROOT_DIR/dist/releases"
mkdir -p "$release_dir"
output_apk="$release_dir/${APP_NAME}-v${version_name}.apk"

cp "$APK_PATH" "$output_apk"
shasum -a 256 "$output_apk" > "$output_apk.sha256"

echo "$output_apk"
