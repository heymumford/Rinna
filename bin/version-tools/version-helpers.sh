#!/usr/bin/env bash
#
# version-helpers.sh - Common helper functions for version management
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

# Get the current version from version.properties
get_version() {
  local version_file="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/version.properties}"
  grep -m 1 "^version=" "$version_file" | cut -d'=' -f2
}

# Get a property from version.properties
get_property() {
  local prop="$1"
  local version_file="${2:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/version.properties}"
  grep -m 1 "^$prop=" "$version_file" | cut -d'=' -f2
}

# Increment a version number (major, minor, or patch)
increment_version() {
  local version="$1" index="$2"
  IFS='.' read -r -a parts <<< "$version"
  
  # Increment the specified part
  parts[$index]=$((parts[$index] + 1))
  
  # Reset lower parts to 0
  for ((i=index+1; i<${#parts[@]}; i++)); do
    parts[$i]=0
  done
  
  echo "${parts[0]}.${parts[1]}.${parts[2]}"
}

# Parse version into components (major, minor, patch)
parse_version() {
  local version="$1"
  if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: Invalid version format: $version (should be x.y.z)" >&2
    return 1
  fi
  
  VERSION_MAJOR=$(echo "$version" | cut -d. -f1)
  VERSION_MINOR=$(echo "$version" | cut -d. -f2)
  VERSION_PATCH=$(echo "$version" | cut -d. -f3)
  export VERSION_MAJOR VERSION_MINOR VERSION_PATCH
  return 0
}

# Update version badges in README.md
update_badges() {
  local version="$1" build="$2"
  local readme="${3:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/README.md}"
  
  # Update version badge
  sed -i "s/\(version-\)[0-9]\+\.[0-9]\+\.[0-9]\+/\1$version/g" "$readme"
  
  # Update build badge
  sed -i "s/\(build-\)[0-9]\+/\1$build/g" "$readme"
}

# Check if a version should have a GitHub release
should_release() {
  local version="$1" release_type="$2"
  IFS='.' read -r major minor patch <<< "$version"
  
  # Always create releases for major and minor versions
  if [[ "$patch" == "0" ]]; then
    return 0 # true
  fi
  
  # Create releases for explicitly marked RELEASE versions
  if [[ "$release_type" == "RELEASE" ]]; then
    return 0 # true
  fi
  
  # Default to not creating a release for regular patches
  return 1 # false
}

# Normalize a file path for display
normalize_path() {
  local path="$1" base_dir="$2"
  echo "${path#$base_dir/}"
}

# Log a message with level
log_message() {
  local level="$1" message="$2" verbose="${3:-false}"
  
  if [[ "$verbose" == "true" ]] || [[ "$level" == "ERROR" ]]; then
    case "$level" in
      "INFO") echo "$message" ;;
      "SUCCESS") echo "$message" ;;
      "WARNING") echo "Warning: $message" ;;
      "ERROR") echo "Error: $message" >&2 ;;
    esac
  fi
}