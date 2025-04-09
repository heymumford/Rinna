#!/usr/bin/env bash
#
# run-python-container.sh - Run Python tests in a podman container
#
# PURPOSE: Execute Python tests in a containerized environment using podman
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
CONTAINER_ENGINE=""

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
REBUILD=false
TEST_LEVEL="all"
PROFILE="testing"
LOG_LEVEL="info"
CUSTOM_OPTIONS=""
IMAGE_TYPE="test"  # test, dev, or prod

# Display help text
show_help() {
    cat <<EOF
${BLUE}run-python-container.sh${NC} - Run Python tests in a container

Usage: run-python-container.sh [options] [test-level]

Test Levels:
  all               Run all tests (default)
  unit              Run only unit tests
  component         Run component tests
  integration       Run integration tests
  acceptance        Run acceptance tests
  performance       Run performance tests
  quality           Run only quality checks
  dev               Start a development container with shell access
  prod              Start a production service container
  install           Build container that installs the package

Options:
  -h, --help            Show this help message
  -v, --verbose         Show detailed output
  --rebuild             Force rebuild container image
  --docker              Use Docker instead of Podman
  --log-level=LEVEL     Set log level: debug, info, warning, error
  --options="OPTS"      Pass custom options to test command
  --tag=TAG             Use specific image tag (default: latest)
  --port=PORT           Port for service container (default: 5000)
  
Environment Types:
  --dev                 Use development environment
  --prod                Use production environment
  --test                Use testing environment (default)
  
Examples:
  ./bin/run-python-container.sh unit              # Run only unit tests
  ./bin/run-python-container.sh --rebuild integration  # Rebuild and run integration tests
  ./bin/run-python-container.sh dev               # Start a development container
  ./bin/run-python-container.sh --prod prod       # Start a production service container
  ./bin/run-python-container.sh --tag=v1.0 prod   # Start a production container with specific tag
EOF
}

# Check if podman is available, fallback to docker if needed or if explicitly requested
check_container_engine() {
    if [[ "$1" == "docker" ]]; then
        CONTAINER_ENGINE="docker"
    elif command -v podman &> /dev/null; then
        CONTAINER_ENGINE="podman"
    elif command -v docker &> /dev/null; then
        CONTAINER_ENGINE="docker"
    else
        echo -e "${RED}Error: Neither podman nor docker found. Please install one of them.${NC}"
        exit 1
    fi
    
    echo -e "${BLUE}Using container engine: ${CONTAINER_ENGINE}${NC}"
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
    --rebuild)
      REBUILD=true
      shift
      ;;
    --docker)
      check_container_engine "docker"
      shift
      ;;
    --log-level=*)
      LOG_LEVEL="${1#*=}"
      shift
      ;;
    --options=*)
      CUSTOM_OPTIONS="${1#*=}"
      shift
      ;;
    --tag=*)
      IMAGE_TAG="${1#*=}"
      shift
      ;;
    --port=*)
      PORT="${1#*=}"
      shift
      ;;
    --dev)
      IMAGE_TYPE="dev"
      shift
      ;;
    --prod)
      IMAGE_TYPE="prod"
      shift
      ;;
    --test)
      IMAGE_TYPE="test"
      shift
      ;;
    unit|component|integration|acceptance|performance|quality)
      TEST_LEVEL="$1"
      PROFILE="$1"
      shift
      ;;
    dev)
      PROFILE="dev"
      IMAGE_TYPE="dev"
      shift
      ;;
    prod)
      PROFILE="prod"
      IMAGE_TYPE="prod"
      shift
      ;;
    install)
      PROFILE="install"
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

# Check for container engine if not already determined
if [[ -z "$CONTAINER_ENGINE" ]]; then
    check_container_engine
fi

# Set user ID for container to match current user
export USER_ID=$(id -u)
export GROUP_ID=$(id -g)
export LOG_LEVEL="$LOG_LEVEL"
export TEST_LEVEL="$TEST_LEVEL"
export TESTOPTS="$CUSTOM_OPTIONS"

# Build or rebuild the container if needed
if [[ "$REBUILD" == "true" ]]; then
    echo -e "${BLUE}Rebuilding ${IMAGE_TYPE} container image...${NC}"
    "$SCRIPT_DIR/cache-python-image.sh" --type="$IMAGE_TYPE" build
else
    # Try to load from cache or update if needed
    if ! "$SCRIPT_DIR/cache-python-image.sh" --type="$IMAGE_TYPE" update; then
        echo -e "${BLUE}No cached image available. Building ${IMAGE_TYPE} container image...${NC}"
        "$SCRIPT_DIR/cache-python-image.sh" --type="$IMAGE_TYPE" build
    fi
fi

# Set environment variables for specific image types
if [[ "$IMAGE_TYPE" == "prod" ]]; then
    export PORT="${PORT:-5000}"
    export API_URL="${API_URL:-http://localhost:8080}"
fi

# Run the appropriate container
if [[ "$PROFILE" == "dev" ]]; then
    echo -e "${BLUE}Starting Python development container...${NC}"
    cd "$PYTHON_DIR" && $CONTAINER_ENGINE compose --profile dev run --rm python-dev
elif [[ "$PROFILE" == "install" ]]; then
    echo -e "${BLUE}Starting Python package installation container...${NC}"
    cd "$PYTHON_DIR" && $CONTAINER_ENGINE compose run --rm install-package
elif [[ "$PROFILE" == "prod" ]]; then
    echo -e "${BLUE}Starting Python production service on port $PORT...${NC}"
    cd "$PYTHON_DIR" && $CONTAINER_ENGINE compose --profile prod run --rm -p "$PORT:5000" python-service
else
    echo -e "${BLUE}Running Python $TEST_LEVEL tests in container...${NC}"
    if [[ "$TEST_LEVEL" == "all" ]]; then
        cd "$PYTHON_DIR" && $CONTAINER_ENGINE compose --profile testing run --rm python-tests
    else
        cd "$PYTHON_DIR" && $CONTAINER_ENGINE compose --profile "$PROFILE" run --rm "$TEST_LEVEL-tests"
    fi
fi