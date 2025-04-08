#!/bin/bash

# Update version in POM files using XMLStarlet
# This script updates the version in all POM files consistently

set -e  # Exit on error

# Get the project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${PROJECT_ROOT}"

# Source the XML tools library
source "${PROJECT_ROOT}/bin/xml-tools.sh"

# Parse command line arguments
if [ $# -lt 1 ]; then
    echo "Usage: $0 <new-version>"
    echo "Example: $0 1.5.2"
    exit 1
fi

NEW_VERSION="$1"

# Display version information
CURRENT_VERSION=$(xml_get_version "${PROJECT_ROOT}/pom.xml")
echo "Updating version: ${CURRENT_VERSION} → ${NEW_VERSION}"

# Create backup directory
BACKUP_DIR="${PROJECT_ROOT}/backup/version-update-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${BACKUP_DIR}"

# Function to update version in a POM file
update_pom_version() {
    local pom_file="$1"
    local pom_name="$2"
    
    if [ ! -f "${pom_file}" ]; then
        echo "Warning: ${pom_file} not found. Skipping."
        return 0
    fi
    
    # Create backup
    cp "${pom_file}" "${BACKUP_DIR}/$(basename ${pom_file})"
    echo "Created backup: ${BACKUP_DIR}/$(basename ${pom_file})"
    
    # Update version using xml_set_version function
    if xml_set_version "${pom_file}" "${NEW_VERSION}"; then
        echo "Updated ${pom_name} version to ${NEW_VERSION}"
    else
        echo "Error: Failed to update version in ${pom_name}"
        return 1
    fi
    
    # Also update the parent version if it's a child module
    if [ "${pom_file}" != "${PROJECT_ROOT}/pom.xml" ]; then
        # Use XMLStarlet to update parent version
        if xmlstarlet ed -N "pom=http://maven.apache.org/POM/4.0.0" \
            -u "/pom:project/pom:parent/pom:version" -v "${NEW_VERSION}" \
            "${pom_file}" > "${pom_file}.tmp"; then
            mv "${pom_file}.tmp" "${pom_file}"
            echo "Updated ${pom_name} parent version to ${NEW_VERSION}"
        else
            rm -f "${pom_file}.tmp"
            echo "Note: No parent version found in ${pom_name} or update failed"
        fi
    fi
    
    return 0
}

# Update version in properties file
update_properties_version() {
    local properties_file="${PROJECT_ROOT}/version.properties"
    
    if [ ! -f "${properties_file}" ]; then
        echo "Warning: ${properties_file} not found. Skipping."
        return 0
    fi
    
    # Create backup
    cp "${properties_file}" "${BACKUP_DIR}/$(basename ${properties_file})"
    echo "Created backup: ${BACKUP_DIR}/$(basename ${properties_file})"
    
    # Parse version components
    local VERSION_MAJOR=$(echo "${NEW_VERSION}" | cut -d. -f1)
    local VERSION_MINOR=$(echo "${NEW_VERSION}" | cut -d. -f2)
    local VERSION_PATCH=$(echo "${NEW_VERSION}" | cut -d. -f3 | cut -d- -f1)
    local VERSION_QUALIFIER=""
    if [[ "${NEW_VERSION}" == *-* ]]; then
        VERSION_QUALIFIER=$(echo "${NEW_VERSION}" | cut -d- -f2-)
    fi
    
    # Update version properties
    sed -i "s/^version=.*/version=${NEW_VERSION}/" "${properties_file}"
    sed -i "s/^version.full=.*/version.full=${NEW_VERSION}/" "${properties_file}"
    sed -i "s/^version.major=.*/version.major=${VERSION_MAJOR}/" "${properties_file}"
    sed -i "s/^version.minor=.*/version.minor=${VERSION_MINOR}/" "${properties_file}"
    sed -i "s/^version.patch=.*/version.patch=${VERSION_PATCH}/" "${properties_file}"
    sed -i "s/^version.qualifier=.*/version.qualifier=${VERSION_QUALIFIER}/" "${properties_file}"
    sed -i "s/^lastUpdated=.*/lastUpdated=$(date +'%Y-%m-%d')/" "${properties_file}"
    sed -i "s/^build.timestamp=.*/build.timestamp=$(date +'%Y-%m-%dT%H:%M:%SZ')/" "${properties_file}"
    
    echo "Updated version properties to ${NEW_VERSION}"
    return 0
}

# Update versions in all POM files
echo "Updating versions in POM files..."
update_pom_version "${PROJECT_ROOT}/pom.xml" "parent POM"
update_pom_version "${PROJECT_ROOT}/rinna-core/pom.xml" "rinna-core POM"
update_pom_version "${PROJECT_ROOT}/rinna-cli/pom.xml" "rinna-cli POM"
update_pom_version "${PROJECT_ROOT}/rinna-data-sqlite/pom.xml" "rinna-data-sqlite POM"

# Update version.properties
echo "Updating version.properties..."
update_properties_version

echo "Version update completed successfully!"
echo "New version: ${NEW_VERSION}"
echo "Backup directory: ${BACKUP_DIR}"

# Add version files to git
echo "Adding version files to git..."
git add "${PROJECT_ROOT}/version.properties" "${PROJECT_ROOT}/pom.xml" "${PROJECT_ROOT}/"*/pom.xml