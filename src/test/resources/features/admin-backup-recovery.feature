@admin @backup @recovery
Feature: Admin Backup and Recovery
  As an administrator
  I want to configure and manage system backups and recovery procedures
  So that I can prevent data loss and ensure business continuity

  Background:
    Given I am logged in as an administrator
    And the backup service is configured

  @smoke
  Scenario: Configure automated backup schedule
    When I run the command "rin admin backup configure"
    Then I should be prompted to select the backup type
    When I select "full"
    Then I should be prompted to enter the backup frequency
    When I enter "daily"
    Then I should be prompted to enter the backup time
    When I enter "02:00"
    Then I should be prompted to enter the retention period in days
    When I enter "30"
    Then I should be prompted to enter the backup location
    When I enter "/backups/rinna"
    Then the command should succeed
    And the backup configuration should be updated
    When I run the command "rin admin backup status"
    Then the output should contain "Type: full"
    And the output should contain "Frequency: daily"
    And the output should contain "Time: 02:00"
    And the output should contain "Retention: 30 days"
    And the output should contain "Location: /backups/rinna"

  Scenario: Manually initiate a full backup
    When I run the command "rin admin backup start --type=full"
    Then the command should succeed
    And the output should show backup progress indicators
    And the output should confirm successful backup completion
    And the output should contain a backup ID for reference
    When I run the command "rin admin backup list"
    Then the output should show the recently created backup
    And the backup should have status "Completed"

  Scenario: Configure incremental backup strategy
    When I run the command "rin admin backup strategy configure"
    Then I should be prompted to select the backup strategy
    When I select "incremental"
    Then I should be prompted to select full backup frequency
    When I select "weekly"
    Then I should be prompted to select incremental backup frequency
    When I select "daily"
    Then the command should succeed
    And the backup strategy should be updated
    When I run the command "rin admin backup strategy status"
    Then the output should contain "Strategy: incremental"
    And the output should contain "Full backup: weekly"
    And the output should contain "Incremental backup: daily"

  Scenario: View backup history and status
    Given there are existing backups in the system
    When I run the command "rin admin backup history"
    Then the command should succeed
    And the output should list all backup operations
    And each backup entry should include:
      | Field          | Description               |
      | ID             | Unique backup identifier  |
      | Type           | Full or incremental       |
      | Date           | Backup date and time      |
      | Size           | Total backup size         |
      | Duration       | Time taken to complete    |
      | Status         | Success, failed, in progress |
      | Location       | Where backup is stored    |

  Scenario: Configure backup encryption
    When I run the command "rin admin backup security configure"
    Then I should be prompted to enable encryption
    When I enter "yes"
    Then I should be prompted to select encryption algorithm
    When I select "AES-256"
    Then I should be prompted to enter a secure passphrase
    When I enter "SecureBackupPassphrase123!"
    Then I should be prompted to confirm the passphrase
    When I enter "SecureBackupPassphrase123!"
    Then the command should succeed
    And the backup encryption should be enabled
    When I run the command "rin admin backup security status"
    Then the output should contain "Encryption: Enabled"
    And the output should contain "Algorithm: AES-256"

  @critical
  Scenario: Perform system recovery from backup
    Given a valid backup ID "BACKUP-20250405-143020"
    When I run the command "rin admin recovery start --backup-id=BACKUP-20250405-143020"
    Then I should be prompted to confirm the recovery operation
    When I enter "yes"
    Then the command should start the recovery process
    And the output should show recovery progress indicators
    When the recovery process completes
    Then the output should confirm successful recovery
    And the system state should match the backup state
    When I run the command "rin admin recovery status"
    Then the output should contain "Last recovery: BACKUP-20250405-143020"
    And the output should contain "Status: Successful"

  Scenario: Test backup integrity
    When I run the command "rin admin backup verify --backup-id=BACKUP-20250405-143020"
    Then the command should succeed
    And the output should show verification progress
    And the output should confirm backup integrity
    And the verification report should include:
      | Check                   | Status    |
      | Backup metadata         | Verified  |
      | File checksums          | Verified  |
      | Database dump integrity | Verified  |
      | Configuration files     | Verified  |
      | User data               | Verified  |

  Scenario: Configure backup notifications
    When I run the command "rin admin backup notifications configure"
    Then I should be prompted to enable backup notifications
    When I enter "yes"
    Then I should be prompted to select notification events
    When I select "success,failure,warning"
    Then I should be prompted to enter notification recipients
    When I enter "admin@example.com,backup@example.com"
    Then the command should succeed
    And the backup notifications should be configured
    When I run the command "rin admin backup notifications status"
    Then the output should contain "Notifications: Enabled"
    And the output should contain "Events: success, failure, warning"
    And the output should contain "Recipients: admin@example.com, backup@example.com"

  Scenario: Configure distributed backup across multiple locations
    When I run the command "rin admin backup locations configure"
    Then I should be prompted to add a backup location
    When I enter "/primary/backups"
    Then I should be prompted to add another location or finish
    When I enter "/secondary/backups"
    Then I should be prompted to add another location or finish
    When I enter "finish"
    Then I should be prompted to select mirroring strategy
    When I select "synchronized"
    Then the command should succeed
    And the backup locations should be configured
    When I run the command "rin admin backup locations list"
    Then the output should contain "/primary/backups"
    And the output should contain "/secondary/backups"
    And the output should contain "Strategy: synchronized"

  @critical
  Scenario: Create disaster recovery plan
    When I run the command "rin admin recovery plan generate"
    Then the command should succeed
    And the generated plan should include the following sections:
      | Section                  | Content                                  |
      | System Requirements      | Hardware and software dependencies       |
      | Backup Restore Procedure | Step-by-step recovery instructions       |
      | Verification Steps       | How to validate a successful recovery    |
      | Estimated Recovery Time  | Timeline for different recovery scenarios|
      | Contact Information      | Emergency contacts and responsibilities  |
    And the plan should be saved as a PDF document
    When I run the command "rin admin recovery plan test --simulation"
    Then the command should succeed
    And the output should contain a simulated recovery timeline
    And the output should identify potential recovery bottlenecks