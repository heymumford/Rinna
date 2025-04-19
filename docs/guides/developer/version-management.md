# Rinna Version Management

This guide explains Rinna's unified version management approach across all languages and components.

## Overview

Rinna implements a centralized version management system that maintains version consistency across Java, Go, and Python components. This ensures all parts of the system share the same version information, simplifying release management and dependency tracking.

## Key Features

1. **Single Source of Truth**: Central `version.properties` file defines the project version
2. **Cross-Language Support**: Java, Go, and Python components share the same version
3. **Clean Architecture Design**: Clear separation between core logic and language-specific adapters
4. **Verification System**: Built-in consistency checks across all languages
5. **XMLStarlet Integration**: XML files are handled through dedicated XMLStarlet tools

## Version Information

The centralized `version.properties` file contains:

```properties
# Core version information
version=1.3.13
version.major=1
version.minor=3
version.patch=13
version.qualifier=
version.full=1.3.13

# Release information
lastUpdated=2025-04-06
releaseType=RELEASE
buildNumber=1

# Build information
build.timestamp=2025-04-06T03:47:28Z
build.git.commit=runtime
```

## Command Line Interface

The version management functionality is accessible through the `rin-version` utility:

```bash
# Show current version
bin/rin-version current

# Verify consistency
bin/rin-version verify

# Bump versions
bin/rin-version major           # 1.0.0 -> 2.0.0
bin/rin-version minor           # 1.3.0 -> 1.4.0
bin/rin-version patch           # 1.3.1 -> 1.3.2
bin/rin-version patch --dry-run # Preview without changes

# Set specific version
bin/rin-version set 2.5.0
bin/rin-version set 2.5.0 --dry-run

# Update all files from properties
bin/rin-version update

# Create release
bin/rin-version release -m "Release notes"
```

## Integration Points

The version management system integrates with all language components:

### Java/Maven

- All POM files with `org.rinna` groupId are updated automatically
- Project and parent versions are maintained consistently
- Maven builds access version information directly from properties

### Go

- Version constants in `api/pkg/health/version.go` and other Go files
- BuildTime and CommitSHA are updated during version changes
- Configuration files are automatically updated

### Python

- `pyproject.toml` version field is kept in sync
- Python modules read directly from `version.properties` at runtime
- Virtual environment version file is maintained

## Verification Process

The system includes a comprehensive verification process that ensures all files have consistent versions:

1. POM files are checked for both project and parent versions
2. Go version files are verified for the correct Version constant
3. Python files and package configuration are validated
4. Documentation and README files are checked for version references

## Implementation Details

### Centralized Version Manager

The core of the version management system is the unified version manager:

- `bin/version-manager.sh`: Centralized version management tool
  - Provides a single interface for all version operations
  - Uses XMLStarlet for precise XML/POM file manipulation
  - Uses explicit file-pattern mappings to target specific version strings
  - Self-monitors and automatically recovers from failures
  - Maintains detailed logs for troubleshooting

### XML Tools Library

A dedicated library for XML manipulation ensures that all POM files are handled correctly:

- `bin/xml-tools.sh`: XML manipulation utilities
  - Uses XMLStarlet for all XML operations
  - Provides functions for querying, updating, and verifying XML content
  - Ensures consistency across all POM files

## Best Practices

1. **Always use the tools**: Never manually edit version references
2. **Verify after changes**: Run `bin/rin-version verify` after any changes
3. **Follow semantic versioning**: Major for breaking changes, minor for features, patch for fixes
4. **Single responsibility**: Each version bump should have a focused purpose
5. **Include release notes**: Use the `-m` flag to document version changes

## Troubleshooting

If version inconsistencies are found:

1. Run `bin/version-manager.sh set [current-version]` to synchronize all files
2. Verify again with `bin/version-manager.sh verify`
3. For complex issues, check the logs in `backup/version-*/version-manager.log`
