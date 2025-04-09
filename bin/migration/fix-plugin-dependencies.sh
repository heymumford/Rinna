#!/bin/bash

# Fix plugin dependencies in POM files
# This script fixes the scope of dependencies in the surefire plugin

# Start with timestamp for log
echo "Starting plugin dependency fixes at $(date)..."

# Set the project root directory
RINNA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Move into the project directory
cd "${RINNA_DIR}"

# Create backup of POM files
BACKUP_DIR="${RINNA_DIR}/backup/pom-plugin-fixes-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${BACKUP_DIR}"
cp "${RINNA_DIR}/pom.xml" "${BACKUP_DIR}/pom.xml"
cp "${RINNA_DIR}/rinna-core/pom.xml" "${BACKUP_DIR}/rinna-core-pom.xml" 2>/dev/null || true

echo "Fixing plugin dependencies in main POM file..."

# Fix 1: Remove test scope from plugin dependencies
sed -i 's/<dependency>\n            <groupId>org.junit.platform<\/groupId>\n            <artifactId>junit-platform-suite-api<\/artifactId>\n            <version>1.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/<dependency>\n                        <groupId>org.junit.platform<\/groupId>\n                        <artifactId>junit-platform-suite-api<\/artifactId>\n                        <version>1.10.2<\/version>\n                    <\/dependency>/g' "${RINNA_DIR}/pom.xml"

sed -i 's/<dependency>\n            <groupId>org.junit.platform<\/groupId>\n            <artifactId>junit-platform-suite-engine<\/artifactId>\n            <version>1.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/<dependency>\n                        <groupId>org.junit.platform<\/groupId>\n                        <artifactId>junit-platform-suite-engine<\/artifactId>\n                        <version>1.10.2<\/version>\n                    <\/dependency>/g' "${RINNA_DIR}/pom.xml"

# Fix 2: Remove duplicated dependencies
# First, count how many times junit-platform-suite-api appears
API_COUNT=$(grep -c "junit-platform-suite-api" "${RINNA_DIR}/pom.xml")
ENGINE_COUNT=$(grep -c "junit-platform-suite-engine" "${RINNA_DIR}/pom.xml")

echo "Found $API_COUNT junit-platform-suite-api dependencies"
echo "Found $ENGINE_COUNT junit-platform-suite-engine dependencies"

# If there are duplicates, keep only one copy in the main dependencies
if [ "$API_COUNT" -gt 2 ]; then
    # Remove all but the first occurrence of the junit-platform-suite-api dependency
    sed -i '/<dependency>.*junit-platform-suite-api/{n;n;n;n;n; s/.*//g; s/^/<!-- REMOVED DUPLICATE DEPENDENCY -->/ }' "${RINNA_DIR}/pom.xml"
fi

if [ "$ENGINE_COUNT" -gt 2 ]; then
    # Remove all but the first occurrence of the junit-platform-suite-engine dependency
    sed -i '/<dependency>.*junit-platform-suite-engine/{n;n;n;n;n; s/.*//g; s/^/<!-- REMOVED DUPLICATE DEPENDENCY -->/ }' "${RINNA_DIR}/pom.xml"
fi

# Fix the rinna-core POM file if it exists
if [ -f "${RINNA_DIR}/rinna-core/pom.xml" ]; then
    echo "Fixing plugin dependencies in rinna-core POM file..."
    
    sed -i 's/<dependency>\n            <groupId>org.junit.platform<\/groupId>\n            <artifactId>junit-platform-suite-api<\/artifactId>\n            <version>1.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/<dependency>\n                        <groupId>org.junit.platform<\/groupId>\n                        <artifactId>junit-platform-suite-api<\/artifactId>\n                        <version>1.10.2<\/version>\n                    <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"

    sed -i 's/<dependency>\n            <groupId>org.junit.platform<\/groupId>\n            <artifactId>junit-platform-suite-engine<\/artifactId>\n            <version>1.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/<dependency>\n                        <groupId>org.junit.platform<\/groupId>\n                        <artifactId>junit-platform-suite-engine<\/artifactId>\n                        <version>1.10.2<\/version>\n                    <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"
    
    # Remove duplicates
    API_COUNT=$(grep -c "junit-platform-suite-api" "${RINNA_DIR}/rinna-core/pom.xml")
    ENGINE_COUNT=$(grep -c "junit-platform-suite-engine" "${RINNA_DIR}/rinna-core/pom.xml")
    
    echo "Found $API_COUNT junit-platform-suite-api dependencies in rinna-core"
    echo "Found $ENGINE_COUNT junit-platform-suite-engine dependencies in rinna-core"
    
    if [ "$API_COUNT" -gt 2 ]; then
        sed -i '/<dependency>.*junit-platform-suite-api/{n;n;n;n;n; s/.*//g; s/^/<!-- REMOVED DUPLICATE DEPENDENCY -->/ }' "${RINNA_DIR}/rinna-core/pom.xml"
    fi
    
    if [ "$ENGINE_COUNT" -gt 2 ]; then
        sed -i '/<dependency>.*junit-platform-suite-engine/{n;n;n;n;n; s/.*//g; s/^/<!-- REMOVED DUPLICATE DEPENDENCY -->/ }' "${RINNA_DIR}/rinna-core/pom.xml"
    fi
fi

echo "Plugin dependency fixes completed at $(date)"
echo "To test the changes, run: cd ${RINNA_DIR} && mvn clean compile -P skip-quality"