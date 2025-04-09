Feature: Workflow Customization and Management
  As an admin user of Rinna
  I want to customize workflow states and transitions
  So that I can adapt the system to my team's specific processes

  Background:
    Given the Rinna server is running
    And I am authenticated as an admin user

  # ===== Workflow State Management =====

  Scenario: View default workflow states
    When I run "rin workflow states"
    Then I should see the default workflow states
    And they should include at least "FOUND", "TRIAGED", "IN_DEV", "TESTING", "DONE"
    And the system should indicate which states are start and end states

  Scenario: Add a new workflow state
    When I run "rin workflow add-state --name CODE_REVIEW --description 'Code is being reviewed'"
    Then the new workflow state should be added to the system
    And I should see a success message
    And the new state should appear when listing workflow states

  Scenario: Add a new workflow state with custom metadata
    When I run "rin workflow add-state --name UAT --description 'User acceptance testing' --meta phase=verification --meta requires-signoff=true"
    Then the new workflow state should be added with the custom metadata
    And I should be able to query for workflow states with this metadata

  Scenario: Attempt to add a duplicate workflow state
    Given the workflow state "CODE_REVIEW" already exists
    When I run "rin workflow add-state --name CODE_REVIEW --description 'New description'"
    Then I should see an error message about duplicate state names
    And the existing state should remain unchanged

  Scenario: Update a workflow state
    Given the workflow state "CODE_REVIEW" exists
    When I run "rin workflow update-state --name CODE_REVIEW --new-name CR --description 'Updated description'"
    Then the workflow state should be updated
    And I should see a success message
    And any work items in this state should reflect the updated state name

  Scenario: Set a workflow state as a start state
    Given the workflow state "BACKLOG" exists
    When I run "rin workflow set-start-state --name BACKLOG"
    Then "BACKLOG" should be marked as a valid start state
    And new work items should be able to start in this state

  Scenario: Set a workflow state as an end state
    Given the workflow state "CANCELLED" exists
    When I run "rin workflow set-end-state --name CANCELLED"
    Then "CANCELLED" should be marked as a valid end state
    And work items should be allowed to terminate in this state

  Scenario: Remove a workflow state that is in use
    Given the workflow state "IN_DEV" exists
    And there are work items currently in the "IN_DEV" state
    When I run "rin workflow remove-state --name IN_DEV"
    Then I should see an error message about the state being in use
    And the state should not be removed

  Scenario: Remove a workflow state that is not in use
    Given the workflow state "BACKLOG_REVIEW" exists
    And no work items are currently in the "BACKLOG_REVIEW" state
    When I run "rin workflow remove-state --name BACKLOG_REVIEW"
    Then the state should be removed from the system
    And it should no longer appear when listing workflow states

  Scenario: Force remove a workflow state that is in use
    Given the workflow state "LOW_PRIORITY" exists
    And there are work items currently in the "LOW_PRIORITY" state
    When I run "rin workflow remove-state --name LOW_PRIORITY --force --target-state BACKLOG"
    Then the state should be removed from the system
    And all work items previously in "LOW_PRIORITY" should be moved to "BACKLOG"
    And I should see a warning about the forced state change

  # ===== Workflow Transition Management =====

  Scenario: View allowed workflow transitions
    When I run "rin workflow transitions"
    Then I should see a list of all permitted transitions between states
    And the default transitions should include paths from start states to end states
    And I should see a visual representation of the workflow

  Scenario: Add a new workflow transition
    Given workflow states "CODE_REVIEW" and "TESTING" exist
    When I run "rin workflow add-transition --from CODE_REVIEW --to TESTING"
    Then a new transition should be added between the states
    And I should be able to move work items from "CODE_REVIEW" to "TESTING"
    And I should see a success message

  Scenario: Add a workflow transition with conditions
    Given workflow states "TESTING" and "DONE" exist
    When I run "rin workflow add-transition --from TESTING --to DONE --requires-role qa --requires-field testsPassed=true"
    Then a conditional transition should be added between the states
    And I should see that this transition requires specific roles and field values
    And I should see a success message

  Scenario: Update a workflow transition
    Given a transition exists from "TESTING" to "DONE"
    When I run "rin workflow update-transition --from TESTING --to DONE --add-requires-role manager"
    Then the transition should be updated with the new requirement
    And I should see that the transition now requires both "qa" and "manager" roles

  Scenario: Remove a workflow transition
    Given a transition exists from "IN_DEV" to "TESTING"
    When I run "rin workflow remove-transition --from IN_DEV --to TESTING"
    Then the transition should be removed
    And I should no longer be able to move work items directly from "IN_DEV" to "TESTING"
    And I should see a success message

  Scenario: Attempt to create an invalid workflow loop
    Given workflow states "A", "B", and "C" exist in a linear flow
    When I run "rin workflow add-transition --from C --to A"
    And I run "rin workflow add-transition --from A --to C"
    Then I should see a warning about creating a potential infinite loop
    But the transitions should still be created
    And the workflow diagram should highlight the loop

  # ===== Workflow Templates and Export/Import =====

  Scenario: Create a workflow template from current configuration
    Given I have customized the workflow states and transitions
    When I run "rin workflow save-template --name 'Agile Development'"
    Then the current workflow configuration should be saved as a template
    And I should see a success message with the template ID

  Scenario: List available workflow templates
    Given multiple workflow templates exist in the system
    When I run "rin workflow templates"
    Then I should see a list of all available templates
    And each template should show a name, description, and creation date

  Scenario: Apply a workflow template
    Given a workflow template "Kanban Process" exists
    When I run "rin workflow apply-template --name 'Kanban Process' --project PROJECT-X"
    Then the workflow for "PROJECT-X" should be updated to match the template
    And I should see a summary of changes made
    And I should get a warning about any work items that need state migration

  Scenario: Export workflow configuration
    Given I have customized the workflow states and transitions
    When I run "rin workflow export --format json"
    Then I should receive a JSON file with the complete workflow configuration
    And the export should include all states, transitions, and metadata

  Scenario: Import workflow configuration
    Given I have a workflow configuration file "custom-workflow.json"
    When I run "rin workflow import --file custom-workflow.json"
    Then the system workflow should be updated to match the imported configuration
    And I should see a summary of the changes made
    And I should be warned about any conflicts or required migrations

  # ===== Workflow Recommendations =====

  Scenario: Get workflow recommendations
    When I run "rin workflow recommend"
    Then I should see suggested workflow improvements
    And the suggestions should be based on historical usage patterns
    And each suggestion should include a rationale and implementation command

  Scenario: Generate default workflow for a new project
    When I run "rin workflow generate-default --project-type software-development"
    Then I should see a suggested workflow configuration
    And the suggestion should match common patterns for software development
    And I should have the option to apply this configuration