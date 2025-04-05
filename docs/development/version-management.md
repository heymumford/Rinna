# Rinna Version Management

This document explains the streamlined version management approach used in the Rinna project.

## Overview

Rinna uses a central properties file as the source of truth for version information, with a single script to manage versions across all files in the repository. This approach follows Clean Code principles to minimize duplication and reduce maintenance overhead.

## Source of Truth: version.properties

The project version is defined in `version.properties` in the root directory:

```properties
version=1.2.4         # Project version in semver format
lastUpdated=2025-04-04 # Date when version was last updated
releaseType=SNAPSHOT  # SNAPSHOT or RELEASE
buildNumber=1         # Build number
```

This file serves as the single source of truth for version information throughout the project, eliminating version inconsistencies.

## Version Management Tool

The `bin/rin-version` script provides commands for version management with minimal overhead:

```bash
# Display current version info
bin/rin version current

# Verify version consistency
bin/rin version verify 

# Bump major/minor/patch version
bin/rin version [major|minor|patch]

# Set to specific version
bin/rin version set 2.0.0

# Set version with custom message
bin/rin version patch -m "Fix critical bug"

# Create release
bin/rin version release

# Create git tag
bin/rin version tag

# Sync all files with version.properties
bin/rin version update
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
   bin/rin version minor -m "Added feature X"
   ```
3. The script will:
   - Update version.properties
   - Update all POM files
   - Update README references
   - Create a git commit
   - Optionally create a git tag

## Release Process

The complete release process is:

1. Ensure all changes are committed
2. Run comprehensive tests:
   ```bash
   bin/rin build verify
   ```
3. Prepare release with the integrated command:
   ```bash
   bin/rin build prepare-release
   ```
   
   This will:
   - Verify version consistency
   - Run tests with coverage
   - Package the application
   - Update the version for release
   - Create a git tag

4. Alternatively, use the version tool directly:
   ```bash
   bin/rin version release -m "Release version 1.2.4"
   ```

## Design Principles

Our version management follows these principles:

1. **Single Source of Truth**: All version information comes from one file
2. **DRY (Don't Repeat Yourself)**: Common operations are abstracted into functions
3. **Fail Fast**: Early validation and clear error messages
4. **Minimal Duplication**: Major/minor/patch changes use a single parameterized function
5. **Clear UI**: Consistent color coding for success/warnings/errors
6. **Git Integration**: Automatic commit and tag creation

## Integration with Build System

The version management system is fully integrated with the build system:

```bash
# Check version during build
bin/rin build verify

# Prepare for release (includes version management)
bin/rin build prepare-release
```

The build system accesses version information directly from version.properties using the `get_version()` function.

## Version Components

### Semantic Versioning

Rinna follows [Semantic Versioning](https://semver.org/) with these components:

- **MAJOR**: Incremented for incompatible API changes
- **MINOR**: Incremented for backward-compatible new features
- **PATCH**: Incremented for backward-compatible bug fixes

### Release Types

The version.properties file tracks two release types:

- **SNAPSHOT**: Development versions
- **RELEASE**: Released versions

### Build Numbers

Build numbers are automatically incremented for each new build during CI/CD processes.

## GitHub Integration

When the GitHub CLI (`gh`) is available, the version management system can create GitHub releases:

```bash
bin/rin version release -m "Release notes here"
```

This will:
1. Update release type to RELEASE
2. Create a git tag
3. Create a GitHub release

## Best Practices

1. Always use the `rin version` commands rather than manual edits
2. Commit version changes separately from code changes
3. Follow semantic versioning conventions:
   - MAJOR: Breaking changes
   - MINOR: New features (backward compatible)
   - PATCH: Bug fixes (backward compatible)
4. Run `verify` before releases to ensure consistency
5. Use the integrated `prepare-release` command for a complete release process
6. Include meaningful release messages with the `-m` option

## Implementation

The version management system is implemented in `bin/rin-version`, with these key functions:

- `get_version()`: Retrieve version from version.properties
- `update_version_properties()`: Update the version.properties file
- `update_pom_versions()`: Update all POM files
- `update_readme_version()`: Update README references
- `verify_consistency()`: Check version consistency across files
- `create_git_tag()`: Create git tag for the current version