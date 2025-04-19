#!/bin/bash
#
# version-manager.sh - Single source of truth version management for Rinna
# 
# This script provides a unified interface for version management across
# all project components using version.properties as the single source of truth.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Constants
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
VERSION_FILE="${PROJECT_ROOT}/version.properties"
BACKUP_DIR="${PROJECT_ROOT}/backup/version-$(date +%Y%m%d-%H%M%S)"
LOG_FILE="${BACKUP_DIR}/version-manager.log"

# Source the XML tools library
source "${SCRIPT_DIR}/xml-tools.sh"

# Create backup directory
mkdir -p "${BACKUP_DIR}"

# Initialize log
init_log() {
  echo "# Rinna Version Manager Log" > "${LOG_FILE}"
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

# Update version.properties
update_version_properties() {
  local version="$1"
  local build_number="$2"
  local current_timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  local today=$(date +%Y-%m-%d)

  # Ensure version is parsed
  parse_version "${version}"

  # Backup the file
  backup_file "${VERSION_FILE}"

  # Update core version information
  sed -i "s/^version=.*/version=${version}/" "${VERSION_FILE}"
  sed -i "s/^version.full=.*/version.full=${version}/" "${VERSION_FILE}"
  sed -i "s/^version.major=.*/version.major=${VERSION_MAJOR}/" "${VERSION_FILE}"
  sed -i "s/^version.minor=.*/version.minor=${VERSION_MINOR}/" "${VERSION_FILE}"
  sed -i "s/^version.patch=.*/version.patch=${VERSION_PATCH}/" "${VERSION_FILE}"
  sed -i "s/^version.qualifier=.*/version.qualifier=${VERSION_QUALIFIER}/" "${VERSION_FILE}"

  # Update release information
  sed -i "s/^lastUpdated=.*/lastUpdated=${today}/" "${VERSION_FILE}"
  sed -i "s/^buildNumber=.*/buildNumber=${build_number}/" "${VERSION_FILE}"

  # Update build information
  sed -i "s/^build.timestamp=.*/build.timestamp=${current_timestamp}/" "${VERSION_FILE}"

  # Copy to version-service if it exists
  local version_service_file="${PROJECT_ROOT}/build/version-service/version.properties"
  if [ -f "${version_service_file}" ]; then
    backup_file "${version_service_file}"
    cp "${VERSION_FILE}" "${version_service_file}"
    log "SUCCESS" "Updated version-service/version.properties to ${version} (build ${build_number})"
  fi

  log "SUCCESS" "Updated version.properties to ${version} (build ${build_number})"
}

# Update POM files using XMLStarlet
update_pom_files() {
  local version="$1"

  # Update parent POM
  local parent_pom="${PROJECT_ROOT}/pom.xml"
  if [ -f "${parent_pom}" ]; then
    backup_file "${parent_pom}"
    if xml_set_version "${parent_pom}" "${version}"; then
      log "SUCCESS" "Updated parent POM version to ${version}"
    else
      log "ERROR" "Failed to update parent POM version"
    fi
  fi

  # Update module POMs
  for module_pom in "${PROJECT_ROOT}"/*/pom.xml; do
    if [ -f "${module_pom}" ]; then
      backup_file "${module_pom}"

      # Update the module's own version if it has one
      if xml_set_version "${module_pom}" "${version}" 2>/dev/null; then
        log "INFO" "Updated module POM version in $(basename $(dirname "${module_pom}"))"
      fi

      # Update the parent reference version
      if xmlstarlet ed -N "pom=http://maven.apache.org/POM/4.0.0" \
          -u "/pom:project/pom:parent/pom:version" -v "${version}" \
          "${module_pom}" > "${module_pom}.tmp"; then
        mv "${module_pom}.tmp" "${module_pom}"
        log "SUCCESS" "Updated parent reference in $(basename $(dirname "${module_pom}"))/pom.xml"
      else
        rm -f "${module_pom}.tmp"
        log "WARNING" "Failed to update parent reference in $(basename $(dirname "${module_pom}"))/pom.xml"
      fi
    fi
  done
}

# Update Go version files
update_go_version_files() {
  local version="$1"
  local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

  # Update API version files
  for version_file in "${PROJECT_ROOT}/api/internal/version/version.go" "${PROJECT_ROOT}/api/pkg/health/version.go" "${PROJECT_ROOT}/build/version-service/core/version.go"; do
    if [ -f "${version_file}" ]; then
      backup_file "${version_file}"

      # Use sed to update version and build time
      sed -i "s/Version\s*=\s*\"[0-9.]\+\"/Version   = \"${version}\"/" "${version_file}"
      sed -i "s/BuildTime\s*=\s*\"[0-9TZ:-]\+\"/BuildTime = \"${timestamp}\"/" "${version_file}"

      log "SUCCESS" "Updated $(basename "${version_file}") to version ${version}"
    fi
  done
}

# Update test scripts that have hardcoded versions
update_test_scripts() {
  local version="$1"

  # Find shell scripts with version references
  local test_scripts=($(grep -l "version.*=.*[0-9]\\.[0-9]\\.[0-9]" "${PROJECT_ROOT}"/bin/*.sh))

  for script in "${test_scripts[@]}"; do
    if [ -f "${script}" ]; then
      backup_file "${script}"

      # Check for temporary pom creation blocks and update versions there
      if grep -q "cat > temp-pom.xml << EOF" "${script}"; then
        sed -i -E "s|<version>[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9._-]+)?</version>|<version>${version}</version>|g" "${script}"
        log "SUCCESS" "Updated version in test script: $(basename "${script}")"
      fi
    fi
  done
}

# Increment build number
increment_build() {
  local current_build=$(get_build_number)
  if [ -z "${current_build}" ]; then
    log "ERROR" "Could not determine current build number"
    return 1
  fi

  local new_build=$((current_build + 1))
  local current_version=$(get_current_version)

  update_version_properties "${current_version}" "${new_build}"
  log "SUCCESS" "Incremented build number from ${current_build} to ${new_build}"

  return 0
}

# Bump major version (x.0.0)
bump_major() {
  local current_version=$(get_current_version)
  local build_number=$(get_build_number)

  parse_version "${current_version}"
  local new_major=$((VERSION_MAJOR + 1))
  local new_version="${new_major}.0.0"

  log "INFO" "Bumping major version: ${current_version} -> ${new_version}"

  # Update all version references
  update_version_properties "${new_version}" "${build_number}"
  update_pom_files "${new_version}"
  update_go_version_files "${new_version}"
  update_test_scripts "${new_version}"

  log "SUCCESS" "Bumped major version to ${new_version}"
  return 0
}

# Bump minor version (x.y+1.0)
bump_minor() {
  local current_version=$(get_current_version)
  local build_number=$(get_build_number)

  parse_version "${current_version}"
  local new_minor=$((VERSION_MINOR + 1))
  local new_version="${VERSION_MAJOR}.${new_minor}.0"

  log "INFO" "Bumping minor version: ${current_version} -> ${new_version}"

  # Update all version references
  update_version_properties "${new_version}" "${build_number}"
  update_pom_files "${new_version}"
  update_go_version_files "${new_version}"
  update_test_scripts "${new_version}"

  log "SUCCESS" "Bumped minor version to ${new_version}"
  return 0
}

# Bump patch version (x.y.z+1)
bump_patch() {
  local current_version=$(get_current_version)
  local build_number=$(get_build_number)

  parse_version "${current_version}"
  local new_patch=$((VERSION_PATCH + 1))
  local new_version="${VERSION_MAJOR}.${VERSION_MINOR}.${new_patch}"

  # Preserve qualifier if it exists
  if [ -n "${VERSION_QUALIFIER}" ]; then
    new_version="${new_version}-${VERSION_QUALIFIER}"
  fi

  log "INFO" "Bumping patch version: ${current_version} -> ${new_version}"

  # Update all version references
  update_version_properties "${new_version}" "${build_number}"
  update_pom_files "${new_version}"
  update_go_version_files "${new_version}"
  update_test_scripts "${new_version}"

  log "SUCCESS" "Bumped patch version to ${new_version}"
  return 0
}

# Set specific version
set_version() {
  local new_version="$1"
  local build_number=$(get_build_number)

  if ! parse_version "${new_version}"; then
    return 1
  fi

  local current_version=$(get_current_version)
  log "INFO" "Setting version: ${current_version} -> ${new_version}"

  # Update all version references
  update_version_properties "${new_version}" "${build_number}"
  update_pom_files "${new_version}"
  update_go_version_files "${new_version}"
  update_test_scripts "${new_version}"

  log "SUCCESS" "Set version to ${new_version}"
  return 0
}

# Set specific build number
set_build() {
  local new_build="$1"
  local current_version=$(get_current_version)

  if [[ ! "${new_build}" =~ ^[0-9]+$ ]]; then
    log "ERROR" "Invalid build number: ${new_build}"
    return 1
  fi

  log "INFO" "Setting build number to ${new_build}"

  # Update all version references
  update_version_properties "${current_version}" "${new_build}"

  log "SUCCESS" "Set build number to ${new_build}"
  return 0
}

# Verify version consistency
verify_consistency() {
  local expected_version=$(get_current_version)
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
  for pom_file in "${PROJECT_ROOT}"/*/pom.xml; do
    if [ -f "${pom_file}" ]; then
      # Check parent reference
      local parent_version=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" \
        -t -v "/pom:project/pom:parent/pom:version" "${pom_file}" 2>/dev/null)
      if [ "${parent_version}" != "${expected_version}" ]; then
        log "ERROR" "Parent version mismatch in ${pom_file}: found ${parent_version}, expected ${expected_version}"
        inconsistencies=$((inconsistencies + 1))
      else
        log "SUCCESS" "Verified parent reference in ${pom_file}: ${parent_version}"
      fi

      # Only check module's own version if it has one (many module POMs inherit version from parent)
      if xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t -v "/pom:project/pom:version" "${pom_file}" &>/dev/null; then
        local pom_version=$(xml_get_version "${pom_file}")
        if [ "${pom_version}" != "${expected_version}" ]; then
          log "ERROR" "Version mismatch in ${pom_file}: found ${pom_version}, expected ${expected_version}"
          inconsistencies=$((inconsistencies + 1))
        else
          log "SUCCESS" "Verified ${pom_file}: ${pom_version}"
        fi
      fi
    fi
  done

  # Check Go version files
  for version_file in "${PROJECT_ROOT}/api/internal/version/version.go" "${PROJECT_ROOT}/api/pkg/health/version.go" "${PROJECT_ROOT}/build/version-service/core/version.go"; do
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

  # Check version-service properties file
  local version_service_file="${PROJECT_ROOT}/build/version-service/version.properties"
  if [ -f "${version_service_file}" ]; then
    local service_version=$(grep -m 1 "^version=" "${version_service_file}" | cut -d'=' -f2)
    if [ "${service_version}" != "${expected_version}" ]; then
      log "ERROR" "Version mismatch in ${version_service_file}: found ${service_version}, expected ${expected_version}"
      inconsistencies=$((inconsistencies + 1))
    else
      log "SUCCESS" "Verified ${version_service_file}: ${service_version}"
    fi
  fi

  if [ ${inconsistencies} -eq 0 ]; then
    log "SUCCESS" "All version references are consistent with version.properties (${expected_version})"
    return 0
  else
    log "ERROR" "Found ${inconsistencies} version inconsistencies. See log for details: ${LOG_FILE}"
    return 1
  fi
}

# Create a git commit with the version changes
commit_changes() {
  local version=$(get_current_version)
  local build_number=$(get_build_number)

  log "INFO" "Creating git commit for version ${version} (build ${build_number})..."

  # Add version files to git
  git add "${VERSION_FILE}" \
         "${PROJECT_ROOT}/build/version-service/version.properties" \
         "${PROJECT_ROOT}/pom.xml" \
         "${PROJECT_ROOT}/"*/pom.xml \
         "${PROJECT_ROOT}/api/internal/version/version.go" \
         "${PROJECT_ROOT}/api/pkg/health/version.go" \
         "${PROJECT_ROOT}/build/version-service/core/version.go" 2>/dev/null

  # Create commit
  if git commit -m "Update version to ${version} (build ${build_number})" --no-verify; then
    log "SUCCESS" "Created commit for version ${version} (build ${build_number})"
    return 0
  else
    log "ERROR" "Failed to create git commit"
    return 1
  fi
}

# Display help message
show_help() {
  cat << EOF
Usage: version-manager.sh [command] [options]

Commands:
  current               Display current version and build number
  verify                Verify version consistency across all files
  bump major            Bump major version (x+1.0.0)
  bump minor            Bump minor version (x.y+1.0)
  bump patch            Bump patch version (x.y.z+1)
  increment-build       Increment build number by 1
  set [version]         Set specific version (e.g., 1.2.3)
  set-build [number]    Set specific build number

Options:
  --no-commit           Don't create a git commit with the changes
  --help                Show this help message

Examples:
  version-manager.sh current
  version-manager.sh bump patch
  version-manager.sh set 1.5.2
  version-manager.sh set-build 123
  version-manager.sh increment-build
  version-manager.sh verify
EOF
}

# Main function
main() {
  init_log "$@"

  # Default option
  local NO_COMMIT=false

  # Parse command
  local COMMAND=""
  local COMMAND_ARG=""

  if [ $# -eq 0 ]; then
    COMMAND="current"
  else
    COMMAND="$1"
    shift
  fi

  # Parse options
  while [ $# -gt 0 ]; do
    case "$1" in
      --no-commit)
        NO_COMMIT=true
        shift
        ;;
      --help)
        show_help
        exit 0
        ;;
      *)
        COMMAND_ARG="$1"
        shift
        ;;
    esac
  done

  # Execute command
  case "${COMMAND}" in
    current)
      local version=$(get_current_version)
      local build=$(get_build_number)
      echo "Current version: ${version} (build ${build})"
      ;;
    verify)
      verify_consistency
      ;;
    bump)
      case "${COMMAND_ARG}" in
        major)
          bump_major
          ;;
        minor)
          bump_minor
          ;;
        patch)
          bump_patch
          ;;
        *)
          log "ERROR" "Invalid bump argument: ${COMMAND_ARG}"
          show_help
          exit 1
          ;;
      esac

      if [ "${NO_COMMIT}" = false ]; then
        commit_changes
      fi
      ;;
    increment-build)
      increment_build

      if [ "${NO_COMMIT}" = false ]; then
        commit_changes
      fi
      ;;
    set)
      if [ -z "${COMMAND_ARG}" ]; then
        log "ERROR" "Version must be specified with 'set' command"
        show_help
        exit 1
      fi

      set_version "${COMMAND_ARG}"

      if [ "${NO_COMMIT}" = false ]; then
        commit_changes
      fi
      ;;
    set-build)
      if [ -z "${COMMAND_ARG}" ]; then
        log "ERROR" "Build number must be specified with 'set-build' command"
        show_help
        exit 1
      fi

      set_build "${COMMAND_ARG}"

      if [ "${NO_COMMIT}" = false ]; then
        commit_changes
      fi
      ;;
    *)
      log "ERROR" "Invalid command: ${COMMAND}"
      show_help
      exit 1
      ;;
  esac

  log "INFO" "Operation completed successfully"
  exit 0
}

# Execute main function
main "$@"
