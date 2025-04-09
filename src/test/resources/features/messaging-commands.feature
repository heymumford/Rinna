Feature: Developer Messaging via Rinna CLI
  As a developer on a project
  I want to communicate with other team members via the Rinna CLI
  So that I can coordinate work without leaving my command line

  Background:
    Given a Rinna server is running at "http://localhost:8088"
    And the following users exist in the system:
      | Username | Password | Auth Token              |
      | eric     | pass123  | eric-auth-token-12345   |
      | steve    | secure99 | steve-auth-token-67890  |
      | maria    | pwd456!  | maria-auth-token-24680  |
    And the following projects exist:
      | Project Name | Key    | Members        |
      | Tracer       | TRACE  | eric, steve    |
      | Quantum      | QNTM   | steve, maria   |
      | Phoenix      | PHNX   | eric, maria    |

  # Authentication tests
  @positive @messaging @authentication
  Scenario: User successfully authenticates with Rinna CLI
    When I run "rin login --username=eric --password=pass123"
    Then the command should succeed
    And I should see a success message "Authentication successful. Welcome, eric!"
    And eric should receive an authentication token
    And the token should be stored securely in the local configuration

  @positive @messaging @authentication
  Scenario: Authentication token is reused for subsequent commands
    Given eric is already authenticated
    When I run "rin project list"
    Then the command should succeed
    And the request should include eric's authentication token
    And I should not be prompted for credentials

  @negative @messaging @authentication
  Scenario: User attempts to authenticate with invalid credentials
    When I run "rin login --username=eric --password=wrongpass"
    Then the command should fail
    And I should see an error message "Authentication failed: Invalid credentials"
    And eric should not receive an authentication token

  # Project switching tests
  @positive @messaging @project
  Scenario: User switches to a project
    Given eric is already authenticated
    When I run "rin project switch Tracer"
    Then the command should succeed
    And I should see a success message "Switched to project: Tracer"
    And the current project context should be set to "Tracer"

  @negative @messaging @project
  Scenario: User attempts to switch to a project they don't have access to
    Given eric is already authenticated
    When I run "rin project switch Quantum"
    Then the command should fail
    And I should see an error message "Access denied: You are not a member of project Quantum"

  # Messaging tests
  @positive @messaging @sending
  Scenario: User sends a message to another user in the same project
    Given eric is already authenticated
    And eric has switched to project "Tracer"
    When I run "rin msg steve Hey, could you review my PR?"
    Then the command should succeed
    And I should see a success message "Message sent to steve"
    And the message should be stored on the server
    And the message metadata should include:
      | Sender   | Recipient | Project | Timestamp | Read Status |
      | eric     | steve     | Tracer  | *         | UNREAD      |

  @positive @messaging @sending
  Scenario: User sends a multi-word message to another user
    Given eric is already authenticated
    And eric has switched to project "Tracer"
    When I run "rin msg steve Can you please check the bug in the login component?"
    Then the command should succeed
    And I should see a success message "Message sent to steve"
    And the message "Can you please check the bug in the login component?" should be sent to steve

  @negative @messaging @sending
  Scenario: User attempts to send a message to a non-existent user
    Given eric is already authenticated
    And eric has switched to project "Tracer"
    When I run "rin msg unknown_user Hey there"
    Then the command should fail
    And I should see an error message "User 'unknown_user' not found"

  @negative @messaging @sending
  Scenario: User attempts to send a message without authentication
    When I run "rin msg steve Hello"
    Then the command should fail
    And I should see an error message "Authentication required. Please login first."

  @negative @messaging @sending
  Scenario: User attempts to send a message without switching to a project
    Given eric is already authenticated
    But eric has not switched to any project
    When I run "rin msg steve Hello"
    Then the command should fail
    And I should see an error message "No active project. Please switch to a project first."

  @negative @messaging @sending
  Scenario: User attempts to send a message to someone not in their current project
    Given eric is already authenticated
    And eric has switched to project "Tracer"
    When I run "rin msg maria Hello"
    Then the command should fail
    And I should see an error message "Cannot send message: maria is not a member of project Tracer"

  # Message notification tests
  @positive @messaging @notification
  Scenario: User sees notification of unread messages when using CLI
    Given steve is already authenticated
    And steve has an unread message from eric in project "Tracer"
    When steve runs any Rinna CLI command
    Then the command output should be preceded by a message notification
    And the notification should show "You have 1 unread message from eric: 'Hey, could you review my PR?'"

  @positive @messaging @notification
  Scenario: User sees notifications of multiple unread messages
    Given steve is already authenticated
    And steve has the following unread messages:
      | Sender | Project | Message                            |
      | eric   | Tracer  | Hey, could you review my PR?       |
      | maria  | Quantum | The deployment is ready for testing |
    When steve runs any Rinna CLI command
    Then the command output should be preceded by message notifications
    And the notifications should show all unread messages

  # Message listing tests
  @positive @messaging @listing
  Scenario: User lists all received messages
    Given steve is already authenticated
    And steve has the following messages:
      | Sender | Project | Message                            | Timestamp           | Read   |
      | eric   | Tracer  | Hey, could you review my PR?       | 2025-04-05T10:30:00 | UNREAD |
      | maria  | Quantum | The deployment is ready for testing | 2025-04-05T09:15:00 | READ   |
    When I run "rin msg"
    Then the command should succeed
    And I should see a list of messages
    And the list should include all messages
    And unread messages should be highlighted

  @positive @messaging @listing
  Scenario: User lists only unread messages
    Given steve is already authenticated
    And steve has both read and unread messages
    When I run "rin msg --unread"
    Then the command should succeed
    And I should see a list of only unread messages

  @positive @messaging @listing
  Scenario: User lists messages filtered by sender
    Given steve is already authenticated
    And steve has messages from multiple senders
    When I run "rin msg --from=eric"
    Then the command should succeed
    And I should see a list of messages only from eric

  @positive @messaging @listing
  Scenario: User lists messages filtered by project
    Given steve is already authenticated
    And steve has messages from multiple projects
    When I run "rin msg --project=Tracer"
    Then the command should succeed
    And I should see a list of messages only from project Tracer

  # Reply tests
  @positive @messaging @reply
  Scenario: User replies to a specific message
    Given steve is already authenticated
    And steve has a message with ID "msg-123" from eric
    When I run "rin msg --reply msg-123 I'll review it today"
    Then the command should succeed
    And I should see a success message "Reply sent to eric"
    And eric should receive the reply "I'll review it today"
    And the reply should reference the original message

  @negative @messaging @reply
  Scenario: User attempts to reply to a non-existent message
    Given steve is already authenticated
    When I run "rin msg --reply non-existent-id Sure thing"
    Then the command should fail
    And I should see an error message "Message with ID 'non-existent-id' not found"

  # Message reading tests
  @positive @messaging @reading
  Scenario: Messages are marked as read when viewed
    Given steve is already authenticated
    And steve has unread messages
    When I run "rin msg"
    Then the command should succeed
    And all displayed messages should be marked as read on the server

  @positive @messaging @reading
  Scenario: User reads a specific message
    Given steve is already authenticated
    And steve has a message with ID "msg-123" from eric
    When I run "rin msg --read msg-123"
    Then the command should succeed
    And I should see the full content of message "msg-123"
    And message "msg-123" should be marked as read on the server

  # Message deletion tests
  @positive @messaging @deletion
  Scenario: User deletes a message
    Given steve is already authenticated
    And steve has a message with ID "msg-123" from eric
    When I run "rin msg --delete msg-123"
    Then the command should succeed
    And I should see a success message "Message deleted"
    And message "msg-123" should be removed from steve's message list

  @negative @messaging @deletion
  Scenario: User attempts to delete a message they don't own
    Given eric is already authenticated
    And steve has a message with ID "msg-123" from eric
    When I run "rin msg --delete msg-123"
    Then the command should fail
    And I should see an error message "Cannot delete: You don't have permission to delete this message"