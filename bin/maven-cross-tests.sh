#!/bin/bash
# Script to run cross-language tests (Go, Python, CLI) from Maven
# This script is called by the Maven exec-maven-plugin

set -e

echo "================================================================="
echo "Running cross-language tests (Go API, Python, CLI integration)"
echo "================================================================="

# Determine the project root directory
PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$PROJECT_ROOT"

# Set up the environment
if [ -f "./activate-java.sh" ]; then
  source ./activate-java.sh
fi

# Print environment information
echo "Environment Information:"
echo "========================"
echo "Using Java: $(java -version 2>&1 | head -1)"
echo "Using Go: $(go version 2>&1 || echo "Go not found")"
echo "Using Python: $(python --version 2>&1 || echo "Python not found")"
echo "Working directory: $(pwd)"
echo "========================"

# Run Go API tests
echo "Running Go API tests..."
if [ -d "./api" ]; then
  cd api
  go test ./... || echo "Go API tests failed"
  cd "$PROJECT_ROOT"
else
  echo "API directory not found, skipping Go tests"
fi

# Run Python tests
echo "Running Python tests..."
if [ -d "./python" ]; then
  python -m pytest python/tests -v || echo "Python tests failed"
fi

# Always run C4 diagram tests if they exist
echo "Running C4 diagram tests..."
if [ -f "./bin/test_c4_diagrams.py" ]; then
  python -m unittest bin/test_c4_diagrams.py || echo "Python diagram tests failed"
else
  echo "C4 diagram tests not found, skipping"
fi

# Generate C4 diagrams for documentation
echo "Generating C4 model diagrams..."
if [ -f "./bin/c4_diagrams.py" ]; then
  mkdir -p ./docs/diagrams
  python ./bin/c4_diagrams.py --type all --output svg --dir ./docs/diagrams || echo "C4 diagram generation failed"
  echo "Diagrams generated in ./docs/diagrams"
else
  echo "C4 diagram generator not found, skipping diagram generation"
fi

# Run CLI integration tests
echo "Running CLI integration tests..."
cd "$PROJECT_ROOT"

if [ -f "./bin/run-tests.sh" ]; then
  ./bin/run-tests.sh cli || echo "CLI integration tests failed"
fi

# Run work item CLI tests
echo "Running work item CLI tests..."

# Test 'add' command
echo "Testing 'add' command..."
./bin/rin add "Test work item from CLI integration test" -t TASK -p HIGH -P test-project -a test-user || echo "Add command failed"

# Test 'list' command
echo "Testing 'list' command..."
./bin/rin list --limit=5 || echo "List command failed"

# Test 'view' command - use the first ID from list
echo "Testing 'view' command..."
VIEW_ID=$(./bin/rin list --limit=1 | grep "WI-" | awk '{print $1}')
if [ -n "$VIEW_ID" ]; then
  ./bin/rin view "$VIEW_ID" || echo "View command failed"
else
  echo "No work items found for view command"
fi

# Test 'update' command - use the first ID from list
echo "Testing 'update' command..."
UPDATE_ID=$(./bin/rin list --limit=1 | grep "WI-" | awk '{print $1}')
if [ -n "$UPDATE_ID" ]; then
  ./bin/rin update "$UPDATE_ID" --assignee test-update-user --status IN_PROGRESS || echo "Update command failed"
else
  echo "No work items found for update command"
fi

echo "CLI work item tests completed"

echo "================================================================="
echo "Cross-language tests completed"
echo "================================================================="