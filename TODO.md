# Rinna Project TODO List

This document provides a consolidated view of todos and next steps for the Rinna project.

## High Priority

### CLI Module Fixes

- [x] Fix type incompatibilities in CLI module:
  - [x] Update imports from `org.rinna.domain` to `org.rinna.domain.model`
  - [x] Fix `WorkflowState` enum incompatibilities
  - [x] Update `ServiceManager` to use proper constructor patterns
  - [x] Fix `BacklogCommand` and `BugCommand` implementations
  - [ ] Re-enable module in parent pom.xml

### Documentation

- [x] Remove redundant documentation files:
  - [x] Archive TEST_MIGRATION_PLAN.md (replaced by UNIFIED_TEST_APPROACH.md)
  - [x] Archive UNIFIED_TEST_NOMENCLATURE.md (incorporated into UNIFIED_TEST_APPROACH.md)
  - [x] Update cross-references in documentation

## Medium Priority

### New Feature Implementation

The following features need to be implemented:

- [ ] Implement QueryService for developer-focused filtering
- [ ] Create SQLite persistence module (rinna-data-sqlite)
- [ ] Complete Main application entry point
- [ ] Add multi-language logging support

### Package Structure Finalization

- [ ] Remove compatibility wrapper classes after CLI module is fixed
- [ ] Add architecture validation checks to enforce package structure

## Low Priority

### Technical Debt

- [ ] Fix Python setuptools installation issue
- [ ] Cleanup CI pipeline configuration to reflect new module structure
- [ ] Address warnings in maven-assembly-plugin configuration
- [ ] Add more comprehensive Javadoc documentation

## Completed Items

The following items have been completed:

- [x] Package structure migration for rinna-core
- [x] Package structure migration for src (main)
- [x] Package structure migration for API
- [x] Test structure migration to testing pyramid
- [x] Version service migration to Clean Architecture
- [x] Documentation consolidation
- [x] Update CHANGELOG.md with accurate migration status

## References

- [MIGRATION_STATUS.md](docs/development/MIGRATION_STATUS.md) - Detailed migration status
- [UNIFIED_TEST_APPROACH.md](docs/testing/UNIFIED_TEST_APPROACH.md) - Testing approach
- [rinna-cli/README.md](rinna-cli/README.md) - CLI module status and fix instructions