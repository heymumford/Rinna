#!/bin/bash

# Script to add copyright headers to all Java source files
# Copyright (c) 2025 Eric C. Mumford [@heymumford], Gemini Deep Research, Claude 3.7.

# Define the copyright notice for Java files
JAVA_COPYRIGHT_HEADER="/*\n * Copyright (c) 2025 Eric C. Mumford (@heymumford), Gemini Deep Research, Claude 3.7.\n * This file is subject to the terms and conditions defined in\n * the LICENSE file, which is part of this source code package.\n */\n\n"

# Find all Java source files
find . -type f -name "*.java" | while read file; do
    echo "Processing $file..."
    
    # Check if file already has the copyright header
    if grep -q "Copyright (c) 2025 Eric C. Mumford" "$file"; then
        echo "  Already has copyright header, skipping..."
        continue
    fi
    
    # Add the copyright header to the beginning of the file
    temp_file=$(mktemp)
    echo -e "$JAVA_COPYRIGHT_HEADER$(cat $file)" > "$temp_file"
    mv "$temp_file" "$file"
    echo "  Added copyright header."
done

echo "All Java source files have been processed."