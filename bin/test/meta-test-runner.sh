#!/bin/bash
# Meta-test runner for Rinna
# Runs tests that verify the development environment and build processes

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/common/test_utils.sh"

# Color definitions
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
BLUE="\033[0;34m"
RED="\033[0;31m"
NC="\033[0m" # No Color
BOLD="\033[1m"

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

# Function to run all meta-tests of a specific type
run_meta_tests() {
    local test_type=$1
    local test_dir="$SCRIPT_DIR/$test_type"
    local overall_status=0
    
    if [ ! -d "$test_dir" ]; then
        echo -e "${YELLOW}Meta-test directory $test_dir not found${NC}"
        return 0
    fi
    
    print_header "Running meta-tests: $test_type"
    
    # Find all test files in the directory
    local test_files=($(find "$test_dir" -name "test_*.sh" -type f | sort))
    
    if [ ${#test_files[@]} -eq 0 ]; then
        echo -e "${YELLOW}No meta-test files found in $test_dir${NC}"
        return 0
    fi
    
    echo "Found ${#test_files[@]} test files"
    
    # Execute each test file
    for test_file in "${test_files[@]}"; do
        local test_name=$(basename "$test_file")
        print_test_header "$test_name"
        
        # Make the file executable
        chmod +x "$test_file"
        
        # Execute the test with timing
        local start_time=$(date +%s.%N)
        "$test_file"
        local status=$?
        local end_time=$(date +%s.%N)
        local duration=$(echo "$end_time - $start_time" | bc)
        local duration_rounded=$(printf "%.2f" $duration)
        
        # Log the result
        if [ $status -eq 0 ]; then
            print_test_success "$test_name" "$duration_rounded"
        else
            print_test_failure "$test_name" "$duration_rounded"
            overall_status=1
        fi
    done
    
    return $overall_status
}

# Show usage
show_usage() {
    echo -e "${BLUE}${BOLD}Rinna Meta-Test Runner${NC}"
    echo "Runs tests that verify the development environment and build processes"
    echo
    echo -e "${BOLD}Usage:${NC}"
    echo "  $0 [test_type]"
    echo
    echo -e "${BOLD}Test types:${NC}"
    echo "  all          Run all meta-tests (default)"
    echo "  unit         Run only unit meta-tests"
    echo "  component    Run only component meta-tests"
    echo "  integration  Run only integration meta-tests"
    echo "  performance  Run only performance meta-tests"
    echo
    echo -e "${BOLD}Examples:${NC}"
    echo "  $0           # Run all meta-tests"
    echo "  $0 unit      # Run only unit meta-tests"
}

# Main execution
main() {
    # Check for help flag
    if [[ "$1" == "--help" || "$1" == "-h" ]]; then
        show_usage
        exit 0
    fi
    
    # Determine test type
    local test_type="${1:-all}"
    local overall_status=0
    
    print_header "Running Meta-Tests: $test_type"
    echo "Meta-tests verify the development environment and build processes"
    
    # Execute tests based on type
    case $test_type in
        all)
            # Run all test types in order
            run_meta_tests "unit" || overall_status=1
            run_meta_tests "component" || overall_status=1
            run_meta_tests "integration" || overall_status=1
            run_meta_tests "performance" || overall_status=1
            ;;
        unit|component|integration|performance)
            # Run just the specified test type
            run_meta_tests "$test_type" || overall_status=1
            ;;
        *)
            echo -e "${RED}Unknown test type: $test_type${NC}"
            show_usage
            exit 1
            ;;
    esac
    
    # Show summary
    print_header "Meta-Test Summary"
    
    if [ $overall_status -eq 0 ]; then
        echo -e "${GREEN}${BOLD}All meta-tests passed successfully!${NC}"
    else
        echo -e "${RED}${BOLD}Some meta-tests failed!${NC}"
    fi
    
    return $overall_status
}

# Execute main function
main "$@"
exit $?