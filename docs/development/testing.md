<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Rinna Testing Guide

This document outlines the testing approach, tools, and best practices for the Rinna workflow management system.

## Table of Contents

- [Testing Philosophy](#testing-philosophy)
- [Test Infrastructure](#test-infrastructure)
- [Running Tests](#running-tests)
- [Unit Testing](#unit-testing)
- [BDD Testing](#bdd-testing)
- [API Integration Testing](#api-integration-testing)
- [TDD Development](#tdd-development)
- [Test Coverage](#test-coverage)
- [Adding New Tests](#adding-new-tests)
- [Troubleshooting](#troubleshooting)

## Testing Philosophy

Rinna follows a comprehensive testing approach that combines:

1. **Test-Driven Development (TDD)** for core domain logic
2. **Behavior-Driven Development (BDD)** for user-facing features
3. **Clean Architecture Testing** for ensuring adherence to architectural principles
4. **API Integration Testing** for validating service interfaces

Tests are considered first-class citizens in the codebase and are treated with the same care and quality standards as production code.

## Test Infrastructure

The test infrastructure is built on:

- **JUnit 5 (Jupiter)** for unit testing (version 5.10.2+)
- **Cucumber JVM** for BDD testing (version 7.16.1+)
- **Mockito** for mocking dependencies in isolation tests (version 5.10.0+)
- **AssertJ** for fluent assertions
- **JaCoCo** for code coverage reporting

Key test infrastructure components:

- `TestHelper.java` - Utility for setting up test scenarios
- `TddTest.java` - Base class for TDD-style development
- `CucumberRunner.java` - Universal runner for BDD tests
- `Specialized Runners` - Feature-specific runners (Release, Workflow, API, etc.)
- `TestContext.java` - Context management for BDD tests
- Custom test scripts for running different test suites

## Running Tests

Rinna provides two primary approaches for running tests:

1. The `rin` CLI utility in the bin directory (streamlined options)
2. The `run-tests.sh` script with more advanced options

### Using the rin CLI

```bash
# Run all tests
./bin/rin test

# Run unit tests only
./bin/rin test unit

# Run all BDD tests
./bin/rin test bdd

# Run specific feature tests
./bin/rin test workflow
./bin/rin test release
./bin/rin test input
./bin/rin test api
./bin/rin test cli

# Run tests with a specific tag
./bin/rin test tag:json-api
```

### Using the run-tests.sh script

```bash
# Run all tests
./bin/run-tests.sh all

# Run unit tests only
./bin/run-tests.sh unit

# Run all BDD tests
./bin/run-tests.sh bdd

# Run with parallel execution
./bin/run-tests.sh -p bdd

# Run with verbose output
./bin/run-tests.sh -v unit

# Run tests for a specific tag
./bin/run-tests.sh tag:client
```

## Unit Testing

Unit tests are organized in the following structure:

- `/rinna-core/src/test/java/org/rinna/*.java` - Core system tests
- `/rinna-core/src/test/java/org/rinna/domain/**/*.java` - Domain layer tests
- `/rinna-core/src/test/java/org/rinna/service/**/*.java` - Service layer tests
- `/rinna-core/src/test/java/org/rinna/model/*.java` - Model tests

### Unit Test Best Practices

1. **One Assert Per Test**: Focus each test on one specific behavior
2. **Descriptive Test Names**: Use explicit names that describe the behavior being tested
3. **Use Test Helpers**: Leverage the `TestHelper` and `TddTest` base classes
4. **Isolation**: Tests should not depend on other tests or external systems
5. **Proper Mocking**: Use `@Mock` and `@ExtendWith(MockitoExtension.class)` for JUnit 5 compatibility
6. **Fast Execution**: Unit tests should execute quickly (<100ms per test)

Example unit test:

```java
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository repository;
    
    private ItemService service;
    
    @BeforeEach
    void setUp() {
        service = new DefaultItemService(repository);
    }
    
    @Test
    @DisplayName("should find items by status")
    void shouldFindItemsByStatus() {
        // Arrange
        List<WorkItem> items = List.of(createTestItem("Test"));
        when(repository.findByStatus(WorkflowState.FOUND)).thenReturn(items);
        
        // Act
        List<WorkItem> result = service.findByStatus(WorkflowState.FOUND);
        
        // Assert
        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getTitle());
    }
}
```

## BDD Testing

BDD tests are written in Gherkin syntax and organized as follows:

- `/rinna-core/src/test/resources/features/*.feature` - Feature files
- `/rinna-core/src/test/java/org/rinna/bdd/*.java` - Step definitions and runners

The test runners are structured hierarchically:

- `CucumberRunner.java` - Universal runner for all feature files
- `InputInterfaceRunner.java` - Runner for input interface features
- `ReleaseRunner.java` - Runner for release management features
- `APIIntegrationRunner.java` - Runner for API integration features
- `CLIIntegrationRunner.java` - Runner for CLI integration features
- `TaggedTestsRunner.java` - Runner for tag-based test execution

### BDD Test Best Practices

1. **Business Language**: Use domain-specific language that business stakeholders understand
2. **Focus on Behavior**: Test from the user's perspective
3. **Reusable Steps**: Create reusable step definitions
4. **Context Management**: Use the `TestContext` class to share state between steps
5. **Tagged Tests**: Use tags to organize tests (@json-api, @webhook, @client, etc.)

Example feature:

```gherkin
Feature: API Integration
  To efficiently integrate with external systems
  As a software engineering team
  We need a secure API for work item management

  @json-api
  Scenario: Accepting a valid JSON payload with authentication
    Given an API authentication token "ri-5e7a9b3f2c8d" for project "billing-system"
    When the following JSON payload is submitted with the token:
      """
      {
        "type": "FEATURE",
        "title": "Support for cryptocurrency payments",
        "description": "Add support for Bitcoin and Ethereum payments",
        "priority": "HIGH",
        "metadata": {
          "source": "product_roadmap",
          "estimated_points": "8",
          "requested_by": "finance_team"
        }
      }
      """
    Then a work item should be created with the specified attributes
    And the work item should be associated with project "billing-system"
    And the work item should include all the provided metadata
```

## API Integration Testing

API integration tests validate the external interfaces of the Rinna system. These include:

- JSON API for external system integration
- GitHub webhook integration
- Local developer client integration

Key components:

- `APIIntegrationSteps.java` - Steps for API authentication and payload handling
- `ClientIntegrationSteps.java` - Steps for local client integration
- `APIService.java` - Service interface for API operations
- `APIToken.java` - Entity for API authentication

### Running API Integration Tests

```bash
# Run all API integration tests
./bin/rin test api

# Run specific API test tags
./bin/run-tests.sh tag:json-api
./bin/run-tests.sh tag:webhook
./bin/run-tests.sh tag:client
```

## CLI Integration Testing

CLI integration tests validate the command-line interface for the Rinna system, which is designed to interact with a local web service that communicates with the Rinna API server.

Key components:

- `cli-integration.feature` - BDD feature file defining CLI behavior
- `CLIIntegrationSteps.java` - Step definitions for CLI commands
- `CLIIntegrationRunner.java` - Runner for CLI-specific tests

The CLI follows a Git-like syntax:

```bash
# Creating work items
rin add 'Fix authentication bug'
rin add --type=BUG --priority=HIGH 'Database connection failure'
rin add --project=billing-system 'Add PayPal integration'
rin add -t BUG -p HIGH 'Login page crashes on Safari'

# Viewing work items
rin view WI-456

# Listing work items
rin list
rin list --status=FOUND
rin list --type=BUG --priority=HIGH

# Updating work items
rin update WI-601 --status=IN_DEV
rin update WI-602 --assignee=developer1
```

### Running CLI Integration Tests

```bash
# Run all CLI integration tests
./bin/rin test cli

# Run specific CLI test tags
./bin/run-tests.sh tag:cli
./bin/run-tests.sh tag:cli-negative
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
mvn verify
```

The coverage report is available at `rinna-core/target/site/jacoco/index.html`.

Coverage goals:
- Line coverage: 90%+
- Branch coverage: 80%+

## Adding New Tests

### Adding a New Unit Test

1. Create a new test class that extends `TddTest` or use JUnit 5 directly with `@ExtendWith(MockitoExtension.class)`
2. Write test methods using JUnit 5 annotations (@Test, @DisplayName, etc.)
3. Run the tests with `./bin/run-tests.sh unit`

### Adding a New BDD Test

1. Add a new scenario to an existing feature file or create a new .feature file
2. Implement step definitions in an existing step class or create a new one
3. Run the tests with `./bin/run-tests.sh bdd` (no need to modify runners in most cases)

### Adding a New API Integration Test

1. Add a new scenario to the input-interface.feature file with the appropriate tag
2. Implement step definitions in APIIntegrationSteps.java or ClientIntegrationSteps.java
3. Run the tests with `./bin/run-tests.sh tag:your-tag`

### Adding a New CLI Integration Test

1. Add a new scenario to the cli-integration.feature file with the appropriate tag (e.g., @cli)
2. Implement step definitions in CLIIntegrationSteps.java
3. Run the tests with `./bin/rin test cli` or `./bin/run-tests.sh tag:cli`

## Troubleshooting

### JDK 21 Compatibility

Rinna is designed to work with JDK 21. To ensure compatibility:

- Use Mockito 5.10.0+ with ByteBuddy 1.14.11+
- Use the `@ExtendWith(MockitoExtension.class)` annotation for JUnit 5 integration
- Use the `@MockitoSettings(strictness = Strictness.LENIENT)` annotation for advanced mocking scenarios

### Cucumber Feature File Discovery

If feature files are not being discovered:

1. Ensure feature files are in `/rinna-core/src/test/resources/features/`
2. Verify that the Maven resources plugin is configured to include *.feature files
3. Check that the appropriate ClasspathResource is selected in the Cucumber runner

### Parallel Test Execution

For parallel test execution:

1. JUnit 5 tests use the configuration in `junit-platform.properties`
2. Use the `-p` flag with `run-tests.sh` to enable parallel execution
3. Ensure tests are properly isolated and do not depend on shared state