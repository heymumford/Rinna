#!/usr/bin/env bash

#
# run-quality-checks.sh - Run quality checks on the Rinna codebase
#
# PURPOSE: Run quality checks with appropriate thresholds for local development
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# 
# Developed with analytical assistance from AI tools.
# All rights reserved.
# 
# This source code is licensed under the MIT License
# found in the LICENSE file in the root directory of this source tree.
#

set -e

# Determine script and project directories
# Resolve symlinks to find the actual script directory
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
RINNA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print error and exit
error() {
  echo -e "${RED}Error: $1${NC}" >&2
  exit 1
}

help() {
  cat << EOF
${BLUE}run-quality-checks.sh${NC} - Run quality checks on the Rinna codebase

Usage: run-quality-checks.sh [options]

Options:
  --local        Run with local thresholds (default)
  --ci           Run with CI thresholds
  --skipOwasp    Skip OWASP security checks
  --owasp-async  Run OWASP security checks asynchronously
  --help         Show this help message

Examples:
  ./bin/run-quality-checks.sh            # Run with local thresholds
  ./bin/run-quality-checks.sh --ci       # Run with CI thresholds
  ./bin/run-quality-checks.sh --owasp-async # Run OWASP asynchronously
EOF
}

# Default settings
RUN_MODE="local"
SKIP_OWASP=true
OWASP_ASYNC=false

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --local)
      RUN_MODE="local"
      shift
      ;;
    --ci)
      RUN_MODE="ci"
      SKIP_OWASP=false
      shift
      ;;
    --skipOwasp)
      SKIP_OWASP=true
      shift
      ;;
    --owasp-async)
      SKIP_OWASP=false
      OWASP_ASYNC=true
      shift
      ;;
    --help)
      help
      exit 0
      ;;
    *)
      error "Unknown option: $1. Use --help for usage information."
      ;;
  esac
done

cd "$RINNA_DIR" || error "Failed to change to the Rinna directory."

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}         Running Quality Checks (${RUN_MODE} mode)          ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Run OWASP asynchronously if requested
if [[ "$OWASP_ASYNC" == "true" ]]; then
  echo -e "${YELLOW}Starting OWASP dependency check asynchronously...${NC}"
  mvn org.owasp:dependency-check-maven:check -Ddependency-check.skip=false \
      -Ddependency-check.failBuildOnCVSS=8 -Ddependency-check.nvdApiDelay=1000 \
      -Ddependency-check.format=HTML,JSON > dependency-check.log 2>&1 &
  
  OWASP_PID=$!
  echo -e "${YELLOW}OWASP scan started with PID: ${OWASP_PID}${NC}"
  echo -e "${YELLOW}Results will be available in dependency-check.log${NC}"
fi

# Quality checks for local or CI mode
if [[ "$RUN_MODE" == "local" ]]; then
  echo -e "${BLUE}Running local quality checks...${NC}"
  # Skip tests to focus on style and static analysis
  mvn clean validate compile -P local-quality -DskipTests=true
  
  # Check for dependency convergence failures separately
  echo -e "${BLUE}Checking dependency convergence...${NC}"
  mvn org.apache.maven.plugins:maven-enforcer-plugin:3.4.1:enforce -P local-quality -Drules=dependencyConvergence
elif [[ "$RUN_MODE" == "ci" ]]; then
  echo -e "${BLUE}Running CI quality checks...${NC}"
  
  if [[ "$SKIP_OWASP" == "true" && "$OWASP_ASYNC" == "false" ]]; then
    # Run without OWASP for faster CI checks
    echo -e "${YELLOW}Skipping OWASP security scan${NC}"
    mvn clean validate compile -P ci -Ddependency-check.skip=true -DskipTests=true
  else
    # Full CI quality checks including OWASP
    echo -e "${BLUE}Running full quality checks including OWASP security scan...${NC}"
    mvn clean validate compile -P ci -DskipTests=true
  fi
  
  # Always check dependency convergence in CI mode
  echo -e "${BLUE}Checking dependency convergence...${NC}"
  mvn org.apache.maven.plugins:maven-enforcer-plugin:3.4.1:enforce -P ci -Drules=dependencyConvergence
fi

# Check if we should wait for async OWASP scan
if [[ "$OWASP_ASYNC" == "true" ]]; then
  echo -e "${YELLOW}OWASP scan running in background (PID: ${OWASP_PID})${NC}"
  echo -e "${YELLOW}You can check the progress with: tail -f dependency-check.log${NC}"
  echo -e "${YELLOW}Or wait for completion with: wait ${OWASP_PID}${NC}"
fi

echo -e "${GREEN}Quality checks completed!${NC}"