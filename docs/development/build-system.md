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

## Build Modes

The build system supports intuitive modes for common development workflows:

```bash
# Quick compilation without tests
rin build fast

# Build and run tests
rin build test

# Build and create package
rin build package

# Full verification with coverage
rin build verify

# Prepare for release
rin build release
```

## Build Operations

Individual build operations are also supported:

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

# Integrated release preparation
rin build prepare-release
```

## Test Categories and Domains

The build system supports various test categories and domain-specific tests:

```bash
# Basic test categories
rin build test unit        # Run unit tests only
rin build test bdd         # Run BDD tests

# Domain-specific test categories
rin build test domain:workflow   # Run workflow domain tests
rin build test domain:release    # Run release domain tests
rin build test domain:input      # Run input interface domain tests
rin build test domain:api        # Run API integration domain tests
rin build test domain:cli        # Run CLI integration domain tests

# Tag-based tests
rin build test tag:feature-x     # Run tests with specific tag
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
rin build test --skip-tests # Skip tests entirely
```

## Version Management Integration

The build system integrates with version management:

```bash
# Prepare for release (tests, version update, package)
rin build prepare-release

# Standard version management operations
rin version current        # Show current version
rin version verify         # Check consistency
rin version patch          # Bump patch version
rin version release        # Create a release
```

## Design Principles

Our build system follows these principles:

1. **Mode-Based Architecture**: Intuitive modes for common development workflows
2. **Domain Mapping**: Smart mapping between user-friendly domains and technical configuration
3. **Convention Over Configuration**: Smart defaults for most operations
4. **Unified Interface**: One entry point (`rin`) to access all commands
5. **Command-Query Separation**: Clear distinction between commands and options
6. **Composable Commands**: Commands can be combined (e.g., `rin build clean compile test`)
7. **DRY**: Common operations abstracted into shared functions

## Integration with Maven

The build system is a thin wrapper around Maven, configured to provide:

- Consistent command interface
- More user-friendly output formatting
- Presets for common operations
- Smart defaults for most operations
- Jacoco code coverage integration

## Environment Variables

The build system respects the following environment variables:

- `JAVA_HOME` - Path to Java installation
- `MAVEN_OPTS` - Options for the Maven process

## Best Practices

1. For quick development iterations: `rin build fast`
2. For testing changes: `rin build test --watch`
3. For comprehensive verification: `rin build verify`
4. For preparing releases: `rin build prepare-release`
5. For CI/CD pipelines: `rin build verify --fail-fast`