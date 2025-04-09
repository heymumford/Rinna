Feature: Add Command
  As a Rinna user
  I want to add new work items to the system
  So that I can track and manage my work

  Background:
    Given a user "testuser" with basic permissions

  Scenario: Add a work item with title only
    When the user runs "add --title='New task'"
    Then the command should succeed
    And the output should contain a work item ID
    And the output should contain "Title: New task"
    And the output should contain "Type: TASK"
    And the output should contain "Priority: MEDIUM"
    And the output should contain "Status: CREATED"
    And the command should track operation details

  Scenario: Add a work item with title and description
    When the user runs "add --title='Documentation task' --description='Update the README file with new features'"
    Then the command should succeed
    And the output should contain a work item ID
    And the output should contain "Title: Documentation task"
    And the command should track operation details

  Scenario: Add a work item with custom type
    When the user runs "add --title='Fix login issue' --type=BUG"
    Then the command should succeed
    And the output should contain a work item ID
    And the output should contain "Type: BUG"
    And the command should track the type parameter

  Scenario: Add a work item with custom priority
    When the user runs "add --title='Security vulnerability' --priority=HIGH"
    Then the command should succeed
    And the output should contain a work item ID
    And the output should contain "Priority: HIGH"
    And the command should track the priority parameter

  Scenario: Add a work item with assignee
    When the user runs "add --title='Assigned task' --assignee=john.doe"
    Then the command should succeed
    And the output should contain a work item ID
    And the output should contain "Assignee: john.doe"
    And the command should track the assignee parameter

  Scenario: Add a work item with project
    When the user runs "add --title='Project task' --project=backend"
    Then the command should succeed
    And the output should contain a work item ID
    And the output should contain "Project: backend"
    And the command should track the project parameter

  Scenario: Add a work item with multiple parameters
    When the user runs "add --title='Complex task' --type=FEATURE --priority=HIGH --assignee=jane.doe --project=frontend"
    Then the command should succeed
    And the output should contain a work item ID
    And the output should contain "Title: Complex task"
    And the output should contain "Type: FEATURE"
    And the output should contain "Priority: HIGH"
    And the output should contain "Assignee: jane.doe"
    And the output should contain "Project: frontend"
    And the command should track all parameters

  Scenario: Add a work item with JSON output
    When the user runs "add --title='JSON output task' --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain "result":"success"
    And the JSON should contain an item_id field
    And the command should track the format parameter

  Scenario: Add a work item with verbose output
    When the user runs "add --title='Verbose task' --description='Test description' --verbose"
    Then the command should succeed
    And the output should contain a work item ID
    And the output should contain "Description:"
    And the output should contain "Test description"
    And the output should contain "Operation ID:"
    And the command should track the verbose parameter

  Scenario: Attempt to add a work item without title
    When the user runs "add" without a title
    Then the command should fail
    And the error output should indicate title is required
    And the command should track operation failure

  Scenario: Add a work item with current user as reporter
    Given the current user is "current.user"
    When the user runs "add --title='Reporter test'"
    Then the command should succeed
    And the output should contain a work item ID
    And when verbose output is requested, the reporter should be "current.user"

  Scenario: Add a work item with current project as default
    Given the current project is "default-project"
    When the user runs "add --title='Project default test'"
    Then the command should succeed
    And the output should contain a work item ID
    And the output should contain "Project: default-project"