# Rinna Version Management

This document explains the version management approach used in the Rinna project.

## Version Source of Truth

The project version information is centrally managed in the `version.properties` file located in the root directory. This file contains the following properties:

```properties
version=x.y.z         # Project version in semver format (required)
lastUpdated=YYYY-MM-DD # Date when version was last updated (required)
releaseType=TYPE      # SNAPSHOT or RELEASE (required)
buildNumber=n         # Build number, incremented for each build (required)
```

All other version references throughout the codebase (POM files, README badges, etc.) should be generated or updated from this central source.

## Version Management Tool

The `bin/rin-version` script provides a comprehensive set of commands for managing the Rinna version:

### Basic Commands

```bash
# Show current version information
./bin/rin-version current

# Verify version consistency across files
./bin/rin-version verify

# Update all files to match version.properties
./bin/rin-version update

# Bump major version (x.0.0)
./bin/rin-version major

# Bump minor version (0.x.0)
./bin/rin-version minor

# Bump patch version (0.0.x)
./bin/rin-version patch

# Set specific version
./bin/rin-version set 1.2.3
```

### Release Commands

```bash
# Create a git tag for current version
./bin/rin-version tag

# Create a release from current version
./bin/rin-version release
```

### Options

```bash
# Custom release/commit message
./bin/rin-version major -m "Version 2.0.0 release"

# Show what would be done without making changes
./bin/rin-version patch -d
```

## Version Consistency

The version management system ensures consistency across various files:

1. `version.properties` - The source of truth
2. All POM files - Both project and parent versions
3. README.md - Version badge

The `verify` command checks this consistency and reports any mismatches.

## Workflow for Version Updates

Follow this workflow when updating the project version:

1. Determine the type of version change needed (major, minor, patch)
2. Run the appropriate version update command:
   ```bash
   ./bin/rin-version minor -m "Added new feature X"
   ```
3. This will:
   - Update version.properties
   - Update all POM files
   - Update README version badge
   - Create a git commit with the changes
   - Optionally create a git tag

4. For releasing:
   ```bash
   ./bin/rin-version release -m "Release notes for version x.y.z"
   ```

## Integration with Build System

The version properties can be accessed from the build system via the `version.properties` file. This enables consistent versioning across various build artifacts.

## Best Practices

1. Always use the `rin-version` commands to update versions rather than manual edits
2. Commit version changes separately from code changes for cleaner history
3. Use semantic versioning conventions:
   - MAJOR: Breaking API changes
   - MINOR: New features (backward compatible)
   - PATCH: Bug fixes (backward compatible)
4. Run `./bin/rin-version verify` before releasing to ensure consistency
