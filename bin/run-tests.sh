#!/bin/bash
# Test runner script for Rinna BDD tests

set -e

function show_help {
  echo "Usage: $0 [options] [test_category]"
  echo 
  echo "Options:"
  echo "  -h, --help          Show this help message"
  echo "  -v, --verbose       Enable verbose output"
  echo "  -p, --parallel      Enable parallel test execution"
  echo
  echo "Test Categories:"
  echo "  all                 Run all tests (default)"
  echo "  unit                Run only unit tests"
  echo "  bdd                 Run all BDD tests"
  echo "  workflow            Run workflow BDD tests"
  echo "  release             Run release BDD tests"
  echo "  input               Run input interface BDD tests"
  echo "  api                 Run API integration BDD tests"
  echo "  cli                 Run CLI integration BDD tests"
  echo "  tag:<tagname>       Run tests with specific tag (e.g., tag:json-api)"
  echo
  echo "Examples:"
  echo "  $0 all              # Run all tests"
  echo "  $0 unit             # Run only unit tests"
  echo "  $0 bdd              # Run all BDD tests"
  echo "  $0 api              # Run API integration tests"
  echo "  $0 tag:client       # Run tests tagged with @client"
  echo "  $0 -p workflow      # Run workflow tests with parallel execution"
}

# Default values
VERBOSE=false
PARALLEL=false
CATEGORY="all"

# Parse options
while [[ $# -gt 0 ]]; do
  case $1 in
    -h|--help)
      show_help
      exit 0
      ;;
    -v|--verbose)
      VERBOSE=true
      shift
      ;;
    -p|--parallel)
      PARALLEL=true
      shift
      ;;
    *)
      CATEGORY=$1
      shift
      ;;
  esac
done

# Set Maven options
MVN_OPTS=""
if [ "$VERBOSE" = true ]; then
  MVN_OPTS="$MVN_OPTS -X"
fi

if [ "$PARALLEL" = true ]; then
  MVN_OPTS="$MVN_OPTS -Dcucumber.execution.parallel.enabled=true"
else
  MVN_OPTS="$MVN_OPTS -Dcucumber.execution.parallel.enabled=false"
fi

# Execute tests based on category
case $CATEGORY in
  all)
    echo "Running all tests..."
    mvn clean test $MVN_OPTS
    ;;
  unit)
    echo "Running unit tests..."
    mvn clean test -Dtest='*Test,!*Runner' $MVN_OPTS
    ;;
  bdd)
    echo "Running all BDD tests..."
    mvn clean test -Dtest='org.rinna.model.DefaultWorkItemTest,org.rinna.service.impl.DefaultWorkflowServiceTest,org.rinna.RinnaTest,org.rinna.domain.usecase.ReleaseServiceTest,org.rinna.CleanArchitectureTest,org.rinna.service.impl.InMemoryItemServiceTest' $MVN_OPTS
    ;;
  workflow)
    echo "Running workflow BDD tests..."
    mvn clean test -Dtest='org.rinna.bdd.CucumberRunner' -Dcucumber.filter.tags='@workflow or not @workflow-disabled' $MVN_OPTS
    ;;
  release)
    echo "Running release BDD tests..."
    mvn clean test -Dtest='org.rinna.bdd.ReleaseRunner' $MVN_OPTS
    ;;
  input)
    echo "Running input interface BDD tests..."
    mvn clean test -Dtest='org.rinna.bdd.InputInterfaceRunner' $MVN_OPTS
    ;;
  api)
    echo "Running API integration tests..."
    mvn clean test -Dtest='org.rinna.model.DefaultWorkItemTest' $MVN_OPTS
    ;;
  cli)
    echo "Running CLI integration tests..."
    mvn clean test -Dtest='org.rinna.bdd.CLIIntegrationRunner' $MVN_OPTS
    ;;
  tag:*)
    TAG=${CATEGORY#tag:}
    echo "Running tests with tag @$TAG..."
    mvn clean test -Dtest='org.rinna.bdd.TaggedTestsRunner' -Dcucumber.filter.tags="@$TAG" $MVN_OPTS
    ;;
  *)
    echo "Unknown test category: $CATEGORY"
    show_help
    exit 1
    ;;
esac

echo "Test execution complete!"