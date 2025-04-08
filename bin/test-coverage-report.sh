#!/bin/bash

# Script to generate and display test coverage information for Rinna project
# This script uses JaCoCo to analyze test coverage and provide a summary report

set -e  # Exit on error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${PROJECT_ROOT}"

echo "======================================================================"
echo "🔍 Generating test coverage report for Rinna project"
echo "======================================================================"

# List of modules to analyze
MODULES=("rinna-core" "rinna-cli")

# Test counts by type
unit_tests=0
component_tests=0
integration_tests=0
acceptance_tests=0
performance_tests=0

# Generate a summary of tests by category
generate_test_summary() {
  echo "Analyzing test files in the project..."
  # Find test files by category
  unit_tests=$(find "${PROJECT_ROOT}" -path "*/unit/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l)
  component_tests=$(find "${PROJECT_ROOT}" -path "*/component/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l)
  integration_tests=$(find "${PROJECT_ROOT}" -path "*/integration/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l)
  acceptance_tests=$(find "${PROJECT_ROOT}" -path "*/acceptance/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l)
  performance_tests=$(find "${PROJECT_ROOT}" -path "*/performance/*Test.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l)
  
  # Count test runners for BDD tests
  bdd_runners=$(find "${PROJECT_ROOT}" -name "*Runner.java" -not -path "*/target/*" -not -path "*/backup/*" -not -path "*Example*" | wc -l)
  bdd_steps=$(find "${PROJECT_ROOT}" -name "*Steps.java" -not -path "*/target/*" -not -path "*/backup/*" | wc -l)

  echo "======================================================================"
  echo "📊 Test Distribution Report"
  echo "======================================================================"
  echo "Unit tests: ${unit_tests}"
  echo "Component tests: ${component_tests}"
  echo "Integration tests: ${integration_tests}"
  echo "Acceptance tests: ${acceptance_tests}"
  echo "Performance tests: ${performance_tests}"
  echo "BDD test runners: ${bdd_runners}"
  echo "BDD test step files: ${bdd_steps}"
  echo ""
  
  total_tests=$((unit_tests + component_tests + integration_tests + acceptance_tests + performance_tests))
  echo "Total test files: ${total_tests}"
  echo ""
  
  # Display test pyramid ratio
  echo "Test Pyramid Ratio:"
  echo "Unit : Component : Integration : Acceptance : Performance"
  if [ $total_tests -gt 0 ]; then
    unit_ratio=$(echo "scale=2; $unit_tests * 100 / $total_tests" | bc)
    comp_ratio=$(echo "scale=2; $component_tests * 100 / $total_tests" | bc)
    integ_ratio=$(echo "scale=2; $integration_tests * 100 / $total_tests" | bc)
    accept_ratio=$(echo "scale=2; $acceptance_tests * 100 / $total_tests" | bc)
    perf_ratio=$(echo "scale=2; $performance_tests * 100 / $total_tests" | bc)
    
    echo "${unit_ratio}% : ${comp_ratio}% : ${integ_ratio}% : ${accept_ratio}% : ${perf_ratio}%"
  else
    echo "0% : 0% : 0% : 0% : 0%"
  fi
  echo ""
}

# Run tests in each module and compile test coverage stats
for module in "${MODULES[@]}"; do
  if [ -d "${PROJECT_ROOT}/${module}" ]; then
    echo "--------------------------------------------------------------------"
    echo "Running tests in ${module}..."
    cd "${PROJECT_ROOT}/${module}"
    
    # Count Java files in module (source files)
    src_files=$(find . -path "./src/main/java/*" -name "*.java" | wc -l)
    echo "Source files in ${module}: ${src_files}"
    
    # Count test files in module
    test_files=$(find . -path "./src/test/java/*" -name "*.java" | wc -l)
    echo "Test files in ${module}: ${test_files}"
    
    # Calculate test ratio
    if [ $src_files -gt 0 ]; then
      ratio=$(echo "scale=2; $test_files / $src_files" | bc)
      echo "Test-to-source ratio: ${ratio}"
    else
      echo "Test-to-source ratio: N/A (no source files)"
    fi
    
    # Get file-level coverage information
    echo ""
    echo "File coverage information (total files with tests):"
    
    # Find source files that have corresponding test files
    src_with_tests=0
    for src_file in $(find ./src/main/java -name "*.java"); do
      # Extract class name
      class_name=$(basename "$src_file" .java)
      # Check if there's a test file for this class
      if find ./src/test -name "${class_name}Test.java" | grep -q .; then
        src_with_tests=$((src_with_tests + 1))
      fi
    done
    
    # Calculate file coverage percentage
    if [ $src_files -gt 0 ]; then
      file_coverage=$(echo "scale=2; $src_with_tests * 100 / $src_files" | bc)
      echo "Files with tests: ${src_with_tests}/${src_files} (${file_coverage}%)"
    else
      echo "Files with tests: 0/0 (0%)"
    fi
    
    echo ""
  else
    echo "Module ${module} not found, skipping..."
  fi
done

# Generate overall test summary
generate_test_summary

echo "======================================================================"
echo "✅ Test coverage report completed"
echo "======================================================================"