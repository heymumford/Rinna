Feature: API Integration Features
  To enable third-party system integration
  As a developer or system administrator
  I need an API with authentication and webhooks

  Background:
    Given the Rinna system is initialized

  @json-api
  Scenario: Creating a work item via the API
    Given an API authentication token "ri-dev-x7b3c9d5" for project "webshop"
    When the following JSON payload is submitted with the token:
      """
      {
        "type": "FEATURE",
        "title": "Add shopping cart persistence",
        "description": "Implement browser storage to persist shopping cart between sessions",
        "priority": "HIGH",
        "metadata": {
          "source": "product_manager",
          "estimated_time": "3d",
          "requested_by": "customer_success"
        }
      }
      """
    Then a work item should be created with the specified attributes
    And the work item should be associated with project "webshop"
    And the work item should include all the provided metadata

  @json-api
  Scenario: Retrieving a work item via the API
    Given an API authentication token "ri-dev-a1b2c3d4" for project "webshop"
    And a work item with title "Implement login system" exists in project "webshop"
    When the API client requests the work item details
    Then the API should return the work item with all its attributes
    And the response should include a valid JSON structure
  
  @json-api
  Scenario: Listing work items via the API
    Given an API authentication token "ri-dev-a1b2c3d4" for project "webshop"
    And the following work items exist in project "webshop":
      | title                       | type    | priority | status  |
      | Implement login system      | FEATURE | HIGH     | IN_DEV  |
      | Fix payment gateway timeout | BUG     | HIGH     | TRIAGED |
      | Update documentation        | CHORE   | LOW      | FOUND   |
    When the API client requests all work items
    Then the API should return a list of 3 work items
    And the response should include pagination information
  
  @json-api
  Scenario: Filtering work items by status
    Given an API authentication token "ri-dev-a1b2c3d4" for project "webshop"
    And the following work items exist in project "webshop":
      | title                       | type    | priority | status  |
      | Implement login system      | FEATURE | HIGH     | IN_DEV  |
      | Fix payment gateway timeout | BUG     | HIGH     | TRIAGED |
      | Update documentation        | CHORE   | LOW      | FOUND   |
    When the API client requests work items with status "TRIAGED"
    Then the API should return a list of 1 work item
    And the returned item should have title "Fix payment gateway timeout"
  
  @json-api
  Scenario: Transitioning a work item via the API
    Given an API authentication token "ri-dev-a1b2c3d4" for project "webshop"
    And a work item with title "Implement login system" exists in project "webshop" with status "FOUND"
    When the API client submits a transition request to "IN_DEV"
    Then the work item should be updated with status "IN_DEV"
    And the API should return the updated work item
  
  @json-api
  Scenario: Rejecting an unauthorized API request
    Given an invalid API authentication token "invalid-token" for project "webshop" 
    When a JSON payload is submitted with the invalid token
    Then the system should reject the request with a 401 status code
    And provide an authentication error message
  
  @webhook
  Scenario: Processing a GitHub pull request webhook
    Given a GitHub webhook secret "gh-webhook-secret-1234" is configured for project "webshop"
    When a GitHub pull request webhook is received with:
      """
      {
        "action": "opened",
        "pull_request": {
          "title": "Add product search functionality",
          "body": "This PR implements product search with filtering by category.",
          "user": {
            "login": "developer1"
          },
          "html_url": "https://github.com/org/webshop/pull/42",
          "additions": 350,
          "deletions": 15,
          "changed_files": 7
        },
        "repository": {
          "full_name": "org/webshop"
        }
      }
      """
    Then a work item should be created with type "FEATURE" and title "PR: Add product search functionality"
    And the work item should have the tag "source:github"
    And the work item should have the tag "github_pr:42"
    And the work item should have the PR description and URL in its description
  
  @webhook
  Scenario: Processing a GitHub workflow failure webhook
    Given a GitHub webhook secret "gh-webhook-secret-1234" is configured for project "webshop"
    When a GitHub workflow run webhook is received with status "failure":
      """
      {
        "action": "completed",
        "workflow_run": {
          "name": "Integration Tests",
          "head_branch": "feature/add-product-search",
          "conclusion": "failure",
          "html_url": "https://github.com/org/webshop/actions/runs/123456",
          "check_suite_url": "https://api.github.com/repos/org/webshop/check-suites/654321"
        },
        "repository": {
          "full_name": "org/webshop"
        }
      }
      """
    Then a work item should be created with type "BUG" and priority "HIGH"
    And the work item should have the tag "source:github_ci"
    And the work item should have the tag "branch:feature/add-product-search"
    And the work item should have a link to the failed workflow run
  
  @webhook
  Scenario: Rejecting a webhook with invalid signature
    Given a GitHub webhook secret "gh-webhook-secret-1234" is configured for project "webshop"
    When a GitHub webhook is received with an invalid signature
    Then the system should reject the request with a 401 status code
    And provide a signature validation error message
  
  @api-token
  Scenario: Creating an API token for a project
    Given a user with admin permissions for project "webshop"
    When the user creates an API token with description "Integration testing token"
    Then a new API token should be generated
    And the token should be associated with project "webshop"
    And the token should be active and valid for 30 days
  
  @api-token
  Scenario: Revoking an API token
    Given a user with admin permissions for project "webshop"
    And an existing API token "ri-dev-f72b159e4a3c" for project "webshop"
    When the user revokes the token
    Then the token should be marked as inactive
    And subsequent API requests using that token should be rejected
  
  @webhook-config
  Scenario: Configuring a GitHub webhook for a project
    Given a user with admin permissions for project "webshop"
    When the user configures a GitHub webhook with:
      | secret     | gh-webhook-secret-5678 |
      | description| Production repository webhook |
    Then a new webhook configuration should be created
    And the webhook should be associated with project "webshop"
    And the system should display the webhook URL to use in GitHub
  
  @webhook-config
  Scenario: Disabling a webhook configuration
    Given a user with admin permissions for project "webshop"
    And an existing webhook configuration for "github" in project "webshop"
    When the user disables the webhook configuration
    Then the webhook should be marked as inactive
    And subsequent webhook requests should be rejected