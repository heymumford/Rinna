#!/usr/bin/env bash
#
# fix-imports.sh - Automatically fix import ordering in Java files
#
# PURPOSE: Automatically fix import ordering and remove unused imports using Spotless
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
${BLUE}fix-imports.sh${NC} - Automatically fix import ordering in Java files

Usage: fix-imports.sh [options]

Options:
  --module=<name>   Run on a specific module (rinna-cli, rinna-core, rinna-data-sqlite)
  --file=<path>     Run on a specific file (relative to module)
  --add-plugin      Add the Spotless plugin to pom.xml if it's not already there
  --help            Show this help message

Examples:
  ./bin/quality-tools/fix-imports.sh --add-plugin   # Add Spotless plugin and fix all imports
  ./bin/quality-tools/fix-imports.sh                # Just run Spotless on all modules
  ./bin/quality-tools/fix-imports.sh --module=rinna-cli  # Run only on CLI module
EOF
}

# Default settings
RUN_MODULE=""
TARGET_FILE=""
ADD_PLUGIN=false

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
    --add-plugin)
      ADD_PLUGIN=true
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
echo -e "${BLUE}            Automatic Import Fix                      ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Add Spotless plugin to parent pom.xml if requested
if [[ "$ADD_PLUGIN" == "true" ]]; then
  echo -e "${BLUE}Adding Spotless plugin to pom.xml...${NC}"
  
  # Check if Spotless plugin is already in pom.xml
  if grep -q "com.diffplug.spotless" pom.xml; then
    echo -e "${YELLOW}Spotless plugin already exists in pom.xml${NC}"
  else
    # Need to add the plugin - use manual patching
    echo -e "${YELLOW}Adding plugin manually...${NC}"
    # Create a backup
    cp pom.xml pom.xml.bak
    
    # Find the plugins section and add the Spotless plugin
    awk '
      /<\/plugins>/ { 
        print "          <plugin>";
        print "            <groupId>com.diffplug.spotless</groupId>";
        print "            <artifactId>spotless-maven-plugin</artifactId>";
        print "            <version>2.43.0</version>";
        print "            <configuration>";
        print "              <java>";
        print "                <importOrder>";
        print "                  <order>java,javax,org,com</order>";
        print "                </importOrder>";
        print "                <removeUnusedImports />";
        print "                <formatAnnotations />";
        print "              </java>";
        print "            </configuration>";
        print "            <executions>";
        print "              <execution>";
        print "                <goals>";
        print "                  <goal>check</goal>";
        print "                </goals>";
        print "                <phase>process-sources</phase>";
        print "              </execution>";
        print "            </executions>";
        print "          </plugin>";
        print $0;
        next;
      }
      { print }
    ' pom.xml.bak > pom.xml
  fi
  
  echo -e "${GREEN}Spotless plugin added to pom.xml${NC}"
fi

# Set up Maven command
if [[ -n "$RUN_MODULE" ]]; then
  # Verify module exists
  if [[ ! -d "$RINNA_DIR/$RUN_MODULE" ]]; then
    error "Module '$RUN_MODULE' not found. Valid modules: rinna-cli, rinna-core, rinna-data-sqlite"
  fi
  MVN_CMD="mvn -pl $RUN_MODULE spotless:apply"
  echo -e "${BLUE}Running Spotless on module: ${YELLOW}$RUN_MODULE${NC}"
else
  MVN_CMD="mvn spotless:apply"
  echo -e "${BLUE}Running Spotless on all modules${NC}"
fi

# Execute the command
echo -e "${BLUE}Fixing imports with Spotless...${NC}"
$MVN_CMD

echo -e "${GREEN}Import ordering completed!${NC}"
echo -e "${YELLOW}Note: Run Checkstyle again to verify the import order issues are fixed.${NC}"