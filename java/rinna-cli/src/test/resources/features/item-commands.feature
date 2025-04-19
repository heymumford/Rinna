@bdd @item
Feature: Item Command Functionality
  As a user of the Rinna CLI
  I want to manage work items
  So that I can create, view, update, and delete items

  Background:
    Given I am logged in as "testuser"
    And the system has the following work items:
      | ID      | Title                     | Description                            | Type | Priority | Status      | Assignee  |
      | WI-1001 | Authentication Feature    | Implement JWT authentication           | TASK | HIGH     | READY       | testuser  |
      | WI-1002 | Bug in Payment Module     | Fix transaction issues after payment   | BUG  | MEDIUM   | IN_PROGRESS | bob       |
      | WI-1003 | Update Documentation      | Add API reference documentation        | TASK | LOW      | DONE        | charlie   |
      | WI-1004 | Security Review           | Review security measures               | TASK | HIGH     | READY       | testuser  |
      | WI-1005 | Performance Optimization  | Improve database query performance     | TASK | MEDIUM   | READY       | testuser  |

  @smoke
  Scenario: Creating a new work item
    When I run the command "rin add --title 'New Feature' --description 'Implement new feature' --type TASK --priority MEDIUM"
    Then the command should execute successfully
    And the output should contain "Work item created successfully"
    And the output should contain a new work item ID
    And the new work item should have the following attributes:
      | title       | New Feature             |
      | description | Implement new feature   |
      | type        | TASK                    |
      | priority    | MEDIUM                  |
      | status      | CREATED                 |
      | assignee    | testuser                |

  Scenario: Creating a work item with default values
    When I run the command "rin add --title 'Simple Task'"
    Then the command should execute successfully
    And the output should contain "Work item created successfully"
    And the new work item should have the following attributes:
      | title       | Simple Task             |
      | type        | TASK                    |
      | priority    | MEDIUM                  |
      | status      | CREATED                 |

  Scenario: Viewing a work item
    When I run the command "rin view WI-1001"
    Then the command should execute successfully
    And the output should contain "Authentication Feature"
    And the output should contain "Implement JWT authentication"
    And the output should contain "READY"
    And the output should contain "HIGH"

  Scenario: Viewing a work item in JSON format
    When I run the command "rin view WI-1001 --format json"
    Then the command should execute successfully
    And the output should be valid JSON
    And the JSON output should contain "Authentication Feature"
    And the JSON output should contain "Implement JWT authentication"

  Scenario: Listing all work items
    When I run the command "rin list"
    Then the command should execute successfully
    And the output should contain "WI-1001"
    And the output should contain "WI-1002"
    And the output should contain "WI-1003"
    And the output should contain "WI-1004"
    And the output should contain "WI-1005"

  Scenario: Listing work items with filtering by status
    When I run the command "rin list --status READY"
    Then the command should execute successfully
    And the output should contain "WI-1001"
    And the output should contain "WI-1004"
    And the output should contain "WI-1005"
    And the output should not contain "WI-1002"
    And the output should not contain "WI-1003"

  Scenario: Listing work items with filtering by assignee
    When I run the command "rin list --assignee testuser"
    Then the command should execute successfully
    And the output should contain "WI-1001"
    And the output should contain "WI-1004"
    And the output should contain "WI-1005"
    And the output should not contain "WI-1002"
    And the output should not contain "WI-1003"

  Scenario: Listing work items with filtering by priority
    When I run the command "rin list --priority HIGH"
    Then the command should execute successfully
    And the output should contain "WI-1001"
    And the output should contain "WI-1004"
    And the output should not contain "WI-1002"
    And the output should not contain "WI-1003"
    And the output should not contain "WI-1005"

  Scenario: Updating a work item title
    When I run the command "rin update WI-1001 --title 'Updated Authentication Feature'"
    Then the command should execute successfully
    And the output should contain "Work item updated successfully"
    And the work item "WI-1001" should have title "Updated Authentication Feature"

  Scenario: Updating a work item description
    When I run the command "rin update WI-1001 --description 'Implement JWT authentication with enhanced security'"
    Then the command should execute successfully
    And the output should contain "Work item updated successfully"
    And the work item "WI-1001" should have description "Implement JWT authentication with enhanced security"

  Scenario: Updating a work item priority
    When I run the command "rin update WI-1001 --priority CRITICAL"
    Then the command should execute successfully
    And the output should contain "Work item updated successfully"
    And the work item "WI-1001" should have priority "CRITICAL"

  Scenario: Updating a work item assignee
    When I run the command "rin update WI-1001 --assignee alice"
    Then the command should execute successfully
    And the output should contain "Work item updated successfully"
    And the work item "WI-1001" should have assignee "alice"

  Scenario: Updating multiple attributes at once
    When I run the command "rin update WI-1001 --title 'Enhanced Auth' --priority LOW --assignee bob"
    Then the command should execute successfully
    And the output should contain "Work item updated successfully"
    And the work item "WI-1001" should have title "Enhanced Auth"
    And the work item "WI-1001" should have priority "LOW"
    And the work item "WI-1001" should have assignee "bob"

  Scenario: Adding a tag to a work item
    When I run the command "rin update WI-1001 --add-tag security"
    Then the command should execute successfully
    And the output should contain "Work item updated successfully"
    And the work item "WI-1001" should have tag "security"

  Scenario: Removing a tag from a work item
    Given the work item "WI-1001" has tag "frontend"
    When I run the command "rin update WI-1001 --remove-tag frontend"
    Then the command should execute successfully
    And the output should contain "Work item updated successfully"
    And the work item "WI-1001" should not have tag "frontend"

  Scenario: Adding a comment to a work item
    When I run the command "rin comment add WI-1001 'This is a test comment'"
    Then the command should execute successfully
    And the output should contain "Comment added successfully"
    And the work item "WI-1001" should have a comment "This is a test comment"

  Scenario: Viewing comments for a work item
    Given the work item "WI-1001" has the following comments:
      | Author    | Text                          | Timestamp           |
      | alice     | Initial design completed      | 2025-04-01 10:00:00 |
      | bob       | API endpoints defined         | 2025-04-02 14:30:00 |
      | testuser  | Starting implementation       | 2025-04-03 09:15:00 |
    When I run the command "rin comment list WI-1001"
    Then the command should execute successfully
    And the output should contain "Comments for WI-1001"
    And the output should contain "Initial design completed"
    And the output should contain "API endpoints defined"
    And the output should contain "Starting implementation"

  Scenario: Deleting a work item
    When I run the command "rin delete WI-1005 --confirm"
    Then the command should execute successfully
    And the output should contain "Work item WI-1005 deleted successfully"
    And the work item "WI-1005" should be deleted

  Scenario: Error when trying to view a non-existent work item
    When I run the command "rin view WI-9999"
    Then the command should fail with error code 1
    And the error output should contain "Error: Work item WI-9999 not found"

  Scenario: Error when trying to update a non-existent work item
    When I run the command "rin update WI-9999 --title 'New Title'"
    Then the command should fail with error code 1
    And the error output should contain "Error: Work item WI-9999 not found"

  Scenario: Error when creating a work item with missing required field
    When I run the command "rin add"
    Then the command should fail with error code 1
    And the error output should contain "Error: Title is required"

  Scenario: Listing work items in different formats
    When I run the command "rin list --format json"
    Then the command should execute successfully
    And the output should be valid JSON
    
  Scenario: Bulk creation of work items from a file
    Given I have a file "items.csv" with the following content:
      """
      title,description,type,priority
      Task 1,Description 1,TASK,MEDIUM
      Task 2,Description 2,TASK,HIGH
      Bug Fix,Critical bug fix,BUG,CRITICAL
      """
    When I run the command "rin bulk import --file items.csv"
    Then the command should execute successfully
    And the output should contain "3 work items imported successfully"
    And 3 new work items should be created with the specified attributes