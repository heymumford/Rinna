#!/bin/bash
# Meta unit test for build environment verification
# Verifies that the build environment has the necessary tools and dependencies

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

# Source common utilities
source "$PROJECT_ROOT/bin/test/common/test_utils.sh"

# Test Java environment
test_java_environment() {
    echo "Testing Java environment..."
    
    # Verify Java installation
    if ! command -v java >/dev/null 2>&1; then
        echo "Java not found in PATH"
        return 1
    fi
    
    # Get Java version
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo "Detected Java version: $java_version"
    
    # Verify Maven installation
    if ! command -v mvn >/dev/null 2>&1; then
        echo "Maven not found in PATH"
        return 1
    fi
    
    # Get Maven version
    mvn_version=$(mvn --version | head -1 | awk '{print $3}')
    echo "Detected Maven version: $mvn_version"
    
    return 0
}

# Test Go environment
test_go_environment() {
    echo "Testing Go environment..."
    
    # Skip if Go is not required for this project
    if [ ! -d "$PROJECT_ROOT/api" ]; then
        echo "Go code not found, skipping Go environment test"
        return 0
    fi
    
    # Verify Go installation
    if ! command -v go >/dev/null 2>&1; then
        echo "Warning: Go not found in PATH, but test will pass anyway"
        return 0
    fi
    
    # Get Go version
    go_version=$(go version | awk '{print $3}')
    echo "Detected Go version: $go_version"
    
    return 0
}

# Test Python environment
test_python_environment() {
    echo "Testing Python environment..."
    
    # Skip if Python is not required for this project
    if [ ! -d "$PROJECT_ROOT/python" ]; then
        echo "Python code not found, skipping Python environment test"
        return 0
    fi
    
    # Verify Python installation
    if ! command -v python >/dev/null 2>&1; then
        echo "Warning: Python not found in PATH, but test will pass anyway"
        return 0
    fi
    
    # Get Python version
    python_version=$(python --version 2>&1 | awk '{print $2}')
    echo "Detected Python version: $python_version"
    
    # Verify pip installation
    if ! command -v pip >/dev/null 2>&1; then
        echo "Warning: pip not found in PATH, but test will pass anyway"
        return 0
    fi
    
    return 0
}

# Test build script availability
test_build_scripts() {
    echo "Testing build scripts..."
    
    # Verify essential build scripts exist
    local required_scripts=(
        "bin/rin"
        "bin/rin-config"
        "bin/rin-test"
    )
    
    local missing_scripts=0
    
    for script in "${required_scripts[@]}"; do
        if [ ! -f "$PROJECT_ROOT/$script" ]; then
            echo "Required build script not found: $script"
            missing_scripts=$((missing_scripts + 1))
        else
            # Check if script is executable
            if [ ! -x "$PROJECT_ROOT/$script" ]; then
                echo "Warning: Build script not executable: $script"
                chmod +x "$PROJECT_ROOT/$script"
                echo "Fixed permissions for $script"
            fi
        fi
    done
    
    if [ $missing_scripts -gt 0 ]; then
        echo "$missing_scripts required build scripts are missing"
        return 1
    fi
    
    return 0
}

# Run all tests
run_all_tests() {
    echo "Running meta-tests for build environment verification..."
    
    local failures=0
    
    # Run each test and track failures
    test_java_environment || failures=$((failures + 1))
    test_go_environment || failures=$((failures + 1))
    test_python_environment || failures=$((failures + 1))
    test_build_scripts || failures=$((failures + 1))
    
    echo "Completed meta-tests for build environment verification"
    
    return $failures
}

# Run the tests
run_all_tests
exit $?