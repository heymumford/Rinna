#!/bin/bash
#
# java_python_integration_test.sh - Cross-language test for Java-Python integration
#
# This test verifies that Java components can interact with Python components
# for data processing, analysis, and integration between the two languages.
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
TEST_TEMP_DIR="${RINNA_TEST_TEMP_DIR:-./target/cross-language-tests}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CLI_PATH="$PROJECT_ROOT/bin/rin"
PYTHON_SCRIPT="${TEST_TEMP_DIR}/java_python_processor.py"
TEST_FILE="${TEST_TEMP_DIR}/test_workitems.json"
TEST_RESULT_FILE="${TEST_TEMP_DIR}/processed_workitems.json"
TEST_METRICS_FILE="${TEST_TEMP_DIR}/workitem_metrics.json"

# Test setup
setup() {
  echo "Setting up Java-Python integration test..."
  
  # Create temp directory if needed
  mkdir -p "$TEST_TEMP_DIR"
  
  # Ensure CLI exists
  if [[ ! -x "$CLI_PATH" ]]; then
    echo "Error: CLI not found at $CLI_PATH"
    exit 1
  fi
  
  # Check Python is available
  if ! command -v python >/dev/null 2>&1; then
    echo "Error: Python not found in PATH"
    exit 1
  fi
  
  # Create Python script for data processing
  create_python_processor
  
  echo "Setup complete"
}

# Create Python processor script
create_python_processor() {
  cat > "$PYTHON_SCRIPT" <<EOF
#!/usr/bin/env python3
"""
Java-Python integration test processor.

This script processes work item data exported from Java components
and performs analysis and transformations on the data.
"""

import json
import os
import sys
import datetime
import statistics
from collections import Counter

def load_work_items(filename):
    """Load work items from JSON file exported by Java."""
    try:
        with open(filename, 'r') as f:
            return json.load(f)
    except Exception as e:
        print(f"Error loading work items: {e}")
        return []

def process_work_items(items):
    """Process work items with Python-specific logic."""
    print(f"Processing {len(items)} work items...")
    
    processed_items = []
    for item in items:
        # Add Python-specific processing metadata
        processed_item = item.copy()
        processed_item['metadata'] = processed_item.get('metadata', {})
        processed_item['metadata'].update({
            'processed_by': 'python-processor',
            'processed_at': datetime.datetime.now().isoformat(),
            'processing_version': '1.0'
        })
        
        # Calculate derived fields
        if 'createdAt' in processed_item and 'updatedAt' in processed_item:
            try:
                created = datetime.datetime.fromisoformat(processed_item['createdAt'].replace('Z', '+00:00'))
                updated = datetime.datetime.fromisoformat(processed_item['updatedAt'].replace('Z', '+00:00'))
                age_days = (updated - created).total_seconds() / 86400
                processed_item['metadata']['age_days'] = round(age_days, 2)
            except Exception as e:
                print(f"Error calculating age for item {item.get('id')}: {e}")
        
        # Enhance description with Python processor info
        if 'description' in processed_item:
            processed_item['description'] += f"\n\nProcessed by Python at {datetime.datetime.now().isoformat()}"
        
        processed_items.append(processed_item)
    
    return processed_items

def analyze_work_items(items):
    """Generate metrics and analysis of work items."""
    if not items:
        return {
            'error': 'No items to analyze',
            'timestamp': datetime.datetime.now().isoformat()
        }
    
    # Count by type
    types = Counter([item.get('type', 'UNKNOWN') for item in items])
    
    # Count by priority
    priorities = Counter([item.get('priority', 'UNKNOWN') for item in items])
    
    # Count by state
    states = Counter([item.get('state', 'UNKNOWN') for item in items])
    
    # Calculate age statistics if available
    age_stats = {}
    ages = [item.get('metadata', {}).get('age_days') for item in items 
            if 'metadata' in item and 'age_days' in item['metadata']]
    
    if ages:
        age_stats = {
            'min': min(ages),
            'max': max(ages),
            'mean': statistics.mean(ages),
            'median': statistics.median(ages)
        }
    
    # Generate report
    return {
        'timestamp': datetime.datetime.now().isoformat(),
        'total_items': len(items),
        'by_type': dict(types),
        'by_priority': dict(priorities),
        'by_state': dict(states),
        'age_statistics': age_stats,
        'analysis_version': '1.0'
    }

def main():
    """Main function to process arguments and run appropriate action."""
    if len(sys.argv) < 3:
        print("Usage: python java_python_processor.py [process|analyze] <input_file> [output_file]")
        sys.exit(1)
    
    action = sys.argv[1]
    input_file = sys.argv[2]
    
    if not os.path.exists(input_file):
        print(f"Error: Input file {input_file} not found")
        sys.exit(1)
    
    items = load_work_items(input_file)
    
    if not items:
        print("Error: No items found in input file")
        sys.exit(1)
    
    if action == 'process':
        processed_items = process_work_items(items)
        
        if len(sys.argv) > 3:
            output_file = sys.argv[3]
        else:
            # Default output file
            output_file = f"{os.path.splitext(input_file)[0]}_processed.json"
        
        with open(output_file, 'w') as f:
            json.dump(processed_items, f, indent=2)
        
        print(f"Processed {len(processed_items)} items and saved to {output_file}")
    
    elif action == 'analyze':
        metrics = analyze_work_items(items)
        
        if len(sys.argv) > 3:
            output_file = sys.argv[3]
        else:
            # Default output file
            output_file = f"{os.path.dirname(input_file)}/workitem_metrics.json"
        
        with open(output_file, 'w') as f:
            json.dump(metrics, f, indent=2)
        
        print(f"Analysis complete. Generated metrics for {metrics['total_items']} items.")
        print(f"Metrics saved to {output_file}")
    
    else:
        print(f"Unknown action: {action}")
        print("Available actions: process, analyze")
        sys.exit(1)

if __name__ == "__main__":
    main()
EOF

  chmod +x "$PYTHON_SCRIPT"
}

# Test teardown
teardown() {
  echo "Tearing down Java-Python integration test..."
  
  # Clean up temp files
  rm -f "$TEST_FILE" "$TEST_RESULT_FILE" "$TEST_METRICS_FILE" 2>/dev/null || true
  
  echo "Teardown complete"
}

# Create test work items via Java CLI
test_java_export_workitems() {
  echo "Exporting work items from Java CLI..."
  
  # Create and export work items
  # First create a few test items if needed
  for i in {1..3}; do
    "$CLI_PATH" add --type=TASK --title="Java-Python Test Item $i" --priority=HIGH --description="Test item for Java-Python integration" > /dev/null || true
  done
  
  # Export all items to JSON
  "$CLI_PATH" list --output=json > "$TEST_FILE"
  
  # Check if export was successful
  if [[ ! -s "$TEST_FILE" ]]; then
    echo "Error: Failed to export work items from Java CLI"
    return 1
  fi
  
  # Count items
  item_count=$(grep -o "\"id\":" "$TEST_FILE" | wc -l)
  echo "Exported $item_count work items from Java CLI"
  
  if [[ $item_count -eq 0 ]]; then
    echo "Error: No work items were exported"
    return 1
  fi
  
  return 0
}

# Process items with Python script
test_python_process_workitems() {
  echo "Processing work items with Python..."
  
  # Run Python processor on exported items
  python "$PYTHON_SCRIPT" process "$TEST_FILE" "$TEST_RESULT_FILE"
  
  # Check if processing was successful
  if [[ ! -s "$TEST_RESULT_FILE" ]]; then
    echo "Error: Failed to process work items with Python"
    return 1
  fi
  
  # Verify processing added Python-specific metadata
  if ! grep -q "\"processed_by\":\"python-processor\"" "$TEST_RESULT_FILE"; then
    echo "Error: Python processing metadata not found in output"
    return 1
  fi
  
  echo "Work items processed successfully with Python"
  return 0
}

# Analyze items with Python script
test_python_analyze_workitems() {
  echo "Analyzing work items with Python..."
  
  # Run Python analyzer on processed items
  python "$PYTHON_SCRIPT" analyze "$TEST_RESULT_FILE" "$TEST_METRICS_FILE"
  
  # Check if analysis was successful
  if [[ ! -s "$TEST_METRICS_FILE" ]]; then
    echo "Error: Failed to analyze work items with Python"
    return 1
  fi
  
  # Verify analysis contains expected sections
  if ! grep -q "\"by_type\":" "$TEST_METRICS_FILE" || \
     ! grep -q "\"by_priority\":" "$TEST_METRICS_FILE" || \
     ! grep -q "\"by_state\":" "$TEST_METRICS_FILE"; then
    echo "Error: Missing expected analysis sections in output"
    return 1
  fi
  
  echo "Work items analyzed successfully with Python"
  return 0
}

# Verify results can be imported back to Java
test_java_import_processed_items() {
  echo "Verifying Java can parse Python-processed items..."
  
  # Use JQ to extract a sample ID to test (if available)
  if command -v jq >/dev/null 2>&1; then
    sample_id=$(jq -r '.[0].id // ""' "$TEST_RESULT_FILE")
  else
    # Fallback if jq not available
    sample_id=$(grep -o "\"id\":\"[^\"]*\"" "$TEST_RESULT_FILE" | head -1 | cut -d'"' -f4)
  fi
  
  if [[ -z "$sample_id" ]]; then
    echo "Error: Could not extract a sample ID for verification"
    return 1
  fi
  
  # Verify Java CLI can read the item (should not error)
  "$CLI_PATH" view "$sample_id" --output=json > /dev/null
  
  if [[ $? -ne 0 ]]; then
    echo "Error: Java CLI failed to read the processed item: $sample_id"
    return 1
  fi
  
  echo "Java CLI successfully parsed Python-processed items"
  return 0
}

# Run the complete test
run_test() {
  local success=true
  
  setup
  
  # Run test steps, aborting on first failure
  if ! test_java_export_workitems; then
    echo "Test failed at step: test_java_export_workitems"
    success=false
  elif ! test_python_process_workitems; then
    echo "Test failed at step: test_python_process_workitems"
    success=false
  elif ! test_python_analyze_workitems; then
    echo "Test failed at step: test_python_analyze_workitems"
    success=false
  elif ! test_java_import_processed_items; then
    echo "Test failed at step: test_java_import_processed_items"
    success=false
  fi
  
  teardown
  
  if $success; then
    echo "Java-Python integration test completed successfully"
    return 0
  else
    echo "Java-Python integration test failed"
    return 1
  fi
}

# Run the test if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run_test
fi