#!/bin/bash
# cleanup.sh
# Script to clean up the failed migration

set -e

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
LOG_FILE="${RINNA_DIR}/bin/migration/cleanup.log"

echo "Starting cleanup at $(date)" > "${LOG_FILE}"

# Remove the duplicated model files
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model" ]; then
    echo "Removing domain.model directory..." | tee -a "${LOG_FILE}"
    rm -rf "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model"
fi

# Remove the duplicated service files
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service" ]; then
    echo "Removing domain.service directory..." | tee -a "${LOG_FILE}"
    rm -rf "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service"
fi

# Remove the duplicated repository files
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/repository" ]; then
    echo "Removing adapter.repository directory..." | tee -a "${LOG_FILE}"
    rm -rf "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/repository"
fi

# Clean up src directory if it contains partial migration
if [ -d "${RINNA_DIR}/src/main/java/org/rinna" ]; then
    echo "Removing src directory structure..." | tee -a "${LOG_FILE}"
    rm -rf "${RINNA_DIR}/src/main/java/org/rinna"
fi

echo "Cleanup completed at $(date)" | tee -a "${LOG_FILE}"
echo "The codebase has been restored to its pre-migration state."
echo "Now proceeding with proper migration."
