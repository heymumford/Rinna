#!/bin/bash

# Fix duplicated dependencies in POM files
# This script cleans up duplicated dependencies in the Maven POM files

# Start with timestamp for log
echo "Starting POM dependency cleanup at $(date)..."

# Set the project root directory
RINNA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Move into the project directory
cd "${RINNA_DIR}"

# Create backup of POM files
BACKUP_DIR="${RINNA_DIR}/backup/pom-backups-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${BACKUP_DIR}"
cp "${RINNA_DIR}/pom.xml" "${BACKUP_DIR}/pom.xml"
cp "${RINNA_DIR}/rinna-core/pom.xml" "${BACKUP_DIR}/rinna-core-pom.xml" 2>/dev/null || true
cp "${RINNA_DIR}/rinna-cli/pom.xml" "${BACKUP_DIR}/rinna-cli-pom.xml" 2>/dev/null || true

echo "Cleaning up duplicated dependencies in main POM file..."

# Clean up duplicated JUnit dependencies 
sed -i '/<dependency>.*\n.*junit-platform-suite-api.*\n.*<version>1.10.2<\/version>.*\n.*\n.*<\/dependency>/{N;N;N;N;d}' "${RINNA_DIR}/pom.xml"
sed -i '/<dependency>.*\n.*junit-platform-suite-engine.*\n.*<version>1.10.2<\/version>.*\n.*\n.*<\/dependency>/{N;N;N;N;d}' "${RINNA_DIR}/pom.xml"

# Clean up duplicated dependencies in plugin sections
sed -i '/<dependency>.*\n.*junit-platform-suite-api.*\n.*<version>1.10.2<\/version>.*\n.*\n.*<\/dependency>/{N;N;N;N;d}' "${RINNA_DIR}/pom.xml"
sed -i '/<dependency>.*\n.*junit-platform-suite-engine.*\n.*<version>1.10.2<\/version>.*\n.*\n.*<\/dependency>/{N;N;N;N;d}' "${RINNA_DIR}/pom.xml"

# Add proper JUnit platform dependencies with test scope
if ! grep -q "<groupId>org.junit.platform</groupId>.*<artifactId>junit-platform-suite-api</artifactId>.*<scope>test</scope>" "${RINNA_DIR}/pom.xml"; then
    sed -i '/<dependencies>/a \
        <!-- JUnit platform suite for tests -->\
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
        </dependency>' "${RINNA_DIR}/pom.xml"
fi

# Add scope to JUnit Jupiter dependencies if missing
sed -i 's/<groupId>org.junit.jupiter<\/groupId>.*<artifactId>junit-jupiter-api<\/artifactId>.*<version>5.10.2<\/version>.*<\/dependency>/<groupId>org.junit.jupiter<\/groupId>\n            <artifactId>junit-jupiter-api<\/artifactId>\n            <version>5.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"
sed -i 's/<groupId>org.junit.jupiter<\/groupId>.*<artifactId>junit-jupiter-engine<\/artifactId>.*<version>5.10.2<\/version>.*<\/dependency>/<groupId>org.junit.jupiter<\/groupId>\n            <artifactId>junit-jupiter-engine<\/artifactId>\n            <version>5.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"
sed -i 's/<groupId>org.junit.jupiter<\/groupId>.*<artifactId>junit-jupiter-params<\/artifactId>.*<version>5.10.2<\/version>.*<\/dependency>/<groupId>org.junit.jupiter<\/groupId>\n            <artifactId>junit-jupiter-params<\/artifactId>\n            <version>5.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"

# Add scope to Mockito dependencies if missing
sed -i 's/<groupId>org.mockito<\/groupId>.*<artifactId>mockito-core<\/artifactId>.*<version>5.17.0<\/version>.*<\/dependency>/<groupId>org.mockito<\/groupId>\n            <artifactId>mockito-core<\/artifactId>\n            <version>5.17.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"
sed -i 's/<groupId>org.mockito<\/groupId>.*<artifactId>mockito-junit-jupiter<\/artifactId>.*<version>5.17.0<\/version>.*<\/dependency>/<groupId>org.mockito<\/groupId>\n            <artifactId>mockito-junit-jupiter<\/artifactId>\n            <version>5.17.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"

# Add scope to Cucumber dependencies if missing
sed -i 's/<groupId>io.cucumber<\/groupId>.*<artifactId>cucumber-java<\/artifactId>.*<version>7.22.0<\/version>.*<\/dependency>/<groupId>io.cucumber<\/groupId>\n            <artifactId>cucumber-java<\/artifactId>\n            <version>7.22.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"
sed -i 's/<groupId>io.cucumber<\/groupId>.*<artifactId>cucumber-junit-platform-engine<\/artifactId>.*<version>7.22.0<\/version>.*<\/dependency>/<groupId>io.cucumber<\/groupId>\n            <artifactId>cucumber-junit-platform-engine<\/artifactId>\n            <version>7.22.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"
sed -i 's/<groupId>io.cucumber<\/groupId>.*<artifactId>cucumber-spring<\/artifactId>.*<version>7.22.0<\/version>.*<\/dependency>/<groupId>io.cucumber<\/groupId>\n            <artifactId>cucumber-spring<\/artifactId>\n            <version>7.22.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"

# Add scope to AssertJ dependency if missing
sed -i 's/<groupId>org.assertj<\/groupId>.*<artifactId>assertj-core<\/artifactId>.*<version>3.25.3<\/version>.*<\/dependency>/<groupId>org.assertj<\/groupId>\n            <artifactId>assertj-core<\/artifactId>\n            <version>3.25.3<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/pom.xml"

# Now clean up rinna-core POM file
if [ -f "${RINNA_DIR}/rinna-core/pom.xml" ]; then
    echo "Cleaning up duplicated dependencies in rinna-core POM file..."

    # Clean up duplicated JUnit dependencies
    sed -i '/<dependency>.*\n.*junit-platform-suite-api.*\n.*<version>1.10.2<\/version>.*\n.*\n.*<\/dependency>/{N;N;N;N;d}' "${RINNA_DIR}/rinna-core/pom.xml"
    sed -i '/<dependency>.*\n.*junit-platform-suite-engine.*\n.*<version>1.10.2<\/version>.*\n.*\n.*<\/dependency>/{N;N;N;N;d}' "${RINNA_DIR}/rinna-core/pom.xml"

    # Add proper JUnit platform dependencies with test scope
    if ! grep -q "<groupId>org.junit.platform</groupId>.*<artifactId>junit-platform-suite-api</artifactId>.*<scope>test</scope>" "${RINNA_DIR}/rinna-core/pom.xml"; then
        sed -i '/<dependencies>/a \
            <!-- JUnit platform suite for tests -->\
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
            </dependency>' "${RINNA_DIR}/rinna-core/pom.xml"
    fi

    # Add scope to JUnit Jupiter dependencies if missing
    sed -i 's/<groupId>org.junit.jupiter<\/groupId>.*<artifactId>junit-jupiter-api<\/artifactId>.*<version>5.10.2<\/version>.*<\/dependency>/<groupId>org.junit.jupiter<\/groupId>\n            <artifactId>junit-jupiter-api<\/artifactId>\n            <version>5.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"
    sed -i 's/<groupId>org.junit.jupiter<\/groupId>.*<artifactId>junit-jupiter-engine<\/artifactId>.*<version>5.10.2<\/version>.*<\/dependency>/<groupId>org.junit.jupiter<\/groupId>\n            <artifactId>junit-jupiter-engine<\/artifactId>\n            <version>5.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"
    sed -i 's/<groupId>org.junit.jupiter<\/groupId>.*<artifactId>junit-jupiter-params<\/artifactId>.*<version>5.10.2<\/version>.*<\/dependency>/<groupId>org.junit.jupiter<\/groupId>\n            <artifactId>junit-jupiter-params<\/artifactId>\n            <version>5.10.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"

    # Add scope to Mockito dependencies if missing
    sed -i 's/<groupId>org.mockito<\/groupId>.*<artifactId>mockito-core<\/artifactId>.*<version>5.17.0<\/version>.*<\/dependency>/<groupId>org.mockito<\/groupId>\n            <artifactId>mockito-core<\/artifactId>\n            <version>5.17.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"
    sed -i 's/<groupId>org.mockito<\/groupId>.*<artifactId>mockito-junit-jupiter<\/artifactId>.*<version>5.17.0<\/version>.*<\/dependency>/<groupId>org.mockito<\/groupId>\n            <artifactId>mockito-junit-jupiter<\/artifactId>\n            <version>5.17.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"

    # Add scope to JUnit 4 dependency if missing (rinna-core uses both JUnit 4 and 5)
    sed -i 's/<groupId>junit<\/groupId>.*<artifactId>junit<\/artifactId>.*<version>4.13.2<\/version>.*<\/dependency>/<groupId>junit<\/groupId>\n            <artifactId>junit<\/artifactId>\n            <version>4.13.2<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"

    # Add scope to Cucumber dependencies if missing
    sed -i 's/<groupId>io.cucumber<\/groupId>.*<artifactId>cucumber-java<\/artifactId>.*<version>7.22.0<\/version>.*<\/dependency>/<groupId>io.cucumber<\/groupId>\n            <artifactId>cucumber-java<\/artifactId>\n            <version>7.22.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"
    sed -i 's/<groupId>io.cucumber<\/groupId>.*<artifactId>cucumber-junit<\/artifactId>.*<version>7.22.0<\/version>.*<\/dependency>/<groupId>io.cucumber<\/groupId>\n            <artifactId>cucumber-junit<\/artifactId>\n            <version>7.22.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"
    sed -i 's/<groupId>io.cucumber<\/groupId>.*<artifactId>cucumber-junit-platform-engine<\/artifactId>.*<version>7.22.0<\/version>.*<\/dependency>/<groupId>io.cucumber<\/groupId>\n            <artifactId>cucumber-junit-platform-engine<\/artifactId>\n            <version>7.22.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"
    sed -i 's/<groupId>io.cucumber<\/groupId>.*<artifactId>cucumber-spring<\/artifactId>.*<version>7.22.0<\/version>.*<\/dependency>/<groupId>io.cucumber<\/groupId>\n            <artifactId>cucumber-spring<\/artifactId>\n            <version>7.22.0<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"

    # Add scope to AssertJ dependency if missing
    sed -i 's/<groupId>org.assertj<\/groupId>.*<artifactId>assertj-core<\/artifactId>.*<version>3.25.3<\/version>.*<\/dependency>/<groupId>org.assertj<\/groupId>\n            <artifactId>assertj-core<\/artifactId>\n            <version>3.25.3<\/version>\n            <scope>test<\/scope>\n        <\/dependency>/g' "${RINNA_DIR}/rinna-core/pom.xml"
fi

echo "POM dependency cleanup completed at $(date)"
echo "You should now be able to build with: cd ${RINNA_DIR} && mvn clean install -DskipTests"