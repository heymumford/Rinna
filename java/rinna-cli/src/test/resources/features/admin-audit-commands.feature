Feature: Admin Audit Commands
  As a system administrator
  I want to manage audit logs and security features
  So that I can ensure system security and compliance

  Background:
    Given I am logged in as an administrator

  Scenario: View audit logs
    When I run "rin admin audit list"
    Then the command should succeed
    And the output should contain "Audit Logs"
    And the output should contain "Date       | Time     | User     | Action           | Details"

  Scenario: View audit logs with filters
    When I run "rin admin audit list --user=admin --days=30 --limit=10"
    Then the command should succeed
    And the output should contain "Filtered by user: admin"
    And the output should contain "Showing logs from the last 30 days"
    And the output should contain "Limited to 10 entries"

  Scenario: Configure audit retention period
    When I run "rin admin audit configure --retention=90"
    Then the command should succeed
    And the output should contain "Audit log retention period updated to 90 days"
    And the output should contain "Configuration changes have been saved successfully"

  Scenario: View audit system status
    When I run "rin admin audit status"
    Then the command should succeed
    And the output should contain "Audit System Status"
    And the output should contain "Audit logging: Enabled"
    And the output should contain "Retention period: 90 days"

  Scenario: Export audit logs
    When I run "rin admin audit export --format=csv"
    Then the command should succeed
    And the output should contain "Exported audit logs to"
    And the output should contain ".csv"

  Scenario: Configure data masking
    When I run "rin admin audit mask configure" with input:
      """
      email,credit_card,ssn
      """
    Then the command should succeed
    And the output should contain "Data masking configuration updated successfully"

  Scenario: View data masking status
    When I run "rin admin audit mask status"
    Then the command should succeed
    And the output should contain "Data Masking Configuration"
    And the output should contain "Masking enabled: true"
    And the output should contain "Masked fields: email, credit_card, ssn"

  Scenario: Add audit alert
    When I run "rin admin audit alert add" with input:
      """
      LoginAlert
      FAILED_LOGIN
      5
      15
      admin@example.com
      """
    Then the command should succeed
    And the output should contain "Audit alert 'LoginAlert' created successfully"

  Scenario: List audit alerts
    When I run "rin admin audit alert list"
    Then the command should succeed
    And the output should contain "Audit Alerts"
    And the output should contain "LoginAlert"
    And the output should contain "FAILED_LOGIN"

  Scenario: Remove audit alert
    When I run "rin admin audit alert remove LoginAlert"
    Then the command should succeed
    And the output should contain "Audit alert 'LoginAlert' removed successfully"

  Scenario: Create security investigation
    When I run "rin admin audit investigation create --user=suspiciousUser --days=14"
    Then the command should succeed
    And the output should contain "Investigation case created successfully"
    And the output should contain "Case ID: INV-"

  Scenario: View investigation findings
    When a security investigation exists with case ID "INV-12345678" for user "suspiciousUser"
    And I run "rin admin audit investigation findings --case=INV-12345678"
    Then the command should succeed
    And the output should contain "Investigation Findings: INV-12345678"
    And the output should contain "Subject: suspiciousUser"
    And the output should contain "Activity Summary:"

  Scenario: Perform investigation action
    When I run "rin admin audit investigation actions --action=LOCK_ACCOUNT --user=suspiciousUser"
    Then the command should succeed
    And the output should contain "Investigation action 'LOCK_ACCOUNT' performed successfully on user 'suspiciousUser'"

  Scenario: Show help for audit commands
    When I run "rin admin audit help"
    Then the command should succeed
    And the output should contain "Usage: rin admin audit <operation> [options]"
    And the output should contain "Operations:"
    And the output should contain "list          - List audit logs"

  Scenario: Admin privileges check
    Given I am logged in as a regular user
    When I run "rin admin audit list"
    Then the command should fail
    And the output should contain "Error: Administrative privileges required to run this command"