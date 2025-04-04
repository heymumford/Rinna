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
    
  # JSON API Integration Scenarios
  @json-api
  Scenario: Accepting a valid JSON payload with authentication
    Given an API authentication token "ri-5e7a9b3f2c8d" for project "billing-system"
    When the following JSON payload is submitted with the token:
      """
      {
        "type": "FEATURE",
        "title": "Support for cryptocurrency payments",
        "description": "Add support for Bitcoin and Ethereum payments",
        "priority": "HIGH",
        "metadata": {
          "source": "product_roadmap",
          "estimated_points": "8",
          "requested_by": "finance_team"
        }
      }
      """
    Then a work item should be created with the specified attributes
    And the work item should be associated with project "billing-system"
    And the work item should include all the provided metadata
  
  @json-api
  Scenario: Rejecting an unauthorized JSON payload
    Given an invalid API authentication token "invalid-token" for project "billing-system"
    When a JSON payload is submitted with the invalid token
    Then the system should reject the request with a 401 status code
    And provide an authentication error message
  
  @webhook
  Scenario: Processing a GitHub webhook for a new pull request
    Given a GitHub webhook secret "gh-webhook-secret-1234" is configured for project "data-platform"
    When a GitHub pull request webhook is received with:
      """
      {
        "action": "opened",
        "pull_request": {
          "title": "Add data validation for CSV imports",
          "body": "This PR adds validation for CSV files before import to prevent data corruption.",
          "user": {
            "login": "developer1"
          },
          "html_url": "https://github.com/org/data-platform/pull/123",
          "additions": 450,
          "deletions": 22,
          "changed_files": 5
        },
        "repository": {
          "full_name": "org/data-platform"
        }
      }
      """
    Then a work item should be created with type "FEATURE" and title "PR: Add data validation for CSV imports"
    And the work item should have the tag "source:github"
    And the work item should have the tag "github_pr:123"
    And the work item should have the PR description and URL in its description
  
  @webhook
  Scenario: Processing a GitHub webhook for a failed CI build
    Given a GitHub webhook secret "gh-webhook-secret-1234" is configured for project "data-platform"
    When a GitHub workflow run webhook is received with status "failure":
      """
      {
        "action": "completed",
        "workflow_run": {
          "name": "CI Pipeline",
          "head_branch": "feature/new-export-format",
          "conclusion": "failure",
          "html_url": "https://github.com/org/data-platform/actions/runs/456789",
          "check_suite_url": "https://api.github.com/repos/org/data-platform/check-suites/123456"
        },
        "repository": {
          "full_name": "org/data-platform"
        }
      }
      """
    Then a work item should be created with type "BUG" and priority "HIGH"
    And the work item should have the tag "source:github_ci"
    And the work item should have the tag "branch:feature/new-export-format"
    And the work item should have a link to the failed workflow run
  
  # Local Development Client Scenarios
  @client
  Scenario: Tracking local repository changes with the Rinna client
    Given a developer has installed the Rinna local client in their repository
    And the client is configured with API token "dev-client-token" for project "user-portal"
    When the developer runs tests that fail locally
    Then the client should send a report to Rinna with:
      | attribute      | value                                 |
      | type           | BUG                                   |
      | title          | Local test failure: UserAuthTests     |
      | branch         | feature/improve-auth                  |
      | failing_tests  | testUserRegistration, testLoginFlow   |
      | status         | LOCAL_ONLY                            |
    And the work item should be visible only to the developer until pushed
  
  @client
  Scenario: Synchronizing local work with the central Rinna system
    Given a developer has local work items tracked by the Rinna client
    When the developer pushes their branch to the remote repository
    Then the local work items should be synchronized with the central Rinna system
    And the items should transition from "LOCAL_ONLY" to "SHARED" status
    And other team members should now be able to see these work items