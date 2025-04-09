#!/bin/bash
#
# run-polyglot-tests.sh - Run cross-language polyglot tests
#
# This script runs the cross-language integration tests for the Rinna project,
# testing the Java CLI, Go API, and Python components together.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

# Source common utilities and formatter
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

source "$SCRIPT_DIR/common/rinna_utils.sh"
source "$SCRIPT_DIR/formatters/build_formatter.sh"

# Command line options
VERBOSE=false
BUILD_FIRST=false
SPECIFIC_TEST=""

usage() {
  echo "Usage: $0 [options]"
  echo
  echo "Options:"
  echo "  -h, --help             Show this help message"
  echo "  -v, --verbose          Show detailed test output"
  echo "  -b, --build            Build all components before running tests"
  echo "  -t, --test <class>     Run a specific test class (e.g., CliApiIntegrationTest)"
  echo
  echo "Examples:"
  echo "  $0                     # Run all polyglot tests"
  echo "  $0 -v                  # Run all tests with verbose output"
  echo "  $0 -b                  # Build components and run all tests"
  echo "  $0 -t PythonIntegrationTest  # Run only the Python integration tests"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    -v|--verbose)
      VERBOSE=true
      export VERBOSE=true
      shift
      ;;
    -b|--build)
      BUILD_FIRST=true
      shift
      ;;
    -t|--test)
      SPECIFIC_TEST="$2"
      shift 2
      ;;
    *)
      echo -e "${RED}Error: Unknown option: $1${NC}"
      usage
      exit 1
      ;;
  esac
done

section_header "Rinna Cross-Language Polyglot Tests"

# Print environment information
echo "Environment Information:"
echo "- Using Java: $(java -version 2>&1 | head -1)"
echo "- Using Go: $(go version 2>&1 || echo "Go not found")"
echo "- Using Python: $(python --version 2>&1 || echo "Python not found")"
echo "- Working directory: $(pwd)"

# Build components if requested
if [[ "$BUILD_FIRST" == "true" ]]; then
  section_header "Building Components"
  
  # Build Java CLI
  run_formatted "mvn -pl rinna-cli clean install -DskipTests" "Building Java CLI"
  
  # Build Go API
  if [[ -d "$PROJECT_ROOT/api" ]]; then
    run_formatted "cd api && go build -o ../bin/rinnasrv ./cmd/rinnasrv" "Building Go API"
  else
    skip_task "Building Go API (directory not found)"
  fi
  
  # Install Python dependencies
  if [[ -d "$PROJECT_ROOT/python" ]]; then
    run_formatted "cd python && pip install -e ." "Installing Python dependencies"
  else
    skip_task "Installing Python dependencies (directory not found)"
  fi
fi

section_header "Running Tests"

# Setup command to run tests
if [[ -n "$SPECIFIC_TEST" ]]; then
  # Run specific test class
  TEST_CLASS="org.rinna.cli.polyglot.$SPECIFIC_TEST"
  echo -e "Running specific test class: $TEST_CLASS"
  
  if [[ "$VERBOSE" == "true" ]]; then
    run_formatted "mvn -pl rinna-cli test -Dtest=\"$TEST_CLASS\" -Dsurefire.useFile=false" "Running test class $SPECIFIC_TEST"
  else
    run_formatted "mvn -pl rinna-cli test -Dtest=\"$TEST_CLASS\"" "Running test class $SPECIFIC_TEST"
  fi
else
  # Run all polyglot tests
  if [[ "$VERBOSE" == "true" ]]; then
    run_formatted "mvn -pl rinna-cli test -Dgroups=\"polyglot\" -Dsurefire.useFile=false" "Running all polyglot tests"
  else
    run_formatted "mvn -pl rinna-cli test -Dgroups=\"polyglot\"" "Running all polyglot tests"
  fi
fi

section_header "Test Summary"
complete_task "Polyglot tests completed successfully"