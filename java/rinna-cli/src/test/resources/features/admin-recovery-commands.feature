Feature: Admin Recovery Commands
  As a system administrator
  I want to manage system recovery operations
  So that I can restore the system after failures or data loss

  Background:
    Given I am logged in as an administrator

  Scenario: View recovery status
    When I run "rin admin recovery status"
    Then the command should succeed
    And the output should contain "Recovery Status"
    And the output should contain "Last Recovery:"
    And the output should contain "Recovery Plan:"

  Scenario: Start recovery from backup with confirmation
    Given a backup with ID "BACKUP-12345678" exists
    When I run "rin admin recovery start --backup-id=BACKUP-12345678" with input:
      """
      yes
      """
    Then the command should succeed
    And the output should contain "Preparing to restore from backup: BACKUP-12345678"
    And the output should contain "Recovery completed successfully"

  Scenario: Cancel recovery from backup
    Given a backup with ID "BACKUP-12345678" exists
    When I run "rin admin recovery start --backup-id=BACKUP-12345678" with input:
      """
      no
      """
    Then the command should succeed
    And the output should contain "Recovery cancelled"

  Scenario: Start recovery without backup ID
    When I run "rin admin recovery start"
    Then the command should fail
    And the output should contain "Error: Missing required parameter --backup-id"

  Scenario: Start recovery with invalid backup ID
    When I run "rin admin recovery start --backup-id=INVALID-BACKUP" with input:
      """
      yes
      """
    Then the command should fail
    And the output should contain "Error during recovery"

  Scenario: Generate recovery plan
    When I run "rin admin recovery plan generate"
    Then the command should succeed
    And the output should contain "Recovery plan generated successfully"
    And the output should contain "Plan saved to:"

  Scenario: Test recovery plan with simulation
    When I run "rin admin recovery plan test --simulation"
    Then the command should succeed
    And the output should contain "Recovery Plan Test Results"
    And the output should contain "Mode: Simulation"
    And the output should contain "Overall Result:"

  Scenario: Test recovery plan in live mode
    When I run "rin admin recovery plan test"
    Then the command should succeed
    And the output should contain "Recovery Plan Test Results"
    And the output should contain "Mode: Live Test"
    And the output should contain "Overall Result:"

  Scenario: Run plan command without subcommand
    When I run "rin admin recovery plan"
    Then the command should fail
    And the output should contain "Error: Missing plan subcommand. Use 'generate' or 'test'"

  Scenario: Run invalid plan subcommand
    When I run "rin admin recovery plan invalid"
    Then the command should fail
    And the output should contain "Error: Unknown plan operation: invalid"
    And the output should contain "Valid operations: generate, test"

  Scenario: Show help for recovery commands
    When I run "rin admin recovery help"
    Then the command should succeed
    And the output should contain "Usage: rin admin recovery <operation> [options]"
    And the output should contain "Operations:"
    And the output should contain "start  - Initiate system recovery from backup"
    And the output should contain "status - Display recovery status"
    And the output should contain "plan   - Manage disaster recovery plans"

  Scenario: Run unknown recovery operation
    When I run "rin admin recovery unknown"
    Then the command should fail
    And the output should contain "Error: Unknown recovery operation: unknown"

  Scenario: Admin privileges check
    Given I am logged in as a regular user
    When I run "rin admin recovery status"
    Then the command should fail
    And the output should contain "Error: Administrative privileges required to run this command"