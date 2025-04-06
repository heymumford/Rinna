#!/bin/bash
# update-tests.sh
# Script to update test files to use the new package structure

set -e

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
LOG_FILE="${RINNA_DIR}/bin/migration/test-updates.log"

echo "Starting test updates at $(date)" > "${LOG_FILE}"

# Find all test files and update imports
echo "Updating test imports..." | tee -a "${LOG_FILE}"

find "${RINNA_DIR}/src/test" -name "*.java" | while read -r file; do
    echo "Updating imports in ${file}" | tee -a "${LOG_FILE}"
    
    # Update entity imports to model
    sed -i 's/import org.rinna.domain.entity\./import org.rinna.domain.model./g' "${file}"
    
    # Update usecase imports to service
    sed -i 's/import org.rinna.domain.usecase\./import org.rinna.domain.service./g' "${file}"
    
    # Update persistence imports to repository
    sed -i 's/import org.rinna.adapter.persistence\./import org.rinna.adapter.repository./g' "${file}"
    
    # Fix direct imports for major domain classes
    domain_classes=("WorkItem" "WorkItemType" "WorkQueue" "WorkflowState" "Priority" "Project" "Release" "WorkItemCreateRequest" "WorkItemMetadata")
    for class in "${domain_classes[@]}"; do
        sed -i "s/import org.rinna.domain.${class};/import org.rinna.domain.model.${class};/g" "${file}"
    done
    
    # Fix direct imports for service classes
    service_classes=("ItemService" "WorkflowService" "ReleaseService" "QueueService" "InvalidTransitionException")
    for class in "${service_classes[@]}"; do
        sed -i "s/import org.rinna.domain.${class};/import org.rinna.domain.service.${class};/g" "${file}"
        sed -i "s/import org.rinna.usecase.${class};/import org.rinna.domain.service.${class};/g" "${file}"
    done
done

# Update imports for rinna-core test files too
echo "Updating rinna-core test imports..." | tee -a "${LOG_FILE}"

find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" | while read -r file; do
    echo "Updating imports in ${file}" | tee -a "${LOG_FILE}"
    
    # Update entity imports to model
    sed -i 's/import org.rinna.domain.entity\./import org.rinna.domain.model./g' "${file}"
    
    # Update usecase imports to service
    sed -i 's/import org.rinna.domain.usecase\./import org.rinna.domain.service./g' "${file}"
    
    # Update persistence imports to repository
    sed -i 's/import org.rinna.adapter.persistence\./import org.rinna.adapter.repository./g' "${file}"
    
    # Fix direct imports for major domain classes
    domain_classes=("WorkItem" "WorkItemType" "WorkQueue" "WorkflowState" "Priority" "Project" "Release" "WorkItemCreateRequest" "WorkItemMetadata")
    for class in "${domain_classes[@]}"; do
        sed -i "s/import org.rinna.domain.${class};/import org.rinna.domain.model.${class};/g" "${file}"
    done
    
    # Fix direct imports for service classes
    service_classes=("ItemService" "WorkflowService" "ReleaseService" "QueueService" "InvalidTransitionException")
    for class in "${service_classes[@]}"; do
        sed -i "s/import org.rinna.domain.${class};/import org.rinna.domain.service.${class};/g" "${file}"
        sed -i "s/import org.rinna.usecase.${class};/import org.rinna.domain.service.${class};/g" "${file}"
    done
done

echo "Test updates completed at $(date)" | tee -a "${LOG_FILE}"
echo "The test files have been updated to use the new package structure."