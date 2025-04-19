Feature: Administrative Operations
  As a system administrator
  I want to perform administrative tasks through the CLI
  So that I can manage the system efficiently and securely

  Background:
    Given I am logged in as a user with admin privileges
    And the system has audit logging enabled

  Scenario: View admin help information
    When I execute the admin command without arguments
    Then I should see the admin help information
    And the help should list all admin subcommands
      | audit       |
      | compliance  |
      | monitor     |
      | diagnostics |
      | backup      |
      | recovery    |

  Scenario: View audit logs through the CLI
    When I execute the command "admin audit list"
    Then I should see a list of recent audit log entries
    And the entries should include timestamp, user, and action information

  Scenario: Generate a compliance report
    When I execute the command "admin compliance report financial"
    Then a compliance report should be generated
    And I should see confirmation that the report was created

  Scenario: Configure system backup settings
    When I execute the command "admin backup configure --location /backup/path --retention 30"
    Then the backup configuration should be updated
    And I should see confirmation that the settings were saved

  Scenario: Start a system backup
    When I execute the command "admin backup start --type full"
    Then a system backup should be initiated
    And I should see progress information for the backup operation

  Scenario: Check system health
    When I execute the command "admin monitor dashboard"
    Then I should see the system health dashboard
    And it should display CPU, memory, and disk usage metrics

  Scenario: Execute system diagnostics
    When I execute the command "admin diagnostics run"
    Then a full system diagnostic should be performed
    And I should see the results of the diagnostic tests

  Scenario: Scheduled diagnostics configuration
    When I execute the command "admin diagnostics schedule --interval daily --time 02:00"
    Then the diagnostic schedule should be updated
    And I should see confirmation that the schedule was set

  Scenario: Unauthorized user attempts admin operations
    Given I am logged in as a regular user without admin privileges
    When I attempt to execute the command "admin audit list"
    Then I should see an authorization error message
    And no admin operations should be performed