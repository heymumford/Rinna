#!/usr/bin/env bash
#
# version-updater.sh - Core script for updating version references
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

# Source helper functions
source "$SCRIPT_DIR/version-helpers.sh"

# Known file patterns - explicit list of files and their version patterns
declare -a VERSION_FILE_PATTERNS=(
  "version.properties:^version=.*:version={version}"
  "version.properties:^version.full=.*:version.full={version}"
  "version.properties:^version.major=.*:version.major={major}"
  "version.properties:^version.minor=.*:version.minor={minor}" 
  "version.properties:^version.patch=.*:version.patch={patch}"
  "pom.xml:<version>.*</version>:<version>{version}</version>"
  "rinna-core/pom.xml:<version>.*</version>:</parent>:<version>{version}</version></parent>"
  "rinna-cli/pom.xml:<version>.*</version>:</parent>:<version>{version}</version></parent>"
  "rinna-data-sqlite/pom.xml:<version>.*</version>:</parent>:<version>{version}</version></parent>"
  "api/internal/version/version.go:Version\\s*=\\s*\"[0-9.]+\":Version = \"{version}\""
  "api/pkg/health/version.go:Version\\s*=\\s*\"[0-9.]+\":Version = \"{version}\""
  "version-service/core/version.go:Version\\s*=\\s*\"[0-9.]+\":Version = \"{version}\""
  "pyproject.toml:python_version\\s*=\\s*\"[0-9.]+\":python_version = \"{version}\""
  "README.md:badge/version-[0-9.]+-blue:badge/version-{version}-blue"
  "README.md:build-[0-9]+:build-{build}"
)

# Parse command line arguments
FROM_VERSION=""
TO_VERSION=""
DRY_RUN=false
VERBOSE=false

# Display help message
show_help() {
  cat << EOF
Usage: $(basename $0) [options]

Precisely updates version references in Rinna project files.

Options:
  --from VERSION      Current version to update from
  --to VERSION        Target version to update to
  --dry-run           Preview changes without making modifications
  --verbose           Show detailed output
  --help              Show this help message

Examples:
  $(basename $0) --from 1.2.3 --to 1.3.0
  $(basename $0) --from 1.2.3 --to 1.3.0 --dry-run --verbose
EOF
}

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --from)
      FROM_VERSION="$2"
      shift 2
      ;;
    --to)
      TO_VERSION="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --verbose)
      VERBOSE=true
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

# Validate required parameters
if [[ -z "$FROM_VERSION" ]]; then
  echo "Error: Source version (--from) is required"
  exit 1
fi

if [[ -z "$TO_VERSION" ]]; then
  echo "Error: Target version (--to) is required"
  exit 1
fi

# Parse versions
if ! parse_version "$FROM_VERSION"; then
  exit 1
fi

if ! parse_version "$TO_VERSION"; then
  exit 1
fi

# Update a specific file's version
update_file_version() {
  local file_pattern="$1" from_version="$2" to_version="$3"
  
  # Parse the pattern
  IFS=':' read -r file_path pattern replacement <<< "$file_pattern"
  local full_path="$RINNA_DIR/$file_path"
  
  # Skip if file doesn't exist
  if [ ! -f "$full_path" ]; then
    log_message "INFO" "File not found: $file_path - skipping" "$VERBOSE"
    return 0
  fi
  
  # Replace version placeholders in template
  local processed_replacement=$(echo "$replacement" | sed -e "s|{version}|$to_version|g")
  
  # Parse version for major/minor/patch replacement
  processed_replacement=$(echo "$processed_replacement" | \
    sed -e "s|{major}|$VERSION_MAJOR|g" \
        -e "s|{minor}|$VERSION_MINOR|g" \
        -e "s|{patch}|$VERSION_PATCH|g")
  
  # Replace build number if needed
  if [[ "$processed_replacement" == *"{build}"* ]]; then
    local build_number=$(get_property "buildNumber" "$VERSION_FILE")
    processed_replacement=$(echo "$processed_replacement" | \
      sed -e "s|{build}|$build_number|g")
  fi

  # Special handling for parent POM references
  if [[ "$file_path" == *"pom.xml" ]] && [[ "$pattern" == *"</parent>"* ]]; then
    # For parent POM references, use a different approach to find the version within parent tag
    if grep -A 10 "<parent>" "$full_path" | grep -q "<version>"; then
      sed -i "/<parent>/,/<\/parent>/s|<version>.*</version>|<version>$to_version</version>|g" "$full_path"
      log_message "SUCCESS" "Updated parent version in $file_path to $to_version" "$VERBOSE"
      return 0
    else 
      log_message "WARNING" "Parent version tag not found in $file_path" "$VERBOSE"
      return 0
    fi
  fi
  
  # Default approach for other files
  if grep -q "$pattern" "$full_path"; then
    # For dry run mode, just show what would change
    if [ "$DRY_RUN" = true ]; then
      log_message "INFO" "Would update in $file_path: $pattern -> $processed_replacement" true
      return 0
    fi
    
    # Perform the actual update
    if sed -i -e "s|$pattern|$processed_replacement|g" "$full_path"; then
      log_message "SUCCESS" "Updated $file_path: $pattern -> $processed_replacement" "$VERBOSE"
      return 0
    else
      log_message "ERROR" "Failed to update $file_path" true
      return 1
    fi
  else
    # If main POM file, use a different pattern for the root version
    if [[ "$file_path" == "pom.xml" ]] && [[ "$pattern" == "<version>.*</version>" ]]; then
      # Use line-oriented approach for the root POM
      if grep -q "<version>" "$full_path"; then
        local line_num=$(grep -n "<version>" "$full_path" | head -1 | cut -d: -f1)
        if [ -n "$line_num" ]; then
          if [ "$DRY_RUN" = true ]; then
            log_message "INFO" "Would update version in $file_path line $line_num to $to_version" true
          else
            sed -i "${line_num}s|<version>.*</version>|<version>$to_version</version>|" "$full_path"
            log_message "SUCCESS" "Updated version in $file_path line $line_num to $to_version" "$VERBOSE"
          fi
          return 0
        fi
      fi
    fi
    
    log_message "WARNING" "Pattern not found in $file_path: $pattern" "$VERBOSE"
    return 0
  fi
}

# Main function to update all version references
update_all_versions() {
  local current_version="$1" new_version="$2"
  local errors=0
  
  log_message "INFO" "Starting version update: $current_version → $new_version" true
  
  # Update each file in the version patterns list
  for pattern in "${VERSION_FILE_PATTERNS[@]}"; do
    if ! update_file_version "$pattern" "$current_version" "$new_version"; then
      errors=$((errors + 1))
    fi
  done
  
  # Update build timestamp in version.properties
  if [ "$DRY_RUN" = false ]; then
    local current_timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    if grep -q "^build.timestamp=" "$VERSION_FILE"; then
      sed -i "s|^build.timestamp=.*|build.timestamp=$current_timestamp|" "$VERSION_FILE"
      log_message "SUCCESS" "Updated build.timestamp in version.properties" "$VERBOSE"
    fi
    
    if grep -q "^lastUpdated=" "$VERSION_FILE"; then
      sed -i "s|^lastUpdated=.*|lastUpdated=$(date +%Y-%m-%d)|" "$VERSION_FILE"
      log_message "SUCCESS" "Updated lastUpdated in version.properties" "$VERBOSE"
    fi
  fi
  
  log_message "INFO" "Version update completed with $errors errors" true
  return $errors
}

# Update file badges
update_badges() {
  local to_version="$1"
  local build_number=$(get_property "buildNumber" "$VERSION_FILE")
  local readme_file="$RINNA_DIR/README.md"
  
  if [ "$DRY_RUN" = true ]; then
    log_message "INFO" "Would update README badges: version=$to_version, build=$build_number" true
    return 0
  fi
  
  if [ -f "$readme_file" ]; then
    # Update badges using sed directly
    sed -i "s/badge\/version-[0-9.]\+-blue/badge\/version-${to_version}-blue/g" "$readme_file"
    sed -i "s/build-[0-9]\+/build-${build_number}/g" "$readme_file"
    log_message "SUCCESS" "Updated badges in README.md" "$VERBOSE"
  else
    log_message "WARNING" "README.md not found" "$VERBOSE"
  fi
}

# Main execution
echo "Rinna Version Updater"
echo "====================="
echo "From version: $FROM_VERSION"
echo "To version:   $TO_VERSION"

if [ "$DRY_RUN" = true ]; then
  echo "Mode: DRY RUN (no changes will be made)"
else
  echo "Mode: LIVE (changes will be applied)"
fi

# Perform the update
update_all_versions "$FROM_VERSION" "$TO_VERSION"
update_result=$?

# Update README badges
update_badges "$TO_VERSION"

if [ $update_result -eq 0 ]; then
  echo "Version update completed successfully."
  exit 0
else
  echo "Version update completed with errors."
  exit 1
fi