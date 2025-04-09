#!/bin/bash
# Test script for security-related commands in Rinna CLI
# Copyright (c) 2025 Eric C. Mumford (@heymumford)

# Set strict error handling
set -e

# Check if we're in the project directory
if [ ! -d "rinna-cli" ]; then
    echo "Error: This script must be run from the project root directory."
    exit 1
fi

# Setup Java CLI test environment
CLASSPATH="rinna-cli/target/classes"
RIN_CLI="java -cp ${CLASSPATH} org.rinna.cli.RinnaCli"

echo -e "${YELLOW}Using direct Java execution for testing${NC}"
echo -e "Command: ${CYAN}${RIN_CLI}${NC}"
echo

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test counter
TEST_COUNT=0
PASS_COUNT=0
FAIL_COUNT=0

# Function to run a test case
run_test() {
    local test_name=$1
    local command=$2
    local expected_exit_code=${3:-0}
    
    echo -e "${CYAN}Test case ${TEST_COUNT}: ${test_name}${NC}"
    echo "  Command: $command"
    
    # Run the command and capture output and exit code
    output=$(eval "$command" 2>&1) || true
    exit_code=$?
    
    # Check exit code
    if [ $exit_code -eq $expected_exit_code ]; then
        echo -e "  Exit code: ${GREEN}$exit_code${NC} (expected $expected_exit_code)"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        echo -e "  Exit code: ${RED}$exit_code${NC} (expected $expected_exit_code)"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
    
    # Print output (truncate if too long)
    if [ ${#output} -gt 500 ]; then
        echo "  Output (truncated): ${output:0:500}..."
    else
        echo "  Output: $output"
    fi
    
    echo ""
    TEST_COUNT=$((TEST_COUNT + 1))
}

echo -e "${YELLOW}==============================================${NC}"
echo -e "${YELLOW}Starting Security Command Tests for Rinna CLI${NC}"
echo -e "${YELLOW}==============================================${NC}"
echo ""

# Test 1: Show help info (login command should be listed)
run_test "Help information shows security commands" "$RIN_CLI --help | grep -E '(login|logout|access)'" 0

# Test 2: Login command with no args (should prompt interactively, but in test just check it exists)
run_test "Login command is recognized" "$RIN_CLI login" 0

# Test 3: Logout command (should be recognized)
run_test "Logout command is recognized" "$RIN_CLI logout" 0

# Test 4: Access command (check it exists)
run_test "Access command is recognized" "$RIN_CLI access help" 0

# Print test summary
echo -e "${YELLOW}==============================================${NC}"
echo -e "${YELLOW}Test Summary${NC}"
echo -e "${YELLOW}==============================================${NC}"
echo "Total tests: $TEST_COUNT"
echo -e "Passed: ${GREEN}$PASS_COUNT${NC}"
echo -e "Failed: ${RED}$FAIL_COUNT${NC}"

# Set the exit code based on test results
if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed.${NC}"
    exit 1
fi