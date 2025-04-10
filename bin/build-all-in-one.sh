#!/usr/bin/env bash
#
# build-all-in-one.sh - Build the Rinna All-in-One container
#
# PURPOSE: Build a single container with all Rinna components for zero-install option
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
CONTAINER_ENGINE=""
IMAGE_NAME="heymumford/rinna"
IMAGE_TAG="latest"
PUSH=false

# Colors and formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Display help text
show_help() {
    cat <<EOF
${BLUE}build-all-in-one.sh${NC} - Build the Rinna All-in-One container

Usage: build-all-in-one.sh [options]

Options:
  -h, --help            Show this help message
  --docker              Use Docker instead of automatic detection
  --podman              Use Podman instead of automatic detection
  --tag=TAG             Image tag (default: latest)
  --name=NAME           Image name (default: heymumford/rinna)
  --push                Push image to registry after building

Examples:
  ./bin/build-all-in-one.sh              # Build with default options
  ./bin/build-all-in-one.sh --tag=v1.0.0 # Build with specific tag
  ./bin/build-all-in-one.sh --push       # Build and push to registry
EOF
}

# Check if podman is available, fallback to docker
check_container_engine() {
    if [[ "$1" == "docker" ]]; then
        CONTAINER_ENGINE="docker"
    elif [[ "$1" == "podman" ]]; then
        CONTAINER_ENGINE="podman"
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
    --docker)
      check_container_engine "docker"
      shift
      ;;
    --podman)
      check_container_engine "podman"
      shift
      ;;
    --tag=*)
      IMAGE_TAG="${1#*=}"
      shift
      ;;
    --name=*)
      IMAGE_NAME="${1#*=}"
      shift
      ;;
    --push)
      PUSH=true
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

# Show build configuration
echo -e "${BLUE}Building Rinna All-in-One container with:${NC}"
echo -e "${CYAN}  Container Engine: ${CONTAINER_ENGINE}${NC}"
echo -e "${CYAN}  Image Name: ${IMAGE_NAME}${NC}"
echo -e "${CYAN}  Image Tag: ${IMAGE_TAG}${NC}"
echo -e "${CYAN}  Push to Registry: ${PUSH}${NC}"
echo ""

# Build the container
echo -e "${BLUE}Building container image...${NC}"
$CONTAINER_ENGINE build \
    -t "${IMAGE_NAME}:${IMAGE_TAG}" \
    -f "${PROJECT_ROOT}/Dockerfile.all-in-one" \
    "${PROJECT_ROOT}"

echo -e "${GREEN}Build completed successfully${NC}"
echo -e "${GREEN}Image: ${IMAGE_NAME}:${IMAGE_TAG}${NC}"

# Push to registry if requested
if [[ "$PUSH" == "true" ]]; then
    echo -e "${BLUE}Pushing image to registry...${NC}"
    $CONTAINER_ENGINE push "${IMAGE_NAME}:${IMAGE_TAG}"
    echo -e "${GREEN}Push completed successfully${NC}"
fi

# Display usage instructions
echo -e "${BLUE}To use the All-in-One container:${NC}"
echo -e "${CYAN}  ${CONTAINER_ENGINE} run -d --name rinna-all-in-one -p 8080:8080 -p 8081:8081 -p 5000:5000 -v rinna-data:/app/shared ${IMAGE_NAME}:${IMAGE_TAG}${NC}"
echo ""
echo -e "${BLUE}Or use the universal container script:${NC}"
echo -e "${CYAN}  ./bin/rinna-container.sh --zero-install start${NC}"