Feature: Display work item details with cat command
  As a user
  I want to view detailed information about work items
  So that I can understand their current state and history

  Background:
    Given the user is logged in
    And there is a work item with ID "WI-123" and title "Sample work item"
    And the work item "WI-123" has description "This is a sample work item for testing"
    And the work item "WI-123" has type "TASK", state "IN_PROGRESS", and priority "MEDIUM"
    And the work item "WI-123" has history entries
      | type        | user       | content                     |
      | CREATED     | john.doe   | Item created                |
      | UPDATED     | jane.smith | Updated description         |
      | STATE_CHANGE| john.doe   | Changed state to IN_PROGRESS|

  Scenario: Display work item with explicit ID
    When the user runs "cat WI-123" command
    Then the command should succeed
    And the output should contain "WORK ITEM WI-123"
    And the output should contain "Title: Sample work item"
    And the output should contain "Type: TASK"
    And the output should contain "State: IN_PROGRESS"
    And the output should contain "Priority: MEDIUM"
    And the output should contain "Description:"
    And the output should contain "This is a sample work item for testing"

  Scenario: Display work item with previously viewed work item
    Given the user has previously viewed work item "WI-123"
    When the user runs "cat" command without ID
    Then the command should succeed
    And the output should contain "WORK ITEM WI-123"
    And the output should contain "Title: Sample work item"

  Scenario: Display work item with line numbers
    When the user runs "cat WI-123 --show-line-numbers" command
    Then the command should succeed
    And the output should contain line numbers before content
    And the output should contain "1  Title: Sample work item"

  Scenario: Display work item with history
    When the user runs "cat WI-123 --show-history" command
    Then the command should succeed
    And the output should contain "History:"
    And the output should contain "CREATED by john.doe: Item created"
    And the output should contain "UPDATED by jane.smith: Updated description"
    And the output should contain "STATE_CHANGE by john.doe: Changed state to IN_PROGRESS"

  Scenario: Display work item with visible formatting
    Given the work item "WI-123" has description with special formatting "Line with tab\tcharacter\nAnother line"
    When the user runs "cat WI-123 --show-all-formatting" command
    Then the command should succeed
    And the output should contain tab markers and line ending markers

  Scenario: Display work item in JSON format
    When the user runs "cat WI-123 --format=json" command
    Then the command should succeed
    And the output should be in JSON format
    And the JSON output should contain "id", "title", and "description" fields
    And the JSON output should contain "type", "priority", and "status" fields

  Scenario: Display work item with history in JSON format
    When the user runs "cat WI-123 --format=json --show-history" command
    Then the command should succeed
    And the output should be in JSON format
    And the JSON output should contain a "history" array
    And each history entry should have "timestamp", "type", "user", and "content" fields

  Scenario: Fail when no work item ID is available
    When the user runs "cat" command without ID
    And no work item has been previously viewed
    Then the command should fail
    And the error output should contain "No work item context available"

  Scenario: Fail when work item ID is invalid
    When the user runs "cat invalid-id" command
    Then the command should fail
    And the error output should contain "Invalid work item ID format"

  Scenario: Fail when work item does not exist
    When the user runs "cat WI-999" command for non-existent work item
    Then the command should fail
    And the error output should contain "Work item not found"