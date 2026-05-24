#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${1:-metronome}"
VARIANT_INPUT="${2:-debug}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

source "$ROOT_DIR/scripts/load_env.sh"
load_project_env "$ROOT_DIR"
setup_project_java

VARIANT_LOWER="$(printf '%s' "$VARIANT_INPUT" | tr '[:upper:]' '[:lower:]')"
case "$VARIANT_LOWER" in
  debug)
    TASK_VARIANT="Debug"
    ;;
  release)
    TASK_VARIANT="Release"
    ;;
  *)
    echo "Unknown variant: $VARIANT_INPUT. Use debug or release." >&2
    exit 2
    ;;
esac

cd "$ROOT_DIR"
./gradlew ":apps:${APP_NAME}:assemble${TASK_VARIANT}"
