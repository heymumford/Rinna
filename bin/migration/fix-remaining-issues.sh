#!/bin/bash
# fix-remaining-issues.sh
# Script to fix the remaining issues with the package structure

set -e

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
LOG_FILE="${RINNA_DIR}/bin/migration/fix-remaining-issues.log"

echo "Starting fix for remaining issues at $(date)" | tee "${LOG_FILE}"

# Fix ApiHealthServer issue
echo "Fixing ApiHealthServer import in Rinna.java..." | tee -a "${LOG_FILE}"
if [ -f "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/Rinna.java" ]; then
    # Update import for ApiHealthServer
    sed -i 's/import org.rinna.service.ApiHealthServer;/import org.rinna.adapter.service.ApiHealthServer;/g' \
        "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/Rinna.java"
    
    # Copy ApiHealthServer from service to adapter.service if it exists
    if [ -f "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/service/ApiHealthServer.java" ]; then
        echo "Moving ApiHealthServer.java to adapter.service..." | tee -a "${LOG_FILE}"
        mkdir -p "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/service"
        cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/service/ApiHealthServer.java" \
           "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/service/"
        
        # Update package declaration
        sed -i 's/package org.rinna.service;/package org.rinna.adapter.service;/g' \
            "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/service/ApiHealthServer.java"
    fi
fi

# Fix imports that still use org.rinna.domain.WorkItemCreateRequest
echo "Fixing WorkItemCreateRequest imports in repository files..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/rinna-core/src/main/java/org/rinna" -name "*.java" -exec \
    sed -i 's/org.rinna.domain.WorkItemCreateRequest/org.rinna.domain.model.WorkItemCreateRequest/g' {} \;

# Fix Rinna.java imports that need to be adapted to new package structure
echo "Fixing all imports in Rinna.java..." | tee -a "${LOG_FILE}"
if [ -f "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/Rinna.java" ]; then
    sed -i 's/import org.rinna.domain.usecase/import org.rinna.domain.service/g; 
            s/import org.rinna.domain.entity/import org.rinna.domain.model/g;
            s/import org.rinna.adapter.persistence/import org.rinna.adapter.repository/g;
            s/import org.rinna.persistence/import org.rinna.adapter.repository/g;
            s/import org.rinna.service.impl/import org.rinna.adapter.service/g' \
        "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/Rinna.java"
fi

# Copy fixes to src directory
echo "Copying fixes to src directory..." | tee -a "${LOG_FILE}"
if [ -f "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/service/ApiHealthServer.java" ]; then
    mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/adapter/service"
    cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/service/ApiHealthServer.java" \
       "${RINNA_DIR}/src/main/java/org/rinna/adapter/service/"
fi

if [ -f "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/Rinna.java" ]; then
    cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/Rinna.java" \
       "${RINNA_DIR}/src/main/java/org/rinna/"
fi

# Update imports in repository files
echo "Fixing imports in repository files..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/src/main/java/org/rinna" -name "*.java" -exec \
    sed -i 's/org.rinna.domain.WorkItemCreateRequest/org.rinna.domain.model.WorkItemCreateRequest/g' {} \;

echo "Fix for remaining issues completed at $(date)" | tee -a "${LOG_FILE}"
echo "Try building with: cd ${RINNA_DIR}/rinna-core && mvn compile"