#!/usr/bin/env bash
#
# install-python-packages.sh - Install Python packages in the correct order
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
#

set -e

# Constants
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PYTHON_DIR="$PROJECT_ROOT/python"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Formatting
BOLD='\033[1m'
NORMAL='\033[0m'

# Parse command line options
VERBOSE=0
FORCE=0
EDITABLE=1

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--verbose)
      VERBOSE=1
      shift
      ;;
    -f|--force)
      FORCE=1
      shift
      ;;
    --no-editable)
      EDITABLE=0
      shift
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

# Check if we're in a virtual environment
if [ -z "$VIRTUAL_ENV" ]; then
  echo -e "${YELLOW}Warning: Not running in a virtual environment.${NC}"
  echo -e "It's recommended to run this script from an activated virtual environment."
  echo -e "You can create one with: ${GREEN}python -m venv .venv && source .venv/bin/activate${NC}"
  echo ""
  read -p "Continue anyway? [y/N] " -n 1 -r
  echo ""
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborting."
    exit 1
  fi
fi

# Find Python executable
if command -v python3 &> /dev/null; then
  PYTHON="python3"
elif command -v python &> /dev/null; then
  PYTHON="python"
else
  echo -e "${RED}Error: Python not found. Please install Python 3.8 or later.${NC}"
  exit 1
fi

# Find pip executable
if [ -n "$VIRTUAL_ENV" ]; then
  PIP="$VIRTUAL_ENV/bin/pip"
else
  if command -v pip3 &> /dev/null; then
    PIP="pip3"
  elif command -v pip &> /dev/null; then
    PIP="pip"
  else
    echo -e "${RED}Error: pip not found. Please install pip.${NC}"
    exit 1
  fi
fi

echo -e "${BLUE}${BOLD}Installing Python packages for Rinna${NC}${NORMAL}"
echo -e "Using Python: $($PYTHON --version)"
echo -e "Using pip: $($PIP --version | cut -d' ' -f1-2)"
echo ""

# Install packages
cd "$PYTHON_DIR"

# Clean up any existing installations if force flag is set
if [ "$FORCE" -eq 1 ]; then
  echo -e "${YELLOW}Removing existing package installations...${NC}"
  rm -rf build/ dist/ *.egg-info/
  $PIP uninstall -y rinna lucidchart-py 2>/dev/null || true
fi

# Helper function for output
run_command() {
  if [ "$VERBOSE" -eq 1 ]; then
    "$@"
  else
    "$@" >/dev/null 2>&1
  fi
}

# Install the Lucidchart mock package
echo -e "${CYAN}Installing lucidchart-py package...${NC}"
if [ "$EDITABLE" -eq 1 ]; then
  SETUP_PACKAGE=lucidchart-py run_command $PIP install -e .
else
  SETUP_PACKAGE=lucidchart-py run_command $PIP install .
fi
echo -e "${GREEN}✓ Installed lucidchart-py package${NC}"

# Install the main Rinna package
echo -e "${CYAN}Installing main rinna package...${NC}"
if [ "$EDITABLE" -eq 1 ]; then
  SETUP_PACKAGE=rinna run_command $PIP install -e .
else
  SETUP_PACKAGE=rinna run_command $PIP install .
fi
echo -e "${GREEN}✓ Installed rinna package${NC}"

# Verify installations
echo -e "${CYAN}Verifying installations...${NC}"
if $PYTHON -c "import rinna; print(f'Rinna version: {rinna.__version__}')" &&
   $PYTHON -c "import lucidchart_py; print(f'Lucidchart-py version: {lucidchart_py.__version__}')"; then
  echo -e "${GREEN}✓ All packages installed successfully!${NC}"
else
  echo -e "${RED}✗ Failed to verify package installations${NC}"
  exit 1
fi

echo ""
echo -e "${BLUE}${BOLD}Installation complete!${NC}${NORMAL}"
echo "You can now import the packages in Python:"
echo "  import rinna"
echo "  import lucidchart_py"