#!/bin/bash

# Fix duplicated dependencies in POM files using XMLStarlet
# This script cleans up duplicated dependencies in the Maven POM files
# using XMLStarlet for precise XML manipulation

# Start with timestamp for log
echo "Starting POM dependency cleanup at $(date)..."

# Set the project root directory
RINNA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Move into the project directory
cd "${RINNA_DIR}"

# Source the XML tools library
source "${RINNA_DIR}/bin/xml-tools.sh"

# Create backup of POM files
BACKUP_DIR="${RINNA_DIR}/backup/pom-backups-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${BACKUP_DIR}"
cp "${RINNA_DIR}/pom.xml" "${BACKUP_DIR}/pom.xml"
cp "${RINNA_DIR}/rinna-core/pom.xml" "${BACKUP_DIR}/rinna-core-pom.xml" 2>/dev/null || true
cp "${RINNA_DIR}/rinna-cli/pom.xml" "${BACKUP_DIR}/rinna-cli-pom.xml" 2>/dev/null || true

echo "Cleaning up dependencies in main POM file..."

# Handle JUnit platform dependencies
process_junit_platform() {
    local pom_file="$1"
    echo "Processing JUnit platform dependencies in $pom_file"
    
    # Remove duplicate JUnit platform dependencies
    local junit_platform_deps=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
        -v "count(//pom:dependency[pom:groupId='org.junit.platform' and pom:artifactId='junit-platform-suite-api'])" \
        "${pom_file}")
    
    if [ "$junit_platform_deps" -gt 1 ]; then
        echo "  Removing duplicate junit-platform-suite-api dependencies"
        # Keep just one by removing all and then adding one back
        xml_remove_dependency "${pom_file}" "org.junit.platform" "junit-platform-suite-api"
        xml_add_dependency "${pom_file}" "org.junit.platform" "junit-platform-suite-api" "1.10.2" "test"
    elif [ "$junit_platform_deps" -eq 1 ]; then
        # Add test scope if missing
        echo "  Ensuring junit-platform-suite-api has test scope"
        xml_add_test_scope "${pom_file}" "org.junit.platform" "junit-platform-suite-api"
    else
        # Add the dependency with test scope
        echo "  Adding junit-platform-suite-api with test scope"
        xml_add_dependency "${pom_file}" "org.junit.platform" "junit-platform-suite-api" "1.10.2" "test"
    fi
    
    # Do the same for junit-platform-suite-engine
    local junit_platform_engine_deps=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
        -v "count(//pom:dependency[pom:groupId='org.junit.platform' and pom:artifactId='junit-platform-suite-engine'])" \
        "${pom_file}")
    
    if [ "$junit_platform_engine_deps" -gt 1 ]; then
        echo "  Removing duplicate junit-platform-suite-engine dependencies"
        xml_remove_dependency "${pom_file}" "org.junit.platform" "junit-platform-suite-engine"
        xml_add_dependency "${pom_file}" "org.junit.platform" "junit-platform-suite-engine" "1.10.2" "test"
    elif [ "$junit_platform_engine_deps" -eq 1 ]; then
        echo "  Ensuring junit-platform-suite-engine has test scope"
        xml_add_test_scope "${pom_file}" "org.junit.platform" "junit-platform-suite-engine"
    else
        echo "  Adding junit-platform-suite-engine with test scope"
        xml_add_dependency "${pom_file}" "org.junit.platform" "junit-platform-suite-engine" "1.10.2" "test"
    fi
}

# Handle JUnit Jupiter dependencies
process_junit_jupiter() {
    local pom_file="$1"
    echo "Processing JUnit Jupiter dependencies in $pom_file"
    
    # JUnit Jupiter artifacts to process
    local jupiter_artifacts=("junit-jupiter-api" "junit-jupiter-engine" "junit-jupiter-params")
    
    for artifact in "${jupiter_artifacts[@]}"; do
        local jupiter_deps=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
            -v "count(//pom:dependency[pom:groupId='org.junit.jupiter' and pom:artifactId='${artifact}'])" \
            "${pom_file}")
        
        if [ "$jupiter_deps" -gt 1 ]; then
            echo "  Removing duplicate ${artifact} dependencies"
            xml_remove_dependency "${pom_file}" "org.junit.jupiter" "${artifact}"
            xml_add_dependency "${pom_file}" "org.junit.jupiter" "${artifact}" "5.10.2" "test"
        elif [ "$jupiter_deps" -eq 1 ]; then
            echo "  Ensuring ${artifact} has test scope"
            xml_add_test_scope "${pom_file}" "org.junit.jupiter" "${artifact}"
        else
            echo "  Adding ${artifact} with test scope"
            xml_add_dependency "${pom_file}" "org.junit.jupiter" "${artifact}" "5.10.2" "test"
        fi
    done
}

# Handle Mockito dependencies
process_mockito() {
    local pom_file="$1"
    echo "Processing Mockito dependencies in $pom_file"
    
    # Mockito artifacts to process
    local mockito_artifacts=("mockito-core" "mockito-junit-jupiter")
    
    for artifact in "${mockito_artifacts[@]}"; do
        local mockito_deps=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
            -v "count(//pom:dependency[pom:groupId='org.mockito' and pom:artifactId='${artifact}'])" \
            "${pom_file}")
        
        if [ "$mockito_deps" -gt 1 ]; then
            echo "  Removing duplicate ${artifact} dependencies"
            xml_remove_dependency "${pom_file}" "org.mockito" "${artifact}"
            xml_add_dependency "${pom_file}" "org.mockito" "${artifact}" "5.17.0" "test"
        elif [ "$mockito_deps" -eq 1 ]; then
            echo "  Ensuring ${artifact} has test scope"
            xml_add_test_scope "${pom_file}" "org.mockito" "${artifact}"
        else
            echo "  Adding ${artifact} with test scope"
            xml_add_dependency "${pom_file}" "org.mockito" "${artifact}" "5.17.0" "test"
        fi
    done
}

# Handle Cucumber dependencies
process_cucumber() {
    local pom_file="$1"
    echo "Processing Cucumber dependencies in $pom_file"
    
    # Cucumber artifacts to process
    local cucumber_artifacts=("cucumber-java" "cucumber-junit" "cucumber-junit-platform-engine" "cucumber-spring")
    
    for artifact in "${cucumber_artifacts[@]}"; do
        local cucumber_deps=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
            -v "count(//pom:dependency[pom:groupId='io.cucumber' and pom:artifactId='${artifact}'])" \
            "${pom_file}")
        
        if [ "$cucumber_deps" -gt 1 ]; then
            echo "  Removing duplicate ${artifact} dependencies"
            xml_remove_dependency "${pom_file}" "io.cucumber" "${artifact}"
            xml_add_dependency "${pom_file}" "io.cucumber" "${artifact}" "7.22.0" "test"
        elif [ "$cucumber_deps" -eq 1 ]; then
            echo "  Ensuring ${artifact} has test scope"
            xml_add_test_scope "${pom_file}" "io.cucumber" "${artifact}"
        fi
    done
}

# Handle AssertJ dependencies
process_assertj() {
    local pom_file="$1"
    echo "Processing AssertJ dependencies in $pom_file"
    
    local assertj_deps=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
        -v "count(//pom:dependency[pom:groupId='org.assertj' and pom:artifactId='assertj-core'])" \
        "${pom_file}")
    
    if [ "$assertj_deps" -gt 1 ]; then
        echo "  Removing duplicate assertj-core dependencies"
        xml_remove_dependency "${pom_file}" "org.assertj" "assertj-core"
        xml_add_dependency "${pom_file}" "org.assertj" "assertj-core" "3.25.3" "test"
    elif [ "$assertj_deps" -eq 1 ]; then
        echo "  Ensuring assertj-core has test scope"
        xml_add_test_scope "${pom_file}" "org.assertj" "assertj-core"
    fi
}

# Process the parent POM file
process_pom() {
    local pom_file="$1"
    echo "Processing $pom_file..."
    
    # Process different types of dependencies
    process_junit_platform "$pom_file"
    process_junit_jupiter "$pom_file"
    process_mockito "$pom_file"
    process_cucumber "$pom_file"
    process_assertj "$pom_file"
    
    # Format the XML file for readability
    xml_format_pom "$pom_file"
    
    echo "Finished processing $pom_file"
}

# Process main POM file
process_pom "${RINNA_DIR}/pom.xml"

# Process rinna-core POM file
if [ -f "${RINNA_DIR}/rinna-core/pom.xml" ]; then
    process_pom "${RINNA_DIR}/rinna-core/pom.xml"
fi

# Process rinna-cli POM file
if [ -f "${RINNA_DIR}/rinna-cli/pom.xml" ]; then
    process_pom "${RINNA_DIR}/rinna-cli/pom.xml"
fi

echo "POM dependency cleanup completed at $(date)"
echo "You should now be able to build with: cd ${RINNA_DIR} && mvn clean install -DskipTests"