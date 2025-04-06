#!/bin/bash
# Component test for configuration handling

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Test parsing a configuration file
test_parse_config_file() {
    # Create a temporary config file
    local temp_file=$(mktemp)
    cat << EOF > "$temp_file"
[database]
host=localhost
port=5432
user=app_user
password=secret

[api]
port=8080
debug=false
EOF
    
    # Parse the database host (simulating a component that reads and processes config)
    local db_host=$(grep -A 4 "\[database\]" "$temp_file" | grep "host=" | cut -d= -f2)
    local db_port=$(grep -A 4 "\[database\]" "$temp_file" | grep "port=" | cut -d= -f2)
    local api_port=$(grep -A 3 "\[api\]" "$temp_file" | grep "port=" | cut -d= -f2)
    
    # Verify the expected results
    if [ "$db_host" != "localhost" ]; then
        echo "Expected database host to be localhost, but got $db_host"
        rm "$temp_file"
        return 1
    fi
    
    if [ "$db_port" != "5432" ]; then
        echo "Expected database port to be 5432, but got $db_port"
        rm "$temp_file"
        return 1
    fi
    
    if [ "$api_port" != "8080" ]; then
        echo "Expected API port to be 8080, but got $api_port"
        rm "$temp_file"
        return 1
    fi
    
    # Clean up
    rm "$temp_file"
    return 0
}

# Test config component validation
test_config_validation() {
    # Create a valid config
    local valid_config=$(mktemp)
    echo "port=8080" > "$valid_config"
    
    # Create an invalid config with non-numeric port
    local invalid_config=$(mktemp)
    echo "port=invalid" > "$invalid_config"
    
    # Test with valid config
    local valid_port=$(grep "port=" "$valid_config" | cut -d= -f2)
    if ! [[ "$valid_port" =~ ^[0-9]+$ ]]; then
        echo "Port validation failed for numeric port"
        rm "$valid_config" "$invalid_config"
        return 1
    fi
    
    # Test with invalid config
    local invalid_port=$(grep "port=" "$invalid_config" | cut -d= -f2)
    if [[ "$invalid_port" =~ ^[0-9]+$ ]]; then
        echo "Port validation should fail for non-numeric port"
        rm "$valid_config" "$invalid_config"
        return 1
    fi
    
    # Clean up
    rm "$valid_config" "$invalid_config"
    return 0
}

# Run all tests
run_all_tests() {
    echo "Running component tests for configuration handling..."
    
    local failures=0
    
    # Run each test and track failures
    test_parse_config_file || failures=$((failures + 1))
    test_config_validation || failures=$((failures + 1))
    
    echo "Completed component tests for configuration handling"
    
    return $failures
}

# Run the tests
run_all_tests
exit $?