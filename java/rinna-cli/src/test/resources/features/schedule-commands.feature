Feature: Schedule Command
  As a Rinna user
  I want to manage scheduled reports
  So that I can automate report generation and delivery

  Background:
    Given a user "testuser" with basic permissions
    And the ReportScheduler is initialized

  Scenario: List scheduled reports
    Given there are existing scheduled reports
    When the user runs "schedule list"
    Then the command should succeed
    And the output should contain "Scheduled Reports"
    And the command should track operation details
    And the command should track hierarchical operations

  Scenario: List scheduled reports with JSON output
    Given there are existing scheduled reports
    When the user runs "schedule list --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain a "count" field
    And the JSON should contain a "reports" array
    And the command should track format parameters

  Scenario: List scheduled reports with verbose output
    Given there are existing scheduled reports
    When the user runs "schedule list --verbose"
    Then the command should succeed
    And the output should contain "Scheduled Reports"
    And the output should contain "Next Run:"
    And the command should track the verbose parameter

  Scenario: Add a daily scheduled report
    When the user runs "schedule add --name='Daily Status' --type=daily --time=09:00 --report=status"
    Then the command should succeed
    And the output should contain "Scheduled report added successfully"
    And the command should track hierarchical operations with "schedule-add"
    And the command should track all report parameters

  Scenario: Add a weekly scheduled report
    When the user runs "schedule add --name='Weekly Summary' --type=weekly --day=monday --time=08:00 --report=summary"
    Then the command should succeed
    And the output should contain "Scheduled report added successfully"
    And the command should track hierarchical operations with "schedule-add"
    And the command should track all report parameters

  Scenario: Add a monthly scheduled report
    When the user runs "schedule add --name='Monthly Report' --type=monthly --date=1 --time=07:00 --report=metrics"
    Then the command should succeed
    And the output should contain "Scheduled report added successfully"
    And the command should track hierarchical operations with "schedule-add"
    And the command should track all report parameters

  Scenario: Add a scheduled report with email delivery
    When the user runs "schedule add --name='Email Report' --type=daily --time=10:00 --report=status --email --email-to=team@example.com --email-subject='Status Report'"
    Then the command should succeed
    And the output should contain "Scheduled report added successfully"
    And the command should track hierarchical operations with "schedule-add"
    And the command should track all report parameters

  Scenario: Add a scheduled report with JSON output
    When the user runs "schedule add --name='JSON Report' --type=daily --time=11:00 --report=status --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain "success"
    And the JSON should contain an "id" field
    And the command should track format parameters

  Scenario: Attempt to add a report without a name
    When the user runs "schedule add --type=daily --time=09:00 --report=status"
    Then the command should fail
    And the error output should indicate name is required
    And the command should track operation failure

  Scenario: Attempt to add a report without a schedule type
    When the user runs "schedule add --name='Test Report' --time=09:00 --report=status"
    Then the command should fail
    And the error output should indicate schedule type is required
    And the command should track operation failure

  Scenario: Attempt to add a report without a time
    When the user runs "schedule add --name='Test Report' --type=daily --report=status"
    Then the command should fail
    And the error output should indicate time is required
    And the command should track operation failure

  Scenario: Attempt to add a report without a report type
    When the user runs "schedule add --name='Test Report' --type=daily --time=09:00"
    Then the command should fail
    And the error output should indicate report type is required
    And the command should track operation failure

  Scenario: Attempt to add a weekly report without day of week
    When the user runs "schedule add --name='Weekly Report' --type=weekly --time=09:00 --report=status"
    Then the command should fail
    And the error output should indicate day of week is required
    And the command should track operation failure

  Scenario: Attempt to add a monthly report without day of month
    When the user runs "schedule add --name='Monthly Report' --type=monthly --time=09:00 --report=status"
    Then the command should fail
    And the error output should indicate day of month is required
    And the command should track operation failure

  Scenario: Show a scheduled report
    Given there is a scheduled report with ID "schedule-123"
    When the user runs "schedule show --id=schedule-123"
    Then the command should succeed
    And the output should contain the report details
    And the command should track hierarchical operations with "schedule-show"
    And the command should track the report ID

  Scenario: Show a scheduled report with JSON output
    Given there is a scheduled report with ID "schedule-123"
    When the user runs "schedule show --id=schedule-123 --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain the report details
    And the command should track format parameters

  Scenario: Show a scheduled report with verbose output
    Given there is a scheduled report with ID "schedule-123"
    When the user runs "schedule show --id=schedule-123 --verbose"
    Then the command should succeed
    And the output should contain the report details
    And the output should contain "Next Run:"
    And the command should track the verbose parameter

  Scenario: Attempt to show a report without an ID
    When the user runs "schedule show"
    Then the command should fail
    And the error output should indicate ID is required
    And the command should track operation failure

  Scenario: Attempt to show a non-existent report
    When the user runs "schedule show --id=non-existent"
    Then the command should fail
    And the error output should indicate report not found
    And the command should track operation failure

  Scenario: Remove a scheduled report
    Given there is a scheduled report with ID "schedule-123"
    When the user runs "schedule remove --id=schedule-123"
    Then the command should succeed
    And the output should contain "Scheduled report removed successfully"
    And the command should track hierarchical operations with "schedule-remove"
    And the command should track the report ID

  Scenario: Remove a scheduled report with JSON output
    Given there is a scheduled report with ID "schedule-123"
    When the user runs "schedule remove --id=schedule-123 --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain "success"
    And the JSON should contain the report ID
    And the command should track format parameters

  Scenario: Attempt to remove a report without an ID
    When the user runs "schedule remove"
    Then the command should fail
    And the error output should indicate ID is required
    And the command should track operation failure

  Scenario: Attempt to remove a non-existent report
    When the user runs "schedule remove --id=non-existent"
    Then the command should fail
    And the error output should indicate report not found
    And the command should track operation failure

  Scenario: Start the report scheduler
    When the user runs "schedule start"
    Then the command should succeed
    And the output should contain "Report scheduler started"
    And the command should track hierarchical operations with "schedule-start"

  Scenario: Start the report scheduler with JSON output
    When the user runs "schedule start --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain "status":"started"
    And the command should track format parameters

  Scenario: Stop the report scheduler
    When the user runs "schedule stop"
    Then the command should succeed
    And the output should contain "Report scheduler stopped"
    And the command should track hierarchical operations with "schedule-stop"

  Scenario: Stop the report scheduler with JSON output
    When the user runs "schedule stop --format=json"
    Then the command should succeed
    And the output should be valid JSON
    And the JSON should contain "status":"stopped"
    And the command should track format parameters

  Scenario: Execute an unknown action
    When the user runs "schedule unknown-action"
    Then the command should fail
    And the error output should indicate unknown action
    And the error output should show valid actions
    And the command should track operation failure