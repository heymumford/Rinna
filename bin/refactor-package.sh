#!/usr/bin/env bash

#
# refactor-package.sh - Refactor Java package structure
#
# USAGE: ./bin/refactor-package.sh
#
# PURPOSE: Refactor Java packages from com.rinna to org.rinna
#
# DESCRIPTION:
# This script migrates the entire codebase from the 'com.rinna' package 
# namespace to 'org.rinna'. It updates package declarations, imports, 
# and references in build files. See docs/development/package-refactoring.md
# for more details.
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

echo -e "${BLUE}Starting package refactoring from com.rinna to org.rinna...${NC}"

# Step 1: Copy all Java files to their new location
echo -e "${BLUE}Copying Java files to new locations...${NC}"
find . -path "*/src/*/java/org/rinna/*" -name "*.java" | while read file; do
    # Skip backup files
    if [[ "$file" == *".bak" ]]; then
        continue
    fi
    
    # Determine new file path
    new_file="${file/\/com\/rinna\//\/org\/rinna\/}"
    
    # Create directory if it doesn't exist
    mkdir -p "$(dirname "$new_file")"
    
    # Copy the file
    cp "$file" "$new_file"
    echo -e "${GREEN}Copied: $file -> $new_file${NC}"
done

# Step 2: Update package declarations and imports in all Java files
echo -e "${BLUE}Updating package declarations and imports...${NC}"
find . -path "*/src/*/java/org/rinna/*" -name "*.java" | while read file; do
    # Update package declaration
    sed -i 's/package com\.rinna/package org.rinna/g' "$file"
    
    # Update import statements
    sed -i 's/import com\.rinna/import org.rinna/g' "$file"
    
    echo -e "${GREEN}Updated package in: $file${NC}"
done

# Step 3: Update pom.xml files for references to main class
echo -e "${BLUE}Updating pom.xml files...${NC}"
find . -name "pom.xml" | while read file; do
    sed -i 's/<mainClass>com\.rinna/<mainClass>org.rinna/g' "$file"
    echo -e "${GREEN}Updated pom.xml: $file${NC}"
done

# Step 4: Update feature files if needed
echo -e "${BLUE}Checking feature files...${NC}"
find . -name "*.feature" | while read file; do
    if grep -q "com\.rinna" "$file"; then
        sed -i 's/com\.rinna/org.rinna/g' "$file"
        echo -e "${GREEN}Updated feature file: $file${NC}"
    fi
done

# Step 5: Update shell scripts that reference paths
echo -e "${BLUE}Updating shell scripts...${NC}"
find ./bin -name "*.sh" | while read file; do
    if grep -q "org/rinna" "$file"; then
        sed -i 's/com\/rinna/org\/rinna/g' "$file"
        echo -e "${GREEN}Updated shell script: $file${NC}"
    fi
done

echo -e "${GREEN}Package refactoring completed successfully!${NC}"
echo -e "${YELLOW}Note: You might need to update any hard-coded class references or reflection code manually.${NC}"