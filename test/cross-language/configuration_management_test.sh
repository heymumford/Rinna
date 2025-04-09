#!/bin/bash
#
# configuration_management_test.sh - Cross-language test for configuration management
#
# This test verifies that configuration settings can be properly shared and
# synchronized between Java, Go, and Python components in the Rinna system.
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
CONFIG_KEY="test.cross-language.setting"
CONFIG_VALUE="value-$(date +%s)"
PYTHON_CONFIG_SCRIPT="${TEST_TEMP_DIR}/config_manager.py"

# Test setup
setup() {
  echo "Setting up configuration management test..."
  
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
  
  # Create Python config management script
  create_python_config_script
  
  echo "Setup complete"
}

# Create Python configuration management script
create_python_config_script() {
  cat > "$PYTHON_CONFIG_SCRIPT" <<EOF
#!/usr/bin/env python3
"""
Configuration manager for cross-language testing.

This script provides a Python interface to Rinna's configuration system
for testing cross-language configuration management.
"""

import os
import sys
import json
import argparse
import requests
from datetime import datetime

# Configuration from environment
API_PORT = os.environ.get("RINNA_TEST_API_PORT", "8085")
API_URL = f"http://localhost:{API_PORT}/api"
TEST_TEMP_DIR = os.environ.get("RINNA_TEST_TEMP_DIR", "./target/cross-language-tests")

def get_config(key):
    """Get configuration value by key."""
    try:
        response = requests.get(f"{API_URL}/config/{key}")
        
        if response.status_code == 200:
            config = response.json()
            print(f"Retrieved configuration for {key}: {config}")
            return config
        else:
            print(f"Failed to get configuration for {key}: {response.status_code}")
            print(response.text)
            return None
    except Exception as e:
        print(f"Error getting configuration: {str(e)}")
        return None

def set_config(key, value, description=None):
    """Set configuration value."""
    try:
        # Prepare configuration data
        config_data = {
            "key": key,
            "value": value,
            "updatedAt": datetime.now().isoformat(),
            "updatedBy": "python-config-script"
        }
        
        if description:
            config_data["description"] = description
            
        # Send request to API
        response = requests.post(
            f"{API_URL}/config",
            json=config_data,
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code in (200, 201):
            result = response.json()
            print(f"Configuration set: {key}={value}")
            return True
        else:
            print(f"Failed to set configuration: {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print(f"Error setting configuration: {str(e)}")
        return False

def list_configs(prefix=None):
    """List configurations, optionally filtered by prefix."""
    try:
        url = f"{API_URL}/config"
        if prefix:
            url += f"?prefix={prefix}"
            
        response = requests.get(url)
        
        if response.status_code == 200:
            configs = response.json()
            print(f"Retrieved {len(configs)} configuration settings")
            print(json.dumps(configs, indent=2))
            return configs
        else:
            print(f"Failed to list configurations: {response.status_code}")
            print(response.text)
            return None
    except Exception as e:
        print(f"Error listing configurations: {str(e)}")
        return None

def delete_config(key):
    """Delete configuration by key."""
    try:
        response = requests.delete(f"{API_URL}/config/{key}")
        
        if response.status_code == 200:
            print(f"Configuration deleted: {key}")
            return True
        else:
            print(f"Failed to delete configuration: {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print(f"Error deleting configuration: {str(e)}")
        return False

def main():
    """Main function to process arguments and manage configurations."""
    parser = argparse.ArgumentParser(description="Configuration manager for cross-language testing")
    subparsers = parser.add_subparsers(dest="command", help="Command to execute")
    
    # Get command
    get_parser = subparsers.add_parser("get", help="Get configuration value")
    get_parser.add_argument("key", help="Configuration key")
    
    # Set command
    set_parser = subparsers.add_parser("set", help="Set configuration value")
    set_parser.add_argument("key", help="Configuration key")
    set_parser.add_argument("value", help="Configuration value")
    set_parser.add_argument("--description", "-d", help="Configuration description")
    
    # List command
    list_parser = subparsers.add_parser("list", help="List configurations")
    list_parser.add_argument("--prefix", "-p", help="Filter by prefix")
    
    # Delete command
    delete_parser = subparsers.add_parser("delete", help="Delete configuration")
    delete_parser.add_argument("key", help="Configuration key")
    
    args = parser.parse_args()
    
    if args.command == "get":
        config = get_config(args.key)
        if config and "value" in config:
            print(config["value"])
            sys.exit(0)
        else:
            sys.exit(1)
    
    elif args.command == "set":
        success = set_config(args.key, args.value, args.description)
        sys.exit(0 if success else 1)
    
    elif args.command == "list":
        configs = list_configs(args.prefix)
        sys.exit(0 if configs is not None else 1)
    
    elif args.command == "delete":
        success = delete_config(args.key)
        sys.exit(0 if success else 1)
    
    else:
        parser.print_help()
        sys.exit(1)

if __name__ == "__main__":
    main()
EOF

  chmod +x "$PYTHON_CONFIG_SCRIPT"
}

# Test teardown
teardown() {
  echo "Tearing down configuration management test..."
  
  # Clean up test configuration
  curl -s -X DELETE "http://localhost:$API_PORT/api/config/$CONFIG_KEY" > /dev/null || true
  
  echo "Teardown complete"
}

# Test Java CLI configuration setting
test_java_cli_config_set() {
  echo "Testing Java CLI configuration setting..."
  
  # Set configuration using CLI
  local set_output
  set_output=$("$CLI_PATH" config set "$CONFIG_KEY" "$CONFIG_VALUE" --description="Cross-language test configuration")
  
  # Check if setting succeeded
  if [[ $? -ne 0 ]]; then
    echo "Error: Failed to set configuration via Java CLI"
    echo "CLI output: $set_output"
    return 1
  fi
  
  echo "Configuration set successfully via Java CLI: $CONFIG_KEY=$CONFIG_VALUE"
  return 0
}

# Test Go API configuration retrieval
test_go_api_config_get() {
  echo "Testing Go API configuration retrieval..."
  
  # Retrieve configuration from API
  local response
  response=$(curl -s "http://localhost:$API_PORT/api/config/$CONFIG_KEY")
  
  # Check if configuration exists with correct value
  if echo "$response" | grep -q "\"key\":\"$CONFIG_KEY\"" && \
     echo "$response" | grep -q "\"value\":\"$CONFIG_VALUE\""; then
    echo "Configuration retrieved successfully from Go API"
    return 0
  else
    echo "Error: Configuration not found or has incorrect value"
    echo "API response: $response"
    return 1
  fi
}

# Test Python configuration update
test_python_config_update() {
  echo "Testing Python configuration update..."
  
  # Update configuration value with Python script
  local updated_value="${CONFIG_VALUE}-updated"
  
  if python "$PYTHON_CONFIG_SCRIPT" set "$CONFIG_KEY" "$updated_value" --description="Updated by Python"; then
    echo "Configuration updated successfully via Python"
    
    # Verify configuration was updated
    local get_result
    get_result=$(python "$PYTHON_CONFIG_SCRIPT" get "$CONFIG_KEY")
    
    if [[ "$get_result" == "$updated_value" ]]; then
      echo "Configuration update verified via Python get: $CONFIG_KEY=$updated_value"
      
      # Update global value for subsequent tests
      CONFIG_VALUE="$updated_value"
      return 0
    else
      echo "Error: Configuration not updated correctly"
      echo "Python get result: $get_result"
      return 1
    fi
  else
    echo "Error: Failed to update configuration via Python"
    return 1
  fi
}

# Test Java CLI configuration list
test_java_cli_config_list() {
  echo "Testing Java CLI configuration listing..."
  
  # List configurations using CLI
  local list_output
  list_output=$("$CLI_PATH" config list --prefix="test.cross-language" --output=json)
  
  # Check if listing succeeded
  if [[ $? -ne 0 ]]; then
    echo "Error: Failed to list configurations via Java CLI"
    echo "CLI output: $list_output"
    return 1
  fi
  
  # Check if our configuration is in the list
  if echo "$list_output" | grep -q "\"key\":\"$CONFIG_KEY\"" && \
     echo "$list_output" | grep -q "\"value\":\"$CONFIG_VALUE\""; then
    echo "Configuration found in Java CLI listing"
    return 0
  else
    echo "Error: Configuration not found in Java CLI listing"
    echo "CLI output: $list_output"
    return 1
  fi
}

# Test Go API configuration listing
test_go_api_config_list() {
  echo "Testing Go API configuration listing..."
  
  # List configurations from API
  local response
  response=$(curl -s "http://localhost:$API_PORT/api/config?prefix=test.cross-language")
  
  # Check if our configuration is in the list
  if echo "$response" | grep -q "\"key\":\"$CONFIG_KEY\"" && \
     echo "$response" | grep -q "\"value\":\"$CONFIG_VALUE\""; then
    echo "Configuration found in Go API listing"
    return 0
  else
    echo "Error: Configuration not found in Go API listing"
    echo "API response: $response"
    return 1
  fi
}

# Test Python configuration listing
test_python_config_list() {
  echo "Testing Python configuration listing..."
  
  # List configurations using Python script
  local list_output
  list_output=$(python "$PYTHON_CONFIG_SCRIPT" list --prefix="test.cross-language")
  
  # Check if listing succeeded and contains our configuration
  if echo "$list_output" | grep -q "\"key\":\"$CONFIG_KEY\"" && \
     echo "$list_output" | grep -q "\"value\":\"$CONFIG_VALUE\""; then
    echo "Configuration found in Python listing"
    return 0
  else
    echo "Error: Configuration not found in Python listing"
    echo "Python output: $list_output"
    return 1
  fi
}

# Test Java CLI configuration deletion
test_java_cli_config_delete() {
  echo "Testing Java CLI configuration deletion..."
  
  # Delete configuration using CLI
  local delete_output
  delete_output=$("$CLI_PATH" config delete "$CONFIG_KEY")
  
  # Check if deletion succeeded
  if [[ $? -ne 0 ]]; then
    echo "Error: Failed to delete configuration via Java CLI"
    echo "CLI output: $delete_output"
    return 1
  fi
  
  # Verify configuration was deleted
  local verify_output
  verify_output=$(curl -s -w "%{http_code}" "http://localhost:$API_PORT/api/config/$CONFIG_KEY")
  
  # Extract HTTP status code (last line)
  local status_code=${verify_output: -3}
  
  if [[ "$status_code" == "404" ]]; then
    echo "Configuration successfully deleted and verified"
    return 0
  else
    echo "Error: Configuration still exists after deletion"
    echo "API response status: $status_code"
    return 1
  fi
}

# Run the complete test
run_test() {
  local success=true
  
  setup
  
  # Run test steps, aborting on first failure
  if ! test_java_cli_config_set; then
    echo "Test failed at step: test_java_cli_config_set"
    success=false
  elif ! test_go_api_config_get; then
    echo "Test failed at step: test_go_api_config_get"
    success=false
  elif ! test_python_config_update; then
    echo "Test failed at step: test_python_config_update"
    success=false
  elif ! test_java_cli_config_list; then
    echo "Test failed at step: test_java_cli_config_list"
    success=false
  elif ! test_go_api_config_list; then
    echo "Test failed at step: test_go_api_config_list"
    success=false
  elif ! test_python_config_list; then
    echo "Test failed at step: test_python_config_list"
    success=false
  elif ! test_java_cli_config_delete; then
    echo "Test failed at step: test_java_cli_config_delete"
    success=false
  fi
  
  teardown
  
  if $success; then
    echo "Configuration management test completed successfully"
    return 0
  else
    echo "Configuration management test failed"
    return 1
  fi
}

# Run the test if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run_test
fi