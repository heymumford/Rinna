#!/bin/bash
# consolidate-modules.sh
# Script to consolidate modules by moving code from rinna-core to src

set -e

# Base directories
RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
CORE_SRC="${RINNA_DIR}/rinna-core/src"
TARGET_SRC="${RINNA_DIR}/src"

# Log file
LOG_FILE="${RINNA_DIR}/bin/migration/consolidate-modules.log"
echo "Starting module consolidation at $(date)" > "${LOG_FILE}"

# Function to safely copy directories
copy_directory() {
    local source_dir=$1
    local target_dir=$2
    
    echo "Copying from ${source_dir} to ${target_dir}" >> "${LOG_FILE}"
    
    # Create target directory if it doesn't exist
    mkdir -p "${target_dir}"
    
    # Copy contents using rsync to preserve permissions and handle conflicts
    rsync -av "${source_dir}/" "${target_dir}/" >> "${LOG_FILE}" 2>&1
}

# Function to update Maven pom.xml
update_pom() {
    echo "Updating Maven pom.xml files..." >> "${LOG_FILE}"
    
    # Copy dependencies from rinna-core/pom.xml to src/pom.xml
    # This is simplified - may need manual adjustment
    if [ -f "${RINNA_DIR}/rinna-core/pom.xml" ] && [ -f "${RINNA_DIR}/src/pom.xml" ]; then
        echo "Dependencies from rinna-core/pom.xml need to be manually merged into src/pom.xml" >> "${LOG_FILE}"
        echo "Please check the following files:" >> "${LOG_FILE}"
        echo "- ${RINNA_DIR}/rinna-core/pom.xml" >> "${LOG_FILE}"
        echo "- ${RINNA_DIR}/src/pom.xml" >> "${LOG_FILE}"
    fi
    
    # Update parent pom.xml to remove rinna-core module
    if [ -f "${RINNA_DIR}/pom.xml" ]; then
        # Backup original pom
        cp "${RINNA_DIR}/pom.xml" "${RINNA_DIR}/pom.xml.backup"
        
        # Remove rinna-core module reference
        # Note: This is a simplified approach - may need manual adjustment
        sed -i '/<module>rinna-core<\/module>/d' "${RINNA_DIR}/pom.xml"
        
        echo "Updated ${RINNA_DIR}/pom.xml - backup saved at ${RINNA_DIR}/pom.xml.backup" >> "${LOG_FILE}"
    fi
}

# 1. Copy main source code
copy_directory "${CORE_SRC}/main" "${TARGET_SRC}/main"

# 2. Copy test code
copy_directory "${CORE_SRC}/test" "${TARGET_SRC}/test"

# 3. Copy resources
if [ -d "${CORE_SRC}/main/resources" ]; then
    copy_directory "${CORE_SRC}/main/resources" "${TARGET_SRC}/main/resources"
fi

if [ -d "${CORE_SRC}/test/resources" ]; then
    copy_directory "${CORE_SRC}/test/resources" "${TARGET_SRC}/test/resources"
fi

# 4. Update Maven pom.xml files
update_pom

echo "Module consolidation completed at $(date)" >> "${LOG_FILE}"
echo "Next step: Verify build and tests pass after consolidation"

echo "IMPORTANT MANUAL STEPS:" | tee -a "${LOG_FILE}"
echo "1. Resolve any conflicts in pom.xml files" | tee -a "${LOG_FILE}"
echo "2. Run 'mvn clean install' to verify the build" | tee -a "${LOG_FILE}"
echo "3. Run all tests to ensure functionality is preserved" | tee -a "${LOG_FILE}"
echo "4. After verification, the rinna-core directory can be removed" | tee -a "${LOG_FILE}"

echo "Module consolidation completed. See ${LOG_FILE} for details."