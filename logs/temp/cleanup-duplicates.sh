#!/bin/bash

# Script to cleanup duplicate tests
# This script removes duplicate tests between modules

set -e  # Exit on error

PROJECT_ROOT="/home/emumford/NativeLinuxProjects/Rinna"
cd "${PROJECT_ROOT}"

echo "======================================================================"
echo "🧹 Cleaning up duplicate test files in Rinna project"
echo "======================================================================"

# Detect duplicated tests between rinna-cli and src
duplicate_tests=(
  "CliPerformanceTest.java"
  "CliServiceIntegrationTest.java"
  "ReportTemplateTest.java"
  "TemplateManagerTest.java"
  "WorkflowAcceptanceTest.java"
  "WorkflowCommandTest.java"
)

removed_count=0

for test in "${duplicate_tests[@]}"; do
  src_files=$(find "${PROJECT_ROOT}/src" -name "$test" -not -path "*/target/*")
  
  if [ -n "$src_files" ]; then
    echo "Removing duplicate test $test from src module:"
    echo "$src_files"
    
    while IFS= read -r file; do
      rm "$file"
      removed_count=$((removed_count + 1))
    done <<< "$src_files"
  fi
done

# Remove placeholder test
if [ -f "${PROJECT_ROOT}/rinna-cli/src/test/java/org/rinna/cli/report/ReportCommandTest.java" ]; then
  echo "Removing placeholder test: ReportCommandTest.java"
  rm "${PROJECT_ROOT}/rinna-cli/src/test/java/org/rinna/cli/report/ReportCommandTest.java"
  removed_count=$((removed_count + 1))
fi

echo "======================================================================"
echo "✅ Removed $removed_count duplicate or placeholder test files"
echo "======================================================================"
echo "Here's a report of tests by module:"
echo "- rinna-core: $(find ${PROJECT_ROOT}/rinna-core -name "*Test.java" -not -path "*/target/*" | wc -l) test files"
echo "- rinna-cli: $(find ${PROJECT_ROOT}/rinna-cli -name "*Test.java" -not -path "*/target/*" | wc -l) test files"
echo "- main src: $(find ${PROJECT_ROOT}/src -name "*Test.java" -not -path "*/target/*" | wc -l) test files"
echo "======================================================================"