#!/bin/bash
#
# workitem_sync_test.sh - Cross-language test for work item synchronization
#
# This test verifies that work item data can be properly synchronized between
# Java, Go, and Python components in the Rinna system.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

set -eo pipefail

# Source test utilities if available
if [[ -f "./test/common/test_utils.sh" ]]; then
  source "./test/common/test_utils.sh"
fi

# Test variables
API_PORT="${RINNA_TEST_API_PORT:-8085}"
TEST_TEMP_DIR="${RINNA_TEST_TEMP_DIR:-./target/cross-language-tests}"
CLI_PATH="./bin/rin"
TEST_ITEM_ID=""

# Test setup
setup() {
  echo "Setting up work item sync test..."
  
  # Create temp directory if needed
  mkdir -p "$TEST_TEMP_DIR"
  
  # Ensure CLI exists
  if [[ ! -x "$CLI_PATH" ]]; then
    echo "Error: CLI not found at $CLI_PATH"
    exit 1
  fi
  
  # Check if API is available
  if ! curl -s "http://localhost:$API_PORT/api/health" > /dev/null; then
    echo "Error: API server not running on port $API_PORT"
    exit 1
  fi
  
  echo "Setup complete"
}

# Test teardown
teardown() {
  echo "Tearing down work item sync test..."
  
  # Clean up test work item if created
  if [[ -n "$TEST_ITEM_ID" ]]; then
    echo "Cleaning up test work item: $TEST_ITEM_ID"
    "$CLI_PATH" delete "$TEST_ITEM_ID" > /dev/null 2>&1 || true
  fi
  
  echo "Teardown complete"
}

# Create work item via Java CLI
test_java_create_workitem() {
  echo "Creating work item via Java CLI..."
  
  # Run CLI command to create work item
  local result
  result=$("$CLI_PATH" add --type=TASK --title="Cross-language test item" --priority=HIGH --output=json)
  
  # Extract work item ID from JSON output
  TEST_ITEM_ID=$(echo "$result" | grep -o '"id":\s*"[^"]*"' | cut -d'"' -f4)
  
  if [[ -z "$TEST_ITEM_ID" ]]; then
    echo "Error: Failed to create work item"
    echo "CLI output: $result"
    return 1
  fi
  
  echo "Created work item with ID: $TEST_ITEM_ID"
  return 0
}

# Verify work item via Go API
test_go_verify_workitem() {
  echo "Verifying work item via Go API..."
  
  if [[ -z "$TEST_ITEM_ID" ]]; then
    echo "Error: No work item ID available"
    return 1
  fi
  
  # Query API directly to verify the item exists
  local response
  response=$(curl -s "http://localhost:$API_PORT/api/workitems/$TEST_ITEM_ID")
  
  # Check if item exists and has correct properties
  if echo "$response" | grep -q "\"title\":\"Cross-language test item\""; then
    echo "Work item verified in Go API"
    return 0
  else
    echo "Error: Work item not found or has incorrect properties"
    echo "API response: $response"
    return 1
  fi
}

# Update work item via Python
test_python_update_workitem() {
  echo "Updating work item via Python..."
  
  if [[ -z "$TEST_ITEM_ID" ]]; then
    echo "Error: No work item ID available"
    return 1
  fi
  
  # Create Python script for updating the work item
  local python_script="$TEST_TEMP_DIR/update_workitem.py"
  
  cat > "$python_script" <<EOF
import json
import requests
import sys

# Work item ID from test
item_id = "$TEST_ITEM_ID"

# API endpoint
api_url = "http://localhost:$API_PORT/api/workitems/" + item_id

# Update data
update_data = {
    "description": "Updated by Python test",
    "state": "IN_PROGRESS"
}

try:
    # Send PUT request to update the item
    response = requests.put(api_url, json=update_data)
    
    # Check response
    if response.status_code == 200:
        print(json.dumps(response.json(), indent=2))
        sys.exit(0)
    else:
        print(f"Error: API returned status code {response.status_code}")
        print(response.text)
        sys.exit(1)
except Exception as e:
    print(f"Error: {str(e)}")
    sys.exit(1)
EOF
  
  # Run the Python script
  python "$python_script"
  
  return $?
}

# Verify updated work item via Java CLI
test_java_verify_updated_workitem() {
  echo "Verifying updated work item via Java CLI..."
  
  if [[ -z "$TEST_ITEM_ID" ]]; then
    echo "Error: No work item ID available"
    return 1
  fi
  
  # Get work item via CLI
  local result
  result=$("$CLI_PATH" view "$TEST_ITEM_ID" --output=json)
  
  # Verify work item has the updated properties
  if echo "$result" | grep -q "\"description\":\"Updated by Python test\"" && \
     echo "$result" | grep -q "\"state\":\"IN_PROGRESS\""; then
    echo "Updated work item verified in Java CLI"
    return 0
  else
    echo "Error: Work item not updated correctly"
    echo "CLI output: $result"
    return 1
  fi
}

# Run the complete test
run_test() {
  local success=true
  
  setup
  
  # Run test steps, aborting on first failure
  if ! test_java_create_workitem; then
    echo "Test failed at step: test_java_create_workitem"
    success=false
  elif ! test_go_verify_workitem; then
    echo "Test failed at step: test_go_verify_workitem"
    success=false
  elif ! test_python_update_workitem; then
    echo "Test failed at step: test_python_update_workitem"
    success=false
  elif ! test_java_verify_updated_workitem; then
    echo "Test failed at step: test_java_verify_updated_workitem"
    success=false
  fi
  
  teardown
  
  if $success; then
    echo "Work item sync test completed successfully"
    return 0
  else
    echo "Work item sync test failed"
    return 1
  fi
}

# Run the test if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run_test
fi