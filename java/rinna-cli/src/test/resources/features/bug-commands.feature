Feature: Bug Command
  As a user of the Rinna CLI
  I want to be able to quickly create bug reports
  So that I can efficiently track and resolve issues

  Background:
    Given the system is initialized
    And the user is authenticated
    And a project context is set

  Scenario: Create a bug with minimal required information
    When the user runs the "bug" command with title "Simple Bug"
    Then a bug should be created successfully
    And the bug should have the title "Simple Bug"
    And the bug should have the default priority "MEDIUM"
    And the bug should have CREATED status
    And the bug should be assigned to the current user
    And an operation should be tracked with type "bug" and action "CREATE"

  Scenario: Create a bug with complete information
    When the user runs the "bug" command with the following parameters:
      | title       | Critical Bug            |
      | description | This is a critical bug! |
      | priority    | HIGH                    |
      | assignee    | qa.team                 |
      | version     | 2.0.0                   |
    Then a bug should be created successfully
    And the bug should have the title "Critical Bug"
    And the bug should have the description "This is a critical bug!"
    And the bug should have the priority "HIGH"
    And the bug should have the assignee "qa.team"
    And the bug should have the version "2.0.0"
    And an operation should be tracked with type "bug" and action "CREATE"

  Scenario Outline: Create bugs with different priority levels
    When the user runs the "bug" command with title "<title>" and priority "<priority>"
    Then a bug should be created successfully
    And the bug should have the priority "<priority>"
    And an operation should be tracked with type "bug" and action "CREATE"

    Examples:
      | title                | priority  |
      | Low Priority Bug     | LOW       |
      | Medium Priority Bug  | MEDIUM    |
      | High Priority Bug    | HIGH      |
      | Critical Priority Bug| CRITICAL  |

  Scenario: Create a bug with automatic default version
    When the user runs the "bug" command with title "Version Bug"
    Then a bug should be created successfully
    And the bug should have the default version from configuration
    And an operation should be tracked with type "bug" and action "CREATE"

  Scenario: Create a bug with specific version
    When the user runs the "bug" command with title "Version Bug" and version "3.1.4"
    Then a bug should be created successfully
    And the bug should have the version "3.1.4"
    And an operation should be tracked with type "bug" and action "CREATE"

  Scenario: Create a bug with automatic assignment to default assignee
    Given the default bug assignee is set to "qa.team"
    When the user runs the "bug" command with title "Auto-assigned Bug"
    Then a bug should be created successfully
    And the bug should have the assignee "qa.team"
    And an operation should be tracked with type "bug" and action "CREATE"

  Scenario: Create a bug with JSON output format
    When the user runs the "bug" command with title "JSON Bug" and format "json"
    Then a bug should be created successfully
    And the output should be in JSON format
    And the JSON should contain the bug details
    And an operation should be tracked with type "bug" and action "CREATE"

  Scenario: Create a bug with verbose output
    When the user runs the "bug" command with title "Verbose Bug" and description "Detailed description" and verbose mode
    Then a bug should be created successfully
    And the output should include the description
    And an operation should be tracked with type "bug" and action "CREATE"

  Scenario: Attempt to create a bug without a title
    When the user runs the "bug" command without a title
    Then the command should fail with an error message about missing title
    And no bug should be created
    And an operation should be tracked with type "bug" and action "CREATE" and failed status

  Scenario: Attempt to create a bug with an empty title
    When the user runs the "bug" command with an empty title
    Then the command should fail with an error message about missing title
    And no bug should be created
    And an operation should be tracked with type "bug" and action "CREATE" and failed status

  Scenario: Handle service exception when creating a bug
    Given the item service will throw an exception
    When the user runs the "bug" command with title "Exception Bug"
    Then the command should fail with an error message about creating bug
    And no bug should be created
    And an operation should be tracked with type "bug" and action "CREATE" and failed status

  Scenario: Show stack trace in verbose mode when exception occurs
    Given the item service will throw an exception
    When the user runs the "bug" command with title "Verbose Exception Bug" and verbose mode
    Then the command should fail with an error message about creating bug
    And the error output should include a stack trace
    And an operation should be tracked with type "bug" and action "CREATE" and failed status