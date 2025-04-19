Feature: Work Item Creation and Management
  As a developer
  I want to create and manage work items
  So that I can track my development tasks

  @acceptance
  Scenario: Creating a valid work item
    Given a new work item with title "Fix authentication bug"
    When I create the work item
    Then the work item should be created successfully

  @acceptance
  Scenario: Creating an invalid work item
    Given a new work item with title ""
    When I create the work item
    Then I should see an error about empty title
