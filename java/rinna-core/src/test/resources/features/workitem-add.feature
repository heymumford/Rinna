Feature: Work Item Addition
  As a developer using Rinna
  I want to be able to add work items to my repository
  So that I can track new work that needs to be done

  Scenario: Adding a work item with required fields
    When I run the "rin add --title 'Implement login form' --type FEATURE" command
    Then the command should succeed
    And a new work item should be created with title "Implement login form" and type "FEATURE"
    And I should see a success message

  Scenario: Adding a work item with all fields
    When I run the "rin add --title 'Fix authentication bug' --type BUG --priority HIGH --status TODO --description 'Users cannot log in' --assignee 'alice'" command
    Then the command should succeed
    And a new work item should be created with all specified attributes
    And I should see a success message

  Scenario: Adding a work item with invalid type
    When I run the "rin add --title 'Invalid type' --type INVALID_TYPE" command
    Then the command should fail
    And I should see an error message about invalid type

  Scenario: Adding a work item without a title
    When I run the "rin add --type BUG" command
    Then the command should fail
    And I should see an error message about missing title