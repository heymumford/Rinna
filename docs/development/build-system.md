# Rinna Build System

This document explains the streamlined build system used in the Rinna project.

## Overview

Rinna uses a unified build system that follows clean code principles to minimize duplication and reduce maintenance overhead. The system consists of a small set of scripts that handle building, testing, and other development workflows.

## Command Structure

All commands are accessed through the main `rin` CLI utility, which provides a consistent interface:

```bash
rin [category] [command] [options]
```

Categories:
- `build` - Building and testing operations
- `version` - Version management operations

## Build Operations

Build operations are handled by the `rin-build` script, accessed via `rin build`:

```bash
# Clean the project
rin build clean

# Compile source code
rin build compile

# Run tests (all tests by default)
rin build test

# Package the application
rin build package

# Clean, compile, and test
rin build all
```

## Test Categories

The build system supports various test categories:

```bash
# Run unit tests only
rin build test unit

# Run BDD tests
rin build test bdd

# Run specific test categories
rin build test workflow
rin build test release
rin build test input
rin build test api
rin build test cli

# Run tests with specific tag
rin build test tag:feature-x
```

## Options

Common options for build operations:

```bash
# Control output verbosity
rin build test --verbose   # Show detailed output
rin build test --terse     # Show minimal output (default)
rin build test --errors    # Show only errors

# Test execution options
rin build test --parallel  # Run tests in parallel
rin build test --fail-fast # Stop at first failure
rin build test --coverage  # Generate coverage report
rin build test --watch     # Monitor and run tests on changes
```

## Version Management

Version operations are handled by the `rin-version` script:

```bash
# Show current version info
rin version current

# Verify version consistency
rin version verify

# Bump versions
rin version major
rin version minor
rin version patch

# Set to specific version
rin version set 2.0.0

# Create release
rin version release

# Update all files with version.properties
rin version update
```

## Design Principles

Our build system follows these principles:

1. **Unified Interface**: One entry point (`rin`) to access all commands
2. **Composable Commands**: Commands can be combined (e.g., `rin build clean compile test`)
3. **DRY**: Common operations abstracted into shared functions
4. **Progressive Disclosure**: Simple commands for common operations, options for advanced use
5. **Consistent Output**: Standardized formatting and color-coding
6. **Backward Compatibility**: Support for legacy command patterns

## Integration with Maven

The build system is a thin wrapper around Maven, configured to provide:

- Consistent command interface
- More user-friendly output formatting
- Presets for common operations
- Smart defaults for most operations

## Environment Variables

The build system respects the following environment variables:

- `JAVA_HOME` - Path to Java installation
- `MAVEN_OPTS` - Options for the Maven process

## Best Practices

1. For simple builds: `rin build all`
2. For development: `rin build test --watch`
3. For CI/CD: `rin build all --fail-fast`
4. For release preparation: `rin version verify` then `rin version patch`