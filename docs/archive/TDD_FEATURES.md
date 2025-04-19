<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Test-Driven Development with Rinna

Rinna provides comprehensive support for Test-Driven Development (TDD) across the entire development lifecycle. This document details how to use Rinna's TDD features effectively to boost developer productivity and code quality.

## What is Test-Driven Development?

Test-Driven Development (TDD) is a software development approach where tests are written before implementing the actual code. The TDD process follows three key steps, often referred to as the "Red-Green-Refactor" cycle:

1. **Red**: Write a test that defines a function or improvements of a function, which should fail initially.
2. **Green**: Write the minimal amount of code necessary to make the test pass.
3. **Refactor**: Clean up the code while ensuring that tests still pass.

## TDD Features in Rinna

Rinna provides several features to support TDD workflows:

1. **BDD-Style Test Scenarios**: Cucumber feature files for both general TDD workflow and specific engineering challenges.
2. **Test Categorization**: Tag-based filtering to run specific types of TDD tests.
3. **Workflow Integration**: Commands for tracking TDD progress within the workflow management system.
4. **Testing Pyramid Support**: Run TDD tests at different levels of the testing pyramid.

## Running TDD Tests

### Basic Commands

```bash
# Run all TDD-related tests
bin/rin test --tag=tdd

# Run only positive TDD scenarios
bin/rin test --tag=tdd --tag=positive

# Run only negative TDD scenarios
bin/rin test --tag=tdd --tag=negative

# Run TDD tests with detailed output
bin/rin test --tag=tdd --verbose
```

### Test Categories

Rinna's TDD features are categorized with tags for flexible test execution:

- **@tdd**: All Test-Driven Development scenarios
- **@positive**: Scenarios that validate correct implementation
- **@negative**: Scenarios that validate handling of edge cases and errors
- **@engineering**: Scenarios specific to engineering tasks
- **@api**: API-related TDD scenarios
- **@database**: Database-related TDD scenarios
- **@concurrent**: Concurrency-related TDD scenarios

### Running Specific Engineering Tests

```bash
# Run API-related TDD tests
bin/rin test --tag=tdd --tag=api

# Run database-related TDD tests
bin/rin test --tag=tdd --tag=database

# Run concurrent access TDD tests
bin/rin test --tag=tdd --tag=concurrent
```

## Workflow Integration

Rinna integrates TDD directly into the workflow management system, allowing you to track TDD progress for work items:

```bash
# Create a work item with TDD approach
bin/rin-cli create feature "Implement caching system" --approach=tdd

# Mark a test as implemented (Red phase)
bin/rin-cli tdd red ITEM-1 

# Mark implementation as complete (Green phase)
bin/rin-cli tdd green ITEM-1

# Mark refactoring as complete
bin/rin-cli tdd refactor ITEM-1

# Show TDD status for a work item
bin/rin-cli tdd status ITEM-1

# List all items using TDD approach
bin/rin-cli list --approach=tdd
```

## Engineering-Specific TDD Scenarios

Rinna includes TDD scenarios for common engineering challenges:

### REST API Testing

```bash
# Run REST API TDD tests
bin/rin test --tag=tdd --include="*REST API*"
```

This tests:
- Successful responses (200 OK)
- Client error responses (4xx)
- Server error responses (5xx)

### Database Migration Testing

```bash
# Run database migration TDD tests
bin/rin test --tag=tdd --include="*database migration*"
```

This tests:
- Forward migration functionality
- Rollback capability
- Data integrity during migration

### Concurrent Access Testing

```bash
# Run concurrent access TDD tests
bin/rin test --tag=tdd --include="*concurrent*"
```

This tests:
- Race condition detection
- Synchronization mechanisms
- Thread safety

### Caching System Testing

```bash
# Run caching TDD tests
bin/rin test --tag=tdd --include="*caching*"
```

This tests:
- Cache hits
- Cache misses
- Cache invalidation

### External Service Integration

```bash
# Run external service integration TDD tests
bin/rin test --tag=tdd --include="*external service*"
```

This tests:
- Successful responses
- Timeout handling
- Retry mechanisms

### Algorithm Testing

```bash
# Run algorithm TDD tests
bin/rin test --tag=tdd --include="*algorithm*"
```

This tests:
- Correctness with multiple inputs
- Edge cases
- Performance characteristics

### Memory Management

```bash
# Run memory management TDD tests
bin/rin test --tag=tdd --include="*memory*"
```

This tests:
- Resource allocation and deallocation
- Memory leak detection
- Long-running operations

### Event-Driven Architecture

```bash
# Run event-driven architecture TDD tests
bin/rin test --tag=tdd --include="*event*"
```

This tests:
- Event publication
- Event consumption
- Error handling in event processing

### Configuration Changes

```bash
# Run configuration change TDD tests
bin/rin test --tag=tdd --include="*configuration*"
```

This tests:
- Dynamic configuration updates
- System adaptation without restarts
- Configuration error handling

### Backward Compatibility

```bash
# Run backward compatibility TDD tests
bin/rin test --tag=tdd --include="*backward compatibility*"
```

This tests:
- API changes maintain compatibility
- New functionality works alongside old
- Client-server version differences

## Extending TDD Features

You can extend Rinna's TDD capabilities by adding new feature files and step definitions:

1. Create a new feature file in `src/test/resources/features/` with your custom scenarios
2. Add step definitions in `src/test/java/org/rinna/bdd/`
3. Register your feature file in the TDD runner

Example of a custom TDD feature file:

```gherkin
Feature: Custom Engineering TDD Workflow
  As a developer
  I want to test my custom engineering component
  So that I can ensure quality and correctness

  @tdd @custom
  Scenario: Testing custom component behavior
    Given I have written a test for my custom component
    When I run the test
    Then the test should fail with a clear error message
    When I implement the component
    And I run the test again
    Then the test should pass
```

## Best Practices

1. **Follow the Red-Green-Refactor Cycle**: Always start with a failing test before implementing the solution.
2. **Keep Tests Independent**: Each test should be able to run in isolation without dependencies on other tests.
3. **Test Behavior, Not Implementation**: Focus on what the code should do, not how it does it.
4. **Maintain a Balance**: Use the testing pyramid approach to balance unit, component, and integration tests.
5. **Address Flaky Tests Immediately**: Fix tests that sometimes pass and sometimes fail as they reduce confidence.

## Troubleshooting

### Tests Run Slowly

If your TDD tests are running slowly:
```bash
# Run with performance tracking
bin/rin test --tag=tdd --performance-log
```

### Tests Interfere With Each Other

If tests seem to be affecting each other:
```bash
# Run tests in isolation
bin/rin test --tag=tdd --isolated
```

### Missing Step Definitions

If you see errors about missing step definitions:
```bash
# Generate stub step definitions
bin/rin test --tag=tdd --generate-stubs
```

## Further Reading

- [Test Pyramid Strategy](TEST_PYRAMID.md)
- [Testing Philosophy](PHILOSOPHY.md)
- [Unified Test Approach](UNIFIED_TEST_APPROACH.md)
- [Quality Standards](QUALITY_STANDARDS.md)