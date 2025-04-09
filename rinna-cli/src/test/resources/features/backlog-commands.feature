Feature: Backlog Command
  As a Rinna user
  I want to manage my project backlog
  So that I can organize work effectively

  Background:
    Given a user "testuser" with basic permissions
    And the backlog service is initialized

  Scenario: List backlog items
    Given the backlog contains some work items
    When the user runs "backlog list"
    Then the command should succeed
    And the output should contain "Backlog Items:"
    And the output should contain "ID"
    And the output should contain "TITLE"
    And the output should contain "TYPE"
    And the output should contain "PRIORITY"
    And the command should track operation details

  Scenario: List backlog items (default action)
    Given the backlog contains some work items
    When the user runs "backlog"
    Then the command should succeed
    And the output should contain "Backlog Items:"
    And the command should track operation details

  Scenario: List backlog items with JSON output
    Given the backlog contains some work items
    When the user runs "backlog list --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain a "count" field
    And the JSON should contain an "items" array
    And the command should track format parameters

  Scenario: List backlog items with verbose output
    Given the backlog contains some work items
    When the user runs "backlog list --verbose"
    Then the command should succeed
    And the output should contain "Backlog Items:"
    And the command should track verbose parameter

  Scenario: Add an item to the backlog
    Given a work item with ID "WI-123" exists
    When the user runs "backlog add WI-123"
    Then the command should succeed
    And the output should contain "added to backlog successfully"
    And the command should track operation details
    And the command should track the item ID

  Scenario: Add an item to the backlog with JSON output
    Given a work item with ID "WI-124" exists
    When the user runs "backlog add WI-124 --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain "added"
    And the command should track format parameters

  Scenario: Attempt to add an item without an ID
    When the user runs "backlog add"
    Then the command should fail
    And the error output should contain "Item ID is required"
    And the command should track operation failure

  Scenario: Attempt to add a non-existent item
    When the user runs "backlog add non-existent"
    Then the command should fail
    And the error output should contain "Failed to add item"
    And the command should track operation failure

  Scenario: Set priority for a backlog item
    Given a work item with ID "WI-123" exists in the backlog
    When the user runs "backlog prioritize WI-123 --priority=HIGH"
    Then the command should succeed
    And the output should contain "Updated priority of WI-123 to HIGH"
    And the command should track operation details
    And the command should track the item ID
    And the command should track the priority

  Scenario: Attempt to set priority without an ID
    When the user runs "backlog prioritize --priority=HIGH"
    Then the command should fail
    And the error output should contain "Item ID is required"
    And the command should track operation failure

  Scenario: Attempt to set priority without priority value
    Given a work item with ID "WI-123" exists in the backlog
    When the user runs "backlog prioritize WI-123"
    Then the command should fail
    And the error output should contain "Priority is required"
    And the command should track operation failure

  Scenario: Remove an item from the backlog
    Given a work item with ID "WI-123" exists in the backlog
    When the user runs "backlog remove WI-123"
    Then the command should succeed
    And the output should contain "Removed WI-123 from backlog"
    And the command should track operation details
    And the command should track the item ID

  Scenario: Remove an item from the backlog with JSON output
    Given a work item with ID "WI-124" exists in the backlog
    When the user runs "backlog remove WI-124 --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain "removed"
    And the command should track format parameters

  Scenario: Attempt to remove an item without an ID
    When the user runs "backlog remove"
    Then the command should fail
    And the error output should contain "Item ID is required"
    And the command should track operation failure

  Scenario: Attempt to remove a non-existent item
    When the user runs "backlog remove non-existent"
    Then the command should fail
    And the error output should contain "Failed to remove item"
    And the command should track operation failure

  Scenario: Execute an unknown action
    When the user runs "backlog unknown-action"
    Then the command should fail
    And the error output should contain "Unknown backlog action"
    And the error output should contain "Valid actions"
    And the command should track operation failure