Feature: Import tasks from markdown files
  As a user
  I want to import tasks from markdown files
  So that I can quickly create work items from existing task lists

  Background:
    Given the user is logged in
    And the current project is "IMPORT-TEST"

  Scenario: Import tasks from a well-formatted markdown file
    Given a markdown file "tasks.md" with the following content:
      """
      # Todo
      - Task 1
      - Task 2
      
      # In Progress
      - Task 3
      
      # Done
      - Task 4
      """
    When the user runs "import tasks.md" command
    Then the command should succeed
    And 4 work items should be created
    And work item with title "Task 1" should have status "READY"
    And work item with title "Task 3" should have status "IN_PROGRESS"
    And work item with title "Task 4" should have status "DONE"
    And the output should contain "Successfully imported 4 task(s)"

  Scenario: Import tasks with different priorities
    Given a markdown file "priorities.md" with the following content:
      """
      # Todo
      - [low] Low priority task
      - [medium] Medium priority task
      - [high] High priority task
      - [critical] Critical priority task
      """
    When the user runs "import priorities.md" command
    Then the command should succeed
    And 4 work items should be created
    And work item with title "Low priority task" should have priority "LOW"
    And work item with title "Medium priority task" should have priority "MEDIUM"
    And work item with title "High priority task" should have priority "HIGH"
    And work item with title "Critical priority task" should have priority "CRITICAL"

  Scenario: Import with JSON output format
    Given a markdown file "simple.md" with the following content:
      """
      # Todo
      - Simple task
      """
    When the user runs "import simple.md --json" command
    Then the command should succeed
    And 1 work item should be created
    And the output should be in JSON format
    And the JSON output should contain "status" field with value "success"
    And the JSON output should contain "imported_count" field with value "1"

  Scenario: Import with partially parsable content
    Given a markdown file "mixed.md" with the following content:
      """
      # Todo
      - Valid task
      - This is not formatted as a task
      """
    When the user runs "import mixed.md" command
    Then the command should succeed
    And 1 work item should be created
    And the output should contain "Warning: Some content couldn't be parsed"
    And an import report should be generated

  Scenario: Fail when importing a non-markdown file
    Given a text file "notmarkdown.txt" with some content
    When the user runs "import notmarkdown.txt" command
    Then the command should fail
    And the error output should contain "File must be a markdown (.md) file"

  Scenario: Fail when importing an empty file
    Given an empty markdown file "empty.md"
    When the user runs "import empty.md" command
    Then the command should fail
    And the error output should contain "No tasks found in file"

  Scenario: Fail when importing a non-existent file
    When the user runs "import nonexistent.md" command
    Then the command should fail
    And the error output should contain "File not found"

  Scenario: Fail when no file path is provided
    When the user runs "import" command
    Then the command should fail
    And the error output should contain "No file path provided"
    And the error output should contain "Usage: rin import <file.md>"

  Scenario: Transition imported work items to correct workflow states
    Given a markdown file "workflow.md" with the following content:
      """
      # Todo
      - Ready task
      
      # In Progress
      - In progress task
      
      # Review
      - Review task
      
      # Testing
      - Testing task
      
      # Done
      - Done task
      
      # Blocked
      - Blocked task
      """
    When the user runs "import workflow.md" command
    Then the command should succeed
    And 6 work items should be created
    And work item with title "Ready task" should have status "READY"
    And work item with title "In progress task" should have status "IN_PROGRESS"
    And work item with title "Review task" should have status "REVIEW" 
    And work item with title "Testing task" should have status "TESTING"
    And work item with title "Done task" should have status "DONE"
    And work item with title "Blocked task" should have status "BLOCKED"

  Scenario: Add items in Backlog section to the backlog
    Given a markdown file "backlog.md" with the following content:
      """
      # Backlog
      - Backlog item 1
      - Backlog item 2
      """
    When the user runs "import backlog.md" command
    Then the command should succeed
    And 2 work items should be created
    And work item with title "Backlog item 1" should have status "CREATED"
    And work item with title "Backlog item 2" should have status "CREATED"
    And work items with titles "Backlog item 1, Backlog item 2" should be added to the backlog