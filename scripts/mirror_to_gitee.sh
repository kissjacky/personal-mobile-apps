#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

source "$ROOT_DIR/scripts/load_env.sh"
load_project_env "$ROOT_DIR"

GITEE_OWNER="${GITEE_OWNER:-jackyyu}"
GITEE_REPO="${GITEE_REPO:-personal-mobile-apps}"
GITEE_SSH_KEY_FILE="${GITEE_SSH_KEY_FILE:-$HOME/.ssh/id_gitee}"
REMOTE_URL="git@gitee.com:${GITEE_OWNER}/${GITEE_REPO}.git"

cd "$ROOT_DIR"

if ! git remote get-url gitee >/dev/null 2>&1; then
  git remote add gitee "$REMOTE_URL"
fi

GIT_SSH_COMMAND="ssh -i $GITEE_SSH_KEY_FILE -o IdentitiesOnly=yes" git push gitee main --tags
