#!/bin/bash
# Script to run unit tests in a TDD-friendly way
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

set -e

# Configuration
TEST_PATTERN="*Test"
RERUN_FAILED=false
SINGLE_TEST=""
PACKAGE=""
COVERAGE=false
CONTINUOUS=false
VERBOSE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --failed)
      RERUN_FAILED=true
      shift
      ;;
    --test=*)
      SINGLE_TEST="${1#*=}"
      shift
      ;;
    --package=*)
      PACKAGE="${1#*=}"
      shift
      ;;
    --coverage)
      COVERAGE=true
      shift
      ;;
    --watch)
      CONTINUOUS=true
      shift
      ;;
    --verbose)
      VERBOSE=true
      shift
      ;;
    --help)
      echo "Usage: $(basename $0) [options]"
      echo
      echo "Options:"
      echo "  --failed          Re-run only failed tests from previous run"
      echo "  --test=TestName   Run a specific test class"
      echo "  --package=pkg     Run tests only in a specific package"
      echo "  --coverage        Generate and display coverage report"
      echo "  --watch           Continuously monitor and run tests on changes"
      echo "  --verbose         Show detailed test output"
      echo
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

# Build the Maven command
MVN_CMD="./mvnw"
MVN_GOALS="test"
MVN_PROPS=""

# Configure test selection
if [ -n "$SINGLE_TEST" ]; then
  MVN_PROPS="${MVN_PROPS} -Dtest=${SINGLE_TEST}"
elif $RERUN_FAILED; then
  MVN_PROPS="${MVN_PROPS} -Dsurefire.rerunFailingTestsCount=1"
elif [ -n "$PACKAGE" ]; then
  MVN_PROPS="${MVN_PROPS} -Dtest=${PACKAGE}/${TEST_PATTERN}"
else
  MVN_PROPS="${MVN_PROPS} -Dtest=${TEST_PATTERN}"
fi

# Configure test execution
if $VERBOSE; then
  MVN_PROPS="${MVN_PROPS} -Dsurefire.useFile=false"
else
  MVN_PROPS="${MVN_PROPS} -Dsurefire.useFile=true -q"
fi

# Configure coverage
if $COVERAGE; then
  MVN_GOALS="jacoco:prepare-agent ${MVN_GOALS} jacoco:report"
fi

# Run tests
function run_tests {
  echo "Running unit tests..."
  ${MVN_CMD} ${MVN_PROPS} ${MVN_GOALS}
  
  # Print test summary
  if [ -d "rinna-core/target/surefire-reports" ]; then
    TOTAL=$(find rinna-core/target/surefire-reports -name "TEST-*.xml" | wc -l)
    FAILED=$(find rinna-core/target/surefire-reports -name "TEST-*.xml" -exec grep -l "<failure" {} \; | wc -l)
    PASSED=$((TOTAL - FAILED))
    
    echo "========== UNIT TEST SUMMARY =========="
    echo "Total: ${TOTAL}"
    echo "Passed: ${PASSED}"
    echo "Failed: ${FAILED}"
    echo "======================================="
    
    # List failed tests if any
    if [ $FAILED -gt 0 ]; then
      echo "Failed tests:"
      find rinna-core/target/surefire-reports -name "TEST-*.xml" -exec grep -l "<failure" {} \; | while read file; do
        TEST_NAME=$(basename "$file" | sed 's/TEST-//' | sed 's/\.xml//')
        echo "  - $TEST_NAME"
      done
    fi
  fi
  
  # Show coverage report if requested
  if $COVERAGE && [ -f "rinna-core/target/site/jacoco/index.html" ]; then
    echo "Coverage report available at: $(realpath rinna-core/target/site/jacoco/index.html)"
    
    # Extract overall coverage percentage
    LINE_COV=$(grep -A 1 "Lines" rinna-core/target/site/jacoco/index.html | grep -o '[0-9.]*%' | head -1)
    BRANCH_COV=$(grep -A 1 "Branches" rinna-core/target/site/jacoco/index.html | grep -o '[0-9.]*%' | head -1)
    
    echo "Line coverage: ${LINE_COV}"
    echo "Branch coverage: ${BRANCH_COV}"
  fi
}

# Single run or continuous mode
if $CONTINUOUS; then
  echo "Watching for changes and running tests automatically..."
  run_tests
  
  DIRS_TO_WATCH="rinna-core/src/main rinna-core/src/test"
  
  # Detect changes and run tests
  while true; do
    echo "Watching for changes (press Ctrl+C to stop)..."
    CHANGES=$(inotifywait -r -e modify,create,delete $DIRS_TO_WATCH 2>/dev/null || echo "CHANGE")
    
    if [ -n "$CHANGES" ]; then
      echo "Changes detected, running tests..."
      run_tests
    fi
    
    sleep 1
  done
else
  run_tests
fi

exit 0