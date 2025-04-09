#!/bin/bash
#
# build_formatter.sh - Standard output formatting for Rinna scripts
#
# This script provides a standard way to format build output across
# all Rinna shell scripts, ensuring consistent user feedback.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/rinna_utils.sh"

# Progress indicators
SPINNER=("⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏")
SPINNER_INDEX=0

# Status indicators
STATUS_PENDING="🔄"
STATUS_SUCCESS="✅"
STATUS_FAILURE="❌"
STATUS_SKIPPED="⏭️" 
STATUS_WARNING="⚠️"

# Start a task with a spinner
start_task() {
  local message="$1"
  echo -e "${STATUS_PENDING} ${BOLD}${message}...${NC}"
  TASK_START_TIME=$(date +%s)
}

# Update task status with spinner (for long-running tasks)
update_task() {
  local message="$1"
  local spinner="${SPINNER[$SPINNER_INDEX]}"
  SPINNER_INDEX=$(( (SPINNER_INDEX + 1) % ${#SPINNER[@]} ))
  
  # Clear line and update with new spinner
  echo -ne "\r\033[K${spinner} ${message}... "
}

# Complete a task with success
complete_task() {
  local message="$1"
  local elapsed=""
  
  if [[ -n "$TASK_START_TIME" ]]; then
    local end_time=$(date +%s)
    local duration=$((end_time - TASK_START_TIME))
    elapsed=" (${duration}s)"
    unset TASK_START_TIME
  fi
  
  echo -e "\r\033[K${STATUS_SUCCESS} ${message}${elapsed}"
}

# Mark a task as failed
fail_task() {
  local message="$1"
  local error="${2:-}"
  local elapsed=""
  
  if [[ -n "$TASK_START_TIME" ]]; then
    local end_time=$(date +%s)
    local duration=$((end_time - TASK_START_TIME))
    elapsed=" (${duration}s)"
    unset TASK_START_TIME
  fi
  
  echo -e "\r\033[K${STATUS_FAILURE} ${message}${elapsed}"
  if [[ -n "$error" ]]; then
    echo -e "   ${RED}${error}${NC}"
  fi
}

# Mark a task as skipped
skip_task() {
  local message="$1"
  echo -e "\r\033[K${STATUS_SKIPPED} ${message} - skipped"
}

# Show a warning
warn_task() {
  local message="$1"
  echo -e "${STATUS_WARNING} ${message}"
}

# Format a section header
section_header() {
  local title="$1"
  local dash_length=$(($(tput cols) - ${#title} - 4))
  local dashes=$(printf '%*s' "$dash_length" | tr ' ' '─')
  
  echo ""
  echo -e "${BLUE}${BOLD}╭─ ${title} ${dashes}${NC}"
  echo ""
}

# Format a section footer
section_footer() {
  local dash_length=$(tput cols)
  local dashes=$(printf '%*s' "$dash_length" | tr ' ' '─')
  
  echo ""
  echo -e "${BLUE}${BOLD}╰${dashes}${NC}"
  echo ""
}

# Run a command with formatted output
run_formatted() {
  local cmd="$1"
  local description="$2"
  local skip_flag="${3:-false}"
  
  if [[ "$skip_flag" == "true" ]]; then
    skip_task "$description"
    return 0
  fi
  
  start_task "$description"
  
  # Run the command and capture output
  local temp_file=$(mktemp)
  if eval "$cmd" > "$temp_file" 2>&1; then
    complete_task "$description"
    if [[ "${VERBOSE:-false}" == "true" ]]; then
      echo ""
      cat "$temp_file"
      echo ""
    fi
    rm -f "$temp_file"
    return 0
  else
    local exit_code=$?
    fail_task "$description" "Command failed with exit code $exit_code"
    echo ""
    cat "$temp_file"
    echo ""
    rm -f "$temp_file"
    return $exit_code
  fi
}

# Show execution plan
show_execution_plan() {
  local title="$1"
  shift
  local steps=("$@")
  
  section_header "$title"
  for i in "${!steps[@]}"; do
    echo -e " ${GRAY}$(($i + 1)).${NC} ${steps[$i]}"
  done
  section_footer
}

# Test the formatter if run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  echo "Build Formatter Self-Test"
  echo "========================="
  
  section_header "Test Execution Plan"
  TEST_STEPS=(
    "Initialize the build environment"
    "Compile Java components"
    "Run unit tests"
    "Generate documentation"
  )
  show_execution_plan "Build Steps" "${TEST_STEPS[@]}"
  
  run_formatted "echo 'This is a successful command'" "Running a successful command"
  run_formatted "ls -la /nonexistent" "Running a failing command" && echo "This shouldn't be printed"
  run_formatted "echo 'This is a skipped command'" "Running a skipped command" true
  
  start_task "Running a task with spinner"
  for i in {1..5}; do
    update_task "Running a task with spinner (step $i of 5)"
    sleep 1
  done
  complete_task "Task with spinner completed"
  
  warn_task "This is a warning message"
  
  section_footer
fi