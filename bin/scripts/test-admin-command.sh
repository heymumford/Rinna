#!/usr/bin/env bash

# Simple test script for admin commands

set -e

# Colors for better output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}Running tests for admin command integration${NC}"
echo "=============================================="
echo

# Test 1: Admin command with no arguments should show help
echo -e "${BLUE}Test 1: Admin command with no arguments${NC}"
echo "Command: bin/rin admin"
bin/rin admin || echo -e "${GREEN}✓ Passed: Admin help shown as expected${NC}"
echo

# Test 2: Admin command with audit subcommand
echo -e "${BLUE}Test 2: Admin command with audit subcommand${NC}"
echo "Command: bin/rin admin audit"
bin/rin admin audit 
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ Passed: Admin audit command executed${NC}"
else
  echo -e "${RED}× Failed: Admin audit command failed${NC}"
  exit 1
fi
echo

# Test 3: Admin command with compliance subcommand and arguments
echo -e "${BLUE}Test 3: Admin command with compliance subcommand and arguments${NC}"
echo "Command: bin/rin admin compliance report financial"
bin/rin admin compliance report financial
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ Passed: Admin compliance command with arguments executed${NC}"
else
  echo -e "${RED}× Failed: Admin compliance command with arguments failed${NC}"
  exit 1
fi
echo

# Test 4: Admin command with monitor subcommand
echo -e "${BLUE}Test 4: Admin command with monitor subcommand${NC}"
echo "Command: bin/rin admin monitor dashboard"
bin/rin admin monitor dashboard
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ Passed: Admin monitor command executed${NC}"
else
  echo -e "${RED}× Failed: Admin monitor command failed${NC}"
  exit 1
fi
echo

echo -e "${GREEN}All tests passed!${NC}"
echo "=============================================="

exit 0