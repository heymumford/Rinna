Feature: Admin Compliance Commands
  As a system administrator
  I want to manage regulatory compliance frameworks and requirements
  So that I can ensure system security and compliance with regulations

  Background:
    Given I am logged in as an administrator

  Scenario: Generate compliance report
    When I run "rin admin compliance report --type=GDPR --period=current"
    Then the command should succeed
    And the output should contain "Compliance Report: GDPR"
    And the output should contain "Reporting Period: current"
    And the output should contain "Overall Compliance:"

  Scenario: Configure project compliance
    When I run "rin admin compliance configure --project=test-project" with input:
      """
      1,2
      compliance-reviewer
      """
    Then the command should succeed
    And the output should contain "Compliance configuration updated successfully for project: test-project"
    And the output should contain "Frameworks: GDPR, HIPAA"
    And the output should contain "Reviewer: compliance-reviewer"

  Scenario: Configure project compliance using framework names
    When I run "rin admin compliance configure --project=test-project2" with input:
      """
      GDPR,PCI-DSS
      security-team
      """
    Then the command should succeed
    And the output should contain "Compliance configuration updated successfully for project: test-project2"
    And the output should contain "Frameworks: GDPR, PCI-DSS"
    And the output should contain "Reviewer: security-team"

  Scenario: Configure project compliance without providing a reviewer
    When I run "rin admin compliance configure --project=test-project3" with input:
      """
      ISO27001

      """
    Then the command should fail
    And the output should contain "Error: Compliance reviewer cannot be empty"

  Scenario: Configure project compliance without providing project name
    When I run "rin admin compliance configure"
    Then the command should fail
    And the output should contain "Error: Missing required parameter --project"

  Scenario: Validate project compliance
    Given a project "test-project" is configured for compliance
    When I run "rin admin compliance validate --project=test-project"
    Then the command should succeed
    And the output should contain "Project Compliance Validation: test-project"
    And the output should contain "Validation Results:"
    And the output should contain "Total checks performed:"

  Scenario: Validate non-existent project compliance
    When I run "rin admin compliance validate --project=non-existent-project"
    Then the command should succeed
    And the output should contain "Project non-existent-project has not been configured for compliance"

  Scenario: Validate project compliance without providing project name
    When I run "rin admin compliance validate"
    Then the command should fail
    And the output should contain "Error: Missing required parameter --project"

  Scenario: View project compliance status
    Given a project "test-project" is configured for compliance
    When I run "rin admin compliance status --project=test-project"
    Then the command should succeed
    And the output should contain "Project Compliance Status: test-project"
    And the output should contain "Issues Summary:"
    And the output should contain "Compliance Status:"

  Scenario: View system-wide compliance status
    When I run "rin admin compliance status"
    Then the command should succeed
    And the output should contain "System Compliance Status"
    And the output should contain "Default Framework:"
    And the output should contain "System Compliance Status:"

  Scenario: Show help for compliance commands
    When I run "rin admin compliance help"
    Then the command should succeed
    And the output should contain "Usage: rin admin compliance <operation> [options]"
    And the output should contain "Operations:"
    And the output should contain "report     - Generate compliance reports"

  Scenario: Admin privileges check
    Given I am logged in as a regular user
    When I run "rin admin compliance status"
    Then the command should fail
    And the output should contain "Error: Administrative privileges required to run this command"

  Scenario: Unknown compliance operation
    When I run "rin admin compliance unknown-operation"
    Then the command should fail
    And the output should contain "Error: Unknown compliance operation: unknown-operation"