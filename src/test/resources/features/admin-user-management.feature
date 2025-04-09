@admin
Feature: Admin User Management and Operations
  As an admin user of Rinna
  I want to manage user profiles and permissions
  So that I can control access to system features and maintain security

  Background:
    Given the Rinna server is running
    And I am authenticated as the default admin user

  # ===== User Profile Management =====
  
  Scenario: View own admin user profile details
    When I run "rin user show"
    Then I should see my user profile with admin privileges
    And I should see all my custom metadata fields
    And I should see the list of available roles includes "admin" and "user"

  Scenario: Create a new user profile
    When I run "rin user create --name 'John Doe' --email 'john@example.com'"
    Then the system should create a new user profile
    And the new user should have the default "user" role
    And I should see a success message with the new user ID

  Scenario: Create a new user profile with admin privileges
    When I run "rin user create --name 'Jane Smith' --email 'jane@example.com' --role admin"
    Then the system should create a new user profile with admin privileges
    And I should see a success message with the new user ID
    And the audit log should show that I created a new admin user

  Scenario: Create a user profile with custom metadata
    When I run "rin user create --name 'Bob Jenkins' --email 'bob@example.com' --meta department=Engineering --meta location=Remote"
    Then the system should create a new user profile
    And the user profile should contain the custom metadata fields
    And I should be able to search for users by custom metadata

  Scenario: Create multiple user profiles for different projects
    Given I already have a user profile
    When I run "rin user create --name 'Project Alpha Profile' --project 'ALPHA'"
    And I run "rin user create --name 'Project Beta Profile' --project 'BETA'"
    Then I should have 3 user profiles
    And I should be able to switch between profiles with "rin user switch <profile-id>"
    And each profile should be associated with the specified project

  Scenario: Attempt to create a user profile with invalid data
    When I run "rin user create --name '' --email 'invalid'"
    Then I should see an error message about invalid user data
    And no new user profile should be created

  # ===== User Role and Permission Management =====

  Scenario: Grant admin privileges to existing user
    Given a regular user with ID "user123" exists in the system
    When I run "rin user role --id user123 --grant admin"
    Then user "user123" should have admin privileges
    And the audit log should record this permission change
    And the user should receive a notification about their new privileges

  Scenario: Revoke admin privileges from a user
    Given a user with ID "admin456" exists with admin privileges
    When I run "rin user role --id admin456 --revoke admin"
    Then user "admin456" should no longer have admin privileges
    And the audit log should record this permission change
    And the user should receive a notification about the change

  Scenario: Attempt to revoke admin privileges from the last admin user
    Given I am the only user with admin privileges
    When I run "rin user role --id self --revoke admin"
    Then I should see an error message about needing at least one admin
    And I should retain admin privileges
    And the audit log should record this failed attempt

  Scenario: Grant and revoke multiple roles
    Given a regular user with ID "user789" exists in the system
    When I run "rin user role --id user789 --grant project-manager,reporter"
    Then user "user789" should have the roles "user,project-manager,reporter"
    When I run "rin user role --id user789 --revoke reporter"
    Then user "user789" should have the roles "user,project-manager"
    And should not have the "reporter" role

  # ===== User Profile Modification =====

  Scenario: Update user profile information
    Given a user with ID "user123" exists in the system
    When I run "rin user update --id user123 --name 'New Name' --email 'new@example.com'"
    Then the user profile should be updated with the new information
    And I should see a success message

  Scenario: Add custom metadata to user profile
    Given a user with ID "user123" exists in the system
    When I run "rin user meta --id user123 --add skillLevel=Senior --add languages=Java,Go,Python"
    Then the user profile should contain the new metadata fields
    And I should be able to query users based on these metadata fields

  Scenario: Remove custom metadata from user profile
    Given a user with ID "user123" exists with metadata "department=Sales"
    When I run "rin user meta --id user123 --remove department"
    Then the metadata field should be removed from the user profile
    And I should see a success message

  Scenario: List all users in the system
    Given the system has multiple user profiles
    When I run "rin user list"
    Then I should see a list of all user profiles
    And the list should include usernames, roles, and creation dates
    And I should see a count of the total number of users

  Scenario: Search for users by criteria
    Given the system has multiple user profiles with various metadata
    When I run "rin user search --meta department=Engineering"
    Then I should see only users with that metadata value
    When I run "rin user search --role admin"
    Then I should see only users with admin privileges

  Scenario: Export user data
    Given the system has multiple user profiles
    When I run "rin user export --format json"
    Then I should receive a JSON file with all user data
    And the sensitive data should be properly protected
    And the export should include all custom metadata

  # ===== User Deactivation and Deletion =====

  Scenario: Deactivate a user account
    Given a user with ID "user123" exists in the system
    When I run "rin user deactivate --id user123"
    Then the user account should be marked as inactive
    And the user should not be able to log in
    But their work items and history should be preserved

  Scenario: Reactivate a user account
    Given a user with ID "user123" exists in the system but is inactive
    When I run "rin user activate --id user123"
    Then the user account should be marked as active
    And the user should be able to log in again

  Scenario: Delete a user account permanently
    Given a user with ID "user123" exists in the system
    When I run "rin user delete --id user123 --confirm"
    Then the user account should be permanently deleted
    And I should see a warning about associated data
    And the audit log should record this deletion

  Scenario: Attempt to delete own admin account
    When I run "rin user delete --id self --confirm"
    Then I should see an error message about not being able to delete own account
    And my account should remain active
    And the audit log should record this failed attempt

  # ===== Profile Switching and Management =====

  Scenario: Switch between user profiles
    Given I have multiple user profiles
    When I run "rin user list-profiles"
    Then I should see all my available profiles
    When I run "rin user switch --profile project-alpha"
    Then my active profile should be changed to "project-alpha"
    And my available permissions should reflect the new profile

  Scenario: Create a new profile for existing user
    Given I am authenticated as an existing user
    When I run "rin profile create --name 'Secondary Profile' --project 'PROJECT-X'"
    Then a new profile should be created for my user account
    And I should be able to switch to this profile
    And the profile should be associated with "PROJECT-X"