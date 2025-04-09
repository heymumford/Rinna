#!/usr/bin/env bash
#
# test-python-pyramid.sh - Run Python tests in the test pyramid structure
#
# PURPOSE: Set up Python environment and run tests organized by test pyramid level
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -eo pipefail

# Determine project directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PYTHON_DIR="$PROJECT_ROOT/python"
TESTS_DIR="$PYTHON_DIR/tests"

# Source common utilities if available
if [[ -f "$SCRIPT_DIR/common/rinna_utils.sh" ]]; then
  source "$SCRIPT_DIR/common/rinna_utils.sh"
fi

# Colors and formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Configuration variables
VERBOSE=false
INSTALL_FIRST=true
FORCE_REINSTALL=false
TEST_LEVEL="all"
OUTPUT_FORMAT="text"
PYTHON_CMD=""
VENV_DIR="$PROJECT_ROOT/.venv"
SKIP_QUALITY=false
COVERAGE=true
LOG_LEVEL="info"
ISOLATED_MODE=false  # For container testing

# Display help text
show_help() {
    cat <<EOF
${BLUE}test-python-pyramid.sh${NC} - Python Test Pyramid Runner for Rinna

Usage: test-python-pyramid.sh [options] [test-level]

Test Levels:
  all               Run all tests (default)
  unit              Run only unit tests
  component         Run component tests
  integration       Run integration tests
  acceptance        Run acceptance tests
  performance       Run performance tests

Options:
  -h, --help            Show this help message
  -v, --verbose         Show detailed output
  --no-install          Skip package installation
  --force-reinstall     Force reinstall packages
  --skip-quality        Skip quality checks
  --no-coverage         Don't run coverage
  --output=FORMAT       Output format: text, json, junit
  --log-level=LEVEL     Set log level: debug, info, warning, error
  --isolated            Run in isolated mode (for containers)
  --venv=DIR            Use specific virtual environment directory
  
Environment Setup Options:
  --system-python       Use system Python rather than venv
  --python=PATH         Use specific Python executable
  
Examples:
  ./bin/test-python-pyramid.sh unit      # Run only unit tests
  ./bin/test-python-pyramid.sh --verbose integration  # Run integration tests with verbose output
  ./bin/test-python-pyramid.sh --isolated --no-install # For container environments
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      show_help
      exit 0
      ;;
    -v|--verbose)
      VERBOSE=true
      shift
      ;;
    --no-install)
      INSTALL_FIRST=false
      shift
      ;;
    --force-reinstall)
      FORCE_REINSTALL=true
      INSTALL_FIRST=true
      shift
      ;;
    --skip-quality)
      SKIP_QUALITY=true
      shift
      ;;
    --no-coverage)
      COVERAGE=false
      shift
      ;;
    --output=*)
      OUTPUT_FORMAT="${1#*=}"
      shift
      ;;
    --log-level=*)
      LOG_LEVEL="${1#*=}"
      shift
      ;;
    --isolated)
      ISOLATED_MODE=true
      shift
      ;;
    --venv=*)
      VENV_DIR="${1#*=}"
      shift
      ;;
    --system-python)
      VENV_DIR=""
      shift
      ;;
    --python=*)
      PYTHON_CMD="${1#*=}"
      shift
      ;;
    unit|component|integration|acceptance|performance)
      TEST_LEVEL="$1"
      shift
      ;;
    all)
      TEST_LEVEL="all"
      shift
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      show_help
      exit 1
      ;;
  esac
done

# Log a message with formatting
log() {
  local level="$1"
  local message="$2"
  local show=false
  
  case "$level" in
    debug)
      [[ "$LOG_LEVEL" == "debug" ]] && show=true
      prefix="${CYAN}DEBUG${NC}"
      ;;
    info)
      [[ "$LOG_LEVEL" == "debug" || "$LOG_LEVEL" == "info" ]] && show=true
      prefix="${BLUE}INFO${NC}"
      ;;
    warning)
      [[ "$LOG_LEVEL" == "debug" || "$LOG_LEVEL" == "info" || "$LOG_LEVEL" == "warning" ]] && show=true
      prefix="${YELLOW}WARNING${NC}"
      ;;
    error)
      show=true
      prefix="${RED}ERROR${NC}"
      ;;
    success)
      show=true
      prefix="${GREEN}SUCCESS${NC}"
      ;;
    *)
      show=true
      prefix=""
      ;;
  esac
  
  if [[ "$show" == "true" ]]; then
    echo -e "[${prefix}] $message"
  fi
}

# Setup Python environment
setup_python() {
  log "info" "Setting up Python environment..."
  
  # Determine Python command
  if [[ -z "$PYTHON_CMD" ]]; then
    if [[ -n "$VENV_DIR" && -f "$VENV_DIR/bin/python" ]]; then
      PYTHON_CMD="$VENV_DIR/bin/python"
      log "debug" "Using virtual environment Python: $PYTHON_CMD"
    elif command -v python3 &> /dev/null; then
      PYTHON_CMD="python3"
      log "debug" "Using system Python3: $PYTHON_CMD"
    elif command -v python &> /dev/null; then
      PYTHON_CMD="python"
      log "debug" "Using system Python: $PYTHON_CMD"
    else
      log "error" "No Python executable found!"
      exit 1
    fi
  fi
  
  # Verify Python version
  PYTHON_VERSION=$("$PYTHON_CMD" --version 2>&1)
  log "info" "Using $PYTHON_VERSION"
  
  # Determine pip command
  if [[ -n "$VENV_DIR" && -f "$VENV_DIR/bin/pip" ]]; then
    PIP_CMD="$VENV_DIR/bin/pip"
  else
    if command -v pip3 &> /dev/null; then
      PIP_CMD="pip3"
    elif command -v pip &> /dev/null; then
      PIP_CMD="pip"
    else
      log "error" "No pip executable found!"
      exit 1
    fi
  fi
  
  log "debug" "Using pip: $PIP_CMD"
  
  # Activate virtual environment if needed and not in isolated mode
  if [[ -n "$VENV_DIR" && "$ISOLATED_MODE" != "true" ]]; then
    if [[ ! -f "$VENV_DIR/bin/activate" ]]; then
      log "warning" "Virtual environment not found at $VENV_DIR"
      
      if [[ "$INSTALL_FIRST" == "true" ]]; then
        log "info" "Creating virtual environment..."
        "$PYTHON_CMD" -m venv "$VENV_DIR"
      else
        log "error" "Cannot continue without virtual environment"
        exit 1
      fi
    fi
    
    log "debug" "Activating virtual environment"
    source "$VENV_DIR/bin/activate"
    PYTHON_CMD="python"
    PIP_CMD="pip"
  fi
  
  # Install test dependencies if needed
  if [[ "$INSTALL_FIRST" == "true" ]]; then
    log "info" "Installing test dependencies..."
    
    # Install pytest and other test requirements
    "$PIP_CMD" install --upgrade pip pytest pytest-cov > /dev/null
    
    if [[ "$OUTPUT_FORMAT" == "json" ]]; then
      "$PIP_CMD" install pytest-json-report > /dev/null
    fi
    
    if [[ "$SKIP_QUALITY" != "true" ]]; then
      "$PIP_CMD" install mypy ruff black > /dev/null
    fi
    
    # Install the package in development mode
    if [[ "$FORCE_REINSTALL" == "true" ]]; then
      log "info" "Force reinstalling Rinna packages..."
      
      # Remove any existing installations
      "$PIP_CMD" uninstall -y rinna lucidchart-py 2>/dev/null || true
      rm -rf "$PYTHON_DIR"/build/ "$PYTHON_DIR"/dist/ "$PYTHON_DIR"/*.egg-info/
      
      # Install Lucidchart mock package
      log "debug" "Installing lucidchart-py package..."
      cd "$PYTHON_DIR" && SETUP_PACKAGE=lucidchart-py "$PIP_CMD" install -e . > /dev/null
      
      # Install Rinna package
      log "debug" "Installing rinna package..."
      cd "$PYTHON_DIR" && SETUP_PACKAGE=rinna "$PIP_CMD" install -e . > /dev/null
    elif [[ ! "$("$PYTHON_CMD" -c "import importlib.util; print(importlib.util.find_spec('rinna') is not None)" 2>/dev/null)" == "True" ]]; then
      # Install only if not already installed
      log "info" "Installing Rinna packages..."
      
      # Install Lucidchart mock package
      log "debug" "Installing lucidchart-py package..."
      cd "$PYTHON_DIR" && SETUP_PACKAGE=lucidchart-py "$PIP_CMD" install -e . > /dev/null
      
      # Install Rinna package
      log "debug" "Installing rinna package..."
      cd "$PYTHON_DIR" && SETUP_PACKAGE=rinna "$PIP_CMD" install -e . > /dev/null
    else
      log "debug" "Rinna packages already installed"
    fi
  fi
  
  # Verify installations
  log "info" "Verifying Python packages..."
  if "$PYTHON_CMD" -c "import rinna; import lucidchart_py" 2>/dev/null; then
    log "success" "Python environment ready"
    return 0
  else
    log "error" "Failed to import required packages"
    return 1
  fi
}

# Run quality checks
run_quality_checks() {
  if [[ "$SKIP_QUALITY" == "true" ]]; then
    log "info" "Skipping quality checks..."
    return 0
  fi
  
  log "info" "Running Python quality checks..."
  cd "$PROJECT_ROOT"
  
  # Run type checking with mypy
  log "debug" "Running type checking..."
  "$PYTHON_CMD" -m mypy "$PYTHON_DIR/rinna" > /dev/null || {
    log "warning" "Type checking found issues"
  }
  
  # Run linting with ruff
  log "debug" "Running linting..."
  "$PYTHON_CMD" -m ruff check "$PYTHON_DIR/rinna" "$PYTHON_DIR/tests" > /dev/null || {
    log "warning" "Linting found issues"
  }
  
  log "success" "Quality checks completed"
  return 0
}

# Run tests by pyramid level
run_tests() {
  local level="$1"
  local pytest_args=""
  
  # Set up pytest arguments
  if [[ "$VERBOSE" == "true" ]]; then
    pytest_args="$pytest_args -v"
  fi
  
  # Configure output format
  case "$OUTPUT_FORMAT" in
    junit)
      mkdir -p "$PROJECT_ROOT/test-output"
      pytest_args="$pytest_args --junitxml=$PROJECT_ROOT/test-output/python-$level-report.xml"
      ;;
    json)
      mkdir -p "$PROJECT_ROOT/test-output"
      pytest_args="$pytest_args --json-report --json-report-file=$PROJECT_ROOT/test-output/python-$level-report.json"
      ;;
  esac
  
  # Configure coverage
  if [[ "$COVERAGE" == "true" ]]; then
    mkdir -p "$PROJECT_ROOT/coverage"
    pytest_args="$pytest_args --cov=python/rinna --cov-report=term-missing --cov-report=xml:$PROJECT_ROOT/coverage/python-$level-coverage.xml"
    
    # Create .coveragerc file to ensure proper coverage reporting
    cat > "$PROJECT_ROOT/.coveragerc" << EOF
[run]
source = python/rinna
omit = */tests/*,*/__pycache__/*,*/\.*
relative_files = True

[report]
exclude_lines =
    pragma: no cover
    def __repr__
    raise NotImplementedError
    if __name__ == .__main__.:
    pass
    raise ImportError
EOF
  fi
  
  # Determine test path and markers based on level
  local test_path="$TESTS_DIR"
  local marker=""
  
  case "$level" in
    unit)
      test_path="$TESTS_DIR/unit"
      marker="unit"
      ;;
    component)
      test_path="$TESTS_DIR/component"
      marker="component"
      ;;
    integration)
      test_path="$TESTS_DIR/integration"
      marker="integration"
      ;;
    acceptance)
      test_path="$TESTS_DIR/acceptance"
      marker="acceptance"
      ;;
    performance)
      test_path="$TESTS_DIR/performance"
      marker="performance"
      ;;
    all)
      # Use the main tests directory
      marker=""
      ;;
  esac
  
  if [[ -n "$marker" && "$level" != "all" ]]; then
    pytest_args="$pytest_args -m $marker"
  fi
  
  # Run the tests
  cd "$PROJECT_ROOT"
  log "info" "Running $level tests..."
  
  if [[ "$VERBOSE" == "true" ]]; then
    "$PYTHON_CMD" -m pytest $pytest_args "$test_path"
  else
    "$PYTHON_CMD" -m pytest $pytest_args "$test_path" || {
      log "error" "Tests failed!"
      return 1
    }
  fi
  
  log "success" "$level tests completed successfully"
  return 0
}

# Main function
main() {
  log "info" "Starting Python test pyramid runner"
  log "debug" "Test level: $TEST_LEVEL"
  
  # Setup Python environment
  setup_python || {
    log "error" "Failed to set up Python environment"
    exit 1
  }
  
  # Run quality checks
  run_quality_checks
  
  # Run tests based on the specified level
  case "$TEST_LEVEL" in
    all)
      run_tests "unit" &&
      run_tests "component" &&
      run_tests "integration" &&
      run_tests "acceptance" &&
      run_tests "performance"
      ;;
    *)
      run_tests "$TEST_LEVEL"
      ;;
  esac
  
  log "success" "Python test pyramid execution complete"
}

# Execute main function
main