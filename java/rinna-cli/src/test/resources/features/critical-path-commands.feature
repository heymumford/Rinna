Feature: Critical Path commands
  As a user of the Rinna CLI
  I want to analyze the critical path of my project
  So that I can identify bottlenecks and optimize workflow

  Background:
    Given I am logged in as "admin"
    And the system has the following work items:
      | ID     | Title                  | Description               | Type    | Priority | Status      | Assignee | Due Date   |
      | WI-101 | Implement login page   | User login page           | FEATURE | HIGH     | IN_PROGRESS | alice    | 2025-05-01 |
      | WI-102 | Fix navigation bug     | Top menu not showing      | BUG     | CRITICAL | OPEN        | bob      | 2025-04-20 |
      | WI-103 | Update documentation   | Add new API docs          | TASK    | LOW      | DONE        | charlie  | 2025-04-05 |
      | WI-104 | Add unit tests         | Increase coverage         | TASK    | MEDIUM   | IN_PROGRESS | alice    | 2025-04-30 |
      | WI-105 | Design new logo        | Create brand logo         | FEATURE | LOW      | DONE        | dave     | 2025-03-25 |
      | WI-106 | Security review        | Audit all endpoints       | TASK    | HIGH     | OPEN        | bob      | 2025-05-10 |
      | WI-107 | Database optimization  | Improve query perf        | TASK    | MEDIUM   | DONE        | charlie  | 2025-04-01 |
      | WI-108 | Fix login timeout      | Session expires fast      | BUG     | HIGH     | DONE        | alice    | 2025-04-10 |
    And the system has the following work item dependencies:
      | Work Item | Depends On |
      | WI-101    | WI-102     |
      | WI-101    | WI-108     |
      | WI-104    | WI-101     |
      | WI-104    | WI-103     |
      | WI-106    | WI-102     |
      | WI-106    | WI-107     |

  Scenario: Viewing the critical path
    When I run the command "rin path"
    Then the command should execute successfully
    And the output should contain "Critical Path Analysis"
    And the output should contain "WI-102 -> WI-101 -> WI-104"
    And the output should contain "Total path length: 3 items"
    And the output should contain "Estimated total effort: "

  Scenario: Viewing detailed critical path information
    When I run the command "rin path --detailed"
    Then the command should execute successfully
    And the output should contain "Critical Path Analysis (Detailed)"
    And the output should contain "WI-102: Fix navigation bug (OPEN, CRITICAL)"
    And the output should contain "WI-101: Implement login page (IN_PROGRESS, HIGH)"
    And the output should contain "WI-104: Add unit tests (IN_PROGRESS, MEDIUM)"
    And the output should contain "Bottleneck: WI-102 (Status: OPEN)"

  Scenario: Viewing critical path for a specific work item
    When I run the command "rin path --item WI-104"
    Then the command should execute successfully
    And the output should contain "Critical Path for WI-104"
    And the output should contain "WI-102 -> WI-101 -> WI-104"
    And the output should contain "Dependencies:"
    And the output should contain "- Directly depends on: WI-101, WI-103"
    And the output should contain "- Indirectly depends on: WI-102, WI-108"

  Scenario: Viewing blockers
    When I run the command "rin path --blockers"
    Then the command should execute successfully
    And the output should contain "Critical Path Blockers"
    And the output should contain "WI-102: Fix navigation bug (OPEN, CRITICAL)"
    And the output should contain "Blocks: WI-101, WI-106"
    And the output should contain "Impact: 2 directly blocked items, 3 total items affected"

  Scenario: Viewing dependency graph
    When I run the command "rin path --graph"
    Then the command should execute successfully
    And the output should contain "Dependency Graph"
    And the output should contain "WI-102 -> WI-101 -> WI-104"
    And the output should contain "WI-108 -> WI-101"
    And the output should contain "WI-103 -> WI-104"
    And the output should contain "WI-102 -> WI-106"
    And the output should contain "WI-107 -> WI-106"

  Scenario: Viewing critical path with time estimates
    When I run the command "rin path --estimates"
    Then the command should execute successfully
    And the output should contain "Critical Path with Time Estimates"
    And the output should contain "WI-102: Fix navigation bug"
    And the output should contain "Estimated time remaining: "
    And the output should contain "Estimated completion date: "
    And the output should contain "Total critical path time: "

  Scenario: Viewing critical path as JSON
    When I run the command "rin path --format=json"
    Then the command should execute successfully
    And the output should be valid JSON
    And the JSON output should contain "criticalPath"
    And the JSON output should contain "pathLength"
    And the JSON output should contain "bottlenecks"

  Scenario: Viewing critical path when there are no dependencies
    Given the system has no work item dependencies
    When I run the command "rin path"
    Then the command should execute successfully
    And the output should contain "No critical path found"
    And the output should contain "No dependencies detected between work items"

  Scenario: Viewing critical path when the specified item doesn't exist
    When I run the command "rin path --item WI-999"
    Then the command should fail with error code 1
    And the error output should contain "Work item WI-999 not found"