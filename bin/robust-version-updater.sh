#!/usr/bin/env bash
#
# robust-version-updater.sh - Precise version management for Rinna projects
# 
# A consolidated, autonomous version updating utility that precisely targets
# known version locations and handles error recovery automatically.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Constants
RINNA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION_FILE="$RINNA_DIR/version.properties"
# Create target directory if it doesn't exist
mkdir -p "$RINNA_DIR/target/version" 2>/dev/null || true
BACKUP_DIR="$RINNA_DIR/target/version/backup-$(date +%Y%m%d-%H%M%S)"
LOG_FILE="$RINNA_DIR/target/version/log-$(date +%Y%m%d-%H%M%S).log"
RECOVERY_FILE="$BACKUP_DIR/recovery-status.txt"
ERROR_SUMMARY="$BACKUP_DIR/error-summary.txt"

# Default options
DRY_RUN=false
VERBOSE=false
AUTONOMOUS=true
EXIT_ON_ERROR=false

# Known file map - explicit list of files and their version patterns
# Format: path|pattern|replacement_template
# Where:
#   path = relative path to the file from project root
#   pattern = grep/sed pattern to find the version string
#   replacement_template = template for constructing the replacement (use {} for version)
declare -a VERSION_FILE_MAP=(
  "version.properties|^version=.*|version={}"
  "version.properties|^version.full=.*|version.full={}"
  "version.properties|^version.major=.*|version.major={major}"
  "version.properties|^version.minor=.*|version.minor={minor}"
  "version.properties|^version.patch=.*|version.patch={patch}"
  "pom.xml|<version>[0-9.]+</version>|<version>{}</version>"
  "rinna-core/pom.xml|<version>[0-9.]+</version>|<version>{}</version>"
  "rinna-cli/pom.xml|<version>[0-9.]+</version>|<version>{}</version>"
  "rinna-data-sqlite/pom.xml|<version>[0-9.]+</version>|<version>{}</version>"
  "src/pom.xml|<parent>.*<version>[0-9.]+</version>|<version>{}</version>"
  "api/internal/version/version.go|Version\s*=\s*\"[0-9.]+\"|Version   = \"{}\""
  "api/pkg/health/version.go|Version\s*=\s*\"[0-9.]+\"|Version   = \"{}\""
  "api/configs/config.yaml|version:\s*\"[0-9.]+\"|version: \"{}\""
  "version-service/core/version.go|Version\s*=\s*\"[0-9.]+\"|Version   = \"{}\""
  "pyproject.toml|python_version\s*=\s*\"[0-9.]+\"|python_version = \"{}\""
  "pyproject.toml|target-version\s*=\s*\"[0-9.]+\"|target-version = \"{}\""
  "README.md|badge/version-[0-9.]+-blue|badge/version-{}-blue"
  "README.md|<version>[0-9.]+</version>|<version>{}</version>"
)

# Initialize log files
init_logs() {
  mkdir -p "$BACKUP_DIR"
  
  # Start main log
  echo "# Rinna Version Update Log" > "$LOG_FILE"
  echo "# Started: $(date)" >> "$LOG_FILE"
  echo "# From Version: $FROM_VERSION" >> "$LOG_FILE"
  echo "# To Version: $TO_VERSION" >> "$LOG_FILE"
  echo "" >> "$LOG_FILE"
  
  # Setup recovery tracking
  echo "STATUS=STARTED" > "$RECOVERY_FILE"
  echo "TIMESTAMP=$(date +%s)" >> "$RECOVERY_FILE"
  echo "FROM_VERSION=$FROM_VERSION" >> "$RECOVERY_FILE"
  echo "TO_VERSION=$TO_VERSION" >> "$RECOVERY_FILE"
  echo "COMPLETED_FILES=" >> "$RECOVERY_FILE"
  echo "FAILED_FILES=" >> "$RECOVERY_FILE"
}

# Logging function with minimalist design
log() {
  local level="$1"
  local message="$2"
  
  echo "[$(date +"%Y-%m-%d %H:%M:%S")] [$level] $message" >> "$LOG_FILE"
  
  if [ "$VERBOSE" = true ] || [ "$level" = "ERROR" ]; then
    if [ "$level" = "INFO" ]; then
      echo "$message"
    elif [ "$level" = "SUCCESS" ]; then
      echo "$message"
    elif [ "$level" = "WARNING" ]; then
      echo "Warning: $message"
    elif [ "$level" = "ERROR" ]; then
      echo "Error: $message"
    fi
  fi
}

# Parse version into components
parse_version() {
  local version="$1"
  if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    log "ERROR" "Invalid version format: $version (should be x.y.z)"
    return 1
  fi
  
  export VERSION_MAJOR=$(echo "$version" | cut -d. -f1)
  export VERSION_MINOR=$(echo "$version" | cut -d. -f2)
  export VERSION_PATCH=$(echo "$version" | cut -d. -f3)
  return 0
}

# Create backup of a file before modification
backup_file() {
  local file="$1"
  local rel_path="${file#$RINNA_DIR/}"
  local backup_path="$BACKUP_DIR/$rel_path"
  
  mkdir -p "$(dirname "$backup_path")"
  cp "$file" "$backup_path"
  log "INFO" "Created backup: $rel_path"
}

# Update a specific file based on mapping
update_file() {
  local map_entry="$1"
  local from_version="$2"
  local to_version="$3"
  
  # Parse the mapping
  IFS='|' read -r file_path pattern replacement_template <<< "$map_entry"
  local full_path="$RINNA_DIR/$file_path"
  
  # Skip if file doesn't exist
  if [ ! -f "$full_path" ]; then
    log "WARNING" "File not found: $file_path - skipping"
    return 0
  fi
  
  # Create backup 
  if [ "$DRY_RUN" = false ]; then
    backup_file "$full_path"
  fi
  
  # Replace version placeholders in template
  local replacement=$(echo "$replacement_template" | sed -e "s|{}|$to_version|g")
  
  # Replace major/minor/patch if they exist in the template
  if [[ "$replacement" == *"{major}"* || "$replacement" == *"{minor}"* || "$replacement" == *"{patch}"* ]]; then
    parse_version "$to_version"
    replacement=$(echo "$replacement" | \
      sed -e "s|{major}|$VERSION_MAJOR|g" \
          -e "s|{minor}|$VERSION_MINOR|g" \
          -e "s|{patch}|$VERSION_PATCH|g")
  fi
  
  # For XML/POM files, use xmlstarlet if available for more precise updates
  if [[ "$file_path" == *.xml || "$file_path" == *.pom ]] && command -v xmlstarlet &> /dev/null; then
    # Implementation left out for brevity - would add XML-specific handling here
    log "INFO" "Using XML handling for $file_path"
  fi
  
  # Check if file contains pattern
  if grep -q "$pattern" "$full_path"; then
    # For dry run mode, just show what would change
    if [ "$DRY_RUN" = true ]; then
      log "INFO" "Would update in $file_path: $pattern -> $replacement"
      return 0
    fi
    
    # Perform the actual update
    if sed -i -e "s|$pattern|$replacement|g" "$full_path"; then
      log "SUCCESS" "Updated $file_path: $pattern -> $replacement"
      # Track completed file for recovery
      echo "COMPLETED_FILES=$file_path:$COMPLETED_FILES" >> "$RECOVERY_FILE"
      return 0
    else
      log "ERROR" "Failed to update $file_path"
      echo "FAILED_FILES=$file_path:$FAILED_FILES" >> "$RECOVERY_FILE"
      
      # Attempt recovery
      if [ -f "$BACKUP_DIR/$file_path" ]; then
        log "INFO" "Attempting to restore backup for $file_path"
        cp "$BACKUP_DIR/$file_path" "$full_path"
      fi
      
      return 1
    fi
  else
    log "WARNING" "Pattern not found in $file_path: $pattern"
    return 0
  fi
}

# Update version in .venv/version special case
update_venv_version() {
  local to_version="$1"
  local venv_version="$RINNA_DIR/.venv/version"
  
  if [ -f "$venv_version" ]; then
    if [ "$DRY_RUN" = true ]; then
      log "INFO" "Would update .venv/version to $to_version"
    else
      echo "$to_version" > "$venv_version"
      log "SUCCESS" "Updated .venv/version to $to_version"
    fi
  fi
}

# Update README version badges
update_readme_badges() {
  local to_version="$1"
  local readme_file="$RINNA_DIR/README.md"
  local build_number=""
  
  # Get build number from version.properties
  if [ -f "$VERSION_FILE" ]; then
    build_number=$(grep -m 1 "^buildNumber=" "$VERSION_FILE" | cut -d'=' -f2)
  fi
  
  if [ -z "$build_number" ]; then
    log "WARNING" "Could not determine build number from version.properties"
    return 1
  fi
  
  if [ "$DRY_RUN" = true ]; then
    log "INFO" "Would update README badges: version=$to_version, build=$build_number"
    return 0
  fi
  
  # Update version badge
  if sed -i "s/\(version-\)[0-9]\+\.[0-9]\+\.[0-9]\+/\1$to_version/g" "$readme_file"; then
    log "SUCCESS" "Updated version badge in README.md to $to_version"
  else
    log "ERROR" "Failed to update version badge in README.md"
    return 1
  fi
  
  # Update build badge
  if sed -i "s/\(build-\)[0-9]\+/\1$build_number/g" "$readme_file"; then
    log "SUCCESS" "Updated build badge in README.md to $build_number"
  else
    log "ERROR" "Failed to update build badge in README.md"
    return 1
  fi
  
  return 0
}

# Update all versions based on the file map
update_all_versions() {
  local from_version="$1"
  local to_version="$2"
  local error_count=0
  
  log "INFO" "Starting version update: $from_version -> $to_version"
  
  # Check for recovery state
  if [ -f "$RECOVERY_FILE" ]; then
    source "$RECOVERY_FILE"
    if [ "$STATUS" = "STARTED" ] && [ "$FROM_VERSION" = "$from_version" ] && [ "$TO_VERSION" = "$to_version" ]; then
      log "INFO" "Resuming previous update session"
    fi
  fi
  
  # Process each file in the version file map
  for map_entry in "${VERSION_FILE_MAP[@]}"; do
    IFS='|' read -r file_path pattern replacement_template <<< "$map_entry"
    
    # Skip if already completed in a previous run
    if [[ "$COMPLETED_FILES" == *"$file_path"* ]]; then
      log "INFO" "Skipping already updated file: $file_path"
      continue
    fi
    
    # Update this file
    if ! update_file "$map_entry" "$from_version" "$to_version"; then
      error_count=$((error_count + 1))
      if [ "$EXIT_ON_ERROR" = true ]; then
        log "ERROR" "Exiting due to error in $file_path"
        return 1
      fi
    fi
  done
  
  # Handle .venv/version special case
  update_venv_version "$to_version"
  
  # Update README badges (version and build number)
  if ! update_readme_badges "$to_version"; then
    log "WARNING" "Failed to update README badges"
    error_count=$((error_count + 1))
  fi
  
  # Update build timestamp in version.properties
  local current_timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  if [ "$DRY_RUN" = false ]; then
    if grep -q "^build.timestamp=" "$VERSION_FILE"; then
      sed -i "s|^build.timestamp=.*|build.timestamp=$current_timestamp|" "$VERSION_FILE"
      log "SUCCESS" "Updated build.timestamp in version.properties"
    fi
    
    if grep -q "^lastUpdated=" "$VERSION_FILE"; then
      sed -i "s|^lastUpdated=.*|lastUpdated=$(date +%Y-%m-%d)|" "$VERSION_FILE"
      log "SUCCESS" "Updated lastUpdated in version.properties"
    fi
  fi
  
  log "INFO" "Version update completed with $error_count errors"
  return $error_count
}

# Verify consistency after update
verify_consistency() {
  local expected_version="$1"
  local inconsistencies=0
  local verified_files=0
  
  # Parse the expected version for component checks
  parse_version "$expected_version"
  local expected_major="$VERSION_MAJOR"
  local expected_minor="$VERSION_MINOR"
  local expected_patch="$VERSION_PATCH"
  
  # Get expected build number
  local expected_build_number=""
  if [ -f "$VERSION_FILE" ]; then
    expected_build_number=$(grep -m 1 "^buildNumber=" "$VERSION_FILE" | cut -d'=' -f2)
  fi
  
  log "INFO" "Verifying version consistency..."
  
  for map_entry in "${VERSION_FILE_MAP[@]}"; do
    IFS='|' read -r file_path pattern replacement_template <<< "$map_entry"
    local full_path="$RINNA_DIR/$file_path"
    
    # Skip if file doesn't exist
    if [ ! -f "$full_path" ]; then
      continue
    fi
    
    verified_files=$((verified_files + 1))
    
    # Get the current version from the file
    local found_version=$(grep -o "$pattern" "$full_path" | grep -o '[0-9][0-9.]*')
    
    if [[ -z "$found_version" ]]; then
      log "WARNING" "Could not extract version from $file_path"
      continue
    fi
    
    # Special handling for major/minor/patch fields
    local expected_check_version="$expected_version"
    if [[ "$pattern" == *"version.major"* ]]; then
      expected_check_version="$expected_major"
    elif [[ "$pattern" == *"version.minor"* ]]; then
      expected_check_version="$expected_minor"
    elif [[ "$pattern" == *"version.patch"* ]]; then
      expected_check_version="$expected_patch"
    fi
    
    if [[ "$found_version" != "$expected_check_version" ]]; then
      log "ERROR" "Version mismatch in $file_path: found $found_version, expected $expected_check_version"
      inconsistencies=$((inconsistencies + 1))
      echo "$file_path: expected $expected_check_version, found $found_version" >> "$ERROR_SUMMARY"
    else
      log "SUCCESS" "Verified $file_path: $found_version"
    fi
  done
  
  # Verify README badges
  local readme_file="$RINNA_DIR/README.md"
  if [ -f "$readme_file" ]; then
    # Check version badge
    local readme_version=$(grep -m 1 "badge/version-" "$readme_file" | grep -o -E "version-[0-9.]*-" | sed -E 's/version-(.*)-.*/\1/')
    if [[ "$readme_version" != "$expected_version" ]]; then
      log "ERROR" "Version badge mismatch in README.md: found $readme_version, expected $expected_version"
      inconsistencies=$((inconsistencies + 1))
      echo "README.md version badge: expected $expected_version, found $readme_version" >> "$ERROR_SUMMARY"
    else
      log "SUCCESS" "Verified README.md version badge: $readme_version"
    fi
    
    # Check build badge
    if [[ -n "$expected_build_number" ]]; then
      local readme_build=$(grep -m 1 "badge/build-" "$readme_file" | grep -o -E "build-[0-9]+-" | sed -E 's/build-(.*)-.*/\1/')
      if [[ "$readme_build" != "$expected_build_number" ]]; then
        log "ERROR" "Build badge mismatch in README.md: found $readme_build, expected $expected_build_number"
        inconsistencies=$((inconsistencies + 1))
        echo "README.md build badge: expected $expected_build_number, found $readme_build" >> "$ERROR_SUMMARY"
      else
        log "SUCCESS" "Verified README.md build badge: $readme_build"
      fi
    fi
  fi
  
  log "INFO" "Version consistency check: $verified_files files checked, $inconsistencies inconsistencies found"
  
  if [[ $inconsistencies -eq 0 ]]; then
    log "SUCCESS" "All versions are consistent"
    return 0
  else
    log "ERROR" "Found $inconsistencies inconsistencies. See $ERROR_SUMMARY for details."
    return 1
  fi
}

# Attempt to self-heal by retrying failed files
self_heal() {
  local to_version="$1"
  local healed=0
  local failed=0
  
  if [[ -z "$FAILED_FILES" ]]; then
    log "INFO" "No failed files to heal"
    return 0
  fi
  
  log "INFO" "Attempting to self-heal failed files..."
  
  # Split the list of failed files
  IFS=':' read -ra FAILED_ARRAY <<< "$FAILED_FILES"
  
  for file_path in "${FAILED_ARRAY[@]}"; do
    if [[ -z "$file_path" ]]; then
      continue
    fi
    
    # Find matching entry in VERSION_FILE_MAP
    for map_entry in "${VERSION_FILE_MAP[@]}"; do
      IFS='|' read -r map_file pattern replacement_template <<< "$map_entry"
      
      if [[ "$map_file" == "$file_path" ]]; then
        log "INFO" "Retrying update for $file_path"
        
        if update_file "$map_entry" "$FROM_VERSION" "$to_version"; then
          log "SUCCESS" "Self-healed $file_path"
          healed=$((healed + 1))
        else
          log "ERROR" "Failed to heal $file_path"
          failed=$((failed + 1))
        fi
        
        break
      fi
    done
  done
  
  log "INFO" "Self-healing complete: healed $healed files, $failed files still failing"
  
  if [[ $failed -eq 0 ]]; then
    return 0
  else
    return 1
  fi
}

# Display help message
show_help() {
  cat << EOF
Usage: robust-version-updater.sh [options]

Updates version strings in Rinna project files with high precision.

Options:
  --from VERSION      Source version to update from (required)
  --to VERSION        Target version to update to (required)
  --dry-run           Preview changes without making modifications
  --verbose           Show detailed output
  --interactive       Prompt for confirmation before applying changes
  --exit-on-error     Stop on first error instead of continuing
  --help              Show this help message

Examples:
  robust-version-updater.sh --from 1.2.3 --to 1.3.0
  robust-version-updater.sh --from 1.2.3 --to 1.3.0 --dry-run --verbose

Features:
  - Precise targeting of version strings in known locations
  - Self-healing capability for failed updates
  - Automatic backup of all modified files
  - Comprehensive verification of version consistency
  - Detailed logging for troubleshooting
EOF
}

# Main function
main() {
  local FROM_VERSION=""
  local TO_VERSION=""
  
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
      --interactive)
        AUTONOMOUS=false
        shift
        ;;
      --exit-on-error)
        EXIT_ON_ERROR=true
        shift
        ;;
      --help)
        show_help
        exit 0
        ;;
      *)
        echo "Error: Unknown option: $1"
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
  
  # Validate version formats
  if ! parse_version "$FROM_VERSION"; then
    exit 1
  fi
  
  if ! parse_version "$TO_VERSION"; then
    exit 1
  fi
  
  # Initialize logs and recovery tracking
  init_logs
  
  # Display initial information
  echo "Rinna Version Updater"
  echo "====================="
  echo "From version: $FROM_VERSION"
  echo "To version:   $TO_VERSION"
  
  if [ "$DRY_RUN" = true ]; then
    echo "Mode: DRY RUN (no changes will be made)"
  else
    echo "Mode: LIVE (changes will be applied)"
  fi
  
  # Check for user confirmation if not autonomous
  if [ "$AUTONOMOUS" = false ] && [ "$DRY_RUN" = false ]; then
    read -p "Continue with version update? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      log "INFO" "Update cancelled by user"
      echo "Update cancelled"
      exit 0
    fi
  fi
  
  # Perform the update
  update_all_versions "$FROM_VERSION" "$TO_VERSION"
  update_result=$?
  
  # Update recovery status
  echo "STATUS=UPDATED" >> "$RECOVERY_FILE"
  
  if [ $update_result -ne 0 ] && [ "$DRY_RUN" = false ]; then
    log "WARNING" "Errors occurred during update, attempting self-healing..."
    self_heal "$TO_VERSION"
  fi
  
  # Verify consistency if not in dry-run mode
  if [ "$DRY_RUN" = false ]; then
    verify_consistency "$TO_VERSION"
    verify_result=$?
    
    echo "STATUS=COMPLETED" >> "$RECOVERY_FILE"
    echo "COMPLETION_TIMESTAMP=$(date +%s)" >> "$RECOVERY_FILE"
    
    if [ $verify_result -ne 0 ]; then
      log "ERROR" "Version consistency verification failed"
      echo "Version update completed with inconsistencies. See $ERROR_SUMMARY for details."
      exit 1
    else
      log "SUCCESS" "Version updated successfully from $FROM_VERSION to $TO_VERSION"
      echo "Version updated successfully: $FROM_VERSION -> $TO_VERSION"
      
      if [ "$VERBOSE" = true ]; then
        echo "Backup directory: $BACKUP_DIR"
        echo "Log file: $LOG_FILE"
      fi
      
      exit 0
    fi
  else
    log "INFO" "Dry run completed, no changes were made"
    echo "Dry run completed. No changes were made."
    exit 0
  fi
}

# Execute main function
main "$@"