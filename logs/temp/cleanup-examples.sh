#!/bin/bash

# Script to cleanup example tests
# This script removes example tests that are just for demonstration

set -e  # Exit on error

PROJECT_ROOT="/home/emumford/NativeLinuxProjects/Rinna"
cd "${PROJECT_ROOT}"

echo "======================================================================"
echo "🧹 Cleaning up example test files in Rinna project"
echo "======================================================================"

# Find example tests
example_files=$(find "${PROJECT_ROOT}" -path "*/examples/*Example*.java" -not -path "*/target/*" -not -path "*/backup/*" -not -path "*/domain/model/*")
additional_examples=$(find "${PROJECT_ROOT}" -path "*/examples/ExampleAcceptanceTestRunner.java" -not -path "*/target/*" -not -path "*/backup/*")

# Combine the lists
all_examples="$example_files"$'\n'"$additional_examples"

removed_count=0

# Remove the example test files
echo "Removing example test files:"
while IFS= read -r file; do
  if [ -n "$file" ] && [ -f "$file" ]; then
    echo "  - $file"
    rm "$file"
    removed_count=$((removed_count + 1))
  fi
done <<< "$all_examples"

echo "======================================================================"
echo "✅ Removed $removed_count example test files"
echo "======================================================================"
echo "Here's a report of tests by module:"
echo "- rinna-core: $(find ${PROJECT_ROOT}/rinna-core -name "*Test.java" -not -path "*/target/*" | wc -l) test files"
echo "- rinna-cli: $(find ${PROJECT_ROOT}/rinna-cli -name "*Test.java" -not -path "*/target/*" | wc -l) test files"
echo "- main src: $(find ${PROJECT_ROOT}/src -name "*Test.java" -not -path "*/target/*" | wc -l) test files"
echo "======================================================================"