#!/bin/bash
# Performance test for build pipeline efficiency

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Define performance thresholds (in seconds)
THRESHOLD_JAVA_COMPILE=30
THRESHOLD_GO_BUILD=10
THRESHOLD_PY_BUILD=5
THRESHOLD_OVERALL=60

# Test Maven build performance
test_maven_build_performance() {
    echo "Testing Maven build performance..."
    
    if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
        echo "Maven configuration not found: pom.xml"
        return 1
    fi
    
    # Prepare by cleaning target directories to ensure accurate timing
    mvn clean -q >/dev/null 2>&1
    
    # Measure build time for compile phase only
    local start_time=$(date +%s.%N)
    mvn compile -DskipTests -q >/dev/null 2>&1
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc)
    local duration_int=$(echo "$duration" | awk '{print int($1)}')
    
    echo "Maven compile phase took $duration seconds"
    
    # Compare with threshold
    if [ "$duration_int" -gt $THRESHOLD_JAVA_COMPILE ]; then
        echo "Maven compile time ($duration seconds) exceeds threshold ($THRESHOLD_JAVA_COMPILE seconds)"
        return 1
    fi
    
    return 0
}

# Test Go build performance
test_go_build_performance() {
    echo "Testing Go build performance..."
    
    if [ ! -d "$PROJECT_ROOT/api" ] || [ ! -f "$PROJECT_ROOT/api/go.mod" ]; then
        echo "Go module configuration not found"
        return 0  # Skip rather than fail if Go not present
    fi
    
    # Measure build time
    (
        cd "$PROJECT_ROOT/api"
        
        # Clean any previous builds
        go clean
        
        # Measure build time
        local start_time=$(date +%s.%N)
        go build ./... >/dev/null 2>&1
        local end_time=$(date +%s.%N)
        local duration=$(echo "$end_time - $start_time" | bc)
        local duration_int=$(echo "$duration" | awk '{print int($1)}')
        
        echo "Go build took $duration seconds"
        
        # Compare with threshold
        if [ "$duration_int" -gt $THRESHOLD_GO_BUILD ]; then
            echo "Go build time ($duration seconds) exceeds threshold ($THRESHOLD_GO_BUILD seconds)"
            exit 1
        fi
        
        exit 0
    )
    return $?
}

# Test Python build/check performance
test_python_build_performance() {
    echo "Testing Python build performance..."
    
    if [ ! -d "$PROJECT_ROOT/python" ]; then
        echo "Python module not found"
        return 0  # Skip rather than fail if Python not present
    fi
    
    # Check if Python requirements installed
    if [ -f "$PROJECT_ROOT/requirements.txt" ]; then
        # Measure the time to run static type checking
        local start_time=$(date +%s.%N)
        python -m mypy "$PROJECT_ROOT/python" >/dev/null 2>&1 || true  # Don't fail if mypy not installed
        local end_time=$(date +%s.%N)
        local duration=$(echo "$end_time - $start_time" | bc)
        local duration_int=$(echo "$duration" | awk '{print int($1)}')
        
        echo "Python type checking took $duration seconds"
        
        # Compare with threshold
        if [ "$duration_int" -gt $THRESHOLD_PY_BUILD ]; then
            echo "Python type checking time ($duration seconds) exceeds threshold ($THRESHOLD_PY_BUILD seconds)"
            return 1
        fi
    else
        echo "Python requirements.txt not found, skipping Python build check"
    fi
    
    return 0
}

# Test overall build orchestration performance
test_build_orchestration() {
    echo "Testing build orchestration performance..."
    
    if [ ! -f "$PROJECT_ROOT/bin/rin" ]; then
        echo "Build orchestration script not found: bin/rin"
        return 1
    fi
    
    # Ensure it's executable
    chmod +x "$PROJECT_ROOT/bin/rin"
    
    # Measure the time to run a minimal build
    local start_time=$(date +%s.%N)
    "$PROJECT_ROOT/bin/rin" build >/dev/null 2>&1 || true
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc)
    local duration_int=$(echo "$duration" | awk '{print int($1)}')
    
    echo "Overall build orchestration took $duration seconds"
    
    # Compare with threshold
    if [ "$duration_int" -gt $THRESHOLD_OVERALL ]; then
        echo "Overall build time ($duration seconds) exceeds threshold ($THRESHOLD_OVERALL seconds)"
        return 1
    fi
    
    return 0
}

# Test build cache effectiveness
test_build_cache_effectiveness() {
    echo "Testing build cache effectiveness..."
    
    # Run a clean build first to establish baseline
    mvn clean -q >/dev/null 2>&1
    local clean_start=$(date +%s.%N)
    mvn compile -DskipTests -q >/dev/null 2>&1
    local clean_end=$(date +%s.%N)
    local clean_duration=$(echo "$clean_end - $clean_start" | bc)
    
    # Now run an incremental build
    local incr_start=$(date +%s.%N)
    mvn compile -DskipTests -q >/dev/null 2>&1
    local incr_end=$(date +%s.%N)
    local incr_duration=$(echo "$incr_end - $incr_start" | bc)
    
    # Calculate the speedup factor
    local speedup=$(echo "$clean_duration / $incr_duration" | bc -l)
    
    echo "Clean build took $clean_duration seconds"
    echo "Incremental build took $incr_duration seconds"
    echo "Cache speedup factor: $speedup"
    
    # The incremental build should be significantly faster
    if (( $(echo "$speedup < 1.5" | bc -l) )); then
        echo "Build cache not providing enough speedup (factor: $speedup, expected at least 1.5)"
        return 1
    fi
    
    return 0
}

# Run all tests
run_all_tests() {
    echo "Running performance tests for build pipeline..."
    
    local failures=0
    
    # Run each test and track failures
    test_maven_build_performance || failures=$((failures + 1))
    test_go_build_performance || failures=$((failures + 1))
    test_python_build_performance || failures=$((failures + 1))
    test_build_orchestration || failures=$((failures + 1))
    test_build_cache_effectiveness || failures=$((failures + 1))
    
    echo "Completed performance tests for build pipeline"
    
    return $failures
}

# Run the tests
run_all_tests
exit $?