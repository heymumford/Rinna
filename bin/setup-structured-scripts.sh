#\!/bin/bash
#
# setup-structured-scripts.sh - Set up the new script structure
#
# PURPOSE: Creates a structured script directory and prepares documentation
#

# Determine script and project directories
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
RINNA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MAPPING_FILE="$SCRIPT_DIR/script-rename-mapping.txt"
DOC_FILE="$RINNA_DIR/docs/development/script-naming-convention.md"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create documentation
create_documentation() {
  mkdir -p "$(dirname "$DOC_FILE")"
  
  cat << 'DOC_CONTENT' > "$DOC_FILE"
# Script Naming Convention

## Overview

This document defines the naming convention for utility scripts in the Rinna project.

## Naming Structure

All scripts follow this naming convention:

```
rin-[module]-[submodule]-[language]-[action].sh
```

Where:
- `rin-` is a short prefix identifying all Rinna project scripts
- `[module]` represents the major component (core, cli, api, data, etc.)
- `[submodule]` defines the specific area (build, test, quality, etc.)
- `[language]` indicates scope (java, go, python, all)
- `[action]` describes the specific operation (build, check, fix, etc.)

Some modules may not use all segments if they're not applicable.

## Examples

```bash
# Build scripts
rin-build-main-all.sh          # Main build script for all components
rin-build-config-java.sh       # Configure Java build

# Quality scripts
rin-quality-check-java-style.sh # Run checkstyle on Java code
rin-quality-check-all.sh        # Run all quality checks
rin-quality-fix-java-imports.sh # Fix Java import ordering

# Test scripts
rin-test-run-all.sh             # Run all tests
rin-api-test-oauth.sh           # Test OAuth integration in API

# Infrastructure scripts
rin-infra-container-all.sh      # Container management script

# Security scripts
rin-security-check-dependencies.sh # Check dependency security

# XML scripts
rin-xml-format-all.sh           # Format all XML files
rin-xml-fix-pom-tags.sh         # Fix POM file tags
```

## Script Mapping

Below is the mapping from old script names to the new naming convention:

DOC_CONTENT

  echo "| Old Script | New Script |" >> "$DOC_FILE"
  echo "|------------|------------|" >> "$DOC_FILE"
  
  grep -v "^#" "$MAPPING_FILE" | grep -v "^$" | while read -r line; do
    old_path=$(echo "$line" | awk '{print $1}')
    new_path=$(echo "$line" | awk '{print $2}')
    
    old_name=$(basename "$old_path")
    new_name=$(basename "$new_path")
    
    echo "| \`$old_name\` | \`$new_name\` |" >> "$DOC_FILE"
  done
  
  echo "" >> "$DOC_FILE"
  echo "## Usage" >> "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  echo "All scripts are located in the \`bin\` directory. The new naming convention has been implemented alongside the old scripts for backward compatibility. To use the new scripts:" >> "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  echo "```bash" >> "$DOC_FILE"
  echo "./bin/rin-build-main-all.sh       # Use the main build script" >> "$DOC_FILE"
  echo "./bin/rin-quality-check-all.sh    # Run all quality checks" >> "$DOC_FILE"
  echo "```" >> "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  echo "Eventually, all references to the old script names will be updated and the old scripts will be deprecated." >> "$DOC_FILE"
  
  echo -e "${GREEN}Created documentation:${NC} $DOC_FILE"
}

# Create symlinks to the new script structure
create_symlinks() {
  echo -e "${BLUE}Creating symlinks for backward compatibility...${NC}"
  
  grep -v "^#" "$MAPPING_FILE" | grep -v "^$" | while read -r line; do
    old_path=$(echo "$line" | awk '{print $1}')
    new_path=$(echo "$line" | awk '{print $2}')
    
    # Get just the script name without the full path
    new_name=$(basename "$new_path")
    
    # Create symlinks in the main bin directory
    target_link="$RINNA_DIR/bin/$new_name"
    
    # Create the symlink
    if [ \! -e "$target_link" ]; then
      ln -sf "$RINNA_DIR/$new_path" "$target_link"
      echo -e "${GREEN}Created symlink:${NC} bin/$new_name -> $new_path"
    else
      echo -e "${YELLOW}Symlink already exists:${NC} bin/$new_name"
    fi
  done
}

# Main function
main() {
  echo -e "${BLUE}Setting up structured scripts...${NC}"
  
  # Check if mapping file exists
  if [ \! -f "$MAPPING_FILE" ]; then
    echo -e "${RED}Error: Mapping file not found: $MAPPING_FILE${NC}" >&2
    exit 1
  fi
  
  # Create documentation
  create_documentation
  
  # Create symlinks
  create_symlinks
  
  echo ""
  echo -e "${GREEN}Setup completed\!${NC}"
  echo -e "${YELLOW}Next steps:${NC}"
  echo "1. Review the script references in script-references.md"
  echo "2. Gradually update references in the codebase to use the new script names"
  echo "3. Once all references are updated, the old scripts can be removed"
}

# Run the main function
main
