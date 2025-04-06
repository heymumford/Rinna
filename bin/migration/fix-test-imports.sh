#\!/bin/bash

# Set the default directory path to the current directory
RINNA_DIR="${RINNA_DIR:-/home/emumford/NativeLinuxProjects/Rinna}"

# Timestamp for logging
timestamp() {
  date
}

echo "Starting to fix test imports at $(timestamp)..."

# Create backup
mkdir -p "${RINNA_DIR}/backup/src-tests-$(date +%Y%m%d%H%M%S)"
cp -r "${RINNA_DIR}/src/test" "${RINNA_DIR}/backup/src-tests-$(date +%Y%m%d%H%M%S)/"

echo "Fixed package imports..."
# Fix imports in test files
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.entity/import org.rinna.domain.model/g' {} \;
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.domain.usecase/import org.rinna.domain.service/g' {} \;
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.service.impl/import org.rinna.adapter.service/g' {} \;
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/import org.rinna.persistence/import org.rinna.adapter.repository/g' {} \;

# Fix direct package references
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.entity/org.rinna.domain.model/g' {} \;
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.domain.usecase/org.rinna.domain.service/g' {} \;
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.service.impl/org.rinna.adapter.service/g' {} \;
find "${RINNA_DIR}/src/test" -name "*.java" -type f -exec sed -i 's/org.rinna.persistence/org.rinna.adapter.repository/g' {} \;

echo "Fixed Cucumber dependency..."
# Add Cucumber dependency to pom.xml if not already present
if \! grep -q "junit-platform-suite-api" "${RINNA_DIR}/src/pom.xml"; then
  sed -i '/<dependencies>/a \
        <\!-- JUnit platform suite for Cucumber -->\
        <dependency>\
            <groupId>org.junit.platform</groupId>\
            <artifactId>junit-platform-suite-api</artifactId>\
            <version>1.10.2</version>\
            <scope>test</scope>\
        </dependency>\
        <dependency>\
            <groupId>org.junit.platform</groupId>\
            <artifactId>junit-platform-suite-engine</artifactId>\
            <version>1.10.2</version>\
            <scope>test</scope>\
        </dependency>' "${RINNA_DIR}/src/pom.xml"
fi

echo "Test import fixes completed at $(timestamp)"
echo "Now run: cd ${RINNA_DIR}/src && mvn test"
