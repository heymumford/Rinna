#!/usr/bin/env bash
#
# rin-build-info.sh - Show information about the optimized build system
#
# This script provides quick help for the optimized build system
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Determine script and project directories
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
RINNA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCS_FILE="$RINNA_DIR/docs/development/optimized-build-system.md"

# Print the header
echo -e "${BLUE}${BOLD}Rinna Optimized Build System${NC}"
echo -e "${BLUE}===============================================${NC}"

# Check if documentation exists
if [[ -f "$DOCS_FILE" ]]; then
    echo -e "${GREEN}Documentation available at:${NC} docs/development/optimized-build-system.md"
else
    echo -e "${YELLOW}Documentation not found. Please run with --create-docs to generate it.${NC}"
fi

# Display quick reference
echo -e "\n${BOLD}Quick Reference:${NC}"
echo -e "
${CYAN}Standard build:${NC}
  ${YELLOW}bin/rin-build-optimize.sh${NC}

${CYAN}Quick build (skip tests):${NC}
  ${YELLOW}bin/rin-build-optimize.sh --skip-tests${NC}

${CYAN}Build specific components:${NC}
  ${YELLOW}bin/rin-build-optimize.sh --components=java${NC}

${CYAN}Run specific phases:${NC}
  ${YELLOW}bin/rin-build-optimize.sh --only=build,test${NC}

${CYAN}Run quality checks:${NC}
  ${YELLOW}bin/rin-quality-check-all.sh${NC}

${CYAN}Run specific quality tool:${NC}
  ${YELLOW}bin/rin-quality-check-all.sh checkstyle${NC}

${CYAN}Auto-fix quality issues:${NC}
  ${YELLOW}bin/rin-quality-check-all.sh --fix checkstyle${NC}
"

echo -e "${BLUE}===============================================${NC}"
echo -e "For more details, run: ${GREEN}bin/rin-build-optimize.sh --help${NC}"