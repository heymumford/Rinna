#!/bin/bash

# Master Check Runner
# Runs all validation checks for the Rinna project

set -e  # Exit on error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${PROJECT_ROOT}"

CHECKS_DIR="${PROJECT_ROOT}/bin/checks"

# Check if the checks directory exists
if [ ! -d "$CHECKS_DIR" ]; then
    echo "❌ ERROR: Checks directory not found at $CHECKS_DIR"
    exit 1
fi

# Make all check scripts executable
chmod +x ${CHECKS_DIR}/*.sh

echo "======================================================================"
echo "🔍 Running Rinna Project Validation Checks"
echo "======================================================================"

run_check() {
    local script="$1"
    echo "----------------------------------------------------------------------"
    echo "▶️ Running check: $(basename "$script")"
    echo "----------------------------------------------------------------------"
    
    if [ -x "$script" ]; then
        "$script"
        local result=$?
        if [ $result -ne 0 ]; then
            echo "❌ Check $(basename "$script") failed with exit code $result"
            return $result
        fi
        echo "✅ Check $(basename "$script") passed"
    else
        echo "❌ Check script $script is not executable"
        return 1
    fi
}

# Run all check scripts
failed_checks=0

for check_script in ${CHECKS_DIR}/*.sh; do
    run_check "$check_script" || failed_checks=$((failed_checks + 1))
done

echo "======================================================================"
if [ $failed_checks -eq 0 ]; then
    echo "✅ All checks passed successfully!"
else
    echo "❌ $failed_checks checks failed. Please fix the issues before committing."
    exit 1
fi
echo "======================================================================"