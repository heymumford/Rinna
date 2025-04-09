@admin
Feature: Rinna Server Auto-Launch
  As an admin user
  I want the Rinna server to automatically start when needed
  So that I can seamlessly use CLI commands without manually managing the server

  Background:
    Given I have installed Rinna in my Java project
    And the Rinna server is not currently running
    And I am logged in as user "currentuser" on machine "testmachine"

  Scenario: Automatic server launch when running CLI commands
    When I run "bin/rin project list"
    Then the Rinna server should start automatically in the background
    And I should see a message "Starting Rinna server..."
    And I should see a message "Rinna server started successfully"
    And the command should execute and display the output
    And a server process should be running on the default port
    When I run "bin/rin project summary"
    Then the command should use the already running server
    And there should be only one server process running

  Scenario: Server launch with default admin credentials
    When I run "bin/rin project list"
    Then the Rinna server should start automatically in the background
    And the server should be initialized with default admin credentials
    And the admin user should be associated with my username "currentuser"
    And the admin user should be associated with my machine name "testmachine"
    When I run "bin/rin user show"
    Then I should be authenticated as the admin user
    And I should see my admin profile details

  Scenario: Server persistence between CLI commands
    When I run "bin/rin project create --name 'Test Project'"
    Then the Rinna server should start automatically in the background
    And a new project should be created in the database
    When I run "bin/rin type create --name EPIC --description 'Epic work item'"
    Then the command should use the already running server
    And a new work item type should be created in the database
    When I wait for 5 minutes without using any CLI commands
    And I run "bin/rin project summary"
    Then the command should use the already running server
    And I should see "Project: Test Project"
    And I should see "Work Item Types: EPIC"

  Scenario: Manual server shutdown and restart
    When I run "bin/rin project list"
    Then the Rinna server should start automatically in the background
    When I run "bin/rin server stop"
    Then I should see "Rinna server stopped successfully"
    And the server process should be terminated
    When I run "bin/rin project list"
    Then the Rinna server should start automatically in the background
    And I should see a message "Starting Rinna server..."
    And the command should execute and display the output

  Scenario: Server launch with custom configurations
    Given I have configured custom server settings in ".rinna-config"
    And I have set the server port to 9091
    And I have set the database location to "custom/data/rinna.db"
    When I run "bin/rin project list"
    Then the Rinna server should start automatically in the background
    And the server should use port 9091
    And the server should use the database at "custom/data/rinna.db"
    And the command should execute and display the output

  Scenario: Server auto-launch in a multi-user environment
    Given I have multiple user profiles configured in Rinna
    And I have a user profile "alice" for user "Alice Smith"
    And I have a user profile "bob" for user "Bob Jones"
    When I run "bin/rin user switch --profile alice"
    Then the Rinna server should start automatically in the background
    And I should be authenticated as user "Alice Smith"
    When I run "bin/rin server stop"
    And I run "bin/rin user switch --profile bob"
    Then the Rinna server should start automatically in the background
    And I should be authenticated as user "Bob Jones"

  Scenario: Server launch with external configuration
    Given I have configured an external Rinna server at "rinna.example.com:9090"
    When I run "bin/rin project list"
    Then the local Rinna server should not start
    And the CLI should connect to the external server at "rinna.example.com:9090"
    And I should be prompted for credentials
    When I enter valid credentials
    Then the command should execute and display the output

  Scenario: Server status monitoring
    When I run "bin/rin server status"
    Then I should see "Rinna server is not running"
    When I run "bin/rin project list"
    And I run "bin/rin server status"
    Then I should see "Rinna server is running"
    And I should see server details including version and uptime
    And I should see "Process ID:" followed by a valid PID
    And I should see "Port:" followed by the server port
    And I should see "Started:" followed by a timestamp