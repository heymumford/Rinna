#\!/bin/bash
#
# find-script-references.sh - Find references to old script names
#
# PURPOSE: Helps identify where script references need to be updated
#

# Determine script and project directories
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
RINNA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MAPPING_FILE="$SCRIPT_DIR/script-rename-mapping.txt"
OUTPUT_FILE="$RINNA_DIR/script-references.md"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if mapping file exists
if [ \! -f "$MAPPING_FILE" ]; then
  echo -e "${RED}Error: Mapping file not found: $MAPPING_FILE${NC}" >&2
  exit 1
fi

# Process the mapping file
echo -e "${BLUE}Searching for script references...${NC}"
echo ""

# Start the output file
echo "# Script Reference Migration Guide" > "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "This document lists all files containing references to scripts that have been renamed." >> "$OUTPUT_FILE"
echo "Use this to help update references to the new script naming convention." >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Process each script
grep -v "^#" "$MAPPING_FILE" | grep -v "^$" | while read -r line; do
  # Extract old path
  old_path=$(echo "$line" | awk '{print $1}')
  new_path=$(echo "$line" | awk '{print $2}')
  
  # Get just the script name without path
  old_script=$(basename "$old_path")
  new_script=$(basename "$new_path")
  
  echo -e "${YELLOW}Searching for references to:${NC} $old_script"
  
  # Add to output file
  echo "## $old_script → $new_script" >> "$OUTPUT_FILE"
  echo "" >> "$OUTPUT_FILE"
  
  # Search for references to the script name
  references=$(grep -r --include="*.sh" --include="*.java" --include="*.xml" --include="*.md" --include="*.yml" "$old_script" "$RINNA_DIR" | grep -v "$MAPPING_FILE" | grep -v "find-script-references.sh" | grep -v "rename-scripts.sh" | sort)
  
  if [ -z "$references" ]; then
    echo "No references found." >> "$OUTPUT_FILE"
  else
    echo "References:" >> "$OUTPUT_FILE"
    echo '```' >> "$OUTPUT_FILE"
    echo "$references" >> "$OUTPUT_FILE"
    echo '```' >> "$OUTPUT_FILE"
  fi
  
  echo "" >> "$OUTPUT_FILE"
done

echo -e "${GREEN}Search completed\!${NC}"
echo -e "${BLUE}Results saved to:${NC} script-references.md"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Review the script-references.md file"
echo "2. Update all references to use the new script names"
echo "3. Test the updated references to ensure they work correctly"
