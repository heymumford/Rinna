#!/usr/bin/env bash
#
# rin-quality-check-all.sh - Run all quality checks sequentially
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

# Define all available quality checks
ALL_CHECKS=("checkstyle" "pmd" "spotbugs" "enforcer" "owasp")

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}          Running All Quality Checks                  ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Calculate how many checks we'll actually run
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

# Count checks that aren't skipped
ACTIVE_CHECKS=()
for check in "${ALL_CHECKS[@]}"; do
  if ! should_skip "$check"; then
    ACTIVE_CHECKS+=("$check")
  fi
done

# Announce the plan
echo -e "${BLUE}> STARTING:${NC} About to run ${#ACTIVE_CHECKS[@]} quality checks"
for i in "${!ACTIVE_CHECKS[@]}"; do
  echo -e "  ${GRAY}$(($i + 1))/${#ACTIVE_CHECKS[@]}:${NC} ${ACTIVE_CHECKS[$i]}"
done
echo ""

# Make scripts executable
chmod +x "$SCRIPT_DIR"/*.sh

# Build module argument if specified
MODULE_ARG=""
if [[ -n "$RUN_MODULE" ]]; then
  MODULE_ARG="--module=$RUN_MODULE"
  echo -e "${BLUE}> TARGET:${NC} Running checks on module: ${YELLOW}$RUN_MODULE${NC}"
  echo ""
fi

# Track overall status
OVERALL_STATUS=0
TOTAL_WARNINGS=0
TOTAL_ERRORS=0
STEP_NUMBER=1
TOTAL_STEPS=${#ACTIVE_CHECKS[@]}

# Run a check with proper error handling
run_check() {
  local script="$1"
  local name="$2"
  local args="$3"
  
  echo -e "\n${BLUE}=====================================================${NC}"
  echo -e "${BLUE}> STEP $STEP_NUMBER/$TOTAL_STEPS: Running $name check${NC}"
  echo -e "${BLUE}=====================================================${NC}"
  
  if should_skip "$name"; then
    echo -e "${YELLOW}> SKIPPED:${NC} $name check was skipped as requested"
    return 0
  fi
  
  echo -e "${YELLOW}> IN PROGRESS:${NC} Running $name quality check"
  
  # Run the check and capture output
  local TEMP_OUTPUT=$(mktemp)
  if "$SCRIPT_DIR/$script" $args > "$TEMP_OUTPUT" 2>&1; then
    local status=0
    echo -e "${GREEN}> SUCCESS:${NC} $name check passed"
  else
    local status=$?
    echo -e "${RED}> FAILED:${NC} $name check failed with exit code $status"
    
    # Count warnings and errors
    local warnings=$(grep -c '\[WARN\]' "$TEMP_OUTPUT" || echo "0")
    local errors=$(grep -c '\[ERROR\]' "$TEMP_OUTPUT" || echo "0")
    
    ((TOTAL_WARNINGS += warnings))
    ((TOTAL_ERRORS += errors))
    
    echo -e "${YELLOW}> STATISTICS:${NC} Found $warnings warnings and $errors errors"
    
    # In verbose mode or on failure, show the output
    if [[ "$status" -ne 0 ]]; then
      echo ""
      cat "$TEMP_OUTPUT"
      echo ""
    fi
    
    if [[ "$CONTINUE_ON_ERROR" != "true" ]]; then
      echo -e "${RED}> BUILD STOPPED:${NC} Halting due to $name failure. Use --continue-on-error to run all checks."
      rm -f "$TEMP_OUTPUT"
      exit $status
    else
      OVERALL_STATUS=$status
    fi
  fi
  
  rm -f "$TEMP_OUTPUT"
  ((STEP_NUMBER++))
  return $status
}

# Run each check in sequence
for check in "${ACTIVE_CHECKS[@]}"; do
  case "$check" in
    checkstyle)
      run_check "checkstyle.sh" "checkstyle" "$MODULE_ARG"
      ;;
    pmd)
      run_check "pmd.sh" "pmd" "$MODULE_ARG"
      ;;
    spotbugs)
      run_check "spotbugs.sh" "spotbugs" "$MODULE_ARG"
      ;;
    enforcer)
      run_check "enforcer.sh" "enforcer" "$MODULE_ARG"
      ;;
    owasp)
      run_check "owasp.sh" "owasp" "$MODULE_ARG --quick"
      ;;
  esac
done

# Summarize results
echo -e "\n${BLUE}=====================================================${NC}"
echo -e "${BLUE}            Quality Check Summary                     ${NC}"
echo -e "${BLUE}=====================================================${NC}"

if [[ $OVERALL_STATUS -eq 0 ]]; then
  echo -e "${GREEN}> SUCCESS:${NC} All ${#ACTIVE_CHECKS[@]} quality checks completed successfully!"
else
  echo -e "${RED}> FAILED:${NC} Some quality checks failed. Review the output above for details."
fi

echo -e "${BLUE}> STATISTICS:${NC} Total: ${#ACTIVE_CHECKS[@]} checks, $TOTAL_WARNINGS warnings, $TOTAL_ERRORS errors"

if [[ $OVERALL_STATUS -ne 0 ]]; then
  echo -e "${YELLOW}> NEXT STEPS:${NC} You can run individual checks to address specific issues:"
  for check in "${ACTIVE_CHECKS[@]}"; do
    echo -e "  ${YELLOW}●${NC} ./bin/quality-tools/${check}.sh $MODULE_ARG --fix"
  done
  echo -e "${RED}> BUILD STOPPED:${NC} Quality check failures detected"
fi

exit $OVERALL_STATUS