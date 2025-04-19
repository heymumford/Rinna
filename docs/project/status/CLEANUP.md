# Rinna Codebase Cleanup Guide

This document provides guidance for maintaining a clean codebase in the Rinna project.

## Documentation Cleanup Progress

### Completed Tasks
- ✅ Removed temporary documentation files (e.g., tmp-docs-restructure-plan.md)
- ✅ Removed `.bak` files from source code
- ✅ Consolidated implementation summaries into structured directory
- ✅ Consolidated implementation plans into structured directory
- ✅ Created centralized index files for implementation documentation

### Pending Tasks
- Standardize README files across the codebase
- Consolidate script directories (see [Script Organization](./implementation-summaries/script-organization.md))
- Clean up duplicate script files between `/scripts/`, `/utils/`, and `/bin/`
- Create standard templates for documentation
- Improve API documentation consistency
- Consolidate test documentation

## Regular Cleaning Tasks

### Java/Maven Artifacts

```bash
# Clean all Maven artifacts
mvn clean

# Clean specific modules
mvn clean -pl rinna-core
mvn clean -pl rinna-cli

# Remove all target directories
find . -name "target" -type d -exec rm -rf {} +
```

### Python Artifacts

```bash
# Remove Python compiled files
find . -name "*.pyc" -delete
find . -name "__pycache__" -type d -exec rm -rf {} +
find . -name "*.egg-info" -type d -exec rm -rf {} +

# Clean up virtual environments (if needed)
# Warning: only run when you want to recreate venvs
# rm -rf .venv
```

### Go Artifacts

```bash
# Remove Go test executables
find ./api -name "*.test" -delete

# Remove compiled Go binaries (only if needed)
find ./api/cmd -name "healthcheck" -type f -perm -100 -delete
find ./api/cmd -name "rinnasrv" -type f -perm -100 -delete
```

### Temporary and Test Files

```bash
# Remove test temp directories
rm -rf .test-tmp

# Remove test log files
find . -name "*.log" -not -path "*/.git/*" -delete
```

## Before Committing

1. Run `mvn clean` to remove build artifacts
2. Clear Python cache with `find . -name "__pycache__" -type d -exec rm -rf {} +`
3. Remove any backup files with `find . -name "*.bak" -o -name "*.backup" -o -name "*~" -delete`
4. Check for large files with `find . -type f -size +5M -not -path "*/\.*"`

## CI Cleanup

The CI pipeline automatically cleans up artifacts, but for local development, it's good practice to regularly run cleanup tasks.

## Recommended Directory Structure

```
Rinna/
├── api/              # Go API server
├── bin/              # CLI scripts and tools
├── config/           # Configuration files
├── docs/             # Documentation
├── python/           # Python modules
├── rinna-cli/        # Java CLI application
├── rinna-core/       # Core Java domain model
├── src/              # Main application source
├── utils/            # Utility scripts
└── version-service/  # Version management service
```

Always maintain this structure and avoid creating temporary directories in the root folder.