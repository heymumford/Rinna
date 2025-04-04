Feature: Workflow management
  To manage software work clearly and transparently
  As a software engineering team
  We need explicit workflow enforcement

  Scenario: Creating a new Bug item
    Given the Rinna system is initialized
    When the developer creates a new Bug with title "Login fails"
    Then the Bug should exist with status "To Do" and priority "medium"

  Scenario: Progressing an item through workflow
    Given a Bug titled "Login fails" exists
    When the developer updates the Bug status to "In Progress"
    Then the Bug's status should be "In Progress"

  Scenario: Validating workflow transitions
    Given a Bug titled "Login fails" exists
    When the developer attempts an invalid status transition to "Done"
    Then the system should explicitly reject the transition