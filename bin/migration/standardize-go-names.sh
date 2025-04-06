#!/bin/bash
# standardize-go-names.sh
# Script to standardize Go file naming conventions

set -e

# Base directory
RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
API_DIR="${RINNA_DIR}/api"

# Log file
LOG_FILE="${RINNA_DIR}/bin/migration/standardize-go-names.log"
echo "Starting Go file naming standardization at $(date)" > "${LOG_FILE}"

# Function to rename files according to snake_case convention
rename_to_snake_case() {
    local dir=$1
    
    echo "Standardizing file names in ${dir}" >> "${LOG_FILE}"
    
    # Find all Go files in the directory
    find "${dir}" -name "*.go" | while read -r file; do
        filename=$(basename "${file}")
        directory=$(dirname "${file}")
        
        # Convert to snake_case if not already
        # This is a simplified approach - may need manual adjustment for some filenames
        new_filename=$(echo "${filename}" | sed -E 's/([a-z0-9])([A-Z])/\1_\L\2/g' | tr '[:upper:]' '[:lower:]')
        
        # Skip if filename already follows convention
        if [ "${filename}" = "${new_filename}" ]; then
            echo "File ${filename} already follows naming convention" >> "${LOG_FILE}"
            continue
        fi
        
        # Rename file
        new_file="${directory}/${new_filename}"
        echo "Renaming ${file} to ${new_file}" >> "${LOG_FILE}"
        
        # Check if target file already exists
        if [ -f "${new_file}" ]; then
            echo "WARNING: Target file ${new_file} already exists, skipping rename" >> "${LOG_FILE}"
            continue
        fi
        
        # Perform the rename
        git mv "${file}" "${new_file}" || mv "${file}" "${new_file}"
    done
}

# Function to standardize test file naming
standardize_test_naming() {
    local dir=$1
    
    echo "Standardizing test file naming in ${dir}" >> "${LOG_FILE}"
    
    # Find all Go test files
    find "${dir}" -name "*test*.go" | while read -r file; do
        filename=$(basename "${file}")
        directory=$(dirname "${file}")
        
        # Ensure test files use _test.go suffix pattern
        if [[ "${filename}" == test_* && "${filename}" != *_test.go ]]; then
            # Convert test_foo.go to foo_test.go
            new_filename=$(echo "${filename}" | sed 's/test_\(.*\)\.go/\1_test.go/')
            new_file="${directory}/${new_filename}"
            
            echo "Renaming ${file} to ${new_file}" >> "${LOG_FILE}"
            
            # Check if target file already exists
            if [ -f "${new_file}" ]; then
                echo "WARNING: Target file ${new_file} already exists, skipping rename" >> "${LOG_FILE}"
                continue
            fi
            
            # Perform the rename
            git mv "${file}" "${new_file}" || mv "${file}" "${new_file}"
        fi
    done
}

# Standardize file names in Go code directories
rename_to_snake_case "${API_DIR}/cmd"
rename_to_snake_case "${API_DIR}/internal"
rename_to_snake_case "${API_DIR}/pkg"

# Standardize test file naming
standardize_test_naming "${API_DIR}"

echo "Go file naming standardization completed at $(date)" >> "${LOG_FILE}"
echo "Next step: Verify Go build and tests pass after renaming"

echo "IMPORTANT MANUAL STEPS:" | tee -a "${LOG_FILE}"
echo "1. Update import paths in Go files to reflect renamed files" | tee -a "${LOG_FILE}"
echo "2. Run 'go build ./...' to verify the build" | tee -a "${LOG_FILE}"
echo "3. Run 'go test ./...' to verify all tests pass" | tee -a "${LOG_FILE}"

echo "Go file naming standardization completed. See ${LOG_FILE} for details."