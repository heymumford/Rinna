@bdd @linux
Feature: Linux-Style Command Functionality
  As a user of the Rinna CLI
  I want to use familiar Linux-style commands
  So that I can interact with the system efficiently

  Background:
    Given the system has the following work items:
      | ID        | Title                    | Description                              | Type | Priority | Status      | Assignee |
      | WI-1001   | Authentication Feature   | Implement JWT authentication             | TASK | HIGH     | READY       | alice    |
      | WI-1002   | Bug in Payment Module    | Fix transaction issues after payment     | BUG  | MEDIUM   | IN_PROGRESS | bob      |
      | WI-1003   | Update Documentation     | Add API reference documentation          | TASK | LOW      | DONE        | charlie  |
      | WI-1004   | Security Authentication  | Review authentication security measures  | TASK | HIGH     | READY       | alice    |
      | WI-1005   | Performance Optimization | Improve database query performance       | TASK | MEDIUM   | IN_PROGRESS | bob      |

  Scenario: List work items with ls command
    When I run the ls command
    Then the command should execute successfully
    And the output should show a tabular list of work items
    And the output should contain all work item IDs

  Scenario: List work items with filtering options
    When I run the ls command with status filter "READY"
    Then the command should execute successfully
    And the output should only contain work items with status "READY"
    And the output should not contain work items with status "IN_PROGRESS"
    And the output should not contain work items with status "DONE"

  Scenario: View work item details with cat command
    When I run the cat command for work item "WI-1001"
    Then the command should execute successfully
    And the output should contain the detailed information for "WI-1001"
    And the output should contain "Authentication Feature"
    And the output should contain "Implement JWT authentication"

  Scenario: Search work items with grep command
    When I run the grep command with pattern "authentication"
    Then the command should execute successfully
    And the output should contain "Authentication Feature"
    And the output should contain "Security Authentication"
    And the output should not contain "Bug in Payment Module"

  Scenario: Edit work item with simple edit command
    When I run the edit command for work item "WI-1001" with new title "Updated Authentication Feature"
    Then the command should execute successfully
    And the work item "WI-1001" should have title "Updated Authentication Feature"

  Scenario: Change work item status with workflow command
    When I run the workflow command to move work item "WI-1001" to status "IN_PROGRESS"
    Then the command should execute successfully
    And the work item "WI-1001" should have status "IN_PROGRESS"

  Scenario: Command history functionality
    When I run the ls command
    And I run the cat command for work item "WI-1001"
    And I run the history command
    Then the command should execute successfully
    And the output should contain "ls"
    And the output should contain "cat WI-1001"

  Scenario: Command pipe functionality
    When I run the ls command with output piped to grep with pattern "READY"
    Then the command should execute successfully
    And the output should only contain work items with status "READY"

  Scenario: Help system with man command
    When I run the man command for "ls"
    Then the command should execute successfully
    And the output should contain usage information for the ls command
    And the output should contain examples of ls command usage