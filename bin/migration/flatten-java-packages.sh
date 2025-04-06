#!/bin/bash
# flatten-java-packages.sh
# Script to optimize Java package structure while maintaining clean architecture separation

set -e

# Base directories
RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
MAIN_SRC="${RINNA_DIR}/rinna-core/src/main/java/org/rinna"
TEST_SRC="${RINNA_DIR}/rinna-core/src/test/java/org/rinna"

# Target directories (same structure for now, will consolidate modules later)
MAIN_TARGET="${RINNA_DIR}/rinna-core/src/main/java/org/rinna"
TEST_TARGET="${RINNA_DIR}/rinna-core/src/test/java/org/rinna"

# Temporary directory for processing files
TEMP_DIR="${RINNA_DIR}/bin/migration/temp"
mkdir -p "${TEMP_DIR}"

# Log file
LOG_FILE="${RINNA_DIR}/bin/migration/java-package-optimization.log"
echo "Starting Java package optimization at $(date)" > "${LOG_FILE}"

# Package mapping (old to new)
declare -A package_mappings
package_mappings["org.rinna.domain.entity"]="org.rinna.domain.model"
package_mappings["org.rinna.domain.usecase"]="org.rinna.domain.service"
package_mappings["org.rinna.adapter.persistence"]="org.rinna.adapter.repository"
# repository and adapter.service stay the same

# Function to migrate files from one package to another
migrate_package() {
    local source_dir=$1
    local target_dir=$2
    local old_package=$3
    local new_package=$4
    
    echo "Migrating from ${old_package} to ${new_package}" >> "${LOG_FILE}"
    
    # Create target directory if it doesn't exist
    mkdir -p "${target_dir}"
    
    # Find all Java files in the source directory
    find "${source_dir}" -name "*.java" | while read -r file; do
        filename=$(basename "${file}")
        echo "Processing ${filename}" >> "${LOG_FILE}"
        
        # Copy file to temp location for processing
        cp "${file}" "${TEMP_DIR}/${filename}"
        
        # Update package declaration
        sed -i "s/package ${old_package};/package ${new_package};/" "${TEMP_DIR}/${filename}"
        
        # Move to target directory
        mv "${TEMP_DIR}/${filename}" "${target_dir}/"
        
        echo "Moved ${filename} to ${target_dir}" >> "${LOG_FILE}"
    done
}

# 1. Migrate domain entities to domain.model
if [ -d "${MAIN_SRC}/domain/entity" ]; then
    mkdir -p "${MAIN_TARGET}/domain/model"
    migrate_package "${MAIN_SRC}/domain/entity" "${MAIN_TARGET}/domain/model" "org.rinna.domain.entity" "org.rinna.domain.model"
fi

# 2. Migrate domain usecases to domain.service
if [ -d "${MAIN_SRC}/domain/usecase" ]; then
    mkdir -p "${MAIN_TARGET}/domain/service"
    migrate_package "${MAIN_SRC}/domain/usecase" "${MAIN_TARGET}/domain/service" "org.rinna.domain.usecase" "org.rinna.domain.service"
fi

# 3. Migrate adapter.persistence to adapter.repository
if [ -d "${MAIN_SRC}/adapter/persistence" ]; then
    mkdir -p "${MAIN_TARGET}/adapter/repository"
    migrate_package "${MAIN_SRC}/adapter/persistence" "${MAIN_TARGET}/adapter/repository" "org.rinna.adapter.persistence" "org.rinna.adapter.repository"
fi

# Repository interfaces and service implementations stay in their current packages
echo "Note: Repository interfaces remain in org.rinna.domain.repository" >> "${LOG_FILE}"
echo "Note: Service implementations remain in org.rinna.adapter.service" >> "${LOG_FILE}"

echo "Package optimization completed at $(date)" >> "${LOG_FILE}"
echo "Next step: Run update-imports.sh to fix import statements"

# Cleanup
rm -rf "${TEMP_DIR}"

echo "Java package optimization completed. See ${LOG_FILE} for details."