#!/usr/bin/env bash
#
# owasp.sh - Run OWASP Dependency Check
#
# PURPOSE: Run OWASP Dependency Check for security vulnerability scanning
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
${BLUE}owasp.sh${NC} - Run OWASP Dependency Check for security vulnerability scanning

Usage: owasp.sh [options]

Options:
  --module=<name>    Run on a specific module (rinna-cli, rinna-core, rinna-data-sqlite)
  --cvss=<score>     Set minimum CVSS score to fail build (0-10, default: 7)
  --formats=<list>   Output formats (HTML,XML,JSON,CSV,SARIF,JUNIT, default: HTML,JSON)
  --update-only      Just update the NVD database without running a scan
  --scan-only        Skip database update, only run scan
  --quick            Run a quicker scan with fewer analyzers
  --async            Run in background and return control to terminal
  --help             Show this help message

Examples:
  ./bin/quality-tools/owasp.sh                 # Run on all code
  ./bin/quality-tools/owasp.sh --module=rinna-cli     # Run on CLI module only
  ./bin/quality-tools/owasp.sh --update-only   # Just update vulnerability database
  ./bin/quality-tools/owasp.sh --quick         # Quick scan for faster results
EOF
}

# Default settings
RUN_MODULE=""
CVSS_SCORE=7
OUTPUT_FORMATS="HTML,JSON"
UPDATE_ONLY=false
SCAN_ONLY=false
QUICK_SCAN=false
RUN_ASYNC=false

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --module=*)
      RUN_MODULE="${1#*=}"
      shift
      ;;
    --cvss=*)
      CVSS_SCORE="${1#*=}"
      shift
      ;;
    --formats=*)
      OUTPUT_FORMATS="${1#*=}"
      shift
      ;;
    --update-only)
      UPDATE_ONLY=true
      shift
      ;;
    --scan-only)
      SCAN_ONLY=true
      shift
      ;;
    --quick)
      QUICK_SCAN=true
      shift
      ;;
    --async)
      RUN_ASYNC=true
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

# Validate CVSS score
if ! [[ "$CVSS_SCORE" =~ ^[0-9]+(\.[0-9]+)?$ ]] || (( $(echo "$CVSS_SCORE > 10" | bc -l) )); then
  error "Invalid CVSS score: $CVSS_SCORE. Must be a number between 0 and 10."
fi

cd "$RINNA_DIR" || error "Failed to change to the Rinna directory."

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}        Running OWASP Dependency Check                ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Set up base command
CMD="mvn org.owasp:dependency-check-maven:check"

# Set up module-specific options
if [[ -n "$RUN_MODULE" ]]; then
  # Verify module exists
  if [[ ! -d "$RINNA_DIR/$RUN_MODULE" ]]; then
    error "Module '$RUN_MODULE' not found. Valid modules: rinna-cli, rinna-core, rinna-data-sqlite"
  fi
  CMD="mvn -pl $RUN_MODULE org.owasp:dependency-check-maven:check"
  echo -e "${BLUE}Running OWASP check on module: ${YELLOW}$RUN_MODULE${NC}"
fi

# Add options based on user selections
CMD="$CMD -Ddependency-check.skip=false -Ddependency-check.failBuildOnCVSS=$CVSS_SCORE"

# Handle update-only mode
if [[ "$UPDATE_ONLY" == "true" ]]; then
  echo -e "${BLUE}Updating NVD database only...${NC}"
  CMD="mvn org.owasp:dependency-check-maven:update-only"
  
  if [[ -n "$RUN_MODULE" ]]; then
    CMD="mvn -pl $RUN_MODULE org.owasp:dependency-check-maven:update-only"
  fi
fi

# Handle scan-only mode (skip update)
if [[ "$SCAN_ONLY" == "true" ]]; then
  echo -e "${BLUE}Running scan only (skipping database update)...${NC}"
  CMD="$CMD -Ddependency-check.skipUpdate=true"
fi

# Set output formats
CMD="$CMD -Dformats=$OUTPUT_FORMATS"

# Configure quick scan if requested
if [[ "$QUICK_SCAN" == "true" ]]; then
  echo -e "${BLUE}Running quick scan with fewer analyzers...${NC}"
  CMD="$CMD -Ddependency-check.nvdApiDelay=2000 -DretireJsAnalyzerEnabled=false -DnodeAuditAnalyzerEnabled=false -DnodejsAuditAnalyzerEnabled=false -DossindexAnalyzerEnabled=false"
fi

# Run asynchronously if requested
if [[ "$RUN_ASYNC" == "true" ]]; then
  echo -e "${BLUE}Running OWASP scan asynchronously...${NC}"
  LOG_FILE="$RINNA_DIR/dependency-check.log"
  
  nohup bash -c "$CMD" > "$LOG_FILE" 2>&1 &
  OWASP_PID=$!
  
  echo -e "${GREEN}OWASP scan started with PID: ${YELLOW}$OWASP_PID${NC}"
  echo -e "${GREEN}Results will be available in: ${YELLOW}$LOG_FILE${NC}"
  echo -e "${YELLOW}You can check progress with: tail -f $LOG_FILE${NC}"
  echo -e "${YELLOW}Or wait for completion with: wait $OWASP_PID${NC}"
else
  # Run synchronously
  echo -e "${BLUE}Running OWASP scan... This may take several minutes.${NC}"
  $CMD
  
  # Show report location
  if [[ "$UPDATE_ONLY" != "true" ]]; then
    if [[ -n "$RUN_MODULE" ]]; then
      REPORT_DIR="$RINNA_DIR/$RUN_MODULE/target/dependency-check-report"
    else
      REPORT_DIR="$RINNA_DIR/target/dependency-check-report"
    fi
    
    echo -e "${GREEN}OWASP Dependency Check completed!${NC}"
    echo -e "${GREEN}Reports can be found in: ${BLUE}$REPORT_DIR${NC}"
  else
    echo -e "${GREEN}NVD database update completed!${NC}"
  fi
fi