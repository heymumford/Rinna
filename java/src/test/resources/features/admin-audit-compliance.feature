@admin @audit @compliance
Feature: Admin Audit and Compliance Management
  As an administrator
  I want to configure and access audit logs and compliance reports
  So that I can maintain system integrity and meet regulatory requirements

  Background:
    Given I am logged in as an administrator
    And the audit logging system is enabled

  @smoke
  Scenario: View audit trail for user actions
    When I run the command "rin admin audit list --user=alice --days=7"
    Then the output should contain the following user actions:
      | Timestamp | User  | Action      | Target | Details        |
      | *         | alice | LOGIN       | system | Successful     |
      | *         | alice | CREATE      | WI-123 | Created task   |
      | *         | alice | UPDATE      | WI-123 | Changed status |
      | *         | alice | ASSIGNMENT  | WI-123 | Assigned to bob|

  Scenario: Configure audit log retention period
    When I run the command "rin admin audit configure --retention=90"
    Then the command should succeed
    And the audit log retention period should be set to 90 days
    When I run the command "rin admin audit status"
    Then the output should contain "Retention period: 90 days"

  Scenario: Export audit logs for compliance reporting
    When I run the command "rin admin audit export --from=2025-01-01 --to=2025-03-31 --format=csv"
    Then the command should succeed
    And the output should contain "Exported audit logs to audit_2025-01-01_2025-03-31.csv"
    And the exported file should contain all required compliance fields

  Scenario: Configure sensitive data masking in audit logs
    When I run the command "rin admin audit mask configure"
    Then I should be prompted to select fields to mask
    When I select "email,phone,address"
    Then the command should succeed
    And sensitive data masking should be enabled for the selected fields
    When I run the command "rin admin audit list --limit=5"
    Then the output should show masked data for sensitive fields

  @critical
  Scenario: Configure audit alerts for security events
    When I run the command "rin admin audit alert add"
    Then I should be prompted to enter an alert name
    When I enter "Failed Login Alert"
    Then I should be prompted to select event types
    When I select "FAILED_LOGIN"
    Then I should be prompted to enter a threshold count
    When I enter "5"
    Then I should be prompted to enter a time window in minutes
    When I enter "30"
    Then I should be prompted to enter notification recipients
    When I enter "security@example.com"
    Then the command should succeed
    And the audit alert "Failed Login Alert" should be created
    When I run the command "rin admin audit alert list"
    Then the output should contain "Failed Login Alert"

  Scenario: Generate compliance report for regulatory requirements
    When I run the command "rin admin compliance report --type=GDPR --period=Q1-2025"
    Then the command should succeed
    And the output should contain "Generated GDPR compliance report for Q1-2025"
    And the report should include the following sections:
      | Section                 | Status    |
      | Data Access Controls    | Compliant |
      | User Consent Management | Compliant |
      | Data Retention Policies | Compliant |
      | Security Measures       | Compliant |
      | Data Breach Procedures  | Compliant |

  Scenario: Configure compliance requirements for a project
    When I run the command "rin admin compliance configure --project=Alpha"
    Then I should be prompted to select applicable compliance frameworks
    When I select "GDPR,HIPAA,SOC2"
    Then I should be prompted to assign a compliance reviewer
    When I enter "carol"
    Then the command should succeed
    And project "Alpha" should be configured with the selected compliance frameworks
    And "carol" should be assigned as the compliance reviewer

  Scenario: Validate project against compliance requirements
    Given project "Beta" is configured with "PCI-DSS" compliance requirements
    When I run the command "rin admin compliance validate --project=Beta"
    Then the command should succeed
    And the validation report should identify compliance gaps:
      | Requirement                    | Status  | Remediation                          |
      | Strong password enforcement    | Pass    | N/A                                  |
      | Two-factor authentication      | Fail    | Enable MFA for all project members   |
      | Data encryption at rest        | Warning | Upgrade encryption to AES-256        |
      | Regular security scanning      | Pass    | N/A                                  |
      | Restricted administrative access | Pass    | N/A                                  |

  @critical
  Scenario: Handle a security incident with audit trail investigation
    Given a security incident is reported for user "mallory"
    When I run the command "rin admin audit investigation create --user=mallory --days=30"
    Then the command should succeed
    And an investigation case should be created
    When I run the command "rin admin audit investigation findings"
    Then the output should identify suspicious activities
    When I run the command "rin admin audit investigation actions --action=LOCK_ACCOUNT --user=mallory"
    Then the command should succeed
    And a compliance record should be created documenting the investigation and actions taken