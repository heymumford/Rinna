Feature: New User Authentication and Authorization
  As a new user of Rinna
  I want to be automatically authenticated and authorized based on my system credentials
  So that I can start working with Rinna without manual configuration

  Scenario: First-time user initialization
    Given a new user is running Rinna for the first time
    When the user runs "rin init"
    Then the system should detect the user's credentials
    And create a local configuration with the user's identity
    And the user should see a welcome message
    And the user should see their identity details

  Scenario: New user listing empty work items
    Given a new user has initialized Rinna
    When the user runs "rin list"
    Then the user should see a message indicating there are no work items
    And the user should see the default workflow stages
    And the user should see they are authorized for all CRUD operations

  Scenario: New user authorization verification
    Given a new user has initialized Rinna
    When the user runs "rin auth status"
    Then the user should see their authentication details
    And the user should see they have full authorization for all work item types
    And the user should see they are registered on their local machine

  Scenario: New user workspace initialization
    Given a new user has initialized Rinna
    When the user runs "rin workspace status"
    Then the user should see that a workspace has been created
    And the workspace should be linked to the current project
    And the user should see they are the owner of the workspace