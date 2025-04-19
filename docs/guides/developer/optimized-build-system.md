# Optimized Build System

This document describes the optimized build system for the Rinna project. The system is designed for faster builds with clear output and better control over the build process.

## Overview

The optimized build system provides several key improvements over the previous system:

- **Faster Builds**: Reduces build time by eliminating unnecessary checks and optimizing core build operations
- **Selective Phases**: Run only the build phases you need (build, test, quality, package)
- **Component Selection**: Build only the components you're working on (Java, Go, Python)
- **Clear Output**: Well-formatted, color-coded output that highlights important information
- **Independent Quality Checks**: Run quality tools individually to focus on specific issues
- **Fix Mode**: Several quality tools support auto-fixing issues

## Key Scripts

The optimized build system includes the following key scripts:

1. `bin/rin-build-optimize.sh` - Main build orchestrator with component and phase selection
2. `bin/rin-quality-check-all.sh` - Unified quality check system
3. `bin/quality-tools/run-all.sh` - Implementation of quality checks
4. `bin/rin-build-main-all.sh` - Reduced complexity build script (50% smaller than the original)

## Usage

### Optimized Build

The optimized build script provides fine-grained control over the build process:

```bash
# Standard build (build, test, package)
bin/rin-build-optimize.sh

# Build only Java components
bin/rin-build-optimize.sh --components=java

# Build and test only (skip packaging)
bin/rin-build-optimize.sh --only=build,test

# Quick build (skip tests)
bin/rin-build-optimize.sh --skip-tests

# Run quality checks only
bin/rin-build-optimize.sh --only=quality

# Run quality checks with auto-fix
bin/rin-build-optimize.sh --only=quality --fix-quality
```

### Quality Check System

The quality check system allows focusing on specific quality issues:

```bash
# Run all quality checks
bin/rin-quality-check-all.sh

# Run only checkstyle
bin/rin-quality-check-all.sh checkstyle

# Run PMD on core module only 
bin/rin-quality-check-all.sh pmd core

# Run checkstyle with auto-fix
bin/rin-quality-check-all.sh --fix checkstyle

# Continue running all checks even if some fail
bin/rin-quality-check-all.sh --continue
```

Available quality tools:
- `checkstyle` - Java code style checks
- `pmd` - Java static code analysis
- `spotbugs` - Java bug detection
- `enforcer` - Maven dependency rules check
- `owasp` - Security vulnerability scan

## Advanced Options

### Phase Options

The build system is divided into the following phases:

- `build` - Compile all source code
- `test` - Run the test suite
- `quality` - Run quality checks (off by default)
- `package` - Create distribution packages

You can control which phases to run using the `--only` or `--skip` flags:

```bash
# Run only the build phase
bin/rin-build-optimize.sh --only=build

# Skip the test phase
bin/rin-build-optimize.sh --skip=test
```

### Component Options

You can build specific components using the `--components` flag:

```bash
# Build only Java components
bin/rin-build-optimize.sh --components=java

# Build Java and Go, but not Python
bin/rin-build-optimize.sh --components=java,go
```

### Other Options

- `--verbose` - Show detailed output
- `--no-parallel` - Disable parallel builds
- `--skip-tests` - Skip running tests
- `--fix-quality` - Try to auto-fix quality issues

## Benefits

The optimized build system provides several benefits:

1. **Faster Build Times**: By focusing only on necessary components and phases
2. **Developer Productivity**: Clearer output and targeted quality checks
3. **Iterative Development**: Quick cycles for build-test-fix iterations
4. **Reduced Complexity**: Simpler, more maintainable build scripts
5. **Better Quality Management**: Deal with quality issues one tool at a time

## Log Files

All build output is logged to the following locations:

- Build logs: `logs/build/build-YYYYMMDD-HHMMSS.log`
- Quality check logs: `logs/quality/TOOL-MODULE-YYYYMMDD-HHMMSS.log`

These logs can be useful for debugging build issues and analyzing quality check failures.