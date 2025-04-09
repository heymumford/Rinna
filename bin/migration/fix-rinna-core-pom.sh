#!/bin/bash

# Fix duplicated dependencies in rinna-core POM file
# This script removes duplicated JUnit platform suite dependencies in the rinna-core module

# Timestamp for logging
echo "Starting rinna-core POM dependency cleanup at $(date)..."

# Set the project root directory
RINNA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Create backup of POM file
BACKUP_DIR="${RINNA_DIR}/backup/rinna-core-pom-fixes-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${BACKUP_DIR}"
cp "${RINNA_DIR}/rinna-core/pom.xml" "${BACKUP_DIR}/rinna-core-pom.xml"

echo "Fixing rinna-core POM file..."

# Create a temporary file to hold the fixed POM
TEMP_FILE=$(mktemp)

# Extract the first instance of each dependency block and remove duplicates
awk '
BEGIN { in_dep = 0; dep_count = 0; found_api = 0; found_engine = 0; }
/<dependency>/ { in_dep = 1; dep_start = NR; dep_lines = ""; }
in_dep == 1 { dep_lines = dep_lines "\n" $0; }
/<\/dependency>/ { 
    in_dep = 0; 
    if ($0 ~ /<\/dependency>/) {
        if (dep_lines ~ /junit-platform-suite-api/ && found_api == 0) {
            found_api = 1;
            print dep_lines;
        } else if (dep_lines ~ /junit-platform-suite-engine/ && found_engine == 0) {
            found_engine = 1;
            print dep_lines;
        } else if (dep_lines !~ /junit-platform-suite-api/ && dep_lines !~ /junit-platform-suite-engine/) {
            print dep_lines;
        }
    }
}
in_dep == 0 && !(/<dependency>/ || /<\/dependency>/) { print $0; }
' "${RINNA_DIR}/rinna-core/pom.xml" > "${TEMP_FILE}"

# Replace the original POM with the fixed version
mv "${TEMP_FILE}" "${RINNA_DIR}/rinna-core/pom.xml"

echo "rinna-core POM dependency cleanup completed at $(date)"
echo "The duplicate JUnit platform suite dependencies have been removed."
echo "You should now be able to build without duplicate warnings with: cd ${RINNA_DIR} && mvn clean compile -DskipTests -P skip-quality"