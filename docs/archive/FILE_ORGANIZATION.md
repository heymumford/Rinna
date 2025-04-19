# File Organization Standards

This document outlines the standards for file organization in the Rinna project.

## Core Principles

1. **Files belong in their logical locations**
   - Every file should be placed in the appropriate folder based on its function
   - Root directory should contain only project-level configuration and documentation
   - Implementation-specific files should be in their respective module directories

2. **No backward compatibility symlinks**
   - When files are moved, references must be updated
   - Never create symlinks to maintain backward compatibility
   - Tests should fail if file locations break functionality
   - Fix the references properly, not the symptoms

3. **Documentation organization**
   - Documentation is organized in the `docs` directory with appropriate subdirectories
   - Module-specific documentation should reside in the module directory
   - Cross-module documentation belongs in the `docs` directory
   - Standards and guidelines belong in the `docs/standards` directory

4. **Path references in documentation**
   - Use relative paths in documentation (e.g., `docs/testing/README.md`)
   - Never use absolute paths (e.g., `/home/user/project/docs/testing/README.md`)
   - Path references should be accurate and maintained as files move

5. **Directory structure**
   - Module directories should follow a consistent structure
   - Java code follows Maven standard directory layout
   - Go code follows standard Go project layout
   - Python code follows standard Python project layout
   - Shell scripts and utilities belong in the `bin` directory

## Specific Directory Usage

| Directory | Purpose |
|-----------|---------|
| `bin` | Command-line utilities and scripts |
| `config` | Configuration files for various tools |
| `docs` | Project documentation by topic |
| `logs` | Application logs (not checked into version control) |
| `build` | Build artifacts (not checked into version control) |
| `backup` | Backup files (not checked into version control) |
| `src` | Main application source code |
| `rinna-core` | Core domain module |
| `rinna-cli` | Command-line interface module |
| `rinna-data-sqlite` | SQLite persistence module |
| `api` | Go API server |
| `python` | Python utilities and modules |
| `target` | Maven build output (not checked into version control) |

## Implementation Files

1. **Java Files**
   - Domain entity interfaces in `rinna-core/src/main/java/org/rinna/domain`
   - Use case interfaces in `rinna-core/src/main/java/org/rinna/usecase`
   - Repository interfaces in `rinna-core/src/main/java/org/rinna/repository`
   - Adapter implementations in `rinna-core/src/main/java/org/rinna/adapter`
   - CLI commands in `rinna-cli/src/main/java/org/rinna/cli/command`

2. **Go Files**
   - API handlers in `api/internal/handlers`
   - API middleware in `api/internal/middleware`
   - Models in `api/internal/models`
   - Configuration in `api/internal/config`
   - Health check utilities in `api/pkg/health`

3. **Python Files**
   - Module code in `python/rinna`
   - Tests in `python/tests`
   - Scripts in `bin` (with .py extension)

## Documentation Files

1. **Markdown Files**
   - User guide in `docs/user-guide`
   - Developer documentation in `docs/development`
   - Architecture documentation in `docs/architecture`
   - Implementation documentation in `docs/implementation`
   - Testing documentation in `docs/testing`
   - Standards documentation in `docs/standards`

2. **Project-Level Files**
   - README.md - Project overview
   - CONTRIBUTING.md - Contribution guidelines
   - LICENSE - Project license
   - CHANGELOG.md - Version history
   - DEVELOPER.md - Developer guide entry point

## Testing Files

1. **Java Tests**
   - Unit tests in `*/src/test/java/org/rinna/unit`
   - Component tests in `*/src/test/java/org/rinna/component`
   - Integration tests in `*/src/test/java/org/rinna/integration`
   - Acceptance tests in `*/src/test/java/org/rinna/acceptance`
   - Performance tests in `*/src/test/java/org/rinna/performance`
   - BDD feature files in `*/src/test/resources/features`

2. **Go Tests**
   - Unit tests in `api/test/unit`
   - Integration tests in `api/test/integration`
   - Component tests in `api/test/component`
   - Acceptance tests in `api/test/acceptance`
   - Performance tests in `api/test/performance`

3. **Python Tests**
   - Tests in `python/tests` with appropriate subdirectories

## Conclusion

Following these file organization standards ensures a consistent, maintainable codebase. When migrating or adding files, ensure they are placed in the appropriate location according to these guidelines. If you're uncertain about where a file belongs, consult the project lead or refer to existing similar files in the project.