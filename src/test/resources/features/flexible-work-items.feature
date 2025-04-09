Feature: Flexible work item management
  As a developer in various contexts
  I want to have flexible work items that adapt to different use cases
  So that I can efficiently track work in any development scenario

  Background:
    Given the Rinna system is initialized

  Scenario: Adding custom fields to a work item
    Given a new "FEATURE" work item with title "Payment gateway integration"
    When I add the following custom fields to the work item:
      | Field Name        | Field Value       | Field Type |
      | estimated_hours   | 16                | NUMBER     |
      | security_review   | true              | BOOLEAN    |
      | target_customers  | premium,enterprise| LIST       |
      | release_notes     | Added payment processing for Stripe and PayPal | TEXT |
    Then the work item should be created with all custom fields
    And retrieving the work item should include all custom field values

  Scenario: Searching work items by custom field values
    Given the following work items with custom fields exist:
      | ID      | Title                | Type     | Custom Fields                   |
      | WI-601  | Frontend redesign    | FEATURE  | team:frontend,platform:web      |
      | WI-602  | Backend optimization | TASK     | team:backend,criticality:high   |
      | WI-603  | Database migration   | TASK     | team:database,criticality:high  |
      | WI-604  | Mobile app fix       | BUG      | team:mobile,platform:ios,android|
    When I search for work items with custom field "team" equal to "backend"
    Then the results should contain only "WI-602"
    When I search for work items with custom field "criticality" equal to "high"
    Then the results should contain "WI-602" and "WI-603"
    When I search for work items with custom field "platform" containing "ios"
    Then the results should contain only "WI-604"

  Scenario: Defining project-specific templates for work items
    Given I create a template named "SecurityBug" with the following structure:
      | Field             | Default Value    | Required |
      | type              | BUG              | true     |
      | priority          | HIGH             | true     |
      | security_impact   | UNKNOWN          | true     |
      | cve_number        |                  | false    |
      | remediation_steps |                  | true     |
      | disclosure_date   |                  | false    |
    When I create a work item using the "SecurityBug" template with:
      | title             | SQL Injection vulnerability    |
      | security_impact   | HIGH                           |
      | remediation_steps | Sanitize user input            |
    Then a work item should be created with type "BUG" and priority "HIGH"
    And the work item should have custom field "security_impact" set to "HIGH"
    And the work item should have custom field "remediation_steps" set to "Sanitize user input"

  Scenario: Validation of required custom fields
    Given a template named "ComplianceTask" with required fields:
      | Field             | Type     |
      | compliance_area   | TEXT     |
      | deadline          | DATE     |
    When I attempt to create a work item using the "ComplianceTask" template without the "deadline" field
    Then the system should reject the creation with a validation error
    And the error should indicate that "deadline" is a required field

  Scenario: Importing work items from CSV with custom fields
    Given a CSV file with the following work items and custom fields:
      """
      id,title,type,status,priority,effort,area,team
      CSV-01,Import feature,FEATURE,TO_DO,MEDIUM,8,Core,Platform
      CSV-02,Fix CSV parsing,BUG,TO_DO,HIGH,3,Import,Data
      CSV-03,Improve performance,TASK,TO_DO,LOW,5,Performance,Platform
      """
    When I import the work items from the CSV file
    Then 3 work items should be created with their respective custom fields
    And the work item "CSV-01" should have custom field "effort" set to "8"
    And the work item "CSV-02" should have custom field "area" set to "Import"
    And the work item "CSV-03" should have custom field "team" set to "Platform"

  Scenario: Exporting work items with custom fields
    Given several work items with various custom fields exist
    When I export the work items to CSV format
    Then the CSV should include all standard fields and custom fields
    And the custom fields should be properly formatted in the export

  Scenario: Bulk updating custom fields across multiple work items
    Given several work items exist with different custom fields
    When I bulk update all work items with field "component" equal to "authentication" to set "security_review" to "required"
    Then all matching work items should be updated with the new custom field value
    And the system should report the number of items updated

  Scenario: Creating a complex work item with nested custom fields
    When I create a work item with nested custom fields:
      """
      {
        "title": "Refactor payment module",
        "type": "TASK",
        "priority": "MEDIUM",
        "custom_fields": {
          "components": ["billing", "payment-gateway"],
          "testing": {
            "unit_tests": true,
            "integration_tests": true,
            "e2e_tests": false
          },
          "performance_metrics": {
            "baseline_time_ms": 250,
            "target_time_ms": 100
          }
        }
      }
      """
    Then the work item should be created with the complex nested structure
    And querying the work item should return the complete custom field hierarchy