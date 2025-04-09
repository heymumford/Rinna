#!/bin/bash
#
# version-sync.sh - Synchronize versions across all project components
# 
# This script ensures consistent version information across all project components,
# including Maven POM files, Go files, Python files, and more. It serves as
# a robust versioning system to handle the multi-language project.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Constants
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
VERSION_FILE="${PROJECT_ROOT}/version.properties"
BACKUP_DIR="${PROJECT_ROOT}/backup/version-sync-$(date +%Y%m%d-%H%M%S)"
LOG_FILE="${BACKUP_DIR}/version-sync.log"

# Source the XML tools library
source "${PROJECT_ROOT}/bin/xml-tools.sh"

# Create backup directory
mkdir -p "${BACKUP_DIR}"

# Initialize log
init_log() {
  echo "# Rinna Version Synchronization Log" > "${LOG_FILE}"
  echo "# $(date)" >> "${LOG_FILE}"
  echo "# Command: $0 $*" >> "${LOG_FILE}"
  echo "" >> "${LOG_FILE}"
}

# Log function
log() {
  local level="$1"
  local message="$2"
  
  # Log to file
  echo "[$(date +"%Y-%m-%d %H:%M:%S")] [${level}] ${message}" >> "${LOG_FILE}"
  
  # Print to console with appropriate formatting
  case "${level}" in
    INFO)
      echo "INFO: ${message}"
      ;;
    SUCCESS)
      echo "✅ ${message}"
      ;;
    WARNING)
      echo "⚠️  WARNING: ${message}"
      ;;
    ERROR)
      echo "❌ ERROR: ${message}"
      ;;
  esac
}

# Backup a file before modification
backup_file() {
  local file="$1"
  local rel_path="${file#$PROJECT_ROOT/}"
  local backup_path="${BACKUP_DIR}/${rel_path}"
  
  mkdir -p "$(dirname "${backup_path}")"
  cp "${file}" "${backup_path}"
  log "INFO" "Created backup: ${rel_path}"
}

# Read current version from version.properties
get_current_version() {
  if [ ! -f "${VERSION_FILE}" ]; then
    log "ERROR" "Version file not found: ${VERSION_FILE}"
    return 1
  fi
  
  grep -m 1 "^version=" "${VERSION_FILE}" | cut -d'=' -f2
}

# Read current build number from version.properties
get_build_number() {
  if [ ! -f "${VERSION_FILE}" ]; then
    log "ERROR" "Version file not found: ${VERSION_FILE}"
    return 1
  fi
  
  grep -m 1 "^buildNumber=" "${VERSION_FILE}" | cut -d'=' -f2
}

# Parse version into components and export them
parse_version() {
  local version="$1"
  
  if [[ ! "${version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9._-]+)?$ ]]; then
    log "ERROR" "Invalid version format: ${version} (should be x.y.z or x.y.z-qualifier)"
    return 1
  fi
  
  # Parse components
  VERSION_MAJOR=$(echo "${version}" | cut -d. -f1)
  VERSION_MINOR=$(echo "${version}" | cut -d. -f2)
  VERSION_PATCH=$(echo "${version}" | cut -d. -f3 | cut -d- -f1)
  VERSION_QUALIFIER=""
  if [[ "${version}" == *-* ]]; then
    VERSION_QUALIFIER=$(echo "${version}" | cut -d- -f2-)
  fi
  
  export VERSION_MAJOR
  export VERSION_MINOR
  export VERSION_PATCH
  export VERSION_QUALIFIER
  
  return 0
}

# Update Maven POM files
update_maven_files() {
  local version="$1"
  local dry_run="$2"
  
  log "INFO" "Updating Maven POM files to version ${version}..."
  
  # Update parent POM version
  local parent_pom="${PROJECT_ROOT}/pom.xml"
  if [ -f "${parent_pom}" ]; then
    if [ "${dry_run}" != "true" ]; then
      backup_file "${parent_pom}"
      if xml_set_version "${parent_pom}" "${version}"; then
        log "SUCCESS" "Updated parent POM version to ${version}"
      else
        log "ERROR" "Failed to update parent POM version"
      fi
    else
      log "INFO" "[DRY RUN] Would update parent POM version to ${version}"
    fi
  fi
  
  # Update module POMs
  for module in rinna-core rinna-cli rinna-data-sqlite; do
    local module_pom="${PROJECT_ROOT}/${module}/pom.xml"
    if [ -f "${module_pom}" ]; then
      # For dry run, just check and report
      if [ "${dry_run}" = "true" ]; then
        local parent_version=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" \
          -t -v "/pom:project/pom:parent/pom:version" "${module_pom}" 2>/dev/null)
          
        if [ "${parent_version}" != "${version}" ]; then
          log "INFO" "[DRY RUN] Would update ${module}/pom.xml parent reference from ${parent_version} to ${version}"
        fi
        continue
      fi
      
      backup_file "${module_pom}"
      
      # Update the parent reference version
      if xmlstarlet ed -N "pom=http://maven.apache.org/POM/4.0.0" \
          -u "/pom:project/pom:parent/pom:version" -v "${version}" \
          "${module_pom}" > "${module_pom}.tmp"; then
        mv "${module_pom}.tmp" "${module_pom}"
        log "SUCCESS" "Updated parent reference in ${module}/pom.xml"
      else
        rm -f "${module_pom}.tmp"
        log "WARNING" "Failed to update parent reference in ${module}/pom.xml"
      fi
    fi
  done
  
  # Update sample project POMs
  for sample_dir in "${PROJECT_ROOT}"/samples/*/; do
    if [ -d "${sample_dir}" ]; then
      local sample_pom="${sample_dir}/pom.xml"
      if [ -f "${sample_pom}" ]; then
        # For dry run, just check and report
        if [ "${dry_run}" = "true" ]; then
          local parent_version=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" \
            -t -v "/pom:project/pom:parent/pom:version" "${sample_pom}" 2>/dev/null)
            
          if [ "${parent_version}" != "${version}" ]; then
            log "INFO" "[DRY RUN] Would update $(basename "${sample_dir}")/pom.xml parent reference from ${parent_version} to ${version}"
          fi
          continue
        fi
        
        backup_file "${sample_pom}"
        
        # Update the parent reference version if it exists
        if xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t -v "/pom:project/pom:parent" "${sample_pom}" &>/dev/null; then
          if xmlstarlet ed -N "pom=http://maven.apache.org/POM/4.0.0" \
              -u "/pom:project/pom:parent/pom:version" -v "${version}" \
              "${sample_pom}" > "${sample_pom}.tmp"; then
            mv "${sample_pom}.tmp" "${sample_pom}"
            log "SUCCESS" "Updated parent reference in $(basename "${sample_dir}")/pom.xml"
          else
            rm -f "${sample_pom}.tmp"
            log "WARNING" "Failed to update parent reference in $(basename "${sample_dir}")/pom.xml"
          fi
        else
          log "INFO" "No parent reference found in $(basename "${sample_dir}")/pom.xml"
        fi
      fi
    fi
  done
}

# Update Go version files
update_go_files() {
  local version="$1"
  local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  local dry_run="$2"
  
  log "INFO" "Updating Go version files to ${version}..."
  
  # Update API version files
  for version_file in "${PROJECT_ROOT}/api/internal/version/version.go" "${PROJECT_ROOT}/api/pkg/health/version.go" "${PROJECT_ROOT}/version-service/core/version.go"; do
    if [ -f "${version_file}" ]; then
      # For dry run, just check and report
      if [ "${dry_run}" = "true" ]; then
        local go_version=$(grep "Version.*=" "${version_file}" | grep -o '"[0-9.]*"' | tr -d '"')
        if [ "${go_version}" != "${version}" ]; then
          log "INFO" "[DRY RUN] Would update $(basename "${version_file}") from ${go_version} to ${version}"
        fi
        continue
      fi
      
      backup_file "${version_file}"
      
      # Use sed to update version and build time
      sed -i "s/Version\s*=\s*\"[0-9.]\+\"/Version   = \"${version}\"/" "${version_file}"
      sed -i "s/BuildTime\s*=\s*\"[0-9TZ:-]\+\"/BuildTime = \"${timestamp}\"/" "${version_file}"
      
      log "SUCCESS" "Updated $(basename "${version_file}") to version ${version}"
    fi
  done
}

# Update Python version files
update_python_files() {
  local version="$1"
  local dry_run="$2"
  
  log "INFO" "Updating Python version files to ${version}..."
  
  # Update Python package version
  local setup_py="${PROJECT_ROOT}/python/setup.py"
  if [ -f "${setup_py}" ]; then
    # For dry run, just check and report
    if [ "${dry_run}" = "true" ]; then
      local py_version=$(grep -m 1 "version=" "${setup_py}" | cut -d'"' -f2)
      if [ "${py_version}" != "${version}" ]; then
        log "INFO" "[DRY RUN] Would update python/setup.py from ${py_version} to ${version}"
      fi
    else
      backup_file "${setup_py}"
      sed -i "s/version=\"[0-9.]*\"/version=\"${version}\"/" "${setup_py}"
      log "SUCCESS" "Updated python/setup.py to version ${version}"
    fi
  fi
  
  # Update pyproject.toml if it exists
  local pyproject="${PROJECT_ROOT}/pyproject.toml"
  if [ -f "${pyproject}" ]; then
    if [ "${dry_run}" = "true" ]; then
      local toml_version=$(grep "version" "${pyproject}" | head -1 | grep -o '"[0-9.]*"' | tr -d '"')
      if [ "${toml_version}" != "${version}" ]; then
        log "INFO" "[DRY RUN] Would update pyproject.toml from ${toml_version} to ${version}"
      fi
    else
      backup_file "${pyproject}"
      sed -i "s/version\s*=\s*\"[0-9.]*\"/version = \"${version}\"/" "${pyproject}"
      log "SUCCESS" "Updated pyproject.toml to version ${version}"
    fi
  fi
  
  # Update version in Python package __init__.py
  local init_py="${PROJECT_ROOT}/python/rinna/__init__.py"
  if [ -f "${init_py}" ]; then
    if [ "${dry_run}" = "true" ]; then
      local init_version=$(grep -m 1 "__version__" "${init_py}" | cut -d'"' -f2)
      if [ "${init_version}" != "${version}" ]; then
        log "INFO" "[DRY RUN] Would update python/rinna/__init__.py from ${init_version} to ${version}"
      fi
    else
      backup_file "${init_py}"
      sed -i "s/__version__ = \"[0-9.]*\"/__version__ = \"${version}\"/" "${init_py}"
      log "SUCCESS" "Updated python/rinna/__init__.py to version ${version}"
    fi
  fi
}

# Update version in README badge
update_readme_badge() {
  local version="$1"
  local dry_run="$2"
  local readme="${PROJECT_ROOT}/README.md"
  
  if [ -f "${readme}" ]; then
    local badge_line=$(grep -m 1 "badge/version-" "${readme}")
    if [ -n "${badge_line}" ]; then
      local old_version=$(echo "${badge_line}" | grep -o -E "version-[0-9.]*-" | sed -E 's/version-(.*)-.*/\1/')
      
      if [ "${dry_run}" = "true" ]; then
        if [ "${old_version}" != "${version}" ]; then
          log "INFO" "[DRY RUN] Would update README badge from ${old_version} to ${version}"
        fi
      else
        if [ "${old_version}" != "${version}" ]; then
          backup_file "${readme}"
          sed -i "s/version-${old_version}-/version-${version}-/g" "${readme}"
          log "SUCCESS" "Updated README badge to version ${version}"
        else
          log "INFO" "README badge already at version ${version}"
        fi
      fi
    else
      log "WARNING" "Could not find version badge in README.md"
    fi
  fi
}

# Verify version consistency
verify_consistency() {
  local expected_version="$1"
  local inconsistencies=0
  
  log "INFO" "Verifying version consistency..."
  
  # Check POM files
  # First check parent POM
  if [ -f "${PROJECT_ROOT}/pom.xml" ]; then
    local pom_version=$(xml_get_version "${PROJECT_ROOT}/pom.xml")
    if [ "${pom_version}" != "${expected_version}" ]; then
      log "ERROR" "Version mismatch in ${PROJECT_ROOT}/pom.xml: found ${pom_version}, expected ${expected_version}"
      inconsistencies=$((inconsistencies + 1))
    else
      log "SUCCESS" "Verified ${PROJECT_ROOT}/pom.xml: ${pom_version}"
    fi
  fi
  
  # Then check module POMs (only checking parent references, as modules might not have their own version element)
  for module in rinna-core rinna-cli rinna-data-sqlite; do
    local module_pom="${PROJECT_ROOT}/${module}/pom.xml"
    if [ -f "${module_pom}" ]; then
      # Check parent reference
      local parent_version=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" \
        -t -v "/pom:project/pom:parent/pom:version" "${module_pom}" 2>/dev/null)
      if [ "${parent_version}" != "${expected_version}" ]; then
        log "ERROR" "Parent version mismatch in ${module_pom}: found ${parent_version}, expected ${expected_version}"
        inconsistencies=$((inconsistencies + 1))
      else
        log "SUCCESS" "Verified parent reference in ${module_pom}: ${parent_version}"
      fi
    fi
  done
  
  # Check Go version files
  for version_file in "${PROJECT_ROOT}/api/internal/version/version.go" "${PROJECT_ROOT}/api/pkg/health/version.go" "${PROJECT_ROOT}/version-service/core/version.go"; do
    if [ -f "${version_file}" ]; then
      if grep -q 'Version\s*=\s*"[0-9.]\+"' "${version_file}"; then
        local go_version=$(grep -o 'Version\s*=\s*"[0-9.]\+"' "${version_file}" | grep -o '[0-9.]\+')
        if [ "${go_version}" != "${expected_version}" ]; then
          log "ERROR" "Version mismatch in ${version_file}: found ${go_version}, expected ${expected_version}"
          inconsistencies=$((inconsistencies + 1))
        else
          log "SUCCESS" "Verified ${version_file}: ${go_version}"
        fi
      else
        log "WARNING" "No version string found in ${version_file}"
      fi
    else
      log "INFO" "Go version file not found: ${version_file}"
    fi
  done
  
  # Check README badge
  local readme="${PROJECT_ROOT}/README.md"
  if [ -f "${readme}" ]; then
    local badge_line=$(grep -m 1 "badge/version-" "${readme}")
    if [ -n "${badge_line}" ]; then
      local readme_version=$(echo "${badge_line}" | grep -o -E "version-[0-9.]*-" | sed -E 's/version-(.*)-.*/\1/')
      if [ "${readme_version}" != "${expected_version}" ]; then
        log "ERROR" "Version mismatch in README badge: found ${readme_version}, expected ${expected_version}"
        inconsistencies=$((inconsistencies + 1))
      else
        log "SUCCESS" "Verified README badge: ${readme_version}"
      fi
    else
      log "WARNING" "Could not find version badge in README.md"
    fi
  fi
  
  # Check version-service properties file
  local version_service_file="${PROJECT_ROOT}/version-service/version.properties"
  if [ -f "${version_service_file}" ]; then
    local service_version=$(grep -m 1 "^version=" "${version_service_file}" | cut -d'=' -f2)
    if [ "${service_version}" != "${expected_version}" ]; then
      log "ERROR" "Version mismatch in ${version_service_file}: found ${service_version}, expected ${expected_version}"
      inconsistencies=$((inconsistencies + 1))
    else
      log "SUCCESS" "Verified ${version_service_file}: ${service_version}"
    fi
  fi
  
  # Check Python version files
  local setup_py="${PROJECT_ROOT}/python/setup.py"
  if [ -f "${setup_py}" ]; then
    local py_version=$(grep -m 1 "version=" "${setup_py}" | cut -d'"' -f2)
    if [ "${py_version}" != "${expected_version}" ]; then
      log "ERROR" "Version mismatch in ${setup_py}: found ${py_version}, expected ${expected_version}"
      inconsistencies=$((inconsistencies + 1))
    else
      log "SUCCESS" "Verified ${setup_py}: ${py_version}"
    fi
  fi
  
  # Check Python package __init__.py
  local init_py="${PROJECT_ROOT}/python/rinna/__init__.py"
  if [ -f "${init_py}" ]; then
    local init_version=$(grep -m 1 "__version__" "${init_py}" | cut -d'"' -f2)
    if [ "${init_version}" != "${expected_version}" ]; then
      log "ERROR" "Version mismatch in ${init_py}: found ${init_version}, expected ${expected_version}"
      inconsistencies=$((inconsistencies + 1))
    else
      log "SUCCESS" "Verified ${init_py}: ${init_version}"
    fi
  fi
  
  # Check pyproject.toml
  local pyproject="${PROJECT_ROOT}/pyproject.toml"
  if [ -f "${pyproject}" ]; then
    local toml_version=$(grep "version" "${pyproject}" | head -1 | grep -o '"[0-9.]*"' | tr -d '"')
    if [ "${toml_version}" != "${expected_version}" ]; then
      log "ERROR" "Version mismatch in ${pyproject}: found ${toml_version}, expected ${expected_version}"
      inconsistencies=$((inconsistencies + 1))
    else
      log "SUCCESS" "Verified ${pyproject}: ${toml_version}"
    fi
  fi
  
  if [ ${inconsistencies} -eq 0 ]; then
    log "SUCCESS" "All version references are consistent with version.properties (${expected_version})"
    return 0
  else
    log "ERROR" "Found ${inconsistencies} version inconsistencies"
    return 1
  fi
}

# Synchronize versions from version.properties to all project files
synchronize_versions() {
  local version=$(get_current_version)
  local dry_run="$1"
  
  if [ "${dry_run}" = "true" ]; then
    log "INFO" "Dry run mode - no changes will be made"
  else
    log "INFO" "Synchronizing all project files to version ${version}..."
  fi
  
  # Update Maven POM files
  update_maven_files "${version}" "${dry_run}"
  
  # Update Go version files
  update_go_files "${version}" "${dry_run}"
  
  # Update Python version files
  update_python_files "${version}" "${dry_run}"
  
  # Update README badge
  update_readme_badge "${version}" "${dry_run}"
  
  # Copy version.properties to version-service if it exists
  local version_service_file="${PROJECT_ROOT}/version-service/version.properties"
  if [ -f "${version_service_file}" ]; then
    if [ "${dry_run}" = "true" ]; then
      local service_version=$(grep -m 1 "^version=" "${version_service_file}" | cut -d'=' -f2)
      if [ "${service_version}" != "${version}" ]; then
        log "INFO" "[DRY RUN] Would update version-service/version.properties from ${service_version} to ${version}"
      fi
    else
      backup_file "${version_service_file}"
      cp "${VERSION_FILE}" "${version_service_file}"
      log "SUCCESS" "Updated version-service/version.properties to ${version}"
    fi
  fi
}

# Display help message
show_help() {
  cat << EOF
Usage: version-sync.sh [command] [options]

Commands:
  verify              Verify version consistency across all files
  sync                Synchronize all files with version.properties
  help                Show this help message

Options:
  --dry-run           Show what would be done without making changes
  --verbose           Show detailed output

Example commands:
  version-sync.sh verify           # Check if all project files are using the same version
  version-sync.sh sync             # Update all project files to match version.properties
  version-sync.sh sync --dry-run   # Show what files would be updated
EOF
}

# Main function
main() {
  init_log "$@"
  
  local COMMAND=""
  local DRY_RUN=false
  local VERBOSE=false
  
  # Parse arguments
  while [ $# -gt 0 ]; do
    case "$1" in
      verify|sync|help)
        COMMAND="$1"
        shift
        ;;
      --dry-run)
        DRY_RUN=true
        shift
        ;;
      --verbose)
        VERBOSE=true
        shift
        ;;
      *)
        log "ERROR" "Unknown option: $1"
        show_help
        exit 1
        ;;
    esac
  done
  
  # Default command
  if [ -z "$COMMAND" ]; then
    COMMAND="help"
  fi
  
  # Execute command
  case "$COMMAND" in
    verify)
      local version=$(get_current_version)
      verify_consistency "$version"
      ;;
    sync)
      synchronize_versions "$DRY_RUN"
      # Verify after sync unless dry run
      if [ "$DRY_RUN" != "true" ]; then
        local version=$(get_current_version)
        verify_consistency "$version"
      fi
      ;;
    help)
      show_help
      ;;
    *)
      log "ERROR" "Unknown command: $COMMAND"
      show_help
      exit 1
      ;;
  esac
  
  log "INFO" "Operation completed"
  exit 0
}

# Execute main function
main "$@"