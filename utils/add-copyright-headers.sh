#!/bin/bash

# Script to add copyright headers to all documentation files
# Copyright (c) 2025 Eric C. Mumford [@heymumford]

# Define the copyright notice
COPYRIGHT_HEADER="<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->\n\n"

# Find all markdown files in the docs directory
find docs -type f -name "*.md" | while read file; do
    echo "Processing $file..."
    
    # Check if file already has the copyright header
    if grep -q "Copyright (c) 2025 \[Eric C. Mumford\]" "$file"; then
        echo "  Already has copyright header, skipping..."
        continue
    fi
    
    # Add the copyright header to the beginning of the file
    temp_file=$(mktemp)
    echo -e "$COPYRIGHT_HEADER$(cat $file)" > "$temp_file"
    mv "$temp_file" "$file"
    echo "  Added copyright header."
done

echo "All documentation files have been processed."