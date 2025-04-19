Feature: Workflow management
  To manage software work clearly and transparently
  As a software engineering team
  We need explicit workflow enforcement

  Background:
    Given the Rinna system is initialized

  Scenario: Creating a new Bug item
    When the developer creates a new Bug with title "Login fails"
    Then the Bug should exist with status "FOUND" and priority "MEDIUM" 

  Scenario: Creating a work item with custom properties
    Given a new "FEATURE" work item with title "User profile page"
    And the work item has description "Add user profile functionality"
    And the work item has priority "HIGH"
    And the work item is assigned to "john.doe"
    When I create the work item
    Then the work item should be created successfully
    And the work item should be a "FEATURE"
    And the work item should have title "User profile page"
    And the work item should have description "Add user profile functionality"
    And the work item should have priority "HIGH"
    And the work item should be assigned to "john.doe"
    And the work item should be in "FOUND" state

  Scenario: Valid workflow transition
    Given a work item in "FOUND" state
    When I transition the work item to "TRIAGED" state
    Then the transition should succeed
    And the work item should be in "TRIAGED" state

  Scenario: Invalid transition
    Given a work item in "FOUND" state
    When I transition the work item to "IN_PROGRESS" state
    Then the transition should fail