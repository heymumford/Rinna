Feature: Admin Backup Commands
  As a system administrator
  I want to manage system backups
  So that I can maintain data integrity and enable system recovery

  Background:
    Given I am logged in as an administrator

  Scenario: Configure backup settings
    When I run "rin admin backup configure" with input:
      """
      1
      2
      02:00
      30
      /backup/primary
      """
    Then the command should succeed
    And the output should contain "Backup configuration updated successfully"
    And the output should contain "Type: full"
    And the output should contain "Frequency: weekly"
    And the output should contain "Time: 02:00"
    And the output should contain "Retention: 30 days"

  Scenario: View backup configuration status
    When I run "rin admin backup status"
    Then the command should succeed
    And the output should contain "Backup Configuration Status"
    And the output should contain "Type:"
    And the output should contain "Frequency:"
    And the output should contain "Time:"
    And the output should contain "Retention:"
    And the output should contain "Location:"

  Scenario: Start a full backup
    When I run "rin admin backup start --type=full"
    Then the command should succeed
    And the output should contain "Backup started successfully"
    And the output should contain "Backup ID: BACKUP-"
    And the output should contain "Type: full"

  Scenario: Start an incremental backup
    When I run "rin admin backup start --type=incremental"
    Then the command should succeed
    And the output should contain "Backup started successfully"
    And the output should contain "Backup ID: BACKUP-"
    And the output should contain "Type: incremental"

  Scenario: Start a backup without specifying type
    When I run "rin admin backup start"
    Then the command should fail
    And the output should contain "Error: Missing required parameter --type"

  Scenario: List available backups
    When I run "rin admin backup list"
    Then the command should succeed
    And the output should contain "Available Backups"
    And the output should contain "ID"
    And the output should contain "Type"
    And the output should contain "Date"
    And the output should contain "Size"

  Scenario: Configure backup strategy
    When I run "rin admin backup strategy" with input:
      """
      2
      2
      1
      """
    Then the command should succeed
    And the output should contain "Backup strategy updated successfully"
    And the output should contain "Strategy: incremental"
    And the output should contain "Full Backup Frequency: weekly"
    And the output should contain "Incremental Backup Frequency: daily"

  Scenario: View backup strategy status
    When I run "rin admin backup strategy status"
    Then the command should succeed
    And the output should contain "Backup Strategy Status"
    And the output should contain "Strategy:"
    And the output should contain "Full Backup Frequency:"
    And the output should contain "Incremental Backup Frequency:"
    And the output should contain "Next Full Backup:"

  Scenario: View backup history
    When I run "rin admin backup history"
    Then the command should succeed
    And the output should contain "Backup History"
    And the output should contain "ID"
    And the output should contain "Type"
    And the output should contain "Date"
    And the output should contain "Status"
    And the output should contain "Size"

  Scenario: Configure backup security
    When I run "rin admin backup security" with input:
      """
      y
      2
      SecurePassword123
      SecurePassword123
      """
    Then the command should succeed
    And the output should contain "Backup security updated successfully"
    And the output should contain "Encryption: enabled"
    And the output should contain "Algorithm: AES-256"

  Scenario: Configure backup security with mismatched passwords
    When I run "rin admin backup security" with input:
      """
      y
      2
      SecurePassword123
      DifferentPassword
      """
    Then the command should fail
    And the output should contain "Error: Passphrases do not match"

  Scenario: View backup security status
    When I run "rin admin backup security status"
    Then the command should succeed
    And the output should contain "Backup Security Status"
    And the output should contain "Encryption:"
    And the output should contain "Algorithm:"

  Scenario: Verify backup integrity
    When I run "rin admin backup verify --id=latest"
    Then the command should succeed
    And the output should contain "Backup Verification Report"
    And the output should contain "Verification Status:"
    And the output should contain "Errors Found:"
    And the output should contain "Verification Time:"

  Scenario: Verify backup integrity without ID
    When I run "rin admin backup verify"
    Then the command should fail
    And the output should contain "Error: Missing required parameter --id"

  Scenario: Configure backup notifications
    When I run "rin admin backup notifications" with input:
      """
      y
      1,2,3
      admin@example.com,monitor@example.com
      """
    Then the command should succeed
    And the output should contain "Backup notifications updated successfully"
    And the output should contain "Notifications: enabled"
    And the output should contain "Events: success, failure, warning"
    And the output should contain "Recipients: admin@example.com, monitor@example.com"

  Scenario: View backup notifications status
    When I run "rin admin backup notifications status"
    Then the command should succeed
    And the output should contain "Backup Notifications Status"
    And the output should contain "Notifications:"
    And the output should contain "Events:"
    And the output should contain "Recipients:"

  Scenario: Configure backup locations
    When I run "rin admin backup locations" with input:
      """
      /backup/primary,/backup/secondary
      2
      """
    Then the command should succeed
    And the output should contain "Backup locations updated successfully"
    And the output should contain "Locations:"
    And the output should contain "/backup/primary"
    And the output should contain "/backup/secondary"
    And the output should contain "Mirroring Strategy: simultaneous"

  Scenario: List backup locations
    When I run "rin admin backup locations list"
    Then the command should succeed
    And the output should contain "Backup Storage Locations"
    And the output should contain "Location"
    And the output should contain "Status"
    And the output should contain "Free Space"

  Scenario: Show help for backup commands
    When I run "rin admin backup help"
    Then the command should succeed
    And the output should contain "Usage: rin admin backup <operation> [options]"
    And the output should contain "Operations:"
    And the output should contain "configure  - Configure backup settings"

  Scenario: Admin privileges check
    Given I am logged in as a regular user
    When I run "rin admin backup status"
    Then the command should fail
    And the output should contain "Error: Administrative privileges required to run this command"