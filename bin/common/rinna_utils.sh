#!/usr/bin/env bash
#
# rinna_utils.sh - Common utility functions for Rinna shell scripts
#
# PURPOSE: Provide shared functions for all Rinna utility scripts
#          to ensure consistent behavior and reduce code duplication
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

# Ensure we fail on errors
set -eo pipefail

# Determine project directory
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
RINNA_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Constants
VERSION_FILE="$RINNA_DIR/version.properties"
LOG_DIR="$RINNA_DIR/logs"
DEFAULT_LOG_FILE="$LOG_DIR/rinna.log"
MAX_LOG_SIZE=10485760  # 10MB

# ANSI color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
BOLD='\033[1m'
RESET='\033[0m'
NC='\033[0m' # No Color

# Initialize log directory
mkdir -p "$LOG_DIR"

# UI functions
print_header() { echo -e "${BLUE}${BOLD}$1${NC}"; }
print_subheader() { echo -e "${CYAN}$1${NC}"; }
print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_warning() { echo -e "${YELLOW}\! $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}" >&2; }
print_fatal() { echo -e "${RED}${BOLD}FATAL ERROR: $1${NC}" >&2; exit 1; }
print_debug() { [[ "${VERBOSE:-$DEBUG}" == "true" ]] && echo -e "${GRAY}DEBUG: $1${NC}"; }
print_step() { echo -e "  ${MAGENTA}→ $1${NC}"; }

# Logging levels
LOG_LEVEL_DEBUG=0
LOG_LEVEL_INFO=1
LOG_LEVEL_WARNING=2
LOG_LEVEL_ERROR=3
LOG_LEVEL_FATAL=4

# Current log level - default to INFO
CURRENT_LOG_LEVEL=${CURRENT_LOG_LEVEL:-$LOG_LEVEL_INFO}

# Set current log level
set_log_level() {
  local level_name="$1"
  case "${level_name,,}" in
    debug) CURRENT_LOG_LEVEL=$LOG_LEVEL_DEBUG ;;
    info) CURRENT_LOG_LEVEL=$LOG_LEVEL_INFO ;;
    warning|warn) CURRENT_LOG_LEVEL=$LOG_LEVEL_WARNING ;;
    error) CURRENT_LOG_LEVEL=$LOG_LEVEL_ERROR ;;
    fatal) CURRENT_LOG_LEVEL=$LOG_LEVEL_FATAL ;;
    *) print_warning "Unknown log level: $level_name, using INFO" >&2
       CURRENT_LOG_LEVEL=$LOG_LEVEL_INFO ;;
  esac
}

# Advanced logging function with log rotation
log() {
  local level="$1"
  local message="$2"
  local log_file="${3:-$DEFAULT_LOG_FILE}"
  local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
  local log_level_num
  local log_level_name
  
  # Set numeric level and name based on input
  case "${level,,}" in
    debug|0) 
      log_level_num=$LOG_LEVEL_DEBUG
      log_level_name="DEBUG"
      ;;
    info|1) 
      log_level_num=$LOG_LEVEL_INFO
      log_level_name="INFO"
      ;;
    warning|warn|2) 
      log_level_num=$LOG_LEVEL_WARNING
      log_level_name="WARNING"
      ;;
    error|3) 
      log_level_num=$LOG_LEVEL_ERROR
      log_level_name="ERROR"
      ;;
    fatal|4) 
      log_level_num=$LOG_LEVEL_FATAL
      log_level_name="FATAL"
      ;;
    *) 
      log_level_num=$LOG_LEVEL_INFO
      log_level_name="INFO"
      ;;
  esac
  
  # Only log if level is greater than or equal to current log level
  if [ "$log_level_num" -ge "$CURRENT_LOG_LEVEL" ]; then
    # Create log directory if it doesn't exist
    mkdir -p "$(dirname "$log_file")"
    
    # Check log file size and rotate if needed
    if [ -f "$log_file" ] && [ $(stat -c%s "$log_file" 2>/dev/null || stat -f%z "$log_file") -gt $MAX_LOG_SIZE ]; then
      mv "$log_file" "${log_file}.1"
      # Remove old rotated logs to prevent disk filling
      rm -f "${log_file}.5"
      [ -f "${log_file}.4" ] && mv "${log_file}.4" "${log_file}.5"
      [ -f "${log_file}.3" ] && mv "${log_file}.3" "${log_file}.4"
      [ -f "${log_file}.2" ] && mv "${log_file}.2" "${log_file}.3"
      [ -f "${log_file}.1" ] && mv "${log_file}.1" "${log_file}.2"
    fi
    
    # Log to file
    echo "[$timestamp][$log_level_name] $message" >> "$log_file"
    
    # Output to console with colors based on level
    case "$log_level_num" in
      $LOG_LEVEL_DEBUG) echo -e "${GRAY}[$log_level_name] $message${NC}" ;;
      $LOG_LEVEL_INFO) echo -e "${BLUE}[$log_level_name] $message${NC}" ;;
      $LOG_LEVEL_WARNING) echo -e "${YELLOW}[$log_level_name] $message${NC}" ;;
      $LOG_LEVEL_ERROR) echo -e "${RED}[$log_level_name] $message${NC}" >&2 ;;
      $LOG_LEVEL_FATAL) echo -e "${RED}${BOLD}[$log_level_name] $message${NC}" >&2 ;;
    esac
    
    # Exit if fatal
    if [ "$log_level_num" -eq "$LOG_LEVEL_FATAL" ]; then
      exit 1
    fi
  fi
}

# Detect OS and architecture
detect_os() {
  case "$(uname -s)" in
    Linux*)
      OS="linux"
      ;;
    Darwin*)
      OS="macos"
      ;;
    MINGW*|CYGWIN*|MSYS*)
      OS="windows"
      ;;
    *)
      OS="unknown"
      ;;
  esac
  
  # Detect architecture
  ARCH="$(uname -m)"
  case "$ARCH" in
    x86_64|amd64)
      ARCH="amd64"
      ;;
    aarch64|arm64)
      ARCH="arm64"
      ;;
    armv7l)
      ARCH="arm"
      ;;
    *)
      ARCH="unknown"
      ;;
  esac
  
  # Return values
  echo "$OS"
  echo "$ARCH"
}

# Detect if running in CI environment
is_ci() {
  if [[ -n "${CI:-}" || -n "${GITHUB_ACTIONS:-}" || -n "${GITLAB_CI:-}" || -n "${JENKINS_URL:-}" || -n "${TRAVIS:-}" ]]; then
    echo "true"
  else
    echo "false"
  fi
}

# Check for necessary tools/dependencies
check_command() {
  local cmd="$1"
  local message="${2:-$cmd is required but not found in PATH}"
  if ! command -v "$cmd" &> /dev/null; then
    print_error "$message"
    return 1
  fi
  return 0
}

# Check Java version
check_java_version() {
  local required_version="${1:-21}"
  
  # Check if java is installed
  if ! check_command "java" "Java is required but not found in PATH"; then
    return 1
  fi
  
  # Get Java version
  local java_version
  java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
  
  # If version is 1, it's old-style (1.8 = Java 8), so get the second part
  if [[ "$java_version" == "1" ]]; then
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f2)
  fi
  
  # Compare versions
  if [[ "$java_version" -lt "$required_version" ]]; then
    print_error "Java version $required_version or higher is required, but found version $java_version"
    return 1
  fi
  
  print_debug "Java version $java_version detected (required: $required_version)"
  return 0
}

# Check Go version
check_go_version() {
  local required_version="${1:-1.20}"
  
  # Check if go is installed
  if ! check_command "go" "Go is required but not found in PATH"; then
    return 1
  fi
  
  # Get Go version
  local go_version
  go_version=$(go version | awk '{print $3}' | sed 's/go//')
  
  # Compare versions (simple string comparison for now)
  if [[ $(printf '%s\n' "$required_version" "$go_version" | sort -V | head -n1) != "$required_version" ]]; then
    print_error "Go version $required_version or higher is required, but found version $go_version"
    return 1
  fi
  
  print_debug "Go version $go_version detected (required: $required_version)"
  return 0
}

# Check Python version
check_python_version() {
  local required_version="${1:-3.9}"
  
  # Check if python3 is installed
  if ! check_command "python3" "Python 3 is required but not found in PATH"; then
    return 1
  fi
  
  # Get Python version
  local python_version
  python_version=$(python3 --version 2>&1 | awk '{print $2}')
  
  # Compare versions (simple string comparison for now)
  if [[ $(printf '%s\n' "$required_version" "$python_version" | sort -V | head -n1) != "$required_version" ]]; then
    print_error "Python version $required_version or higher is required, but found version $python_version"
    return 1
  fi
  
  print_debug "Python version $python_version detected (required: $required_version)"
  return 0
}

# Get Rinna version from version.properties
get_version() {
  if [ ! -f "$VERSION_FILE" ]; then
    print_error "Version file not found: $VERSION_FILE"
    echo "unknown"
    return 1
  fi
  
  local version
  version=$(grep -m 1 "^version=" "$VERSION_FILE" | cut -d'=' -f2)
  
  if [[ -z "$version" ]]; then
    print_error "Failed to parse version from $VERSION_FILE"
    echo "unknown"
    return 1
  fi
  
  echo "$version"
  return 0
}

# Get a property from version.properties
get_property() {
  local prop="$1"
  local default_value="${2:-}"
  
  if [ ! -f "$VERSION_FILE" ]; then
    print_error "Version file not found: $VERSION_FILE"
    echo "$default_value"
    return 1
  fi
  
  local value
  value=$(grep -m 1 "^$prop=" "$VERSION_FILE" | cut -d'=' -f2)
  
  if [[ -z "$value" ]]; then
    if [[ -n "$default_value" ]]; then
      echo "$default_value"
      return 0
    else
      print_error "Property '$prop' not found in $VERSION_FILE"
      return 1
    fi
  fi
  
  echo "$value"
  return 0
}

# Parse boolean value (handles various forms)
parse_bool() {
  local value="${1,,}"  # Convert to lowercase
  
  case "$value" in
    true|yes|y|1|on)
      echo "true"
      ;;
    false|no|n|0|off)
      echo "false"
      ;;
    *)
      echo "false"
      ;;
  esac
}

# Safely create a backup of a file or directory
create_backup() {
  local target="$1"
  local suffix="${2:-$(date +%Y%m%d%H%M%S)}"
  
  if [ ! -e "$target" ]; then
    print_warning "Cannot backup non-existent path: $target"
    return 1
  fi
  
  local backup_path="${target}_backup_${suffix}"
  
  if [ -d "$target" ]; then
    cp -r "$target" "$backup_path"
  else
    cp "$target" "$backup_path"
  fi
  
  print_success "Created backup: $backup_path"
  echo "$backup_path"
  return 0
}

# Find all Maven projects
find_maven_projects() {
  find "$RINNA_DIR" -name "pom.xml" -not -path "*/target/*" -not -path "*/\.*" | sort
}

# Execute a command with a timeout and capture output
execute_with_timeout() {
  local cmd="$1"
  local timeout="${2:-60}"
  local description="${3:-Command execution}"
  local verbose="${4:-false}"
  
  if [[ "$verbose" == "true" ]]; then
    print_debug "Executing: $cmd"
  fi
  
  # Create target directory if it doesn't exist
  mkdir -p "$RINNA_DIR/target/temp"
  local temp_output="$RINNA_DIR/target/temp/cmd_output_$(date +%s%N).tmp"
  local exit_code=0
  
  if command -v timeout >/dev/null 2>&1; then
    # GNU timeout command available
    timeout "$timeout" bash -c "$cmd" > "$temp_output" 2>&1 || exit_code=$?
  else
    # Fallback using perl
    perl -e "alarm $timeout; exec '$cmd'" > "$temp_output" 2>&1 || exit_code=$?
  fi
  
  local output=$(cat "$temp_output")
  rm -f "$temp_output"
  
  # Handle timeout
  if [[ $exit_code -eq 124 ]]; then
    print_error "$description timed out after ${timeout}s"
    echo "TIMEOUT"
    return 124
  elif [[ $exit_code -ne 0 ]]; then
    print_error "$description failed with exit code $exit_code"
    if [[ "$verbose" == "true" ]]; then
      echo "$output"
    fi
    return $exit_code
  fi
  
  echo "$output"
  return 0
}

# Generate a JUnit XML report from test results
generate_junit_report() {
  local test_name="$1"
  local output_file="$2"
  local success="$3"
  local duration="${4:-0}"
  local failure_message="${5:-}"
  local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%S")
  
  mkdir -p "$(dirname "$output_file")"
  
  echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > "$output_file"
  echo "<testsuites>" >> "$output_file"
  echo "  <testsuite name=\"$test_name\" tests=\"1\" failures=\"$([ "$success" == "true" ] && echo "0" || echo "1")\" errors=\"0\" skipped=\"0\" timestamp=\"$timestamp\">" >> "$output_file"
  echo "    <testcase classname=\"$test_name\" name=\"execution\" time=\"$duration\">" >> "$output_file"
  
  # Add failure if test failed
  if [[ "$success" != "true" ]]; then
    echo "      <failure message=\"Test failed\">$failure_message</failure>" >> "$output_file"
  fi
  
  # Close XML document
  echo "    </testcase>" >> "$output_file"
  echo "  </testsuite>" >> "$output_file"
  echo "</testsuites>" >> "$output_file"
  
  print_success "Generated JUnit report: $output_file"
}

# Format test results as JSON
format_test_results_json() {
  local test_name="$1"
  local success="$2" 
  local duration="${3:-0}"
  local output="${4:-}"
  local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%S")
  
  echo "{"
  echo "  \"test\": \"$test_name\","
  echo "  \"result\": $([ "$success" == "true" ] && echo "\"success\"" || echo "\"failure\""),"
  echo "  \"duration\": $duration,"
  echo "  \"timestamp\": \"$timestamp\","
  echo "  \"output\": $(echo "$output" | python -c 'import json,sys; print(json.dumps(sys.stdin.read()))')"
  echo "}"
}

# Check if command exists and is executable
# Silent version for internal use
command_exists() {
  command -v "$1" &> /dev/null
}

# Convert string to uppercase
to_uppercase() {
  echo "$1" | tr '[:lower:]' '[:upper:]'
}

# Get valid work item types
get_work_item_types() {
  echo "BUG FEATURE TASK"
}

# Get valid work item statuses
get_work_item_statuses() {
  echo "TODO IN_PROGRESS REVIEW DONE"
}

# Get valid work item priorities
get_work_item_priorities() {
  echo "LOW MEDIUM HIGH CRITICAL"
}

# Generate a UUID
create_uuid() {
  cat /proc/sys/kernel/random/uuid 2>/dev/null || 
  python -c 'import uuid; print(uuid.uuid4())' 2>/dev/null ||
  echo "$(date +%s)-$(od -N 8 -t x /dev/urandom | head -1 | cut -d' ' -f2-)"
}

# Initialize Rinna directory structure
init_rinna_dir() {
  local project_root="$1"
  local rinna_dir="$project_root/.rinna"
  
  # Create directory structure if it doesn't exist
  if [ ! -d "$rinna_dir" ]; then
    mkdir -p "$rinna_dir/items"
    mkdir -p "$rinna_dir/workflow"
    print_success "Initialized Rinna workspace at $rinna_dir"
  fi
  
  echo "$rinna_dir"
  return 0
}

# Store work item to JSON file
store_work_item() {
  local rinna_dir="$1"
  local item_id="$2"
  local title="$3"
  local description="$4"
  local type="$5"
  local priority="$6"
  local status="$7"
  local assignee="$8"
  
  local items_dir="$rinna_dir/items"
  local item_file="$items_dir/${item_id}.json"
  
  # Make sure directory exists
  mkdir -p "$items_dir"
  
  # Create timestamp
  local created=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  
  # Create JSON file
  cat > "$item_file" << EOF
{
  "id": "$item_id",
  "title": "$title",
  "description": "$description",
  "type": "$type",
  "priority": "$priority",
  "status": "$status",
  "assignee": "$assignee",
  "created": "$created",
  "updated": "$created"
}
EOF
  
  # Return the file path if successful
  if [ -f "$item_file" ]; then
    echo "$item_file"
    return 0
  else
    return 1
  fi
}

# Ensure the script is being sourced, not executed
# Only apply this check when the script is being run directly, not when sourced by another script
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  echo -e "${RED}✗ This script should be sourced, not executed directly.${NC}" >&2
  echo -e "${RED}✗ Usage: source $(basename "${BASH_SOURCE[0]}")${NC}" >&2
  exit 1
else
  # Display a message when the script is sourced
  if [[ "${DEBUG:-}" == "true" || "${VERBOSE:-}" == "true" ]]; then
    echo -e "${GRAY}DEBUG: Rinna utilities loaded from $(basename "${BASH_SOURCE[0]}")${NC}"
  fi
fi
