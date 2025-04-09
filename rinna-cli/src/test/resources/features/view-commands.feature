Feature: View Command
  As a Rinna user
  I want to view work item details
  So that I can see all relevant information about a specific work item

  Background:
    Given a user "testuser" with basic permissions
    And a work item "WI-123" with title "Fix login bug" exists
    And the work item has detailed information

  Scenario: View work item details in text format
    When the user runs "view WI-123"
    Then the command should succeed
    And the output should display basic work item information
    And the command should track operation details

  Scenario: View work item details in JSON format
    When the user runs "view WI-123 --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should include work item details
    And the command should track format parameters

  Scenario: View work item with verbose details
    When the user runs "view WI-123 --verbose"
    Then the command should succeed
    And the output should include additional work item details
    And the command should track the verbose parameter

  Scenario: View work item with both JSON format and verbose output
    When the user runs "view WI-123 --format=json --verbose"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should include extended work item details
    And the command should track both format and verbose parameters

  Scenario: Attempt to view without providing an item ID
    When the user runs "view" without an item ID
    Then the command should fail
    And the error output should indicate item ID is required
    And the command should track operation failure

  Scenario: Attempt to view with invalid item ID format
    When the user runs "view invalid-id"
    Then the command should fail
    And the error output should indicate invalid item ID format
    And the command should track operation failure

  Scenario: Attempt to view non-existent work item
    When the user runs "view non-existent-id"
    Then the command should fail
    And the error output should indicate work item not found
    And the command should track operation failure

  Scenario: Handle service exception gracefully
    When the item service throws an exception
    Then the command should fail
    And the error output should indicate issue retrieving work item
    And the command should track operation failure