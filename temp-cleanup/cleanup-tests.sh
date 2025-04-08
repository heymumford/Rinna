#!/bin/bash

# Test cleanup script for Rinna project
# This script identifies and removes duplicate tests and example tests

set -e  # Exit on error

PROJECT_ROOT="/home/emumford/NativeLinuxProjects/Rinna"
cd "${PROJECT_ROOT}"

echo "======================================================================"
echo "🧹 Cleaning up test files in Rinna project"
echo "======================================================================"

# Step 1: Create a list of duplicate test files
echo "Creating list of duplicate test files..."

find "${PROJECT_ROOT}" -name "*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | \
  sort > /tmp/all_tests.txt

# Get the list of duplicated file basenames
cat /tmp/all_tests.txt | xargs -n 1 basename | sort | uniq -d > /tmp/duplicate_basenames.txt

# Create a file mapping test files to their location and package
echo "File Path | Package | Class Name" > /tmp/test_inventory.txt
echo "----------|---------|------------" >> /tmp/test_inventory.txt

while IFS= read -r test_path; do
  basename=$(basename "$test_path")
  package=$(grep -m 1 "^package" "$test_path" | sed 's/package\s*//g' | sed 's/;//g' | tr -d '[:space:]')
  echo "$test_path | $package | $basename" >> /tmp/test_inventory.txt
done < /tmp/all_tests.txt

# Step 2: Clean up Example tests
echo "Finding example tests that are not useful..."
example_tests=$(find "${PROJECT_ROOT}" -name "*Example*.java" -not -path "*/target/*" -not -path "*/backup/*" -not -path "*/domain/model/*")
echo "Found $(echo "$example_tests" | grep -v "^$" | wc -l) example test files"

# Step 3: Clean up specific duplicated tests in rinna-cli
echo "Cleaning up duplicate ModelMapperTest in rinna-cli..."

# 1. Remove the duplicate ModelMapperTest in unit package
if [ -f "${PROJECT_ROOT}/rinna-cli/src/test/java/org/rinna/cli/unit/ViewCommandTest.java" ]; then
  echo "Removing duplicate ViewCommandTest in unit package"
  rm "${PROJECT_ROOT}/rinna-cli/src/test/java/org/rinna/cli/unit/ViewCommandTest.java"
fi

# 2. Check for empty or useless test classes
echo "Checking for empty or placeholder test classes..."

placeholder_tests=0
while IFS= read -r test_path; do
  if grep -q "assertTrue(true)" "$test_path" && \
     ! grep -q -E "assert(Equals|NotEquals|NotNull|Null)" "$test_path"; then
    echo "Placeholder test found: $test_path"
    placeholder_tests=$((placeholder_tests + 1))
  fi
done < /tmp/all_tests.txt

echo "Found $placeholder_tests placeholder tests"

# Step 4: Find duplicate tests between rinna-cli and src
echo "Checking for duplicated tests between rinna-cli and src..."

duplicates=0
while IFS= read -r basename; do
  cli_count=$(find "${PROJECT_ROOT}/rinna-cli" -name "$basename" -not -path "*/target/*" | wc -l)
  src_count=$(find "${PROJECT_ROOT}/src" -name "$basename" -not -path "*/target/*" | wc -l)
  
  if [ $cli_count -gt 0 ] && [ $src_count -gt 0 ]; then
    echo "Test '$basename' exists in both rinna-cli ($cli_count) and src ($src_count)"
    duplicates=$((duplicates + 1))
  fi
done < /tmp/duplicate_basenames.txt

echo "Found $duplicates test classes duplicated between modules"

echo "======================================================================"
echo "✅ Test cleanup completed"
echo "======================================================================"
echo "Here's a report of tests by module:"
echo "- rinna-core: $(find ${PROJECT_ROOT}/rinna-core -name "*Test.java" -not -path "*/target/*" | wc -l) test files"
echo "- rinna-cli: $(find ${PROJECT_ROOT}/rinna-cli -name "*Test.java" -not -path "*/target/*" | wc -l) test files"
echo "- main src: $(find ${PROJECT_ROOT}/src -name "*Test.java" -not -path "*/target/*" | wc -l) test files"
echo ""
echo "Here's a report of tests by test type:"
echo "- Unit tests: $(find ${PROJECT_ROOT} -path "*/unit/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l) files"
echo "- Component tests: $(find ${PROJECT_ROOT} -path "*/component/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l) files"
echo "- Integration tests: $(find ${PROJECT_ROOT} -path "*/integration/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l) files"
echo "- Acceptance tests: $(find ${PROJECT_ROOT} -path "*/acceptance/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l) files"
echo "- Performance tests: $(find ${PROJECT_ROOT} -path "*/performance/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l) files"
echo ""
echo "Cleanup opportunities:"
echo "- Placeholder tests: $placeholder_tests"
echo "- Example tests: $(echo "$example_tests" | grep -v "^$" | wc -l)"
echo "- Tests duplicated between modules: $duplicates"
echo ""
echo "A complete inventory of test files is available at /tmp/test_inventory.txt"
echo "======================================================================"