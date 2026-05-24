#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${1:-metronome}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

source "$ROOT_DIR/scripts/load_env.sh"
load_project_env "$ROOT_DIR"
setup_project_java

version_var="$(printf '%s_VERSION_NAME' "$APP_NAME" | tr '[:lower:]-' '[:upper:]_')"
version_name="${!version_var:-}"
if [[ -z "$version_name" ]]; then
  echo "Missing ${version_var} in .env." >&2
  exit 1
fi

tag="${APP_NAME}-v${version_name}"
apk_path="$("$ROOT_DIR/scripts/build_release.sh" "$APP_NAME")"
notes_file="$ROOT_DIR/dist/releases/${tag}-notes.md"

cat > "$notes_file" <<NOTES
${APP_NAME} ${version_name}

- Android APK for direct installation.
- Source code is available in this repository.
NOTES

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree has uncommitted changes. Commit before creating a release." >&2
  exit 1
fi

git rev-parse "$tag" >/dev/null 2>&1 || git tag -a "$tag" -m "${APP_NAME} ${version_name}"
git push origin main
git push origin "$tag"

if gh auth status >/dev/null 2>&1; then
  gh release create "$tag" "$apk_path#${APP_NAME}-${version_name}.apk" "$apk_path.sha256#${APP_NAME}-${version_name}.apk.sha256" \
    --title "${APP_NAME} ${version_name}" \
    --notes-file "$notes_file" \
    --verify-tag
else
  echo "GitHub CLI is not logged in. Run gh auth login, then:" >&2
  echo "gh release create '$tag' '$apk_path#${APP_NAME}-${version_name}.apk' '$apk_path.sha256#${APP_NAME}-${version_name}.apk.sha256' --title '${APP_NAME} ${version_name}' --notes-file '$notes_file' --verify-tag" >&2
fi
