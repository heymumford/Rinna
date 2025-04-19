Feature: Authentication Commands
  As a user of the Rinna CLI
  I want to authenticate with the system
  So that I can access protected functionality

  Scenario: Logging in with valid credentials
    Given the user is not authenticated
    When I run the command "rin login admin admin123"
    Then the command should execute successfully
    And the output should contain "Successfully logged in as: admin"
    And the output should contain "Role: Administrator"

  Scenario: Logging in with invalid credentials
    Given the user is not authenticated
    When I run the command "rin login admin wrongpassword"
    Then the command should fail with exit code 1
    And the output should contain "Login failed: Invalid username or password"

  Scenario: Logging in as a regular user
    Given the user is not authenticated
    When I run the command "rin login user user123"
    Then the command should execute successfully
    And the output should contain "Successfully logged in as: user"
    And the output should contain "Role: User"

  Scenario: Checking login status when already authenticated
    Given the user is authenticated as "admin"
    When I run the command "rin login"
    Then the command should execute successfully
    And the output should contain "You are already logged in as: admin"
    And the output should contain "Role: Administrator"

  Scenario: Switching users when already authenticated
    Given the user is authenticated as "admin"
    When I run the command "rin login user user123"
    Then the command should execute successfully
    And the output should contain "Logging out current user: admin"
    And the output should contain "Successfully logged in as: user"

  Scenario: Logging out when authenticated
    Given the user is authenticated as "admin"
    When I run the command "rin logout"
    Then the command should execute successfully
    And the output should contain "Successfully logged out user: admin"

  Scenario: Attempting to log out when not authenticated
    Given the user is not authenticated
    When I run the command "rin logout"
    Then the command should execute successfully
    And the output should contain "You are not currently logged in"