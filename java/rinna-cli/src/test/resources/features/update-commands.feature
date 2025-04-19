Feature: Update Command
  As a Rinna user
  I want to update work item details
  So that I can modify information and transition work items through their lifecycle

  Background:
    Given a user "testuser" with basic permissions
    And a work item "WI-123" with title "Original title" exists

  Scenario: Update work item title
    When the user runs "update WI-123 --title='Updated title'"
    Then the command should succeed
    And the output should confirm the work item was updated
    And the output should show the updated title
    And the command should track operation details

  Scenario: Update work item description
    When the user runs "update WI-123 --description='Updated description text'"
    Then the command should succeed
    And the output should confirm the work item was updated
    And the command should track the updated field

  Scenario: Update work item type
    When the user runs "update WI-123 --type=BUG"
    Then the command should succeed
    And the output should confirm the work item was updated
    And the output should show the updated type
    And the command should track the updated field

  Scenario: Update work item priority
    When the user runs "update WI-123 --priority=HIGH"
    Then the command should succeed
    And the output should confirm the work item was updated
    And the output should show the updated priority
    And the command should track the updated field

  Scenario: Update work item assignee
    When the user runs "update WI-123 --assignee=john.doe"
    Then the command should succeed
    And the output should confirm the work item was updated
    And the output should show the updated assignee
    And the command should track the updated field

  Scenario: Update work item status with a valid transition
    Given the work item has status "CREATED"
    And the transition to "IN_PROGRESS" is valid
    When the user runs "update WI-123 --status=IN_PROGRESS"
    Then the command should succeed
    And the output should confirm the work item was updated
    And the output should show the updated status
    And the command should track the state change

  Scenario: Update work item status with comment
    Given the work item has status "IN_PROGRESS"
    And the transition to "READY" is valid
    When the user runs "update WI-123 --status=READY --comment='Ready for testing'"
    Then the command should succeed
    And the output should confirm the work item was updated
    And the output should show the updated status
    And the command should track both the state change and comment

  Scenario: Update multiple fields at once
    When the user runs "update WI-123 --title='New title' --priority=LOW --assignee=jane.doe"
    Then the command should succeed
    And the output should confirm the work item was updated
    And the output should show all updated fields
    And the command should track all updated fields

  Scenario: Update work item with JSON output
    When the user runs "update WI-123 --title='JSON title' --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should include the updated fields
    And the command should track format parameters

  Scenario: Update work item with verbose output
    When the user runs "update WI-123 --title='Verbose title' --verbose"
    Then the command should succeed
    And the output should include additional work item details
    And the output should include operation tracking information
    And the command should track the verbose parameter

  Scenario: Attempt to update without providing an item ID
    When the user runs "update" without an item ID
    Then the command should fail
    And the error output should indicate item ID is required
    And the command should track operation failure

  Scenario: Attempt to update with invalid item ID
    When the user runs "update invalid-id --title='Test'"
    Then the command should fail
    And the error output should indicate invalid item ID format
    And the command should track operation failure

  Scenario: Attempt to update non-existent work item
    When the user runs "update non-existent-id --title='Test'"
    Then the command should fail
    And the error output should indicate work item not found
    And the command should track operation failure

  Scenario: Attempt to update without specifying any fields
    When the user runs "update WI-123" without specifying any fields
    Then the command should fail
    And the error output should indicate no updates specified
    And the command should track operation failure

  Scenario: Attempt invalid status transition
    Given the work item has status "CREATED"
    And the transition to "DONE" is invalid
    When the user runs "update WI-123 --status=DONE"
    Then the command should fail
    And the error output should indicate invalid transition
    And the error output should include available transitions
    And the command should track operation failure