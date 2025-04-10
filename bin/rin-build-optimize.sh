#!/usr/bin/env bash
#
# rin-build-optimize.sh - Optimized build coordination script
#
# This script orchestrates the optimized build process, giving users
# control over which phases to run while emphasizing speed and clarity.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.

set -eo pipefail

# Determine script and project directories
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
RINNA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Import the common color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Status indicators
STATUS_SUCCESS="✅"
STATUS_FAILURE="❌"
STATUS_SKIPPED="⏭️"
STATUS_WARNING="⚠️"

# Default options
PHASES=("build" "test" "quality" "package")
ENABLED_PHASES=("build" "test" "package") # Default enabled phases (quality disabled by default)
COMPONENTS=("java" "go" "python")
ENABLED_COMPONENTS=("java" "go" "python") # All enabled by default
SKIP_TESTS=false
FIX_QUALITY=false
PARALLEL=true
VERBOSE=false
LOG_DIR="$RINNA_DIR/logs/build"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/build-$(date +%Y%m%d-%H%M%S).log"

# Display help message
show_help() {
    cat << EOF
${BLUE}Rinna Optimized Build System${NC}

${BOLD}Usage:${NC} $(basename "$0") [options]

${BOLD}Options:${NC}
  --only=PHASES     Run only specific build phases (comma-separated)
                    Available: ${PHASES[*]}
  --skip=PHASES     Skip specific build phases (comma-separated)
  --components=LIST Build only specific components (comma-separated)
                    Available: ${COMPONENTS[*]}
  --skip-tests      Skip running tests
  --fix-quality     Try to auto-fix quality issues
  --no-parallel     Disable parallel builds
  --verbose         Show verbose output
  --help            Show this help message

${BOLD}Examples:${NC}
  $(basename "$0")                    # Standard build (build, test, package)
  $(basename "$0") --only=build,test  # Build and test only, no packaging
  $(basename "$0") --components=java  # Build only Java components
  $(basename "$0") --only=quality --fix-quality  # Run quality checks with auto-fix

${BOLD}Build Phase Details:${NC}
  build    - Compile all source code
  test     - Run the test suite
  quality  - Run quality checks (off by default, enable with --only=quality)
  package  - Create distribution packages

EOF
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --only=*)
                # Override default enabled phases with only those specified
                IFS=',' read -r -a ENABLED_PHASES <<< "${1#*=}"
                shift
                ;;
            --skip=*)
                # Remove specified phases from enabled phases
                IFS=',' read -r -a SKIP_PHASES <<< "${1#*=}"
                for skip_phase in "${SKIP_PHASES[@]}"; do
                    for i in "${!ENABLED_PHASES[@]}"; do
                        if [[ "${ENABLED_PHASES[i]}" == "$skip_phase" ]]; then
                            unset 'ENABLED_PHASES[i]'
                        fi
                    done
                done
                # Reindex array
                ENABLED_PHASES=("${ENABLED_PHASES[@]}")
                shift
                ;;
            --components=*)
                # Override default components with only those specified
                IFS=',' read -r -a ENABLED_COMPONENTS <<< "${1#*=}"
                # Convert to lowercase
                for i in "${!ENABLED_COMPONENTS[@]}"; do
                    ENABLED_COMPONENTS[$i]=$(echo "${ENABLED_COMPONENTS[$i]}" | tr '[:upper:]' '[:lower:]')
                done
                shift
                ;;
            --skip-tests)
                SKIP_TESTS=true
                # Also remove test phase if specified
                for i in "${!ENABLED_PHASES[@]}"; do
                    if [[ "${ENABLED_PHASES[i]}" == "test" ]]; then
                        unset 'ENABLED_PHASES[i]'
                    fi
                done
                # Reindex array
                ENABLED_PHASES=("${ENABLED_PHASES[@]}")
                shift
                ;;
            --fix-quality)
                FIX_QUALITY=true
                shift
                ;;
            --no-parallel)
                PARALLEL=false
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                echo -e "${RED}Unknown option: $1${NC}" >&2
                show_help
                exit 1
                ;;
        esac
    done
}

# Print a phase header
print_phase_header() {
    echo -e "\n${BLUE}${BOLD}===== PHASE: $1 =====${NC}\n"
}

# Run a task with proper timing and output
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
    
    # Create a temp file for output capture
    local temp_output=$(mktemp)
    
    # Run the command
    if eval "$cmd" > "$temp_output" 2>&1; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        echo -e "${STATUS_SUCCESS} ${description} completed in ${duration}s"
        
        # In verbose mode, show the full output
        if [[ "$VERBOSE" == "true" ]]; then
            echo -e "${GRAY}--- Command output ---${NC}"
            cat "$temp_output"
            echo -e "${GRAY}--- End output ---${NC}"
        fi
        
        # Append to log file
        cat "$temp_output" >> "$LOG_FILE"
        rm -f "$temp_output"
        return 0
    else
        local exit_code=$?
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        echo -e "${STATUS_FAILURE} ${description} failed after ${duration}s (exit code $exit_code)"
        echo -e "${RED}> ERROR OUTPUT:${NC}"
        tail -n 20 "$temp_output"
        echo -e "${RED}> FULL LOG: $LOG_FILE${NC}"
        
        # Append to log file
        echo "ERROR OUTPUT FROM COMMAND: $cmd" >> "$LOG_FILE"
        cat "$temp_output" >> "$LOG_FILE"
        rm -f "$temp_output"
        
        return $exit_code
    fi
}

# Build phase
run_build_phase() {
    print_phase_header "Build"

    local build_args=""
    if [[ "$PARALLEL" == "false" ]]; then
        build_args="$build_args --parallel=false"
    fi
    if [[ "$SKIP_TESTS" == "true" ]]; then
        build_args="$build_args --skip-tests"
    fi
    if [[ "$VERBOSE" == "true" ]]; then
        build_args="$build_args --verbose"
    fi
    
    # Add specific components if specified
    if [[ ${#ENABLED_COMPONENTS[@]} -gt 0 ]]; then
        build_args="$build_args ${ENABLED_COMPONENTS[*]}"
    fi
    
    run_task "Building all components" "$SCRIPT_DIR/rin-build $build_args"
}

# Test phase
run_test_phase() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        echo -e "${STATUS_SKIPPED} Test phase skipped due to --skip-tests flag"
        return 0
    fi

    print_phase_header "Test"

    local test_args=""
    if [[ "$PARALLEL" == "false" ]]; then
        test_args="$test_args --no-parallel"
    fi
    if [[ "$VERBOSE" == "true" ]]; then
        test_args="$test_args --verbose"
    fi
    
    # Add specific language components if specified
    for component in "${ENABLED_COMPONENTS[@]}"; do
        case "$component" in
            java)
                test_args="$test_args --java"
                ;;
            go)
                test_args="$test_args --go"
                ;;
            python)
                test_args="$test_args --python"
                ;;
        esac
    done
    
    run_task "Running tests" "$SCRIPT_DIR/rin-test $test_args all"
}

# Quality phase
run_quality_phase() {
    print_phase_header "Quality"

    local quality_args=""
    
    # Add fix flag if requested
    if [[ "$FIX_QUALITY" == "true" ]]; then
        quality_args="$quality_args --fix"
    fi
    
    # Add verbose flag if requested
    if [[ "$VERBOSE" == "true" ]]; then
        quality_args="$quality_args --verbose"
    fi
    
    # Run quality checks for Java components only
    # Other components have their own quality processes
    if [[ " ${ENABLED_COMPONENTS[*]} " =~ " java " ]]; then
        run_task "Running quality checks" "$SCRIPT_DIR/rin-quality-check-all.sh $quality_args"
    else
        echo -e "${STATUS_SKIPPED} Quality checks skipped (Java components not enabled)"
    fi
}

# Package phase
run_package_phase() {
    print_phase_header "Package"

    # Common packaging options
    local pkg_args=""
    if [[ "$VERBOSE" == "true" ]]; then
        pkg_args="$pkg_args --verbose"
    fi
    
    # Create target directory if it doesn't exist
    mkdir -p "$RINNA_DIR/target"
    
    # Get version from version.properties
    local version="unknown"
    if [[ -f "$RINNA_DIR/version.properties" ]]; then
        version=$(grep "^version=" "$RINNA_DIR/version.properties" | cut -d= -f2)
    fi
    
    # Package Java components if enabled
    if [[ " ${ENABLED_COMPONENTS[*]} " =~ " java " ]]; then
        run_task "Packaging Java modules" "cd $RINNA_DIR && mvn package -DskipTests=true"
    fi
    
    # Package Go components if enabled
    if [[ " ${ENABLED_COMPONENTS[*]} " =~ " go " ]]; then
        # Ensure bin directory exists
        mkdir -p "$RINNA_DIR/bin"
        
        # API server
        if [[ -d "$RINNA_DIR/api" ]]; then
            run_task "Packaging Go API server" "cd $RINNA_DIR/api && go build -o $RINNA_DIR/bin/rinnasrv cmd/rinnasrv/main.go"
        fi
        
        # Version service
        if [[ -d "$RINNA_DIR/version-service" ]]; then
            run_task "Packaging version service" "cd $RINNA_DIR/version-service && go build -o $RINNA_DIR/bin/version-cli cli/version_cli.go"
        fi
    fi
    
    # Package Python components if enabled
    if [[ " ${ENABLED_COMPONENTS[*]} " =~ " python " ]]; then
        if [[ -d "$RINNA_DIR/python" ]]; then
            run_task "Packaging Python modules" "cd $RINNA_DIR/python && python setup.py bdist_wheel"
        fi
    fi
    
    # Create distribution package with all components
    local dist_dir="$RINNA_DIR/target/distribution"
    mkdir -p "$dist_dir/bin" "$dist_dir/lib" "$dist_dir/config"
    
    # Copy binaries
    run_task "Creating distribution package" "cp -R $RINNA_DIR/bin/rin* $dist_dir/bin/ 2>/dev/null || true"
    
    # Copy JARs if Java is enabled
    if [[ " ${ENABLED_COMPONENTS[*]} " =~ " java " ]]; then
        run_task "Adding Java artifacts" "find $RINNA_DIR -name \"*.jar\" -not -path \"*/target/classes/*\" -exec cp {} $dist_dir/lib/ \\; 2>/dev/null || true"
    fi
    
    # Copy version file
    if [[ -f "$RINNA_DIR/version.properties" ]]; then
        cp "$RINNA_DIR/version.properties" "$dist_dir/"
    fi
    
    # Create tarball
    local dist_tarball="$RINNA_DIR/target/rinna-${version}.tar.gz"
    run_task "Creating distribution archive" "tar -czf $dist_tarball -C $RINNA_DIR/target distribution"
    
    echo -e "${STATUS_SUCCESS} Distribution package created: ${BLUE}$dist_tarball${NC}"
}

# Main function
main() {
    parse_args "$@"
    
    # Print build header
    echo -e "\n${BLUE}═════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}           Rinna Optimized Build                ${NC}"
    echo -e "${BLUE}═════════════════════════════════════════════════${NC}\n"
    
    # Print build configuration
    echo -e "${BLUE}Build Configuration:${NC}"
    echo -e "• Enabled phases:    ${ENABLED_PHASES[*]}"
    echo -e "• Enabled components: ${ENABLED_COMPONENTS[*]}"
    echo -e "• Skip tests:        $SKIP_TESTS"
    echo -e "• Fix quality:       $FIX_QUALITY"
    echo -e "• Parallel build:    $PARALLEL"
    echo -e "• Verbose output:    $VERBOSE"
    echo -e "• Log file:          $LOG_FILE"
    echo ""
    
    # Record start time
    local start_time=$(date +%s)
    
    # Run enabled phases
    local exit_code=0
    for phase in "${PHASES[@]}"; do
        if [[ " ${ENABLED_PHASES[*]} " =~ " $phase " ]]; then
            case "$phase" in
                build)
                    if ! run_build_phase; then
                        exit_code=$?
                        echo -e "${RED}Build phase failed with exit code $exit_code${NC}"
                        exit $exit_code
                    fi
                    ;;
                test)
                    if ! run_test_phase; then
                        exit_code=$?
                        echo -e "${YELLOW}Tests failed with exit code $exit_code, but continuing with other phases${NC}"
                    fi
                    ;;
                quality)
                    if ! run_quality_phase; then
                        exit_code=$?
                        echo -e "${YELLOW}Quality checks failed with exit code $exit_code, but continuing with other phases${NC}"
                    fi
                    ;;
                package)
                    if ! run_package_phase; then
                        exit_code=$?
                        echo -e "${RED}Package phase failed with exit code $exit_code${NC}"
                        exit $exit_code
                    fi
                    ;;
                *)
                    echo -e "${YELLOW}Unknown phase: $phase${NC}"
                    ;;
            esac
        fi
    done
    
    # Calculate total build time
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    # Build summary
    echo -e "\n${BLUE}═════════════════════════════════════════════════${NC}"
    if [[ $exit_code -eq 0 ]]; then
        echo -e "${GREEN}             BUILD SUCCESSFUL                  ${NC}"
    else
        echo -e "${RED}             BUILD COMPLETED WITH WARNINGS       ${NC}"
    fi
    echo -e "${BLUE}═════════════════════════════════════════════════${NC}"
    echo -e "• Total build time:   ${duration} seconds"
    echo -e "• Phases executed:    ${ENABLED_PHASES[*]}"
    echo -e "• Components built:   ${ENABLED_COMPONENTS[*]}"
    echo -e "• Build log:          $LOG_FILE"
    
    exit $exit_code
}

# Run the main function
main "$@"