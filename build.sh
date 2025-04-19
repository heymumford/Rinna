#!/usr/bin/env bash
#
# build.sh - Consolidated build script for Rinna
#
# PURPOSE: Efficiently orchestrates the entire build process with minimal verbosity
#          and uses consolidated configuration files from the build/ directory
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# 
# This source code is licensed under the MIT License
# found in the LICENSE file in the root directory of this source tree.
#

set -eo pipefail

# Core variables - minimal and efficient
SCRIPT_DIR="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
RINNA_DIR="$(cd "$SCRIPT_DIR" && pwd)"
BUILD_DIR="$RINNA_DIR/build"
CONFIG_DIR="$BUILD_DIR/config"
LOG_DIR="$RINNA_DIR/logs"
VERSION_FILE="$RINNA_DIR/version.properties"
VERSION_SERVICE_PROPS="$RINNA_DIR/build/version-service/version.properties"
TARGET_DIR="$RINNA_DIR/target"
DIST_DIR="$TARGET_DIR/distribution"
TEST_RESULTS_DIR="$TARGET_DIR/test-results"

# Create required directories at once
mkdir -p "$LOG_DIR" "$TARGET_DIR" "$DIST_DIR" "$TEST_RESULTS_DIR"

# Set up logging - simplified
BUILD_LOG="$LOG_DIR/build-$(date +%Y%m%d-%H%M%S).log"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

# Status emojis - kept for UX consistency
SUCCESS="✅"
FAILURE="❌"
PENDING="🔄"
SKIPPED="⏭️" 
WARNING="⚠️"

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
PARALLEL_BUILD=true

# Begin tracking build time
BUILD_START_TIME=$(date +%s)

# Function to show usage - simplified but complete
show_usage() {
  cat << EOF
${BOLD}Rinna Build Script${NC}

USAGE: $(basename "$0") [options]

OPTIONS:
  --skip-tests              Skip all tests
  --skip-quality            Skip quality checks
  --skip-version-increment  Don't increment the build number
  --components=LIST         Comma-separated list of components (java,go,python)
  --env=ENV                 Environment: local, ci, prod [default: local]
  --release                 Create a release build
  --push-version            Push version changes to git
  --quick                   Quick build (minimal checks)
  --verbose                 Show verbose output
  --help                    Show this help message
EOF
}

# Optimized command line argument parsing
parse_options() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --skip-tests) SKIP_TESTS=true; shift ;;
      --skip-quality) SKIP_QUALITY=true; shift ;;
      --skip-version-increment) SKIP_VERSION_INCREMENT=true; shift ;;
      --components=*) IFS=',' read -r -a COMPONENTS <<< "${1#*=}"; shift ;;
      --env=*) BUILD_ENV="${1#*=}"; shift ;;
      --release) RELEASE=true; shift ;;
      --push-version) PUSH_VERSION=true; shift ;;
      --quick) QUICK=true; SKIP_TESTS=true; SKIP_QUALITY=true; shift ;;
      --verbose) VERBOSE=true; shift ;;
      --help|-h) HELP=true; shift ;;
      *) echo -e "${RED}ERROR: Unknown option: $1${NC}" >&2; show_usage; exit 1 ;;
    esac
  done
}

# Simplified logging functions
log() { echo -e "$1" | tee -a "$BUILD_LOG"; }
log_info() { log "ℹ️ $1"; }
log_error() { log "${RED}ERROR: $1${NC}" >&2; }
log_warning() { log "${YELLOW}WARNING: $1${NC}" >&2; }
log_success() { log "${GREEN}$1${NC}"; }

# Task status functions - streamlined
start_task() {
  echo -e "${PENDING} ${BOLD}$1...${NC}" | tee -a "$BUILD_LOG"
  TASK_START_TIME=$(date +%s)
}

complete_task() {
  local elapsed=""
  if [[ -n "$TASK_START_TIME" ]]; then
    local duration=$(($(date +%s) - TASK_START_TIME))
    elapsed=" (${duration}s)"
    TASK_START_TIME=0
  fi
  echo -e "\r\033[K${SUCCESS} $1${elapsed}" | tee -a "$BUILD_LOG"
}

fail_task() {
  local elapsed=""
  if [[ -n "$TASK_START_TIME" ]]; then
    local duration=$(($(date +%s) - TASK_START_TIME))
    elapsed=" (${duration}s)"
    TASK_START_TIME=0
  fi
  echo -e "\r\033[K${FAILURE} $1${elapsed}" | tee -a "$BUILD_LOG"
  [[ -n "$2" ]] && echo -e "   ${RED}$2${NC}" | tee -a "$BUILD_LOG"
}

skip_task() {
  echo -e "${SKIPPED} $1 - skipped" | tee -a "$BUILD_LOG"
}

# Section headers - minimal but clear
section_header() {
  echo -e "\n${BLUE}${BOLD}== $1 ==${NC}\n" | tee -a "$BUILD_LOG"
}

# Optimized command runner
run_command() {
  local cmd="$1"
  local description="$2"
  local skip_flag="${3:-false}"
  local allow_fail="${4:-false}"

  [[ "$skip_flag" == "true" ]] && { skip_task "$description"; return 0; }

  start_task "$description"

  # Run command and capture output directly without temp files
  if output=$(eval "$cmd" 2>&1); then
    complete_task "$description"
    [[ "${VERBOSE}" == "true" ]] && echo "$output" | tee -a "$BUILD_LOG"
    return 0
  else
    local exit_code=$?
    fail_task "$description" "Failed with code $exit_code"
    echo "$output" | tee -a "$BUILD_LOG"

    [[ "$allow_fail" == "true" ]] && return 0
    return $exit_code
  fi
}

# Efficient component check
is_component_enabled() {
  local component=$(echo "$1" | tr '[:upper:]' '[:lower:]')
  for c in "${COMPONENTS[@]}"; do
    [[ "$(echo "$c" | tr '[:upper:]' '[:lower:]')" == "$component" ]] && return 0
  done
  return 1
}

# Optimized version information reading
read_version() {
  section_header "Version Information"

  if [[ ! -f "$VERSION_FILE" ]]; then
    log_error "Version file not found: $VERSION_FILE"
    exit 1
  fi

  # Read version data in one pass to avoid multiple file reads
  VERSION_MAJOR=$(grep "^version.major=" "$VERSION_FILE" | cut -d= -f2)
  VERSION_MINOR=$(grep "^version.minor=" "$VERSION_FILE" | cut -d= -f2)
  VERSION_PATCH=$(grep "^version.patch=" "$VERSION_FILE" | cut -d= -f2)
  VERSION_QUALIFIER=$(grep "^version.qualifier=" "$VERSION_FILE" | cut -d= -f2)
  BUILD_NUMBER=$(grep "^buildNumber=" "$VERSION_FILE" | cut -d= -f2)

  # Validate version information
  if [[ -z "$VERSION_MAJOR" || -z "$VERSION_MINOR" || -z "$VERSION_PATCH" ]]; then
    log_error "Could not read version information"
    exit 1
  fi

  # Get Git information efficiently
  GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
  GIT_RELEASE=$(git describe --tags --always 2>/dev/null || echo "unknown")

  log_info "Version: ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}${VERSION_QUALIFIER} (Build ${BUILD_NUMBER})"
  log_info "Git: ${GIT_COMMIT} / ${GIT_RELEASE}"
}

# Increment build number
increment_build_number() {
  [[ "$SKIP_VERSION_INCREMENT" == "true" ]] && return 0

  section_header "Incrementing Build Number"

  # Read current build number
  local current_build_number=$BUILD_NUMBER
  local new_build_number=$((current_build_number + 1))

  log_info "Incrementing build: $current_build_number → $new_build_number"

  # Update version files in a single operation if possible
  if [[ -f "$VERSION_FILE" ]]; then
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    local today=$(date +"%Y-%m-%d")

    # Update multiple properties at once to reduce file operations
    sed -i -e "s/^buildNumber=.*/buildNumber=$new_build_number/" \
           -e "s/^build.timestamp=.*/build.timestamp=$timestamp/" \
           -e "s/^build.git.commit=.*/build.git.commit=$GIT_COMMIT/" \
           -e "s/^lastUpdated=.*/lastUpdated=$today/" "$VERSION_FILE"

    BUILD_NUMBER="$new_build_number"
    log_success "Updated build number in $VERSION_FILE"
  fi

  # Update version service properties if it exists
  [[ -f "$VERSION_SERVICE_PROPS" ]] && \
    sed -i "s/^buildNumber=.*/buildNumber=$new_build_number/" "$VERSION_SERVICE_PROPS"
}

# Initialize build environment
initialize_build() {
  section_header "Build Environment"

  # Check for required tools based on enabled components
  local missing_tools=false

  is_component_enabled "java" && {
    if ! command -v mvn &>/dev/null || ! java -version 2>&1 | grep -q "version \"21"; then
      log_error "Java 21 and Maven are required"
      missing_tools=true
    else
      log_info "Java: $(java -version 2>&1 | head -1)"
    fi
  }

  is_component_enabled "go" && {
    if ! command -v go &>/dev/null; then
      log_error "Go is required"
      missing_tools=true
    else
      log_info "Go: $(go version | awk '{print $3}')"
    fi
  }

  is_component_enabled "python" && {
    if ! command -v python3 &>/dev/null; then
      log_error "Python 3 is required"
      missing_tools=true
    else
      log_info "Python: $(python3 --version 2>&1)"
    fi
  }

  [[ "$missing_tools" == "true" ]] && { log_error "Missing required tools"; exit 1; }

  # Activate environment if available
  [[ -f "$RINNA_DIR/activate-rinna.sh" ]] && \
    run_command "source \"$RINNA_DIR/activate-rinna.sh\"" "Activating environment"
}

# Build Java components efficiently
build_java() {
  is_component_enabled "java" || return 0

  section_header "Java Build"

  # Determine Maven profile based on environment
  local mvn_profile
  case "$BUILD_ENV" in
    local) mvn_profile="local-quality" ;;
    ci) mvn_profile="ci" ;;
    prod) mvn_profile="production" ;;
  esac

  # Optimize Maven command options
  local mvn_options="-P $mvn_profile"
  [[ "$SKIP_TESTS" == "true" ]] && mvn_options="$mvn_options -DskipTests=true"
  [[ "$SKIP_QUALITY" == "true" ]] && mvn_options="$mvn_options -P skip-quality"
  [[ "$RELEASE" == "true" ]] && mvn_options="$mvn_options -P release"

  # Use parallel build for better performance
  mvn_options="$mvn_options -T 1C"

  # Set Checkstyle configuration to use the consolidated location
  mvn_options="$mvn_options -Dcheckstyle.config.location=$CONFIG_DIR/java/checkstyle.xml"
  mvn_options="$mvn_options -Dcheckstyle.suppressions.location=$CONFIG_DIR/java/checkstyle-suppressions.xml"

  # Integrated Maven build process (clean+compile+package in one command)
  run_command "mvn clean compile package $mvn_options" "Building Java project"

  # Run tests if not skipped
  if [[ "$SKIP_TESTS" != "true" ]]; then
    run_command "mvn test $mvn_options" "Running Java tests"

    # Collect test statistics efficiently
    if [[ -d "$RINNA_DIR/target/surefire-reports" ]]; then
      local total=$(find "$RINNA_DIR" -name "TEST-*.xml" | wc -l)
      local failed=$(find "$RINNA_DIR" -name "TEST-*.xml" | xargs grep -l "failures=\"[1-9]" | wc -l)
      local passed=$((total - failed))

      log_info "Tests: $passed passed, $failed failed, $total total"

      # Copy test results
      mkdir -p "$TEST_RESULTS_DIR/java"
      cp -r "$RINNA_DIR/target/surefire-reports" "$TEST_RESULTS_DIR/java/"
    fi
  fi

  # Run verification if quality checks are enabled
  [[ "$SKIP_QUALITY" != "true" ]] && \
    run_command "mvn verify $mvn_options" "Verifying Java project"

  # Copy JARs to distribution directory
  mkdir -p "$DIST_DIR/lib"
  find "$RINNA_DIR" -name "*.jar" -not -path "*/target/classes/*" -not -path "*/test-classes/*" \
    -exec cp {} "$DIST_DIR/lib/" \; 2>/dev/null || true
}

# Build Go components
build_go() {
  is_component_enabled "go" || return 0

  section_header "Go Build"

  # Change to the API directory
  cd "$RINNA_DIR/api"

  # Run linting if quality checks are enabled
  if [[ "$SKIP_QUALITY" != "true" ]]; then
    if command -v golangci-lint &> /dev/null; then
      run_command "golangci-lint run --config=$CONFIG_DIR/go/.golangci.yml" "Running Go linting"
    else
      log_warning "golangci-lint not found, skipping Go linting"
    fi
  fi

  # Set version flags
  local version="${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"
  local build_flags="-X github.com/heymumford/rinna/api/pkg/health.Version=$version \
                    -X github.com/heymumford/rinna/api/pkg/health.BuildNumber=$BUILD_NUMBER \
                    -X github.com/heymumford/rinna/api/pkg/health.CommitSHA=$GIT_COMMIT"

  # Build components in parallel for efficiency when possible
  if [[ -d "./cmd/healthcheck" ]]; then
    echo "Building Go components in parallel..."
    {
      go build -ldflags="$build_flags" -o "$RINNA_DIR/bin/rinnasrv" ./cmd/rinnasrv &&
      echo -e "${SUCCESS} Built API server"
    } &
    {
      go build -ldflags="$build_flags" -o "$RINNA_DIR/bin/healthcheck" ./cmd/healthcheck &&
      echo -e "${SUCCESS} Built healthcheck utility"
    } &
    wait
  else
    run_command "go build -ldflags=\"$build_flags\" -o \"$RINNA_DIR/bin/rinnasrv\" ./cmd/rinnasrv" "Building Go API server"
  fi

  # Run tests if enabled
  if [[ "$SKIP_TESTS" != "true" ]]; then
    run_command "go test -v ./..." "Running Go tests"

    # Copy binaries to distribution directory
    mkdir -p "$DIST_DIR/bin"
    cp "$RINNA_DIR/bin/rinnasrv" "$DIST_DIR/bin/" 2>/dev/null || true
    cp "$RINNA_DIR/bin/healthcheck" "$DIST_DIR/bin/" 2>/dev/null || true
  fi

  # Return to project root
  cd "$RINNA_DIR"
}

# Build Python components
build_python() {
  is_component_enabled "python" || return 0

  section_header "Python Build"

  # Quick check for Python directory
  [[ ! -d "$RINNA_DIR/python" ]] && { log_warning "Python directory not found"; return 0; }

  # Change to Python directory
  cd "$RINNA_DIR/python"

  # Install package in development mode
  run_command "pip install -e ." "Installing Python package"

  # Run linting if quality checks are enabled
  if [[ "$SKIP_QUALITY" != "true" ]]; then
    # Run mypy for type checking
    if command -v mypy &> /dev/null; then
      run_command "mypy --config-file=$CONFIG_DIR/python/python-linting.toml ." "Running Python type checking (mypy)"
    else
      log_warning "mypy not found, skipping Python type checking"
    fi

    # Run ruff for linting
    if command -v ruff &> /dev/null; then
      run_command "ruff --config=$CONFIG_DIR/python/python-linting.toml check ." "Running Python linting (ruff)"
    else
      log_warning "ruff not found, skipping Python linting"
    fi

    # Run black for code formatting check
    if command -v black &> /dev/null; then
      run_command "black --config=$CONFIG_DIR/python/python-linting.toml --check ." "Checking Python code formatting (black)"
    else
      log_warning "black not found, skipping Python formatting check"
    fi

    # Run isort for import sorting check
    if command -v isort &> /dev/null; then
      run_command "isort --settings-file=$CONFIG_DIR/python/python-linting.toml --check ." "Checking Python import sorting (isort)"
    else
      log_warning "isort not found, skipping Python import sorting check"
    fi
  fi

  # Run tests if enabled
  if [[ "$SKIP_TESTS" != "true" && -d "$RINNA_DIR/python/tests" ]]; then
    run_command "python -m pytest tests" "Running Python tests"

    # Copy Python packages to distribution directory
    mkdir -p "$DIST_DIR/python"
    run_command "python setup.py sdist bdist_wheel" "Creating Python distribution"
    cp -r dist/* "$DIST_DIR/python/" 2>/dev/null || true
  fi

  # Return to project root
  cd "$RINNA_DIR"
}

# Create distribution package
create_distribution() {
  section_header "Distribution Package"

  # Create bin directory and copy scripts
  mkdir -p "$DIST_DIR/bin"
  cp "$RINNA_DIR/bin/rin"* "$DIST_DIR/bin/" 2>/dev/null || true
  chmod +x "$DIST_DIR/bin/"* 2>/dev/null || true

  # Copy configuration files
  mkdir -p "$DIST_DIR/config"
  cp -r "$CONFIG_DIR"/* "$DIST_DIR/config/" 2>/dev/null || true

  # Copy version file
  cp "$VERSION_FILE" "$DIST_DIR/" 2>/dev/null || true

  # Build simplified README
  cat > "$DIST_DIR/README.md" << EOF
# Rinna ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH} (Build ${BUILD_NUMBER})

Built: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
Git: ${GIT_COMMIT}

## Components
$(is_component_enabled "java" && echo "- Java: Included" || echo "- Java: Not included")
$(is_component_enabled "go" && echo "- Go: Included" || echo "- Go: Not included")
$(is_component_enabled "python" && echo "- Python: Included" || echo "- Python: Not included")
EOF

  # Create the distribution archive efficiently
  local dist_name="rinna-${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}-b${BUILD_NUMBER}"
  local dist_archive="$TARGET_DIR/${dist_name}.tar.gz"

  run_command "tar -czf \"$dist_archive\" -C \"$TARGET_DIR\" distribution" "Creating archive: $(basename "$dist_archive")"
  log_success "Distribution package: $dist_archive"
}

# Push version changes to git if requested
push_version_changes() {
  [[ "$PUSH_VERSION" != "true" ]] && return 0

  section_header "Git Operations"

  # Add and commit version files
  run_command "git add \"$VERSION_FILE\" \"$VERSION_SERVICE_PROPS\"" "Adding version files to git"

  # Commit changes
  local commit_message="Build ${BUILD_NUMBER}: Version ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"
  run_command "git commit -m \"$commit_message\"" "Committing version changes" true

  # Push changes if commit succeeded
  [[ $? -eq 0 ]] && run_command "git push" "Pushing version changes" true

  # Create tag for releases
  if [[ "$RELEASE" == "true" ]]; then
    local tag="v${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"
    run_command "git tag -a \"$tag\" -m \"Release ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH} (Build ${BUILD_NUMBER})\"" \
      "Creating release tag: $tag" true

    [[ $? -eq 0 ]] && run_command "git push origin \"$tag\"" "Pushing release tag" true
  fi
}

# Generate build summary
generate_summary() {
  section_header "Build Summary"

  # Calculate build duration
  local build_end_time=$(date +%s)
  local build_duration=$((build_end_time - BUILD_START_TIME))
  local duration_min=$((build_duration / 60))
  local duration_sec=$((build_duration % 60))

  # Print compact summary to console
  log_success "Build completed in ${duration_min}m ${duration_sec}s"
  log_info "Version: ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH} (Build ${BUILD_NUMBER})"
  log_info "Components: ${COMPONENTS[*]}"

  # Create detailed summary file for reference
  local summary_file="$LOG_DIR/build-summary-latest.log"
  cat > "$summary_file" << EOF
# Rinna Build Summary

- **Version**: ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH} (Build ${BUILD_NUMBER})
- **Git**: ${GIT_COMMIT}
- **Build Date**: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
- **Build Duration**: ${duration_min}m ${duration_sec}s
- **Components**: ${COMPONENTS[*]}
- **Environment**: ${BUILD_ENV}
- **Config**: Tests: $([ "$SKIP_TESTS" == "true" ] && echo "Skipped" || echo "Run"), Quality: $([ "$SKIP_QUALITY" == "true" ] && echo "Skipped" || echo "Run")

## Artifacts
- Distribution: ${TARGET_DIR}/rinna-${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}-b${BUILD_NUMBER}.tar.gz
- Logs: ${BUILD_LOG}
EOF

  echo ""
  log_info "Detailed summary: $summary_file"
}

# Main function
main() {
  # Parse command line options
  parse_options "$@"

  # Show help and exit if requested
  [[ "$HELP" == "true" ]] && { show_usage; exit 0; }

  # Print build configuration
  section_header "Rinna Build"
  log_info "Starting build with config:"
  log_info "- Environment: $BUILD_ENV"
  log_info "- Components: ${COMPONENTS[*]}"
  log_info "- Options: Tests: $([ "$SKIP_TESTS" == "true" ] && echo "Skip" || echo "Run"), Quality: $([ "$SKIP_QUALITY" == "true" ] && echo "Skip" || echo "Run")"

  # Core build pipeline - sequential but efficient
  read_version
  initialize_build
  increment_build_number

  # Build components - each function checks if its component is enabled
  build_java
  build_go
  build_python

  # Create distribution and finalize
  create_distribution
  push_version_changes
  generate_summary

  echo -e "\n${GREEN}${BOLD}Build completed successfully!${NC}\n"
  return 0
}

# Run the main function with all arguments
main "$@"
