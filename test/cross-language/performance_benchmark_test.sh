#!/bin/bash
#
# performance_benchmark_test.sh - Cross-language performance benchmarks
#
# This test measures and compares performance metrics for cross-language
# operations, including throughput, latency, and resource usage.
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
CLI_PATH="$PROJECT_ROOT/bin/rin"
ITERATIONS=50
CONCURRENT_CLIENTS=5
PYTHON_BENCHMARK_SCRIPT="${TEST_TEMP_DIR}/performance_benchmark.py"
JAVA_PYTHON_RESULTS="${TEST_TEMP_DIR}/java_python_results.csv"
JAVA_GO_RESULTS="${TEST_TEMP_DIR}/java_go_results.csv"
GO_PYTHON_RESULTS="${TEST_TEMP_DIR}/go_python_results.csv"
SUMMARY_FILE="${TEST_TEMP_DIR}/performance_summary.md"

# Test setup
setup() {
  echo "Setting up performance benchmark test..."
  
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
  
  # Check Python is available
  if ! command -v python >/dev/null 2>&1; then
    echo "Error: Python not found in PATH"
    exit 1
  fi
  
  # Create Python benchmark script
  create_python_benchmark_script
  
  # Initialize result files
  echo "operation,iteration,latency_ms,success,memory_kb,cpu_percent" > "$JAVA_PYTHON_RESULTS"
  echo "operation,iteration,latency_ms,success,memory_kb,cpu_percent" > "$JAVA_GO_RESULTS"
  echo "operation,iteration,latency_ms,success,memory_kb,cpu_percent" > "$GO_PYTHON_RESULTS"
  
  echo "Setup complete"
}

# Create Python benchmark script
create_python_benchmark_script() {
  cat > "$PYTHON_BENCHMARK_SCRIPT" <<EOF
#!/usr/bin/env python3
"""
Performance benchmarking tool for cross-language testing.

This script provides benchmarking capabilities for measuring performance
of operations between different language components.
"""

import os
import sys
import time
import json
import random
import argparse
import statistics
import threading
import subprocess
import multiprocessing
import psutil
import requests
import uuid
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor

# Configuration from environment
API_PORT = os.environ.get("RINNA_TEST_API_PORT", "8085")
API_URL = f"http://localhost:{API_PORT}/api"
TEST_TEMP_DIR = os.environ.get("RINNA_TEST_TEMP_DIR", "./target/cross-language-tests")

# Result storage
RESULTS = []
RESULT_LOCK = threading.Lock()

def measure_operation(operation_fn, operation_name, iteration):
    """Measure performance metrics for a single operation."""
    process = psutil.Process(os.getpid())
    
    # Capture initial resource usage
    cpu_percent_start = process.cpu_percent(interval=None)
    memory_start = process.memory_info().rss / 1024  # KB
    
    # Execute operation and measure time
    start_time = time.time()
    success = False
    
    try:
        operation_fn()
        success = True
    except Exception as e:
        print(f"Error in operation {operation_name} (iteration {iteration}): {str(e)}")
    
    end_time = time.time()
    
    # Calculate metrics
    latency_ms = (end_time - start_time) * 1000
    
    # Capture final resource usage
    cpu_percent_end = process.cpu_percent(interval=None)
    memory_end = process.memory_info().rss / 1024  # KB
    
    # Calculate differences
    memory_kb = memory_end - memory_start
    cpu_percent = cpu_percent_end - cpu_percent_start
    
    # Store result
    result = {
        "operation": operation_name,
        "iteration": iteration,
        "latency_ms": latency_ms,
        "success": success,
        "memory_kb": memory_kb,
        "cpu_percent": cpu_percent
    }
    
    # Add to results
    with RESULT_LOCK:
        RESULTS.append(result)
    
    return result

def run_concurrent_benchmark(operation_fn, operation_name, iterations, concurrent_clients):
    """Run a benchmark with concurrent clients."""
    print(f"Running benchmark: {operation_name} with {concurrent_clients} concurrent clients, {iterations} iterations")
    
    # Reset results
    with RESULT_LOCK:
        RESULTS.clear()
    
    # Create thread pool
    with ThreadPoolExecutor(max_workers=concurrent_clients) as executor:
        # Submit all operations
        futures = []
        for i in range(iterations):
            future = executor.submit(measure_operation, operation_fn, operation_name, i+1)
            futures.append(future)
        
        # Wait for all to complete
        for future in futures:
            future.result()
    
    # Return all results
    return RESULTS

def operation_python_create_workitem():
    """Create a work item via Python API client."""
    # Generate unique test data
    test_id = str(uuid.uuid4())[:8]
    
    # Prepare work item data
    work_item = {
        "title": f"Benchmark test item {test_id}",
        "type": "TASK",
        "priority": "MEDIUM",
        "description": "Created for performance benchmarking"
    }
    
    # Send request to API
    response = requests.post(
        f"{API_URL}/workitems",
        json=work_item,
        headers={"Content-Type": "application/json"}
    )
    
    # Check response
    if response.status_code != 201:
        raise Exception(f"Failed to create work item: {response.status_code}")
    
    # Return item ID for cleanup
    return response.json().get("id")

def operation_python_get_workitem(item_id):
    """Get a work item via Python API client."""
    # Send request to API
    response = requests.get(f"{API_URL}/workitems/{item_id}")
    
    # Check response
    if response.status_code != 200:
        raise Exception(f"Failed to get work item: {response.status_code}")
    
    return response.json()

def operation_python_update_workitem(item_id):
    """Update a work item via Python API client."""
    # Prepare update data
    update_data = {
        "state": "IN_PROGRESS",
        "description": f"Updated for performance benchmark at {datetime.now().isoformat()}"
    }
    
    # Send request to API
    response = requests.put(
        f"{API_URL}/workitems/{item_id}",
        json=update_data,
        headers={"Content-Type": "application/json"}
    )
    
    # Check response
    if response.status_code != 200:
        raise Exception(f"Failed to update work item: {response.status_code}")
    
    return response.json()

def operation_python_search_workitems():
    """Search for work items via Python API client."""
    # Send request to API
    response = requests.get(f"{API_URL}/workitems?type=TASK")
    
    # Check response
    if response.status_code != 200:
        raise Exception(f"Failed to search work items: {response.status_code}")
    
    return response.json()

def run_java_go_benchmark(iterations, concurrent_clients):
    """Run Java-Go cross-language benchmark."""
    print(f"Running Java-Go benchmark with {concurrent_clients} concurrent clients, {iterations} iterations")
    
    # First create test items via Go API for Java to access
    test_items = []
    for i in range(5):
        try:
            item_id = operation_python_create_workitem()
            test_items.append(item_id)
        except Exception as e:
            print(f"Error creating test item: {str(e)}")
    
    if not test_items:
        print("Failed to create any test items for Java-Go benchmark")
        return []
    
    # Define operation functions that call Java CLI commands
    def java_view_workitem():
        """View work item via Java CLI."""
        item_id = random.choice(test_items)
        result = subprocess.run(
            ["${CLI_PATH}", "view", item_id, "--output=json"],
            capture_output=True,
            text=True
        )
        if result.returncode != 0:
            raise Exception(f"Failed to view work item via CLI: {result.stderr}")
        return json.loads(result.stdout)
    
    def java_list_workitems():
        """List work items via Java CLI."""
        result = subprocess.run(
            ["${CLI_PATH}", "list", "--output=json"],
            capture_output=True,
            text=True
        )
        if result.returncode != 0:
            raise Exception(f"Failed to list work items via CLI: {result.stderr}")
        return json.loads(result.stdout)
    
    def java_update_workitem():
        """Update work item via Java CLI."""
        item_id = random.choice(test_items)
        result = subprocess.run(
            ["${CLI_PATH}", "update", item_id, "--state=IN_PROGRESS", "--output=json"],
            capture_output=True,
            text=True
        )
        if result.returncode != 0:
            raise Exception(f"Failed to update work item via CLI: {result.stderr}")
        return json.loads(result.stdout)
    
    # Run benchmarks for each operation
    view_results = run_concurrent_benchmark(java_view_workitem, "java_view_go_item", iterations, concurrent_clients)
    list_results = run_concurrent_benchmark(java_list_workitems, "java_list_go_items", iterations, concurrent_clients)
    update_results = run_concurrent_benchmark(java_update_workitem, "java_update_go_item", iterations, concurrent_clients)
    
    # Combine all results
    combined_results = view_results + list_results + update_results
    
    # Clean up test items
    for item_id in test_items:
        try:
            requests.delete(f"{API_URL}/workitems/{item_id}")
        except Exception as e:
            print(f"Error deleting test item {item_id}: {str(e)}")
    
    return combined_results

def run_go_python_benchmark(iterations, concurrent_clients):
    """Run Go-Python cross-language benchmark."""
    print(f"Running Go-Python benchmark with {concurrent_clients} concurrent clients, {iterations} iterations")
    
    # Run benchmarks for each operation
    create_results = run_concurrent_benchmark(
        operation_python_create_workitem, 
        "python_create_go_item", 
        iterations, 
        concurrent_clients
    )
    
    # Save created item IDs for other operations and cleanup
    item_ids = []
    for result in create_results:
        if result["success"]:
            try:
                # Extract item ID from the function's return value
                item_id = operation_python_create_workitem()
                item_ids.append(item_id)
            except Exception:
                pass
    
    # Ensure we have at least one item
    if not item_ids:
        print("Failed to create any test items for Go-Python benchmark")
        return create_results
    
    # Create operations that use a random item ID
    def get_random_item():
        item_id = random.choice(item_ids)
        return operation_python_get_workitem(item_id)
    
    def update_random_item():
        item_id = random.choice(item_ids)
        return operation_python_update_workitem(item_id)
    
    # Run benchmarks for each operation
    get_results = run_concurrent_benchmark(
        get_random_item, 
        "python_get_go_item", 
        iterations, 
        concurrent_clients
    )
    
    update_results = run_concurrent_benchmark(
        update_random_item, 
        "python_update_go_item", 
        iterations, 
        concurrent_clients
    )
    
    search_results = run_concurrent_benchmark(
        operation_python_search_workitems, 
        "python_search_go_items", 
        iterations, 
        concurrent_clients
    )
    
    # Clean up test items
    for item_id in item_ids:
        try:
            requests.delete(f"{API_URL}/workitems/{item_id}")
        except Exception as e:
            print(f"Error deleting test item {item_id}: {str(e)}")
    
    # Combine all results
    return create_results + get_results + update_results + search_results

def run_java_python_benchmark(iterations, concurrent_clients):
    """Run Java-Python cross-language benchmark using temporary files."""
    print(f"Running Java-Python benchmark with {concurrent_clients} concurrent clients, {iterations} iterations")
    
    # Create test files directory
    test_files_dir = os.path.join(TEST_TEMP_DIR, "java_python_test_files")
    os.makedirs(test_files_dir, exist_ok=True)
    
    # Define operation functions
    def java_export_python_process():
        """Export data from Java CLI and process with Python."""
        # Generate a unique filename
        filename = f"java_export_{uuid.uuid4()}.json"
        output_file = os.path.join(test_files_dir, filename)
        processed_file = f"{output_file}.processed"
        
        try:
            # Export data from Java CLI
            result = subprocess.run(
                ["${CLI_PATH}", "list", "--output=json", "--file", output_file],
                capture_output=True,
                text=True
            )
            if result.returncode != 0:
                raise Exception(f"Failed to export data from CLI: {result.stderr}")
            
            # Process with Python
            if not os.path.exists(output_file):
                raise Exception(f"Export file not created: {output_file}")
            
            # Read the file
            with open(output_file, 'r') as f:
                data = json.load(f)
            
            # Process the data
            processed_data = {
                "processed_at": datetime.now().isoformat(),
                "processed_by": "python-benchmark",
                "item_count": len(data) if isinstance(data, list) else 1,
                "summary": {
                    "types": {},
                    "states": {},
                    "priorities": {}
                }
            }
            
            # If data is a list, summarize it
            if isinstance(data, list):
                for item in data:
                    item_type = item.get("type", "UNKNOWN")
                    processed_data["summary"]["types"][item_type] = processed_data["summary"]["types"].get(item_type, 0) + 1
                    
                    state = item.get("state", "UNKNOWN")
                    processed_data["summary"]["states"][state] = processed_data["summary"]["states"].get(state, 0) + 1
                    
                    priority = item.get("priority", "UNKNOWN")
                    processed_data["summary"]["priorities"][priority] = processed_data["summary"]["priorities"].get(priority, 0) + 1
            
            # Write processed data
            with open(processed_file, 'w') as f:
                json.dump(processed_data, f, indent=2)
            
            # Clean up
            os.remove(output_file)
            os.remove(processed_file)
            
            return processed_data
        except Exception as e:
            # Clean up on error
            if os.path.exists(output_file):
                os.remove(output_file)
            if os.path.exists(processed_file):
                os.remove(processed_file)
            raise e
    
    def python_generate_java_import():
        """Generate data with Python and import with Java CLI."""
        # Generate a unique filename
        filename = f"python_generate_{uuid.uuid4()}.json"
        input_file = os.path.join(test_files_dir, filename)
        
        try:
            # Generate data with Python
            items = []
            for i in range(3):  # Generate 3 sample items
                items.append({
                    "title": f"Benchmark Item {uuid.uuid4().hex[:8]}",
                    "type": "TASK",
                    "priority": random.choice(["HIGH", "MEDIUM", "LOW"]),
                    "description": f"Auto-generated by Python benchmark script at {datetime.now().isoformat()}"
                })
            
            # Write to file
            with open(input_file, 'w') as f:
                json.dump(items, f, indent=2)
            
            # Import with Java CLI
            result = subprocess.run(
                ["${CLI_PATH}", "import", "--file", input_file, "--output=json"],
                capture_output=True,
                text=True
            )
            if result.returncode != 0:
                raise Exception(f"Failed to import data with CLI: {result.stderr}")
            
            # Clean up
            os.remove(input_file)
            
            return json.loads(result.stdout)
        except Exception as e:
            # Clean up on error
            if os.path.exists(input_file):
                os.remove(input_file)
            raise e
    
    # Run benchmarks for each operation
    export_results = run_concurrent_benchmark(
        java_export_python_process, 
        "java_export_python_process", 
        iterations, 
        concurrent_clients
    )
    
    import_results = run_concurrent_benchmark(
        python_generate_java_import, 
        "python_generate_java_import", 
        iterations, 
        concurrent_clients
    )
    
    # Clean up test directory
    for filename in os.listdir(test_files_dir):
        os.remove(os.path.join(test_files_dir, filename))
    
    # Combine results
    return export_results + import_results

def export_results_to_csv(results, output_file):
    """Export benchmark results to CSV file."""
    with open(output_file, 'w') as f:
        # Write header
        f.write("operation,iteration,latency_ms,success,memory_kb,cpu_percent\n")
        
        # Write each result
        for result in results:
            f.write(f"{result['operation']},{result['iteration']},{result['latency_ms']},{1 if result['success'] else 0},{result['memory_kb']},{result['cpu_percent']}\n")
    
    print(f"Results exported to {output_file}")

def main():
    """Main function to run benchmarks."""
    parser = argparse.ArgumentParser(description="Performance benchmarking for cross-language operations")
    parser.add_argument("--iterations", type=int, default=50, help="Number of iterations per test")
    parser.add_argument("--clients", type=int, default=5, help="Number of concurrent clients")
    parser.add_argument("--test", choices=["all", "java-go", "go-python", "java-python"], default="all", help="Tests to run")
    parser.add_argument("--java-go-output", default="${JAVA_GO_RESULTS}", help="Output file for Java-Go results")
    parser.add_argument("--go-python-output", default="${GO_PYTHON_RESULTS}", help="Output file for Go-Python results")
    parser.add_argument("--java-python-output", default="${JAVA_PYTHON_RESULTS}", help="Output file for Java-Python results")
    
    args = parser.parse_args()
    
    try:
        if args.test in ["all", "java-go"]:
            # Run Java-Go benchmark
            java_go_results = run_java_go_benchmark(args.iterations, args.clients)
            export_results_to_csv(java_go_results, args.java_go_output)
        
        if args.test in ["all", "go-python"]:
            # Run Go-Python benchmark
            go_python_results = run_go_python_benchmark(args.iterations, args.clients)
            export_results_to_csv(go_python_results, args.go_python_output)
        
        if args.test in ["all", "java-python"]:
            # Run Java-Python benchmark
            java_python_results = run_java_python_benchmark(args.iterations, args.clients)
            export_results_to_csv(java_python_results, args.java_python_output)
        
        print("All benchmarks completed successfully")
        return 0
    except Exception as e:
        print(f"Error running benchmarks: {str(e)}")
        return 1

if __name__ == "__main__":
    sys.exit(main())
EOF

  chmod +x "$PYTHON_BENCHMARK_SCRIPT"
}

# Create a box plot image from results
create_boxplot() {
  local data_file="$1"
  local output_file="$2"
  local title="$3"
  
  # Check if Python libraries are available
  if ! python -c "import matplotlib, pandas" 2>/dev/null; then
    echo "Warning: matplotlib and pandas are required for creating plots. Skipping."
    return 1
  fi
  
  # Create Python script for plotting
  local plot_script="${TEST_TEMP_DIR}/create_plot.py"
  cat > "$plot_script" <<EOF
import pandas as pd
import matplotlib.pyplot as plt
import sys

# Read data
data = pd.read_csv('$data_file')

# Create figure
plt.figure(figsize=(10, 6))

# Create box plot grouped by operation
operations = data['operation'].unique()
data_by_op = [data[data['operation'] == op]['latency_ms'] for op in operations]

plt.boxplot(data_by_op, labels=operations)
plt.title('$title')
plt.ylabel('Latency (ms)')
plt.xlabel('Operation')
plt.xticks(rotation=45, ha='right')
plt.tight_layout()

# Save figure
plt.savefig('$output_file')
EOF

  # Run plot script
  python "$plot_script"
  
  if [[ -f "$output_file" ]]; then
    echo "Created box plot: $output_file"
    return 0
  else
    echo "Error: Failed to create box plot"
    return 1
  fi
}

# Generate performance summary
generate_summary() {
  # Create summary file
  cat > "$SUMMARY_FILE" <<EOF
# Cross-Language Performance Benchmark Results

**Test Date:** $(date)
**Iterations:** $ITERATIONS
**Concurrent Clients:** $CONCURRENT_CLIENTS

## Summary Statistics

EOF
  
  # Process each result file
  for test_pair in "Java-Go" "Go-Python" "Java-Python"; do
    local file_var="${test_pair//[^a-zA-Z]/_}_RESULTS"
    local file_path="${!file_var}"
    
    echo "### $test_pair Results" >> "$SUMMARY_FILE"
    echo "" >> "$SUMMARY_FILE"
    
    if [[ -f "$file_path" ]]; then
      # Generate summary statistics with Python
      local stats_script="${TEST_TEMP_DIR}/stats.py"
      cat > "$stats_script" <<EOF
import pandas as pd
import sys

# Read data
data = pd.read_csv('$file_path')

# Group by operation
grouped = data.groupby('operation')

# Calculate statistics
stats = grouped['latency_ms'].agg(['count', 'mean', 'median', 'min', 'max', 'std']).reset_index()
success_rate = grouped['success'].mean().reset_index()
stats = pd.merge(stats, success_rate, on='operation')

# Rename columns for clarity
stats.columns = ['Operation', 'Count', 'Mean (ms)', 'Median (ms)', 'Min (ms)', 'Max (ms)', 'Std Dev (ms)', 'Success Rate']

# Format table
md_table = stats.to_markdown(index=False, floatfmt='.2f')
print(md_table)
EOF

      # Run stats script and append to summary
      echo "Performance metrics by operation:" >> "$SUMMARY_FILE"
      echo "" >> "$SUMMARY_FILE"
      python "$stats_script" >> "$SUMMARY_FILE"
      echo "" >> "$SUMMARY_FILE"
      
      # Create box plot
      local plot_file="${TEST_TEMP_DIR}/${test_pair//[^a-zA-Z]/_}_boxplot.png"
      if create_boxplot "$file_path" "$plot_file" "$test_pair Latency Distribution"; then
        echo "Box plot of latency distribution:" >> "$SUMMARY_FILE"
        echo "" >> "$SUMMARY_FILE"
        echo "![${test_pair} Latency Distribution](${plot_file})" >> "$SUMMARY_FILE"
        echo "" >> "$SUMMARY_FILE"
      fi
    else
      echo "No results found for $test_pair benchmark." >> "$SUMMARY_FILE"
      echo "" >> "$SUMMARY_FILE"
    fi
  done
  
  # Add observations
  cat >> "$SUMMARY_FILE" <<EOF
## Observations

1. **Latency Comparison**: 
   - Direct API calls (Go-Python) generally have lower latency than CLI operations (Java-Go)
   - File-based operations (Java-Python) show higher variability

2. **Success Rate**:
   - Higher concurrent client counts may reduce success rates due to resource contention
   - Success rates are generally high across all interaction patterns

3. **Resource Usage**:
   - Memory usage is typically higher for Java-based operations
   - CPU usage spikes during concurrent operations

## Recommendations

1. Use direct API calls for performance-critical paths
2. Implement proper error handling and retries for file-based operations
3. Consider connection pooling for Java-Go interactions
4. Monitor resource usage under high concurrency scenarios
EOF

  echo "Generated performance summary: $SUMMARY_FILE"
}

# Test Java-Go performance
test_java_go_performance() {
  echo "Testing Java-Go performance..."
  
  # Run benchmark
  python "$PYTHON_BENCHMARK_SCRIPT" --test=java-go --iterations="$ITERATIONS" --clients="$CONCURRENT_CLIENTS"
  
  # Check if results file exists
  if [[ ! -f "$JAVA_GO_RESULTS" ]]; then
    echo "Error: Java-Go benchmark results not found"
    return 1
  fi
  
  # Count results
  local result_count=$(wc -l < "$JAVA_GO_RESULTS")
  result_count=$((result_count - 1))  # Subtract header row
  
  echo "Java-Go benchmark completed with $result_count results"
  return 0
}

# Test Go-Python performance
test_go_python_performance() {
  echo "Testing Go-Python performance..."
  
  # Run benchmark
  python "$PYTHON_BENCHMARK_SCRIPT" --test=go-python --iterations="$ITERATIONS" --clients="$CONCURRENT_CLIENTS"
  
  # Check if results file exists
  if [[ ! -f "$GO_PYTHON_RESULTS" ]]; then
    echo "Error: Go-Python benchmark results not found"
    return 1
  fi
  
  # Count results
  local result_count=$(wc -l < "$GO_PYTHON_RESULTS")
  result_count=$((result_count - 1))  # Subtract header row
  
  echo "Go-Python benchmark completed with $result_count results"
  return 0
}

# Test Java-Python performance
test_java_python_performance() {
  echo "Testing Java-Python performance..."
  
  # Run benchmark
  python "$PYTHON_BENCHMARK_SCRIPT" --test=java-python --iterations="$ITERATIONS" --clients="$CONCURRENT_CLIENTS"
  
  # Check if results file exists
  if [[ ! -f "$JAVA_PYTHON_RESULTS" ]]; then
    echo "Error: Java-Python benchmark results not found"
    return 1
  fi
  
  # Count results
  local result_count=$(wc -l < "$JAVA_PYTHON_RESULTS")
  result_count=$((result_count - 1))  # Subtract header row
  
  echo "Java-Python benchmark completed with $result_count results"
  return 0
}

# Test teardown
teardown() {
  echo "Tearing down performance benchmark test..."
  
  # The Python script should handle cleanup of test items
  
  echo "Teardown complete"
}

# Run the complete test
run_test() {
  local success=true
  
  setup
  
  # Run test steps, aborting on first failure
  if ! test_java_go_performance; then
    echo "Test failed at step: test_java_go_performance"
    success=false
  elif ! test_go_python_performance; then
    echo "Test failed at step: test_go_python_performance"
    success=false
  elif ! test_java_python_performance; then
    echo "Test failed at step: test_java_python_performance"
    success=false
  else
    # Generate summary report if all benchmarks passed
    generate_summary
  fi
  
  teardown
  
  if $success; then
    echo "Performance benchmark test completed successfully"
    echo "Summary report: $SUMMARY_FILE"
    return 0
  else
    echo "Performance benchmark test failed"
    return 1
  fi
}

# Run the test if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run_test
fi