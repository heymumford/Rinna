Feature: Advanced Linux-Style Command Shortcuts
  As a Linux power user
  I want to use more advanced Linux-style commands in Rinna
  So that I can work more efficiently with familiar commands

  Background:
    Given the Rinna system is initialized
    And I am logged in as a developer
    And I have the following work items:
      | ID | Title                    | State       | Priority | Assignee | Description            |
      | 1  | Implement login screen   | IN_PROGRESS | MEDIUM   | bob      | Login form with auth   |
      | 2  | Create user registration | READY       | LOW      | alice    | Registration workflow  |
      | 3  | Design password reset    | BACKLOG     | HIGH     | bob      | Password reset feature |
      | 4  | Refactor API endpoints   | DONE        | LOW      | charlie  | Cleanup API code       |
      | 5  | Update documentation     | BACKLOG     | MEDIUM   | alice    | Improve user docs      |
    And work item 2 is a child of work item 1
    And I have permission level "standard"

  # mv command tests - Moving work items between states or reassigning
  @positive @unix @mv
  Scenario: Move work item to a different state
    When I run "rin mv 3 --state=READY"
    Then the command should succeed
    And work item 3 should be in state "READY"
    And I should see a success message "Work item 3 moved to READY state"

  @positive @unix @mv
  Scenario: Reassign work item to a different assignee
    When I run "rin mv 5 --assignee=bob"
    Then the command should succeed
    And work item 5 should be assigned to "bob"
    And I should see a success message "Work item 5 assigned to bob"

  @positive @unix @mv
  Scenario: Move work item to a different parent
    When I run "rin mv 3 --parent=1"
    Then the command should succeed
    And work item 3 should be a child of work item 1
    And I should see a success message "Work item 3 moved to parent 1"

  @positive @unix @mv
  Scenario: Move work item to a different state and assignee at once
    When I run "rin mv 5 --state=IN_PROGRESS --assignee=charlie"
    Then the command should succeed
    And work item 5 should be in state "IN_PROGRESS"
    And work item 5 should be assigned to "charlie"
    And I should see a success message "Work item 5 moved to IN_PROGRESS state and assigned to charlie"

  @negative @unix @mv
  Scenario: Attempt to move non-existent work item
    When I run "rin mv 99 --state=READY"
    Then the command should fail
    And I should see an error message "Work item not found: 99"

  @negative @unix @mv
  Scenario: Attempt to move work item to invalid state
    When I run "rin mv 3 --state=INVALID_STATE"
    Then the command should fail
    And I should see an error message "Invalid state: INVALID_STATE"

  @negative @unix @mv @security
  Scenario: Attempt to move work item with insufficient permissions
    Given I have permission level "readonly"
    When I run "rin mv 3 --state=READY"
    Then the command should fail
    And I should see an error message "Insufficient permissions to move work item"

  @negative @unix @mv @security
  Scenario: Attempt command injection in mv
    When I run "rin mv 3 --state=READY; rm -rf /"
    Then the command should fail
    And I should see an error message "Invalid arguments"
    And the command injection should not succeed

  # cp command tests - Duplicating work items
  @positive @unix @cp
  Scenario: Copy work item to create a duplicate
    When I run "rin cp 1 --title='Copy of login screen'"
    Then the command should succeed
    And a new work item should be created
    And the new work item should have title "Copy of login screen"
    And the new work item should have the same description as work item 1
    And I should see a success message "Created new work item from copy of work item 1"

  @positive @unix @cp
  Scenario: Copy work item with modified fields
    When I run "rin cp 1 --title='New login implementation' --state=BACKLOG --priority=HIGH"
    Then the command should succeed
    And a new work item should be created
    And the new work item should have title "New login implementation"
    And the new work item should be in state "BACKLOG"
    And the new work item should have priority "HIGH"
    And the new work item should have the same description as work item 1
    And I should see a success message "Created new work item from copy of work item 1"

  @positive @unix @cp
  Scenario: Copy work item and preserve parent relationship
    When I run "rin cp 2 --keep-parent"
    Then the command should succeed
    And a new work item should be created
    And the new work item should be a child of work item 1
    And I should see a success message "Created new work item from copy of work item 2"

  @negative @unix @cp
  Scenario: Attempt to copy non-existent work item
    When I run "rin cp 99"
    Then the command should fail
    And I should see an error message "Work item not found: 99"

  @negative @unix @cp @security
  Scenario: Attempt to copy work item with insufficient permissions
    Given I have permission level "readonly"
    When I run "rin cp 1"
    Then the command should fail
    And I should see an error message "Insufficient permissions to create work items"

  # mkdir command tests - Creating new projects or categories
  @positive @unix @mkdir
  Scenario: Create a new project
    When I run "rin mkdir NewProject --desc='A new project for testing'"
    Then the command should succeed
    And a new project should be created with name "NewProject"
    And I should see a success message "Created new project: NewProject"

  @positive @unix @mkdir
  Scenario: Create a new category within a project
    When I run "rin mkdir ProjectA/CategoryB --type=category"
    Then the command should succeed
    And a new category should be created with name "CategoryB" in project "ProjectA"
    And I should see a success message "Created new category: CategoryB in project ProjectA"

  @positive @unix @mkdir
  Scenario: Create nested project structure
    When I run "rin mkdir -p ProjectX/ModuleY/ComponentZ"
    Then the command should succeed
    And a new project structure should be created with path "ProjectX/ModuleY/ComponentZ"
    And I should see a success message "Created project hierarchy: ProjectX/ModuleY/ComponentZ"

  @negative @unix @mkdir
  Scenario: Attempt to create project with name that already exists
    Given a project named "ExistingProject" already exists
    When I run "rin mkdir ExistingProject"
    Then the command should fail
    And I should see an error message "Project already exists: ExistingProject"

  @negative @unix @mkdir @security
  Scenario: Attempt to create project with insufficient permissions
    Given I have permission level "user"
    When I run "rin mkdir NewProject"
    Then the command should fail
    And I should see an error message "Insufficient permissions to create projects"

  # rm command tests - Deleting work items
  @positive @unix @rm
  Scenario: Delete a work item
    When I run "rin rm 4"
    Then the command should succeed
    And work item 4 should be deleted
    And I should see a success message "Deleted work item 4"

  @positive @unix @rm
  Scenario: Force delete a work item without confirmation
    When I run "rin rm -f 5"
    Then the command should succeed
    And work item 5 should be deleted
    And I should see a success message "Deleted work item 5"

  @positive @unix @rm
  Scenario: Delete multiple work items
    When I run "rin rm 4 5"
    Then the command should succeed
    And work item 4 should be deleted
    And work item 5 should be deleted
    And I should see a success message "Deleted work items: 4, 5"

  @negative @unix @rm
  Scenario: Attempt to delete non-existent work item
    When I run "rin rm 99"
    Then the command should fail
    And I should see an error message "Work item not found: 99"

  @negative @unix @rm
  Scenario: Attempt to delete a work item that has children without force flag
    When I run "rin rm 1"
    Then the command should fail
    And I should see an error message "Cannot delete work item with children. Use -f to force delete."

  @negative @unix @rm @security
  Scenario: Attempt to delete work item with insufficient permissions
    Given I have permission level "readonly"
    When I run "rin rm 3"
    Then the command should fail
    And I should see an error message "Insufficient permissions to delete work items"

  @negative @unix @rm @security
  Scenario: Attempt command injection in rm
    When I run "rin rm 4; echo HACKED"
    Then the command should fail
    And I should see an error message "Invalid arguments"
    And the command injection should not succeed

  # touch command tests - Updating timestamps on work items
  @positive @unix @touch
  Scenario: Update the modified timestamp of a work item
    When I run "rin touch 3"
    Then the command should succeed
    And work item 3 should have an updated timestamp
    And I should see a success message "Updated timestamp for work item 3"

  @positive @unix @touch
  Scenario: Update the modified timestamp of multiple work items
    When I run "rin touch 1 2 3"
    Then the command should succeed
    And work item 1 should have an updated timestamp
    And work item 2 should have an updated timestamp
    And work item 3 should have an updated timestamp
    And I should see a success message "Updated timestamps for work items: 1, 2, 3"

  @positive @unix @touch
  Scenario: Create new work item with touch if it doesn't exist
    When I run "rin touch --create TaskTitle"
    Then the command should succeed
    And a new work item should be created with title "TaskTitle"
    And I should see a success message "Created new work item: TaskTitle"

  @negative @unix @touch
  Scenario: Attempt to touch non-existent work item without create flag
    When I run "rin touch 99"
    Then the command should fail
    And I should see an error message "Work item not found: 99"

  @negative @unix @touch @security
  Scenario: Attempt to touch work item with insufficient permissions
    Given I have permission level "readonly"
    When I run "rin touch 3"
    Then the command should fail
    And I should see an error message "Insufficient permissions to update work items"

  # chmod command tests - Changing permissions on work items
  @positive @unix @chmod
  Scenario: Change work item visibility to public
    When I run "rin chmod public 3"
    Then the command should succeed
    And work item 3 should have "public" visibility
    And I should see a success message "Changed work item 3 visibility to public"

  @positive @unix @chmod
  Scenario: Change work item visibility to private
    When I run "rin chmod private 1"
    Then the command should succeed
    And work item 1 should have "private" visibility
    And I should see a success message "Changed work item 1 visibility to private"

  @positive @unix @chmod
  Scenario: Change work item visibility for multiple items
    When I run "rin chmod team 1 2 3"
    Then the command should succeed
    And work item 1 should have "team" visibility
    And work item 2 should have "team" visibility
    And work item 3 should have "team" visibility
    And I should see a success message "Changed visibility to team for work items: 1, 2, 3"

  @positive @unix @chmod
  Scenario: Add specific user access to work item
    When I run "rin chmod +user dave 3"
    Then the command should succeed
    And work item 3 should have access for user "dave"
    And I should see a success message "Added access for dave to work item 3"

  @positive @unix @chmod
  Scenario: Remove specific user access from work item
    When I run "rin chmod -user eve 1"
    Then the command should succeed
    And work item 1 should not have access for user "eve"
    And I should see a success message "Removed access for eve from work item 1"

  @negative @unix @chmod
  Scenario: Attempt to change permissions on non-existent work item
    When I run "rin chmod public 99"
    Then the command should fail
    And I should see an error message "Work item not found: 99"

  @negative @unix @chmod
  Scenario: Attempt to change permissions to invalid value
    When I run "rin chmod superuser 3"
    Then the command should fail
    And I should see an error message "Invalid visibility level: superuser"

  @negative @unix @chmod @security
  Scenario: Attempt to change permissions with insufficient privileges
    Given I have permission level "user"
    When I run "rin chmod public 3"
    Then the command should fail
    And I should see an error message "Insufficient permissions to change work item visibility"

  # Command pipeline tests - Combining commands
  @positive @unix @pipeline
  Scenario: Find and display work items with pipeline
    When I run "rin find -assignee=bob | rin cat"
    Then the command should succeed
    And I should see detailed information for a work item assigned to "bob"

  @positive @unix @pipeline
  Scenario: Find, filter, and display work items with pipeline
    When I run "rin find -state=BACKLOG | rin grep HIGH | rin cat"
    Then the command should succeed
    And I should see detailed information for high priority backlog items

  @positive @unix @pipeline
  Scenario: Find and change work items with pipeline
    When I run "rin find -assignee=alice | rin mv --assignee=bob"
    Then the command should succeed
    And work items previously assigned to "alice" should now be assigned to "bob"

  @negative @unix @pipeline @security
  Scenario: Attempt command injection in pipeline
    When I run "rin find -state=BACKLOG | rm -rf /; rin cat"
    Then the command should fail
    And I should see an error message "Invalid pipeline command"
    And the command injection should not succeed

  @negative @unix @pipeline
  Scenario: Attempt to use incompatible commands in pipeline
    When I run "rin mkdir NewProject | rin cat"
    Then the command should fail
    And I should see an error message "Cannot pipe output from mkdir to cat"