#!/bin/bash

# Dependency Validator Script
# This script checks for circular dependencies and proper module relationships in the Rinna project.

set -e  # Exit on error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${PROJECT_ROOT}"

echo "🔍 Checking for circular dependencies in Rinna project modules..."

# Required dependency chain based on Clean Architecture principles:
# api -> rinna-cli -> rinna-core
# Anything else is a violation

# Extract dependencies from POM files
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
        if grep -q "<artifactId>${prohibited}</artifactId>" "${module_path}/pom.xml"; then
            echo "❌ ERROR: Circular dependency detected in ${module_name}!"
            echo "   ${module_name} must not depend on: ${prohibited}"
            echo "   Please remove this dependency from ${module_path}/pom.xml"
            return 1
        fi
    done
    
    echo "  ✅ ${module_name} dependencies are valid"
    return 0
}

check_test_scope() {
    local module_path="$1"
    local module_name="$2"
    
    if [ ! -f "${module_path}/pom.xml" ]; then
        return 0
    fi
    
    echo "  Checking $module_name for non-test dependencies that should have test scope..."
    
    # Define libraries that should always have test scope
    test_libraries=(
        "junit"
        "mockito"
        "assertj"
        "cucumber"
        "junit-platform"
        "junit.platform"
    )
    
    for lib in "${test_libraries[@]}"; do
        non_test_deps=$(grep -A5 -B1 "${lib}" "${module_path}/pom.xml" | grep -v "<scope>test</scope>" | grep -v "provided" | grep "<artifactId>" || true)
        
        if [ ! -z "$non_test_deps" ]; then
            echo "⚠️  Warning: Found test dependencies without test scope in ${module_name}:"
            echo "$non_test_deps"
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