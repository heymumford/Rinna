Feature: Work input interface and prioritization
  To efficiently manage and prioritize software work
  As a development team
  We need a flexible input interface with dynamic prioritization

  Background:
    Given the Rinna system is initialized

  # Basic Input Scenarios
  Scenario: Submitting a production incident as high priority
    When a critical production incident "Database connection failure" is submitted
    Then a work item should be created with type "BUG" and priority "HIGH"
    And the work item should have title "Database connection failure"
    And the work item should be in "FOUND" state

  Scenario: Submitting a feature request from a product manager
    When a feature request "User export functionality" is submitted with description "Allow users to export their data"
    Then a work item should be created with type "FEATURE" and priority "MEDIUM" 
    And the work item should have title "User export functionality"
    And the work item should have description "Allow users to export their data"
    And the work item should be in "FOUND" state

  Scenario: Submitting a technical debt task
    When a technical task "Refactor authentication module" is submitted with priority "LOW"
    Then a work item should be created with type "CHORE" and priority "LOW"
    And the work item should have title "Refactor authentication module"
    And the work item should be in "FOUND" state

  # Work Hierarchy Scenarios
  Scenario: Creating a user story under an epic
    Given a "GOAL" work item with title "User Management Epic" exists
    When a feature "User profile editing" is added as a child to "User Management Epic"
    Then the feature should be linked to the parent goal
    And the feature should inherit the parent's priority if not specified

  Scenario: Creating tasks under a user story
    Given a "FEATURE" work item with title "User login functionality" exists
    When a technical task "Implement password reset" is added as a child to "User login functionality"
    Then the task should be linked to the parent feature
    And the parent feature should be updated to reflect a child item

  # Batch Input Scenarios
  Scenario: Importing multiple work items from a planning session
    When the following work items are imported:
      | title                      | type    | priority | description                |
      | Improve search performance | FEATURE | HIGH     | Make search results faster |
      | Fix login timeout issue    | BUG     | MEDIUM   | Users get timeout errors   |
      | Update documentation       | CHORE   | LOW      | Update developer docs      |
    Then 3 work items should be created
    And they should maintain their specified priorities and types

  # Queue and Prioritization Scenarios
  Scenario: Auto-prioritization based on type and age
    Given the following work items exist:
      | title               | type    | priority | created_days_ago |
      | Old low bug         | BUG     | LOW      | 10               |
      | New high feature    | FEATURE | HIGH     | 1                |
      | Medium age chore    | CHORE   | MEDIUM   | 5                |
    When the work queue is prioritized automatically
    Then the order of items should be "New high feature,Old low bug,Medium age chore"

  Scenario: Urgent production issue trumps all other priorities
    Given a work queue with several items
    When a production incident is reported with "URGENT" flag
    Then the production incident should be placed at the top of the queue
    And the team should be notified about the urgent item

  # Input Source Scenarios
  Scenario: Creating a work item from email integration
    When an email with subject "New feature request: Dark mode" is received
    Then the system should create a work item from the email
    And the work item should have the email content as its description
    And the work item should be tagged with "source:email"

  Scenario: Creating a work item from Slack command
    When a Slack message "/rinna add bug 'Mobile app crashes on startup'" is received
    Then the system should create a work item from the Slack command
    And the work item should have type "BUG"
    And the work item should be tagged with "source:slack"

  # Input Validation Scenarios
  Scenario: Rejecting a work item with insufficient information
    When an invalid work item request is submitted without a title
    Then the system should reject the request
    And provide an error message about missing title

  Scenario: Validating parent-child type relationships
    Given a "CHORE" work item with title "Fix linting errors" exists
    When attempting to add a feature as a child to "Fix linting errors"
    Then the system should reject the invalid hierarchy
    And provide an error message about invalid parent-child relationship

  # Workflow Integration Scenarios
  Scenario: Automatically transitioning imported items
    Given the system is configured to auto-triage imported items
    When a batch of work items is imported from JIRA
    Then the items should be created in "TRIAGED" state
    And each item should have an automatic workflow comment about the import

  Scenario: Dynamically adjusting queue based on team capacity
    Given the team has a capacity of 5 story points per developer
    And there are 3 active developers
    When the work queue is prioritized based on team capacity
    Then the top items should not exceed 15 story points in total