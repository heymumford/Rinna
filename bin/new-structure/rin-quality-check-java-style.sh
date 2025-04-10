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

# Define the steps in this script
STEPS=()

# Step 1: Configure Checkstyle settings
STEPS+=("Configuring Checkstyle settings")

# Step 2: Run Checkstyle analysis
STEPS+=("Running Checkstyle analysis")

# Step 3: Process and display results
STEPS+=("Processing Checkstyle results")

# Show script header with steps count
echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}               Running Checkstyle                     ${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}> STARTING:${NC} About to run Checkstyle with ${#STEPS[@]} steps"

for i in "${!STEPS[@]}"; do
  echo -e "  ${GRAY}$(($i + 1))/${#STEPS[@]}:${NC} ${STEPS[$i]}"
done
echo ""

# Set up variables to track warnings and errors
WARNING_COUNT=0
ERROR_COUNT=0

# Step 1: Configure Checkstyle settings
echo -e "${BLUE}> STEP 1/${#STEPS[@]}:${NC} ${STEPS[0]}"
echo -e "${YELLOW}> IN PROGRESS:${NC} Setting up Checkstyle configuration"

# Set up command
MVN_CMD="mvn checkstyle:check"

# Add module if specified
if [[ -n "$RUN_MODULE" ]]; then
  # Verify module exists
  if [[ ! -d "$RINNA_DIR/$RUN_MODULE" ]]; then
    echo -e "${RED}> ERROR:${NC} Module '$RUN_MODULE' not found. Valid modules: rinna-cli, rinna-core, rinna-data-sqlite"
    exit 1
  fi
  MVN_CMD="mvn -pl $RUN_MODULE checkstyle:check"
  echo -e "${BLUE}> CONFIGURED:${NC} Running Checkstyle on module: ${YELLOW}$RUN_MODULE${NC}"
fi

# If specific file is provided
if [[ -n "$TARGET_FILE" ]]; then
  if [[ -z "$RUN_MODULE" ]]; then
    echo -e "${RED}> ERROR:${NC} When specifying a file, you must also specify a module with --module"
    exit 1
  fi
  
  # Verify file exists
  FULL_PATH="$RINNA_DIR/$RUN_MODULE/$TARGET_FILE"
  if [[ ! -f "$FULL_PATH" ]]; then
    echo -e "${RED}> ERROR:${NC} File not found: $FULL_PATH"
    exit 1
  fi
  
  # Run checkstyle on specific file
  MVN_CMD="mvn -pl $RUN_MODULE checkstyle:check -Dcheckstyle.includes=$TARGET_FILE"
  echo -e "${BLUE}> CONFIGURED:${NC} Running Checkstyle on file: ${YELLOW}$TARGET_FILE${NC}"
fi

echo -e "${GREEN}> COMPLETED:${NC} Successfully configured Checkstyle settings"
echo ""

# Step 2: Run Checkstyle analysis
echo -e "${BLUE}> STEP 2/${#STEPS[@]}:${NC} ${STEPS[1]}"
echo -e "${YELLOW}> IN PROGRESS:${NC} Executing Checkstyle analysis - this may take a moment"

# Execute the command and save output to a temp file
TEMP_OUTPUT=$(mktemp)
if $MVN_CMD > "$TEMP_OUTPUT" 2>&1; then
  CHECKSTYLE_STATUS=0
  echo -e "${GREEN}> COMPLETED:${NC} Checkstyle analysis finished successfully"
else
  CHECKSTYLE_STATUS=$?
  echo -e "${RED}> ERROR:${NC} Checkstyle analysis failed with exit code $CHECKSTYLE_STATUS"
fi
echo ""

# Step 3: Process and display results
echo -e "${BLUE}> STEP 3/${#STEPS[@]}:${NC} ${STEPS[2]}"
echo -e "${YELLOW}> IN PROGRESS:${NC} Processing Checkstyle results"

# Count warnings and errors
WARNING_COUNT=$(grep -c '\[WARN\]' "$TEMP_OUTPUT" || echo "0")
ERROR_COUNT=$(grep -c '\[ERROR\]' "$TEMP_OUTPUT" || echo "0")

# Display common fixes if requested
if [[ "$SHOW_FIXES" == "true" ]]; then
  echo -e "\n${BLUE}Common Checkstyle Fixes:${NC}"
  echo -e "${YELLOW}1. Line Length:${NC} Break long lines at 100 characters"
  echo -e "${YELLOW}2. Whitespace:${NC} Check spacing around operators, brackets, and braces"
  echo -e "${YELLOW}3. Javadoc:${NC} Add proper Javadoc to public methods and classes"
  echo -e "${YELLOW}4. Imports:${NC} Fix import order and remove unused imports"
  echo -e "${YELLOW}5. Naming:${NC} Use proper naming conventions for variables and methods"
fi

echo -e "${GREEN}> COMPLETED:${NC} Finished processing Checkstyle results"
echo ""

# Summary
echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}               Checkstyle Summary                     ${NC}"
echo -e "${BLUE}=====================================================${NC}"

if [[ $CHECKSTYLE_STATUS -eq 0 ]]; then
  echo -e "${GREEN}> SUCCESS:${NC} Checkstyle check completed successfully"
else
  echo -e "${RED}> FAILED:${NC} Checkstyle check failed with exit code $CHECKSTYLE_STATUS"
fi

echo -e "${BLUE}> STATISTICS:${NC} Found $WARNING_COUNT warnings and $ERROR_COUNT errors"

# Show the most common issues if there are any
if [[ $WARNING_COUNT -gt 0 || $ERROR_COUNT -gt 0 ]]; then
  echo -e "${YELLOW}> ISSUES:${NC} Most common issues:"
  grep '\[WARN\]' "$TEMP_OUTPUT" | sort | uniq -c | sort -nr | head -5 | while read -r line; do
    echo -e "  ${YELLOW}●${NC} $line"
  done
fi

# Clean up
rm -f "$TEMP_OUTPUT"

# Exit with the original status
if [[ $CHECKSTYLE_STATUS -ne 0 ]]; then
  echo -e "${RED}> BUILD STOPPED:${NC} Halting due to Checkstyle errors"
  exit $CHECKSTYLE_STATUS
fi