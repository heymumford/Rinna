#!/bin/bash
# comprehensive-migration.sh
# Script for complete package structure migration

set -e

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
LOG_FILE="${RINNA_DIR}/bin/migration/comprehensive-migration.log"

echo "Starting comprehensive migration at $(date)" | tee "${LOG_FILE}"

# Step 1: Clean up any partial migration
echo "Step 1: Cleaning up partial migrations..." | tee -a "${LOG_FILE}"
rm -rf "${RINNA_DIR}/src/main/java/org/rinna/domain/model" 2>/dev/null || true
rm -rf "${RINNA_DIR}/src/main/java/org/rinna/domain/service" 2>/dev/null || true
rm -rf "${RINNA_DIR}/src/main/java/org/rinna/adapter/repository" 2>/dev/null || true
rm -rf "${RINNA_DIR}/src/main/java/org/rinna/repository" 2>/dev/null || true

# Step 2: Prepare new package structure in rinna-core
echo "Step 2: Creating target package structure..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model"
mkdir -p "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service"
mkdir -p "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/repository"

echo "Step 2a: Moving entity files to model package..." | tee -a "${LOG_FILE}"
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/entity" ]; then
    mv "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/entity/"* \
       "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model/"
fi

echo "Step 2b: Moving usecase files to service package..." | tee -a "${LOG_FILE}"
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/usecase" ]; then
    mv "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/usecase/"* \
       "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service/"
fi

echo "Step 2c: Moving persistence files to repository package..." | tee -a "${LOG_FILE}"
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/persistence" ]; then
    mv "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/persistence/"* \
       "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/repository/"
fi

echo "Step 2d: Removing old directories..." | tee -a "${LOG_FILE}"
rmdir "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/entity" 2>/dev/null || true
rmdir "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/usecase" 2>/dev/null || true
rmdir "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/persistence" 2>/dev/null || true

# Step 3: Update package declarations
echo "Step 3: Updating package declarations..." | tee -a "${LOG_FILE}"
echo "Step 3a: Updating model package declarations..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model" -name "*.java" -exec \
  sed -i 's/package org.rinna.domain.entity;/package org.rinna.domain.model;/g' {} \;

echo "Step 3b: Updating service package declarations..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service" -name "*.java" -exec \
  sed -i 's/package org.rinna.domain.usecase;/package org.rinna.domain.service;/g' {} \;

echo "Step 3c: Updating repository package declarations..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/adapter/repository" -name "*.java" -exec \
  sed -i 's/package org.rinna.adapter.persistence;/package org.rinna.adapter.repository;/g' {} \;

# Step 4: Update imports in all files
echo "Step 4: Updating imports in all files..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/rinna-core/src" -name "*.java" -exec \
  sed -i 's/import org.rinna.domain.entity\./import org.rinna.domain.model./g; 
          s/import org.rinna.domain.usecase\./import org.rinna.domain.service./g;
          s/import org.rinna.adapter.persistence\./import org.rinna.adapter.repository./g' {} \;

# Step 5: Fix direct entity and service imports
echo "Step 5: Fixing direct imports..." | tee -a "${LOG_FILE}"

# Domain model classes
domain_classes=("WorkItem" "WorkItemType" "WorkQueue" "WorkflowState" "Priority" "Project" "Release" "WorkItemCreateRequest" "WorkItemMetadata" "DefaultWorkItem" "DefaultProject" "DefaultRelease" "DefaultWorkQueue" "DocumentConfig")

for class in "${domain_classes[@]}"; do
  echo "  - Fixing direct imports for ${class}" | tee -a "${LOG_FILE}"
  find "${RINNA_DIR}/rinna-core/src" -name "*.java" -exec \
    sed -i "s/import org.rinna.domain.${class};/import org.rinna.domain.model.${class};/g" {} \;
done

# Service classes
service_classes=("ItemService" "WorkflowService" "ReleaseService" "QueueService" "DocumentService" "InvalidTransitionException")

for service in "${service_classes[@]}"; do
  echo "  - Fixing direct imports for ${service}" | tee -a "${LOG_FILE}"
  find "${RINNA_DIR}/rinna-core/src" -name "*.java" -exec \
    sed -i "s/import org.rinna.domain.${service};/import org.rinna.domain.service.${service};/g;
            s/import org.rinna.usecase.${service};/import org.rinna.domain.service.${service};/g" {} \;
done

# Step 6: Verify rinna-core builds
echo "Step 6: Verifying rinna-core build..." | tee -a "${LOG_FILE}"
if cd "${RINNA_DIR}/rinna-core" && mvn compile -q; then
    echo "  ✓ rinna-core build successful" | tee -a "${LOG_FILE}"
else
    echo "  ✗ rinna-core build failed. See Maven output for details." | tee -a "${LOG_FILE}"
    echo "Migration halted due to build failure." | tee -a "${LOG_FILE}"
    exit 1
fi

# Step 7: Copy to src directory
echo "Step 7: Copying to src directory..." | tee -a "${LOG_FILE}"
echo "Step 7a: Creating target directory structure..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna"

echo "Step 7b: Copying Java sources..." | tee -a "${LOG_FILE}"
rsync -av "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/" "${RINNA_DIR}/src/main/java/org/rinna/" >> "${LOG_FILE}" 2>&1

echo "Step 7c: Copying resources..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/src/main/resources"
if [ -d "${RINNA_DIR}/rinna-core/src/main/resources" ]; then
    rsync -av "${RINNA_DIR}/rinna-core/src/main/resources/" "${RINNA_DIR}/src/main/resources/" >> "${LOG_FILE}" 2>&1
fi

# Step 8: Update tests
echo "Step 8: Updating tests..." | tee -a "${LOG_FILE}"
echo "Step 8a: Creating test directories..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/src/test/java/org/rinna"

echo "Step 8b: Copying test sources..." | tee -a "${LOG_FILE}"
if [ -d "${RINNA_DIR}/rinna-core/src/test/java/org/rinna" ]; then
    rsync -av "${RINNA_DIR}/rinna-core/src/test/java/org/rinna/" "${RINNA_DIR}/src/test/java/org/rinna/" >> "${LOG_FILE}" 2>&1
fi

echo "Step 8c: Copying test resources..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/src/test/resources"
if [ -d "${RINNA_DIR}/rinna-core/src/test/resources" ]; then
    rsync -av "${RINNA_DIR}/rinna-core/src/test/resources/" "${RINNA_DIR}/src/test/resources/" >> "${LOG_FILE}" 2>&1
fi

# Step 9: Update all import statements in src directory
echo "Step 9: Fixing imports in src directory..." | tee -a "${LOG_FILE}"
find "${RINNA_DIR}/src" -name "*.java" -exec \
  sed -i 's/import org.rinna.domain.entity\./import org.rinna.domain.model./g; 
          s/import org.rinna.domain.usecase\./import org.rinna.domain.service./g;
          s/import org.rinna.adapter.persistence\./import org.rinna.adapter.repository./g' {} \;

# Fix direct entity imports in src directory
for class in "${domain_classes[@]}"; do
  echo "  - Fixing direct imports for ${class} in src" | tee -a "${LOG_FILE}"
  find "${RINNA_DIR}/src" -name "*.java" -exec \
    sed -i "s/import org.rinna.domain.${class};/import org.rinna.domain.model.${class};/g" {} \;
done

# Fix direct service imports in src directory
for service in "${service_classes[@]}"; do
  echo "  - Fixing direct imports for ${service} in src" | tee -a "${LOG_FILE}"
  find "${RINNA_DIR}/src" -name "*.java" -exec \
    sed -i "s/import org.rinna.domain.${service};/import org.rinna.domain.service.${service};/g;
            s/import org.rinna.usecase.${service};/import org.rinna.domain.service.${service};/g" {} \;
done

# Step 10: Verify full build
echo "Step 10: Verifying full project build..." | tee -a "${LOG_FILE}"
if cd "${RINNA_DIR}" && mvn clean compile -q; then
    echo "  ✓ Full project build successful" | tee -a "${LOG_FILE}"
else
    echo "  ✗ Full project build failed. See Maven output for details." | tee -a "${LOG_FILE}"
    echo "Migration completed with build errors. Please check Maven output." | tee -a "${LOG_FILE}"
    exit 1
fi

echo "Migration completed successfully at $(date)" | tee -a "${LOG_FILE}"
echo "The new package structure is now in place."
echo "You can verify with: cd ${RINNA_DIR} && mvn test"