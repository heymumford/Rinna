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

# Test Go logger
echo "=== Testing Go Logger ==="
bin/rinna-logger --level INFO --name "test_go" --message "This is a test message from Go logger" --field "source=test_script"
echo ""

# Test Python logger
echo "=== Testing Python Logger ==="
python3 bin/log_python.py --level INFO --name "test_python" --message "This is a test message from Python logger" --field "source=test_script"
echo ""

# Test Bash logger
echo "=== Testing Bash Logger ==="
bash bin/log_bash.sh --level INFO --name "test_bash" --message "This is a test message from Bash logger" --field "source=test_script"
echo ""

echo "=== All tests completed successfully! ==="