@admin
Feature: Maven Integration and Admin Initialization
  As an admin user
  I want to add Rinna to my new empty Java project POM and run mvn clean package
  So that the Rinna CLI is activated with default admin credentials based on my system

  Background:
    Given I have an empty Java project with a standard POM file
    And I have Maven installed on my system
    And my current username is "currentuser"
    And my hostname is "testmachine"

  Scenario: Add Rinna to Java project POM and build
    When I add the Rinna core dependency to my project's POM file
    And I add the Rinna CLI dependency to my project's POM file
    And I add the Rinna Maven plugin to my project's POM file with admin initialization enabled
    And I run "mvn clean package" in my project directory
    Then the build should complete successfully
    And the Rinna CLI should be installed in the project's bin directory
    And a default admin user should be created with username "admin" and password "nimda"
    And the admin user should be associated with my local username "currentuser"
    And the admin user should be associated with my machine name "testmachine"
    And the Rinna server executable should be present in the project

  Scenario: Run the Rinna CLI after Maven build
    Given I have successfully built my project with Rinna
    When I run "bin/rin server status" from my project directory
    Then I should see "Rinna server is not running"
    When I run "bin/rin project summary"
    Then the Rinna server should start automatically
    And I should be prompted to authenticate
    When I enter "admin" as the username
    And I enter "nimda" as the password
    Then I should be authenticated successfully
    And I should see a message about no projects being configured yet

  Scenario: Create a project and configure work items after installation
    Given I have successfully built my project with Rinna
    And I am authenticated as the default admin user
    When I run "bin/rin project create --name 'My Test Project' --description 'Project for testing'"
    Then I should see "Successfully created project: My Test Project"
    And a new project should be created in the Rinna database
    When I run "bin/rin type create --name EPIC --description 'Large feature that spans multiple releases'"
    Then I should see "Successfully created work item type: EPIC"
    When I run "bin/rin type create --name STORY --description 'User story' --field storyPoints:number --field acceptanceCriteria:text"
    Then I should see "Successfully created work item type: STORY"
    When I run "bin/rin project summary"
    Then I should see "Project: My Test Project"
    And I should see "Work Item Types: EPIC, STORY"
    And I should see the field definitions for "STORY" include "storyPoints" and "acceptanceCriteria"

  Scenario: Add users to the project
    Given I have successfully built my project with Rinna
    And I am authenticated as the default admin user
    And I have created a project named "My Test Project"
    When I run "bin/rin user create --name 'John Doe' --email 'john@example.com'"
    Then I should see "Successfully created user: John Doe"
    And a new user should exist in the Rinna database with email "john@example.com"
    When I run "bin/rin user create --name 'Jane Smith' --email 'jane@example.com' --role admin"
    Then I should see "Successfully created user: Jane Smith"
    And a new user should exist in the Rinna database with email "jane@example.com" and role "admin"
    When I run "bin/rin user list"
    Then I should see "admin" in the user list
    And I should see "John Doe" in the user list
    And I should see "Jane Smith" in the user list with role "admin"

  Scenario: Rename a project
    Given I have successfully built my project with Rinna
    And I am authenticated as the default admin user
    And I have created a project named "My Test Project"
    When I run "bin/rin project rename --current --name 'Renamed Project'"
    Then I should see "Successfully renamed project to: Renamed Project"
    When I run "bin/rin project summary"
    Then I should see "Project: Renamed Project"
    And I should not see "My Test Project"

  Scenario: Define workflow states and transitions
    Given I have successfully built my project with Rinna
    And I am authenticated as the default admin user
    And I have created a project named "My Test Project"
    When I run "bin/rin workflow add-state --name PLANNING --description 'Planning phase'"
    Then I should see "Successfully added workflow state: PLANNING"
    When I run "bin/rin workflow add-state --name DEVELOPMENT --description 'Development phase'"
    Then I should see "Successfully added workflow state: DEVELOPMENT"
    When I run "bin/rin workflow add-state --name REVIEW --description 'Review phase'"
    Then I should see "Successfully added workflow state: REVIEW"
    When I run "bin/rin workflow add-state --name DONE --description 'Completed work'"
    Then I should see "Successfully added workflow state: DONE"
    When I run "bin/rin workflow set-start-state --name PLANNING"
    Then I should see "Successfully set PLANNING as a start state"
    When I run "bin/rin workflow set-end-state --name DONE"
    Then I should see "Successfully set DONE as an end state"
    When I run "bin/rin workflow add-transition --from PLANNING --to DEVELOPMENT"
    Then I should see "Successfully added transition: PLANNING → DEVELOPMENT"
    When I run "bin/rin workflow add-transition --from DEVELOPMENT --to REVIEW"
    Then I should see "Successfully added transition: DEVELOPMENT → REVIEW"
    When I run "bin/rin workflow add-transition --from REVIEW --to DONE"
    Then I should see "Successfully added transition: REVIEW → DONE"
    When I run "bin/rin workflow add-transition --from REVIEW --to DEVELOPMENT"
    Then I should see "Successfully added transition: REVIEW → DEVELOPMENT"
    When I run "bin/rin workflow transitions"
    Then I should see all 4 configured transitions in the workflow
    And I should see a visual representation of the workflow

  Scenario: View full project configuration summary
    Given I have successfully built my project with Rinna
    And I am authenticated as the default admin user
    And I have created a project named "My Test Project"
    And I have added work item types "EPIC" and "STORY" to the project
    And I have configured workflow states and transitions for the project
    When I run "bin/rin project summary --detailed"
    Then I should see "Project: My Test Project"
    And I should see "Work Item Types:" section
    And I should see details of the "EPIC" work item type
    And I should see details of the "STORY" work item type with its custom fields
    And I should see "Workflow:" section
    And I should see all workflow states including "PLANNING", "DEVELOPMENT", "REVIEW", and "DONE"
    And I should see all workflow transitions between the states
    And I should see the start state is "PLANNING"
    And I should see the end state is "DONE"