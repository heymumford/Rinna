#!/usr/bin/env bash
#
# enforcer.sh - Run Maven Enforcer Rules
#
# PURPOSE: Run Maven Enforcer Rules independently for faster quality verification
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
${BLUE}enforcer.sh${NC} - Run Maven Enforcer Rules

Usage: enforcer.sh [options]

Options:
  --module=<name>   Run on a specific module (rinna-cli, rinna-core, rinna-data-sqlite)
  --rule=<name>     Run a specific rule. Available rules:
                    - dependencyConvergence (check for version conflicts)
                    - requireJavaVersion (verify Java version)
                    - requireMavenVersion (verify Maven version)
                    - banDuplicatePomDependencyVersions
                    - all (run all rules)
  --help            Show this help message

Examples:
  ./bin/quality-tools/enforcer.sh                                  # Run all rules on all modules
  ./bin/quality-tools/enforcer.sh --module=rinna-cli               # Run all rules on CLI module
  ./bin/quality-tools/enforcer.sh --rule=dependencyConvergence     # Check dependency versions
EOF
}

# Default settings
RUN_MODULE=""
RULE="all"

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --module=*)
      RUN_MODULE="${1#*=}"
      shift
      ;;
    --rule=*)
      RULE="${1#*=}"
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

# Validate rule
VALID_RULES=("all" "dependencyConvergence" "requireJavaVersion" "requireMavenVersion" "banDuplicatePomDependencyVersions")
RULE_VALID=false
for valid_rule in "${VALID_RULES[@]}"; do
  if [[ "$RULE" == "$valid_rule" ]]; then
    RULE_VALID=true
    break
  fi
done

if [[ "$RULE_VALID" == "false" ]]; then
  error "Invalid rule: $RULE. Valid rules: ${VALID_RULES[*]}"
fi

cd "$RINNA_DIR" || error "Failed to change to the Rinna directory."

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}           Running Maven Enforcer Rules              ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Set up command
if [[ "$RULE" == "all" ]]; then
  CMD="mvn org.apache.maven.plugins:maven-enforcer-plugin:3.4.1:enforce"
else
  CMD="mvn org.apache.maven.plugins:maven-enforcer-plugin:3.4.1:enforce -Drules=$RULE"
  echo -e "${BLUE}Running rule: ${YELLOW}$RULE${NC}"
fi

# Add module if specified
if [[ -n "$RUN_MODULE" ]]; then
  # Verify module exists
  if [[ ! -d "$RINNA_DIR/$RUN_MODULE" ]]; then
    error "Module '$RUN_MODULE' not found. Valid modules: rinna-cli, rinna-core, rinna-data-sqlite"
  fi
  CMD=${CMD/mvn/mvn -pl $RUN_MODULE}
  echo -e "${BLUE}Running enforcer rules on module: ${YELLOW}$RUN_MODULE${NC}"
fi

# Execute the command
$CMD

echo -e "${GREEN}Maven Enforcer check completed!${NC}"

# Display helpful information for specific rules
if [[ "$RULE" == "dependencyConvergence" || "$RULE" == "all" ]]; then
  echo -e "\n${BLUE}Dependency Convergence Tips:${NC}"
  echo -e "${YELLOW}If you have dependency conflicts, you can resolve them by:${NC}"
  echo -e "1. Adding explicit dependency exclusions in your pom.xml"
  echo -e "2. Adding dependency management entries to control versions"
  echo -e "3. Using 'mvn dependency:tree -Dverbose' to see the conflict paths"
fi