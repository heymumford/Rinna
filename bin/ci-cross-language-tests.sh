#!/bin/bash
#
# ci-cross-language-tests.sh - CI integration for cross-language testing
#
# This script is designed to run cross-language tests in CI environments with
# proper setup, logging, and reporting to CI systems.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

set -eo pipefail

# Determine script and project directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
TEST_REPORTS_DIR="${PROJECT_ROOT}/target/cross-language-reports"
JUNIT_REPORT="${TEST_REPORTS_DIR}/cross-language-${TIMESTAMP}.xml"
SUMMARY_REPORT="${TEST_REPORTS_DIR}/summary-${TIMESTAMP}.md"
LOG_FILE="${TEST_REPORTS_DIR}/cross-language-${TIMESTAMP}.log"

# Configuration defaults
CI_MODE=false
PARALLEL=false
DEBUG=false
TEST_GROUPS=("core" "auth" "notification" "config" "performance" "security")
MAX_WORKERS=4
FAIL_FAST=false
PORT_BASE=9000  # Base port for parallel execution

# Define test groups and their test scripts
declare -A TEST_GROUP_SCRIPTS
TEST_GROUP_SCRIPTS["core"]="workitem_sync_test.sh java_python_integration_test.sh go_python_integration_test.sh"
TEST_GROUP_SCRIPTS["auth"]="authentication_flow_test.sh"
TEST_GROUP_SCRIPTS["notification"]="notification_system_test.sh"
TEST_GROUP_SCRIPTS["config"]="configuration_management_test.sh"
TEST_GROUP_SCRIPTS["performance"]="performance_benchmark_test.sh"
TEST_GROUP_SCRIPTS["security"]="security_validation_test.sh"

# Color configuration for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'  # No Color

# Function to display usage information
usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

CI integration script for cross-language testing in Rinna.

Options:
  --ci                 Run in CI mode (generates JUnit reports)
  --parallel           Run test groups in parallel
  --workers <n>        Maximum parallel workers (default: 4)
  --debug              Enable debug output
  --fail-fast          Stop on first test failure
  --groups <groups>    Comma-separated list of test groups to run
                       Available groups: core, auth, notification, config, all
  --port-base <port>   Base port for services (default: 9000)
  --junit <file>       Custom JUnit report output file
  --help               Show this help message and exit

Examples:
  $(basename "$0") --ci --parallel           # Run all tests in CI mode with parallel execution
  $(basename "$0") --groups=core,auth        # Run only core and auth test groups
  $(basename "$0") --debug --fail-fast       # Run with debug output and fail on first error
EOF
}

# Function to log messages
log() {
  local level="$1"
  local message="$2"
  local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
  local color=""
  
  case "$level" in
    INFO)  color="${CYAN}" ;;
    WARN)  color="${YELLOW}" ;;
    ERROR) color="${RED}" ;;
    DEBUG) color="${BLUE}" ;;
    *)     color="" ;;
  esac
  
  if [[ "$DEBUG" == "true" || "$level" != "DEBUG" ]]; then
    echo -e "[${timestamp}] ${color}${level}${NC}: ${message}" | tee -a "$LOG_FILE"
  fi
}

# Parse command line arguments
parse_arguments() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --ci)
        CI_MODE=true
        shift
        ;;
      --parallel)
        PARALLEL=true
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
      --groups=*)
        IFS=',' read -r -a TEST_GROUPS <<< "${1#*=}"
        shift
        ;;
      --groups)
        if [[ -n "$2" && "$2" != --* ]]; then
          IFS=',' read -r -a TEST_GROUPS <<< "$2"
          shift 2
        else
          log "ERROR" "Missing argument for --groups"
          usage
          exit 1
        fi
        ;;
      --workers=*)
        MAX_WORKERS="${1#*=}"
        shift
        ;;
      --workers)
        if [[ -n "$2" && "$2" != --* ]]; then
          MAX_WORKERS="$2"
          shift 2
        else
          log "ERROR" "Missing argument for --workers"
          usage
          exit 1
        fi
        ;;
      --port-base=*)
        PORT_BASE="${1#*=}"
        shift
        ;;
      --port-base)
        if [[ -n "$2" && "$2" != --* ]]; then
          PORT_BASE="$2"
          shift 2
        else
          log "ERROR" "Missing argument for --port-base"
          usage
          exit 1
        fi
        ;;
      --junit=*)
        JUNIT_REPORT="${1#*=}"
        shift
        ;;
      --junit)
        if [[ -n "$2" && "$2" != --* ]]; then
          JUNIT_REPORT="$2"
          shift 2
        else
          log "ERROR" "Missing argument for --junit"
          usage
          exit 1
        fi
        ;;
      --help)
        usage
        exit 0
        ;;
      *)
        log "ERROR" "Unknown option: $1"
        usage
        exit 1
        ;;
    esac
  done
  
  # Handle "all" group
  for i in "${!TEST_GROUPS[@]}"; do
    if [[ "${TEST_GROUPS[$i]}" == "all" ]]; then
      TEST_GROUPS=("core" "auth" "notification" "config")
      break
    fi
  done
  
  # Validate test groups
  for group in "${TEST_GROUPS[@]}"; do
    if [[ "$group" != "core" && "$group" != "auth" && "$group" != "notification" && "$group" != "config" ]]; then
      log "ERROR" "Invalid test group: $group"
      usage
      exit 1
    fi
  done
}

# Initialize test environment
initialize() {
  mkdir -p "$TEST_REPORTS_DIR"
  
  # Initialize log file
  : > "$LOG_FILE"
  
  # Initialize JUnit report
  if [[ "$CI_MODE" == "true" ]]; then
    cat > "$JUNIT_REPORT" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="Cross-Language Tests">
</testsuites>
EOF
  fi
  
  # Initialize summary report
  cat > "$SUMMARY_REPORT" <<EOF
# Cross-Language Test Summary

* Date: $(date)
* Mode: ${CI_MODE:+CI}${CI_MODE:-Local}
* Parallel: ${PARALLEL:+Yes}${PARALLEL:-No}
* Test Groups: ${TEST_GROUPS[*]}

## Results

| Test Group | Status | Duration | Details |
|------------|--------|----------|---------|
EOF
  
  log "INFO" "Initialized test environment"
  log "INFO" "Log file: $LOG_FILE"
  log "INFO" "JUnit report: $JUNIT_REPORT"
  log "INFO" "Summary report: $SUMMARY_REPORT"
}

# Run a single test group
run_test_group() {
  local group="$1"
  local port_offset="$2"
  local start_time=$(date +%s)
  local success=true
  local test_results=()
  
  log "INFO" "Running test group: $group (port offset: $port_offset)"
  
  # Get the test scripts for this group
  local scripts=(${TEST_GROUP_SCRIPTS["$group"]})
  
  for script in "${scripts[@]}"; do
    local script_path="${PROJECT_ROOT}/test/cross-language/${script}"
    local script_name="${script%.sh}"
    local script_start_time=$(date +%s)
    local result=0
    
    log "INFO" "Running test: $script"
    
    # Set environment variables for the test
    export RINNA_TEST_API_PORT=$((PORT_BASE + port_offset))
    export RINNA_TEST_MOCK_PORT=$((PORT_BASE + port_offset + 1))
    export RINNA_TEST_WEB_PORT=$((PORT_BASE + port_offset + 2))
    export RINNA_TEST_TEMP_DIR="${TEST_REPORTS_DIR}/temp-${group}-${script_name}"
    export RINNA_TEST_LOG_FILE="${TEST_REPORTS_DIR}/${script_name}.log"
    
    # Create temp directory
    mkdir -p "$RINNA_TEST_TEMP_DIR"
    
    # Run the test script
    if [[ "$DEBUG" == "true" ]]; then
      bash "$script_path" | tee -a "$RINNA_TEST_LOG_FILE" || result=$?
    else
      bash "$script_path" > "$RINNA_TEST_LOG_FILE" 2>&1 || result=$?
    fi
    
    local script_end_time=$(date +%s)
    local duration=$((script_end_time - script_start_time))
    
    if [[ "$result" -eq 0 ]]; then
      log "INFO" "Test passed: $script (${duration}s)"
      test_results+=("PASS:$script:$duration")
    else
      log "ERROR" "Test failed: $script (${duration}s)"
      test_results+=("FAIL:$script:$duration")
      success=false
      
      # Fail fast if enabled
      if [[ "$FAIL_FAST" == "true" ]]; then
        log "WARN" "Stopping due to test failure (--fail-fast enabled)"
        break
      fi
    fi
  done
  
  local end_time=$(date +%s)
  local total_duration=$((end_time - start_time))
  
  # Update JUnit report
  if [[ "$CI_MODE" == "true" ]]; then
    generate_junit_report "$group" "${test_results[@]}"
  fi
  
  # Return results
  if [[ "$success" == "true" ]]; then
    echo "PASS:$group:$total_duration"
  else
    echo "FAIL:$group:$total_duration"
  fi
}

# Generate JUnit report for a test group
generate_junit_report() {
  local group="$1"
  shift
  local test_results=("$@")
  
  # Create a temporary file for this test suite
  local temp_file="${TEST_REPORTS_DIR}/temp-${group}-junit.xml"
  
  # Calculate test counts
  local tests=${#test_results[@]}
  local failures=0
  local skipped=0
  
  for result in "${test_results[@]}"; do
    if [[ "$result" == FAIL:* ]]; then
      ((failures++))
    elif [[ "$result" == SKIP:* ]]; then
      ((skipped++))
    fi
  done
  
  # Create the test suite element
  cat > "$temp_file" <<EOF
  <testsuite name="${group}" tests="${tests}" failures="${failures}" skipped="${skipped}">
EOF
  
  # Add test case elements
  for result in "${test_results[@]}"; do
    IFS=':' read -r status script duration <<< "$result"
    local script_name="${script%.sh}"
    
    if [[ "$status" == "PASS" ]]; then
      cat >> "$temp_file" <<EOF
    <testcase name="${script_name}" classname="cross-language.${group}" time="${duration}"/>
EOF
    elif [[ "$status" == "FAIL" ]]; then
      cat >> "$temp_file" <<EOF
    <testcase name="${script_name}" classname="cross-language.${group}" time="${duration}">
      <failure message="Test failed">
        <![CDATA[
$(cat "${TEST_REPORTS_DIR}/${script_name}.log")
        ]]>
      </failure>
    </testcase>
EOF
    elif [[ "$status" == "SKIP" ]]; then
      cat >> "$temp_file" <<EOF
    <testcase name="${script_name}" classname="cross-language.${group}" time="${duration}">
      <skipped message="Test skipped"/>
    </testcase>
EOF
    fi
  done
  
  # Close the test suite element
  cat >> "$temp_file" <<EOF
  </testsuite>
EOF
  
  # Insert into the JUnit report
  sed -i "s#</testsuites>#  $(cat "$temp_file")\n</testsuites>#" "$JUNIT_REPORT"
  
  # Remove the temporary file
  rm -f "$temp_file"
}

# Update the summary report
update_summary_report() {
  local results=("$@")
  local total_passed=0
  local total_failed=0
  local total_duration=0
  
  # Add rows to summary table
  for result in "${results[@]}"; do
    IFS=':' read -r status group duration <<< "$result"
    local status_text=""
    
    if [[ "$status" == "PASS" ]]; then
      status_text="✅ Pass"
      ((total_passed++))
    else
      status_text="❌ Fail"
      ((total_failed++))
    fi
    
    ((total_duration += duration))
    
    cat >> "$SUMMARY_REPORT" <<EOF
| $group | $status_text | ${duration}s | [Log](${group}.log) |
EOF
  done
  
  # Add summary section
  cat >> "$SUMMARY_REPORT" <<EOF

## Summary

* Total Groups: ${#results[@]}
* Passed: $total_passed
* Failed: $total_failed
* Total Duration: ${total_duration}s

EOF
  
  # Add details for failed tests
  if [[ "$total_failed" -gt 0 ]]; then
    cat >> "$SUMMARY_REPORT" <<EOF

## Failed Tests

EOF
    
    for result in "${results[@]}"; do
      IFS=':' read -r status group duration <<< "$result"
      
      if [[ "$status" == "FAIL" ]]; then
        cat >> "$SUMMARY_REPORT" <<EOF
### $group

\`\`\`
$(grep -A 10 "Test failed" "${TEST_REPORTS_DIR}/${group}.log" || echo "No failure details available")
\`\`\`

EOF
      fi
    done
  fi
}

# Main function to run all tests
main() {
  parse_arguments "$@"
  initialize
  
  log "INFO" "Starting cross-language tests"
  log "INFO" "Test groups: ${TEST_GROUPS[*]}"
  log "INFO" "Parallel mode: $PARALLEL"
  log "INFO" "CI mode: $CI_MODE"
  
  local start_time=$(date +%s)
  local group_results=()
  
  if [[ "$PARALLEL" == "true" ]]; then
    # Run test groups in parallel with limited concurrency
    log "INFO" "Running test groups in parallel (max workers: $MAX_WORKERS)"
    
    # Track running processes
    local pids=()
    local groups=()
    local port_offsets=()
    local offset=0
    
    for group in "${TEST_GROUPS[@]}"; do
      # Wait if we've reached max workers
      while [[ ${#pids[@]} -ge $MAX_WORKERS ]]; do
        for i in "${!pids[@]}"; do
          if ! kill -0 ${pids[$i]} 2>/dev/null; then
            # Process finished - collect its output
            if [[ -f "${TEST_REPORTS_DIR}/result-${groups[$i]}.txt" ]]; then
              group_results+=("$(cat "${TEST_REPORTS_DIR}/result-${groups[$i]}.txt")")
              rm -f "${TEST_REPORTS_DIR}/result-${groups[$i]}.txt"
            else
              group_results+=("FAIL:${groups[$i]}:0")
              log "ERROR" "No result file for ${groups[$i]}"
            fi
            
            # Remove from tracking arrays
            unset pids[$i]
            unset groups[$i]
            unset port_offsets[$i]
            
            # Rebuild arrays to remove gaps
            pids=("${pids[@]}")
            groups=("${groups[@]}")
            port_offsets=("${port_offsets[@]}")
            break
          fi
        done
        sleep 0.5
      done
      
      # Start this test group
      run_test_group "$group" "$offset" > "${TEST_REPORTS_DIR}/result-${group}.txt" &
      local pid=$!
      pids+=($pid)
      groups+=("$group")
      port_offsets+=($offset)
      
      log "DEBUG" "Started test group $group with PID $pid on port offset $offset"
      
      # Increment port offset for next group
      offset=$((offset + 10))
    done
    
    # Wait for remaining processes to finish
    for i in "${!pids[@]}"; do
      log "DEBUG" "Waiting for test group ${groups[$i]} (PID: ${pids[$i]})"
      wait ${pids[$i]} || true
      
      if [[ -f "${TEST_REPORTS_DIR}/result-${groups[$i]}.txt" ]]; then
        group_results+=("$(cat "${TEST_REPORTS_DIR}/result-${groups[$i]}.txt")")
        rm -f "${TEST_REPORTS_DIR}/result-${groups[$i]}.txt"
      else
        group_results+=("FAIL:${groups[$i]}:0")
        log "ERROR" "No result file for ${groups[$i]}"
      fi
    done
  else
    # Run test groups sequentially
    log "INFO" "Running test groups sequentially"
    
    local offset=0
    for group in "${TEST_GROUPS[@]}"; do
      local result=$(run_test_group "$group" "$offset")
      group_results+=("$result")
      
      # If test failed and fail-fast is enabled, stop
      if [[ "$result" == FAIL:* && "$FAIL_FAST" == "true" ]]; then
        log "WARN" "Stopping due to test failure (--fail-fast enabled)"
        break
      fi
      
      # Increment port offset for next group
      offset=$((offset + 10))
    done
  fi
  
  # Calculate total results
  local end_time=$(date +%s)
  local total_duration=$((end_time - start_time))
  local total_passed=0
  local total_failed=0
  
  for result in "${group_results[@]}"; do
    if [[ "$result" == PASS:* ]]; then
      ((total_passed++))
    else
      ((total_failed++))
    fi
  done
  
  # Update summary report
  update_summary_report "${group_results[@]}"
  
  # Print summary
  log "INFO" "Test execution completed in ${total_duration}s"
  log "INFO" "Test groups passed: $total_passed"
  log "INFO" "Test groups failed: $total_failed"
  
  if [[ "$CI_MODE" == "true" ]]; then
    log "INFO" "JUnit report: $JUNIT_REPORT"
  fi
  
  log "INFO" "Summary report: $SUMMARY_REPORT"
  
  # Exit with appropriate status code
  if [[ "$total_failed" -gt 0 ]]; then
    log "ERROR" "Some test groups failed"
    exit 1
  else
    log "INFO" "All test groups passed"
    exit 0
  fi
}

main "$@"