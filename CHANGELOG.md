# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.9.0] - 2025-04-07

### Added
- Multi-language logging system supporting Java, Python, Bash, and Go
- Unified logging interface with consistent formatting across languages
- Context field support for structured logging in all language components
- Go logging bridge for integration with Java components
- Python logging bridge with TRACE level support
- Bash logging bridge with colored console output
- Cross-language logging tests
- Comprehensive logging documentation

### Fixed
- Go module import paths in rinna-logger component
- Build script for Go logger bridge
- Module path resolution for cross-language communication
- Log file management across all supported languages

## [1.4.0] - 2025-04-06

### Added
- Comprehensive testing framework as a first-class citizen in the project
- Meta-test system to verify the build environment and development tools
- Test unification across Java, Go, Python, and Bash languages
- Smart test runner with testing pyramid implementation
- Documentation for testing philosophy, quality standards, and unified test nomenclature
- Cross-language logging strategy for consistent logging across all languages
- Support for Python 3.13

### Changed
- Refactored test organization to follow a consistent structure across languages
- Streamlined repository by removing unnecessary files and normalizing structures
- Consolidated version tracking across all modules
- Improved build efficiency through parallel test execution
- Updated CI pipeline to support the new testing framework and Python 3.13

### Fixed
- Exit code propagation in test scripts to properly report test failures
- Code organization to better follow Clean Architecture principles
- Testing pyramid approach to ensure proper coverage across all test types

## [1.7.0] - 2025-04-06

### Added
- Consolidated documentation for migration tracking
- Comprehensive test migration status report
- New clean architecture version service
- Standardized cross-language approach for all components

### Changed
- Updated package structure migration status to reflect completion
- Officially marked CLI module as disabled in build process
- Streamlined dependency management
- Improved documentation clarity and organization

### Fixed
- Finalized package migration for core, main, and API modules
- Consolidated redundant documentation files
- Added specific CLI disable flag to Maven build
- Created migration roadmap for CLI module re-enablement

## [1.3.14] - 2025-04-05

### Added
- Comprehensive test pyramid strategy following Uncle Bob and Martin Fowler principles
- Five distinct test categories (unit, component, integration, acceptance, performance)
- JUnit 5 Tag-based test categorization system
- Maven profiles for selective test execution
- Enhanced CI workflow with stage-appropriate test execution
- Detailed testing strategy documentation
- Tagged test examples for each test category
- Mode-based build system with fast, test, package, verify, and release modes
- Domain-specific test categories with smart mapping (workflow, release, input, api, cli)
- Integrated version management with prepare-release command
- Test skipping option for faster builds (--skip-tests)
- Enhanced coverage reporting with Jacoco integration
- Version consistency checking script
- Improved configuration file management

### Changed
- Streamlined build system from 686 lines to 487 lines (29% reduction)
- Updated POM with better Java 21 configuration
- Improved error handling in build scripts
- Consolidated test scripts into a unified approach
- Enhanced documentation with comprehensive examples
- Version management now fully integrated with build process
- Updated version to 1.3.0 across all configuration files
- Fixed SQLite JDBC dependency version in rinna-core/pom.xml

### Fixed
- Tag-based test execution with proper Cucumber integration
- Build pipeline to handle test failures gracefully
- Command-line option consistency and help text
- Test summary reporting to correctly identify failed tests
- Coverage report discovery and display
- Corrected version inconsistencies in Go module files
- Temporarily disabled rinna-cli module due to compilation issues
- Improved .gitignore to exclude build artifacts and temporary files

## [1.2.3] - 2025-04-04

### Added
- Streamlined version management system
- centralized version.properties as single source of truth
- Comprehensive documentation of version management

### Changed
- Reduced version management code by 29%
- Simplified version update commands
- Improved version consistency verification

## [1.2.2] - 2025-03-28

### Added
- Initial implementation of workflow management
- Basic CLI interface
- Feature tests for core functionality