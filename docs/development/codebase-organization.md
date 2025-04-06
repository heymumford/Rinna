# Codebase Organization and Maintenance

This document outlines the current state of the codebase organization, cleanup procedures, and future plans for maintaining a clean and efficient project structure.

## Current State

The Rinna project has been organized into several logical modules:

1. **API Module (`api/`)**: Go-based API server for external communication
2. **Core Domain (`rinna-core/`)**: Main domain model and business logic
3. **CLI Application (`rinna-cli/`)**: Command-line interface for interacting with Rinna
4. **Version Service (`version-service/`)**: Clean Architecture service for version management
5. **Python Tools (`python/`)**: Python-based utilities and tools
6. **Configuration (`config/`)**: Project-wide configuration files

## Recent Cleanup Actions

The following cleanup actions have been performed:

1. Removed build artifacts (Maven `target/` directories)
2. Cleaned up Python cache files (`__pycache__/` and `.pyc` files)
3. Removed unnecessary backup directories (`backup/`)
4. Cleaned up temporary test directories (`.test-tmp/`)
5. Created cleanup documentation (`CLEANUP.md`)
6. Created a cleanup script (`bin/cleanup.sh`)
7. Updated the folder structure documentation (`FOLDERS.md`)

## Package Structure Migration

We are in progress of migrating to a cleaner package structure following Clean Architecture principles. The migration status is tracked in the `FOLDERS.md` document.

## Cleanup Procedures

### Regular Maintenance

For regular maintenance, run:

```bash
./bin/cleanup.sh
```

This will:
- Clean Maven build artifacts
- Remove Python compiled files and cache
- Clean Go test executables 
- Remove temporary and backup files

### Before Commits

Before committing code, developers should:

1. Run `./bin/cleanup.sh` to remove build artifacts and temporary files
2. Check for any large files that shouldn't be committed
3. Ensure all code follows the project's package structure guidelines

## CI/CD Integration

The CI/CD pipeline should incorporate these cleanup procedures:

1. Before building, clean the workspace
2. After testing, clean up test artifacts
3. Run validation to ensure no oversized artifacts are being committed

## Future Plans

### Short-term

1. Complete the package structure migration in the `src/` module
2. Migrate the API module to the new structure
3. Migrate the CLI module to the new structure
4. Remove legacy compatibility classes

### Medium-term

1. Optimize the build process to reduce artifact size
2. Implement automatic checks for package structure violations
3. Develop visualization tools for package dependencies

### Long-term

1. Complete Java 21 feature adoption
2. Consolidate duplicate code paths
3. Optimize repository size by moving large assets to external storage

## Tools and Scripts

The following tools and scripts are available for maintaining the codebase:

1. `bin/cleanup.sh`: General cleanup script
2. `bin/check-dependencies.sh`: Check for package structure violations
3. `bin/fix-imports.sh`: Fix imports after moving classes
4. `bin/migration/`: Scripts for package structure migration

## Monitoring and Enforcement

To ensure the codebase remains clean and well-organized:

1. Regular code reviews should check for adherence to structure guidelines
2. Pre-commit hooks can enforce basic cleanup tasks
3. Quarterly codebase reviews should identify areas for cleanup