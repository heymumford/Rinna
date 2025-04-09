Feature: Mark work items as done with done command
  As a user of the Rinna CLI
  I want to mark work items as done
  So that I can update the work item status and track completed work

  Background:
    Given the user is logged in with username "test.user"
    And there are work items in various states

  Scenario: Mark a work item as done with valid ID
    When the user executes done command with item ID "123e4567-e89b-12d3-a456-426614174000"
    Then the command should execute successfully
    And the work item status should be changed to "DONE"
    And the output should contain "Work item 123e4567-e89b-12d3-a456-426614174000 marked as DONE"
    And the output should contain "Title:"
    And the output should contain "Updated state: DONE"
    And the output should contain "Completion date:"

  Scenario: Mark a work item as done with comment
    When the user executes done command with item ID "123e4567-e89b-12d3-a456-426614174000" and comment "Implementation completed successfully"
    Then the command should execute successfully
    And the work item status should be changed to "DONE"
    And the comment should be added to the work item
    And the transition should be done by user "test.user"

  Scenario: Mark a work item as done without comment
    When the user executes done command with item ID "123e4567-e89b-12d3-a456-426614174000" without comment
    Then the command should execute successfully
    And the work item status should be changed to "DONE"
    And the simple transition method should be used

  Scenario: Mark a work item as done with JSON output format
    When the user executes done command with item ID "123e4567-e89b-12d3-a456-426614174000" and format "json"
    Then the command should execute successfully
    And the work item status should be changed to "DONE"
    And the output should be in valid JSON format
    And the JSON output should contain key "itemId"
    And the JSON output should contain key "title"
    And the JSON output should contain key "newState"
    And the JSON output should contain key "completionDate"

  Scenario: Mark a work item as done with JSON output format and verbose flag
    When the user executes done command with item ID "123e4567-e89b-12d3-a456-426614174000" and format "json" and verbose flag
    Then the command should execute successfully
    And the work item status should be changed to "DONE"
    And the output should be in valid JSON format
    And the JSON output should contain key "itemId"
    And the JSON output should contain key "title"
    And the JSON output should contain key "newState"
    And the JSON output should contain key "completionDate"
    And the JSON output should contain key "type"
    And the JSON output should contain key "priority"
    And the JSON output should contain key "description"
    And the JSON output should contain key "created"

  Scenario: Attempt to mark a non-existent work item as done
    When the user executes done command with item ID "123e4567-e89b-12d3-a456-426614174999"
    Then the command should fail with exit code 1
    And the error output should contain "Cannot transition work item to DONE state"

  Scenario: Attempt to mark a work item as done with invalid ID format
    When the user executes done command with item ID "invalid-id"
    Then the command should fail with exit code 1
    And the error output should contain "Invalid work item ID format"

  Scenario: Attempt to mark a work item as done without providing ID
    When the user executes done command without item ID
    Then the command should fail with exit code 1
    And the error output should contain "Work item ID is required"

  Scenario: Attempt to mark a work item as done with empty ID
    When the user executes done command with empty item ID
    Then the command should fail with exit code 1
    And the error output should contain "Work item ID is required"

  Scenario: Attempt to mark a work item as done when transition is not allowed
    Given a work item with ID "blocked-item" that cannot be transitioned to "DONE"
    When the user executes done command with item ID "blocked-item"
    Then the command should fail with exit code 1
    And the error output should contain "Cannot transition work item to DONE state"
    And the error output should contain "Valid transitions:"

  Scenario: Attempt to mark a work item as done when transition throws exception
    Given a work item with ID "error-item" that throws exception when transitioned
    When the user executes done command with item ID "error-item"
    Then the command should fail with exit code 1
    And the error output should contain "Error transitioning work item:"

  Scenario: Mark a work item in READY state as done
    Given a work item with ID "ready-item" in "READY" state
    When the user executes done command with item ID "ready-item"
    Then the command should execute successfully
    And the work item status should be changed to "DONE"
    And the output should contain "Work item ready-item marked as DONE"

  Scenario: Mark a work item in IN_PROGRESS state as done
    Given a work item with ID "in-progress-item" in "IN_PROGRESS" state
    When the user executes done command with item ID "in-progress-item"
    Then the command should execute successfully
    And the work item status should be changed to "DONE"
    And the output should contain "Work item in-progress-item marked as DONE"

  Scenario: Mark a work item as done with operation tracking
    When the user executes done command with item ID "123e4567-e89b-12d3-a456-426614174000"
    Then the command should execute successfully
    And an operation with name "done" and type "UPDATE" should be started
    And the operation parameters should include item ID
    And the operation should be completed
    And the operation result should include item ID and title

  Scenario: Fail to mark a work item as done with operation tracking
    When the user executes done command with item ID "invalid-id"
    Then the command should fail with exit code 1
    And an operation with name "done" and type "UPDATE" should be started
    And the operation should be marked as failed
    And the failure reason should be captured