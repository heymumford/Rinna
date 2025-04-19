# Rinna Build System

This guide explains Rinna's unified build system that spans multiple languages and components.

## Overview

Rinna uses a streamlined build system that follows clean code principles to minimize duplication and reduce maintenance overhead. The build system consists of two main components:

1. The primary `build.sh` script in the project root
2. The `rin` CLI utility that provides a command-line interface for various operations

## Direct Build with build.sh

For building Rinna from scratch or when the `rin` CLI is not yet available, use the root `build.sh` script directly:

```bash
# Build all components in dev mode
./build.sh

# Build specific components
./build.sh java      # Build only Java components
./build.sh python    # Build only Python components
./build.sh go        # Build only Go components

# Choose build mode
./build.sh all dev   # Development build (default)
./build.sh all test  # Run tests during build
./build.sh all prod  # Production build with optimizations
```

The `build.sh` script handles building the entire project from scratch and is the most reliable way to build the system, especially after major changes.

## The rin CLI Interface

Once you have a working installation of Rinna, the `rin` CLI utility provides a more convenient interface:

```bash
rin [category] [command] [options]
```

Categories:
- `build` - Building and testing operations
- `test` - Test execution operations
- `version` - Version management operations

**Important**: Using `rin build` will invoke the root `build.sh` script with appropriate parameters, ensuring consistency across build methods.

## Build Modes

The build system supports intuitive modes for common development workflows:

```bash
# Quick compilation without tests
./build.sh all dev
# Or with rin CLI:
rin build fast

# Build and run tests
./build.sh all test
# Or with rin CLI:
rin build test

# Build and create package
rin build package

# Full verification with coverage
rin build verify

# Prepare for release
rin build prepare-release
```

Each mode sets appropriate defaults for skip_tests, package, coverage, and fail_fast settings to enable common workflows with a single command.

## Test Categories

The build system supports various test categories:

```bash
# Basic test categories
rin test unit        # Run unit tests only
rin test component   # Run component tests
rin test integration # Run integration tests
rin test bdd         # Run BDD tests

# Domain-specific test categories
rin test domain:workflow   # Run workflow domain tests
rin test domain:release    # Run release domain tests
rin test domain:input      # Run input interface domain tests
rin test domain:api        # Run API integration domain tests
rin test domain:cli        # Run CLI integration domain tests

# Tag-based tests
rin test tag:feature-x     # Run tests with specific tag
```

## Build Options

Common options for build operations:

```bash
# Control output verbosity (with rin CLI)
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

## Version Management

The build system integrates with version management:

```bash
# Standard version management operations
rin version current        # Show current version
rin version verify         # Check consistency
rin version patch          # Bump patch version
rin version release        # Create a release

# Prepare for release (tests, version update, package)
rin build prepare-release
```

## Quality Checks

The build system includes quality check tools:

```bash
# Run all quality checks
bin/quality-check all

# Run a specific check
bin/quality-check checkstyle
bin/quality-check pmd
bin/quality-check spotbugs

# Run a check on a specific module
bin/quality-check checkstyle --module=rinna-cli
```

## Integration with Maven

The build system is a thin wrapper around Maven, configured to provide:

- Consistent command interface
- More user-friendly output formatting
- Presets for common operations
- Smart defaults for most operations
- Jacoco code coverage integration

## Advanced Features

### Watch Mode

Watch mode continuously monitors source files for changes and automatically runs tests:

```bash
rin build test --watch
```

### Release Preparation

The release preparation workflow:

```bash
rin build prepare-release
```

This verifies version consistency, runs tests with coverage, creates packages, and prepares the version for release.

## Troubleshooting Builds

If you encounter build issues:

1. **Use direct build.sh**: When experiencing problems with the `rin` CLI, always fall back to using the root `./build.sh` script directly
2. **Check logs**: Build logs are stored in the `logs/` directory
3. **Clean before rebuilding**: Try `./build.sh clean` to remove all build artifacts before rebuilding
4. **Verify environment**: Ensure all required tools are correctly installed (Java 21, Go, Python)

## Best Practices

1. For first-time builds: `./build.sh all dev`
2. For quick development iterations: `rin build fast`
3. For testing changes: `rin build test --watch`
4. For comprehensive verification: `rin build verify`
5. For preparing releases: `rin build prepare-release`
6. For CI/CD pipelines: `rin build verify --fail-fast`
7. When in doubt: Use the direct `./build.sh` script
