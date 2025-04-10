#!/usr/bin/env bash
#
# checkstyle.sh - Run Checkstyle on Java code
#
# PURPOSE: Run Checkstyle independently for faster quality verification
#

set -e

# Determine script and project directories
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
RINNA_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Error handling
error() {
  echo -e "${RED}Error: $1${NC}" >&2
  exit 1
}

# Help message
help() {
  cat << EOF
${BLUE}checkstyle.sh${NC} - Run Checkstyle on Java code

Usage: checkstyle.sh [options]

Options:
  --module=<name>   Run on a specific module (rinna-cli, rinna-core, rinna-data-sqlite)
  --file=<path>     Run on a specific file (relative to module)
  --fix             Print instructions for fixing common issues
  --help            Show this help message

Examples:
  ./bin/quality-tools/checkstyle.sh                      # Run on all code
  ./bin/quality-tools/checkstyle.sh --module=rinna-cli   # Run on CLI module only
  ./bin/quality-tools/checkstyle.sh --file=src/main/java/org/rinna/Rinna.java
EOF
}

# Default settings
RUN_MODULE=""
TARGET_FILE=""
SHOW_FIXES=false

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --module=*)
      RUN_MODULE="${1#*=}"
      shift
      ;;
    --file=*)
      TARGET_FILE="${1#*=}"
      shift
      ;;
    --fix)
      SHOW_FIXES=true
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
echo -e "${BLUE}               Running Checkstyle                     ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Set up command
MVN_CMD="mvn checkstyle:check"

# Add module if specified
if [[ -n "$RUN_MODULE" ]]; then
  # Verify module exists
  if [[ ! -d "$RINNA_DIR/$RUN_MODULE" ]]; then
    error "Module '$RUN_MODULE' not found. Valid modules: rinna-cli, rinna-core, rinna-data-sqlite"
  fi
  MVN_CMD="mvn -pl $RUN_MODULE checkstyle:check"
  echo -e "${BLUE}Running Checkstyle on module: ${YELLOW}$RUN_MODULE${NC}"
fi

# If specific file is provided
if [[ -n "$TARGET_FILE" ]]; then
  if [[ -z "$RUN_MODULE" ]]; then
    error "When specifying a file, you must also specify a module with --module"
  fi
  
  # Verify file exists
  FULL_PATH="$RINNA_DIR/$RUN_MODULE/$TARGET_FILE"
  if [[ ! -f "$FULL_PATH" ]]; then
    error "File not found: $FULL_PATH"
  fi
  
  # Run checkstyle on specific file
  MVN_CMD="mvn -pl $RUN_MODULE checkstyle:check -Dcheckstyle.includes=$TARGET_FILE"
  echo -e "${BLUE}Running Checkstyle on file: ${YELLOW}$TARGET_FILE${NC}"
fi

# Execute the command
$MVN_CMD

# Display common fixes if requested
if [[ "$SHOW_FIXES" == "true" ]]; then
  echo -e "\n${BLUE}Common Checkstyle Fixes:${NC}"
  echo -e "${YELLOW}1. Line Length:${NC} Break long lines at 100 characters"
  echo -e "${YELLOW}2. Whitespace:${NC} Check spacing around operators, brackets, and braces"
  echo -e "${YELLOW}3. Javadoc:${NC} Add proper Javadoc to public methods and classes"
  echo -e "${YELLOW}4. Imports:${NC} Fix import order and remove unused imports"
  echo -e "${YELLOW}5. Naming:${NC} Use proper naming conventions for variables and methods"
fi

echo -e "${GREEN}Checkstyle check completed!${NC}"