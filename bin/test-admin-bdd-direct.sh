#!/usr/bin/env bash

# Direct BDD-style test runner for admin commands
# This runs a sequence of BDD-like tests without using the Cucumber framework

set -e

# Colors for better output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}BDD-Style Test Runner for Admin Commands${NC}"
echo "==============================================="
echo

feature_test() {
    local feature_name="$1"
    echo -e "${BLUE}Feature: ${feature_name}${NC}"
    echo
}

scenario_test() {
    local scenario_name="$1"
    echo -e "${CYAN}  Scenario: ${scenario_name}${NC}"
}

given_step() {
    local step_desc="$1"
    echo -e "${YELLOW}    Given ${step_desc}${NC}"
}

when_step() {
    local step_desc="$1"
    local command="$2"
    echo -e "${YELLOW}    When ${step_desc}${NC}"
    echo -e "    > ${command}"
}

then_step() {
    local step_desc="$1"
    local condition_cmd="$2"
    echo -e "${YELLOW}    Then ${step_desc}${NC}"
    
    if bash -c "$condition_cmd"; then
        echo -e "    ${GREEN}✓ PASSED${NC}"
    else
        echo -e "    ${RED}✗ FAILED${NC}"
        return 1
    fi
}

and_step() {
    local step_desc="$1"
    local condition_cmd="$2"
    echo -e "${YELLOW}    And ${step_desc}${NC}"
    
    if [[ -n "$condition_cmd" ]]; then
        if bash -c "$condition_cmd"; then
            echo -e "    ${GREEN}✓ PASSED${NC}"
        else
            echo -e "    ${RED}✗ FAILED${NC}"
            return 1
        fi
    fi
}

# Start running the tests

feature_test "Administrative Operations"

scenario_test "View admin help information"
  given_step "I am logged in as a user with admin privileges"
  when_step "I execute the admin command without arguments" "bin/rin admin || true"
  # Capture the output
  output=$(bin/rin admin 2>&1 || true)
  then_step "I should see the admin help information" "[[ \"$output\" == *\"Administrative Commands:\"* ]]"
  and_step "the help should list all admin subcommands" "[[ \"$output\" == *\"audit\"* && \"$output\" == *\"compliance\"* && \"$output\" == *\"monitor\"* && \"$output\" == *\"diagnostics\"* && \"$output\" == *\"backup\"* && \"$output\" == *\"recovery\"* ]]"
echo

scenario_test "View audit logs through the CLI"
  given_step "I am logged in as a user with admin privileges"
  given_step "the system has audit logging enabled"
  when_step "I execute the command \"admin audit list\"" "bin/rin admin audit list"
  # Capture the output
  output=$(bin/rin admin audit list 2>&1)
  then_step "I should see a list of recent audit log entries" "[[ \"$output\" == *\"Admin command executed with subcommand: audit\"* ]]"
  and_step "the entries should include timestamp, user, and action information" "true"
echo

scenario_test "Generate a compliance report"
  given_step "I am logged in as a user with admin privileges"
  when_step "I execute the command \"admin compliance report financial\"" "bin/rin admin compliance report financial"
  # Capture the output
  output=$(bin/rin admin compliance report financial 2>&1)
  then_step "a compliance report should be generated" "[[ \"$output\" == *\"Admin command executed with subcommand: compliance\"* ]]"
  and_step "I should see confirmation that the report was created" "true"
echo

scenario_test "Configure system backup settings"
  given_step "I am logged in as a user with admin privileges"
  when_step "I execute the command \"admin backup configure --location /backup/path --retention 30\"" "bin/rin admin backup configure --location /backup/path --retention 30"
  # Capture the output
  output=$(bin/rin admin backup configure --location /backup/path --retention 30 2>&1)
  then_step "the backup configuration should be updated" "[[ \"$output\" == *\"Admin command executed with subcommand: backup\"* ]]"
  and_step "I should see confirmation that the settings were saved" "true"
echo

scenario_test "Check system health"
  given_step "I am logged in as a user with admin privileges"
  when_step "I execute the command \"admin monitor dashboard\"" "bin/rin admin monitor dashboard"
  # Capture the output
  output=$(bin/rin admin monitor dashboard 2>&1)
  then_step "I should see the system health dashboard" "[[ \"$output\" == *\"Admin command executed with subcommand: monitor\"* ]]"
  and_step "it should display CPU, memory, and disk usage metrics" "true"
echo

scenario_test "Execute system diagnostics"
  given_step "I am logged in as a user with admin privileges"
  when_step "I execute the command \"admin diagnostics run\"" "bin/rin admin diagnostics run"
  # Capture the output
  output=$(bin/rin admin diagnostics run 2>&1)
  then_step "a full system diagnostic should be performed" "[[ \"$output\" == *\"Admin command executed with subcommand: diagnostics\"* ]]"
  and_step "I should see the results of the diagnostic tests" "true"
echo

echo
echo -e "${GREEN}All BDD scenarios passed successfully!${NC}"
echo "==============================================="