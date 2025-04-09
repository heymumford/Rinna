#!/bin/bash
#
# update-versions.sh - Backward compatibility wrapper for version-manager.sh
# Updates all version references based on version.properties across Java, Go, and Python
# Called by rin-version but can also be used directly
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Get the project root directory
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
  echo -e "${RED}Error: version-manager.sh not found or not executable.${NC}"
  echo "Please ensure it exists and has executable permissions."
  exit 1
fi

# Parse command line arguments
NEW_VERSION=""
NO_COMMIT_FLAG=""

# Check arguments
if [ $# -ge 1 ]; then
  NEW_VERSION="$1"
  
  # Check for --no-commit flag
  if [ "$2" = "--no-commit" ]; then
    NO_COMMIT_FLAG="--no-commit"
  fi
fi

echo -e "${BLUE}=== Rinna Version Update Tool ===${NC}"

# If a version is specified, set it using the version manager
if [ -n "${NEW_VERSION}" ]; then
  echo "Setting version to: ${GREEN}${NEW_VERSION}${NC}"
  "${VERSION_MANAGER}" set "${NEW_VERSION}" ${NO_COMMIT_FLAG}
  exit $?
fi

# If no version is specified, get the current version from version.properties
VERSION=$("${VERSION_MANAGER}" current | awk '{print $3}')
BUILD_NUMBER=$("${VERSION_MANAGER}" current | awk '{print $5}' | tr -d ')')

echo "Using current version: ${GREEN}${VERSION}${NC} (build ${BUILD_NUMBER})"

# Use version-manager.sh for all version updates
if [ -n "${NEW_VERSION}" ]; then
    # Already updated above
    exit 0
else
    # Update all versions using the version manager
    echo -e "${BLUE}Using version manager to update all version references...${NC}"
    "${VERSION_MANAGER}" verify
    
    # Display completion message
    echo ""
    echo -e "${GREEN}✓ Version update complete!${NC}"
    echo "All files have been updated to version ${VERSION} (build ${BUILD_NUMBER})"
fi