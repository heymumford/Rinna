#!/bin/bash
# manual-fix.sh
# Script to manually fix the package structure issues

set -e

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
LOG_FILE="${RINNA_DIR}/bin/migration/manual-fix.log"

echo "Starting manual fix at $(date)" | tee "${LOG_FILE}"

# Clean up everything for a fresh start
echo "Cleaning up for a fresh start..." | tee -a "${LOG_FILE}"

# Remove conflicting service directories
rm -rf "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/service" 2>/dev/null || true
rm -rf "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/persistence" 2>/dev/null || true
rm -rf "${RINNA_DIR}/src/main/java/org/rinna/service" 2>/dev/null || true
rm -rf "${RINNA_DIR}/src/main/java/org/rinna/domain/model" 2>/dev/null || true
rm -rf "${RINNA_DIR}/src/main/java/org/rinna/domain/service" 2>/dev/null || true
rm -rf "${RINNA_DIR}/src/main/java/org/rinna/adapter/repository" 2>/dev/null || true

# Create the standard package directories
echo "Creating standard package structure..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model"
mkdir -p "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service"
mkdir -p "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/service"
mkdir -p "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/repository"

# Move domain entity files to domain.model
echo "Moving entity files to domain.model..." | tee -a "${LOG_FILE}"
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/entity" ]; then
    cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/entity/"*.java \
       "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model/"
    
    # Update package declarations
    for file in "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model/"*.java; do
        echo "Updating package in $(basename ${file})..." | tee -a "${LOG_FILE}"
        sed -i 's/package org.rinna.domain.entity;/package org.rinna.domain.model;/g' "${file}"
    done
    
    rm -rf "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/entity"
fi

# Move domain usecase files to domain.service
echo "Moving usecase files to domain.service..." | tee -a "${LOG_FILE}"
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/usecase" ]; then
    cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/usecase/"*.java \
       "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service/"
    
    # Update package declarations
    for file in "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service/"*.java; do
        echo "Updating package in $(basename ${file})..." | tee -a "${LOG_FILE}"
        sed -i 's/package org.rinna.domain.usecase;/package org.rinna.domain.service;/g' "${file}"
    done
    
    rm -rf "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/usecase"
fi

# Move adapter persistence files to adapter.repository
echo "Moving persistence files to adapter.repository..." | tee -a "${LOG_FILE}"
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/persistence" ]; then
    cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/persistence/"*.java \
       "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/repository/"
    
    # Update package declarations
    for file in "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/repository/"*.java; do
        echo "Updating package in $(basename ${file})..." | tee -a "${LOG_FILE}"
        sed -i 's/package org.rinna.adapter.persistence;/package org.rinna.adapter.repository;/g' "${file}"
    done
    
    rm -rf "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/persistence"
fi

# Update imports throughout the codebase
echo "Updating imports throughout the codebase..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/rinna-core/src" -name "*.java" -exec \
  sed -i 's/org.rinna.domain.entity/org.rinna.domain.model/g; 
          s/org.rinna.domain.usecase/org.rinna.domain.service/g;
          s/org.rinna.adapter.persistence/org.rinna.adapter.repository/g' {} \;

# Copy the updated structure to src
echo "Copying the updated structure to src..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/model"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/service"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/adapter/service"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/adapter/repository"

# Copy the updated files
echo "Copying domain model files..." | tee -a "${LOG_FILE}"
cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model/"*.java \
   "${RINNA_DIR}/src/main/java/org/rinna/domain/model/"

echo "Copying domain service files..." | tee -a "${LOG_FILE}"
cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service/"*.java \
   "${RINNA_DIR}/src/main/java/org/rinna/domain/service/"

echo "Copying adapter service files..." | tee -a "${LOG_FILE}"
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/service" ]; then
    cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/service/"*.java \
       "${RINNA_DIR}/src/main/java/org/rinna/adapter/service/"
fi

echo "Copying adapter repository files..." | tee -a "${LOG_FILE}"
cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/repository/"*.java \
   "${RINNA_DIR}/src/main/java/org/rinna/adapter/repository/"

# Copy resources
echo "Copying resources..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/src/main/resources"
if [ -d "${RINNA_DIR}/rinna-core/src/main/resources" ]; then
    cp -r "${RINNA_DIR}/rinna-core/src/main/resources/"* \
        "${RINNA_DIR}/src/main/resources/"
fi

# Update tests
echo "Updating test directories..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/src/test/java/org/rinna/domain/service"
mkdir -p "${RINNA_DIR}/src/test/java/org/rinna/model"
mkdir -p "${RINNA_DIR}/src/test/java/org/rinna/service"
mkdir -p "${RINNA_DIR}/src/test/java/org/rinna/service/impl"
mkdir -p "${RINNA_DIR}/src/test/resources"

# Copy test files if they exist
if [ -d "${RINNA_DIR}/rinna-core/src/test/java/org/rinna" ]; then
    echo "Copying test files..." | tee -a "${LOG_FILE}"
    cp -r "${RINNA_DIR}/rinna-core/src/test/java/org/rinna/"* \
        "${RINNA_DIR}/src/test/java/org/rinna/"
fi

# Copy test resources if they exist
if [ -d "${RINNA_DIR}/rinna-core/src/test/resources" ]; then
    echo "Copying test resources..." | tee -a "${LOG_FILE}"
    cp -r "${RINNA_DIR}/rinna-core/src/test/resources/"* \
        "${RINNA_DIR}/src/test/resources/"
fi

# Update test imports
echo "Updating imports in test files..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/src/test" -name "*.java" -exec \
  sed -i 's/org.rinna.domain.entity/org.rinna.domain.model/g; 
          s/org.rinna.domain.usecase/org.rinna.domain.service/g;
          s/org.rinna.adapter.persistence/org.rinna.adapter.repository/g' {} \;

echo "Manual fix completed at $(date)" | tee -a "${LOG_FILE}"
echo "The package structure should now be consistent."
echo "Try building with: cd ${RINNA_DIR}/rinna-core && mvn compile"