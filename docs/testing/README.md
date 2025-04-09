# Rinna Test Documentation

This directory contains comprehensive documentation for the Rinna testing strategy, architecture, and implementation guidelines. These documents provide a unified approach to testing across the multi-language Rinna codebase (Java, Go, Python).

## Core Test Documentation

| Document | Description |
|----------|-------------|
| [Test Automation Guide](TEST_AUTOMATION_GUIDE.md) | Complete guide to implementing automated tests at all levels across all languages |
| [Test Pyramid Strategy](TEST_PYRAMID.md) | Overview of our five-layer test pyramid approach and philosophy |
| [Unified Test Approach](UNIFIED_TEST_APPROACH.md) | Standardized approach to test organization, naming, and execution |
| [Testing Strategy](TESTING_STRATEGY.md) | Overall testing strategy and guiding principles |

## Practical Guidelines

| Document | Description |
|----------|-------------|
| [Test Templates](TEST_TEMPLATES.md) | Ready-to-use templates for all test types across all languages |
| [Test Automation Checklist](TEST_AUTOMATION_CHECKLIST.md) | Practical checklist for ensuring adequate test coverage |
| [Test Troubleshooting Guide](TEST_TROUBLESHOOTING.md) | Solutions for common test automation issues |
| [Test Compatibility Matrix](TEST_COMPATIBILITY_MATRIX.md) | Framework for ensuring cross-language test coverage |
| [TDD Practical Guide](TDD_PRACTICAL_GUIDE.md) | Step-by-step guide to Test-Driven Development in a multi-language environment |
| [Test Command Reference](TEST_COMMAND_REFERENCE.md) | Quick reference for test commands across all languages |

## Specialized Topics

| Document | Description |
|----------|-------------|
| [Admin Testing](ADMIN_TESTING.md) | Guidelines for testing administrative functionality |
| [Cross-Language Testing](CROSS_LANGUAGE_TESTING.md) | Guide to cross-language testing with the test harness |
| [Quality Standards](QUALITY_STANDARDS.md) | Quality standards for test implementation |
| [TDD Features](TDD_FEATURES.md) | Test-Driven Development approach for feature implementation |
| [Test Coverage Improvement Plan](TEST_COVERAGE_IMPROVEMENT_PLAN.md) | Plan for improving test coverage across the codebase |
| [Model Mapper Testing](MODEL_MAPPER_TESTING.md) | Detailed testing of the ModelMapper component |

## Migration and Implementation

| Document | Description |
|----------|-------------|
| [Test Migration Summary](TEST_MIGRATION_SUMMARY.md) | Summary of test migration efforts |
| [Test Implementation Plan](TEST_IMPLEMENTATION_PLAN.md) | Plan for implementing comprehensive test coverage |
| [Test Cleanup Summary](TEST_CLEANUP_SUMMARY.md) | Summary of test cleanup efforts |

## Standardized Test Structure

We have implemented a standardized test approach with the following components:

1. **Unified Directory Structure**:
   - `src/test/java/org/rinna/unit/` - Unit tests
   - `src/test/java/org/rinna/component/` - Component tests
   - `src/test/java/org/rinna/integration/` - Integration tests
   - `src/test/java/org/rinna/acceptance/` - Acceptance tests
   - `src/test/java/org/rinna/performance/` - Performance tests

2. **Standardized Test Tagging**:
   - `@Tag("unit")` for unit tests
   - `@Tag("component")` for component tests
   - `@Tag("integration")` for integration tests
   - `@Tag("acceptance")` for acceptance tests
   - `@Tag("performance")` for performance tests
   - `@Tag("admin")` for admin functionality tests

3. **Standardized File Naming**:
   - Unit tests: `*Test.java`
   - Component tests: `*ComponentTest.java`
   - Integration tests: `*IntegrationTest.java`
   - Acceptance tests: `*AcceptanceTest.java`
   - Performance tests: `*PerformanceTest.java`

## Test Framework Resources

### Command-Line Tools

The Rinna project provides several command-line tools for test execution and analysis:

```bash
# Run tests with the unified test framework
./bin/rin-test                       # Run all tests
./bin/rin-test unit                  # Run unit tests only
./bin/rin-test component             # Run component tests only
./bin/rin-test integration           # Run integration tests only
./bin/rin-test acceptance            # Run acceptance tests only
./bin/rin-test performance           # Run performance tests only
./bin/rin-test admin                 # Run admin functionality tests

# Run tests with additional options
./bin/rin-test --parallel unit       # Run tests in parallel
./bin/rin-test fast                  # Run fast tests (unit + component)
./bin/rin-test tag:workflow          # Run tests with specific tag

# Run admin tests using the dedicated script
./bin/run-admin-tests.sh             # All admin tests
./bin/run-admin-tests.sh --config    # Configuration tests only
./bin/run-admin-tests.sh --integration # Maven & server tests only
./bin/run-admin-tests.sh --project   # Project management tests only

# Analyze test distribution
./bin/test-discovery.sh              # Discover and categorize tests
./bin/test-pyramid-coverage.sh       # Analyze test pyramid coverage

# Generate code coverage reports
./bin/polyglot-coverage.sh           # Generate unified code coverage report
```

### Maven Profiles

Java tests can be run using Maven profiles:

```bash
# Run tests with specific profile
mvn test -P unit-tests               # Run unit tests only
mvn test -P component-tests          # Run component tests only
mvn verify -P integration-tests      # Run integration tests only
mvn verify -P acceptance-tests       # Run acceptance tests only
mvn verify -P performance-tests      # Run performance tests only
```

### Go Test Commands

Go tests can be run using the standard Go test command:

```bash
# Run all Go tests
cd api && go test ./...

# Run specific test package
cd api && go test ./pkg/health

# Run tests with verbose output
cd api && go test -v ./...

# Run benchmarks
cd api && go test -bench=. ./...
```

### Python Test Commands

Python tests can be run using pytest:

```bash
# Run all Python tests
cd python && python -m pytest

# Run specific test module
cd python && python -m pytest tests/unit/test_version.py

# Run tests with coverage
cd python && python -m pytest --cov=rinna
```

## Cross-Language Test Structure

The Rinna project follows a standardized test directory structure across languages:

```
# Java tests
src/test/java/org/rinna/
├── unit/           # Unit tests 
├── component/      # Component tests
├── integration/    # Integration tests
├── acceptance/     # Acceptance tests
│   └── steps/      # Step definitions for BDD tests
└── performance/    # Performance tests

src/test/resources/
├── features/       # BDD feature files
└── testdata/       # Test data files

# Go tests
api/test/
├── unit/           # Unit tests
├── component/      # Component tests
├── integration/    # Integration tests
├── acceptance/     # Acceptance tests
└── performance/    # Performance tests

# Python tests
python/tests/
├── unit/           # Unit tests
├── component/      # Component tests
├── integration/    # Integration tests
├── acceptance/     # Acceptance tests
└── performance/    # Performance tests
```

## Test Implementation Examples

### Java Unit Test Example

```java
package org.rinna.unit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("Service Unit Tests")
public class ServiceTest {
    @Test
    @DisplayName("Should perform operation correctly")
    void shouldPerformOperationCorrectly() {
        // Test implementation
    }
}
```

### BDD Testing Example

BDD tests follow a standard structure:

1. Feature files define scenarios in Gherkin syntax:

```gherkin
Feature: Work Item Management
  As a user
  I want to create and manage work items
  So that I can track my project tasks

  Scenario: Create a new work item
    Given I am logged in
    When I create a work item with title "Test Task"
    Then the work item should be created successfully
    And the work item should have the status "FOUND"
```

2. Step definitions implement the scenario steps:

```java
@Given("I am logged in")
public void iAmLoggedIn() {
    // Authentication implementation
}

@When("I create a work item with title {string}")
public void iCreateWorkItem(String title) {
    workItem = workItemService.create(new WorkItemCreateRequest(title));
}

@Then("the work item should be created successfully")
public void workItemShouldBeCreated() {
    assertNotNull(workItem.getId());
}
```

For more detailed examples and patterns, see the [Test Templates](TEST_TEMPLATES.md) document.

## Contributing to Test Documentation

When contributing to test documentation:

1. Follow the established document structure
2. Update relevant documentation when adding new test approaches
3. Ensure examples cover all supported languages (Java, Go, Python)
4. Keep test documentation aligned with code practices
5. Include practical examples whenever possible

## Related Documentation

- [User Guide - Testing](../user-guide/testing.md) - Test documentation for users
- [Development - Testing Strategy](../development/testing.md) - Testing information for developers
- [Project CI/CD Pipeline](../development/ci-workflow.md) - CI/CD pipeline documentation