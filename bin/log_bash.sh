#!/usr/bin/env bash
#
# log_bash.sh - Bridge script for the multi-language logging system
#
# This script is called by the Java MultiLanguageLogger to log messages
# from Java code to the Bash logging system.
#
# Usage:
#   bash log_bash.sh --level INFO --name org.rinna.Main --message "Hello, world!" --field "key1=value1" --field "key2=value2"
#
# Arguments:
#   --level    Log level (TRACE, DEBUG, INFO, WARN, ERROR)
#   --name     Logger name
#   --message  Message to log
#   --field    Context field in the format key=value (can be specified multiple times)
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

# Source the common logger
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common/rinna_logger.sh"

# Ensure log directory exists
LOG_DIR="${RINNA_LOG_DIR:-$HOME/.rinna/logs}"
mkdir -p "$LOG_DIR" 2>/dev/null || echo "Warning: Could not create log directory $LOG_DIR" >&2

# Default variables
log_level="INFO"
module_name="java_bridge"
message=""
fields=()

# Validate field key (alphanumeric with underscores)
validate_field_key() {
    local key="$1"
    [[ "$key" =~ ^[a-zA-Z0-9_]+$ ]]
}

# Sanitize field key (convert invalid characters to underscores)
sanitize_field_key() {
    local key="$1"
    echo "${key//[^a-zA-Z0-9_]/_}"
}

# Ensure log directory exists
ensure_log_directory() {
    local dir="$1"
    
    # Check if directory exists
    if [[ -d "$dir" ]]; then
        return 0  # Directory exists
    elif [[ -e "$dir" ]]; then
        echo "Warning: Path exists but is not a directory: $dir" >&2
        return 1
    fi
    
    # Create directory with parents
    mkdir -p "$dir" 2>/dev/null
    if [[ $? -ne 0 ]]; then
        echo "Error creating log directory: $dir" >&2
        return 1
    fi
    
    return 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --level)
            log_level="$2"
            shift 2
            ;;
        --name)
            module_name="$2"
            shift 2
            ;;
        --message)
            message="$2"
            shift 2
            ;;
        --field)
            field_arg="$2"
            
            # Parse key=value format
            if [[ "$field_arg" =~ = ]]; then
                key="${field_arg%%=*}"
                value="${field_arg#*=}"
                
                # Trim whitespace
                key="${key#"${key%%[![:space:]]*}"}"
                key="${key%"${key##*[![:space:]]}"}"
                value="${value#"${value%%[![:space:]]*}"}"
                value="${value%"${value##*[![:space:]]}"}"
                
                # Validate key
                if [[ -z "$key" ]]; then
                    echo "Warning: Empty field key found, skipping" >&2
                elif ! validate_field_key "$key"; then
                    echo "Warning: Invalid field key '$key', using sanitized version" >&2
                    key="$(sanitize_field_key "$key")"
                    fields+=("$key=$value")
                else
                    fields+=("$key=$value")
                fi
            else
                echo "Warning: Invalid field format '$field_arg', expected key=value" >&2
            fi
            
            shift 2
            ;;
        *)
            echo "Unknown argument: $1" >&2
            exit 1
            ;;
    esac
done

# Set module name for logging
set_module_name "$module_name"

# Log the message with appropriate level and fields
case "${log_level^^}" in
    TRACE)
        if [[ ${#fields[@]} -gt 0 ]]; then
            log_with_fields "TRACE" "$message" "${fields[@]}"
        else
            log_trace "$message"
        fi
        ;;
    DEBUG)
        if [[ ${#fields[@]} -gt 0 ]]; then
            log_with_fields "DEBUG" "$message" "${fields[@]}"
        else
            log_debug "$message"
        fi
        ;;
    INFO)
        if [[ ${#fields[@]} -gt 0 ]]; then
            log_with_fields "INFO" "$message" "${fields[@]}"
        else
            log_info "$message"
        fi
        ;;
    WARN|WARNING)
        if [[ ${#fields[@]} -gt 0 ]]; then
            log_with_fields "WARN" "$message" "${fields[@]}"
        else
            log_warn "$message"
        fi
        ;;
    ERROR)
        if [[ ${#fields[@]} -gt 0 ]]; then
            log_with_fields "ERROR" "$message" "${fields[@]}"
        else
            log_error "$message"
        fi
        ;;
    FATAL|CRITICAL)
        if [[ ${#fields[@]} -gt 0 ]]; then
            log_with_fields "FATAL" "$message" "${fields[@]}"
        else
            log_fatal "$message"
        fi
        ;;
    *)
        log_warn "Unknown log level '$log_level', defaulting to INFO"
        if [[ ${#fields[@]} -gt 0 ]]; then
            log_with_fields "INFO" "$message" "${fields[@]}"
        else
            log_info "$message"
        fi
        ;;
esac