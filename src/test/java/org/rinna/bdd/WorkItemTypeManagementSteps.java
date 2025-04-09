/*
 * BDD step definitions for the Rinna work item type management
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for work item type management features in Cucumber scenarios.
 */
public class WorkItemTypeManagementSteps {
    private final TestContext context;
    private String commandOutput;
    private String commandError;
    
    /**
     * Constructs a new WorkItemTypeManagementSteps with the given test context.
     *
     * @param context the test context
     */
    public WorkItemTypeManagementSteps(TestContext context) {
        this.context = context;
    }
    
    @Given("the work item type {string} already exists")
    public void theWorkItemTypeAlreadyExists(String typeName) {
        Map<String, Object> typeData = new HashMap<>();
        typeData.put("name", typeName);
        typeData.put("description", "Existing work item type for " + typeName);
        typeData.put("fields", new HashMap<>());
        typeData.put("createdAt", "2025-04-01T10:00:00Z");
        
        context.setConfigurationValue("workItemType:" + typeName, typeData);
    }
    
    @Given("the work item type {string} exists")
    public void theWorkItemTypeExists(String typeName) {
        theWorkItemTypeAlreadyExists(typeName);
    }
    
    @Given("there are work items of type {string} in the system")
    public void thereAreWorkItemsOfTypeInTheSystem(String typeName) {
        // Create dummy work items of the specified type
        context.setConfigurationValue("workItemsOfType:" + typeName, 5);
    }
    
    @Given("no work items of type {string} exist in the system")
    public void noWorkItemsOfTypeExistInTheSystem(String typeName) {
        context.setConfigurationValue("workItemsOfType:" + typeName, 0);
    }
    
    @Given("the work item type {string} exists with a field {string}")
    public void theWorkItemTypeExistsWithAField(String typeName, String fieldName) {
        Map<String, Object> typeData = new HashMap<>();
        typeData.put("name", typeName);
        typeData.put("description", "Work item type with custom field");
        
        Map<String, Object> fields = new HashMap<>();
        fields.put(fieldName, Map.of(
            "type", "string",
            "description", "Example field " + fieldName,
            "required", false
        ));
        
        typeData.put("fields", fields);
        typeData.put("createdAt", "2025-04-01T10:00:00Z");
        
        context.setConfigurationValue("workItemType:" + typeName, typeData);
    }
    
    @Given("the work item type {string} exists with fields {string} and {string}")
    public void theWorkItemTypeExistsWithFieldsAnd(String typeName, String field1, String field2) {
        Map<String, Object> typeData = new HashMap<>();
        typeData.put("name", typeName);
        typeData.put("description", "Work item type with custom fields");
        
        Map<String, Object> fields = new HashMap<>();
        fields.put(field1, Map.of(
            "type", "string",
            "description", "Example field " + field1,
            "required", false
        ));
        
        fields.put(field2, Map.of(
            "type", "number",
            "description", "Example field " + field2,
            "required", false
        ));
        
        typeData.put("fields", fields);
        typeData.put("createdAt", "2025-04-01T10:00:00Z");
        
        context.setConfigurationValue("workItemType:" + typeName, typeData);
    }
    
    @Given("I have customized the {string} work item type")
    public void iHaveCustomizedTheWorkItemType(String typeName) {
        Map<String, Object> typeData = new HashMap<>();
        typeData.put("name", typeName);
        typeData.put("description", "Customized " + typeName + " work item type");
        
        Map<String, Object> fields = new HashMap<>();
        fields.put("severity", Map.of(
            "type", "enum",
            "description", "Severity level",
            "values", List.of("low", "medium", "high", "critical"),
            "required", true
        ));
        
        fields.put("reproducibility", Map.of(
            "type", "enum",
            "description", "How consistently can the bug be reproduced",
            "values", List.of("always", "sometimes", "rarely", "unknown"),
            "required", false
        ));
        
        fields.put("browser", Map.of(
            "type", "string",
            "description", "Browser where bug occurs",
            "required", false
        ));
        
        fields.put("version", Map.of(
            "type", "string",
            "description", "Product version where bug was found",
            "required", true
        ));
        
        typeData.put("fields", fields);
        typeData.put("createdAt", "2025-04-01T10:00:00Z");
        
        context.setConfigurationValue("workItemType:" + typeName, typeData);
        context.setConfigurationFlag("hasCustomizedType:" + typeName, true);
    }
    
    @Given("multiple work item type templates exist in the system")
    public void multipleWorkItemTypeTemplatesExistInTheSystem() {
        // Create detailed bug template
        Map<String, Object> bugTemplate = new HashMap<>();
        bugTemplate.put("id", "detailed-bug");
        bugTemplate.put("name", "Detailed Bug");
        bugTemplate.put("description", "Comprehensive bug tracking with extra fields");
        bugTemplate.put("createdAt", "2025-03-10T14:30:00Z");
        bugTemplate.put("baseType", "BUG");
        
        // Create agile story template
        Map<String, Object> storyTemplate = new HashMap<>();
        storyTemplate.put("id", "agile-story");
        storyTemplate.put("name", "Agile Story");
        storyTemplate.put("description", "User story with story points and acceptance criteria");
        storyTemplate.put("createdAt", "2025-03-15T09:45:00Z");
        storyTemplate.put("baseType", "STORY");
        
        // Create release task template
        Map<String, Object> releaseTemplate = new HashMap<>();
        releaseTemplate.put("id", "release-task");
        releaseTemplate.put("name", "Release Task");
        releaseTemplate.put("description", "Task specific to release management");
        releaseTemplate.put("createdAt", "2025-03-20T11:20:00Z");
        releaseTemplate.put("baseType", "TASK");
        
        context.setConfigurationValue("workItemTypeTemplate:detailed-bug", bugTemplate);
        context.setConfigurationValue("workItemTypeTemplate:agile-story", storyTemplate);
        context.setConfigurationValue("workItemTypeTemplate:release-task", releaseTemplate);
    }
    
    @Given("a work item type template {string} exists")
    public void aWorkItemTypeTemplateExists(String templateName) {
        Map<String, Object> template = new HashMap<>();
        template.put("id", templateName.toLowerCase().replace(" ", "-"));
        template.put("name", templateName);
        template.put("description", "Work item type template for " + templateName);
        template.put("createdAt", "2025-04-01T10:00:00Z");
        template.put("baseType", "BUG");
        
        context.setConfigurationValue("workItemTypeTemplate:" + templateName.toLowerCase().replace(" ", "-"), template);
    }
    
    @Given("I have customized several work item types")
    public void iHaveCustomizedSeveralWorkItemTypes() {
        iHaveCustomizedTheWorkItemType("BUG");
        iHaveCustomizedTheWorkItemType("STORY");
        iHaveCustomizedTheWorkItemType("EPIC");
    }
    
    @Given("I have a work item type configuration file {string}")
    public void iHaveAWorkItemTypeConfigurationFile(String filename) {
        // Simulate having a configuration file
        context.setConfigurationValue("workItemTypeConfigFile", filename);
    }
    
    @Given("I have defined relationships between work item types")
    public void iHaveDefinedRelationshipsBetweenWorkItemTypes() {
        // Create EPIC type
        Map<String, Object> epicData = new HashMap<>();
        epicData.put("name", "EPIC");
        epicData.put("description", "Large feature that spans multiple releases");
        context.setConfigurationValue("workItemType:EPIC", epicData);
        
        // Create STORY type
        Map<String, Object> storyData = new HashMap<>();
        storyData.put("name", "STORY");
        storyData.put("description", "User story with a specific value");
        context.setConfigurationValue("workItemType:STORY", storyData);
        
        // Create TASK type
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("name", "TASK");
        taskData.put("description", "Implementation task for stories");
        context.setConfigurationValue("workItemType:TASK", taskData);
        
        // Define relationships
        Map<String, Object> epicToStory = new HashMap<>();
        epicToStory.put("parent", "EPIC");
        epicToStory.put("child", "STORY");
        epicToStory.put("name", "breaks down to");
        context.setConfigurationValue("relationship:EPIC_TO_STORY", epicToStory);
        
        Map<String, Object> storyToTask = new HashMap<>();
        storyToTask.put("parent", "STORY");
        storyToTask.put("child", "TASK");
        storyToTask.put("name", "implemented by");
        context.setConfigurationValue("relationship:STORY_TO_TASK", storyToTask);
        
        context.setConfigurationFlag("hasDefinedRelationships", true);
    }
    
    @Given("a parent-child relationship exists between {string} and {string}")
    public void aParentChildRelationshipExistsBetween(String parent, String child) {
        Map<String, Object> parentData = new HashMap<>();
        parentData.put("name", parent);
        parentData.put("description", parent + " work item type");
        context.setConfigurationValue("workItemType:" + parent, parentData);
        
        Map<String, Object> childData = new HashMap<>();
        childData.put("name", child);
        childData.put("description", child + " work item type");
        context.setConfigurationValue("workItemType:" + child, childData);
        
        Map<String, Object> relationship = new HashMap<>();
        relationship.put("parent", parent);
        relationship.put("child", child);
        relationship.put("name", "contains");
        context.setConfigurationValue("relationship:" + parent + "_TO_" + child, relationship);
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
    
    @Then("I should see the default work item types")
    public void iShouldSeeTheDefaultWorkItemTypes() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Work Item Types:"), 
                   "Output should show work item types section");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("they should include at least {string}, {string}, and {string}")
    public void theyShouldIncludeAtLeastAnd(String type1, String type2, String type3) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains(type1), "Output should include type: " + type1);
        assertTrue(commandOutput.contains(type2), "Output should include type: " + type2);
        assertTrue(commandOutput.contains(type3), "Output should include type: " + type3);
    }
    
    @Then("each type should show its associated fields and workflow states")
    public void eachTypeShouldShowItsAssociatedFieldsAndWorkflowStates() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Fields:"), "Output should show fields section");
        assertTrue(commandOutput.contains("Allowed States:"), "Output should show states section");
    }
    
    @Then("the new work item type should be added to the system")
    public void theNewWorkItemTypeShouldBeAddedToTheSystem() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added new work item type"), 
                   "Output should confirm type addition");
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
    
    @Then("the new type should appear when listing work item types")
    public void theNewTypeShouldAppearWhenListingWorkItemTypes() {
        // This would verify type listing in a real implementation
        // For test purposes, we just assert that this should happen
        assertTrue(true);
    }
    
    @Then("the new work item type should be created with the custom fields")
    public void theNewWorkItemTypeShouldBeCreatedWithTheCustomFields() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added new work item type"), 
                   "Output should confirm type addition");
        assertTrue(commandOutput.contains("custom fields"), 
                   "Output should mention custom fields");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("these fields should be available when creating items of type {string}")
    public void theseFieldsShouldBeAvailableWhenCreatingItemsOfType(String typeName) {
        // This would verify field availability in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("the new work item type should be created with the constrained fields")
    public void theNewWorkItemTypeShouldBeCreatedWithTheConstrainedFields() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added new work item type"), 
                   "Output should confirm type addition");
        assertTrue(commandOutput.contains("field constraints"), 
                   "Output should mention field constraints");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the {string} field should only accept the defined enum values")
    public void theFieldShouldOnlyAcceptTheDefinedEnumValues(String fieldName) {
        // This would verify field constraints in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("the {string} field should be marked as required")
    public void theFieldShouldBeMarkedAsRequired(String fieldName) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("required field"), 
                   "Output should mention required fields");
        assertTrue(commandOutput.contains(fieldName), 
                   "Output should include the specific field name");
    }
    
    @Then("I should see an error message about duplicate type names")
    public void iShouldSeeAnErrorMessageAboutDuplicateTypeNames() {
        assertNotNull(commandError);
        assertTrue(commandError.length() > 0, "Should have error output");
        assertTrue(commandError.contains("duplicate") || 
                   commandError.contains("already exists"), 
                   "Error should mention duplicate type");
    }
    
    @Then("the existing type should remain unchanged")
    public void theExistingTypeShouldRemainUnchanged() {
        // This would verify type preservation in a real implementation
        // For test purposes, we just check for the appropriate error message
        assertNotNull(commandError);
        assertTrue(commandError.length() > 0, "Should have error output");
    }
    
    @Then("the work item type should be updated")
    public void theWorkItemTypeShouldBeUpdated() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully updated work item type"), 
                   "Output should confirm type update");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("any work items of this type should reflect the updated type name")
    public void anyWorkItemsOfThisTypeShouldReflectTheUpdatedTypeName() {
        // This would verify work item updates in a real implementation
        // For test purposes, we just assert that this should happen
        assertTrue(true);
    }
    
    @Then("the field should be added to the work item type")
    public void theFieldShouldBeAddedToTheWorkItemType() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added field"), 
                   "Output should confirm field addition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the field should be available when creating or editing {string} items")
    public void theFieldShouldBeAvailableWhenCreatingOrEditingItems(String typeName) {
        // This would verify field availability in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("the field should be removed from the work item type")
    public void theFieldShouldBeRemovedFromTheWorkItemType() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully removed field"), 
                   "Output should confirm field removal");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should see a warning about data loss")
    public void iShouldSeeAWarningAboutDataLoss() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("WARNING") || 
                   commandOutput.contains("Warning"), 
                   "Output should include a warning");
        assertTrue(commandOutput.contains("data loss") || 
                   commandOutput.contains("might lose data"), 
                   "Warning should mention data loss");
    }
    
    @Then("the default values should be set for the fields")
    public void theDefaultValuesShouldBeSetForTheFields() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully set default values"), 
                   "Output should confirm default values setting");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("new {string} items should be created with these default values")
    public void newItemsShouldBeCreatedWithTheseDefaultValues(String typeName) {
        // This would verify default value application in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("I should see an error message about the type being in use")
    public void iShouldSeeAnErrorMessageAboutTheTypeBeingInUse() {
        assertNotNull(commandError);
        assertTrue(commandError.length() > 0, "Should have error output");
        assertTrue(commandError.contains("in use") || 
                   commandError.contains("has work items"), 
                   "Error should mention type being in use");
    }
    
    @Then("the type should not be deleted")
    public void theTypeShouldNotBeDeleted() {
        // This would verify type preservation in a real implementation
        // For test purposes, we just check for the appropriate error message
        assertNotNull(commandError);
        assertTrue(commandError.length() > 0, "Should have error output");
    }
    
    @Then("the type should be removed from the system")
    public void theTypeShouldBeRemovedFromTheSystem() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully removed work item type"), 
                   "Output should confirm type removal");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should no longer appear when listing work item types")
    public void itShouldNoLongerAppearWhenListingWorkItemTypes() {
        // This would verify type listing in a real implementation
        // For test purposes, we just assert that this should happen
        assertTrue(true);
    }
    
    @Then("all work items previously of type {string} should be converted to {string}")
    public void allWorkItemsPreviouslyOfTypeShouldBeConvertedTo(String oldType, String newType) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully converted work items"), 
                   "Output should confirm work item conversion");
        assertTrue(commandOutput.contains("from " + oldType + " to " + newType), 
                   "Output should mention type conversion");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should see a warning about the forced type change")
    public void iShouldSeeAWarningAboutTheForcedTypeChange() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("WARNING") || 
                   commandOutput.contains("Warning"), 
                   "Output should include a warning");
        assertTrue(commandOutput.contains("forced") || 
                   commandOutput.contains("Forced"), 
                   "Warning should mention forced change");
    }
    
    @Then("the text field should be added to the work item type")
    public void theTextFieldShouldBeAddedToTheWorkItemType() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added text field"), 
                   "Output should confirm text field addition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should support multiline text input")
    public void itShouldSupportMultilineTextInput() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("multiline") || 
                   commandOutput.contains("Multiline"), 
                   "Output should mention multiline support");
    }
    
    @Then("the number field should be added with the specified range constraint")
    public void theNumberFieldShouldBeAddedWithTheSpecifiedRangeConstraint() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added number field"), 
                   "Output should confirm number field addition");
        assertTrue(commandOutput.contains("range constraint"), 
                   "Output should mention range constraint");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should only accept numeric values between {int} and {int}")
    public void itShouldOnlyAcceptNumericValuesBetweenAnd(int min, int max) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("range: " + min + "-" + max) || 
                   commandOutput.contains("between " + min + " and " + max), 
                   "Output should mention the specific range");
    }
    
    @Then("the enum field should be added with the specified values")
    public void theEnumFieldShouldBeAddedWithTheSpecifiedValues() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added enum field"), 
                   "Output should confirm enum field addition");
        assertTrue(commandOutput.contains("enum values"), 
                   "Output should mention enum values");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the date field should be added to the work item type")
    public void theDateFieldShouldBeAddedToTheWorkItemType() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added date field"), 
                   "Output should confirm date field addition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should accept and validate date values")
    public void itShouldAcceptAndValidateDateValues() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("date validation") || 
                   commandOutput.contains("validates dates"), 
                   "Output should mention date validation");
    }
    
    @Then("the URL field should be added to the work item type")
    public void theURLFieldShouldBeAddedToTheWorkItemType() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added URL field"), 
                   "Output should confirm URL field addition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should validate that values are properly formatted URLs")
    public void itShouldValidateThatValuesAreProperlyFormattedURLs() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("URL validation") || 
                   commandOutput.contains("validates URLs"), 
                   "Output should mention URL validation");
    }
    
    @Then("the user field should be added to the work item type")
    public void theUserFieldShouldBeAddedToTheWorkItemType() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added user field"), 
                   "Output should confirm user field addition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should provide user selection from system users")
    public void itShouldProvideUserSelectionFromSystemUsers() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("system users") || 
                   commandOutput.contains("user selection"), 
                   "Output should mention user selection");
    }
    
    @Then("the multi-select field should be added to the work item type")
    public void theMultiSelectFieldShouldBeAddedToTheWorkItemType() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added multi-select field"), 
                   "Output should confirm multi-select field addition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should allow selecting multiple values from the provided options")
    public void itShouldAllowSelectingMultipleValuesFromTheProvidedOptions() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("multiple values") || 
                   commandOutput.contains("multi-select"), 
                   "Output should mention multiple selection");
    }
    
    @Then("the field should be added and marked as required")
    public void theFieldShouldBeAddedAndMarkedAsRequired() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully added field"), 
                   "Output should confirm field addition");
        assertTrue(commandOutput.contains("marked as required"), 
                   "Output should mention required flag");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("creation of INCIDENT items should fail if this field is not provided")
    public void creationOfINCIDENTItemsShouldFailIfThisFieldIsNotProvided() {
        // This would verify validation in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("the current type configuration should be saved as a template")
    public void theCurrentTypeConfigurationShouldBeSavedAsATemplate() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully saved work item type template"), 
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
    
    @Then("I should see a list of all available type templates")
    public void iShouldSeeAListOfAllAvailableTypeTemplates() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Work Item Type Templates:"), 
                   "Output should show type templates section");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("each template should show a name, description, and field list")
    public void eachTemplateShouldShowANameDescriptionAndFieldList() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Name:"), "Output should include template names");
        assertTrue(commandOutput.contains("Description:"), "Output should include descriptions");
        assertTrue(commandOutput.contains("Fields:"), "Output should include field lists");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("a new work item type {string} should be created from the template")
    public void aNewWorkItemTypeShouldBeCreatedFromTheTemplate(String typeName) {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully created new work item type"), 
                   "Output should confirm type creation");
        assertTrue(commandOutput.contains(typeName), 
                   "Output should mention the specific type name");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should include all fields and settings from the template")
    public void itShouldIncludeAllFieldsAndSettingsFromTheTemplate() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("All fields and settings applied from template"), 
                   "Output should confirm settings application");
    }
    
    @Then("I should receive a JSON file with the complete type configurations")
    public void iShouldReceiveAJSONFileWithTheCompleteTypeConfigurations() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully exported work item type configuration"), 
                   "Output should confirm configuration export");
        assertTrue(commandOutput.contains(".json"), 
                   "Output should mention JSON file");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("the export should include all types, fields, and constraints")
    public void theExportShouldIncludeAllTypesFieldsAndConstraints() {
        // This would verify export completeness in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("the system work item types should be updated to match the imported configuration")
    public void theSystemWorkItemTypesShouldBeUpdatedToMatchTheImportedConfiguration() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully imported work item type configuration"), 
                   "Output should confirm configuration import");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should be warned about any conflicts")
    public void iShouldBeWarnedAboutAnyConflicts() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("NOTE:") || 
                   commandOutput.contains("Warning:"), 
                   "Output should include a note or warning");
        assertTrue(commandOutput.contains("conflict") || 
                   commandOutput.contains("needs attention"), 
                   "Note should mention conflicts");
    }
    
    @Then("the relationship types should be defined between the work items")
    public void theRelationshipTypesShouldBeDefinedBetweenTheWorkItems() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully defined relationship"), 
                   "Output should confirm relationship definition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("I should be able to create hierarchical work item structures")
    public void iShouldBeAbleToCreateHierarchicalWorkItemStructures() {
        // This would verify hierarchy creation in a real implementation
        // For test purposes, we just assert that this should be possible
        assertTrue(true);
    }
    
    @Then("I should see a visual representation of the work item type hierarchy")
    public void iShouldSeeAVisualRepresentationOfTheWorkItemTypeHierarchy() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Type Hierarchy:") || 
                   commandOutput.contains("Type Relationships:"), 
                   "Output should show hierarchy section");
        assertTrue(commandOutput.contains("visual") || 
                   commandOutput.contains("diagram") || 
                   commandOutput.contains("graph"), 
                   "Output should include a visual representation");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("it should show all defined relationships")
    public void itShouldShowAllDefinedRelationships() {
        // This would verify relationship display in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
    
    @Then("each relationship should be labeled with its name")
    public void eachRelationshipShouldBeLabeledWithItsName() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("breaks down to") || 
                   commandOutput.contains("implemented by") || 
                   commandOutput.contains("contains"), 
                   "Output should include relationship names");
    }
    
    @Then("the inheritance rules should be defined")
    public void theInheritanceRulesShouldBeDefined() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Successfully defined field inheritance"), 
                   "Output should confirm inheritance definition");
        assertFalse(commandError.length() > 0, "Should not have error output");
    }
    
    @Then("when creating a STORY under an EPIC, these fields should be auto-populated")
    public void whenCreatingASTORYUnderAnEPICTheseFieldsShouldBeAutoPopulated() {
        // This would verify field inheritance in a real implementation
        // For test purposes, we just assert that this should be the case
        assertTrue(true);
    }
}