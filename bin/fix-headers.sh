#!/usr/bin/env bash

#
# fix-headers.sh - Fix source file headers
#
# PURPOSE: Adds or updates copyright and license headers in source files
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

RINNA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RINNA_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Java header template
JAVA_HEADER='/*
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
'

# Bash header template
BASH_HEADER='#
# PURPOSE: '

# Fix Java file headers
function fix_java_file() {
    local file=$1
    local filename=$(basename "$file")
    local purpose=""
    
    # Determine purpose based on filename or directory
    if [[ "$file" == *"/model/"* ]]; then
        purpose="Model class for the Rinna workflow management system"
    elif [[ "$file" == *"/service/"* ]]; then
        purpose="Service component for the Rinna workflow management system"
    elif [[ "$file" == *"/Test.java" ]]; then
        purpose="Test class for the Rinna workflow management system"
    elif [[ "$filename" == "Rinna.java" ]]; then
        purpose="Entry point for the Rinna workflow management system"
    elif [[ "$filename" == "Main.java" ]]; then
        purpose="Main application entry point for the Rinna workflow management system"
    else
        purpose="Component of the Rinna workflow management system"
    fi
    
    # Get package line, including the case where it's on the same line as closing comment
    local package_line=$(grep -m 1 -E "^package|^\*/package" "$file" || echo "")
    if [[ -z "$package_line" ]]; then
        # Try to find if the package line is immediately after a comment (same line)
        package_line=$(grep -m 1 "*/package" "$file" || echo "")
        if [[ -z "$package_line" ]]; then
            echo -e "${YELLOW}Skipping $file - no package line found${NC}"
            return
        fi
    fi
    
    # Create temp file
    local temp_file=$(mktemp)
    
    # Add header with purpose
    echo -e "/*\n * $purpose\n *\n * Copyright (c) 2025 Eric C. Mumford (@heymumford)\n * This file is subject to the terms and conditions defined in\n * the LICENSE file, which is part of this source code package.\n */\n" > "$temp_file"
    
    # Add package and rest of file, skipping any existing header
    sed -n '/^package/,$p' "$file" >> "$temp_file"
    
    # Backup the original file
    cp "$file" "${file}.bak"
    
    # Replace the original file with the fixed one
    mv "$temp_file" "$file"
    
    echo -e "${GREEN}Fixed header in $file${NC}"
}

# Fix Bash file headers
function fix_bash_file() {
    local file=$1
    local filename=$(basename "$file")
    local purpose=""
    
    # Determine if it's already a good header
    if grep -q "PURPOSE:" "$file" && grep -q "Copyright (c) 2025 Eric C. Mumford" "$file"; then
        echo -e "${BLUE}Header looks good in $file, skipping${NC}"
        return
    fi
    
    # Determine purpose based on filename
    case "$filename" in
        "rin")
            purpose="Command-line utility to simplify building, cleaning, and running tests for Rinna"
            ;;
        "rin-simple")
            purpose="Simplified CLI utility for Rinna build operations"
            ;;
        "rin-demo")
            purpose="Demonstration of Rinna CLI modes and capabilities"
            ;;
        *)
            purpose="Utility script for the Rinna workflow management system"
            ;;
    esac
    
    # Create temp file
    local temp_file=$(mktemp)
    
    # Add shebang if it exists
    if head -1 "$file" | grep -q "^#!/"; then
        head -1 "$file" > "$temp_file"
        echo >> "$temp_file"
    fi
    
    # Add header
    echo -e "#\n# $filename - Rinna utility\n#\n# PURPOSE: $purpose\n#\n# Copyright (c) 2025 Eric C. Mumford (@heymumford)\n# This file is subject to the terms and conditions defined in\n# the LICENSE file, which is part of this source code package.\n# (MIT License)\n#\n" >> "$temp_file"
    
    # Add the rest of the file content, skipping existing header
    if head -1 "$file" | grep -q "^#!/"; then
        # Skip shebang and any existing header comments
        sed -n '/^[^#]/,$p' "$file" | tail -n +2 >> "$temp_file"
    else
        # Skip any existing header comments
        sed -n '/^[^#]/,$p' "$file" >> "$temp_file"
    fi
    
    # Backup the original file
    cp "$file" "${file}.bak"
    
    # Replace the original file with the fixed one
    mv "$temp_file" "$file"
    
    echo -e "${GREEN}Fixed header in $file${NC}"
}

# Process all Java files
echo -e "${BLUE}Fixing Java file headers...${NC}"
find . -type f -name "*.java" | grep -v "./target/" | while read file; do
    fix_java_file "$file"
done

# Process all Bash files
echo -e "${BLUE}Fixing Bash file headers...${NC}"
find ./bin -type f -not -path "*/\.*" -exec file {} \; | grep "shell script" | cut -d: -f1 | while read file; do
    fix_bash_file "$file"
done

echo -e "${GREEN}All headers have been fixed!${NC}"