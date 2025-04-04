# Testing Strategy

## Overview

Rinna follows a comprehensive testing approach to ensure all components work as expected and maintain compatibility.

## Test Levels

### System Tests

BDD tests using Cucumber verify the system meets the core requirements:

```gherkin
Feature: Workflow management
  To manage software work clearly and transparently
  As a software engineering team
  We need explicit workflow enforcement

  Scenario: Creating a new Bug item
    Given the Rinna software is initialized
    When the developer creates a new Bug with title "Login fails"
    Then the Bug should exist with status "To Do" and priority "medium"

  Scenario: Progressing an item through workflow
    Given a Bug titled "Login fails" exists
    When the developer updates the Bug status to "In Progress"
    Then the Bug's status should be "In Progress"

  Scenario: Validating workflow transitions
    Given a Bug titled "Login fails" exists
    When the developer attempts an invalid status transition to "Released"
    Then the system should explicitly reject the transition
```

### Unit Tests

Unit tests cover individual classes and methods:

```bash
# Run all unit tests
mvn test

# Run specific test class
java -cp bin:lib/* org.junit.runner.JUnitCore com.rinna.test.TestClassName
```

## Test Environment

- **Local**: In-memory database or SQLite
- **CI/CD**: Test containers for database dependencies

## Code Coverage

Rinna maintains high test coverage requirements:

- Core domain logic: >90% coverage
- Services: >85% coverage 
- Overall: >80% coverage

## Running Tests

```bash
# Compile and run tests
mvn clean test

# Run Cucumber tests
cucumber features/

# Generate test coverage report
mvn jacoco:report
```
