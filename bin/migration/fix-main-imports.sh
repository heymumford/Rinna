#\!/bin/bash

# Set the default directory path to the current directory
RINNA_DIR="${RINNA_DIR:-/home/emumford/NativeLinuxProjects/Rinna}"

# Timestamp for logging
timestamp() {
  date
}

echo "Starting to fix main imports at $(timestamp)..."

# Create output directories if they don't exist
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/service"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/model"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/repository"

# Copy necessary interfaces from core to main
echo "Copying core interfaces to main..."
cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/service/"*.java "${RINNA_DIR}/src/main/java/org/rinna/domain/service/"
cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model/"*.java "${RINNA_DIR}/src/main/java/org/rinna/domain/model/"
cp "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/repository/"*.java "${RINNA_DIR}/src/main/java/org/rinna/repository/"

echo "Updating imports in test files..."
# Fix imports in test files
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.entity/import org.rinna.domain.model/g' {} \;
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.usecase/import org.rinna.domain.service/g' {} \;
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.service.impl/import org.rinna.adapter.service/g' {} \;
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.persistence/import org.rinna.adapter.repository/g' {} \;

# Update main package references
echo "Updating package references..."
find "${RINNA_DIR}/src/main" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.entity/import org.rinna.domain.model/g' {} \;
find "${RINNA_DIR}/src/main" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.usecase/import org.rinna.domain.service/g' {} \;
find "${RINNA_DIR}/src/main" -name "*.java" -type f -exec sed -i 's/import org.rinna.service.impl/import org.rinna.adapter.service/g' {} \;
find "${RINNA_DIR}/src/main" -name "*.java" -type f -exec sed -i 's/import org.rinna.persistence/import org.rinna.adapter.repository/g' {} \;

echo "Import fixes completed at $(timestamp)"
echo "Now run: cd ${RINNA_DIR} && mvn clean test"
