#!/bin/bash
# Common test utilities for Rinna bash tests

# Color definitions
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
BLUE="\033[0;34m"
RED="\033[0;31m"
NC="\033[0m" # No Color
BOLD="\033[1m"

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Function to print section headers
print_test_header() {
    local test_name=$1
    echo -e "\n${BLUE}${BOLD}Running test: $test_name${NC}"
    echo "----------------------------------------"
}

# Function to print test success
print_test_success() {
    local test_name=$1
    local duration=$2
    echo -e "${GREEN}✓ $test_name passed${NC} ${YELLOW}($duration seconds)${NC}"
}

# Function to print test failure
print_test_failure() {
    local test_name=$1
    local duration=$2
    local message=$3
    echo -e "${RED}✗ $test_name failed${NC} ${YELLOW}($duration seconds)${NC}"
    if [ -n "$message" ]; then
        echo -e "${RED}  $message${NC}"
    fi
}

# Function to run a test with timing and reporting
run_timed_test() {
    local test_name=$1
    local test_function=$2
    
    print_test_header "$test_name"
    
    local start_time=$(date +%s.%N)
    local output=$(mktemp)
    
    # Run the test function and capture its output and return code
    $test_function > "$output" 2>&1
    local status=$?
    
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc)
    local duration_rounded=$(printf "%.2f" $duration)
    
    # Display the output regardless of success or failure
    cat "$output"
    rm "$output"
    
    if [ $status -eq 0 ]; then
        print_test_success "$test_name" "$duration_rounded"
    else
        print_test_failure "$test_name" "$duration_rounded" "See above for details"
    fi
    
    return $status
}

# Function to verify command availability
verify_command() {
    local cmd=$1
    if ! command -v $cmd >/dev/null 2>&1; then
        echo -e "${YELLOW}Command '$cmd' not found, skipping related tests${NC}"
        return 1
    fi
    return 0
}

# Function to measure command execution time
measure_command_time() {
    local cmd=$1
    local start_time=$(date +%s.%N)
    eval "$cmd" >/dev/null 2>&1
    local status=$?
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc)
    
    echo "$duration"
    return $status
}

# Function to compare durations
is_faster_than() {
    local actual=$1
    local threshold=$2
    
    if (( $(echo "$actual < $threshold" | bc -l) )); then
        return 0
    else
        return 1
    fi
}

# Function to extract version from properties file
extract_version_from_properties() {
    local file=$1
    local key=$2
    
    if [ ! -f "$file" ]; then
        echo "File not found: $file" >&2
        return 1
    fi
    
    local value=$(grep -m 1 "^$key=" "$file" | cut -d'=' -f2)
    if [ -z "$value" ]; then
        echo "Key not found: $key in $file" >&2
        return 1
    fi
    
    echo "$value"
    return 0
}

# Function to parse semantic version components
parse_semver() {
    local version=$1
    local component=$2  # "major", "minor", or "patch"
    
    case $component in
        major)
            echo "$version" | cut -d. -f1
            ;;
        minor)
            echo "$version" | cut -d. -f2
            ;;
        patch)
            echo "$version" | cut -d. -f3
            ;;
        *)
            echo "Invalid component: $component (must be major, minor, or patch)" >&2
            return 1
            ;;
    esac
}

# Test environment detection
detect_test_environment() {
    # Detect CI environment
    if [ -n "$CI" ] || [ -n "$GITHUB_ACTIONS" ] || [ -n "$JENKINS_URL" ]; then
        echo "ci"
    # Detect Docker environment
    elif [ -f "/.dockerenv" ] || grep -q docker /proc/1/cgroup 2>/dev/null; then
        echo "docker"
    # Default to local
    else
        echo "local"
    fi
}

# Helper function for temporary test files
create_temp_test_file() {
    local content="$1"
    local ext="${2:-txt}"
    
    local temp_file=$(mktemp --suffix=".$ext")
    echo "$content" > "$temp_file"
    echo "$temp_file"
}