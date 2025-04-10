/*
 * BDD step definitions for Rinna admin work item type configuration
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
 * Step definitions for testing Rinna admin work item type configuration.
 */
public class AdminWorkItemConfigurationSteps {
    private final TestContext context;
    private String commandOutput;
    private String commandError;
    
    /**
     * Constructs a new AdminWorkItemConfigurationSteps with the given test context.
     *
     * @param context the test context
     */
    public AdminWorkItemConfigurationSteps(TestContext context) {
        this.context = context;
    }
    
    @Given("the Rinna server is running")
    public void theRinnaServerIsRunning() {
        context.setConfigurationFlag("serverRunning", true);
    }
    
    @Then("the work item type {string} should be available in the project")
    public void theWorkItemTypeShouldBeAvailableInTheProject(String workItemTypeName) {
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
        
        assertTrue(workItemTypes.containsKey(workItemTypeName), 
                "Work item type " + workItemTypeName + " should be available in the project");
    }
    
    @Then("I should see all {int} work item types listed")
    public void iShouldSeeAllWorkItemTypesListed(Integer count) {
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
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        assertEquals(count.intValue(), workItemTypes.size(), 
                "Should have exactly " + count + " work item types");
        
        for (String typeName : workItemTypes.keySet()) {
            assertTrue(lastOutput.contains(typeName), 
                    "Output should contain work item type: " + typeName);
        }
    }
    
    @Then("each type should show its description")
    public void eachTypeShouldShowItsDescription() {
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
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        for (Map.Entry<String, Object> entry : workItemTypes.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typeDetails = (Map<String, Object>) entry.getValue();
            String description = (String) typeDetails.get("description");
            
            if (description != null && !description.isEmpty()) {
                assertTrue(lastOutput.contains(description), 
                        "Output should contain description for " + entry.getKey() + ": " + description);
            }
        }
    }
    
    @Then("the work item type {string} should have field {string} of type {string}")
    public void theWorkItemTypeShouldHaveFieldOfType(String workItemTypeName, String fieldName, String fieldType) {
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
        
        assertTrue(workItemTypes.containsKey(workItemTypeName), 
                "Work item type " + workItemTypeName + " should exist");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> typeDetails = (Map<String, Object>) workItemTypes.get(workItemTypeName);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) typeDetails.getOrDefault("fields", new HashMap<>());
        
        assertTrue(fields.containsKey(fieldName), 
                "Field " + fieldName + " should exist for work item type " + workItemTypeName);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldDetails = (Map<String, Object>) fields.get(fieldName);
        
        assertEquals(fieldType, fieldDetails.get("type"), 
                "Field " + fieldName + " should be of type " + fieldType);
    }
    
    @Then("the work item type {string} should have field {string} of type {string} with values {string}")
    public void theWorkItemTypeShouldHaveFieldOfTypeWithValues(String workItemTypeName, String fieldName, 
                                                             String fieldType, String valuesString) {
        // First check the field exists and has the correct type
        theWorkItemTypeShouldHaveFieldOfType(workItemTypeName, fieldName, fieldType);
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> typeDetails = (Map<String, Object>) workItemTypes.get(workItemTypeName);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) typeDetails.getOrDefault("fields", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldDetails = (Map<String, Object>) fields.get(fieldName);
        
        // Check for values
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) fieldDetails.get("values");
        assertNotNull(values, "Field " + fieldName + " should have values defined");
        
        String[] expectedValues = valuesString.split(",");
        assertEquals(expectedValues.length, values.size(), 
                "Field " + fieldName + " should have the correct number of values");
        
        for (String value : expectedValues) {
            assertTrue(values.contains(value), 
                    "Field " + fieldName + " should contain value: " + value);
        }
    }
    
    @Given("I have created a work item type {string} with description {string}")
    public void iHaveCreatedAWorkItemTypeWithDescription(String typeName, String description) {
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
        typeDetails.put("description", description);
        typeDetails.put("fields", new HashMap<>());
        
        workItemTypes.put(typeName, typeDetails);
        project.put("workItemTypes", workItemTypes);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("the work item type {string} should have required field {string}")
    public void theWorkItemTypeShouldHaveRequiredField(String workItemTypeName, String fieldName) {
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
        
        assertTrue(workItemTypes.containsKey(workItemTypeName), 
                "Work item type " + workItemTypeName + " should exist");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> typeDetails = (Map<String, Object>) workItemTypes.get(workItemTypeName);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) typeDetails.getOrDefault("fields", new HashMap<>());
        
        assertTrue(fields.containsKey(fieldName), 
                "Field " + fieldName + " should exist for work item type " + workItemTypeName);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldDetails = (Map<String, Object>) fields.get(fieldName);
        
        assertTrue((Boolean) fieldDetails.getOrDefault("required", false), 
                "Field " + fieldName + " should be required");
    }
    
    @Then("I should see that {string} is marked as required")
    public void iShouldSeeThatIsMarkedAsRequired(String fieldName) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains(fieldName) && lastOutput.contains("Required: Yes"), 
                "Output should show that field " + fieldName + " is required");
    }
    
    @Given("I have created a work item type {string} with fields {string} and {string}")
    public void iHaveCreatedAWorkItemTypeWithFieldsAnd(String typeName, String field1, String field2) {
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
        typeDetails.put("description", "Work item type with fields");
        
        Map<String, Object> fields = new HashMap<>();
        
        // Parse field type if provided in format "name:type"
        String field1Name = field1;
        String field1Type = "string";
        if (field1.contains(":")) {
            String[] parts = field1.split(":", 2);
            field1Name = parts[0];
            field1Type = parts[1];
        }
        
        Map<String, Object> field1Details = new HashMap<>();
        field1Details.put("type", field1Type);
        fields.put(field1Name, field1Details);
        
        String field2Name = field2;
        String field2Type = "string";
        if (field2.contains(":")) {
            String[] parts = field2.split(":", 2);
            field2Name = parts[0];
            field2Type = parts[1];
        }
        
        Map<String, Object> field2Details = new HashMap<>();
        field2Details.put("type", field2Type);
        fields.put(field2Name, field2Details);
        
        typeDetails.put("fields", fields);
        
        workItemTypes.put(typeName, typeDetails);
        project.put("workItemTypes", workItemTypes);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("the work item type {string} should have default value {string} for field {string}")
    public void theWorkItemTypeShouldHaveDefaultValueForField(String typeName, String defaultValue, String fieldName) {
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
        
        assertTrue(workItemTypes.containsKey(typeName), 
                "Work item type " + typeName + " should exist");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> typeDetails = (Map<String, Object>) workItemTypes.get(typeName);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) typeDetails.getOrDefault("fields", new HashMap<>());
        
        assertTrue(fields.containsKey(fieldName), 
                "Field " + fieldName + " should exist for work item type " + typeName);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldDetails = (Map<String, Object>) fields.get(fieldName);
        
        assertEquals(defaultValue, fieldDetails.get("defaultValue"), 
                "Field " + fieldName + " should have default value " + defaultValue);
    }
    
    @Then("I should see {string} for field {string}")
    public void iShouldSeeForField(String expectedText, String fieldName) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        // The output format should show fields like "Field: fieldName... Default Value: X"
        assertTrue(lastOutput.contains(fieldName) && lastOutput.contains(expectedText), 
                "Output should show field " + fieldName + " with text " + expectedText);
    }
    
    @Then("a new work item of type {string} should be created in the database")
    public void aNewWorkItemOfTypeShouldBeCreatedInTheDatabase(String typeName) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workItems = (List<Map<String, Object>>) context.getConfigurationValue("workItems")
                .orElse(new ArrayList<>());
        
        assertFalse(workItems.isEmpty(), "Work items list should not be empty");
        
        Map<String, Object> lastWorkItem = workItems.get(workItems.size() - 1);
        assertEquals(typeName, lastWorkItem.get("type"), 
                "Last created work item should be of type " + typeName);
    }
    
    @Then("the work item should have {string} set to {string}")
    public void theWorkItemShouldHaveSetTo(String fieldName, String value) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workItems = (List<Map<String, Object>>) context.getConfigurationValue("workItems")
                .orElse(new ArrayList<>());
        
        assertFalse(workItems.isEmpty(), "Work items list should not be empty");
        
        Map<String, Object> lastWorkItem = workItems.get(workItems.size() - 1);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) lastWorkItem.getOrDefault("fields", new HashMap<>());
        
        assertEquals(value, fields.get(fieldName), 
                "Work item field " + fieldName + " should have value " + value);
    }
    
    @Then("the work item should have {string} containing the specified acceptance criteria")
    public void theWorkItemShouldHaveContainingTheSpecifiedAcceptanceCriteria(String fieldName) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workItems = (List<Map<String, Object>>) context.getConfigurationValue("workItems")
                .orElse(new ArrayList<>());
        
        assertFalse(workItems.isEmpty(), "Work items list should not be empty");
        
        Map<String, Object> lastWorkItem = workItems.get(workItems.size() - 1);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) lastWorkItem.getOrDefault("fields", new HashMap<>());
        
        assertTrue(fields.containsKey(fieldName), "Work item should have field " + fieldName);
        
        String value = (String) fields.get(fieldName);
        assertTrue(value.contains("User should be able to log in"), 
                "Field " + fieldName + " should contain the specified criteria");
    }
    
    @Given("I have created work item types {string}, {string}, and {string}")
    public void iHaveCreatedWorkItemTypesAnd(String type1, String type2, String type3) {
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
        
        for (String typeName : Arrays.asList(type1, type2, type3)) {
            Map<String, Object> typeDetails = new HashMap<>();
            typeDetails.put("name", typeName);
            typeDetails.put("description", "Description for " + typeName);
            typeDetails.put("fields", new HashMap<>());
            
            workItemTypes.put(typeName, typeDetails);
        }
        
        project.put("workItemTypes", workItemTypes);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Given("no items exist with type {string}")
    public void noItemsExistWithType(String typeName) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workItems = (List<Map<String, Object>>) context.getConfigurationValue("workItems")
                .orElse(new ArrayList<>());
        
        // Filter to keep only items that don't have the given type
        List<Map<String, Object>> filteredItems = new ArrayList<>();
        for (Map<String, Object> item : workItems) {
            if (!typeName.equals(item.get("type"))) {
                filteredItems.add(item);
            }
        }
        
        context.setConfigurationValue("workItems", filteredItems);
    }
    
    @Then("the work item type {string} should no longer exist")
    public void theWorkItemTypeShouldNoLongerExist(String typeName) {
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
        
        assertFalse(workItemTypes.containsKey(typeName), 
                "Work item type " + typeName + " should not exist");
    }
    
    @Then("the work item type {string} should exist with the new description")
    public void theWorkItemTypeShouldExistWithTheNewDescription(String typeName) {
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
        
        assertTrue(workItemTypes.containsKey(typeName), 
                "Work item type " + typeName + " should exist");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> typeDetails = (Map<String, Object>) workItemTypes.get(typeName);
        
        assertNotNull(typeDetails.get("description"), 
                "Work item type " + typeName + " should have a description");
        
        assertTrue(((String) typeDetails.get("description")).contains("Software issue requiring resolution"), 
                "Work item type " + typeName + " should have the updated description");
    }
    
    @Then("I should see {string} instead of {string}")
    public void iShouldSeeInsteadOf(String newName, String oldName) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains(newName), 
                "Output should contain new type name: " + newName);
        
        assertFalse(lastOutput.contains(oldName), 
                "Output should not contain old type name: " + oldName);
    }
    
    @Given("I have created work item types with various field configurations")
    public void iHaveCreatedWorkItemTypesWithVariousFieldConfigurations() {
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
        
        // Create STORY type with fields
        Map<String, Object> storyType = new HashMap<>();
        storyType.put("name", "STORY");
        storyType.put("description", "User story representing user value");
        
        Map<String, Object> storyFields = new HashMap<>();
        
        Map<String, Object> storyPointsField = new HashMap<>();
        storyPointsField.put("type", "number");
        storyPointsField.put("description", "Story points representing effort");
        
        Map<String, Object> acceptanceCriteriaField = new HashMap<>();
        acceptanceCriteriaField.put("type", "text");
        acceptanceCriteriaField.put("description", "Acceptance criteria for the story");
        
        Map<String, Object> priorityField = new HashMap<>();
        priorityField.put("type", "enum");
        priorityField.put("description", "Priority level");
        priorityField.put("values", Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        priorityField.put("defaultValue", "MEDIUM");
        
        storyFields.put("storyPoints", storyPointsField);
        storyFields.put("acceptanceCriteria", acceptanceCriteriaField);
        storyFields.put("priority", priorityField);
        
        storyType.put("fields", storyFields);
        workItemTypes.put("STORY", storyType);
        
        // Create more types with different configurations
        Map<String, Object> bugType = new HashMap<>();
        bugType.put("name", "BUG");
        bugType.put("description", "Software defect");
        
        Map<String, Object> bugFields = new HashMap<>();
        
        Map<String, Object> severityField = new HashMap<>();
        severityField.put("type", "enum");
        severityField.put("description", "Bug severity");
        severityField.put("values", Arrays.asList("low", "medium", "high", "critical"));
        severityField.put("required", true);
        
        Map<String, Object> stepsToReproduceField = new HashMap<>();
        stepsToReproduceField.put("type", "text");
        stepsToReproduceField.put("description", "Steps to reproduce the bug");
        
        bugFields.put("severity", severityField);
        bugFields.put("stepsToReproduce", stepsToReproduceField);
        
        bugType.put("fields", bugFields);
        workItemTypes.put("BUG", bugType);
        
        project.put("workItemTypes", workItemTypes);
        projects.put(currentProject, project);
        context.setConfigurationValue("projects", projects);
    }
    
    @Then("I should see detailed information about the {string} type")
    public void iShouldSeeDetailedInformationAboutTheType(String typeName) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        assertTrue(lastOutput.contains("Work Item Type: " + typeName), 
                "Output should contain work item type name");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        assertNotNull(currentProject, "Current project should be set");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> typeDetails = (Map<String, Object>) workItemTypes.get(typeName);
        
        assertTrue(lastOutput.contains((String) typeDetails.get("description")), 
                "Output should contain type description");
    }
    
    @Then("I should see all fields defined for {string} including their types and constraints")
    public void iShouldSeeAllFieldsDefinedForIncludingTheirTypesAndConstraints(String typeName) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> typeDetails = (Map<String, Object>) workItemTypes.get(typeName);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) typeDetails.getOrDefault("fields", new HashMap<>());
        
        for (String fieldName : fields.keySet()) {
            assertTrue(lastOutput.contains(fieldName), 
                    "Output should list field: " + fieldName);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldDetails = (Map<String, Object>) fields.get(fieldName);
            
            String fieldType = (String) fieldDetails.get("type");
            assertTrue(lastOutput.contains(fieldType), 
                    "Output should show field type: " + fieldType);
            
            // Check for required flag if set
            if (Boolean.TRUE.equals(fieldDetails.get("required"))) {
                assertTrue(lastOutput.contains("Required: Yes") || lastOutput.contains("Required"), 
                        "Output should indicate required field");
            }
            
            // Check for default values if set
            if (fieldDetails.containsKey("defaultValue")) {
                String defaultValue = fieldDetails.get("defaultValue").toString();
                assertTrue(lastOutput.contains("Default: " + defaultValue) || 
                           lastOutput.contains("Default Value: " + defaultValue), 
                        "Output should show default value: " + defaultValue);
            }
        }
    }
    
    @Then("I should see a comprehensive list of all work item types")
    public void iShouldSeeAComprehensiveListOfAllWorkItemTypes() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        for (String typeName : workItemTypes.keySet()) {
            assertTrue(lastOutput.contains(typeName), 
                    "Output should list work item type: " + typeName);
        }
    }
    
    @Then("each type should show its fields, validations, and default values")
    public void eachTypeShouldShowItsFieldsValidationsAndDefaultValues() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        
        String currentProject = (String) context.getConfigurationValue("currentProject").orElse(null);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> projects = (Map<String, Object>) context.getConfigurationValue("projects")
                .orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projects.get(currentProject);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> workItemTypes = (Map<String, Object>) project.getOrDefault("workItemTypes", new HashMap<>());
        
        // For each work item type
        for (Map.Entry<String, Object> typeEntry : workItemTypes.entrySet()) {
            String typeName = typeEntry.getKey();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> typeDetails = (Map<String, Object>) typeEntry.getValue();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) typeDetails.getOrDefault("fields", new HashMap<>());
            
            // For verbose listing, at least one field from each type should be mentioned
            if (!fields.isEmpty()) {
                String fieldName = fields.keySet().iterator().next();
                assertTrue(lastOutput.contains(fieldName), 
                        "Output should mention field " + fieldName + " for work item type " + typeName);
            }
            
            // Check for required fields
            for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
                String fieldName = fieldEntry.getKey();
                
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldDetails = (Map<String, Object>) fieldEntry.getValue();
                
                if (Boolean.TRUE.equals(fieldDetails.get("required"))) {
                    assertTrue(lastOutput.contains(fieldName) && 
                            (lastOutput.contains("Required") || lastOutput.contains("required")), 
                            "Output should indicate that field " + fieldName + " is required");
                }
                
                // Check for default values
                if (fieldDetails.containsKey("defaultValue")) {
                    String defaultValue = fieldDetails.get("defaultValue").toString();
                    assertTrue(lastOutput.contains(fieldName) && lastOutput.contains(defaultValue), 
                            "Output should show default value for field " + fieldName);
                }
            }
        }
    }
}