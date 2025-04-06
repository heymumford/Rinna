#!/bin/bash
# Meta-test runner for Rinna
# Runs tests that verify our development environment and build processes

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
if [ -f "$SCRIPT_DIR/common/test_utils.sh" ]; then
    source "$SCRIPT_DIR/common/test_utils.sh"
else
    # In case we're running from a different location
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
    if [ -f "$SCRIPT_DIR/common/test_utils.sh" ]; then
        source "$SCRIPT_DIR/common/test_utils.sh"
    else
        echo "Error: test_utils.sh not found!"
        exit 1
    fi
fi

# Set up environment for tests
setup_environment() {
    echo -e "${BLUE}${BOLD}Setting up test environment...${NC}"
    
    # Ensure we're in the project root
    cd "$PROJECT_ROOT"
    
    # Load environment variables if activate script exists
    if [ -f "$PROJECT_ROOT/activate-python.sh" ]; then
        echo "Sourcing activate-python.sh for environment setup"
        source "$PROJECT_ROOT/activate-python.sh"
    fi
    
    # Detect test environment
    TEST_ENV=$(detect_test_environment)
    echo "Running in $TEST_ENV environment"
    
    # Set appropriate timeouts based on environment
    if [ "$TEST_ENV" == "ci" ]; then
        echo "Using CI-specific timeouts"
        TIMEOUT_FACTOR=2.0  # CI environments might be slower
    else
        TIMEOUT_FACTOR=1.0
    fi
    
    return 0
}

# Run meta-tests for a specific category
run_meta_tests() {
    local test_category=$1
    local successes=0
    local failures=0
    
    echo -e "\n${BLUE}${BOLD}Running $test_category meta-tests${NC}"
    echo "========================================"
    
    # Find meta-test directories
    local meta_dirs=()
    
    case $test_category in
        all)
            # All meta-test directories
            meta_dirs=(
                "$PROJECT_ROOT/bin/test/unit/meta" 
                "$PROJECT_ROOT/bin/test/component/meta" 
                "$PROJECT_ROOT/bin/test/integration/meta" 
                "$PROJECT_ROOT/bin/test/performance/meta"
            )
            ;;
        unit)
            # Unit test meta-test directory
            meta_dirs=("$PROJECT_ROOT/bin/test/unit/meta")
            ;;
        component)
            # Component test meta-test directory
            meta_dirs=("$PROJECT_ROOT/bin/test/component/meta")
            ;;
        integration)
            # Integration test meta-test directory
            meta_dirs=("$PROJECT_ROOT/bin/test/integration/meta")
            ;;
        performance)
            # Performance test meta-test directory
            meta_dirs=("$PROJECT_ROOT/bin/test/performance/meta")
            ;;
        *)
            # Category-specific meta-test directory
            meta_dirs=("$PROJECT_ROOT/bin/test/$test_category/meta")
            echo "Warning: Unknown test category: $test_category"
            ;;
    esac
    
    echo "Looking for meta-tests in the following directories:"
    for dir in "${meta_dirs[@]}"; do
        echo "- $dir"
    done
    
    # Debug directory where we actually are
    echo "Current script directory: $SCRIPT_DIR"
    echo "Checking for unit/meta: $(ls -la "$SCRIPT_DIR/unit/meta" 2>/dev/null || echo "Not found")"
    
    # Run tests from each directory
    for meta_dir in "${meta_dirs[@]}"; do
        if [ -d "$meta_dir" ]; then
            echo -e "\nSearching for meta-tests in $meta_dir"
            
            # Find all test files in the directory
            local test_files=($(find "$meta_dir" -name "test_*.sh" -type f | sort))
            
            if [ ${#test_files[@]} -eq 0 ]; then
                echo -e "${YELLOW}No meta-test files found in $meta_dir${NC}"
                continue
            fi
            
            echo "Found ${#test_files[@]} meta-test files"
            
            # Execute each test file
            for test_file in "${test_files[@]}"; do
                test_name=$(basename "$test_file")
                print_test_header "$test_name"
                
                # Make sure the file is executable
                chmod +x "$test_file"
                
                # Execute the test with timing
                start_time=$(date +%s.%N)
                "$test_file"
                status=$?
                end_time=$(date +%s.%N)
                duration=$(echo "$end_time - $start_time" | bc)
                duration_rounded=$(printf "%.2f" $duration)
                
                # Log the result
                if [ $status -eq 0 ]; then
                    print_test_success "$test_name" "$duration_rounded"
                    successes=$((successes + 1))
                else
                    print_test_failure "$test_name" "$duration_rounded"
                    failures=$((failures + 1))
                fi
            done
        else
            echo -e "${YELLOW}Meta-test directory not found: $meta_dir${NC}"
        fi
    done
    
    # Also run legacy meta-tests for backward compatibility
    if [ "$test_category" == "all" ] || [ "$test_category" == "unit" ]; then
        local legacy_test="$SCRIPT_DIR/unit/test_unit_build_efficiency.sh"
        if [ -f "$legacy_test" ]; then
            test_name=$(basename "$legacy_test")
            print_test_header "$test_name (legacy)"
            
            # Make sure the file is executable
            chmod +x "$legacy_test"
            
            # Execute the test with timing
            start_time=$(date +%s.%N)
            "$legacy_test"
            status=$?
            end_time=$(date +%s.%N)
            duration=$(echo "$end_time - $start_time" | bc)
            duration_rounded=$(printf "%.2f" $duration)
            
            # Log the result
            if [ $status -eq 0 ]; then
                print_test_success "$test_name" "$duration_rounded"
                successes=$((successes + 1))
            else
                print_test_failure "$test_name" "$duration_rounded"
                failures=$((failures + 1))
            fi
        fi
    fi
    
    if [ "$test_category" == "all" ] || [ "$test_category" == "component" ]; then
        local legacy_test="$SCRIPT_DIR/component/test_component_cross_language.sh"
        if [ -f "$legacy_test" ]; then
            test_name=$(basename "$legacy_test")
            print_test_header "$test_name (legacy)"
            
            # Make sure the file is executable
            chmod +x "$legacy_test"
            
            # Execute the test with timing
            start_time=$(date +%s.%N)
            "$legacy_test"
            status=$?
            end_time=$(date +%s.%N)
            duration=$(echo "$end_time - $start_time" | bc)
            duration_rounded=$(printf "%.2f" $duration)
            
            # Log the result
            if [ $status -eq 0 ]; then
                print_test_success "$test_name" "$duration_rounded"
                successes=$((successes + 1))
            else
                print_test_failure "$test_name" "$duration_rounded"
                failures=$((failures + 1))
            fi
        fi
    fi
    
    # Print summary
    echo -e "\n${BOLD}Meta-Test Summary:${NC}"
    echo -e "${GREEN}$successes tests passed${NC}, ${RED}$failures tests failed${NC}"
    
    if [ $failures -gt 0 ]; then
        return 1
    else
        return 0
    fi
}

# Show usage
show_usage() {
    echo -e "${BLUE}${BOLD}Rinna Meta-Test Runner${NC}"
    echo "Runs tests that verify the development environment and build processes"
    echo
    echo -e "${BOLD}Usage:${NC}"
    echo "  $0 [category]"
    echo
    echo -e "${BOLD}Categories:${NC}"
    echo "  all          Run all meta-tests (default)"
    echo "  unit         Run unit-level meta-tests"
    echo "  component    Run component-level meta-tests"
    echo "  integration  Run integration-level meta-tests"
    echo "  performance  Run performance-level meta-tests"
    echo
    echo -e "${BOLD}Examples:${NC}"
    echo "  $0           # Run all meta-tests"
    echo "  $0 unit      # Run only unit-level meta-tests"
}

# Main execution
main() {
    # Process arguments
    case $1 in
        unit|component|integration|performance|all)
            test_category=$1
            ;;
        help|--help|-h)
            show_usage
            return 0
            ;;
        "")
            test_category="all"
            ;;
        *)
            echo -e "${RED}Invalid category: $1${NC}"
            show_usage
            return 1
            ;;
    esac
    
    # Setup test environment
    setup_environment || return 1
    
    # Run selected tests
    run_meta_tests "$test_category"
    return $?
}

# Execute main with all arguments
main "$@"
exit $?