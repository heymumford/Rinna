Feature: History Command
  As a Rinna user
  I want to view the history of work items
  So that I can track changes and understand the work item's evolution

  Background:
    Given a user "testuser" with view permissions
    And work item "WI-123" with title "Fix login bug" exists
    And work item "WI-123" has state changes, field changes, and comments

  Scenario: Display history for a specific work item
    When the user runs "history WI-123"
    Then the command should succeed
    And the output should contain the work item title "Fix login bug"
    And the output should show state changes
    And the output should show field changes
    And the output should show comments
    And the command should track operation details

  Scenario: Display history for the current work item in progress
    Given user "testuser" has work item "WI-456" in progress
    When the user runs "history" without specifying an item ID
    Then the command should succeed
    And the output should indicate showing history for the current work item
    And the command should track operation details

  Scenario: Filter history to show only comments
    When the user runs "history WI-123 --show-comments --no-state-changes --no-field-changes --no-assignments"
    Then the command should succeed
    And the output should contain comments
    And the output should not contain state changes
    And the output should not contain field changes
    And the output should not contain assignment changes
    And the command should track filtering parameters

  Scenario: Filter history to show only state changes
    When the user runs "history WI-123 --show-state-changes --no-comments --no-field-changes --no-assignments"
    Then the command should succeed
    And the output should contain state changes
    And the output should not contain comments
    And the output should not contain field changes
    And the output should not contain assignment changes
    And the command should track filtering parameters

  Scenario: View history from the last 24 hours
    When the user runs "history WI-123 --time-range=24h"
    Then the command should succeed
    And the output should indicate showing history from the last 24 hours
    And the command should track time range parameters

  Scenario: View history from the last 7 days
    When the user runs "history WI-123 --time-range=7d"
    Then the command should succeed
    And the output should indicate showing history from the last 7 days
    And the command should track time range parameters

  Scenario: View history from the last 2 weeks
    When the user runs "history WI-123 --time-range=2w"
    Then the command should succeed
    And the output should indicate showing history from the last 2 weeks
    And the command should track time range parameters

  Scenario: Output history in JSON format
    When the user runs "history WI-123 --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the output should contain JSON fields for work item and history entries
    And the command should track output format parameters

  Scenario: Output history in verbose JSON format
    When the user runs "history WI-123 --format=json --verbose"
    Then the command should succeed
    And the output should be valid JSON
    And the output should contain additional JSON fields for work item details
    And the command should track verbose output parameters

  Scenario: Handle non-existent work item
    When the user runs "history WI-999" with a non-existent item ID
    Then the command should fail
    And the error output should indicate the work item was not found
    And the command should track operation failure

  Scenario: Handle invalid work item ID format
    When the user runs "history invalid-id" with an invalid ID format
    Then the command should fail
    And the error output should indicate an invalid work item ID format
    And the command should track operation failure

  Scenario: Handle invalid time range format
    When the user runs "history WI-123 --time-range=invalid"
    Then the command should fail
    And the error output should indicate an invalid time range format
    And the command should track operation failure

  Scenario: Handle unauthorized access to work item
    Given a user "unauthorized" without view permissions for item "WI-123"
    When the user "unauthorized" runs "history WI-123"
    Then the command should fail
    And the error output should indicate permission denied
    And the command should track operation failure with security error

  Scenario: Handle missing work item in progress
    Given user "testuser" has no work item in progress
    When the user runs "history" without specifying an item ID
    Then the command should fail
    And the error output should indicate no work item is in progress
    And the command should track operation failure