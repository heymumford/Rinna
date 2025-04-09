#!/usr/bin/env bash
#
# rinna_logger.sh - Unified logging system for shell scripts
#
# This script provides a consistent logging framework used by
# all Rinna shell scripts for uniform output and log management.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

# Create log directory if it doesn't exist
LOG_DIR="${LOG_DIR:-$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")/../../../logs}"
mkdir -p "$LOG_DIR" 2>/dev/null || true

# ANSI color codes (if not already defined)
RED="${RED:-\033[0;31m}"
GREEN="${GREEN:-\033[0;32m}"
YELLOW="${YELLOW:-\033[0;33m}"
BLUE="${BLUE:-\033[0;34m}"
PURPLE="${PURPLE:-\033[0;35m}"
CYAN="${CYAN:-\033[0;36m}"
GRAY="${GRAY:-\033[0;37m}"
BOLD="${BOLD:-\033[1m}"
NC="${NC:-\033[0m}" # No Color

# Set module name for logging
CURRENT_MODULE="undefined"

# Set module name
set_module_name() {
  CURRENT_MODULE="$1"
}

# Get current timestamp
get_timestamp() {
  date "+%Y-%m-%d %H:%M:%S"
}

# Log an information message
log_info() {
  local message="$1"
  local timestamp=$(get_timestamp)
  echo -e "ℹ️ ${message}"
  echo "[INFO] $timestamp [$CURRENT_MODULE] $message" >> "$LOG_DIR/rinna.log"
}

# Log a warning message
log_warning() {
  local message="$1"
  local timestamp=$(get_timestamp)
  echo -e "⚠️ ${YELLOW}WARNING:${NC} ${message}"
  echo "[WARN] $timestamp [$CURRENT_MODULE] $message" >> "$LOG_DIR/rinna.log"
}

# Log an error message
log_error() {
  local message="$1"
  local timestamp=$(get_timestamp)
  echo -e "❌ ${RED}ERROR:${NC} ${message}" >&2
  echo "[ERROR] $timestamp [$CURRENT_MODULE] $message" >> "$LOG_DIR/rinna.log"
}

# Log a debug message (only if VERBOSE is true)
log_debug() {
  local message="$1"
  local timestamp=$(get_timestamp)
  if [[ "${VERBOSE:-false}" == "true" ]]; then
    echo -e "🔍 ${GRAY}DEBUG:${NC} ${message}"
  fi
  echo "[DEBUG] $timestamp [$CURRENT_MODULE] $message" >> "$LOG_DIR/rinna.log"
}