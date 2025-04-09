Feature: Test-Driven Development Workflow
  As a software engineer
  I want to follow TDD principles
  So that I can build robust, well-tested software

  Background:
    Given the development environment is set up
    And I have a clean working branch

  @tdd @positive
  Scenario: Red-Green-Refactor cycle for a new feature
    Given I have written a failing test for a feature not yet implemented
    When I run the test
    Then the test should fail with a clear error message
    When I implement the minimum code to make the test pass
    And I run the test again
    Then the test should pass
    When I refactor the code while keeping the tests passing
    And I run the test again
    Then the test should still pass
    And the code should be clean and maintainable

  @tdd @positive
  Scenario: Adding edge case tests for a feature
    Given I have implemented a feature with basic test coverage
    When I identify an edge case for the feature
    And I write a test for that edge case
    Then the test should fail
    When I modify the implementation to handle the edge case
    And I run the tests again
    Then all tests should pass
    And the feature should handle the edge case correctly

  @tdd @positive
  Scenario: Fixing a bug using TDD
    Given a bug has been reported in a feature
    When I reproduce the bug with a failing test
    Then the test should fail and demonstrate the bug
    When I fix the bug in the implementation
    And I run the test again
    Then the test should pass
    And the bug should be fixed

  @tdd @negative
  Scenario: Handling regression when modifying existing code
    Given I have a feature with comprehensive test coverage
    When I modify the feature implementation
    And I run the tests
    And a previously passing test now fails
    Then I should identify the regression
    And I should restore or fix the broken functionality
    When I run the tests again
    Then all tests should pass
    And no regression should be introduced

  @tdd @negative
  Scenario: Detecting test contamination
    Given I have a suite of tests for my feature
    When I notice that one test passing is dependent on another test running first
    Then I should identify the shared state between tests
    When I isolate the tests to run independently
    And I run each test in isolation
    Then each test should pass on its own merit
    And the tests should be free from cross-contamination

  @tdd @negative
  Scenario: Dealing with flaky tests
    Given I have a test that sometimes passes and sometimes fails
    When I identify the cause of the flakiness
    Then I should fix the test to be deterministic
    When I run the test multiple times
    Then the test should consistently pass
    And the test should be reliable

  @tdd @positive
  Scenario: Test-driven API evolution
    Given I have an existing API with test coverage
    When I need to add a new endpoint to the API
    Then I should write tests for the new endpoint first
    When I implement the new endpoint
    And I run the tests
    Then all tests should pass
    And the API should be backward compatible

  @tdd @negative
  Scenario: Handling slow tests in TDD workflow
    Given I have a test that takes a long time to run
    When I identify why the test is slow
    Then I should improve the test without compromising its value
    When I run the improved test
    Then the test should complete more quickly
    And still provide the same level of confidence

  @tdd @positive
  Scenario: Using mocks and stubs effectively
    Given I need to test a component with external dependencies
    When I replace real dependencies with test doubles
    And I write tests using these test doubles
    Then the tests should run quickly and reliably
    When I implement the component
    And I run the tests
    Then all tests should pass
    And the component should work with both real and test dependencies

  @tdd @negative
  Scenario: Addressing test technical debt
    Given I have tests with duplicated setup code
    When I refactor the tests to eliminate duplication
    Then the tests should be more maintainable
    When I run the refactored tests
    Then all tests should still pass
    And the test code should be cleaner