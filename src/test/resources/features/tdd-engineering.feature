Feature: TDD for Specific Engineering Challenges
  As a software engineer
  I want to apply TDD to common engineering tasks
  So that I can ensure quality and maintainability

  Background:
    Given I am following TDD principles
    And I have a properly configured development environment

  @tdd @positive
  Scenario: Testing a REST API endpoint with different response codes
    Given I need to implement a REST API endpoint
    When I write tests for successful responses
    And I write tests for client error responses
    And I write tests for server error responses
    Then all tests should fail initially
    When I implement the endpoint with proper error handling
    And I run the tests
    Then all tests should pass
    And the API should handle all response scenarios correctly

  @tdd @positive
  Scenario: Implementing a database migration with rollback capability
    Given I need to update a database schema
    When I write tests verifying the migration works
    And I write tests verifying the rollback works
    Then all tests should fail initially
    When I implement the migration and rollback scripts
    And I run the tests
    Then all tests should pass
    And the migration should be safely reversible

  @tdd @negative
  Scenario: Handling concurrent access to shared resources
    Given I have code that accesses shared resources
    When I write tests simulating concurrent access
    Then the tests should fail due to race conditions
    When I implement proper synchronization mechanisms
    And I run the tests with high concurrency
    Then all tests should pass
    And the code should handle concurrent access safely

  @tdd @positive
  Scenario: Building a caching layer with proper invalidation
    Given I need to implement a caching mechanism
    When I write tests for cache hits
    And I write tests for cache misses
    And I write tests for cache invalidation
    Then all tests should fail initially
    When I implement the caching layer with invalidation
    And I run the tests
    Then all tests should pass
    And the cache should work effectively

  @tdd @negative
  Scenario: Handling timeouts and retries in external service calls
    Given I have code that calls external services
    When I write tests for successful responses
    And I write tests for timeout scenarios
    And I write tests for retry scenarios
    Then all tests should fail initially
    When I implement proper timeout and retry handling
    And I run the tests
    Then all tests should pass
    And the code should be resilient against temporary failures

  @tdd @positive
  Scenario: Implementing a complex algorithm with parameterized tests
    Given I need to implement a complex algorithm
    When I write parameterized tests with multiple inputs and expected outputs
    Then all tests should fail initially
    When I implement the algorithm
    And I run the tests
    Then all tests should pass
    And the algorithm should work for all defined parameters

  @tdd @negative
  Scenario: Detecting and handling memory leaks
    Given I have code that manages memory resources
    When I write tests for memory allocation and deallocation
    And I write tests that simulate long-running operations
    Then the memory leak tests should fail initially
    When I implement proper resource management
    And I run the tests
    Then all tests should pass
    And no memory leaks should be detected

  @tdd @positive
  Scenario: Testing event-driven architecture
    Given I have an event-driven system
    When I write tests for event publication
    And I write tests for event consumption
    And I write tests for event error handling
    Then all tests should fail initially
    When I implement the event publication and consumption mechanisms
    And I run the tests
    Then all tests should pass
    And events should flow correctly through the system

  @tdd @negative
  Scenario: Handling configuration changes in running system
    Given I have a system that needs to respond to configuration changes
    When I write tests for configuration updates while system is running
    Then the tests should fail initially
    When I implement dynamic configuration handling
    And I run the tests
    Then all tests should pass
    And the system should adapt to configuration changes without restarting

  @tdd @positive
  Scenario: Implementing backward compatibility in API changes
    Given I need to update an existing API
    When I write tests ensuring existing clients still function
    And I write tests for the new functionality
    Then the backward compatibility tests should pass
    And the new functionality tests should fail
    When I implement the API changes with backward compatibility
    And I run the tests
    Then all tests should pass
    And both old and new clients should work correctly