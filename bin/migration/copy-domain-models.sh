#!/bin/bash
# copy-domain-models.sh
# Script to ensure all domain model and service files are available in src

set -e

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"
LOG_FILE="${RINNA_DIR}/bin/migration/copy-domain-models.log"

echo "Starting domain model copy at $(date)" > "${LOG_FILE}"

# Copy domain model classes
echo "Copying domain model classes..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/model"

# Copy all domain model files
if [ -d "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model" ]; then
    cp -f "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/domain/model/"*.java \
        "${RINNA_DIR}/src/main/java/org/rinna/domain/model/"
    echo "Copied domain model files" | tee -a "${LOG_FILE}"
fi

# Create a minimal pom.xml in src directory if it doesn't already exist
if [ ! -f "${RINNA_DIR}/src/pom.xml" ]; then
    echo "Creating minimal pom.xml in src directory..." | tee -a "${LOG_FILE}"
    cat > "${RINNA_DIR}/src/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.rinna</groupId>
        <artifactId>Rinna</artifactId>
        <version>1.3.0</version>
    </parent>
    <artifactId>rinna-src</artifactId>
    <packaging>jar</packaging>
    <name>Rinna Source</name>
    <dependencies>
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-java</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-junit-platform-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
EOF
fi

# Copy Rinna.java
if [ -f "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/Rinna.java" ]; then
    mkdir -p "${RINNA_DIR}/src/main/java/org/rinna"
    cp -f "${RINNA_DIR}/rinna-core/src/main/java/org/rinna/Rinna.java" \
        "${RINNA_DIR}/src/main/java/org/rinna/"
    echo "Copied Rinna.java" | tee -a "${LOG_FILE}"
fi

# We'll try a different approach - copy all Java sources from rinna-core to src
echo "Copying all source files from rinna-core to src..." | tee -a "${LOG_FILE}"
rsync -av --include="*.java" --include="*/" --exclude="*" \
    "${RINNA_DIR}/rinna-core/src/main/java/" \
    "${RINNA_DIR}/src/main/java/" >> "${LOG_FILE}" 2>&1

rsync -av --include="*.java" --include="*/" --exclude="*" \
    "${RINNA_DIR}/rinna-core/src/test/java/" \
    "${RINNA_DIR}/src/test/java/" >> "${LOG_FILE}" 2>&1

# Copy resources too
echo "Copying resources..." | tee -a "${LOG_FILE}"
mkdir -p "${RINNA_DIR}/src/main/resources"
if [ -d "${RINNA_DIR}/rinna-core/src/main/resources" ]; then
    rsync -av "${RINNA_DIR}/rinna-core/src/main/resources/" \
        "${RINNA_DIR}/src/main/resources/" >> "${LOG_FILE}" 2>&1
fi

mkdir -p "${RINNA_DIR}/src/test/resources"
if [ -d "${RINNA_DIR}/rinna-core/src/test/resources" ]; then
    rsync -av "${RINNA_DIR}/rinna-core/src/test/resources/" \
        "${RINNA_DIR}/src/test/resources/" >> "${LOG_FILE}" 2>&1
fi

echo "Domain model copy completed at $(date)" | tee -a "${LOG_FILE}"
echo "Try running tests with: cd ${RINNA_DIR}/src && mvn test"