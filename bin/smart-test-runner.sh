#!/bin/bash
# Rinna Smart Test Runner
# Provides an intelligent, layered approach to test execution based on the testing pyramid

set -e

# Color definitions
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
BLUE="\033[0;34m"
RED="\033[0;31m"
NC="\033[0m" # No Color
BOLD="\033[1m"

# Get the project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Environment variables that can override defaults
SKIP_UNIT=${SKIP_UNIT:-false}
SKIP_COMPONENT=${SKIP_COMPONENT:-false}
SKIP_INTEGRATION=${SKIP_INTEGRATION:-false}
SKIP_ACCEPTANCE=${SKIP_ACCEPTANCE:-false}
SKIP_PERFORMANCE=${SKIP_PERFORMANCE:-false}
PARALLEL=${PARALLEL:-true}
DEBUG=${DEBUG:-false}
FAIL_FAST=${FAIL_FAST:-false}
MAX_WORKERS=${MAX_WORKERS:-4}
TEST_LANGUAGE_FILTER=${TEST_LANGUAGE_FILTER:-""}

# Default timeouts (in seconds)
UNIT_TIMEOUT=${UNIT_TIMEOUT:-30}
COMPONENT_TIMEOUT=${COMPONENT_TIMEOUT:-60}
INTEGRATION_TIMEOUT=${INTEGRATION_TIMEOUT:-120}
ACCEPTANCE_TIMEOUT=${ACCEPTANCE_TIMEOUT:-300}
PERFORMANCE_TIMEOUT=${PERFORMANCE_TIMEOUT:-600}

# Status tracking
SUCCESS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0
TOTAL_START_TIME=$(date +%s)

# Function to print section headers
print_header() {
    echo -e "\n${BLUE}${BOLD}$1${NC}"
    echo "========================================"
}

# Function to format elapsed time
format_time() {
    local seconds=$1
    if [ $seconds -lt 60 ]; then
        echo "${seconds}s"
    else
        local minutes=$((seconds / 60))
        local remaining_seconds=$((seconds % 60))
        echo "${minutes}m ${remaining_seconds}s"
    fi
}

# Function to log test execution status
log_status() {
    local status=$1
    local test_type=$2
    local duration=$3
    
    if [ "$status" -eq 0 ]; then
        echo -e "${GREEN}✓ $test_type tests passed ($(format_time $duration))${NC}"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo -e "${RED}✗ $test_type tests failed ($(format_time $duration))${NC}"
        FAIL_COUNT=$((FAIL_COUNT + 1))
        
        if [ "$FAIL_FAST" == "true" ]; then
            echo -e "${RED}${BOLD}Stopping on first failure (--fail-fast)${NC}"
            exit 1
        fi
    fi
}

# Function to run a specific test type
run_tests() {
    local test_type=$1
    local profile=$2
    local timeout=$3
    local skip_var="SKIP_$(echo $test_type | tr '[:lower:]' '[:upper:]')"
    
    if [ "${!skip_var}" == "true" ]; then
        echo -e "${YELLOW}Skipping $test_type tests (--skip-$test_type)${NC}"
        SKIP_COUNT=$((SKIP_COUNT + 1))
        return 0
    fi
    
    # Check if we should filter by language
    local language_suffix=""
    if [ -n "$TEST_LANGUAGE_FILTER" ]; then
        language_suffix=" ($TEST_LANGUAGE_FILTER)"
        print_header "Running $test_type tests$language_suffix..."
    else
        print_header "Running $test_type tests..."
    fi
    
    # Set MVN_OPTS based on configuration
    MVN_OPTS=""
    
    if [ "$PARALLEL" == "true" ]; then
        MVN_OPTS="$MVN_OPTS -Djunit.jupiter.execution.parallel.enabled=true -Djunit.jupiter.execution.parallel.config.dynamic.factor=1"
        
        # Set worker count if specified
        if [ "$MAX_WORKERS" -gt 0 ]; then
            MVN_OPTS="$MVN_OPTS -Djunit.jupiter.execution.parallel.config.fixed.parallelism=$MAX_WORKERS"
            MVN_OPTS="$MVN_OPTS -Djunit.jupiter.execution.parallel.config.strategy=fixed"
        fi
    else
        MVN_OPTS="$MVN_OPTS -Djunit.jupiter.execution.parallel.enabled=false"
    fi
    
    if [ "$DEBUG" == "true" ]; then
        MVN_OPTS="$MVN_OPTS -X"
    fi
    
    local start_time=$(date +%s)
    local exit_code=0
    
    # Execute the appropriate tests based on language filter
    case "$TEST_LANGUAGE_FILTER" in
        java)
            # Run only Java tests
            timeout $timeout mvn test -P $profile $MVN_OPTS || exit_code=1
            ;;
        go)
            # Run only Go tests
            if [ -d "$PROJECT_ROOT/api" ]; then
                (cd "$PROJECT_ROOT/api" && \
                 timeout $timeout go test -v ./test/... -tags "$test_type") || exit_code=1
            else
                echo -e "${YELLOW}No Go code found in api/ directory${NC}"
            fi
            ;;
        python)
            # Run only Python tests
            if [ -d "$PROJECT_ROOT/python" ]; then
                (cd "$PROJECT_ROOT" && \
                 timeout $timeout python -m pytest python/tests -k "$test_type" -v) || exit_code=1
            else
                echo -e "${YELLOW}No Python tests found in python/tests/ directory${NC}"
            fi
            ;;
        bash)
            # Run only Bash tests
            if [ -d "$PROJECT_ROOT/bin/test" ]; then
                (cd "$PROJECT_ROOT" && \
                 timeout $timeout bash bin/test/test_runner.sh --type "$test_type") || exit_code=1
            else
                echo -e "${YELLOW}No Bash test runner found in bin/test/ directory${NC}"
            fi
            ;;
        *)
            # Run tests for all languages
            
            # 1. Java tests via Maven
            if [ -f "$PROJECT_ROOT/pom.xml" ]; then
                timeout $timeout mvn test -P $profile $MVN_OPTS || exit_code=1
            fi
            
            # 2. Go tests
            if [ -d "$PROJECT_ROOT/api" ] && [ "$test_type" != "acceptance" ] && [ "$test_type" != "performance" ]; then
                (cd "$PROJECT_ROOT/api" && \
                 timeout $timeout go test -v ./test/... -tags "$test_type") || exit_code=1
            fi
            
            # 3. Python tests
            if [ -d "$PROJECT_ROOT/python" ] && [ "$test_type" != "acceptance" ] && [ "$test_type" != "performance" ]; then
                (cd "$PROJECT_ROOT" && \
                 timeout $timeout python -m pytest python/tests -k "$test_type" -v) || exit_code=1
            fi
            
            # 4. Bash tests
            if [ -d "$PROJECT_ROOT/bin/test" ] && [ "$test_type" != "acceptance" ] && [ "$test_type" != "performance" ]; then
                (cd "$PROJECT_ROOT" && \
                 timeout $timeout bash bin/test/test_runner.sh --type "$test_type") || exit_code=1
            fi
            ;;
    esac
    
    log_status $exit_code "$test_type" $(($(date +%s) - start_time))
    return $exit_code
}

# Function to run BDD tests
run_bdd_tests() {
    if [ "$SKIP_ACCEPTANCE" == "true" ]; then
        echo -e "${YELLOW}Skipping BDD tests (--skip-acceptance)${NC}"
        SKIP_COUNT=$((SKIP_COUNT + 1))
        return 0
    fi
    
    print_header "Running BDD tests..."
    
    # Set Cucumber options based on configuration
    CUCUMBER_OPTS="-Dcucumber.publish.quiet=true"
    
    if [ "$PARALLEL" == "true" ]; then
        CUCUMBER_OPTS="$CUCUMBER_OPTS -Dcucumber.execution.parallel.enabled=true"
    else
        CUCUMBER_OPTS="$CUCUMBER_OPTS -Dcucumber.execution.parallel.enabled=false"
    fi
    
    MVN_OPTS="$CUCUMBER_OPTS"
    
    if [ "$DEBUG" == "true" ]; then
        MVN_OPTS="$MVN_OPTS -X"
    fi
    
    local start_time=$(date +%s)
    local exit_code=0
    
    # Check if we should filter by language
    if [ "$TEST_LANGUAGE_FILTER" == "java" ] || [ -z "$TEST_LANGUAGE_FILTER" ]; then
        # Execute BDD tests
        timeout $ACCEPTANCE_TIMEOUT mvn test -Dtest='org.rinna.bdd.CucumberRunner' $MVN_OPTS || exit_code=1
    else
        echo -e "${YELLOW}BDD tests are only supported for Java${NC}"
    fi
    
    log_status $exit_code "BDD" $(($(date +%s) - start_time))
    return $exit_code
}

# Function to show test execution summary
show_summary() {
    local total_duration=$(($(date +%s) - TOTAL_START_TIME))
    
    print_header "Test Execution Summary"
    
    echo -e "${BOLD}Duration:${NC} $(format_time $total_duration)"
    echo -e "${BOLD}Results:${NC}"
    
    if [ $SUCCESS_COUNT -gt 0 ]; then
        echo -e "  ${GREEN}✓ $SUCCESS_COUNT test categories passed${NC}"
    fi
    
    if [ $FAIL_COUNT -gt 0 ]; then
        echo -e "  ${RED}✗ $FAIL_COUNT test categories failed${NC}"
    fi
    
    if [ $SKIP_COUNT -gt 0 ]; then
        echo -e "  ${YELLOW}○ $SKIP_COUNT test categories skipped${NC}"
    fi
    
    if [ $FAIL_COUNT -eq 0 ]; then
        echo -e "\n${GREEN}${BOLD}All tests completed successfully!${NC}"
        return 0
    else
        echo -e "\n${RED}${BOLD}Some tests failed!${NC}"
        return 1
    fi
}

# Function to show usage instructions
show_help() {
    echo -e "${BLUE}${BOLD}Rinna Smart Test Runner${NC}"
    echo -e "Intelligent, layered approach to test execution based on the testing pyramid"
    echo ""
    echo -e "${BOLD}Usage:${NC}"
    echo "  $0 [options] [test_types]"
    echo ""
    echo -e "${BOLD}Options:${NC}"
    echo "  --skip-unit           Skip unit tests"
    echo "  --skip-component      Skip component tests"
    echo "  --skip-integration    Skip integration tests"
    echo "  --skip-acceptance     Skip acceptance tests (including BDD)"
    echo "  --skip-performance    Skip performance tests"
    echo "  --no-parallel         Disable parallel test execution"
    echo "  --debug               Enable debug output"
    echo "  --fail-fast           Stop on first test failure"
    echo "  --workers <n>         Set maximum number of parallel test workers"
    echo "  --language <lang>     Run tests only for a specific language (java|go|python|bash)"
    echo "  --help                Show this help message"
    echo ""
    echo -e "${BOLD}Test types:${NC}"
    echo "  all                  Run all tests (default)"
    echo "  unit                 Run only unit tests"
    echo "  component            Run only component tests"
    echo "  integration          Run only integration tests"
    echo "  acceptance           Run only acceptance tests"
    echo "  bdd                  Run only BDD tests (subset of acceptance)"
    echo "  performance          Run only performance tests"
    echo "  fast                 Run unit and component tests only"
    echo "  essential            Run unit, component, and integration tests (no UI)"
    echo ""
    echo -e "${BOLD}Examples:${NC}"
    echo "  $0                   # Run all tests"
    echo "  $0 unit              # Run only unit tests"
    echo "  $0 fast              # Run unit and component tests"
    echo "  $0 --language java unit  # Run only Java unit tests"
    echo "  $0 --skip-integration --skip-performance  # Skip integration and performance tests"
    echo ""
}

# Parse command line arguments
TEST_TYPES=()
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-unit)
            SKIP_UNIT=true
            shift
            ;;
        --skip-component)
            SKIP_COMPONENT=true
            shift
            ;;
        --skip-integration)
            SKIP_INTEGRATION=true
            shift
            ;;
        --skip-acceptance)
            SKIP_ACCEPTANCE=true
            shift
            ;;
        --skip-performance)
            SKIP_PERFORMANCE=true
            shift
            ;;
        --no-parallel)
            PARALLEL=false
            shift
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        --fail-fast)
            FAIL_FAST=true
            shift
            ;;
        --workers)
            MAX_WORKERS=$2
            shift 2
            ;;
        --language)
            TEST_LANGUAGE_FILTER=$2
            shift 2
            ;;
        --help)
            show_help
            exit 0
            ;;
        unit|component|integration|acceptance|bdd|performance|fast|essential|all)
            TEST_TYPES+=("$1")
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# If no test types specified, run all
if [ ${#TEST_TYPES[@]} -eq 0 ]; then
    TEST_TYPES=("all")
fi

# Process special test type combinations
for type in "${TEST_TYPES[@]}"; do
    case $type in
        all)
            # Run all tests (default)
            ;;
        fast)
            # Skip all except unit and component
            SKIP_INTEGRATION=true
            SKIP_ACCEPTANCE=true
            SKIP_PERFORMANCE=true
            ;;
        essential)
            # Skip acceptance and performance
            SKIP_ACCEPTANCE=true
            SKIP_PERFORMANCE=true
            ;;
        unit)
            # Run only unit tests
            SKIP_COMPONENT=true
            SKIP_INTEGRATION=true
            SKIP_ACCEPTANCE=true
            SKIP_PERFORMANCE=true
            ;;
        component)
            # Run only component tests
            SKIP_UNIT=true
            SKIP_INTEGRATION=true
            SKIP_ACCEPTANCE=true
            SKIP_PERFORMANCE=true
            ;;
        integration)
            # Run only integration tests
            SKIP_UNIT=true
            SKIP_COMPONENT=true
            SKIP_ACCEPTANCE=true
            SKIP_PERFORMANCE=true
            ;;
        acceptance)
            # Run only acceptance tests
            SKIP_UNIT=true
            SKIP_COMPONENT=true
            SKIP_INTEGRATION=true
            SKIP_PERFORMANCE=true
            ;;
        bdd)
            # Run only BDD tests
            SKIP_UNIT=true
            SKIP_COMPONENT=true
            SKIP_INTEGRATION=true
            SKIP_PERFORMANCE=true
            # Special case: run only BDD tests
            run_bdd_tests
            show_summary
            exit $?
            ;;
        performance)
            # Run only performance tests
            SKIP_UNIT=true
            SKIP_COMPONENT=true
            SKIP_INTEGRATION=true
            SKIP_ACCEPTANCE=true
            ;;
    esac
done

# Show test execution plan
print_header "Test Execution Plan"

echo -e "${BOLD}Will run:${NC}"
[ "$SKIP_UNIT" != "true" ] && echo -e "${GREEN}✓${NC} Unit tests"
[ "$SKIP_COMPONENT" != "true" ] && echo -e "${GREEN}✓${NC} Component tests"
[ "$SKIP_INTEGRATION" != "true" ] && echo -e "${GREEN}✓${NC} Integration tests"
[ "$SKIP_ACCEPTANCE" != "true" ] && echo -e "${GREEN}✓${NC} Acceptance tests"
[ "$SKIP_PERFORMANCE" != "true" ] && echo -e "${GREEN}✓${NC} Performance tests"

echo -e "\n${BOLD}Configuration:${NC}"
[ "$PARALLEL" == "true" ] && echo -e "Parallel execution: ${GREEN}enabled${NC} (max workers: $MAX_WORKERS)" || echo -e "Parallel execution: ${YELLOW}disabled${NC}"
[ "$FAIL_FAST" == "true" ] && echo -e "Fail fast: ${YELLOW}enabled${NC}" || echo -e "Fail fast: ${BLUE}disabled${NC}"
[ "$DEBUG" == "true" ] && echo -e "Debug output: ${YELLOW}enabled${NC}" || echo -e "Debug output: ${BLUE}disabled${NC}"
[ -n "$TEST_LANGUAGE_FILTER" ] && echo -e "Language filter: ${BLUE}$TEST_LANGUAGE_FILTER${NC}" || echo -e "Language filter: ${BLUE}all${NC}"

# Execute tests in the correct order (pyramid layers)
# Tests are run in order of increasing complexity and execution time:
# Unit → Component → Integration → Acceptance → Performance

OVERALL_STATUS=0

# 1. Unit Tests (smallest, fastest)
if [ "$SKIP_UNIT" != "true" ]; then
    run_tests "unit" "unit-tests" $UNIT_TIMEOUT || OVERALL_STATUS=1
fi

# 2. Component Tests
if [ "$SKIP_COMPONENT" != "true" ]; then
    run_tests "component" "component-tests" $COMPONENT_TIMEOUT || OVERALL_STATUS=1
fi

# 3. Integration Tests
if [ "$SKIP_INTEGRATION" != "true" ]; then
    run_tests "integration" "integration-tests" $INTEGRATION_TIMEOUT || OVERALL_STATUS=1
fi

# 4. Acceptance Tests (including BDD)
if [ "$SKIP_ACCEPTANCE" != "true" ]; then
    run_tests "acceptance" "acceptance-tests" $ACCEPTANCE_TIMEOUT || OVERALL_STATUS=1
    run_bdd_tests || OVERALL_STATUS=1
fi

# 5. Performance Tests (largest, slowest)
if [ "$SKIP_PERFORMANCE" != "true" ]; then
    run_tests "performance" "performance-tests" $PERFORMANCE_TIMEOUT || OVERALL_STATUS=1
fi

# Show summary and exit with the correct status
show_summary
exit $OVERALL_STATUS