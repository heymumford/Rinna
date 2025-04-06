#!/bin/bash
# Enhanced version consistency checker for Rinna cross-language architecture

set -e

# Get the project root directory
PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
cd "$PROJECT_ROOT"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the expected version from version.properties
if [ ! -f "version.properties" ]; then
    echo -e "${RED}ERROR: version.properties file not found!${NC}"
    exit 1
fi

EXPECTED_VERSION=$(grep "^version=" version.properties | cut -d'=' -f2)

if [[ -z "$EXPECTED_VERSION" ]]; then
    echo -e "${RED}ERROR: Could not extract version from version.properties!${NC}"
    exit 1
fi

echo -e "${BLUE}=== Rinna Version Check Tool ===${NC}"
echo "Checking version consistency across Java, Go, and Python..."
echo "Expected version from version.properties: ${GREEN}${EXPECTED_VERSION}${NC}"
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

# Function to check all POM files in the project
function check_all_pom_files() {
    echo -e "${BLUE}Checking Java/Maven...${NC}"
    
    # Find all pom.xml files in the project
    local pom_files=$(find "$PROJECT_ROOT" -name "pom.xml" -type f)
    
    for pom in $pom_files; do
        # Check if it's a Rinna project (contains org.rinna groupId)
        if grep -q "<groupId>org.rinna</groupId>" "$pom"; then
            # Check project version
            check_version "$pom" "<version>[0-9]+\.[0-9]+\.[0-9]+</version>" "groupId>org.rinna</groupId>"
            
            # Check parent version if it references org.rinna
            if grep -q "<parent>" "$pom" && grep -q "<groupId>org.rinna</groupId>" "$pom"; then
                check_version "$pom" "<version>[0-9]+\.[0-9]+\.[0-9]+</version>" "<parent>"
            fi
        fi
    done
}

# Check all Go version files in the project
function check_go_files() {
    echo -e "${BLUE}Checking Go...${NC}"

    # Check specific Go version files we know about
    check_version "api/internal/version/version.go" "Version[[:space:]]*=[[:space:]]*\"[0-9]+\.[0-9]+\.[0-9]+\"" 
    check_version "api/pkg/health/version.go" "Version[[:space:]]*=[[:space:]]*\"[0-9]+\.[0-9]+\.[0-9]+\"" 
    
    # Find any additional version.go files
    local additional_files=$(find "$PROJECT_ROOT/api" -name "version.go" -type f | grep -v -E "/(internal/version|pkg/health)/")
    
    for file in $additional_files; do
        check_version "$file" "Version[[:space:]]*=[[:space:]]*\"[0-9]+\.[0-9]+\.[0-9]+\"" 
    done
    
    # Check YAML config files
    check_version "api/configs/config.yaml" "version:.*\"[0-9]+\.[0-9]+\.[0-9]+\"" "project:"
}

# Check Python files
function check_python_files() {
    echo -e "${BLUE}Checking Python...${NC}"
    
    # Check pyproject.toml
    check_version "pyproject.toml" "version[[:space:]]*=[[:space:]]*\"[0-9]+\.[0-9]+\.[0-9]+\""
    
    # Check Python version files
    check_version "python/rinna/__init__.py" "__version__[[:space:]]*=[[:space:]]*[\"'][0-9]+\.[0-9]+\.[0-9]+[\"']"
    check_version "bin/rinna_config.py" "VERSION[[:space:]]*=[[:space:]]*[\"'][0-9]+\.[0-9]+\.[0-9]+[\"']"
    
    # Check virtual env version file
    if [ -f ".venv/version" ]; then
        local venv_version=$(cat ".venv/version" | tr -d '[:space:]')
        CHECKS=$((CHECKS+1))
        
        if [ "$venv_version" != "$EXPECTED_VERSION" ]; then
            echo -e "${RED}ERROR: Version mismatch in .venv/version${NC}"
            echo -e "  Expected: $EXPECTED_VERSION"
            echo -e "  Found:    $venv_version"
            INCONSISTENCIES=$((INCONSISTENCIES+1))
        else
            echo -e "${GREEN}✓ .venv/version: $venv_version${NC}"
        fi
    fi
}

# Check documentation files
function check_documentation() {
    echo -e "${BLUE}Checking Documentation...${NC}"
    
    # Common documentation files that might contain version numbers
    check_version "README.md" "badge/version-[0-9]+\.[0-9]+\.[0-9]+"
    check_version "docs/development/configuration.md" "current project version is \*\*[0-9]+\.[0-9]+\.[0-9]+\*\*"
    check_version "docs/user-guide/README.md" "[Vv]ersion[[:space:]]*[0-9]+\.[0-9]+\.[0-9]+"
}

# Run all checks
check_all_pom_files
check_go_files
check_python_files
check_documentation

# Print summary
echo ""
echo -e "${BLUE}Version check summary:${NC}"
echo "---------------------"
echo "Checks performed: $CHECKS"
echo "Inconsistencies found: $INCONSISTENCIES"

if [ $INCONSISTENCIES -eq 0 ]; then
    echo -e "${GREEN}✓ All version references are consistent across Java, Go, and Python: $EXPECTED_VERSION${NC}"
    exit 0
else
    echo -e "${RED}✗ Found version inconsistencies. Run 'bin/rin-version update' to fix them.${NC}"
    exit 1
fi