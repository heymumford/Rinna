#!/bin/bash
# Bash Test Runner for Rinna
# Runs tests organized by the testing pyramid structure

# Remove set -e to allow for proper error handling and propagation
# set -e

# Color definitions
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
BLUE="\033[0;34m"
RED="\033[0;31m"
NC="\033[0m" # No Color
BOLD="\033[1m"

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default settings
TEST_TYPE="all"
VERBOSE=false
exit_code=0

# Function to print section headers
print_header() {
    echo -e "\n${BLUE}${BOLD}$1${NC}"
    echo "========================================"
}

# Function to log test execution status
log_status() {
    local status=$1
    local test_name=$2
    
    if [ "$status" -eq 0 ]; then
        echo -e "${GREEN}✓ $test_name passed${NC}"
    else
        echo -e "${RED}✗ $test_name failed${NC}"
    fi
}

# Function to run tests in a directory
run_tests_in_dir() {
    local dir="$1"
    local successes=0
    local failures=0
    
    if [ ! -d "$dir" ]; then
        echo -e "${YELLOW}Directory $dir does not exist${NC}"
        return 0
    fi
    
    # Find all test files in the directory
    local test_files=$(find "$dir" -name "test_*.sh" -type f)
    
    if [ -z "$test_files" ]; then
        echo -e "${YELLOW}No test files found in $dir${NC}"
        return 0
    fi
    
    # Execute each test file
    for test_file in $test_files; do
        local test_name=$(basename "$test_file")
        print_header "Running $test_name"
        
        # Make the file executable
        chmod +x "$test_file"
        
        # Execute the test
        if [ "$VERBOSE" == "true" ]; then
            "$test_file"
            local status=$?
        else
            "$test_file" > /dev/null 2>&1
            local status=$?
        fi
        
        # Log the result
        log_status $status "$test_name"
        
        if [ $status -eq 0 ]; then
            successes=$((successes + 1))
        else
            failures=$((failures + 1))
        fi
    done
    
    echo ""
    echo -e "${BOLD}Results:${NC}"
    echo -e "${GREEN}$successes tests passed${NC}, ${RED}$failures tests failed${NC}"
    
    if [ $failures -gt 0 ]; then
        return 1
    else
        return 0
    fi
}

# Function to show usage instructions
show_help() {
    echo -e "${BLUE}${BOLD}Rinna Bash Test Runner${NC}"
    echo -e "Runs Bash tests organized by the testing pyramid structure"
    echo ""
    echo -e "${BOLD}Usage:${NC}"
    echo "  $0 [options]"
    echo ""
    echo -e "${BOLD}Options:${NC}"
    echo "  --type <type>         Type of tests to run (all|unit|component|integration|acceptance|performance)"
    echo "  --verbose             Show detailed output"
    echo "  --help                Show this help message"
    echo ""
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --type)
            TEST_TYPE="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# Validate test type
if [[ ! "$TEST_TYPE" =~ ^(all|unit|component|integration|acceptance|performance)$ ]]; then
    echo -e "${RED}Invalid test type: $TEST_TYPE${NC}"
    show_help
    exit 1
fi

# Execute tests based on type
case $TEST_TYPE in
    all)
        print_header "Running all Bash tests"
        if ! run_tests_in_dir "$SCRIPT_DIR/unit"; then
            exit_code=1
        fi
        if ! run_tests_in_dir "$SCRIPT_DIR/component"; then
            exit_code=1
        fi
        if ! run_tests_in_dir "$SCRIPT_DIR/integration"; then
            exit_code=1
        fi
        if ! run_tests_in_dir "$SCRIPT_DIR/acceptance"; then
            exit_code=1
        fi
        if ! run_tests_in_dir "$SCRIPT_DIR/performance"; then
            exit_code=1
        fi
        exit $exit_code
        ;;
    unit|component|integration|acceptance|performance)
        print_header "Running $TEST_TYPE Bash tests"
        if ! run_tests_in_dir "$SCRIPT_DIR/$TEST_TYPE"; then
            exit_code=1
        fi
        ;;
esac

if [ $exit_code -eq 0 ]; then
    echo -e "${GREEN}${BOLD}All Bash tests completed successfully!${NC}"
else
    echo -e "${RED}${BOLD}Some Bash tests failed!${NC}"
fi
exit $exit_code