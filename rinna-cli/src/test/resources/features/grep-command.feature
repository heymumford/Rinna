@bdd @grep
Feature: Grep Command Functionality
  As a user of the Rinna CLI
  I want to search for text patterns in work items
  So that I can quickly locate relevant information

  Background:
    Given the system has work items with the following details:
      | ID        | Title                    | Description                              | Type | Priority | Status      | Assignee |
      | WI-1001   | Authentication Feature   | Implement JWT authentication             | TASK | HIGH     | READY       | alice    |
      | WI-1002   | Bug in Payment Module    | Fix transaction issues after payment     | BUG  | MEDIUM   | IN_PROGRESS | bob      |
      | WI-1003   | Update Documentation     | Add API reference documentation          | TASK | LOW      | DONE        | charlie  |
      | WI-1004   | Security Authentication  | Review authentication security measures  | TASK | HIGH     | READY       | alice    |
      | WI-1005   | Performance Optimization | Improve database query performance       | TASK | MEDIUM   | IN_PROGRESS | bob      |

  Scenario: Basic search using the grep command
    When I run the grep command with pattern "authentication"
    Then the command should execute successfully
    And the output should contain "Authentication Feature"
    And the output should contain "Security Authentication"
    And the output should not contain "Update Documentation"
    And the output should include the work item details for matching items

  Scenario: Case-sensitive search using the grep command
    When I run the grep command with pattern "Authentication" and case-sensitive option
    Then the command should execute successfully
    And the output should contain "Authentication Feature"
    And the output should not contain "security authentication"

  Scenario: Displaying results in JSON format
    When I run the grep command with pattern "authentication" and format "json"
    Then the command should execute successfully
    And the output should be valid JSON
    And the JSON output should contain work items with "authentication" in their content

  Scenario: Displaying results in CSV format
    When I run the grep command with pattern "authentication" and format "csv"
    Then the command should execute successfully
    And the output should start with a CSV header
    And the CSV output should contain work items with "authentication" in their content

  Scenario: Count-only mode for search results
    When I run the grep command with pattern "authentication" and count-only option
    Then the command should execute successfully
    And the output should contain "Total matches:"
    And the output should contain "Matched work items:"
    And the output should not contain detailed work item information

  Scenario: Search with context lines
    When I run the grep command with pattern "authentication" and context 2
    Then the command should execute successfully
    And the output should show context lines around the matched text

  Scenario: Handling searches with no results
    When I run the grep command with pattern "nonexistent"
    Then the command should execute successfully
    And the output should indicate no matches were found

  Scenario: Error handling for empty pattern
    When I run the grep command with an empty pattern
    Then the command should fail
    And the error output should explain that a pattern is required