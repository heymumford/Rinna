#!/bin/bash

# Fix package imports in all Java test files to align with Clean Architecture

# Start with timestamp for log
echo "Starting comprehensive test imports fix at $(date)..."

# Set the project root directory
RINNA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Backup the current test files before making changes
BACKUP_DIR="${RINNA_DIR}/backup/src-tests-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${BACKUP_DIR}"
cp -r "${RINNA_DIR}/src/test" "${BACKUP_DIR}/"
cp -r "${RINNA_DIR}/rinna-core/src/test" "${BACKUP_DIR}/" 2>/dev/null || true
echo "Created backup in ${BACKUP_DIR}"

# Move into the project directory
cd "${RINNA_DIR}"

echo "Fixing package imports in test files..."

# Fix domain model class imports
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.Priority;/import org.rinna.domain.model.Priority;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkItemType;/import org.rinna.domain.model.WorkItemType;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkItem;/import org.rinna.domain.model.WorkItem;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkflowState;/import org.rinna.domain.model.WorkflowState;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.Project;/import org.rinna.domain.model.Project;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.Release;/import org.rinna.domain.model.Release;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkQueue;/import org.rinna.domain.model.WorkQueue;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkItemCreateRequest;/import org.rinna.domain.model.WorkItemCreateRequest;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.DocumentConfig;/import org.rinna.domain.model.DocumentConfig;/g' {} \;

# Fix service interface imports
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.service.ItemService;/import org.rinna.usecase.ItemService;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.service.WorkflowService;/import org.rinna.usecase.WorkflowService;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.service.ReleaseService;/import org.rinna.usecase.ReleaseService;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.service.QueueService;/import org.rinna.usecase.QueueService;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.service.DocumentService;/import org.rinna.usecase.DocumentService;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.service.InvalidTransitionException;/import org.rinna.usecase.InvalidTransitionException;/g' {} \;

# Fix repository imports
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.repository.ItemRepository;/import org.rinna.repository.ItemRepository;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.repository.MetadataRepository;/import org.rinna.repository.MetadataRepository;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.repository.QueueRepository;/import org.rinna.repository.QueueRepository;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.repository.ReleaseRepository;/import org.rinna.repository.ReleaseRepository;/g' {} \;

# Fix implementation imports
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.DefaultProject;/import org.rinna.domain.model.DefaultProject;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.DefaultRelease;/import org.rinna.domain.model.DefaultRelease;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.DefaultWorkItem;/import org.rinna.domain.model.DefaultWorkItem;/g' {} \;
find "${RINNA_DIR}/src" "${RINNA_DIR}/rinna-core/src" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.DefaultWorkQueue;/import org.rinna.domain.model.DefaultWorkQueue;/g' {} \;

echo "Fixing missing JUnit Platform Suite API dependency..."
# Check the Maven POM file and fix duplicated dependencies
if grep -q "junit-platform-suite-api.*1.10.2.*scope.*test" "${RINNA_DIR}/pom.xml"; then
    sed -i 's/<dependency>.*\n.*junit-platform-suite-api.*\n.*1.10.2.*\n.*scope.*test.*\n.*<\/dependency>/<dependency>\n            <groupId>org.junit.platform<\/groupId>\n            <artifactId>junit-platform-suite-api<\/artifactId>\n            <version>1.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"
else 
    echo "JUnit Platform Suite API dependency appears to be OK"
fi

echo "Checking for build issues and adding any missing dependencies..."
# Add any other build fixes here if needed

echo "Comprehensive import fixes completed at $(date)"
echo "To test the changes, run: cd ${RINNA_DIR} && ./bin/rin test unit"