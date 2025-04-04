# Testing Strategy

## Overview

Rinna follows a comprehensive testing approach to ensure all components work as expected and maintain compatibility. Rinna employs both unit tests and BDD tests to ensure correct behavior at multiple levels.

## Test Levels

### System Tests: BDD with Cucumber

Rinna uses Cucumber for Behavior-Driven Development (BDD) testing. These tests are written in Gherkin syntax and located in `src/test/resources/features/`.

There are two main feature files:
- `workflow.feature`: Tests for the workflow management functionality
- `release.feature`: Tests for the release management functionality

The BDD tests have been refactored to follow Clean Architecture principles:

```gherkin
Feature: Workflow management
  To manage software work clearly and transparently
  As a software engineering team
  We need explicit workflow enforcement

  Scenario: Creating a new Bug item
    Given the Rinna system is initialized
    When the developer creates a new Bug with title "Login fails"
    Then the Bug should exist with status "FOUND" and priority "MEDIUM"

  Scenario: Valid workflow transition
    Given a work item in "FOUND" state
    When I transition the work item to "TRIAGED" state
    Then the transition should succeed
    And the work item should be in "TRIAGED" state

  Scenario: Invalid transition
    Given a work item in "FOUND" state
    When I transition the work item to "IN_PROGRESS" state
    Then the transition should fail
```

To run the BDD tests, use one of the following commands:

```bash
# Run all tests
mvn test

# Run just the workflow tests
mvn -Dtest=CucumberRunner test

# Run just the release tests
mvn -Dtest=ReleaseRunner test
```

### Unit Tests

Unit tests cover individual classes and methods. Rinna uses JUnit 5 (Jupiter) for unit testing.

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn -Dtest=ClassNameTest test
```

## Test Architecture

The BDD tests are structured to align with Clean Architecture principles:

1. **Feature files**: Located in `src/test/resources/features/`
2. **Step definitions**: Located in `org.rinna.bdd` package
3. **TestContext**: For sharing test state between steps
4. **Test runners**: For configuring and running Cucumber tests

Unit tests are located in the same package as the code they test, following standard Java package structure.

## Code Coverage

Rinna maintains high test coverage requirements using JaCoCo:

- Core domain logic: >90% coverage
- Services: >85% coverage 
- Overall: >80% coverage

To generate a coverage report:

```bash
mvn verify
```

The report will be available in `target/site/jacoco/index.html`.

## Running Tests

```bash
# Compile and run all tests
mvn clean test

# Run BDD tests only
mvn -Dtest=CucumberRunner,ReleaseRunner test

# Run unit tests only
mvn -Dtest="*Test" test

# Generate test coverage report
mvn jacoco:report
```

## Writing BDD Tests

1. Add scenarios to the appropriate feature file using Gherkin syntax
2. Implement step definitions in `WorkflowSteps.java` or `ReleaseSteps.java`
3. Run the tests to verify the behavior

The test context (`TestContext.java`) allows for sharing state between steps.