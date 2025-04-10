#!/bin/bash
#
# increment-build.sh - Increment the build number in version.properties
# Wrapper script for version-manager.sh
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Path to the project root and version manager
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
VERSION_MANAGER="${SCRIPT_DIR}/version-manager.sh"

# Ensure version-manager.sh exists and is executable
if [ ! -x "${VERSION_MANAGER}" ]; then
  echo "Error: version-manager.sh not found or not executable."
  echo "Please ensure it exists and has executable permissions."
  exit 1
fi

# Parse command line arguments
ACTION="increment"
BUILD_NUMBER=""
NO_COMMIT_FLAG=""

while [[ $# -gt 0 ]]; do
  case $1 in
    set)
      ACTION="set"
      if [[ $# -gt 1 && "$2" =~ ^[0-9]+$ ]]; then
        BUILD_NUMBER="$2"
        shift
      else
        echo "Error: Build number must be provided after 'set' command"
        exit 1
      fi
      shift
      ;;
    --no-commit)
      NO_COMMIT_FLAG="--no-commit"
      shift
      ;;
    -h|--help)
      echo "Usage: increment-build.sh [set NUMBER] [--no-commit]"
      echo ""
      echo "Commands:"
      echo "  (default)      Increment the build number by 1"
      echo "  set NUMBER     Set the build number to a specific value"
      echo ""
      echo "Options:"
      echo "  --no-commit    Don't create a Git commit with the changes"
      echo "  -h, --help     Show this help message and exit"
      exit 0
      ;;
    *)
      echo "Error: Unknown option: $1"
      exit 1
      ;;
  esac
done

# Get current version and build number for display
CURRENT_VERSION=$("${VERSION_MANAGER}" current | awk '{print $3}')
CURRENT_BUILD=$("${VERSION_MANAGER}" current | awk '{print $5}' | tr -d ')')

# Perform the requested action using version-manager.sh
if [ "$ACTION" = "set" ]; then
  echo "Setting build number to ${BUILD_NUMBER} (current: ${CURRENT_BUILD})..."
  "${VERSION_MANAGER}" set-build "${BUILD_NUMBER}" ${NO_COMMIT_FLAG}
  RESULT=$?
else
  echo "Incrementing build number from ${CURRENT_BUILD}..."
  "${VERSION_MANAGER}" increment-build ${NO_COMMIT_FLAG}
  RESULT=$?
fi

# Display outcome
if [ $RESULT -eq 0 ]; then
  # Get the new build number
  NEW_BUILD=$("${VERSION_MANAGER}" current | awk '{print $5}' | tr -d ')')
  echo "Build number updated successfully to ${NEW_BUILD}"
  
  if [ -z "${NO_COMMIT_FLAG}" ]; then
    echo "Changes committed. Push the changes with 'git push' to apply them to the repository."
  fi
else
  echo "Failed to update build number"
fi

exit $RESULT