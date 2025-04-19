@report @cli
Feature: Report Generation and Management
  As a project manager
  I want to generate various reports about work items
  So that I can track project progress and share insights with stakeholders

  Background:
    Given the system contains the following work items:
      | ID    | Title               | Type    | State       | Priority | Assignee       |
      | WI-01 | Setup CI/CD         | TASK    | DONE        | HIGH     | johndoe        |
      | WI-02 | Design database     | TASK    | IN_PROGRESS | MEDIUM   | janedoe        |
      | WI-03 | Fix login bug       | BUG     | TODO        | HIGH     | alexsmith      |
      | WI-04 | Update docs         | TASK    | IN_PROGRESS | LOW      | johndoe        |
      | WI-05 | Performance issue   | BUG     | TODO        | MEDIUM   | null           |
      | WI-06 | Security review     | TASK    | DONE        | HIGH     | janedoe        |

  @smoke
  Scenario: Generate basic summary report
    When I run the command "rin report summary"
    Then the command should succeed
    And the output should contain "Work Item Summary Report"
    And the output should contain "Total Items: 6"
    And the output should contain "Completed: 2"
    And the output should contain "In Progress: 2"
    And the output should contain "Not Started: 2"
    And the output should contain a breakdown by priority
    And the output should contain a breakdown by type

  Scenario: Generate detailed report with filtering
    When I run the command "rin report detailed --filter=type=BUG"
    Then the command should succeed
    And the output should contain "Detailed Work Item Report"
    And the output should contain information about "WI-03"
    And the output should contain information about "WI-05"
    And the output should not contain information about "WI-01"
    And the output should not contain information about "WI-02"

  Scenario: Generate status report with grouping
    When I run the command "rin report status --group=state"
    Then the command should succeed
    And the output should contain "Work Item Status Report"
    And the output should contain sections for each state
    And the "DONE" section should contain "WI-01" and "WI-06"
    And the "IN_PROGRESS" section should contain "WI-02" and "WI-04"
    And the "TODO" section should contain "WI-03" and "WI-05"

  Scenario: Generate report in different formats
    When I run the command "rin report summary --format=html --output=report.html"
    Then the command should succeed
    And the file "report.html" should be created
    And the file "report.html" should contain HTML report content
    When I run the command "rin report summary --format=csv --output=report.csv"
    Then the command should succeed
    And the file "report.csv" should be created
    And the file "report.csv" should contain CSV report content
    When I run the command "rin report summary --format=json --output=report.json"
    Then the command should succeed
    And the file "report.json" should be created
    And the file "report.json" should contain JSON report content

  Scenario: Generate report with custom title and date range
    When I run the command "rin report summary --title='Q1 Progress Report' --start=2025-01-01 --end=2025-03-31"
    Then the command should succeed
    And the output should contain "Q1 Progress Report"
    And the output should indicate filtered by date range from "2025-01-01" to "2025-03-31"

  Scenario: Generate assignee report
    When I run the command "rin report assignee"
    Then the command should succeed
    And the output should contain "Work Item Assignee Report"
    And the output should contain a section for "johndoe" with 2 items
    And the output should contain a section for "janedoe" with 2 items
    And the output should contain a section for "alexsmith" with 1 item
    And the output should contain a section for "Unassigned" with 1 item

  Scenario: Generate report using templates
    Given template "custom_summary.html" exists
    When I run the command "rin report summary --format=html --template=custom_summary"
    Then the command should succeed
    And the output should contain content formatted according to the template
    And template variables should be correctly substituted

  @email
  Scenario: Generate and email a report
    When I run the command "rin report summary --email --email-to=team@example.com --email-subject='Weekly Status'"
    Then the command should succeed
    And the output should indicate that the report was sent via email
    And an email should be sent to "team@example.com"
    And the email should have subject "Weekly Status"
    And the email should contain the report content

  @schedule
  Scenario: Schedule a recurring report
    When I run the command "rin schedule add --name='Weekly Status' --type=weekly --day=monday --time=09:00 --report=summary --format=html --email --email-to=team@example.com"
    Then the command should succeed
    And the output should indicate that the report has been scheduled
    When I run the command "rin schedule list"
    Then the output should contain "Weekly Status"
    And the scheduled report details should include:
      | Field      | Value                |
      | Name       | Weekly Status        |
      | Type       | weekly               |
      | Day        | monday               |
      | Time       | 09:00                |
      | Report     | summary              |
      | Format     | html                 |
      | Recipients | team@example.com     |

  Scenario: Remove a scheduled report
    Given a scheduled report "Weekly Status" exists
    When I run the command "rin schedule list"
    Then the output should contain "Weekly Status"
    When I run the command "rin schedule remove --id={report-id}"
    Then the command should succeed
    And the output should indicate that the report has been removed
    When I run the command "rin schedule list"
    Then the output should not contain "Weekly Status"

  Scenario: Start and stop the report scheduler
    When I run the command "rin schedule start"
    Then the command should succeed
    And the output should indicate that the scheduler has been started
    When I run the command "rin schedule stop"
    Then the command should succeed
    And the output should indicate that the scheduler has been stopped