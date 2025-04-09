@admin
Feature: Workflow Configuration and Management
  As an admin user
  I want to define workflow states and transitions for my project
  So that work items can follow a structured process

  Background:
    Given I have installed Rinna in my Java project
    And the Rinna server is running
    And I am authenticated as the admin user
    And I have created a project named "Workflow Test"

  Scenario: View default workflow states
    When I run "bin/rin workflow states"
    Then I should see the default workflow states
    And they should include at least "FOUND", "TRIAGED", "TO_DO", "IN_PROGRESS", "IN_TEST", "DONE"
    And I should see which states are marked as start states
    And I should see which states are marked as end states

  Scenario: Define custom workflow states
    When I run "bin/rin workflow add-state --name PLANNING --description 'Planning phase'"
    Then I should see "Successfully added workflow state: PLANNING"
    And the workflow state "PLANNING" should exist in the system
    When I run "bin/rin workflow add-state --name REQUIREMENTS --description 'Requirements gathering'"
    Then I should see "Successfully added workflow state: REQUIREMENTS"
    And the workflow state "REQUIREMENTS" should exist in the system
    When I run "bin/rin workflow add-state --name DEVELOPMENT --description 'Development work'"
    Then I should see "Successfully added workflow state: DEVELOPMENT"
    And the workflow state "DEVELOPMENT" should exist in the system
    When I run "bin/rin workflow add-state --name CODE_REVIEW --description 'Code review process'"
    Then I should see "Successfully added workflow state: CODE_REVIEW"
    And the workflow state "CODE_REVIEW" should exist in the system
    When I run "bin/rin workflow add-state --name TESTING --description 'Testing phase'"
    Then I should see "Successfully added workflow state: TESTING"
    And the workflow state "TESTING" should exist in the system
    When I run "bin/rin workflow add-state --name DEPLOYMENT --description 'Deployment phase'"
    Then I should see "Successfully added workflow state: DEPLOYMENT"
    And the workflow state "DEPLOYMENT" should exist in the system
    When I run "bin/rin workflow add-state --name COMPLETED --description 'Completed work'"
    Then I should see "Successfully added workflow state: COMPLETED"
    And the workflow state "COMPLETED" should exist in the system

  Scenario: Define start and end states
    Given I have defined custom workflow states
    When I run "bin/rin workflow set-start-state --name PLANNING"
    Then I should see "Successfully set PLANNING as a start state"
    And "PLANNING" should be marked as a valid start state
    When I run "bin/rin workflow set-start-state --name REQUIREMENTS"
    Then I should see "Successfully set REQUIREMENTS as a start state"
    And "REQUIREMENTS" should be marked as a valid start state
    When I run "bin/rin workflow set-end-state --name COMPLETED"
    Then I should see "Successfully set COMPLETED as an end state"
    And "COMPLETED" should be marked as a valid end state
    When I run "bin/rin workflow set-end-state --name DEPLOYMENT"
    Then I should see "Successfully set DEPLOYMENT as an end state"
    And "DEPLOYMENT" should be marked as a valid end state
    When I run "bin/rin workflow states"
    Then I should see "PLANNING" and "REQUIREMENTS" marked as start states
    And I should see "COMPLETED" and "DEPLOYMENT" marked as end states

  Scenario: Define workflow transitions
    Given I have defined custom workflow states
    And I have set "PLANNING" as a start state
    And I have set "COMPLETED" as an end state
    When I run "bin/rin workflow add-transition --from PLANNING --to REQUIREMENTS"
    Then I should see "Successfully added transition: PLANNING → REQUIREMENTS"
    And a transition should exist from "PLANNING" to "REQUIREMENTS"
    When I run "bin/rin workflow add-transition --from REQUIREMENTS --to DEVELOPMENT"
    Then I should see "Successfully added transition: REQUIREMENTS → DEVELOPMENT"
    And a transition should exist from "REQUIREMENTS" to "DEVELOPMENT"
    When I run "bin/rin workflow add-transition --from DEVELOPMENT --to CODE_REVIEW"
    Then I should see "Successfully added transition: DEVELOPMENT → CODE_REVIEW"
    And a transition should exist from "DEVELOPMENT" to "CODE_REVIEW"
    When I run "bin/rin workflow add-transition --from CODE_REVIEW --to TESTING"
    Then I should see "Successfully added transition: CODE_REVIEW → TESTING"
    And a transition should exist from "CODE_REVIEW" to "TESTING"
    When I run "bin/rin workflow add-transition --from TESTING --to DEPLOYMENT"
    Then I should see "Successfully added transition: TESTING → DEPLOYMENT"
    And a transition should exist from "TESTING" to "DEPLOYMENT"
    When I run "bin/rin workflow add-transition --from DEPLOYMENT --to COMPLETED"
    Then I should see "Successfully added transition: DEPLOYMENT → COMPLETED"
    And a transition should exist from "DEPLOYMENT" to "COMPLETED"
    When I run "bin/rin workflow add-transition --from CODE_REVIEW --to DEVELOPMENT"
    Then I should see "Successfully added transition: CODE_REVIEW → DEVELOPMENT"
    And a transition should exist from "CODE_REVIEW" to "DEVELOPMENT"
    When I run "bin/rin workflow add-transition --from TESTING --to DEVELOPMENT"
    Then I should see "Successfully added transition: TESTING → DEVELOPMENT"
    And a transition should exist from "TESTING" to "DEVELOPMENT"

  Scenario: Define conditional transitions
    Given I have defined custom workflow states
    When I run "bin/rin workflow add-transition --from TESTING --to DEPLOYMENT --requires-role qa --requires-field testsPassed=true"
    Then I should see "Successfully added conditional transition"
    And a conditional transition should exist from "TESTING" to "DEPLOYMENT"
    And the transition should require the "qa" role
    And the transition should require the "testsPassed" field to be "true"
    When I run "bin/rin workflow transitions"
    Then I should see the conditional requirements for the transition from "TESTING" to "DEPLOYMENT"

  Scenario: Visualize workflow
    Given I have defined custom workflow states and transitions
    When I run "bin/rin workflow diagram"
    Then I should see a visual representation of the workflow
    And the diagram should show all states including "PLANNING", "REQUIREMENTS", "DEVELOPMENT", "CODE_REVIEW", "TESTING", "DEPLOYMENT", "COMPLETED"
    And the diagram should show all transitions between states
    And start states should be highlighted in the diagram
    And end states should be highlighted in the diagram
    And conditional transitions should be marked in the diagram

  Scenario: Update workflow state
    Given I have defined a workflow state "TESTING" with description "Testing phase"
    When I run "bin/rin workflow update-state --name TESTING --new-name QA --description 'Quality Assurance'"
    Then I should see "Successfully updated workflow state"
    And the workflow state "QA" should exist with description "Quality Assurance"
    And the workflow state "TESTING" should no longer exist
    And all transitions to and from "TESTING" should now reference "QA"

  Scenario: Remove workflow transition
    Given I have defined workflow states and transitions
    And a transition exists from "CODE_REVIEW" to "DEVELOPMENT"
    When I run "bin/rin workflow remove-transition --from CODE_REVIEW --to DEVELOPMENT"
    Then I should see "Successfully removed transition"
    And the transition from "CODE_REVIEW" to "DEVELOPMENT" should no longer exist
    When I run "bin/rin workflow transitions"
    Then I should not see a transition from "CODE_REVIEW" to "DEVELOPMENT"

  Scenario: Test workflow with work items
    Given I have defined workflow states and transitions
    And I have defined a work item type "TASK"
    When I run "bin/rin item create --type TASK --title 'Test workflow' --state PLANNING"
    Then I should see "Successfully created work item"
    And a new work item should be created in the "PLANNING" state
    When I run "bin/rin item transition --id 1 --to REQUIREMENTS"
    Then I should see "Successfully transitioned work item"
    And the work item should now be in the "REQUIREMENTS" state
    When I run "bin/rin item transition --id 1 --to DEVELOPMENT"
    Then I should see "Successfully transitioned work item"
    And the work item should now be in the "DEVELOPMENT" state

  Scenario: Export workflow configuration
    Given I have defined custom workflow states and transitions
    When I run "bin/rin workflow export --format json --file workflow-config.json"
    Then I should see "Successfully exported workflow configuration"
    And a file "workflow-config.json" should be created
    And the file should contain all defined states
    And the file should contain all defined transitions
    And the file should include start and end state designations

  Scenario: Import workflow configuration
    Given I have a workflow configuration file "workflow-config.json"
    When I run "bin/rin workflow import --file workflow-config.json"
    Then I should see "Successfully imported workflow configuration"
    And all states from the configuration file should be created
    And all transitions from the configuration file should be established
    And start and end states should be properly designated
    When I run "bin/rin workflow states"
    Then I should see all the imported workflow states
    When I run "bin/rin workflow transitions"
    Then I should see all the imported workflow transitions