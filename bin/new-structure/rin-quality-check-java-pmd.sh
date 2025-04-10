#!/usr/bin/env bash
#
# pmd.sh - Run PMD on Java code
#
# PURPOSE: Run PMD independently for faster quality verification
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
${BLUE}pmd.sh${NC} - Run PMD on Java code

Usage: pmd.sh [options]

Options:
  --module=<name>   Run on a specific module (rinna-cli, rinna-core, rinna-data-sqlite)
  --file=<path>     Run on a specific file (relative to module)
  --category=<cat>  Run specific rule category (bestpractices, codestyle, design, 
                    documentation, errorprone, multithreading, performance, security)
  --fix             Print instructions for fixing common issues
  --help            Show this help message

Examples:
  ./bin/quality-tools/pmd.sh                       # Run on all code
  ./bin/quality-tools/pmd.sh --module=rinna-cli    # Run on CLI module only
  ./bin/quality-tools/pmd.sh --category=security   # Run only security rules
EOF
}

# Default settings
RUN_MODULE=""
TARGET_FILE=""
CATEGORY=""
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
    --category=*)
      CATEGORY="${1#*=}"
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
echo -e "${BLUE}                  Running PMD                         ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Validate category if specified
if [[ -n "$CATEGORY" ]]; then
  valid_categories=("bestpractices" "codestyle" "design" "documentation" "errorprone" "multithreading" "performance" "security")
  valid=false
  for val in "${valid_categories[@]}"; do
    if [[ "$CATEGORY" == "$val" ]]; then
      valid=true
      break
    fi
  done
  
  if [[ "$valid" == "false" ]]; then
    error "Invalid category: $CATEGORY. Valid categories: ${valid_categories[*]}"
  fi
  
  echo -e "${BLUE}Running PMD with category: ${YELLOW}$CATEGORY${NC}"
fi

# Set up command
PMD_CMD="mvn pmd:check"

# Add module if specified
if [[ -n "$RUN_MODULE" ]]; then
  # Verify module exists
  if [[ ! -d "$RINNA_DIR/$RUN_MODULE" ]]; then
    error "Module '$RUN_MODULE' not found. Valid modules: rinna-cli, rinna-core, rinna-data-sqlite"
  fi
  PMD_CMD="mvn -pl $RUN_MODULE pmd:check"
  echo -e "${BLUE}Running PMD on module: ${YELLOW}$RUN_MODULE${NC}"
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
  
  # Run PMD on specific file
  PMD_CMD="mvn -pl $RUN_MODULE pmd:check -Dinclude=$TARGET_FILE"
  echo -e "${BLUE}Running PMD on file: ${YELLOW}$TARGET_FILE${NC}"
fi

# If specific category is provided
if [[ -n "$CATEGORY" ]]; then
  # Create a temporary ruleset with just the specified category
  TEMP_RULESET="/tmp/pmd-temp-ruleset-$$.xml"
  cat > "$TEMP_RULESET" << EOF
<?xml version="1.0"?>
<ruleset name="Temporary Ruleset"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    <description>Temporary ruleset for category: $CATEGORY</description>
    <rule ref="category/java/$CATEGORY.xml"/>
</ruleset>
EOF

  # Add ruleset to PMD command
  PMD_CMD="$PMD_CMD -Drulesets=$TEMP_RULESET"
fi

# Execute the command
$PMD_CMD

# Clean up temp file if created
if [[ -n "$CATEGORY" && -f "$TEMP_RULESET" ]]; then
  rm -f "$TEMP_RULESET"
fi

# Display common fixes if requested
if [[ "$SHOW_FIXES" == "true" ]]; then
  echo -e "\n${BLUE}Common PMD Fixes:${NC}"
  echo -e "${YELLOW}1. Unused imports/variables:${NC} Remove unused code"
  echo -e "${YELLOW}2. Proper exception handling:${NC} Don't catch exceptions without handling them"
  echo -e "${YELLOW}3. Code complexity:${NC} Refactor complex methods into smaller units"
  echo -e "${YELLOW}4. Resource handling:${NC} Use try-with-resources for file/connection handling"
  echo -e "${YELLOW}5. Best practices:${NC} Follow Java best practices for null checks, loops, etc."
fi

echo -e "${GREEN}PMD check completed!${NC}"