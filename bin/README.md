# Rinna Command Line Utilities

This directory contains command line utilities for the Rinna workflow management system.

## Available Scripts

### rin - Main CLI Tool

The `rin` script is the primary CLI tool for interacting with the Rinna system.

```bash
# Build commands
./bin/rin build       # Build the project
./bin/rin clean       # Clean the project
./bin/rin test        # Run all tests
./bin/rin all         # Clean, build, and test

# Specialized test commands
./bin/rin test unit       # Run unit tests only
./bin/rin test bdd        # Run all BDD tests
./bin/rin test workflow   # Run workflow BDD tests only 
./bin/rin test release    # Run release BDD tests only
./bin/rin test input      # Run input interface BDD tests only
./bin/rin test api        # Run API integration tests only
./bin/rin test tag:<name> # Run tests with a specific tag (e.g., tag:client)

# Version management
./bin/rin version current       # Show current version information
./bin/rin version major         # Bump major version (x.0.0)
./bin/rin version minor         # Bump minor version (0.x.0)
./bin/rin version patch         # Bump patch version (0.0.x)
./bin/rin version set <version> # Set to specific version (e.g., 1.2.3)
./bin/rin version release       # Create a release from current version
./bin/rin version tag           # Create a git tag for current version
```

### run-tests.sh - Advanced Test Runner

The `run-tests.sh` script provides more fine-grained control over test execution with advanced options.

```bash
# Basic usage
./bin/run-tests.sh all      # Run all tests
./bin/run-tests.sh unit     # Run only unit tests
./bin/run-tests.sh bdd      # Run all BDD tests

# Advanced options
./bin/run-tests.sh -p bdd   # Run BDD tests in parallel mode
./bin/run-tests.sh -v unit  # Run unit tests with verbose output
./bin/run-tests.sh -h       # Show help message

# Tag-based testing
./bin/run-tests.sh tag:json-api    # Run tests tagged with @json-api
./bin/run-tests.sh tag:webhook     # Run tests tagged with @webhook
./bin/run-tests.sh tag:client      # Run tests tagged with @client
```

### Other Utilities

- **add-copyright-headers.sh** - Adds copyright headers to source files
- **add-readme-copyright-header.sh** - Adds copyright headers to README files
- **add-source-copyright-headers.sh** - Adds copyright headers to Java source files
- **custom-maven.sh** - Sets custom Maven options like --enable-preview
- **rinnasrv** - Go API server executable
- **robust-version-updater.sh** - Enhanced version updating utility with precise pattern matching

### Version Management

The `robust-version-updater.sh` provides enhanced version management for precise pattern matching:

```bash
# Preview version changes
./bin/robust-version-updater.sh --from 1.2.3 --to 1.3.0 --dry-run --verbose

# Apply version changes with safety checks
./bin/robust-version-updater.sh --from 1.2.3 --to 1.3.0 --verbose

# Update all files to match version.properties
./bin/robust-version-updater.sh --to 1.3.0 --verbose

# See all options
./bin/robust-version-updater.sh --help
```

Features:
- File-type specific pattern matching
- Preserves external dependency version strings
- Safe preview mode with confirmation
- Automatic backup of modified files
- Detailed logging and verification

## Usage Notes

1. Most scripts must be run from the project root directory
2. Some scripts (like rin) may require executable permissions
3. The scripts use system-installed Maven (version 3.8+)

For detailed information about testing, see the [Testing Guide](/docs/development/testing.md).