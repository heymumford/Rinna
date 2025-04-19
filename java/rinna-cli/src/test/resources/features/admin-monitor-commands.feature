Feature: Admin Monitor Commands
  As a system administrator
  I want to monitor system health and performance
  So that I can ensure system stability and performance

  Background:
    Given I am logged in as an administrator

  Scenario: View system dashboard
    When I run "rin admin monitor dashboard"
    Then the command should succeed
    And the output should contain "System Health Dashboard"
    And the output should contain "CPU Usage:"
    And the output should contain "Memory Usage:"
    And the output should contain "Disk Usage:"

  Scenario: View server metrics
    When I run "rin admin monitor server"
    Then the command should succeed
    And the output should contain "Server Metrics"
    And the output should contain "System Load"
    And the output should contain "Memory Statistics"

  Scenario: View detailed server metrics
    When I run "rin admin monitor server --detailed"
    Then the command should succeed
    And the output should contain "Detailed Server Metrics"
    And the output should contain "Process Information"
    And the output should contain "Network Statistics"
    And the output should contain "Disk I/O Statistics"

  Scenario: Configure monitoring threshold
    When I run "rin admin monitor configure" with input:
      """
      1
      75
      """
    Then the command should succeed
    And the output should contain "Monitoring threshold for CPU Load updated to 75%"

  Scenario: Configure monitoring threshold with invalid selection
    When I run "rin admin monitor configure" with input:
      """
      9
      """
    Then the command should fail
    And the output should contain "Error: Invalid selection"

  Scenario: Generate hourly performance report
    When I run "rin admin monitor report --period=hourly"
    Then the command should succeed
    And the output should contain "System Performance Report (Hourly)"
    And the output should contain "Report Period: Last Hour"

  Scenario: Generate daily performance report
    When I run "rin admin monitor report --period=daily"
    Then the command should succeed
    And the output should contain "System Performance Report (Daily)"
    And the output should contain "Report Period: Last 24 Hours"

  Scenario: Generate report with invalid period
    When I run "rin admin monitor report --period=invalid"
    Then the command should fail
    And the output should contain "Error: Invalid period. Must be one of: hourly, daily, weekly, monthly"

  Scenario: Add monitoring alert
    When I run "rin admin monitor alerts add" with input:
      """
      HighCPUAlert
      1
      80
      admin@example.com
      """
    Then the command should succeed
    And the output should contain "Monitoring alert 'HighCPUAlert' created successfully"

  Scenario: Add monitoring alert without name
    When I run "rin admin monitor alerts add" with input:
      """

      """
    Then the command should fail
    And the output should contain "Error: Alert name cannot be empty"

  Scenario: List monitoring alerts
    Given a monitoring alert "HighCPUAlert" exists
    When I run "rin admin monitor alerts list"
    Then the command should succeed
    And the output should contain "Monitoring Alerts"
    And the output should contain "HighCPUAlert"
    And the output should contain "CPU Load"

  Scenario: Remove monitoring alert
    Given a monitoring alert "HighCPUAlert" exists
    When I run "rin admin monitor alerts remove HighCPUAlert"
    Then the command should succeed
    And the output should contain "Monitoring alert 'HighCPUAlert' removed successfully"

  Scenario: View active sessions
    When I run "rin admin monitor sessions"
    Then the command should succeed
    And the output should contain "Active User Sessions"
    And the output should contain "User"
    And the output should contain "Login Time"
    And the output should contain "IP Address"

  Scenario: View monitoring thresholds
    When I run "rin admin monitor thresholds"
    Then the command should succeed
    And the output should contain "System Monitoring Thresholds"
    And the output should contain "CPU Load"
    And the output should contain "Memory Usage"
    And the output should contain "Disk Usage"

  Scenario: Run unknown monitor operation
    When I run "rin admin monitor unknown"
    Then the command should fail
    And the output should contain "Error: Unknown monitoring operation: unknown"

  Scenario: Show help for monitor commands
    When I run "rin admin monitor help"
    Then the command should succeed
    And the output should contain "Usage: rin admin monitor <operation> [options]"
    And the output should contain "Operations:"
    And the output should contain "dashboard  - View system health dashboard"

  Scenario: Admin privileges check
    Given I am logged in as a regular user
    When I run "rin admin monitor dashboard"
    Then the command should fail
    And the output should contain "Error: Administrative privileges required to run this command"