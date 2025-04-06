#!/bin/bash
# update-imports.sh
# Script to update import statements after Java package optimization

set -e

# Base directory
RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"

# Log file
LOG_FILE="${RINNA_DIR}/bin/migration/update-imports.log"
echo "Starting import updates at $(date)" > "${LOG_FILE}"

# Package mappings (old to new)
declare -A package_mappings
package_mappings["org.rinna.domain.entity"]="org.rinna.domain.model"
package_mappings["org.rinna.domain.usecase"]="org.rinna.domain.service"
package_mappings["org.rinna.adapter.persistence"]="org.rinna.adapter.repository"

# Function to update imports in all Java files
update_imports() {
    echo "Updating imports in all Java files..." >> "${LOG_FILE}"
    
    # Find all Java files in the project
    find "${RINNA_DIR}" -name "*.java" | while read -r file; do
        echo "Processing ${file}" >> "${LOG_FILE}"
        
        # Apply package mappings to import statements
        for old_pkg in "${!package_mappings[@]}"; do
            new_pkg="${package_mappings[$old_pkg]}"
            sed -i "s/import ${old_pkg}\./import ${new_pkg}./g" "${file}"
        done
        
        # Check if file uses classes from old packages
        for old_pkg in "${!package_mappings[@]}"; do
            if grep -q "import ${old_pkg}\." "${file}"; then
                echo "WARNING: File ${file} still has imports from ${old_pkg}" >> "${LOG_FILE}"
            fi
        done
    done
}

# Update imports in all Java files
update_imports

echo "Import updates completed at $(date)" >> "${LOG_FILE}"
echo "Next step: Run consolidate-modules.sh to move code from rinna-core to src"

echo "Import updates completed. See ${LOG_FILE} for details."
echo "NOTE: This only updates simple import statements. More complex usages may need manual updating."