#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

source "$ROOT_DIR/scripts/load_env.sh"
load_project_env "$ROOT_DIR"
setup_project_java

cd "$ROOT_DIR"
exec ./gradlew "$@"
