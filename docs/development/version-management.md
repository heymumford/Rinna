# Rinna Unified Version Management

This document explains the unified version management approach used across Java, Go, and Python components in the Rinna project.

## Overview

Rinna implements a centralized version management system that maintains version consistency across all languages and components. This approach ensures that all parts of the system share the same version information, making release management and dependency tracking much simpler.

## Key Features

1. **Single Source of Truth**: Central `version.properties` file defines the project version
2. **Cross-Language Support**: Java, Go, and Python components share the same version
3. **Clean Architecture Design**: Clear separation between core logic and language-specific adapters
4. **Verification System**: Built-in consistency checks across all languages
5. **Simplified Tooling**: One command to update versions across all components
6. **Precise Targeting**: Version updater uses explicit file-pattern mappings
7. **Self-Healing**: Automatic recovery from failed updates
8. **Autonomous Operation**: No user input required during version updates
9. **Dry-Run Support**: Preview version changes without applying them

## Architecture

The version management system follows Clean Architecture principles:

```
┌────────────────────────────────────────┐
│             version.properties         │ ◄── Single source of truth
└───────────────┬────────────────────────┘
                │
                ▼
┌────────────────────────────────────────┐
│        version-service/core            │ ◄── Core business logic
└─────┬──────────────┬──────────────┬────┘
      │              │              │
      ▼              ▼              ▼
┌──────────┐   ┌──────────┐   ┌──────────┐
│  Java    │   │   Go     │   │  Python  │ ◄── Language adapters
│ Adapter  │   │ Adapter  │   │ Adapter  │
└──────────┘   └──────────┘   └──────────┘
      │              │              │
      ▼              ▼              ▼
┌──────────┐   ┌──────────┐   ┌──────────┐
│ POM Files│   │Go Version│   │Python    │ ◄── Implementation files
│          │   │  Files   │   │ Files    │
└──────────┘   └──────────┘   └──────────┘
```

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

## Integration Points

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

## Command Line Interface

The version management system is accessible through the `bin/rin-version` utility:

```bash
# Show current version
bin/rin-version current

# Bump versions
bin/rin-version major           # 1.0.0 -> 2.0.0
bin/rin-version minor           # 1.3.0 -> 1.4.0
bin/rin-version patch           # 1.3.1 -> 1.3.2
bin/rin-version minor --dry-run # Preview without changes

# Specific version
bin/rin-version set 2.5.0
bin/rin-version set 2.5.0 --dry-run

# Verify consistency
bin/rin-version verify

# Update all files from properties
bin/rin-version update

# Create release
bin/rin-version release -m "Release notes"
```

## Verification Process

The system includes a comprehensive verification process that ensures all files have consistent versions:

1. POM files are checked for both project and parent versions
2. Go version files are verified for the correct Version constant
3. Python files and package configuration are validated
4. Documentation and README files are checked for version references

## Implementation Details

### Robust Version Updater

The core of the version management system is the robust version updater:

- `bin/robust-version-updater.sh`: Precise version updating utility
  - Uses explicit file-pattern mappings to target specific version strings
  - Self-monitors and automatically recovers from failures
  - Maintains detailed logs for troubleshooting
  - Executes autonomously without user intervention
  - Includes dry-run mode for previewing changes
  - Creates automatic backups of modified files

### Core Version Service

The version service architecture follows Clean Architecture principles:

- `version-service/core/version.go`: Core domain model and rules
- `version-service/core/registry.go`: Interface definitions
- `version-service/core/properties_registry.go`: Properties file implementation

### Language Adapters

Each language has a dedicated adapter for reading and writing version information:

- `version-service/adapters/java/maven_handler.go`: Handles Maven POM files
- `version-service/adapters/go/go_handler.go`: Manages Go version files
- `version-service/adapters/python/python_handler.go`: Updates Python version references

### Command-Line Interface

The user-facing interface is implemented as a unified command:

- `bin/rin-version`: User-friendly CLI for version management
  - Delegates to the robust version updater for actual updates
  - Provides simplified commands for common operations
  - Supports dry-run mode for all commands

## Best Practices

1. **Always use the tools**: Never manually edit version references
2. **Verify after changes**: Run `bin/rin-version verify` after any changes
3. **Follow semantic versioning**: Major for breaking changes, minor for features, patch for fixes
4. **Single responsibility**: Each version bump should have a focused purpose
5. **Include release notes**: Use the `-m` flag to document version changes

## Troubleshooting

If version inconsistencies are found:

1. Run `bin/rin-version update` to synchronize all files
2. Verify again with `bin/rin-version verify`
3. For complex issues, check the implementation-specific handlers

For more complex issues, detailed logs and backups are available:

- Logs: `/tmp/rinna-version-*.log` files contain detailed operation logs
- Backups: `/tmp/rinna-version-*/` directories contain backups of all modified files
- Error Summaries: `/tmp/rinna-version-*/error-summary.txt` provides concise error details

The robust updater automatically maintains a recovery state and will attempt to resume interrupted operations and self-heal any failed updates.

## Future Enhancements

Planned improvements to the version management system:

1. Add support for component-specific versioning for microservices
2. Integrate with CI/CD to automate version bumping
3. Enhance GitHub release creation with automated changelogs
4. Add support for more advanced versioning strategies
5. Implement pre-release and build metadata support