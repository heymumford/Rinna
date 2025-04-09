#!/bin/bash
#
# go_python_integration_test.sh - Cross-language test for Go-Python integration
#
# This test verifies that Go API endpoints can be properly integrated with
# Python scripting components, specifically focusing on data transformation
# and processing flows between languages.
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
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TEST_ITEM_IDS=()
PYTHON_SCRIPT="${TEST_TEMP_DIR}/test_data_processor.py"

# Test setup
setup() {
  echo "Setting up Go-Python integration test..."
  
  # Create temp directory if needed
  mkdir -p "$TEST_TEMP_DIR"
  
  # Check if API is available
  if ! curl -s "http://localhost:$API_PORT/api/health" > /dev/null; then
    echo "Error: API server not running on port $API_PORT"
    exit 1
  fi
  
  # Check Python is available
  if ! command -v python >/dev/null 2>&1; then
    echo "Error: Python not found in PATH"
    exit 1
  fi
  
  # Create Python data processing script
  create_python_data_processor
  
  echo "Setup complete"
}

# Create Python data processing script used for the test
create_python_data_processor() {
  cat > "$PYTHON_SCRIPT" <<EOF
#!/usr/bin/env python3
"""
Test data processor for Go-Python integration tests.
This script processes work item data from the Go API and transforms it
for testing cross-language integration.
"""

import json
import os
import sys
import requests
from datetime import datetime

# Get API port from environment
API_PORT = os.environ.get("RINNA_TEST_API_PORT", "8085")
API_URL = f"http://localhost:{API_PORT}/api"

def create_test_items(count=3):
    """Create multiple test work items via the API."""
    print(f"Creating {count} test work items...")
    item_ids = []
    
    for i in range(count):
        work_item = {
            "title": f"Go-Python integration test item {i+1}",
            "type": "TASK",
            "priority": "HIGH" if i % 2 == 0 else "MEDIUM",
            "description": f"Test item {i+1} created for Go-Python integration testing"
        }
        
        try:
            response = requests.post(
                f"{API_URL}/workitems",
                json=work_item,
                headers={"Content-Type": "application/json"}
            )
            
            if response.status_code == 201:
                item_data = response.json()
                item_ids.append(item_data["id"])
                print(f"Created item with ID: {item_data['id']}")
            else:
                print(f"Failed to create item {i+1}: {response.status_code} - {response.text}")
        except Exception as e:
            print(f"Error creating item {i+1}: {str(e)}")
    
    # Write IDs to file for the test harness
    with open("${TEST_TEMP_DIR}/test_item_ids.txt", "w") as f:
        f.write("\n".join(item_ids))
    
    return item_ids

def process_and_transform_items(item_ids):
    """Process and transform work items for testing."""
    print(f"Processing {len(item_ids)} work items...")
    processed_data = []
    
    for item_id in item_ids:
        try:
            response = requests.get(f"{API_URL}/workitems/{item_id}")
            
            if response.status_code == 200:
                item = response.json()
                
                # Transform the item
                transformed_item = {
                    "id": item["id"],
                    "title": item["title"],
                    "type": item["type"],
                    "priority": item["priority"],
                    "state": "IN_PROGRESS",  # Change state
                    "description": f"Transformed by Python at {datetime.now().isoformat()}",
                    "metadata": {
                        "processed_by": "python-test-script",
                        "original_state": item.get("state", "NEW"),
                        "processing_timestamp": datetime.now().isoformat()
                    }
                }
                
                # Update the item with transformed data
                update_response = requests.put(
                    f"{API_URL}/workitems/{item_id}",
                    json=transformed_item,
                    headers={"Content-Type": "application/json"}
                )
                
                if update_response.status_code == 200:
                    print(f"Transformed item {item_id}")
                    processed_data.append(transformed_item)
                else:
                    print(f"Failed to update item {item_id}: {update_response.status_code}")
            else:
                print(f"Failed to get item {item_id}: {response.status_code}")
        except Exception as e:
            print(f"Error processing item {item_id}: {str(e)}")
    
    # Write processed data to file
    with open("${TEST_TEMP_DIR}/processed_items.json", "w") as f:
        json.dump(processed_data, f, indent=2)
    
    return processed_data

def analyze_item_statistics():
    """Analyze work items and generate statistics."""
    print("Analyzing work item statistics...")
    
    try:
        # Get all work items
        response = requests.get(f"{API_URL}/workitems")
        
        if response.status_code != 200:
            print(f"Failed to get work items: {response.status_code}")
            return None
        
        items = response.json()
        
        # Count by type
        type_counts = {}
        priority_counts = {}
        state_counts = {}
        
        for item in items:
            # Count by type
            item_type = item.get("type", "UNKNOWN")
            type_counts[item_type] = type_counts.get(item_type, 0) + 1
            
            # Count by priority
            priority = item.get("priority", "UNKNOWN")
            priority_counts[priority] = priority_counts.get(priority, 0) + 1
            
            # Count by state
            state = item.get("state", "UNKNOWN")
            state_counts[state] = state_counts.get(state, 0) + 1
        
        # Generate statistics
        stats = {
            "total_items": len(items),
            "by_type": type_counts,
            "by_priority": priority_counts,
            "by_state": state_counts,
            "analysis_timestamp": datetime.now().isoformat()
        }
        
        # Write statistics to file
        with open("${TEST_TEMP_DIR}/item_statistics.json", "w") as f:
            json.dump(stats, f, indent=2)
        
        print(f"Analysis complete. Found {len(items)} total items.")
        return stats
    
    except Exception as e:
        print(f"Error analyzing items: {str(e)}")
        return None

if __name__ == "__main__":
    # Check for command argument
    if len(sys.argv) < 2:
        print("Error: Missing command. Use 'create', 'process', or 'analyze'")
        sys.exit(1)
    
    command = sys.argv[1]
    
    if command == "create":
        count = 3
        if len(sys.argv) > 2 and sys.argv[2].isdigit():
            count = int(sys.argv[2])
        item_ids = create_test_items(count)
        print(json.dumps(item_ids))
    
    elif command == "process":
        # Process items from file or command line
        if len(sys.argv) > 2:
            # IDs provided as arguments
            item_ids = sys.argv[2:]
        elif os.path.exists("${TEST_TEMP_DIR}/test_item_ids.txt"):
            # Read IDs from file
            with open("${TEST_TEMP_DIR}/test_item_ids.txt", "r") as f:
                item_ids = [line.strip() for line in f if line.strip()]
        else:
            print("Error: No item IDs provided or found")
            sys.exit(1)
        
        processed = process_and_transform_items(item_ids)
        print(f"Processed {len(processed)} items")
    
    elif command == "analyze":
        stats = analyze_item_statistics()
        if stats:
            print(f"Analysis complete. Found {stats['total_items']} total items.")
        else:
            print("Analysis failed")
            sys.exit(1)
    
    else:
        print(f"Error: Unknown command '{command}'")
        print("Available commands: create, process, analyze")
        sys.exit(1)
EOF

  chmod +x "$PYTHON_SCRIPT"
}

# Test teardown
teardown() {
  echo "Tearing down Go-Python integration test..."
  
  # Clean up test work items
  if [[ ${#TEST_ITEM_IDS[@]} -gt 0 ]]; then
    echo "Cleaning up test work items: ${TEST_ITEM_IDS[*]}"
    for id in "${TEST_ITEM_IDS[@]}"; do
      curl -s -X DELETE "http://localhost:$API_PORT/api/workitems/$id" > /dev/null || true
    done
  fi
  
  echo "Teardown complete"
}

# Create test work items via Python script
test_python_create_items() {
  echo "Creating test work items via Python..."
  
  # Run Python script to create items
  result=$(python "$PYTHON_SCRIPT" create 3)
  
  # Extract item IDs from result
  if [[ -f "${TEST_TEMP_DIR}/test_item_ids.txt" ]]; then
    mapfile -t TEST_ITEM_IDS < "${TEST_TEMP_DIR}/test_item_ids.txt"
  fi
  
  # Check if items were created
  if [[ ${#TEST_ITEM_IDS[@]} -eq 0 ]]; then
    echo "Error: Failed to create test items"
    echo "Python script output: $result"
    return 1
  fi
  
  echo "Created ${#TEST_ITEM_IDS[@]} test items: ${TEST_ITEM_IDS[*]}"
  return 0
}

# Verify item creation in Go API
test_go_verify_created_items() {
  echo "Verifying created items via Go API..."
  
  if [[ ${#TEST_ITEM_IDS[@]} -eq 0 ]]; then
    echo "Error: No test item IDs available"
    return 1
  fi
  
  # Check each item exists in API
  local success=true
  for id in "${TEST_ITEM_IDS[@]}"; do
    echo "Checking item $id..."
    local response
    response=$(curl -s "http://localhost:$API_PORT/api/workitems/$id")
    
    # Check if item exists and has correct properties
    if echo "$response" | grep -q "\"title\":\"Go-Python integration test item"; then
      echo "Item $id verified in Go API"
    else
      echo "Error: Item $id not found or has incorrect properties"
      echo "API response: $response"
      success=false
    fi
  done
  
  if [[ "$success" == "true" ]]; then
    return 0
  else
    return 1
  fi
}

# Process items via Python script
test_python_process_items() {
  echo "Processing items via Python script..."
  
  if [[ ${#TEST_ITEM_IDS[@]} -eq 0 ]]; then
    echo "Error: No test item IDs available"
    return 1
  fi
  
  # Run Python script to process items
  result=$(python "$PYTHON_SCRIPT" process "${TEST_ITEM_IDS[@]}")
  
  # Check if items were processed
  if [[ ! -f "${TEST_TEMP_DIR}/processed_items.json" ]]; then
    echo "Error: Failed to process test items"
    echo "Python script output: $result"
    return 1
  fi
  
  echo "Items processed successfully"
  return 0
}

# Verify processed items in Go API
test_go_verify_processed_items() {
  echo "Verifying processed items via Go API..."
  
  if [[ ${#TEST_ITEM_IDS[@]} -eq 0 ]]; then
    echo "Error: No test item IDs available"
    return 1
  fi
  
  # Check each item has been updated
  local success=true
  for id in "${TEST_ITEM_IDS[@]}"; do
    echo "Checking processed item $id..."
    local response
    response=$(curl -s "http://localhost:$API_PORT/api/workitems/$id")
    
    # Check if item has been processed (state should be IN_PROGRESS)
    if echo "$response" | grep -q "\"state\":\"IN_PROGRESS\"" && \
       echo "$response" | grep -q "Transformed by Python"; then
      echo "Item $id verified as processed"
    else
      echo "Error: Item $id not processed correctly"
      echo "API response: $response"
      success=false
    fi
  done
  
  if [[ "$success" == "true" ]]; then
    return 0
  else
    return 1
  fi
}

# Analyze items via Python script
test_python_analyze_items() {
  echo "Analyzing items via Python script..."
  
  # Run Python script to analyze items
  result=$(python "$PYTHON_SCRIPT" analyze)
  
  # Check if analysis was generated
  if [[ ! -f "${TEST_TEMP_DIR}/item_statistics.json" ]]; then
    echo "Error: Failed to analyze items"
    echo "Python script output: $result"
    return 1
  fi
  
  # Validate analysis results
  local stats
  stats=$(cat "${TEST_TEMP_DIR}/item_statistics.json")
  
  # Check structure of statistics
  if echo "$stats" | grep -q "\"total_items\":" && \
     echo "$stats" | grep -q "\"by_type\":" && \
     echo "$stats" | grep -q "\"by_priority\":"; then
    echo "Analysis results validated"
    echo "Statistics summary: $(echo "$stats" | grep -o "\"total_items\":[^,]*")"
    return 0
  else
    echo "Error: Invalid analysis results"
    echo "Analysis output: $stats"
    return 1
  fi
}

# Run the complete test
run_test() {
  local success=true
  
  setup
  
  # Run test steps, aborting on first failure
  if ! test_python_create_items; then
    echo "Test failed at step: test_python_create_items"
    success=false
  elif ! test_go_verify_created_items; then
    echo "Test failed at step: test_go_verify_created_items"
    success=false
  elif ! test_python_process_items; then
    echo "Test failed at step: test_python_process_items"
    success=false
  elif ! test_go_verify_processed_items; then
    echo "Test failed at step: test_go_verify_processed_items"
    success=false
  elif ! test_python_analyze_items; then
    echo "Test failed at step: test_python_analyze_items"
    success=false
  fi
  
  teardown
  
  if $success; then
    echo "Go-Python integration test completed successfully"
    return 0
  else
    echo "Go-Python integration test failed"
    return 1
  fi
}

# Run the test if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run_test
fi