#!/usr/bin/env bash
#
# build-logger.sh - Compile the Go logger bridge for the multi-language logging system
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Source common logging functions if available
if [[ -f "${PROJECT_ROOT}/bin/common/rinna_logger.sh" ]]; then
    source "${PROJECT_ROOT}/bin/common/rinna_logger.sh"
    set_module_name "build-logger"
    log_info "Building Go logger bridge"
else
    echo "Building Go logger bridge"
fi

# Explicitly set API directory
API_DIR="${PROJECT_ROOT}/api"
log_info "API directory: ${API_DIR}"

# Check if Go is available
if ! command -v go &> /dev/null; then
    log_error "Go is not installed or not in PATH"
    exit 1
fi

# Create output directory
OUTPUT_DIR="${PROJECT_ROOT}/bin"
mkdir -p "${OUTPUT_DIR}"

# Build the Go logger bridge
GO_SRC="${SCRIPT_DIR}/cmd/rinna-logger"
OUTPUT="${OUTPUT_DIR}/rinna-logger"

cd "${SCRIPT_DIR}"

# Make sure we build inside the Go module directory
log_info "Building Go logger bridge from module directory"
cd "${API_DIR}"  # This is the api directory where go.mod is located
log_info "Current directory: $(pwd)"
log_info "Go module path: $(cat go.mod | grep "module" || echo "No module found")"
go build -o "${OUTPUT}" "./cmd/rinna-logger"

if [[ -f "${OUTPUT}" ]]; then
    log_info "Successfully built Go logger bridge at ${OUTPUT}"
    chmod +x "${OUTPUT}"
else
    log_error "Failed to build Go logger bridge"
    exit 1
fi