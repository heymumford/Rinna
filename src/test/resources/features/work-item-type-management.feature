Feature: Work Item Type Management and Customization
  As an admin user of Rinna
  I want to customize work item types and their associated fields
  So that I can capture the right information for different work items

  Background:
    Given the Rinna server is running
    And I am authenticated as an admin user

  # ===== Work Item Type Management =====

  Scenario: View default work item types
    When I run "rin type list"
    Then I should see the default work item types
    And they should include at least "BUG", "FEATURE", and "CHORE"
    And each type should show its associated fields and workflow states

  Scenario: Create a new work item type
    When I run "rin type create --name EPIC --description 'Large feature that spans multiple releases'"
    Then the new work item type should be added to the system
    And I should see a success message
    And the new type should appear when listing work item types

  Scenario: Create a work item type with custom fields
    When I run "rin type create --name STORY --description 'User story' --field storyPoints:number --field acceptanceCriteria:text"
    Then the new work item type should be created with the custom fields
    And these fields should be available when creating items of type "STORY"
    And I should see a success message

  Scenario: Create a work item type with field constraints
    When I run "rin type create --name SECURITY_BUG --description 'Security vulnerability' --field severity:enum:low,medium,high,critical --field cve:string:required"
    Then the new work item type should be created with the constrained fields
    And the "severity" field should only accept the defined enum values
    And the "cve" field should be marked as required

  Scenario: Attempt to create a duplicate work item type
    Given the work item type "EPIC" already exists
    When I run "rin type create --name EPIC --description 'New description'"
    Then I should see an error message about duplicate type names
    And the existing type should remain unchanged

  Scenario: Update a work item type
    Given the work item type "STORY" exists
    When I run "rin type update --name STORY --new-name USER_STORY --description 'Updated description'"
    Then the work item type should be updated
    And I should see a success message
    And any work items of this type should reflect the updated type name

  Scenario: Add a field to an existing work item type
    Given the work item type "BUG" exists
    When I run "rin type field --type BUG --add browser:string 'Browser where bug occurs'"
    Then the field should be added to the work item type
    And the field should be available when creating or editing "BUG" items
    And I should see a success message

  Scenario: Remove a field from a work item type
    Given the work item type "FEATURE" exists with a field "marketValue"
    When I run "rin type field --type FEATURE --remove marketValue"
    Then the field should be removed from the work item type
    And I should see a warning about data loss
    And I should see a success message

  Scenario: Set default values for work item fields
    Given the work item type "TASK" exists with fields "priority" and "estimatedHours"
    When I run "rin type default --type TASK --field priority=MEDIUM --field estimatedHours=4"
    Then the default values should be set for the fields
    And new "TASK" items should be created with these default values
    And I should see a success message

  Scenario: Delete a work item type that is in use
    Given the work item type "CHORE" exists
    And there are work items of type "CHORE" in the system
    When I run "rin type delete --name CHORE"
    Then I should see an error message about the type being in use
    And the type should not be deleted

  Scenario: Delete a work item type that is not in use
    Given the work item type "SPIKE" exists
    And no work items of type "SPIKE" exist in the system
    When I run "rin type delete --name SPIKE"
    Then the type should be removed from the system
    And it should no longer appear when listing work item types

  Scenario: Force delete a work item type that is in use
    Given the work item type "TECHNICAL_DEBT" exists
    And there are work items of type "TECHNICAL_DEBT" in the system
    When I run "rin type delete --name TECHNICAL_DEBT --force --target-type TASK"
    Then the type should be removed from the system
    And all work items previously of type "TECHNICAL_DEBT" should be converted to "TASK"
    And I should see a warning about the forced type change

  # ===== Work Item Type Field Customization =====

  Scenario: Add a text field to a work item type
    Given the work item type "BUG" exists
    When I run "rin type field --type BUG --add reproduceSteps:text 'Steps to reproduce'"
    Then the text field should be added to the work item type
    And it should support multiline text input
    And I should see a success message

  Scenario: Add a number field to a work item type
    Given the work item type "STORY" exists
    When I run "rin type field --type STORY --add storyPoints:number 'Story point estimate' --range 1-13"
    Then the number field should be added with the specified range constraint
    And it should only accept numeric values between 1 and 13
    And I should see a success message

  Scenario: Add an enum field to a work item type
    Given the work item type "FEATURE" exists
    When I run "rin type field --type FEATURE --add impact:enum 'Business impact' --values low,medium,high"
    Then the enum field should be added with the specified values
    And it should only accept the defined enum values
    And I should see a success message

  Scenario: Add a date field to a work item type
    Given the work item type "RELEASE" exists
    When I run "rin type field --type RELEASE --add targetDate:date 'Target release date'"
    Then the date field should be added to the work item type
    And it should accept and validate date values
    And I should see a success message

  Scenario: Add a URL field to a work item type
    Given the work item type "DOCUMENTATION" exists
    When I run "rin type field --type DOCUMENTATION --add documentLink:url 'Link to document'"
    Then the URL field should be added to the work item type
    And it should validate that values are properly formatted URLs
    And I should see a success message

  Scenario: Add a user field to a work item type
    Given the work item type "REVIEW" exists
    When I run "rin type field --type REVIEW --add reviewer:user 'Person to review'"
    Then the user field should be added to the work item type
    And it should provide user selection from system users
    And I should see a success message

  Scenario: Add a multi-select field to a work item type
    Given the work item type "DEPLOYMENT" exists
    When I run "rin type field --type DEPLOYMENT --add environments:multi 'Target environments' --values dev,qa,staging,production"
    Then the multi-select field should be added to the work item type
    And it should allow selecting multiple values from the provided options
    And I should see a success message

  Scenario: Add a required field to a work item type
    Given the work item type "INCIDENT" exists
    When I run "rin type field --type INCIDENT --add severity:enum 'Severity level' --values low,medium,high,critical --required"
    Then the field should be added and marked as required
    And creation of INCIDENT items should fail if this field is not provided
    And I should see a success message

  # ===== Work Item Type Templates and Export/Import =====

  Scenario: Create a work item type template
    Given I have customized the "BUG" work item type
    When I run "rin type save-template --name 'Security Bug' --from BUG"
    Then the current type configuration should be saved as a template
    And I should see a success message with the template ID

  Scenario: List available work item type templates
    Given multiple work item type templates exist in the system
    When I run "rin type templates"
    Then I should see a list of all available type templates
    And each template should show a name, description, and field list

  Scenario: Apply a work item type template
    Given a work item type template "Detailed Bug" exists
    When I run "rin type apply-template --name 'Detailed Bug' --as 'REGRESSION_BUG'"
    Then a new work item type "REGRESSION_BUG" should be created from the template
    And it should include all fields and settings from the template
    And I should see a success message

  Scenario: Export work item type configuration
    Given I have customized several work item types
    When I run "rin type export --format json"
    Then I should receive a JSON file with the complete type configurations
    And the export should include all types, fields, and constraints

  Scenario: Import work item type configuration
    Given I have a work item type configuration file "custom-types.json"
    When I run "rin type import --file custom-types.json"
    Then the system work item types should be updated to match the imported configuration
    And I should see a summary of the changes made
    And I should be warned about any conflicts

  # ===== Work Item Relationships and Hierarchy =====

  Scenario: Define parent-child relationships between work item types
    When I run "rin type relate --parent EPIC --child STORY --name 'breaks down to'"
    And I run "rin type relate --parent STORY --child TASK --name 'implemented by'"
    Then the relationship types should be defined between the work items
    And I should be able to create hierarchical work item structures
    And I should see a success message

  Scenario: View work item type hierarchy
    Given I have defined relationships between work item types
    When I run "rin type hierarchy"
    Then I should see a visual representation of the work item type hierarchy
    And it should show all defined relationships
    And each relationship should be labeled with its name

  Scenario: Set automatic field inheritance between related types
    Given a parent-child relationship exists between "EPIC" and "STORY"
    When I run "rin type inherit --parent EPIC --child STORY --field team --field priority"
    Then the inheritance rules should be defined
    And when creating a STORY under an EPIC, these fields should be auto-populated
    And I should see a success message