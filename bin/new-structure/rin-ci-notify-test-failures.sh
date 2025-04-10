#!/bin/bash
# test-failure-notify.sh - Test failure notification system for Rinna
#
# This script processes JUnit XML test reports, identifies failures, 
# and sends notifications through appropriate channels.
#
# Usage: 
#   ./test-failure-notify.sh [OPTIONS]
#
# Options:
#   --reports-dir DIR   Directory containing test reports (default: target/surefire-reports)
#   --notify-method METHOD  Notification method (slack, email, internal, all) (default: internal)
#   --slack-webhook URL     Slack webhook URL for notifications
#   --email-to EMAIL        Email address to send notifications to
#   --threshold NUMBER      Minimum number of failures to trigger notification (default: 1)
#   --summary-only          Only include summary in notification (no test details)
#   --ci                    Running in CI environment (adjusts output format)
#   --verbose               Show verbose output
#   --help                  Show this help message

set -eo pipefail

# Default values
REPORTS_DIR="target/surefire-reports"
NOTIFY_METHOD="internal"
SLACK_WEBHOOK=""
EMAIL_TO=""
THRESHOLD=1
SUMMARY_ONLY=false
CI_MODE=false
VERBOSE=false

# ANSI color codes for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
CLEAR='\033[0m'

# Load Rinna logger if available
if [ -f "bin/common/rinna_logger.sh" ]; then
    source bin/common/rinna_logger.sh
else
    # Simple logger functions if rinna_logger.sh is not available
    function log_info() { echo -e "${BLUE}[INFO]${CLEAR} $*"; }
    function log_success() { echo -e "${GREEN}[SUCCESS]${CLEAR} $*"; }
    function log_warning() { echo -e "${YELLOW}[WARNING]${CLEAR} $*"; }
    function log_error() { echo -e "${RED}[ERROR]${CLEAR} $*"; }
    function log_debug() { if $VERBOSE; then echo -e "${CYAN}[DEBUG]${CLEAR} $*"; fi; }
fi

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --reports-dir)
            REPORTS_DIR="$2"
            shift 2
            ;;
        --notify-method)
            NOTIFY_METHOD="$2"
            shift 2
            ;;
        --slack-webhook)
            SLACK_WEBHOOK="$2"
            shift 2
            ;;
        --email-to)
            EMAIL_TO="$2"
            shift 2
            ;;
        --threshold)
            THRESHOLD="$2"
            shift 2
            ;;
        --summary-only)
            SUMMARY_ONLY=true
            shift
            ;;
        --ci)
            CI_MODE=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Test Failure Notification System for Rinna"
            echo ""
            echo "Usage: ./test-failure-notify.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --reports-dir DIR       Directory containing test reports (default: target/surefire-reports)"
            echo "  --notify-method METHOD  Notification method (slack, email, internal, all) (default: internal)"
            echo "  --slack-webhook URL     Slack webhook URL for notifications"
            echo "  --email-to EMAIL        Email address to send notifications to"
            echo "  --threshold NUMBER      Minimum number of failures to trigger notification (default: 1)"
            echo "  --summary-only          Only include summary in notification (no test details)"
            echo "  --ci                    Running in CI environment (adjusts output format)"
            echo "  --verbose               Show verbose output"
            echo "  --help                  Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Validate inputs
if [ "$NOTIFY_METHOD" == "slack" ] && [ -z "$SLACK_WEBHOOK" ]; then
    log_error "Slack webhook URL must be provided when using slack notification method"
    exit 1
fi

if [ "$NOTIFY_METHOD" == "email" ] && [ -z "$EMAIL_TO" ]; then
    log_error "Email address must be provided when using email notification method"
    exit 1
fi

if [ ! -d "$REPORTS_DIR" ]; then
    log_warning "Reports directory '$REPORTS_DIR' not found. No test results to process."
    exit 0
fi

# Function to parse JUnit XML test reports and extract failure information
parse_test_reports() {
    local reports_dir="$1"
    local total_tests=0
    local total_failures=0
    local total_errors=0
    local total_skipped=0
    local failure_details=""
    
    log_debug "Parsing test reports in directory: $reports_dir"
    
    # Process all XML report files
    for report in $(find "$reports_dir" -name "TEST-*.xml" 2>/dev/null); do
        log_debug "Processing report: $report"
        
        # Extract test counts from the testsuites element
        local testsuite_tests=$(grep -o 'tests="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
        local testsuite_failures=$(grep -o 'failures="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
        local testsuite_errors=$(grep -o 'errors="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
        local testsuite_skipped=$(grep -o 'skipped="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
        local testsuite_name=$(grep -o 'name="[^"]*"' "$report" | head -1 | cut -d'"' -f2)
        
        # Add to totals if values are found
        [[ -n "$testsuite_tests" ]] && total_tests=$((total_tests + testsuite_tests)) || true
        [[ -n "$testsuite_failures" ]] && total_failures=$((total_failures + testsuite_failures)) || true
        [[ -n "$testsuite_errors" ]] && total_errors=$((total_errors + testsuite_errors)) || true
        [[ -n "$testsuite_skipped" ]] && total_skipped=$((total_skipped + testsuite_skipped)) || true
        
        # If there are failures or errors, extract details
        if [[ -n "$testsuite_failures" && "$testsuite_failures" -gt 0 ]] || [[ -n "$testsuite_errors" && "$testsuite_errors" -gt 0 ]]; then
            log_debug "Found failures/errors in $testsuite_name"
            
            # Extract failure/error details
            local testcase_pattern='<testcase[^>]*name="([^"]*)"[^>]*class="([^"]*)"[^>]*time="([^"]*)"[^>]*>'
            local failure_pattern='<failure[^>]*message="([^"]*)"[^>]*type="([^"]*)"[^>]*>(.*?)<\/failure>'
            local error_pattern='<error[^>]*message="([^"]*)"[^>]*type="([^"]*)"[^>]*>(.*?)<\/error>'
            
            # Use a combination of grep and sed to extract test cases with failures/errors
            grep -A 50 '<testcase' "$report" | grep -B 50 '</testcase>' | tr '\n' ' ' | \
            sed -E "s/<testcase/\n<testcase/g" | grep -E '(<failure|<error)' | \
            while read -r test_block; do
                local test_name=$(echo "$test_block" | grep -oP 'name="\K[^"]+')
                local test_class=$(echo "$test_block" | grep -oP 'classname="\K[^"]+')
                local error_msg=$(echo "$test_block" | grep -oP 'message="\K[^"]+')
                
                if [ -n "$test_name" ] && [ -n "$test_class" ]; then
                    local formatted_msg="${test_class}#${test_name}: ${error_msg:-Error details not available}"
                    failure_details="${failure_details}\n- ${formatted_msg}"
                    log_debug "Added failure detail: $formatted_msg"
                fi
            done
        fi
    done
    
    # Return results as a JSON-like string
    echo "{\"total_tests\":$total_tests,\"total_failures\":$total_failures,\"total_errors\":$total_errors,\"total_skipped\":$total_skipped,\"failure_details\":\"$failure_details\"}"
}

# Function to send notification to Slack
send_slack_notification() {
    local webhook_url="$1"
    local message="$2"
    
    if [ -z "$webhook_url" ]; then
        log_error "Slack webhook URL is required"
        return 1
    fi
    
    log_debug "Sending Slack notification..."
    
    # Format the message for Slack (basic markdown)
    local slack_payload="{\"text\":\"$message\"}"
    
    # Send to Slack
    if curl -s -X POST -H 'Content-type: application/json' --data "$slack_payload" "$webhook_url"; then
        log_success "Slack notification sent successfully"
    else
        log_error "Failed to send Slack notification"
        return 1
    fi
}

# Function to send notification via email
send_email_notification() {
    local to_address="$1"
    local subject="$2"
    local message="$3"
    
    if [ -z "$to_address" ]; then
        log_error "Email address is required"
        return 1
    fi
    
    log_debug "Sending email notification to $to_address..."
    
    # Use mail command to send email
    if echo -e "$message" | mail -s "$subject" "$to_address"; then
        log_success "Email notification sent successfully to $to_address"
    else
        log_error "Failed to send email notification"
        return 1
    fi
}

# Function to create an internal notification using the Rinna notification system
create_internal_notification() {
    local message="$1"
    
    log_debug "Creating internal notification..."
    
    # Check if rin command is available
    if ! command -v bin/rin &> /dev/null; then
        log_warning "bin/rin command not found, skipping internal notification"
        return 0
    fi
    
    # Create notification using Rinna's notification system
    if bin/rin notify create --type SYSTEM --priority HIGH --message "$message"; then
        log_success "Internal notification created successfully"
    else
        log_error "Failed to create internal notification"
        return 1
    fi
}

# Main execution
log_info "Starting test failure notification process..."
log_debug "Reports directory: $REPORTS_DIR"
log_debug "Notification method: $NOTIFY_METHOD"
log_debug "Failure threshold: $THRESHOLD"

# Parse test reports
report_data=$(parse_test_reports "$REPORTS_DIR")
log_debug "Parsed report data: $report_data"

# Extract values from report data
total_tests=$(echo "$report_data" | grep -o '"total_tests":[0-9]*' | cut -d':' -f2)
total_failures=$(echo "$report_data" | grep -o '"total_failures":[0-9]*' | cut -d':' -f2)
total_errors=$(echo "$report_data" | grep -o '"total_errors":[0-9]*' | cut -d':' -f2)
total_skipped=$(echo "$report_data" | grep -o '"total_skipped":[0-9]*' | cut -d':' -f2)
failure_details=$(echo "$report_data" | grep -o '"failure_details":"[^"]*"' | cut -d':' -f2- | sed 's/^"//;s/"$//' | sed 's/\\n/\n/g')

# Get build information
git_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "Unknown Branch")
git_commit=$(git rev-parse --short HEAD 2>/dev/null || echo "Unknown Commit")
builder=$(whoami)
build_date=$(date "+%Y-%m-%d %H:%M:%S")
hostname=$(hostname)

# Determine if we need to send notifications
total_issues=$((total_failures + total_errors))

if [ "$total_issues" -ge "$THRESHOLD" ]; then
    log_warning "Found $total_issues test issues, preparing notification..."
    
    # Create notification message
    notification_title="🚨 Test Failure Alert: $total_issues test issues detected ($total_failures failures, $total_errors errors)"
    notification_summary="Test Results Summary:\n"
    notification_summary+="- Total Tests: $total_tests\n"
    notification_summary+="- Failures: $total_failures\n"
    notification_summary+="- Errors: $total_errors\n"
    notification_summary+="- Skipped: $total_skipped\n\n"
    notification_summary+="Build Information:\n"
    notification_summary+="- Branch: $git_branch\n"
    notification_summary+="- Commit: $git_commit\n"
    notification_summary+="- Date: $build_date\n"
    notification_summary+="- Builder: $builder\n"
    notification_summary+="- Host: $hostname\n\n"
    
    # Add failure details if not summary only
    if [ "$SUMMARY_ONLY" = false ] && [ -n "$failure_details" ]; then
        notification_summary+="Test Failure Details:\n$failure_details\n\n"
    fi
    
    notification_summary+="Please investigate these failures as soon as possible."
    
    # Send notifications based on selected method
    case "$NOTIFY_METHOD" in
        "slack")
            send_slack_notification "$SLACK_WEBHOOK" "$notification_title\n\n$notification_summary"
            ;;
        "email")
            send_email_notification "$EMAIL_TO" "Rinna Test Failure Notification" "$notification_title\n\n$notification_summary"
            ;;
        "internal")
            create_internal_notification "$notification_title\n\n$notification_summary"
            ;;
        "all")
            log_info "Sending notifications via all available methods..."
            
            [ -n "$SLACK_WEBHOOK" ] && send_slack_notification "$SLACK_WEBHOOK" "$notification_title\n\n$notification_summary"
            [ -n "$EMAIL_TO" ] && send_email_notification "$EMAIL_TO" "Rinna Test Failure Notification" "$notification_title\n\n$notification_summary"
            create_internal_notification "$notification_title\n\n$notification_summary"
            ;;
        *)
            log_error "Unknown notification method: $NOTIFY_METHOD"
            exit 1
            ;;
    esac
    
    # Output message for CI environments
    if [ "$CI_MODE" = true ]; then
        echo "::warning::$total_issues test issues detected ($total_failures failures, $total_errors errors)"
    fi
else
    log_success "All tests passed or below notification threshold (Found: $total_issues, Threshold: $THRESHOLD)"
fi

# Exit with non-zero code if there were failures
if [ "$total_issues" -gt 0 ]; then
    exit 1
else
    exit 0
fi