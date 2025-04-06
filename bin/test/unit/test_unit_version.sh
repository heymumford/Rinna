#!/bin/bash
# Unit test for version extraction functions

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Source any common test functions
# source "$SCRIPT_DIR/../common/test_utils.sh"

# Test extracting version from a properties file
test_extract_version_from_properties() {
    # Create a temporary properties file
    local temp_file=$(mktemp)
    echo "# This is a comment" > "$temp_file"
    echo "version=1.2.3" >> "$temp_file"
    echo "other=value" >> "$temp_file"
    
    # Extract the version using grep
    local version=$(grep -m 1 "^version=" "$temp_file" | cut -d'=' -f2)
    
    # Verify the expected result
    if [ "$version" != "1.2.3" ]; then
        echo "Expected version to be 1.2.3, but got $version"
        rm "$temp_file"
        return 1
    fi
    
    # Clean up
    rm "$temp_file"
    return 0
}

# Test parsing a semantic version string
test_parse_semver() {
    local version="1.2.3"
    
    # Parse the version string
    local major=$(echo "$version" | cut -d. -f1)
    local minor=$(echo "$version" | cut -d. -f2)
    local patch=$(echo "$version" | cut -d. -f3)
    
    # Verify the expected result
    if [ "$major" != "1" ] || [ "$minor" != "2" ] || [ "$patch" != "3" ]; then
        echo "Failed to parse version $version correctly"
        echo "Got: major=$major, minor=$minor, patch=$patch"
        return 1
    fi
    
    return 0
}

# Run all tests
run_all_tests() {
    echo "Running unit tests for version functions..."
    
    local failures=0
    
    # Run each test and track failures
    test_extract_version_from_properties || failures=$((failures + 1))
    test_parse_semver || failures=$((failures + 1))
    
    echo "Completed unit tests for version functions"
    
    return $failures
}

# Run the tests
run_all_tests
exit $?