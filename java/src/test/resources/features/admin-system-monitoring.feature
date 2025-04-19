@admin @monitoring @diagnostics
Feature: Admin System Monitoring and Diagnostics
  As an administrator
  I want to monitor system health and perform diagnostics
  So that I can ensure optimal performance and troubleshoot issues

  Background:
    Given I am logged in as an administrator
    And the system monitoring service is enabled

  @smoke
  Scenario: View system health dashboard
    When I run the command "rin admin monitor dashboard"
    Then the command should succeed
    And the output should contain the following system metrics:
      | Metric           | Status    |
      | API Server       | Available |
      | Database         | Available |
      | Storage          | Available |
      | Queue Processing | Active    |
      | Task Scheduler   | Running   |
    And the output should show CPU and memory usage statistics
    And the output should show active users count

  Scenario: Check detailed server metrics
    When I run the command "rin admin monitor server --detailed"
    Then the command should succeed
    And the output should contain the following server metrics:
      | Metric              | Value   | Threshold |
      | CPU Load            | *       | 85%       |
      | Memory Usage        | *       | 90%       |
      | Disk Usage          | *       | 85%       |
      | Network Connections | *       | 1000      |
      | Response Time       | *       | 500ms     |
      | Error Rate          | *       | 1%        |
    And the output should show thread pool utilization
    And the output should include uptime information

  Scenario: Configure monitoring thresholds
    When I run the command "rin admin monitor configure"
    Then I should be prompted to select which threshold to configure
    When I select "CPU Load"
    Then I should be prompted to enter a new threshold value
    When I enter "75"
    Then the command should succeed
    And the monitoring threshold for "CPU Load" should be set to "75%"
    When I run the command "rin admin monitor thresholds"
    Then the output should contain "CPU Load: 75%"

  Scenario: Generate system performance report
    When I run the command "rin admin monitor report --period=daily"
    Then the command should succeed
    And the output should contain "Generated daily performance report"
    And the report should include the following sections:
      | Section                  | Details                          |
      | Performance Summary      | Overall system performance       |
      | Resource Utilization     | CPU, memory, disk usage trends   |
      | Service Availability     | Uptime and response time metrics |
      | Error Analysis           | Error rates and common errors    |
      | Throughput Metrics       | Request processing statistics    |

  Scenario: Setup automatic alerts for system issues
    When I run the command "rin admin monitor alerts add"
    Then I should be prompted to enter an alert name
    When I enter "High CPU Usage Alert"
    Then I should be prompted to select a metric
    When I select "CPU Load"
    Then I should be prompted to enter a threshold value
    When I enter "80"
    Then I should be prompted to enter notification recipients
    When I enter "sysadmin@example.com"
    Then the command should succeed
    And the alert "High CPU Usage Alert" should be created
    When I run the command "rin admin monitor alerts list"
    Then the output should contain "High CPU Usage Alert"

  @critical
  Scenario: Run system diagnostics to detect issues
    When I run the command "rin admin diagnostics run --full"
    Then the command should succeed
    And the output should contain "Running full system diagnostics"
    And the diagnostics should check the following components:
      | Component         | Checks                                  |
      | API Server        | Connectivity, response time, errors     |
      | Database          | Connectivity, query performance, locks  |
      | File Storage      | Access, capacity, read/write speed      |
      | Memory Management | Leaks, fragmentation, allocation        |
      | Thread Pools      | Deadlocks, starvation, excessive wait   |
      | Network           | Latency, packet loss, DNS resolution    |
    And the diagnostics should identify any system bottlenecks

  Scenario: View active user sessions with resource usage
    When I run the command "rin admin monitor sessions"
    Then the command should succeed
    And the output should contain a list of active user sessions
    And each session should include the following information:
      | Field                 | Description                |
      | Session ID            | Unique identifier          |
      | User                  | Username                   |
      | Login Time            | Session start time         |
      | Client IP             | Source IP address          |
      | Resource Usage        | CPU, memory consumption    |
      | Current Activity      | Currently executing action |
    And the output should include total session count

  Scenario: Schedule recurring diagnostic checks
    When I run the command "rin admin diagnostics schedule"
    Then I should be prompted to select diagnostic check types
    When I select "connectivity,storage,memory"
    Then I should be prompted to select schedule frequency
    When I select "daily"
    Then I should be prompted to enter schedule time
    When I enter "02:00"
    Then I should be prompted to enter notification recipients
    When I enter "admin@example.com"
    Then the command should succeed
    And a scheduled diagnostic task should be created
    When I run the command "rin admin diagnostics schedule list"
    Then the output should contain a scheduled task for "connectivity,storage,memory" checks at "02:00"

  Scenario: Generate database performance report
    When I run the command "rin admin diagnostics database --analyze"
    Then the command should succeed
    And the output should contain "Database Performance Analysis"
    And the report should include the following metrics:
      | Metric                     | Details                               |
      | Query Performance          | Slow queries, execution times         |
      | Index Utilization          | Table scans, unused indexes           |
      | Connection Pool            | Utilization, wait times, timeout rate |
      | Transaction Volume         | Commits, rollbacks per minute         |
      | Lock Contention            | Lock wait times, deadlocks            |
      | Storage Utilization        | Table sizes, growth rates             |
    And the report should include optimization recommendations

  @critical
  Scenario: Handle critical system warning
    Given the system has generated a "HIGH_MEMORY_USAGE" warning
    When I run the command "rin admin diagnostics warning resolve --id=MEM-001"
    Then the command should succeed
    And I should see a detailed analysis of the memory issue
    When I select the "Analyze Heap Dump" option
    Then I should see a breakdown of memory consumption by component
    And the output should contain recommendations for resolving the issue
    When I run the command "rin admin diagnostics action --memory-reclaim"
    Then the command should succeed
    And the system resource warning should be resolved