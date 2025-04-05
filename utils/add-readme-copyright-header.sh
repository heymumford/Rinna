#!/bin/bash

# Script to add copyright header to the main README.md file
# Copyright (c) 2025 Eric C. Mumford [@heymumford]

# Define the copyright notice
COPYRIGHT_HEADER="<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->\n\n"

# Main README.md file
README_FILE="README.md"

# Check if file already has the copyright header
if grep -q "Copyright (c) 2025 \[Eric C. Mumford\]" "$README_FILE"; then
    echo "README.md already has copyright header, skipping..."
else
    # Add the copyright header to the beginning of the file
    echo "Adding copyright header to README.md..."
    temp_file=$(mktemp)
    echo -e "$COPYRIGHT_HEADER$(cat $README_FILE)" > "$temp_file"
    mv "$temp_file" "$README_FILE"
    echo "Added copyright header to README.md."
fi