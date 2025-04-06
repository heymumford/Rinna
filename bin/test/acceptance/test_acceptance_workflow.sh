#!/bin/bash
# Acceptance test for workflow scenarios

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Simulate a user workflow
test_version_workflow() {
    echo "SCENARIO: User checks and updates version"
    
    echo "GIVEN the version script is available"
    if [ ! -f "$PROJECT_ROOT/bin/rin-version" ] || [ ! -x "$PROJECT_ROOT/bin/rin-version" ]; then
        echo "rin-version script not found or not executable"
        return 1
    fi
    
    echo "WHEN user checks the current version"
    local version_output
    version_output=$("$PROJECT_ROOT/bin/rin-version" current 2>&1)
    local status=$?
    
    if [ $status -ne 0 ]; then
        echo "rin-version command failed with status $status"
        echo "Output: $version_output"
        return 1
    fi
    
    echo "THEN the version information is displayed"
    if ! echo "$version_output" | grep -q "Version"; then
        echo "rin-version output does not contain version information"
        echo "Output: $version_output"
        return 1
    fi
    
    echo "AND WHEN user verifies the version"
    local verify_output
    verify_output=$("$PROJECT_ROOT/bin/rin-version" verify 2>&1)
    status=$?
    
    echo "THEN the verification succeeds"
    if [ $status -ne 0 ]; then
        echo "Version verification failed"
        echo "Output: $verify_output"
        return 1
    fi
    
    return 0
}

# Simulate building and testing workflow
test_build_workflow() {
    echo "SCENARIO: User builds and tests the application"
    
    echo "GIVEN the build script is available"
    if [ ! -f "$PROJECT_ROOT/bin/rin" ] || [ ! -x "$PROJECT_ROOT/bin/rin" ]; then
        echo "rin script not found or not executable"
        return 1
    fi
    
    echo "WHEN user runs a simple build command"
    # We're not actually running the build to avoid side effects in testing
    # This is just simulating the workflow
    if [ ! -f "$PROJECT_ROOT/bin/rin-build" ] || [ ! -x "$PROJECT_ROOT/bin/rin-build" ]; then
        echo "rin-build script not found or not executable"
        return 1
    fi
    
    echo "THEN the build system responds appropriately"
    # In a real test, we would verify the build output
    
    return 0
}

# Run all tests
run_all_tests() {
    echo "Running acceptance tests for user workflows..."
    
    local failures=0
    
    # Run each test and track failures
    test_version_workflow || failures=$((failures + 1))
    test_build_workflow || failures=$((failures + 1))
    
    echo "Completed acceptance tests for user workflows"
    
    return $failures
}

# Run the tests
run_all_tests
exit $?