#!/usr/bin/env bash
set -euo pipefail

TAG="${1:?usage: publish_gitee_release.sh <tag> <asset-path> [release-name]}"
ASSET_PATH="${2:?usage: publish_gitee_release.sh <tag> <asset-path> [release-name]}"
RELEASE_NAME="${3:-$TAG}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

source "$ROOT_DIR/scripts/load_env.sh"
load_project_env "$ROOT_DIR"

command -v jq >/dev/null 2>&1 || {
  echo "jq is required for parsing Gitee API responses." >&2
  exit 1
}

for name in GITEE_ACCESS_TOKEN GITEE_OWNER GITEE_REPO; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing $name in .env or environment." >&2
    exit 1
  fi
done

if [[ ! -f "$ASSET_PATH" ]]; then
  echo "Asset not found: $ASSET_PATH" >&2
  exit 1
fi

API_BASE="${GITEE_API_BASE:-https://gitee.com/api/v5}"
RELEASE_BODY="${GITEE_RELEASE_BODY:-Android APK release for $TAG.}"

release_json="$(
  curl -fsS --get \
    --data-urlencode "access_token=$GITEE_ACCESS_TOKEN" \
    "$API_BASE/repos/$GITEE_OWNER/$GITEE_REPO/releases/tags/$TAG" 2>/dev/null || true
)"
release_id="$(printf '%s' "$release_json" | jq -r '.id // empty' 2>/dev/null || true)"

if [[ -z "$release_id" ]]; then
  release_payload="$(
    jq -n \
      --arg tagName "$TAG" \
      --arg name "$RELEASE_NAME" \
      --arg body "$RELEASE_BODY" \
      --arg targetCommitish "main" \
      '{
        tagName: $tagName,
        name: $name,
        body: $body,
        targetCommitish: $targetCommitish,
        prerelease: false
      }'
  )"
  release_json="$(
    curl -fsS -X POST "$API_BASE/repos/$GITEE_OWNER/$GITEE_REPO/releases?access_token=$GITEE_ACCESS_TOKEN" \
      -H "Content-Type: application/json" \
      --data "$release_payload"
  )"
  release_id="$(printf '%s' "$release_json" | jq -r '.id // empty')"
fi

if [[ -z "$release_id" ]]; then
  echo "Unable to create or find Gitee release for $TAG." >&2
  printf '%s\n' "$release_json" >&2
  exit 1
fi

asset_name="$(basename "$ASSET_PATH")"
attachments_json="$(
  curl -fsS --get \
    --data-urlencode "access_token=$GITEE_ACCESS_TOKEN" \
    "$API_BASE/repos/$GITEE_OWNER/$GITEE_REPO/releases/$release_id/attach_files" 2>/dev/null || true
)"

if [[ -n "$attachments_json" ]]; then
  printf '%s' "$attachments_json" |
    jq -r --arg name "$asset_name" '.[]? | select(.name == $name) | .id' |
    while IFS= read -r attach_id; do
      [[ -n "$attach_id" ]] || continue
      curl -fsS -X DELETE "$API_BASE/repos/$GITEE_OWNER/$GITEE_REPO/releases/$release_id/attach_files/$attach_id?access_token=$GITEE_ACCESS_TOKEN" >/dev/null
    done
fi

upload_json="$(
  curl -fsS -X POST "$API_BASE/repos/$GITEE_OWNER/$GITEE_REPO/releases/$release_id/attach_files?access_token=$GITEE_ACCESS_TOKEN" \
    -F "file=@$ASSET_PATH"
)"

printf '%s\n' "$upload_json" | jq -r '.browser_download_url // .download_url // .name // empty'
