#!/bin/bash
# Script to run Python tests for Rinna project

set -e

cd "$(dirname "$0")/.."
PYTHONPATH="$PWD:$PYTHONPATH"

# Default flags
COVERAGE=0
TEST_LEVEL="all"
LINT=0
FIX_LINT=0
FORMAT_CODE=0
TYPE_CHECK=0
TEST_ONLY=0
TEST_ARGS=""

show_help() {
  echo "Run Python Tests for Rinna"
  echo
  echo "Usage: $0 [options]"
  echo
  echo "Options:"
  echo "  -h, --help           Show this help message"
  echo "  -c, --coverage       Run tests with coverage"
  echo "  -u, --unit           Run only unit tests"
  echo "  -C, --component      Run only component tests"
  echo "  -i, --integration    Run only integration tests"
  echo "  -a, --acceptance     Run only acceptance tests"
  echo "  -p, --performance    Run only performance tests"
  echo "  -l, --lint           Run linting with pylint"
  echo "  --fix-lint           Fix linting issues automatically where possible"
  echo "  -f, --format         Format code with black and isort"
  echo "  -t, --type-check     Run type checking with mypy"
  echo "  -T, --test-only      Run only tests, no linting or formatting"
  echo "  -v, --verbose        Show verbose test output"
  echo
  echo "Examples:"
  echo "  $0 -c -u              Run unit tests with coverage"
  echo "  $0 -l -f              Run linting and format code"
  echo "  $0 --all              Run all tests and checks"
}

while [[ $# -gt 0 ]]; do
  case $1 in
    -h|--help)
      show_help
      exit 0
      ;;
    -c|--coverage)
      COVERAGE=1
      shift
      ;;
    -u|--unit)
      TEST_LEVEL="unit"
      shift
      ;;
    -C|--component)
      TEST_LEVEL="component"
      shift
      ;;
    -i|--integration)
      TEST_LEVEL="integration"
      shift
      ;;
    -a|--acceptance)
      TEST_LEVEL="acceptance"
      shift
      ;;
    -p|--performance)
      TEST_LEVEL="performance"
      shift
      ;;
    -l|--lint)
      LINT=1
      shift
      ;;
    --fix-lint)
      LINT=1
      FIX_LINT=1
      shift
      ;;
    -f|--format)
      FORMAT_CODE=1
      shift
      ;;
    -t|--type-check)
      TYPE_CHECK=1
      shift
      ;;
    -T|--test-only)
      TEST_ONLY=1
      LINT=0
      FORMAT_CODE=0
      TYPE_CHECK=0
      shift
      ;;
    -v|--verbose)
      TEST_ARGS="${TEST_ARGS} -v"
      shift
      ;;
    --all)
      COVERAGE=1
      TEST_LEVEL="all"
      LINT=1
      FORMAT_CODE=1
      TYPE_CHECK=1
      shift
      ;;
    *)
      echo "Unknown option: $1"
      show_help
      exit 1
      ;;
  esac
done

# Check if we are in a Python environment
if ! command -v python3 &> /dev/null; then
  echo "Python 3 not found. Please install Python 3."
  exit 1
fi

# Make sure Poetry is installed
if ! command -v poetry &> /dev/null; then
  echo "Poetry not found. Installing poetry..."
  curl -sSL https://install.python-poetry.org | python3 -
fi

cd python

# Install dependencies
echo "Installing dependencies with Poetry..."
poetry install

# Run formatting if requested
if [ $FORMAT_CODE -eq 1 ]; then
  echo "Formatting code with Black and isort..."
  poetry run black .
  poetry run isort .
fi

# Run linting if requested
if [ $LINT -eq 1 ]; then
  echo "Running pylint..."
  if [ $FIX_LINT -eq 1 ]; then
    # Run pylint with auto-fix options
    poetry run pylint --output-format=colorized --disable=all --enable=C0303,C0304,C0305,C0301,C0321,C0325,C0326,C0327,C0328 --fix rinna
    poetry run pylint --output-format=colorized rinna
  else
    poetry run pylint --output-format=colorized rinna
  fi
fi

# Run type checking if requested
if [ $TYPE_CHECK -eq 1 ]; then
  echo "Running mypy type checking..."
  poetry run mypy rinna
fi

# Run tests with appropriate flags
if [ $TEST_ONLY -eq 1 ] || [ $TEST_LEVEL != "none" ]; then
  echo "Running tests..."

  # Prepare test command
  TEST_CMD="poetry run pytest"
  
  # Add coverage flag if requested
  if [ $COVERAGE -eq 1 ]; then
    TEST_CMD="${TEST_CMD} --cov=rinna --cov-report=term --cov-report=xml:../../coverage/python-unit-coverage.xml"
  fi

  # Add test level filters
  if [ "$TEST_LEVEL" != "all" ]; then
    TEST_CMD="${TEST_CMD} -m $TEST_LEVEL"
  fi

  # Add any additional args
  TEST_CMD="${TEST_CMD} ${TEST_ARGS}"

  # Run the command
  echo "Running command: $TEST_CMD"
  eval "$TEST_CMD"
fi

echo "Python tests and checks completed."