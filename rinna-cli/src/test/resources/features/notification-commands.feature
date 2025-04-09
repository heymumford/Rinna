Feature: Notification Management Commands
  As a user of the Rinna CLI
  I want to manage my notifications
  So that I can stay informed about important updates

  Background:
    Given the current user is authenticated

  Scenario: Displaying all notifications
    Given the user has the following notifications:
      | Type      | Message                        | Read  |
      | SYSTEM    | System maintenance completed   | false |
      | UPDATE    | Work item WI-123 was updated   | true  |
      | COMMENT   | New comment on WI-456          | false |
    When I run the command "rin notify list"
    Then the command should execute successfully
    And the output should contain "=== Your Notifications ==="
    And the output should contain "System maintenance completed"
    And the output should contain "Work item WI-123 was updated"
    And the output should contain "New comment on WI-456"

  Scenario: Displaying unread notifications
    Given the user has the following notifications:
      | Type      | Message                        | Read  |
      | SYSTEM    | System maintenance completed   | false |
      | UPDATE    | Work item WI-123 was updated   | true  |
      | COMMENT   | New comment on WI-456          | false |
    When I run the command "rin notify unread"
    Then the command should execute successfully
    And the output should contain "=== Unread Notifications ==="
    And the output should contain "System maintenance completed"
    And the output should contain "New comment on WI-456"
    And the output should not contain "Work item WI-123 was updated"

  Scenario: Marking a notification as read
    Given the user has an unread notification with ID "123e4567-e89b-12d3-a456-426614174000" and message "Test notification"
    When I run the command "rin notify read 123e4567-e89b-12d3-a456-426614174000"
    Then the command should execute successfully
    And the output should contain "Notification marked as read"

  Scenario: Marking all notifications as read
    Given the user has 3 unread notifications
    When I run the command "rin notify markall"
    Then the command should execute successfully
    And the output should contain "All notifications marked as read"

  Scenario: Clearing old notifications
    Given the user has 5 notifications, with 2 older than 30 days
    When I run the command "rin notify clear"
    Then the command should execute successfully
    And the output should contain "Cleared 2 old notification"

  Scenario: Clearing notifications with custom days parameter
    Given the user has 5 notifications, with 3 older than 10 days
    When I run the command "rin notify clear --days=10"
    Then the command should execute successfully
    And the output should contain "Cleared 3 old notification"

  Scenario: Displaying notification help
    When I run the command "rin notify help"
    Then the command should execute successfully
    And the output should contain "Notification Command Usage:"
    And the output should contain "rin notify list"
    And the output should contain "rin notify unread"
    And the output should contain "rin notify markall"
    And the output should contain "rin notify clear"

  Scenario: Displaying notifications in JSON format
    Given the user has the following notifications:
      | Type      | Message                        | Read  |
      | SYSTEM    | System maintenance completed   | false |
      | UPDATE    | Work item WI-123 was updated   | true  |
    When I run the command "rin notify list --json"
    Then the command should execute successfully
    And the output should contain "\"result\": \"success\""
    And the output should contain "\"notifications\": ["

  Scenario: Attempting to access notifications when not authenticated
    Given the current user is not authenticated
    When I run the command "rin notify list"
    Then the command should fail with exit code 1
    And the output should contain "You must be logged in to access notifications"

  Scenario: Using an unknown notification action
    When I run the command "rin notify unknown"
    Then the command should fail with exit code 1
    And the output should contain "Unknown action: unknown"
