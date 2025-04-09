#!/bin/bash
# Security commands comprehensive test script for Rinna CLI
# Copyright (c) 2025 Eric C. Mumford (@heymumford)

# Set strict error handling
set -e

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Check if we're in the project directory
if [ ! -d "rinna-cli" ]; then
    echo -e "${RED}Error: This script must be run from the project root directory.${NC}"
    exit 1
fi

# Setup Java CLI test environment
CLASSPATH="rinna-cli/target/classes"
RIN_CLI="java -cp ${CLASSPATH} org.rinna.cli.RinnaCli"

echo -e "${YELLOW}Using direct Java execution for testing${NC}"
echo -e "Command: ${CYAN}${RIN_CLI}${NC}"
echo

function print_section() {
    echo -e "${YELLOW}===========================================${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${YELLOW}===========================================${NC}"
    echo ""
}

function run_test() {
    local test_name=$1
    local command=$2
    local expected_output=$3
    local expected_exit_code=${4:-0}
    
    echo -e "${CYAN}Test: ${test_name}${NC}"
    echo "Command: $command"
    
    # Run the command and capture output and exit code
    output=$(eval "$command" 2>&1) || true
    exit_code=$?
    
    # Check exit code
    if [ $exit_code -eq $expected_exit_code ]; then
        echo -e "  Exit code: ${GREEN}$exit_code${NC} (expected $expected_exit_code)"
    else
        echo -e "  Exit code: ${RED}$exit_code${NC} (expected $expected_exit_code)"
        echo -e "${RED}Test failed: Unexpected exit code${NC}"
        echo -e "Output: $output"
        return 1
    fi
    
    # Check output if expected output is provided
    if [ -n "$expected_output" ]; then
        if echo "$output" | grep -q "$expected_output"; then
            echo -e "  Output: ${GREEN}Contains expected text${NC}"
        else
            echo -e "  Output: ${RED}Does not contain expected text${NC}"
            echo -e "${RED}Test failed: Unexpected output${NC}"
            echo -e "Expected to find: $expected_output"
            echo -e "Actual output: $output"
            return 1
        fi
    fi
    
    echo -e "${GREEN}Test passed${NC}"
    echo ""
    return 0
}

print_section "Running Security Command Tests"

# Test command recognition 
echo "Checking basic command recognition..."
run_test "Login command is recognized" "$RIN_CLI --help" "login"
run_test "Logout command is recognized" "$RIN_CLI --help" "logout"
run_test "Access command is recognized" "$RIN_CLI --help" "access"

# Check individual commands
echo "Testing individual security commands..."

# Test login command
run_test "Login command exists" "$RIN_CLI login" "login"

# Test logout command
run_test "Logout command exists" "$RIN_CLI logout" "not currently logged in"

# Test access command
run_test "Access command exists" "$RIN_CLI access help" "Usage: rin access"

# Run the detailed test script for thorough testing
if [ -f "bin/test/test-security-commands.sh" ]; then
    print_section "Running Detailed Security Tests"
    bash bin/test/test-security-commands.sh
else
    echo -e "${YELLOW}Detailed test script not found. Skipping detailed tests.${NC}"
fi

print_section "Security Command Tests Complete"
echo -e "${GREEN}All tests completed successfully!${NC}"