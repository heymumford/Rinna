#!/bin/bash
#
# security_validation_test.sh - Cross-language security validation testing
#
# This test verifies security controls and validations work properly across
# Java, Go, and Python components, ensuring consistent enforcement of security
# policies across language boundaries.
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
PYTHON_SECURITY_SCRIPT="${TEST_TEMP_DIR}/security_tester.py"
TEST_USER="securitytest"
TEST_PASS="Security@Test123"
TEST_TOKEN=""
TOKEN_FILE="${TEST_TEMP_DIR}/security_token.txt"
RESULTS_FILE="${TEST_TEMP_DIR}/security_results.json"

# Test setup
setup() {
  echo "Setting up security validation test..."
  
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
  
  # Create Python security testing script
  create_python_security_script
  
  # Create test user for security testing (if needed)
  # This assumes there's an admin capability to create users
  # Replace with actual user creation process for your application
  if "$CLI_PATH" access list-users 2>/dev/null | grep -q "$TEST_USER"; then
    echo "Test user already exists: $TEST_USER"
  else
    echo "Creating test user: $TEST_USER"
    "$CLI_PATH" access create-user --username="$TEST_USER" --password="$TEST_PASS" --permission=BASIC_USER || true
  fi
  
  echo "Setup complete"
}

# Create Python security testing script
create_python_security_script() {
  cat > "$PYTHON_SECURITY_SCRIPT" <<EOF
#!/usr/bin/env python3
"""
Security testing tool for cross-language validation.

This script provides security testing capabilities across language boundaries
to ensure consistent security enforcement in the Rinna system.
"""

import os
import sys
import json
import uuid
import argparse
import time
import logging
import requests
import subprocess
from datetime import datetime, timedelta

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('security-tester')

# Configuration from environment
API_PORT = os.environ.get("RINNA_TEST_API_PORT", "8085")
API_URL = f"http://localhost:{API_PORT}/api"
TEST_TEMP_DIR = os.environ.get("RINNA_TEST_TEMP_DIR", "./target/cross-language-tests")
CLI_PATH = "${CLI_PATH}"
RESULTS_FILE = "${RESULTS_FILE}"

# Security test results tracker
class SecurityTestResults:
    def __init__(self):
        self.tests = []
        self.passed = 0
        self.failed = 0
        self.warnings = 0
        
    def add_result(self, test_name, result, message, details=None):
        """Add a test result."""
        if details is None:
            details = {}
            
        status = "PASS" if result else "FAIL"
        self.tests.append({
            "name": test_name,
            "status": status,
            "timestamp": datetime.now().isoformat(),
            "message": message,
            "details": details
        })
        
        if result:
            self.passed += 1
        else:
            self.failed += 1
    
    def add_warning(self, test_name, message, details=None):
        """Add a test warning."""
        if details is None:
            details = {}
            
        self.tests.append({
            "name": test_name,
            "status": "WARNING",
            "timestamp": datetime.now().isoformat(),
            "message": message,
            "details": details
        })
        self.warnings += 1
    
    def save(self, filename):
        """Save results to file."""
        with open(filename, 'w') as f:
            json.dump({
                "summary": {
                    "total": len(self.tests),
                    "passed": self.passed,
                    "failed": self.failed,
                    "warnings": self.warnings,
                    "timestamp": datetime.now().isoformat()
                },
                "tests": self.tests
            }, f, indent=2)
        logger.info(f"Results saved to {filename}")

# Security test functions
def test_authentication_token_validation(results):
    """Test authentication token validation across language boundaries."""
    logger.info("Testing authentication token validation")
    
    # Create token via CLI (Java)
    try:
        # Get token from file or login
        token_file = "${TOKEN_FILE}"
        if os.path.exists(token_file):
            with open(token_file, 'r') as f:
                token = f.read().strip()
                logger.info("Using existing token from file")
        else:
            result = subprocess.run(
                [CLI_PATH, "login", "${TEST_USER}", "--password=${TEST_PASS}", "--output=token"],
                capture_output=True,
                text=True
            )
            if result.returncode != 0:
                results.add_result(
                    "authentication_token_creation",
                    False,
                    "Failed to create authentication token via CLI",
                    {"error": result.stderr}
                )
                return
            
            token = result.stdout.strip()
            
            # Save token for later tests
            with open(token_file, 'w') as f:
                f.write(token)
            
            logger.info("Created new token via CLI login")
        
        # Test if token is valid via Go API
        headers = {"Authorization": f"Bearer {token}"}
        response = requests.get(f"{API_URL}/user/validate", headers=headers)
        
        if response.status_code == 200:
            results.add_result(
                "authentication_token_validation_go",
                True,
                "Go API correctly validates Java-created token",
                {"token_prefix": token[:8] + "..."}
            )
        else:
            results.add_result(
                "authentication_token_validation_go",
                False,
                "Go API failed to validate Java-created token",
                {
                    "status_code": response.status_code,
                    "response": response.text,
                    "token_prefix": token[:8] + "..."
                }
            )
            return
            
        # Test token expiration validation
        # Create a manipulated token with a past expiration
        if "." in token:
            try:
                # Simple JWT structure manipulation
                parts = token.split('.')
                if len(parts) == 3:  # header.payload.signature
                    import base64
                    import json
                    
                    # Decode payload
                    padding = '=' * (4 - len(parts[1]) % 4)
                    payload = json.loads(base64.b64decode(parts[1] + padding).decode('utf-8'))
                    
                    # Change expiration to past time
                    if 'exp' in payload:
                        payload['exp'] = int(time.time()) - 3600  # 1 hour ago
                        
                        # Encode modified payload (without proper signature)
                        modified_payload = base64.b64encode(json.dumps(payload).encode('utf-8')).decode('utf-8').rstrip('=')
                        expired_token = f"{parts[0]}.{modified_payload}.{parts[2]}"
                        
                        # Test expired token
                        headers = {"Authorization": f"Bearer {expired_token}"}
                        response = requests.get(f"{API_URL}/user/validate", headers=headers)
                        
                        if response.status_code == 401:
                            results.add_result(
                                "expired_token_validation",
                                True,
                                "Go API correctly rejects expired token",
                                {"status_code": response.status_code}
                            )
                        else:
                            results.add_result(
                                "expired_token_validation",
                                False,
                                "Go API failed to reject expired token",
                                {"status_code": response.status_code}
                            )
            except Exception as e:
                results.add_warning(
                    "expired_token_validation",
                    "Unable to test expired token validation",
                    {"error": str(e)}
                )
        
        return token
        
    except Exception as e:
        results.add_result(
            "authentication_token_validation",
            False,
            "Error during token validation test",
            {"error": str(e)}
        )
        return None

def test_input_validation(results, token=None):
    """Test input validation across language boundaries."""
    logger.info("Testing input validation")
    
    # Test SQL injection protection in Go API
    test_sql_injection(results, token)
    
    # Test XSS validation in Java and Go
    test_xss_validation(results, token)
    
    # Test command injection protection
    test_command_injection(results, token)

def test_sql_injection(results, token=None):
    """Test SQL injection protection."""
    # SQL injection test patterns
    injection_patterns = [
        "' OR '1'='1",
        "'; DROP TABLE users; --",
        "' UNION SELECT * FROM users WHERE '1'='1",
        "') OR 1=1--",
        "admin'--"
    ]
    
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    # Test against Go API search endpoint
    for pattern in injection_patterns:
        try:
            # Test in URL parameters
            response = requests.get(f"{API_URL}/workitems?title={pattern}", headers=headers)
            
            # Check if response indicates successful injection (differs by system)
            # For this test, we just verify we don't get a 500 error which might indicate SQL issues
            if response.status_code >= 500:
                results.add_result(
                    f"sql_injection_url_protection_{pattern[:10]}",
                    False,
                    f"Possible SQL injection vulnerability in URL parameters",
                    {"pattern": pattern, "status_code": response.status_code}
                )
            else:
                results.add_result(
                    f"sql_injection_url_protection_{pattern[:10]}",
                    True,
                    f"URL parameter protected against SQL injection",
                    {"pattern": pattern, "status_code": response.status_code}
                )
                
            # Test in JSON body
            payload = {
                "title": f"Test Item {uuid.uuid4()}",
                "description": pattern,
                "type": "TASK"
            }
            
            response = requests.post(
                f"{API_URL}/workitems", 
                json=payload,
                headers=headers
            )
            
            # Check if response indicates successful injection
            if response.status_code >= 500:
                results.add_result(
                    f"sql_injection_json_protection_{pattern[:10]}",
                    False,
                    f"Possible SQL injection vulnerability in JSON body",
                    {"pattern": pattern, "status_code": response.status_code}
                )
            else:
                results.add_result(
                    f"sql_injection_json_protection_{pattern[:10]}",
                    True,
                    f"JSON body protected against SQL injection",
                    {"pattern": pattern, "status_code": response.status_code}
                )
                
        except Exception as e:
            results.add_warning(
                f"sql_injection_test_{pattern[:10]}",
                f"Error testing SQL injection pattern",
                {"pattern": pattern, "error": str(e)}
            )

def test_xss_validation(results, token=None):
    """Test XSS validation."""
    # XSS test patterns
    xss_patterns = [
        "<script>alert('XSS')</script>",
        "<img src='x' onerror='alert(\"XSS\")'>",
        "<svg onload='alert(\"XSS\")'>",
        "javascript:alert('XSS')",
        "data:text/html;base64,PHNjcmlwdD5hbGVydCgnWFNTJyk7PC9zY3JpcHQ+"
    ]
    
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    # Test against API endpoints
    for pattern in xss_patterns:
        try:
            # Create a work item with XSS payload
            payload = {
                "title": f"XSS Test {uuid.uuid4()}",
                "description": pattern,
                "type": "TASK"
            }
            
            response = requests.post(
                f"{API_URL}/workitems", 
                json=payload,
                headers=headers
            )
            
            # Check if we can create the item
            if response.status_code in (200, 201):
                # If created, get it back and check if XSS payload was sanitized
                item_id = response.json().get("id")
                
                if item_id:
                    # Get the item back
                    response = requests.get(f"{API_URL}/workitems/{item_id}", headers=headers)
                    
                    if response.status_code == 200:
                        item = response.json()
                        description = item.get("description", "")
                        
                        # Check if the XSS payload was sanitized
                        if pattern == description:
                            results.add_result(
                                f"xss_validation_{pattern[:10]}",
                                False,
                                f"XSS payload was not sanitized",
                                {"original": pattern, "stored": description}
                            )
                        else:
                            results.add_result(
                                f"xss_validation_{pattern[:10]}",
                                True,
                                f"XSS payload was properly sanitized or escaped",
                                {"original": pattern, "stored": description}
                            )
                        
                        # Clean up the test item
                        requests.delete(f"{API_URL}/workitems/{item_id}", headers=headers)
            
        except Exception as e:
            results.add_warning(
                f"xss_validation_{pattern[:10]}",
                f"Error testing XSS pattern",
                {"pattern": pattern, "error": str(e)}
            )

def test_command_injection(results, token=None):
    """Test command injection protection in CLI commands."""
    # Command injection patterns
    cmd_patterns = [
        "; ls -la",
        "& cat /etc/passwd",
        "| echo 'Injected'",
        "> /tmp/test",
        "; rm -rf ./",
        "\\\$(id)"
    ]
    
    for pattern in cmd_patterns:
        try:
            # Try command injection in CLI arguments
            result = subprocess.run(
                [CLI_PATH, "list", f"--filter={pattern}"],
                capture_output=True,
                text=True
            )
            
            # Check for command execution signs in output
            if "root:" in result.stdout or "Injected" in result.stdout:
                results.add_result(
                    f"command_injection_{pattern[:10]}",
                    False,
                    f"Possible command injection vulnerability in CLI",
                    {"pattern": pattern, "stdout": result.stdout[:500]}
                )
            else:
                results.add_result(
                    f"command_injection_{pattern[:10]}",
                    True,
                    f"CLI protected against command injection",
                    {"pattern": pattern}
                )
                
        except Exception as e:
            results.add_warning(
                f"command_injection_{pattern[:10]}",
                f"Error testing command injection pattern",
                {"pattern": pattern, "error": str(e)}
            )

def test_authorization_controls(results, token=None):
    """Test authorization controls across language boundaries."""
    logger.info("Testing authorization controls")
    
    if not token:
        results.add_warning(
            "authorization_controls",
            "No token available for authorization tests",
            {}
        )
        return
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test access to admin endpoints from non-admin user
    admin_endpoints = [
        "/api/admin/users",
        "/api/admin/system/status",
        "/api/admin/settings"
    ]
    
    for endpoint in admin_endpoints:
        try:
            response = requests.get(f"http://localhost:{API_PORT}{endpoint}", headers=headers)
            
            if response.status_code in (401, 403):
                results.add_result(
                    f"authorization_admin_protection_{endpoint}",
                    True,
                    f"Go API correctly denied access to admin endpoint",
                    {"endpoint": endpoint, "status_code": response.status_code}
                )
            else:
                results.add_result(
                    f"authorization_admin_protection_{endpoint}",
                    False,
                    f"Go API failed to protect admin endpoint",
                    {"endpoint": endpoint, "status_code": response.status_code}
                )
                
        except Exception as e:
            results.add_warning(
                f"authorization_admin_protection_{endpoint}",
                f"Error testing admin endpoint protection",
                {"endpoint": endpoint, "error": str(e)}
            )
    
    # Test Java CLI authorization controls
    try:
        # Try admin command with non-admin user
        result = subprocess.run(
            [CLI_PATH, "admin", "status"],
            capture_output=True,
            text=True
        )
        
        if result.returncode != 0 and ("permission" in result.stderr.lower() or 
                                     "denied" in result.stderr.lower() or
                                     "unauthorized" in result.stderr.lower()):
            results.add_result(
                "authorization_cli_admin_protection",
                True,
                "Java CLI correctly denied access to admin command",
                {"stderr": result.stderr}
            )
        else:
            results.add_result(
                "authorization_cli_admin_protection",
                False,
                "Java CLI failed to protect admin command",
                {"returncode": result.returncode, "stderr": result.stderr, "stdout": result.stdout}
            )
            
    except Exception as e:
        results.add_warning(
            "authorization_cli_admin_protection",
            "Error testing CLI admin command protection",
            {"error": str(e)}
        )

def test_data_validation(results, token=None):
    """Test data validation consistency across languages."""
    logger.info("Testing data validation consistency")
    
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    # Test boundary values
    test_data = [
        # Empty values
        {"case": "empty_title", "payload": {"title": "", "type": "TASK", "description": "Test"}},
        {"case": "null_type", "payload": {"title": "Test", "type": None, "description": "Test"}},
        
        # Too long values
        {"case": "title_too_long", "payload": {"title": "x" * 1000, "type": "TASK", "description": "Test"}},
        {"case": "description_too_long", "payload": {"title": "Test", "type": "TASK", "description": "x" * 10000}},
        
        # Invalid enum values
        {"case": "invalid_type", "payload": {"title": "Test", "type": "INVALID_TYPE", "description": "Test"}},
        {"case": "invalid_priority", "payload": {"title": "Test", "type": "TASK", "priority": "INVALID_PRIORITY", "description": "Test"}},
        
        # Invalid data types
        {"case": "numeric_title", "payload": {"title": 12345, "type": "TASK", "description": "Test"}},
        {"case": "array_description", "payload": {"title": "Test", "type": "TASK", "description": ["Item 1", "Item 2"]}},
        
        # Special characters
        {"case": "unicode_title", "payload": {"title": "Test 💻🌍", "type": "TASK", "description": "Test"}},
        {"case": "html_description", "payload": {"title": "Test", "type": "TASK", "description": "<p>HTML content</p>"}}
    ]
    
    for test in test_data:
        try:
            # Test via Go API
            response = requests.post(
                f"{API_URL}/workitems", 
                json=test["payload"],
                headers=headers
            )
            
            go_validation = {
                "status_code": response.status_code,
                "success": response.status_code in (200, 201),
                "response": response.text[:500]
            }
            
            # Test via Java CLI
            # Write payload to temp file for CLI to read
            payload_file = os.path.join(TEST_TEMP_DIR, f"payload_{test['case']}.json")
            with open(payload_file, 'w') as f:
                json.dump(test["payload"], f)
            
            result = subprocess.run(
                [CLI_PATH, "add", "--file", payload_file, "--output=json"],
                capture_output=True,
                text=True
            )
            
            java_validation = {
                "returncode": result.returncode,
                "success": result.returncode == 0,
                "stdout": result.stdout[:500],
                "stderr": result.stderr[:500]
            }
            
            # Compare validation results for consistency
            if go_validation["success"] == java_validation["success"]:
                results.add_result(
                    f"data_validation_{test['case']}",
                    True,
                    f"Consistent data validation across Go and Java",
                    {
                        "case": test["case"],
                        "go_validation": go_validation,
                        "java_validation": java_validation
                    }
                )
            else:
                results.add_result(
                    f"data_validation_{test['case']}",
                    False,
                    f"Inconsistent data validation across Go and Java",
                    {
                        "case": test["case"],
                        "go_validation": go_validation,
                        "java_validation": java_validation
                    }
                )
                
            # Clean up temp file
            os.remove(payload_file)
            
        except Exception as e:
            results.add_warning(
                f"data_validation_{test['case']}",
                f"Error testing data validation",
                {"case": test["case"], "error": str(e)}
            )

def run_security_tests():
    """Run all security tests."""
    results = SecurityTestResults()
    
    try:
        # Step 1: Authentication token validation
        token = test_authentication_token_validation(results)
        
        # Step 2: Input validation tests
        test_input_validation(results, token)
        
        # Step 3: Authorization controls
        test_authorization_controls(results, token)
        
        # Step 4: Data validation consistency
        test_data_validation(results, token)
        
        # Save results
        results.save(RESULTS_FILE)
        
        # Return overall status
        if results.failed > 0:
            logger.error(f"Security validation failed: {results.failed} tests failed")
            return 1
        else:
            logger.info(f"Security validation passed: {results.passed} tests passed, {results.warnings} warnings")
            return 0
    
    except Exception as e:
        logger.error(f"Error running security tests: {str(e)}")
        results.add_result(
            "overall_execution",
            False,
            "Error running security tests",
            {"error": str(e)}
        )
        results.save(RESULTS_FILE)
        return 1

def main():
    """Main function."""
    parser = argparse.ArgumentParser(description="Security validation for cross-language testing")
    parser.add_argument("--token-file", default=None, help="Path to token file for reusing existing token")
    parser.add_argument("--results-file", default=None, help="Path to results file")
    
    args = parser.parse_args()
    
    return run_security_tests()

if __name__ == "__main__":
    sys.exit(main())
EOF

  chmod +x "$PYTHON_SECURITY_SCRIPT"
}

# Test teardown
teardown() {
  echo "Tearing down security validation test..."
  
  # Clean up token file
  rm -f "$TOKEN_FILE" 2>/dev/null || true
  
  # Logout if we're logged in
  "$CLI_PATH" logout > /dev/null 2>&1 || true
  
  echo "Teardown complete"
}

# Execute security tests
test_security_validation() {
  echo "Running security validation tests..."
  
  # Run Python security test script
  if python "$PYTHON_SECURITY_SCRIPT"; then
    echo "Security validation passed"
    return 0
  else
    echo "Security validation failed"
    
    # Show summary of failures if available
    if [[ -f "$RESULTS_FILE" ]]; then
      echo "Security test failures:"
      python -c "import json; data = json.load(open('$RESULTS_FILE')); [print(f' - {t[\"name\"]}: {t[\"message\"]}') for t in data['tests'] if t['status'] == 'FAIL']"
    fi
    
    return 1
  fi
}

# Check consistency of security controls
test_security_consistency() {
  echo "Checking consistency of security controls..."
  
  # Parse the results file for consistency checks
  if [[ -f "$RESULTS_FILE" ]]; then
    # Count consistent vs inconsistent validations
    local consistent=$(python -c "import json; data = json.load(open('$RESULTS_FILE')); print(sum(1 for t in data['tests'] if t['name'].startswith('data_validation_') and t['status'] == 'PASS'))")
    local inconsistent=$(python -c "import json; data = json.load(open('$RESULTS_FILE')); print(sum(1 for t in data['tests'] if t['name'].startswith('data_validation_') and t['status'] == 'FAIL'))")
    
    echo "Data validation consistency: $consistent consistent, $inconsistent inconsistent"
    
    if (( inconsistent > 0 )); then
      echo "Warning: Inconsistent data validation detected between language components"
      python -c "import json; data = json.load(open('$RESULTS_FILE')); [print(f' - {t[\"name\"]}: {t[\"message\"]}') for t in data['tests'] if t['name'].startswith('data_validation_') and t['status'] == 'FAIL']"
      
      # Only warn, don't fail the test
      return 0
    else
      echo "All data validation checks are consistent across languages"
      return 0
    fi
  else
    echo "Error: Results file not found"
    return 1
  fi
}

# Generate security report
generate_security_report() {
  echo "Generating security report..."
  
  # Create report file
  local report_file="${TEST_TEMP_DIR}/security_report.md"
  
  # Check if results file exists
  if [[ ! -f "$RESULTS_FILE" ]]; then
    echo "Error: Results file not found"
    return 1
  fi
  
  # Generate report using Python
  python -c "
import json
import sys
from datetime import datetime

# Load results
with open('$RESULTS_FILE', 'r') as f:
    data = json.load(f)

summary = data['summary']
tests = data['tests']

# Generate report
with open('${report_file}', 'w') as f:
    f.write('# Cross-Language Security Validation Report\n\n')
    f.write(f'**Generated:** {datetime.now().strftime(\"%Y-%m-%d %H:%M:%S\")}\n\n')
    
    f.write('## Summary\n\n')
    f.write(f'- **Total Tests:** {summary[\"total\"]}\n')
    f.write(f'- **Passed:** {summary[\"passed\"]}\n')
    f.write(f'- **Failed:** {summary[\"failed\"]}\n')
    f.write(f'- **Warnings:** {summary[\"warnings\"]}\n\n')
    
    # Calculate pass rate
    if summary['total'] > 0:
        pass_rate = summary['passed'] / summary['total'] * 100
        f.write(f'- **Pass Rate:** {pass_rate:.1f}%\n\n')
    
    # Security Risk Assessment
    f.write('## Security Risk Assessment\n\n')
    
    if summary['failed'] == 0:
        f.write('🟢 **LOW RISK** - No security validation failures detected.\n\n')
    elif summary['failed'] <= 2:
        f.write('🟡 **MEDIUM RISK** - Minor security validation issues detected.\n\n')
    else:
        f.write('🔴 **HIGH RISK** - Multiple security validation failures detected.\n\n')
    
    # Group tests by category
    categories = {}
    for test in tests:
        # Extract category from name (before first underscore)
        if '_' in test['name']:
            category = test['name'].split('_')[0]
        else:
            category = 'other'
        
        if category not in categories:
            categories[category] = []
        
        categories[category].append(test)
    
    # Write test results by category
    f.write('## Test Results by Category\n\n')
    
    for category, cat_tests in categories.items():
        pass_count = sum(1 for t in cat_tests if t['status'] == 'PASS')
        fail_count = sum(1 for t in cat_tests if t['status'] == 'FAIL')
        warn_count = sum(1 for t in cat_tests if t['status'] == 'WARNING')
        
        if pass_count == len(cat_tests):
            status = '✅'
        elif fail_count > 0:
            status = '❌'
        else:
            status = '⚠️'
        
        f.write(f'### {status} {category.capitalize()}\n\n')
        f.write(f'- Passed: {pass_count}\n')
        f.write(f'- Failed: {fail_count}\n')
        f.write(f'- Warnings: {warn_count}\n\n')
        
        # Write failures and warnings
        failures = [t for t in cat_tests if t['status'] == 'FAIL']
        if failures:
            f.write('#### Failures\n\n')
            for test in failures:
                f.write(f'- **{test[\"name\"]}**: {test[\"message\"]}\n')
            f.write('\n')
        
        warnings = [t for t in cat_tests if t['status'] == 'WARNING']
        if warnings:
            f.write('#### Warnings\n\n')
            for test in warnings:
                f.write(f'- **{test[\"name\"]}**: {test[\"message\"]}\n')
            f.write('\n')
    
    # Recommendations
    f.write('## Recommendations\n\n')
    
    if summary['failed'] == 0:
        f.write('- Continue monitoring and testing security controls regularly\n')
        f.write('- Implement additional security validation tests as new features are added\n')
    else:
        f.write('- Address security validation failures as soon as possible\n')
        f.write('- Review cross-language security implementations for consistency\n')
        
        # Add specific recommendations based on failures
        auth_failures = sum(1 for t in tests if 'authentication' in t['name'] and t['status'] == 'FAIL')
        if auth_failures > 0:
            f.write('- Review authentication implementation for proper token validation across components\n')
            
        sql_failures = sum(1 for t in tests if 'sql_injection' in t['name'] and t['status'] == 'FAIL')
        if sql_failures > 0:
            f.write('- Implement parameterized queries and proper input sanitization for all database access\n')
            
        xss_failures = sum(1 for t in tests if 'xss' in t['name'] and t['status'] == 'FAIL')
        if xss_failures > 0:
            f.write('- Implement output encoding and context-specific escaping for all user-controlled data\n')
            
        cmd_failures = sum(1 for t in tests if 'command_injection' in t['name'] and t['status'] == 'FAIL')
        if cmd_failures > 0:
            f.write('- Review command execution patterns and implement strict input validation\n')
            
        auth_control_failures = sum(1 for t in tests if 'authorization' in t['name'] and t['status'] == 'FAIL')
        if auth_control_failures > 0:
            f.write('- Review authorization controls for proper enforcement across all components\n')
            
        data_validation_failures = sum(1 for t in tests if 'data_validation' in t['name'] and t['status'] == 'FAIL')
        if data_validation_failures > 0:
            f.write('- Standardize data validation rules across all components\n')
    
    f.write('\n')
    
    print(f'Security report generated: {report_file}')
" || return 1

  echo "Security report generated: $report_file"
  return 0
}

# Run the complete test
run_test() {
  local success=true
  
  setup
  
  # Run test steps, aborting on first failure
  if ! test_security_validation; then
    echo "Test failed at step: test_security_validation"
    success=false
  elif ! test_security_consistency; then
    echo "Test failed at step: test_security_consistency"
    success=false
  elif ! generate_security_report; then
    echo "Test failed at step: generate_security_report"
    success=false
  fi
  
  teardown
  
  if $success; then
    echo "Security validation test completed successfully"
    return 0
  else
    echo "Security validation test failed"
    return 1
  fi
}

# Run the test if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run_test
fi