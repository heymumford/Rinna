Feature: Import Tasks from Markdown Files
  As a developer
  I want to import tasks from markdown kanban backlogs
  So that I can quickly migrate my existing tasks into Rinna

  Background:
    Given the Rinna system is initialized
    And the test markdown files are prepared

  @positive @import
  Scenario: Successfully import tasks from a well-formatted markdown file
    When I run "rin import test-data/well-formatted.md"
    Then the command should succeed
    And 3 tasks should be imported
    And each task should have the correct title and status
    And the system should display a success message
    When I run "rin list --source=imported"
    Then I should see all imported tasks in the list

  @positive @import
  Scenario: Import tasks with minimal information
    When I run "rin import test-data/minimal-info.md"
    Then the command should succeed
    And 2 tasks should be imported
    And tasks should have default values for missing fields
    And the status should be set to "BACKLOG" for tasks without status

  @positive @import
  Scenario: Import tasks with various status formats
    When I run "rin import test-data/various-statuses.md"
    Then the command should succeed
    And 4 tasks should be imported
    And the system should correctly map "In Progress" to "IN_PROGRESS"
    And the system should correctly map "todo" to "TO_DO"
    And the system should correctly map "DONE" to "DONE"
    And the system should correctly map "blocked" to "BLOCKED"

  @positive @import
  Scenario: Import tasks with priority information
    When I run "rin import test-data/with-priorities.md"
    Then the command should succeed
    And the tasks should have the correct priority levels
    And tasks without priority should have the default priority "MEDIUM"

  @negative @import
  Scenario: Attempt to import from a non-existent file
    When I run "rin import non-existent-file.md"
    Then the command should fail
    And the system should display an error message "File not found: non-existent-file.md"
    And no tasks should be imported

  @negative @import
  Scenario: Attempt to import from a malformed markdown file
    When I run "rin import test-data/malformed.md"
    Then the command should succeed with warnings
    And the system should display a warning message about unrecognized format
    And only recognizable tasks should be imported
    And a report should be generated listing items that couldn't be imported

  @negative @import
  Scenario: Attempt to import from an empty file
    When I run "rin import test-data/empty.md"
    Then the command should fail
    And the system should display an error message "No tasks found in file"
    And no tasks should be imported

  @negative @import
  Scenario: Attempt to import from a non-markdown file
    When I run "rin import test-data/not-markdown.txt"
    Then the command should fail
    And the system should display an error message "File must be a markdown (.md) file"
    And no tasks should be imported

  @positive @bulk
  Scenario: Bulk update imported tasks status
    Given I have imported tasks from "test-data/well-formatted.md"
    When I run "rin bulk --source=imported --set-status=IN_PROGRESS"
    Then the command should succeed
    And all imported tasks should have the status "IN_PROGRESS"
    And the system should display a success message with count of updated tasks

  @positive @bulk
  Scenario: Bulk update imported tasks assignee
    Given I have imported tasks from "test-data/well-formatted.md"
    When I run "rin bulk --source=imported --set-assignee=developer1"
    Then the command should succeed
    And all imported tasks should have the assignee "developer1"
    And the system should display a success message with count of updated tasks

  @positive @bulk
  Scenario: Bulk update with filtering by status
    Given I have imported tasks from "test-data/various-statuses.md"
    When I run "rin bulk --source=imported --status=TO_DO --set-priority=HIGH"
    Then the command should succeed
    And only tasks with status "TO_DO" should have priority set to "HIGH"
    And the system should display a success message with count of updated tasks

  @negative @bulk
  Scenario: Bulk update with invalid status
    Given I have imported tasks from "test-data/well-formatted.md"
    When I run "rin bulk --source=imported --set-status=INVALID_STATUS"
    Then the command should fail
    And the system should display an error message "Invalid status: INVALID_STATUS"
    And no tasks should be updated

  @negative @bulk
  Scenario: Bulk update with invalid priority
    Given I have imported tasks from "test-data/well-formatted.md"
    When I run "rin bulk --source=imported --set-priority=INVALID_PRIORITY"
    Then the command should fail
    And the system should display an error message "Invalid priority: INVALID_PRIORITY"
    And no tasks should be updated

  @negative @bulk
  Scenario: Bulk update with no tasks matching filter
    Given I have imported tasks from "test-data/well-formatted.md"
    When I run "rin bulk --source=imported --status=DONE --set-priority=HIGH"
    Then the command should succeed with warnings
    And the system should display a warning message "No tasks found matching the filter criteria"
    And no tasks should be updated