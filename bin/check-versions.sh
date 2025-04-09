#!/bin/bash
#
# check-versions.sh - Version consistency checker for Rinna project
# Wrapper script for version-manager.sh
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Get the project root directory and version manager
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
VERSION_MANAGER="${SCRIPT_DIR}/version-manager.sh"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Ensure version-manager.sh exists and is executable
if [ ! -x "${VERSION_MANAGER}" ]; then
  echo -e "${RED}ERROR: version-manager.sh not found or not executable.${NC}"
  echo "Please ensure it exists and has executable permissions."
  exit 1
fi

# Get the current version and build number
CURRENT_VERSION=$("${VERSION_MANAGER}" current | awk '{print $3}')
BUILD_NUMBER=$("${VERSION_MANAGER}" current | awk '{print $5}' | tr -d ')')

if [[ -z "$CURRENT_VERSION" ]]; then
    echo -e "${RED}ERROR: Could not extract version from version-manager!${NC}"
    exit 1
fi

echo -e "${BLUE}=== Rinna Version Check Tool ===${NC}"
echo "Checking version consistency across Java, Go, and Python..."
echo "Current version from version.properties: ${GREEN}${CURRENT_VERSION}${NC} (build ${BUILD_NUMBER})"
echo ""

# Run version verification using the version manager
echo -e "${BLUE}Running version consistency check...${NC}"
"${VERSION_MANAGER}" verify

# Capture exit code
RESULT=$?

# If everything is consistent, show a nice message
if [ $RESULT -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ All version references are consistent: ${CURRENT_VERSION} (build ${BUILD_NUMBER})${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}✗ Found version inconsistencies. Run the following to fix them:${NC}"
    echo "  ${VERSION_MANAGER} set ${CURRENT_VERSION}"
    exit 1
fi