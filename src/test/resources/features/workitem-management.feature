Feature: Work Item Management
  As a developer using Rinna
  I want to effectively manage my work items
  So that I can track and organize my tasks

  Scenario: Adding and listing work items
    Given there are no work items in the repository
    When I add a new work item with title "Implement user authentication" and type "FEATURE"
    And I add a new work item with title "Fix login bug" and type "BUG" and priority "HIGH"
    And I list all work items
    Then I should see both work items in the list
    And the work item counts should match

  Scenario: Filtering work items
    Given there are work items of different types in the repository
    When I filter work items by type "FEATURE"
    Then I should only see work items of type "FEATURE"
    When I filter work items by status "TODO"
    Then I should only see work items with status "TODO"
    When I filter work items by priority "HIGH"
    Then I should only see work items with priority "HIGH"

  Scenario: Adding a work item with all attributes
    When I add a work item with the following attributes:
      | title       | Refactor database layer       |
      | type        | TASK                          |
      | description | Improve code and performance  |
      | priority    | MEDIUM                        |
      | assignee    | john                          |
      | status      | IN_PROGRESS                   |
    Then the work item should be added successfully
    And when I list all work items, the new item should be included