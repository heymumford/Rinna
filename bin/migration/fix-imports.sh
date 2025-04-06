#!/bin/bash
# fix-imports.sh
# Script to fix any remaining import issues

set -e

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
LOG_FILE="${RINNA_DIR}/bin/migration/fix-imports.log"

echo "Starting import fixes at $(date)" > "${LOG_FILE}"

# Add more import fixes for domain model classes that were directly in domain package
echo "Fixing direct domain imports to use domain.model package..." | tee -a "${LOG_FILE}"

find "${RINNA_DIR}/rinna-core/src/main/java" -name "*.java" | while read -r file; do
    # Fix DefaultDocumentService
    if [[ "${file}" == *"DefaultDocumentService.java" ]]; then
        echo "Fixing imports in ${file}" | tee -a "${LOG_FILE}"
        sed -i 's/import org.rinna.domain.DocumentConfig;/import org.rinna.domain.model.DocumentConfig;/g' "${file}"
        sed -i 's/import org.rinna.domain.Project;/import org.rinna.domain.model.Project;/g' "${file}"
        sed -i 's/import org.rinna.domain.Release;/import org.rinna.domain.model.Release;/g' "${file}"
        sed -i 's/import org.rinna.domain.WorkItem;/import org.rinna.domain.model.WorkItem;/g' "${file}"
        sed -i 's/import org.rinna.usecase.DocumentService;/import org.rinna.domain.service.DocumentService;/g' "${file}"
    fi
    
    # Fix default implementations to use correct imports
    if [[ "${file}" == *"service/Default"*"Service.java" ]]; then
        echo "Fixing service implementation imports in ${file}" | tee -a "${LOG_FILE}"
        sed -i 's/import org.rinna.domain.\([A-Za-z]*\);/import org.rinna.domain.model.\1;/g' "${file}"
        sed -i 's/import org.rinna.usecase.\([A-Za-z]*Service\|InvalidTransitionException\);/import org.rinna.domain.service.\1;/g' "${file}"
    fi
done

echo "Fixing remaining imports in src directory..." | tee -a "${LOG_FILE}"

find "${RINNA_DIR}/src/main/java" -name "*.java" | while read -r file; do
    # Fix DefaultDocumentService
    if [[ "${file}" == *"DefaultDocumentService.java" ]]; then
        echo "Fixing imports in ${file}" | tee -a "${LOG_FILE}"
        sed -i 's/import org.rinna.domain.DocumentConfig;/import org.rinna.domain.model.DocumentConfig;/g' "${file}"
        sed -i 's/import org.rinna.domain.Project;/import org.rinna.domain.model.Project;/g' "${file}"
        sed -i 's/import org.rinna.domain.Release;/import org.rinna.domain.model.Release;/g' "${file}"
        sed -i 's/import org.rinna.domain.WorkItem;/import org.rinna.domain.model.WorkItem;/g' "${file}"
        sed -i 's/import org.rinna.usecase.DocumentService;/import org.rinna.domain.service.DocumentService;/g' "${file}"
    fi
    
    # Fix default implementations to use correct imports
    if [[ "${file}" == *"service/Default"*"Service.java" ]]; then
        echo "Fixing service implementation imports in ${file}" | tee -a "${LOG_FILE}"
        sed -i 's/import org.rinna.domain.\([A-Za-z]*\);/import org.rinna.domain.model.\1;/g' "${file}"
        sed -i 's/import org.rinna.usecase.\([A-Za-z]*Service\|InvalidTransitionException\);/import org.rinna.domain.service.\1;/g' "${file}"
    fi
done

echo "Import fixes completed at $(date)" | tee -a "${LOG_FILE}"
echo "Remaining imports have been fixed."