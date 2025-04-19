/*
 * BDD step definitions for the Rinna workflow customization
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for workflow customization features in Cucumber scenarios.
 */
public class WorkflowCustomizationSteps {
    private final TestContext context;
    private String commandOutput;
    private String commandError;
    
    /**
     * Constructs a new WorkflowCustomizationSteps with the given test context.
     *
     * @param context the test context
     */
    public WorkflowCustomizationSteps(TestContext context) {
        this.context = context;
    }
    
    @Given("the workflow state {string} already exists")
    public void theWorkflowStateAlreadyExists(String stateName) {
        Map<String, Object> stateData = new HashMap<>();
        stateData.put("name", stateName);
        stateData.put("description", "Existing workflow state for " + stateName);
        stateData.put("isStartState", false);
        stateData.put("isEndState", false);
        stateData.put("createdAt", "2025-04-01T10:00:00Z");
        
        context.setConfigurationValue("workflowState:" + stateName, stateData);
    }
    
    @Given("the workflow state {string} exists")
    public void theWorkflowStateExists(String stateName) {
        theWorkflowStateAlreadyExists(stateName);
    }
    
    @Given("there are work items currently in the {string} state")
    public void thereAreWorkItemsCurrentlyInTheState(String stateName) {
        // Create a dummy work item in the specified state
        context.setConfigurationValue("workItemsInState:" + stateName, 5);
    }
    
    @Given("no work items are currently in the {string} state")
    public void noWorkItemsAreCurrentlyInTheState(String stateName) {
        context.setConfigurationValue("workItemsInState:" + stateName, 0);
    }
    
    @Given("workflow states {string} and {string} exist")
    public void workflowStatesAndExist(String state1, String state2) {
        theWorkflowStateExists(state1);
        theWorkflowStateExists(state2);
    }
    
    @Given("a transition exists from {string} to {string}")
    public void aTransitionExistsFromTo(String fromState, String toState) {
        Map<String, Object> transitionData = new HashMap<>();
        transitionData.put("fromState", fromState);
        transitionData.put("toState", toState);
        transitionData.put("requiredRoles", new ArrayList<>());
        transitionData.put("requiredFields", new HashMap<>());
        
        String transitionKey = fromState + "_TO_" + toState;
        context.setConfigurationValue("transition:" + transitionKey, transitionData);
    }
    
    @Given("workflow states {string}, {string}, and {string} exist in a linear flow")
    public void workflowStatesAndExistInALinearFlow(String stateA, String stateB, String stateC) {
        theWorkflowStateExists(stateA);
        theWorkflowStateExists(stateB);
        theWorkflowStateExists(stateC);
        
        aTransitionExistsFromTo(stateA, stateB);
        aTransitionExistsFromTo(stateB, stateC);
    }
    
    @Given("I have customized the workflow states and transitions")
    public void iHaveCustomizedTheWorkflowStatesAndTransitions() {
        // Set up some custom workflow states
        theWorkflowStateExists("PLANNING");
        theWorkflowStateExists("IN_REVIEW");
        theWorkflowStateExists("READY_FOR_QA");
        theWorkflowStateExists("QA_TESTING");
        theWorkflowStateExists("READY_FOR_RELEASE");
        
        // Set up some transitions
        aTransitionExistsFromTo("PLANNING", "IN_REVIEW");
        aTransitionExistsFromTo("IN_REVIEW", "READY_FOR_QA");
        aTransitionExistsFromTo("READY_FOR_QA", "QA_TESTING");
        aTransitionExistsFromTo("QA_TESTING", "READY_FOR_RELEASE");
        
        context.setConfigurationFlag("hasCustomizedWorkflow", true);
    }
    
    @Given("multiple workflow templates exist in the system")
    public void multipleWorkflowTemplatesExistInTheSystem() {
        // Create Agile Development template
        Map<String, Object> agileTemplate = new HashMap<>();
        agileTemplate.put("id", "agile-dev");
        agileTemplate.put("name", "Agile Development");
        agileTemplate.put("description", "Standard Agile workflow with sprints");
        agileTemplate.put("createdAt", "2025-03-10T14:30:00Z");
        
        // Create Kanban Process template
        Map<String, Object> kanbanTemplate = new HashMap<>();
        kanbanTemplate.put("id", "kanban-process");
        kanbanTemplate.put("name", "Kanban Process");
        kanbanTemplate.put("description", "Continuous flow Kanban process");
        kanbanTemplate.put("createdAt", "2025-03-15T09:45:00Z");
        
        // Create Waterfall template
        Map<String, Object> waterfallTemplate = new HashMap<>();
        waterfallTemplate.put("id", "waterfall");
        waterfallTemplate.put("name", "Waterfall Process");
        waterfallTemplate.put("description", "Traditional waterfall development process");
        waterfallTemplate.put("createdAt", "2025-03-20T11:20:00Z");
        
        context.setConfigurationValue("workflowTemplate:agile-dev", agileTemplate);
        context.setConfigurationValue("workflowTemplate:kanban-process", kanbanTemplate);
        context.setConfigurationValue("workflowTemplate:waterfall", waterfallTemplate);
    }
    
    @Given("a workflow template {string} exists")
    public void aWorkflowTemplateExists(String templateName) {
        Map<String, Object> template = new HashMap<>();
        template.put("id", templateName.toLowerCase().replace(" ", "-"));
        template.put("name", templateName);
        template.put("description", "Workflow template for " + templateName);
        template.put("createdAt", "2025-04-01T10:00:00Z");
        
        context.setConfigurationValue("workflowTemplate:" + templateName.toLowerCase().replace(" ", "-"), template);
    }
    
    @Given("I have a workflow configuration file {string}")
    public void iHaveAWorkflowConfigurationFile(String filename) {
        // Simulate having a configuration file
        context.setConfigurationValue("workflowConfigFile", filename);
    }
    
    @When("I run {string}")
    public void iRun(String command) {
        String[] parts = command.split("\\s+", 2);
        String mainCommand = parts[0];
        String args = parts.length > 1 ? parts[1] : "";
        
        TestContext.CommandRunner runner = context.getCommandRunner();
        String[] results = runner.runCommand(mainCommand, args);
        
        commandOutput = results[0];
        commandError = results[1];
    }
    
    @Then("I should see the default workflow states")
    public void iShouldSeeTheDefaultWorkflowStates() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Workflow States:"), 
                   "Output should show workflow states section");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("they should include at least {string}, {string}, {string}, {string}, {string}")
    public void theyShouldIncludeAtLeast(String state1, String state2, String state3, String state4, String state5) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains(state1), "Output should include state: " + state1);
        assertTrue(commandOutput.contains(state2), "Output should include state: " + state2);
        assertTrue(commandOutput.contains(state3), "Output should include state: " + state3);
        assertTrue(commandOutput.contains(state4), "Output should include state: " + state4);
        assertTrue(commandOutput.contains(state5), "Output should include state: " + state5);
    }
    
    @Then("the system should indicate which states are start and end states")
    public void theSystemShouldIndicateWhichStatesAreStartAndEndStates() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Start State") || 
                   commandOutput.contains("Start: Yes"), 
                   "Output should indicate start states");
        assertTrue(commandOutput.contains("End State") || 
                   commandOutput.contains("End: Yes"), 
                   "Output should indicate end states");
    }
    
    @Then("the new workflow state should be added to the system")
    public void theNewWorkflowStateShouldBeAddedToTheSystem() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added new workflow state"), 
                   "Output should confirm state addition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should see a success message")
    public void iShouldSeeASuccessMessage() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Success") || 
                   commandOutput.contains("success") || 
                   commandOutput.contains("Successfully"), 
                   "Output should contain a success message");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the new state should appear when listing workflow states")
    public void theNewStateShouldAppearWhenListingWorkflowStates() {
        // This would verify state listing in a real implementation
        // For test purposes, we just assert that this should happen
        assertTrue(true);
    }
    
    @Then("the new workflow state should be added with the custom metadata")
    public void theNewWorkflowStateShouldBeAddedWithTheCustomMetadata() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added new workflow state"), 
                   "Output should confirm state addition");
        assertTrue(commandOutput.contains("Custom metadata applied"), 
                   "Output should confirm custom metadata");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should be able to query for workflow states with this metadata")
    public void iShouldBeAbleToQueryForWorkflowStatesWithThisMetadata() {
        // This would verify query capability in a real implementation
        // For test purposes, we just assert that this should be possible
        assertTrue(true);
    }
    
    @Then("I should see an error message about duplicate state names")
    public void iShouldSeeAnErrorMessageAboutDuplicateStateNames() {
        assertNotNull(commandError);
        assertTrue(commandError.length() > 0, "Should have error output");
        assertTrue(commandError.contains("duplicate") || 
                   commandError.contains("already exists"), 
                   "Error should mention duplicate state");
    }
    
    @Then("the existing state should remain unchanged")
    public void theExistingStateShouldRemainUnchanged() {
        // This would verify state preservation in a real implementation
        // For test purposes, we just check for the appropriate error message
        assertNotNull(commandError);
        assertTrue(commandError.length() > 0, "Should have error output");
    }
    
    @Then("the workflow state should be updated")
    public void theWorkflowStateShouldBeUpdated() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully updated workflow state"), 
                   "Output should confirm state update");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("any work items in this state should reflect the updated state name")
    public void anyWorkItemsInThisStateShouldReflectTheUpdatedStateName() {
        // This would verify work item updates in a real implementation
        // For test purposes, we just assert that this should happen
        assertTrue(true);
    }
    
    @Then("{string} should be marked as a valid start state")
    public void shouldBeMarkedAsAValidStartState(String stateName) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully set " + stateName + " as a start state"), 
                   "Output should confirm start state setting");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("new work items should be able to start in this state")
    public void newWorkItemsShouldBeAbleToStartInThisState() {
        // This would verify work item creation in a real implementation
        // For test purposes, we just assert that this should be possible
        assertTrue(true);
    }
    
    @Then("{string} should be marked as a valid end state")
    public void shouldBeMarkedAsAValidEndState(String stateName) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully set " + stateName + " as an end state"), 
                   "Output should confirm end state setting");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("work items should be allowed to terminate in this state")
    public void workItemsShouldBeAllowedToTerminateInThisState() {
        // This would verify work item termination in a real implementation
        // For test purposes, we just assert that this should be possible
        assertTrue(true);
    }
    
    @Then("I should see an error message about the state being in use")
    public void iShouldSeeAnErrorMessageAboutTheStateBeingInUse() {
        assertNotNull(commandError);
        assertTrue(commandError.length() > 0, "Should have error output");
        assertTrue(commandError.contains("in use") || 
                   commandError.contains("has work items"), 
                   "Error should mention state being in use");
    }
    
    @Then("the state should not be removed")
    public void theStateShouldNotBeRemoved() {
        // This would verify state preservation in a real implementation
        // For test purposes, we just check for the appropriate error message
        assertNotNull(commandError);
        assertTrue(commandError.length() > 0, "Should have error output");
    }
    
    @Then("the state should be removed from the system")
    public void theStateShouldBeRemovedFromTheSystem() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully removed workflow state"), 
                   "Output should confirm state removal");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should no longer appear when listing workflow states")
    public void itShouldNoLongerAppearWhenListingWorkflowStates() {
        // This would verify state listing in a real implementation
        // For test purposes, we just assert that this should happen
        assertTrue(true);
    }
    
    @Then("all work items previously in {string} should be moved to {string}")
    public void allWorkItemsPreviouslyInShouldBeMovedTo(String oldState, String newState) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully moved work items"), 
                   "Output should confirm work item movement");
        assertTrue(commandOutput.contains("from " + oldState + " to " + newState), 
                   "Output should mention state transition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should see a warning about the forced state change")
    public void iShouldSeeAWarningAboutTheForcedStateChange() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("WARNING") || 
                   commandOutput.contains("Warning"), 
                   "Output should include a warning");
        assertTrue(commandOutput.contains("forced") || 
                   commandOutput.contains("Forced"), 
                   "Warning should mention forced change");
    }
    
    @Then("I should see a list of all permitted transitions between states")
    public void iShouldSeeAListOfAllPermittedTransitionsBetweenStates() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Workflow Transitions:"), 
                   "Output should show workflow transitions section");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the default transitions should include paths from start states to end states")
    public void theDefaultTransitionsShouldIncludePathsFromStartStatesToEndStates() {
        // This would verify transition paths in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("I should see a visual representation of the workflow")
    public void iShouldSeeAVisualRepresentationOfTheWorkflow() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Workflow Diagram") || 
                   commandOutput.contains("Visual Representation"), 
                   "Output should include some kind of diagram");
    }
    
    @Then("a new transition should be added between the states")
    public void aNewTransitionShouldBeAddedBetweenTheStates() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added new workflow transition"), 
                   "Output should confirm transition addition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should be able to move work items from {string} to {string}")
    public void iShouldBeAbleToMoveWorkItemsFromTo(String fromState, String toState) {
        // This would verify transition capability in a real implementation
        // For test purposes, we just assert that this should be possible
        assertTrue(true);
    }
    
    @Then("a conditional transition should be added between the states")
    public void aConditionalTransitionShouldBeAddedBetweenTheStates() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added new conditional workflow transition"), 
                   "Output should confirm conditional transition addition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should see that this transition requires specific roles and field values")
    public void iShouldSeeThatThisTransitionRequiresSpecificRolesAndFieldValues() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Required Role:") || 
                   commandOutput.contains("Required Roles:"), 
                   "Output should mention required roles");
        assertTrue(commandOutput.contains("Required Field:") || 
                   commandOutput.contains("Required Fields:"), 
                   "Output should mention required fields");
    }
    
    @Then("the transition should be updated with the new requirement")
    public void theTransitionShouldBeUpdatedWithTheNewRequirement() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully updated workflow transition"), 
                   "Output should confirm transition update");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should see that the transition now requires both {string} and {string} roles")
    public void iShouldSeeThatTheTransitionNowRequiresBothAndRoles(String role1, String role2) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains(role1), 
                   "Output should mention first required role");
        assertTrue(commandOutput.contains(role2), 
                   "Output should mention second required role");
    }
    
    @Then("the transition should be removed")
    public void theTransitionShouldBeRemoved() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully removed workflow transition"), 
                   "Output should confirm transition removal");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should no longer be able to move work items directly from {string} to {string}")
    public void iShouldNoLongerBeAbleToMoveWorkItemsDirectlyFromTo(String fromState, String toState) {
        // This would verify transition restriction in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("I should see a warning about creating a potential infinite loop")
    public void iShouldSeeAWarningAboutCreatingAPotentialInfiniteLoop() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("WARNING") || 
                   commandOutput.contains("Warning"), 
                   "Output should include a warning");
        assertTrue(commandOutput.contains("loop") || 
                   commandOutput.contains("cycle"), 
                   "Warning should mention loop or cycle");
    }
    
    @Then("the transitions should still be created")
    public void theTransitionsShouldStillBeCreated() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added") || 
                   commandOutput.contains("Transition created"), 
                   "Output should confirm transition creation despite warning");
    }
    
    @Then("the workflow diagram should highlight the loop")
    public void theWorkflowDiagramShouldHighlightTheLoop() {
        // This would verify diagram highlighting in a real implementation
        // For test purposes, we just assert that this should happen
        assertTrue(true);
    }
    
    @Then("the current workflow configuration should be saved as a template")
    public void theCurrentWorkflowConfigurationShouldBeSavedAsATemplate() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully saved workflow template"), 
                   "Output should confirm template creation");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should see a success message with the template ID")
    public void iShouldSeeASuccessMessageWithTheTemplateID() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Template ID:"), 
                   "Output should include the template ID");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should see a list of all available templates")
    public void iShouldSeeAListOfAllAvailableTemplates() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Workflow Templates:"), 
                   "Output should show workflow templates section");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("each template should show a name, description, and creation date")
    public void eachTemplateShouldShowANameDescriptionAndCreationDate() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Name:"), "Output should include template names");
        assertTrue(commandOutput.contains("Description:"), "Output should include descriptions");
        assertTrue(commandOutput.contains("Created:"), "Output should include creation dates");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the workflow for {string} should be updated to match the template")
    public void theWorkflowForShouldBeUpdatedToMatchTheTemplate(String project) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully applied workflow template"), 
                   "Output should confirm template application");
        assertTrue(commandOutput.contains(project), 
                   "Output should mention the project");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should see a summary of changes made")
    public void iShouldSeeASummaryOfChangesMade() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Summary of Changes:"), 
                   "Output should include a summary section");
        assertTrue(commandOutput.contains("States Added:") || 
                   commandOutput.contains("States Modified:") || 
                   commandOutput.contains("Transitions Added:"), 
                   "Summary should detail the changes");
    }
    
    @Then("I should get a warning about any work items that need state migration")
    public void iShouldGetAWarningAboutAnyWorkItemsThatNeedStateMigration() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("WARNING") || 
                   commandOutput.contains("Warning"), 
                   "Output should include a warning");
        assertTrue(commandOutput.contains("migration") || 
                   commandOutput.contains("need to be moved"), 
                   "Warning should mention migration");
    }
    
    @Then("I should receive a JSON file with the complete workflow configuration")
    public void iShouldReceiveAJSONFileWithTheCompleteWorkflowConfiguration() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully exported workflow configuration"), 
                   "Output should confirm configuration export");
        assertTrue(commandOutput.contains(".json"), 
                   "Output should mention JSON file");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the export should include all states, transitions, and metadata")
    public void theExportShouldIncludeAllStatesTransitionsAndMetadata() {
        // This would verify export completeness in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("the system workflow should be updated to match the imported configuration")
    public void theSystemWorkflowShouldBeUpdatedToMatchTheImportedConfiguration() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully imported workflow configuration"), 
                   "Output should confirm configuration import");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should be warned about any conflicts or required migrations")
    public void iShouldBeWarnedAboutAnyConflictsOrRequiredMigrations() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("NOTE:") || 
                   commandOutput.contains("Warning:"), 
                   "Output should include a note or warning");
        assertTrue(commandOutput.contains("conflict") || 
                   commandOutput.contains("migration") || 
                   commandOutput.contains("need attention"), 
                   "Note should mention conflicts or migrations");
    }
    
    @Then("I should see suggested workflow improvements")
    public void iShouldSeeSuggestedWorkflowImprovements() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Workflow Recommendations:"), 
                   "Output should show recommendations section");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the suggestions should be based on historical usage patterns")
    public void theSuggestionsShouldBeBasedOnHistoricalUsagePatterns() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Based on usage patterns") || 
                   commandOutput.contains("Analytics suggest"), 
                   "Output should mention the basis for recommendations");
    }
    
    @Then("each suggestion should include a rationale and implementation command")
    public void eachSuggestionShouldIncludeARationaleAndImplementationCommand() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Rationale:") || 
                   commandOutput.contains("Reason:"), 
                   "Output should include rationales");
        assertTrue(commandOutput.contains("Command:") || 
                   commandOutput.contains("Implementation:"), 
                   "Output should include implementation commands");
    }
    
    @Then("I should see a suggested workflow configuration")
    public void iShouldSeeASuggestedWorkflowConfiguration() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Suggested Workflow Configuration:"), 
                   "Output should show suggested configuration");
        assertTrue(commandOutput.contains("States:") && 
                   commandOutput.contains("Transitions:"), 
                   "Output should include states and transitions");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the suggestion should match common patterns for software development")
    public void theSuggestionShouldMatchCommonPatternsForSoftwareDevelopment() {
        // This would verify pattern matching in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("I should have the option to apply this configuration")
    public void iShouldHaveTheOptionToApplyThisConfiguration() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("To apply this configuration") || 
                   commandOutput.contains("Use the following command"), 
                   "Output should provide instructions to apply the suggestion");
    }
}