#\!/bin/bash
#
# rename-scripts.sh - Script to implement the new naming convention
#
# PURPOSE: Copies scripts to new locations with new names and updates references
#

# Determine script and project directories
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
RINNA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MAPPING_FILE="$SCRIPT_DIR/script-rename-mapping.txt"

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
echo -e "${BLUE}Processing script renaming...${NC}"
echo ""

grep -v "^#" "$MAPPING_FILE" | grep -v "^$" | while read -r line; do
  # Extract old and new paths
  old_path=$(echo "$line" | awk '{print $1}')
  new_path=$(echo "$line" | awk '{print $2}')
  
  # Convert to absolute paths
  old_abs_path="$RINNA_DIR/$old_path"
  new_abs_path="$RINNA_DIR/$new_path"
  
  # Check if source file exists
  if [ \! -f "$old_abs_path" ]; then
    echo -e "${YELLOW}Warning: Source file not found: $old_abs_path${NC}"
    continue
  fi
  
  # Create directory if it doesn't exist
  mkdir -p "$(dirname "$new_abs_path")"
  
  # Copy the file
  cp "$old_abs_path" "$new_abs_path"
  
  # Make it executable
  chmod +x "$new_abs_path"
  
  echo -e "${GREEN}Copied:${NC} $old_path -> $new_path"
done

echo ""
echo -e "${GREEN}Script renaming completed\!${NC}"
echo -e "${YELLOW}Note: You will need to update references to these scripts in other files.${NC}"
