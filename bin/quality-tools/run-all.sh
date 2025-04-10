#!/usr/bin/env bash
#
# run-all.sh - Run all quality checks sequentially
#
# PURPOSE: Run all quality checks in a controlled sequence
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
${BLUE}run-all.sh${NC} - Run all quality checks sequentially

Usage: run-all.sh [options]

Options:
  --module=<name>     Run on a specific module (rinna-cli, rinna-core, rinna-data-sqlite)
  --skip=<checks>     Comma-separated list of checks to skip
                      (checkstyle,pmd,spotbugs,enforcer,owasp)
  --continue-on-error Continue running checks even if one fails
  --help              Show this help message

Examples:
  ./bin/quality-tools/run-all.sh                               # Run all checks
  ./bin/quality-tools/run-all.sh --module=rinna-cli            # Run checks on CLI module
  ./bin/quality-tools/run-all.sh --skip=owasp,spotbugs         # Skip OWASP and SpotBugs
EOF
}

# Default settings
RUN_MODULE=""
SKIP_CHECKS=""
CONTINUE_ON_ERROR=false

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --module=*)
      RUN_MODULE="${1#*=}"
      shift
      ;;
    --skip=*)
      SKIP_CHECKS="${1#*=}"
      shift
      ;;
    --continue-on-error)
      CONTINUE_ON_ERROR=true
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
echo -e "${BLUE}          Running All Quality Checks                  ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Make scripts executable
chmod +x "$SCRIPT_DIR"/*.sh

# Build module argument if specified
MODULE_ARG=""
if [[ -n "$RUN_MODULE" ]]; then
  MODULE_ARG="--module=$RUN_MODULE"
  echo -e "${BLUE}Running checks on module: ${YELLOW}$RUN_MODULE${NC}"
fi

# Parse skip list
IFS=',' read -r -a SKIP_ARRAY <<< "$SKIP_CHECKS"
should_skip() {
  local check="$1"
  for skip_item in "${SKIP_ARRAY[@]}"; do
    if [[ "$skip_item" == "$check" ]]; then
      return 0
    fi
  done
  return 1
}

# Track overall status
OVERALL_STATUS=0

# Run a check with proper error handling
run_check() {
  local script="$1"
  local name="$2"
  local args="$3"
  
  echo -e "\n${BLUE}=====================================================${NC}"
  echo -e "${BLUE}Running $name check...${NC}"
  echo -e "${BLUE}=====================================================${NC}"
  
  if should_skip "$name"; then
    echo -e "${YELLOW}Skipping $name check as requested${NC}"
    return 0
  fi
  
  if "$SCRIPT_DIR/$script" $args; then
    echo -e "${GREEN}✓ $name check passed${NC}"
    return 0
  else
    local status=$?
    echo -e "${RED}✗ $name check failed with exit code $status${NC}"
    if [[ "$CONTINUE_ON_ERROR" != "true" ]]; then
      echo -e "${RED}Stopping due to failure. Use --continue-on-error to run all checks.${NC}"
      exit $status
    else
      OVERALL_STATUS=$status
      return $status
    fi
  fi
}

# Run each check in sequence
run_check "checkstyle.sh" "checkstyle" "$MODULE_ARG"
run_check "pmd.sh" "pmd" "$MODULE_ARG"
run_check "spotbugs.sh" "spotbugs" "$MODULE_ARG"
run_check "enforcer.sh" "enforcer" "$MODULE_ARG"

# Only run OWASP if not skipped (it's slow)
if ! should_skip "owasp"; then
  run_check "owasp.sh" "owasp" "$MODULE_ARG --quick"
fi

# Summarize results
echo -e "\n${BLUE}=====================================================${NC}"
echo -e "${BLUE}            Quality Check Summary                     ${NC}"
echo -e "${BLUE}=====================================================${NC}"

if [[ $OVERALL_STATUS -eq 0 ]]; then
  echo -e "${GREEN}✓ All quality checks completed successfully!${NC}"
else
  echo -e "${RED}✗ Some quality checks failed. Review the output above for details.${NC}"
  echo -e "${YELLOW}You can run individual checks to address specific issues.${NC}"
fi

exit $OVERALL_STATUS