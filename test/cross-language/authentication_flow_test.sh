#!/bin/bash
#
# authentication_flow_test.sh - Cross-language test for authentication flow
#
# This test verifies that authentication tokens and user sessions can be
# properly maintained and validated across Java, Go, and Python components.
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
TEST_USER="testuser"
TEST_PASS="Test@123"
TEST_TOKEN=""
TEST_SESSION_FILE="${TEST_TEMP_DIR}/test_session.json"
TEST_PYTHON_SCRIPT="${TEST_TEMP_DIR}/auth_validator.py"

# Test setup
setup() {
  echo "Setting up authentication flow test..."
  
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
  
  # Create Python authentication validator script
  create_python_auth_validator
  
  echo "Setup complete"
}

# Create Python validator script
create_python_auth_validator() {
  cat > "$TEST_PYTHON_SCRIPT" <<EOF
#!/usr/bin/env python3
"""
Authentication validator for cross-language testing.

This script validates authentication tokens across language boundaries
and provides a Python interface for testing authentication flows.
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

def validate_token(token):
    """Validate a token against the API."""
    if not token:
        print("Error: No token provided")
        return False
    
    # Call API with token
    headers = {"Authorization": f"Bearer {token}"}
    
    try:
        response = requests.get(f"{API_URL}/user/validate", headers=headers)
        
        if response.status_code == 200:
            print(f"Token validated successfully: {token[:8]}...")
            return True
        else:
            print(f"Token validation failed: {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print(f"Error validating token: {str(e)}")
        return False

def create_session_from_token(token, session_file):
    """Create a Python session file from a token."""
    if not token:
        print("Error: No token provided")
        return False
    
    # Create session data
    session = {
        "token": token,
        "created_at": datetime.now().isoformat(),
        "created_by": "python-auth-validator",
        "metadata": {
            "source": "cross-language-test",
            "api_url": API_URL
        }
    }
    
    # Write to file
    try:
        with open(session_file, 'w') as f:
            json.dump(session, f, indent=2)
        print(f"Session created at {session_file}")
        return True
    except Exception as e:
        print(f"Error creating session: {str(e)}")
        return False

def test_api_with_session(session_file):
    """Test API access using a session file."""
    try:
        # Read session from file
        with open(session_file, 'r') as f:
            session = json.load(f)
        
        if "token" not in session:
            print("Error: Invalid session file, no token found")
            return False
        
        # Use token to access a protected endpoint
        headers = {"Authorization": f"Bearer {session['token']}"}
        response = requests.get(f"{API_URL}/user/profile", headers=headers)
        
        if response.status_code == 200:
            print("API access with session successful")
            user_data = response.json()
            print(f"User: {user_data.get('username', 'unknown')}")
            return True
        else:
            print(f"API access with session failed: {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print(f"Error testing API with session: {str(e)}")
        return False

def login_and_get_token(username, password):
    """Login using Python client and get token."""
    try:
        # Login request
        login_data = {
            "username": username,
            "password": password
        }
        
        response = requests.post(
            f"{API_URL}/auth/login",
            json=login_data,
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            token_data = response.json()
            if "token" in token_data:
                print(f"Login successful, token: {token_data['token'][:8]}...")
                return token_data["token"]
            else:
                print("Error: Login succeeded but no token returned")
                return None
        else:
            print(f"Login failed: {response.status_code}")
            print(response.text)
            return None
    except Exception as e:
        print(f"Error during login: {str(e)}")
        return None

def main():
    """Main function to process arguments and run appropriate action."""
    parser = argparse.ArgumentParser(description="Authentication validator for cross-language testing")
    subparsers = parser.add_subparsers(dest="command", help="Command to execute")
    
    # Validate token command
    validate_parser = subparsers.add_parser("validate", help="Validate a token")
    validate_parser.add_argument("token", help="Authentication token to validate")
    
    # Create session command
    create_parser = subparsers.add_parser("create-session", help="Create a session from token")
    create_parser.add_argument("token", help="Authentication token")
    create_parser.add_argument("--output", "-o", help="Output session file path")
    
    # Test session command
    test_parser = subparsers.add_parser("test-session", help="Test API access using a session")
    test_parser.add_argument("session_file", help="Path to session file")
    
    # Login command
    login_parser = subparsers.add_parser("login", help="Login and get token")
    login_parser.add_argument("username", help="Username for login")
    login_parser.add_argument("password", help="Password for login")
    
    args = parser.parse_args()
    
    if args.command == "validate":
        success = validate_token(args.token)
        sys.exit(0 if success else 1)
    
    elif args.command == "create-session":
        output = args.output or os.path.join(TEST_TEMP_DIR, "python_session.json")
        success = create_session_from_token(args.token, output)
        sys.exit(0 if success else 1)
    
    elif args.command == "test-session":
        success = test_api_with_session(args.session_file)
        sys.exit(0 if success else 1)
    
    elif args.command == "login":
        token = login_and_get_token(args.username, args.password)
        if token:
            print(token)
            sys.exit(0)
        else:
            sys.exit(1)
    
    else:
        parser.print_help()
        sys.exit(1)

if __name__ == "__main__":
    main()
EOF

  chmod +x "$TEST_PYTHON_SCRIPT"
}

# Test teardown
teardown() {
  echo "Tearing down authentication flow test..."
  
  # Clean up test files
  rm -f "$TEST_SESSION_FILE" 2>/dev/null || true
  
  # Logout if logged in
  "$CLI_PATH" logout > /dev/null 2>&1 || true
  
  echo "Teardown complete"
}

# Test Java CLI login and token generation
test_java_cli_login() {
  echo "Testing Java CLI login..."
  
  # Use CLI login command with test credentials
  # In a real test, we might use an environment-specific test user
  local login_output
  login_output=$("$CLI_PATH" login "$TEST_USER" --password="$TEST_PASS" --output=json)
  
  # Check for error
  if [[ $? -ne 0 ]]; then
    echo "Error: Login failed"
    echo "CLI output: $login_output"
    return 1
  fi
  
  # Extract token from output
  TEST_TOKEN=$(echo "$login_output" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
  
  if [[ -z "$TEST_TOKEN" ]]; then
    echo "Error: Failed to extract token from login output"
    echo "CLI output: $login_output"
    return 1
  fi
  
  echo "Successfully logged in and obtained token"
  echo "Token starts with: ${TEST_TOKEN:0:8}..."
  return 0
}

# Test Go API token validation
test_go_api_token_validation() {
  echo "Testing Go API token validation..."
  
  if [[ -z "$TEST_TOKEN" ]]; then
    echo "Error: No token available for validation"
    return 1
  fi
  
  # Call API to validate token
  local response
  response=$(curl -s -w "%{http_code}" -H "Authorization: Bearer $TEST_TOKEN" "http://localhost:$API_PORT/api/user/validate")
  
  # Extract HTTP status code (last line)
  local status_code=${response: -3}
  # Remove status code from response body
  local response_body=${response:0:$((${#response} - 3))}
  
  if [[ "$status_code" == "200" ]]; then
    echo "Token validated successfully by Go API"
    return 0
  else
    echo "Error: Token validation failed"
    echo "API response: $response_body (status: $status_code)"
    return 1
  fi
}

# Test Python session creation from token
test_python_session_creation() {
  echo "Testing Python session creation..."
  
  if [[ -z "$TEST_TOKEN" ]]; then
    echo "Error: No token available for session creation"
    return 1
  fi
  
  # Use Python script to create session
  if python "$TEST_PYTHON_SCRIPT" create-session "$TEST_TOKEN" --output="$TEST_SESSION_FILE"; then
    echo "Python session created successfully"
    
    # Verify session file exists and contains token
    if [[ -f "$TEST_SESSION_FILE" && $(grep -q "token" "$TEST_SESSION_FILE"; echo $?) -eq 0 ]]; then
      echo "Session file created and contains token"
      return 0
    else
      echo "Error: Session file not created properly"
      return 1
    fi
  else
    echo "Error: Failed to create Python session"
    return 1
  fi
}

# Test Python API access with session
test_python_api_access() {
  echo "Testing Python API access with session..."
  
  if [[ ! -f "$TEST_SESSION_FILE" ]]; then
    echo "Error: Session file not found"
    return 1
  fi
  
  # Use Python script to test API access with session
  if python "$TEST_PYTHON_SCRIPT" test-session "$TEST_SESSION_FILE"; then
    echo "Python API access with session successful"
    return 0
  else
    echo "Error: Python API access with session failed"
    return 1
  fi
}

# Test Python direct login
test_python_direct_login() {
  echo "Testing Python direct login..."
  
  # Use Python script to login and get token
  local python_token
  python_token=$(python "$TEST_PYTHON_SCRIPT" login "$TEST_USER" "$TEST_PASS")
  
  if [[ $? -eq 0 && -n "$python_token" ]]; then
    echo "Python login successful"
    
    # Validate token with Go API
    local response
    response=$(curl -s -w "%{http_code}" -H "Authorization: Bearer $python_token" "http://localhost:$API_PORT/api/user/validate")
    
    # Extract HTTP status code (last line)
    local status_code=${response: -3}
    
    if [[ "$status_code" == "200" ]]; then
      echo "Python-obtained token validated successfully by Go API"
      return 0
    else
      echo "Error: Python-obtained token validation failed"
      return 1
    fi
  else
    echo "Error: Python login failed"
    return 1
  fi
}

# Test token revocation via CLI
test_java_cli_logout() {
  echo "Testing Java CLI logout (token revocation)..."
  
  # Use CLI to logout
  local logout_output
  logout_output=$("$CLI_PATH" logout)
  
  if [[ $? -ne 0 ]]; then
    echo "Error: Logout failed"
    echo "CLI output: $logout_output"
    return 1
  fi
  
  # Verify token is no longer valid by trying to use it
  local response
  response=$(curl -s -w "%{http_code}" -H "Authorization: Bearer $TEST_TOKEN" "http://localhost:$API_PORT/api/user/validate")
  
  # Extract HTTP status code (last line)
  local status_code=${response: -3}
  
  if [[ "$status_code" == "401" ]]; then
    echo "Token successfully revoked"
    return 0
  else
    echo "Error: Token still valid after logout"
    echo "API response status: $status_code"
    return 1
  fi
}

# Run the complete test
run_test() {
  local success=true
  
  setup
  
  # Run test steps, aborting on first failure
  if ! test_java_cli_login; then
    echo "Test failed at step: test_java_cli_login"
    success=false
  elif ! test_go_api_token_validation; then
    echo "Test failed at step: test_go_api_token_validation"
    success=false
  elif ! test_python_session_creation; then
    echo "Test failed at step: test_python_session_creation"
    success=false
  elif ! test_python_api_access; then
    echo "Test failed at step: test_python_api_access"
    success=false
  elif ! test_python_direct_login; then
    echo "Test failed at step: test_python_direct_login"
    success=false
  elif ! test_java_cli_logout; then
    echo "Test failed at step: test_java_cli_logout"
    success=false
  fi
  
  teardown
  
  if $success; then
    echo "Authentication flow test completed successfully"
    return 0
  else
    echo "Authentication flow test failed"
    return 1
  fi
}

# Run the test if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run_test
fi