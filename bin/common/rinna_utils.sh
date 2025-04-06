#!/bin/bash
# Common utilities for Rinna CLI commands
#
# This file contains shared functions used by multiple Rinna CLI commands.
# It should be sourced by other scripts, not executed directly.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# 
# Developed with analytical assistance from AI tools.
# All rights reserved.
# 
# This source code is licensed under the MIT License
# found in the LICENSE file in the root directory of this source tree.

# Use set -e when running as a standalone script (not when sourced)
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  set -e
fi

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color
BOLD="\033[1m"

# Echo helpers
function echo_info() { echo -e "${BLUE}${BOLD}$1${NC}"; }
function echo_success() { echo -e "${GREEN}${BOLD}$1${NC}"; }
function echo_warning() { echo -e "${YELLOW}${BOLD}$1${NC}"; }
function echo_error() { echo -e "${RED}${BOLD}$1${NC}"; }

# Get the Rinna data directory
function get_rinna_dir() {
    local PROJECT_ROOT="$1"
    if [ -z "$PROJECT_ROOT" ]; then
        PROJECT_ROOT="$(pwd)"
    fi
    
    echo "$PROJECT_ROOT/.rinna"
}

# Initialize the Rinna data directory
function init_rinna_dir() {
    local PROJECT_ROOT="$1"
    local RINNA_DIR=$(get_rinna_dir "$PROJECT_ROOT")
    
    if [ ! -d "$RINNA_DIR" ]; then
        mkdir -p "$RINNA_DIR/items"
        echo_info "Initialized Rinna repository structure at $RINNA_DIR" >&2
    fi
    
    echo "$RINNA_DIR"
}

# Get the default work item types
function get_work_item_types() {
    echo "BUG FEATURE TASK"
}

# Get the default work item statuses
function get_work_item_statuses() {
    echo "TODO IN_PROGRESS REVIEW DONE"
}

# Get the default work item priorities
function get_work_item_priorities() {
    echo "LOW MEDIUM HIGH CRITICAL"
}

# Convert a string to uppercase
function to_uppercase() {
    echo "$1" | tr '[:lower:]' '[:upper:]'
}

# Create a UUID
function create_uuid() {
    if command -v uuidgen &> /dev/null; then
        uuidgen
    else
        cat /proc/sys/kernel/random/uuid
    fi
}

# Store a work item in the repository
function store_work_item() {
    local RINNA_DIR="$1"
    local ID="$2"
    local TITLE="$3"
    local DESCRIPTION="$4"
    local TYPE="$5"
    local PRIORITY="$6"
    local STATUS="$7"
    local ASSIGNEE="$8"
    
    # Ensure items directory exists
    mkdir -p "$RINNA_DIR/items"
    
    # Create JSON file
    local ITEM_FILE="$RINNA_DIR/items/$ID.json"
    
    cat > "$ITEM_FILE" << EOF
{
  "id": "$ID",
  "title": "$TITLE",
  "description": "$DESCRIPTION",
  "type": "$TYPE",
  "priority": "$PRIORITY",
  "status": "$STATUS",
  "assignee": "$ASSIGNEE",
  "createdAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "updatedAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
}
EOF
    
    echo "$ITEM_FILE"
}

# Get all work items
function get_all_work_items() {
    local RINNA_DIR="$1"
    local ITEMS_DIR="$RINNA_DIR/items"
    
    if [ ! -d "$ITEMS_DIR" ]; then
        return 1
    fi
    
    find "$ITEMS_DIR" -type f -name "*.json" | sort
}

# Filter work items by property value
function filter_work_items() {
    local PROPERTY="$1"
    local VALUE="$2"
    local FILES=("${@:3}")
    
    local FILTERED_FILES=()
    
    for FILE in "${FILES[@]}"; do
        if grep -q "\"$PROPERTY\": \"$VALUE\"" "$FILE"; then
            FILTERED_FILES+=("$FILE")
        fi
    done
    
    echo "${FILTERED_FILES[@]}"
}

# Get work item count
function get_work_item_count() {
    local RINNA_DIR="$1"
    local ITEMS_DIR="$RINNA_DIR/items"
    
    if [ ! -d "$ITEMS_DIR" ]; then
        echo 0
        return
    fi
    
    find "$ITEMS_DIR" -type f -name "*.json" | wc -l
}

# Truncate text to a maximum length
function truncate_text() {
    local TEXT="$1"
    local MAX_LENGTH="$2"
    
    if [ ${#TEXT} -gt $MAX_LENGTH ]; then
        echo "${TEXT:0:$((MAX_LENGTH-3))}..."
    else
        echo "$TEXT"
    fi
}