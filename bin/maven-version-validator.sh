#!/bin/bash
#
# maven-version-validator.sh - Maven-friendly version validator for CI integration
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BOLD='\033[1m'
RESET='\033[0m'

echo -e "${BOLD}Running Version Validation for Maven integration${RESET}"
echo "========================================================="

# Run the version validator in CI mode
"${SCRIPT_DIR}/version-tools/version-validator.sh" --ci
RESULT=$?

if [ $RESULT -eq 0 ]; then
  echo -e "\n${GREEN}${BOLD}✓ Version validation passed${RESET}"
  echo -e "All project components have consistent version information"
  exit 0
else
  echo -e "\n${RED}${BOLD}✗ Version validation failed${RESET}"
  echo -e "Some project components have inconsistent version information"
  echo -e "\n${YELLOW}To fix inconsistencies, run:${RESET}"
  echo -e "  ${BOLD}./bin/sync-versions.sh sync${RESET}"
  exit 1
fi