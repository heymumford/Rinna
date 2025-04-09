@ruwi @core
Feature: Rinna Universal Work Item (RUWI) Management
  As a user with different roles in the software development process
  I want to manage my work using a consistent work item paradigm
  So that everyone can track, prioritize, and report on their work in the same way

  Background:
    Given the Rinna system is initialized
    And the following categories exist:
      | Name               | Code   | Description                                    |
      | Product            | PROD   | Product requirements and features              |
      | Architecture       | ARCH   | Architecture designs and decisions             |
      | Development        | DEV    | Development tasks and implementation           |
      | Testing            | TEST   | Testing activities and quality assurance       |
      | Operations         | OPS    | Deployment and operational concerns            |
      | Documentation      | DOC    | Documentation tasks                            |
      | Cross-Functional   | CROSS  | Tasks involving multiple disciplines           |
    And the following work item types exist:
      | Name          | Category       | Icon   |
      | Epic          | PROD           | 🏔️     |
      | Feature       | PROD           | ✨     |
      | User Story    | PROD           | 👤     |
      | Requirement   | PROD           | 📋     |
      | Design        | ARCH           | 📐     |
      | Decision      | ARCH           | 🧩     |
      | Task          | DEV            | 🔨     |
      | Bug           | DEV            | 🐞     |
      | Test Case     | TEST           | ✓      |
      | Test Suite    | TEST           | 📊     |
      | Deployment    | OPS            | 🚀     |
      | Documentation | DOC            | 📚     |
      | Spike         | CROSS          | ⚡     |

  @smoke @product-owner
  Scenario: Product Owner creates a feature requirement
    Given I am logged in as a "Product Owner"
    When I run the command "rin add feature --title='User authentication system' --description='Allow users to register and log in' --priority=HIGH --category=PROD"
    Then the command should succeed
    And the output should contain a work item ID
    When I run the command "rin view --id={last-id}"
    Then the output should contain "User authentication system"
    And the output should contain "Type: Feature"
    And the output should contain "Category: PROD"
    And the output should contain "Priority: HIGH"
    And the output should contain "State: BACKLOG"

  @product-owner
  Scenario: Product Owner breaks down a feature into user stories
    Given I am logged in as a "Product Owner"
    And a feature with ID "FEAT-123" and title "User authentication system" exists
    When I run the command "rin add story --title='User registration' --description='As a new user, I want to register an account' --parent=FEAT-123 --priority=HIGH --category=PROD"
    Then the command should succeed
    And the output should contain a work item ID
    When I run the command "rin add story --title='User login' --description='As a registered user, I want to log in' --parent=FEAT-123 --priority=HIGH --category=PROD"
    Then the command should succeed
    And the output should contain a work item ID
    When I run the command "rin list --parent=FEAT-123"
    Then the output should contain "User registration"
    And the output should contain "User login"
    And the output should contain "PROD" for each item

  @product-owner
  Scenario: Product Owner manages product backlog
    Given I am logged in as a "Product Owner"
    And the following work items exist:
      | ID        | Type    | Title               | Priority | Category | State    |
      | FEAT-101  | Feature | Payment processing  | HIGH     | PROD     | BACKLOG  |
      | FEAT-102  | Feature | User profiles       | MEDIUM   | PROD     | BACKLOG  |
      | FEAT-103  | Feature | Search functionality| LOW      | PROD     | BACKLOG  |
    When I run the command "rin backlog --category=PROD"
    Then the output should contain "Payment processing" before "User profiles"
    And the output should contain "User profiles" before "Search functionality"
    When I run the command "rin update --id=FEAT-102 --priority=HIGH"
    And I run the command "rin backlog --category=PROD"
    Then the output should contain "Payment processing"
    And the output should contain "User profiles"
    And they should have the same priority level

  @architect
  Scenario: Architect creates architecture design work items
    Given I am logged in as an "Architect"
    When I run the command "rin add design --title='Authentication service design' --description='Design the authentication microservice' --priority=HIGH --category=ARCH"
    Then the command should succeed
    And the output should contain a work item ID
    When I run the command "rin view --id={last-id}"
    Then the output should contain "Authentication service design"
    And the output should contain "Type: Design"
    And the output should contain "Category: ARCH"

  @architect
  Scenario: Architect links architectural decisions to requirements
    Given I am logged in as an "Architect"
    And a requirement with ID "REQ-123" and title "User authentication" exists
    When I run the command "rin add decision --title='OAuth implementation' --description='Use OAuth 2.0 for authentication' --priority=HIGH --category=ARCH --linked=REQ-123"
    Then the command should succeed
    When I run the command "rin view --id={last-id}"
    Then the output should contain "OAuth implementation"
    And the output should contain "Linked to: REQ-123"

  @architect
  Scenario: Architect tracks architecture work plan
    Given I am logged in as an "Architect"
    And the following work items exist:
      | ID        | Type     | Title                      | Priority | Category | State    |
      | ARCH-101  | Design   | API Gateway design         | HIGH     | ARCH     | BACKLOG  |
      | ARCH-102  | Design   | Database schema design     | HIGH     | ARCH     | BACKLOG  |
      | ARCH-103  | Decision | Authentication technology  | MEDIUM   | ARCH     | BACKLOG  |
    When I run the command "rin list --category=ARCH --timeframe=week"
    Then the output should contain "API Gateway design"
    And the output should contain "Database schema design" 
    And the output should contain "Authentication technology"
    When I run the command "rin update --id=ARCH-101 --state=IN_PROGRESS --assignee=current"
    And I run the command "rin list --category=ARCH --state=IN_PROGRESS"
    Then the output should contain "API Gateway design"
    And the output should not contain "Database schema design"

  @developer
  Scenario: Developer creates implementation tasks
    Given I am logged in as a "Developer"
    And a user story with ID "STORY-123" and title "User registration" exists
    When I run the command "rin add task --title='Implement registration form' --description='Create the frontend registration form' --priority=HIGH --category=DEV --parent=STORY-123"
    Then the command should succeed
    And the output should contain a work item ID
    When I run the command "rin add task --title='Create user database table' --description='Design and implement user table' --priority=HIGH --category=DEV --parent=STORY-123"
    Then the command should succeed
    When I run the command "rin list --parent=STORY-123"
    Then the output should contain "Implement registration form"
    And the output should contain "Create user database table"

  @developer
  Scenario: Developer reports on development progress
    Given I am logged in as a "Developer"
    And the following work items exist:
      | ID      | Type | Title                      | Priority | Category | State        | Assignee        |
      | TASK-101 | Task | Implement login page      | HIGH     | DEV      | DONE         | current_user    |
      | TASK-102 | Task | API endpoints for auth    | HIGH     | DEV      | IN_PROGRESS  | current_user    |
      | TASK-103 | Task | Password reset function   | MEDIUM   | DEV      | BACKLOG      | current_user    |
      | TASK-104 | Task | User session management   | LOW      | DEV      | BACKLOG      | current_user    |
    When I run the command "rin report progress --assignee=current --category=DEV"
    Then the output should contain "Completed: 1"
    And the output should contain "In Progress: 1"
    And the output should contain "Not Started: 2"
    And the output should contain "Completion rate: 25%"

  @developer
  Scenario: Developer tracks work for a sprint
    Given I am logged in as a "Developer"
    And a sprint with ID "SPRINT-7" exists
    And the following work items exist:
      | ID      | Type | Title                     | Priority | Category | Sprint    | State        |
      | TASK-201 | Task | User avatar upload       | HIGH     | DEV      | SPRINT-7  | BACKLOG      |
      | TASK-202 | Task | Profile edit form        | MEDIUM   | DEV      | SPRINT-7  | BACKLOG      |
      | BUG-101  | Bug  | Login error message      | HIGH     | DEV      | SPRINT-7  | BACKLOG      |
    When I run the command "rin list --sprint=SPRINT-7 --assignee=current"
    Then the output should contain "User avatar upload"
    And the output should contain "Profile edit form"
    And the output should contain "Login error message"
    When I run the command "rin update --id=TASK-201 --state=IN_PROGRESS"
    And I run the command "rin list --sprint=SPRINT-7 --state=IN_PROGRESS"
    Then the output should contain "User avatar upload"
    And the output should not contain "Profile edit form"

  @tester
  Scenario: Tester creates test cases
    Given I am logged in as a "Tester"
    And a user story with ID "STORY-123" and title "User registration" exists
    When I run the command "rin add testcase --title='Valid registration test' --description='Test registration with valid inputs' --priority=HIGH --category=TEST --linked=STORY-123"
    Then the command should succeed
    And the output should contain a work item ID
    When I run the command "rin add testcase --title='Invalid email test' --description='Test registration with invalid email' --priority=MEDIUM --category=TEST --linked=STORY-123"
    Then the command should succeed
    When I run the command "rin list --type=testcase --linked=STORY-123"
    Then the output should contain "Valid registration test"
    And the output should contain "Invalid email test"

  @tester
  Scenario: Tester organizes test cases into test suites
    Given I am logged in as a "Tester"
    And the following work items exist:
      | ID          | Type      | Title                     | Category | State    |
      | TEST-101    | Test Case | Valid login test          | TEST     | BACKLOG  |
      | TEST-102    | Test Case | Invalid login test        | TEST     | BACKLOG  |
      | TEST-103    | Test Case | Password validation test  | TEST     | BACKLOG  |
    When I run the command "rin add testsuite --title='Authentication Test Suite' --description='Tests for authentication features' --category=TEST"
    Then the command should succeed
    And the output should contain a work item ID
    When I run the command "rin update --id=TEST-101 --parent={last-id}"
    And I run the command "rin update --id=TEST-102 --parent={last-id}"
    And I run the command "rin update --id=TEST-103 --parent={last-id}"
    And I run the command "rin list --parent={last-id}"
    Then the output should contain "Valid login test"
    And the output should contain "Invalid login test"
    And the output should contain "Password validation test"

  @tester
  Scenario: Tester tracks test plan and execution
    Given I am logged in as a "Tester"
    And the following work items exist:
      | ID          | Type      | Title                       | Category | State     |
      | SUITE-101   | Test Suite| Registration Test Suite     | TEST     | BACKLOG   |
      | SUITE-102   | Test Suite| Login Test Suite            | TEST     | BACKLOG   |
      | SUITE-103   | Test Suite| Admin Features Test Suite   | TEST     | BACKLOG   |
    When I run the command "rin list --type=testsuite --category=TEST"
    Then the output should contain "Registration Test Suite"
    And the output should contain "Login Test Suite"
    And the output should contain "Admin Features Test Suite"
    When I run the command "rin update --id=SUITE-101 --state=IN_PROGRESS"
    And I run the command "rin list --type=testsuite --state=IN_PROGRESS"
    Then the output should contain "Registration Test Suite"
    And the output should not contain "Login Test Suite"

  @cross-role @product-owner @architect @developer @tester
  Scenario: All roles view the same work item through different lenses
    Given the following work item exists:
      | ID      | Type     | Title             | Category | State     |
      | FEAT-123 | Feature | User authentication | PROD    | BACKLOG   |
    And the following related work items exist:
      | ID         | Type       | Title                       | Category | Parent/Linked |
      | STORY-101  | User Story | User registration           | PROD     | FEAT-123      |
      | ARCH-101   | Design     | Authentication flow design  | ARCH     | FEAT-123      |
      | TASK-101   | Task       | Implement login form        | DEV      | STORY-101     |
      | TEST-101   | Test Case  | Valid credentials test      | TEST     | STORY-101     |
    When I am logged in as a "Product Owner"
    And I run the command "rin view --id=FEAT-123 --format=detail"
    Then the output should contain "User authentication"
    And the output should contain "User registration"
    When I am logged in as an "Architect"
    And I run the command "rin view --id=ARCH-101 --linked"
    Then the output should contain "Authentication flow design"
    And the output should contain "Linked to feature: User authentication"
    When I am logged in as a "Developer"
    And I run the command "rin view --id=TASK-101 --trace"
    Then the output should contain "Implement login form"
    And the output should show the trace "FEAT-123 > STORY-101 > TASK-101"
    When I am logged in as a "Tester"
    And I run the command "rin view --id=TEST-101 --trace"
    Then the output should contain "Valid credentials test"
    And the output should show the trace "FEAT-123 > STORY-101 > TEST-101"

  @negative @product-owner
  Scenario: Product Owner attempts to create a work item with an invalid category
    Given I am logged in as a "Product Owner"
    When I run the command "rin add feature --title='Invalid category feature' --description='This has an invalid category' --priority=HIGH --category=INVALID"
    Then the command should fail
    And the output should contain "Error: Invalid category 'INVALID'"
    And the output should contain "Valid categories are: PROD, ARCH, DEV, TEST, OPS, DOC, CROSS"

  @negative @developer
  Scenario: Developer attempts to update a work item they don't own
    Given I am logged in as a "Developer"
    And a work item with ID "ARCH-101" and title "Authentication design" exists with owner "architect_user"
    When I run the command "rin update --id=ARCH-101 --state=IN_PROGRESS"
    Then the command should fail
    And the output should contain "Error: You don't have permission to update this work item"

  @negative @tester
  Scenario: Tester attempts to change work item type
    Given I am logged in as a "Tester"
    And a work item with ID "TEST-101" and type "Test Case" exists
    When I run the command "rin update --id=TEST-101 --type=Bug"
    Then the command should fail
    And the output should contain "Error: Changing work item type is not allowed"

  @negative @architect
  Scenario: Architect attempts to create a work item with missing required fields
    Given I am logged in as an "Architect"
    When I run the command "rin add design --title='Missing fields design'"
    Then the command should fail
    And the output should contain "Error: Missing required fields"
    And the output should contain "Required fields for design: title, category, priority"

  @reporting @product-owner
  Scenario: Product Owner generates report on feature progress
    Given I am logged in as a "Product Owner"
    And the following work items exist:
      | ID        | Type    | Title               | State       | Category |
      | FEAT-101  | Feature | User management     | IN_PROGRESS | PROD     |
      | STORY-101 | Story   | User creation       | DONE        | PROD     |
      | STORY-102 | Story   | User editing        | IN_PROGRESS | PROD     |
      | STORY-103 | Story   | User deletion       | BACKLOG     | PROD     |
    When I run the command "rin report feature-progress --id=FEAT-101"
    Then the output should contain "Feature: User management"
    And the output should contain "Progress: 33%"
    And the output should contain "Stories completed: 1/3"
    And the output should contain "User creation: DONE"
    And the output should contain "User editing: IN_PROGRESS"
    And the output should contain "User deletion: BACKLOG"

  @reporting @tester
  Scenario: Tester generates test coverage report
    Given I am logged in as a "Tester"
    And the following work items exist:
      | ID        | Type      | Title                  | State   | Category | Linked   |
      | STORY-101 | Story     | User registration      | DONE    | PROD     |          |
      | TEST-101  | Test Case | Valid registration     | DONE    | TEST     | STORY-101|
      | TEST-102  | Test Case | Invalid email          | DONE    | TEST     | STORY-101|
      | STORY-102 | Story     | User login             | DONE    | PROD     |          |
      | TEST-103  | Test Case | Valid login            | DONE    | TEST     | STORY-102|
      | STORY-103 | Story     | Password reset         | DONE    | PROD     |          |
    When I run the command "rin report test-coverage"
    Then the output should contain "Test Coverage Report"
    And the output should contain "Story: User registration - Coverage: 100%"
    And the output should contain "Story: User login - Coverage: 100%"
    And the output should contain "Story: Password reset - Coverage: 0%"
    And the output should contain "Overall test coverage: 67%"
    And the output should contain "Stories without tests: 1"

  @reporting @architect
  Scenario: Architect generates architecture decision report
    Given I am logged in as an "Architect"
    And the following work items exist:
      | ID        | Type     | Title                      | State   | Category |
      | ARCH-101  | Decision | Authentication protocol    | DONE    | ARCH     |
      | ARCH-102  | Decision | Database technology        | DONE    | ARCH     |
      | ARCH-103  | Decision | API design                 | DONE    | ARCH     |
      | ARCH-104  | Decision | Deployment architecture    | BACKLOG | ARCH     |
    When I run the command "rin report architecture-decisions"
    Then the output should contain "Architecture Decisions Report"
    And the output should contain "Authentication protocol: DONE"
    And the output should contain "Database technology: DONE"
    And the output should contain "API design: DONE"
    And the output should contain "Deployment architecture: BACKLOG"
    And the output should contain "Decisions made: 3/4 (75%)"