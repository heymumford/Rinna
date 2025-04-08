#!/bin/bash

# Dependency Validator Script (XML version)
# This script checks for circular dependencies and proper module relationships in the Rinna project.
# It uses XMLStarlet for precise XML manipulation and validation.

set -e  # Exit on error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${PROJECT_ROOT}"

# Source the XML tools library
source "${PROJECT_ROOT}/bin/xml-tools.sh"

echo "🔍 Checking for circular dependencies in Rinna project modules..."

# Required dependency chain based on Clean Architecture principles:
# api -> rinna-cli -> rinna-core
# Anything else is a violation

# Check if a module has prohibited dependencies
check_dependencies() {
    local module_path="$1"
    local module_name="$2"
    local prohibited_deps=($3)
    
    if [ ! -f "${module_path}/pom.xml" ]; then
        echo "  Warning: $module_path/pom.xml not found. Skipping."
        return 0
    fi
    
    echo "  Checking $module_name for invalid dependencies..."
    
    for prohibited in "${prohibited_deps[@]}"; do
        # Check if the module name is mentioned in artifactIds
        local has_dependency=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
            -v "count(//pom:dependency[contains(pom:artifactId, '${prohibited}')])" \
            "${module_path}/pom.xml")
        
        if [ "$has_dependency" -gt 0 ]; then
            echo "❌ ERROR: Circular dependency detected in ${module_name}!"
            echo "   ${module_name} must not depend on: ${prohibited}"
            echo "   Please remove this dependency from ${module_path}/pom.xml"
            return 1
        fi
    done
    
    echo "  ✅ ${module_name} dependencies are valid"
    return 0
}

# Check for test-scope dependencies that are missing the test scope
check_test_scope() {
    local module_path="$1"
    local module_name="$2"
    
    if [ ! -f "${module_path}/pom.xml" ]; then
        return 0
    fi
    
    echo "  Checking $module_name for non-test dependencies that should have test scope..."
    
    # Define libraries that should always have test scope
    test_libraries=("junit" "mockito" "assertj" "cucumber" "junit-platform" "junit.platform")
    
    for lib in "${test_libraries[@]}"; do
        # Find dependencies containing the library name without test scope
        local missing_scope=$(xml_find_missing_test_scope "${module_path}/pom.xml" "${lib}")
        
        if [ ! -z "$missing_scope" ]; then
            echo "⚠️  Warning: Found test dependencies without test scope in ${module_name}:"
            echo "$missing_scope"
            echo "   Consider adding <scope>test</scope> to these dependencies"
        fi
    done
    
    echo "  ✅ ${module_name} test scope check completed"
}

# 1. Check rinna-core dependencies (should not depend on rinna-cli or api)
check_dependencies "${PROJECT_ROOT}/rinna-core" "rinna-core" "rinna-cli api"

# 2. Check rinna-cli dependencies (should not depend on api)
check_dependencies "${PROJECT_ROOT}/rinna-cli" "rinna-cli" "api"

# 3. Check parent POM to ensure it doesn't depend on any of its children
check_dependencies "${PROJECT_ROOT}" "parent POM" "rinna-core rinna-cli api"

# 4. Check test scopes in all modules
check_test_scope "${PROJECT_ROOT}" "parent POM"
check_test_scope "${PROJECT_ROOT}/rinna-core" "rinna-core"
check_test_scope "${PROJECT_ROOT}/rinna-cli" "rinna-cli"
[ -d "${PROJECT_ROOT}/api" ] && check_test_scope "${PROJECT_ROOT}/api" "api"

echo "✅ Dependency validation completed successfully!"