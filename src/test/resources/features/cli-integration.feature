Feature: CLI Integration with Rinna API
  To efficiently create and manage work items from the command line
  As a developer
  I need a CLI tool that integrates with the Rinna API service

  Background:
    Given the Rinna system is running locally
    And the CLI tool is configured with the API endpoint "http://localhost:8080/api"
    And a valid API token "ri-dev-f72a159e4bdc" is configured for the CLI

  @cli
  Scenario: Creating a work item using the CLI
    When the developer runs "rin add 'Fix authentication bug in login form'"
    Then a witem should be created on the server with title "Fix authentication bug in login form"
    And the CLI should display the witem ID and status
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Adding a work item with type and priority
    When the developer runs "rin add --type=BUG --priority=HIGH 'Database connection failure'"
    Then a witem should be created on the server with title "Database connection failure"
    And the witem should have type "BUG" and priority "HIGH"
    And the CLI should display the witem ID and status
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Adding a work item with a description
    When the developer runs:
      """
      rin add 'Add export feature' --description 'Users need to export their data in CSV format'
      """
    Then a witem should be created on the server with title "Add export feature"
    And the witem should have description "Users need to export their data in CSV format"
    And the CLI should display the witem ID and status
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Adding a work item to a specific project
    Given a project "billing-system" exists on the server
    When the developer runs "rin add --project=billing-system 'Add PayPal integration'"
    Then a witem should be created on the server with title "Add PayPal integration"
    And the witem should be associated with project "billing-system"
    And the CLI should display the witem ID and status
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Using short form command options
    When the developer runs "rin add -t BUG -p HIGH 'Login page crashes on Safari'"
    Then a witem should be created on the server with title "Login page crashes on Safari"
    And the witem should have type "BUG" and priority "HIGH"
    And the CLI should display the witem ID and status
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Adding metadata to a work item
    When the developer runs:
      """
      rin add 'Implement dark mode' --meta source=design-team --meta estimated_points=5
      """
    Then a witem should be created on the server with title "Implement dark mode"
    And the witem should have metadata "source" with value "design-team"
    And the witem should have metadata "estimated_points" with value "5"
    And the CLI should display the witem ID and status
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Adding a work item with a parent
    Given a witem with ID "WI-123" and title "User Authentication Epic" exists on the server
    When the developer runs "rin add 'Implement two-factor auth' --parent WI-123"
    Then a witem should be created on the server with title "Implement two-factor auth"
    And the witem should have parent ID "WI-123"
    And the CLI should display the witem ID and status
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Viewing a work item using the CLI
    Given a witem with ID "WI-456" and title "Fix navigation bug" exists on the server
    When the developer runs "rin view WI-456"
    Then the CLI should display the witem details including:
      | Field       | Value            |
      | ID          | WI-456           |
      | Title       | Fix navigation bug |
      | Type        | BUG              |
      | Priority    | MEDIUM           |
      | Status      | FOUND            |
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Listing work items using the CLI
    Given the following witems exist on the server:
      | ID      | Title                        | Type    | Priority | Status  |
      | WI-501  | Implement user settings page | FEATURE | MEDIUM   | FOUND   |
      | WI-502  | Fix login button styling     | CHORE   | LOW      | TRIAGED |
      | WI-503  | Database timeout error       | BUG     | HIGH     | IN_DEV  |
    When the developer runs "rin list"
    Then the CLI should display a table with all witems
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Filtering work items by status
    Given the following witems exist on the server:
      | ID      | Title                        | Type    | Priority | Status  |
      | WI-501  | Implement user settings page | FEATURE | MEDIUM   | FOUND   |
      | WI-502  | Fix login button styling     | CHORE   | LOW      | TRIAGED |
      | WI-503  | Database timeout error       | BUG     | HIGH     | IN_DEV  |
    When the developer runs "rin list --status=FOUND"
    Then the CLI should display a table with 1 witem
    And the table should include witem "WI-501"
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Updating a work item status using the CLI
    Given a witem with ID "WI-601" and title "Implement search feature" exists on the server
    When the developer runs "rin update WI-601 --status=IN_DEV"
    Then the witem "WI-601" should be updated with status "IN_DEV"
    And the CLI should display the updated witem details
    And the CLI should indicate success with status code 0

  @cli
  Scenario: Updating a work item assignee using the CLI
    Given a witem with ID "WI-602" and title "Fix CSS on homepage" exists on the server
    When the developer runs "rin update WI-602 --assignee=developer1"
    Then the witem "WI-602" should be updated with assignee "developer1"
    And the CLI should display the updated witem details
    And the CLI should indicate success with status code 0

  # Negative Scenarios

  @cli @negative
  Scenario: Attempting to create a work item with invalid token
    Given the CLI tool is configured with an invalid API token "invalid-token"
    When the developer runs "rin add 'Add new feature'"
    Then the CLI should display an authentication error message
    And the CLI should indicate error with non-zero status code

  @cli @negative
  Scenario: Attempting to create a work item with server offline
    Given the Rinna system is not running
    When the developer runs "rin add 'Add new feature'"
    Then the CLI should display a server connection error message
    And the CLI should indicate error with non-zero status code

  @cli @negative
  Scenario: Attempting to create a work item with missing title
    When the developer runs "rin add"
    Then the CLI should display a usage error message
    And the CLI should indicate error with non-zero status code

  @cli @negative
  Scenario: Attempting to create a work item with invalid type
    When the developer runs "rin add --type=INVALID 'Add new feature'"
    Then the CLI should display a validation error for field "type"
    And the CLI should indicate error with non-zero status code

  @cli @negative
  Scenario: Attempting to create a work item with invalid priority
    When the developer runs "rin add --priority=INVALID 'Add new feature'"
    Then the CLI should display a validation error for field "priority"
    And the CLI should indicate error with non-zero status code

  @cli @negative
  Scenario: Attempting to view a non-existent work item
    When the developer runs "rin view WI-999"
    Then the CLI should display a "work item not found" error message
    And the CLI should indicate error with non-zero status code

  @cli @negative
  Scenario: Attempting to update a non-existent work item
    When the developer runs "rin update WI-999 --status=IN_DEV"
    Then the CLI should display a "work item not found" error message
    And the CLI should indicate error with non-zero status code

  @cli @negative
  Scenario: Attempting to update a work item with invalid status
    Given a witem with ID "WI-701" and title "Refactor authentication" exists on the server
    When the developer runs "rin update WI-701 --status=INVALID_STATUS"
    Then the CLI should display a validation error for field "status"
    And the CLI should indicate error with non-zero status code

  @cli @negative
  Scenario: Attempting to create a work item in a non-existent project
    When the developer runs "rin add --project=nonexistent 'Add new feature'"
    Then the CLI should display a "project not found" error message
    And the CLI should indicate error with non-zero status code