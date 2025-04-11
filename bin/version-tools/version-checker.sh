#!/usr/bin/env bash
#
# version-checker.sh - Verify version consistency across files
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Get directory paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RINNA_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
VERSION_FILE="$RINNA_DIR/version.properties"
README_FILE="$RINNA_DIR/README.md"

# Source helper functions
source "$SCRIPT_DIR/version-helpers.sh"

# Main verification function
verify_version_consistency() {
  local expected_version=${1:-$(get_version)}
  local quiet=${2:-false}
  local exit_code=0
  
  if [[ "$quiet" == "false" ]]; then
    echo "Verifying version consistency across files:"
    echo "=========================================="
    echo "Expected version: $expected_version"
    echo
  fi
  
  # Check version in main POM
  if [[ "$quiet" == "false" ]]; then
    echo "Checking POM files:"
  fi
  
  if [ -f "$RINNA_DIR/pom.xml" ]; then
    pom_version=$(grep -m 1 "<version>" "$RINNA_DIR/pom.xml" | sed -E 's/.*<version>(.*)<\/version>.*/\1/')
    if [[ "$pom_version" == "$expected_version" ]]; then
      [[ "$quiet" == "false" ]] && echo "✅ Main pom.xml: $pom_version"
    else
      [[ "$quiet" == "false" ]] && echo "❌ Main pom.xml: $pom_version (should be $expected_version)"
      exit_code=1
    fi
  fi
  
  # Check module POM files
  for module_pom in "$RINNA_DIR/rinna-core/pom.xml" "$RINNA_DIR/rinna-cli/pom.xml" "$RINNA_DIR/rinna-data-sqlite/pom.xml"; do
    if [ -f "$module_pom" ]; then
      parent_version=$(grep -A 3 "<parent>" "$module_pom" | grep -m 1 "<version>" | sed -E 's/.*<version>(.*)<\/version>.*/\1/')
      if [[ "$parent_version" == "$expected_version" ]]; then
        [[ "$quiet" == "false" ]] && echo "✅ $(basename $(dirname "$module_pom"))/pom.xml: $parent_version"
      else
        [[ "$quiet" == "false" ]] && echo "❌ $(basename $(dirname "$module_pom"))/pom.xml: $parent_version (should be $expected_version)"
        exit_code=1
      fi
    fi
  done
  
  # Check README badge
  if [[ "$quiet" == "false" ]]; then
    echo -e "\nChecking README badge:"
  fi
  
  if [ -f "$README_FILE" ]; then
    readme_version=$(grep -m 1 "badge/version-" "$README_FILE" | grep -o -E "version-[0-9.]*-" | sed -E 's/version-(.*)-.*/\1/')
    if [[ "$readme_version" == "$expected_version" ]]; then
      [[ "$quiet" == "false" ]] && echo "✅ README badge: $readme_version"
    else
      [[ "$quiet" == "false" ]] && echo "❌ README badge: $readme_version (should be $expected_version)"
      exit_code=1
    fi
  fi
  
  # Check Go version files
  if [[ "$quiet" == "false" ]]; then
    echo -e "\nChecking Go version files:"
  fi
  
  for go_file in $(find "$RINNA_DIR/api" "$RINNA_DIR/version-service" -name "version.go" -type f 2>/dev/null); do
    if [ -f "$go_file" ]; then
      go_version=$(grep "Version.*=" "$go_file" | grep -o '"[0-9.]*"' | tr -d '"')
      rel_path=${go_file#$RINNA_DIR/}
      if [[ "$go_version" == "$expected_version" ]]; then
        [[ "$quiet" == "false" ]] && echo "✅ $rel_path: $go_version"
      else
        [[ "$quiet" == "false" ]] && echo "❌ $rel_path: $go_version (should be $expected_version)"
        exit_code=1
      fi
    fi
  done
  
  # Check Python version
  if [[ "$quiet" == "false" ]]; then
    echo -e "\nChecking Python files:"
  fi
  
  if [ -f "$RINNA_DIR/pyproject.toml" ]; then
    python_version=$(grep "python_version" "$RINNA_DIR/pyproject.toml" | head -1 | grep -o '"[0-9.]*"' | tr -d '"')
    if [[ "$python_version" == "$expected_version" ]]; then
      [[ "$quiet" == "false" ]] && echo "✅ pyproject.toml: $python_version"
    else
      [[ "$quiet" == "false" ]] && echo "❌ pyproject.toml: $python_version (should be $expected_version)"
      exit_code=1
    fi
  fi
  
  # Check build badge
  if [[ "$quiet" == "false" ]]; then
    echo -e "\nChecking build badge:"
  fi
  
  build_number=$(get_property "buildNumber")
  if [ -f "$README_FILE" ]; then
    readme_build=$(grep -m 1 "badge/build-" "$README_FILE" | grep -o -E "build-[0-9]+-" | sed -E 's/build-(.*)-.*/\1/')
    if [[ "$readme_build" == "$build_number" ]]; then
      [[ "$quiet" == "false" ]] && echo "✅ README build badge: $readme_build"
    else
      [[ "$quiet" == "false" ]] && echo "❌ README build badge: $readme_build (should be $build_number)"
      exit_code=1
    fi
  fi
  
  if [[ $exit_code -eq 0 ]]; then
    [[ "$quiet" == "false" ]] && echo -e "\n✅ All version references are consistent"
  else
    [[ "$quiet" == "false" ]] && echo -e "\n❌ Version inconsistencies detected"
  fi
  
  return $exit_code
}

# Display current version info
show_version_info() {
  local current_version=$(get_version)
  
  # Get other properties
  local last_updated=$(get_property "lastUpdated")
  local release_type=$(get_property "releaseType")
  local build_number=$(get_property "buildNumber")
  
  echo "Current version information:"
  echo "  Version: $current_version"
  echo "  Last Updated: $last_updated"
  echo "  Release Type: $release_type"
  echo "  Build Number: $build_number"
  
  # Check git tag
  if git tag -l "v${current_version}" | grep -q "v${current_version}"; then
    echo "  Git tag: v${current_version} exists"
  else
    echo "  Git tag: v${current_version} does not exist"
  fi
  
  # Check GitHub release if gh is available
  if command -v gh &> /dev/null; then
    if gh release view "v${current_version}" &> /dev/null; then
      echo "  GitHub release: v${current_version} exists"
    else
      echo "  GitHub release: v${current_version} does not exist"
    fi
  fi
  
  # Check release eligibility
  if should_release "$current_version" "$release_type"; then
    echo "  GitHub release: Eligible for GitHub release"
  else
    echo "  GitHub release: Not eligible for GitHub release (minor patch or SNAPSHOT)"
  fi
}

# Parse command line arguments
show_info=false
version=""
quiet=false

# Display help message
show_help() {
  cat << EOF
Usage: $(basename $0) [options]

Verifies version consistency across Rinna project files.

Options:
  --version VERSION   Verify against specific version (default: from version.properties)
  --info              Show current version information
  --quiet             Suppress detailed output, only return exit code
  --help              Show this help message

Examples:
  $(basename $0)                # Verify current version consistency
  $(basename $0) --version 1.2.3   # Verify against version 1.2.3
  $(basename $0) --info            # Show current version information
EOF
}

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --version)
      version="$2"
      shift 2
      ;;
    --info)
      show_info=true
      shift
      ;;
    --quiet)
      quiet=true
      shift
      ;;
    --help)
      show_help
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      show_help
      exit 1
      ;;
  esac
done

# Execute requested actions
if [[ "$show_info" == "true" ]]; then
  show_version_info
fi

if [[ -z "$version" ]]; then
  version=$(get_version)
fi

verify_version_consistency "$version" "$quiet"
exit $?