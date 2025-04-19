# Quality Tools Guide

This document describes the modular quality check tools available in the Rinna project. These tools are designed to allow developers to run individual quality checks quickly and target specific modules or files.

## Quick Start

The main entry point for all quality tools is the `quality-check` script:

```bash
# Run all quality checks
./bin/quality-check all

# Run a specific check
./bin/quality-check checkstyle
./bin/quality-check pmd
./bin/quality-check spotbugs

# Run a check on a specific module
./bin/quality-check checkstyle --module=rinna-cli

# Get help for a specific tool
./bin/quality-check checkstyle --help
```

## Available Tools

### Checkstyle
Checks Java code style against project standards.

```bash
./bin/quality-check checkstyle --module=rinna-cli
./bin/quality-check checkstyle --file=src/main/java/org/rinna/cli/RinnaCli.java
./bin/quality-check checkstyle --fix # Shows common fixes
```

### Fix Imports
Automatically fixes import ordering issues using Spotless.

```bash
# First time setup (adds Spotless plugin to pom.xml)
./bin/quality-check fix-imports --add-plugin

# Fix imports in all modules
./bin/quality-check fix-imports

# Fix imports in a specific module
./bin/quality-check fix-imports --module=rinna-cli
```

### PMD
Static code analysis to find common programming flaws.

```bash
./bin/quality-check pmd --module=rinna-core
./bin/quality-check pmd --category=security # Run only security rules
./bin/quality-check pmd --file=src/main/java/org/rinna/domain/WorkItem.java
```

### SpotBugs
Bug detection tool using static analysis.

```bash
./bin/quality-check spotbugs --module=rinna-data-sqlite
./bin/quality-check spotbugs --effort=max --threshold=low # Most thorough scan
./bin/quality-check spotbugs --html # Generate HTML report
```

### OWASP Dependency Check
Security vulnerability scanning for dependencies.

```bash
./bin/quality-check owasp --module=rinna-cli
./bin/quality-check owasp --quick # Faster scan with fewer analyzers
./bin/quality-check owasp --update-only # Just update vulnerability database
./bin/quality-check owasp --async # Run in background
```

### Maven Enforcer
Enforces project rules like dependency convergence.

```bash
./bin/quality-check enforcer --module=rinna-core
./bin/quality-check enforcer --rule=dependencyConvergence
```

## Running All Checks

The `all` command runs all checks in sequence:

```bash
./bin/quality-check all --module=rinna-cli
./bin/quality-check all --skip=owasp,spotbugs # Skip slow checks
./bin/quality-check all --continue-on-error # Run all checks even if some fail
```

## Integration with Build Process

These tools are designed to work alongside the main build process. The standard build still runs all quality checks, but these tools allow you to:

1. Run individual checks during development
2. Focus on specific issues in specific modules or files
3. Run checks quickly without rebuilding the entire project
4. Debug quality issues more efficiently

## Common Workflow

A typical workflow might look like:

1. Run the full build once: `./bin/build.sh`
2. If quality checks fail, use individual tools to fix issues:
   ```bash
   ./bin/quality-check checkstyle --module=rinna-cli
   # Fix issues...
   ./bin/quality-check pmd --module=rinna-cli
   # Fix issues...
   ```
3. Re-run the full build when all issues are fixed