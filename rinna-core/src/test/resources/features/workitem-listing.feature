Feature: Work Item Listing
  As a developer using Rinna
  I want to be able to list all the work items in my repository
  So that I can track my work effectively

  Scenario: Listing work items when there are none
    Given there are no work items in the repository
    When I run the "rin list" command
    Then I should see a message indicating there are no work items
    And I should see instructions for adding work items

  Scenario: Listing work items when there are some
    Given there are work items in the repository
    When I run the "rin list" command
    Then I should see a list of all work items
    And each item should display its ID, title, type, priority, and status

  Scenario: Filtering work items by type
    Given there are work items of different types in the repository
    When I run the "rin list --type FEATURE" command
    Then I should see only the work items of type "FEATURE"

  Scenario: Filtering work items by status
    Given there are work items with different statuses in the repository
    When I run the "rin list --status TODO" command
    Then I should see only the work items with status "TODO"