#!/bin/bash
# Master script to run all version system tests

set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Available test levels
declare -a TEST_LEVELS=("unit" "integration" "all")

# Print usage information
print_usage() {
  echo "Usage: $0 [options]"
  echo ""
  echo "Options:"
  echo "  --unit            Run unit tests only"
  echo "  --integration     Run integration tests only"
  echo "  --all             Run all tests (default)"
  echo "  --verbose         Show detailed test output"
  echo "  --help            Show this help message"
  echo ""
  echo "Examples:"
  echo "  $0                Run all version system tests"
  echo "  $0 --unit         Run only unit tests"
  echo "  $0 --verbose      Run all tests with detailed output"
}

# Parse command line arguments
LEVEL="all"
VERBOSE=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --unit)
      LEVEL="unit"
      shift
      ;;
    --integration)
      LEVEL="integration"
      shift
      ;;
    --all)
      LEVEL="all"
      shift
      ;;
    --verbose)
      VERBOSE=true
      shift
      ;;
    --help)
      print_usage
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      print_usage
      exit 1
      ;;
  esac
done

# Banner display
echo -e "${YELLOW}====================================================${NC}"
echo -e "${YELLOW}       Rinna Version System Tests                    ${NC}"
echo -e "${YELLOW}====================================================${NC}"
echo ""
echo -e "Running ${YELLOW}$LEVEL${NC} tests..."
echo ""

# Track overall test results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Run unit tests
run_unit_tests() {
  echo -e "${YELLOW}Running unit tests...${NC}"
  
  # List of unit test scripts
  local unit_tests=(
    "$PROJECT_ROOT/bin/test/unit/test_unit_version.sh"
    "$PROJECT_ROOT/bin/test/unit/test_robust_version_updater.sh"
  )
  
  # Run each unit test
  for test in "${unit_tests[@]}"; do
    if [ -f "$test" ] && [ -x "$test" ]; then
      TOTAL_TESTS=$((TOTAL_TESTS + 1))
      
      # Get the test name
      local test_name=$(basename "$test")
      echo -e "${YELLOW}Running ${test_name}...${NC}"
      
      # Run the test
      if [ "$VERBOSE" = true ]; then
        if "$test"; then
          echo -e "${GREEN}PASSED:${NC} $test_name"
          PASSED_TESTS=$((PASSED_TESTS + 1))
        else
          echo -e "${RED}FAILED:${NC} $test_name"
          FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
      else
        if "$test" > /dev/null 2>&1; then
          echo -e "${GREEN}PASSED:${NC} $test_name"
          PASSED_TESTS=$((PASSED_TESTS + 1))
        else
          echo -e "${RED}FAILED:${NC} $test_name"
          FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
      fi
    else
      echo -e "${YELLOW}WARNING:${NC} Test script not found or not executable: $test"
    fi
  done
  
  echo ""
}

# Run integration tests
run_integration_tests() {
  echo -e "${YELLOW}Running integration tests...${NC}"
  
  # List of integration test scripts
  local integration_tests=(
    "$PROJECT_ROOT/bin/test/integration/test_integration_version.sh"
  )
  
  # Run each integration test
  for test in "${integration_tests[@]}"; do
    if [ -f "$test" ] && [ -x "$test" ]; then
      TOTAL_TESTS=$((TOTAL_TESTS + 1))
      
      # Get the test name
      local test_name=$(basename "$test")
      echo -e "${YELLOW}Running ${test_name}...${NC}"
      
      # Run the test
      if [ "$VERBOSE" = true ]; then
        if "$test"; then
          echo -e "${GREEN}PASSED:${NC} $test_name"
          PASSED_TESTS=$((PASSED_TESTS + 1))
        else
          echo -e "${RED}FAILED:${NC} $test_name"
          FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
      else
        if "$test" > /dev/null 2>&1; then
          echo -e "${GREEN}PASSED:${NC} $test_name"
          PASSED_TESTS=$((PASSED_TESTS + 1))
        else
          echo -e "${RED}FAILED:${NC} $test_name"
          FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
      fi
    else
      echo -e "${YELLOW}WARNING:${NC} Test script not found or not executable: $test"
    fi
  done
  
  echo ""
}

# Run tests based on level
if [ "$LEVEL" = "unit" ] || [ "$LEVEL" = "all" ]; then
  run_unit_tests
fi

if [ "$LEVEL" = "integration" ] || [ "$LEVEL" = "all" ]; then
  run_integration_tests
fi

# Print summary
echo -e "${YELLOW}====================================================${NC}"
echo -e "${YELLOW}                 Test Summary                        ${NC}"
echo -e "${YELLOW}====================================================${NC}"
echo -e "Total tests run: ${TOTAL_TESTS}"
echo -e "Passed: ${GREEN}${PASSED_TESTS}${NC}"
echo -e "Failed: ${RED}${FAILED_TESTS}${NC}"
echo ""

# Return appropriate exit code
if [ $FAILED_TESTS -eq 0 ]; then
  echo -e "${GREEN}All tests passed successfully!${NC}"
  exit 0
else
  echo -e "${RED}Some tests failed!${NC}"
  exit 1
fi