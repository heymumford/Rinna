@bdd @messaging
Feature: Messaging Command Functionality
  As a user of the Rinna CLI
  I want to send, receive, and manage messages
  So that I can communicate with team members through the CLI

  Background:
    Given I am logged in as "test.user"
    And I am working on project "Tracer"

  @smoke
  Scenario: Listing messages
    Given I have unread messages in my inbox
    When I run the command "rin msg"
    Then the command should execute successfully
    And the output should contain "Messages for test.user"
    And all messages should be marked as read

  Scenario: Viewing unread messages
    Given I have unread messages in my inbox
    When I run the command "rin msg unread"
    Then the command should execute successfully
    And the output should contain "Unread messages for test.user"
    And the messages should remain marked as unread

  Scenario: Reading a specific message
    Given I have a message with ID "msg-12345678" in my inbox
    When I run the command "rin msg read msg-12345678"
    Then the command should execute successfully
    And the output should contain "Message msg-12345678"
    And the message should be marked as read

  Scenario: Deleting a message
    Given I have a message with ID "msg-12345678" in my inbox
    When I run the command "rin msg delete msg-12345678"
    Then the command should execute successfully
    And the output should contain "Message deleted"
    And the message should no longer exist in my inbox

  Scenario: Replying to a message
    Given I have a message with ID "msg-12345678" from "sender1" in my inbox
    When I run the command "rin msg reply msg-12345678 This is my reply"
    Then the command should execute successfully
    And the output should contain "Reply sent to sender1"
    And a reply should be sent to "sender1" with content "This is my reply"

  Scenario: Sending a direct message
    Given the user "team.member" exists in my current project
    When I run the command "rin msg team.member Hello, this is a direct message"
    Then the command should execute successfully
    And the output should contain "Message sent to team.member"
    And a message should be sent to "team.member" with content "Hello, this is a direct message"

  Scenario: Filtering messages by sender
    Given I have messages from different senders in my inbox
    When I run the command "rin msg from sender1"
    Then the command should execute successfully
    And the output should contain "Messages from sender1"
    And only messages from "sender1" should be displayed

  Scenario: Filtering messages by project
    Given I have messages from different projects in my inbox
    When I run the command "rin msg --project=Quantum"
    Then the command should execute successfully
    And the output should contain "Messages from project Quantum"
    And only messages from project "Quantum" should be displayed

  Scenario: Listing projects for messaging
    When I run the command "rin msg project list"
    Then the command should execute successfully
    And the output should contain "Projects:"
    And the output should contain "Tracer (TRC)"
    And the output should contain "Quantum (QTM)"

  Scenario: Switching projects for messaging
    When I run the command "rin msg project switch Quantum"
    Then the command should execute successfully
    And the output should contain "Switched to project: Quantum"
    And the current project should be set to "Quantum"

  Scenario: Error when sending message to non-project member
    Given the user "external.user" does not exist in my current project
    When I run the command "rin msg external.user This message should not be sent"
    Then the command should fail with error code 1
    And the error output should contain "Error: Cannot send message: external.user is not a member of project"

  Scenario: Error when attempting to read non-existent message
    When I run the command "rin msg read non-existent-message"
    Then the command should fail with error code 1
    And the error output should contain "Error: Message with ID 'non-existent-message' not found"

  Scenario: Error when attempting to read message belonging to another user
    Given there is a message with ID "msg-other" belonging to another user
    When I run the command "rin msg read msg-other"
    Then the command should fail with error code 1
    And the error output should contain "Error: Cannot read: You don't have permission"

  Scenario: Message formatting options are displayed in help
    When I run the command "rin msg --help"
    Then the command should execute successfully
    And the output should contain "Message Formatting:"
    And the output should contain "|RED|"
    And the output should contain "|GREEN|"
    And the output should contain "|BOLD|"