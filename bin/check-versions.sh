#!/bin/bash
# Script to validate version consistency across the project

set -e

# Get the project root directory
PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
cd "$PROJECT_ROOT"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Get the expected version from version.properties
EXPECTED_VERSION=$(grep "^version=" version.properties | cut -d'=' -f2)

echo "Checking version consistency across the project..."
echo "Expected version: ${EXPECTED_VERSION}"
echo ""

# Initialize counters
INCONSISTENCIES=0
CHECKS=0

function check_version() {
    local file=$1
    local pattern=$2
    local line_filter=$3  # Optional: grep pattern to select specific lines
    local found_version

    if [ ! -f "$file" ]; then
        echo -e "${YELLOW}WARNING: File not found: $file${NC}"
        return 0
    fi

    if [ -z "$line_filter" ]; then
        # No line filter provided, use pattern directly
        found_version=$(grep -E "$pattern" "$file" | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
    else
        # Use line filter to select specific lines first
        found_version=$(grep -E "$line_filter" "$file" | grep -E "$pattern" | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
    fi
    
    if [ -z "$found_version" ]; then
        echo -e "${YELLOW}WARNING: Version pattern not found in $file${NC}"
        return 0
    fi

    CHECKS=$((CHECKS+1))
    
    if [ "$found_version" != "$EXPECTED_VERSION" ]; then
        echo -e "${RED}ERROR: Version mismatch in $file${NC}"
        echo -e "  Expected: $EXPECTED_VERSION"
        echo -e "  Found:    $found_version"
        INCONSISTENCIES=$((INCONSISTENCIES+1))
        return 1
    else
        echo -e "${GREEN}✓ $file: $found_version${NC}"
        return 0
    fi
}

# Check POM files
check_version "pom.xml" "<version>[0-9]+\.[0-9]+\.[0-9]+</version>" "groupId>org.rinna</groupId>"
check_version "rinna-core/pom.xml" "<version>[0-9]+\.[0-9]+\.[0-9]+</version>" "<parent>"

# Check Go version files
check_version "api/internal/version/version.go" "Version.*=.*\"[0-9]+\.[0-9]+\.[0-9]+\"" 
check_version "api/pkg/health/version.go" "Version.*=.*\"[0-9]+\.[0-9]+\.[0-9]+\"" 

# Check YAML config files
check_version "api/configs/config.yaml" "version:.*\"[0-9]+\.[0-9]+\.[0-9]+\"" "project:"

# Check documentation files
check_version "docs/development/configuration.md" "current project version is \*\*[0-9]+\.[0-9]+\.[0-9]+\*\*" 

# Print summary
echo ""
echo "Version check summary:"
echo "---------------------"
echo "Checks performed: $CHECKS"
echo "Inconsistencies found: $INCONSISTENCIES"

if [ $INCONSISTENCIES -eq 0 ]; then
    echo -e "${GREEN}All version references are consistent!${NC}"
    exit 0
else
    echo -e "${RED}Found version inconsistencies that need to be fixed.${NC}"
    exit 1
fi