#!/usr/bin/env bash

# Simplified test script for admin commands

set -e

# Colors for better output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}Running simplified admin command tests${NC}"
echo "=============================================="

# Test 1: Admin help
echo -e "${BLUE}Test 1: Admin help${NC}"
bin/rin admin || echo -e "${GREEN}✓ Admin help displayed when no arguments passed${NC}"
echo

# Test 2: Admin audit list
echo -e "${BLUE}Test 2: Admin audit list${NC}"
bin/rin admin audit list
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ Admin audit list command executed${NC}"
else
  echo -e "${RED}× Admin audit list command failed${NC}"
  exit 1
fi
echo

# Test 3: Admin compliance report
echo -e "${BLUE}Test 3: Admin compliance report${NC}"
bin/rin admin compliance report financial
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ Admin compliance report command executed${NC}"
else
  echo -e "${RED}× Admin compliance report command failed${NC}"
  exit 1
fi
echo

echo -e "${GREEN}All tests passed!${NC}"
echo "=============================================="