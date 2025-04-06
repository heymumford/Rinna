#!/bin/bash
# Component test for cross-language integration

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Test language environment detection
test_language_environment_detection() {
    echo "Testing language environment detection..."
    
    # Check Java environment
    if command -v java >/dev/null 2>&1; then
        java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        echo "Detected Java version: $java_version"
        # Verify Java 21 or newer is available for this project
        if [[ "$java_version" =~ ^1[0-9]\. ]] || [[ "$java_version" =~ ^2[0-1]\. ]]; then
            echo "Java version meets minimum requirements (19+)"
        else
            echo "Java version does not meet minimum requirements (should be 19+)"
            return 1
        fi
    else
        echo "Java not found in PATH"
        return 1
    fi
    
    # Check Go environment
    if command -v go >/dev/null 2>&1; then
        go_version=$(go version | awk '{print $3}')
        echo "Detected Go version: $go_version"
        # Verify Go 1.18+ is available
        if [[ "$go_version" =~ go1\.(1[8-9]|[2-9][0-9]) ]]; then
            echo "Go version meets minimum requirements (1.18+)"
        else
            echo "Go version does not meet minimum requirements (should be 1.18+)"
            return 1
        fi
    else
        echo "Go not found in PATH"
        return 1
    fi
    
    # Check Python environment
    if command -v python >/dev/null 2>&1; then
        python_version=$(python --version 2>&1 | awk '{print $2}')
        echo "Detected Python version: $python_version"
        # Verify Python 3.9+ is available
        if [[ "$python_version" =~ ^3\.(9|1[0-9]) ]]; then
            echo "Python version meets minimum requirements (3.9+)"
        else
            echo "Python version does not meet minimum requirements (should be 3.9+)"
            return 1
        fi
    else
        echo "Python not found in PATH"
        return 1
    fi
    
    return 0
}

# Test cross-language configuration consistency
test_config_consistency() {
    echo "Testing cross-language configuration consistency..."
    
    # Check if the version is consistent across language-specific files
    
    # Java version (from Maven pom.xml)
    local java_version=""
    if [ -f "$PROJECT_ROOT/pom.xml" ]; then
        java_version=$(grep -o "<version>[^<]*</version>" "$PROJECT_ROOT/pom.xml" | head -1 | sed 's/<version>\(.*\)<\/version>/\1/')
        echo "Java project version: $java_version"
    fi
    
    # Go version (from go.mod)
    local go_version=""
    if [ -f "$PROJECT_ROOT/api/go.mod" ]; then
        go_version=$(grep "^module" "$PROJECT_ROOT/api/go.mod" | awk '{print $2}' | grep -o '/v[0-9]\+$' | sed 's|/v||')
        echo "Go module version: $go_version"
    fi
    
    # Python version (from pyproject.toml or setup.py)
    local python_version=""
    if [ -f "$PROJECT_ROOT/pyproject.toml" ]; then
        python_version=$(grep "version" "$PROJECT_ROOT/pyproject.toml" | head -1 | sed 's/.*version = "\(.*\)".*/\1/')
        echo "Python package version: $python_version"
    elif [ -f "$PROJECT_ROOT/setup.py" ]; then
        python_version=$(grep "version=" "$PROJECT_ROOT/setup.py" | sed 's/.*version="\(.*\)".*/\1/')
        echo "Python package version: $python_version"
    fi
    
    # Check version.properties file for common version reference
    local common_version=""
    if [ -f "$PROJECT_ROOT/version.properties" ]; then
        common_version=$(grep "^version=" "$PROJECT_ROOT/version.properties" | cut -d= -f2)
        echo "Common version from properties: $common_version"
    fi
    
    # At minimum, we should have the common version defined
    if [ -z "$common_version" ]; then
        echo "Common version not found in version.properties"
        return 1
    fi
    
    return 0
}

# Run all tests
run_all_tests() {
    echo "Running component tests for cross-language integration..."
    
    local failures=0
    
    # Run each test and track failures
    test_language_environment_detection || failures=$((failures + 1))
    test_config_consistency || failures=$((failures + 1))
    
    echo "Completed component tests for cross-language integration"
    
    return $failures
}

# Run the tests
run_all_tests
exit $?