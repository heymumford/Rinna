#!/bin/bash
# Unit test for build process efficiency

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Test measuring build time metrics
test_build_time_measurement() {
    echo "Testing build time measurement functions..."
    
    # Create temporary build metrics file
    local metrics_file=$(mktemp)
    
    # Function to record build metrics
    record_build_metric() {
        local component=$1
        local duration=$2
        echo "$(date +%Y-%m-%d,%H:%M:%S),$component,$duration" >> "$metrics_file"
    }
    
    # Record some sample metrics
    record_build_metric "java-compile" "3.45"
    record_build_metric "go-build" "1.20"
    record_build_metric "python-lint" "0.75"
    
    # Verify metrics were recorded correctly
    local count=$(wc -l < "$metrics_file")
    if [ "$count" -ne 3 ]; then
        echo "Expected 3 metrics records, but got $count"
        rm "$metrics_file"
        return 1
    fi
    
    # Verify we can extract and analyze metrics
    local sum=$(awk -F, '{sum+=$3} END {print sum}' "$metrics_file")
    local count=$(wc -l < "$metrics_file")
    local avg_duration=$(echo "scale=2; $sum/$count" | bc)
    
    if ! [[ "$avg_duration" =~ ^[0-9]+\.[0-9]+$ ]] && ! [[ "$avg_duration" =~ ^[0-9]+$ ]]; then
        echo "Failed to calculate average duration: $avg_duration"
        rm "$metrics_file"
        return 1
    fi
    
    # Clean up
    rm "$metrics_file"
    return 0
}

# Test build threshold compliance
test_build_threshold_compliance() {
    echo "Testing build threshold compliance functions..."
    
    # Define build thresholds
    local max_compile_time=10
    local max_test_time=20
    local max_package_time=5
    
    # Create sample build durations
    local compile_time=8.2
    local test_time=15.7
    local package_time=3.4
    
    # Check threshold compliance
    local failures=0
    
    # Use simple integer comparisons with multiplication to avoid bc errors
    if (( $(printf "%.0f" $(echo "$compile_time*100" | bc)) > $(printf "%.0f" $(echo "$max_compile_time*100" | bc)) )); then
        echo "Compile time $compile_time exceeds threshold $max_compile_time"
        failures=$((failures + 1))
    fi
    
    if (( $(printf "%.0f" $(echo "$test_time*100" | bc)) > $(printf "%.0f" $(echo "$max_test_time*100" | bc)) )); then
        echo "Test time $test_time exceeds threshold $max_test_time"
        failures=$((failures + 1))
    fi
    
    if (( $(printf "%.0f" $(echo "$package_time*100" | bc)) > $(printf "%.0f" $(echo "$max_package_time*100" | bc)) )); then
        echo "Package time $package_time exceeds threshold $max_package_time"
        failures=$((failures + 1))
    fi
    
    # This test should pass as all times are below thresholds
    return $failures
}

# Run all tests
run_all_tests() {
    echo "Running unit tests for build efficiency..."
    
    local failures=0
    
    # Run each test and track failures
    test_build_time_measurement || failures=$((failures + 1))
    test_build_threshold_compliance || failures=$((failures + 1))
    
    echo "Completed unit tests for build efficiency"
    
    return $failures
}

# Run the tests
run_all_tests
exit $?