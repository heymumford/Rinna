Feature: Work Item Edit Command
  As a Rinna user
  I want to edit work items interactively
  So that I can update their properties efficiently

  Background:
    Given a valid user session

  Scenario: View work item editing options
    Given an existing work item with ID "WI-123"
    When I execute the command "edit id=WI-123"
    Then I should see the work item details
    And I should see the following fields that can be edited
      | Title        |
      | Description  |
      | Priority     |
      | State        |
      | Assignee     |
    And I should see a prompt for selecting a field to edit
    And the command should track this operation with MetadataService

  Scenario: Update work item title
    Given an existing work item with ID "WI-123" and title "Original Title"
    When I execute the command "edit id=WI-123"
    And I select field "1" for title
    And I enter a new value "Updated Title"
    Then the work item title should be updated
    And I should see a success message for title update
    And the command should track this operation with MetadataService
    And the command should track a field-level operation for "Title"

  Scenario: Update work item description
    Given an existing work item with ID "WI-123" and description "Original description"
    When I execute the command "edit id=WI-123"
    And I select field "2" for description
    And I enter a new value "Updated description with more details"
    Then the work item description should be updated
    And I should see a success message for description update
    And the command should track this operation with MetadataService
    And the command should track a field-level operation for "Description"

  Scenario: Update work item priority
    Given an existing work item with ID "WI-123" and priority "MEDIUM"
    When I execute the command "edit id=WI-123"
    And I select field "3" for priority
    And I enter a new value "HIGH"
    Then the work item priority should be updated to "HIGH"
    And I should see a success message for priority update
    And the command should track this operation with MetadataService
    And the command should track a field-level operation for "Priority"

  Scenario: Update work item state
    Given an existing work item with ID "WI-123" and state "IN_PROGRESS"
    When I execute the command "edit id=WI-123"
    And I select field "4" for state
    And I enter a new value "DONE"
    Then the work item state should be updated to "DONE"
    And I should see a success message for state update
    And the command should track this operation with MetadataService
    And the command should track a field-level operation for "State"

  Scenario: Update work item assignee
    Given an existing work item with ID "WI-123" and assignee "user1@example.com"
    When I execute the command "edit id=WI-123"
    And I select field "5" for assignee
    And I enter a new value "user2@example.com"
    Then the work item assignee should be updated to "user2@example.com"
    And I should see a success message for assignee update
    And the command should track this operation with MetadataService
    And the command should track a field-level operation for "Assignee"

  Scenario: Cancel work item editing
    Given an existing work item with ID "WI-123"
    When I execute the command "edit id=WI-123"
    And I select field "0" to cancel
    Then the edit should be cancelled
    And I should see a cancellation message
    And the command should track this operation with MetadataService
    And no field updates should be tracked

  Scenario: Use JSON output format
    Given an existing work item with ID "WI-123"
    When I execute the command "edit --json id=WI-123"
    Then I should see the work item details in JSON format
    And I should see edit options in JSON format
    And the command should track this operation with MetadataService
    And the tracking parameters should include "format" as "json"

  Scenario: Use explicitly provided ID
    Given an existing work item with ID "WI-456"
    When I execute the command "edit WI-456"
    Then I should see the work item details for "WI-456"
    And the command should track this operation with MetadataService
    And the tracking parameters should include "itemId" as "WI-456"

  Scenario: Use last viewed item
    Given I previously viewed work item with ID "WI-789"
    When I execute the command "edit"
    Then I should see the work item details for "WI-789"
    And the command should track this operation with MetadataService

  Scenario: Handle invalid priority value
    Given an existing work item with ID "WI-123" and priority "MEDIUM"
    When I execute the command "edit id=WI-123"
    And I select field "3" for priority
    And I enter an invalid value "INVALID_PRIORITY"
    Then I should see an error message about invalid priority
    And the error message should list valid priority values
    And the command should track this operation failure with MetadataService
    And the command should track a field-level operation failure for "Priority"

  Scenario: Handle invalid state value
    Given an existing work item with ID "WI-123" and state "IN_PROGRESS"
    When I execute the command "edit id=WI-123"
    And I select field "4" for state
    And I enter an invalid value "INVALID_STATE"
    Then I should see an error message about invalid state
    And the error message should list valid state values
    And the command should track this operation failure with MetadataService
    And the command should track a field-level operation failure for "State"

  Scenario: Handle work item not found
    When I execute the command "edit id=NONEXISTENT"
    Then I should see an error message about item not found
    And the command should track this operation failure with MetadataService

  Scenario: Handle invalid ID format
    When I execute the command "edit id=invalid-id"
    Then I should see an error message about invalid ID format
    And the command should track this operation failure with MetadataService

  Scenario: Handle no context and no ID
    When I execute the command "edit"
    And there is no previously viewed work item
    Then I should see an error message about no context available
    And the error message should provide guidance on how to specify an ID
    And the command should track this operation failure with MetadataService
