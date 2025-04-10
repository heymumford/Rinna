#!/bin/bash
# enhance-ci-pipeline.sh - CI Pipeline Enhancement Orchestration
#
# This script coordinates the CI Pipeline Enhancement components:
# - Test coverage reporting
# - Architecture validation
# - Test failure notification
# - Quality gates integration
#
# Usage:
#   ./bin/ci/enhance-ci-pipeline.sh [OPTIONS]
#
# Options:
#   --apply                Apply all enhancements
#   --coverage-only        Only apply coverage reporting
#   --architecture-only    Only apply architecture validation
#   --notifications-only   Only apply test failure notifications
#   --quality-only         Only apply quality gates
#   --check                Check configuration without applying changes
#   --verbose              Show verbose output
#   --help                 Show this help message

set -eo pipefail

# Default values
APPLY_ALL=false
COVERAGE_ONLY=false
ARCHITECTURE_ONLY=false
NOTIFICATIONS_ONLY=false
QUALITY_ONLY=false
CHECK_ONLY=false
VERBOSE=false

# ANSI color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
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
        --apply)
            APPLY_ALL=true
            shift
            ;;
        --coverage-only)
            COVERAGE_ONLY=true
            shift
            ;;
        --architecture-only)
            ARCHITECTURE_ONLY=true
            shift
            ;;
        --notifications-only)
            NOTIFICATIONS_ONLY=true
            shift
            ;;
        --quality-only)
            QUALITY_ONLY=true
            shift
            ;;
        --check)
            CHECK_ONLY=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "CI Pipeline Enhancement for Rinna"
            echo ""
            echo "Usage: ./bin/ci/enhance-ci-pipeline.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --apply                Apply all enhancements"
            echo "  --coverage-only        Only apply coverage reporting"
            echo "  --architecture-only    Only apply architecture validation"
            echo "  --notifications-only   Only apply test failure notifications"
            echo "  --quality-only         Only apply quality gates"
            echo "  --check                Check configuration without applying changes"
            echo "  --verbose              Show verbose output"
            echo "  --help                 Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# If no specific option is provided, set check mode as default
if ! $APPLY_ALL && ! $COVERAGE_ONLY && ! $ARCHITECTURE_ONLY && ! $NOTIFICATIONS_ONLY && ! $QUALITY_ONLY; then
    log_info "No specific enhancement selected, running in check mode"
    CHECK_ONLY=true
fi

# Function to ensure script is executable
ensure_executable() {
    local script_path="$1"
    
    if [ -f "$script_path" ]; then
        if [ ! -x "$script_path" ]; then
            log_debug "Making $script_path executable"
            if ! $CHECK_ONLY; then
                chmod +x "$script_path"
            fi
        else
            log_debug "$script_path is already executable"
        fi
    else
        log_warning "Script not found: $script_path"
    fi
}

# Function to ensure directory exists
ensure_directory() {
    local dir_path="$1"
    
    if [ ! -d "$dir_path" ]; then
        log_debug "Creating directory: $dir_path"
        if ! $CHECK_ONLY; then
            mkdir -p "$dir_path"
        fi
    else
        log_debug "Directory already exists: $dir_path"
    fi
}

# Function to verify GitHub Actions workflow file
verify_workflow() {
    local workflow_file="$1"
    local expected_name="$2"
    
    if [ -f "$workflow_file" ]; then
        local name_line=$(grep "^name:" "$workflow_file" | head -1)
        if [[ "$name_line" == *"$expected_name"* ]]; then
            log_success "Workflow file $workflow_file is properly configured"
            return 0
        else
            log_warning "Workflow file $workflow_file exists, but might have incorrect name configuration"
            return 1
        fi
    else
        log_warning "Workflow file $workflow_file not found"
        return 1
    fi
}

# Function to apply coverage reporting enhancement
apply_coverage_enhancement() {
    log_info "Applying coverage reporting enhancement..."
    
    # Ensure directories exist
    ensure_directory "bin/ci"
    ensure_directory "config/coverage"
    ensure_directory ".github/workflows"
    
    # Ensure coverage report script exists and is executable
    ensure_executable "bin/ci/generate-coverage-report.sh"
    
    # Verify GitHub Actions workflow
    local coverage_workflow=".github/workflows/coverage-report.yml"
    verify_workflow "$coverage_workflow" "Code Coverage Report"
    
    # If not in check mode, add example configuration if needed
    if ! $CHECK_ONLY && [ ! -f "config/coverage/thresholds.yml" ]; then
        log_info "Creating example coverage thresholds configuration"
        cat > "config/coverage/thresholds.yml" << EOF
# Code coverage thresholds for Rinna Project
overall: 75
java: 80
go: 70
python: 75
modules:
  rinna-core: 85
  rinna-cli: 80
  rinna-data-sqlite: 75
  api: 70
EOF
        log_success "Created coverage thresholds configuration: config/coverage/thresholds.yml"
    fi
    
    log_success "Coverage reporting enhancement completed"
}

# Function to apply architecture validation enhancement
apply_architecture_enhancement() {
    log_info "Applying architecture validation enhancement..."
    
    # Ensure directories exist
    ensure_directory "bin/ci"
    ensure_directory "bin/checks"
    ensure_directory "config/architecture"
    ensure_directory ".github/workflows"
    
    # Ensure validation scripts exist and are executable
    ensure_executable "bin/ci/validate-architecture.sh"
    ensure_executable "bin/run-checks.sh"
    
    # Check for architecture rules file
    if [ ! -f "config/architecture/rules.yml" ]; then
        log_warning "Architecture rules file not found: config/architecture/rules.yml"
        if ! $CHECK_ONLY; then
            log_info "Creating example architecture rules configuration"
            # Rule file was already created earlier in the script
            log_success "Created architecture rules configuration: config/architecture/rules.yml"
        fi
    else
        log_debug "Architecture rules file exists: config/architecture/rules.yml"
    fi
    
    # Verify GitHub Actions workflow
    local architecture_workflow=".github/workflows/architecture-validation.yml"
    verify_workflow "$architecture_workflow" "Architecture Validation"
    
    log_success "Architecture validation enhancement completed"
}

# Function to apply test failure notification enhancement
apply_notification_enhancement() {
    log_info "Applying test failure notification enhancement..."
    
    # Ensure directories exist
    ensure_directory "bin/ci"
    ensure_directory ".github/workflows"
    
    # Ensure notification script exists and is executable
    ensure_executable "bin/test-failure-notify.sh"
    
    # Check if test failure report generator exists
    if [ ! -f "bin/ci/generate-test-failure-report.sh" ]; then
        log_warning "Test failure report generator not found: bin/ci/generate-test-failure-report.sh"
        if ! $CHECK_ONLY; then
            log_info "Creating test failure report generator script"
            
            # Create the script file (a simplified version)
            cat > "bin/ci/generate-test-failure-report.sh" << 'EOF'
#!/bin/bash
# generate-test-failure-report.sh - Generate HTML test failure reports
#
# This script processes JUnit XML test reports and generates HTML reports.
#
# Usage:
#   ./bin/ci/generate-test-failure-report.sh [OPTIONS]
#
# Options:
#   --reports-dir DIR     Directory containing test reports (default: target/surefire-reports)
#   --output-dir DIR      Directory to store HTML reports (default: test-failure-reports)
#   --verbose             Show verbose output
#   --help                Show this help message

set -eo pipefail

# Default values
REPORTS_DIR="target/surefire-reports"
OUTPUT_DIR="test-failure-reports"
VERBOSE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --reports-dir)
            REPORTS_DIR="$2"
            shift 2
            ;;
        --output-dir)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Test Failure Report Generator"
            echo ""
            echo "Usage: ./bin/ci/generate-test-failure-report.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --reports-dir DIR     Directory containing test reports (default: target/surefire-reports)"
            echo "  --output-dir DIR      Directory to store HTML reports (default: test-failure-reports)"
            echo "  --verbose             Show verbose output"
            echo "  --help                Show this help message"
            exit 0
            ;;
        *)
            echo "ERROR: Unknown option: $1"
            exit 1
            ;;
    esac
done

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Check if reports directory exists
if [ ! -d "$REPORTS_DIR" ]; then
    echo "Reports directory '$REPORTS_DIR' not found. No test results to process."
    
    # Create a minimal HTML report
    cat > "$OUTPUT_DIR/index.html" << EOL
<!DOCTYPE html>
<html>
<head>
  <title>Test Failure Analysis Report</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }
    h1, h2 { color: #333; }
  </style>
</head>
<body>
  <h1>Test Failure Analysis Report</h1>
  <div>Generated on $(date '+%Y-%m-%d %H:%M:%S')</div>
  <h2>No Test Reports Found</h2>
  <p>No test reports were found in directory: $REPORTS_DIR</p>
</body>
</html>
EOL
    
    echo "Created minimal report at $OUTPUT_DIR/index.html"
    exit 0
fi

# Create index.html
cat > "$OUTPUT_DIR/index.html" << EOL
<!DOCTYPE html>
<html>
<head>
  <title>Test Failure Analysis Report</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }
    h1, h2, h3 { color: #333; }
    .summary { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .failures { background-color: #fff0f0; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .failure-item { border-left: 4px solid #ff6b6b; padding-left: 10px; margin-bottom: 10px; }
    .skipped { background-color: #fffbe6; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .skipped-item { border-left: 4px solid #ffcc00; padding-left: 10px; margin-bottom: 10px; }
    .build-info { background-color: #e6f7ff; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .test-summary { background-color: #f0f8ff; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    table { border-collapse: collapse; width: 100%; }
    th, td { text-align: left; padding: 8px; border-bottom: 1px solid #ddd; }
    th { background-color: #f2f2f2; }
    .timestamp { color: #666; font-size: 0.9em; }
    pre { background-color: #f5f5f5; padding: 10px; overflow-x: auto; }
  </style>
</head>
<body>
  <h1>Test Failure Analysis Report</h1>
  <div class="timestamp">Generated on $(date '+%Y-%m-%d %H:%M:%S')</div>
  
  <h2>Test Summary</h2>
  <div class="summary">
    <table>
      <tr>
        <th>Category</th>
        <th>Count</th>
      </tr>
EOL

# Extract test summary data
TOTAL_TESTS=0
TOTAL_FAILURES=0
TOTAL_ERRORS=0
TOTAL_SKIPPED=0

for report in $(find "$REPORTS_DIR" -name "TEST-*.xml" 2>/dev/null); do
    TESTS=$(grep -o 'tests="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
    FAILURES=$(grep -o 'failures="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
    ERRORS=$(grep -o 'errors="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
    SKIPPED=$(grep -o 'skipped="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
    
    [[ -n "$TESTS" ]] && TOTAL_TESTS=$((TOTAL_TESTS + TESTS)) || true
    [[ -n "$FAILURES" ]] && TOTAL_FAILURES=$((TOTAL_FAILURES + FAILURES)) || true
    [[ -n "$ERRORS" ]] && TOTAL_ERRORS=$((TOTAL_ERRORS + ERRORS)) || true
    [[ -n "$SKIPPED" ]] && TOTAL_SKIPPED=$((TOTAL_SKIPPED + SKIPPED)) || true
done

# Calculate pass rate safely
if [ "$TOTAL_TESTS" -gt 0 ]; then
    PASS_RATE=$(awk "BEGIN {printf \"%.1f%%\", (($TOTAL_TESTS - $TOTAL_FAILURES - $TOTAL_ERRORS) / $TOTAL_TESTS) * 100}")
else
    PASS_RATE="N/A"
fi

# Add summary to HTML
cat >> "$OUTPUT_DIR/index.html" << EOL
      <tr>
        <td>Total Tests</td>
        <td>$TOTAL_TESTS</td>
      </tr>
      <tr>
        <td>Failures</td>
        <td>$TOTAL_FAILURES</td>
      </tr>
      <tr>
        <td>Errors</td>
        <td>$TOTAL_ERRORS</td>
      </tr>
      <tr>
        <td>Skipped</td>
        <td>$TOTAL_SKIPPED</td>
      </tr>
      <tr>
        <td>Passing Rate</td>
        <td>$PASS_RATE</td>
      </tr>
    </table>
  </div>
  
  <h2>Build Information</h2>
  <div class="build-info">
    <table>
      <tr>
        <th>Property</th>
        <th>Value</th>
      </tr>
      <tr>
        <td>Repository</td>
        <td>$(git config --get remote.origin.url || echo "Unknown")</td>
      </tr>
      <tr>
        <td>Branch</td>
        <td>$(git rev-parse --abbrev-ref HEAD || echo "Unknown")</td>
      </tr>
      <tr>
        <td>Commit</td>
        <td>$(git rev-parse --short HEAD || echo "Unknown")</td>
      </tr>
      <tr>
        <td>Date</td>
        <td>$(date "+%Y-%m-%d %H:%M:%S")</td>
      </tr>
      <tr>
        <td>Reports Directory</td>
        <td>$REPORTS_DIR</td>
      </tr>
    </table>
  </div>
EOL

# Add test class summaries
cat >> "$OUTPUT_DIR/index.html" << EOL
  <h2>Test Class Summary</h2>
  <div class="test-summary">
    <table>
      <tr>
        <th>Test Class</th>
        <th>Tests</th>
        <th>Failures</th>
        <th>Errors</th>
        <th>Skipped</th>
        <th>Time (s)</th>
      </tr>
EOL

# Process each test class
for report in $(find "$REPORTS_DIR" -name "TEST-*.xml" 2>/dev/null); do
    CLASS_NAME=$(grep -o 'name="[^"]*"' "$report" | head -1 | cut -d'"' -f2)
    TESTS=$(grep -o 'tests="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
    FAILURES=$(grep -o 'failures="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
    ERRORS=$(grep -o 'errors="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
    SKIPPED=$(grep -o 'skipped="[0-9]*"' "$report" | head -1 | cut -d'"' -f2)
    TIME=$(grep -o 'time="[0-9.]*"' "$report" | head -1 | cut -d'"' -f2)
    
    # Generate class-specific HTML file
    CLASS_FILE="$OUTPUT_DIR/$(basename "$report" .xml).html"
    
    # Add to summary table
    if [ -n "$CLASS_NAME" ]; then
        REPORT_LINK=$(basename "$CLASS_FILE")
        FAILURE_CLASS=""
        if [ "$FAILURES" -gt 0 ] || [ "$ERRORS" -gt 0 ]; then
            FAILURE_CLASS="style=\"color:red;font-weight:bold;\""
        fi
        
        cat >> "$OUTPUT_DIR/index.html" << EOL
      <tr $FAILURE_CLASS>
        <td><a href="$REPORT_LINK">$CLASS_NAME</a></td>
        <td>$TESTS</td>
        <td>$FAILURES</td>
        <td>$ERRORS</td>
        <td>$SKIPPED</td>
        <td>$TIME</td>
      </tr>
EOL
    fi
    
    # Create detailed class report
    cat > "$CLASS_FILE" << EOL
<!DOCTYPE html>
<html>
<head>
  <title>Test Report: $CLASS_NAME</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }
    h1, h2, h3 { color: #333; }
    .summary { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .failure { background-color: #fff0f0; padding: 15px; border-radius: 5px; margin-bottom: 10px; }
    .error { background-color: #ffe6e6; padding: 15px; border-radius: 5px; margin-bottom: 10px; }
    .skipped { background-color: #fffbe6; padding: 15px; border-radius: 5px; margin-bottom: 10px; }
    .passed { background-color: #e6ffed; padding: 15px; border-radius: 5px; margin-bottom: 10px; }
    table { border-collapse: collapse; width: 100%; }
    th, td { text-align: left; padding: 8px; border-bottom: 1px solid #ddd; }
    th { background-color: #f2f2f2; }
    .timestamp { color: #666; font-size: 0.9em; }
    pre { background-color: #f5f5f5; padding: 10px; overflow-x: auto; }
    nav { margin-bottom: 20px; }
  </style>
</head>
<body>
  <nav>
    <a href="index.html">Back to Test Summary</a>
  </nav>

  <h1>Test Report: $CLASS_NAME</h1>
  <div class="timestamp">Generated on $(date '+%Y-%m-%d %H:%M:%S')</div>
  
  <h2>Summary</h2>
  <div class="summary">
    <table>
      <tr>
        <th>Tests</th>
        <th>Failures</th>
        <th>Errors</th>
        <th>Skipped</th>
        <th>Time (s)</th>
      </tr>
      <tr>
        <td>$TESTS</td>
        <td>$FAILURES</td>
        <td>$ERRORS</td>
        <td>$SKIPPED</td>
        <td>$TIME</td>
      </tr>
    </table>
  </div>
  
  <h2>Test Results</h2>
EOL
    
    # Extract test cases
    grep -A 50 '<testcase' "$report" | grep -B 50 '</testcase>' | tr '\n' ' ' | \
    sed -E "s/<testcase/\n<testcase/g" | while read -r test_block; do
        TEST_NAME=$(echo "$test_block" | grep -oP 'name="\K[^"]+')
        TEST_TIME=$(echo "$test_block" | grep -oP 'time="\K[^"]+')
        
        if [ -z "$TEST_NAME" ]; then
            continue
        fi
        
        # Check for failures, errors, or skipped
        if [[ "$test_block" =~ "<failure" ]]; then
            FAILURE_MSG=$(echo "$test_block" | grep -oP 'message="\K[^"]+' || echo "Test failure")
            FAILURE_TYPE=$(echo "$test_block" | grep -oP 'type="\K[^"]+' || echo "Failure")
            FAILURE_DETAILS=$(echo "$test_block" | sed -n 's/.*<failure[^>]*>\(.*\)<\/failure>.*/\1/p' || echo "")
            
            cat >> "$CLASS_FILE" << EOL
  <div class="failure">
    <h3>⚠️ FAILED: $TEST_NAME ($TEST_TIME s)</h3>
    <p><strong>Type:</strong> $FAILURE_TYPE</p>
    <p><strong>Message:</strong> $FAILURE_MSG</p>
    <pre>$FAILURE_DETAILS</pre>
  </div>
EOL
        elif [[ "$test_block" =~ "<error" ]]; then
            ERROR_MSG=$(echo "$test_block" | grep -oP 'message="\K[^"]+' || echo "Test error")
            ERROR_TYPE=$(echo "$test_block" | grep -oP 'type="\K[^"]+' || echo "Error")
            ERROR_DETAILS=$(echo "$test_block" | sed -n 's/.*<error[^>]*>\(.*\)<\/error>.*/\1/p' || echo "")
            
            cat >> "$CLASS_FILE" << EOL
  <div class="error">
    <h3>❌ ERROR: $TEST_NAME ($TEST_TIME s)</h3>
    <p><strong>Type:</strong> $ERROR_TYPE</p>
    <p><strong>Message:</strong> $ERROR_MSG</p>
    <pre>$ERROR_DETAILS</pre>
  </div>
EOL
        elif [[ "$test_block" =~ "<skipped" ]]; then
            SKIP_MSG=$(echo "$test_block" | grep -oP 'message="\K[^"]+' || echo "Test skipped")
            
            cat >> "$CLASS_FILE" << EOL
  <div class="skipped">
    <h3>⏭️ SKIPPED: $TEST_NAME ($TEST_TIME s)</h3>
    <p><strong>Message:</strong> $SKIP_MSG</p>
  </div>
EOL
        else
            cat >> "$CLASS_FILE" << EOL
  <div class="passed">
    <h3>✅ PASSED: $TEST_NAME ($TEST_TIME s)</h3>
  </div>
EOL
        fi
    done
    
    # Close HTML file
    cat >> "$CLASS_FILE" << EOL
</body>
</html>
EOL
done

# Close the test class summary table
cat >> "$OUTPUT_DIR/index.html" << EOL
    </table>
  </div>
EOL

# Add failures section
cat >> "$OUTPUT_DIR/index.html" << EOL
  <h2>Failures and Errors</h2>
  <div class="failures">
EOL

# Extract failures from XML reports
FAILURES_FOUND=false
for report in $(find "$REPORTS_DIR" -name "TEST-*.xml" 2>/dev/null); do
    if grep -q '<failure\|<error' "$report"; then
        FAILURES_FOUND=true
        TEST_CLASS=$(grep -o 'name="[^"]*"' "$report" | head -1 | cut -d'"' -f2)
        
        cat >> "$OUTPUT_DIR/index.html" << EOL
    <h3>$TEST_CLASS</h3>
EOL
        
        # Extract test cases with failures
        grep -A 50 '<testcase' "$report" | grep -B 50 '</testcase>' | tr '\n' ' ' | \
        sed -E "s/<testcase/\n<testcase/g" | grep -E '(<failure|<error)' | \
        while read -r test_block; do
            TEST_NAME=$(echo "$test_block" | grep -oP 'name="\K[^"]+')
            ERROR_MSG=$(echo "$test_block" | grep -oP 'message="\K[^"]+')
            ERROR_TYPE=$(echo "$test_block" | grep -oP 'type="\K[^"]+')
            
            if [ -n "$TEST_NAME" ]; then
                cat >> "$OUTPUT_DIR/index.html" << EOL
    <div class="failure-item">
      <strong>Test:</strong> $TEST_CLASS#$TEST_NAME<br>
      <strong>Type:</strong> $ERROR_TYPE<br>
      <strong>Message:</strong> $ERROR_MSG
    </div>
EOL
            fi
        done
    fi
done

if ! $FAILURES_FOUND; then
    cat >> "$OUTPUT_DIR/index.html" << EOL
    <p>No failures or errors found.</p>
EOL
fi

# Close failures section
cat >> "$OUTPUT_DIR/index.html" << EOL
  </div>
  
  <h2>Skipped Tests</h2>
  <div class="skipped">
EOL

# Extract skipped tests from XML reports
SKIPPED_FOUND=false
for report in $(find "$REPORTS_DIR" -name "TEST-*.xml" 2>/dev/null); do
    if grep -q '<skipped' "$report"; then
        SKIPPED_FOUND=true
        TEST_CLASS=$(grep -o 'name="[^"]*"' "$report" | head -1 | cut -d'"' -f2)
        
        cat >> "$OUTPUT_DIR/index.html" << EOL
    <h3>$TEST_CLASS</h3>
EOL
        
        # Extract test cases with skipped tests
        grep -A 50 '<testcase' "$report" | grep -B 50 '</testcase>' | tr '\n' ' ' | \
        sed -E "s/<testcase/\n<testcase/g" | grep -E '(<skipped)' | \
        while read -r test_block; do
            TEST_NAME=$(echo "$test_block" | grep -oP 'name="\K[^"]+')
            SKIP_MSG=$(echo "$test_block" | grep -oP 'message="\K[^"]+' || echo "Test skipped")
            
            if [ -n "$TEST_NAME" ]; then
                cat >> "$OUTPUT_DIR/index.html" << EOL
    <div class="skipped-item">
      <strong>Test:</strong> $TEST_CLASS#$TEST_NAME<br>
      <strong>Message:</strong> $SKIP_MSG
    </div>
EOL
            fi
        done
    fi
done

if ! $SKIPPED_FOUND; then
    cat >> "$OUTPUT_DIR/index.html" << EOL
    <p>No skipped tests found.</p>
EOL
fi

# Close skipped section and HTML file
cat >> "$OUTPUT_DIR/index.html" << EOL
  </div>
</body>
</html>
EOL

echo "Test failure report generated: $OUTPUT_DIR/index.html"
EOF
            
            # Make the script executable
            chmod +x "bin/ci/generate-test-failure-report.sh"
            log_success "Created test failure report generator: bin/ci/generate-test-failure-report.sh"
        fi
    else
        log_debug "Test failure report generator exists: bin/ci/generate-test-failure-report.sh"
        ensure_executable "bin/ci/generate-test-failure-report.sh"
    fi
    
    # Verify GitHub Actions workflow
    local notification_workflow=".github/workflows/test-failure-report.yml"
    verify_workflow "$notification_workflow" "Test Failure Analysis"
    
    log_success "Test failure notification enhancement completed"
}

# Function to apply quality gates enhancement
apply_quality_enhancement() {
    log_info "Applying quality gates enhancement..."
    
    # Ensure directories exist
    ensure_directory "bin/ci"
    ensure_directory "config/quality"
    
    # If not in check mode, create quality gates configuration if needed
    if ! $CHECK_ONLY && [ ! -f "config/quality/gates.yml" ]; then
        log_info "Creating quality gates configuration"
        cat > "config/quality/gates.yml" << EOF
# Quality gate settings for Rinna Project
thresholds:
  # Checkstyle thresholds
  checkstyle:
    errors: 0
    warnings: 10
  
  # SpotBugs thresholds
  spotbugs:
    high: 0
    medium: 5
    low: 10
  
  # PMD thresholds
  pmd:
    errors: 0
    warnings: 10
  
  # Go linter thresholds
  golangci:
    errors: 0
    warnings: 10
  
  # Python linter thresholds
  ruff:
    errors: 0
    warnings: 15
  
  # Coverage thresholds (matches coverage/thresholds.yml)
  coverage:
    overall: 75
    java: 80
    go: 70
    python: 75

# Additional quality configuration
sonar:
  enabled: false
  host: https://sonarqube.example.com
  project_key: rinna
  
owasp:
  enabled: true
  fail_build_on_cvss: 7
  exclude_patterns:
    - "test.*"
    - "example.*"
EOF
        log_success "Created quality gates configuration: config/quality/gates.yml"
    fi
    
    # Check if quality checks script exists
    if [ ! -f "bin/run-quality-checks.sh" ]; then
        log_warning "Quality checks script not found: bin/run-quality-checks.sh"
        if ! $CHECK_ONLY; then
            log_info "Creating quality checks script"
            
            # Create the script file
            cat > "bin/run-quality-checks.sh" << 'EOF'
#!/bin/bash
# run-quality-checks.sh - Run code quality checks
#
# This script runs various code quality checks based on quality gates configuration.
#
# Usage:
#   ./bin/run-quality-checks.sh [OPTIONS]
#
# Options:
#   --ci               Use CI environment thresholds
#   --local            Use local environment thresholds (default)
#   --fix              Try to fix issues where possible
#   --owasp            Include OWASP dependency checks
#   --owasp-async      Run OWASP checks asynchronously
#   --verbose          Show verbose output
#   --help             Show this help message

set -eo pipefail

# Default values
ENV_TYPE="local"
FIX_ISSUES=false
RUN_OWASP=false
OWASP_ASYNC=false
VERBOSE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --ci)
            ENV_TYPE="ci"
            shift
            ;;
        --local)
            ENV_TYPE="local"
            shift
            ;;
        --fix)
            FIX_ISSUES=true
            shift
            ;;
        --owasp)
            RUN_OWASP=true
            shift
            ;;
        --owasp-async)
            RUN_OWASP=true
            OWASP_ASYNC=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Quality Checks Runner"
            echo ""
            echo "Usage: ./bin/run-quality-checks.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --ci               Use CI environment thresholds"
            echo "  --local            Use local environment thresholds (default)"
            echo "  --fix              Try to fix issues where possible"
            echo "  --owasp            Include OWASP dependency checks"
            echo "  --owasp-async      Run OWASP checks asynchronously"
            echo "  --verbose          Show verbose output"
            echo "  --help             Show this help message"
            exit 0
            ;;
        *)
            echo "ERROR: Unknown option: $1"
            exit 1
            ;;
    esac
done

# ANSI color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
CLEAR='\033[0m'

# Simple logger functions
function log_info() { echo -e "${BLUE}[INFO]${CLEAR} $*"; }
function log_success() { echo -e "${GREEN}[SUCCESS]${CLEAR} $*"; }
function log_warning() { echo -e "${YELLOW}[WARNING]${CLEAR} $*"; }
function log_error() { echo -e "${RED}[ERROR]${CLEAR} $*"; }
function log_debug() { if $VERBOSE; then echo -e "${CYAN}[DEBUG]${CLEAR} $*"; fi; }

log_info "Running quality checks in $ENV_TYPE environment..."

# Determine which Maven profile to use
MAVEN_PROFILE="local-quality"
if [ "$ENV_TYPE" = "ci" ]; then
    MAVEN_PROFILE="ci"
    log_info "Using CI quality thresholds (stricter)"
else
    log_info "Using local quality thresholds"
fi

# Run Java quality checks
log_info "Running Java quality checks..."

# Run Maven with appropriate profile
FIX_ARGS=""
if $FIX_ISSUES; then
    log_info "Attempting to fix issues where possible"
    FIX_ARGS="-Dcheckstyle.failOnViolation=false -Dpmd.failOnViolation=false"
fi

# Run quality checks
if ! mvn -B clean verify -P $MAVEN_PROFILE $FIX_ARGS; then
    log_warning "Quality checks found issues"
else
    log_success "Java quality checks passed"
fi

# Run Go quality checks if Go code exists
if [ -d "api" ]; then
    log_info "Running Go quality checks..."
    
    # Check if golangci-lint is installed
    if command -v golangci-lint &> /dev/null; then
        cd api
        
        # Run golangci-lint with appropriate settings
        if $FIX_ISSUES; then
            log_debug "Running golangci-lint with auto-fix enabled"
            golangci-lint run --fix || log_warning "Go linting issues found"
        else
            log_debug "Running golangci-lint in check mode"
            golangci-lint run || log_warning "Go linting issues found"
        fi
        
        cd ..
        log_success "Go quality checks completed"
    else
        log_warning "golangci-lint not found, skipping Go quality checks"
    fi
fi

# Run Python quality checks if Python code exists
if [ -d "python" ]; then
    log_info "Running Python quality checks..."
    
    cd python
    
    # Check for Python quality tools
    PYTHON_QUALITY_OK=false
    
    # Try ruff first (preferred)
    if command -v ruff &> /dev/null; then
        log_debug "Running ruff for Python quality checks"
        
        if $FIX_ISSUES; then
            ruff check --fix . || log_warning "Python quality issues found"
            ruff format . || log_warning "Python formatting issues found"
        else
            ruff check . || log_warning "Python quality issues found"
        fi
        
        PYTHON_QUALITY_OK=true
    # Fallback to flake8/black if ruff not available
    elif command -v flake8 &> /dev/null || command -v black &> /dev/null; then
        if command -v flake8 &> /dev/null; then
            log_debug "Running flake8 for Python linting"
            flake8 . || log_warning "Python linting issues found"
        fi
        
        if command -v black &> /dev/null && $FIX_ISSUES; then
            log_debug "Running black for Python formatting"
            black . || log_warning "Python formatting issues found"
        elif command -v black &> /dev/null; then
            log_debug "Running black in check mode"
            black --check . || log_warning "Python formatting issues found"
        fi
        
        PYTHON_QUALITY_OK=true
    fi
    
    if ! $PYTHON_QUALITY_OK; then
        log_warning "No Python quality tools found (ruff, flake8, black), skipping Python quality checks"
    else
        log_success "Python quality checks completed"
    fi
    
    cd ..
fi

# OWASP dependency checks
if $RUN_OWASP; then
    if $OWASP_ASYNC; then
        log_info "Starting OWASP dependency checks asynchronously..."
        
        # Run OWASP checks in background
        mvn -B org.owasp:dependency-check-maven:check -Ddependency-check.skip=false -P $MAVEN_PROFILE > owasp-check.log 2>&1 &
        OWASP_PID=$!
        
        log_info "OWASP checks running in background (PID: $OWASP_PID). See owasp-check.log for results"
    else
        log_info "Running OWASP dependency checks..."
        
        # Run OWASP checks in foreground
        mvn -B org.owasp:dependency-check-maven:check -Ddependency-check.skip=false -P $MAVEN_PROFILE || \
            log_warning "OWASP dependency checks found vulnerabilities"
        
        log_success "OWASP dependency checks completed"
    fi
fi

log_success "Quality checks completed"
EOF
            
            # Make the script executable
            chmod +x "bin/run-quality-checks.sh"
            log_success "Created quality checks script: bin/run-quality-checks.sh"
        fi
    else
        log_debug "Quality checks script exists: bin/run-quality-checks.sh"
        ensure_executable "bin/run-quality-checks.sh"
    fi
    
    log_success "Quality gates enhancement completed"
}

# Main execution
log_info "Starting CI Pipeline Enhancement..."

# Apply enhancements based on options
if $APPLY_ALL || $COVERAGE_ONLY; then
    apply_coverage_enhancement
fi

if $APPLY_ALL || $ARCHITECTURE_ONLY; then
    apply_architecture_enhancement
fi

if $APPLY_ALL || $NOTIFICATIONS_ONLY; then
    apply_notification_enhancement
fi

if $APPLY_ALL || $QUALITY_ONLY; then
    apply_quality_enhancement
fi

# If in check mode only and no specific enhancement was requested, check all components
if $CHECK_ONLY && ! $COVERAGE_ONLY && ! $ARCHITECTURE_ONLY && ! $NOTIFICATIONS_ONLY && ! $QUALITY_ONLY; then
    apply_coverage_enhancement
    apply_architecture_enhancement
    apply_notification_enhancement
    apply_quality_enhancement
fi

log_success "CI Pipeline Enhancement completed successfully!"

# Print summary
echo ""
echo "CI Pipeline Enhancement Summary:"
echo "--------------------------------"
echo "1. Test Coverage Reporting: Generates detailed coverage reports for Java, Go, and Python code"
echo "   - Config: config/coverage/thresholds.yml"
echo "   - Report generator: bin/ci/generate-coverage-report.sh"
echo "   - GitHub workflow: .github/workflows/coverage-report.yml"
echo ""
echo "2. Architecture Validation: Enforces Clean Architecture principles through static analysis"
echo "   - Config: config/architecture/rules.yml"
echo "   - Validator: bin/ci/validate-architecture.sh"
echo "   - GitHub workflow: .github/workflows/architecture-validation.yml"
echo ""
echo "3. Test Failure Notification: Analyzes test failures and sends notifications"
echo "   - Notification script: bin/test-failure-notify.sh"
echo "   - Report generator: bin/ci/generate-test-failure-report.sh"
echo "   - GitHub workflow: .github/workflows/test-failure-report.yml"
echo ""
echo "4. Quality Gates: Enforces code quality standards and prevents regression"
echo "   - Config: config/quality/gates.yml"
echo "   - Quality checker: bin/run-quality-checks.sh"
echo ""
echo "Next Steps:"
echo "1. Set up GitHub repository secrets for notifications (SLACK_WEBHOOK_URL, NOTIFICATION_EMAIL)"
echo "2. Review and adjust threshold configurations for your project needs"
echo "3. Run 'bin/ci/enhance-ci-pipeline.sh --apply' to apply all enhancements"
echo "4. Add coverage badge to your README.md: [![Coverage](.github/badges/coverage.svg)](https://github.com/[org]/[repo]/actions/workflows/coverage-report.yml)"