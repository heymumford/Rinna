# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive test pyramid strategy following Uncle Bob and Martin Fowler principles
- Five distinct test categories (unit, component, integration, acceptance, performance)
- JUnit 5 Tag-based test categorization system
- Maven profiles for selective test execution
- Enhanced CI workflow with stage-appropriate test execution
- Detailed testing strategy documentation
- Sample test class demonstrating tag usage
- Mode-based build system with fast, test, package, verify, and release modes
- Domain-specific test categories with smart mapping (workflow, release, input, api, cli)
- Integrated version management with prepare-release command
- Test skipping option for faster builds (--skip-tests)
- Enhanced coverage reporting with Jacoco integration
- Continuous testing with file change monitoring (--watch)
- CHANGELOG to track project changes
- Output and reporting improvements with duration tracking and color coding
- GitHub release automation with release command

### Changed
- Streamlined build system from 686 lines to 487 lines (29% reduction)
- Updated POM with better Java 21 configuration
- Improved error handling in build scripts
- Consolidated test scripts into a unified approach
- Enhanced documentation with comprehensive examples
- Version management now fully integrated with build process

### Fixed
- Tag-based test execution with proper Cucumber integration
- Build pipeline to handle test failures gracefully
- Command-line option consistency and help text
- Test summary reporting to correctly identify failed tests
- Coverage report discovery and display

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