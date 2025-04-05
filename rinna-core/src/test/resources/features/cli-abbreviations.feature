Feature: CLI Command Abbreviations
  As a developer
  I want to use abbreviated commands in the Rinna CLI
  So that I can be more productive with fewer keystrokes

  Background:
    Given the Rinna system is running locally
    And the CLI tool is configured with the API endpoint "http://localhost:9080/api/v1"
    And a valid API token "ri-dev-token" is configured for the CLI
    And the following work items exist:
      | ID    | Type  | Title                      | Description                                       | Status    | Priority |
      | RIN-01 | TASK  | Update API documentation  | The API docs need to be updated with new endpoints| TO_DO     | MEDIUM   |
      | RIN-02 | BUG   | Login fails on Safari     | Users cannot login using Safari browsers          | IN_PROGRESS| HIGH     |
      | RIN-03 | TASK  | Refactor database layer   | Improve performance by optimizing queries         | TO_DO     | LOW      |
      | RIN-04 | FEATURE | Add export functionality | Allow users to export data in multiple formats    | TO_DO     | MEDIUM   |
      | RIN-05 | CHORE | Update dependencies       | Update NPM packages to latest versions            | TO_DO     | LOW      |

  @cli @abbreviations @list @smoke
  Scenario Outline: List tasks using equivalent abbreviated commands
    When the developer runs "<command>"
    Then the output should contain "RIN-01" and "Update API documentation"
    And the output should contain "RIN-03" and "Refactor database layer"
    And the output should not contain "RIN-02" or "RIN-04" or "RIN-05"
    And each line should contain a title and no more than 40 characters of the description

    Examples:
      | command         |
      | rin l t         |
      | rin list tasks  |
      | rin l tasks     |
      | rin list t      |
      | rin --list --tasks |
      | rin l --type=TASK |

  @cli @abbreviations @list @smoke
  Scenario: List all work items with default format
    When the developer runs "rin l"
    Then the output should contain all work items
    And each item should be displayed in the format "ID    TITLE    DESCRIPTION(40)..."

  @cli @abbreviations @list
  Scenario Outline: List items by type using various abbreviations
    When the developer runs "<command>"
    Then the output should only contain items of type "<type>"
    And the output should contain "<id>" and "<title>"

    Examples:
      | command | type    | id      | title                   |
      | rin l b | BUG     | RIN-02  | Login fails on Safari   |
      | rin l f | FEATURE | RIN-04  | Add export functionality|
      | rin l c | CHORE   | RIN-05  | Update dependencies     |

  @cli @abbreviations @list
  Scenario: List with custom output format
    When the developer runs "rin l --format=id,title,status"
    Then each line should only contain ID, title, and status columns
    And column headers should be displayed

  @cli @abbreviations @list
  Scenario Outline: List by status using abbreviations
    When the developer runs "<command>"
    Then the output should only contain items with status "<full_status>"
    And the output should contain "<expected_id>"

    Examples:
      | command              | full_status  | expected_id |
      | rin l --status=in    | IN_PROGRESS  | RIN-02      |
      | rin l -s in          | IN_PROGRESS  | RIN-02      |
      | rin l --status=todo  | TO_DO        | RIN-01      |
      | rin l -s todo        | TO_DO        | RIN-01      |

  @cli @abbreviations @list
  Scenario Outline: List with multiple filters using abbreviations
    When the developer runs "<command>"
    Then the output should only contain items that match all filters
    And the output should contain "<expected_id>"
    And the output should not contain "<excluded_id>"

    Examples:
      | command                         | expected_id | excluded_id |
      | rin l -t TASK -s todo           | RIN-01      | RIN-02      |
      | rin l --type=TASK --priority=l  | RIN-03      | RIN-01      |
      | rin l t -p m                    | RIN-01      | RIN-03      |

  @cli @abbreviations @create @smoke
  Scenario Outline: Create a new work item with abbreviated commands
    When the developer runs "<command>"
    Then a work item should be created with:
      | title      | New documentation task |
      | type       | TASK                   |
      | status     | FOUND                  |
    And the CLI should display the new work item ID and details

    Examples:
      | command                                             |
      | rin a "New documentation task"                      |
      | rin add "New documentation task"                    |
      | rin c "New documentation task" --type=t             |
      | rin create "New documentation task" --type=TASK     |
      | rin n "New documentation task" -t TASK              |
      | rin new "New documentation task" -t TASK            |

  @cli @abbreviations @view @smoke
  Scenario Outline: View a work item with abbreviated commands
    When the developer runs "<command>"
    Then the output should show the full details of work item "RIN-01"
    And the output should include:
      | Field       | Value                      |
      | ID          | RIN-01                     |
      | Title       | Update API documentation   |
      | Type        | TASK                       |
      | Description | The API docs need to be updated with new endpoints |
      | Status      | TO_DO                      |
      | Priority    | MEDIUM                     |

    Examples:
      | command        |
      | rin v RIN-01   |
      | rin view RIN-01|
      | rin s RIN-01   |
      | rin show RIN-01|
      | rin g RIN-01   |
      | rin get RIN-01 |

  @cli @abbreviations @update @smoke
  Scenario Outline: Update a work item status with abbreviated commands
    When the developer runs "<command>"
    Then the work item "RIN-01" should be updated with status "IN_PROGRESS"
    And the CLI should display the updated work item details

    Examples:
      | command                              |
      | rin u RIN-01 -s in                   |
      | rin update RIN-01 --status=in        |
      | rin m RIN-01 -s IN_PROGRESS          |
      | rin mod RIN-01 --status=IN_PROGRESS  |

  @cli @abbreviations @update
  Scenario Outline: Update a work item with multiple properties using abbreviations
    When the developer runs "<command>"
    Then the work item "RIN-03" should be updated with:
      | status      | IN_PROGRESS |
      | priority    | HIGH        |
      | assignee    | developer1  |
    And the CLI should display the updated work item details

    Examples:
      | command                                                        |
      | rin u RIN-03 -s in -p h -a developer1                          |
      | rin update RIN-03 --status=in --priority=HIGH --assignee=developer1 |

  @cli @abbreviations @negative
  Scenario Outline: Invalid abbreviations should display helpful error messages
    When the developer runs "<command>"
    Then the CLI should display a helpful error message
    And the error message should suggest valid alternatives
    And the CLI should indicate error with non-zero status code

    Examples:
      | command                           |
      | rin ls                            |
      | rin lst                           |
      | rin listings                      |
      | rin views RIN-01                  |
      | rin upd RIN-01 -s in              |

  @cli @abbreviations @help
  Scenario: Help command shows abbreviation documentation
    When the developer runs "rin help"
    Then the output should contain a section for command abbreviations
    And the abbreviation section should list all supported short forms
    And each abbreviation should show the equivalent full command

  @cli @abbreviations @config
  Scenario: User can configure custom aliases in config file
    Given the user has a config file with custom aliases:
      """
      aliases:
        tasks: "list --type=TASK"
        bugs: "list --type=BUG --priority=HIGH"
        mine: "list --assignee=current-user"
      """
    When the developer runs "rin tasks"
    Then the output should contain "RIN-01" and "Update API documentation"
    And the output should contain "RIN-03" and "Refactor database layer"
    And the output should not contain "RIN-02" or "RIN-04" or "RIN-05"