#!/bin/bash
set -euo pipefail

echo "==> Setting up pre-commit for this repository"

export PATH="$HOME/.local/bin:$PATH"

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to install pre-commit."
  exit 1
fi

if ! command -v pre-commit >/dev/null 2>&1; then
  if command -v pipx >/dev/null 2>&1; then
    echo "==> Installing pre-commit via pipx"
    pipx install pre-commit
  else
    echo "==> pipx not found. Installing pre-commit via pip --user"
    python3 -m pip install --user -U pre-commit
  fi
else
  echo "==> pre-commit is already installed. Skipping installation."
fi

echo "==> Installing git hooks"
pre-commit install
pre-commit install --hook-type commit-msg

echo "==> pre-commit is ready!"
