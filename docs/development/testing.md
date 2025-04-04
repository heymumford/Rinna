<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford], Gemini Deep Research, Claude 3.7. -->

# Rinna Testing Guide

This document outlines the testing approach, tools, and best practices for the Rinna workflow management system.

## Table of Contents

- [Testing Philosophy](#testing-philosophy)
- [Test Infrastructure](#test-infrastructure)
- [Running Tests](#running-tests)
- [Unit Testing](#unit-testing)
- [BDD Testing](#bdd-testing)
- [TDD Development](#tdd-development)
- [Test Coverage](#test-coverage)
- [Adding New Tests](#adding-new-tests)

## Testing Philosophy

Rinna follows a comprehensive testing approach that combines:

1. **Test-Driven Development (TDD)** for core domain logic
2. **Behavior-Driven Development (BDD)** for user-facing features
3. **Clean Architecture Testing** for ensuring adherence to architectural principles

Tests are considered first-class citizens in the codebase and are treated with the same care and quality standards as production code.

## Test Infrastructure

The test infrastructure is built on:

- **JUnit 5 (Jupiter)** for unit testing
- **Cucumber JVM** for BDD testing
- **Mockito** for mocking dependencies in isolation tests
- **AssertJ** for fluent assertions
- **JaCoCo** for code coverage reporting

Key test infrastructure components:

- `TestHelper.java` - Utility for setting up test scenarios
- `TddTest.java` - Base class for TDD-style development
- `BDD Runners` - Cucumber test runners for different feature sets
- `TestContext.java` - Context management for BDD tests
- Custom test scripts for running different test suites

## Running Tests

### All Tests

Run all tests with:

```bash
./bin/rin test
```

### Unit Tests

Run unit tests with:

```bash
./bin/rin test unit
# or
./bin/run-unit-tests.sh
```

For more targeted unit test execution:

```bash
# Run a specific test class
./bin/run-unit-tests.sh --test=DefaultWorkItemTest

# Run with coverage report
./bin/run-unit-tests.sh --coverage

# Continuously run tests when code changes (requires inotify-tools)
./bin/run-unit-tests.sh --watch
```

### BDD Tests

Run BDD tests with:

```bash
./bin/rin test bdd
# or
./bin/run-bdd-tests.sh
```

For more targeted BDD test execution:

```bash
# Run workflow feature tests only
./bin/run-bdd-tests.sh workflow

# Run release feature tests only
./bin/run-bdd-tests.sh release

# Run with debug output
./bin/run-bdd-tests.sh --debug

# Stop at first failure
./bin/run-bdd-tests.sh --fail-fast
```

## Unit Testing

Unit tests are organized in the following structure:

- `/rinna-core/src/test/java/org/rinna/*.java` - Core system tests
- `/rinna-core/src/test/java/org/rinna/domain/**/*.java` - Domain layer tests
- `/rinna-core/src/test/java/org/rinna/service/**/*.java` - Service layer tests

### Unit Test Best Practices

1. **One Assert Per Test**: Focus each test on one specific behavior
2. **Descriptive Test Names**: Use explicit names that describe the behavior being tested
3. **Use Test Helpers**: Leverage the `TestHelper` and `TddTest` base classes
4. **Isolation**: Tests should not depend on other tests or external systems
5. **Fast Execution**: Unit tests should execute quickly (<100ms per test)

## BDD Testing

BDD tests are written in Gherkin syntax and organized as follows:

- `/rinna-core/src/test/resources/features/*.feature` - Feature files
- `/rinna-core/src/test/java/org/rinna/bdd/*.java` - Step definitions

### BDD Test Best Practices

1. **Business Language**: Use domain-specific language that business stakeholders understand
2. **Focus on Behavior**: Test from the user's perspective
3. **Reusable Steps**: Create reusable step definitions
4. **Context Management**: Use the `TestContext` class to share state between steps
5. **Tagged Tests**: Use tags to organize tests (@Release, @Workflow, etc.)

Example feature:

```gherkin
Feature: Workflow management
  To manage software work clearly and transparently
  As a software engineering team
  We need explicit workflow enforcement

  Scenario: Creating a new Bug item
    When the developer creates a new Bug with title "Login fails"
    Then the Bug should exist with status "FOUND" and priority "MEDIUM"
```

## TDD Development

Rinna encourages Test-Driven Development for all new features. The workflow is:

1. **RED**: Write a failing test that defines the expected behavior
2. **GREEN**: Write the minimal code necessary to make the test pass
3. **REFACTOR**: Clean up the code while ensuring tests still pass

The `TddTest` base class provides utilities to simplify TDD development.

Example TDD test:

```java
@DisplayName("Item queries")
class ItemQueryTest extends TddTest {

    @Test
    @DisplayName("should filter items by status")
    void shouldFilterItemsByStatus() {
        // Arrange
        createSampleWorkItem("Item 1");
        WorkItem item2 = createSampleWorkItem("Item 2");
        transitionToState(item2, WorkflowState.TRIAGED);
        
        // Act
        List<WorkItem> foundItems = itemService.findByStatus(WorkflowState.FOUND);
        List<WorkItem> triagedItems = itemService.findByStatus(WorkflowState.TRIAGED);
        
        // Assert
        assertEquals(1, foundItems.size());
        assertEquals("Item 1", foundItems.get(0).getTitle());
        assertEquals(1, triagedItems.size());
        assertEquals("Item 2", triagedItems.get(0).getTitle());
    }
}
```

## Test Coverage

Code coverage is measured using JaCoCo and can be generated with:

```bash
./bin/run-unit-tests.sh --coverage
```

The coverage report is available at `rinna-core/target/site/jacoco/index.html`.

Coverage goals:
- Line coverage: 90%+
- Branch coverage: 80%+

## Adding New Tests

### Adding a New Unit Test

1. Create a new test class that extends `TddTest` or use `TestHelper` directly
2. Write test methods using JUnit 5 annotations (@Test, @DisplayName, etc.)
3. Run the tests with `./bin/run-unit-tests.sh`

### Adding a New BDD Test

1. Add a new scenario to an existing feature file or create a new .feature file
2. Implement step definitions in an existing step class or create a new one
3. Update the appropriate Cucumber runner if necessary
4. Run the tests with `./bin/run-bdd-tests.sh`