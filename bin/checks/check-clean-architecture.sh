#!/bin/bash

# Clean Architecture Validator Script
# Validates that the codebase adheres to Clean Architecture principles

set -e  # Exit on error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${PROJECT_ROOT}"

echo "🔍 Checking Clean Architecture principles in Rinna project..."

# Define package layers
DOMAIN_LAYER="org.rinna.domain"
USECASE_LAYER="org.rinna.usecase"
ADAPTER_LAYER="org.rinna.adapter"
INFRASTRUCTURE_LAYER="org.rinna.persistence org.rinna.repository"

# Check forbidden imports
check_forbidden_imports() {
    local from_package="$1"
    local forbidden_packages=($2)
    local src_dir="$3"
    
    echo "  Checking imports from $from_package..."
    
    # Find all Java files in the specified package
    local java_files=$(find "$src_dir" -path "*/$from_package/**/*.java" -type f)
    
    if [ -z "$java_files" ]; then
        echo "  ⚠️ No Java files found in $from_package package"
        return 0
    fi
    
    local violations=0
    
    for forbidden in "${forbidden_packages[@]}"; do
        for file in $java_files; do
            local imports=$(grep "^import $forbidden" "$file" || true)
            if [ ! -z "$imports" ]; then
                echo "❌ VIOLATION: $file imports from forbidden package $forbidden"
                echo "   $imports"
                violations=$((violations + 1))
            fi
        done
    done
    
    if [ $violations -eq 0 ]; then
        echo "  ✅ No forbidden imports found in $from_package"
    else
        echo "  ❌ Found $violations clean architecture violations in $from_package"
        return 1
    fi
}

# Check each module
check_module() {
    local module_path="$1"
    local module_name="$2"
    
    if [ ! -d "$module_path" ]; then
        echo "  Module $module_name not found. Skipping."
        return 0
    fi
    
    local src_dir="${module_path}/src/main/java"
    if [ ! -d "$src_dir" ]; then
        echo "  No source directory found for $module_name. Skipping."
        return 0
    fi
    
    echo "Checking module: $module_name"
    
    # Domain layer should not depend on any other layer
    check_forbidden_imports "$DOMAIN_LAYER" "$USECASE_LAYER $ADAPTER_LAYER $INFRASTRUCTURE_LAYER" "$src_dir"
    
    # Usecase layer should not depend on adapter or infrastructure layers
    check_forbidden_imports "$USECASE_LAYER" "$ADAPTER_LAYER $INFRASTRUCTURE_LAYER" "$src_dir"
    
    # Adapter layer should not depend on infrastructure layer
    check_forbidden_imports "$ADAPTER_LAYER" "$INFRASTRUCTURE_LAYER" "$src_dir"
}

# Check each module
check_module "${PROJECT_ROOT}/rinna-core" "rinna-core"
check_module "${PROJECT_ROOT}/rinna-cli" "rinna-cli"
[ -d "${PROJECT_ROOT}/api" ] && check_module "${PROJECT_ROOT}/api" "api"

echo "✅ Clean Architecture validation completed!"