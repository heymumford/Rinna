#!/bin/bash
# run-migration.sh
# Master script to run the package structure optimization and naming standardization

set -e

# Base directory
RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
MIGRATION_DIR="${RINNA_DIR}/bin/migration"

# Log file
LOG_FILE="${MIGRATION_DIR}/migration.log"
echo "Starting migration process at $(date)" > "${LOG_FILE}"

# Make all migration scripts executable
chmod +x "${MIGRATION_DIR}"/*.sh

# Function to run a migration step
run_step() {
    local step_name=$1
    local script_name=$2
    local verify_command=$3
    
    echo "==============================================" | tee -a "${LOG_FILE}"
    echo "Starting step: ${step_name}" | tee -a "${LOG_FILE}"
    echo "Running script: ${script_name}" | tee -a "${LOG_FILE}"
    echo "==============================================" | tee -a "${LOG_FILE}"
    
    # Run the migration script
    "${MIGRATION_DIR}/${script_name}" | tee -a "${LOG_FILE}"
    
    # Verify the step if a verify command was provided
    if [ -n "${verify_command}" ]; then
        echo "Verifying step..." | tee -a "${LOG_FILE}"
        eval "${verify_command}" | tee -a "${LOG_FILE}"
    fi
    
    # Prompt for continuation
    read -p "Step ${step_name} completed. Continue to next step? (y/n): " response
    if [[ "${response}" != "y" ]]; then
        echo "Migration paused after step: ${step_name}" | tee -a "${LOG_FILE}"
        exit 0
    fi
}

# Display introduction
echo "Rinna Project Migration Tool"
echo "This script will optimize the package structure and standardize naming conventions."
echo "The process will be performed in steps, with verification after each step."
echo "You can cancel the migration at any point by answering 'n' when prompted."
echo ""
echo "IMPORTANT: This script modifies your codebase. Make sure you have a backup or clean git state."
read -p "Are you ready to proceed? (y/n): " start_response
if [[ "${start_response}" != "y" ]]; then
    echo "Migration cancelled."
    exit 0
fi

# Step 1: Java package optimization
run_step "Java Package Optimization" "flatten-java-packages.sh" "find ${RINNA_DIR}/rinna-core/src -name \"*.java\" | wc -l"

# Step 2: Update Java imports
run_step "Java Import Updates" "update-imports.sh" ""

# Step 3: Module consolidation
run_step "Module Consolidation" "consolidate-modules.sh" "find ${RINNA_DIR}/src/main -name \"*.java\" | wc -l"

# Step 4: Go file naming standardization
run_step "Go File Naming" "standardize-go-names.sh" "find ${RINNA_DIR}/api -name \"*.go\" | sort"

# Step 5: Shell script naming standardization
run_step "Shell Script Naming" "standardize-script-names.sh" "find ${RINNA_DIR} -name \"*.sh\" | sort"

# Final verification
echo "==============================================" | tee -a "${LOG_FILE}"
echo "Running final verification..." | tee -a "${LOG_FILE}"
echo "==============================================" | tee -a "${LOG_FILE}"

# Build verification
echo "Verifying Java build..." | tee -a "${LOG_FILE}"
cd "${RINNA_DIR}" && mvn compile | tee -a "${LOG_FILE}"

echo "Verifying Go build..." | tee -a "${LOG_FILE}"
cd "${RINNA_DIR}/api" && go build ./... | tee -a "${LOG_FILE}"

echo "==============================================" | tee -a "${LOG_FILE}"
echo "Migration process completed at $(date)" | tee -a "${LOG_FILE}"
echo "==============================================" | tee -a "${LOG_FILE}"

echo "IMPORTANT MANUAL STEPS:" | tee -a "${LOG_FILE}"
echo "1. Run tests to ensure functionality is preserved:" | tee -a "${LOG_FILE}"
echo "   - Java: mvn test" | tee -a "${LOG_FILE}"
echo "   - Go: go test ./..." | tee -a "${LOG_FILE}"
echo "2. Update documentation to reflect the new structure" | tee -a "${LOG_FILE}"
echo "3. Remove the rinna-core directory if all tests pass" | tee -a "${LOG_FILE}"
echo "4. Update CI/CD pipelines to reflect the new structure" | tee -a "${LOG_FILE}"

echo "Migration completed. See ${LOG_FILE} for details."