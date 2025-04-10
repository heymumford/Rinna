# Script Naming Convention

## Overview

This document defines the naming convention for utility scripts in the Rinna project.

## Naming Structure

All scripts follow this naming convention:

```
rin-[module]-[submodule]-[language]-[action].sh
```

Where:
- `rin-` is a short prefix identifying all Rinna project scripts
- `[module]` represents the major component (core, cli, api, data, etc.)
- `[submodule]` defines the specific area (build, test, quality, etc.)
- `[language]` indicates scope (java, go, python, all)
- `[action]` describes the specific operation (build, check, fix, etc.)

Some modules may not use all segments if they're not applicable.

## Examples

```bash
# Build scripts
rin-build-main-all.sh          # Main build script for all components
rin-build-config-java.sh       # Configure Java build

# Quality scripts
rin-quality-check-java-style.sh # Run checkstyle on Java code
rin-quality-check-all.sh        # Run all quality checks
rin-quality-fix-java-imports.sh # Fix Java import ordering

# Test scripts
rin-test-run-all.sh             # Run all tests
rin-api-test-oauth.sh           # Test OAuth integration in API

# Infrastructure scripts
rin-infra-container-all.sh      # Container management script

# Security scripts
rin-security-check-dependencies.sh # Check dependency security

# XML scripts
rin-xml-format-all.sh           # Format all XML files
rin-xml-fix-pom-tags.sh         # Fix POM file tags
```

## Script Mapping

Below is the mapping from old script names to the new naming convention:

| Old Script | New Script |
|------------|------------|
| `build.sh` | `rin-build-main-all.sh` |
| `rinna-container.sh` | `rin-infra-container-all.sh` |
| `test-rate-limiting.sh` | `rin-api-test-rate-limiting.sh` |
| `test-oauth-integration.sh` | `rin-api-test-oauth.sh` |
| `test-failure-notify.sh` | `rin-ci-notify-test-failures.sh` |
| `quality-thresholds.sh` | `rin-quality-config-thresholds.sh` |
| `increment-build.sh` | `rin-util-version-increment.sh` |
| `update-versions.sh` | `rin-util-version-update.sh` |
| `setup-hooks.sh` | `rin-git-setup-hooks.sh` |
| `run-security-tests.sh` | `rin-security-test-all.sh` |
| `run-checks.sh` | `rin-quality-check-all.sh` |
| `rinna-tests.sh` | `rin-test-run-all.sh` |
| `checkstyle.sh` | `rin-quality-check-java-style.sh` |
| `count-warnings.sh` | `rin-quality-analyze-warnings.sh` |
| `enforcer.sh` | `rin-quality-check-maven-rules.sh` |
| `fix-imports.sh` | `rin-quality-fix-java-imports.sh` |
| `owasp.sh` | `rin-security-check-dependencies.sh` |
| `pmd.sh` | `rin-quality-check-java-pmd.sh` |
| `run-all.sh` | `rin-quality-check-all.sh` |
| `spotbugs.sh` | `rin-quality-check-java-bugs.sh` |
| `pom-n-tag-fixer.sh` | `rin-xml-fix-pom-tags.sh` |
| `xml-cleanup-scheduler.sh` | `rin-xml-schedule-cleanup.sh` |
| `xml-cleanup.sh` | `rin-xml-format-all.sh` |

## Usage

All scripts are located in the `bin` directory. The new naming convention has been implemented alongside the old scripts for backward compatibility. To use the new scripts:

```bash
./bin/rin-build-main-all.sh       # Use the main build script
./bin/rin-quality-check-all.sh    # Run all quality checks
```

Eventually, all references to the old script names will be updated and the old scripts will be deprecated.
