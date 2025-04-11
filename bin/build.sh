#!/bin/bash
#
# build.sh - Unified Build Orchestrator for Rinna
#
# This script orchestrates the entire build process for the Rinna project,
# covering Java, Go, and Python components with consistent status reporting.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#
# OPTIMIZATION NOTE: This script has been optimized to reduce build time 
# and improve performance while maintaining the same functionality.

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
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Status indicators
STATUS_PENDING="🔄"
STATUS_SUCCESS="✅"
STATUS_FAILURE="❌"
STATUS_SKIPPED="⏭️" 
STATUS_WARNING="⚠️"

# Main build phases
BUILD_PHASES=(
  "initialize"
  "validate"
  "compile"
  "test"
  "package"
  "verify"
  "install"
)

# Parse command line options
SKIP_TESTS=false
SKIP_QUALITY=false
QUICK_BUILD=false
BUILD_PROFILE="local-quality"
SPECIFIC_PHASE=""
VERBOSE=false
BUILD_COMPONENTS=("java" "go" "python")
HELP=false
RUN_POLYGLOT=true
PARALLEL_BUILD=false  # Enable parallel build for supported components

# Parse options
parse_options() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --skip-tests)
        SKIP_TESTS=true
        shift
        ;;
      --skip-quality)
        SKIP_QUALITY=true
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
      --skip-polyglot)
        RUN_POLYGLOT=false
        shift
        ;;
      --parallel)
        PARALLEL_BUILD=true
        shift
        ;;
      --verbose)
        VERBOSE=true
        export VERBOSE=true
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
  --skip-quality       Skip quality checks
  --quick              Quick build (minimal checks, faster)
  --profile=NAME       Set Maven profile (default: local-quality)
  --phase=NAME         Run specific phase only
  --components=LIST    Comma-separated list of components to build (java,go,python)
  --parallel           Run supported build steps in parallel (faster build)
  --skip-polyglot      Skip polyglot cross-language tests
  --verbose            Show verbose output
  --help, -h           Show this help message

Available phases: ${BUILD_PHASES[*]}
Available components: java, go, python

Examples:
  $(basename "$0")                # Run full build with default settings
  $(basename "$0") --quick        # Run quick build (skip tests and quality)
  $(basename "$0") --parallel     # Run build with parallel execution where possible
  $(basename "$0") --phase=test   # Run only the test phase
  $(basename "$0") --components=java,go # Build only Java and Go components
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

start_task() {
  local message="$1"
  echo -e "${STATUS_PENDING} ${BOLD}${message}...${NC}"
  TASK_START_TIME=$(date +%s)
}

complete_task() {
  local message="$1"
  local elapsed=""
  
  if [[ -n "$TASK_START_TIME" ]]; then
    local end_time=$(date +%s)
    local duration=$((end_time - TASK_START_TIME))
    elapsed=" (${duration}s)"
    unset TASK_START_TIME
  fi
  
  echo -e "\r\033[K${STATUS_SUCCESS} ${message}${elapsed}"
}

fail_task() {
  local message="$1"
  local error="${2:-}"
  local elapsed=""
  
  if [[ -n "$TASK_START_TIME" ]]; then
    local end_time=$(date +%s)
    local duration=$((end_time - TASK_START_TIME))
    elapsed=" (${duration}s)"
    unset TASK_START_TIME
  fi
  
  echo -e "\r\033[K${STATUS_FAILURE} ${message}${elapsed}"
  if [[ -n "$error" ]]; then
    echo -e "   ${RED}${error}${NC}"
  fi
}

skip_task() {
  local message="$1"
  echo -e "\r\033[K${STATUS_SKIPPED} ${message} - skipped"
}

warn_task() {
  local message="$1"
  echo -e "${STATUS_WARNING} ${message}"
  echo -e "${YELLOW}> WARNING:${NC} ${message}"
  ((WARNING_COUNT++))
}

section_header() {
  local title="$1"
  local dash_length=$(($(tput cols) - ${#title} - 4))
  local dashes=$(printf '%*s' "$dash_length" | tr ' ' '─')
  
  echo ""
  echo -e "${BLUE}${BOLD}╭─ ${title} ${dashes}${NC}"
  echo ""
}

section_footer() {
  local dash_length=$(tput cols)
  local dashes=$(printf '%*s' "$dash_length" | tr ' ' '─')
  
  echo ""
  echo -e "${BLUE}${BOLD}╰${dashes}${NC}"
  echo ""
}

show_execution_plan() {
  local title="$1"
  shift
  local steps=("$@")
  
  section_header "$title"
  for i in "${!steps[@]}"; do
    echo -e " ${GRAY}$(($i + 1)).${NC} ${steps[$i]}"
  done
  section_footer
}

run_formatted() {
  local cmd="$1"
  local description="$2"
  local skip_flag="${3:-false}"
  
  if [[ "$skip_flag" == "true" ]]; then
    skip_task "$description"
    return 0
  fi
  
  # Announce what we're about to do
  echo -e "${BLUE}> STEP:${NC} About to $description"
  start_task "$description"
  
  # Run the command and capture output - optimized to avoid temp files
  local output
  if output=$(eval "$cmd" 2>&1); then
    # Report success
    complete_task "$description"
    echo -e "${GREEN}> COMPLETED:${NC} Successfully finished $description"
    
    # Show verbose output if requested
    if [[ "${VERBOSE:-false}" == "true" ]]; then
      echo ""
      echo "$output"
      echo ""
    fi
    
    return 0
  else
    # Capture error information
    local exit_code=$?
    fail_task "$description" "Command failed with exit code $exit_code"
    
    # Show error output
    echo -e "${RED}> ERROR:${NC} Failed while $description"
    echo ""
    echo "$output"
    echo ""
    
    # Exit immediately on error
    echo -e "${RED}> BUILD STOPPED:${NC} Halting build process due to error"
    exit $exit_code
  fi
}

# Main function
main() {
  parse_options "$@"
  
  if [[ "$HELP" == "true" ]]; then
    show_usage
    exit 0
  fi
  
  section_header "Rinna Build Process"
  
  log_info "Build configuration: profile=$BUILD_PROFILE, components=${BUILD_COMPONENTS[*]}"
  if [[ -n "$SPECIFIC_PHASE" ]]; then
    echo "- Phase: $SPECIFIC_PHASE, skip-tests=$SKIP_TESTS, skip-quality=$SKIP_QUALITY, parallel=$PARALLEL_BUILD"
  else
    echo "- All phases, skip-tests=$SKIP_TESTS, skip-quality=$SKIP_QUALITY, parallel=$PARALLEL_BUILD"
  fi
  
  # Set quick build shortcuts
  if [[ "$QUICK_BUILD" == "true" ]]; then
    SKIP_TESTS=true
    SKIP_QUALITY=true
    log_info "Quick build enabled - skipping tests and quality checks"
  fi
  
  # Announce build plan
  if [[ -n "$SPECIFIC_PHASE" ]]; then
    if [[ " ${BUILD_PHASES[*]} " == *" $SPECIFIC_PHASE "* ]]; then
      echo -e "${BLUE}> BUILD PLAN:${NC} Running 1 phase: $SPECIFIC_PHASE"
    else
      log_error "Unknown build phase: $SPECIFIC_PHASE"
      log_info "Available phases: ${BUILD_PHASES[*]}"
      exit 1
    fi
  else
    echo -e "${BLUE}> BUILD PLAN:${NC} Running all ${#BUILD_PHASES[@]} phases: ${BUILD_PHASES[*]}"
  fi
  
  echo ""
  
  # Track warnings
  WARNING_COUNT=0
  BUILD_START_TIME=$(date +%s)
  
  # Run phases
  if [[ -n "$SPECIFIC_PHASE" ]]; then
    run_phase "$SPECIFIC_PHASE"
  else
    # Run all phases in sequence
    for phase in "${BUILD_PHASES[@]}"; do
      run_phase "$phase"
    done
  fi
  
  # Calculate total build time
  BUILD_END_TIME=$(date +%s)
  BUILD_DURATION=$((BUILD_END_TIME - BUILD_START_TIME))
  
  # Build summary section
  section_header "Build Summary"
  
  echo -e "${GREEN}> BUILD SUCCESSFUL:${NC} All build phases completed without errors"
  echo -e "${BLUE}> BUILD TIME:${NC} Total build time: ${BUILD_DURATION}s"
  
  if [[ $WARNING_COUNT -gt 0 ]]; then
    echo -e "${YELLOW}> WARNINGS:${NC} Build completed with $WARNING_COUNT warnings"
  else
    echo -e "${GREEN}> WARNINGS:${NC} No warnings were reported during the build"
  fi
  
  # Run XML cleanup scheduler if it exists
  if [[ -f "$PROJECT_ROOT/bin/xml-tools/xml-cleanup-scheduler.sh" ]]; then
    echo -e "${BLUE}> CLEANUP:${NC} Running XML cleanup scheduler"
    "$PROJECT_ROOT/bin/xml-tools/xml-cleanup-scheduler.sh" || true
  fi
  
  section_footer
}

# Run a specific build phase
run_phase() {
  local phase="$1"
  local start_time=$(date +%s)
  
  # Get the number of steps for the phase
  local total_steps=0
  local phase_steps=()
  
  case "$phase" in
    initialize)
      phase_steps=("Checking build requirements" "Creating build directories" "Loading Rinna environment" "Reading version information")
      ;;
    validate)
      phase_steps=("Validating architecture" "Running Maven validation" "Checking Go code" "Checking Python code")
      ;;
    compile)
      if is_component_enabled "java"; then phase_steps+=("Compiling Java components"); fi
      if is_component_enabled "go"; then phase_steps+=("Compiling Go API server"); fi
      if is_component_enabled "python"; then phase_steps+=("Installing Python modules"); fi
      ;;
    test)
      if [[ "$SKIP_TESTS" != "true" ]]; then
        if is_component_enabled "java"; then phase_steps+=("Running Java tests"); fi
        if is_component_enabled "go"; then phase_steps+=("Running Go API tests"); fi
        if is_component_enabled "python"; then phase_steps+=("Running Python tests"); fi
        if [[ "$RUN_POLYGLOT" == "true" ]]; then phase_steps+=("Running cross-language integration tests"); fi
      fi
      ;;
    package)
      if is_component_enabled "java"; then phase_steps+=("Packaging Java components"); fi
      phase_steps+=("Creating distribution package")
      ;;
    verify)
      if [[ "$SKIP_QUALITY" != "true" ]]; then
        phase_steps=("Running quality verification" "Generating coverage reports" "Generating architecture diagrams")
      fi
      ;;
    install)
      if is_component_enabled "java"; then phase_steps+=("Installing Maven artifacts"); fi
      phase_steps+=("Creating CLI tool symlinks")
      ;;
    *)
      log_error "Unknown phase: $phase"
      return 1
      ;;
  esac
  
  total_steps=${#phase_steps[@]}
  
  section_header "Phase: $phase"
  
  # Announce phase information
  echo -e "${BLUE}> PHASE START:${NC} Beginning $phase phase with $total_steps steps"
  echo ""
  for i in "${!phase_steps[@]}"; do
    echo -e "  ${GRAY}$(($i + 1))/${total_steps}:${NC} ${phase_steps[$i]}"
  done
  echo ""
  
  # Run the actual phase
  case "$phase" in
    initialize)
      run_initialize_phase
      ;;
    validate)
      run_validate_phase
      ;;
    compile)
      run_compile_phase
      ;;
    test)
      run_test_phase
      ;;
    package)
      run_package_phase
      ;;
    verify)
      run_verify_phase
      ;;
    install)
      run_install_phase
      ;;
  esac
  
  local result=$?
  local end_time=$(date +%s)
  local duration=$((end_time - start_time))
  
  if [[ $result -eq 0 ]]; then
    log_info "✅ Phase $phase completed successfully (${duration}s)"
    echo -e "${GREEN}> PHASE COMPLETE:${NC} All $total_steps steps of $phase phase completed successfully"
  else
    log_error "❌ Phase $phase failed after ${duration}s"
    echo -e "${RED}> PHASE FAILED:${NC} Phase $phase did not complete successfully"
    exit $result
  fi
  
  return $result
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

# Check Java version
check_java_version() {
  local required_version="$1"
  
  if ! command -v java &> /dev/null; then
    return 1
  fi
  
  local java_version
  java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
  
  if [[ -z "$java_version" ]]; then
    return 1
  fi
  
  if [[ "$java_version" -lt "$required_version" ]]; then
    return 1
  fi
  
  return 0
}

# Check Go version
check_go_version() {
  local required_version="$1"
  
  if ! command -v go &> /dev/null; then
    return 1
  fi
  
  local go_version
  go_version=$(go version | awk '{print $3}' | cut -c 3-)
  local major_version=$(echo "$go_version" | cut -d. -f1)
  local minor_version=$(echo "$go_version" | cut -d. -f2)
  local required_major=$(echo "$required_version" | cut -d. -f1)
  local required_minor=$(echo "$required_version" | cut -d. -f2)
  
  if [[ "$major_version" -lt "$required_major" ]]; then
    return 1
  fi
  
  if [[ "$major_version" -eq "$required_major" && "$minor_version" -lt "$required_minor" ]]; then
    return 1
  fi
  
  return 0
}

# Check Python version
check_python_version() {
  local required_version="$1"
  
  if ! command -v python3 &> /dev/null; then
    return 1
  fi
  
  local python_version
  python_version=$(python3 --version 2>&1 | awk '{print $2}')
  local major_version=$(echo "$python_version" | cut -d. -f1)
  local minor_version=$(echo "$python_version" | cut -d. -f2)
  local required_major=$(echo "$required_version" | cut -d. -f1)
  local required_minor=$(echo "$required_version" | cut -d. -f2)
  
  if [[ "$major_version" -lt "$required_major" ]]; then
    return 1
  fi
  
  if [[ "$major_version" -eq "$required_major" && "$minor_version" -lt "$required_minor" ]]; then
    return 1
  fi
  
  return 0
}

# Initialize phase - prepare environment
run_initialize_phase() {
  # Define initialization steps
  local STEPS=(
    "Checking build requirements"
    "Creating build directories"
    "Loading Rinna environment"
    "Reading version information"
  )
  
  # Show execution plan
  show_execution_plan "Initialization Steps" "${STEPS[@]}"
  
  # Check for required tools
  start_task "Checking build requirements"
  
  local req_failed=false
  
  if is_component_enabled "java"; then
    if ! check_java_version 21; then
      log_error "Java 21 is required for the build"
      req_failed=true
    fi
  fi
  
  if is_component_enabled "go"; then
    if ! check_go_version 1.21; then
      log_error "Go 1.21+ is required for the build"
      req_failed=true
    fi
  fi
  
  if is_component_enabled "python"; then
    if ! check_python_version 3.9; then
      log_error "Python 3.9+ is required for the build"
      req_failed=true
    fi
  fi
  
  if [[ "$req_failed" == "true" ]]; then
    fail_task "Build requirements check failed"
    return 1
  fi
  
  complete_task "Build requirements check passed"
  
  # Create required directories
  run_formatted "mkdir -p \"$PROJECT_ROOT/target\"" "Creating build directories"
  
  # Load environment if available
  if [[ -f "$PROJECT_ROOT/activate-rinna.sh" ]]; then
    run_formatted "source \"$PROJECT_ROOT/activate-rinna.sh\"" "Loading Rinna environment"
  else
    skip_task "Loading Rinna environment"
  fi
  
  # Check version properties
  if [[ -f "$VERSION_FILE" ]]; then
    start_task "Reading version information"
    local version=$(get_version)
    if [[ $? -eq 0 ]]; then
      complete_task "Building Rinna version $version"
    else
      fail_task "Failed to read version information"
    fi
  else
    warn_task "Version file not found: $VERSION_FILE"
  fi
  
  return 0
}

# Validate phase - check code and configurations
run_validate_phase() {
  local STEPS=(
    "Validating architecture"
    "Running Maven validation"
    "Checking Go code"
    "Checking Python code"
  )
  
  # Show execution plan
  show_execution_plan "Validation Steps" "${STEPS[@]}"
  
  if [[ "$SKIP_QUALITY" == "true" ]]; then
    skip_task "Validation phase (skipped due to --skip-quality)"
    return 0
  fi
  
  # Run architectural validation
  if [[ -f "$PROJECT_ROOT/bin/run-checks.sh" ]]; then
    run_formatted "$PROJECT_ROOT/bin/run-checks.sh" "Validating architecture"
  else
    skip_task "Architecture validation (script not found)"
  fi
  
  # Run Maven validation
  if is_component_enabled "java"; then
    run_formatted "mvn validate -P validate-architecture,$BUILD_PROFILE" "Running Maven validation"
  else
    skip_task "Maven validation (Java component disabled)"
  fi
  
  # Run Go validation
  if is_component_enabled "go" && [[ -d "$PROJECT_ROOT/api" ]]; then
    run_formatted "cd \"$PROJECT_ROOT/api\" && go vet ./..." "Checking Go code"
  else
    skip_task "Go validation (Go component disabled or not found)"
  fi
  
  # Run Python validation
  if is_component_enabled "python" && [[ -d "$PROJECT_ROOT/python" ]]; then
    if command -v pylint &> /dev/null; then
      run_formatted "cd \"$PROJECT_ROOT/python\" && pylint rinna" "Checking Python code"
    else
      warn_task "Python linter not found, skipping Python validation"
    fi
  else
    skip_task "Python validation (Python component disabled or not found)"
  fi
  
  return 0
}

# Compile phase - build all components
run_compile_phase() {
  local STEPS=()
  
  if is_component_enabled "java"; then
    STEPS+=("Compiling Java components")
  fi
  
  if is_component_enabled "go"; then
    STEPS+=("Compiling Go API server")
  fi
  
  if is_component_enabled "python"; then
    STEPS+=("Installing Python modules")
  fi
  
  # Show execution plan
  show_execution_plan "Compilation Steps" "${STEPS[@]}"
  
  # Compile Java components
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    
    if [[ "$SKIP_TESTS" == "true" ]]; then
      mvn_options="$mvn_options -DskipTests=true"
    fi
    
    # Add parallel execution if enabled
    if [[ "$PARALLEL_BUILD" == "true" ]]; then
      echo -e "${BLUE}> PARALLEL:${NC} Using Maven parallel build mode"
      mvn_options="$mvn_options -T 1C"  # Use 1 thread per core
    fi
    
    run_formatted "mvn compile $mvn_options" "Compiling Java components"
  else
    skip_task "Java compilation (Java component disabled)"
  fi
  
  # Compile Go API
  if is_component_enabled "go" && [[ -d "$PROJECT_ROOT/api" ]]; then
    if [[ "$PARALLEL_BUILD" == "true" && -d "$PROJECT_ROOT/api/cmd/healthcheck" ]]; then
      # Build server and healthcheck in parallel for faster builds
      echo -e "${BLUE}> PARALLEL:${NC} Compiling Go components in parallel"
      (run_formatted "cd \"$PROJECT_ROOT/api\" && go build -o \"$PROJECT_ROOT/bin/rinnasrv\" ./cmd/rinnasrv" "Compiling Go API server") &
      (run_formatted "cd \"$PROJECT_ROOT/api\" && go build -o \"$PROJECT_ROOT/bin/healthcheck\" ./cmd/healthcheck" "Compiling Go healthcheck utility") &
      wait
    else
      # Sequential build
      run_formatted "cd \"$PROJECT_ROOT/api\" && go build -o \"$PROJECT_ROOT/bin/rinnasrv\" ./cmd/rinnasrv" "Compiling Go API server"
      
      # Also build the health check utility
      if [[ -d "$PROJECT_ROOT/api/cmd/healthcheck" ]]; then
        run_formatted "cd \"$PROJECT_ROOT/api\" && go build -o \"$PROJECT_ROOT/bin/healthcheck\" ./cmd/healthcheck" "Compiling Go healthcheck utility"
      fi
    fi
    
    # Generate Swagger/OpenAPI documentation if the script exists
    if [[ -f "$PROJECT_ROOT/bin/generate-swagger.sh" ]]; then
      run_formatted "$PROJECT_ROOT/bin/generate-swagger.sh" "Generating API documentation"
    fi
  else
    skip_task "Go compilation (Go component disabled or not found)"
  fi
  
  # Install Python modules
  if is_component_enabled "python" && [[ -d "$PROJECT_ROOT/python" ]]; then
    run_formatted "cd \"$PROJECT_ROOT/python\" && pip install -e ." "Installing Python modules"
  else
    skip_task "Python installation (Python component disabled or not found)"
  fi
  
  return 0
}

# Test phase - run all tests
run_test_phase() {
  if [[ "$SKIP_TESTS" == "true" ]]; then
    skip_task "Test phase (skipped due to --skip-tests)"
    return 0
  fi
  
  local STEPS=()
  
  if is_component_enabled "java"; then
    STEPS+=("Running Java tests")
  fi
  
  if is_component_enabled "go"; then
    STEPS+=("Running Go API tests")
  fi
  
  if is_component_enabled "python"; then
    STEPS+=("Running Python tests")
  fi
  
  if [[ "$RUN_POLYGLOT" == "true" ]]; then
    STEPS+=("Running cross-language integration tests")
  fi
  
  # Show execution plan
  show_execution_plan "Test Steps" "${STEPS[@]}"
  
  # Run Java tests
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    
    # Add parallel execution if enabled
    if [[ "$PARALLEL_BUILD" == "true" ]]; then
      echo -e "${BLUE}> PARALLEL:${NC} Using Maven parallel test mode"
      mvn_options="$mvn_options -T 1C -Dsurefire.parallel=classes -DforkCount=2C"
    fi
    
    run_formatted "mvn test $mvn_options" "Running Java tests"
  else
    skip_task "Java tests (Java component disabled)"
  fi
  
  # Run Go tests
  if is_component_enabled "go" && [[ -d "$PROJECT_ROOT/api" ]]; then
    if [[ "$PARALLEL_BUILD" == "true" ]]; then
      # Use Go's built-in parallelism for tests
      run_formatted "cd \"$PROJECT_ROOT/api\" && go test -parallel 4 ./..." "Running Go API tests (parallel mode)"
    else
      run_formatted "cd \"$PROJECT_ROOT/api\" && go test ./..." "Running Go API tests"
    fi
  else
    skip_task "Go tests (Go component disabled or not found)"
  fi
  
  # Run Python tests
  if is_component_enabled "python" && [[ -d "$PROJECT_ROOT/python/tests" ]]; then
    if [[ "$PARALLEL_BUILD" == "true" ]]; then
      # Use pytest-xdist for parallel execution if available
      if pip list | grep -q pytest-xdist; then
        run_formatted "cd \"$PROJECT_ROOT\" && python -m pytest -xvs -n auto python/tests" "Running Python tests (parallel mode)"
      else
        warn_task "pytest-xdist not found, falling back to sequential tests"
        run_formatted "cd \"$PROJECT_ROOT\" && python -m pytest python/tests" "Running Python tests"
      fi
    else
      run_formatted "cd \"$PROJECT_ROOT\" && python -m pytest python/tests" "Running Python tests"
    fi
  else
    skip_task "Python tests (Python component disabled or not found)"
  fi
  
  # Run cross-language integration tests
  if [[ "$RUN_POLYGLOT" == "true" ]] && [[ -f "$PROJECT_ROOT/bin/run-polyglot-tests.sh" ]]; then
    run_formatted "$PROJECT_ROOT/bin/run-polyglot-tests.sh" "Running cross-language integration tests"
  else
    skip_task "Cross-language tests (disabled or not found)"
  fi
  
  return 0
}

# Package phase - create distributable packages
run_package_phase() {
  local STEPS=()
  
  if is_component_enabled "java"; then
    STEPS+=("Packaging Java components")
  fi
  
  STEPS+=("Creating distribution package")
  
  # Show execution plan
  show_execution_plan "Packaging Steps" "${STEPS[@]}"
  
  # Package Java components
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    
    if [[ "$SKIP_TESTS" == "true" ]]; then
      mvn_options="$mvn_options -DskipTests=true"
    fi
    
    run_formatted "mvn package $mvn_options" "Packaging Java components"
  else
    skip_task "Java packaging (Java component disabled)"
  fi
  
  # Create a distributable archive
  start_task "Creating distribution package"
  
  DIST_DIR="$PROJECT_ROOT/target/distribution"
  mkdir -p "$DIST_DIR/bin"
  
  # Copy binaries
  if is_component_enabled "go"; then
    cp "$PROJECT_ROOT/bin/rinnasrv" "$DIST_DIR/bin/" 2>/dev/null || true
  fi
  
  cp "$PROJECT_ROOT/bin/rin"* "$DIST_DIR/bin/" 2>/dev/null || true
  
  # Copy JARs if Java is enabled
  if is_component_enabled "java"; then
    mkdir -p "$DIST_DIR/lib"
    find "$PROJECT_ROOT" -name "*.jar" -not -path "*/target/classes/*" -not -path "*/test-classes/*" -exec cp {} "$DIST_DIR/lib/" \; 2>/dev/null || true
  fi
  
  # Create version file
  if [[ -f "$VERSION_FILE" ]]; then
    cp "$VERSION_FILE" "$DIST_DIR/"
  fi
  
  # Create tarball
  VERSION=$(get_version || echo "unknown")
  DIST_TARBALL="$PROJECT_ROOT/target/rinna-${VERSION}.tar.gz"
  tar -czf "$DIST_TARBALL" -C "$PROJECT_ROOT/target" distribution
  
  complete_task "Package created: $DIST_TARBALL"
  return 0
}

# Verify phase - run quality checks and integration tests
run_verify_phase() {
  if [[ "$SKIP_QUALITY" == "true" ]]; then
    skip_task "Verification phase (skipped due to --skip-quality)"
    return 0
  fi
  
  local STEPS=(
    "Running quality verification"
    "Generating coverage reports"
    "Generating architecture diagrams"
  )
  
  # Show execution plan
  show_execution_plan "Verification Steps" "${STEPS[@]}"
  
  # Run Maven verify with quality checks
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    
    if [[ "$SKIP_TESTS" == "true" ]]; then
      mvn_options="$mvn_options -DskipTests=true"
    fi
    
    run_formatted "mvn verify $mvn_options" "Running quality verification"
  else
    skip_task "Quality verification (Java component disabled)"
  fi
  
  # Generate polyglot coverage report
  if [[ -f "$PROJECT_ROOT/bin/polyglot-coverage.sh" ]]; then
    run_formatted "$PROJECT_ROOT/bin/polyglot-coverage.sh -o html" "Generating coverage reports" \
      || warn_task "Coverage report generation failed - continuing anyway"
  else
    skip_task "Coverage report generation (script not found)"
  fi
  
  # Generate architecture diagrams
  if [[ -f "$PROJECT_ROOT/bin/generate-diagrams.sh" ]]; then
    run_formatted "$PROJECT_ROOT/bin/generate-diagrams.sh --clean" "Generating architecture diagrams" \
      || warn_task "Diagram generation failed - continuing anyway"
  else
    skip_task "Architecture diagram generation (script not found)"
  fi
  
  return 0
}

# Install phase - install artifacts to local repo
run_install_phase() {
  local STEPS=()
  
  if is_component_enabled "java"; then
    STEPS+=("Installing Maven artifacts")
  fi
  
  STEPS+=("Creating CLI tool symlinks")
  
  # Show execution plan
  show_execution_plan "Installation Steps" "${STEPS[@]}"
  
  # Install Maven artifacts
  if is_component_enabled "java"; then
    local mvn_options="-P $BUILD_PROFILE"
    
    if [[ "$SKIP_TESTS" == "true" ]]; then
      mvn_options="$mvn_options -DskipTests=true"
    fi
    
    run_formatted "mvn install $mvn_options" "Installing Maven artifacts"
  else
    skip_task "Maven installation (Java component disabled)"
  fi
  
  # Create symbolic links for CLI tools if requested
  if [[ "${INSTALL_CLI_LINKS:-false}" == "true" ]]; then
    start_task "Creating CLI tool symlinks"
    
    mkdir -p "$HOME/.local/bin"
    local link_count=0
    
    for cmd in rin rin-add rin-list rin-version; do
      if [[ -f "$PROJECT_ROOT/bin/$cmd" ]]; then
        ln -sf "$PROJECT_ROOT/bin/$cmd" "$HOME/.local/bin/$cmd"
        ((link_count++))
      fi
    done
    
    if [[ $link_count -gt 0 ]]; then
      complete_task "CLI tools installed to $HOME/.local/bin ($link_count tools)"
    else
      skip_task "No CLI tools found to link"
    fi
  else
    skip_task "CLI tool symlinks (not requested)"
  fi
  
  # Display final message
  echo ""
  echo -e "${GREEN}${BOLD}Rinna build completed successfully${NC}"
  echo ""
  echo -e "You can now run the CLI tools from ${BLUE}$PROJECT_ROOT/bin${NC}"
  echo -e "Or use the Maven artifacts from ${BLUE}$PROJECT_ROOT/target${NC}"
  
  # Show optimization hint
  if [[ "$PARALLEL_BUILD" != "true" ]]; then
    echo ""
    echo -e "${CYAN}TIP: Use ${BOLD}--parallel${NC}${CYAN} flag for faster builds${NC}"
  fi
  echo ""
  
  return 0
}

# Run the main function
main "$@"