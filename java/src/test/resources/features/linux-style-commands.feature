Feature: Linux-Style Command Shortcuts
  As a Linux power user
  I want to use familiar Linux-style commands in Rinna
  So that I can work more efficiently and intuitively

  Background:
    Given the Rinna system is initialized
    And I am logged in as a developer
    And I have the following work items:
      | ID | Title                    | State       | Priority | Assignee |
      | 5  | Implement login screen   | IN_PROGRESS | MEDIUM   | bob      |
      | 6  | Create user registration | READY       | LOW      | alice    |
      | 7  | Design password reset    | BACKLOG     | HIGH     | bob      |
    And work item 6 is a child of work item 5

  @positive @linux-style
  Scenario: Basic list command shows work items with inheritance
    When I run "rin ls"
    Then the command should succeed
    And I should see a summary listing of all work items with their titles
    And I should see inheritance information for parent-child relationships
    And I should see work item 5 as a parent of work item 6

  @positive @linux-style
  Scenario: Long list command shows detailed work item information
    When I run "rin ls -l"
    Then the command should succeed
    And I should see a detailed listing of all work items
    And each work item should display all defined fields
    And the system should record work item 5 as the last viewed item

  @positive @linux-style
  Scenario: Super long list command shows detailed info with history
    When I run "rin ls -al"
    Then the command should succeed
    And I should see a detailed listing of all work items
    And each work item should display all defined fields
    And each work item should display its complete history and changelog
    And the system should record work item 5 as the last viewed item

  @positive @linux-style
  Scenario: List a specific work item
    When I run "rin ls 6"
    Then the command should succeed
    And I should see a summary of work item 6
    And I should see that work item 6 is a child of work item 5
    And the system should record work item 6 as the last viewed item

  @positive @linux-style
  Scenario: Long list a specific work item
    When I run "rin ls -l 7"
    Then the command should succeed
    And I should see a detailed view of work item 7
    And I should see all defined fields for work item 7
    And the system should record work item 7 as the last viewed item

  @positive @linux-style
  Scenario: Super long list a specific work item
    When I run "rin ls -al 6"
    Then the command should succeed
    And I should see a detailed view of work item 6
    And I should see all defined fields for work item 6
    And I should see the complete history of work item 6
    And the system should record work item 6 as the last viewed item

  @positive @linux-style
  Scenario: Edit command uses last viewed work item
    Given I have viewed work item 7 using "rin ls 7"
    When I run "rin edit"
    Then the command should succeed
    And I should see an interactive editor for work item 7
    And I should be able to edit the fields of work item 7

  @positive @linux-style
  Scenario: Edit command with explicit ID
    When I run "rin edit id=6"
    Then the command should succeed
    And I should see an interactive editor for work item 6
    And I should be able to edit the fields of work item 6
    
  @positive @linux-style
  Scenario: Explicit ID parameter overrides last viewed
    Given I have viewed work item 7 using "rin ls 7"
    When I run "rin edit id=5"
    Then the command should succeed
    And I should see an interactive editor for work item 5
    And I should be able to edit the fields of work item 5

  @negative @linux-style
  Scenario: Edit command fails when no item has been viewed
    Given no work item has been viewed yet
    When I run "rin edit"
    Then the command should fail
    And I should see an error message "No work item context available. Please specify an ID with id=X"

  @negative @linux-style
  Scenario: Invalid work item ID in ls command
    When I run "rin ls 99"
    Then the command should fail
    And I should see an error message "Work item not found: 99"

  @negative @linux-style
  Scenario: Invalid parameter in ls command
    When I run "rin ls -z"
    Then the command should fail
    And I should see an error message "Unknown option: -z"

  @negative @linux-style @security
  Scenario: Attempt command injection in ls
    When I run "rin ls; echo HACKED"
    Then the command should fail
    And I should see an error message "Invalid work item ID"
    And the command injection should not succeed

  @negative @linux-style @security
  Scenario: Attempt command injection in edit
    When I run "rin edit id=5; rm -rf /"
    Then the command should fail
    And I should see an error message "Invalid work item ID format"
    And the command injection should not succeed
    
  # Grep Command scenarios
  @positive @unix @grep
  Scenario: Basic grep search finds matching work items
    Given I have the following work items:
      | ID | Title                    | Description                       | State       | Priority | Assignee |
      | 1  | API authentication task  | Implement the API auth flow       | IN_PROGRESS | HIGH     | bob      |
      | 2  | UI development          | Create user interface components  | READY       | MEDIUM   | alice    |
      | 3  | API documentation       | Document all API endpoints        | BACKLOG     | LOW      | charlie  |
    When I run "rin grep API"
    Then the command should succeed
    And I should see work item 1 in the results
    And I should see work item 3 in the results
    And I should not see work item 2 in the results
  
  @positive @unix @grep
  Scenario: Case-insensitive search
    When I run "rin grep -i api"
    Then the command should succeed
    And I should see work item 1 in the results
    And I should see work item 3 in the results
    And both "API" and "api" matches should be highlighted
  
  @positive @unix @grep
  Scenario: Case-sensitive search
    When I run "rin grep -s api"
    Then the command should succeed
    And I should not see work item 1 in the results
    And I should not see work item 3 in the results
  
  @positive @unix @grep
  Scenario: Count-only search
    When I run "rin grep -c API"
    Then the command should succeed
    And I should see total matches 2
    
  @positive @unix @grep
  Scenario: Exact match search
    Given I have the following work items:
      | ID | Title                 | Description                          | State       | Priority | Assignee |
      | 4  | APIs overview         | Testing APIs and their integration   | IN_PROGRESS | MEDIUM   | dave     |
    When I run "rin grep -w API"
    Then the command should succeed
    And I should see work item 1 in the results
    And I should see work item 3 in the results
    And I should not see work item 4 in the results

  @positive @unix @grep
  Scenario: Context search shows surrounding lines
    When I run "rin grep -A 2 -B 2 API" 
    Then the command should succeed
    And I should see lines including "Implement the API auth flow"
    And I should see lines including "Document all API endpoints"

  @negative @unix @grep
  Scenario: Empty pattern should fail
    When I run "rin grep"
    Then the command should fail
    And I should see an error message "Empty search pattern"

  @negative @unix @grep @security
  Scenario: Command injection attempt in grep
    When I run "rin grep 'API; cat /etc/passwd'"
    Then the command should succeed
    And I should see work item 1 in the results
    And I should see work item 3 in the results
    And the command injection should not succeed

  @positive @unix @grep
  Scenario: Search history is maintained
    When I run "rin grep API"
    And I run "rin grep documentation"
    And I run "rin grep --history"
    Then the command should succeed
    And I should see my search history including "API"
    And I should see my search history including "documentation"