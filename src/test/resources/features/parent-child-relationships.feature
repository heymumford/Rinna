Feature: Parent-Child Relationships for Work Items
  As a developer
  I want to organize related work items into parent-child hierarchies
  So that I can better manage complex tasks and their dependencies

  Background:
    Given the Rinna system is initialized
    And I am logged in as a developer
    And I have the following work items:
      | ID | Title                    | State       | Priority | Assignee |
      | 5  | Implement login screen   | IN_PROGRESS | MEDIUM   | bob      |
      | 6  | Create user registration | READY       | LOW      | alice    |
      | 7  | Design password reset    | BACKLOG     | HIGH     | bob      |

  @positive @parent-child
  Scenario: Create a parent work item for multiple children
    When I run "rin makechildren 5,6,7"
    Then the command should succeed
    And a new parent work item should be created
    And the new parent work item should have title "Parent of 5,6,7"
    And work items 5, 6, and 7 should have a "belongs to parent" relationship to the new work item
    And I should see the ID of the newly created parent work item

  @positive @parent-child
  Scenario: List all parent work items
    Given I have created parent work items with children:
      | Parent ID | Parent Title       | Child IDs |
      | 10        | User Auth Features | 5,6,7     |
      | 11        | Admin Functions    | 8,9       |
    When I run "rin list p"
    Then the command should succeed
    And I should see work item 10 "User Auth Features" in the results
    And I should see work item 11 "Admin Functions" in the results
    And I should not see any child work items in the results

  @positive @parent-child
  Scenario: Display pretty inheritance diagram
    Given I have a multi-level hierarchy of work items:
      | ID | Title           | Parent ID |
      | 1  | Project Alpha   | -         |
      | 2  | User Management | 1         |
      | 3  | Admin Panel     | 1         |
      | 4  | Settings UI     | 1         |
      | 5  | Login UI        | 2         |
      | 6  | Registration    | 2         |
      | 7  | User Roles      | 3         |
    When I run "rin list pretty"
    Then the command should succeed
    And I should see an ASCII inheritance diagram
    And the diagram should show "Project Alpha" as the top-level item
    And the diagram should show "User Management" indented under "Project Alpha"
    And the diagram should show "Login UI" indented under "User Management"

  @positive @parent-child
  Scenario: Interactive update of a work item
    Given I have a work item with ID 5
    When I run "rin update 5"
    Then I should see the current values of the work item fields
    And each field should be numbered for selection
    When I enter "2" to select the second field
    Then I should be prompted to enter a new value for that field
    When I enter a new value
    Then the field should be updated with the new value
    And the command should succeed

  @positive @parent-child
  Scenario: Print work item with all metadata and relationships
    Given I have work item 6 that belongs to parent work item 10
    When I run "rin print 6"
    Then the command should succeed
    And I should see all standard fields of work item 6
    And I should see the parent relationship to work item 10
    And I should see the complete history of work item 6
    And I should see all internal metadata for work item 6

  @positive @parent-child
  Scenario: Create parent from children with different priorities
    When I run "rin makechildren 5,7"
    Then the command should succeed
    And a new parent work item should be created
    And the new parent work item should have the highest priority of its children
    
  @positive @parent-child
  Scenario: Parent work item inherits state from children
    Given I have created a parent work item 10 for children 5, 6, and 7
    When all child work items are in "DONE" state
    Then the parent work item should automatically update to "DONE" state
    
  @positive @parent-child
  Scenario: Create parent from children with custom title
    When I run "rin makechildren 5,6,7 --title='User Authentication Feature'"
    Then the command should succeed
    And a new parent work item should be created with title "User Authentication Feature"
    
  @negative @parent-child
  Scenario: Attempt to create parent with invalid work item IDs
    When I run "rin makechildren 99,100,101"
    Then the command should fail
    And I should see an error message "One or more work items not found: 99, 100, 101"

  @negative @parent-child
  Scenario: Attempt to create parent with already parented work items
    Given work item 5 already belongs to parent work item 10
    When I run "rin makechildren 5,6,7"
    Then the command should fail
    And I should see an error message "Work item 5 already has a parent"

  @negative @parent-child
  Scenario: Attempt to create parent with no work item IDs
    When I run "rin makechildren"
    Then the command should fail
    And I should see an error message "No work item IDs provided"

  @negative @parent-child
  Scenario: Attempt to create parent with invalid ID format
    When I run "rin makechildren abc,def"
    Then the command should fail
    And I should see an error message "Invalid work item ID format"

  @negative @parent-child
  Scenario: Attempt to create circular dependency
    Given I have a work item 10 that is a parent of work item 5
    When I run "rin makechildren 10 --parent=5"
    Then the command should fail
    And I should see an error message "Circular dependency detected"

  @negative @parent-child
  Scenario: Interactive update with invalid field selection
    Given I have a work item with ID 5
    When I run "rin update 5"
    And I enter "99" to select a non-existent field
    Then I should see an error message "Invalid selection"
    And I should be prompted to try again

  @negative @parent-child @security
  Scenario: Print command with malicious input
    When I run "rin print '; DROP TABLE WORKITEMS; --"
    Then the command should fail
    And I should see an error message "Invalid work item ID"
    And no database changes should occur

  @negative @parent-child
  Scenario: List pretty with no hierarchical relationships
    Given I have only flat work items with no parent-child relationships
    When I run "rin list pretty"
    Then the command should succeed
    And I should see a message "No parent-child relationships found"