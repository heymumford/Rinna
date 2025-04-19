@bdd @report
Feature: Report Command Functionality
  As a user of the Rinna CLI
  I want to generate various reports about work items
  So that I can analyze project status and progress

  Background:
    Given I am logged in as "testuser"
    And the system has the following work items:
      | ID      | Title                     | Description                            | Type | Priority | Status      | Assignee  | Due Date    |
      | WI-1001 | Authentication Feature    | Implement JWT authentication           | TASK | HIGH     | READY       | testuser  | 2025-05-01  |
      | WI-1002 | Bug in Payment Module     | Fix transaction issues after payment   | BUG  | MEDIUM   | IN_PROGRESS | bob       | 2025-04-20  |
      | WI-1003 | Update Documentation      | Add API reference documentation        | TASK | LOW      | DONE        | charlie   | 2025-04-10  |
      | WI-1004 | Security Review           | Review security measures               | TASK | HIGH     | READY       | testuser  | 2025-05-15  |
      | WI-1005 | Performance Optimization  | Improve database query performance     | TASK | MEDIUM   | READY       | testuser  | 2025-05-05  |
      | WI-1006 | UI Improvements           | Enhance user interface                 | TASK | LOW      | IN_PROGRESS | alice     | 2025-05-10  |
      | WI-1007 | Critical Security Issue   | Fix XSS vulnerability                  | BUG  | CRITICAL | IN_PROGRESS | bob       | 2025-04-15  |
      | WI-1008 | Database Schema Updates   | Update database schema for new feature | TASK | MEDIUM   | DONE        | charlie   | 2025-04-05  |
      | WI-1009 | User Management           | Implement user roles and permissions   | TASK | HIGH     | BLOCKED     | testuser  | 2025-05-20  |
      | WI-1010 | API Rate Limiting         | Implement rate limiting for API        | TASK | MEDIUM   | READY       | alice     | 2025-06-01  |

  @smoke
  Scenario: Generating a basic summary report
    When I run the command "rin report summary"
    Then the command should execute successfully
    And the output should contain "Work Item Summary Report"
    And the output should contain "Total Items: 10"
    And the output should contain "READY: 4"
    And the output should contain "IN_PROGRESS: 3"
    And the output should contain "DONE: 2"
    And the output should contain "BLOCKED: 1"

  Scenario: Generating a summary report with specific format
    When I run the command "rin report summary --format json"
    Then the command should execute successfully
    And the output should be valid JSON
    And the JSON output should contain "workItemCount"
    And the JSON output should contain "statusDistribution"

  Scenario: Generating a detailed report
    When I run the command "rin report detailed"
    Then the command should execute successfully
    And the output should contain "Detailed Work Item Report"
    And the output should contain "WI-1001"
    And the output should contain "Authentication Feature"
    And the output should contain "WI-1002"
    And the output should contain "Bug in Payment Module"
    And the output should show all work item fields including description and due date

  Scenario: Generating a status report
    When I run the command "rin report status"
    Then the command should execute successfully
    And the output should contain "Status Distribution Report"
    And the output should contain "READY: 4 (40%)"
    And the output should contain "IN_PROGRESS: 3 (30%)"
    And the output should contain "DONE: 2 (20%)"
    And the output should contain "BLOCKED: 1 (10%)"

  Scenario: Generating a progress report
    When I run the command "rin report progress"
    Then the command should execute successfully
    And the output should contain "Project Progress Report"
    And the output should contain "Completion Rate: 20%"
    And the output should contain "Items in Progress: 3"
    And the output should contain "Items Remaining: 5"

  Scenario: Generating an assignee report
    When I run the command "rin report assignee"
    Then the command should execute successfully
    And the output should contain "Work Items by Assignee"
    And the output should contain "testuser: 4"
    And the output should contain "bob: 2"
    And the output should contain "charlie: 2"
    And the output should contain "alice: 2"

  Scenario: Generating a priority report
    When I run the command "rin report priority"
    Then the command should execute successfully
    And the output should contain "Work Items by Priority"
    And the output should contain "CRITICAL: 1"
    And the output should contain "HIGH: 3"
    And the output should contain "MEDIUM: 4"
    And the output should contain "LOW: 2"

  Scenario: Generating an overdue report
    Given today is "2025-04-16"
    When I run the command "rin report overdue"
    Then the command should execute successfully
    And the output should contain "Overdue Work Items Report"
    And the output should contain "WI-1007"
    And the output should contain "Critical Security Issue"
    And the output should contain "1 day(s) overdue"
    And the output should contain "WI-1002"
    And the output should contain "Bug in Payment Module"
    And the output should contain "4 day(s) overdue"
    And the output should not contain "WI-1001"

  Scenario: Generating a report with filtering by status
    When I run the command "rin report detailed --status READY"
    Then the command should execute successfully
    And the output should contain "Detailed Work Item Report (Filtered by Status: READY)"
    And the output should contain "WI-1001"
    And the output should contain "WI-1004"
    And the output should contain "WI-1005"
    And the output should contain "WI-1010"
    And the output should not contain "WI-1002"
    And the output should not contain "WI-1003"

  Scenario: Generating a report with filtering by assignee
    When I run the command "rin report detailed --assignee testuser"
    Then the command should execute successfully
    And the output should contain "Detailed Work Item Report (Filtered by Assignee: testuser)"
    And the output should contain "WI-1001"
    And the output should contain "WI-1004"
    And the output should contain "WI-1005"
    And the output should contain "WI-1009"
    And the output should not contain "WI-1002"
    And the output should not contain "WI-1003"

  Scenario: Generating a report with filtering by priority
    When I run the command "rin report detailed --priority HIGH"
    Then the command should execute successfully
    And the output should contain "Detailed Work Item Report (Filtered by Priority: HIGH)"
    And the output should contain "WI-1001"
    And the output should contain "WI-1004"
    And the output should contain "WI-1009"
    And the output should not contain "WI-1002"
    And the output should not contain "WI-1003"

  Scenario: Generating a burndown report
    When I run the command "rin report burndown"
    Then the command should execute successfully
    And the output should contain "Burndown Report"
    And the output should contain "Start Date"
    And the output should contain "End Date"
    And the output should contain "Initial Count"
    And the output should contain "Current Count"
    And the output should contain "Ideal Burndown"
    And the output should contain "Actual Burndown"

  Scenario: Generating an activity report
    When I run the command "rin report activity"
    Then the command should execute successfully
    And the output should contain "Activity Report"
    And the output should contain "Recent Changes"
    And the output should list recent work item changes

  Scenario: Exporting a report to a file
    When I run the command "rin report summary --output report.txt"
    Then the command should execute successfully
    And the output should contain "Report exported to 'report.txt'"
    And the file "report.txt" should exist
    And the file "report.txt" should contain "Work Item Summary Report"

  Scenario: Exporting a report with automatic file extension
    When I run the command "rin report summary --output report --format json"
    Then the command should execute successfully
    And the output should contain "Report exported to 'report.json'"
    And the file "report.json" should exist
    And the file "report.json" should contain valid JSON

  Scenario: Scheduling a report for periodic generation
    When I run the command "rin report schedule daily summary --email admin@example.com"
    Then the command should execute successfully
    And the output should contain "Report scheduled successfully"
    And the scheduled report should have the following properties:
      | type      | summary               |
      | frequency | daily                 |
      | email     | admin@example.com     |
      | format    | TEXT                  |

  Scenario: Modifying a scheduled report
    Given I have a scheduled report with ID "report-123" and the following properties:
      | type      | summary               |
      | frequency | daily                 |
      | email     | admin@example.com     |
      | format    | TEXT                  |
    When I run the command "rin report schedule update report-123 --frequency weekly --format HTML"
    Then the command should execute successfully
    And the output should contain "Scheduled report updated successfully"
    And the scheduled report "report-123" should have the following properties:
      | frequency | weekly                |
      | format    | HTML                  |

  Scenario: Listing scheduled reports
    Given I have the following scheduled reports:
      | ID          | Type     | Frequency | Email               | Format |
      | report-123  | summary  | daily     | admin@example.com   | TEXT   |
      | report-456  | detailed | weekly    | manager@example.com | HTML   |
    When I run the command "rin report schedule list"
    Then the command should execute successfully
    And the output should contain "Scheduled Reports"
    And the output should contain "report-123"
    And the output should contain "summary"
    And the output should contain "daily"
    And the output should contain "admin@example.com"
    And the output should contain "report-456"
    And the output should contain "detailed"
    And the output should contain "weekly"
    And the output should contain "manager@example.com"

  Scenario: Deleting a scheduled report
    Given I have a scheduled report with ID "report-123"
    When I run the command "rin report schedule delete report-123"
    Then the command should execute successfully
    And the output should contain "Scheduled report deleted successfully"
    And the scheduled report "report-123" should be deleted

  Scenario: Error handling for invalid report type
    When I run the command "rin report invalid-type"
    Then the command should fail with error code 1
    And the error output should contain "Error: Invalid report type 'invalid-type'"
    And the error output should contain "Available report types:"

  Scenario: Error handling for invalid format
    When I run the command "rin report summary --format invalid-format"
    Then the command should fail with error code 1
    And the error output should contain "Error: Invalid format 'invalid-format'"
    And the error output should contain "Available formats:"

  Scenario: Error handling for nonexistent scheduled report
    When I run the command "rin report schedule delete nonexistent-id"
    Then the command should fail with error code 1
    And the error output should contain "Error: Scheduled report with ID 'nonexistent-id' not found"

  Scenario: Using a custom template for a report
    Given I have a custom template "my-template.html" with the following content:
      """
      <!DOCTYPE html>
      <html>
      <head>
        <title>{{ title }}</title>
        <style>
          body { font-family: Arial, sans-serif; }
          .item { border: 1px solid #ccc; padding: 10px; margin-bottom: 10px; }
          .high { background-color: #ffe0e0; }
          .medium { background-color: #fffbe0; }
          .low { background-color: #e0ffe0; }
        </style>
      </head>
      <body>
        <h1>{{ title }}</h1>
        <p>Generated on: {{ date }}</p>
        <p>Total Items: {{ totalItems }}</p>
        
        <div class="items">
          {{#each items}}
          <div class="item {{priority}}">
            <h3>{{ id }}: {{ title }}</h3>
            <p>{{ description }}</p>
            <p>Status: {{ status }}, Assignee: {{ assignee }}</p>
          </div>
          {{/each}}
        </div>
      </body>
      </html>
      """
    When I run the command "rin report custom --template my-template.html --output custom-report.html"
    Then the command should execute successfully
    And the output should contain "Custom report generated successfully"
    And the file "custom-report.html" should exist
    And the file "custom-report.html" should contain "Total Items: 10"