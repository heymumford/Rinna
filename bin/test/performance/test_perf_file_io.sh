#!/bin/bash
# Performance test for file I/O operations

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Test file reading performance
test_file_read_performance() {
    echo "Measuring file read performance..."
    
    # Create a test file with 10000 lines
    local test_file=$(mktemp)
    for i in {1..10000}; do
        echo "This is test line $i with some content to make it realistic" >> "$test_file"
    done
    
    # Time reading the file line by line
    local start_time=$(date +%s.%N)
    
    while read -r line; do
        # Just read, don't process
        :
    done < "$test_file"
    
    local end_time=$(date +%s.%N)
    local elapsed=$(echo "$end_time - $start_time" | bc)
    
    echo "Reading 10000 lines took $elapsed seconds"
    
    # Establish a baseline threshold (adjust as needed)
    local threshold=0.5  # 500 milliseconds
    if (( $(echo "$elapsed > $threshold" | bc -l) )); then
        echo "WARNING: File reading performance is below threshold"
        echo "Expected: <$threshold seconds, Actual: $elapsed seconds"
    else
        echo "File reading performance is acceptable"
    fi
    
    # Clean up
    rm "$test_file"
    
    # This is a benchmark, so it doesn't fail unless there's an error
    return 0
}

# Test file writing performance
test_file_write_performance() {
    echo "Measuring file write performance..."
    
    local test_file=$(mktemp)
    
    # Time writing 10000 lines to a file
    local start_time=$(date +%s.%N)
    
    for i in {1..10000}; do
        echo "This is test line $i with some content to make it realistic" >> "$test_file"
    done
    
    local end_time=$(date +%s.%N)
    local elapsed=$(echo "$end_time - $start_time" | bc)
    
    echo "Writing 10000 lines took $elapsed seconds"
    
    # Establish a baseline threshold (adjust as needed)
    local threshold=1.0  # 1 second
    if (( $(echo "$elapsed > $threshold" | bc -l) )); then
        echo "WARNING: File writing performance is below threshold"
        echo "Expected: <$threshold seconds, Actual: $elapsed seconds"
    else
        echo "File writing performance is acceptable"
    fi
    
    # Clean up
    rm "$test_file"
    
    # This is a benchmark, so it doesn't fail unless there's an error
    return 0
}

# Run all tests
run_all_tests() {
    echo "Running performance tests for file I/O operations..."
    
    local failures=0
    
    # Run each test and track failures
    test_file_read_performance || failures=$((failures + 1))
    test_file_write_performance || failures=$((failures + 1))
    
    echo "Completed performance tests for file I/O operations"
    
    return $failures
}

# Run the tests
run_all_tests
exit $?