#!/bin/bash
# Script to run only the new tests for Rinna CLI commands

set -e  # Exit immediately if a command exits with a non-zero status

# Color definitions
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
function header() {
    echo -e "\n${BLUE}$1${NC}"
    echo -e "${BLUE}===========================================${NC}"
}

function subheader() {
    echo -e "\n${YELLOW}$1${NC}"
    echo -e "${YELLOW}-----------------------------------------${NC}"
}

function success() {
    echo -e "${GREEN}✅ $1${NC}"
}

function failure() {
    echo -e "${RED}❌ $1${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
}

function run_test() {
    TEST_NAME="$1"
    shift
    COMMAND="$@"
    
    subheader "Testing $TEST_NAME"
    echo -e "Running: ${YELLOW}$COMMAND${NC}"
    OUTPUT=$($COMMAND 2>&1)
    EXIT_CODE=$?
    
    echo "$OUTPUT"
    
    if [ $EXIT_CODE -eq 0 ]; then
        success "$TEST_NAME test PASSED"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        failure "$TEST_NAME test FAILED (exit code: $EXIT_CODE)"
        echo -e "${RED}Command output: $OUTPUT${NC}"
    fi
}

function run_test_with_verification() {
    TEST_NAME="$1"
    COMMAND="$2"
    
    subheader "Testing $TEST_NAME"
    echo -e "Running: ${YELLOW}$COMMAND${NC}"
    
    # Run the command
    eval "$COMMAND" > /tmp/cmd_output.txt 2>&1
    EXIT_CODE=$?
    
    # Show truncated output
    head -n 10 /tmp/cmd_output.txt
    if [ "$(wc -l < /tmp/cmd_output.txt)" -gt 10 ]; then
        echo "... (output truncated)"
    fi
    
    if [ $EXIT_CODE -eq 0 ]; then
        success "$TEST_NAME test PASSED"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        failure "$TEST_NAME test FAILED (exit code: $EXIT_CODE)"
        VERIFICATION_FAILURES=$((VERIFICATION_FAILURES + 1))
    fi
}

# Check if java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Java not found. Please install Java to run the tests.${NC}"
    exit 1
fi

# Initialize counters
PASSED_TESTS=0
FAILED_TESTS=0
VERIFICATION_FAILURES=0

# Set paths
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RINNA_CORE_DIR="$PROJECT_ROOT/rinna-core"
TEST_CLASSES_DIR="$RINNA_CORE_DIR/target/test-classes"
MAIN_CLASSES_DIR="$RINNA_CORE_DIR/target/classes"
TEST_TMP_DIR="$PROJECT_ROOT/.test-tmp"

# Create test temp directory
mkdir -p "$TEST_TMP_DIR"

# Make sure the .rinna directory is clean for testing
rm -rf "$PROJECT_ROOT/.rinna" 2>/dev/null || true

header "Rinna CLI Command Tests"
echo "Running comprehensive tests for Rinna CLI commands"
echo "Project root: $PROJECT_ROOT"

# Make sure classes are compiled if Java tests are enabled
if [ -d "$RINNA_CORE_DIR" ]; then
    subheader "Compiling Java test classes"
    cd "$RINNA_CORE_DIR" || { failure "Cannot cd to $RINNA_CORE_DIR"; exit 1; }
    
    # Create test classes directory if not exists
    mkdir -p "$TEST_CLASSES_DIR"
    
    # Try to compile only the classes we need
    MAVEN_REPO="$HOME/.m2/repository"
    if [ -d "$MAVEN_REPO" ]; then
        javac -d "$TEST_CLASSES_DIR" \
            -classpath "$MAVEN_REPO/junit/junit/4.13.2/junit-4.13.2.jar:$MAVEN_REPO/io/cucumber/cucumber-java/7.22.0/cucumber-java-7.22.0.jar:$MAVEN_REPO/io/cucumber/cucumber-junit/7.22.0/cucumber-junit-7.22.0.jar:$MAIN_CLASSES_DIR" \
            src/test/java/org/rinna/bdd/TestContext.java \
            src/test/java/org/rinna/bdd/NewUserAuthRunner.java \
            src/test/java/org/rinna/bdd/NewUserAuthSteps.java \
            src/test/java/org/rinna/bdd/WorkItemAddRunner.java \
            src/test/java/org/rinna/bdd/WorkItemAddSteps.java \
            src/test/java/org/rinna/bdd/WorkItemListingRunner.java \
            src/test/java/org/rinna/bdd/WorkItemListingSteps.java \
            src/test/java/org/rinna/bdd/WorkItemManagementRunner.java \
            src/test/java/org/rinna/bdd/WorkItemManagementSteps.java 2>/dev/null || true
    fi
fi

# Test 1: Check if scripts exist and are executable
subheader "Verifying script existence and permissions"
for script in rin rin-init rin-auth rin-workspace rin-list rin-add; do
    if [ -x "$PROJECT_ROOT/bin/$script" ]; then
        success "$script exists and is executable"
    else
        failure "$script does not exist or is not executable"
    fi
done

# Test user initialization
run_test_with_verification "rin-init" "$PROJECT_ROOT/bin/rin-init --force"

# Test auth status
run_test_with_verification "rin-auth status" "$PROJECT_ROOT/bin/rin-auth status"

# Test workspace status
run_test_with_verification "rin-workspace status" "$PROJECT_ROOT/bin/rin-workspace status"

# Test workspace info
run_test "rin-workspace info" "$PROJECT_ROOT/bin/rin-workspace" info

# Test listing with no items
run_test_with_verification "rin-list (empty)" "$PROJECT_ROOT/bin/rin-list"

# Test add command for different item types
for type in TASK BUG FEATURE EPIC; do
    run_test_with_verification "rin-add ($type)" \
        "$PROJECT_ROOT/bin/rin-add --title 'Test $type' --type $type --description 'Test description for $type'"
done

# Test add with priority
run_test_with_verification "rin-add with priority" \
    "$PROJECT_ROOT/bin/rin-add --title 'High Priority Task' --type TASK --priority HIGH"

# Test add with status
run_test_with_verification "rin-add with status" \
    "$PROJECT_ROOT/bin/rin-add --title 'In Progress Task' --type TASK --status IN_PROGRESS"

# Test listing all items - just verify it runs
run_test "rin-list (all items)" "$PROJECT_ROOT/bin/rin-list"

# Test listing with type filter - just verify commands run without errors
for type in TASK BUG FEATURE EPIC; do
    run_test "rin-list --type $type" "$PROJECT_ROOT/bin/rin-list" "--type" "$type"
done

# Test listing with priority filter
run_test "rin-list --priority HIGH" "$PROJECT_ROOT/bin/rin-list" "--priority" "HIGH"

# Test listing with status filter
run_test "rin-list --status IN_PROGRESS" "$PROJECT_ROOT/bin/rin-list" "--status" "IN_PROGRESS"

# Test listing with format JSON
run_test "rin-list --format json" "$PROJECT_ROOT/bin/rin-list" "--format" "json"

# Test querying user config
run_test_with_verification "rin-auth config" "$PROJECT_ROOT/bin/rin-auth config"

# Test summary report
header "Test Summary"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"
echo -e "${YELLOW}Verification Failures: $VERIFICATION_FAILURES${NC}"

if [ $FAILED_TESTS -eq 0 ] && [ $VERIFICATION_FAILURES -eq 0 ]; then
    echo -e "\n${GREEN}🎉 All tests passed!${NC}"
    exit 0
else
    echo -e "\n${RED}❌ One or more tests failed.${NC}"
    exit 1
fi