#!/bin/bash
# migrate.sh
# Script to implement package structure changes with direct edits

set -e

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
LOG_FILE="${RINNA_DIR}/bin/migration/migration-direct.log"

# Start fresh log
echo "Starting direct migration at $(date)" > "${LOG_FILE}"

# Create the target directories
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/model"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/repository"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/service"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/adapter/service"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/adapter/repository"

# Step 1: Copy the source code structure
echo "Copying source code structure..." | tee -a "${LOG_FILE}"
rsync -av "${RINNA_DIR}/rinna-core/src/" "${RINNA_DIR}/src/" >> "${LOG_FILE}" 2>&1

# Step 2: Move entity files to domain.model
echo "Moving entity files to domain.model..." | tee -a "${LOG_FILE}"
ENTITY_DIR="${RINNA_DIR}/src/main/java/org/rinna/domain/entity"
MODEL_DIR="${RINNA_DIR}/src/main/java/org/rinna/domain/model"

if [ -d "${ENTITY_DIR}" ]; then
    echo "Found entity directory, moving files..." | tee -a "${LOG_FILE}"
    
    # Convert package declarations in all entity files
    find "${ENTITY_DIR}" -name "*.java" | while read -r file; do
        filename=$(basename "${file}")
        # Update package declaration
        sed -i 's/package org.rinna.domain.entity;/package org.rinna.domain.model;/' "${file}"
        # Move file to model directory
        mv "${file}" "${MODEL_DIR}/"
        echo "Moved ${filename} to model package" | tee -a "${LOG_FILE}"
    done
    
    # Remove the now-empty entity directory
    rmdir "${ENTITY_DIR}" 2>/dev/null || true
else
    echo "Entity directory not found, skipping" | tee -a "${LOG_FILE}"
fi

# Step 3: Move usecase files to domain.service
echo "Moving usecase files to domain.service..." | tee -a "${LOG_FILE}"
USECASE_DIR="${RINNA_DIR}/src/main/java/org/rinna/domain/usecase"
SERVICE_DIR="${RINNA_DIR}/src/main/java/org/rinna/domain/service"

if [ -d "${USECASE_DIR}" ]; then
    echo "Found usecase directory, moving files..." | tee -a "${LOG_FILE}"
    
    # Convert package declarations in all usecase files
    find "${USECASE_DIR}" -name "*.java" | while read -r file; do
        filename=$(basename "${file}")
        # Update package declaration
        sed -i 's/package org.rinna.domain.usecase;/package org.rinna.domain.service;/' "${file}"
        # Move file to service directory
        mv "${file}" "${SERVICE_DIR}/"
        echo "Moved ${filename} to service package" | tee -a "${LOG_FILE}"
    done
    
    # Remove the now-empty usecase directory
    rmdir "${USECASE_DIR}" 2>/dev/null || true
else
    echo "Usecase directory not found, skipping" | tee -a "${LOG_FILE}"
fi

# Step 4: Move persistence files to adapter.repository
echo "Moving persistence files to adapter.repository..." | tee -a "${LOG_FILE}"
PERSISTENCE_DIR="${RINNA_DIR}/src/main/java/org/rinna/adapter/persistence"
REPOSITORY_DIR="${RINNA_DIR}/src/main/java/org/rinna/adapter/repository"

if [ -d "${PERSISTENCE_DIR}" ]; then
    echo "Found persistence directory, moving files..." | tee -a "${LOG_FILE}"
    
    # Convert package declarations in all persistence files
    find "${PERSISTENCE_DIR}" -name "*.java" | while read -r file; do
        filename=$(basename "${file}")
        # Update package declaration
        sed -i 's/package org.rinna.adapter.persistence;/package org.rinna.adapter.repository;/' "${file}"
        # Move file to repository directory
        mv "${file}" "${REPOSITORY_DIR}/"
        echo "Moved ${filename} to repository package" | tee -a "${LOG_FILE}"
    done
    
    # Remove the now-empty persistence directory
    rmdir "${PERSISTENCE_DIR}" 2>/dev/null || true
else
    echo "Persistence directory not found, skipping" | tee -a "${LOG_FILE}"
fi

# Step 5: Update import statements throughout the codebase
echo "Updating import statements throughout the codebase..." | tee -a "${LOG_FILE}"

find "${RINNA_DIR}/src" -name "*.java" | while read -r file; do
    # Update entity imports to model
    sed -i 's/import org.rinna.domain.entity\./import org.rinna.domain.model./g' "${file}"
    
    # Update usecase imports to service
    sed -i 's/import org.rinna.domain.usecase\./import org.rinna.domain.service./g' "${file}"
    
    # Update persistence imports to repository
    sed -i 's/import org.rinna.adapter.persistence\./import org.rinna.adapter.repository./g' "${file}"
    
    echo "Updated imports in ${file}" >> "${LOG_FILE}"
done

# Step 6: Direct package imports (org.rinna.domain.Class) need special handling
echo "Handling direct package imports..." | tee -a "${LOG_FILE}"

# Create a list of all domain model classes
find "${MODEL_DIR}" -name "*.java" | while read -r file; do
    classname=$(basename "${file}" .java)
    
    # Find all files importing this class directly from domain package
    find "${RINNA_DIR}/src" -name "*.java" | xargs grep -l "import org.rinna.domain.${classname};" | while read -r importfile; do
        # Replace with model package import
        sed -i "s/import org.rinna.domain.${classname};/import org.rinna.domain.model.${classname};/g" "${importfile}"
        echo "Updated direct import of ${classname} in ${importfile}" >> "${LOG_FILE}"
    done
done

# Create a list of all domain service classes
find "${SERVICE_DIR}" -name "*.java" | while read -r file; do
    classname=$(basename "${file}" .java)
    
    # Find all files importing this class directly from domain package
    find "${RINNA_DIR}/src" -name "*.java" | xargs grep -l "import org.rinna.domain.${classname};" | while read -r importfile; do
        # Replace with service package import
        sed -i "s/import org.rinna.domain.${classname};/import org.rinna.domain.service.${classname};/g" "${importfile}"
        echo "Updated direct import of ${classname} in ${importfile}" >> "${LOG_FILE}"
    done
done

# Find all files importing from usecase
find "${RINNA_DIR}/src" -name "*.java" | xargs grep -l "import org.rinna.usecase." | while read -r importfile; do
    # Replace with service package import
    sed -i "s/import org.rinna.usecase./import org.rinna.domain.service./g" "${importfile}"
    echo "Updated usecase import in ${importfile}" >> "${LOG_FILE}"
done

echo "Migration completed at $(date)" | tee -a "${LOG_FILE}"
echo "Next step: Verify the build with 'cd ${RINNA_DIR} && mvn compile'"
echo "Complete! The migration has been applied."