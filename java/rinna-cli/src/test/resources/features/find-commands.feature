Feature: Work Item Find Command
  As a Rinna user
  I want to find work items based on various criteria
  So that I can locate specific items in the system

  Background:
    Given the user is authenticated
    And the following work items exist in the system:
      | ID    | Type      | Title           | State       | Priority  | Assignee | Created Date | Modified Date |
      | WI-101 | Task      | Setup Server    | In Progress | Medium    | user1    | 2023-10-01   | 2023-10-05    |
      | WI-102 | Bug       | Critical Bug    | Open        | Critical  | user2    | 2023-10-02   | 2023-10-02    |
      | WI-103 | Feature   | New Feature     | Closed      | Low       | user1    | 2023-09-15   | 2023-10-10    |
      | WI-104 | Task      | Documentation   | Open        | Medium    | user3    | 2023-10-05   | 2023-10-05    |
      | WI-105 | Bug       | Minor Bug       | Open        | Low       | user2    | 2023-10-07   | 2023-10-07    |

  Scenario: Find work items by name pattern
    When the user executes find command with name pattern "Bug"
    Then the command should succeed
    And the output should contain "WI-102" and "WI-105"
    And the output should not contain "WI-101", "WI-103", and "WI-104"
    And the MetadataService should record the operation

  Scenario: Find work items by type
    When the user executes find command with type "Bug"
    Then the command should succeed
    And the output should contain "WI-102" and "WI-105"
    And the output should not contain "WI-101", "WI-103", and "WI-104"
    And the MetadataService should record the operation

  Scenario: Find work items by state
    When the user executes find command with state "Open"
    Then the command should succeed
    And the output should contain "WI-102", "WI-104", and "WI-105"
    And the output should not contain "WI-101" and "WI-103"
    And the MetadataService should record the operation

  Scenario: Find work items by priority
    When the user executes find command with priority "Critical"
    Then the command should succeed
    And the output should contain "WI-102"
    And the output should not contain "WI-101", "WI-103", "WI-104", and "WI-105"
    And the MetadataService should record the operation

  Scenario: Find work items by assignee
    When the user executes find command with assignee "user2"
    Then the command should succeed
    And the output should contain "WI-102" and "WI-105"
    And the output should not contain "WI-101", "WI-103", and "WI-104"
    And the MetadataService should record the operation

  Scenario: Find work items created after a specific date
    When the user executes find command with created after "2023-10-03"
    Then the command should succeed
    And the output should contain "WI-104" and "WI-105"
    And the output should not contain "WI-101", "WI-102", and "WI-103"
    And the MetadataService should record the operation

  Scenario: Find work items updated before a specific date
    When the user executes find command with updated before "2023-10-03"
    Then the command should succeed
    And the output should contain "WI-102"
    And the output should not contain "WI-101", "WI-103", "WI-104", and "WI-105"
    And the MetadataService should record the operation

  Scenario: Find work items with multiple criteria
    When the user executes find command with type "Task" and state "Open"
    Then the command should succeed
    And the output should contain "WI-104"
    And the output should not contain "WI-101", "WI-102", "WI-103", and "WI-105"
    And the MetadataService should record the operation

  Scenario: Find work items with JSON output
    When the user executes find command with name pattern "Bug" and JSON output
    Then the command should succeed
    And the output should contain valid JSON with items "WI-102" and "WI-105"
    And the MetadataService should record the operation

  Scenario: Find work items with count only option
    When the user executes find command with type "Bug" and count only option
    Then the command should succeed
    And the output should contain "Found 2 work items" 
    And the output should not contain item details
    And the MetadataService should record the operation

  Scenario: No work items match the specified criteria
    When the user executes find command with name pattern "NonExistent"
    Then the command should succeed
    And the output should contain "No work items found"
    And the MetadataService should record the operation

  Scenario: Invalid date format in search criteria
    When the user executes find command with created after "invalid-date"
    Then the command should fail
    And the output should contain an error message about invalid date format
    And the MetadataService should not record any operation

  Scenario: Invalid priority in search criteria
    When the user executes find command with priority "UltraCritical"
    Then the command should fail
    And the output should contain an error message about invalid priority
    And the MetadataService should not record any operation

  Scenario: Help option displays usage information
    When the user executes find command with help option
    Then the command should succeed
    And the output should contain usage instructions for the find command
    And the output should list all available options
    And the MetadataService should not record any operation