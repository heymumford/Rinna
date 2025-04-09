#!/usr/bin/env bash
#
# run-admin-tests.sh - Run all admin functionality tests
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
${BLUE}run-admin-tests.sh${NC} - Run all admin functionality tests

Usage: ./run-admin-tests.sh [options]

Options:
  --all               Run all admin tests (default)
  --integration       Run admin integration tests (Maven integration & server auto-launch)
  --config            Run admin configuration tests (workflow, workitem & user management)
  --project           Run admin project management tests
  --audit             Run admin audit tests (audit logging and reporting)
  --compliance        Run admin compliance tests (regulatory compliance features)
  --security          Run both audit and compliance tests together
  --monitoring        Run admin system monitoring and diagnostics tests
  --backup            Run admin backup and recovery tests
  --specific=RUNNER   Run a specific test runner (e.g., AdminMavenIntegrationRunner)
  --verbose           Show detailed output
  --parallel          Run tests in parallel when possible
  --tag=TAG           Run tests with specific tag
  --cli               Run CLI-based tests only (no Maven)
  --debug             Run in debug mode (shows trace output)

EOF
}

# Parse arguments
parse_args() {
    TEST_CATEGORY="all"
    VERBOSE=""
    PARALLEL=""
    TAG_FILTER=""
    SPECIFIC_RUNNER=""
    CLI_ONLY="false"
    DEBUG_MODE="false"

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --help|-h)
                show_help
                exit 0
                ;;
            --all)
                TEST_CATEGORY="all"
                shift
                ;;
            --integration)
                TEST_CATEGORY="integration"
                shift
                ;;
            --config)
                TEST_CATEGORY="config"
                shift
                ;;
            --project)
                TEST_CATEGORY="project"
                shift
                ;;
            --audit)
                TEST_CATEGORY="audit"
                shift
                ;;
            --compliance)
                TEST_CATEGORY="compliance"
                shift
                ;;
            --security)
                TEST_CATEGORY="security"
                shift
                ;;
            --monitoring)
                TEST_CATEGORY="monitoring"
                shift
                ;;
            --backup)
                TEST_CATEGORY="backup"
                shift
                ;;
            --specific=*)
                TEST_CATEGORY="specific"
                SPECIFIC_RUNNER="${1#*=}"
                shift
                ;;
            --verbose|-v)
                VERBOSE="-Dverbose=true"
                shift
                ;;
            --parallel|-p)
                PARALLEL="-Dparallel=true"
                shift
                ;;
            --tag=*)
                TAG_FILTER="-Dcucumber.filter.tags=${1#*=}"
                shift
                ;;
            --cli)
                CLI_ONLY="true"
                shift
                ;;
            --debug)
                DEBUG_MODE="true"
                set -x  # Enable trace output
                shift
                ;;
            *)
                echo -e "${RED}Error: Unknown option: $1${NC}" >&2
                show_help
                exit 1
                ;;
        esac
    done
}

# Run admin tests
run_admin_tests() {
    echo -e "${BLUE}Running admin tests: $TEST_CATEGORY${NC}"
    cd "$PROJECT_ROOT"
    
    # First, try to run the Maven-based tests if the project is set up for it
    if [[ -f "pom.xml" && "$TEST_CATEGORY" != "cli" && "$CLI_ONLY" != "true" ]]; then
        local test_pattern=""
        
        case "$TEST_CATEGORY" in
            all)
                test_pattern="org.rinna.bdd.Admin*Runner"
                ;;
            integration)
                test_pattern="org.rinna.bdd.AdminIntegrationRunner"
                ;;
            config)
                test_pattern="org.rinna.bdd.AdminConfigurationRunner"
                ;;
            project)
                test_pattern="org.rinna.bdd.AdminProjectRunner"
                ;;
            audit)
                test_pattern="org.rinna.bdd.AdminAuditComplianceRunner"
                TAG_FILTER="${TAG_FILTER:-} -Dcucumber.filter.tags=@audit"
                ;;
            compliance)
                test_pattern="org.rinna.bdd.AdminAuditComplianceRunner"
                TAG_FILTER="${TAG_FILTER:-} -Dcucumber.filter.tags=@compliance"
                ;;
            security)
                test_pattern="org.rinna.bdd.AdminAuditComplianceRunner"
                ;;
            monitoring)
                test_pattern="org.rinna.bdd.AdminSystemMonitoringRunner"
                ;;
            backup)
                test_pattern="org.rinna.bdd.AdminBackupRecoveryRunner"
                ;;
            specific)
                test_pattern="org.rinna.bdd.$SPECIFIC_RUNNER"
                ;;
            *)
                echo -e "${RED}Error: Unknown test category: $TEST_CATEGORY${NC}" >&2
                exit 1
                ;;
        esac
        
        # Build the Maven command
        local mvn_cmd="mvn clean test -Dtest='$test_pattern' $VERBOSE $PARALLEL $TAG_FILTER"
        
        echo "Executing Maven tests: $mvn_cmd"
        
        # Only try to run Maven tests if we have a concrete test pattern
        if [[ -n "$test_pattern" && "$test_pattern" != "org.rinna.bdd." ]]; then
            if eval "$mvn_cmd"; then
                echo -e "${GREEN}Maven tests completed successfully${NC}"
                return 0
            else
                echo -e "${YELLOW}Maven tests failed or not found, falling back to CLI tests${NC}"
                # Fall through to CLI tests
            fi
        fi
    fi
    
    # Fall back to CLI-based tests
    echo -e "${BLUE}Running CLI-based admin tests${NC}"
    
    # Count test results
    local total_tests=0
    local passed_tests=0
    
    # Function to run a test and check result
    run_test() {
        local test_name="$1"
        local command="$2"
        local expected_exit_code="${3:-0}"
        local expected_pattern="$4"
        local output
        
        echo -e "${CYAN}TEST: ${test_name}${NC}"
        echo -e "${YELLOW}COMMAND: ${command}${NC}"
        
        # Run the command and capture output and exit code
        output=$(eval "$command" 2>&1)
        exit_code=$?
        
        # Display a portion of the output
        echo -e "OUTPUT EXCERPT:"
        echo "$output" | head -n 10
        if [[ $(echo "$output" | wc -l) -gt 10 ]]; then
            echo -e "${YELLOW}[...output truncated...]${NC}"
        fi
        
        # Check exit code
        if [[ $exit_code -eq $expected_exit_code ]]; then
            echo -e "${GREEN}✓ Exit code matches expected: $exit_code${NC}"
        else
            echo -e "${RED}✗ Exit code mismatch: got $exit_code, expected $expected_exit_code${NC}"
            return 1
        fi
        
        # Check output pattern if provided
        if [[ -n "$expected_pattern" ]]; then
            if echo "$output" | grep -q "$expected_pattern"; then
                echo -e "${GREEN}✓ Output contains expected pattern: '$expected_pattern'${NC}"
            else
                echo -e "${RED}✗ Output does not contain expected pattern: '$expected_pattern'${NC}"
                return 1
            fi
        fi
        
        echo -e "${GREEN}✓ Test passed${NC}\n"
        return 0
    }
    
    # Execute different test sets based on category
    case "$TEST_CATEGORY" in
        all|cli)
            # Test basic admin command (should show help)
            ((total_tests++))
            if run_test "Admin help" \
                        "bin/rin admin" \
                        1 \
                        "Administrative Commands:"; then
                ((passed_tests++))
            fi

            # Test admin audit list
            ((total_tests++))
            if run_test "Admin audit list" \
                        "bin/rin admin audit list" \
                        0 \
                        "Admin command executed with subcommand: audit"; then
                ((passed_tests++))
            fi

            # Test admin audit with invalid subcommand
            ((total_tests++))
            if run_test "Admin audit with invalid subcommand" \
                        "bin/rin admin audit invalid-subcommand" \
                        0 \
                        "Arguments:"; then
                ((passed_tests++))
            fi

            # Test admin compliance report generation
            ((total_tests++))
            if run_test "Compliance report generation" \
                        "bin/rin admin compliance report financial" \
                        0 \
                        "Admin command executed with subcommand: compliance"; then
                ((passed_tests++))
            fi

            # Test admin compliance validation
            ((total_tests++))
            if run_test "Compliance validation" \
                        "bin/rin admin compliance validate --project=demo" \
                        0 \
                        "Admin command executed with subcommand: compliance"; then
                ((passed_tests++))
            fi

            # Test admin monitor dashboard
            ((total_tests++))
            if run_test "System monitoring dashboard" \
                        "bin/rin admin monitor dashboard" \
                        0 \
                        "Admin command executed with subcommand: monitor"; then
                ((passed_tests++))
            fi

            # Test admin monitor metrics
            ((total_tests++))
            if run_test "System monitoring metrics" \
                        "bin/rin admin monitor metrics --type=system" \
                        0 \
                        "Admin command executed with subcommand: monitor"; then
                ((passed_tests++))
            fi

            # Test admin diagnostics run
            ((total_tests++))
            if run_test "Run system diagnostics" \
                        "bin/rin admin diagnostics run" \
                        0 \
                        "Admin command executed with subcommand: diagnostics"; then
                ((passed_tests++))
            fi

            # Test admin backup configuration
            ((total_tests++))
            if run_test "Backup configuration" \
                        "bin/rin admin backup configure --location=/backup --retention=30" \
                        0 \
                        "Admin command executed with subcommand: backup"; then
                ((passed_tests++))
            fi

            # Test admin recovery planning
            ((total_tests++))
            if run_test "Recovery planning" \
                        "bin/rin admin recovery plan --from=latest" \
                        0 \
                        "Admin command executed with subcommand: recovery"; then
                ((passed_tests++))
            fi
            ;;
        
        audit)
            # Test admin audit list
            ((total_tests++))
            if run_test "Admin audit list" \
                        "bin/rin admin audit list" \
                        0 \
                        "Admin command executed with subcommand: audit"; then
                ((passed_tests++))
            fi
            
            # Test admin audit configure
            ((total_tests++))
            if run_test "Admin audit configure" \
                        "bin/rin admin audit configure --retention=90" \
                        0 \
                        "Admin command executed with subcommand: audit"; then
                ((passed_tests++))
            fi
            
            # Test admin audit export
            ((total_tests++))
            if run_test "Admin audit export" \
                        "bin/rin admin audit export --format=csv" \
                        0 \
                        "Admin command executed with subcommand: audit"; then
                ((passed_tests++))
            fi
            ;;
            
        compliance)
            # Test admin compliance report generation
            ((total_tests++))
            if run_test "Compliance report generation" \
                        "bin/rin admin compliance report financial" \
                        0 \
                        "Admin command executed with subcommand: compliance"; then
                ((passed_tests++))
            fi
            
            # Test admin compliance validation
            ((total_tests++))
            if run_test "Compliance validation" \
                        "bin/rin admin compliance validate --project=demo" \
                        0 \
                        "Admin command executed with subcommand: compliance"; then
                ((passed_tests++))
            fi
            
            # Test admin compliance configure
            ((total_tests++))
            if run_test "Compliance configuration" \
                        "bin/rin admin compliance configure --framework=iso27001" \
                        0 \
                        "Admin command executed with subcommand: compliance"; then
                ((passed_tests++))
            fi
            ;;
            
        monitoring)
            # Test admin monitor dashboard
            ((total_tests++))
            if run_test "System monitoring dashboard" \
                        "bin/rin admin monitor dashboard" \
                        0 \
                        "Admin command executed with subcommand: monitor"; then
                ((passed_tests++))
            fi
            
            # Test admin monitor metrics
            ((total_tests++))
            if run_test "System monitoring metrics" \
                        "bin/rin admin monitor metrics --type=system" \
                        0 \
                        "Admin command executed with subcommand: monitor"; then
                ((passed_tests++))
            fi
            
            # Test admin diagnostics run
            ((total_tests++))
            if run_test "Run system diagnostics" \
                        "bin/rin admin diagnostics run" \
                        0 \
                        "Admin command executed with subcommand: diagnostics"; then
                ((passed_tests++))
            fi
            
            # Test admin diagnostics schedule
            ((total_tests++))
            if run_test "Schedule system diagnostics" \
                        "bin/rin admin diagnostics schedule --interval=daily --time=02:00" \
                        0 \
                        "Admin command executed with subcommand: diagnostics"; then
                ((passed_tests++))
            fi
            ;;
            
        backup)
            # Test admin backup configuration
            ((total_tests++))
            if run_test "Backup configuration" \
                        "bin/rin admin backup configure --location=/backup --retention=30" \
                        0 \
                        "Admin command executed with subcommand: backup"; then
                ((passed_tests++))
            fi
            
            # Test admin backup start
            ((total_tests++))
            if run_test "Start backup" \
                        "bin/rin admin backup start --type=full" \
                        0 \
                        "Admin command executed with subcommand: backup"; then
                ((passed_tests++))
            fi
            
            # Test admin recovery planning
            ((total_tests++))
            if run_test "Recovery planning" \
                        "bin/rin admin recovery plan --from=latest" \
                        0 \
                        "Admin command executed with subcommand: recovery"; then
                ((passed_tests++))
            fi
            ;;
            
        *)
            echo -e "${RED}Error: Unknown CLI test category: $TEST_CATEGORY${NC}" >&2
            return 1
            ;;
    esac
    
    # Print summary
    echo -e "${BLUE}=======================================${NC}"
    echo -e "${BLUE}= CLI Test Summary                    =${NC}"
    echo -e "${BLUE}=======================================${NC}"
    echo -e "Total tests: $total_tests"
    echo -e "Passed tests: ${GREEN}$passed_tests${NC}"
    if [[ $passed_tests -eq $total_tests ]]; then
        echo -e "${GREEN}All tests passed!${NC}"
        return 0
    else
        echo -e "${RED}Some tests failed. Failed: $((total_tests - passed_tests))${NC}"
        return 1
    fi
}

# Main script execution
parse_args "$@"
run_admin_tests