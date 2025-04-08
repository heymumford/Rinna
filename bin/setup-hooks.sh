#!/bin/bash

# Rinna Git Hooks Setup Script
# This script sets up Git hooks for the Rinna project

set -e  # Exit on error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${PROJECT_ROOT}"

HOOKS_DIR="${PROJECT_ROOT}/.git/hooks"
SCRIPTS_DIR="${PROJECT_ROOT}/bin/hooks"

echo "======================================================================"
echo "🔧 Setting up Git hooks for the Rinna project"
echo "======================================================================"

# Make sure the hooks directory exists
mkdir -p "${HOOKS_DIR}"
mkdir -p "${SCRIPTS_DIR}"

# Create the hooks directory if it doesn't exist
if [ ! -d "${SCRIPTS_DIR}" ]; then
    mkdir -p "${SCRIPTS_DIR}"
fi

# Create pre-commit hook
cat > "${SCRIPTS_DIR}/pre-commit" << 'EOF'
#!/bin/bash

# Rinna Pre-Commit Hook
# Runs validation checks before allowing commits

# Path to the project root
PROJECT_ROOT="$(git rev-parse --show-toplevel)"

echo "Running Rinna validation checks before commit..."

# Run only PMD check for fast validation
${PROJECT_ROOT}/bin/run-pmd-check.sh
PMD_RESULT=$?

if [ $PMD_RESULT -ne 0 ]; then
    echo "❌ PMD checks failed. Please fix the issues before committing."
    exit 1
fi

# Run the remaining check scripts
${PROJECT_ROOT}/bin/run-checks.sh
CHECKS_RESULT=$?

if [ $CHECKS_RESULT -ne 0 ]; then
    echo "❌ Pre-commit checks failed. Please fix the issues before committing."
    exit 1
else
    echo "✅ Pre-commit checks passed. Proceeding with commit."
    exit 0
fi
EOF

# Create the run-pmd-check.sh script
cat > "${PROJECT_ROOT}/bin/run-pmd-check.sh" << 'EOF'
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
EOF

# Make the scripts executable
chmod +x "${SCRIPTS_DIR}/pre-commit"
chmod +x "${PROJECT_ROOT}/bin/run-pmd-check.sh"

# Install the hooks
ln -sf "${SCRIPTS_DIR}/pre-commit" "${HOOKS_DIR}/pre-commit"

# Create or update the pre-commit config file
cat > "${PROJECT_ROOT}/.pre-commit-config.yaml" << 'EOF'
# Rinna pre-commit configuration
# This file configures pre-commit hooks for the Rinna project

# Default hook execution order
default_stages: [commit]

# Hooks configuration
hooks:
  - id: pmd-check
    name: PMD Code Quality Check
    entry: bin/run-pmd-check.sh
    language: script
    pass_filenames: false
    
  - id: clean-architecture
    name: Clean Architecture Validation
    entry: bin/checks/check-clean-architecture.sh
    language: script
    pass_filenames: false
    
  - id: dependency-validation
    name: Dependency Validation
    entry: bin/checks/dependency-validator.sh
    language: script
    pass_filenames: false
    
  - id: test-structure
    name: Test Structure Validation
    entry: bin/checks/test-structure-validator.sh
    language: script
    pass_filenames: false
EOF

echo "======================================================================"
echo "✅ Git hooks setup completed successfully!"
echo "The following hooks have been installed:"
echo "- pre-commit: Runs code quality checks before committing"
echo ""
echo "To bypass hooks when needed, use: git commit --no-verify"
echo "======================================================================"