#!/bin/bash

# Test Structure Validator Script
# This script validates the test structure and ensures tests are properly tagged and configured

set -e  # Exit on error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${PROJECT_ROOT}"

echo "🔍 Checking test structure in Rinna project..."

# Define test categories based on our test pyramid
categories=("unit" "component" "integration" "acceptance" "performance")

# Function to check if test classes are properly tagged
check_test_tags() {
    local module_path="$1"
    local module_name="$2"
    local test_dir="${module_path}/src/test/java"
    
    if [ ! -d "$test_dir" ]; then
        echo "  Info: No test directory found at $test_dir. Skipping tag check."
        return 0
    fi
    
    echo "  Checking $module_name for properly tagged tests..."
    
    # Find all test classes
    local test_files=$(find "$test_dir" -name "*Test.java")
    local untagged_tests=()
    
    for test_file in $test_files; do
        # Skip BDD test runners
        if [[ "$test_file" == *Runner.java ]]; then
            continue
        fi
        
        # Check for category tags
        local has_tag=0
        for category in "${categories[@]}"; do
            if grep -q "@Tag(\"$category\")" "$test_file" || grep -q "@$category" "$test_file" || grep -q "${category^}TaggedTest" "$test_file"; then
                has_tag=1
                break
            fi
        done
        
        if [ $has_tag -eq 0 ]; then
            untagged_tests+=("$test_file")
        fi
    done
    
    if [ ${#untagged_tests[@]} -gt 0 ]; then
        echo "⚠️  Warning: Found test classes without category tags in ${module_name}:"
        for test in "${untagged_tests[@]}"; do
            echo "   - $(basename "$test")"
        done
        echo "   Consider adding @Tag(\"unit\"), @Tag(\"component\"), etc. to these test classes"
    else
        echo "  ✅ All test classes in ${module_name} have appropriate category tags"
    fi
}

# Function to check for proper test resources
check_test_resources() {
    local module_path="$1"
    local module_name="$2"
    
    if [ ! -d "${module_path}/src/test" ]; then
        return 0
    fi
    
    echo "  Checking $module_name for proper test resources..."
    
    # Check for JUnit platform properties
    if [ ! -f "${module_path}/src/test/resources/junit-platform.properties" ]; then
        echo "⚠️  Warning: No junit-platform.properties found in ${module_name}"
        echo "   Consider adding src/test/resources/junit-platform.properties for consistent test configuration"
    fi
    
    # Check for Cucumber properties (if BDD tests exist)
    if [ -d "${module_path}/src/test/java/org/rinna/bdd" ] && [ ! -f "${module_path}/src/test/resources/cucumber.properties" ]; then
        echo "⚠️  Warning: BDD tests found but no cucumber.properties in ${module_name}"
        echo "   Consider adding src/test/resources/cucumber.properties for BDD test configuration"
    fi
    
    echo "  ✅ ${module_name} test resources check completed"
}

# Check POM for proper test configuration
check_test_execution() {
    local module_path="$1"
    local module_name="$2"
    
    if [ ! -f "${module_path}/pom.xml" ]; then
        return 0
    fi
    
    echo "  Checking $module_name for proper test execution configuration..."
    
    # Check for surefire plugin configuration
    if ! grep -q "<artifactId>maven-surefire-plugin</artifactId>" "${module_path}/pom.xml"; then
        echo "⚠️  Warning: No maven-surefire-plugin configuration found in ${module_name}"
        echo "   Consider adding proper surefire configuration for test execution"
    fi
    
    # Check for test category profiles
    for category in "${categories[@]}"; do
        if ! grep -q "<id>${category}-tests</id>" "${module_path}/pom.xml"; then
            echo "⚠️  Warning: No ${category}-tests profile found in ${module_name}"
            echo "   Consider adding a profile for ${category} tests"
        fi
    done
    
    echo "  ✅ ${module_name} test execution configuration check completed"
}

# Check tests in each module
check_module() {
    local module_path="$1"
    local module_name="$2"
    
    if [ ! -d "$module_path" ]; then
        echo "  Module $module_name not found. Skipping."
        return 0
    fi
    
    echo "Checking module: $module_name"
    check_test_tags "$module_path" "$module_name"
    check_test_resources "$module_path" "$module_name"
    check_test_execution "$module_path" "$module_name"
}

# Check parent project
check_module "${PROJECT_ROOT}" "parent project"

# Check each module
check_module "${PROJECT_ROOT}/rinna-core" "rinna-core"
check_module "${PROJECT_ROOT}/rinna-cli" "rinna-cli"
[ -d "${PROJECT_ROOT}/api" ] && check_module "${PROJECT_ROOT}/api" "api"

echo "✅ Test structure validation completed!"