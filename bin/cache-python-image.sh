#!/usr/bin/env bash
#
# cache-python-image.sh - Save and load Docker/Podman images to local cache
#
# PURPOSE: Manage cached Docker/Podman images for faster builds
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
CACHE_DIR="$PROJECT_ROOT/docker-cache"
CONTAINER_ENGINE=""

# Ensure cache directory exists
mkdir -p "$CACHE_DIR"

# Configuration variables
IMAGE_NAME="rinna-python-tests"
IMAGE_TAG="latest"
IMAGE_TYPE="test"  # test, dev, or prod
TARGET_STAGE="test"  # test, development, or production
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"
CACHE_FILE="$CACHE_DIR/${IMAGE_NAME}-${IMAGE_TAG}.tar"
CHECKSUM_FILE="$CACHE_DIR/${IMAGE_NAME}-${IMAGE_TAG}.md5"
DOCKERFILE="python/Dockerfile"
ACTION=""
VERBOSE=false

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
${BLUE}cache-python-image.sh${NC} - Save and load Docker/Podman images

Usage: cache-python-image.sh [options] ACTION

Actions:
  save              Save current image to cache
  load              Load image from cache
  update            Update cache if image has changed
  build             Build image and update cache

Options:
  -h, --help            Show this help message
  -v, --verbose         Show detailed output
  --docker              Use Docker instead of Podman
  --name=NAME           Image name (default: rinna-python-tests)
  --tag=TAG             Image tag (default: latest)
  --type=TYPE           Image type: test, dev, prod (default: test)
  --dockerfile=PATH     Path to Dockerfile (default: python/Dockerfile)
  --target=STAGE        Target stage: test, development, production (default: test)
  
Examples:
  ./bin/cache-python-image.sh save                   # Save test image to cache
  ./bin/cache-python-image.sh load                   # Load test image from cache
  ./bin/cache-python-image.sh build                  # Build test image and save to cache
  ./bin/cache-python-image.sh --type=dev build       # Build dev image and save to cache
  ./bin/cache-python-image.sh --type=prod build      # Build production image and save to cache
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
    
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}Using container engine: ${CONTAINER_ENGINE}${NC}"
    fi
}

# Get hash of Dockerfile and related files to determine if rebuild is needed
get_build_hash() {
    (
        cat "$PROJECT_ROOT/$DOCKERFILE" 
        [[ -f "$PROJECT_ROOT/python/Dockerfile.prod" ]] && cat "$PROJECT_ROOT/python/Dockerfile.prod"
        cat "$PROJECT_ROOT/requirements.txt" 
        cat "$PROJECT_ROOT/requirements-core.txt"
        find "$PROJECT_ROOT/bin" -name "*python*.sh" -type f -exec cat {} \;
        find "$PROJECT_ROOT/python/rinna" -type f -name "*.py" -exec cat {} \;
    ) | md5sum | awk '{print $1}'
}

# Save image to cache
save_image() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}Saving image ${FULL_IMAGE_NAME} to cache...${NC}"
    fi
    
    # Check if image exists
    if ! $CONTAINER_ENGINE images -q "$FULL_IMAGE_NAME" &>/dev/null; then
        echo -e "${RED}Error: Image ${FULL_IMAGE_NAME} not found${NC}"
        exit 1
    fi
    
    # Save image
    $CONTAINER_ENGINE save -o "$CACHE_FILE" "$FULL_IMAGE_NAME"
    
    # Save build hash
    get_build_hash > "$CHECKSUM_FILE"
    
    echo -e "${GREEN}Image ${FULL_IMAGE_NAME} saved to cache${NC}"
}

# Load image from cache
load_image() {
    if [[ ! -f "$CACHE_FILE" ]]; then
        echo -e "${YELLOW}Cache file not found. Build a new image with:${NC}"
        echo -e "  $0 build"
        return 1
    fi
    
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}Loading image ${FULL_IMAGE_NAME} from cache...${NC}"
    fi
    
    # Load image
    $CONTAINER_ENGINE load -i "$CACHE_FILE"
    
    echo -e "${GREEN}Image ${FULL_IMAGE_NAME} loaded from cache${NC}"
    return 0
}

# Build image and update cache
build_image() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}Building image ${FULL_IMAGE_NAME}...${NC}"
    fi
    
    # Determine which target to build
    local target_args=""
    if [[ -n "$TARGET_STAGE" ]]; then
        target_args="--target $TARGET_STAGE"
    fi
    
    # Build image
    if [[ "$DOCKERFILE" == "python/Dockerfile.prod" && "$TARGET_STAGE" == "production" ]]; then
        cd "$PROJECT_ROOT/python" && $CONTAINER_ENGINE compose --profile prod build python-service
    elif [[ "$TARGET_STAGE" == "development" ]]; then
        cd "$PROJECT_ROOT/python" && $CONTAINER_ENGINE compose --profile dev build python-dev
    else
        cd "$PROJECT_ROOT/python" && $CONTAINER_ENGINE compose --profile testing build python-tests
    fi
    
    # Save to cache
    save_image
    
    echo -e "${GREEN}Image ${FULL_IMAGE_NAME} built and saved to cache${NC}"
}

# Update cache if needed
update_image() {
    if [[ ! -f "$CHECKSUM_FILE" ]]; then
        echo -e "${YELLOW}No cached image found. Building new image...${NC}"
        build_image
        return
    fi
    
    local stored_hash=$(cat "$CHECKSUM_FILE")
    local current_hash=$(get_build_hash)
    
    if [[ "$stored_hash" != "$current_hash" ]]; then
        echo -e "${YELLOW}Image files have changed. Rebuilding...${NC}"
        build_image
    else
        if ! $CONTAINER_ENGINE images -q "$FULL_IMAGE_NAME" &>/dev/null; then
            echo -e "${YELLOW}Image not found in local registry. Loading from cache...${NC}"
            load_image
        else
            echo -e "${GREEN}Image ${FULL_IMAGE_NAME} is up to date${NC}"
        fi
    fi
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
    --docker)
      check_container_engine "docker"
      shift
      ;;
    --name=*)
      IMAGE_NAME="${1#*=}"
      shift
      ;;
    --tag=*)
      IMAGE_TAG="${1#*=}"
      shift
      ;;
    --type=*)
      IMAGE_TYPE="${1#*=}"
      # Set appropriate values based on type
      case "${IMAGE_TYPE}" in
        test)
          IMAGE_NAME="rinna-python-tests"
          TARGET_STAGE="test"
          DOCKERFILE="python/Dockerfile"
          ;;
        dev)
          IMAGE_NAME="rinna-python-dev"
          TARGET_STAGE="development"
          DOCKERFILE="python/Dockerfile"
          ;;
        prod)
          IMAGE_NAME="rinna-python"
          TARGET_STAGE="production"
          DOCKERFILE="python/Dockerfile.prod"
          ;;
        *)
          echo -e "${RED}Unknown image type: ${IMAGE_TYPE}${NC}"
          show_help
          exit 1
          ;;
      esac
      shift
      ;;
    --dockerfile=*)
      DOCKERFILE="${1#*=}"
      shift
      ;;
    --target=*)
      TARGET_STAGE="${1#*=}"
      shift
      ;;
    save|load|update|build)
      ACTION="$1"
      shift
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      show_help
      exit 1
      ;;
  esac
done

# Update derived values
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"
CACHE_FILE="$CACHE_DIR/${IMAGE_NAME}-${IMAGE_TAG}.tar"
CHECKSUM_FILE="$CACHE_DIR/${IMAGE_NAME}-${IMAGE_TAG}.md5"

# Check for container engine if not already determined
if [[ -z "$CONTAINER_ENGINE" ]]; then
    check_container_engine
fi

# Validate action
if [[ -z "$ACTION" ]]; then
    echo -e "${RED}Error: No action specified${NC}"
    show_help
    exit 1
fi

# Execute action
case "$ACTION" in
    save)
        save_image
        ;;
    load)
        load_image
        ;;
    update)
        update_image
        ;;
    build)
        build_image
        ;;
    *)
        echo -e "${RED}Unknown action: $ACTION${NC}"
        show_help
        exit 1
        ;;
esac