#!/bin/bash

# Maven Architecture Validation
# This script runs architecture validation checks before a build

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${PROJECT_ROOT}"

echo "======================================================================"
echo "🏛️ Maven Architecture Validation"
echo "======================================================================"

# Run the validation checks script
"${PROJECT_ROOT}/bin/run-checks.sh"
result=$?

if [ $result -ne 0 ]; then
    echo "❌ Architecture validation failed!"
    echo "Please fix violations before continuing the build."
    exit $result
else
    echo "✅ Architecture validation passed. Continuing build."
fi

exit 0