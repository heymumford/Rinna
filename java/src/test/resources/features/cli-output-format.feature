Feature: CLI Output Formatting
  As a developer
  I want the CLI output to be well-formatted and customizable
  So that I can quickly find the information I need

  Background:
    Given the Rinna system is running locally
    And the CLI tool is configured with the API endpoint "http://localhost:9080/api/v1"
    And a valid API token "ri-dev-token" is configured for the CLI
    And the following work items exist:
      | ID     | Type     | Title                      | Description                                          | Status      | Priority | Assignee    | Created     |
      | RIN-01 | TASK     | Update API documentation   | The API docs need to be updated with new endpoints   | TO_DO       | MEDIUM   | unassigned  | 2025-04-01  |
      | RIN-02 | BUG      | Login fails on Safari      | Users cannot login using Safari browsers             | IN_PROGRESS | HIGH     | developer1  | 2025-04-02  |
      | RIN-03 | TASK     | Refactor database layer    | Improve performance by optimizing queries with caching and indexing techniques | TO_DO       | LOW      | developer2  | 2025-04-03  |
      | RIN-04 | FEATURE  | Add export functionality   | Allow users to export data in CSV, JSON, and XML formats | TO_DO    | MEDIUM   | unassigned  | 2025-04-04  |
      | RIN-05 | CHORE    | Update dependencies        | Update NPM packages to latest versions to fix security vulnerabilities | TO_DO | LOW    | unassigned  | 2025-04-05  |

  @cli @format @smoke
  Scenario: Default list output format
    When the developer runs "rin list"
    Then the output should contain all work items
    And the output should have headers with columns "ID", "TYPE", "TITLE", "DESCRIPTION", "STATUS", "PRIORITY"
    And each description should be truncated to 40 characters with ellipses if longer
    And the output should be formatted in a table with proper alignment

  @cli @format
  Scenario: Custom output format with specific columns
    When the developer runs "rin list --format=id,title,status,assignee"
    Then the output should contain all work items
    And the output should only have headers with columns "ID", "TITLE", "STATUS", "ASSIGNEE"
    And the output should not contain "DESCRIPTION" or "PRIORITY" columns
    And the output should be formatted in a table with proper alignment

  @cli @format
  Scenario Outline: Different output formats
    When the developer runs "rin list --output=<format>"
    Then the output should be in <format> format
    And the output should contain data for all work items

    Examples:
      | format |
      | table  |
      | json   |
      | csv    |
      | yaml   |

  @cli @format
  Scenario: List output with truncated descriptions
    When the developer runs "rin list"
    Then the description for "RIN-03" should be truncated to "Improve performance by optimizing queries..."
    And the description for "RIN-05" should be truncated to "Update NPM packages to latest versions to..."

  @cli @format
  Scenario: List output without truncation
    When the developer runs "rin list --no-truncate"
    Then the description for "RIN-03" should not be truncated
    And all descriptions should be shown in full

  @cli @format
  Scenario: Compact output format
    When the developer runs "rin list --compact"
    Then the output should be in a compact single-line format per item
    And each item should be displayed as "ID: TITLE (STATUS)"

  @cli @format
  Scenario: Setting custom truncation length
    When the developer runs "rin list --truncate=20"
    Then the description for "RIN-03" should be truncated to "Improve performance..."
    And the description for "RIN-05" should be truncated to "Update NPM packages..."

  @cli @format @color
  Scenario: Output with color-coded status
    When the developer runs "rin list --color"
    Then the "IN_PROGRESS" status should be highlighted in yellow
    And the "TO_DO" status should be highlighted in blue
    And the "HIGH" priority should be highlighted in red

  @cli @format @color
  Scenario: Disable colored output
    When the developer runs "rin list --no-color"
    Then the output should not contain any color codes
    And all text should be in plain format

  @cli @format @sort
  Scenario Outline: Sorting output by different columns
    When the developer runs "rin list --sort=<column>"
    Then the output should be sorted by <column> in <direction> order

    Examples:
      | column   | direction |
      | id       | ascending |
      | priority | descending |
      | created  | descending |
      | status   | ascending |

  @cli @format @sort
  Scenario: Reverse sort order
    When the developer runs "rin list --sort=id --reverse"
    Then the output should be sorted by id in descending order
    And "RIN-05" should appear before "RIN-01"

  @cli @format @pagination
  Scenario: Paginated output
    When the developer runs "rin list --page=1 --per-page=2"
    Then the output should contain exactly 2 work items
    And the output should indicate "Page 1 of 3"
    And navigation hints should be displayed

  @cli @format @pagination
  Scenario: Last page in pagination
    When the developer runs "rin list --page=3 --per-page=2"
    Then the output should contain exactly 1 work item
    And the output should indicate "Page 3 of 3"

  @cli @format @view
  Scenario: Detailed view of a work item
    When the developer runs "rin view RIN-01"
    Then the output should show a detailed view of "RIN-01"
    And the output should include all fields including full description
    And the output should include creation timestamp and history
    And the output should be well-formatted with proper spacing

  @cli @format @summary
  Scenario: Summary mode
    When the developer runs "rin list --summary"
    Then the output should include summary statistics:
      | Total items   | 5       |
      | By type       | TASK: 2, BUG: 1, FEATURE: 1, CHORE: 1 |
      | By status     | TO_DO: 4, IN_PROGRESS: 1 |
      | By priority   | HIGH: 1, MEDIUM: 2, LOW: 2 |
      | By assignee   | unassigned: 3, developer1: 1, developer2: 1 |

  @cli @format @negative
  Scenario: Invalid format option
    When the developer runs "rin list --format=invalid,columns"
    Then the CLI should display an error message about invalid format columns
    And the error message should suggest valid columns
    And the CLI should indicate error with non-zero status code

  @cli @format @negative
  Scenario: Invalid output format
    When the developer runs "rin list --output=invalid"
    Then the CLI should display an error message about invalid output format
    And the error message should list valid output formats
    And the CLI should indicate error with non-zero status code

  @cli @format @config
  Scenario: User-defined format in configuration
    Given the user has a config file with custom format settings:
      """
      output:
        default_format: "id,title,status,assignee"
        truncate_length: 30
        use_color: true
      """
    When the developer runs "rin list"
    Then the output should only have headers with columns "ID", "TITLE", "STATUS", "ASSIGNEE"
    And the description for "RIN-03" should be truncated to 30 characters
    And the statuses should be color-coded