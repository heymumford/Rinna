#!/bin/bash
#
# polyglot-coverage.sh - Unified code coverage for all languages
#
# This script generates and reports code coverage for Java, Go and Python components
# of the Rinna project, displaying a consolidated summary.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

set -eo pipefail

# Check if bc is available
if command -v bc >/dev/null 2>&1; then
  HAS_BC=true
else
  HAS_BC=false
  echo "Warning: 'bc' command not found. Using basic shell arithmetic instead."
fi

# Function to calculate percentage without bc
calc_percentage() {
  if [[ "$HAS_BC" == "true" ]]; then
    echo "scale=1; $1" | bc -l
  else
    # Basic arithmetic using awk as a fallback
    awk "BEGIN {printf \"%.1f\", $1}"
  fi
}

# Function to compare numbers without bc
compare_numbers() {
  if [[ "$HAS_BC" == "true" ]]; then
    echo "$1" | bc -l
  else
    # Basic comparison using awk as a fallback
    awk "BEGIN {print ($1) ? 1 : 0}"
  fi
}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Determine project directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COVERAGE_DIR="$PROJECT_ROOT/target/coverage"
REPORT_DIR="$COVERAGE_DIR/report"

# Command line options
VERBOSE=false
FAIL_THRESHOLD=0
CLEAN=false
REPORT_FORMAT="text"
LANGUAGES=("java" "go" "python")
NO_FAIL=false

# Default thresholds for each language (percentage)
JAVA_THRESHOLD=70
GO_THRESHOLD=60
PYTHON_THRESHOLD=60

# Default output directories for each language
JAVA_COVERAGE_DIR="$COVERAGE_DIR/java"
GO_COVERAGE_DIR="$COVERAGE_DIR/go"
PYTHON_COVERAGE_DIR="$COVERAGE_DIR/python"
HTML_REPORT_DIR="$COVERAGE_DIR/html"

usage() {
  echo "Usage: $0 [options]"
  echo
  echo "Options:"
  echo "  -h, --help             Show this help message"
  echo "  -v, --verbose          Show more detailed output"
  echo "  -c, --clean            Clean existing coverage data before running"
  echo "  -f, --fail <percent>   Set threshold to fail build (e.g. 80 for 80%)"
  echo "  -l, --languages <list> Comma-separated list of languages to include (java,go,python)"
  echo "  -o, --output <format>  Output format (text,html,json)"
  echo "  -n, --no-fail          Do not fail the build even if coverage is below thresholds"
  echo
  echo "Language-Specific Options:"
  echo "  --java-threshold <num>   Set Java coverage threshold (default: $JAVA_THRESHOLD%)"
  echo "  --go-threshold <num>     Set Go coverage threshold (default: $GO_THRESHOLD%)"
  echo "  --python-threshold <num> Set Python coverage threshold (default: $PYTHON_THRESHOLD%)"
  echo
  echo "Examples:"
  echo "  $0                       # Generate coverage for all languages with default settings"
  echo "  $0 -l java,go            # Generate coverage for Java and Go only"
  echo "  $0 -f 80                 # Fail if overall coverage is below 80%"
  echo "  $0 -o html               # Generate HTML reports"
  echo "  $0 --java-threshold 85   # Set Java specific threshold to 85%"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    -v|--verbose)
      VERBOSE=true
      shift
      ;;
    -c|--clean)
      CLEAN=true
      shift
      ;;
    -f|--fail)
      FAIL_THRESHOLD="$2"
      shift 2
      ;;
    -l|--languages)
      IFS=',' read -r -a LANGUAGES <<< "$2"
      shift 2
      ;;
    -o|--output)
      REPORT_FORMAT="$2"
      shift 2
      ;;
    -n|--no-fail)
      NO_FAIL=true
      shift
      ;;
    --java-threshold)
      JAVA_THRESHOLD="$2"
      shift 2
      ;;
    --go-threshold)
      GO_THRESHOLD="$2"
      shift 2
      ;;
    --python-threshold)
      PYTHON_THRESHOLD="$2"
      shift 2
      ;;
    *)
      echo -e "${RED}Error: Unknown option: $1${NC}"
      usage
      exit 1
      ;;
  esac
done

# Create coverage directories
mkdir -p "$COVERAGE_DIR" "$JAVA_COVERAGE_DIR" "$GO_COVERAGE_DIR" "$PYTHON_COVERAGE_DIR" "$HTML_REPORT_DIR"

# Clean coverage data if requested
if [[ "$CLEAN" == "true" ]]; then
  echo -e "${BLUE}Cleaning existing coverage data...${NC}"
  rm -rf "$COVERAGE_DIR"/*
  mkdir -p "$JAVA_COVERAGE_DIR" "$GO_COVERAGE_DIR" "$PYTHON_COVERAGE_DIR" "$HTML_REPORT_DIR"
fi

echo -e "${BLUE}Starting Rinna polyglot code coverage analysis${NC}"
echo "==========================================="

# Variables to store coverage results
java_coverage=0
go_coverage=0
python_coverage=0
overall_coverage=0
java_files=0
go_files=0
python_files=0

# Function to generate Java coverage using JaCoCo
generate_java_coverage() {
  if [[ ! " ${LANGUAGES[*]} " =~ " java " ]]; then
    return
  fi

  echo -e "${CYAN}Generating Java code coverage...${NC}"

  # Run Maven with JaCoCo
  cd "$PROJECT_ROOT"

  # Use set +e to prevent script from exiting if Maven fails
  set +e
  if [[ "$VERBOSE" == "true" ]]; then
    mvn clean test -P all-tests
  else
    mvn clean test -P all-tests > /dev/null
  fi
  MAVEN_EXIT_CODE=$?
  # Restore exit on error
  set -e

  if [[ $MAVEN_EXIT_CODE -ne 0 ]]; then
    echo -e "${YELLOW}Warning: Maven build failed. Java coverage may not be accurate.${NC}"
  fi

  # Extract coverage from JaCoCo report
  jacoco_csv="$(find "$PROJECT_ROOT" -name "jacoco.csv" | head -n 1)"

  if [[ -n "$jacoco_csv" ]]; then
    # Using awk to calculate coverage from CSV file
    coverage_data=$(awk -F, 'NR>1 {covered+=$5; missed+=$6} END {if (covered+missed > 0) print covered*100/(covered+missed); else print 0}' "$jacoco_csv")
    java_coverage=$(printf "%.1f" "$coverage_data")

    # Count Java files
    java_files=$(find "$PROJECT_ROOT" -path "*/target/*" -prune -o -path "*/backup/*" -prune -o -name "*.java" -not -path "*/test/*" | wc -l)

    echo -e "${GREEN}Java coverage: $java_coverage%${NC}"

    # Copy reports to our coverage directory
    cp -r "$(dirname "$jacoco_csv")"/* "$JAVA_COVERAGE_DIR/"
  else
    echo -e "${RED}Error: No Java coverage data found${NC}"
    java_coverage=0
  fi
}

# Function to generate Go coverage
generate_go_coverage() {
  if [[ ! " ${LANGUAGES[*]} " =~ " go " ]]; then
    return
  fi

  echo -e "${CYAN}Generating Go code coverage...${NC}"

  # Create Go coverage profile
  cd "$PROJECT_ROOT/api"

  # Use set +e to prevent script from exiting if Go tests fail
  set +e
  if [[ "$VERBOSE" == "true" ]]; then
    go test -coverprofile="$GO_COVERAGE_DIR/coverage.out" ./...
  else
    go test -coverprofile="$GO_COVERAGE_DIR/coverage.out" ./... > /dev/null
  fi
  GO_EXIT_CODE=$?
  # Restore exit on error
  set -e

  if [[ $GO_EXIT_CODE -ne 0 ]]; then
    echo -e "${YELLOW}Warning: Go tests failed. Go coverage may not be accurate.${NC}"
  fi

  # Generate HTML report if coverage file exists
  if [[ -f "$GO_COVERAGE_DIR/coverage.out" ]]; then
    go tool cover -html="$GO_COVERAGE_DIR/coverage.out" -o "$GO_COVERAGE_DIR/coverage.html"
  else
    echo -e "${YELLOW}Warning: No Go coverage data found.${NC}"
  fi

  # Extract coverage percentage
  if [[ -f "$GO_COVERAGE_DIR/coverage.out" ]]; then
    coverage_data=$(go tool cover -func="$GO_COVERAGE_DIR/coverage.out" | grep total | awk '{print $3}' | sed 's/%//')
    go_coverage="$coverage_data"

    # Count Go files
    go_files=$(find "$PROJECT_ROOT/api" -name "*.go" -not -path "*/test/*" | wc -l)

    echo -e "${GREEN}Go coverage: $go_coverage%${NC}"
  else
    echo -e "${RED}Error: No Go coverage data found${NC}"
    go_coverage=0
  fi
}

# Function to generate Python coverage
generate_python_coverage() {
  if [[ ! " ${LANGUAGES[*]} " =~ " python " ]]; then
    return
  fi

  echo -e "${CYAN}Generating Python code coverage...${NC}"

  # Run pytest with coverage
  cd "$PROJECT_ROOT"

  if [[ -d "$PROJECT_ROOT/python" ]]; then
    # Use set +e to prevent script from exiting if Python tests fail
    set +e
    if [[ "$VERBOSE" == "true" ]]; then
      python -m pytest python/tests --cov=python/rinna --cov-report=xml:$PYTHON_COVERAGE_DIR/coverage.xml --cov-report=html:$PYTHON_COVERAGE_DIR/html
    else
      python -m pytest python/tests --cov=python/rinna --cov-report=xml:$PYTHON_COVERAGE_DIR/coverage.xml --cov-report=html:$PYTHON_COVERAGE_DIR/html > /dev/null
    fi
    PYTHON_EXIT_CODE=$?
    # Restore exit on error
    set -e

    if [[ $PYTHON_EXIT_CODE -ne 0 ]]; then
      echo -e "${YELLOW}Warning: Python tests failed. Python coverage may not be accurate.${NC}"
    fi

    # Extract coverage percentage from XML
    if [[ -f "$PYTHON_COVERAGE_DIR/coverage.xml" ]]; then
      coverage_data=$(grep -o 'line-rate="[0-9.]*"' "$PYTHON_COVERAGE_DIR/coverage.xml" | head -1 | cut -d'"' -f2)
      python_coverage=$(calc_percentage "$coverage_data * 100")

      # Count Python files
      python_files=$(find "$PROJECT_ROOT/python" -name "*.py" -not -path "*/tests/*" | wc -l)

      echo -e "${GREEN}Python coverage: $python_coverage%${NC}"
    else
      echo -e "${RED}Error: No Python coverage data found${NC}"
      python_coverage=0
    fi
  else
    echo -e "${YELLOW}Warning: Python module not found${NC}"
    python_coverage=0
  fi
}

# Generate coverage for each language
generate_java_coverage
generate_go_coverage
generate_python_coverage

# Calculate overall coverage
total_files=$((java_files + go_files + python_files))

if [[ $total_files -gt 0 ]]; then
  overall_coverage=$(calc_percentage "($java_coverage * $java_files + $go_coverage * $go_files + $python_coverage * $python_files) / $total_files")
else
  overall_coverage=0
fi

# Generate unified HTML report
generate_html_report() {
  echo -e "${CYAN}Generating unified HTML report...${NC}"

  mkdir -p "$HTML_REPORT_DIR"

  # Create index.html with summary and links to language-specific reports
  cat > "$HTML_REPORT_DIR/index.html" << EOF
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Rinna Polyglot Code Coverage Report</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; color: #333; }
    h1 { color: #2c3e50; }
    h2 { color: #3498db; }
    .summary { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .coverage-bar { height: 20px; background-color: #ecf0f1; border-radius: 3px; margin: 10px 0; overflow: hidden; }
    .coverage-bar-fill { height: 100%; background-color: #2ecc71; }
    .language-section { margin-bottom: 30px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
    th { background-color: #f2f2f2; }
    tr:nth-child(even) { background-color: #f9f9f9; }
    .threshold-warning { background-color: #ffe6e6; }
    .threshold-ok { background-color: #e6ffe6; }
    a { color: #3498db; text-decoration: none; }
    a:hover { text-decoration: underline; }
    .timestamp { margin-top: 40px; font-size: 0.8em; color: #7f8c8d; }
  </style>
</head>
<body>
  <h1>Rinna Polyglot Code Coverage Report</h1>

  <div class="summary">
    <h2>Overall Coverage: ${overall_coverage}%</h2>
    <div class="coverage-bar">
      <div class="coverage-bar-fill" style="width: ${overall_coverage}%;"></div>
    </div>

    <table>
      <tr>
        <th>Language</th>
        <th>Coverage</th>
        <th>Files</th>
        <th>Threshold</th>
        <th>Status</th>
        <th>Report</th>
      </tr>
      <tr class="${java_coverage < JAVA_THRESHOLD ? 'threshold-warning' : 'threshold-ok'}">
        <td>Java</td>
        <td>${java_coverage}%</td>
        <td>${java_files}</td>
        <td>${JAVA_THRESHOLD}%</td>
        <td>${java_coverage < JAVA_THRESHOLD ? '⚠️ Below threshold' : '✅ Passed'}</td>
        <td><a href="../java/index.html">View Report</a></td>
      </tr>
      <tr class="${go_coverage < GO_THRESHOLD ? 'threshold-warning' : 'threshold-ok'}">
        <td>Go</td>
        <td>${go_coverage}%</td>
        <td>${go_files}</td>
        <td>${GO_THRESHOLD}%</td>
        <td>${go_coverage < GO_THRESHOLD ? '⚠️ Below threshold' : '✅ Passed'}</td>
        <td><a href="../go/coverage.html">View Report</a></td>
      </tr>
      <tr class="${python_coverage < PYTHON_THRESHOLD ? 'threshold-warning' : 'threshold-ok'}">
        <td>Python</td>
        <td>${python_coverage}%</td>
        <td>${python_files}</td>
        <td>${PYTHON_THRESHOLD}%</td>
        <td>${python_coverage < PYTHON_THRESHOLD ? '⚠️ Below threshold' : '✅ Passed'}</td>
        <td><a href="../python/html/index.html">View Report</a></td>
      </tr>
    </table>
  </div>

  <div class="timestamp">
    Report generated on $(date)
  </div>
</body>
</html>
EOF

  echo -e "${GREEN}HTML report generated at: ${HTML_REPORT_DIR}/index.html${NC}"
}

# Generate JSON report
generate_json_report() {
  echo -e "${CYAN}Generating JSON report...${NC}"

  cat > "$COVERAGE_DIR/coverage.json" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "overall_coverage": $overall_coverage,
  "total_files": $total_files,
  "languages": {
    "java": {
      "coverage": $java_coverage,
      "files": $java_files,
      "threshold": $JAVA_THRESHOLD,
      "passed": $(( $(compare_numbers "$java_coverage >= $JAVA_THRESHOLD") ))
    },
    "go": {
      "coverage": $go_coverage,
      "files": $go_files,
      "threshold": $GO_THRESHOLD,
      "passed": $(( $(compare_numbers "$go_coverage >= $GO_THRESHOLD") ))
    },
    "python": {
      "coverage": $python_coverage,
      "files": $python_files,
      "threshold": $PYTHON_THRESHOLD,
      "passed": $(( $(compare_numbers "$python_coverage >= $PYTHON_THRESHOLD") ))
    }
  }
}
EOF

  echo -e "${GREEN}JSON report generated at: ${COVERAGE_DIR}/coverage.json${NC}"
}

# Generate reports based on format
if [[ "$REPORT_FORMAT" == "html" ]]; then
  generate_html_report
elif [[ "$REPORT_FORMAT" == "json" ]]; then
  generate_json_report
fi

# Print summary report
echo 
echo "==========================================="
echo -e "${BLUE}Rinna Polyglot Code Coverage Summary${NC}"
echo "==========================================="
echo -e "Total files analyzed: ${total_files}"
echo -e "Overall coverage: ${CYAN}${overall_coverage}%${NC}"
echo
echo "Coverage by language:"
echo "-------------------------------------------"
echo -e "Java:   ${CYAN}${java_coverage}%${NC} (${java_files} files, threshold: ${JAVA_THRESHOLD}%)"
echo -e "Go:     ${CYAN}${go_coverage}%${NC} (${go_files} files, threshold: ${GO_THRESHOLD}%)"
echo -e "Python: ${CYAN}${python_coverage}%${NC} (${python_files} files, threshold: ${PYTHON_THRESHOLD}%)"
echo "-------------------------------------------"

# Check if coverage meets thresholds
failed=false

if (( $(compare_numbers "$java_coverage < $JAVA_THRESHOLD") )); then
  echo -e "${RED}⚠️ Java coverage is below threshold!${NC}"
  failed=true
fi

if (( $(compare_numbers "$go_coverage < $GO_THRESHOLD") )); then
  echo -e "${RED}⚠️ Go coverage is below threshold!${NC}"
  failed=true
fi

if (( $(compare_numbers "$python_coverage < $PYTHON_THRESHOLD") )); then
  echo -e "${RED}⚠️ Python coverage is below threshold!${NC}"
  failed=true
fi

if (( $(compare_numbers "$overall_coverage < $FAIL_THRESHOLD") )) && [[ "$FAIL_THRESHOLD" -gt 0 ]]; then
  echo -e "${RED}⚠️ Overall coverage is below failure threshold of ${FAIL_THRESHOLD}%!${NC}"
  failed=true
else
  echo -e "${GREEN}✅ Overall coverage is satisfactory.${NC}"
fi

# Display report location
echo
echo -e "Coverage reports available at: ${CYAN}${COVERAGE_DIR}${NC}"
if [[ "$REPORT_FORMAT" == "html" ]]; then
  echo -e "HTML report: ${CYAN}${HTML_REPORT_DIR}/index.html${NC}"
elif [[ "$REPORT_FORMAT" == "json" ]]; then
  echo -e "JSON report: ${CYAN}${COVERAGE_DIR}/coverage.json${NC}"
fi

# Debug information
echo
echo "Debug information:"
echo "FAIL_THRESHOLD: $FAIL_THRESHOLD"
echo "NO_FAIL: $NO_FAIL"
echo "failed: $failed"
echo "MAVEN_EXIT_CODE: ${MAVEN_EXIT_CODE:-N/A}"
echo "GO_EXIT_CODE: ${GO_EXIT_CODE:-N/A}"
echo "PYTHON_EXIT_CODE: ${PYTHON_EXIT_CODE:-N/A}"
echo "PATH: $PATH"
echo "PWD: $(pwd)"
echo "USER: $(whoami)"

# Exit with failure if thresholds not met
if [[ "$failed" == "true" ]] && [[ "$FAIL_THRESHOLD" -gt 0 ]] && [[ "$NO_FAIL" != "true" ]]; then
  echo
  echo -e "${RED}❌ Coverage check failed!${NC}"
  echo "Exiting with code 1"
  exit 1
elif [[ "$failed" == "true" ]] && [[ "$NO_FAIL" == "true" ]]; then
  echo
  echo -e "${YELLOW}⚠️ Coverage check would have failed, but --no-fail option is set.${NC}"
  echo "Exiting with code 0"
fi

echo "Exiting with code 0"
exit 0
