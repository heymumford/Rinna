#!/bin/bash
# standardize-script-names.sh
# Script to standardize shell script naming conventions to kebab-case

set -e

# Base directory
RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"

# Log file
LOG_FILE="${RINNA_DIR}/bin/migration/standardize-script-names.log"
echo "Starting shell script naming standardization at $(date)" > "${LOG_FILE}"

# Function to rename shell scripts to kebab-case
rename_to_kebab_case() {
    local dir=$1
    
    echo "Standardizing script names in ${dir}" >> "${LOG_FILE}"
    
    # Find all shell scripts in the directory
    find "${dir}" -name "*.sh" | while read -r script; do
        filename=$(basename "${script}")
        directory=$(dirname "${script}")
        
        # Convert to kebab-case
        # 1. Convert camelCase to kebab-case
        # 2. Convert snake_case to kebab-case
        # 3. Replace multiple hyphens with a single hyphen
        new_filename=$(echo "${filename}" | sed -E 's/([a-z0-9])([A-Z])/\1-\L\2/g' | tr '_' '-' | sed -E 's/-+/-/g')
        
        # Skip if filename already follows convention
        if [ "${filename}" = "${new_filename}" ]; then
            echo "File ${filename} already follows naming convention" >> "${LOG_FILE}"
            continue
        fi
        
        # Rename script
        new_script="${directory}/${new_filename}"
        echo "Renaming ${script} to ${new_script}" >> "${LOG_FILE}"
        
        # Check if target file already exists
        if [ -f "${new_script}" ]; then
            echo "WARNING: Target file ${new_script} already exists, skipping rename" >> "${LOG_FILE}"
            continue
        fi
        
        # Perform the rename
        git mv "${script}" "${new_script}" || mv "${script}" "${new_script}"
    done
}

# Function to update references to renamed scripts
update_script_references() {
    echo "Updating references to renamed scripts..." >> "${LOG_FILE}"
    
    # Find all shell scripts and potential reference files
    find "${RINNA_DIR}" -type f \( -name "*.sh" -o -name "*.md" -o -name "*.java" -o -name "*.go" -o -name "Makefile" \) | while read -r file; do
        # Backup the file first
        cp "${file}" "${file}.bak"
        
        # Update references to renamed scripts
        # Note: This is a simplified approach that would need to be tailored to actual renamed scripts
        updated=false
        
        # Example replacements - would need to be customized based on actual renames
        if sed -i 's/runTests\.sh/run-tests.sh/g' "${file}"; then updated=true; fi
        if sed -i 's/rinnaTelemetry\.sh/rinna-telemetry.sh/g' "${file}"; then updated=true; fi
        if sed -i 's/startJavaServer\.sh/start-java-server.sh/g' "${file}"; then updated=true; fi
        
        # If no changes were made, restore the backup
        if [ "$updated" = false ]; then
            mv "${file}.bak" "${file}"
        else
            echo "Updated references in ${file}" >> "${LOG_FILE}"
            rm "${file}.bak"
        fi
    done
}

# Standardize script names in key directories
rename_to_kebab_case "${RINNA_DIR}/bin"
rename_to_kebab_case "${RINNA_DIR}/api/bin"
rename_to_kebab_case "${RINNA_DIR}/utils"

# Update references to renamed scripts
update_script_references

echo "Shell script naming standardization completed at $(date)" >> "${LOG_FILE}"
echo "Next step: Verify that all script references have been updated"

echo "IMPORTANT MANUAL STEPS:" | tee -a "${LOG_FILE}"
echo "1. Check documentation for references to renamed scripts" | tee -a "${LOG_FILE}"
echo "2. Update any CI/CD pipelines or automation that may use these scripts" | tee -a "${LOG_FILE}"
echo "3. Verify that all scripts still work as expected" | tee -a "${LOG_FILE}"

echo "Shell script naming standardization completed. See ${LOG_FILE} for details."