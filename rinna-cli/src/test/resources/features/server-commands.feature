Feature: Server Management Commands
  As a system administrator using the Rinna CLI
  I want to monitor and manage Rinna services
  So that I can ensure the system is running properly

  Scenario: Displaying status of all services
    When I run the command "rin server status"
    Then the command should execute successfully
    And the output should contain "Rinna Services Status"
    And the output should contain "SERVICE"
    And the output should contain "api"
    And the output should contain "database"
    And the output should contain "docs"

  Scenario: Displaying status of a specific service
    When I run the command "rin server status api"
    Then the command should execute successfully
    And the output should contain "Service: api"
    And the output should contain "Status: RUNNING"

  Scenario: Displaying server status in JSON format
    When I run the command "rin server status --json"
    Then the command should execute successfully
    And the output should contain "\"result\": \"success\""
    And the output should contain "\"services\": ["

  Scenario: Displaying server help
    When I run the command "rin server help"
    Then the command should execute successfully
    And the output should contain "Server Command Usage"
    And the output should contain "Subcommands:"
    And the output should contain "status"
    And the output should contain "start"
    And the output should contain "stop"
    And the output should contain "restart"
    And the output should contain "config"

  Scenario: Starting a service
    When I run the command "rin server start api"
    Then the command should execute successfully
    And the output should contain "Starting service: api"
    And the output should contain "Service started successfully"

  Scenario: Trying to start a service without specifying a service name
    When I run the command "rin server start"
    Then the command should execute successfully
    And the output should contain "Error: Service name is required to start a service"

  Scenario: Stopping a service
    When I run the command "rin server stop api"
    Then the command should execute successfully
    And the output should contain "Stopping service: api"
    And the output should contain "Service stopped successfully"

  Scenario: Restarting a service
    When I run the command "rin server restart api"
    Then the command should execute successfully
    And the output should contain "Restarting service: api"
    And the output should contain "Service restarted successfully"

  Scenario: Configuring a service
    When I run the command "rin server config api"
    Then the command should execute successfully
    And the output should contain "Created configuration for api"

  Scenario: Using an unknown subcommand
    When I run the command "rin server unknown"
    Then the command should fail with exit code 1
    And the output should contain "Unknown server subcommand: unknown"

  Scenario: Starting a service with verbose output
    When I run the command "rin server start api --verbose"
    Then the command should execute successfully
    And the output should contain "Service process ID: 12345"

  Scenario: Configuring a service with a specific config path
    When I run the command "rin server config api /tmp/api-config.json"
    Then the command should execute successfully
    And the output should contain "Created configuration for api"
    And the output should contain "/tmp/api-config.json"