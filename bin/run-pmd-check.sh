#!/bin/bash

# Run only PMD check to validate basic code quality 
# This is a faster check for pre-commit hooks

set -e  # Exit on error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${PROJECT_ROOT}"

echo "======================================================================"
echo "🔍 Running PMD code quality check"
echo "======================================================================"

# Run PMD check on rinna-cli module
cd "${PROJECT_ROOT}/rinna-cli"
mvn -q -P local-quality pmd:check
PMD_RESULT=$?

if [ $PMD_RESULT -ne 0 ]; then
    echo "❌ PMD check failed. Please fix the issues before committing."
    exit 1
else
    echo "✅ PMD check passed."
    exit 0
fi
# Added test comment
