#!/bin/bash
#
# run-cross-language-tests.sh - Developer-friendly runner for cross-language tests
#
# This script provides a simple interface for running cross-language tests
# during local development with helpful output and options.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

set -eo pipefail

# Determine script and project directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Color configuration
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'  # No Color

# Print a section header
print_header() {
  echo -e "\n${BOLD}${BLUE}=== $1 ===${NC}\n"
}

# Print a test module header
print_test_header() {
  echo -e "${BOLD}${CYAN}• $1${NC}"
}

# Print success message
print_success() {
  echo -e "${GREEN}✓ $1${NC}"
}

# Print error message
print_error() {
  echo -e "${RED}✗ $1${NC}"
}

# Print warning message
print_warning() {
  echo -e "${YELLOW}! $1${NC}"
}

# Print usage information
usage() {
  cat <<EOF
Usage: $(basename "$0") [options] [test-name]

Developer tool for running cross-language tests in Rinna.

Options:
  -a, --all             Run all cross-language tests
  -c, --core            Run core data synchronization tests
  -u, --auth            Run authentication flow tests
  -n, --notification    Run notification system tests
  -g, --config          Run configuration management tests
  -e, --performance     Run performance benchmark tests
  -s, --security        Run security validation tests
  -p, --parallel        Run tests in parallel
  -v, --verbose         Show detailed test output
  -h, --help            Show this help message

Examples:
  $(basename "$0") --all                    # Run all tests
  $(basename "$0") --core --auth            # Run core and auth tests
  $(basename "$0") workitem_sync_test.sh    # Run a specific test
EOF
}

# Check if a command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Check environment and dependencies
check_environment() {
  print_header "Environment Check"
  
  # Check Java
  if command_exists java; then
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    print_success "Java found: $java_version"
  else
    print_error "Java not found"
    exit 1
  fi
  
  # Check Go
  if command_exists go; then
    go_version=$(go version | awk '{print $3}')
    print_success "Go found: $go_version"
  else
    print_error "Go not found"
    exit 1
  fi
  
  # Check Python
  if command_exists python; then
    python_version=$(python --version 2>&1)
    print_success "Python found: $python_version"
  else
    print_error "Python not found"
    exit 1
  fi
  
  # Check Maven
  if command_exists mvn; then
    mvn_version=$(mvn --version | head -1)
    print_success "Maven found: $mvn_version"
  else
    print_error "Maven not found"
    exit 1
  fi
  
  # Check required scripts
  local missing_scripts=false
  
  # Check cross-language harness script
  if [[ -x "$SCRIPT_DIR/cross-language-test-harness.sh" ]]; then
    print_success "Cross-language test harness script found"
  else
    print_error "Cross-language test harness script not found or not executable"
    missing_scripts=true
  fi
  
  # Check test scripts directory
  if [[ -d "$PROJECT_ROOT/test/cross-language" ]]; then
    local script_count=$(find "$PROJECT_ROOT/test/cross-language" -name "*_test.sh" | wc -l)
    print_success "Test scripts directory found ($script_count tests)"
  else
    print_error "Test scripts directory not found"
    missing_scripts=true
  fi
  
  if [[ "$missing_scripts" == "true" ]]; then
    print_warning "Some required scripts are missing. Please check the installation."
    exit 1
  fi
}

# Build required components
build_components() {
  print_header "Building Components"
  
  # Build Java components
  print_test_header "Building Java components..."
  if mvn -f "$PROJECT_ROOT/pom.xml" compile -DskipTests -q; then
    print_success "Java components built successfully"
  else
    print_error "Failed to build Java components"
    exit 1
  fi
  
  # Build Go components
  print_test_header "Building Go API..."
  if (cd "$PROJECT_ROOT/api" && go build -o ../bin/rinnasrv ./cmd/rinnasrv); then
    print_success "Go API built successfully"
  else
    print_error "Failed to build Go API"
    exit 1
  fi
  
  # Install Python package
  print_test_header "Setting up Python components..."
  if pip install -e "$PROJECT_ROOT/python/" -q; then
    print_success "Python components set up successfully"
  else
    print_error "Failed to set up Python components"
    exit 1
  fi
}

# Run standard test groups
run_test_groups() {
  local test_args=()
  
  # Add specified test groups
  for group in "$@"; do
    case "$group" in
      core)
        test_args+=("--mode=all")
        ;;
      auth)
        test_args+=("--test=authentication_flow")
        ;;
      notification)
        test_args+=("--test=notification_system")
        ;;
      config)
        test_args+=("--test=configuration_management")
        ;;
      performance)
        test_args+=("--test=performance_benchmark")
        ;;
      security)
        test_args+=("--test=security_validation")
        ;;
      *)
        print_warning "Unknown test group: $group"
        ;;
    esac
  done
  
  # Add common args
  if [[ "$PARALLEL" == "true" ]]; then
    test_args+=("--parallel")
  fi
  
  if [[ "$VERBOSE" == "true" ]]; then
    test_args+=("--verbose")
  fi
  
  # Run the harness with args
  print_header "Running Tests"
  echo -e "Test harness arguments: ${test_args[*]}\n"
  
  if "$SCRIPT_DIR/cross-language-test-harness.sh" "${test_args[@]}"; then
    print_header "Tests Completed Successfully"
    return 0
  else
    print_header "Tests Failed"
    return 1
  fi
}

# Run a specific test script
run_specific_test() {
  local test_name="$1"
  local test_path="$PROJECT_ROOT/test/cross-language/$test_name"
  
  if [[ ! -f "$test_path" ]]; then
    print_error "Test script not found: $test_name"
    exit 1
  fi
  
  print_header "Running Test: $test_name"
  
  # Make sure test script is executable
  chmod +x "$test_path"
  
  # Run the test
  if [[ "$VERBOSE" == "true" ]]; then
    if "$test_path"; then
      print_success "Test passed: $test_name"
      return 0
    else
      print_error "Test failed: $test_name"
      return 1
    fi
  else
    local temp_log=$(mktemp)
    
    if "$test_path" > "$temp_log" 2>&1; then
      print_success "Test passed: $test_name"
      rm -f "$temp_log"
      return 0
    else
      print_error "Test failed: $test_name"
      echo -e "\n${YELLOW}Test output:${NC}"
      cat "$temp_log"
      rm -f "$temp_log"
      return 1
    fi
  fi
}

# Main function
main() {
  local run_all=false
  local run_core=false
  local run_auth=false
  local run_notification=false
  local run_config=false
  local run_performance=false
  local run_security=false
  local specific_test=""
  
  # Default options
  PARALLEL=false
  VERBOSE=false
  
  # Parse command line arguments
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -a|--all)
        run_all=true
        shift
        ;;
      -c|--core)
        run_core=true
        shift
        ;;
      -u|--auth)
        run_auth=true
        shift
        ;;
      -n|--notification)
        run_notification=true
        shift
        ;;
      -g|--config)
        run_config=true
        shift
        ;;
      -e|--performance)
        run_performance=true
        shift
        ;;
      -s|--security)
        run_security=true
        shift
        ;;
      -p|--parallel)
        PARALLEL=true
        shift
        ;;
      -v|--verbose)
        VERBOSE=true
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      -*)
        print_error "Unknown option: $1"
        usage
        exit 1
        ;;
      *)
        specific_test="$1"
        shift
        ;;
    esac
  done
  
  # If no test groups specified and no specific test, assume --all
  if [[ "$run_all" == "false" && "$run_core" == "false" && "$run_auth" == "false" && \
        "$run_notification" == "false" && "$run_config" == "false" && \
        "$run_performance" == "false" && "$run_security" == "false" && \
        -z "$specific_test" ]]; then
    run_all=true
  fi
  
  # Print banner
  echo -e "${BOLD}${BLUE}"
  echo "╔═════════════════════════════════════════════════════════╗"
  echo "║       Rinna Cross-Language Test Runner                  ║"
  echo "╚═════════════════════════════════════════════════════════╝"
  echo -e "${NC}"
  
  # Check environment
  check_environment
  
  # Build components
  build_components
  
  # Run tests
  if [[ -n "$specific_test" ]]; then
    # Run a specific test
    run_specific_test "$specific_test"
  else
    # Prepare test groups
    local groups=()
    
    [[ "$run_all" == "true" || "$run_core" == "true" ]] && groups+=("core")
    [[ "$run_all" == "true" || "$run_auth" == "true" ]] && groups+=("auth")
    [[ "$run_all" == "true" || "$run_notification" == "true" ]] && groups+=("notification")
    [[ "$run_all" == "true" || "$run_config" == "true" ]] && groups+=("config")
    [[ "$run_all" == "true" || "$run_performance" == "true" ]] && groups+=("performance")
    [[ "$run_all" == "true" || "$run_security" == "true" ]] && groups+=("security")
    
    run_test_groups "${groups[@]}"
  fi
}

# Run main if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi