# Rinna CI Workflow

This document explains the Continuous Integration workflow used in the Rinna project.

## Overview

The Rinna project uses GitHub Actions for CI. The main workflow is defined in `.github/workflows/rin-ci.yml`.

## Workflow Details

The workflow `rin-ci` includes basic steps to:

1. Build and test the Java Core module
2. Build and test the Go API server

## Running Locally

You can test the GitHub Actions workflow locally using [act](https://github.com/nektos/act).

```bash
# Install act (if not already installed)
# Ubuntu: curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Run the workflow
cd /path/to/rinna
act -j rin-ci

# Run in dry-run mode (no actual execution)
act -j rin-ci -n
```

## Current CI Steps

The workflow includes:

- **Environment Setup**:
  - Java 21 (Temurin distribution)
  - Go 1.22

- **Java Core**:
  - Compile the core module
  - Run basic tests focusing on the `DocumentServiceTest`

- **Go API**:
  - Build the API server
  - Run tests for the health package

## Troubleshooting

If you encounter issues with the CI:

1. **For Java issues**: Check if you're using Java 21 locally (`java -version`)
2. **For Go issues**: Verify Go modules are properly initialized (`go mod tidy`)
3. **For Docker issues**: Make sure you have Docker installed and running

## Future Improvements

Planned improvements for the CI workflow:

1. Add code coverage reporting
2. Add static code analysis
3. Add integration tests
4. Add deployment steps for staging environments
5. Add automatic versioning