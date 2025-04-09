@admin
Feature: Project Management with Rinna CLI
  As an admin user
  I want to use the Rinna CLI to create and manage projects
  So that I can organize work items and workflows effectively

  Background:
    Given I have installed Rinna in my Java project
    And the Rinna server is running
    And I am authenticated as the admin user

  Scenario: Create a new project
    When I run "bin/rin project create --name 'Development Portal' --description 'Main development project'"
    Then I should see "Successfully created project: Development Portal"
    And a new project should be created in the Rinna database
    And the project should have the description "Main development project"
    When I run "bin/rin project list"
    Then I should see "Development Portal" in the project list
    And I should see the project description and creation date

  Scenario: Create a project with template
    When I run "bin/rin project create --name 'Agile Project' --template agile"
    Then I should see "Successfully created project: Agile Project"
    And a new project should be created in the Rinna database
    And the project should have standard Agile workflow states
    And the project should have standard Agile work item types
    When I run "bin/rin project summary --name 'Agile Project'"
    Then I should see Agile-specific states like "BACKLOG", "SPRINT", "IN_PROGRESS"
    And I should see Agile-specific work item types like "EPIC", "STORY", "TASK"

  Scenario: Set current project
    Given I have created projects "Project A" and "Project B"
    When I run "bin/rin project list"
    Then I should see both "Project A" and "Project B" in the list
    When I run "bin/rin project set-current --name 'Project A'"
    Then I should see "Successfully set 'Project A' as the current project"
    And "Project A" should be marked as the current project
    When I run "bin/rin project current"
    Then I should see "Current project: Project A"

  Scenario: Rename a project
    Given I have created a project named "Old Project Name"
    And "Old Project Name" is set as the current project
    When I run "bin/rin project rename --current --name 'New Project Name'"
    Then I should see "Successfully renamed project to: New Project Name"
    When I run "bin/rin project current"
    Then I should see "Current project: New Project Name"
    And I should not see "Old Project Name"
    When I run "bin/rin project list"
    Then I should see "New Project Name" in the project list
    And I should not see "Old Project Name" in the project list

  Scenario: Add metadata to a project
    Given I have created a project named "Metadata Test"
    When I run "bin/rin project meta --name 'Metadata Test' --add department=Engineering --add priority=High"
    Then I should see "Successfully added metadata to project"
    When I run "bin/rin project show --name 'Metadata Test'"
    Then I should see "department: Engineering" in the metadata section
    And I should see "priority: High" in the metadata section

  Scenario: Configure project permissions
    Given I have created a project named "Team Project"
    And I have created users "alice", "bob", and "charlie"
    When I run "bin/rin project perm --name 'Team Project' --grant alice=read,write --grant bob=read,write,admin --grant charlie=read"
    Then I should see "Successfully updated project permissions"
    When I run "bin/rin project show-perms --name 'Team Project'"
    Then I should see that "alice" has "read,write" permissions
    And I should see that "bob" has "read,write,admin" permissions
    And I should see that "charlie" has "read" permissions
    When I run "bin/rin project perm --name 'Team Project' --revoke charlie=read"
    Then I should see "Successfully updated project permissions"
    When I run "bin/rin project show-perms --name 'Team Project'"
    Then I should not see "charlie" in the permissions list

  Scenario: Project summary shows work item types and workflow
    Given I have created a project named "Summary Test"
    And I have added work item types "EPIC", "STORY", and "TASK" to the project
    And I have configured workflow states "PLANNING", "DEVELOPMENT", "TESTING", "DONE"
    And I have configured transitions between the workflow states
    When I run "bin/rin project summary --name 'Summary Test'"
    Then I should see "Project: Summary Test"
    And I should see a "Work Item Types" section listing "EPIC", "STORY", and "TASK"
    And I should see a "Workflow" section listing the defined states
    And I should see the transitions between workflow states
    And I should see which states are start and end states

  Scenario: Export project configuration
    Given I have created a project named "Export Test"
    And I have fully configured work item types and workflow for the project
    When I run "bin/rin project export --name 'Export Test' --file project-config.json"
    Then I should see "Successfully exported project configuration"
    And a file "project-config.json" should be created
    And the file should contain all project settings
    And the file should contain all work item type definitions
    And the file should contain all workflow state and transition definitions

  Scenario: Import project configuration
    Given I have a project configuration file "project-config.json"
    When I run "bin/rin project import --name 'Imported Project' --file project-config.json"
    Then I should see "Successfully imported project configuration"
    And a new project "Imported Project" should be created
    And the project should have all work item types from the configuration
    And the project should have all workflow states from the configuration
    And the project should have all transitions from the configuration
    When I run "bin/rin project summary --name 'Imported Project'"
    Then I should see a complete summary matching the imported configuration

  Scenario: Delete a project
    Given I have created a project named "Temporary Project"
    When I run "bin/rin project delete --name 'Temporary Project' --confirm"
    Then I should see "Successfully deleted project: Temporary Project"
    And the project should be removed from the database
    When I run "bin/rin project list"
    Then I should not see "Temporary Project" in the project list

  Scenario: Project list with filtering and sorting
    Given I have created multiple projects with different attributes
    When I run "bin/rin project list --filter department=Engineering"
    Then I should see only projects with department "Engineering"
    When I run "bin/rin project list --sort created_date:desc"
    Then I should see projects listed in descending order of creation date
    When I run "bin/rin project list --filter priority=High --sort name:asc"
    Then I should see only high priority projects sorted alphabetically by name