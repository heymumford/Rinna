#!/bin/bash

# Set the default directory path to the project directory
RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"

# Timestamp for logging
timestamp() {
  date
}

echo "Starting test package structure fixes at $(timestamp)..."

# Create backup directory
BACKUP_DIR="${RINNA_DIR}/backup/test-fixes-$(date +%Y%m%d%H%M%S)"
mkdir -p "${BACKUP_DIR}/src"
mkdir -p "${BACKUP_DIR}/rinna-core"

# Backup test files
if [ -d "${RINNA_DIR}/src/test" ]; then
  cp -r "${RINNA_DIR}/src/test" "${BACKUP_DIR}/src/"
fi

if [ -d "${RINNA_DIR}/rinna-core/src/test" ]; then
  cp -r "${RINNA_DIR}/rinna-core/src/test" "${BACKUP_DIR}/rinna-core/"
fi

echo "Created backup in ${BACKUP_DIR}"

echo "Fixing package imports in test files..."

# Fix imports in src/test files
if [ -d "${RINNA_DIR}/src/test" ]; then
  # Fix package imports for domain model classes
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.Priority/import org.rinna.domain.model.Priority/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkItem/import org.rinna.domain.model.WorkItem/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkItemCreateRequest/import org.rinna.domain.model.WorkItemCreateRequest/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkItemType/import org.rinna.domain.model.WorkItemType/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkflowState/import org.rinna.domain.model.WorkflowState/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.Project/import org.rinna.domain.model.Project/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.Release/import org.rinna.domain.model.Release/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkQueue/import org.rinna.domain.model.WorkQueue/g' {} \;
  
  # Fix package imports for service interfaces
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.InvalidTransitionException/import org.rinna.domain.service.InvalidTransitionException/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.ItemService/import org.rinna.domain.service.ItemService/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.WorkflowService/import org.rinna.domain.service.WorkflowService/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.ReleaseService/import org.rinna.domain.service.ReleaseService/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.QueueService/import org.rinna.domain.service.QueueService/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.DocumentService/import org.rinna.domain.service.DocumentService/g' {} \;
  
  # Fix non-import references to the packages in code
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.Priority/org.rinna.domain.model.Priority/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.WorkItem/org.rinna.domain.model.WorkItem/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.WorkItemCreateRequest/org.rinna.domain.model.WorkItemCreateRequest/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.WorkItemType/org.rinna.domain.model.WorkItemType/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.WorkflowState/org.rinna.domain.model.WorkflowState/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.usecase.InvalidTransitionException/org.rinna.domain.service.InvalidTransitionException/g' {} \;
fi

# Fix imports in rinna-core/src/test files
if [ -d "${RINNA_DIR}/rinna-core/src/test" ]; then
  # Fix package imports for domain model classes
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.Priority/import org.rinna.domain.model.Priority/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkItem/import org.rinna.domain.model.WorkItem/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkItemCreateRequest/import org.rinna.domain.model.WorkItemCreateRequest/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkItemType/import org.rinna.domain.model.WorkItemType/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkflowState/import org.rinna.domain.model.WorkflowState/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.Project/import org.rinna.domain.model.Project/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.Release/import org.rinna.domain.model.Release/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.WorkQueue/import org.rinna.domain.model.WorkQueue/g' {} \;
  
  # Fix package imports for service interfaces
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.InvalidTransitionException/import org.rinna.domain.service.InvalidTransitionException/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.ItemService/import org.rinna.domain.service.ItemService/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.WorkflowService/import org.rinna.domain.service.WorkflowService/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.ReleaseService/import org.rinna.domain.service.ReleaseService/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.QueueService/import org.rinna.domain.service.QueueService/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.usecase.DocumentService/import org.rinna.domain.service.DocumentService/g' {} \;
  
  # Fix non-import references to the packages in code
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.Priority/org.rinna.domain.model.Priority/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.WorkItem/org.rinna.domain.model.WorkItem/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.WorkItemCreateRequest/org.rinna.domain.model.WorkItemCreateRequest/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.WorkItemType/org.rinna.domain.model.WorkItemType/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.WorkflowState/org.rinna.domain.model.WorkflowState/g' {} \;
  find "${RINNA_DIR}/rinna-core/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.usecase.InvalidTransitionException/org.rinna.domain.service.InvalidTransitionException/g' {} \;
fi

# Create Rinna class in src/test if it doesn't exist
if grep -q "class Rinna" "${RINNA_DIR}/src/test" -r; then
  echo "Rinna class already exists in test files."
else
  echo "Creating Rinna test class for compatibility..."
  mkdir -p "${RINNA_DIR}/src/test/java/org/rinna/utils"
  cat > "${RINNA_DIR}/src/test/java/org/rinna/utils/TestRinna.java" << 'EOF'
package org.rinna.utils;

import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.WorkflowService;
import org.rinna.domain.service.ReleaseService;
import org.rinna.domain.service.QueueService;

/**
 * Test utility class that provides access to services for testing.
 * This class is used in tests that were written for the old package structure.
 */
public class TestRinna {

    private static TestRinna instance;
    
    private final ItemService itemService;
    private final WorkflowService workflowService;
    private final ReleaseService releaseService;
    private final QueueService queueService;
    
    private TestRinna() {
        this.itemService = null; // For now just null, replace with mock implementations if needed
        this.workflowService = null;
        this.releaseService = null;
        this.queueService = null;
    }
    
    public static TestRinna initialize() {
        if (instance == null) {
            instance = new TestRinna();
        }
        return instance;
    }
    
    public ItemService items() {
        return itemService;
    }
    
    public WorkflowService workflow() {
        return workflowService;
    }
    
    public ReleaseService releases() {
        return releaseService;
    }
    
    public QueueService queues() {
        return queueService;
    }
}
EOF
  
  # Update imports to use the test Rinna class
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.Rinna;/import org.rinna.utils.TestRinna;/g' {} \;
  find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/Rinna rinna = Rinna.initialize()/TestRinna rinna = TestRinna.initialize()/g' {} \;
fi

echo "Ensuring necessary dependencies for tests..."

# Ensure JUnit 5 dependencies are in the pom.xml files
for POM_FILE in "${RINNA_DIR}/pom.xml" "${RINNA_DIR}/src/pom.xml" "${RINNA_DIR}/rinna-core/pom.xml"; do
  if [ -f "$POM_FILE" ] && ! grep -q "<artifactId>junit-jupiter-api</artifactId>" "$POM_FILE"; then
    sed -i '/<dependencies>/a \
        <!-- JUnit 5 dependencies -->\
        <dependency>\
            <groupId>org.junit.jupiter</groupId>\
            <artifactId>junit-jupiter-api</artifactId>\
            <version>5.10.2</version>\
            <scope>test</scope>\
        </dependency>\
        <dependency>\
            <groupId>org.junit.jupiter</groupId>\
            <artifactId>junit-jupiter-engine</artifactId>\
            <version>5.10.2</version>\
            <scope>test</scope>\
        </dependency>' "$POM_FILE"
    
    echo "Added JUnit 5 dependencies to $POM_FILE"
  fi
done

# For convenience, copy the rinna-core domain model classes to the main src/test directory
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model" ] && [ -d "${RINNA_DIR}/src/test/java/org/rinna/domain" ]; then
  echo "Copying domain model classes to src/test/java for test compatibility..."
  mkdir -p "${RINNA_DIR}/src/test/java/org/rinna/domain/model"
  cp -n "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model/"*.java "${RINNA_DIR}/src/test/java/org/rinna/domain/model/" || true
fi

echo "Test package structure fixes completed at $(timestamp)"
echo "Now try running: cd ${RINNA_DIR} && ./bin/rin test unit"