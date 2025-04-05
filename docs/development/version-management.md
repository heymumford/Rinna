# Rinna Version Management

This document explains the streamlined version management approach used in the Rinna project.

## Overview

Rinna uses a central properties file as the source of truth for version information, with a single script to manage versions across all files in the repository. This approach follows Clean Code principles to minimize duplication and reduce maintenance overhead.

## Source of Truth: version.properties

The project version is defined in `version.properties` in the root directory:

```properties
version=1.2.2         # Project version in semver format
lastUpdated=2025-04-04 # Date when version was last updated
releaseType=SNAPSHOT  # SNAPSHOT or RELEASE
buildNumber=1         # Build number
```

## Version Management Tool

The `bin/rin-version` script provides commands for version management with minimal overhead:

```bash
# Display current version info
./bin/rin-version current

# Verify version consistency
./bin/rin-version verify 

# Bump major/minor/patch version
./bin/rin-version [major|minor|patch]

# Set to specific version
./bin/rin-version set 2.0.0

# Set version with custom message
./bin/rin-version patch -m "Fix critical bug"

# Create release
./bin/rin-version release

# Create git tag
./bin/rin-version tag

# Sync all files with version.properties
./bin/rin-version update
```

## Managed Files

The version management system automatically maintains consistency across:

1. `version.properties` - Source of truth
2. All POM files - Both project and parent versions  
3. README.md - Version badge and Maven examples

## Workflow for Version Changes

To update the project version:

1. Determine the type of change (major, minor, patch)
2. Run the appropriate command:
   ```bash
   ./bin/rin-version minor -m "Added feature X"
   ```
3. The script will:
   - Update version.properties
   - Update all POM files
   - Update README references
   - Create a git commit
   - Optionally create a git tag

## Design Principles

Our version management follows these principles:

1. **Single Source of Truth**: All version information comes from one file
2. **DRY (Don't Repeat Yourself)**: Common operations are abstracted into functions
3. **Fail Fast**: Early validation and clear error messages
4. **Minimal Duplication**: Major/minor/patch changes use a single parameterized function
5. **Clear UI**: Consistent color coding for success/warnings/errors

## Integration with Build System

Build systems can access version information directly from version.properties.

## Best Practices

1. Always use the `rin-version` commands rather than manual edits
2. Commit version changes separately from code changes
3. Follow semantic versioning conventions:
   - MAJOR: Breaking changes
   - MINOR: New features (backward compatible)
   - PATCH: Bug fixes (backward compatible)
4. Run `verify` before releases to ensure consistency