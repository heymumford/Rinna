Feature: List work items with ls command
  As a user of the Rinna CLI
  I want to list work items with Unix-style options
  So that I can view and organize my work items efficiently

  Background:
    Given the user is authenticated with username "test.user"
    And the system has work items in various states

  Scenario: List all work items with default format
    When the user executes ls command without options
    Then the command should execute successfully
    And the output should display work items in tabular format
    And the output should contain column headers
    And the output should include the work item IDs

  Scenario: List work items with detailed information (long format)
    When the user executes ls command with long format option
    Then the command should execute successfully
    And the output should display detailed work item information
    And the output should contain "Detailed Work Item Listing"

  Scenario: List work items with history information (all format)
    When the user executes ls command with long and all format options
    Then the command should execute successfully
    And the output should display detailed work item information
    And the output should include history information

  Scenario: List a specific work item by ID
    When the user executes ls command with item ID "123e4567-e89b-12d3-a456-426614174000"
    Then the command should execute successfully
    And the output should display information for only that work item
    And the context manager should update with the viewed item

  Scenario: List a specific work item with detailed information
    When the user executes ls command with item ID "123e4567-e89b-12d3-a456-426614174000" and long format
    Then the command should execute successfully
    And the output should display detailed information for that work item
    And the output should include relationship information

  Scenario: List a specific work item with history
    When the user executes ls command with item ID "123e4567-e89b-12d3-a456-426614174000" and long and all format
    Then the command should execute successfully
    And the output should display detailed information for that work item
    And the output should include history information
    And the output should include timestamp information

  Scenario: List all work items in JSON format
    When the user executes ls command with JSON format
    Then the command should execute successfully
    And the output should be in valid JSON format
    And the JSON output should contain array of work items
    And the JSON output should contain metadata like item count

  Scenario: List a specific work item in JSON format
    When the user executes ls command with item ID "123e4567-e89b-12d3-a456-426614174000" and JSON format
    Then the command should execute successfully
    And the output should be in valid JSON format
    And the JSON output should contain item details
    And the JSON output should contain relationship information

  Scenario: List a specific work item with verbose JSON output
    When the user executes ls command with item ID "123e4567-e89b-12d3-a456-426614174000" and JSON format and verbose flag
    Then the command should execute successfully
    And the output should be in valid JSON format
    And the JSON output should contain extended item information

  Scenario: Try to list a non-existent work item
    When the user executes ls command with item ID "nonexistent-item-id"
    Then the command should fail with exit code 1
    And the error output should contain "Work item not found"

  Scenario: Try to list a work item with invalid ID format
    When the user executes ls command with item ID "invalid-id-format"
    Then the command should fail with exit code 1
    And the error output should contain "Invalid work item ID format"

  Scenario: List empty work items
    Given the system has no work items
    When the user executes ls command without options
    Then the command should execute successfully
    And the output should contain "No work items found"

  Scenario: List work items with parent relationships
    Given the system has work items with parent-child relationships
    When the user executes ls command without options
    Then the command should execute successfully
    And the output should include parent relationship information
    And the output should contain "Child of:"

  Scenario: List work items with child relationships
    Given the system has work items with parent-child relationships
    When the user executes ls command without options
    Then the command should execute successfully
    And the output should include child relationship information
    And the output should contain "Parent of:"

  Scenario: Operation tracking during successful listing of all items
    When the user executes ls command without options
    Then the command should execute successfully
    And an operation with name "ls" and type "READ" should be tracked
    And the operation parameters should include format options
    And the operation should be completed successfully
    And the operation result should include item count

  Scenario: Operation tracking during failed item lookup
    When the user executes ls command with item ID "invalid-id-format"
    Then the command should fail with exit code 1
    And an operation with name "ls" and type "READ" should be tracked
    And the operation should be marked as failed
    And the failure reason should be captured