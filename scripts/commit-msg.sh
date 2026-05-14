#!/bin/bash
set -euo pipefail

MSG_FILE="${1:-}"
MSG="$(cat "$MSG_FILE")"

REGEX='^(feat|fix|chore|docs|refactor|test|build|ci)(\([a-z0-9-]+\))?: .+'

if echo "$MSG" | grep -E '^(Merge |Revert )' >/dev/null 2>&1; then
  exit 0
fi

if ! echo "$MSG" | grep -E "$REGEX" >/dev/null 2>&1; then
  echo "Invalid commit message."
  echo "Expected: type(scope): subject"
  echo "Example : feat(auth): add refresh token rotation"
  exit 1
fi
