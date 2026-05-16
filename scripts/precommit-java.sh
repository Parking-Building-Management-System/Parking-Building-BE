#!/bin/bash
set -euo pipefail

GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[0;33m"
NC="\033[0m"

echo -e "${YELLOW}Running Java pre-commit checks...${NC}"

CHANGED=$(git diff --cached --name-only)
if ! echo "$CHANGED" | grep -E '\.java$|pom\.xml$' >/dev/null 2>&1; then
  echo "No Java-related changes staged. Skipping."
  exit 0
fi

echo "Running Spotless Check..."
if ! ./mvnw -q spotless:check; then
    echo -e "${RED}Spotless check failed!${NC}"
    echo -e "Please run ${GREEN}make fmt${NC} to format your code before committing."
    exit 1
fi

echo -e "${GREEN} All Java checks passed!${NC}"
exit 0
