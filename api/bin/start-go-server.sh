#!/bin/bash

#
# Start script for the Go API server
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

# Exit on any error
set -e

# Get script directory and project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
API_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
PROJECT_ROOT="$( cd "$API_DIR/.." && pwd )"

# Print colored output
print_info() {
  echo -e "\033[1;34m[INFO]\033[0m $1"
}

print_success() {
  echo -e "\033[1;32m[SUCCESS]\033[0m $1"
}

print_error() {
  echo -e "\033[1;31m[ERROR]\033[0m $1" >&2
}

# Parse command-line arguments
PORT=9080
CONFIG_PATH="$API_DIR/configs/config.yaml"

while [[ $# -gt 0 ]]; do
  case $1 in
    --port=*)
      PORT="${1#*=}"
      shift
      ;;
    --config=*)
      CONFIG_PATH="${1#*=}"
      shift
      ;;
    *)
      print_error "Unknown parameter: $1"
      exit 1
      ;;
  esac
done

# Build if needed
if [ ! -f "$API_DIR/rinnasrv" ]; then
  print_info "Building Go API server..."
  cd "$API_DIR" && go build -o rinnasrv ./cmd/rinnasrv
  if [ $? -ne 0 ]; then
    print_error "Failed to build Go API server"
    exit 1
  fi
  print_success "Go API server built successfully"
fi

# Start the server
print_info "Starting Go API server on port $PORT..."
cd "$API_DIR" && ./rinnasrv --port=$PORT --config="$CONFIG_PATH"