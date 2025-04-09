#!/bin/bash
# Advanced test discovery utility for Rinna
# 
# This script scans the project for tests and creates a detailed report
# of all available tests by category, helping with test organization and discovery

set -e

# Color definitions for output
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
BLUE="\033[0;34m"
RED="\033[0;31m"
NC="\033[0m" # No Color
BOLD="\033[1m"

# Get the project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Create a temporary directory in target for test discovery output
TEMP_DIR="${SCRIPT_DIR}/../target/test-discovery-temp"
mkdir -p "$TEMP_DIR"
trap 'rm -rf "$TEMP_DIR"' EXIT

# Output files
UNIT_TESTS="$TEMP_DIR/unit-tests.txt"
COMPONENT_TESTS="$TEMP_DIR/component-tests.txt"
INTEGRATION_TESTS="$TEMP_DIR/integration-tests.txt"
ACCEPTANCE_TESTS="$TEMP_DIR/acceptance-tests.txt"
PERFORMANCE_TESTS="$TEMP_DIR/performance-tests.txt"
BDD_FEATURES="$TEMP_DIR/bdd-features.txt"

# Function to print a section header
print_header() {
    echo -e "\n${BLUE}${BOLD}$1${NC}\n"
    echo "========================================"
}

# Function to count tests in a file
count_tests() {
    if [ ! -s "$1" ]; then
        echo "0"
    else
        wc -l < "$1" | tr -d ' '
    fi
}

# Discover tests by annotations (@Tag)
print_header "Discovering tests by annotations..."

echo "Looking for unit tests (@Tag(\"unit\"))..."
find "$PROJECT_ROOT" -name "*.java" -type f -exec grep -l "@Tag.*unit" {} \; > "$UNIT_TESTS"

echo "Looking for component tests (@Tag(\"component\"))..."
find "$PROJECT_ROOT" -name "*.java" -type f -exec grep -l "@Tag.*component" {} \; > "$COMPONENT_TESTS"

echo "Looking for integration tests (@Tag(\"integration\"))..."
find "$PROJECT_ROOT" -name "*.java" -type f -exec grep -l "@Tag.*integration" {} \; > "$INTEGRATION_TESTS"

echo "Looking for acceptance tests (@Tag(\"acceptance\"))..."
find "$PROJECT_ROOT" -name "*.java" -type f -exec grep -l "@Tag.*acceptance" {} \; > "$ACCEPTANCE_TESTS"

echo "Looking for performance tests (@Tag(\"performance\"))..."
find "$PROJECT_ROOT" -name "*.java" -type f -exec grep -l "@Tag.*performance" {} \; > "$PERFORMANCE_TESTS"

# Discover tests by naming convention
print_header "Discovering tests by naming convention..."

echo "Looking for unit tests (*Test.java)..."
find "$PROJECT_ROOT" -name "*Test.java" -not -name "*Integration*" -not -name "*Component*" -not -name "*Perf*" -not -path "*/target/*" >> "$UNIT_TESTS"

echo "Looking for component tests (*ComponentTest.java)..."
find "$PROJECT_ROOT" -name "*ComponentTest.java" -not -path "*/target/*" >> "$COMPONENT_TESTS"

echo "Looking for integration tests (*IntegrationTest.java)..."
find "$PROJECT_ROOT" -name "*IntegrationTest.java" -not -path "*/target/*" >> "$INTEGRATION_TESTS"

echo "Looking for performance tests (*PerfTest.java)..."
find "$PROJECT_ROOT" -name "*PerfTest.java" -not -path "*/target/*" >> "$PERFORMANCE_TESTS"

# Discover BDD features
print_header "Discovering BDD features..."

echo "Looking for Cucumber feature files..."
find "$PROJECT_ROOT" -name "*.feature" -not -path "*/target/*" > "$BDD_FEATURES"

# Remove duplicates
sort -u "$UNIT_TESTS" -o "$UNIT_TESTS"
sort -u "$COMPONENT_TESTS" -o "$COMPONENT_TESTS"
sort -u "$INTEGRATION_TESTS" -o "$INTEGRATION_TESTS"
sort -u "$ACCEPTANCE_TESTS" -o "$ACCEPTANCE_TESTS"
sort -u "$PERFORMANCE_TESTS" -o "$PERFORMANCE_TESTS"
sort -u "$BDD_FEATURES" -o "$BDD_FEATURES"

# Count tests
UNIT_COUNT=$(count_tests "$UNIT_TESTS")
COMPONENT_COUNT=$(count_tests "$COMPONENT_TESTS")
INTEGRATION_COUNT=$(count_tests "$INTEGRATION_TESTS")
ACCEPTANCE_COUNT=$(count_tests "$ACCEPTANCE_TESTS")
PERFORMANCE_COUNT=$(count_tests "$PERFORMANCE_TESTS")
BDD_COUNT=$(count_tests "$BDD_FEATURES")

TOTAL_COUNT=$((UNIT_COUNT + COMPONENT_COUNT + INTEGRATION_COUNT + ACCEPTANCE_COUNT + PERFORMANCE_COUNT + BDD_COUNT))

# Print summary
print_header "Test Discovery Summary"

echo -e "${GREEN}${BOLD}Found $TOTAL_COUNT tests across all categories:${NC}"
echo -e "${BLUE}Unit Tests:${NC} $UNIT_COUNT files"
echo -e "${BLUE}Component Tests:${NC} $COMPONENT_COUNT files"
echo -e "${BLUE}Integration Tests:${NC} $INTEGRATION_COUNT files"
echo -e "${BLUE}Acceptance Tests:${NC} $ACCEPTANCE_COUNT files"
echo -e "${BLUE}Performance Tests:${NC} $PERFORMANCE_COUNT files"
echo -e "${BLUE}BDD Feature Files:${NC} $BDD_COUNT files"

# Print detailed report if requested
if [ "$1" == "--detailed" ] || [ "$1" == "-d" ]; then
    print_header "Detailed Test Report"
    
    if [ "$UNIT_COUNT" -gt 0 ]; then
        echo -e "${BOLD}Unit Tests:${NC}"
        cat "$UNIT_TESTS" | while read -r line; do
            echo "  - $line"
        done
        echo ""
    fi
    
    if [ "$COMPONENT_COUNT" -gt 0 ]; then
        echo -e "${BOLD}Component Tests:${NC}"
        cat "$COMPONENT_TESTS" | while read -r line; do
            echo "  - $line"
        done
        echo ""
    fi
    
    if [ "$INTEGRATION_COUNT" -gt 0 ]; then
        echo -e "${BOLD}Integration Tests:${NC}"
        cat "$INTEGRATION_TESTS" | while read -r line; do
            echo "  - $line"
        done
        echo ""
    fi
    
    if [ "$ACCEPTANCE_COUNT" -gt 0 ]; then
        echo -e "${BOLD}Acceptance Tests:${NC}"
        cat "$ACCEPTANCE_TESTS" | while read -r line; do
            echo "  - $line"
        done
        echo ""
    fi
    
    if [ "$PERFORMANCE_COUNT" -gt 0 ]; then
        echo -e "${BOLD}Performance Tests:${NC}"
        cat "$PERFORMANCE_TESTS" | while read -r line; do
            echo "  - $line"
        done
        echo ""
    fi
    
    if [ "$BDD_COUNT" -gt 0 ]; then
        echo -e "${BOLD}BDD Feature Files:${NC}"
        cat "$BDD_FEATURES" | while read -r line; do
            echo "  - $line"
        done
        echo ""
    fi
fi

# Generate Maven test execution commands
print_header "Maven Test Commands"

echo -e "${YELLOW}To run all tests:${NC}"
echo "mvn clean test"
echo ""

echo -e "${YELLOW}To run unit tests only:${NC}"
echo "mvn clean test -P unit-tests"
echo ""

echo -e "${YELLOW}To run component tests only:${NC}"
echo "mvn clean test -P component-tests"
echo ""

echo -e "${YELLOW}To run integration tests only:${NC}"
echo "mvn clean test -P integration-tests"
echo ""

echo -e "${YELLOW}To run acceptance tests only:${NC}"
echo "mvn clean test -P acceptance-tests"
echo ""

echo -e "${YELLOW}To run performance tests only:${NC}"
echo "mvn clean test -P performance-tests"
echo ""

# Generate CLI commands
print_header "CLI Commands"

echo -e "${YELLOW}To run all tests:${NC}"
echo "./bin/rin test"
echo ""

echo -e "${YELLOW}To run unit tests only:${NC}"
echo "./bin/rin test unit"
echo ""

echo -e "${YELLOW}To run component tests only:${NC}"
echo "./bin/rin test component"
echo ""

echo -e "${YELLOW}To run integration tests only:${NC}"
echo "./bin/rin test integration"
echo ""

echo -e "${YELLOW}To run acceptance tests only:${NC}"
echo "./bin/rin test acceptance"
echo ""

echo -e "${YELLOW}To run BDD tests only:${NC}"
echo "./bin/rin test bdd"
echo ""

# Print help
print_header "Usage Instructions"

echo "Use this script to discover and organize tests across the project."
echo ""
echo -e "${BOLD}Options:${NC}"
echo "  --detailed, -d    Show detailed list of all tests"
echo "  --help, -h        Show this help message"
echo ""
echo -e "${BOLD}Examples:${NC}"
echo "  ./bin/test-discovery.sh             # Basic discovery report"
echo "  ./bin/test-discovery.sh --detailed  # Detailed test listing"