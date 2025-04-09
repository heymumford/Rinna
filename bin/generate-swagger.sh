#!/bin/bash
#
# generate-swagger.sh - Generate Swagger/OpenAPI Documentation
#
# This script generates Swagger/OpenAPI documentation for the Rinna API.
# It validates the swagger.yaml file and can optionally convert it to other formats.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

# Source common utilities and logger
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# Try to source common utilities if they exist
if [[ -f "$SCRIPT_DIR/common/rinna_utils.sh" ]]; then
  source "$SCRIPT_DIR/common/rinna_utils.sh"
else
  # Simple placeholder for missing utilities
  function log_info() { echo "[INFO] $*"; }
  function log_error() { echo "[ERROR] $*" >&2; }
  function log_warn() { echo "[WARN] $*" >&2; }
  # Export the functions
  export -f log_info
  export -f log_error
  export -f log_warn
fi

# Create simple formatter functions if the formatter doesn't exist
if [[ -f "$SCRIPT_DIR/formatters/build_formatter.sh" ]]; then
  source "$SCRIPT_DIR/formatters/build_formatter.sh"
else
  # Simple placeholder for missing formatters
  function start_task() { echo "→ Starting: $*"; }
  function complete_task() { echo "✓ Complete: $*"; }
  function fail_task() { echo "✗ Failed: $*" >&2; }
  function skip_task() { echo "⏭️ Skipped: $*"; }
  function warn_task() { echo "⚠️ Warning: $*" >&2; }
  function section_header() { echo -e "\n== $* =="; }
  function show_execution_plan() { 
    echo "Execution plan for $1:"; 
    shift; 
    for step in "$@"; do echo "- $step"; done; 
  }
  function run_formatted() {
    local cmd="$1"
    local desc="${2:-Running command}"
    echo "→ $desc"
    eval "$cmd"
    local result=$?
    if [[ $result -eq 0 ]]; then
      echo "✓ $desc completed"
    else
      echo "✗ $desc failed" >&2
    fi
    return $result
  }
  # Color definitions for output
  GREEN='\033[0;32m'
  BLUE='\033[0;34m'
  BOLD='\033[1m'
  NC='\033[0m' # No Color
fi

# Try to source logger if it exists
if [[ -f "$SCRIPT_DIR/common/rinna_logger.sh" ]]; then
  source "$SCRIPT_DIR/common/rinna_logger.sh"
fi

# Set module name for logging if function exists
if declare -f set_module_name > /dev/null; then
  set_module_name "swagger-generator"
fi

# Define API directory
API_DIR="$PROJECT_ROOT/api"
SWAGGER_FILE="$API_DIR/swagger.yaml"
OUTPUT_DIR="$API_DIR/docs"

# Parse command line options
VALIDATE_ONLY=false
FORMAT="yaml"
HELP=false
VERBOSE=false

# Show usage
show_usage() {
  cat << EOF
Usage: $(basename "$0") [options]

Options:
  --validate-only     Only validate the swagger.yaml file, don't generate outputs
  --format=FORMAT     Output format: yaml (default), json, html
  --verbose           Show verbose output
  --help, -h          Show this help message

Examples:
  $(basename "$0")                # Validate and generate swagger docs
  $(basename "$0") --validate-only # Only validate the swagger.yaml file
  $(basename "$0") --format=json  # Generate JSON format
EOF
}

# Parse options
parse_options() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --validate-only)
        VALIDATE_ONLY=true
        shift
        ;;
      --format=*)
        FORMAT="${1#*=}"
        shift
        ;;
      --verbose)
        VERBOSE=true
        export VERBOSE=true
        shift
        ;;
      --help|-h)
        HELP=true
        shift
        ;;
      *)
        log_error "Unknown option: $1"
        show_usage
        exit 1
        ;;
    esac
  done
}

# Validate swagger.yaml file
validate_swagger() {
  start_task "Validating Swagger file: $SWAGGER_FILE"
  
  if [[ ! -f "$SWAGGER_FILE" ]]; then
    fail_task "Swagger file not found: $SWAGGER_FILE"
    return 1
  fi
  
  # Try to use swagger-cli if available
  if command -v swagger-cli &> /dev/null; then
    if swagger-cli validate "$SWAGGER_FILE"; then
      complete_task "Swagger file validation passed"
      return 0
    else
      fail_task "Swagger file validation failed"
      return 1
    fi
  fi
  
  # If swagger-cli is not available, try to use online validator
  log_warn "swagger-cli not found. Trying basic YAML validation..."
  
  # Basic YAML validation
  if command -v python3 &> /dev/null; then
    if python3 -c "import yaml; yaml.safe_load(open('$SWAGGER_FILE'))" 2>/dev/null; then
      complete_task "Basic YAML validation passed (install swagger-cli for full validation)"
      return 0
    else
      fail_task "YAML validation failed - invalid YAML format"
      return 1
    fi
  fi
  
  # If neither is available, just check if the file exists
  warn_task "No YAML validator found. Skipping validation."
  return 0
}

# Generate documentation in various formats
generate_documentation() {
  mkdir -p "$OUTPUT_DIR"
  
  case "$FORMAT" in
    yaml)
      # YAML is already the source format, just copy it
      cp "$SWAGGER_FILE" "$OUTPUT_DIR/swagger.yaml"
      log_info "✅ Generated YAML documentation: $OUTPUT_DIR/swagger.yaml"
      ;;
    json)
      start_task "Converting Swagger to JSON format"
      if command -v python3 &> /dev/null; then
        python3 -c "import yaml, json, sys; json.dump(yaml.safe_load(open('$SWAGGER_FILE')), open('$OUTPUT_DIR/swagger.json', 'w'), indent=2)" 2>/dev/null
        if [[ $? -eq 0 ]]; then
          complete_task "Generated JSON documentation: $OUTPUT_DIR/swagger.json"
        else
          fail_task "Failed to convert to JSON format"
          return 1
        fi
      else
        fail_task "Python3 required for JSON conversion"
        return 1
      fi
      ;;
    html)
      start_task "Generating HTML documentation"
      if command -v npx &> /dev/null && npx redoc-cli -v &> /dev/null; then
        npx redoc-cli bundle "$SWAGGER_FILE" -o "$OUTPUT_DIR/index.html"
        if [[ $? -eq 0 ]]; then
          complete_task "Generated HTML documentation: $OUTPUT_DIR/index.html"
        else
          fail_task "Failed to generate HTML documentation"
          return 1
        fi
      else
        log_warn "redoc-cli not found. Trying to install it temporarily..."
        npm install -g redoc-cli &> /dev/null
        if [[ $? -eq 0 ]]; then
          npx redoc-cli bundle "$SWAGGER_FILE" -o "$OUTPUT_DIR/index.html"
          if [[ $? -eq 0 ]]; then
            complete_task "Generated HTML documentation: $OUTPUT_DIR/index.html"
          else
            fail_task "Failed to generate HTML documentation"
            return 1
          fi
        else
          fail_task "redoc-cli required for HTML documentation generation"
          return 1
        fi
      fi
      ;;
    *)
      log_error "Unknown format: $FORMAT"
      return 1
      ;;
  esac
  
  return 0
}

# Check for go-swagger and install it if necessary
check_go_swagger() {
  if command -v swagger &> /dev/null; then
    return 0
  fi
  
  start_task "go-swagger not found. Checking if we can install it"
  
  # Only try to install if go is available
  if ! command -v go &> /dev/null; then
    warn_task "Go not installed. Cannot install go-swagger"
    return 1
  fi
  
  log_info "Attempting to install go-swagger..."
  go install github.com/go-swagger/go-swagger/cmd/swagger@latest
  
  if [[ $? -eq 0 ]]; then
    complete_task "go-swagger installed successfully"
    export PATH="$PATH:$(go env GOPATH)/bin"
    return 0
  else
    fail_task "Failed to install go-swagger"
    return 1
  fi
}

# Main function
main() {
  parse_options "$@"
  
  if [[ "$HELP" == "true" ]]; then
    show_usage
    exit 0
  fi
  
  section_header "Swagger/OpenAPI Documentation Generator"
  
  # Validate Swagger file
  validate_swagger
  if [[ $? -ne 0 ]]; then
    exit 1
  fi
  
  # Generate documentation if requested
  if [[ "$VALIDATE_ONLY" != "true" ]]; then
    generate_documentation
    if [[ $? -ne 0 ]]; then
      exit 1
    fi
  fi
  
  log_info "✅ Swagger documentation process completed successfully"
  return 0
}

# Set execute permissions
chmod +x "$0"

# Run the main function
main "$@"