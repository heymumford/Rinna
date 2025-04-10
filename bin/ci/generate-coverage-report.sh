#!/usr/bin/env bash
#
# generate-coverage-report.sh - Generate unified test coverage reports
#
# PURPOSE: This script consolidates coverage data from multiple languages 
#          (Java, Go, Python) into a comprehensive report for CI/CD pipelines.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -eo pipefail

# Determine script and project directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_DIR="$PROJECT_ROOT/config"
TARGET_DIR="$PROJECT_ROOT/target"
COVERAGE_DIR="$TARGET_DIR/coverage"
REPORT_DIR="$COVERAGE_DIR/html"
BADGES_DIR="$COVERAGE_DIR/badges"

# Import common utilities if available
if [[ -f "$PROJECT_ROOT/bin/common/rinna_utils.sh" ]]; then
  source "$PROJECT_ROOT/bin/common/rinna_utils.sh"
fi

# Source logging utilities if available
if [[ -f "$PROJECT_ROOT/bin/common/rinna_logger.sh" ]]; then
  source "$PROJECT_ROOT/bin/common/rinna_logger.sh"
fi

# Default configuration values
OUTPUT_FORMAT="html"
VERBOSE=false
JAVA_COVERAGE_THRESHOLD=70
GO_COVERAGE_THRESHOLD=60
PYTHON_THRESHOLD=70
OVERALL_THRESHOLD=70
BADGE_COLOR_SUCCESS="brightgreen"
BADGE_COLOR_WARNING="yellow"
BADGE_COLOR_FAILURE="red"
INCLUDE_BADGES=true
GITHUB_COMMENTS=false
PR_COMMENT=false
CI_MODE=false
GITHUB_PR_NUMBER=""
GITHUB_REPOSITORY=""
GITHUB_TOKEN=""
COVERAGE_BADGE_PATH="badge-overall.svg"
BADGE_STYLE="flat"
CONFIG_FILE="$CONFIG_DIR/ci/coverage-config.json"

# Colors and formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Display help text
show_help() {
  cat <<EOF
${BOLD}${BLUE}generate-coverage-report.sh${NC} - Generate unified test coverage reports

${BOLD}USAGE:${NC}
  $0 [options]

${BOLD}OPTIONS:${NC}
  -h, --help            Show this help message
  -v, --verbose         Enable verbose output
  -f, --format FORMAT   Output format: html, json, xml, markdown (default: html)
  -o, --output DIR      Output directory (default: $REPORT_DIR)
  -c, --config FILE     Configuration file (default: $CONFIG_FILE)
  --ci                  Run in CI mode (use preset paths and generate PR comments)
  --badge               Generate badge image
  --pr-comment          Generate PR comment markdown file
  --java-threshold N    Java coverage threshold percentage (default: $JAVA_COVERAGE_THRESHOLD)
  --go-threshold N      Go coverage threshold percentage (default: $GO_COVERAGE_THRESHOLD)
  --python-threshold N  Python coverage threshold percentage (default: $PYTHON_THRESHOLD)
  --overall-threshold N Overall coverage threshold percentage (default: $OVERALL_THRESHOLD)
  --no-badges           Disable generation of coverage badges
  --badge-style STYLE   Badge style: flat, flat-square, plastic, social (default: flat)
  --badge-path PATH     Path to save the coverage badge (default: $COVERAGE_BADGE_PATH)
  --github-comment      Add a comment to GitHub PR with coverage results
  --pr-number NUMBER    GitHub PR number for commenting (for GitHub Actions)
  --repository REPO     GitHub repository name (format: owner/repo)
  --github-token TOKEN  GitHub token for API access

${BOLD}EXAMPLES:${NC}
  # Generate HTML report with default settings
  $0

  # Generate JSON report with verbose output
  $0 -v -f json

  # Generate report with custom thresholds
  $0 --java-threshold 80 --go-threshold 70 --python-threshold 75 --overall-threshold 75

  # Generate report for CI environment with PR comments
  $0 --ci --pr-comment --badge

${BOLD}DESCRIPTION:${NC}
  This script collects and consolidates coverage data from multiple language sources
  (Java, Go, Python) and produces a unified report. It can generate badges for inclusion
  in README files and can post comments to GitHub PRs with coverage information.

  It looks for coverage data in these locations:
  - Java: target/site/jacoco/jacoco.xml
  - Go: api/coverage.out
  - Python: python/.coverage

  The script can be configured via command line arguments or a JSON configuration file.
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      show_help
      exit 0
      ;;
    -v|--verbose)
      VERBOSE=true
      shift
      ;;
    -f|--format)
      OUTPUT_FORMAT="$2"
      shift 2
      ;;
    --format=*)
      OUTPUT_FORMAT="${1#*=}"
      shift
      ;;
    -o|--output)
      REPORT_DIR="$2"
      shift 2
      ;;
    --output=*)
      REPORT_DIR="${1#*=}"
      shift
      ;;
    -c|--config)
      CONFIG_FILE="$2"
      shift 2
      ;;
    --config=*)
      CONFIG_FILE="${1#*=}"
      shift
      ;;
    --ci)
      CI_MODE=true
      GITHUB_COMMENTS=true
      PR_COMMENT=true
      INCLUDE_BADGES=true
      shift
      ;;
    --badge)
      INCLUDE_BADGES=true
      shift
      ;;
    --pr-comment)
      PR_COMMENT=true
      shift
      ;;
    --java-threshold)
      JAVA_COVERAGE_THRESHOLD="$2"
      shift 2
      ;;
    --java-threshold=*)
      JAVA_COVERAGE_THRESHOLD="${1#*=}"
      shift
      ;;
    --go-threshold)
      GO_COVERAGE_THRESHOLD="$2"
      shift 2
      ;;
    --go-threshold=*)
      GO_COVERAGE_THRESHOLD="${1#*=}"
      shift
      ;;
    --python-threshold)
      PYTHON_THRESHOLD="$2"
      shift 2
      ;;
    --python-threshold=*)
      PYTHON_THRESHOLD="${1#*=}"
      shift
      ;;
    --overall-threshold)
      OVERALL_THRESHOLD="$2"
      shift 2
      ;;
    --overall-threshold=*)
      OVERALL_THRESHOLD="${1#*=}"
      shift
      ;;
    --no-badges)
      INCLUDE_BADGES=false
      shift
      ;;
    --badge-style)
      BADGE_STYLE="$2"
      shift 2
      ;;
    --badge-style=*)
      BADGE_STYLE="${1#*=}"
      shift
      ;;
    --badge-path)
      COVERAGE_BADGE_PATH="$2"
      shift 2
      ;;
    --badge-path=*)
      COVERAGE_BADGE_PATH="${1#*=}"
      shift
      ;;
    --github-comment)
      GITHUB_COMMENTS=true
      shift
      ;;
    --pr-number)
      GITHUB_PR_NUMBER="$2"
      shift 2
      ;;
    --pr-number=*)
      GITHUB_PR_NUMBER="${1#*=}"
      shift
      ;;
    --repository)
      GITHUB_REPOSITORY="$2"
      shift 2
      ;;
    --repository=*)
      GITHUB_REPOSITORY="${1#*=}"
      shift
      ;;
    --github-token)
      GITHUB_TOKEN="$2"
      shift 2
      ;;
    --github-token=*)
      GITHUB_TOKEN="${1#*=}"
      shift
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}" >&2
      show_help
      exit 1
      ;;
  esac
done

# Load configuration from file if available
if [[ -f "$CONFIG_FILE" ]]; then
  if which jq > /dev/null 2>&1; then
    if [[ "$VERBOSE" == "true" ]]; then
      echo -e "${BLUE}Loading configuration from ${CONFIG_FILE}${NC}"
    fi
    
    # Extract values from JSON config using jq
    if [[ -n "$(jq -r '.output_format // empty' "$CONFIG_FILE")" ]]; then
      OUTPUT_FORMAT=$(jq -r '.output_format' "$CONFIG_FILE")
    fi
    
    if [[ -n "$(jq -r '.report_dir // empty' "$CONFIG_FILE")" ]]; then
      REPORT_DIR=$(jq -r '.report_dir' "$CONFIG_FILE")
    fi
    
    if [[ -n "$(jq -r '.thresholds.java // empty' "$CONFIG_FILE")" ]]; then
      JAVA_COVERAGE_THRESHOLD=$(jq -r '.thresholds.java' "$CONFIG_FILE")
    fi
    
    if [[ -n "$(jq -r '.thresholds.go // empty' "$CONFIG_FILE")" ]]; then
      GO_COVERAGE_THRESHOLD=$(jq -r '.thresholds.go' "$CONFIG_FILE")
    fi
    
    if [[ -n "$(jq -r '.thresholds.python // empty' "$CONFIG_FILE")" ]]; then
      PYTHON_THRESHOLD=$(jq -r '.thresholds.python' "$CONFIG_FILE")
    fi
    
    if [[ -n "$(jq -r '.thresholds.overall // empty' "$CONFIG_FILE")" ]]; then
      OVERALL_THRESHOLD=$(jq -r '.thresholds.overall' "$CONFIG_FILE")
    fi
    
    if [[ -n "$(jq -r '.include_badges // empty' "$CONFIG_FILE")" ]]; then
      INCLUDE_BADGES=$(jq -r '.include_badges' "$CONFIG_FILE")
    fi
    
    if [[ -n "$(jq -r '.badge_style // empty' "$CONFIG_FILE")" ]]; then
      BADGE_STYLE=$(jq -r '.badge_style' "$CONFIG_FILE")
    fi
    
    if [[ -n "$(jq -r '.coverage_badge_path // empty' "$CONFIG_FILE")" ]]; then
      COVERAGE_BADGE_PATH=$(jq -r '.coverage_badge_path' "$CONFIG_FILE")
    fi
    
    if [[ -n "$(jq -r '.github_comments // empty' "$CONFIG_FILE")" ]]; then
      GITHUB_COMMENTS=$(jq -r '.github_comments' "$CONFIG_FILE")
    fi
  else
    echo -e "${YELLOW}Warning: jq not found. Skipping config file parsing.${NC}" >&2
  fi
fi

# Create output directories
mkdir -p "$COVERAGE_DIR" "$REPORT_DIR" "$BADGES_DIR"

# Log function
log() {
  local level="$1"
  local message="$2"
  
  case "$level" in
    debug)
      if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${CYAN}[DEBUG] $message${NC}" >&2
      fi
      ;;
    info)
      echo -e "${BLUE}[INFO] $message${NC}" >&2
      ;;
    warning)
      echo -e "${YELLOW}[WARNING] $message${NC}" >&2
      ;;
    error)
      echo -e "${RED}[ERROR] $message${NC}" >&2
      ;;
    success)
      echo -e "${GREEN}[SUCCESS] $message${NC}" >&2
      ;;
  esac
}

# Check requirements
check_requirements() {
  log debug "Checking requirements"
  
  if ! command -v xmlstarlet &> /dev/null; then
    log warning "XMLStarlet not found. XML parsing capabilities will be limited."
  fi
  
  if ! command -v jq &> /dev/null; then
    log warning "jq not found. JSON processing capabilities will be limited."
  fi
  
  if ! command -v bc &> /dev/null; then
    log warning "bc not found. Calculation capabilities will be limited."
  fi
  
  if [[ "$GITHUB_COMMENTS" == "true" && "$CI_MODE" != "true" ]]; then
    if ! command -v curl &> /dev/null; then
      log error "curl not found but required for GitHub integration"
      return 1
    fi
    
    if [[ -z "$GITHUB_TOKEN" || -z "$GITHUB_REPOSITORY" ]]; then
      log error "GitHub token and repository are required for GitHub integration"
      return 1
    fi
    
    if [[ "$PR_COMMENT" == "true" && -z "$GITHUB_PR_NUMBER" ]]; then
      log error "PR number is required for GitHub PR comments"
      return 1
    fi
  fi
  
  return 0
}

# Process JaCoCo coverage data
process_java_coverage() {
  local jacoco_file="$PROJECT_ROOT/target/site/jacoco/jacoco.xml"
  local module_reports=($(find "$PROJECT_ROOT" -path "*/target/site/jacoco/jacoco.xml"))
  local java_lines_covered=0
  local java_lines_total=0
  local java_coverage=0
  
  log info "Processing Java coverage data"
  
  # Check if main JaCoCo report exists
  if [[ -f "$jacoco_file" ]]; then
    log debug "Found main JaCoCo report: $jacoco_file"
    module_reports=("$jacoco_file")
  elif [[ ${#module_reports[@]} -eq 0 ]]; then
    log warning "No JaCoCo reports found"
    return 1
  else
    log debug "Found ${#module_reports[@]} module JaCoCo reports"
  fi
  
  # Process each JaCoCo report
  for report in "${module_reports[@]}"; do
    log debug "Processing $report"
    
    if [[ ! -f "$report" ]]; then
      log warning "JaCoCo report not found: $report"
      continue
    fi
    
    if command -v xmlstarlet &> /dev/null; then
      # Use XMLStarlet for more accurate parsing
      local covered=$(xmlstarlet sel -t -v "sum(//counter[@type='LINE']/@covered)" "$report")
      local missed=$(xmlstarlet sel -t -v "sum(//counter[@type='LINE']/@missed)" "$report")
      
      java_lines_covered=$((java_lines_covered + covered))
      java_lines_total=$((java_lines_total + covered + missed))
    else
      # Fallback to grep/sed if XMLStarlet is not available
      local counter_line=$(grep -A1 "<counter type=\"LINE\"" "$report" | tail -n1)
      local covered=$(echo "$counter_line" | sed -E 's/.*covered="([0-9]+)".*/\1/')
      local missed=$(echo "$counter_line" | sed -E 's/.*missed="([0-9]+)".*/\1/')
      
      if [[ -n "$covered" && -n "$missed" ]]; then
        java_lines_covered=$((java_lines_covered + covered))
        java_lines_total=$((java_lines_total + covered + missed))
      fi
    fi
  done
  
  # Calculate coverage percentage
  if [[ $java_lines_total -gt 0 ]]; then
    if command -v bc &> /dev/null; then
      java_coverage=$(echo "scale=1; ($java_lines_covered * 100) / $java_lines_total" | bc)
    else
      java_coverage=$(( (java_lines_covered * 100) / java_lines_total ))
    fi
  fi
  
  log debug "Java lines covered: $java_lines_covered"
  log debug "Java lines total: $java_lines_total"
  log debug "Java coverage: $java_coverage%"
  
  # Write results to file
  echo "$java_coverage" > "$COVERAGE_DIR/java-coverage.txt"
  
  # Check against threshold
  if command -v bc &> /dev/null; then
    if [[ $(echo "$java_coverage >= $JAVA_COVERAGE_THRESHOLD" | bc) -eq 1 ]]; then
      log success "Java coverage: $java_coverage% (threshold: $JAVA_COVERAGE_THRESHOLD%)"
      JAVA_STATUS="success"
    else
      log warning "Java coverage: $java_coverage% (below threshold: $JAVA_COVERAGE_THRESHOLD%)"
      JAVA_STATUS="failure"
    fi
  else
    if [[ ${java_coverage%.*} -ge $JAVA_COVERAGE_THRESHOLD ]]; then
      log success "Java coverage: $java_coverage% (threshold: $JAVA_COVERAGE_THRESHOLD%)"
      JAVA_STATUS="success"
    else
      log warning "Java coverage: $java_coverage% (below threshold: $JAVA_COVERAGE_THRESHOLD%)"
      JAVA_STATUS="failure"
    fi
  fi
  
  # Return values
  JAVA_LINES_COVERED=$java_lines_covered
  JAVA_LINES_TOTAL=$java_lines_total
  JAVA_COVERAGE=$java_coverage
}

# Process Go coverage data
process_go_coverage() {
  local go_coverage_file="$PROJECT_ROOT/api/coverage.out"
  local go_coverage=0
  local go_lines_covered=0
  local go_lines_total=0
  
  log info "Processing Go coverage data"
  
  # Check if Go coverage file exists
  if [[ ! -f "$go_coverage_file" ]]; then
    local alt_file=$(find "$PROJECT_ROOT" -name "*.out" -path "*/api/*" | head -n1)
    if [[ -n "$alt_file" ]]; then
      go_coverage_file="$alt_file"
      log debug "Using alternative Go coverage file: $go_coverage_file"
    else
      log warning "No Go coverage data found"
      return 1
    fi
  fi
  
  # Process Go coverage data
  if command -v go &> /dev/null; then
    # Use go tool to calculate coverage
    local coverage_output=$(go tool cover -func="$go_coverage_file" | grep "total:" | awk '{print $3}')
    go_coverage=${coverage_output//%/}
    
    # Extract lines covered/total - this is approximate
    local coverage_lines=$(go tool cover -func="$go_coverage_file" | grep -v "total:")
    go_lines_total=$(echo "$coverage_lines" | wc -l)
    go_lines_covered=$(echo "$coverage_lines" | grep -v "0.0%" | wc -l)
  else
    # Fallback if go tool is not available
    log warning "Go tool not found. Using simplified coverage calculation."
    
    # Simple coverage calculation
    local covered_lines=$(grep -v "^mode:" "$go_coverage_file" | grep -v " 0$" | wc -l)
    local total_lines=$(grep -v "^mode:" "$go_coverage_file" | wc -l)
    
    if [[ $total_lines -gt 0 ]]; then
      go_coverage=$(( (covered_lines * 100) / total_lines ))
      go_lines_covered=$covered_lines
      go_lines_total=$total_lines
    fi
  fi
  
  log debug "Go lines covered: $go_lines_covered"
  log debug "Go lines total: $go_lines_total"
  log debug "Go coverage: $go_coverage%"
  
  # Write results to file
  echo "$go_coverage" > "$COVERAGE_DIR/go-coverage.txt"
  
  # Check against threshold
  if command -v bc &> /dev/null; then
    if [[ $(echo "$go_coverage >= $GO_COVERAGE_THRESHOLD" | bc) -eq 1 ]]; then
      log success "Go coverage: $go_coverage% (threshold: $GO_COVERAGE_THRESHOLD%)"
      GO_STATUS="success"
    else
      log warning "Go coverage: $go_coverage% (below threshold: $GO_COVERAGE_THRESHOLD%)"
      GO_STATUS="failure"
    fi
  else
    if [[ ${go_coverage%.*} -ge $GO_COVERAGE_THRESHOLD ]]; then
      log success "Go coverage: $go_coverage% (threshold: $GO_COVERAGE_THRESHOLD%)"
      GO_STATUS="success"
    else
      log warning "Go coverage: $go_coverage% (below threshold: $GO_COVERAGE_THRESHOLD%)"
      GO_STATUS="failure"
    fi
  fi
  
  # Return values
  GO_LINES_COVERED=$go_lines_covered
  GO_LINES_TOTAL=$go_lines_total
  GO_COVERAGE=$go_coverage
}

# Process Python coverage data
process_python_coverage() {
  local python_coverage_file="$PROJECT_ROOT/python/.coverage"
  local python_coverage_xml="$COVERAGE_DIR/python-coverage.xml"
  local python_coverage=0
  local python_lines_covered=0
  local python_lines_total=0
  
  log info "Processing Python coverage data"
  
  # Check if Python coverage file exists
  if [[ ! -f "$python_coverage_file" && ! -f "$python_coverage_xml" ]]; then
    local alt_file=$(find "$PROJECT_ROOT" -name ".coverage" -o -name "coverage.xml" | head -n1)
    if [[ -n "$alt_file" ]]; then
      if [[ "$alt_file" == *.xml ]]; then
        python_coverage_xml="$alt_file"
      else
        python_coverage_file="$alt_file"
      fi
      log debug "Using alternative Python coverage file: $alt_file"
    else
      log warning "No Python coverage data found"
      return 1
    fi
  fi
  
  # If we have .coverage file but no XML, try to generate XML
  if [[ -f "$python_coverage_file" && ! -f "$python_coverage_xml" ]]; then
    if command -v python &> /dev/null; then
      cd "$(dirname "$python_coverage_file")"
      python -m coverage xml -o "$python_coverage_xml" || true
      cd "$PROJECT_ROOT"
    fi
  fi
  
  # Process Python coverage data from XML
  if [[ -f "$python_coverage_xml" ]]; then
    if command -v xmlstarlet &> /dev/null; then
      # Use XMLStarlet for more accurate parsing
      python_lines_covered=$(xmlstarlet sel -t -v "sum(//class/@hits)" "$python_coverage_xml")
      local lines_missed=$(xmlstarlet sel -t -v "sum(//class/@misses)" "$python_coverage_xml")
      python_lines_total=$((python_lines_covered + lines_missed))
      
      if [[ $python_lines_total -gt 0 ]]; then
        if command -v bc &> /dev/null; then
          python_coverage=$(echo "scale=1; ($python_lines_covered * 100) / $python_lines_total" | bc)
        else
          python_coverage=$(( (python_lines_covered * 100) / python_lines_total ))
        fi
      fi
    else
      # Fallback to grep/sed if XMLStarlet is not available
      local line_rate=$(grep "line-rate=" "$python_coverage_xml" | head -n1 | sed -E 's/.*line-rate="([0-9.]+)".*/\1/')
      if [[ -n "$line_rate" ]]; then
        if command -v bc &> /dev/null; then
          python_coverage=$(echo "scale=1; $line_rate * 100" | bc)
        else
          python_coverage=$(( $line_rate * 100 ))
        fi
        
        # Estimate lines covered/total
        local lines_valid=$(grep "lines-valid=" "$python_coverage_xml" | head -n1 | sed -E 's/.*lines-valid="([0-9]+)".*/\1/')
        if [[ -n "$lines_valid" ]]; then
          python_lines_total=$lines_valid
          python_lines_covered=$(echo "scale=0; $line_rate * $lines_valid / 1" | bc)
        fi
      fi
    fi
  # Fallback to direct usage of coverage tool
  elif [[ -f "$python_coverage_file" ]] && command -v python &> /dev/null; then
    cd "$(dirname "$python_coverage_file")"
    local coverage_output=$(python -m coverage report | tail -n 1)
    python_coverage=$(echo "$coverage_output" | awk '{print $NF}' | tr -d '%')
    
    # Extract lines covered/total
    python_lines_total=$(echo "$coverage_output" | awk '{print $(NF-2)}')
    python_lines_covered=$(echo "$coverage_output" | awk '{print $(NF-3)}')
    
    cd "$PROJECT_ROOT"
  fi
  
  log debug "Python lines covered: $python_lines_covered"
  log debug "Python lines total: $python_lines_total"
  log debug "Python coverage: $python_coverage%"
  
  # Write results to file
  echo "$python_coverage" > "$COVERAGE_DIR/python-coverage.txt"
  
  # Check against threshold
  if command -v bc &> /dev/null; then
    if [[ $(echo "$python_coverage >= $PYTHON_THRESHOLD" | bc) -eq 1 ]]; then
      log success "Python coverage: $python_coverage% (threshold: $PYTHON_THRESHOLD%)"
      PYTHON_STATUS="success"
    else
      log warning "Python coverage: $python_coverage% (below threshold: $PYTHON_THRESHOLD%)"
      PYTHON_STATUS="failure"
    fi
  else
    if [[ ${python_coverage%.*} -ge $PYTHON_THRESHOLD ]]; then
      log success "Python coverage: $python_coverage% (threshold: $PYTHON_THRESHOLD%)"
      PYTHON_STATUS="success"
    else
      log warning "Python coverage: $python_coverage% (below threshold: $PYTHON_THRESHOLD%)"
      PYTHON_STATUS="failure"
    fi
  fi
  
  # Return values
  PYTHON_LINES_COVERED=$python_lines_covered
  PYTHON_LINES_TOTAL=$python_lines_total
  PYTHON_COVERAGE=$python_coverage
}

# Calculate overall coverage
calculate_overall_coverage() {
  log info "Calculating overall coverage"
  
  # Default values for each language
  JAVA_LINES_COVERED=${JAVA_LINES_COVERED:-0}
  JAVA_LINES_TOTAL=${JAVA_LINES_TOTAL:-0}
  JAVA_COVERAGE=${JAVA_COVERAGE:-0}
  JAVA_STATUS=${JAVA_STATUS:-"failure"}
  
  GO_LINES_COVERED=${GO_LINES_COVERED:-0}
  GO_LINES_TOTAL=${GO_LINES_TOTAL:-0}
  GO_COVERAGE=${GO_COVERAGE:-0}
  GO_STATUS=${GO_STATUS:-"failure"}
  
  PYTHON_LINES_COVERED=${PYTHON_LINES_COVERED:-0}
  PYTHON_LINES_TOTAL=${PYTHON_LINES_TOTAL:-0}
  PYTHON_COVERAGE=${PYTHON_COVERAGE:-0}
  PYTHON_STATUS=${PYTHON_STATUS:-"failure"}
  
  # Calculate weighted overall coverage
  local total_lines=$((JAVA_LINES_TOTAL + GO_LINES_TOTAL + PYTHON_LINES_TOTAL))
  local total_covered=$((JAVA_LINES_COVERED + GO_LINES_COVERED + PYTHON_LINES_COVERED))
  
  if [[ $total_lines -gt 0 ]]; then
    if command -v bc &> /dev/null; then
      OVERALL_COVERAGE=$(echo "scale=1; ($total_covered * 100) / $total_lines" | bc)
    else
      OVERALL_COVERAGE=$(( (total_covered * 100) / total_lines ))
    fi
  else
    # Fallback to average if no lines data
    local language_count=0
    local coverage_sum=0
    
    if [[ $JAVA_COVERAGE != "0" ]]; then
      coverage_sum=$(echo "$coverage_sum + $JAVA_COVERAGE" | bc)
      language_count=$((language_count + 1))
    fi
    
    if [[ $GO_COVERAGE != "0" ]]; then
      coverage_sum=$(echo "$coverage_sum + $GO_COVERAGE" | bc)
      language_count=$((language_count + 1))
    fi
    
    if [[ $PYTHON_COVERAGE != "0" ]]; then
      coverage_sum=$(echo "$coverage_sum + $PYTHON_COVERAGE" | bc)
      language_count=$((language_count + 1))
    fi
    
    if [[ $language_count -gt 0 ]]; then
      if command -v bc &> /dev/null; then
        OVERALL_COVERAGE=$(echo "scale=1; $coverage_sum / $language_count" | bc)
      else
        OVERALL_COVERAGE=$((coverage_sum / language_count))
      fi
    else
      OVERALL_COVERAGE=0
    fi
  fi
  
  log debug "Overall coverage: $OVERALL_COVERAGE% (threshold: $OVERALL_THRESHOLD%)"
  
  # Write results to file
  echo "$OVERALL_COVERAGE" > "$COVERAGE_DIR/overall-coverage.txt"
  
  # Check against threshold
  if command -v bc &> /dev/null; then
    if [[ $(echo "$OVERALL_COVERAGE >= $OVERALL_THRESHOLD" | bc) -eq 1 ]]; then
      log success "Overall coverage: $OVERALL_COVERAGE% meets threshold of $OVERALL_THRESHOLD%"
      OVERALL_STATUS="success"
    else
      log warning "Overall coverage: $OVERALL_COVERAGE% is below threshold of $OVERALL_THRESHOLD%"
      OVERALL_STATUS="failure"
    fi
  else
    if [[ ${OVERALL_COVERAGE%.*} -ge $OVERALL_THRESHOLD ]]; then
      log success "Overall coverage: $OVERALL_COVERAGE% meets threshold of $OVERALL_THRESHOLD%"
      OVERALL_STATUS="success"
    else
      log warning "Overall coverage: $OVERALL_COVERAGE% is below threshold of $OVERALL_THRESHOLD%"
      OVERALL_STATUS="failure"
    fi
  fi
  
  # Save total counts for later use
  TOTAL_COVERED=$total_covered
  TOTAL_LINES=$total_lines
}

# Generate coverage badge
generate_badge() {
  local badge_dir="$BADGES_DIR"
  mkdir -p "$badge_dir"
  
  log info "Generating coverage badges"
  
  # Generate badges for each language and overall
  generate_single_badge "overall" "$OVERALL_COVERAGE" "$OVERALL_THRESHOLD" "$OVERALL_STATUS" "$badge_dir/badge-overall.svg"
  generate_single_badge "java" "$JAVA_COVERAGE" "$JAVA_COVERAGE_THRESHOLD" "$JAVA_STATUS" "$badge_dir/badge-java.svg"
  generate_single_badge "go" "$GO_COVERAGE" "$GO_COVERAGE_THRESHOLD" "$GO_STATUS" "$badge_dir/badge-go.svg"
  generate_single_badge "python" "$PYTHON_COVERAGE" "$PYTHON_THRESHOLD" "$PYTHON_STATUS" "$badge_dir/badge-python.svg"
  
  # Generate Markdown reference for easy embedding
  cat > "$COVERAGE_DIR/badge-markdown.txt" << EOF
![Overall Coverage](badges/badge-overall.svg)
![Java Coverage](badges/badge-java.svg)
![Go Coverage](badges/badge-go.svg)
![Python Coverage](badges/badge-python.svg)
EOF
  
  log success "Coverage badges generated in $badge_dir"
}

# Helper function to generate a single badge
generate_single_badge() {
  local name="$1"
  local coverage="$2"
  local threshold="$3"
  local status="$4"
  local output_path="$5"
  
  # Determine badge color based on coverage level
  local badge_color="$BADGE_COLOR_FAILURE"
  
  if [[ "$status" == "success" ]]; then
    badge_color="$BADGE_COLOR_SUCCESS"
  elif command -v bc &> /dev/null; then
    if [[ $(echo "$coverage >= ($threshold * 0.8)" | bc) -eq 1 ]]; then
      badge_color="$BADGE_COLOR_WARNING"
    fi
  else
    if [[ ${coverage%.*} -ge $(($threshold * 8 / 10)) ]]; then
      badge_color="$BADGE_COLOR_WARNING"
    fi
  fi
  
  # Create badge using shields.io
  local badge_url="https://img.shields.io/badge/${name}-${coverage}%25-${badge_color}?style=${BADGE_STYLE}"
  
  if command -v curl &> /dev/null; then
    curl -s "$badge_url" -o "$output_path"
    log debug "Coverage badge for $name saved to $output_path"
  else
    log warning "curl not found. Cannot generate coverage badge for $name."
    return 1
  fi
}

# Generate HTML report
generate_html_report() {
  log info "Generating HTML coverage report"
  
  # Create index.html for the report
  cat > "$REPORT_DIR/index.html" <<EOF
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Rinna Test Coverage Report</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, "Open Sans", "Helvetica Neue", sans-serif;
      line-height: 1.6;
      color: #333;
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
    }
    h1, h2, h3 {
      color: #444;
    }
    .header {
      margin-bottom: 20px;
      border-bottom: 1px solid #eee;
      padding-bottom: 10px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .header h1 {
      margin: 0;
    }
    .badge {
      padding-left: 20px;
    }
    .summary {
      display: flex;
      flex-wrap: wrap;
      gap: 20px;
      margin-bottom: 30px;
    }
    .card {
      flex: 1;
      min-width: 220px;
      border: 1px solid #ddd;
      border-radius: 5px;
      padding: 15px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .card h3 {
      margin-top: 0;
      border-bottom: 1px solid #eee;
      padding-bottom: 10px;
    }
    .meter {
      height: 20px;
      background: #f3f3f3;
      border-radius: 10px;
      position: relative;
      margin: 10px 0;
    }
    .meter-fill {
      height: 100%;
      border-radius: 10px;
      transition: width 0.5s ease-in-out;
    }
    .success { background-color: #4caf50; }
    .warning { background-color: #ff9800; }
    .failure { background-color: #f44336; }
    .details {
      margin-top: 30px;
    }
    .language-details {
      margin-top: 20px;
      border: 1px solid #ddd;
      border-radius: 5px;
      padding: 15px;
    }
    .language-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid #eee;
      padding-bottom: 10px;
      margin-bottom: 15px;
    }
    .language-title h3 {
      margin: 0;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 15px;
    }
    th, td {
      padding: 10px;
      text-align: left;
      border-bottom: 1px solid #ddd;
    }
    th {
      background-color: #f5f5f5;
    }
    tr:hover {
      background-color: #f9f9f9;
    }
    footer {
      margin-top: 30px;
      text-align: center;
      font-size: 0.8em;
      color: #777;
      border-top: 1px solid #eee;
      padding-top: 10px;
    }
    @media (max-width: 600px) {
      .summary {
        flex-direction: column;
      }
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>Rinna Test Coverage Report</h1>
    <div class="badge">
      <img src="../badges/badge-overall.svg" alt="Coverage"/>
    </div>
  </div>
  
  <div class="summary">
    <div class="card">
      <h3>Overall Coverage</h3>
      <div class="meter">
        <div class="meter-fill ${OVERALL_STATUS}" style="width: ${OVERALL_COVERAGE}%;"></div>
      </div>
      <p><strong>${OVERALL_COVERAGE}%</strong> (threshold: ${OVERALL_THRESHOLD}%)</p>
      <p>Lines: ${TOTAL_COVERED} / ${TOTAL_LINES}</p>
    </div>
    
    <div class="card">
      <h3>Java Coverage</h3>
      <div class="meter">
        <div class="meter-fill ${JAVA_STATUS}" style="width: ${JAVA_COVERAGE}%;"></div>
      </div>
      <p><strong>${JAVA_COVERAGE}%</strong> (threshold: ${JAVA_COVERAGE_THRESHOLD}%)</p>
      <p>Lines: ${JAVA_LINES_COVERED} / ${JAVA_LINES_TOTAL}</p>
    </div>
    
    <div class="card">
      <h3>Go Coverage</h3>
      <div class="meter">
        <div class="meter-fill ${GO_STATUS}" style="width: ${GO_COVERAGE}%;"></div>
      </div>
      <p><strong>${GO_COVERAGE}%</strong> (threshold: ${GO_COVERAGE_THRESHOLD}%)</p>
      <p>Lines: ${GO_LINES_COVERED} / ${GO_LINES_TOTAL}</p>
    </div>
    
    <div class="card">
      <h3>Python Coverage</h3>
      <div class="meter">
        <div class="meter-fill ${PYTHON_STATUS}" style="width: ${PYTHON_COVERAGE}%;"></div>
      </div>
      <p><strong>${PYTHON_COVERAGE}%</strong> (threshold: ${PYTHON_THRESHOLD}%)</p>
      <p>Lines: ${PYTHON_LINES_COVERED} / ${PYTHON_LINES_TOTAL}</p>
    </div>
  </div>
  
  <div class="details">
    <h2>Detailed Coverage Report</h2>
    
    <div class="language-details">
      <div class="language-title">
        <h3>Java Coverage Details</h3>
        <img src="../badges/badge-java.svg" alt="Java Coverage"/>
      </div>
      <p>Java coverage data is generated using JaCoCo during the Maven build process.</p>
      <p><strong>Status:</strong> ${JAVA_STATUS == "success" ? "✅ Passed" : "❌ Failed"}</p>
      <p>For detailed Java coverage information, see:</p>
      <ul>
        <li><a href="../../site/jacoco/index.html">JaCoCo HTML Report</a></li>
      </ul>
    </div>
    
    <div class="language-details">
      <div class="language-title">
        <h3>Go Coverage Details</h3>
        <img src="../badges/badge-go.svg" alt="Go Coverage"/>
      </div>
      <p>Go coverage data is generated using the Go built-in test coverage tools.</p>
      <p><strong>Status:</strong> ${GO_STATUS == "success" ? "✅ Passed" : "❌ Failed"}</p>
      <p>For detailed Go coverage information, see:</p>
      <ul>
        <li><a href="../../go/coverage.html">Go Coverage HTML Report</a></li>
      </ul>
    </div>
    
    <div class="language-details">
      <div class="language-title">
        <h3>Python Coverage Details</h3>
        <img src="../badges/badge-python.svg" alt="Python Coverage"/>
      </div>
      <p>Python coverage data is generated using the pytest-cov plugin.</p>
      <p><strong>Status:</strong> ${PYTHON_STATUS == "success" ? "✅ Passed" : "❌ Failed"}</p>
      <p>For detailed Python coverage information, see:</p>
      <ul>
        <li><a href="../../python/html/index.html">Python Coverage HTML Report</a></li>
      </ul>
    </div>
  </div>
  
  <footer>
    <p>Generated by <code>generate-coverage-report.sh</code> on $(date '+%Y-%m-%d %H:%M:%S')</p>
  </footer>
</body>
</html>
EOF
  
  log success "HTML report saved to $REPORT_DIR/index.html"
}

# Generate JSON report
generate_json_report() {
  local report_file="$COVERAGE_DIR/coverage.json"
  
  log info "Generating JSON coverage report"
  
  cat > "$report_file" <<EOF
{
  "timestamp": "$(date '+%Y-%m-%d %H:%M:%S')",
  "summary": {
    "overall": ${OVERALL_COVERAGE},
    "threshold": ${OVERALL_THRESHOLD},
    "total_covered": ${TOTAL_COVERED},
    "total_lines": ${TOTAL_LINES},
    "status": ${OVERALL_STATUS == "success" ? "true" : "false"}
  },
  "languages": {
    "java": {
      "coverage": ${JAVA_COVERAGE},
      "threshold": ${JAVA_COVERAGE_THRESHOLD},
      "lines_covered": ${JAVA_LINES_COVERED},
      "lines_total": ${JAVA_LINES_TOTAL},
      "status": ${JAVA_STATUS == "success" ? "true" : "false"}
    },
    "go": {
      "coverage": ${GO_COVERAGE},
      "threshold": ${GO_COVERAGE_THRESHOLD},
      "lines_covered": ${GO_LINES_COVERED},
      "lines_total": ${GO_LINES_TOTAL},
      "status": ${GO_STATUS == "success" ? "true" : "false"}
    },
    "python": {
      "coverage": ${PYTHON_COVERAGE},
      "threshold": ${PYTHON_THRESHOLD},
      "lines_covered": ${PYTHON_LINES_COVERED},
      "lines_total": ${PYTHON_LINES_TOTAL},
      "status": ${PYTHON_STATUS == "success" ? "true" : "false"}
    }
  }
}
EOF
  
  log success "JSON report saved to $report_file"
}

# Generate Markdown report for PR comments
generate_pr_comment() {
  local comment_file="$COVERAGE_DIR/pr-comment.md"
  
  log info "Generating Markdown PR comment"
  
  cat > "$comment_file" <<EOF
## 📊 Test Coverage Report

| Language | Coverage | Threshold | Status |
|----------|----------|-----------|--------|
| Overall  | ${OVERALL_COVERAGE}% | ${OVERALL_THRESHOLD}% | ${OVERALL_STATUS == "success" ? "✅" : "❌"} |
| Java     | ${JAVA_COVERAGE}% | ${JAVA_COVERAGE_THRESHOLD}% | ${JAVA_STATUS == "success" ? "✅" : "❌"} |
| Go       | ${GO_COVERAGE}% | ${GO_COVERAGE_THRESHOLD}% | ${GO_STATUS == "success" ? "✅" : "❌"} |
| Python   | ${PYTHON_COVERAGE}% | ${PYTHON_THRESHOLD}% | ${PYTHON_STATUS == "success" ? "✅" : "❌"} |

${OVERALL_STATUS == "success" ? "🟢 **Coverage meets or exceeds the required threshold.**" : "🔴 **Coverage is below the required threshold.**
"}
${OVERALL_STATUS != "success" ? "Areas to improve:
$([ "$JAVA_STATUS" != "success" ] && echo "- Java coverage needs an additional $(echo "${JAVA_COVERAGE_THRESHOLD} - ${JAVA_COVERAGE}" | bc)%")
$([ "$GO_STATUS" != "success" ] && echo "- Go coverage needs an additional $(echo "${GO_COVERAGE_THRESHOLD} - ${GO_COVERAGE}" | bc)%")
$([ "$PYTHON_STATUS" != "success" ] && echo "- Python coverage needs an additional $(echo "${PYTHON_THRESHOLD} - ${PYTHON_COVERAGE}" | bc)%")
" : ""}

<details>
<summary>View detailed coverage information</summary>

### Java Coverage: ${JAVA_COVERAGE}%
- Lines covered: ${JAVA_LINES_COVERED} of ${JAVA_LINES_TOTAL}
- Required threshold: ${JAVA_COVERAGE_THRESHOLD}%

### Go Coverage: ${GO_COVERAGE}%
- Lines covered: ${GO_LINES_COVERED} of ${GO_LINES_TOTAL}
- Required threshold: ${GO_COVERAGE_THRESHOLD}%

### Python Coverage: ${PYTHON_COVERAGE}%
- Lines covered: ${PYTHON_LINES_COVERED} of ${PYTHON_LINES_TOTAL}
- Required threshold: ${PYTHON_THRESHOLD}%

</details>

> This comment was automatically generated by the CI coverage reporting system.
EOF
  
  log success "PR comment saved to $comment_file"
}

# Post comment to GitHub PR
post_to_github_pr() {
  if [[ "$GITHUB_COMMENTS" != "true" || -z "$GITHUB_PR_NUMBER" || -z "$GITHUB_REPOSITORY" || -z "$GITHUB_TOKEN" ]]; then
    log debug "Skipping GitHub PR comment"
    return 0
  fi
  
  log info "Posting coverage results to GitHub PR #$GITHUB_PR_NUMBER"
  
  local comment_file="$COVERAGE_DIR/pr-comment.md"
  
  # Make sure we have a PR comment file
  if [[ ! -f "$comment_file" ]]; then
    generate_pr_comment
  fi
  
  # Post comment to GitHub PR
  if command -v curl &> /dev/null; then
    local api_url="https://api.github.com/repos/$GITHUB_REPOSITORY/issues/$GITHUB_PR_NUMBER/comments"
    local comment_content=$(cat "$comment_file")
    
    # Create JSON payload
    local json_payload=$(cat <<EOF
{
  "body": $(echo "$comment_content" | jq -R -s .)
}
EOF
)
    
    # Post comment
    local response=$(curl -s -X POST \
      -H "Authorization: token $GITHUB_TOKEN" \
      -H "Accept: application/vnd.github.v3+json" \
      -d "$json_payload" \
      "$api_url")
    
    if echo "$response" | grep -q '"id":'; then
      log success "Posted coverage results to GitHub PR #$GITHUB_PR_NUMBER"
    else
      log error "Failed to post comment to GitHub PR: $(echo "$response" | grep "message" || echo "Unknown error")"
      return 1
    fi
  else
    log error "curl not found. Cannot post to GitHub PR."
    return 1
  fi
  
  return 0
}

# Main function
main() {
  log info "Starting test coverage report generation"
  
  # Check requirements
  check_requirements || {
    log error "Requirements check failed"
    exit 1
  }
  
  # Process language-specific coverage data
  process_java_coverage || log warning "Failed to process Java coverage data"
  process_go_coverage || log warning "Failed to process Go coverage data"
  process_python_coverage || log warning "Failed to process Python coverage data"
  
  # Calculate overall coverage
  calculate_overall_coverage
  
  # Generate reports based on format
  case "$OUTPUT_FORMAT" in
    html)
      generate_html_report
      ;;
    json)
      generate_json_report
      ;;
    markdown|md)
      generate_pr_comment
      ;;
    all)
      generate_html_report
      generate_json_report
      generate_pr_comment
      ;;
    *)
      log warning "Unknown output format: $OUTPUT_FORMAT. Defaulting to HTML."
      generate_html_report
      ;;
  esac
  
  # Generate coverage badge if needed
  if [[ "$INCLUDE_BADGES" == "true" ]]; then
    generate_badge || log warning "Failed to generate coverage badge"
  fi
  
  # Generate PR comment if requested
  if [[ "$PR_COMMENT" == "true" ]]; then
    generate_pr_comment || log warning "Failed to generate PR comment"
  fi
  
  # Post to GitHub PR if configured
  if [[ "$GITHUB_COMMENTS" == "true" && "$PR_COMMENT" == "true" ]]; then
    post_to_github_pr || log warning "Failed to post to GitHub PR"
  fi
  
  # Final status
  if [[ "$OVERALL_STATUS" == "success" ]]; then
    log success "Overall coverage: $OVERALL_COVERAGE% meets threshold of $OVERALL_THRESHOLD%"
    exit 0
  else
    log warning "Overall coverage: $OVERALL_COVERAGE% is below threshold of $OVERALL_THRESHOLD%"
    exit 1
  fi
}

# Run the main function
main "$@"