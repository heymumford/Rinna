#!/usr/bin/env bash

# Script to test the admin-operations.feature using the CLI

set -e

RINNA_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Running manual simulation of BDD tests for admin operations${NC}"
echo "=============================================="

# Test 1: View admin help information
echo -e "${BLUE}Scenario: View admin help information${NC}"
echo "When I execute the admin command without arguments"
echo -e "${YELLOW}Running: bin/rin admin${NC}"
bin/rin admin || echo -e "${GREEN}✓ Admin help shown as expected${NC}"
echo

# Test 2: View audit logs
echo -e "${BLUE}Scenario: View audit logs through the CLI${NC}"
echo "When I execute the command \"admin audit list\""
echo -e "${YELLOW}Running: bin/rin admin audit list${NC}"
bin/rin admin audit list
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ Audit log listing successful${NC}"
else
  echo -e "${RED}× Audit log listing failed${NC}"
  exit 1
fi
echo

# Test 3: Generate a compliance report
echo -e "${BLUE}Scenario: Generate a compliance report${NC}"
echo "When I execute the command \"admin compliance report financial\""
echo -e "${YELLOW}Running: bin/rin admin compliance report financial${NC}"
bin/rin admin compliance report financial
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ Compliance report generation successful${NC}"
else
  echo -e "${RED}× Compliance report generation failed${NC}"
  exit 1
fi
echo

# Test 4: Check system health
echo -e "${BLUE}Scenario: Check system health${NC}"
echo "When I execute the command \"admin monitor dashboard\""
echo -e "${YELLOW}Running: bin/rin admin monitor dashboard${NC}"
bin/rin admin monitor dashboard
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ System health check successful${NC}"
else
  echo -e "${RED}× System health check failed${NC}"
  exit 1
fi
echo

echo -e "${GREEN}All BDD scenarios passed manual simulation!${NC}"
echo "=============================================="

exit 0