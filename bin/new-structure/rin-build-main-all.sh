#!/bin/bash
#
# rin-build-main-all.sh - Optimized Build Orchestrator for Rinna
#
# This script orchestrates the build process for the Rinna project with reduced overhead
# and faster execution by focusing on essential build steps.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.

# Setup environment
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# Create log directory
mkdir -p "$PROJECT_ROOT/logs"

# Define variables
VERSION_FILE="$PROJECT_ROOT/version.properties"
WARNING_COUNT=0

# Set color variables
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
GRAY='\033[0;37m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Status indicators
STATUS_SUCCESS="✅"
STATUS_FAILURE="❌"
STATUS_SKIPPED="⏭️" 
STATUS_WARNING="⚠️"

# Essential build phases
BUILD_PHASES=(
  "initialize"
  "compile"
  "test"
  "package"
)

# Parse command line options
SKIP_TESTS=false
SKIP_QUALITY=true # Skip quality by default for faster builds
QUICK_BUILD=false
BUILD_PROFILE="dev"
SPECIFIC_PHASE=""
BUILD_COMPONENTS=("java" "go" "python")
HELP=false

# Parse options
parse_options() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --skip-tests)
        SKIP_TESTS=true
        shift
        ;;
      --with-quality)
        SKIP_QUALITY=false
        shift
        ;;
      --quick)
        QUICK_BUILD=true
        shift
        ;;
      --profile=*)
        BUILD_PROFILE="${1#*=}"
        shift
        ;;
      --phase=*)
        SPECIFIC_PHASE="${1#*=}"
        shift
        ;;
      --components=*)
        IFS=',' read -r -a BUILD_COMPONENTS <<< "${1#*=}"
        shift
        ;;
      --help|-h)
        HELP=true
        shift
        ;;
      *)
        echo -e "${RED}ERROR:${NC} Unknown option: $1"
        show_usage
        exit 1
        ;;
    esac
  done
  
  # Make sure all component names are lowercase
  for i in "${!BUILD_COMPONENTS[@]}"; do
    BUILD_COMPONENTS[$i]=$(echo "${BUILD_COMPONENTS[$i]}" | tr '[:upper:]' '[:lower:]')
  done
}

# Show usage
show_usage() {
  cat << EOF
Usage: $(basename "$0") [options]

Options:
  --skip-tests         Skip all tests
  --with-quality       Run quality checks (skipped by default)
  --quick              Quickest build (minimal checks)
  --profile=NAME       Set Maven profile (default: dev)
  --phase=NAME         Run specific phase only
  --components=LIST    Comma-separated list of components to build (java,go,python)
  --help, -h           Show this help message

Available phases: ${BUILD_PHASES[*]}
Available components: java, go, python

Examples:
  $(basename "$0")                # Run standard build (skips quality)
  $(basename "$0") --quick        # Run quickest build (skips tests and quality)
  $(basename "$0") --phase=test   # Run only the test phase
  $(basename "$0") --with-quality # Run full build with quality checks
EOF
}

# Helper functions for output formatting
log_info() {
  local message="$1"
  echo -e "ℹ️ ${message}"
}

log_error() {
  local message="$1"
  echo -e "❌ ${RED}ERROR:${NC} ${message}" >&2
}

# Streamlined task execution
run_task() {
  local description="$1"
  local cmd="$2"
  local skip_flag="${3:-false}"
  
  if [[ "$skip_flag" == "true" ]]; then
    echo -e "${STATUS_SKIPPED} ${description} - skipped"
    return 0
  fi
  
  echo -e "${BLUE}> RUNNING:${NC} ${description}"
  
  # Start timer
  local start_time=$(date +%s)
  
  # Run the command
  if eval "$cmd" > .build.log 2>&1; then
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    echo -e "${STATUS_SUCCESS} ${description} completed in ${duration}s"
    return 0
  else
    local exit_code=$?
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo -e "${STATUS_FAILURE} ${description} failed after ${duration}s (exit code $exit_code)"
    echo -e "${RED}> ERROR OUTPUT:${NC}"
    tail -n 20 .build.log
    echo -e "${RED}> BUILD STOPPED${NC}"
    rm -f .build.log
    exit $exit_code
  fi
}

# Check if a component is enabled
is_component_enabled() {
  local component="${1,,}"  # Convert to lowercase
  for c in "${BUILD_COMPONENTS[@]}"; do
    if [[ "$c" == "$component" ]]; then
      return 0
    fi
  done
  return 1
}

# Get version from version.properties file
get_version() {
  if [[ -f "$VERSION_FILE" ]]; then
    grep "^version=" "$VERSION_FILE" | cut -d= -f2
  else
    echo "1.0.0-SNAPSHOT"
  fi
}

# Initialize phase - prepare environment
run_initialize_phase() {
  echo -e "\n${BLUE}> PHASE: Initialize${NC}\n"
  
  # Create required directories
  mkdir -p "$PROJECT_ROOT/target"
  
  # Basic language version checks
  if is_component_enabled "java"; then
    if ! java -version &>/dev/null; then
      log_error "Java is required for the build"
      exit 1
    fi
  fi
  
  if is_component_enabled "go"; then
    if ! go version &>/dev/null; then
      log_error "Go is required for the build"
      exit 1
    fi
  fi
  
  if is_component_enabled "python"; then
    if ! python3 --version &>/dev/null; then
      log_error "Python 3 is required for the build"
      exit 1
    fi
  fi
  
  # Set version
  if [[ -f "$VERSION_FILE" ]]; then
    local version=$(get_version)
    echo -e "${STATUS_SUCCESS} Building Rinna version ${version}"
  else
    echo -e "${STATUS_WARNING} Version file not found, using default version"
  fi
  
  return 0
}

# Compile phase - build all components
run_compile_phase() {
  echo -e "\n${BLUE}> PHASE: Compile${NC}\n"
  
  # Compile Java components
  if is_component_enabled "java"; then
    local mvn_opts="-P $BUILD_PROFILE -DskipTests=true"
    if [[ "$SKIP_QUALITY" == "true" ]]; then
      mvn_opts="$mvn_opts -Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true"
    fi
    
    run_task "Compiling Java components" "mvn compile $mvn_opts"
  fi
  
  # Compile Go API
  if is_component_enabled "go" && [[ -d "$PROJECT_ROOT/api" ]]; then
    run_task "Compiling Go API server" "cd \"$PROJECT_ROOT/api\" && go build -o \"$PROJECT_ROOT/bin/rinnasrv\" ./cmd/rinnasrv"
    
    # Also build the health check utility (optional)
    if [[ -d "$PROJECT_ROOT/api/cmd/healthcheck" ]]; then
      run_task "Compiling Go healthcheck utility" "cd \"$PROJECT_ROOT/api\" && go build -o \"$PROJECT_ROOT/bin/healthcheck\" ./cmd/healthcheck"
    fi
  fi
  
  # Install Python modules
  if is_component_enabled "python" && [[ -d "$PROJECT_ROOT/python" ]]; then
    run_task "Installing Python modules" "cd \"$PROJECT_ROOT/python\" && pip install -e ."
  fi
  
  return 0
}

# Test phase - run all tests
run_test_phase() {
  if [[ "$SKIP_TESTS" == "true" ]]; then
    echo -e "\n${STATUS_SKIPPED} Test phase skipped due to --skip-tests flag\n"
    return 0
  fi
  
  echo -e "\n${BLUE}> PHASE: Test${NC}\n"
  
  # Run Java tests
  if is_component_enabled "java"; then
    local mvn_opts="-P $BUILD_PROFILE"
    if [[ "$SKIP_QUALITY" == "true" ]]; then
      mvn_opts="$mvn_opts -Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true"
    fi
    
    run_task "Running Java tests" "mvn test $mvn_opts"
  fi
  
  # Run Go tests
  if is_component_enabled "go" && [[ -d "$PROJECT_ROOT/api" ]]; then
    run_task "Running Go API tests" "cd \"$PROJECT_ROOT/api\" && go test ./..."
  fi
  
  # Run Python tests
  if is_component_enabled "python" && [[ -d "$PROJECT_ROOT/python/tests" ]]; then
    run_task "Running Python tests" "cd \"$PROJECT_ROOT\" && python -m pytest python/tests"
  fi
  
  return 0
}

# Package phase - create distributable packages
run_package_phase() {
  echo -e "\n${BLUE}> PHASE: Package${NC}\n"
  
  # Package Java components
  if is_component_enabled "java"; then
    local mvn_opts="-P $BUILD_PROFILE -DskipTests=$SKIP_TESTS"
    if [[ "$SKIP_QUALITY" == "true" ]]; then
      mvn_opts="$mvn_opts -Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true"
    fi
    
    run_task "Packaging Java components" "mvn package $mvn_opts"
  fi
  
  # Create distribution package
  echo -e "${BLUE}> CREATING:${NC} Distribution package"
  DIST_DIR="$PROJECT_ROOT/target/distribution"
  mkdir -p "$DIST_DIR/bin"
  
  # Copy binaries and JARs
  if is_component_enabled "go"; then
    cp "$PROJECT_ROOT/bin/rinnasrv" "$DIST_DIR/bin/" 2>/dev/null || true
  fi
  
  cp "$PROJECT_ROOT/bin/rin"* "$DIST_DIR/bin/" 2>/dev/null || true
  
  if is_component_enabled "java"; then
    mkdir -p "$DIST_DIR/lib"
    find "$PROJECT_ROOT" -name "*.jar" -not -path "*/target/classes/*" -not -path "*/test-classes/*" -exec cp {} "$DIST_DIR/lib/" \; 2>/dev/null || true
  fi
  
  # Add version file and create tarball
  if [[ -f "$VERSION_FILE" ]]; then
    cp "$VERSION_FILE" "$DIST_DIR/"
  fi
  
  VERSION=$(get_version || echo "unknown")
  DIST_TARBALL="$PROJECT_ROOT/target/rinna-${VERSION}.tar.gz"
  tar -czf "$DIST_TARBALL" -C "$PROJECT_ROOT/target" distribution
  
  echo -e "${STATUS_SUCCESS} Package created: $DIST_TARBALL"
  return 0
}

# Main function
main() {
  parse_options "$@"
  
  if [[ "$HELP" == "true" ]]; then
    show_usage
    exit 0
  fi
  
  # Print build header
  echo -e "\n${BLUE}═════════════════════════════════════════════════${NC}"
  echo -e "${BLUE}           Rinna Optimized Build                ${NC}"
  echo -e "${BLUE}═════════════════════════════════════════════════${NC}\n"
  
  # Build configuration
  log_info "Build configuration:"
  echo -e "• Components: ${BUILD_COMPONENTS[*]}"
  echo -e "• Skip tests: $SKIP_TESTS"
  echo -e "• Skip quality: $SKIP_QUALITY"
  echo -e "• Profile: $BUILD_PROFILE"
  
  # Quick build shortcut
  if [[ "$QUICK_BUILD" == "true" ]]; then
    SKIP_TESTS=true
    SKIP_QUALITY=true
    echo -e "${YELLOW}> NOTE:${NC} Quick build enabled - skipping tests and quality"
  fi
  
  # Track timing
  BUILD_START_TIME=$(date +%s)
  
  # Run phases
  if [[ -n "$SPECIFIC_PHASE" ]]; then
    if [[ " ${BUILD_PHASES[*]} " == *" $SPECIFIC_PHASE "* ]]; then
      case "$SPECIFIC_PHASE" in
        initialize) run_initialize_phase ;;
        compile) run_compile_phase ;;
        test) run_test_phase ;;
        package) run_package_phase ;;
      esac
    else
      log_error "Unknown build phase: $SPECIFIC_PHASE"
      log_info "Available phases: ${BUILD_PHASES[*]}"
      exit 1
    fi
  else
    # Run all phases in sequence
    run_initialize_phase
    run_compile_phase
    run_test_phase
    run_package_phase
  fi
  
  # Clean up
  rm -f .build.log 2>/dev/null || true
  
  # Calculate total build time
  BUILD_END_TIME=$(date +%s)
  BUILD_DURATION=$((BUILD_END_TIME - BUILD_START_TIME))
  
  # Build summary
  echo -e "\n${BLUE}═════════════════════════════════════════════════${NC}"
  echo -e "${GREEN}             BUILD SUCCESSFUL                  ${NC}"
  echo -e "${BLUE}═════════════════════════════════════════════════${NC}"
  echo -e "• Total build time: ${BUILD_DURATION} seconds"
  echo -e "• Components built: ${BUILD_COMPONENTS[*]}"
  
  echo -e "\n${GREEN}You can now run the CLI tools from ${BLUE}$PROJECT_ROOT/bin${NC}"
  echo -e "${GREEN}Or use the artifacts from ${BLUE}$PROJECT_ROOT/target${NC}\n"
}

# Run the main function
main "$@"