#!/usr/bin/env bash
#
# rinna_logger.sh - Standardized logging functions for Rinna bash scripts
#
# PURPOSE: Provide consistent logging across all Rinna shell scripts
#          that matches the cross-language logging standards
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

# Ensure we fail on errors
set -eo pipefail

# Get the base directory of the script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# Determine log directory
DEFAULT_LOG_DIR="${HOME}/.rinna/logs"
if [[ -n "$RINNA_LOG_DIR" ]]; then
    LOG_DIR="$RINNA_LOG_DIR"
else
    LOG_DIR="$DEFAULT_LOG_DIR"
fi

# Create log directory if it doesn't exist
mkdir -p "$LOG_DIR"

# Determine log file
DEFAULT_LOG_FILE="$LOG_DIR/rinna-bash.log"
if [[ -n "$RINNA_LOG_FILE" ]]; then
    LOG_FILE="$RINNA_LOG_FILE"
else
    LOG_FILE="$DEFAULT_LOG_FILE"
fi

# Get the name of the calling script or module
if [[ -n "$CALLER_NAME" ]]; then
    MODULE_NAME="$CALLER_NAME"
else
    MODULE_NAME=$(basename "${BASH_SOURCE[1]}" .sh)
    if [[ "$MODULE_NAME" == "bash" || "$MODULE_NAME" == "" ]]; then
        MODULE_NAME="shell"
    fi
fi

# Define log levels
LOG_LEVEL_TRACE=10
LOG_LEVEL_DEBUG=20
LOG_LEVEL_INFO=30
LOG_LEVEL_WARN=40
LOG_LEVEL_ERROR=50
LOG_LEVEL_FATAL=60

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

# Determine current log level from environment or default to INFO
_parse_log_level() {
    local level_name="${1:-INFO}"
    
    case "${level_name^^}" in
        TRACE) echo $LOG_LEVEL_TRACE ;;
        DEBUG) echo $LOG_LEVEL_DEBUG ;;
        INFO)  echo $LOG_LEVEL_INFO ;;
        WARN|WARNING) echo $LOG_LEVEL_WARN ;;
        ERROR) echo $LOG_LEVEL_ERROR ;;
        FATAL|CRITICAL) echo $LOG_LEVEL_FATAL ;;
        *)
            # Also handle numeric values
            if [[ "$level_name" =~ ^[0-9]+$ ]]; then
                echo "$level_name"
            else
                echo $LOG_LEVEL_INFO
            fi
            ;;
    esac
}

# Get the current log level
LOG_LEVEL=$(_parse_log_level "${RINNA_LOG_LEVEL:-INFO}")

# Log level to name mapping
_log_level_name() {
    local level=$1
    
    if (( level >= LOG_LEVEL_FATAL )); then
        echo "FATAL"
    elif (( level >= LOG_LEVEL_ERROR )); then
        echo "ERROR"
    elif (( level >= LOG_LEVEL_WARN )); then
        echo "WARN"
    elif (( level >= LOG_LEVEL_INFO )); then
        echo "INFO"
    elif (( level >= LOG_LEVEL_DEBUG )); then
        echo "DEBUG"
    else
        echo "TRACE"
    fi
}

# Log message with specific level
# Usage: _log_with_level level "message" [field_name=field_value ...]
_log_with_level() {
    local level=$1
    local message="$2"
    shift 2
    
    # Only log if level is greater than or equal to current log level
    if (( level < LOG_LEVEL )); then
        return 0
    fi
    
    # Format timestamp in ISO-8601 format
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    local level_name=$(_log_level_name "$level")
    
    # Process context fields
    local context=""
    if [[ $# -gt 0 ]]; then
        context=" $*"
    fi
    
    # Create message with timestamp, level, module, and message
    local formatted_message="$timestamp [$level_name] [$MODULE_NAME] $message$context"
    
    # Select color based on level
    local color
    case "$level_name" in
        FATAL) color="$RED$BOLD" ;;
        ERROR) color="$RED" ;;
        WARN)  color="$YELLOW" ;;
        INFO)  color="$BLUE" ;;
        DEBUG) color="$GRAY" ;;
        TRACE) color="$GRAY" ;;
        *)     color="$NC" ;;
    esac
    
    # Print to console with color
    echo -e "${color}${formatted_message}${NC}" >&2
    
    # Write to log file without color
    echo "$formatted_message" >> "$LOG_FILE"
    
    # Exit for fatal errors
    if [[ "$level_name" == "FATAL" ]]; then
        exit 1
    fi
}

# Primary logging functions
log_trace() {
    _log_with_level $LOG_LEVEL_TRACE "$@"
}

log_debug() {
    _log_with_level $LOG_LEVEL_DEBUG "$@"
}

log_info() {
    _log_with_level $LOG_LEVEL_INFO "$@"
}

log_warn() {
    _log_with_level $LOG_LEVEL_WARN "$@"
}

log_error() {
    _log_with_level $LOG_LEVEL_ERROR "$@"
}

log_fatal() {
    _log_with_level $LOG_LEVEL_FATAL "$@"
}

# Set the log level (can be used to override environment variable)
# Usage: set_log_level "INFO"
set_log_level() {
    LOG_LEVEL=$(_parse_log_level "$1")
    log_debug "Log level set to $(_log_level_name $LOG_LEVEL)"
}

# Log a message with specific context fields
# Usage: log_with_field "INFO" "message" "field_name" "field_value"
log_with_field() {
    local level_name="$1"
    local message="$2"
    local field_name="$3"
    local field_value="$4"
    
    local level=$(_parse_log_level "$level_name")
    _log_with_level $level "$message" "${field_name}=${field_value}"
}

# Log a message with multiple context fields
# Usage: log_with_fields "INFO" "message" "name1=value1 name2=value2 ..."
log_with_fields() {
    local level_name="$1"
    local message="$2"
    shift 2
    
    local level=$(_parse_log_level "$level_name")
    _log_with_level $level "$message" "$@"
}

# Rotate log file if it gets too large
rotate_log() {
    local max_size=${1:-10485760}  # Default 10MB
    
    if [[ -f "$LOG_FILE" ]]; then
        local size=$(stat -c%s "$LOG_FILE" 2>/dev/null || stat -f%z "$LOG_FILE")
        
        if (( size > max_size )); then
            log_debug "Rotating log file: $LOG_FILE (size: $size bytes)"
            
            # Create timestamped archive
            local timestamp=$(date +"%Y%m%d-%H%M%S")
            mv "$LOG_FILE" "${LOG_FILE}.${timestamp}.old"
            
            # Clean up old log files (keep last 5)
            ls -t "${LOG_FILE}".*.old 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true
            
            # Touch the file to create it
            touch "$LOG_FILE"
            
            # Log rotation event
            log_info "Log file rotated, archived to ${LOG_FILE}.${timestamp}.old"
        fi
    fi
}

# Sets the module name for logging
set_module_name() {
    MODULE_NAME="$1"
    log_debug "Module name set to $MODULE_NAME"
}

# Show current logging configuration
show_log_config() {
    log_info "Logging configuration:"
    log_info "Log level: $(_log_level_name $LOG_LEVEL) ($LOG_LEVEL)"
    log_info "Log file: $LOG_FILE"
    log_info "Module: $MODULE_NAME"
}

# Automatically rotate log on load
rotate_log

# Signal that the logger has been loaded
log_debug "Logger initialized"

# Execute self-test if the script is run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Self-test if run directly
    echo "Rinna Logger Self-Test"
    echo "======================"
    set_module_name "rinna_logger"
    set_log_level "TRACE"
    show_log_config
    
    log_trace "This is a TRACE message"
    log_debug "This is a DEBUG message"
    log_info "This is an INFO message"
    log_warn "This is a WARN message"
    log_error "This is an ERROR message"
    
    log_with_field "INFO" "This is a message with a field" "request_id" "12345"
    log_with_fields "INFO" "This is a message with multiple fields" "user_id=user-123" "action=login" "client_ip=192.168.1.1"
    
    echo "To see the log file:"
    echo "cat $LOG_FILE"
fi