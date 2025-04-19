Feature: Quality gates for development workflow
  As a developer or engineering manager
  I want to enforce quality standards at various stages of development
  So that we maintain code quality and avoid common issues

  Background:
    Given the Rinna system is initialized

  Scenario: Running local quality checks before status update
    Given a work item "WI-701" with title "Add notifications feature" in status "IN_PROGRESS"
    When I attempt to transition the work item to "IN_TEST" status
    Then the system should run local quality checks including:
      | Check Type        | Description                       |
      | Unit Tests        | All unit tests must pass          |
      | Code Coverage     | Minimum 80% coverage required     |
      | Static Analysis   | No critical or high issues        |
      | Documentation     | Javadoc coverage for public APIs  |
    And the transition should only succeed if all checks pass

  Scenario: Setting up project-specific quality gates
    Given I am configuring a project with the following quality gates:
      | Gate Name         | When to Apply                | Required Checks                    |
      | code-review       | Before TO_DO to IN_PROGRESS  | PullRequestCreated                 |
      | test-validation   | Before IN_PROGRESS to IN_TEST| UnitTestsPassing, IntegrationTestsPassing, CodeCoverageMinimum |
      | release-readiness | Before IN_TEST to DONE       | PerformanceTestsPassing, SecurityScanComplete, DocumentationUpdated |
    When I save the quality gate configuration
    Then the configuration should be stored successfully
    And the gates should be enforced at the appropriate workflow transitions

  Scenario: Blocking a transition when quality gates fail
    Given a work item "WI-702" with title "Optimize database queries" in status "IN_PROGRESS"
    And the local quality check "CodeCoverageMinimum" is failing
    When I attempt to transition the work item to "IN_TEST" status
    Then the transition should be blocked
    And the system should provide detailed information about the failing check
    And the work item should remain in "IN_PROGRESS" status

  Scenario: Quality gate with manual approval
    Given a work item "WI-703" with title "Add payment processing" in status "IN_TEST"
    And a quality gate "security-review" that requires manual approval
    When I attempt to transition the work item to "DONE" status
    Then the system should create an approval request
    And the transition should be pending until approved by an authorized reviewer
    And the work item status should show as "IN_TEST (Approval Pending)"

  Scenario: Quality gate bypass with proper authorization
    Given a work item "WI-704" with title "Emergency security fix" in status "IN_PROGRESS"
    And a failing quality gate check for test coverage
    When an authorized user bypasses the quality gate with reason "Critical security vulnerability"
    Then the transition should be allowed despite the failing check
    And the bypass should be recorded in the audit log with the provided reason
    And the work item should be flagged for post-deployment verification

  Scenario: CI/CD integration with quality gates
    Given a work item "WI-705" with title "Refactor authentication module" in status "IN_PROGRESS"
    And the CI system is integrated with Rinna quality gates
    When the CI build completes for the work item branch
    Then the quality gate results should be automatically updated
    And if all checks pass, the work item should be eligible for transition
    And if any check fails, the system should notify the assigned developer

  Scenario: Tracking technical debt through quality gates
    Given a work item "WI-706" with title "Add dashboard widgets" in status "IN_PROGRESS"
    And the code quality metrics show:
      | Metric            | Value  | Threshold | Status  |
      | Code Coverage     | 75%    | 80%       | Failing |
      | Code Duplication  | 4%     | 5%        | Passing |
      | Complexity        | Medium | High      | Passing |
      | Technical Debt    | 2d     | 3d        | Passing |
    When I request a technical debt exemption with reason "Will address in next iteration"
    Then the system should create a technical debt work item linked to "WI-706"
    And the original work item should be allowed to proceed
    And the technical debt item should be scheduled for the next development cycle

  Scenario: Viewing quality gate history for a work item
    Given a work item "WI-707" with title "Implement user preferences" that has passed through multiple statuses
    When I view the quality gate history for the work item
    Then I should see a complete record of:
      | Date       | Status Transition     | Quality Gates   | Result  | Details           |
      | 2025-03-15 | TO_DO → IN_PROGRESS   | code-review     | Passed  | PR #123 approved  |
      | 2025-03-20 | IN_PROGRESS → IN_TEST | test-validation | Failed  | Coverage 70% < 80%|
      | 2025-03-21 | IN_PROGRESS → IN_TEST | test-validation | Passed  | Coverage 85% > 80%|
      | 2025-03-25 | IN_TEST → DONE        | release-readiness | Passed | All checks passed |
    And each entry should include who performed or approved the checks

  Scenario: Executing quality gate checks from the CLI
    Given a work item "WI-708" with title "Optimize image loading" in status "IN_PROGRESS"
    When I run the command "rin quality-check WI-708 --pre-transition IN_TEST"
    Then the CLI should execute all required quality checks
    And display a detailed report of passing and failing checks
    And provide guidance on how to fix any failing checks