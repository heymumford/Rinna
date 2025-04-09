Feature: Undo Last Action on Work Item
  As a developer
  I want to undo my last action on a work item
  So that I can easily revert mistakes or unwanted changes

  Background:
    Given the Rinna system is initialized
    And I am logged in as a developer
    And I have a work item in progress

  @positive @undo
  Scenario: Successfully undo last state change
    Given I have changed the state of my work item to "TESTING"
    When I run "rin undo"
    Then I should see the current state "TESTING"
    And I should see the previous state "IN_PROGRESS"
    And I should be prompted for confirmation
    When I confirm the undo action
    Then the state should be reverted to "IN_PROGRESS"
    And the command should succeed
    And a history entry should be recorded for the undo action

  @positive @undo
  Scenario: Successfully undo last field change
    Given I have updated the title of my work item to "New feature implementation"
    When I run "rin undo"
    Then I should see the current title "New feature implementation" 
    And I should see the previous title "Implement authentication feature"
    And I should be prompted for confirmation
    When I confirm the undo action
    Then the title should be reverted to "Implement authentication feature"
    And the command should succeed
    And a history entry should be recorded for the undo action

  @positive @undo
  Scenario: Successfully undo last assignment change
    Given I have assigned my work item to "alice"
    When I run "rin undo"
    Then I should see the current assignee "alice"
    And I should see the previous assignee "bob"
    And I should be prompted for confirmation
    When I confirm the undo action
    Then the assignee should be reverted to "bob"
    And the command should succeed
    And a history entry should be recorded for the undo action

  @positive @undo
  Scenario: Successfully undo last priority change
    Given I have changed the priority of my work item to "HIGH"
    When I run "rin undo"
    Then I should see the current priority "HIGH"
    And I should see the previous priority "MEDIUM"
    And I should be prompted for confirmation
    When I confirm the undo action
    Then the priority should be reverted to "MEDIUM"
    And the command should succeed
    And a history entry should be recorded for the undo action

  @positive @undo
  Scenario: Cancel undo operation
    Given I have changed the state of my work item to "TESTING"
    When I run "rin undo"
    Then I should see the current state "TESTING"
    And I should see the previous state "IN_PROGRESS"
    And I should be prompted for confirmation
    When I cancel the undo action
    Then the state should remain "TESTING"
    And the command should succeed with message "Undo operation canceled"

  @positive @undo
  Scenario: Undo multiple consecutive changes with multiple undo commands
    Given I have made the following changes to my work item:
      | Field     | Original Value | New Value     |
      | state     | IN_PROGRESS    | TESTING       |
      | priority  | MEDIUM         | HIGH          |
      | assignee  | bob            | alice         |
    When I run "rin undo"
    Then I should see the current assignee "alice"
    And I should see the previous assignee "bob"
    When I confirm the undo action
    Then the assignee should be reverted to "bob"
    When I run "rin undo"
    Then I should see the current priority "HIGH"
    And I should see the previous priority "MEDIUM"
    When I confirm the undo action
    Then the priority should be reverted to "MEDIUM"
    When I run "rin undo"
    Then I should see the current state "TESTING"
    And I should see the previous state "IN_PROGRESS"
    When I confirm the undo action
    Then the state should be reverted to "IN_PROGRESS"

  @positive @undo
  Scenario: Undo action with --force flag skips confirmation
    Given I have changed the state of my work item to "TESTING"
    When I run "rin undo --force"
    Then I should not be prompted for confirmation
    And the state should be reverted to "IN_PROGRESS"
    And the command should succeed

  @negative @undo
  Scenario: Attempt to undo when no changes exist
    Given I have not made any changes to my work item
    When I run "rin undo"
    Then the command should fail
    And I should see an error message "No recent changes found to undo"

  @negative @undo
  Scenario: Attempt to undo with no work item in progress
    Given I have no work item in progress
    When I run "rin undo"
    Then the command should fail
    And I should see an error message "No work item is currently in progress"
    And I should see a tip about viewing in-progress items

  @negative @undo
  Scenario: Attempt to undo a change that was already undone
    Given I have changed the state of my work item to "TESTING"
    And I have successfully undone this change
    When I run "rin undo"
    And I confirm the undo action
    Then the command should fail
    And I should see an error message "No more changes to undo"

  @negative @undo
  Scenario: Attempt to undo after a certain undo time limit has passed
    Given I have changed the state of my work item to "TESTING" more than 24 hours ago
    When I run "rin undo"
    Then the command should fail
    And I should see an error message "Cannot undo changes older than 24 hours"

  @negative @undo
  Scenario: Attempt to undo when work item state prevents it
    Given my work item has been promoted to "RELEASED" state
    When I run "rin undo"
    Then the command should fail
    And I should see an error message "Cannot undo changes to items in RELEASED state"

  @negative @undo
  Scenario: Attempt to undo a change on a work item I do not own
    Given there is a work item assigned to "alice"
    And I am logged in as "bob"
    When I run "rin undo --item=WI-123"
    Then the command should fail
    And I should see an error message "You do not have permission to undo changes to this work item"
    
  @negative @undo @security
  Scenario: Attempt to undo with malicious item ID
    When I run "rin undo --item='; DROP TABLE WORKITEMS; --"
    Then the command should fail
    And I should see an error message "Invalid work item ID"
    And no database changes should occur
    
  @negative @undo @security
  Scenario: Attempt to undo with extremely long item ID
    Given I generate an item ID of 10000 characters
    When I run "rin undo --item={longId}"
    Then the command should fail gracefully
    And I should see an error message "Invalid work item ID"
    And the system should not crash
    
  @negative @undo @security
  Scenario: Attempt command injection through item parameter
    When I run "rin undo --item=123 && echo HACKED"
    Then the command should fail
    And I should see an error message "Invalid work item ID"
    And the command injection should not succeed
    
  @negative @undo @security
  Scenario: Attempt path traversal attack
    When I run "rin undo --item=../../../etc/passwd"
    Then the command should fail
    And I should see an error message "Invalid work item ID"
    And no sensitive files should be accessed
    
  @negative @undo @boundary
  Scenario: Handle undo with null input responses
    Given I have changed the state of my work item to "TESTING"
    When I run "rin undo"
    And I press enter without typing anything
    Then the undo action should be canceled
    And the state should remain "TESTING"
    
  @negative @undo @boundary
  Scenario: Handle undo with invalid history entry type
    Given I have a work item with an unsupported history entry type
    When I run "rin undo"
    Then the command should fail
    And I should see an error message "Unsupported change type"
    
  @positive @undo @boundary
  Scenario: Undo immediately after the change
    Given I have just changed the state of my work item to "TESTING"
    When I immediately run "rin undo --force"
    Then the state should be reverted to "IN_PROGRESS"
    And the command should succeed
    
  @negative @undo @boundary
  Scenario: Attempt to undo with exactly 24 hours old change
    Given I have changed the state of my work item to "TESTING" exactly 24 hours ago
    When I run "rin undo"
    Then the command should succeed
    And I should be able to undo the change
    
  @negative @undo @boundary
  Scenario: Attempt to undo with just over 24 hours old change
    Given I have changed the state of my work item to "TESTING" 24 hours and 1 minute ago
    When I run "rin undo"
    Then the command should fail
    And I should see an error message "Cannot undo changes older than 24 hours"
    
  @positive @undo @edge
  Scenario: Undo a change with special characters in values
    Given I have updated the title of my work item to "Title with special chars: <>&'\"\n\t"
    When I run "rin undo"
    And I confirm the undo action
    Then the title should be reverted successfully
    And the special characters should be handled correctly
    
  @positive @undo @multi-step
  Scenario: Successfully undo up to 3 changes back with --steps parameter
    Given I have made the following changes to my work item:
      | Field     | Original Value | New Value     |
      | state     | READY          | IN_PROGRESS   |
      | title     | Original Title | Updated Title |
      | priority  | LOW            | MEDIUM        |
      | assignee  | alice          | bob           |
    When I run "rin undo --steps=3"
    Then I should see a list of the 3 most recent changes
    And I should be prompted to select a change to undo
    When I select the 3rd change from the list
    Then the state should be reverted to "READY"
    And the command should succeed
    And a history entry should be recorded for the undo action
    
  @positive @undo @multi-step
  Scenario: Undo the 2nd most recent change
    Given I have made the following changes to my work item:
      | Field     | Original Value | New Value     |
      | title     | Original Title | Updated Title |
      | priority  | LOW            | MEDIUM        |
      | assignee  | alice          | bob           |
    When I run "rin undo --steps=2"
    Then I should see a list of the 2 most recent changes
    And I should be prompted to select a change to undo
    When I select the 2nd change from the list
    Then the priority should be reverted to "LOW"
    And the command should succeed
    
  @positive @undo @multi-step
  Scenario: Undo with specific step number directly
    Given I have made the following changes to my work item:
      | Field     | Original Value | New Value     |
      | title     | Original Title | Updated Title |
      | priority  | LOW            | MEDIUM        |
      | assignee  | alice          | bob           |
    When I run "rin undo --step=2"
    Then I should see the current priority "MEDIUM"
    And I should see the previous priority "LOW"
    And I should be prompted for confirmation
    When I confirm the undo action
    Then the priority should be reverted to "LOW"
    And the command should succeed
    
  @negative @undo @multi-step
  Scenario: Attempt to undo more than 3 steps back
    Given I have made the following changes to my work item:
      | Field     | Original Value | New Value     |
      | state     | READY          | IN_PROGRESS   |
      | title     | Original Title | Updated Title |
      | priority  | LOW            | MEDIUM        |
      | assignee  | alice          | bob           |
    When I run "rin undo --step=4"
    Then the command should fail
    And I should see an error message "Cannot undo more than 3 steps back"
    
  @negative @undo @multi-step
  Scenario: Attempt to undo after changing to another work item
    Given I have made changes to work item "WI-123"
    And I have switched to work item "WI-456"
    When I run "rin undo --item=WI-123"
    Then the command should fail
    And I should see an error message "Undo history is cleared when changing work items"
    
  @negative @undo @multi-step
  Scenario: Attempt to undo after closing a work item
    Given I have made changes to my work item
    And I have closed my work item
    When I run "rin undo"
    Then the command should fail
    And I should see an error message "Undo history is cleared when work item is closed"