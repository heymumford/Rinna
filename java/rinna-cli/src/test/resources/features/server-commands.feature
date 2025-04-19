Feature: Server Command
  As a Rinna user
  I want to manage Rinna services
  So that I can control their lifecycle and configuration

  Background:
    Given a valid user session

  Scenario: List all service statuses
    When I execute the command "server"
    Then I should see a list of available services
    And I should see status information for each service
    And the command should track this operation with MetadataService

  Scenario: Get status of a specific service
    When I execute the command "server status api"
    Then I should see detailed status for the "api" service
    And the command should track this operation with MetadataService

  Scenario: Show help information
    When I execute the command "server help"
    Then I should see help information for the server command
    And I should see a list of available subcommands
    And the command should track this operation with MetadataService

  Scenario: Start a service successfully
    When I execute the command "server start api"
    Then the service "api" should be started
    And I should see a confirmation message
    And the command should track this operation with MetadataService

  Scenario: Stop a running service
    Given the service "api" is running
    When I execute the command "server stop api"
    Then the service "api" should be stopped
    And I should see a confirmation message
    And the command should track this operation with MetadataService

  Scenario: Restart a service
    Given the service "api" is running
    When I execute the command "server restart api"
    Then the service "api" should be restarted
    And I should see a confirmation message
    And the command should track this operation with MetadataService

  Scenario: Configure a service
    When I execute the command "server config api /tmp/api.json"
    Then a configuration file should be created at "/tmp/api.json"
    And I should see a confirmation message
    And the command should track this operation with MetadataService

  Scenario: Output in JSON format
    When I execute the command "server status api --json"
    Then I should see the output in JSON format
    And the JSON should contain service status information
    And the command should track this operation with MetadataService

  Scenario: Handle unknown service
    When I execute the command "server status unknown"
    Then I should see an error message about unknown service
    And the command should track this operation failure with MetadataService

  Scenario: Handle invalid subcommand
    When I execute the command "server invalid"
    Then I should see an error message about invalid subcommand
    And the command should track this operation failure with MetadataService
