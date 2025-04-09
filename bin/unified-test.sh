#!/usr/bin/env bash
#
# unified-test.sh - Simplified, unified test runner for Rinna
#
# This script provides a streamlined approach to executing tests across all languages
# with a consistent interface and minimal complexity.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -eo pipefail

# Determine project directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_DIR="$PROJECT_ROOT/target/logs"
mkdir -p "$LOG_DIR" 2>/dev/null || true
LOG_FILE="$LOG_DIR/unified-test-$(date +%Y%m%d-%H%M%S).log"

# Colors and formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Default configuration
CATEGORY="all"    # Default test category
MODE="normal"     # normal, fast, ci
PARALLEL=true     # Whether to run tests in parallel
LANGUAGES=("java" "go" "python")
COVERAGE=false    # Whether to generate coverage report
JVM_ARGS="-Djunit.jupiter.execution.parallel.enabled=true"
VERBOSE=false
FAIL_FAST=false
MVN_ARGS=""

# Display help text
show_help() {
    cat <<EOF
${BLUE}Unified Test Runner${NC} - Streamlined test execution for Rinna

${BOLD}Usage:${NC} unified-test.sh [options] [category]

${BOLD}Test Categories:${NC}
  all                 Run all tests (default)
  unit                Run only unit tests
  component           Run only component tests
  integration         Run only integration tests
  acceptance          Run only acceptance tests
  performance         Run only performance tests

${BOLD}Language Options:${NC}
  --java              Run only Java tests
  --go                Run only Go tests
  --python            Run only Python tests
  --all-languages     Run tests for all languages (default)

${BOLD}Mode Options:${NC}
  --fast              Run only fast tests (unit + component)
  --ci                Run in CI mode (optimized for continuous integration)

${BOLD}Execution Options:${NC}
  --no-parallel       Disable parallel test execution
  --fail-fast         Stop on first test failure
  --coverage          Generate code coverage report
  --verbose, -v       Show verbose output

${BOLD}Examples:${NC}
  $0                  # Run all tests with default settings
  $0 unit             # Run only unit tests
  $0 --java unit      # Run only Java unit tests
  $0 --fast           # Run fast tests only (unit and component)
  $0 integration --coverage  # Run integration tests with coverage report
EOF
}

# Parse command line arguments
parse_args() {
    # Reset languages array if explicitly specified
    LANG_SPECIFIED=false
    
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                show_help
                exit 0
                ;;
            --java)
                if [[ "$LANG_SPECIFIED" == "false" ]]; then
                    LANGUAGES=()
                    LANG_SPECIFIED=true
                fi
                LANGUAGES+=("java")
                shift
                ;;
            --go)
                if [[ "$LANG_SPECIFIED" == "false" ]]; then
                    LANGUAGES=()
                    LANG_SPECIFIED=true
                fi
                LANGUAGES+=("go")
                shift
                ;;
            --python)
                if [[ "$LANG_SPECIFIED" == "false" ]]; then
                    LANGUAGES=()
                    LANG_SPECIFIED=true
                fi
                LANGUAGES+=("python")
                shift
                ;;
            --all-languages)
                LANGUAGES=("java" "go" "python")
                shift
                ;;
            --fast)
                MODE="fast"
                shift
                ;;
            --ci)
                MODE="ci"
                shift
                ;;
            --no-parallel)
                PARALLEL=false
                shift
                ;;
            --fail-fast)
                FAIL_FAST=true
                shift
                ;;
            --coverage)
                COVERAGE=true
                shift
                ;;
            --verbose|-v)
                VERBOSE=true
                shift
                ;;
            unit|component|integration|acceptance|performance|all)
                CATEGORY="$1"
                shift
                ;;
            *)
                echo -e "${RED}Unknown option: $1${NC}"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Set Maven options
    if [[ "$PARALLEL" == "false" ]]; then
        JVM_ARGS="-Djunit.jupiter.execution.parallel.enabled=false"
    fi
    
    if [[ "$VERBOSE" == "true" ]]; then
        MVN_ARGS="$MVN_ARGS -X"
    fi
    
    if [[ "$FAIL_FAST" == "true" ]]; then
        MVN_ARGS="$MVN_ARGS -Djunit.jupiter.execution.parallel.mode.default=same_thread"
        MVN_ARGS="$MVN_ARGS -Dcucumber.execution.parallel.enabled=false"
        
        # Set fail-fast option for each language
        if [[ " ${LANGUAGES[*]} " =~ " go " ]]; then
            GO_ARGS="$GO_ARGS -failfast"
        fi
    fi
}

# Print execution plan
print_plan() {
    echo -e "${BLUE}${BOLD}Rinna Unified Test Runner${NC}"
    echo "==============================="
    echo -e "${BOLD}Test Category:${NC} $CATEGORY"
    echo -e "${BOLD}Mode:${NC} $MODE"
    echo -e "${BOLD}Languages:${NC} ${LANGUAGES[*]}"
    echo -e "${BOLD}Parallel Execution:${NC} $PARALLEL"
    echo -e "${BOLD}Fail Fast:${NC} $FAIL_FAST"
    echo -e "${BOLD}Coverage:${NC} $COVERAGE"
    echo "==============================="
    echo
}

# Run Java tests
run_java_tests() {
    local category="$1"
    local profile=""
    
    # Skip if Java not in languages list
    if [[ ! " ${LANGUAGES[*]} " =~ " java " ]]; then
        return 0
    fi
    
    echo -e "${BLUE}Running Java ${category} tests...${NC}"
    
    # Determine the right Maven profile
    case "$category" in
        unit)
            profile="unit-tests"
            test_pattern='*Test,!*ComponentTest,!*IntegrationTest,!*AcceptanceTest,!*PerformanceTest,!*Runner'
            ;;
        component)
            profile="component-tests" 
            test_pattern='*ComponentTest'
            ;;
        integration)
            profile="integration-tests"
            test_pattern='*IntegrationTest'
            ;;
        acceptance)
            profile="acceptance-tests"
            test_pattern='*AcceptanceTest,org.rinna.bdd.*Runner'
            ;;
        performance)
            profile="performance-tests"
            test_pattern='*PerformanceTest'
            ;;
        all)
            profile="all-tests"
            test_pattern='*Test'
            ;;
        *)
            echo -e "${RED}Unknown test category: $category${NC}"
            return 1
            ;;
    esac
    
    # Add coverage if requested
    if [[ "$COVERAGE" == "true" ]]; then
        profile="$profile,jacoco"
    fi
    
    # Run with appropriate settings
    if [[ "$MODE" == "fast" && "$category" != "unit" && "$category" != "component" && "$category" != "all" ]]; then
        echo -e "${YELLOW}Skipping Java $category tests in fast mode${NC}"
        return 0
    fi
    
    cd "$PROJECT_ROOT"
    if mvn test -P $profile -Dtest=$test_pattern $JVM_ARGS $MVN_ARGS; then
        echo -e "${GREEN}✓ Java $category tests passed${NC}"
        return 0
    else
        echo -e "${RED}✗ Java $category tests failed${NC}"
        if [[ "$FAIL_FAST" == "true" ]]; then
            exit 1
        fi
        return 1
    fi
}

# Run Go tests
run_go_tests() {
    local category="$1"
    local tags=""
    
    # Skip if Go not in languages list
    if [[ ! " ${LANGUAGES[*]} " =~ " go " ]]; then
        return 0
    fi
    
    # Go API directory check
    if [[ ! -d "$PROJECT_ROOT/api" ]]; then
        echo -e "${YELLOW}Go API directory not found, skipping Go tests${NC}"
        return 0
    fi
    
    echo -e "${BLUE}Running Go ${category} tests...${NC}"
    
    # Determine tags/paths based on category
    case "$category" in
        unit)
            tags="unit"
            ;;
        component)
            tags="component"
            ;;
        integration)
            tags="integration"
            ;;
        performance)
            tags="performance" 
            ;;
        all)
            tags=""
            ;;
        acceptance)
            echo -e "${YELLOW}Skipping Go acceptance tests (not implemented)${NC}"
            return 0
            ;;
        *)
            echo -e "${RED}Unknown test category: $category${NC}"
            return 1
            ;;
    esac
    
    # Skip non-fast tests in fast mode
    if [[ "$MODE" == "fast" && "$category" != "unit" && "$category" != "component" && "$category" != "all" ]]; then
        echo -e "${YELLOW}Skipping Go $category tests in fast mode${NC}"
        return 0
    fi
    
    # Run with coverage if requested
    cd "$PROJECT_ROOT/api"
    if [[ "$COVERAGE" == "true" ]]; then
        if [[ -n "$tags" ]]; then
            if go test -v -coverprofile=coverage.out -tags="$tags" ./...; then
                echo -e "${GREEN}✓ Go $category tests passed${NC}"
                go tool cover -func=coverage.out
                return 0
            else
                echo -e "${RED}✗ Go $category tests failed${NC}"
                if [[ "$FAIL_FAST" == "true" ]]; then
                    exit 1
                fi
                return 1
            fi
        else
            if go test -v -coverprofile=coverage.out ./...; then
                echo -e "${GREEN}✓ Go $category tests passed${NC}"
                go tool cover -func=coverage.out
                return 0
            else
                echo -e "${RED}✗ Go $category tests failed${NC}"
                if [[ "$FAIL_FAST" == "true" ]]; then
                    exit 1
                fi
                return 1
            fi
        fi
    else
        if [[ -n "$tags" ]]; then
            if go test -v -tags="$tags" ./...; then
                echo -e "${GREEN}✓ Go $category tests passed${NC}"
                return 0
            else
                echo -e "${RED}✗ Go $category tests failed${NC}"
                if [[ "$FAIL_FAST" == "true" ]]; then
                    exit 1
                fi
                return 1
            fi
        else
            if go test -v ./...; then
                echo -e "${GREEN}✓ Go $category tests passed${NC}"
                return 0
            else
                echo -e "${RED}✗ Go $category tests failed${NC}"
                if [[ "$FAIL_FAST" == "true" ]]; then
                    exit 1
                fi
                return 1
            fi
        fi
    fi
}

# Run Python tests
run_python_tests() {
    local category="$1"
    local pytest_args=""
    
    # Skip if Python not in languages list
    if [[ ! " ${LANGUAGES[*]} " =~ " python " ]]; then
        return 0
    fi
    
    # Python directory check
    if [[ ! -d "$PROJECT_ROOT/python" ]]; then
        echo -e "${YELLOW}Python directory not found, skipping Python tests${NC}"
        return 0
    fi
    
    echo -e "${BLUE}Running Python ${category} tests...${NC}"
    
    # Add coverage if requested
    if [[ "$COVERAGE" == "true" ]]; then
        pytest_args="$pytest_args --cov=python/rinna"
    fi
    
    # Add verbose output if requested
    if [[ "$VERBOSE" == "true" ]]; then
        pytest_args="$pytest_args -v"
    fi
    
    # Add fail fast if requested
    if [[ "$FAIL_FAST" == "true" ]]; then
        pytest_args="$pytest_args -x"
    fi
    
    # Determine test path/markers based on category
    case "$category" in
        unit)
            pytest_args="$pytest_args -m unit"
            ;;
        component)
            pytest_args="$pytest_args -m component"
            ;;
        integration)
            pytest_args="$pytest_args -m integration"
            ;;
        acceptance)
            pytest_args="$pytest_args -m acceptance"
            ;;
        performance)
            pytest_args="$pytest_args -m performance"
            ;;
        all)
            # No specific markers needed
            ;;
        *)
            echo -e "${RED}Unknown test category: $category${NC}"
            return 1
            ;;
    esac
    
    # Skip non-fast tests in fast mode
    if [[ "$MODE" == "fast" && "$category" != "unit" && "$category" != "component" && "$category" != "all" ]]; then
        echo -e "${YELLOW}Skipping Python $category tests in fast mode${NC}"
        return 0
    fi
    
    # Run Python tests
    cd "$PROJECT_ROOT"
    if python -m pytest python/tests $pytest_args; then
        echo -e "${GREEN}✓ Python $category tests passed${NC}"
        return 0
    else
        echo -e "${RED}✗ Python $category tests failed${NC}"
        if [[ "$FAIL_FAST" == "true" ]]; then
            exit 1
        fi
        return 1
    fi
}

# Run tests for a specific category across all languages
run_category_tests() {
    local category="$1"
    local failures=0
    
    echo -e "${BLUE}${BOLD}Running $category tests across ${LANGUAGES[*]}...${NC}"
    
    for lang in "${LANGUAGES[@]}"; do
        case "$lang" in
            java)
                run_java_tests "$category" || failures=$((failures + 1))
                ;;
            go)
                run_go_tests "$category" || failures=$((failures + 1))
                ;;
            python)
                run_python_tests "$category" || failures=$((failures + 1))
                ;;
        esac
    done
    
    if [[ $failures -eq 0 ]]; then
        echo -e "${GREEN}${BOLD}All $category tests passed!${NC}"
        return 0
    else
        echo -e "${RED}${BOLD}$failures language(s) reported test failures in $category tests${NC}"
        return 1
    fi
}

# Handle special category 'all'
run_all_tests() {
    local failures=0
    
    # In fast mode, only run unit and component tests
    if [[ "$MODE" == "fast" ]]; then
        run_category_tests "unit" || failures=$((failures + 1))
        run_category_tests "component" || failures=$((failures + 1))
    else
        run_category_tests "unit" || failures=$((failures + 1))
        run_category_tests "component" || failures=$((failures + 1))
        run_category_tests "integration" || failures=$((failures + 1))
        run_category_tests "acceptance" || failures=$((failures + 1))
        
        # Skip performance tests in CI mode unless explicitly requested
        if [[ "$MODE" != "ci" || "$CATEGORY" == "performance" ]]; then
            run_category_tests "performance" || failures=$((failures + 1))
        fi
    fi
    
    return $failures
}

# Generate final coverage report
generate_coverage_report() {
    if [[ "$COVERAGE" == "true" ]]; then
        echo -e "${BLUE}${BOLD}Generating unified coverage report...${NC}"
        if [[ -f "$PROJECT_ROOT/bin/polyglot-coverage.sh" ]]; then
            "$PROJECT_ROOT/bin/polyglot-coverage.sh" \
                $(if [[ "$VERBOSE" == "true" ]]; then echo "-v"; fi) \
                -l "$(IFS=,; echo "${LANGUAGES[*]}")"
        else
            echo -e "${YELLOW}polyglot-coverage.sh script not found, skipping unified coverage report${NC}"
        fi
    fi
}

# Main function
main() {
    parse_args "$@"
    print_plan
    
    # Record start time
    local start_time=$(date +%s)
    
    # Run tests based on category
    local status=0
    if [[ "$CATEGORY" == "all" ]]; then
        run_all_tests || status=$?
    else
        run_category_tests "$CATEGORY" || status=$?
    fi
    
    # Generate coverage report
    generate_coverage_report
    
    # Calculate and print execution time
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo -e "${BLUE}${BOLD}Test Execution Summary${NC}"
    echo "==============================="
    echo -e "${BOLD}Duration:${NC} $duration seconds"
    echo -e "${BOLD}Category:${NC} $CATEGORY"
    echo -e "${BOLD}Languages:${NC} ${LANGUAGES[*]}"
    echo -e "${BOLD}Mode:${NC} $MODE"
    
    if [[ $status -eq 0 ]]; then
        echo -e "${GREEN}${BOLD}✓ All tests passed!${NC}"
    else
        echo -e "${RED}${BOLD}✗ Some tests failed!${NC}"
    fi
    
    exit $status
}

# Run the main function
main "$@"