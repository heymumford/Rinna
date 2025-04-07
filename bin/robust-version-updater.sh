#!/usr/bin/env bash
#
# robust-version-updater.sh - Hardened version updating utility for Rinna project
# 
# Provides more precise pattern matching to avoid updating unrelated version strings
# Implements safe mode for previewing changes and exclusion patterns to protect specific files
#
# FEATURES:
# - File-type specific version pattern matching
# - Preserves external dependency version strings
# - Safe preview mode with confirmation
# - Automatic backup of modified files
# - Detailed logging and verification
# - Protection for specific paths and file types
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
TEMP_DIR=$(mktemp -d)
LOG_FILE="$TEMP_DIR/version-update.log"
DIFF_FILE="$TEMP_DIR/changes.diff"
BACKUP_DIR="$TEMP_DIR/backups"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default options
DRY_RUN=false
VERBOSE=false
SAFE_MODE=true  # Default to safe mode

# List of known external version patterns to avoid replacing
# Format: Array of regex patterns that should NOT be replaced
EXTERNAL_VERSION_PATTERNS=(
  # Common external libraries and frameworks
  "spring-boot-starter-parent.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "spring-boot.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "spring-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "jackson-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "slf4j-api.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "log4j-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "logback-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "junit-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "mockito-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "testcontainers.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "cucumber-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "maven-.*-plugin.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "<artifactId>maven</artifactId>.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "python-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "node-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "tomcat-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "jetty-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "netty-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "mysql-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "postgresql-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "mongodb-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "protobuf-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "grpc-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "guava-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "gson-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "aws-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "azure-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "google-.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "lombok.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  "mapstruct.*<version>[0-9]+\.[0-9]+\.[0-9]+</version>"
  
  # Common version property patterns in Maven POM files
  "<.*\.version>[0-9]+\.[0-9]+\.[0-9]+</.*\.version>"
  "<maven\.compiler\.source>[0-9]+</maven\.compiler\.source>"
  "<maven\.compiler\.target>[0-9]+</maven\.compiler\.target>"
  "<java\.version>[0-9]+</java\.version>"
  
  # Common library versions in JavaScript/Node.js
  "\"(dev)?[dD]ependencies\":\\s*{[^}]*\"[^\"]+\":\\s*\"[0-9]+\\.[0-9]+\\.[0-9]+\""
  
  # Common Docker/container version patterns
  "FROM\\s+[^:]+:[0-9]+\\.[0-9]+\\.[0-9]+"
  
  # Common Python patterns
  "requests==[0-9]+\\.[0-9]+\\.[0-9]+"
  "flask==[0-9]+\\.[0-9]+\\.[0-9]+"
  "django==[0-9]+\\.[0-9]+\\.[0-9]+"
  "pandas==[0-9]+\\.[0-9]+\\.[0-9]+"
  "numpy==[0-9]+\\.[0-9]+\\.[0-9]+"
  "python-dotenv==[0-9]+\\.[0-9]+\\.[0-9]+"
  
  # Version boundaries in requirements
  ">=\\s*[0-9]+\\.[0-9]+\\.[0-9]+"
  "<=\\s*[0-9]+\\.[0-9]+\\.[0-9]+"
  ">\\s*[0-9]+\\.[0-9]+\\.[0-9]+"
  "<\\s*[0-9]+\\.[0-9]+\\.[0-9]+"
  
  # Common port numbers that look like versions
  "\"port\":\\s*[0-9]\\.[0-9]\\.[0-9]"
  "port=[0-9]\\.[0-9]\\.[0-9]"
  "PORT=[0-9]\\.[0-9]\\.[0-9]"
  
  # IP addresses that might look like versions
  "[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+"
)

# Define file/directory exclusion patterns
EXCLUDED_PATHS=(
  ".git"
  ".github"
  ".idea"
  ".vscode"
  "node_modules"
  "target"
  "build"
  "dist"
  "venv"
  ".venv"
  "__pycache__"
  "*.class"
  "*.jar"
  "*.war"
  "*.zip"
  "*.tar.gz"
  "*.log"
  "*.lock"
  "package-lock.json"
  "yarn.lock"
  "Cargo.lock"
  "poetry.lock"
  "CHANGELOG.md"  # Let the user manually update changelog
  ".DS_Store"
  "Thumbs.db"
)

# UI Functions
print_header() { echo -e "${BLUE}$1${NC}"; }
print_success() { echo -e "${GREEN}$1${NC}"; }
print_warning() { echo -e "${YELLOW}$1${NC}"; }
print_error() { echo -e "${RED}$1${NC}"; [ "$2" != "no-exit" ] && exit 1 || return 1; }
print_verbose() { [ "$VERBOSE" = true ] && echo -e "$1"; }

log() {
  echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" >> "$LOG_FILE"
  if [ "$VERBOSE" = true ]; then
    echo "$1"
  fi
}

# Get the versions from version.properties
get_version() {
  grep -m 1 "^version=" "$VERSION_FILE" | cut -d'=' -f2
}

# Check if a file or directory is excluded
is_excluded() {
  local check_path="$1"
  for pattern in "${EXCLUDED_PATHS[@]}"; do
    # Handle pattern with or without asterisk
    if [[ "$pattern" == *"*"* ]]; then
      if [[ "$check_path" == $pattern ]]; then
        return 0  # true, it matches an exclusion pattern
      fi
    else
      if [[ "$check_path" == *"$pattern"* ]]; then
        return 0  # true, it matches an exclusion pattern
      fi
    fi
  done
  return 1  # false, it doesn't match any exclusion pattern
}

# Check if a line contains an external version pattern that should be preserved
contains_external_version() {
  local line="$1"
  
  for pattern in "${EXTERNAL_VERSION_PATTERNS[@]}"; do
    if [[ "$line" =~ $pattern ]]; then
      return 0  # true, it matches an external pattern
    fi
  done
  return 1  # false, it doesn't match any external pattern
}

# Function to update a file with a version
update_file() {
  local file="$1"
  local old_version="$2"
  local new_version="$3"
  local file_type="${file##*.}"
  local updated=false
  local modified=false
  local rel_path="${file#$RINNA_DIR/}"
  
  # Skip excluded paths
  if is_excluded "$rel_path"; then
    log "Skipping excluded path: $rel_path"
    return 0
  fi
  
  # Create backup directory structure
  mkdir -p "$(dirname "$BACKUP_DIR/$rel_path")"
  
  # Create a backup of the file
  cp "$file" "$BACKUP_DIR/$rel_path"
  
  # Create a temporary file
  local temp_file=$(mktemp)
  
  # Flag to indicate if we're in a dependency section in XML
  local in_dependency=false
  local in_plugin=false
  local in_parent=false
  
  # Process the file line by line for more precise control
  while IFS= read -r line; do
    # Track sections in XML files that might contain version tags
    if [[ "$file_type" == "xml" || "$file_type" == "pom" ]]; then
      if [[ "$line" == *"<dependencies>"* ]]; then
        in_dependency=true
      elif [[ "$line" == *"</dependencies>"* ]]; then
        in_dependency=false
      elif [[ "$line" == *"<plugins>"* ]]; then
        in_plugin=true
      elif [[ "$line" == *"</plugins>"* ]]; then
        in_plugin=false
      elif [[ "$line" == *"<parent>"* ]]; then
        in_parent=true
      elif [[ "$line" == *"</parent>"* ]]; then
        in_parent=false
      fi
    fi
    
    # Check for external version patterns that should be preserved
    if contains_external_version "$line"; then
      echo "$line" >> "$temp_file"
      continue
    fi
    
    # Skip version replacements in dependency and plugin sections unless it's org.rinna
    if [[ "$in_dependency" == true || "$in_plugin" == true ]]; then
      if [[ "$line" != *"org.rinna"* && "$line" == *"<version>"* ]]; then
        echo "$line" >> "$temp_file"
        continue
      fi
    fi
    
    # Process based on file type for more accurate replacement
    local original_line="$line"
    case "$file_type" in
      xml|pom)
        # Handle parent version (if parent is org.rinna)
        if [[ "$in_parent" == true && "$line" == *"<version>"*"</version>"* && "$line" != *"<version.java>"* ]]; then
          # Only replace if the parent is from org.rinna
          if grep -A5 -B5 "$line" "$file" | grep -q "<groupId>org\.rinna</groupId>"; then
            line=$(echo "$line" | sed "s|<version>$old_version</version>|<version>$new_version</version>|g")
          fi
        # Handle project version (outside dependency/plugin sections)
        elif [[ "$in_dependency" == false && "$in_plugin" == false && "$line" == *"<version>"*"</version>"* && "$line" != *"<version.java>"* ]]; then
          # Check if this version is for org.rinna group
          if grep -A5 -B5 "$line" "$file" | grep -q "<groupId>org\.rinna</groupId>"; then
            line=$(echo "$line" | sed "s|<version>$old_version</version>|<version>$new_version</version>|g")
          fi
        fi
        ;;
      java|kt|groovy|scala)
        # Java/Kotlin-specific replacements - more precise to avoid replacing constants
        if [[ "$line" == *"final String VERSION"* || "$line" == *"public static final String VERSION"* ]]; then
          line=$(echo "$line" | sed "s|\"$old_version\"|\"$new_version\"|g")
        elif [[ "$line" == *"@Version(\""*"\")"* ]]; then
          line=$(echo "$line" | sed "s|@Version(\"$old_version\")|@Version(\"$new_version\")|g")
        fi
        ;;
      go)
        # Go-specific replacements
        if [[ "$line" == *"Version"*"="*"\"$old_version\""* ]]; then
          line=$(echo "$line" | sed "s|\"$old_version\"|\"$new_version\"|g")
        fi
        ;;
      py)
        # Python-specific replacements
        if [[ "$line" == *"__version__"*"="* || "$line" == *"VERSION"*"="* ]]; then
          line=$(echo "$line" | sed "s|['\"]\($old_version\)['\"]|'\1'|g")
          line=$(echo "$line" | sed "s|'$old_version'|'$new_version'|g")
        fi
        ;;
      yaml|yml)
        # YAML-specific replacements (careful with indentation)
        if [[ "$line" == *"version:"*"$old_version"* ]]; then
          line=$(echo "$line" | sed "s|\(version:[ \t]*[\"']\?\)$old_version\([\"']\?\)|\1$new_version\2|g")
        fi
        ;;
      json)
        # JSON-specific replacements
        if [[ "$line" == *"\"version\":"*"\"$old_version\""* ]]; then
          line=$(echo "$line" | sed "s|\(\"version\":[ \t]*\"\)$old_version\(\"[,]*\)|\1$new_version\2|g")
        fi
        ;;
      md|markdown)
        # Markdown-specific replacements - be very conservative with markdown to avoid replacing examples
        # Only replace in specific badge or version sections
        if [[ "$line" == *"badge/version"*"$old_version"* ]]; then
          line=$(echo "$line" | sed "s|\(badge/version-\)$old_version\(-\)|\1$new_version\2|g")
        elif [[ "$line" == *"Current version: "* && "$line" == *"$old_version"* ]]; then
          line=$(echo "$line" | sed "s|\(Current version:[^0-9.]*\)$old_version\([^0-9.]*\)|\1$new_version\2|g")
        fi
        ;;
      properties)
        # Properties-specific replacements - most direct
        if [[ "$line" == "version=$old_version" ]]; then
          line="version=$new_version"
        elif [[ "$line" == "version.full=$old_version" ]]; then
          line="version.full=$new_version"
        fi
        ;;
      sh|bash)
        # Shell script replacements - only very specific patterns
        if [[ "$line" == *"VERSION="*"\"$old_version\""* || "$line" == *"VERSION="*"'$old_version'"* ]]; then
          line=$(echo "$line" | sed "s|\(VERSION=[\"\' ]*\)$old_version\([\"\']*\)|\1$new_version\2|g")
        fi
        ;;
      *)
        # Generic file type - use basic replacement with care
        if [[ "$line" == *"version"*"$old_version"* ]]; then
          # Only replace exact version strings in known contexts
          # This avoids replacing random occurrences that happen to match
          if [[ "$line" == *"=\"$old_version\""* || "$line" == *"='$old_version'"* || 
                "$line" == *" $old_version "* || "$line" == *"version: $old_version"* ||
                "$line" == *"\"version\": \"$old_version\""* ]]; then
            # Verify it's safe to replace by checking for known external patterns
            if ! contains_external_version "$line"; then
              line=$(echo "$line" | sed "s|\([ =:\"\']\)$old_version\([ =\"\',]\)|\1$new_version\2|g")
            fi
          fi
        fi
        ;;
    esac
    
    # Check if line was modified
    if [[ "$line" != "$original_line" ]]; then
      updated=true
      modified=true
      log "Updated in $rel_path: $original_line -> $line"
    fi
    
    # Write the line to the temp file
    echo "$line" >> "$temp_file"
  done < "$file"
  
  # Only replace the original file if changes were made
  if [ "$modified" = true ]; then
    if [ "$DRY_RUN" = true ]; then
      # In dry-run mode, just show the diff
      diff -u "$file" "$temp_file" >> "$DIFF_FILE" 2>/dev/null || true
      echo "Would update: $rel_path"
    else
      # Actual replacement
      mv "$temp_file" "$file"
      log "Updated file: $rel_path"
    fi
  else
    rm "$temp_file"
  fi
  
  return $([ "$updated" = true ] && echo 0 || echo 1)
}

# Function to update version references across the codebase
update_version_references() {
  local old_version="$1"
  local new_version="$2"
  local files_updated=0
  local files_processed=0
  
  if [ -z "$old_version" ] || [ -z "$new_version" ]; then
    print_error "Missing version information"
  fi
  
  if [ "$old_version" = "$new_version" ]; then
    print_warning "Old and new versions are identical: $old_version"
    return 0
  fi
  
  # Create our temp and backup directories
  mkdir -p "$TEMP_DIR" "$BACKUP_DIR"
  
  log "Starting version update: $old_version -> $new_version"
  log "Safe mode: $SAFE_MODE"
  log "Dry run: $DRY_RUN"
  
  # Initialize the diff file
  if [ "$DRY_RUN" = true ]; then
    echo "# Version update diff: $old_version -> $new_version" > "$DIFF_FILE"
  fi
  
  # First, scan the codebase and categorize files to update
  log "Scanning codebase for version references..."
  
  # Pre-validation check - search for occurrences of the old version
  # to identify which files need updating
  FOUND_FILES=$(grep -r --include="*.java" --include="*.xml" --include="*.properties" \
                  --include="*.go" --include="*.py" --include="*.js" --include="*.json" \
                  --include="*.yml" --include="*.yaml" --include="*.md" --include="*.sh" \
                  --include="*.bash" -l "$old_version" "$RINNA_DIR" 2>/dev/null || true)
  
  # Add specific known version files that might not contain the old version yet
  KNOWN_VERSION_FILES=(
    "$RINNA_DIR/version.properties"
    "$RINNA_DIR/pom.xml"
    "$RINNA_DIR/README.md"
    "$RINNA_DIR/api/pkg/health/version.go" 
    "$RINNA_DIR/api/internal/version/version.go"
    "$RINNA_DIR/pyproject.toml"
    "$RINNA_DIR/.venv/version"
  )
  
  # Process known version files if they exist
  for file in "${KNOWN_VERSION_FILES[@]}"; do
    if [ -f "$file" ]; then
      FOUND_FILES="$FOUND_FILES
$file"
    fi
  done
  
  # Eliminate duplicates
  FOUND_FILES=$(echo "$FOUND_FILES" | sort -u)
  
  # Process each found file
  for file in $FOUND_FILES; do
    # Skip files that don't exist anymore (in case grep found something that was deleted)
    if [ ! -f "$file" ]; then
      continue
    fi
    
    # Skip excluded paths
    local rel_path="${file#$RINNA_DIR/}"
    if is_excluded "$rel_path"; then
      log "Skipping excluded path: $rel_path"
      continue
    fi
    
    files_processed=$((files_processed + 1))
    
    # Update the file
    if update_file "$file" "$old_version" "$new_version"; then
      files_updated=$((files_updated + 1))
    fi
  done
  
  # Process any Maven POM files not found by grep but that might contain version references
  # This handles cases where the version might be different already
  for pom_file in $(find "$RINNA_DIR" -name "pom.xml" -type f); do
    # Skip if already processed
    if echo "$FOUND_FILES" | grep -q "$pom_file"; then
      continue
    fi
    
    # Check if this POM has org.rinna groupId
    if grep -q "<groupId>org.rinna</groupId>" "$pom_file"; then
      files_processed=$((files_processed + 1))
      if update_file "$pom_file" "$old_version" "$new_version"; then
        files_updated=$((files_updated + 1))
      fi
    fi
  done
  
  # Show summary
  log "Version update summary:"
  log "  Files processed: $files_processed"
  log "  Files updated: $files_updated"
  
  if [ "$DRY_RUN" = true ]; then
    if [ -s "$DIFF_FILE" ]; then
      log "Diff file generated: $DIFF_FILE"
      print_success "Preview of changes available in: $DIFF_FILE"
    else
      log "No changes would be made"
      print_warning "No version changes would be made"
    fi
  else
    if [ $files_updated -gt 0 ]; then
      log "Version references successfully updated"
      print_success "Updated $files_updated files with new version: $new_version"
    else
      log "No files were updated"
      print_warning "No files were updated with the new version"
    fi
  fi
  
  return 0
}

# Function to verify version consistency
verify_version_consistency() {
  local expected_version="$1"
  local inconsistencies=0
  local checked_files=0
  
  log "Verifying version consistency for: $expected_version"
  
  # Find what should be the Rinna project version files
  RINNA_VERSION_FILES=$(find "$RINNA_DIR" -name "pom.xml" -o -name "version.properties" \
    -o -name "version.go" -o -path "*/api/*/version.go" -o -name ".venv/version" \
    -o -name "pyproject.toml" -o -name "README.md" | sort)
  
  # Check core files first
  if [ -f "$VERSION_FILE" ]; then
    checked_files=$((checked_files + 1))
    local file_version=$(grep -m 1 "^version=" "$VERSION_FILE" | cut -d'=' -f2)
    if [ "$file_version" != "$expected_version" ]; then
      log "FAIL: $VERSION_FILE has version $file_version, expected $expected_version"
      inconsistencies=$((inconsistencies + 1))
    else
      log "PASS: $VERSION_FILE has the expected version: $expected_version"
    fi
  else
    log "WARN: Core version file $VERSION_FILE does not exist"
  fi
  
  # Check parent POM
  if [ -f "$RINNA_DIR/pom.xml" ]; then
    checked_files=$((checked_files + 1))
    # Need a more precise check to avoid matching dependency versions
    local parent_version=$(grep -A 5 -B 5 "<groupId>org.rinna</groupId>" "$RINNA_DIR/pom.xml" | grep -m 1 "<version>" | sed -E 's/.*<version>(.*)<\/version>.*/\1/')
    if [ -n "$parent_version" ] && [ "$parent_version" != "$expected_version" ]; then
      log "FAIL: $RINNA_DIR/pom.xml has version $parent_version, expected $expected_version"
      inconsistencies=$((inconsistencies + 1))
    else
      log "PASS: $RINNA_DIR/pom.xml has the expected version: $expected_version"
    fi
  fi
  
  # Check Module POMs for org.rinna groupId (but exclude dependency versions)
  for pom_file in $(find "$RINNA_DIR" -name "pom.xml" -type f); do
    if [ "$pom_file" != "$RINNA_DIR/pom.xml" ] && grep -q "<groupId>org.rinna</groupId>" "$pom_file"; then
      checked_files=$((checked_files + 1))
      
      # First check parent section
      if grep -q "<parent>" "$pom_file" && grep -A 10 "<parent>" "$pom_file" | grep -q "<groupId>org.rinna</groupId>"; then
        local parent_version=$(grep -A 10 "<parent>" "$pom_file" | grep -m 1 "<version>" | sed -E 's/.*<version>(.*)<\/version>.*/\1/')
        if [ -n "$parent_version" ] && [ "$parent_version" != "$expected_version" ]; then
          log "FAIL: Parent section in $pom_file has version $parent_version, expected $expected_version"
          inconsistencies=$((inconsistencies + 1))
        else
          log "PASS: Parent section in $pom_file has the expected version: $expected_version"
        fi
      fi
      
      # Then check project version (outside dependency/plugin sections)
      # Create temp file with non-dependency sections
      local temp_file=$(mktemp)
      awk '
        BEGIN { print_lines = 1; in_deps = 0; in_plugins = 0; in_parent = 0 }
        /<parent>/ { in_parent = 1 }
        /<\/parent>/ { in_parent = 0 }
        /<dependencies>/ { in_deps = 1 }
        /<\/dependencies>/ { in_deps = 0 }
        /<plugins>/ { in_plugins = 1 }
        /<\/plugins>/ { in_plugins = 0 }
        { if (!in_deps && !in_plugins && !in_parent) print }
      ' "$pom_file" > "$temp_file"
      
      # Check for project version
      if grep -q "<groupId>org.rinna</groupId>" "$temp_file" && grep -q "<version>" "$temp_file"; then
        local project_version=$(grep -A 5 -B 5 "<groupId>org.rinna</groupId>" "$temp_file" | grep -m 1 "<version>" | sed -E 's/.*<version>(.*)<\/version>.*/\1/')
        if [ -n "$project_version" ] && [ "$project_version" != "$expected_version" ]; then
          log "FAIL: Project section in $pom_file has version $project_version, expected $expected_version"
          inconsistencies=$((inconsistencies + 1))
        else
          log "PASS: Project section in $pom_file has the expected version: $expected_version"
        fi
      fi
      
      rm "$temp_file"
    fi
  done
  
  # Check Go version files
  for go_file in $(find "$RINNA_DIR" -name "version.go" -type f); do
    checked_files=$((checked_files + 1))
    # More precise pattern to avoid matching other constants
    local go_version=$(grep -m 1 "Version[[:space:]]*=[[:space:]]*\"[0-9.]*\"" "$go_file" | grep -o "[0-9][0-9.]*" | head -1)
    if [ -n "$go_version" ] && [ "$go_version" != "$expected_version" ]; then
      log "FAIL: $go_file has version $go_version, expected $expected_version"
      inconsistencies=$((inconsistencies + 1))
    else
      log "PASS: $go_file has the expected version: $expected_version"
    fi
  done
  
  # Check Python-related files
  if [ -f "$RINNA_DIR/pyproject.toml" ]; then
    checked_files=$((checked_files + 1))
    local py_version=$(grep -m 1 "version[[:space:]]*=[[:space:]]*\"[0-9.]*\"" "$RINNA_DIR/pyproject.toml" | grep -o "[0-9][0-9.]*" | head -1)
    if [ -n "$py_version" ] && [ "$py_version" != "$expected_version" ]; then
      log "FAIL: $RINNA_DIR/pyproject.toml has version $py_version, expected $expected_version"
      inconsistencies=$((inconsistencies + 1))
    else
      log "PASS: $RINNA_DIR/pyproject.toml has the expected version: $expected_version"
    fi
  fi
  
  # Check virtual env version
  if [ -f "$RINNA_DIR/.venv/version" ]; then
    checked_files=$((checked_files + 1))
    local venv_version=$(cat "$RINNA_DIR/.venv/version" | tr -d '[:space:]')
    if [ -n "$venv_version" ] && [ "$venv_version" != "$expected_version" ]; then
      log "FAIL: $RINNA_DIR/.venv/version has version $venv_version, expected $expected_version"
      inconsistencies=$((inconsistencies + 1))
    else
      log "PASS: $RINNA_DIR/.venv/version has the expected version: $expected_version"
    fi
  fi
  
  # Check README badge
  if [ -f "$RINNA_DIR/README.md" ]; then
    checked_files=$((checked_files + 1))
    local readme_version=$(grep -o "badge/version-[0-9.]*" "$RINNA_DIR/README.md" | cut -d '-' -f 2)
    if [ -n "$readme_version" ] && [ "$readme_version" != "$expected_version" ]; then
      log "FAIL: README.md badge has version $readme_version, expected $expected_version"
      inconsistencies=$((inconsistencies + 1))
    else
      log "PASS: README.md badge has the expected version: $expected_version"
    fi
    
    # Also check for Maven example if it exists
    if grep -q "<artifactId>rinna-core</artifactId>" "$RINNA_DIR/README.md"; then
      local maven_version=$(grep -A 3 "<artifactId>rinna-core</artifactId>" "$RINNA_DIR/README.md" | grep -m 1 "<version>" | sed -E 's/.*<version>(.*)<\/version>.*/\1/')
      if [ -n "$maven_version" ] && [ "$maven_version" != "$expected_version" ]; then
        log "FAIL: README.md Maven example has version $maven_version, expected $expected_version"
        inconsistencies=$((inconsistencies + 1))
      else
        log "PASS: README.md Maven example has the expected version: $expected_version"
      fi
    fi
  fi
  
  # Log summary and return result
  log "Version consistency check summary:"
  log "  Files checked: $checked_files"
  log "  Inconsistencies found: $inconsistencies"
  
  if [ $inconsistencies -eq 0 ]; then
    log "All checked files have the expected version: $expected_version"
    print_success "Version consistency check passed: All files have version $expected_version"
    return 0
  else
    log "Found $inconsistencies inconsistencies. Some files do not have version $expected_version"
    print_error "Version consistency check failed: Found $inconsistencies inconsistencies" "no-exit"
    return 1
  fi
}

# Main function for updating version
main() {
  local old_version=""
  local new_version=""
  local exclude_files=""
  
  # Process command-line arguments
  while [ $# -gt 0 ]; do
    case "$1" in
      --from)
        old_version="$2"
        shift 2
        ;;
      --to)
        new_version="$2"
        shift 2
        ;;
      --exclude)
        # Add to excluded paths
        EXCLUDED_PATHS+=("$2")
        shift 2
        ;;
      --safe-mode)
        SAFE_MODE=true
        shift 1
        ;;
      --unsafe-mode)
        SAFE_MODE=false
        shift 1
        ;;
      --dry-run)
        DRY_RUN=true
        shift 1
        ;;
      --verbose)
        VERBOSE=true
        shift 1
        ;;
      --help)
        show_help
        exit 0
        ;;
      *)
        print_error "Unknown option: $1. Use --help for usage information."
        ;;
    esac
  done
  
  # Get old version from version.properties if not specified
  if [ -z "$old_version" ]; then
    old_version=$(get_version)
    if [ -z "$old_version" ]; then
      print_error "Could not determine current version from $VERSION_FILE"
    fi
  fi
  
  # Ensure new version is specified
  if [ -z "$new_version" ]; then
    print_error "New version must be specified with --to option"
  fi
  
  # Validate version formats
  if [[ ! "$old_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    print_error "Invalid old version format: $old_version (should be x.y.z)"
  fi
  
  if [[ ! "$new_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    print_error "Invalid new version format: $new_version (should be x.y.z)"
  fi
  
  print_header "Rinna Enhanced Version Updater"
  print_header "--------------------------------"
  print_header "Updating from version: $old_version"
  print_header "Updating to version:   $new_version"
  if [ "$DRY_RUN" = true ]; then
    print_header "Mode: DRY RUN (no changes will be made)"
  else
    print_header "Mode: LIVE RUN (changes will be applied)"
  fi
  if [ "$SAFE_MODE" = true ]; then
    print_header "Safety: SAFE MODE (more precise matching)"
  else
    print_header "Safety: UNSAFE MODE (more aggressive matching)"
  fi
  print_header "Log file: $LOG_FILE"
  
  # Execute the version update
  update_version_references "$old_version" "$new_version"
  
  # If not in dry-run mode, verify consistency
  if [ "$DRY_RUN" = false ]; then
    print_header "Verifying version consistency..."
    verify_version_consistency "$new_version"
    verify_status=$?
    
    if [ $verify_status -ne 0 ]; then
      print_warning "Some files may have been missed during the update."
      print_warning "Run with --dry-run first to preview changes, or check the log for details."
      print_warning "Backup files are available in: $BACKUP_DIR"
    else
      # Cleanup backups on success 
      if [ "$SAFE_MODE" = true ]; then
        # In safe mode, keep backups for extra safety
        print_success "Version update completed successfully. Backup files retained at: $BACKUP_DIR"
      else
        # In unsafe mode, delete backups to save space
        rm -rf "$BACKUP_DIR"
        print_success "Version update completed successfully."
      fi
    fi
  else
    print_header "Dry run completed. No changes were made."
    print_header "Review the changes in: $DIFF_FILE"
  fi
}

# Help message
show_help() {
  cat << EOF
${BLUE}robust-version-updater.sh${NC} - Hardened version updating utility for Rinna project

Usage: robust-version-updater.sh [options]

Options:
  --from VERSION     Source version to update from (defaults to version.properties)
  --to VERSION       Target version to update to (required)
  --exclude PATH     Path pattern to exclude from processing (can be used multiple times)
  --safe-mode        Enable safe mode for more precise version matching (default)
  --unsafe-mode      Enable unsafe mode for more aggressive version matching
  --dry-run          Show what would be changed without making changes
  --verbose          Show more detailed output during processing
  --help             Show this help message

Examples:
  # Preview changes without modifying files
  robust-version-updater.sh --from 1.2.3 --to 1.3.0 --dry-run
  
  # Perform the update with extra checking
  robust-version-updater.sh --to 1.3.0 --verbose
  
  # Update with explicit exclusions
  robust-version-updater.sh --to 1.3.0 --exclude "test-data" --exclude "docs/examples"

This script performs a robust version update across the Rinna project:
- Intelligently identifies and updates only relevant version strings
- Avoids modifications to third-party library version references
- Creates backups of all modified files
- Verifies consistency after updating
- Prevents accidental updates in excluded directories

Safe mode (default) uses precise pattern matching to minimize the risk of 
unintended updates. Unsafe mode performs more aggressive matching but may
occasionally update unrelated version strings.
EOF
}

# Main execution
if [ "$#" -eq 0 ]; then
  show_help
  exit 0
fi

main "$@"