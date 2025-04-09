#!/bin/bash
#
# cross-language-test-harness.sh - Cross-language test harness for Rinna
#
# This script provides a comprehensive test harness for running and managing 
# tests that span multiple programming languages (Java, Go, Python) in the 
# Rinna project. It handles test environment setup, dependency management,
# cross-language communication, and result aggregation.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

set -eo pipefail

# Determine script and project directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_DIR="$PROJECT_ROOT/test/cross-language"
TEMP_DIR="$PROJECT_ROOT/target/cross-language-tests"

# Source common utilities and formatter
source "$SCRIPT_DIR/common/rinna_utils.sh"
source "$SCRIPT_DIR/common/rinna_logger.sh"
source "$SCRIPT_DIR/formatters/build_formatter.sh"

# Set default values for configuration
VERBOSE=false
DEBUG=false
SKIP_BUILD=false
TEST_MODE="all"       # all, java-go, java-python, go-python, specific
SPECIFIC_TEST=""
PORT_OFFSET=0
CLEANUP=true
LOG_LEVEL="info"
TEST_TIMEOUT=120      # Default timeout in seconds
PARALLEL=false

# Configure colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'          # No Color

# Persistent configuration
CONFIG_FILE="$PROJECT_ROOT/.rinna-config/cross-language-test.conf"
mkdir -p "$(dirname "$CONFIG_FILE")" 2>/dev/null || true

# Test ports for various services
DEFAULT_API_PORT=8085
DEFAULT_MOCK_PORT=8086
DEFAULT_WEB_PORT=8087

# Process IDs for background services
API_PID=""
MOCK_API_PID=""
WEB_SERVER_PID=""

# Test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0
TEST_START_TIME=0
TEST_RESULTS=()

# Display usage information
usage() {
  cat <<EOF
Usage: $0 [options]

Cross-language test harness for Rinna - runs tests spanning Java, Go, and Python components.

Options:
  -h, --help             Show this help message
  -v, --verbose          Show detailed test output
  -d, --debug            Enable debug mode with additional logging
  --skip-build           Skip building components before testing
  --mode <mode>          Test mode: all, java-go, java-python, go-python
  --test <name>          Run a specific test by name
  --port-offset <n>      Offset all port numbers by n (for parallel runs)
  --no-cleanup           Don't clean up temporary resources after tests
  --log-level <level>    Set log level: debug, info, warn, error
  --timeout <seconds>    Maximum test execution time (default: 120s)
  --parallel             Run compatible tests in parallel

Examples:
  $0                     # Run all cross-language tests
  $0 --mode java-go      # Run only Java-Go integration tests
  $0 --test WorkItemSync # Run a specific test by name
  $0 --port-offset 10    # Use ports offset by 10 from defaults
  $0 --debug             # Run with debug logging enabled
EOF
}

# Parse command line arguments
parse_arguments() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -h|--help)
        usage
        exit 0
        ;;
      -v|--verbose)
        VERBOSE=true
        shift
        ;;
      -d|--debug)
        DEBUG=true
        LOG_LEVEL="debug"
        shift
        ;;
      --skip-build)
        SKIP_BUILD=true
        shift
        ;;
      --mode)
        if [[ "$2" == "all" || "$2" == "java-go" || "$2" == "java-python" || "$2" == "go-python" ]]; then
          TEST_MODE="$2"
        else
          echo -e "${RED}Error: Invalid test mode: $2${NC}"
          usage
          exit 1
        fi
        shift 2
        ;;
      --test)
        TEST_MODE="specific"
        SPECIFIC_TEST="$2"
        shift 2
        ;;
      --port-offset)
        if [[ "$2" =~ ^[0-9]+$ ]]; then
          PORT_OFFSET="$2"
        else
          echo -e "${RED}Error: Port offset must be a positive integer${NC}"
          exit 1
        fi
        shift 2
        ;;
      --no-cleanup)
        CLEANUP=false
        shift
        ;;
      --log-level)
        if [[ "$2" == "debug" || "$2" == "info" || "$2" == "warn" || "$2" == "error" ]]; then
          LOG_LEVEL="$2"
        else
          echo -e "${RED}Error: Invalid log level: $2${NC}"
          exit 1
        fi
        shift 2
        ;;
      --timeout)
        if [[ "$2" =~ ^[0-9]+$ ]]; then
          TEST_TIMEOUT="$2"
        else
          echo -e "${RED}Error: Timeout must be a positive integer${NC}"
          exit 1
        fi
        shift 2
        ;;
      --parallel)
        PARALLEL=true
        shift
        ;;
      *)
        echo -e "${RED}Error: Unknown option: $1${NC}"
        usage
        exit 1
        ;;
    esac
  done
}

# Initialize test environment
initialize_environment() {
  # Create directories
  mkdir -p "$TEMP_DIR"
  mkdir -p "$TEST_DIR"
  
  # Calculate ports with offset
  API_PORT=$((DEFAULT_API_PORT + PORT_OFFSET))
  MOCK_PORT=$((DEFAULT_MOCK_PORT + PORT_OFFSET))
  WEB_PORT=$((DEFAULT_WEB_PORT + PORT_OFFSET))
  
  # Check if ports are available
  check_port_available "$API_PORT" "API"
  check_port_available "$MOCK_PORT" "Mock API"
  check_port_available "$WEB_PORT" "Web Server"
  
  # Export variables for use in tests
  export RINNA_TEST_API_PORT="$API_PORT"
  export RINNA_TEST_MOCK_PORT="$MOCK_PORT"
  export RINNA_TEST_WEB_PORT="$WEB_PORT"
  export RINNA_TEST_TEMP_DIR="$TEMP_DIR"
  export RINNA_TEST_LOG_LEVEL="$LOG_LEVEL"
  export RINNA_TEST_VERBOSE="$VERBOSE"
  export RINNA_TEST_DEBUG="$DEBUG"
  
  log_info "Cross-language test harness initialized"
  log_debug "API port: $API_PORT, Mock port: $MOCK_PORT, Web port: $WEB_PORT"
}

# Check if a port is available
check_port_available() {
  local port="$1"
  local service_name="$2"
  
  # Check if port is already in use
  if command -v nc >/dev/null 2>&1; then
    if nc -z localhost "$port" >/dev/null 2>&1; then
      log_error "Port $port for $service_name is already in use"
      exit 1
    fi
  elif command -v lsof >/dev/null 2>&1; then
    if lsof -i:"$port" >/dev/null 2>&1; then
      log_error "Port $port for $service_name is already in use"
      exit 1
    fi
  else
    log_warn "Cannot check port availability (nc or lsof not found)"
  fi
}

# Start required background services for testing
start_background_services() {
  case "$TEST_MODE" in
    all|java-go)
      start_go_api_server
      ;;
    java-python)
      # No background services needed for Java-Python tests
      ;;
    go-python)
      start_go_api_server
      ;;
  esac
}

# Start the Go API server for testing
start_go_api_server() {
  log_info "Starting Go API server on port $API_PORT"
  
  if [[ ! -x "$PROJECT_ROOT/bin/rinnasrv" ]]; then
    log_error "Go API server binary not found at $PROJECT_ROOT/bin/rinnasrv"
    log_error "Run with --build to build all components first"
    exit 1
  fi
  
  # Create test configuration file for API server
  local api_config="$TEMP_DIR/test-api-config.yaml"
  cat > "$api_config" <<EOF
server:
  port: $API_PORT
  host: localhost
  cors: true
  readTimeout: 5s
  writeTimeout: 10s
  idleTimeout: 120s
  test: true
database:
  type: inmemory
EOF
  
  # Start API server with test configuration
  "$PROJECT_ROOT/bin/rinnasrv" --config="$api_config" > "$TEMP_DIR/api-server.log" 2>&1 &
  API_PID=$!
  
  # Wait for API server to start
  log_info "Waiting for API server to be ready (PID: $API_PID)"
  local max_attempts=15
  local attempt=0
  local ready=false
  
  while [[ $attempt -lt $max_attempts && $ready == "false" ]]; do
    attempt=$((attempt + 1))
    
    if curl -s "http://localhost:$API_PORT/api/health" >/dev/null 2>&1; then
      ready=true
      log_info "API server ready"
    else
      sleep 1
      log_debug "Waiting for API server (attempt $attempt/$max_attempts)..."
      
      # Check if process is still running
      if ! kill -0 $API_PID 2>/dev/null; then
        log_error "API server failed to start. Check logs at $TEMP_DIR/api-server.log"
        cat "$TEMP_DIR/api-server.log"
        exit 1
      fi
    fi
  done
  
  if [[ $ready == "false" ]]; then
    log_error "API server failed to start after $max_attempts attempts"
    kill $API_PID 2>/dev/null || true
    cat "$TEMP_DIR/api-server.log"
    exit 1
  fi
}

# Build all required components
build_components() {
  if [[ "$SKIP_BUILD" == "true" ]]; then
    log_info "Skipping component build"
    return
  fi
  
  log_info "Building required components"
  
  case "$TEST_MODE" in
    all|java-go|java-python)
      build_java_components
      ;;
  esac
  
  case "$TEST_MODE" in
    all|java-go|go-python)
      build_go_components
      ;;
  esac
  
  case "$TEST_MODE" in
    all|java-python|go-python)
      build_python_components
      ;;
  esac
}

# Build Java components
build_java_components() {
  log_info "Building Java components"
  run_formatted "mvn -f $PROJECT_ROOT/pom.xml -pl rinna-cli,rinna-core install -DskipTests" "Building Java modules"
}

# Build Go components
build_go_components() {
  log_info "Building Go components"
  run_formatted "cd $PROJECT_ROOT/api && go build -o ../bin/rinnasrv ./cmd/rinnasrv" "Building Go API server"
}

# Build Python components
build_python_components() {
  log_info "Building Python components"
  run_formatted "cd $PROJECT_ROOT/python && pip install -e ." "Installing Python package"
}

# Run integration tests based on test mode
run_integration_tests() {
  log_info "Running integration tests (mode: $TEST_MODE)"
  
  # Track start time
  TEST_START_TIME=$(date +%s)
  
  # Setup test environment
  export RINNA_TEST_MODE="$TEST_MODE"
  export RINNA_TEST_DIR="$TEST_DIR"
  
  case "$TEST_MODE" in
    all)
      run_formatted_test "Run Java-Go integration tests" run_java_go_tests
      run_formatted_test "Run Java-Python integration tests" run_java_python_tests
      run_formatted_test "Run Go-Python integration tests" run_go_python_tests
      ;;
    java-go)
      run_formatted_test "Run Java-Go integration tests" run_java_go_tests
      ;;
    java-python)
      run_formatted_test "Run Java-Python integration tests" run_java_python_tests
      ;;
    go-python)
      run_formatted_test "Run Go-Python integration tests" run_go_python_tests
      ;;
    specific)
      run_formatted_test "Run specific test: $SPECIFIC_TEST" run_specific_test
      ;;
  esac
}

# Run Java-Go integration tests
run_java_go_tests() {
  log_info "Running Java-Go cross-language tests"
  
  # Run Java tests that interact with Go API
  mvn -f "$PROJECT_ROOT/pom.xml" -pl rinna-cli test \
    -Dtest="org.rinna.integration.api.*Test" \
    -Drinna.test.api.port="$API_PORT" \
    ${VERBOSE:+-Dsurefire.useFile=false}
    
  # Get test results
  local result=$?
  return $result
}

# Run Java-Python integration tests
run_java_python_tests() {
  log_info "Running Java-Python cross-language tests"
  
  # Check for Python availability
  if ! command -v python >/dev/null 2>&1; then
    log_error "Python not found in PATH"
    return 1
  fi
  
  # Run Python tests that interact with Java
  cd "$PROJECT_ROOT"
  python -m pytest python/tests/integration/test_integration_cli.py \
    ${VERBOSE:+-v}
    
  # Get test results
  local result=$?
  return $result
}

# Run Go-Python integration tests
run_go_python_tests() {
  log_info "Running Go-Python cross-language tests"
  
  # Check for Python availability
  if ! command -v python >/dev/null 2>&1; then
    log_error "Python not found in PATH"
    return 1
  fi
  
  # Run Python tests that interact with Go API
  cd "$PROJECT_ROOT"
  python -m pytest python/tests/integration/test_integration_api.py \
    ${VERBOSE:+-v}
    
  # Get test results
  local result=$?
  return $result
}

# Run a specific named test
run_specific_test() {
  log_info "Running specific test: $SPECIFIC_TEST"
  
  # Look for test in Java test classes
  if find "$PROJECT_ROOT" -path "*/src/test/java/org/rinna/integration*" -name "*${SPECIFIC_TEST}*Test.java" | grep -q .; then
    run_formatted "mvn -f $PROJECT_ROOT/pom.xml test -Dtest=\"*${SPECIFIC_TEST}*Test\" ${VERBOSE:+-Dsurefire.useFile=false}" "Running Java test: $SPECIFIC_TEST"
    return $?
  fi
  
  # Look for test in Go test files
  if find "$PROJECT_ROOT/api/test" -name "*${SPECIFIC_TEST}*_test.go" | grep -q .; then
    run_formatted "cd $PROJECT_ROOT/api && go test ./... -run=${SPECIFIC_TEST} ${VERBOSE:+-v}" "Running Go test: $SPECIFIC_TEST"
    return $?
  fi
  
  # Look for test in Python test files
  if find "$PROJECT_ROOT/python/tests" -name "*${SPECIFIC_TEST}*.py" | grep -q .; then
    run_formatted "cd $PROJECT_ROOT && python -m pytest python/tests/*/${SPECIFIC_TEST}*.py ${VERBOSE:+-v}" "Running Python test: $SPECIFIC_TEST"
    return $?
  fi
  
  log_error "Test not found: $SPECIFIC_TEST"
  return 1
}

# Run a formatted test with proper output and error handling
run_formatted_test() {
  local description="$1"
  local test_function="$2"
  
  TOTAL_TESTS=$((TOTAL_TESTS + 1))
  
  printf "${BLUE}=== ${BOLD}%s${NC}${BLUE} ===${NC}\n" "$description"
  
  # Set timeout using timeout command if available
  if command -v timeout >/dev/null 2>&1; then
    if timeout "$TEST_TIMEOUT" bash -c "$test_function"; then
      printf "${GREEN}✓ ${BOLD}PASSED:${NC}${GREEN} %s${NC}\n" "$description"
      PASSED_TESTS=$((PASSED_TESTS + 1))
      TEST_RESULTS+=("PASS: $description")
      return 0
    else
      local result=$?
      if [[ $result -eq 124 ]]; then
        printf "${RED}✖ ${BOLD}TIMEOUT:${NC}${RED} %s (exceeded %s seconds)${NC}\n" "$description" "$TEST_TIMEOUT"
      else
        printf "${RED}✖ ${BOLD}FAILED:${NC}${RED} %s (exit code %d)${NC}\n" "$description" "$result"
      fi
      FAILED_TESTS=$((FAILED_TESTS + 1))
      TEST_RESULTS+=("FAIL: $description (code: $result)")
      return $result
    fi
  else
    # Fall back to running without timeout
    if $test_function; then
      printf "${GREEN}✓ ${BOLD}PASSED:${NC}${GREEN} %s${NC}\n" "$description"
      PASSED_TESTS=$((PASSED_TESTS + 1))
      TEST_RESULTS+=("PASS: $description")
      return 0
    else
      local result=$?
      printf "${RED}✖ ${BOLD}FAILED:${NC}${RED} %s (exit code %d)${NC}\n" "$description" "$result"
      FAILED_TESTS=$((FAILED_TESTS + 1))
      TEST_RESULTS+=("FAIL: $description (code: $result)")
      return $result
    fi
  fi
}

# Clean up resources
cleanup() {
  if [[ "$CLEANUP" != "true" ]]; then
    log_info "Skipping cleanup (--no-cleanup was specified)"
    return
  fi
  
  log_info "Cleaning up resources"
  
  # Stop background processes
  if [[ -n "$API_PID" ]]; then
    log_debug "Stopping API server (PID: $API_PID)"
    kill $API_PID 2>/dev/null || true
  fi
  
  if [[ -n "$MOCK_API_PID" ]]; then
    log_debug "Stopping mock API server (PID: $MOCK_API_PID)"
    kill $MOCK_API_PID 2>/dev/null || true
  fi
  
  if [[ -n "$WEB_SERVER_PID" ]]; then
    log_debug "Stopping web server (PID: $WEB_SERVER_PID)"
    kill $WEB_SERVER_PID 2>/dev/null || true
  fi
  
  # Keep logs but clean up temp files
  find "$TEMP_DIR" -type f -not -name "*.log" -delete 2>/dev/null || true
  
  # Remove environment variables
  unset RINNA_TEST_API_PORT
  unset RINNA_TEST_MOCK_PORT
  unset RINNA_TEST_WEB_PORT
  unset RINNA_TEST_TEMP_DIR
  unset RINNA_TEST_LOG_LEVEL
  unset RINNA_TEST_VERBOSE
  unset RINNA_TEST_DEBUG
  unset RINNA_TEST_MODE
  unset RINNA_TEST_DIR
}

# Print test summary
print_summary() {
  local end_time=$(date +%s)
  local duration=$((end_time - TEST_START_TIME))
  
  echo
  printf "${BLUE}${BOLD}Test Summary${NC}\n"
  printf "${BLUE}===================${NC}\n"
  printf "Mode: %s\n" "$TEST_MODE"
  printf "Total duration: %d seconds\n" "$duration"
  printf "Total tests: %d\n" "$TOTAL_TESTS"
  printf "${GREEN}Passed: %d${NC}\n" "$PASSED_TESTS"
  printf "${RED}Failed: %d${NC}\n" "$FAILED_TESTS"
  printf "${YELLOW}Skipped: %d${NC}\n" "$SKIPPED_TESTS"
  echo
  
  if [[ ${#TEST_RESULTS[@]} -gt 0 ]]; then
    printf "${BLUE}${BOLD}Test Results:${NC}\n"
    for result in "${TEST_RESULTS[@]}"; do
      if [[ "$result" == PASS:* ]]; then
        printf "${GREEN}%s${NC}\n" "$result"
      elif [[ "$result" == FAIL:* ]]; then
        printf "${RED}%s${NC}\n" "$result"
      elif [[ "$result" == SKIP:* ]]; then
        printf "${YELLOW}%s${NC}\n" "$result"
      else
        printf "%s\n" "$result"
      fi
    done
  fi
  
  echo
  if [[ "$FAILED_TESTS" -eq 0 ]]; then
    printf "${GREEN}${BOLD}All tests passed!${NC}\n"
  else
    printf "${RED}${BOLD}Some tests failed!${NC}\n"
  fi
}

# Log utilities
log_debug() {
  if [[ "$DEBUG" == "true" ]]; then
    printf "[$(date +%T)] ${BLUE}DEBUG: %s${NC}\n" "$1" >&2
  fi
}

log_info() {
  if [[ "$VERBOSE" == "true" || "$DEBUG" == "true" ]]; then
    printf "[$(date +%T)] ${CYAN}INFO: %s${NC}\n" "$1" >&2
  fi
}

log_warn() {
  printf "[$(date +%T)] ${YELLOW}WARN: %s${NC}\n" "$1" >&2
}

log_error() {
  printf "[$(date +%T)] ${RED}ERROR: %s${NC}\n" "$1" >&2
}

# Main function
main() {
  # Parse command line arguments
  parse_arguments "$@"
  
  # Initialize test environment
  initialize_environment
  
  # Show test configuration
  section_header "Cross-Language Test Harness (${TEST_MODE} mode)"
  
  # Setup cleanup handler for graceful exit
  trap cleanup EXIT
  
  # Build required components
  build_components
  
  # Start required background services
  start_background_services
  
  # Run integration tests
  run_integration_tests
  
  # Print test summary
  print_summary
  
  # Exit with appropriate status code
  if [[ "$FAILED_TESTS" -gt 0 ]]; then
    exit 1
  else
    exit 0
  fi
}

# Execute main function
main "$@"