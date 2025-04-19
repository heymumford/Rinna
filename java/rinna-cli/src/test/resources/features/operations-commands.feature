Feature: Operations Command
  As a Rinna user
  I want to view and manage operation records
  So that I can track and monitor system activities

  Background:
    Given the user is authenticated
    And the following operations exist in the system:
      | Operation ID | Type        | Status     | Start Time              | End Time                | Parameters                        | Results                      | Error Details              |
      | op-123       | ADD_ITEM    | COMPLETED  | 2023-10-05T14:30:00Z    | 2023-10-05T14:32:00Z    | {"title":"Task 1"}               | {"id":"WI-101"}             |                            |
      | op-124       | UPDATE_ITEM | COMPLETED  | 2023-10-05T15:15:00Z    | 2023-10-05T15:16:30Z    | {"id":"WI-101","state":"OPEN"}   | {"success":true}            |                            |
      | op-125       | VIEW_ITEM   | COMPLETED  | 2023-10-06T09:45:00Z    | 2023-10-06T09:45:10Z    | {"id":"WI-101"}                  | {"found":true}              |                            |
      | op-126       | DELETE_ITEM | FAILED     | 2023-10-06T10:20:00Z    | 2023-10-06T10:20:15Z    | {"id":"WI-102"}                  |                             | "Item locked for editing"  |
      | op-127       | LIST_ITEMS  | COMPLETED  | 2023-10-06T11:30:00Z    | 2023-10-06T11:30:30Z    | {"filter":"OPEN"}                | {"count":5}                 |                            |
      | op-128       | ADD_ITEM    | IN_PROGRESS| 2023-10-06T12:00:00Z    |                         | {"title":"Task 2"}               |                             |                            |

  Scenario: List recent operations with default settings
    When the user executes the operations command
    Then the command should succeed
    And the output should show 6 operations
    And the output should include operation details
    And the MetadataService should record the operation

  Scenario: List operations with custom limit
    When the user executes the operations command with limit 3
    Then the command should succeed
    And the output should show 3 operations
    And the MetadataService should record the operation with limit parameter

  Scenario: Filter operations by type
    When the user executes the operations command with filter "ADD_ITEM"
    Then the command should succeed
    And the output should show only operations of type "ADD_ITEM"
    And the output should show 2 operations
    And the MetadataService should record the operation with filter parameter

  Scenario: Display operations in JSON format
    When the user executes the operations command with JSON output
    Then the command should succeed
    And the output should contain valid JSON
    And the JSON should include all operation fields
    And the MetadataService should record the operation with format parameter

  Scenario: Show detailed information in verbose mode
    When the user executes the operations command with verbose flag
    Then the command should succeed
    And the output should include operation parameters
    And the output should include operation results
    And the output should include error details for failed operations
    And the MetadataService should record the operation with verbose parameter

  Scenario: Filter operations by type with no matches
    When the user executes the operations command with filter "NON_EXISTENT_TYPE"
    Then the command should succeed
    And the output should indicate no matching operations found
    And the MetadataService should record the operation with filter parameter

  Scenario: Handle empty operation list
    Given no operations exist in the system
    When the user executes the operations command
    Then the command should succeed
    And the output should indicate no operations found
    And the MetadataService should record the operation

  Scenario: Display operations with custom recent count
    When the user executes the operations command with recent 4
    Then the command should succeed
    And the output should show 4 operations
    And the MetadataService should record the operation with recent parameter

  Scenario: Display help documentation
    When the user executes the operations command with help option
    Then the command should succeed
    And the output should contain usage instructions
    And the output should list all available options
    And the MetadataService should not record any operation

  Scenario: Handle unauthorized access
    Given the user is not authorized to view operations
    When the user executes the operations command
    Then the command should fail
    And the output should contain an error message about permissions
    And the MetadataService should not record any operation