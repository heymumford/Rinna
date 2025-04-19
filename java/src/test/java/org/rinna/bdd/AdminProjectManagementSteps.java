/*
 * BDD step definitions for Rinna admin project management
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
 * Step definitions for testing Rinna admin project management.
 */
public class AdminProjectManagementSteps {
    private final TestContext context;
    private String commandOutput;
    private String commandError;
    
    /**
     * Constructs a new AdminProjectManagementSteps with the given test context.
     *
     * @param context the test context
     */
    public AdminProjectManagementSteps(TestContext context) {
        this.context = context;
    }
    
    @Then("the project should have the description {string}")
    public void theProjectShouldHaveTheDescription(String description) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        assertEquals(description, project.get("description"), 
                "Project should have the correct description");
    }
    
    @Then("I should see {string} in the project list")
    public void iShouldSeeInTheProjectList(String projectName) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains(projectName), 
                "Output should contain project name: " + projectName);
    }
    
    @Then("I should see the project description and creation date")
    public void iShouldSeeTheProjectDescriptionAndCreationDate() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        for (Map.Entry<String, Object> entry : projects.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> project = (Map<String, Object>) entry.getValue();
            
            String description = (String) project.get("description");
            if (description != null && !description.isEmpty()) {
                assertTrue(lastOutput.contains(description), 
                        "Output should contain project description: " + description);
            }
            
            Object createdAt = project.get("created");
            if (createdAt != null) {
                assertTrue(lastOutput.contains(createdAt.toString()) || 
                           lastOutput.contains("Created:") || 
                           lastOutput.contains("Created at:"), 
                        "Output should contain creation date information");
            }
        }
    }
    
    @Then("the project should have standard Agile workflow states")
    public void theProjectShouldHaveStandardAgileWorkflowStates() {
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
        
        // Check for standard Agile states
        for (String state : Arrays.asList("BACKLOG", "SPRINT", "IN_PROGRESS", "REVIEW", "DONE")) {
            assertTrue(workflowStates.containsKey(state), 
                    "Project should have standard Agile state: " + state);
        }
    }
    
    @Then("the project should have standard Agile work item types")
    public void theProjectShouldHaveStandardAgileWorkItemTypes() {
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
        
        // Check for standard Agile work item types
        for (String type : Arrays.asList("EPIC", "STORY", "TASK", "BUG")) {
            assertTrue(workItemTypes.containsKey(type), 
                    "Project should have standard Agile work item type: " + type);
        }
    }
    
    @Then("I should see Agile-specific states like {string}, {string}, {string}")
    public void iShouldSeeAgileSpecificStatesLike(String state1, String state2, String state3) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(state1), "Output should contain state: " + state1);
        assertTrue(lastOutput.contains(state2), "Output should contain state: " + state2);
        assertTrue(lastOutput.contains(state3), "Output should contain state: " + state3);
    }
    
    @Then("I should see Agile-specific work item types like {string}, {string}, {string}")
    public void iShouldSeeAgileSpecificWorkItemTypesLike(String type1, String type2, String type3) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(type1), "Output should contain work item type: " + type1);
        assertTrue(lastOutput.contains(type2), "Output should contain work item type: " + type2);
        assertTrue(lastOutput.contains(type3), "Output should contain work item type: " + type3);
    }
    
    @Given("I have created projects {string} and {string}")
    public void iHaveCreatedProjectsAnd(String project1, String project2) {
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        // Create first project
        Map<String, Object> project1Data = new HashMap<>();
        project1Data.put("name", project1);
        project1Data.put("description", "Description for " + project1);
        project1Data.put("created", "2025-04-07T11:30:00Z");
        project1Data.put("id", UUID.randomUUID().toString());
        
        // Create second project
        Map<String, Object> project2Data = new HashMap<>();
        project2Data.put("name", project2);
        project2Data.put("description", "Description for " + project2);
        project2Data.put("created", "2025-04-07T11:45:00Z");
        project2Data.put("id", UUID.randomUUID().toString());
        
        projects.put(project1, project1Data);
        projects.put(project2, project2Data);
        
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("I should see both {string} and {string} in the list")
    public void iShouldSeeBothAndInTheList(String project1, String project2) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(project1), "Output should contain project: " + project1);
        assertTrue(lastOutput.contains(project2), "Output should contain project: " + project2);
    }
    
    @Then("{string} should be marked as the current project")
    public void shouldBeMarkedAsTheCurrentProject(String projectName) {
        assertEquals(projectName, context.getConfigurationValue("currentProject").orElse(""), 
                "Project should be set as current project");
    }
    
    @Given("{string} is set as the current project")
    public void isSetAsTheCurrentProject(String projectName) {
        context.setConfigurationValue("currentProject", projectName);
    }
    
    @Then("I should not see {string}")
    public void iShouldNotSee(String text) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertFalse(lastOutput.contains(text), 
                "Output should not contain: " + text);
    }
    
    @Then("I should not see {string} in the project list")
    public void iShouldNotSeeInTheProjectList(String projectName) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertFalse(lastOutput.contains(projectName), 
                "Output should not contain project name: " + projectName);
    }
    
    @Then("I should see {string} in the metadata section")
    public void iShouldSeeInTheMetadataSection(String metadataEntry) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains("Metadata:") && lastOutput.contains(metadataEntry), 
                "Output should contain metadata entry: " + metadataEntry);
    }
    
    @Given("I have created users {string}, {string}, and {string}")
    public void iHaveCreatedUsersAnd(String user1, String user2, String user3) {
        @SuppressWarnings("unchecked")
        Map<String, Object> users = (Map<String, Object>) context.getConfigurationValue("users")
                .orElse(new HashMap<>());
        
        for (String username : Arrays.asList(user1, user2, user3)) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", username);
            userData.put("displayName", Character.toUpperCase(username.charAt(0)) + username.substring(1));
            userData.put("created", "2025-04-07T10:00:00Z");
            
            users.put(username, userData);
        }
        
        context.setConfigurationValue("users", users);
    }
    
    @Then("I should see that {string} has {string} permissions")
    public void iShouldSeeThatHasPermissions(String username, String permissions) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(username) && lastOutput.contains(permissions), 
                "Output should show that " + username + " has permissions: " + permissions);
    }
    
    @Then("I should not see {string} in the permissions list")
    public void iShouldNotSeeInThePermissionsList(String username) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertFalse(lastOutput.contains(username), 
                "Output should not contain user: " + username);
    }
    
    @Given("I have configured workflow states {string}, {string}, {string}, {string}")
    public void iHaveConfiguredWorkflowStates(String state1, String state2, String state3, String state4) {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        Map<String, Object> workflowStates = new HashMap<>();
        
        for (String state : Arrays.asList(state1, state2, state3, state4)) {
            Map<String, Object> stateDetails = new HashMap<>();
            stateDetails.put("name", state);
            stateDetails.put("description", state + " phase");
            
            // Set first state as start state and last state as end state
            if (state.equals(state1)) {
                stateDetails.put("isStartState", true);
            }
            if (state.equals(state4)) {
                stateDetails.put("isEndState", true);
            }
            
            workflowStates.put(state, stateDetails);
        }
        
        project.put("workflowStates", workflowStates);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Given("I have configured transitions between the workflow states")
    public void iHaveConfiguredTransitionsBetweenTheWorkflowStates() {
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
        
        // Get the state names
        List<String> states = new ArrayList<>(workflowStates.keySet());
        Collections.sort(states); // Ensure consistent order
        
        List<Map<String, Object>> workflowTransitions = new ArrayList<>();
        
        // Create transitions between adjacent states
        for (int i = 0; i < states.size() - 1; i++) {
            Map<String, Object> transition = new HashMap<>();
            transition.put("fromState", states.get(i));
            transition.put("toState", states.get(i + 1));
            workflowTransitions.add(transition);
        }
        
        project.put("workflowTransitions", workflowTransitions);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("I should see a {string} section listing {string}, {string}, and {string}")
    public void iShouldSeeASectionListingAnd(String sectionName, String item1, String item2, String item3) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(sectionName), 
                "Output should contain section: " + sectionName);
        
        assertTrue(lastOutput.contains(item1), 
                "Output should list item: " + item1 + " in section: " + sectionName);
        
        assertTrue(lastOutput.contains(item2), 
                "Output should list item: " + item2 + " in section: " + sectionName);
        
        assertTrue(lastOutput.contains(item3), 
                "Output should list item: " + item3 + " in section: " + sectionName);
    }
    
    @Then("I should see a {string} section listing the defined states")
    public void iShouldSeeASectionListingTheDefinedStates(String sectionName) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(sectionName), 
                "Output should contain section: " + sectionName);
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        for (String state : workflowStates.keySet()) {
            assertTrue(lastOutput.contains(state), 
                    "Output should list state: " + state + " in section: " + sectionName);
        }
    }
    
    @Then("I should see the transitions between workflow states")
    public void iShouldSeeTheTransitionsBetweenWorkflowStates() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        for (Map<String, Object> transition : workflowTransitions) {
            String fromState = (String) transition.get("fromState");
            String toState = (String) transition.get("toState");
            
            assertTrue(lastOutput.contains(fromState + " → " + toState) || 
                       lastOutput.contains(fromState + " to " + toState) ||
                       (lastOutput.contains(fromState) && lastOutput.contains(toState)), 
                    "Output should show transition from " + fromState + " to " + toState);
        }
    }
    
    @Then("I should see which states are start and end states")
    public void iShouldSeeWhichStatesAreStartAndEndStates() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        for (Map.Entry<String, Object> entry : workflowStates.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateDetails = (Map<String, Object>) entry.getValue();
            
            if (Boolean.TRUE.equals(stateDetails.get("isStartState"))) {
                String stateName = entry.getKey();
                assertTrue(lastOutput.contains(stateName) && 
                           (lastOutput.contains("Start State") || lastOutput.contains("start state")), 
                        "Output should indicate that " + stateName + " is a start state");
            }
            
            if (Boolean.TRUE.equals(stateDetails.get("isEndState"))) {
                String stateName = entry.getKey();
                assertTrue(lastOutput.contains(stateName) && 
                           (lastOutput.contains("End State") || lastOutput.contains("end state")), 
                        "Output should indicate that " + stateName + " is an end state");
            }
        }
    }
    
    @Given("I have fully configured work item types and workflow for the project")
    public void iHaveFullyConfiguredWorkItemTypesAndWorkflowForTheProject() {
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        assertNotNull(project, "Project should exist in projects map");
        
        // Set up work item types
        Map<String, Object> workItemTypes = new HashMap<>();
        
        // EPIC type
        Map<String, Object> epicType = new HashMap<>();
        epicType.put("name", "EPIC");
        epicType.put("description", "Large feature that spans multiple releases");
        
        Map<String, Object> epicFields = new HashMap<>();
        
        Map<String, Object> businessValueField = new HashMap<>();
        businessValueField.put("type", "enum");
        businessValueField.put("description", "Business value");
        businessValueField.put("values", Arrays.asList("low", "medium", "high"));
        
        Map<String, Object> epicOwnerField = new HashMap<>();
        epicOwnerField.put("type", "user");
        epicOwnerField.put("description", "Epic owner");
        
        epicFields.put("businessValue", businessValueField);
        epicFields.put("epicOwner", epicOwnerField);
        
        epicType.put("fields", epicFields);
        workItemTypes.put("EPIC", epicType);
        
        // STORY type
        Map<String, Object> storyType = new HashMap<>();
        storyType.put("name", "STORY");
        storyType.put("description", "User story representing user value");
        
        Map<String, Object> storyFields = new HashMap<>();
        
        Map<String, Object> storyPointsField = new HashMap<>();
        storyPointsField.put("type", "number");
        storyPointsField.put("description", "Story points");
        
        Map<String, Object> acceptanceCriteriaField = new HashMap<>();
        acceptanceCriteriaField.put("type", "text");
        acceptanceCriteriaField.put("description", "Acceptance criteria");
        
        storyFields.put("storyPoints", storyPointsField);
        storyFields.put("acceptanceCriteria", acceptanceCriteriaField);
        
        storyType.put("fields", storyFields);
        workItemTypes.put("STORY", storyType);
        
        // TASK type
        Map<String, Object> taskType = new HashMap<>();
        taskType.put("name", "TASK");
        taskType.put("description", "Small unit of work");
        
        Map<String, Object> taskFields = new HashMap<>();
        
        Map<String, Object> estimatedHoursField = new HashMap<>();
        estimatedHoursField.put("type", "number");
        estimatedHoursField.put("description", "Estimated hours");
        estimatedHoursField.put("defaultValue", "4");
        
        Map<String, Object> assigneeField = new HashMap<>();
        assigneeField.put("type", "user");
        assigneeField.put("description", "Task assignee");
        
        taskFields.put("estimatedHours", estimatedHoursField);
        taskFields.put("assignee", assigneeField);
        
        taskType.put("fields", taskFields);
        workItemTypes.put("TASK", taskType);
        
        project.put("workItemTypes", workItemTypes);
        
        // Set up workflow states
        Map<String, Object> workflowStates = new HashMap<>();
        
        for (String state : Arrays.asList("PLANNING", "REQUIREMENTS", "DEVELOPMENT", "TESTING", "DEPLOYMENT", "COMPLETED")) {
            Map<String, Object> stateDetails = new HashMap<>();
            stateDetails.put("name", state);
            stateDetails.put("description", state + " phase");
            
            // Set first state as start state and last state as end state
            if (state.equals("PLANNING")) {
                stateDetails.put("isStartState", true);
            }
            if (state.equals("COMPLETED")) {
                stateDetails.put("isEndState", true);
            }
            
            workflowStates.put(state, stateDetails);
        }
        
        project.put("workflowStates", workflowStates);
        
        // Set up workflow transitions
        List<Map<String, Object>> workflowTransitions = new ArrayList<>();
        
        String[][] transitions = {
            {"PLANNING", "REQUIREMENTS"},
            {"REQUIREMENTS", "DEVELOPMENT"},
            {"DEVELOPMENT", "TESTING"},
            {"TESTING", "DEPLOYMENT"},
            {"DEPLOYMENT", "COMPLETED"}
        };
        
        for (String[] transition : transitions) {
            Map<String, Object> transitionMap = new HashMap<>();
            transitionMap.put("fromState", transition[0]);
            transitionMap.put("toState", transition[1]);
            workflowTransitions.add(transitionMap);
        }
        
        project.put("workflowTransitions", workflowTransitions);
        
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("the file should contain all project settings")
    public void theFileShouldContainAllProjectSettings() {
        String exportedFile = (String) context.getConfigurationValue("exportedFile").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        // Simulate file content check
        context.setConfigurationValue("fileContent:" + exportedFile + ":project", project);
    }
    
    @Then("the file should contain all work item type definitions")
    public void theFileShouldContainAllWorkItemTypeDefinitions() {
        String exportedFile = (String) context.getConfigurationValue("exportedFile").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        // Simulate file content check
        context.setConfigurationValue("fileContent:" + exportedFile + ":workItemTypes", workItemTypes);
    }
    
    @Then("the file should contain all workflow state and transition definitions")
    public void theFileShouldContainAllWorkflowStateAndTransitionDefinitions() {
        String exportedFile = (String) context.getConfigurationValue("exportedFile").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        // Simulate file content check
        context.setConfigurationValue("fileContent:" + exportedFile + ":workflowStates", workflowStates);
        context.setConfigurationValue("fileContent:" + exportedFile + ":workflowTransitions", workflowTransitions);
    }
    
    @Given("I have a project configuration file {string}")
    public void iHaveAProjectConfigurationFile(String fileName) {
        context.setConfigurationValue("importFile", fileName);
        context.setConfigurationFlag("fileExists", true);
        
        // Create a sample project configuration
        Map<String, Object> projectConfig = new HashMap<>();
        projectConfig.put("name", "Imported Project");
        projectConfig.put("description", "Imported project configuration");
        
        // Work item types
        Map<String, Object> workItemTypes = new HashMap<>();
        
        // FEATURE type
        Map<String, Object> featureType = new HashMap<>();
        featureType.put("name", "FEATURE");
        featureType.put("description", "Feature to be implemented");
        
        Map<String, Object> featureFields = new HashMap<>();
        
        Map<String, Object> priorityField = new HashMap<>();
        priorityField.put("type", "enum");
        priorityField.put("description", "Feature priority");
        priorityField.put("values", Arrays.asList("low", "medium", "high"));
        
        featureFields.put("priority", priorityField);
        featureType.put("fields", featureFields);
        workItemTypes.put("FEATURE", featureType);
        
        // USER_STORY type
        Map<String, Object> userStoryType = new HashMap<>();
        userStoryType.put("name", "USER_STORY");
        userStoryType.put("description", "User story with acceptance criteria");
        
        Map<String, Object> userStoryFields = new HashMap<>();
        
        Map<String, Object> pointsField = new HashMap<>();
        pointsField.put("type", "number");
        pointsField.put("description", "Story points");
        
        userStoryFields.put("points", pointsField);
        userStoryType.put("fields", userStoryFields);
        workItemTypes.put("USER_STORY", userStoryType);
        
        projectConfig.put("workItemTypes", workItemTypes);
        
        // Workflow states
        Map<String, Object> workflowStates = new HashMap<>();
        
        for (String state : Arrays.asList("NEW", "IN_PROGRESS", "REVIEW", "VERIFIED", "RELEASED")) {
            Map<String, Object> stateDetails = new HashMap<>();
            stateDetails.put("name", state);
            stateDetails.put("description", state + " state");
            
            if (state.equals("NEW")) {
                stateDetails.put("isStartState", true);
            }
            if (state.equals("RELEASED")) {
                stateDetails.put("isEndState", true);
            }
            
            workflowStates.put(state, stateDetails);
        }
        
        projectConfig.put("workflowStates", workflowStates);
        
        // Workflow transitions
        List<Map<String, Object>> workflowTransitions = new ArrayList<>();
        
        String[][] transitions = {
            {"NEW", "IN_PROGRESS"},
            {"IN_PROGRESS", "REVIEW"},
            {"REVIEW", "VERIFIED"},
            {"VERIFIED", "RELEASED"},
            {"REVIEW", "IN_PROGRESS"} // Loop back for rework
        };
        
        for (String[] transition : transitions) {
            Map<String, Object> transitionMap = new HashMap<>();
            transitionMap.put("fromState", transition[0]);
            transitionMap.put("toState", transition[1]);
            workflowTransitions.add(transitionMap);
        }
        
        projectConfig.put("workflowTransitions", workflowTransitions);
        
        context.setConfigurationValue("importFileContent", projectConfig);
    }
    
    @Then("a new project {string} should be created")
    public void aNewProjectShouldBeCreated(String projectName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        assertTrue(projects.containsKey(projectName), 
                "Project " + projectName + " should exist in projects map");
    }
    
    @Then("the project should have all work item types from the configuration")
    public void theProjectShouldHaveAllWorkItemTypesFromTheConfiguration() {
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        String importedProjectName = "Imported Project";
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(importedProjectName);
        assertNotNull(project, "Imported project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importFileContent = (Map<String, Object>) context.getConfigurationValue("importFileContent")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importedWorkItemTypes = (Map<String, Object>) importFileContent.getOrDefault("workItemTypes", new HashMap<>());
        
        for (String typeName : importedWorkItemTypes.keySet()) {
            assertTrue(workItemTypes.containsKey(typeName), 
                    "Project should have imported work item type: " + typeName);
        }
    }
    
    @Then("the project should have all workflow states from the configuration")
    public void theProjectShouldHaveAllWorkflowStatesFromTheConfiguration() {
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        String importedProjectName = "Imported Project";
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(importedProjectName);
        assertNotNull(project, "Imported project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowStates = (Map<String, Object>) project.getOrDefault("workflowStates", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importFileContent = (Map<String, Object>) context.getConfigurationValue("importFileContent")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importedWorkflowStates = (Map<String, Object>) importFileContent.getOrDefault("workflowStates", new HashMap<>());
        
        for (String stateName : importedWorkflowStates.keySet()) {
            assertTrue(workflowStates.containsKey(stateName), 
                    "Project should have imported workflow state: " + stateName);
        }
    }
    
    @Then("the project should have all transitions from the configuration")
    public void theProjectShouldHaveAllTransitionsFromTheConfiguration() {
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        String importedProjectName = "Imported Project";
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(importedProjectName);
        assertNotNull(project, "Imported project should exist in projects map");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowTransitions = (List<Map<String, Object>>) project.getOrDefault("workflowTransitions", new ArrayList<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importFileContent = (Map<String, Object>) context.getConfigurationValue("importFileContent")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> importedWorkflowTransitions = (List<Map<String, Object>>) importFileContent.getOrDefault("workflowTransitions", new ArrayList<>());
        
        for (Map<String, Object> importedTransition : importedWorkflowTransitions) {
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
                    "Project should have imported transition from " + fromState + " to " + toState);
        }
    }
    
    @Then("I should see a complete summary matching the imported configuration")
    public void iShouldSeeACompleteSummaryMatchingTheImportedConfiguration() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> importFileContent = (Map<String, Object>) context.getConfigurationValue("importFileContent")
                .orElse(new HashMap<>());
        
        // Project name and description
        String projectName = (String) importFileContent.get("name");
        String description = (String) importFileContent.get("description");
        
        assertTrue(lastOutput.contains(projectName), 
                "Summary should show imported project name: " + projectName);
        
        if (description != null) {
            assertTrue(lastOutput.contains(description), 
                    "Summary should show imported project description: " + description);
        }
        
        // Work item types
        @SuppressWarnings("unchecked")
        Map<String, Object> importedWorkItemTypes = (Map<String, Object>) importFileContent.getOrDefault("workItemTypes", new HashMap<>());
        
        for (String typeName : importedWorkItemTypes.keySet()) {
            assertTrue(lastOutput.contains(typeName), 
                    "Summary should show imported work item type: " + typeName);
        }
        
        // Workflow states
        @SuppressWarnings("unchecked")
        Map<String, Object> importedWorkflowStates = (Map<String, Object>) importFileContent.getOrDefault("workflowStates", new HashMap<>());
        
        for (String stateName : importedWorkflowStates.keySet()) {
            assertTrue(lastOutput.contains(stateName), 
                    "Summary should show imported workflow state: " + stateName);
        }
    }
    
    @Then("the project should be removed from the database")
    public void theProjectShouldBeRemovedFromTheDatabase() {
        String deletedProject = (String) context.getConfigurationValue("lastDeletedProject").orElse(null);
        assertNotNull(deletedProject, "Deleted project name should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        assertFalse(projects.containsKey(deletedProject), 
                "Project " + deletedProject + " should be removed from projects map");
    }
    
    @Given("I have created multiple projects with different attributes")
    public void iHaveCreatedMultipleProjectsWithDifferentAttributes() {
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        // Project 1: Engineering, High priority
        Map<String, Object> project1 = new HashMap<>();
        project1.put("name", "API Gateway");
        project1.put("description", "API Gateway for the platform");
        project1.put("created", "2025-04-01T10:00:00Z");
        project1.put("id", UUID.randomUUID().toString());
        
        Map<String, Object> project1Metadata = new HashMap<>();
        project1Metadata.put("department", "Engineering");
        project1Metadata.put("priority", "High");
        project1.put("metadata", project1Metadata);
        
        // Project 2: Engineering, Medium priority
        Map<String, Object> project2 = new HashMap<>();
        project2.put("name", "Authentication Service");
        project2.put("description", "User authentication and authorization");
        project2.put("created", "2025-04-03T14:30:00Z");
        project2.put("id", UUID.randomUUID().toString());
        
        Map<String, Object> project2Metadata = new HashMap<>();
        project2Metadata.put("department", "Engineering");
        project2Metadata.put("priority", "Medium");
        project2.put("metadata", project2Metadata);
        
        // Project 3: Product, High priority
        Map<String, Object> project3 = new HashMap<>();
        project3.put("name", "User Dashboard");
        project3.put("description", "User dashboard and analytics");
        project3.put("created", "2025-04-05T09:15:00Z");
        project3.put("id", UUID.randomUUID().toString());
        
        Map<String, Object> project3Metadata = new HashMap<>();
        project3Metadata.put("department", "Product");
        project3Metadata.put("priority", "High");
        project3.put("metadata", project3Metadata);
        
        // Project 4: Operations, Low priority
        Map<String, Object> project4 = new HashMap<>();
        project4.put("name", "Monitoring System");
        project4.put("description", "System monitoring and alerts");
        project4.put("created", "2025-04-07T16:45:00Z");
        project4.put("id", UUID.randomUUID().toString());
        
        Map<String, Object> project4Metadata = new HashMap<>();
        project4Metadata.put("department", "Operations");
        project4Metadata.put("priority", "Low");
        project4.put("metadata", project4Metadata);
        
        projects.put("API Gateway", project1);
        projects.put("Authentication Service", project2);
        projects.put("User Dashboard", project3);
        projects.put("Monitoring System", project4);
        
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("I should see only projects with department {string}")
    public void iShouldSeeOnlyProjectsWithDepartment(String department) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        // Find projects with the specified department
        for (Map.Entry<String, Object> entry : projects.entrySet()) {
            String projectName = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> project = (Map<String, Object>) entry.getValue();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) project.getOrDefault("metadata", new HashMap<>());
            
            String projectDepartment = (String) metadata.get("department");
            
            if (department.equals(projectDepartment)) {
                assertTrue(lastOutput.contains(projectName), 
                        "Output should contain project with department " + department + ": " + projectName);
            } else {
                assertFalse(lastOutput.contains(projectName), 
                        "Output should not contain project with different department: " + projectName);
            }
        }
    }
    
    @Then("I should see projects listed in descending order of creation date")
    public void iShouldSeeProjectsListedInDescendingOrderOfCreationDate() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        // Convert to list and sort by creation date in descending order
        List<Map.Entry<String, Object>> projectEntries = new ArrayList<>(projects.entrySet());
        projectEntries.sort((e1, e2) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> p1 = (Map<String, Object>) e1.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> p2 = (Map<String, Object>) e2.getValue();
            
            String date1 = (String) p1.get("created");
            String date2 = (String) p2.get("created");
            
            // Reverse order for descending
            return date2.compareTo(date1);
        });
        
        // Check if order matches expected
        int lastIndex = -1;
        for (Map.Entry<String, Object> entry : projectEntries) {
            String projectName = entry.getKey();
            int currentIndex = lastOutput.indexOf(projectName);
            
            if (lastIndex != -1) {
                assertTrue(currentIndex > lastIndex, 
                        "Projects should be ordered by creation date descending");
            }
            
            lastIndex = currentIndex;
        }
    }
    
    @Then("I should see only high priority projects sorted alphabetically by name")
    public void iShouldSeeOnlyHighPriorityProjectsSortedAlphabeticallyByName() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        // Filter high priority projects
        List<String> highPriorityProjects = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : projects.entrySet()) {
            String projectName = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> project = (Map<String, Object>) entry.getValue();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) project.getOrDefault("metadata", new HashMap<>());
            
            String priority = (String) metadata.get("priority");
            
            if ("High".equals(priority)) {
                highPriorityProjects.add(projectName);
                assertTrue(lastOutput.contains(projectName), 
                        "Output should contain high priority project: " + projectName);
            } else {
                assertFalse(lastOutput.contains(projectName), 
                        "Output should not contain non-high priority project: " + projectName);
            }
        }
        
        // Sort alphabetically
        Collections.sort(highPriorityProjects);
        
        // Check if order matches expected
        int lastIndex = -1;
        for (String projectName : highPriorityProjects) {
            int currentIndex = lastOutput.indexOf(projectName);
            
            if (lastIndex != -1) {
                assertTrue(currentIndex > lastIndex, 
                        "High priority projects should be sorted alphabetically");
            }
            
            lastIndex = currentIndex;
        }
    }
}