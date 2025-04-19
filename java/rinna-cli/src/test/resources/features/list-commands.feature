Feature: List Command
  As a Rinna user
  I want to list and filter work items
  So that I can easily find items of interest

  Background:
    Given a user "testuser" with basic permissions
    And work items with various types, priorities, and states exist

  Scenario: List all work items
    When the user runs "list"
    Then the command should succeed
    And the output should display all work items in a tabular format
    And the output should include column headers for ID, TITLE, TYPE, PRIORITY, STATUS, and ASSIGNEE
    And the command should track operation details

  Scenario: Filter work items by type
    When the user runs "list --type=BUG"
    Then the command should succeed
    And the output should only display items of type "BUG"
    And the command should track filter parameters

  Scenario: Filter work items by priority
    When the user runs "list --priority=HIGH"
    Then the command should succeed
    And the output should only display items with priority "HIGH"
    And the command should track filter parameters

  Scenario: Filter work items by state
    When the user runs "list --state=IN_PROGRESS"
    Then the command should succeed
    And the output should only display items in "IN_PROGRESS" state
    And the command should track filter parameters

  Scenario: Filter work items by project
    When the user runs "list --project=Project-A"
    Then the command should succeed
    And the output should only display items from project "Project-A"
    And the command should track filter parameters

  Scenario: Filter work items by assignee
    When the user runs "list --assignee=alice"
    Then the command should succeed
    And the output should only display items assigned to "alice"
    And the command should track filter parameters

  Scenario: Limit the number of displayed items
    When the user runs "list --limit=2"
    Then the command should succeed
    And the output should display at most 2 items
    And the output should indicate there are more items that could be displayed
    And the command should track the limit parameter

  Scenario: Sort items by field in ascending order
    When the user runs "list --sort-by=title"
    Then the command should succeed
    And the output should display items sorted by title in ascending order
    And the command should track sort parameters

  Scenario: Sort items by field in descending order
    When the user runs "list --sort-by=priority --descending"
    Then the command should succeed
    And the output should display items sorted by priority in descending order
    And the command should track sort parameters

  Scenario: Display output in JSON format
    When the user runs "list --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should include an array of work items
    And the command should track format parameters

  Scenario: Display verbose output with additional details
    When the user runs "list --verbose"
    Then the command should succeed
    And the output should include additional details for each work item
    And the command should track the verbose parameter

  Scenario: Combine multiple filters
    When the user runs "list --type=TASK --priority=MEDIUM --state=READY"
    Then the command should succeed
    And the output should only display items matching all filter criteria
    And the command should track all filter parameters

  Scenario: Handle empty result set gracefully
    When the user runs "list --type=EPIC" with no matching items
    Then the command should succeed
    And the output should indicate no work items were found
    And the command should track operation completion with zero results

  Scenario: Handle search service exceptions
    When the search service throws an exception
    Then the command should fail
    And the error output should indicate an issue with listing work items
    And the command should track operation failure