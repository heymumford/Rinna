# Linting and Build Procedure for IntelliJ on Mac Studio

This document summarizes the implementation of an effective linting and build procedure for IntelliJ IDEA on a Mac Studio for the Rinna polyglot project.

## Overview

The Rinna project is a polyglot application with components written in:
- Java 21+ (core domain logic)
- Go 1.23+ (API server)
- Python 3.8+ (utilities and scripts)

To ensure code quality and consistency across all components, a comprehensive linting and build procedure has been implemented that integrates with IntelliJ IDEA on Mac Studio.

## Implementation Summary

The following components have been implemented or configured:

### 1. IntelliJ IDEA Configuration

A pre-configured IntelliJ setup file has been created at `.idea/rinna-intellij-setup.xml` that includes:
- Linting configurations for all languages
- Build configurations for all components
- Test configurations for all components
- Compound configurations for building and testing everything at once

### 2. Language-Specific Linting

#### Java
- Checkstyle configuration is used from `config/checkstyle/checkstyle.xml`
- Integrated with Maven's verify phase for quality checks
- Configured to run automatically before commits

#### Go
- golangci-lint configuration added at `api/.golangci.yml`
- Comprehensive linter settings for Go code quality
- Integrated with the build process

#### Python
- Multiple linters configured in `pyproject.toml`:
  - mypy for static type checking
  - ruff for fast linting
  - black for code formatting
  - isort for import sorting
  - bandit for security checks
- Integrated with the build process

### 3. Build Process Integration

The build orchestrator script (`utils/build-orchestrator.sh`) has been updated to:
- Run linting for Go code using golangci-lint
- Run linting for Python code using mypy, ruff, black, and isort
- Skip linting when the `--skip-quality` flag is used
- Provide clear warnings when linting tools are not installed

### 4. Documentation

Comprehensive documentation has been created:
- `docs/development/intellij-mac-setup.md` - Detailed guide for setting up IntelliJ on Mac Studio
- Updated `docs/development/environment-setup.md` to reference the new setup guide
- This summary document

## Usage

### Setting Up IntelliJ IDEA

Follow the detailed instructions in [IntelliJ Mac Setup Guide](intellij-mac-setup.md) to set up IntelliJ IDEA on your Mac Studio with all the necessary configurations.

### Running Builds with Linting

#### From IntelliJ IDEA
1. Use the pre-configured run configurations:
   - "Build All" to build all components
   - "Test All" to run all tests
   - Individual component builds and tests are also available

#### From Command Line
```bash
# Build with linting (default)
./build.sh

# Skip quality checks (including linting)
./build.sh --skip-quality

# Build specific components
./build.sh --components=java,go,python
```

## Mac Studio Optimizations

The setup includes specific optimizations for Mac Studio:
- Memory settings adjusted for M1/M2 processors
- Parallel builds enabled for better performance
- Indexing optimizations to reduce resource usage
- Apple Silicon optimized version of IntelliJ IDEA recommended

## Maintenance

To maintain this setup:

1. **Adding New Linters**:
   - Update the appropriate configuration file:
     - Java: `config/checkstyle/checkstyle.xml`
     - Go: `api/.golangci.yml`
     - Python: `pyproject.toml`
   - Update the build orchestrator script if needed
   - Update the IntelliJ configuration file

2. **Updating Linter Versions**:
   - Update the version in the appropriate configuration file
   - Test the new version to ensure compatibility

3. **Adding New Languages**:
   - Add appropriate linting configuration
   - Update the build orchestrator script
   - Update the IntelliJ configuration file
   - Update the documentation

## Conclusion

This implementation provides a comprehensive linting and build procedure for the Rinna polyglot project on IntelliJ IDEA and Mac Studio. It ensures code quality across all components while maintaining a smooth developer experience.

The solution is:
- **Integrated**: Works seamlessly with IntelliJ IDEA
- **Comprehensive**: Covers all languages in the project
- **Flexible**: Can be run from IDE or command line
- **Optimized**: Specifically tuned for Mac Studio performance
- **Maintainable**: Well-documented and easy to update