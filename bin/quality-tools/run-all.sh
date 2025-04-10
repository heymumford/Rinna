#!/usr/bin/env bash
#
# run-all.sh - Fast and actionable quality checks for Rinna
#
# This script orchestrates all quality checks with a focus on:
# 1. Making quality tools run independently
# 2. Providing clear, categorized output to fix issues
# 3. Supporting targeted checks to fix one set of issues at a time
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.

set -eo pipefail

# Determine script and project directories
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
RINNA_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOG_DIR="$RINNA_DIR/logs/quality"
mkdir -p "$LOG_DIR"

# Define the timestamp for logs
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
MASTER_LOG="$LOG_DIR/quality-check-$TIMESTAMP.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Status indicators for better visibility
STATUS_SUCCESS="✅"
STATUS_FAILURE="❌"
STATUS_SKIPPED="⏭️"
STATUS_WARNING="⚠️"

# Initialize variables
SELECTED_TOOL=""
TARGET_MODULE=""
FIX_MODE=false
SUMMARY_ONLY=false
VERBOSE=false
HELP=false
CHECK_TOOLS=("checkstyle" "pmd" "spotbugs" "enforcer" "owasp")

# Display usage help
show_help() {
    cat << EOF
${BLUE}Rinna Quality Check${NC} - Fast and targeted quality checks

${BOLD}Usage:${NC} $(basename "$0") [options] [tool] [module]

${BOLD}Tools:${NC}
  checkstyle       Run Java code style checks
  pmd              Run Java static code analysis
  spotbugs         Run Java bug detection
  enforcer         Run Maven dependency rules check
  owasp            Run security vulnerability scan
  all              Run all quality checks (default)

${BOLD}Modules:${NC}
  core             Run checks on rinna-core only
  cli              Run checks on rinna-cli only
  all              Run checks on all modules (default)

${BOLD}Options:${NC}
  --fix            Attempt to auto-fix issues where possible
  --summary        Show summary report only
  --verbose        Show detailed output
  --continue       Continue on error (run all tools even if some fail)
  --help           Show this help message

${BOLD}Examples:${NC}
  $(basename "$0")                    # Run all quality checks on all modules
  $(basename "$0") checkstyle         # Run only checkstyle on all modules
  $(basename "$0") pmd core           # Run PMD on core module only
  $(basename "$0") --fix checkstyle   # Run checkstyle and fix issues where possible
EOF
}

# Parse command line arguments
parse_args() {
    CONTINUE_ON_ERROR=false
    
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --fix)
                FIX_MODE=true
                shift
                ;;
            --summary)
                SUMMARY_ONLY=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --continue)
                CONTINUE_ON_ERROR=true
                shift
                ;;
            --help)
                HELP=true
                shift
                ;;
            checkstyle|pmd|spotbugs|enforcer|owasp|all)
                SELECTED_TOOL="$1"
                shift
                ;;
            core|cli|data|all)
                TARGET_MODULE="$1"
                shift
                ;;
            --module=*)
                TARGET_MODULE="${1#*=}"
                shift
                ;;
            --skip=*)
                SKIP_CHECKS="${1#*=}"
                shift
                ;;
            *)
                echo -e "${RED}Unknown option or tool: $1${NC}"
                show_help
                exit 1
                ;;
        esac
    done

    # Set defaults if not specified
    SELECTED_TOOL="${SELECTED_TOOL:-all}"
    TARGET_MODULE="${TARGET_MODULE:-all}"
    
    # Process skip list if provided
    if [[ -n "$SKIP_CHECKS" ]]; then
        IFS=',' read -r -a SKIP_ARRAY <<< "$SKIP_CHECKS"
        for skip_item in "${SKIP_ARRAY[@]}"; do
            # Remove skipped tool from CHECK_TOOLS array
            for i in "${!CHECK_TOOLS[@]}"; do
                if [[ "${CHECK_TOOLS[i]}" = "$skip_item" ]]; then
                    unset 'CHECK_TOOLS[i]'
                fi
            done
        done
        # Reindex array
        CHECK_TOOLS=("${CHECK_TOOLS[@]}")
    fi
}

# Print header
print_header() {
    echo -e "\n${BLUE}${BOLD}$1${NC}\n"
}

# Print section
print_section() {
    echo -e "${CYAN}$1${NC}"
}

# Print success
print_success() {
    echo -e "${STATUS_SUCCESS} ${GREEN}$1${NC}"
}

# Print warning
print_warning() {
    echo -e "${STATUS_WARNING} ${YELLOW}$1${NC}"
}

# Print error
print_error() {
    echo -e "${STATUS_FAILURE} ${RED}$1${NC}"
}

# Print skipped
print_skip() {
    echo -e "${STATUS_SKIPPED} ${BLUE}$1${NC}"
}

# Run a quality check tool
run_quality_tool() {
    local tool="$1"
    local module="$2"
    local fix_flag="$3"
    local tool_script="$SCRIPT_DIR/$tool.sh"
    local tool_log="$LOG_DIR/$tool-$module-$TIMESTAMP.log"
    
    # Skip if tool script doesn't exist
    if [[ ! -f "$tool_script" ]]; then
        print_error "Tool script not found: $tool_script"
        return 1
    fi
    
    # Build command arguments
    local cmd_args=""
    [[ "$module" != "all" ]] && cmd_args="$cmd_args --module=$module"
    [[ "$fix_flag" == "true" ]] && cmd_args="$cmd_args --fix"
    [[ "$VERBOSE" == "true" ]] && cmd_args="$cmd_args --verbose"
    
    # Announce the tool we're running
    print_section "Running $tool on $module"
    
    # Ensure script is executable
    chmod +x "$tool_script"
    
    # Start timer
    local start_time=$(date +%s)
    
    # Execute the tool
    if "$tool_script" $cmd_args > "$tool_log" 2>&1; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        print_success "$tool completed in ${duration}s"
        
        # Count warnings
        local warning_count=0
        local error_count=0
        if [[ -f "$tool_log" ]]; then
            warning_count=$(grep -c '\[WARN\]' "$tool_log" || true)
            error_count=$(grep -c '\[ERROR\]' "$tool_log" || true)
            
            if [[ $warning_count -gt 0 || $error_count -gt 0 ]]; then
                print_warning "$warning_count warnings, $error_count errors found"
                if [[ "$SUMMARY_ONLY" != "true" ]]; then
                    echo -e "${YELLOW}Important warnings/errors:${NC}"
                    grep -E '\[WARN\]|\[ERROR\]' "$tool_log" | head -5
                    if [[ $(grep -E '\[WARN\]|\[ERROR\]' "$tool_log" | wc -l) -gt 5 ]]; then
                        echo -e "${YELLOW}(more warnings/errors in log file)${NC}"
                    fi
                    echo -e "${BLUE}Full log: $tool_log${NC}"
                fi
            else
                print_success "No warnings or errors found"
            fi
        fi
        
        return 0
    else
        local result=$?
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        print_error "$tool failed after ${duration}s (exit code $result)"
        
        if [[ "$SUMMARY_ONLY" != "true" ]]; then
            echo -e "${RED}Last 10 lines of output:${NC}"
            tail -10 "$tool_log"
            echo -e "${RED}Full log: $tool_log${NC}"
        fi
        
        # Return failure unless continue-on-error is set
        if [[ "$CONTINUE_ON_ERROR" == "true" ]]; then
            return 0
        else
            return $result
        fi
    fi
}

# Run all quality tools sequentially
run_all_tools() {
    local module="$1"
    local fix_flag="$2"
    local success_count=0
    local failure_count=0
    local tools_to_run=()
    
    # Determine which tools to run
    if [[ "$SELECTED_TOOL" == "all" ]]; then
        tools_to_run=("${CHECK_TOOLS[@]}")
    else
        tools_to_run=("$SELECTED_TOOL")
    fi
    
    # Announce the plan
    echo -e "${BLUE}> STARTING:${NC} About to run ${#tools_to_run[@]} quality checks"
    for i in "${!tools_to_run[@]}"; do
        echo -e "  ${GRAY}$(($i + 1))/${#tools_to_run[@]}:${NC} ${tools_to_run[$i]}"
    done
    echo ""
    
    # Run each tool
    for tool in "${tools_to_run[@]}"; do
        if run_quality_tool "$tool" "$module" "$fix_flag"; then
            ((success_count++))
        else
            ((failure_count++))
            
            # Add helpful suggestions based on the failing tool
            case "$tool" in
                checkstyle)
                    echo -e "${YELLOW}Checkstyle issues can often be fixed with:${NC}"
                    echo -e "  $SCRIPT_DIR/fix-imports.sh --module=$module"
                    ;;
                pmd)
                    echo -e "${YELLOW}PMD issues usually require manual code fixes.${NC}"
                    echo -e "  See: $RINNA_DIR/config/pmd/pmd-ruleset.xml for rule definitions"
                    ;;
                spotbugs)
                    echo -e "${YELLOW}SpotBugs issues may indicate real bugs that need manual review.${NC}"
                    echo -e "  See: $RINNA_DIR/config/spotbugs/spotbugs-exclude.xml for exclusions"
                    ;;
                enforcer)
                    echo -e "${YELLOW}Enforcer issues indicate dependency or versioning problems.${NC}"
                    echo -e "  Check POM files for conflicting dependencies"
                    ;;
                owasp)
                    echo -e "${YELLOW}OWASP issues indicate security vulnerabilities in dependencies.${NC}"
                    echo -e "  See: $RINNA_DIR/config/owasp/suppressions.xml for suppression options"
                    ;;
            esac
            
            if [[ "$CONTINUE_ON_ERROR" != "true" ]]; then
                echo -e "${RED}Stopping due to errors. Use --continue to run all tools regardless of failures.${NC}"
                return $failure_count
            fi
        fi
        
        echo ""  # Add a blank line between tools
    done
    
    if [[ $failure_count -gt 0 ]]; then
        return $failure_count
    else
        return 0
    fi
}

# Generate a summary report
generate_summary() {
    local module="$1"
    local has_errors=false
    local total_warnings=0
    local total_errors=0
    
    print_header "Quality Check Summary"
    
    # Calculate totals for all tools
    for tool in "${CHECK_TOOLS[@]}"; do
        local tool_log="$LOG_DIR/$tool-$module-$TIMESTAMP.log"
        if [[ -f "$tool_log" ]]; then
            local warning_count=$(grep -c '\[WARN\]' "$tool_log" || true)
            local error_count=$(grep -c '\[ERROR\]' "$tool_log" || true)
            
            ((total_warnings += warning_count))
            ((total_errors += error_count))
            
            if [[ $error_count -gt 0 ]]; then
                print_error "$tool: $error_count errors, $warning_count warnings"
                has_errors=true
            elif [[ $warning_count -gt 0 ]]; then
                print_warning "$tool: $warning_count warnings"
            else
                print_success "$tool: passed"
            fi
        fi
    done
    
    # Print overall statistics
    echo -e "\n${BLUE}Overall Statistics:${NC}"
    echo -e "  Tools run: ${#CHECK_TOOLS[@]}"
    echo -e "  Total warnings: $total_warnings"
    echo -e "  Total errors: $total_errors"
    
    # Print log file location
    echo -e "\n${BLUE}Detailed logs available at:${NC}"
    echo "$LOG_DIR"
    
    # Suggest next steps if there are failures
    if [[ "$has_errors" == "true" ]]; then
        echo -e "\n${YELLOW}Suggested next steps:${NC}"
        echo "1. Run individual tools to focus on specific issues:"
        echo "   $(basename "$0") checkstyle"
        echo "2. Try auto-fixing where possible:"
        echo "   $(basename "$0") --fix checkstyle"
        echo "3. Check detailed logs for specific issues"
    fi
    
    return 0
}

# Main function
main() {
    # Parse command-line args
    parse_args "$@"
    
    # Show help and exit if requested
    if [[ "$HELP" == "true" ]]; then
        show_help
        exit 0
    fi
    
    # Print configuration
    print_header "Rinna Quality Check"
    echo "Configuration:"
    echo "  Tool:       $SELECTED_TOOL"
    echo "  Module:     $TARGET_MODULE"
    echo "  Fix mode:   $FIX_MODE"
    echo "  Summary:    $SUMMARY_ONLY"
    echo "  Verbose:    $VERBOSE"
    echo "  Continue:   $CONTINUE_ON_ERROR"
    echo "  Log dir:    $LOG_DIR"
    echo ""
    
    # Record start time
    local start_time=$(date +%s)
    
    # Run quality checks
    local exit_code=0
    if run_all_tools "$TARGET_MODULE" "$FIX_MODE"; then
        print_success "All quality checks completed successfully"
    else
        exit_code=$?
        print_error "$exit_code quality tools reported issues"
    fi
    
    # Calculate total duration
    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    
    # Generate and show summary
    generate_summary "$TARGET_MODULE"
    
    # Final status message
    echo -e "\n${BLUE}Total execution time: ${total_duration}s${NC}"
    if [[ $exit_code -eq 0 ]]; then
        print_success "Quality check completed successfully"
    else
        print_error "Quality check completed with issues"
    fi
    
    exit $exit_code
}

# Execute main function
main "$@"