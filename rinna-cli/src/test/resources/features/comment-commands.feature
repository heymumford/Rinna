Feature: Comment Command
  As a Rinna user
  I want to add comments to work items
  So that I can track discussions and contextual information

  Background:
    Given a user "testuser" with basic permissions
    And a work item "WI-123" with title "Fix login bug" exists

  Scenario: Add a comment to a specific work item
    When the user runs "comment WI-123 'This is a test comment'"
    Then the command should succeed
    And the output should indicate the comment was added successfully
    And the command should track operation details

  Scenario: Add a comment to the current work item in progress
    Given user "testuser" has work item "WI-456" in progress
    When the user runs "comment 'Comment on current work item'" without specifying an item ID
    Then the command should succeed
    And the output should indicate the comment was added successfully
    And the command should track operation details including item resolution

  Scenario: View all comments when adding a comment with verbose mode
    Given work item "WI-123" has existing comments
    When the user runs "comment WI-123 'Another comment' --verbose"
    Then the command should succeed
    And the output should show all comments for the work item
    And the output should include the newly added comment
    And the command should track comment listing operation

  Scenario: Output comment in JSON format
    When the user runs "comment WI-123 'JSON format comment' --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should include success status and work item ID
    And the command should track operation details

  Scenario: Output all comments in JSON format with verbose mode
    Given work item "WI-123" has existing comments
    When the user runs "comment WI-123 'Another JSON comment' --format=json --verbose"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should include all comments for the work item
    And the JSON should include the comment count
    And the command should track comment listing operation

  Scenario: Handle missing comment text
    When the user runs "comment WI-123" without providing comment text
    Then the command should fail
    And the error output should indicate comment text is required
    And the command should track operation failure

  Scenario: Handle invalid work item ID
    When the user runs "comment invalid-id 'Test comment'"
    Then the command should fail
    And the error output should indicate invalid work item ID
    And the command should track operation failure

  Scenario: Handle non-existent work item
    When the user runs "comment WI-999 'Test comment'" with a non-existent item ID
    Then the command should fail
    And the error output should indicate work item not found
    And the command should track operation failure

  Scenario: Handle error when no work item is in progress
    Given user "testuser" has no work item in progress
    When the user runs "comment 'Test comment'" without specifying an item ID
    Then the command should fail
    And the error output should indicate no work item is in progress
    And the command should track operation failure

  Scenario: Handle error in JSON format
    When the user runs "comment WI-123" without providing comment text and with format "json"
    Then the command should fail
    And the output should be valid JSON with error information
    And the command should track operation failure