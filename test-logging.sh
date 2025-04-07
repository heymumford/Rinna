#!/usr/bin/env bash
#
# Test script for the multi-language logging system
#
# This script tests all components of the multi-language logging system:
# - Java logging via SLF4J
# - Bash logging via Bash script
# - Python logging via Python script
# - Go logging via Go executable
#

set -e

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Ensure log directory exists
LOG_DIR="${RINNA_LOG_DIR:-$HOME/.rinna/logs}"
mkdir -p "$LOG_DIR"

# Test Go logger - basic use
echo "=== Testing Go Logger (Basic) ==="
bin/rinna-logger --level INFO --name "test_go" --message "This is a test message from Go logger" --field "source=test_script"

# Test Go logger - field validation
echo -e "\n=== Testing Go Logger (Field Validation) ==="
bin/rinna-logger --level INFO --name "test_go" --message "Testing field validation" --field "invalid-key=value" --field "=empty_key" --field "spaces in key=value"

# Test Python logger - basic use
echo -e "\n=== Testing Python Logger (Basic) ==="
python3 bin/log_python.py --level INFO --name "test_python" --message "This is a test message from Python logger" --field "source=test_script"

# Test Python logger - field validation
echo -e "\n=== Testing Python Logger (Field Validation) ==="
python3 bin/log_python.py --level INFO --name "test_python" --message "Testing field validation" --field "invalid-key=value" --field "=empty_key" --field "spaces in key=value"

# Test Bash logger - basic use
echo -e "\n=== Testing Bash Logger (Basic) ==="
bash bin/log_bash.sh --level INFO --name "test_bash" --message "This is a test message from Bash logger" --field "source=test_script"

# Test Bash logger - field validation
echo -e "\n=== Testing Bash Logger (Field Validation) ==="
bash bin/log_bash.sh --level INFO --name "test_bash" --message "Testing field validation" --field "invalid-key=value" --field "=empty_key" --field "spaces in key=value"

# Test cross-language consistency
echo -e "\n=== Testing Cross-Language Consistency ==="
message="Same message in all languages"
fields="request_id=abc-123 user_id=user-456"

bin/rinna-logger --level INFO --name "cross_test" --message "$message" --field "request_id=abc-123" --field "user_id=user-456"
python3 bin/log_python.py --level INFO --name "cross_test" --message "$message" --field "request_id=abc-123" --field "user_id=user-456"
bash bin/log_bash.sh --level INFO --name "cross_test" --message "$message" --field "request_id=abc-123" --field "user_id=user-456"

echo -e "\n=== All tests completed successfully! ==="
echo "Log files can be found in: $LOG_DIR"