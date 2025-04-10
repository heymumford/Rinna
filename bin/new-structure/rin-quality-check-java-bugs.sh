#!/usr/bin/env bash
#
# spotbugs.sh - Run SpotBugs on Java code
#
# PURPOSE: Run SpotBugs independently for faster quality verification
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
${BLUE}spotbugs.sh${NC} - Run SpotBugs on Java code

Usage: spotbugs.sh [options]

Options:
  --module=<name>   Run on a specific module (rinna-cli, rinna-core, rinna-data-sqlite)
  --effort=<level>  Set effort level (min, default, max)
  --threshold=<lvl> Set bug threshold (low, medium, high)
  --html            Generate HTML report instead of XML
  --gui             Launch SpotBugs GUI (if available)
  --fix             Print instructions for fixing common issues
  --help            Show this help message

Examples:
  ./bin/quality-tools/spotbugs.sh                      # Run on all code
  ./bin/quality-tools/spotbugs.sh --module=rinna-cli   # Run on CLI module only
  ./bin/quality-tools/spotbugs.sh --effort=max --threshold=low   # Most thorough scan
EOF
}

# Default settings
RUN_MODULE=""
EFFORT="default"
THRESHOLD="medium"
FORMAT="xml"
LAUNCH_GUI=false
SHOW_FIXES=false

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --module=*)
      RUN_MODULE="${1#*=}"
      shift
      ;;
    --effort=*)
      EFFORT="${1#*=}"
      shift
      ;;
    --threshold=*)
      THRESHOLD="${1#*=}"
      shift
      ;;
    --html)
      FORMAT="html"
      shift
      ;;
    --gui)
      LAUNCH_GUI=true
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

# Validate effort
if [[ "$EFFORT" != "min" && "$EFFORT" != "default" && "$EFFORT" != "max" ]]; then
  error "Invalid effort level: $EFFORT. Valid values: min, default, max"
fi

# Validate threshold
if [[ "$THRESHOLD" != "low" && "$THRESHOLD" != "medium" && "$THRESHOLD" != "high" ]]; then
  error "Invalid threshold level: $THRESHOLD. Valid values: low, medium, high"
fi

cd "$RINNA_DIR" || error "Failed to change to the Rinna directory."

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}                Running SpotBugs                      ${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}Effort: ${YELLOW}$EFFORT${BLUE}, Threshold: ${YELLOW}$THRESHOLD${BLUE}, Format: ${YELLOW}$FORMAT${NC}"

# Set up command
if [[ "$LAUNCH_GUI" == "true" ]]; then
  CMD="mvn com.github.spotbugs:spotbugs-maven-plugin:gui"
else
  CMD="mvn spotbugs:check -Dspotbugs.effort=$EFFORT -Dspotbugs.threshold=$THRESHOLD"
  
  if [[ "$FORMAT" == "html" ]]; then
    CMD="mvn spotbugs:spotbugs -Dspotbugs.effort=$EFFORT -Dspotbugs.threshold=$THRESHOLD && mvn spotbugs:gui"
  fi
fi

# Add module if specified
if [[ -n "$RUN_MODULE" ]]; then
  # Verify module exists
  if [[ ! -d "$RINNA_DIR/$RUN_MODULE" ]]; then
    error "Module '$RUN_MODULE' not found. Valid modules: rinna-cli, rinna-core, rinna-data-sqlite"
  fi
  CMD=${CMD/mvn/mvn -pl $RUN_MODULE}
  echo -e "${BLUE}Running SpotBugs on module: ${YELLOW}$RUN_MODULE${NC}"
fi

# Execute the command
$CMD

# Display common fixes if requested
if [[ "$SHOW_FIXES" == "true" ]]; then
  echo -e "\n${BLUE}Common SpotBugs Fixes:${NC}"
  echo -e "${YELLOW}1. Null Pointer:${NC} Add null checks before dereferencing objects"
  echo -e "${YELLOW}2. Resource Leaks:${NC} Close resources in finally blocks or use try-with-resources"
  echo -e "${YELLOW}3. Security Issues:${NC} Don't expose sensitive information in exceptions or logs"
  echo -e "${YELLOW}4. Synchronization:${NC} Fix thread safety issues in concurrent code"
  echo -e "${YELLOW}5. Bad Practice:${NC} Override equals and hashCode together, don't ignore exceptions"
fi

# If HTML report was generated
if [[ "$FORMAT" == "html" && "$LAUNCH_GUI" == "false" ]]; then
  if [[ -n "$RUN_MODULE" ]]; then
    REPORT_PATH="$RINNA_DIR/$RUN_MODULE/target/spotbugsXml.html"
  else
    REPORT_PATH="$RINNA_DIR/target/spotbugsXml.html"
  fi
  
  if [[ -f "$REPORT_PATH" ]]; then
    echo -e "${GREEN}SpotBugs HTML report generated at: ${BLUE}$REPORT_PATH${NC}"
    echo -e "${YELLOW}You can open this file in a browser to view detailed results.${NC}"
  fi
fi

echo -e "${GREEN}SpotBugs check completed!${NC}"