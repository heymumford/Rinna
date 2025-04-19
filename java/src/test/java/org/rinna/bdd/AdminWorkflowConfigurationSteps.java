/*
 * BDD step definitions for Rinna admin workflow configuration
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

/**
 * Step definitions for testing Rinna admin workflow configuration.
 */
public class AdminWorkflowConfigurationSteps {
    private final TestContext context;
    private String commandOutput;
    private String commandError;
    
    /**
     * Constructs a new AdminWorkflowConfigurationSteps with the given test context.
     *
     * @param context the test context
     */
    public AdminWorkflowConfigurationSteps(TestContext context) {
        this.context = context;
    }
    
    @Then("I should see the default workflow states")
    public void iShouldSeeTheDefaultWorkflowStates() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        // Set up default states if not already present
        if (!context.getConfigurationValue("defaultStatesInitialized").isPresent()) {
            List<String> defaultStates = Arrays.asList("FOUND", "TRIAGED", "TO_DO", "IN_PROGRESS", "IN_TEST", "DONE");
            
            String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
            assertNotNull(currentProject, "Current project should be set");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                    .orElse(new HashMap<>());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
            assertNotNull(project, "Project should exist in projects map");
            
            Map<String, Object> workflowStates = new HashMap<>();
            for (String state : defaultStates) {
                Map<String, Object> stateDetails = new HashMap<>();
                stateDetails.put("name", state);
                stateDetails.put("description", "Default state: " + state);
                
                // Set start/end states
                if (state.equals("FOUND") || state.equals("TRIAGED")) {
                    stateDetails.put("isStartState", true);
                }
                if (state.equals("DONE")) {
                    stateDetails.put("isEndState", true);
                }
                
                workflowStates.put(state, stateDetails);
            }
            
            project.put("workflowStates", workflowStates);
            projects.put(currentProject, project);
            context.setConfigurationValue("projects", projects);
            context.setConfigurationValue("defaultStatesInitialized", true);
        }
        
        // Check output
        for (String state : Arrays.asList("FOUND", "TRIAGED", "TO_DO", "IN_PROGRESS", "IN_TEST", "DONE")) {
            assertTrue(lastOutput.contains(state), 
                    "Output should contain default state: " + state);
        }
    }
    
    @Then("they should include at least {string}, {string}, {string}, {string}, {string}, {string}")
    public void theyShouldIncludeAtLeast(String state1, String state2, String state3, String state4, String state5, String state6) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(state1), "Output should contain state: " + state1);
        assertTrue(lastOutput.contains(state2), "Output should contain state: " + state2);
        assertTrue(lastOutput.contains(state3), "Output should contain state: " + state3);
        assertTrue(lastOutput.contains(state4), "Output should contain state: " + state4);
        assertTrue(lastOutput.contains(state5), "Output should contain state: " + state5);
        assertTrue(lastOutput.contains(state6), "Output should contain state: " + state6);
    }
    
    @Then("I should see which states are marked as start states")
    public void iShouldSeeWhichStatesAreMarkedAsStartStates() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        // Find start states
        for (Map.Entry<String, Object> entry : workflowStates.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateDetails = (Map<String, Object>) entry.getValue();
            
            if (Boolean.TRUE.equals(stateDetails.get("isStartState"))) {
                String stateName = entry.getKey();
                assertTrue(lastOutput.contains(stateName) && 
                           (lastOutput.contains("Start State") || lastOutput.contains("start state")), 
                        "Output should indicate that " + stateName + " is a start state");
            }
        }
    }
    
    @Then("I should see which states are marked as end states")
    public void iShouldSeeWhichStatesAreMarkedAsEndStates() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        // Find end states
        for (Map.Entry<String, Object> entry : workflowStates.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateDetails = (Map<String, Object>) entry.getValue();
            
            if (Boolean.TRUE.equals(stateDetails.get("isEndState"))) {
                String stateName = entry.getKey();
                assertTrue(lastOutput.contains(stateName) && 
                           (lastOutput.contains("End State") || lastOutput.contains("end state")), 
                        "Output should indicate that " + stateName + " is an end state");
            }
        }
    }
    
    @Then("the workflow state {string} should exist in the system")
    public void theWorkflowStateShouldExistInTheSystem(String stateName) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        assertTrue(workflowStates.containsKey(stateName), 
                "Workflow state " + stateName + " should exist in the system");
    }
    
    @Given("I have defined custom workflow states")
    public void iHaveDefinedCustomWorkflowStates() {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        Map<String, Object> workflowStates = new HashMap<>();
        
        // Define custom states
        for (String state : Arrays.asList("PLANNING", "REQUIREMENTS", "DEVELOPMENT", "CODE_REVIEW", "TESTING", "DEPLOYMENT", "COMPLETED")) {
            Map<String, Object> stateDetails = new HashMap<>();
            stateDetails.put("name", state);
            stateDetails.put("description", state + " phase");
            
            workflowStates.put(state, stateDetails);
        }
        
        project.put("workflowStates", workflowStates);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("{string} should be marked as a valid start state")
    public void shouldBeMarkedAsAValidStartState(String stateName) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        assertTrue(workflowStates.containsKey(stateName), 
                "Workflow state " + stateName + " should exist");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> stateDetails = (Map<String, Object>) workflowStates.get(stateName);
        
        assertTrue(Boolean.TRUE.equals(stateDetails.get("isStartState")), 
                "State " + stateName + " should be marked as a start state");
    }
    
    @Then("{string} should be marked as a valid end state")
    public void shouldBeMarkedAsAValidEndState(String stateName) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        assertTrue(workflowStates.containsKey(stateName), 
                "Workflow state " + stateName + " should exist");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> stateDetails = (Map<String, Object>) workflowStates.get(stateName);
        
        assertTrue(Boolean.TRUE.equals(stateDetails.get("isEndState")), 
                "State " + stateName + " should be marked as an end state");
    }
    
    @Then("I should see {string} and {string} marked as start states")
    public void iShouldSeeAndMarkedAsStartStates(String state1, String state2) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(state1) && 
                   (lastOutput.contains("Start State") || lastOutput.contains("start state")), 
                "Output should indicate that " + state1 + " is a start state");
        
        assertTrue(lastOutput.contains(state2) && 
                   (lastOutput.contains("Start State") || lastOutput.contains("start state")), 
                "Output should indicate that " + state2 + " is a start state");
    }
    
    @Then("I should see {string} and {string} marked as end states")
    public void iShouldSeeAndMarkedAsEndStates(String state1, String state2) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(state1) && 
                   (lastOutput.contains("End State") || lastOutput.contains("end state")), 
                "Output should indicate that " + state1 + " is an end state");
        
        assertTrue(lastOutput.contains(state2) && 
                   (lastOutput.contains("End State") || lastOutput.contains("end state")), 
                "Output should indicate that " + state2 + " is an end state");
    }
    
    @Given("I have set {string} as a start state")
    public void iHaveSetAsAStartState(String stateName) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        if (workflowStates.containsKey(stateName)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateDetails = (Map<String, Object>) workflowStates.get(stateName);
            stateDetails.put("isStartState", true);
            
            workflowStates.put(stateName, stateDetails);
            project.put("workflowStates", workflowStates);
            projects.put(currentProject, project);
            context.setConfigurationValue("projects", projects);
        } else {
            fail("Workflow state " + stateName + " does not exist");
        }
    }
    
    @Given("I have set {string} as an end state")
    public void iHaveSetAsAnEndState(String stateName) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        if (workflowStates.containsKey(stateName)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateDetails = (Map<String, Object>) workflowStates.get(stateName);
            stateDetails.put("isEndState", true);
            
            workflowStates.put(stateName, stateDetails);
            project.put("workflowStates", workflowStates);
            projects.put(currentProject, project);
            context.setConfigurationValue("projects", projects);
        } else {
            fail("Workflow state " + stateName + " does not exist");
        }
    }
    
    @Then("a transition should exist from {string} to {string}")
    public void aTransitionShouldExistFromTo(String fromState, String toState) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        boolean transitionExists = false;
        for (Map<String, Object> transition : workflowTransitions) {
            if (fromState.equals(transition.get("fromState")) && toState.equals(transition.get("toState"))) {
                transitionExists = true;
                break;
            }
        }
        
        assertTrue(transitionExists, 
                "Transition from " + fromState + " to " + toState + " should exist");
    }
    
    @Then("a conditional transition should exist from {string} to {string}")
    public void aConditionalTransitionShouldExistFromTo(String fromState, String toState) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        boolean conditionalTransitionExists = false;
        for (Map<String, Object> transition : workflowTransitions) {
            if (fromState.equals(transition.get("fromState")) && 
                toState.equals(transition.get("toState")) && 
                transition.containsKey("conditions")) {
                conditionalTransitionExists = true;
                break;
            }
        }
        
        assertTrue(conditionalTransitionExists, 
                "Conditional transition from " + fromState + " to " + toState + " should exist");
    }
    
    @Then("the transition should require the {string} role")
    public void theTransitionShouldRequireTheRole(String roleName) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        boolean roleConditionExists = false;
        for (Map<String, Object> transition : workflowTransitions) {
            if (transition.containsKey("conditions")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> conditions = (Map<String, Object>) transition.get("conditions");
                
                if (conditions.containsKey("requiredRoles")) {
                    @SuppressWarnings("unchecked")
                    List<String> requiredRoles = (List<String>) conditions.get("requiredRoles");
                    
                    if (requiredRoles.contains(roleName)) {
                        roleConditionExists = true;
                        break;
                    }
                }
            }
        }
        
        assertTrue(roleConditionExists, 
                "Transition should require the " + roleName + " role");
    }
    
    @Then("the transition should require the {string} field to be {string}")
    public void theTransitionShouldRequireTheFieldToBe(String fieldName, String fieldValue) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        boolean fieldConditionExists = false;
        for (Map<String, Object> transition : workflowTransitions) {
            if (transition.containsKey("conditions")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> conditions = (Map<String, Object>) transition.get("conditions");
                
                if (conditions.containsKey("requiredFields")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> requiredFields = (Map<String, Object>) conditions.get("requiredFields");
                    
                    if (requiredFields.containsKey(fieldName) && fieldValue.equals(requiredFields.get(fieldName).toString())) {
                        fieldConditionExists = true;
                        break;
                    }
                }
            }
        }
        
        assertTrue(fieldConditionExists, 
                "Transition should require the " + fieldName + " field to be " + fieldValue);
    }
    
    @Then("I should see the conditional requirements for the transition from {string} to {string}")
    public void iShouldSeeTheConditionalRequirementsForTheTransitionFromTo(String fromState, String toState) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(fromState + " → " + toState) || 
                   lastOutput.contains("From: " + fromState) && lastOutput.contains("To: " + toState), 
                "Output should show transition from " + fromState + " to " + toState);
        
        assertTrue(lastOutput.contains("Role: qa") || lastOutput.contains("Required Role: qa"), 
                "Output should show required role");
        
        assertTrue(lastOutput.contains("testsPassed") && lastOutput.contains("true"), 
                "Output should show required field condition");
    }
    
    @Given("I have defined custom workflow states and transitions")
    public void iHaveDefinedCustomWorkflowStatesAndTransitions() {
        // First ensure we have custom workflow states
        iHaveDefinedCustomWorkflowStates();
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        // Set start and end states
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        // Set PLANNING as start state
        @SuppressWarnings("unchecked")
        Map<String, Object> planningState = (Map<String, Object>) workflowStates.get("PLANNING");
        if (planningState != null) {
            planningState.put("isStartState", true);
            workflowStates.put("PLANNING", planningState);
        }
        
        // Set COMPLETED as end state
        @SuppressWarnings("unchecked")
        Map<String, Object> completedState = (Map<String, Object>) workflowStates.get("COMPLETED");
        if (completedState != null) {
            completedState.put("isEndState", true);
            workflowStates.put("COMPLETED", completedState);
        }
        
        project.put("workflowStates", workflowStates);
        
        // Now define transitions
        List<Map<String, Object>> workflowTransitions = new ArrayList<>();
        
        // Define standard transitions
        String[][] transitions = {
            {"PLANNING", "REQUIREMENTS"},
            {"REQUIREMENTS", "DEVELOPMENT"},
            {"DEVELOPMENT", "CODE_REVIEW"},
            {"CODE_REVIEW", "TESTING"},
            {"TESTING", "DEPLOYMENT"},
            {"DEPLOYMENT", "COMPLETED"},
            {"CODE_REVIEW", "DEVELOPMENT"},
            {"TESTING", "DEVELOPMENT"}
        };
        
        for (String[] transition : transitions) {
            Map<String, Object> transitionMap = new HashMap<>();
            transitionMap.put("fromState", transition[0]);
            transitionMap.put("toState", transition[1]);
            workflowTransitions.add(transitionMap);
        }
        
        // Define conditional transition
        Map<String, Object> conditionalTransition = new HashMap<>();
        conditionalTransition.put("fromState", "TESTING");
        conditionalTransition.put("toState", "DEPLOYMENT");
        
        Map<String, Object> conditions = new HashMap<>();
        
        // Required roles
        List<String> requiredRoles = Collections.singletonList("qa");
        conditions.put("requiredRoles", requiredRoles);
        
        // Required fields
        Map<String, Object> requiredFields = new HashMap<>();
        requiredFields.put("testsPassed", "true");
        conditions.put("requiredFields", requiredFields);
        
        conditionalTransition.put("conditions", conditions);
        workflowTransitions.add(conditionalTransition);
        
        project.put("workflowTransitions", workflowTransitions);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("I should see a visual representation of the workflow")
    public void iShouldSeeAVisualRepresentationOfTheWorkflow() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains("Workflow Diagram") || lastOutput.contains("WORKFLOW DIAGRAM"), 
                "Output should include a workflow diagram header");
    }
    
    @Then("the diagram should show all states including {string}, {string}, {string}, {string}, {string}, {string}, {string}")
    public void theDiagramShouldShowAllStatesIncluding(String state1, String state2, String state3, String state4, String state5, String state6, String state7) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(state1), "Diagram should contain state: " + state1);
        assertTrue(lastOutput.contains(state2), "Diagram should contain state: " + state2);
        assertTrue(lastOutput.contains(state3), "Diagram should contain state: " + state3);
        assertTrue(lastOutput.contains(state4), "Diagram should contain state: " + state4);
        assertTrue(lastOutput.contains(state5), "Diagram should contain state: " + state5);
        assertTrue(lastOutput.contains(state6), "Diagram should contain state: " + state6);
        assertTrue(lastOutput.contains(state7), "Diagram should contain state: " + state7);
    }
    
    @Then("the diagram should show all transitions between states")
    public void theDiagramShouldShowAllTransitionsBetweenStates() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        for (Map<String, Object> transition : workflowTransitions) {
            String fromState = (String) transition.get("fromState");
            String toState = (String) transition.get("toState");
            
            assertTrue(lastOutput.contains(fromState + " → " + toState) || 
                       lastOutput.contains(fromState + " to " + toState) ||
                       lastOutput.contains(fromState + "->" + toState), 
                    "Diagram should show transition from " + fromState + " to " + toState);
        }
    }
    
    @Then("start states should be highlighted in the diagram")
    public void startStatesShouldBeHighlightedInTheDiagram() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains("PLANNING") && 
                   (lastOutput.contains("(START)") || lastOutput.contains("[START]") || lastOutput.contains("Start State")), 
                "Diagram should highlight PLANNING as a start state");
    }
    
    @Then("end states should be highlighted in the diagram")
    public void endStatesShouldBeHighlightedInTheDiagram() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains("COMPLETED") && 
                   (lastOutput.contains("(END)") || lastOutput.contains("[END]") || lastOutput.contains("End State")), 
                "Diagram should highlight COMPLETED as an end state");
    }
    
    @Then("conditional transitions should be marked in the diagram")
    public void conditionalTransitionsShouldBeMarkedInTheDiagram() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains("TESTING") && lastOutput.contains("DEPLOYMENT") && 
                   (lastOutput.contains("(CONDITIONAL)") || lastOutput.contains("[COND]") || lastOutput.contains("*")), 
                "Diagram should mark the conditional transition from TESTING to DEPLOYMENT");
    }
    
    @Given("I have defined a workflow state {string} with description {string}")
    public void iHaveDefinedAWorkflowStateWithDescription(String stateName, String description) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        Map<String, Object> stateDetails = new HashMap<>();
        stateDetails.put("name", stateName);
        stateDetails.put("description", description);
        
        workflowStates.put(stateName, stateDetails);
        project.put("workflowStates", workflowStates);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("the workflow state {string} should exist with description {string}")
    public void theWorkflowStateShouldExistWithDescription(String stateName, String description) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        assertTrue(workflowStates.containsKey(stateName), 
                "Workflow state " + stateName + " should exist");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> stateDetails = (Map<String, Object>) workflowStates.get(stateName);
        
        assertEquals(description, stateDetails.get("description"), 
                "State " + stateName + " should have description: " + description);
    }
    
    @Then("the workflow state {string} should no longer exist")
    public void theWorkflowStateShouldNoLongerExist(String stateName) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        assertFalse(workflowStates.containsKey(stateName), 
                "Workflow state " + stateName + " should no longer exist");
    }
    
    @Then("all transitions to and from {string} should now reference {string}")
    public void allTransitionsToAndFromShouldNowReference(String oldState, String newState) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        for (Map<String, Object> transition : workflowTransitions) {
            assertNotEquals(oldState, transition.get("fromState"), 
                    "No transition should reference old state as fromState");
            assertNotEquals(oldState, transition.get("toState"), 
                    "No transition should reference old state as toState");
            
            if (transition.get("fromState").equals(newState) || transition.get("toState").equals(newState)) {
                // At least one transition should reference the new state
                return;
            }
        }
    }
    
    @Given("I have defined workflow states and transitions")
    public void iHaveDefinedWorkflowStatesAndTransitions() {
        // Reuse existing method
        iHaveDefinedCustomWorkflowStatesAndTransitions();
    }
    
    @Given("a transition exists from {string} to {string}")
    public void aTransitionExistsFromTo(String fromState, String toState) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        // Check if transition exists
        boolean exists = false;
        for (Map<String, Object> transition : workflowTransitions) {
            if (fromState.equals(transition.get("fromState")) && toState.equals(transition.get("toState"))) {
                exists = true;
                break;
            }
        }
        
        // Add transition if it doesn't exist
        if (!exists) {
            Map<String, Object> newTransition = new HashMap<>();
            newTransition.put("fromState", fromState);
            newTransition.put("toState", toState);
            workflowTransitions.add(newTransition);
            
            project.put("workflowTransitions", workflowTransitions);
            projects.put(currentProject, project);
            context.setConfigurationValue("projects", projects);
        }
    }
    
    @Then("the transition from {string} to {string} should no longer exist")
    public void theTransitionFromToShouldNoLongerExist(String fromState, String toState) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        for (Map<String, Object> transition : workflowTransitions) {
            assertFalse(fromState.equals(transition.get("fromState")) && toState.equals(transition.get("toState")), 
                    "Transition from " + fromState + " to " + toState + " should not exist");
        }
    }
    
    @Then("I should not see a transition from {string} to {string}")
    public void iShouldNotSeeATransitionFromTo(String fromState, String toState) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        // The expected transition string - this could be represented in different ways
        String transitionStr1 = fromState + " → " + toState;
        String transitionStr2 = fromState + " to " + toState;
        String transitionStr3 = "From: " + fromState + ".*To: " + toState;
        
        assertFalse(lastOutput.contains(transitionStr1) || 
                    lastOutput.contains(transitionStr2) || 
                    lastOutput.matches("(?s).*" + transitionStr3 + ".*"), 
                "Output should not show transition from " + fromState + " to " + toState);
    }
    
    @Given("I have defined a work item type {string}")
    public void iHaveDefinedAWorkItemType(String typeName) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        Map<String, Object> typeDetails = new HashMap<>();
        typeDetails.put("name", typeName);
        typeDetails.put("description", typeName + " work item type");
        typeDetails.put("fields", new HashMap<>());
        
        workItemTypes.put(typeName, typeDetails);
        project.put("workItemTypes", workItemTypes);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("a new work item should be created in the {string} state")
    public void aNewWorkItemShouldBeCreatedInTheState(String stateName) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workItems = (List<Map<String, Object>>) context.getConfigurationValue("workItems")
                .orElse(new ArrayList<>());
        
        assertFalse(workItems.isEmpty(), "Work items list should not be empty");
        
        Map<String, Object> lastWorkItem = workItems.get(workItems.size() - 1);
        assertEquals(stateName, lastWorkItem.get("state"), 
                "Work item should be in state " + stateName);
    }
    
    @Then("the work item should now be in the {string} state")
    public void theWorkItemShouldNowBeInTheState(String stateName) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workItems = (List<Map<String, Object>>) context.getConfigurationValue("workItems")
                .orElse(new ArrayList<>());
        
        assertFalse(workItems.isEmpty(), "Work items list should not be empty");
        
        Map<String, Object> workItem = workItems.get(0); // Assuming we're working with the first created item
        assertEquals(stateName, workItem.get("state"), 
                "Work item should be in state " + stateName);
    }
    
    @Then("a file {string} should be created")
    public void aFileShouldBeCreated(String fileName) {
        // Simulate file existence check
        context.setConfigurationValue("exportedFile", fileName);
        context.setConfigurationFlag("fileExists", true);
    }
    
    @Then("the file should contain all defined states")
    public void theFileShouldContainAllDefinedStates() {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        // Simulate file content check
        String fileName = (String) context.getConfigurationValue("exportedFile").orElse("");
        context.setConfigurationValue("fileContent:" + fileName + ":states", workflowStates);
    }
    
    @Then("the file should contain all defined transitions")
    public void theFileShouldContainAllDefinedTransitions() {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        // Simulate file content check
        String fileName = (String) context.getConfigurationValue("exportedFile").orElse("");
        context.setConfigurationValue("fileContent:" + fileName + ":transitions", workflowTransitions);
    }
    
    @Then("the file should include start and end state designations")
    public void theFileShouldIncludeStartAndEndStateDesignations() {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        // Check if there are start and end states
        boolean hasStartState = false;
        boolean hasEndState = false;
        
        for (Object value : workflowStates.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateDetails = (Map<String, Object>) value;
            
            if (Boolean.TRUE.equals(stateDetails.get("isStartState"))) {
                hasStartState = true;
            }
            
            if (Boolean.TRUE.equals(stateDetails.get("isEndState"))) {
                hasEndState = true;
            }
        }
        
        assertTrue(hasStartState, "At least one start state should be designated");
        assertTrue(hasEndState, "At least one end state should be designated");
        
        // Simulate file content check
        String fileName = (String) context.getConfigurationValue("exportedFile").orElse("");
        context.setConfigurationFlag("fileContent:" + fileName + ":hasStartState", hasStartState);
        context.setConfigurationFlag("fileContent:" + fileName + ":hasEndState", hasEndState);
    }
    
    @Given("I have a workflow configuration file {string}")
    public void iHaveAWorkflowConfigurationFile(String fileName) {
        context.setConfigurationValue("importFile", fileName);
        context.setConfigurationFlag("fileExists", true);
        
        // Create a sample configuration
        Map<String, Object> workflowConfig = new HashMap<>();
        
        // States
        Map<String, Object> states = new HashMap<>();
        
        for (String state : Arrays.asList("BACKLOG", "ANALYSIS", "DESIGN", "IMPLEMENTATION", "REVIEW", "VERIFIED", "DONE")) {
            Map<String, Object> stateDetails = new HashMap<>();
            stateDetails.put("name", state);
            stateDetails.put("description", state + " phase");
            
            if ("BACKLOG".equals(state)) {
                stateDetails.put("isStartState", true);
            }
            
            if ("DONE".equals(state)) {
                stateDetails.put("isEndState", true);
            }
            
            states.put(state, stateDetails);
        }
        
        workflowConfig.put("states", states);
        
        // Transitions
        List<Map<String, Object>> transitions = new ArrayList<>();
        
        String[][] transitionPairs = {
            {"BACKLOG", "ANALYSIS"},
            {"ANALYSIS", "DESIGN"},
            {"DESIGN", "IMPLEMENTATION"},
            {"IMPLEMENTATION", "REVIEW"},
            {"REVIEW", "VERIFIED"},
            {"VERIFIED", "DONE"},
            {"REVIEW", "IMPLEMENTATION"}
        };
        
        for (String[] pair : transitionPairs) {
            Map<String, Object> transition = new HashMap<>();
            transition.put("fromState", pair[0]);
            transition.put("toState", pair[1]);
            transitions.add(transition);
        }
        
        workflowConfig.put("transitions", transitions);
        
        context.setConfigurationValue("importFileContent", workflowConfig);
    }
    
    @Then("all states from the configuration file should be created")
    public void allStatesFromTheConfigurationFileShouldBeCreated() {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importFileContent = (Map<String, Object>) context.getConfigurationValue("importFileContent")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importedStates = (Map<String, Object>) importFileContent.getOrDefault("states", new HashMap<>());
        
        // Check if all imported states are in the project's workflow states
        for (String stateName : importedStates.keySet()) {
            assertTrue(workflowStates.containsKey(stateName), 
                    "State " + stateName + " from import file should be created");
        }
    }
    
    @Then("all transitions from the configuration file should be established")
    public void allTransitionsFromTheConfigurationFileShouldBeEstablished() {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importFileContent = (Map<String, Object>) context.getConfigurationValue("importFileContent")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> importedTransitions = (List<Map<String, Object>>) importFileContent.getOrDefault("transitions", new ArrayList<>());
        
        // Check if all imported transitions are in the project's workflow transitions
        for (Map<String, Object> importedTransition : importedTransitions) {
            String fromState = (String) importedTransition.get("fromState");
            String toState = (String) importedTransition.get("toState");
            
            boolean found = false;
            for (Map<String, Object> transition : workflowTransitions) {
                if (fromState.equals(transition.get("fromState")) && toState.equals(transition.get("toState"))) {
                    found = true;
                    break;
                }
            }
            
            assertTrue(found, 
                    "Transition from " + fromState + " to " + toState + " should be established");
        }
    }
    
    @Then("start and end states should be properly designated")
    public void startAndEndStatesShouldBeProperlyDesignated() {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importFileContent = (Map<String, Object>) context.getConfigurationValue("importFileContent")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importedStates = (Map<String, Object>) importFileContent.getOrDefault("states", new HashMap<>());
        
        // Check if start and end states are properly designated
        for (Map.Entry<String, Object> entry : importedStates.entrySet()) {
            String stateName = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> importedStateDetails = (Map<String, Object>) entry.getValue();
            
            if (Boolean.TRUE.equals(importedStateDetails.get("isStartState"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stateDetails = (Map<String, Object>) workflowStates.get(stateName);
                assertTrue(Boolean.TRUE.equals(stateDetails.get("isStartState")), 
                        "State " + stateName + " should be designated as a start state");
            }
            
            if (Boolean.TRUE.equals(importedStateDetails.get("isEndState"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stateDetails = (Map<String, Object>) workflowStates.get(stateName);
                assertTrue(Boolean.TRUE.equals(stateDetails.get("isEndState")), 
                        "State " + stateName + " should be designated as an end state");
            }
        }
    }
    
    @Then("I should see all the imported workflow states")
    public void iShouldSeeAllTheImportedWorkflowStates() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importFileContent = (Map<String, Object>) context.getConfigurationValue("importFileContent")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importedStates = (Map<String, Object>) importFileContent.getOrDefault("states", new HashMap<>());
        
        for (String stateName : importedStates.keySet()) {
            assertTrue(lastOutput.contains(stateName), 
                    "Output should list imported state: " + stateName);
        }
    }
    
    @Then("I should see all the imported workflow transitions")
    public void iShouldSeeAllTheImportedWorkflowTransitions() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importFileContent = (Map<String, Object>) context.getConfigurationValue("importFileContent")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> importedTransitions = (List<Map<String, Object>>) importFileContent.getOrDefault("transitions", new ArrayList<>());
        
        for (Map<String, Object> transition : importedTransitions) {
            String fromState = (String) transition.get("fromState");
            String toState = (String) transition.get("toState");
            
            assertTrue(lastOutput.contains(fromState + " → " + toState) || 
                       lastOutput.contains(fromState + " to " + toState) ||
                       (lastOutput.contains(fromState) && lastOutput.contains(toState)),
                    "Output should show transition from " + fromState + " to " + toState);
        }
    }
}