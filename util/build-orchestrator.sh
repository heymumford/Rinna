#!/usr/bin/env bash
#
# build-orchestrator.sh - Single point of truth for Rinna build process
#
# PURPOSE: Orchestrate the entire build process with build number incrementing and summary reports
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# 
# This source code is licensed under the MIT License
# found in the LICENSE file in the root directory of this source tree.
#

set -eo pipefail

# Determine script and project directories
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
RINNA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_DIR="$RINNA_DIR/logs"

# Ensure the log directory exists
mkdir -p "$LOG_DIR"

# Set up logging
BUILD_LOG="$LOG_DIR/build-$(date +%Y%m%d-%H%M%S).log"
SUMMARY_LOG="$LOG_DIR/build-summary-latest.log"

# Version file paths
VERSION_FILE="$RINNA_DIR/version.properties"
VERSION_SERVICE_PROPS="$RINNA_DIR/version-service/version.properties"

# Build artifact directories
TARGET_DIR="$RINNA_DIR/target"
DIST_DIR="$TARGET_DIR/distribution"

# Create artifact directories
mkdir -p "$TARGET_DIR" "$DIST_DIR"

# Test results directory
TEST_RESULTS_DIR="$TARGET_DIR/test-results"
mkdir -p "$TEST_RESULTS_DIR"

# Define colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Status indicators
STATUS_PENDING="🔄"
STATUS_SUCCESS="✅"
STATUS_FAILURE="❌"
STATUS_SKIPPED="⏭️"
STATUS_WARNING="⚠️"

# Default options
SKIP_TESTS=false
SKIP_QUALITY=false
SKIP_VERSION_INCREMENT=false
COMPONENTS=("java" "go" "python")
VERBOSE=false
HELP=false
BUILD_ENV="local"
RELEASE=false
PUSH_VERSION=false
QUICK=false

# Version information
VERSION_MAJOR=""
VERSION_MINOR=""
VERSION_PATCH=""
VERSION_QUALIFIER=""
BUILD_NUMBER=""
GIT_COMMIT=""
GIT_RELEASE=""

# Test statistics
JAVA_TESTS_TOTAL=0
JAVA_TESTS_PASSED=0
JAVA_TESTS_FAILED=0
GO_TESTS_TOTAL=0
GO_TESTS_PASSED=0
GO_TESTS_FAILED=0
PYTHON_TESTS_TOTAL=0
PYTHON_TESTS_PASSED=0
PYTHON_TESTS_FAILED=0

# Time tracking
BUILD_START_TIME=$(date +%s)
TASK_START_TIME=0

# Function to show usage
show_usage() {
  cat << EOF
${BOLD}Rinna Build Orchestrator${NC}

A unified build script that manages all build processes for the Rinna project.

${BOLD}Usage:${NC} $(basename "$0") [options]

${BOLD}Options:${NC}
  --skip-tests              Skip all tests
  --skip-quality            Skip quality checks
  --skip-version-increment  Don't increment the build number
  --components=LIST         Comma-separated list of components to build (java,go,python)
  --env=ENV                 Build environment (local, ci, prod) [default: local]
  --release                 Create a release build
  --push-version            Push version changes to git
  --quick                   Quick build (skips tests and quality checks)
  --verbose                 Show verbose output
  --help                    Show this help message

${BOLD}Examples:${NC}
  $(basename "$0")                    # Run full build with default settings
  $(basename "$0") --skip-tests       # Run build without tests
  $(basename "$0") --release          # Create a release build
  $(basename "$0") --components=java  # Build only Java components
  $(basename "$0") --env=ci           # Run in CI environment
EOF
}

# Function to parse command line arguments
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
      --skip-version-increment)
        SKIP_VERSION_INCREMENT=true
        shift
        ;;
      --components=*)
        IFS=',' read -r -a COMPONENTS <<< "${1#*=}"
        shift
        ;;
      --env=*)
        BUILD_ENV="${1#*=}"
        shift
        ;;
      --release)
        RELEASE=true
        shift
        ;;
      --push-version)
        PUSH_VERSION=true
        shift
        ;;
      --quick)
        QUICK=true
        SKIP_TESTS=true
        SKIP_QUALITY=true
        shift
        ;;
      --verbose)
        VERBOSE=true
        shift
        ;;
      --help|-h)
        HELP=true
        shift
        ;;
      *)
        echo -e "${RED}ERROR: Unknown option: $1${NC}" >&2
        show_usage
        exit 1
        ;;
    esac
  done
}

# Function to log messages
log_info() {
  local message="$1"
  echo -e "ℹ️ ${message}" | tee -a "$BUILD_LOG"
}

log_error() {
  local message="$1"
  echo -e "${RED}ERROR: ${message}${NC}" | tee -a "$BUILD_LOG" >&2
}

log_warning() {
  local message="$1"
  echo -e "${YELLOW}WARNING: ${message}${NC}" | tee -a "$BUILD_LOG" >&2
}

log_success() {
  local message="$1"
  echo -e "${GREEN}${message}${NC}" | tee -a "$BUILD_LOG"
}

# Task status functions
start_task() {
  local message="$1"
  echo -e "${STATUS_PENDING} ${BOLD}${message}...${NC}" | tee -a "$BUILD_LOG"
  TASK_START_TIME=$(date +%s)
}

complete_task() {
  local message="$1"
  local elapsed=""
  
  if [[ -n "$TASK_START_TIME" ]]; then
    local end_time=$(date +%s)
    local duration=$((end_time - TASK_START_TIME))
    elapsed=" (${duration}s)"
    TASK_START_TIME=0
  fi
  
  echo -e "\r\033[K${STATUS_SUCCESS} ${message}${elapsed}" | tee -a "$BUILD_LOG"
}

fail_task() {
  local message="$1"
  local error="${2:-}"
  local elapsed=""
  
  if [[ -n "$TASK_START_TIME" ]]; then
    local end_time=$(date +%s)
    local duration=$((end_time - TASK_START_TIME))
    elapsed=" (${duration}s)"
    TASK_START_TIME=0
  fi
  
  echo -e "\r\033[K${STATUS_FAILURE} ${message}${elapsed}" | tee -a "$BUILD_LOG"
  if [[ -n "$error" ]]; then
    echo -e "   ${RED}${error}${NC}" | tee -a "$BUILD_LOG"
  fi
}

skip_task() {
  local message="$1"
  echo -e "\r\033[K${STATUS_SKIPPED} ${message} - skipped" | tee -a "$BUILD_LOG"
}

warn_task() {
  local message="$1"
  echo -e "${STATUS_WARNING} ${message}" | tee -a "$BUILD_LOG"
}

# Function to display a section header
section_header() {
  local title="$1"
  local dash_length=$(($(tput cols) - ${#title} - 4))
  local dashes=$(printf '%*s' "$dash_length" | tr ' ' '─')
  
  echo "" | tee -a "$BUILD_LOG"
  echo -e "${BLUE}${BOLD}╭─ ${title} ${dashes}${NC}" | tee -a "$BUILD_LOG"
  echo "" | tee -a "$BUILD_LOG"
}

# Function to display a section footer
section_footer() {
  local dash_length=$(tput cols)
  local dashes=$(printf '%*s' "$dash_length" | tr ' ' '─')
  
  echo "" | tee -a "$BUILD_LOG"
  echo -e "${BLUE}${BOLD}╰${dashes}${NC}" | tee -a "$BUILD_LOG"
  echo "" | tee -a "$BUILD_LOG"
}

# Function to run a command with formatted output
run_command() {
  local cmd="$1"
  local description="$2"
  local skip_flag="${3:-false}"
  local allow_fail="${4:-false}"
  
  if [[ "$skip_flag" == "true" ]]; then
    skip_task "$description"
    return 0
  fi
  
  start_task "$description"
  
  # Create a temporary file for output
  local temp_file=$(mktemp)
  
  # Run the command and capture output
  if eval "$cmd" > "$temp_file" 2>&1; then
    complete_task "$description"
    if [[ "${VERBOSE:-false}" == "true" ]]; then
      echo "" | tee -a "$BUILD_LOG"
      cat "$temp_file" | tee -a "$BUILD_LOG"
      echo "" | tee -a "$BUILD_LOG"
    fi
    rm -f "$temp_file"
    return 0
  else
    local exit_code=$?
    fail_task "$description" "Command failed with exit code $exit_code"
    echo "" | tee -a "$BUILD_LOG"
    cat "$temp_file" | tee -a "$BUILD_LOG"
    echo "" | tee -a "$BUILD_LOG"
    rm -f "$temp_file"
    
    if [[ "$allow_fail" == "true" ]]; then
      warn_task "Continuing despite failure (allow_fail=true)"
      return 0
    fi
    
    return $exit_code
  fi
}

# Function to check if a component is enabled
is_component_enabled() {
  local component="${1,,}" # Convert to lowercase
  for c in "${COMPONENTS[@]}"; do
    if [[ "${c,,}" == "$component" ]]; then
      return 0
    fi
  done
  return 1
}

# Function to read the current version
read_version() {
  section_header "Reading Version Information"
  
  if [[ ! -f "$VERSION_FILE" ]]; then
    log_error "Version file not found: $VERSION_FILE"
    exit 1
  fi
  
  # Read version components from the version file
  VERSION_MAJOR=$(grep "^version.major=" "$VERSION_FILE" | cut -d= -f2)
  VERSION_MINOR=$(grep "^version.minor=" "$VERSION_FILE" | cut -d= -f2)
  VERSION_PATCH=$(grep "^version.patch=" "$VERSION_FILE" | cut -d= -f2)
  VERSION_QUALIFIER=$(grep "^version.qualifier=" "$VERSION_FILE" | cut -d= -f2)
  BUILD_NUMBER=$(grep "^buildNumber=" "$VERSION_FILE" | cut -d= -f2)
  
  # Check if version components were found
  if [[ -z "$VERSION_MAJOR" || -z "$VERSION_MINOR" || -z "$VERSION_PATCH" ]]; then
    log_error "Could not read version information from $VERSION_FILE"
    exit 1
  fi
  
  # Get Git information
  GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
  GIT_RELEASE=$(git describe --tags --always 2>/dev/null || echo "unknown")
  
  # Log the current version
  log_info "Current Version: ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}${VERSION_QUALIFIER}"
  log_info "Current Build Number: ${BUILD_NUMBER}"
  log_info "Git Commit: ${GIT_COMMIT}"
  log_info "Git Release: ${GIT_RELEASE}"
  
  section_footer
}

# Function to increment the build number
increment_build_number() {
  if [[ "$SKIP_VERSION_INCREMENT" == "true" ]]; then
    log_info "Skipping build number increment (--skip-version-increment flag is set)"
    return 0
  }
  
  section_header "Incrementing Build Number"
  
  # Read the current build number
  local current_build_number=$BUILD_NUMBER
  
  # Increment the build number
  local new_build_number=$((current_build_number + 1))
  log_info "Incrementing build number from $current_build_number to $new_build_number"
  
  # Update the build number in the version file
  if [[ -f "$VERSION_FILE" ]]; then
    sed -i "s/^buildNumber=.*/buildNumber=$new_build_number/" "$VERSION_FILE"
    BUILD_NUMBER="$new_build_number"
    log_success "Updated build number in $VERSION_FILE"
  else
    log_error "Version file not found: $VERSION_FILE"
    exit 1
  fi
  
  # Also update the build number in the version service if it exists
  if [[ -f "$VERSION_SERVICE_PROPS" ]]; then
    sed -i "s/^buildNumber=.*/buildNumber=$new_build_number/" "$VERSION_SERVICE_PROPS"
    log_success "Updated build number in $VERSION_SERVICE_PROPS"
  fi
  
  # Update the build timestamp
  local build_timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  sed -i "s/^build.timestamp=.*/build.timestamp=$build_timestamp/" "$VERSION_FILE"
  
  # Update the git commit hash
  if [[ "$GIT_COMMIT" != "unknown" ]]; then
    sed -i "s/^build.git.commit=.*/build.git.commit=$GIT_COMMIT/" "$VERSION_FILE"
  fi
  
  # Also update the lastUpdated date
  local today=$(date +"%Y-%m-%d")
  sed -i "s/^lastUpdated=.*/lastUpdated=$today/" "$VERSION_FILE"
  
  section_footer
}

# Function to initialize the build environment
initialize_build() {
  section_header "Initializing Build Environment"
  
  # Create required directories
  run_command "mkdir -p \"$TARGET_DIR\" \"$DIST_DIR\" \"$TEST_RESULTS_DIR\"" "Creating build directories"
  
  # Check for required tools based on enabled components
  local missing_tools=false
  
  if is_component_enabled "java"; then
    if ! command -v java &>/dev/null || ! command -v mvn &>/dev/null; then
      log_error "Java and Maven are required for building Java components"
      missing_tools=true
    else
      local java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
      if [[ "$java_version" -lt 21 ]]; then
        log_error "Java 21 or higher is required (found Java $java_version)"
        missing_tools=true
      else
        log_info "Using Java $java_version"
      fi
    fi
  fi
  
  if is_component_enabled "go"; then
    if ! command -v go &>/dev/null; then
      log_error "Go is required for building Go components"
      missing_tools=true
    else
      local go_version=$(go version | awk '{print $3}' | cut -c 3-)
      log_info "Using Go $go_version"
    fi
  fi
  
  if is_component_enabled "python"; then
    if ! command -v python3 &>/dev/null || ! command -v pip &>/dev/null; then
      log_error "Python 3 and pip are required for building Python components"
      missing_tools=true
    else
      local python_version=$(python3 --version 2>&1 | awk '{print $2}')
      log_info "Using Python $python_version"
    fi
  fi
  
  if [[ "$missing_tools" == "true" ]]; then
    log_error "Missing required tools for the build"
    exit 1
  fi
  
  # Set environment-specific variables
  case "$BUILD_ENV" in
    local)
      log_info "Building in local environment"
      ;;
    ci)
      log_info "Building in CI environment"
      ;;
    prod)
      log_info "Building in production environment"
      ;;
    *)
      log_error "Unknown build environment: $BUILD_ENV"
      exit 1
      ;;
  esac
  
  # Activate environment if available
  if [[ -f "$RINNA_DIR/activate-rinna.sh" ]]; then
    run_command "source \"$RINNA_DIR/activate-rinna.sh\"" "Activating Rinna environment"
  fi
  
  section_footer
}

# Function to build Java components
build_java() {
  if ! is_component_enabled "java"; then
    return 0
  }
  
  section_header "Building Java Components"
  
  local mvn_profile
  local mvn_options=""
  
  case "$BUILD_ENV" in
    local)
      mvn_profile="local-quality"
      ;;
    ci)
      mvn_profile="ci"
      ;;
    prod)
      mvn_profile="production"
      ;;
  esac
  
  if [[ "$SKIP_TESTS" == "true" ]]; then
    mvn_options="$mvn_options -DskipTests=true"
  fi
  
  if [[ "$SKIP_QUALITY" == "true" ]]; then
    mvn_options="$mvn_options -P skip-quality"
  else
    mvn_options="$mvn_options -P $mvn_profile"
  fi
  
  if [[ "$RELEASE" == "true" ]]; then
    mvn_options="$mvn_options -P release"
  fi
  
  # Clean the project
  run_command "mvn clean $mvn_options" "Cleaning Java project"
  
  # Compile the project
  run_command "mvn compile $mvn_options" "Compiling Java project"
  
  # Run tests if not skipped
  if [[ "$SKIP_TESTS" != "true" ]]; then
    run_command "mvn test $mvn_options" "Running Java tests"
    
    # Collect test statistics
    if [[ -d "$RINNA_DIR/target/surefire-reports" ]]; then
      JAVA_TESTS_TOTAL=$(find "$RINNA_DIR" -name "TEST-*.xml" | xargs grep -l "<testsuite" | wc -l)
      JAVA_TESTS_FAILED=$(find "$RINNA_DIR" -name "TEST-*.xml" | xargs grep -l "failures=\"[1-9]" | wc -l)
      JAVA_TESTS_PASSED=$((JAVA_TESTS_TOTAL - JAVA_TESTS_FAILED))
      
      log_info "Java Tests: $JAVA_TESTS_PASSED passed, $JAVA_TESTS_FAILED failed, $JAVA_TESTS_TOTAL total"
      
      # Copy test results
      mkdir -p "$TEST_RESULTS_DIR/java"
      cp -r "$RINNA_DIR/target/surefire-reports" "$TEST_RESULTS_DIR/java/"
    fi
  fi
  
  # Package the project
  run_command "mvn package $mvn_options" "Packaging Java project"
  
  # Run verification if quality checks are not skipped
  if [[ "$SKIP_QUALITY" != "true" ]]; then
    run_command "mvn verify $mvn_options" "Verifying Java project"
  fi
  
  # Copy JARs to the distribution directory
  mkdir -p "$DIST_DIR/lib"
  find "$RINNA_DIR" -name "*.jar" -not -path "*/target/classes/*" -not -path "*/test-classes/*" -exec cp {} "$DIST_DIR/lib/" \; 2>/dev/null || true
  
  section_footer
}

# Function to build Go components
build_go() {
  if ! is_component_enabled "go"; then
    return 0
  }
  
  section_header "Building Go Components"
  
  # Set Go build flags
  local build_flags=(
    "-X" "github.com/heymumford/rinna/api/pkg/health.Version=${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"
    "-X" "github.com/heymumford/rinna/api/pkg/health.BuildNumber=${BUILD_NUMBER}"
    "-X" "github.com/heymumford/rinna/api/pkg/health.CommitSHA=${GIT_COMMIT}"
    "-X" "github.com/heymumford/rinna/api/pkg/health.BuildTime=$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  )
  
  # Change to the API directory
  cd "$RINNA_DIR/api"
  
  # Clean the Go modules
  run_command "go clean -cache" "Cleaning Go cache"
  
  # Get dependencies
  run_command "go mod download" "Downloading Go dependencies"
  
  # Build the API server
  run_command "go build -ldflags=\"${build_flags[*]}\" -o \"$RINNA_DIR/bin/rinnasrv\" ./cmd/rinnasrv" "Building Go API server"
  
  # Build the healthcheck utility
  if [[ -d "$RINNA_DIR/api/cmd/healthcheck" ]]; then
    run_command "go build -ldflags=\"${build_flags[*]}\" -o \"$RINNA_DIR/bin/healthcheck\" ./cmd/healthcheck" "Building Go healthcheck utility"
  fi
  
  # Run tests if not skipped
  if [[ "$SKIP_TESTS" != "true" ]]; then
    run_command "go test ./..." "Running Go tests"
    
    # Collect test statistics
    GO_TESTS_TOTAL=$(go test -list '.' ./... 2>/dev/null | grep -v '^ok' | grep -v '^?' | wc -l)
    GO_TESTS_PASSED=$GO_TESTS_TOTAL # Assume all passed, we'll subtract failures
    
    # Run tests with JSON output to capture failures
    local go_test_output=$(mktemp)
    go test -json ./... > "$go_test_output" 2>/dev/null || true
    GO_TESTS_FAILED=$(grep -c '"Action":"fail"' "$go_test_output" || echo "0")
    GO_TESTS_PASSED=$((GO_TESTS_TOTAL - GO_TESTS_FAILED))
    
    log_info "Go Tests: $GO_TESTS_PASSED passed, $GO_TESTS_FAILED failed, $GO_TESTS_TOTAL total"
    
    # Copy test results
    mkdir -p "$TEST_RESULTS_DIR/go"
    cp "$go_test_output" "$TEST_RESULTS_DIR/go/test-output.json"
    rm -f "$go_test_output"
  fi
  
  # Copy binaries to the distribution directory
  mkdir -p "$DIST_DIR/bin"
  cp "$RINNA_DIR/bin/rinnasrv" "$DIST_DIR/bin/" 2>/dev/null || true
  cp "$RINNA_DIR/bin/healthcheck" "$DIST_DIR/bin/" 2>/dev/null || true
  
  # Change back to the root directory
  cd "$RINNA_DIR"
  
  section_footer
}

# Function to build Python components
build_python() {
  if ! is_component_enabled "python"; then
    return 0
  }
  
  section_header "Building Python Components"
  
  # Change to the Python directory
  cd "$RINNA_DIR/python"
  
  # Install Python package in development mode
  run_command "pip install -e ." "Installing Python package"
  
  # Run tests if not skipped
  if [[ "$SKIP_TESTS" != "true" && -d "$RINNA_DIR/python/tests" ]]; then
    run_command "python -m pytest tests" "Running Python tests"
    
    # Collect test statistics if pytest-json-report is installed
    if pip list | grep -q "pytest-json-report"; then
      run_command "python -m pytest tests --json-report --json-report-file=\"$TEST_RESULTS_DIR/python/test-report.json\"" \
        "Generating Python test report"
      
      if [[ -f "$TEST_RESULTS_DIR/python/test-report.json" ]]; then
        PYTHON_TESTS_TOTAL=$(grep -o '"total":[0-9]*' "$TEST_RESULTS_DIR/python/test-report.json" | cut -d: -f2)
        PYTHON_TESTS_FAILED=$(grep -o '"failed":[0-9]*' "$TEST_RESULTS_DIR/python/test-report.json" | cut -d: -f2)
        PYTHON_TESTS_PASSED=$((PYTHON_TESTS_TOTAL - PYTHON_TESTS_FAILED))
        
        log_info "Python Tests: $PYTHON_TESTS_PASSED passed, $PYTHON_TESTS_FAILED failed, $PYTHON_TESTS_TOTAL total"
      fi
    else
      log_warning "pytest-json-report not installed - can't collect detailed Python test statistics"
      # Rough estimate of test count
      PYTHON_TESTS_TOTAL=$(find "$RINNA_DIR/python/tests" -name "test_*.py" | wc -l)
      PYTHON_TESTS_PASSED=$PYTHON_TESTS_TOTAL
      PYTHON_TESTS_FAILED=0
    fi
  fi
  
  # Create Python package distribution
  run_command "python setup.py sdist bdist_wheel" "Creating Python distribution package"
  
  # Copy Python packages to the distribution directory
  mkdir -p "$DIST_DIR/python"
  cp -r dist/* "$DIST_DIR/python/" 2>/dev/null || true
  
  # Change back to the root directory
  cd "$RINNA_DIR"
  
  section_footer
}

# Function to create the distribution package
create_distribution() {
  section_header "Creating Distribution Package"
  
  # Create bin directory and copy scripts
  mkdir -p "$DIST_DIR/bin"
  cp "$RINNA_DIR/bin/rin"* "$DIST_DIR/bin/" 2>/dev/null || true
  chmod +x "$DIST_DIR/bin/"* 2>/dev/null || true
  
  # Copy configuration files
  mkdir -p "$DIST_DIR/config"
  cp -r "$RINNA_DIR/config"/* "$DIST_DIR/config/" 2>/dev/null || true
  
  # Copy version information
  cp "$VERSION_FILE" "$DIST_DIR/" 2>/dev/null || true
  
  # Create README for the distribution
  cat > "$DIST_DIR/README.md" << EOF
# Rinna ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH} (Build ${BUILD_NUMBER})

Built on: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
Git commit: ${GIT_COMMIT}
Git release: ${GIT_RELEASE}

## Components

- Java: $(is_component_enabled "java" && echo "Included" || echo "Not included")
- Go: $(is_component_enabled "go" && echo "Included" || echo "Not included")
- Python: $(is_component_enabled "python" && echo "Included" || echo "Not included")

## Installation

Please refer to the documentation for installation instructions.
EOF
  
  # Create the distribution archive
  local dist_name="rinna-${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}-b${BUILD_NUMBER}"
  local dist_archive="$TARGET_DIR/${dist_name}.tar.gz"
  
  run_command "tar -czf \"$dist_archive\" -C \"$TARGET_DIR\" distribution" "Creating distribution archive: $(basename "$dist_archive")"
  
  log_success "Distribution package created: $dist_archive"
  
  section_footer
}

# Function to push version changes to git
push_version_changes() {
  if [[ "$PUSH_VERSION" != "true" ]]; then
    return 0
  }
  
  section_header "Pushing Version Changes"
  
  # Add the version files to git
  run_command "git add \"$VERSION_FILE\" \"$VERSION_SERVICE_PROPS\"" "Adding version files to git"
  
  # Commit the changes
  local commit_message="Build ${BUILD_NUMBER}: Version ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"
  run_command "git commit -m \"$commit_message\"" "Committing version changes" true
  
  # Push the changes if successful
  if [[ $? -eq 0 ]]; then
    run_command "git push" "Pushing version changes" true
  else
    log_warning "No changes to commit or commit failed"
  fi
  
  # Create a tag if this is a release build
  if [[ "$RELEASE" == "true" ]]; then
    local tag_name="v${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"
    run_command "git tag -a \"$tag_name\" -m \"Release ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH} (Build ${BUILD_NUMBER})\"" \
      "Creating release tag: $tag_name" true
    
    # Push the tag if successful
    if [[ $? -eq 0 ]]; then
      run_command "git push origin \"$tag_name\"" "Pushing release tag" true
    fi
  fi
  
  section_footer
}

# Function to generate a build summary
generate_summary() {
  section_header "Build Summary"
  
  # Calculate build duration
  local build_end_time=$(date +%s)
  local build_duration=$((build_end_time - BUILD_START_TIME))
  local duration_min=$((build_duration / 60))
  local duration_sec=$((build_duration % 60))
  
  # Create summary report
  cat > "$SUMMARY_LOG" << EOF
# Rinna Build Summary

- **Version**: ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}
- **Build Number**: ${BUILD_NUMBER}
- **Git Commit**: ${GIT_COMMIT}
- **Git Release**: ${GIT_RELEASE}
- **Build Date**: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
- **Build Duration**: ${duration_min}m ${duration_sec}s

## Components Built
$(is_component_enabled "java" && echo "- ✅ Java" || echo "- ❌ Java (skipped)")
$(is_component_enabled "go" && echo "- ✅ Go" || echo "- ❌ Go (skipped)")
$(is_component_enabled "python" && echo "- ✅ Python" || echo "- ❌ Python (skipped)")

## Test Results
- Java: ${JAVA_TESTS_PASSED} passed, ${JAVA_TESTS_FAILED} failed, ${JAVA_TESTS_TOTAL} total
- Go: ${GO_TESTS_PASSED} passed, ${GO_TESTS_FAILED} failed, ${GO_TESTS_TOTAL} total
- Python: ${PYTHON_TESTS_PASSED} passed, ${PYTHON_TESTS_FAILED} failed, ${PYTHON_TESTS_TOTAL} total

## Artifacts
- Distribution: ${TARGET_DIR}/rinna-${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}-b${BUILD_NUMBER}.tar.gz
- Test Reports: ${TEST_RESULTS_DIR}
- Build Log: ${BUILD_LOG}

## Build Configuration
- Skip Tests: $([ "$SKIP_TESTS" == "true" ] && echo "Yes" || echo "No")
- Skip Quality: $([ "$SKIP_QUALITY" == "true" ] && echo "Yes" || echo "No")
- Environment: ${BUILD_ENV}
- Release Build: $([ "$RELEASE" == "true" ] && echo "Yes" || echo "No")
EOF
  
  # Display the summary
  cat "$SUMMARY_LOG"
  
  log_success "Build summary saved to: $SUMMARY_LOG"
  
  section_footer
}

# Main function
main() {
  # Parse command line options
  parse_options "$@"
  
  # Show help and exit if requested
  if [[ "$HELP" == "true" ]]; then
    show_usage
    exit 0
  fi
  
  # Print build configuration
  section_header "Rinna Build Orchestrator"
  
  log_info "Starting build with the following configuration:"
  log_info "- Build Environment: $BUILD_ENV"
  log_info "- Components: ${COMPONENTS[*]}"
  log_info "- Skip Tests: $SKIP_TESTS"
  log_info "- Skip Quality: $SKIP_QUALITY"
  log_info "- Release Build: $RELEASE"
  
  # Read the current version
  read_version
  
  # Initialize the build environment
  initialize_build
  
  # Increment the build number
  increment_build_number
  
  # Build each component
  build_java
  build_go
  build_python
  
  # Create distribution package
  create_distribution
  
  # Push version changes to git if requested
  push_version_changes
  
  # Generate build summary
  generate_summary
  
  # Clean up
  echo ""
  log_success "Build completed successfully!"
  echo ""
  
  return 0
}

# Run the main function with all arguments
main "$@"