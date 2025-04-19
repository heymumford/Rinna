Feature: Admin Diagnostics Commands
  As a system administrator
  I want to run system diagnostics and manage system warnings
  So that I can identify and resolve system issues

  Background:
    Given I am logged in as an administrator

  Scenario: Run basic diagnostics
    When I run "rin admin diagnostics run"
    Then the command should succeed
    And the output should contain "System Diagnostics Results"
    And the output should contain "API Server"
    And the output should contain "Database Connectivity"
    And the output should contain "Storage System"

  Scenario: Run full diagnostics
    When I run "rin admin diagnostics run --full"
    Then the command should succeed
    And the output should contain "System Diagnostics Results (Full)"
    And the output should contain "Thread Pool Analysis"
    And the output should contain "Detailed Memory Analysis"
    And the output should contain "Network Connectivity Tests"

  Scenario: List scheduled diagnostics
    When I run "rin admin diagnostics schedule list"
    Then the command should succeed
    And the output should contain "Scheduled Diagnostic Tasks"
    And the output should contain "Task ID"
    And the output should contain "Frequency"

  Scenario: Schedule recurring diagnostics
    When I run "rin admin diagnostics schedule" with input:
      """
      api,database,memory
      2
      03:30
      admin@example.com
      """
    Then the command should succeed
    And the output should contain "Diagnostic task scheduled successfully"
    And the output should contain "Task ID: DIAG-"

  Scenario: Schedule diagnostics with invalid input
    When I run "rin admin diagnostics schedule" with input:
      """

      """
    Then the command should fail
    And the output should contain "Error: Check types cannot be empty"

  Scenario: Analyze database performance
    When I run "rin admin diagnostics database --analyze"
    Then the command should succeed
    And the output should contain "Database Performance Analysis"
    And the output should contain "Query Performance"
    And the output should contain "Indexing Analysis"
    And the output should contain "Connection Pool Status"

  Scenario: Analyze database without required flag
    When I run "rin admin diagnostics database"
    Then the command should fail
    And the output should contain "Error: Missing required flag --analyze"

  Scenario: Resolve system warning
    Given a system warning with ID "WARN-12345" exists
    When I run "rin admin diagnostics warning resolve --id=WARN-12345" with input:
      """
      1
      """
    Then the command should succeed
    And the output should contain "Warning Resolution: WARN-12345"
    And the output should contain "Action performed successfully"

  Scenario: Attempt to resolve non-existent warning
    When I run "rin admin diagnostics warning resolve --id=NONEXISTENT"
    Then the command should fail
    And the output should contain "Error: Warning not found: NONEXISTENT"

  Scenario: Resolve warning with invalid action
    Given a system warning with ID "WARN-12345" exists
    When I run "rin admin diagnostics warning resolve --id=WARN-12345" with input:
      """
      99
      """
    Then the command should fail
    And the output should contain "Error: Invalid selection"

  Scenario: Resolve warning without ID
    When I run "rin admin diagnostics warning resolve"
    Then the command should fail
    And the output should contain "Error: Missing required parameter --id"

  Scenario: Use warning command without resolve action
    When I run "rin admin diagnostics warning"
    Then the command should fail
    And the output should contain "Error: Missing 'resolve' subcommand"

  Scenario: Perform memory reclamation
    When I run "rin admin diagnostics action --memory-reclaim"
    Then the command should succeed
    And the output should contain "Memory reclamation completed successfully"

  Scenario: Run action without specifying which action
    When I run "rin admin diagnostics action"
    Then the command should fail
    And the output should contain "Error: Missing required action flag"
    And the output should contain "Available actions: --memory-reclaim"

  Scenario: Show help for diagnostics commands
    When I run "rin admin diagnostics help"
    Then the command should succeed
    And the output should contain "Usage: rin admin diagnostics <operation> [options]"
    And the output should contain "Operations:"
    And the output should contain "run      - Run system diagnostics"

  Scenario: Run unknown diagnostics operation
    When I run "rin admin diagnostics unknown"
    Then the command should fail
    And the output should contain "Error: Unknown diagnostics operation: unknown"

  Scenario: Admin privileges check
    Given I am logged in as a regular user
    When I run "rin admin diagnostics run"
    Then the command should fail
    And the output should contain "Error: Administrative privileges required to run this command"