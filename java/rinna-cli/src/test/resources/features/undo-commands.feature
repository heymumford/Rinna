Feature: Undo Command
  As a user of the Rinna CLI
  I want to be able to undo recent changes to work items
  So that I can correct mistakes and revert unwanted changes

  Background:
    Given the system is initialized
    And the user is authenticated
    And a work item exists with id "WI-123"
    And the work item is assigned to the current user
    And the work item has history entries

  Scenario: Undo the most recent state change
    When the user runs the "undo" command with the work item ID "WI-123"
    And the user confirms the undo operation
    Then the previous state should be restored
    And a success message should be displayed
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Undo a priority change
    Given the work item has a priority change in its history
    When the user runs the "undo" command with the work item ID "WI-123"
    And the user confirms the undo operation
    Then the previous priority should be restored
    And a success message should be displayed
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Undo a title change
    Given the work item has a title change in its history
    When the user runs the "undo" command with the work item ID "WI-123"
    And the user confirms the undo operation
    Then the previous title should be restored
    And a success message should be displayed
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Undo an assignment change
    Given the work item has an assignment change in its history
    When the user runs the "undo" command with the work item ID "WI-123"
    And the user confirms the undo operation
    Then the previous assignee should be restored
    And a success message should be displayed
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Undo without specifying a work item ID
    Given the user has a work item in progress
    When the user runs the "undo" command without specifying a work item ID
    And the user confirms the undo operation
    Then the previous state of the in-progress work item should be restored
    And a success message should be displayed
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Display available undo steps
    When the user runs the "undo --steps" command with the work item ID "WI-123"
    Then a list of available undo steps should be displayed
    And no changes should be made to the work item
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Undo a specific step by index
    Given the work item has multiple history entries
    When the user runs the "undo --step 1" command with the work item ID "WI-123"
    And the user confirms the undo operation
    Then the specified step should be undone
    And a success message should be displayed
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Cancel undo operation during confirmation
    When the user runs the "undo" command with the work item ID "WI-123"
    And the user cancels the undo operation
    Then no changes should be made to the work item
    And a cancellation message should be displayed
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Force undo without confirmation
    When the user runs the "undo --force" command with the work item ID "WI-123"
    Then the most recent change should be undone without confirmation
    And a success message should be displayed
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Output in JSON format
    When the user runs the "undo --format json" command with the work item ID "WI-123" with force flag
    Then the output should be in JSON format
    And the response should contain success status information
    And an operation should be tracked with type "undo" and action "UPDATE"

  Scenario: Attempt to undo with non-existent work item ID
    When the user runs the "undo" command with a non-existent work item ID "WI-999"
    Then an error message should be displayed indicating the work item was not found
    And an operation should be tracked with type "undo" and failed status

  Scenario: Attempt to undo changes to a work item in DONE state
    Given the work item is in "DONE" state
    When the user runs the "undo" command with the work item ID "WI-123"
    Then an error message should be displayed about restricted state
    And an operation should be tracked with type "undo" and failed status

  Scenario: Attempt to undo with an invalid step index
    When the user runs the "undo --step 99" command with the work item ID "WI-123"
    Then an error message should be displayed about invalid step index
    And an operation should be tracked with type "undo" and failed status

  Scenario: Attempt to undo with empty history
    Given the work item has no history entries
    When the user runs the "undo" command with the work item ID "WI-123"
    Then an error message should be displayed about no changes to undo
    And an operation should be tracked with type "undo" and failed status