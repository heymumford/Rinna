#!/bin/bash

# Script to clean up Claude AI references from markdown files
# Usage: ./clean-claude-references.sh

find /home/emumford/NativeLinuxProjects/Rinna/docs -type f -name "*.md" -exec sed -i \
  's/<!-- Copyright (c) 2025 \[Eric C. Mumford\](https:\/\/github.com\/heymumford) \[@heymumford\], Gemini Deep Research, Claude 3.7. -->/<!-- Copyright (c) 2025 \[Eric C. Mumford\](https:\/\/github.com\/heymumford) \[@heymumford\] -->/' {} \;

echo "Claude references have been cleaned up from markdown files."