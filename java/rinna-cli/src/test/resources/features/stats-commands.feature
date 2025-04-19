Feature: Statistics commands
  As a user of the Rinna CLI
  I want to view statistics about my work items
  So that I can gain insights into the project's progress and status

  Background:
    Given I am logged in as "admin"
    And the system has the following work items:
      | ID     | Title                 | Description          | Type    | Priority | Status      | Assignee | Due Date   |
      | WI-101 | Implement login page  | User login page      | FEATURE | HIGH     | IN_PROGRESS | alice    | 2025-05-01 |
      | WI-102 | Fix navigation bug    | Top menu not showing | BUG     | CRITICAL | OPEN        | bob      | 2025-04-20 |
      | WI-103 | Update documentation  | Add new API docs     | TASK    | LOW      | DONE        | charlie  | 2025-04-05 |
      | WI-104 | Add unit tests        | Increase coverage    | TASK    | MEDIUM   | IN_PROGRESS | alice    | 2025-04-30 |
      | WI-105 | Design new logo       | Create brand logo    | FEATURE | LOW      | DONE        | dave     | 2025-03-25 |
      | WI-106 | Security review       | Audit all endpoints  | TASK    | HIGH     | OPEN        | bob      | 2025-05-10 |
      | WI-107 | Database optimization | Improve query perf   | TASK    | MEDIUM   | DONE        | charlie  | 2025-04-01 |
      | WI-108 | Fix login timeout     | Session expires fast | BUG     | HIGH     | DONE        | alice    | 2025-04-10 |

  Scenario: Displaying summary statistics
    When I run the command "rin stats"
    Then the command should execute successfully
    And the output should contain "Work Item Statistics Summary"
    And the output should contain "Total work items: 8"
    And the output should contain "Done: 4"
    And the output should contain "In Progress: 2"
    And the output should contain "Open: 2"
    And the output should contain "Completion Rate: 50.0%"

  Scenario: Displaying detailed statistics
    When I run the command "rin stats all"
    Then the command should execute successfully
    And the output should contain "Work Item Statistics Details"
    And the output should contain "By Priority"
    And the output should contain "CRITICAL: 1"
    And the output should contain "HIGH: 3"
    And the output should contain "MEDIUM: 2"
    And the output should contain "LOW: 2"
    And the output should contain "By Status"
    And the output should contain "By Type"
    And the output should contain "By Assignee"
    And the output should contain "alice: 3"
    
  Scenario: Displaying status distribution
    When I run the command "rin stats distribution status"
    Then the command should execute successfully
    And the output should contain "Status Distribution"
    And the output should contain "DONE: 4 (50.0%)"
    And the output should contain "IN_PROGRESS: 2 (25.0%)"
    And the output should contain "OPEN: 2 (25.0%)"

  Scenario: Displaying priority distribution
    When I run the command "rin stats distribution priority"
    Then the command should execute successfully
    And the output should contain "Priority Distribution"
    And the output should contain "CRITICAL: 1 (12.5%)"
    And the output should contain "HIGH: 3 (37.5%)"
    And the output should contain "MEDIUM: 2 (25.0%)"
    And the output should contain "LOW: 2 (25.0%)"

  Scenario: Displaying type distribution
    When I run the command "rin stats distribution type"
    Then the command should execute successfully
    And the output should contain "Type Distribution"
    And the output should contain "FEATURE: 2 (25.0%)"
    And the output should contain "BUG: 2 (25.0%)"
    And the output should contain "TASK: 4 (50.0%)"

  Scenario: Displaying assignee distribution
    When I run the command "rin stats distribution assignee"
    Then the command should execute successfully
    And the output should contain "Assignee Distribution"
    And the output should contain "alice: 3 (37.5%)"
    And the output should contain "bob: 2 (25.0%)"
    And the output should contain "charlie: 2 (25.0%)"
    And the output should contain "dave: 1 (12.5%)"

  Scenario: Viewing completion metrics
    When I run the command "rin stats detail completion"
    Then the command should execute successfully
    And the output should contain "Completion Metrics"
    And the output should contain "Overall Completion Rate: 50.0%"
    And the output should contain "Average time to completion:"
    And the output should contain "By Priority:"
    And the output should contain "HIGH: 33.3%"
    And the output should contain "MEDIUM: 50.0%"
    And the output should contain "LOW: 100.0%"

  Scenario: Viewing workflow metrics
    When I run the command "rin stats detail workflow"
    Then the command should execute successfully
    And the output should contain "Workflow Metrics"
    And the output should contain "Average time in each status:"
    And the output should contain "OPEN:"
    And the output should contain "IN_PROGRESS:"
    And the output should contain "State transition frequency:"

  Scenario: Viewing statistics in JSON format
    When I run the command "rin stats --format=json"
    Then the command should execute successfully
    And the output should be valid JSON
    And the JSON output should contain "totalCount"
    And the JSON output should contain "completionRate"
    And the JSON output should contain "statusDistribution"

  Scenario: Viewing statistics with limited output
    When I run the command "rin stats --limit=2"
    Then the command should execute successfully
    And the output should contain "Work Item Statistics Summary"
    And the output should contain "Top 2 items by priority"
    And the output should contain "Top 2 items by status"
    And the output should not contain "Top 3 items by priority"

  Scenario: Generating statistics dashboard
    When I run the command "rin stats dashboard"
    Then the command should execute successfully
    And the output should contain "Project Dashboard"
    And the output should contain "Summary Statistics"
    And the output should contain "Progress Chart"
    And the output should contain "Priority Distribution"
    And the output should contain "Status Distribution"