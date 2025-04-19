Feature: Critical path visualization and management
  As a developer or team lead
  I want to visualize and manage the critical path of work items
  So that I can identify blockers and optimize the workflow

  Background:
    Given the Rinna system is initialized

  Scenario: Viewing the critical path for a project
    Given the following work items exist:
      | ID      | Title                       | Type     | Status      | Dependencies    |
      | WI-101  | Design database schema      | TASK     | DONE        |                 |
      | WI-102  | Implement data access layer | TASK     | IN_PROGRESS | WI-101          |
      | WI-103  | Create API endpoints        | TASK     | TO_DO       | WI-102          |
      | WI-104  | Create frontend views       | TASK     | TO_DO       | WI-103          |
      | WI-105  | Implement auth service      | TASK     | TO_DO       |                 |
      | WI-106  | Add admin dashboard         | FEATURE  | TO_DO       | WI-104, WI-105  |
      | WI-107  | Manual testing              | TASK     | TO_DO       | WI-106          |
      | WI-108  | Automated testing           | TASK     | TO_DO       | WI-106          |
      | WI-109  | Deployment prep             | TASK     | TO_DO       | WI-107, WI-108  |
    When I request the critical path for the project
    Then the critical path should be displayed in order:
      | WI-101 | WI-102 | WI-103 | WI-104 | WI-106 | WI-107 | WI-109 |
    And each item on the critical path should be marked as critical

  Scenario: Adding a dependency between work items
    Given a work item "WI-201" with title "Setup dev environment"
    And a work item "WI-202" with title "Configure CI pipeline"
    When I add "WI-201" as a dependency for "WI-202"
    Then "WI-202" should have "WI-201" as a dependency
    And the critical path should include both items in the correct order

  Scenario: Removing a dependency between work items
    Given a work item "WI-301" with title "Design API"
    And a work item "WI-302" with title "Implement API" with dependency "WI-301"
    When I remove the dependency between "WI-302" and "WI-301"
    Then "WI-302" should not have "WI-301" as a dependency
    And the critical path should be updated accordingly

  Scenario: Automatic recalculation of critical path when statuses change
    Given the following work items exist with dependencies:
      | ID      | Title             | Type  | Status      | Dependencies |
      | WI-401  | Task A            | TASK  | IN_PROGRESS |              |
      | WI-402  | Task B            | TASK  | TO_DO       | WI-401       |
      | WI-403  | Task C            | TASK  | TO_DO       | WI-402       |
    When I mark "WI-401" as "DONE"
    Then the critical path should be updated
    And "WI-402" should now be the first active item on the critical path

  Scenario: CLI command for viewing critical path
    Given a project with multiple interconnected work items
    When I run the command "rin path"
    Then the CLI should display the critical path visually
    And it should highlight the currently blocked items
    And it should show the estimated completion time based on dependencies

  Scenario: Visualizing parallel paths
    Given a project with parallel work streams
    When I run the command "rin path --parallel"
    Then the CLI should display multiple critical paths
    And it should show which paths are truly critical versus near-critical
    And it should identify which parallel paths could be safely delayed

  Scenario: Identifying and escalating blockers
    Given the following work items exist with dependencies:
      | ID      | Title             | Type  | Status      | Dependencies | Assigned To |
      | WI-501  | Core module       | TASK  | IN_PROGRESS |              | developer1  |
      | WI-502  | API integration   | TASK  | TO_DO       | WI-501       | developer2  |
      | WI-503  | Frontend module   | TASK  | TO_DO       | WI-502       | developer3  |
    When "WI-501" is flagged as blocked
    Then the system should identify all dependent items as blocked
    And the CLI command "rin blockers" should show the blocked critical path
    And it should suggest escalation actions based on the blocker's impact