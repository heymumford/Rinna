@bdd @workflow
Feature: Workflow Command Functionality
  As a user of the Rinna CLI
  I want to manage work item workflows
  So that I can track progress and transition items through their lifecycle

  Background:
    Given I am logged in as "testuser"
    And the system has the following work items:
      | ID      | Title                     | Description                            | Type | Priority | Status      | Assignee  |
      | WI-1001 | Authentication Feature    | Implement JWT authentication           | TASK | HIGH     | READY       | testuser  |
      | WI-1002 | Bug in Payment Module     | Fix transaction issues after payment   | BUG  | MEDIUM   | IN_PROGRESS | bob       |
      | WI-1003 | Update Documentation      | Add API reference documentation        | TASK | LOW      | DONE        | charlie   |
      | WI-1004 | Security Review           | Review security measures               | TASK | HIGH     | READY       | testuser  |
      | WI-1005 | Performance Optimization  | Improve database query performance     | TASK | MEDIUM   | READY       | testuser  |

  @smoke
  Scenario: View available workflow transitions for a work item
    When I run the command "rin workflow transitions WI-1001"
    Then the command should execute successfully
    And the output should contain "Available transitions for WI-1001"
    And the output should contain "READY → IN_PROGRESS"
    And the output should contain "READY → BLOCKED"

  Scenario: Transition a work item to a new status
    When I run the command "rin workflow move WI-1001 IN_PROGRESS"
    Then the command should execute successfully
    And the output should contain "Work item WI-1001 moved to IN_PROGRESS"
    And the work item "WI-1001" should have status "IN_PROGRESS"

  Scenario: Transition a work item to a blocking status with reason
    When I run the command "rin workflow block WI-1004 'Waiting for security clearance'"
    Then the command should execute successfully
    And the output should contain "Work item WI-1004 blocked"
    And the work item "WI-1004" should have status "BLOCKED"
    And the work item "WI-1004" should have blocking reason "Waiting for security clearance"

  Scenario: Unblock a previously blocked work item
    Given the work item "WI-1002" is blocked with reason "Waiting for API fix"
    When I run the command "rin workflow unblock WI-1002"
    Then the command should execute successfully
    And the output should contain "Work item WI-1002 unblocked"
    And the work item "WI-1002" should have status "READY"
    And the work item "WI-1002" should not have a blocking reason

  Scenario: Mark a work item as done
    When I run the command "rin workflow done WI-1005"
    Then the command should execute successfully
    And the output should contain "Work item WI-1005 marked as DONE"
    And the work item "WI-1005" should have status "DONE"
    And the work item history should show a transition to "DONE"

  Scenario: Add a comment when transitioning a work item
    When I run the command "rin workflow move WI-1001 IN_PROGRESS --comment 'Starting work on this task'"
    Then the command should execute successfully
    And the output should contain "Work item WI-1001 moved to IN_PROGRESS"
    And the work item "WI-1001" should have a comment "Starting work on this task"

  Scenario: View workflow history for a work item
    Given the work item "WI-1001" has the following workflow history:
      | From        | To          | User      | Timestamp           | Comment                |
      | CREATED     | READY       | admin     | 2025-04-01 10:00:00 | Initial state          |
      | READY       | IN_PROGRESS | testuser  | 2025-04-02 14:30:00 | Started implementation |
      | IN_PROGRESS | BLOCKED     | testuser  | 2025-04-03 09:15:00 | Blocked on API issue   |
      | BLOCKED     | IN_PROGRESS | testuser  | 2025-04-04 11:45:00 | API issue resolved     |
    When I run the command "rin workflow history WI-1001"
    Then the command should execute successfully
    And the output should contain "Workflow history for WI-1001"
    And the output should contain all workflow transitions in chronological order
    And the output should show comments associated with each transition

  Scenario: View work items with a specific workflow status
    When I run the command "rin workflow list READY"
    Then the command should execute successfully
    And the output should contain "Work items with status: READY"
    And the output should contain "WI-1001"
    And the output should contain "WI-1004"
    And the output should contain "WI-1005"
    And the output should not contain "WI-1002"
    And the output should not contain "WI-1003"

  Scenario: Reset a work item back to the initial state
    When I run the command "rin workflow reset WI-1001 --confirm"
    Then the command should execute successfully
    And the output should contain "Work item WI-1001 reset to initial state"
    And the work item "WI-1001" should have status "CREATED"

  Scenario: Error when trying to transition a work item to an invalid state
    When I run the command "rin workflow move WI-1001 INVALID_STATE"
    Then the command should fail with error code 1
    And the error output should contain "Error: Invalid workflow state 'INVALID_STATE'"
    And the error output should contain "Valid states are:"

  Scenario: Error when trying to transition a work item with invalid transition
    When I run the command "rin workflow move WI-1003 IN_PROGRESS"
    Then the command should fail with error code 1
    And the error output should contain "Error: Invalid workflow transition from DONE to IN_PROGRESS"
    And the error output should contain "Valid transitions from DONE are:"

  Scenario: View available workflow states
    When I run the command "rin workflow states"
    Then the command should execute successfully
    And the output should contain "Available workflow states"
    And the output should contain "CREATED"
    And the output should contain "READY"
    And the output should contain "IN_PROGRESS"
    And the output should contain "BLOCKED"
    And the output should contain "DONE"

  Scenario: Advanced workflow command with multiple flags
    When I run the command "rin workflow move WI-1001 IN_PROGRESS --comment 'Starting work' --assignee testuser --priority HIGH"
    Then the command should execute successfully
    And the output should contain "Work item WI-1001 moved to IN_PROGRESS"
    And the work item "WI-1001" should have status "IN_PROGRESS"
    And the work item "WI-1001" should have assignee "testuser"
    And the work item "WI-1001" should have priority "HIGH"
    And the work item "WI-1001" should have a comment "Starting work"