#!/bin/bash
#
# build.sh - Unified Build Orchestrator for Rinna
#
# This script provides a streamlined build process for the Rinna project
# with minimal output and maximum efficiency.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in the LICENSE file.
#

# Core variables - minimal setup
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT" || exit 1

# Create log directory
mkdir -p "$PROJECT_ROOT/logs"

# Define basic build phases - for clear progression
BUILD_PHASES=("initialize" "validate" "compile" "test" "package" "verify" "install")

# Initialize options with sensible defaults
SKIP_TESTS=false
SKIP_QUALITY=false
QUICK_BUILD=false
BUILD_PROFILE="local-quality"
SPECIFIC_PHASE=""
VERBOSE=false
BUILD_COMPONENTS=("java" "go" "python")
PARALLEL_BUILD=true

# Colors for better readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

# Status indicators - visual consistency
SUCCESS="✅"
FAILURE="❌"
PENDING="🔄"
SKIPPED="⏭️"
WARNING="⚠️"

# Track build time and warnings
BUILD_START_TIME=$(date +%s)
WARNING_COUNT=0

# Parse command line options
parse_options() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --skip-tests) SKIP_TESTS=true; shift ;;
      --skip-quality) SKIP_QUALITY=true; shift ;;
      --quick) QUICK_BUILD=true; shift ;;
      --profile=*) BUILD_PROFILE="${1#*=}"; shift ;;
      --phase=*) SPECIFIC_PHASE="${1#*=}"; shift ;;
      --components=*) IFS=',' read -r -a BUILD_COMPONENTS <<< "${1#*=}"; shift ;;
      --parallel) PARALLEL_BUILD=true; shift ;;
      --verbose) VERBOSE=true; export VERBOSE=true; shift ;;
      --help|-h) show_usage; exit 0 ;;
      *) echo -e "${RED}Unknown option: $1${NC}" >&2; show_usage; exit 1 ;;
    esac
  done
  
  # Normalize component names to lowercase
  for i in "${!BUILD_COMPONENTS[@]}"; do
    BUILD_COMPONENTS[$i]=$(echo "${BUILD_COMPONENTS[$i]}" | tr '[:upper:]' '[:lower:]')
  done
}

# Show usage information
show_usage() {
  cat << EOF
${BOLD}Rinna Build${NC} - Unified build system

USAGE: $(basename "$0") [options]

OPTIONS:
  --skip-tests         Skip all tests
  --skip-quality       Skip quality checks
  --quick              Quick build (minimal checks)
  --profile=NAME       Set Maven profile (default: local-quality)
  --phase=NAME         Run specific phase only
  --components=LIST    Components to build (java,go,python)
  --parallel           Run supported steps in parallel
  --verbose            Show detailed output
  --help, -h           Show this help message

${BOLD}Examples:${NC}
  $(basename "$0")                # Full build with default settings
  $(basename "$0") --quick        # Fast build (skip tests and quality)
  $(basename "$0") --phase=test   # Run only the test phase
EOF
}

# Log helpers - simplified for reduced output
log_info() { echo -e "ℹ️ $1"; }
log_error() { echo -e "${RED}ERROR: $1${NC}" >&2; }
log_success() { echo -e "${GREEN}$1${NC}"; }

# Task tracking functions - minimal but informative
start_task() { echo -e "${PENDING} ${BOLD}$1...${NC}"; TASK_START_TIME=$(date +%s); }

complete_task() {
  local duration=$(($(date +%s) - TASK_START_TIME))
  echo -e "\r\033[K${SUCCESS} $1 (${duration}s)"
}

fail_task() {
  local duration=$(($(date +%s) - TASK_START_TIME))
  echo -e "\r\033[K${FAILURE} $1 (${duration}s)"
  [[ -n "$2" ]] && echo -e "   ${RED}$2${NC}"
}

skip_task() { echo -e "${SKIPPED} $1 - skipped"; }

warn_task() {
  echo -e "${WARNING} $1"
  ((WARNING_COUNT++))
}

# Section headers - clean and visible
section_header() {
  echo -e "\n${BLUE}${BOLD}== $1 ==${NC}\n"
}

# Section footer - minimal
section_footer() { echo ""; }

# Check if component is enabled
is_component_enabled() {
  local component="${1,,}"
  for c in "${BUILD_COMPONENTS[@]}"; do
    [[ "$c" == "$component" ]] && return 0
  done
  return 1
}

# Get current version
get_version() {
  local version_file="$PROJECT_ROOT/version.properties"
  if [[ -f "$version_file" ]]; then
    grep "^version=" "$version_file" | cut -d= -f2
  else
    echo "1.0.0-SNAPSHOT"
  fi
}

# Command runner with formatted output
run_formatted() {
  local cmd="$1"
  local description="$2"
  local skip_flag="${3:-false}"
  
  [[ "$skip_flag" == "true" ]] && { skip_task "$description"; return 0; }
  
  start_task "$description"
  
  # Run command and capture output directly without temp files
  if output=$(eval "$cmd" 2>&1); then
    complete_task "$description"
    [[ "${VERBOSE:-false}" == "true" ]] && echo "$output"
    return 0
  else
    local exit_code=$?
    fail_task "$description" "Failed with exit code $exit_code"
    echo "$output"
    exit $exit_code
  fi
}

# Phase runners - each implements a specific build phase

# Initialize phase - environment preparation
run_initialize_phase() {
  section_header "Initializing Build Environment"
  
  # Check build tools
  if is_component_enabled "java"; then
    run_formatted "command -v java >/dev/null && command -v mvn >/dev/null" "Checking build tools"
  else
    skip_task "Checking build tools (Java component disabled)"
  fi
  
  # Create build directories
  run_formatted "mkdir -p \"$PROJECT_ROOT/target\"" "Creating build directories"
  
  # Load environment
  if [[ -f "$PROJECT_ROOT/activate-rinna.sh" ]]; then
    run_formatted "source \"$PROJECT_ROOT/activate-rinna.sh\"" "Loading environment"
  else
    skip_task "Loading environment (file not found)"
  fi
  
  section_footer
}

# Validate phase - code checks
run_validate_phase() {
  [[ "$SKIP_QUALITY" == "true" ]] && { skip_task "Validation phase (--skip-quality)"; return 0; }
  
  section_header "Validating Code and Configuration"
  
  # Architecture validation
  if [[ -f "$PROJECT_ROOT/bin/run-checks.sh" ]]; then
    run_formatted "$PROJECT_ROOT/bin/run-checks.sh" "Validating architecture"
  else
    skip_task "Architecture validation (script not found)"
  fi
  
  # Maven validation
  if is_component_enabled "java"; then
    run_formatted "mvn validate -P $BUILD_PROFILE" "Running Maven validation"
  else
    skip_task "Maven validation (Java component disabled)"
  fi
  
  section_footer
}

# Compile phase - build all components
run_compile_phase() {
  section_header "Compiling Components"
  
  # Java compilation
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    [[ "$SKIP_TESTS" == "true" ]] && mvn_options="$mvn_options -DskipTests=true"
    [[ "$PARALLEL_BUILD" == "true" ]] && mvn_options="$mvn_options -T 1C"
    
    run_formatted "mvn compile $mvn_options" "Compiling Java components"
  else
    skip_task "Java compilation (disabled)"
  fi
  
  # Go compilation
  if is_component_enabled "go" && [[ -d "$PROJECT_ROOT/api" ]]; then
    run_formatted "cd \"$PROJECT_ROOT/api\" && go build -o \"$PROJECT_ROOT/bin/rinnasrv\" ./cmd/rinnasrv" \
      "Compiling Go API server"
  else
    skip_task "Go compilation (disabled or not found)"
  fi
  
  # Python setup
  if is_component_enabled "python" && [[ -d "$PROJECT_ROOT/python" ]]; then
    run_formatted "cd \"$PROJECT_ROOT/python\" && pip install -e ." "Installing Python package"
  else
    skip_task "Python installation (disabled or not found)"
  fi
  
  section_footer
}

# Test phase - run tests
run_test_phase() {
  [[ "$SKIP_TESTS" == "true" ]] && { skip_task "Test phase (--skip-tests)"; return 0; }
  
  section_header "Running Tests"
  
  # Java tests
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    [[ "$PARALLEL_BUILD" == "true" ]] && mvn_options="$mvn_options -T 1C"
    
    run_formatted "mvn test $mvn_options" "Running Java tests"
  else
    skip_task "Java tests (disabled)"
  fi
  
  # Go tests
  if is_component_enabled "go" && [[ -d "$PROJECT_ROOT/api" ]]; then
    run_formatted "cd \"$PROJECT_ROOT/api\" && go test ./..." "Running Go tests"
  else
    skip_task "Go tests (disabled or not found)"
  fi
  
  # Python tests
  if is_component_enabled "python" && [[ -d "$PROJECT_ROOT/python/tests" ]]; then
    run_formatted "cd \"$PROJECT_ROOT\" && python -m pytest python/tests" "Running Python tests"
  else
    skip_task "Python tests (disabled or not found)"
  fi
  
  section_footer
}

# Package phase - create distribution
run_package_phase() {
  section_header "Packaging Components"
  
  # Package Java components
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    [[ "$SKIP_TESTS" == "true" ]] && mvn_options="$mvn_options -DskipTests=true"
    
    run_formatted "mvn package $mvn_options" "Packaging Java components"
  else
    skip_task "Java packaging (disabled)"
  fi
  
  # Create distribution package
  start_task "Creating distribution package"
  
  # Setup distribution directory
  DIST_DIR="$PROJECT_ROOT/target/distribution"
  mkdir -p "$DIST_DIR/bin"
  
  # Copy binaries and JARs
  cp "$PROJECT_ROOT/bin/rin"* "$DIST_DIR/bin/" 2>/dev/null || true
  
  if is_component_enabled "java"; then
    mkdir -p "$DIST_DIR/lib"
    find "$PROJECT_ROOT" -name "*.jar" -not -path "*/target/classes/*" -not -path "*/test-classes/*" \
      -exec cp {} "$DIST_DIR/lib/" \; 2>/dev/null || true
  fi
  
  # Create tarball
  VERSION=$(get_version || echo "unknown")
  DIST_TARBALL="$PROJECT_ROOT/target/rinna-${VERSION}.tar.gz"
  tar -czf "$DIST_TARBALL" -C "$PROJECT_ROOT/target" distribution
  
  complete_task "Package created: $(basename "$DIST_TARBALL")"
  section_footer
}

# Verify phase - quality checks
run_verify_phase() {
  [[ "$SKIP_QUALITY" == "true" ]] && { skip_task "Verification phase (--skip-quality)"; return 0; }
  
  section_header "Verifying Build Quality"
  
  # Maven verify with quality checks
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    [[ "$SKIP_TESTS" == "true" ]] && mvn_options="$mvn_options -DskipTests=true"
    
    run_formatted "mvn verify $mvn_options" "Running quality verification"
  else
    skip_task "Quality verification (Java disabled)"
  fi
  
  section_footer
}

# Install phase - install artifacts
run_install_phase() {
  section_header "Installing Components"
  
  # Install Maven artifacts
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    [[ "$SKIP_TESTS" == "true" ]] && mvn_options="$mvn_options -DskipTests=true"
    
    run_formatted "mvn install $mvn_options" "Installing Maven artifacts"
  else
    skip_task "Maven installation (Java disabled)"
  fi
  
  # Display success message
  echo -e "\n${GREEN}${BOLD}Rinna build completed successfully${NC}\n"
  
  section_footer
}

# Run a specific build phase
run_phase() {
  local phase="$1"
  
  case "$phase" in
    initialize) run_initialize_phase ;;
    validate) run_validate_phase ;;
    compile) run_compile_phase ;;
    test) run_test_phase ;;
    package) run_package_phase ;;
    verify) run_verify_phase ;;
    install) run_install_phase ;;
    *)
      log_error "Unknown phase: $phase"
      log_info "Available phases: ${BUILD_PHASES[*]}"
      return 1
      ;;
  esac
  
  return $?
}

# Main function
main() {
  parse_options "$@"
  
  # Set quick build shortcuts
  if [[ "$QUICK_BUILD" == "true" ]]; then
    SKIP_TESTS=true
    SKIP_QUALITY=true
    log_info "Quick build enabled - skipping tests and quality checks"
  fi
  
  section_header "Rinna Build System"
  log_info "Building with: profile=$BUILD_PROFILE, components=${BUILD_COMPONENTS[*]}"
  
  if [[ -n "$SPECIFIC_PHASE" ]]; then
    log_info "Running single phase: $SPECIFIC_PHASE"
    run_phase "$SPECIFIC_PHASE"
  else
    log_info "Running all build phases"
    for phase in "${BUILD_PHASES[@]}"; do
      run_phase "$phase"
    done
  fi
  
  # Calculate build time
  BUILD_END_TIME=$(date +%s)
  BUILD_DURATION=$((BUILD_END_TIME - BUILD_START_TIME))
  
  # Show summary
  section_header "Build Summary"
  log_success "Build completed in ${BUILD_DURATION}s"
  
  if [[ $WARNING_COUNT -gt 0 ]]; then
    echo -e "${YELLOW}Build generated $WARNING_COUNT warnings${NC}"
  else
    echo -e "${GREEN}Build completed with no warnings${NC}"
  fi
  
  # Show performance tip
  if [[ "$PARALLEL_BUILD" != "true" ]]; then
    echo -e "\n${BLUE}TIP: Use ${BOLD}--parallel${NC}${BLUE} flag for faster builds${NC}"
  fi
  
  echo ""
  return 0
}

# Run the main function
main "$@"