Feature: Quick Bug Entry via CLI
  As a developer
  I want to quickly enter bugs through the CLI with minimal input
  So that I can efficiently track issues without interrupting my workflow

  Background:
    Given the Rinna CLI is installed and configured
    And the developer has an active workspace

  # Positive test cases
  Scenario: Quick bug entry with just a description
    When the developer runs "rin bug 'Login button fails when using Firefox'"
    Then a work item should be created with:
      | type        | BUG                                    |
      | title       | Login button fails when using Firefox  |
      | status      | FOUND                                  |
      | priority    | MEDIUM                                 |
    And the CLI should display the new bug ID and details
    And the bug should be added to the developer's backlog

  Scenario: Quick bug entry with priority flag
    When the developer runs "rin bug --priority=HIGH 'Database connection timeout during peak hours'"
    Then a work item should be created with:
      | type        | BUG                                             |
      | title       | Database connection timeout during peak hours   |
      | status      | FOUND                                           |
      | priority    | HIGH                                            |
    And the CLI should display the new bug ID and details
    And the bug should be added to the developer's backlog with HIGH priority

  Scenario: Quick bug entry with short priority flag
    When the developer runs "rin bug -p LOW 'Minor alignment issue in sidebar'"
    Then a work item should be created with:
      | type        | BUG                                |
      | title       | Minor alignment issue in sidebar   |
      | status      | FOUND                              |
      | priority    | LOW                                |
    And the CLI should display the new bug ID and details

  Scenario: Setting bug metadata at creation time
    When the developer runs "rin bug 'Form validation fails' --browser=Chrome --component=auth"
    Then a work item should be created with:
      | type        | BUG                      |
      | title       | Form validation fails    |
      | status      | FOUND                    |
      | priority    | MEDIUM                   |
    And the work item should have metadata:
      | browser     | Chrome                   |
      | component   | auth                     |

  Scenario: Moving a bug up in the backlog
    Given the developer's backlog contains:
      | ID     | Type   | Title                  | Priority |
      | WI-101 | TASK   | Implement login page   | MEDIUM   |
      | WI-102 | BUG    | Button alignment issue | LOW      |
      | WI-103 | TASK   | Add unit tests         | MEDIUM   |
    When the developer runs "rin backlog move WI-102 up"
    Then the backlog order should be "WI-101, WI-102, WI-103"
    And the CLI should display the updated backlog

  Scenario: Moving a bug to the top of the backlog
    Given the developer's backlog contains:
      | ID     | Type   | Title                  | Priority |
      | WI-101 | TASK   | Implement login page   | MEDIUM   |
      | WI-102 | BUG    | Button alignment issue | LOW      |
      | WI-103 | TASK   | Add unit tests         | MEDIUM   |
    When the developer runs "rin backlog top WI-102"
    Then the backlog order should be "WI-102, WI-101, WI-103"
    And the CLI should display the updated backlog

  Scenario: Moving a bug down in the backlog
    Given the developer's backlog contains:
      | ID     | Type   | Title                  | Priority |
      | WI-101 | BUG    | Button alignment issue | LOW      |
      | WI-102 | TASK   | Implement login page   | MEDIUM   |
      | WI-103 | TASK   | Add unit tests         | MEDIUM   |
    When the developer runs "rin backlog move WI-101 down"
    Then the backlog order should be "WI-102, WI-101, WI-103"
    And the CLI should display the updated backlog

  Scenario: Moving a bug to the bottom of the backlog
    Given the developer's backlog contains:
      | ID     | Type   | Title                  | Priority |
      | WI-101 | BUG    | Button alignment issue | LOW      |
      | WI-102 | TASK   | Implement login page   | MEDIUM   |
      | WI-103 | TASK   | Add unit tests         | MEDIUM   |
    When the developer runs "rin backlog bottom WI-101"
    Then the backlog order should be "WI-102, WI-103, WI-101"
    And the CLI should display the updated backlog

  Scenario: Marking a bug as tested
    Given a bug "WI-201" with title "Login fails" in status "IN_PROGRESS"
    When the developer runs "rin test WI-201"
    Then the work item "WI-201" should be updated with status "IN_TEST"
    And the CLI should display the updated work item details

  Scenario: Marking a bug as completed
    Given a bug "WI-301" with title "Button misaligned" in status "IN_TEST"
    When the developer runs "rin done WI-301"
    Then the work item "WI-301" should be updated with status "DONE"
    And the CLI should display the updated work item details
    And the CLI should congratulate the developer on fixing the bug

  Scenario: Viewing developer's backlog
    Given the developer has several bugs and tasks in their backlog
    When the developer runs "rin backlog"
    Then the CLI should display the backlog items in order
    And each item should show its ID, type, title, and priority
    And bugs should be highlighted differently than other work items

  # Negative test cases
  Scenario: Bug entry without description
    When the developer runs "rin bug"
    Then the CLI should display an error message
    And the error should explain that a description is required

  Scenario: Bug entry with invalid priority
    When the developer runs "rin bug --priority=CRITICAL 'System crash on startup'"
    Then the CLI should display an error message
    And the error should list the valid priority values

  Scenario: Moving a non-existent item in the backlog
    When the developer runs "rin backlog move WI-999 up"
    Then the CLI should display an error message
    And the error should indicate that the item does not exist in the backlog

  Scenario: Moving an already top item further up
    Given the developer's backlog contains:
      | ID     | Type   | Title                  | Priority |
      | WI-101 | BUG    | Button alignment issue | LOW      |
      | WI-102 | TASK   | Implement login page   | MEDIUM   |
    When the developer runs "rin backlog move WI-101 up"
    Then the CLI should display a message indicating the item is already at the top

  Scenario: Moving an already bottom item further down
    Given the developer's backlog contains:
      | ID     | Type   | Title                  | Priority |
      | WI-101 | TASK   | Implement login page   | MEDIUM   |
      | WI-102 | BUG    | Button alignment issue | LOW      |
    When the developer runs "rin backlog move WI-102 down"
    Then the CLI should display a message indicating the item is already at the bottom

  Scenario: Marking an already DONE bug as done
    Given a bug "WI-401" with title "Fixed issue" in status "DONE"
    When the developer runs "rin done WI-401"
    Then the CLI should display a message that the bug is already completed