#!/bin/bash
#
# version-validator.sh - Validate version consistency across project components
# 
# This script monitors and validates version consistency across different
# language environments, ensuring all components use the same version.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Constants
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
VERSION_FILE="${PROJECT_ROOT}/version.properties"
VERSION_REPORT="${PROJECT_ROOT}/target/version-report.json"

# Source the XML tools library
source "${PROJECT_ROOT}/bin/xml-tools.sh"

# Colorized output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
RESET='\033[0m'

# Make sure target directory exists
mkdir -p "${PROJECT_ROOT}/target"

# Get the current official version from version.properties
get_official_version() {
  if [ ! -f "${VERSION_FILE}" ]; then
    echo "ERROR: Version file not found: ${VERSION_FILE}" >&2
    return 1
  fi
  
  grep -m 1 "^version=" "${VERSION_FILE}" | cut -d'=' -f2
}

# Check if version is valid semver
is_valid_semver() {
  local version="$1"
  
  if [[ "${version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9._-]+)?$ ]]; then
    return 0
  else
    return 1
  fi
}

# Parse Maven POM files and collect versions
check_maven_versions() {
  local official_version="$1"
  local report="$2"
  local errors=0
  
  echo "Checking Maven POM files..."
  
  # Initialize JSON array for Maven versions
  echo '  "maven": [' >> "${report}"
  
  # Check parent POM
  if [ -f "${PROJECT_ROOT}/pom.xml" ]; then
    local pom_version=$(xml_get_version "${PROJECT_ROOT}/pom.xml")
    local result="pass"
    local message=""
    
    if [ "${pom_version}" != "${official_version}" ]; then
      result="fail"
      message="Version mismatch (expected ${official_version})"
      errors=$((errors + 1))
      echo -e "  ${RED}✗ pom.xml: ${pom_version} ${RESET}(should be ${official_version})"
    else
      echo -e "  ${GREEN}✓ pom.xml: ${pom_version}${RESET}"
    fi
    
    # Add to JSON report
    echo '    {' >> "${report}"
    echo '      "file": "pom.xml",' >> "${report}"
    echo '      "version": "'"${pom_version}"'",' >> "${report}"
    echo '      "expected": "'"${official_version}"'",' >> "${report}"
    echo '      "result": "'"${result}"'",' >> "${report}"
    echo '      "message": "'"${message}"'"' >> "${report}"
    echo '    },' >> "${report}"
  fi
  
  # Check module POMs
  for module in rinna-core rinna-cli rinna-data-sqlite; do
    local module_pom="${PROJECT_ROOT}/${module}/pom.xml"
    if [ -f "${module_pom}" ]; then
      local parent_version=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" \
        -t -v "/pom:project/pom:parent/pom:version" "${module_pom}" 2>/dev/null)
      
      local result="pass"
      local message=""
      
      if [ "${parent_version}" != "${official_version}" ]; then
        result="fail"
        message="Parent version mismatch (expected ${official_version})"
        errors=$((errors + 1))
        echo -e "  ${RED}✗ ${module}/pom.xml: ${parent_version}${RESET} (should be ${official_version})"
      else
        echo -e "  ${GREEN}✓ ${module}/pom.xml: ${parent_version}${RESET}"
      fi
      
      # Add to JSON report
      echo '    {' >> "${report}"
      echo '      "file": "'"${module}/pom.xml"'",' >> "${report}"
      echo '      "version": "'"${parent_version}"'",' >> "${report}"
      echo '      "expected": "'"${official_version}"'",' >> "${report}"
      echo '      "result": "'"${result}"'",' >> "${report}"
      echo '      "message": "'"${message}"'"' >> "${report}"
      echo '    },' >> "${report}"
    fi
  done
  
  # Close the Maven array in JSON report (remove trailing comma from last entry)
  sed -i '$ s/,$//' "${report}"
  echo '  ],' >> "${report}"
  
  return $errors
}

# Check Go version files
check_go_versions() {
  local official_version="$1"
  local report="$2"
  local errors=0
  
  echo "Checking Go version files..."
  
  # Initialize JSON array for Go versions
  echo '  "go": [' >> "${report}"
  
  # Find and check Go version files
  local first_file=true
  for version_file in "${PROJECT_ROOT}/api/internal/version/version.go" "${PROJECT_ROOT}/api/pkg/health/version.go" "${PROJECT_ROOT}/version-service/core/version.go"; do
    if [ -f "${version_file}" ]; then
      # Skip comma for first file
      if [ "$first_file" = true ]; then
        first_file=false
      else
        # Ensure trailing comma for previous entry
        echo '    },' >> "${report}"
      fi
      
      local rel_path="${version_file#$PROJECT_ROOT/}"
      
      if grep -q 'Version\s*=\s*"[0-9.]\+"' "${version_file}"; then
        local go_version=$(grep -o 'Version\s*=\s*"[0-9.]\+"' "${version_file}" | grep -o '[0-9.]\+')
        local result="pass"
        local message=""
        
        if [ "${go_version}" != "${official_version}" ]; then
          result="fail"
          message="Version mismatch (expected ${official_version})"
          errors=$((errors + 1))
          echo -e "  ${RED}✗ ${rel_path}: ${go_version}${RESET} (should be ${official_version})"
        else
          echo -e "  ${GREEN}✓ ${rel_path}: ${go_version}${RESET}"
        fi
        
        # Add to JSON report
        echo '    {' >> "${report}"
        echo '      "file": "'"${rel_path}"'",' >> "${report}"
        echo '      "version": "'"${go_version}"'",' >> "${report}"
        echo '      "expected": "'"${official_version}"'",' >> "${report}"
        echo '      "result": "'"${result}"'",' >> "${report}"
        echo '      "message": "'"${message}"'"' >> "${report}"
      else
        echo -e "  ${YELLOW}? ${rel_path}: No version found${RESET}"
        
        # Add to JSON report
        echo '    {' >> "${report}"
        echo '      "file": "'"${rel_path}"'",' >> "${report}"
        echo '      "version": "unknown",' >> "${report}"
        echo '      "expected": "'"${official_version}"'",' >> "${report}"
        echo '      "result": "unknown",' >> "${report}"
        echo '      "message": "No version found"' >> "${report}"
      fi
    fi
  done
  
  # Close the Go array in JSON report
  echo '    }' >> "${report}"
  echo '  ],' >> "${report}"
  
  return $errors
}

# Check Python version files
check_python_versions() {
  local official_version="$1"
  local report="$2"
  local errors=0
  
  echo "Checking Python version files..."
  
  # Initialize JSON array for Python versions
  echo '  "python": [' >> "${report}"
  
  # Check setup.py
  local setup_py="${PROJECT_ROOT}/python/setup.py"
  if [ -f "${setup_py}" ]; then
    local py_version=$(grep -m 1 "version=" "${setup_py}" | cut -d'"' -f2)
    local result="pass"
    local message=""
    
    if [ "${py_version}" != "${official_version}" ]; then
      result="fail"
      message="Version mismatch (expected ${official_version})"
      errors=$((errors + 1))
      echo -e "  ${RED}✗ python/setup.py: ${py_version}${RESET} (should be ${official_version})"
    else
      echo -e "  ${GREEN}✓ python/setup.py: ${py_version}${RESET}"
    fi
    
    # Add to JSON report
    echo '    {' >> "${report}"
    echo '      "file": "python/setup.py",' >> "${report}"
    echo '      "version": "'"${py_version}"'",' >> "${report}"
    echo '      "expected": "'"${official_version}"'",' >> "${report}"
    echo '      "result": "'"${result}"'",' >> "${report}"
    echo '      "message": "'"${message}"'"' >> "${report}"
    echo '    },' >> "${report}"
  fi
  
  # Check __init__.py
  local init_py="${PROJECT_ROOT}/python/rinna/__init__.py"
  if [ -f "${init_py}" ]; then
    if grep -q "__version__" "${init_py}"; then
      local init_version=$(grep -m 1 "__version__" "${init_py}" | cut -d'"' -f2)
      local result="pass"
      local message=""
      
      if [ "${init_version}" != "${official_version}" ]; then
        result="fail"
        message="Version mismatch (expected ${official_version})"
        errors=$((errors + 1))
        echo -e "  ${RED}✗ python/rinna/__init__.py: ${init_version}${RESET} (should be ${official_version})"
      else
        echo -e "  ${GREEN}✓ python/rinna/__init__.py: ${init_version}${RESET}"
      fi
      
      # Add to JSON report
      echo '    {' >> "${report}"
      echo '      "file": "python/rinna/__init__.py",' >> "${report}"
      echo '      "version": "'"${init_version}"'",' >> "${report}"
      echo '      "expected": "'"${official_version}"'",' >> "${report}"
      echo '      "result": "'"${result}"'",' >> "${report}"
      echo '      "message": "'"${message}"'"' >> "${report}"
      echo '    },' >> "${report}"
    fi
  fi
  
  # Check pyproject.toml
  local pyproject="${PROJECT_ROOT}/pyproject.toml"
  if [ -f "${pyproject}" ]; then
    if grep -q "version" "${pyproject}"; then
      local toml_version=$(grep "version" "${pyproject}" | head -1 | grep -o '"[0-9.]*"' | tr -d '"')
      local result="pass"
      local message=""
      
      if [ "${toml_version}" != "${official_version}" ]; then
        result="fail"
        message="Version mismatch (expected ${official_version})"
        errors=$((errors + 1))
        echo -e "  ${RED}✗ pyproject.toml: ${toml_version}${RESET} (should be ${official_version})"
      else
        echo -e "  ${GREEN}✓ pyproject.toml: ${toml_version}${RESET}"
      fi
      
      # Add to JSON report
      echo '    {' >> "${report}"
      echo '      "file": "pyproject.toml",' >> "${report}"
      echo '      "version": "'"${toml_version}"'",' >> "${report}"
      echo '      "expected": "'"${official_version}"'",' >> "${report}"
      echo '      "result": "'"${result}"'",' >> "${report}"
      echo '      "message": "'"${message}"'"' >> "${report}"
      echo '    }' >> "${report}"
    fi
  fi
  
  # Close the Python array in JSON report
  echo '  ],' >> "${report}"
  
  return $errors
}

# Check README badge
check_readme() {
  local official_version="$1"
  local report="$2"
  local errors=0
  
  echo "Checking README badge..."
  
  # Initialize JSON array for README
  echo '  "readme": [' >> "${report}"
  
  local readme="${PROJECT_ROOT}/README.md"
  if [ -f "${readme}" ]; then
    local badge_line=$(grep -m 1 "badge/version-" "${readme}")
    if [ -n "${badge_line}" ]; then
      local readme_version=$(echo "${badge_line}" | grep -o -E "version-[0-9.]*-" | sed -E 's/version-(.*)-.*/\1/')
      local result="pass"
      local message=""
      
      if [ "${readme_version}" != "${official_version}" ]; then
        result="fail"
        message="Version mismatch (expected ${official_version})"
        errors=$((errors + 1))
        echo -e "  ${RED}✗ README badge: ${readme_version}${RESET} (should be ${official_version})"
      else
        echo -e "  ${GREEN}✓ README badge: ${readme_version}${RESET}"
      fi
      
      # Add to JSON report
      echo '    {' >> "${report}"
      echo '      "file": "README.md",' >> "${report}"
      echo '      "version": "'"${readme_version}"'",' >> "${report}"
      echo '      "expected": "'"${official_version}"'",' >> "${report}"
      echo '      "result": "'"${result}"'",' >> "${report}"
      echo '      "message": "'"${message}"'"' >> "${report}"
      echo '    }' >> "${report}"
    else
      echo -e "  ${YELLOW}? README badge: Not found${RESET}"
      
      # Add to JSON report
      echo '    {' >> "${report}"
      echo '      "file": "README.md",' >> "${report}"
      echo '      "version": "unknown",' >> "${report}"
      echo '      "expected": "'"${official_version}"'",' >> "${report}"
      echo '      "result": "unknown",' >> "${report}"
      echo '      "message": "Badge not found"' >> "${report}"
      echo '    }' >> "${report}"
    fi
  fi
  
  # Close the README array in JSON report
  echo '  ]' >> "${report}"
  
  return $errors
}

# Main validation function
validate_versions() {
  local official_version=$(get_official_version)
  if [ -z "$official_version" ]; then
    echo -e "${RED}ERROR: Could not determine official version${RESET}"
    exit 1
  fi
  
  # Create JSON report
  echo '{' > "${VERSION_REPORT}"
  echo '  "timestamp": "'"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"'",' >> "${VERSION_REPORT}"
  echo '  "official_version": "'"${official_version}"'",' >> "${VERSION_REPORT}"
  
  echo -e "\n${BOLD}Validating Rinna version consistency${RESET}"
  echo -e "Official version from version.properties: ${BLUE}${official_version}${RESET}\n"
  
  local maven_errors=0
  local go_errors=0
  local python_errors=0
  local readme_errors=0
  
  # Check all components
  check_maven_versions "$official_version" "$VERSION_REPORT"
  maven_errors=$?
  
  check_go_versions "$official_version" "$VERSION_REPORT"
  go_errors=$?
  
  check_python_versions "$official_version" "$VERSION_REPORT"
  python_errors=$?
  
  check_readme "$official_version" "$VERSION_REPORT"
  readme_errors=$?
  
  # Calculate total errors
  local total_errors=$((maven_errors + go_errors + python_errors + readme_errors))
  
  # Close JSON report with summary
  echo '  "summary": {' >> "${VERSION_REPORT}"
  echo '    "maven_errors": '"${maven_errors}"',' >> "${VERSION_REPORT}"
  echo '    "go_errors": '"${go_errors}"',' >> "${VERSION_REPORT}"
  echo '    "python_errors": '"${python_errors}"',' >> "${VERSION_REPORT}"
  echo '    "readme_errors": '"${readme_errors}"',' >> "${VERSION_REPORT}"
  echo '    "total_errors": '"${total_errors}"',' >> "${VERSION_REPORT}"
  echo '    "status": "'"$([ $total_errors -eq 0 ] && echo "pass" || echo "fail")"'"' >> "${VERSION_REPORT}"
  echo '  }' >> "${VERSION_REPORT}"
  echo '}' >> "${VERSION_REPORT}"
  
  # Print summary
  echo -e "\n${BOLD}Validation Summary${RESET}"
  echo -e "Maven errors:   ${maven_errors}"
  echo -e "Go errors:      ${go_errors}"
  echo -e "Python errors:  ${python_errors}"
  echo -e "README errors:  ${readme_errors}"
  echo -e "---------------------"
  echo -e "${BOLD}Total errors:  ${total_errors}${RESET}"
  
  if [ $total_errors -eq 0 ]; then
    echo -e "\n${GREEN}✓ All components have consistent versions${RESET}"
    echo -e "Report saved to: ${VERSION_REPORT}"
    return 0
  else
    echo -e "\n${RED}✗ Version inconsistencies detected${RESET}"
    echo -e "To fix these inconsistencies, run: ${BLUE}bin/version-tools/version-sync.sh sync${RESET}"
    echo -e "Report saved to: ${VERSION_REPORT}"
    return 1
  fi
}

# Print JSON report nicely
print_report() {
  if [ -f "${VERSION_REPORT}" ]; then
    if command -v jq &> /dev/null; then
      jq '.' "${VERSION_REPORT}"
    else
      cat "${VERSION_REPORT}"
    fi
  else
    echo "No version report found. Please run validation first."
    return 1
  fi
}

# Display help message
show_help() {
  echo -e "${BOLD}Version Validator${RESET} - Check version consistency across project components"
  echo ""
  echo "Usage: $0 [OPTION]"
  echo ""
  echo "Options:"
  echo "  --help        Display this help message"
  echo "  --ci          Run in CI mode (exit code reflects validation result)"
  echo "  --report      Display the latest validation report"
  echo ""
  echo "Examples:"
  echo "  $0            Run version validation"
  echo "  $0 --report   Show the latest validation report"
  echo ""
}

# Main function
main() {
  local CI_MODE=false
  local SHOW_REPORT=false
  
  # Parse arguments
  for arg in "$@"; do
    case $arg in
      --help)
        show_help
        exit 0
        ;;
      --ci)
        CI_MODE=true
        ;;
      --report)
        SHOW_REPORT=true
        ;;
      *)
        echo "Unknown option: $arg"
        show_help
        exit 1
        ;;
    esac
  done
  
  if [ "$SHOW_REPORT" = true ]; then
    print_report
    exit $?
  fi
  
  # Validate versions
  validate_versions
  local exit_code=$?
  
  # In CI mode, return the validation result as exit code
  if [ "$CI_MODE" = true ]; then
    exit $exit_code
  fi
  
  # In normal mode, exit with 0 (success)
  exit 0
}

# Execute main function with all arguments
main "$@"