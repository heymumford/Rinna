#!/bin/bash
#
# increment-build.sh - Increment the build number in version.properties
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Path to the project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION_FILE="$PROJECT_ROOT/version.properties"
VERSION_SERVICE_FILE="$PROJECT_ROOT/version-service/version.properties"

# Function to increment build number in a version.properties file
increment_build_number() {
  local file="$1"
  if [ ! -f "$file" ]; then
    echo "Error: Version file not found: $file"
    return 1
  fi
  
  # Get current build number
  current_build=$(grep -m 1 "^buildNumber=" "$file" | cut -d'=' -f2)
  if [ -z "$current_build" ]; then
    echo "Error: Could not find buildNumber in $file"
    return 1
  fi
  
  # Increment build number
  new_build=$((current_build + 1))
  
  # Update the file
  sed -i "s/^buildNumber=.*/buildNumber=$new_build/" "$file"
  
  # Update the timestamp
  current_timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  sed -i "s/^build.timestamp=.*/build.timestamp=$current_timestamp/" "$file"
  
  echo "Updated build number in $file from $current_build to $new_build"
  return 0
}

# Function to set a specific build number
set_build_number() {
  local file="$1"
  local new_build="$2"
  
  if [ ! -f "$file" ]; then
    echo "Error: Version file not found: $file"
    return 1
  fi
  
  # Get current build number
  current_build=$(grep -m 1 "^buildNumber=" "$file" | cut -d'=' -f2)
  if [ -z "$current_build" ]; then
    echo "Error: Could not find buildNumber in $file"
    return 1
  fi
  
  # Update the file
  sed -i "s/^buildNumber=.*/buildNumber=$new_build/" "$file"
  
  # Update the timestamp
  current_timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  sed -i "s/^build.timestamp=.*/build.timestamp=$current_timestamp/" "$file"
  
  echo "Updated build number in $file from $current_build to $new_build"
  return 0
}

# Parse command line arguments
ACTION="increment"
BUILD_NUMBER=""

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
      NO_COMMIT="true"
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

# Perform the requested action
if [ "$ACTION" = "set" ]; then
  echo "Setting build number to $BUILD_NUMBER..."
  set_build_number "$VERSION_FILE" "$BUILD_NUMBER"
  MAIN_RESULT=$?
  
  if [ -f "$VERSION_SERVICE_FILE" ]; then
    set_build_number "$VERSION_SERVICE_FILE" "$BUILD_NUMBER"
    SERVICE_RESULT=$?
  else
    SERVICE_RESULT=0
  fi
else
  echo "Incrementing build number..."
  increment_build_number "$VERSION_FILE"
  MAIN_RESULT=$?
  
  if [ -f "$VERSION_SERVICE_FILE" ]; then
    increment_build_number "$VERSION_SERVICE_FILE"
    SERVICE_RESULT=$?
  else
    SERVICE_RESULT=0
  fi
fi

# Commit the changes if requested
if [ "$NO_COMMIT" != "true" ] && ([ $MAIN_RESULT -eq 0 ] || [ $SERVICE_RESULT -eq 0 ]); then
  # Get the new build number for the commit message
  new_build=$(grep -m 1 "^buildNumber=" "$VERSION_FILE" | cut -d'=' -f2)
  
  # Add the files to git
  git add "$VERSION_FILE" "$VERSION_SERVICE_FILE" 2>/dev/null
  
  # Create a commit
  git commit -m "Update build number to $new_build [skip ci]" --no-verify
  
  echo "Changes committed. Push the changes with 'git push' to apply them to the repository."
fi

exit 0