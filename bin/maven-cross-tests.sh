#!/bin/bash
#
# maven-cross-tests.sh - Run cross-language tests from Maven
#
# This script uses standardized formatting and reporting for consistent output
# across all build and test scripts.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# Source common utilities
source "$SCRIPT_DIR/common/rinna_utils.sh"
source "$SCRIPT_DIR/formatters/build_formatter.sh"

# Set up the environment
section_header "Cross-Language Test Environment"

# Load environment if available
if [ -f "./activate-rinna.sh" ]; then
  run_formatted "source ./activate-rinna.sh" "Loading Rinna environment"
fi

# Print environment information
echo "Environment Information:"
echo "- Using Java: $(java -version 2>&1 | head -1)"
echo "- Using Go: $(go version 2>&1 || echo "Go not found")"
echo "- Using Python: $(python --version 2>&1 || echo "Python not found")"
echo "- Working directory: $(pwd)"

# Show test execution plan
TEST_STEPS=(
  "Run polyglot tests using the cross-language test harness"
  "Run C4 diagram tests"
  "Generate C4 diagrams for documentation"
  "Verify CLI commands"
)
show_execution_plan "Cross-Language Test Steps" "${TEST_STEPS[@]}"

# Check if in CI environment
CI_MODE=false
if [ -n "$CI" ] || [ -n "$GITHUB_ACTIONS" ] || [ -n "$JENKINS_URL" ]; then
  CI_MODE=true
fi

# Run the polyglot tests
section_header "Running Polyglot Tests"
if [ "$CI_MODE" = true ]; then
  run_formatted "./bin/run-polyglot-tests.sh --verbose --build" "Running polyglot tests in CI environment"
else
  run_formatted "./bin/run-polyglot-tests.sh" "Running polyglot tests in standard environment"
fi

# Run C4 diagram tests
section_header "Running C4 Diagram Tests"
if [ -f "./bin/test_c4_diagrams.py" ]; then
  run_formatted "python -m unittest bin/test_c4_diagrams.py" "Running C4 diagram tests"
else
  skip_task "C4 diagram tests not found, skipping"
fi

# Generate C4 diagrams
section_header "Generating Documentation Diagrams"
if [ -f "./bin/generate-diagrams.sh" ]; then
  # Use async mode in CI environment, synchronous otherwise
  if [ "$CI_MODE" = true ]; then
    run_formatted "./bin/generate-diagrams.sh --async --clean" "Generating diagrams asynchronously (CI mode)"
  else
    run_formatted "./bin/generate-diagrams.sh" "Generating diagrams"
  fi
else
  skip_task "C4 diagram generator not found"
fi

# Verify CLI commands
section_header "Verifying CLI Commands"

# Test 'add' command
run_formatted "./bin/rin-add --title 'Test work item from cross-language test' --type TASK --priority HIGH --project test-project" "Testing 'add' command"

# Test 'list' command
run_formatted "./bin/rin-list --limit 5" "Testing 'list' command"

# Summarize results
section_header "Cross-Language Test Summary"
complete_task "Cross-language tests completed successfully"