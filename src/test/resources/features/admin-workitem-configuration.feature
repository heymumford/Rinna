@admin
Feature: Work Item Type Configuration
  As an admin user
  I want to define work item types with custom fields for my project
  So that project data can be structured according to specific requirements

  Background:
    Given I have installed Rinna in my Java project
    And the Rinna server is running
    And I am authenticated as the admin user
    And I have created a project named "Configuration Test"

  Scenario: Define basic work item types
    When I run "bin/rin type create --name EPIC --description 'Large feature that spans multiple releases'"
    Then I should see "Successfully created work item type: EPIC"
    And the work item type "EPIC" should be available in the project
    When I run "bin/rin type create --name STORY --description 'User story representing user value'"
    Then I should see "Successfully created work item type: STORY"
    And the work item type "STORY" should be available in the project
    When I run "bin/rin type create --name TASK --description 'Small unit of work'"
    Then I should see "Successfully created work item type: TASK"
    And the work item type "TASK" should be available in the project
    When I run "bin/rin type create --name BUG --description 'Software defect'"
    Then I should see "Successfully created work item type: BUG"
    And the work item type "BUG" should be available in the project
    When I run "bin/rin type list"
    Then I should see all 4 work item types listed
    And each type should show its description

  Scenario: Define work item types with custom fields
    When I run "bin/rin type create --name STORY --description 'User story' --field storyPoints:number --field acceptanceCriteria:text"
    Then I should see "Successfully created work item type: STORY"
    And the work item type "STORY" should have field "storyPoints" of type "number"
    And the work item type "STORY" should have field "acceptanceCriteria" of type "text"
    When I run "bin/rin type create --name BUG --description 'Software defect' --field severity:enum --values low,medium,high,critical --field stepsToReproduce:text"
    Then I should see "Successfully created work item type: BUG"
    And the work item type "BUG" should have field "severity" of type "enum" with values "low,medium,high,critical"
    And the work item type "BUG" should have field "stepsToReproduce" of type "text"
    When I run "bin/rin type create --name TASK --description 'Small unit of work' --field estimatedHours:number --field assignedTo:user"
    Then I should see "Successfully created work item type: TASK"
    And the work item type "TASK" should have field "estimatedHours" of type "number"
    And the work item type "TASK" should have field "assignedTo" of type "user"

  Scenario: Add fields to existing work item type
    Given I have created a work item type "EPIC" with description "Large feature"
    When I run "bin/rin type field --type EPIC --add businessValue:enum 'Business value' --values low,medium,high"
    Then I should see "Successfully added field to work item type: EPIC"
    And the work item type "EPIC" should have field "businessValue" of type "enum" with values "low,medium,high"
    When I run "bin/rin type field --type EPIC --add targetRelease:string 'Target release version'"
    Then I should see "Successfully added field to work item type: EPIC"
    And the work item type "EPIC" should have field "targetRelease" of type "string"
    When I run "bin/rin type field --type EPIC --add dueDate:date 'Due date'"
    Then I should see "Successfully added field to work item type: EPIC"
    And the work item type "EPIC" should have field "dueDate" of type "date"

  Scenario: Define required fields
    When I run "bin/rin type create --name SECURITY_BUG --description 'Security vulnerability' --field severity:enum --values low,medium,high,critical --required --field affectedVersion:string --required"
    Then I should see "Successfully created work item type: SECURITY_BUG"
    And the work item type "SECURITY_BUG" should have required field "severity"
    And the work item type "SECURITY_BUG" should have required field "affectedVersion"
    When I run "bin/rin type show --name SECURITY_BUG"
    Then I should see that "severity" is marked as required
    And I should see that "affectedVersion" is marked as required

  Scenario: Set default values for fields
    Given I have created a work item type "TASK" with fields "priority" and "estimatedHours"
    When I run "bin/rin type default --type TASK --field priority=MEDIUM --field estimatedHours=4"
    Then I should see "Successfully set default values for TASK"
    And the work item type "TASK" should have default value "MEDIUM" for field "priority"
    And the work item type "TASK" should have default value "4" for field "estimatedHours"
    When I run "bin/rin type show --name TASK"
    Then I should see "Default Value: MEDIUM" for field "priority"
    And I should see "Default Value: 4" for field "estimatedHours"

  Scenario: Create work items with configured types
    Given I have created a work item type "STORY" with fields "storyPoints:number" and "acceptanceCriteria:text"
    When I run "bin/rin item create --type STORY --title 'Login Feature' --field storyPoints=5 --field acceptanceCriteria='User should be able to log in with valid credentials'"
    Then I should see "Successfully created work item"
    And a new work item of type "STORY" should be created in the database
    And the work item should have "storyPoints" set to "5"
    And the work item should have "acceptanceCriteria" containing the specified acceptance criteria

  Scenario: Delete work item type
    Given I have created work item types "EPIC", "STORY", and "FEATURE"
    And no items exist with type "FEATURE"
    When I run "bin/rin type delete --name FEATURE"
    Then I should see "Successfully deleted work item type: FEATURE"
    And the work item type "FEATURE" should no longer exist
    When I run "bin/rin type list"
    Then I should see "EPIC" and "STORY"
    But I should not see "FEATURE"

  Scenario: Update work item type
    Given I have created a work item type "BUG" with description "Software defect"
    When I run "bin/rin type update --name BUG --new-name DEFECT --description 'Software issue requiring resolution'"
    Then I should see "Successfully updated work item type"
    And the work item type "DEFECT" should exist with the new description
    And the work item type "BUG" should no longer exist
    When I run "bin/rin type list"
    Then I should see "DEFECT" instead of "BUG"

  Scenario: View work item type definitions
    Given I have created work item types with various field configurations
    When I run "bin/rin type show --name STORY"
    Then I should see detailed information about the "STORY" type
    And I should see all fields defined for "STORY" including their types and constraints
    When I run "bin/rin type list --verbose"
    Then I should see a comprehensive list of all work item types
    And each type should show its fields, validations, and default values