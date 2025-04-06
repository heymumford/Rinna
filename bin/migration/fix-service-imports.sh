#!/bin/bash
# fix-service-imports.sh
# Script to fix imports in service files directly

set -e

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
LOG_FILE="${RINNA_DIR}/bin/migration/fix-service-imports.log"

echo "Starting service import fixes at $(date)" > "${LOG_FILE}"

# Create a more targeted fix for service implementations
fix_service_file() {
    local file=$1
    local class_name=$(basename "${file}" .java)
    
    echo "Fixing imports in ${file}" | tee -a "${LOG_FILE}"
    
    # First replace the package declaration if needed
    if [[ "${file}" == *"/service/"* && ! "${file}" == *"/domain/service/"* ]]; then
        # Check if it's in org.rinna.service package
        if grep -q "package org.rinna.service;" "${file}"; then
            sed -i 's/package org.rinna.service;/package org.rinna.adapter.service;/g' "${file}"
            echo "  - Updated package declaration" | tee -a "${LOG_FILE}"
        fi
    fi
    
    # Then fix imports for domain classes
    domain_classes=("WorkItem" "WorkItemType" "WorkQueue" "WorkflowState" "Priority" "Project" "Release" "WorkItemCreateRequest" "WorkItemMetadata" "DefaultWorkItem" "DefaultProject" "DefaultRelease" "DefaultWorkQueue" "DocumentConfig")
    
    for class in "${domain_classes[@]}"; do
        if grep -q "import org.rinna.domain.${class};" "${file}"; then
            sed -i "s/import org.rinna.domain.${class};/import org.rinna.domain.model.${class};/g" "${file}"
            echo "  - Fixed import for ${class}" | tee -a "${LOG_FILE}"
        fi
    done
    
    # Fix imports for service interfaces
    service_classes=("ItemService" "WorkflowService" "ReleaseService" "QueueService" "DocumentService" "InvalidTransitionException")
    
    for class in "${service_classes[@]}"; do
        if grep -q "import org.rinna.domain.${class};" "${file}"; then
            sed -i "s/import org.rinna.domain.${class};/import org.rinna.domain.service.${class};/g" "${file}"
            echo "  - Fixed domain import for ${class}" | tee -a "${LOG_FILE}"
        fi
        
        if grep -q "import org.rinna.usecase.${class};" "${file}"; then
            sed -i "s/import org.rinna.usecase.${class};/import org.rinna.domain.service.${class};/g" "${file}"
            echo "  - Fixed usecase import for ${class}" | tee -a "${LOG_FILE}"
        fi
    done
}

# Process all service files in rinna-core
echo "Processing service files in rinna-core..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/rinna-core/src/main/java" -path "*/service/*Service.java" | while read -r file; do
    fix_service_file "${file}"
done

# Process all service files in src
echo "Processing service files in src..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/src/main/java" -path "*/service/*Service.java" | while read -r file; do
    fix_service_file "${file}"
done

echo "Service import fixes completed at $(date)" | tee -a "${LOG_FILE}"
echo "Service imports have been fixed."