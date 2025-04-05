#!/bin/bash

#
# Service startup script for Rinna
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
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Source environment if exists
if [ -f "$PROJECT_ROOT/.env" ]; then
  source "$PROJECT_ROOT/.env"
fi

# Source activate-java.sh if exists
if [ -f "$PROJECT_ROOT/activate-java.sh" ]; then
  source "$PROJECT_ROOT/activate-java.sh"
fi

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

# Start Java backend
start_java_backend() {
  print_info "Starting Java backend service..."
  
  # Check if already running
  if pgrep -f "org.rinna.Rinna" > /dev/null; then
    print_info "Java backend is already running. Skipping startup."
    return 0
  fi
  
  # Start the Java server in the background
  "$SCRIPT_DIR/start-java-server.sh" &
  JAVA_PID=$!
  
  # Store PID for later use
  echo $JAVA_PID > "$PROJECT_ROOT/.java.pid"
  
  print_info "Java backend starting with PID $JAVA_PID"
  
  # Wait briefly to ensure it's starting up
  sleep 2
  
  # Check if process is still running
  if ! kill -0 $JAVA_PID 2>/dev/null; then
    print_error "Java backend failed to start"
    return 1
  fi
  
  print_success "Java backend started successfully"
  return 0
}

# Start Go API server
start_go_api() {
  print_info "Starting Go API server..."
  
  # Check if already running
  if pgrep -f "rinnasrv" > /dev/null; then
    print_info "Go API server is already running. Skipping startup."
    return 0
  fi
  
  # Navigate to API directory
  cd "$PROJECT_ROOT/api"
  
  # Build if needed
  if [ ! -f "$PROJECT_ROOT/api/rinnasrv" ] || [ "$1" == "--rebuild" ]; then
    print_info "Building Go API server..."
    cd "$PROJECT_ROOT/api" && go build -o rinnasrv ./cmd/rinnasrv
  fi
  
  # Start the Go server in the background
  "$PROJECT_ROOT/api/rinnasrv" &
  GO_PID=$!
  
  # Store PID for later use
  echo $GO_PID > "$PROJECT_ROOT/.go.pid"
  
  print_info "Go API server starting with PID $GO_PID"
  
  # Wait briefly to ensure it's starting up
  sleep 1
  
  # Check if process is still running
  if ! kill -0 $GO_PID 2>/dev/null; then
    print_error "Go API server failed to start"
    return 1
  fi
  
  print_success "Go API server started successfully"
  return 0
}

# Ensure directories exist
ensure_directories() {
  # Create logs directory if it doesn't exist
  mkdir -p "$PROJECT_ROOT/logs"
}

# Check health of services
check_health() {
  local max_attempts=30
  local attempt=1
  local delay=1
  
  print_info "Checking service health..."
  
  # Check Java backend health
  while [ $attempt -le $max_attempts ]; do
    if curl -s "http://localhost:8081/health" | grep -q "status.*ok"; then
      print_success "Java backend is healthy"
      break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
      print_error "Java backend health check failed after $max_attempts attempts"
      return 1
    fi
    
    print_info "Waiting for Java backend to be ready (attempt $attempt/$max_attempts)..."
    sleep $delay
    attempt=$((attempt + 1))
  done
  
  # Reset attempt counter
  attempt=1
  
  # Check Go API health
  while [ $attempt -le $max_attempts ]; do
    if curl -s "http://localhost:9080/health" | grep -q "status.*ok"; then
      print_success "Go API server is healthy"
      break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
      print_error "Go API server health check failed after $max_attempts attempts"
      return 1
    fi
    
    print_info "Waiting for Go API server to be ready (attempt $attempt/$max_attempts)..."
    sleep $delay
    attempt=$((attempt + 1))
  done
  
  print_success "All services are healthy and ready to use"
  return 0
}

# Main function
main() {
  print_info "Starting Rinna services..."
  
  # Ensure required directories exist
  ensure_directories
  
  # Start the Java backend first
  if ! start_java_backend; then
    print_error "Failed to start Java backend"
    exit 1
  fi
  
  # Then start the Go API server
  if ! start_go_api; then
    print_error "Failed to start Go API server"
    exit 1
  fi
  
  # Check health
  if ! check_health; then
    print_error "Service health check failed"
    exit 1
  fi
  
  print_success "Rinna services started successfully"
  
  # Keep running if requested
  if [ "$1" == "--foreground" ]; then
    print_info "Running in foreground mode. Press Ctrl+C to stop services."
    # Wait for keyboard interrupt
    trap "exit" INT TERM
    trap "kill 0" EXIT
    wait
  fi
  
  exit 0
}

# Run main function
main "$@"