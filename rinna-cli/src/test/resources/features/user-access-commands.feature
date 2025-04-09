Feature: User Access Management Commands
  As an administrator of the Rinna CLI
  I want to manage user permissions and access rights
  So that I can control who has access to different system functions

  Background:
    Given the current user is authenticated as an administrator

  Scenario: Viewing user access management help
    When I run the command "rin access help"
    Then the command should execute successfully
    And the output should contain "Usage: rin access <action> [options]"
    And the output should contain "grant-permission"
    And the output should contain "revoke-permission"
    And the output should contain "grant-admin"
    And the output should contain "revoke-admin"
    And the output should contain "promote"

  Scenario: Granting permission to a user
    When I run the command "rin access grant-permission --user=testuser --permission=view"
    Then the command should execute successfully
    And the output should contain "Successfully granted permission 'view' to user 'testuser'"

  Scenario: Revoking permission from a user
    When I run the command "rin access revoke-permission --user=testuser --permission=view"
    Then the command should execute successfully
    And the output should contain "Successfully revoked permission 'view' from user 'testuser'"

  Scenario: Granting admin access for specific area
    When I run the command "rin access grant-admin --user=testuser --area=projects"
    Then the command should execute successfully
    And the output should contain "Successfully granted admin access for area 'projects' to user 'testuser'"

  Scenario: Revoking admin access for specific area
    When I run the command "rin access revoke-admin --user=testuser --area=projects"
    Then the command should execute successfully
    And the output should contain "Successfully revoked admin access for area 'projects' from user 'testuser'"

  Scenario: Promoting user to admin role
    When I run the command "rin access promote --user=testuser"
    Then the command should execute successfully
    And the output should contain "Successfully promoted user 'testuser' to administrator role"

  Scenario: Attempting to run user access commands without admin privileges
    Given the current user is authenticated but not an administrator
    When I run the command "rin access grant-permission --user=testuser --permission=view"
    Then the command should fail with exit code 1
    And the output should contain "Error: Administrative privileges required for user access management"

  Scenario: Attempting to run user access commands without authentication
    Given the current user is not authenticated
    When I run the command "rin access grant-permission --user=testuser --permission=view"
    Then the command should fail with exit code 1
    And the output should contain "Error: Authentication required"

  Scenario: Attempting to grant permission without specifying a user
    When I run the command "rin access grant-permission --permission=view"
    Then the command should fail with exit code 1
    And the output should contain "Error: Username required"

  Scenario: Attempting to grant permission without specifying a permission
    When I run the command "rin access grant-permission --user=testuser"
    Then the command should fail with exit code 1
    And the output should contain "Error: Permission required"

  Scenario: Attempting to grant admin access without specifying an area
    When I run the command "rin access grant-admin --user=testuser"
    Then the command should fail with exit code 1
    And the output should contain "Error: Administrative area required"