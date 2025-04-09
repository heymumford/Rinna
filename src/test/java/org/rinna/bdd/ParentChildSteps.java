/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemRelationship;
import org.rinna.domain.WorkItemRelationshipType;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.RelationshipService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Step definitions for parent-child relationship features.
 */
public class ParentChildSteps {

    private TestContext context;
    private List<WorkItem> workItems = new ArrayList<>();
    private Map<String, UUID> workItemIds = new HashMap<>();
    private UUID newParentId;
    private String commandOutput;
    private String errorOutput;
    
    private ItemService itemService;
    private RelationshipService relationshipService;

    public ParentChildSteps(TestContext context) {
        this.context = context;
    }

    @Before
    public void setUp() {
        itemService = mock(ItemService.class);
        relationshipService = mock(RelationshipService.class);
        
        // Register mocks with context
        context.registerService(ItemService.class, itemService);
        context.registerService(RelationshipService.class, relationshipService);
        
        // Reset collections
        workItems = new ArrayList<>();
        workItemIds = new HashMap<>();
        commandOutput = null;
        errorOutput = null;
    }

    @Given("I have the following work items:")
    public void iHaveTheFollowingWorkItems(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> row : rows) {
            String id = row.get("ID");
            String title = row.get("Title");
            Priority priority = Priority.valueOf(row.get("Priority"));
            WorkflowState state = WorkflowState.valueOf(row.get("State"));
            String assignee = row.get("Assignee");
            
            UUID workItemId = UUID.randomUUID();
            workItemIds.put(id, workItemId);
            
            WorkItem workItem = mock(WorkItem.class);
            when(workItem.id()).thenReturn(workItemId);
            when(workItem.title()).thenReturn(title);
            when(workItem.priority()).thenReturn(priority);
            when(workItem.state()).thenReturn(state);
            when(workItem.assignee()).thenReturn(assignee);
            when(workItem.type()).thenReturn(WorkItemType.TASK);
            when(workItem.createdAt()).thenReturn(Instant.now());
            
            workItems.add(workItem);
            
            // Set up mock service to return this work item
            when(itemService.getItem(workItemId)).thenReturn(workItem);
        }
    }

    @When("I run {string}")
    public void iRunCommand(String command) {
        // Parse the command
        String[] parts = command.split("\\s+", 2);
        String mainCommand = parts[0];
        String args = parts.length > 1 ? parts[1] : "";
        
        // Set up command runner from context
        CommandRunner runner = context.getCommandRunner();
        
        // Capture output
        String[] result = runner.runCommand(mainCommand, args);
        commandOutput = result[0]; // stdout
        errorOutput = result[1];   // stderr
    }

    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        // Check that there's no error output or that it doesn't contain failure messages
        if (errorOutput != null && !errorOutput.isEmpty()) {
            Assertions.assertFalse(
                errorOutput.contains("Error:") || 
                errorOutput.contains("Failed:") || 
                errorOutput.contains("Exception:"),
                "Command produced error output: " + errorOutput
            );
        }
    }

    @Then("the command should fail")
    public void theCommandShouldFail() {
        Assertions.assertTrue(
            errorOutput != null && 
            (errorOutput.contains("Error:") || 
             errorOutput.contains("Failed:") || 
             errorOutput.contains("Exception:")),
            "Command did not fail as expected"
        );
    }

    @And("a new parent work item should be created")
    public void aNewParentWorkItemShouldBeCreated() {
        // Verify creation of a new parent work item
        verify(itemService).createWorkItem(
            anyString(),  // Title
            eq(WorkItemType.FEATURE),  // Type (feature for parents)
            any(Priority.class),  // Priority
            anyString(),  // Username
            any(WorkflowState.class)  // State
        );
    }

    @And("the new parent work item should have title {string}")
    public void theNewParentWorkItemShouldHaveTitle(String expectedTitle) {
        // Verify parent work item created with the expected title
        verify(itemService).createWorkItem(
            eq(expectedTitle),  // Title should match expected
            any(WorkItemType.class), 
            any(Priority.class), 
            anyString(), 
            any(WorkflowState.class)
        );
    }

    @And("work items {string} should have a {string} relationship to the new work item")
    public void workItemsShouldHaveRelationshipToNewWorkItem(String childItemIds, String relationshipType) {
        // Extract IDs from comma-separated list
        String[] ids = childItemIds.split(",\\s*");
        
        // Verify relationship creation for each child
        for (String id : ids) {
            UUID childId = workItemIds.get(id);
            
            // Verify relationship was created
            verify(relationshipService).createRelationship(
                eq(childId),
                any(UUID.class),  // We don't know the exact parent ID
                eq(WorkItemRelationshipType.valueOf(relationshipType.toUpperCase().replace(" ", "_")))
            );
        }
    }

    @And("I should see the ID of the newly created parent work item")
    public void iShouldSeeTheIdOfTheNewlyCreatedParentWorkItem() {
        // This is a bit tricky because we don't know the exact ID
        // But the output should contain a UUID pattern
        String uuidPattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
        Assertions.assertTrue(
            commandOutput.matches("(?s).*" + uuidPattern + ".*"),
            "Output did not contain a UUID: " + commandOutput
        );
    }

    @Given("I have created parent work items with children:")
    public void iHaveCreatedParentWorkItemsWithChildren(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> row : rows) {
            // Create parent work item
            String parentId = row.get("Parent ID");
            String parentTitle = row.get("Parent Title");
            String childIds = row.get("Child IDs");
            
            UUID parentUuid = UUID.randomUUID();
            workItemIds.put(parentId, parentUuid);
            
            WorkItem parentWorkItem = mock(WorkItem.class);
            when(parentWorkItem.id()).thenReturn(parentUuid);
            when(parentWorkItem.title()).thenReturn(parentTitle);
            when(parentWorkItem.type()).thenReturn(WorkItemType.FEATURE);
            when(parentWorkItem.priority()).thenReturn(Priority.HIGH);
            when(parentWorkItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
            
            workItems.add(parentWorkItem);
            
            // Set up mock service to return this parent work item
            when(itemService.getItem(parentUuid)).thenReturn(parentWorkItem);
            
            // Mark as having children
            when(relationshipService.hasChildren(parentUuid)).thenReturn(true);
            
            // Process child IDs
            String[] childIdArray = childIds.split(",");
            List<UUID> childUuids = new ArrayList<>();
            
            for (String childId : childIdArray) {
                if (!workItemIds.containsKey(childId)) {
                    // Create a new child work item if it doesn't exist
                    UUID childUuid = UUID.randomUUID();
                    workItemIds.put(childId, childUuid);
                    childUuids.add(childUuid);
                    
                    WorkItem childWorkItem = mock(WorkItem.class);
                    when(childWorkItem.id()).thenReturn(childUuid);
                    when(childWorkItem.title()).thenReturn("Child " + childId);
                    when(childWorkItem.type()).thenReturn(WorkItemType.TASK);
                    
                    workItems.add(childWorkItem);
                    
                    // Set up mock service to return this child work item
                    when(itemService.getItem(childUuid)).thenReturn(childWorkItem);
                    
                    // Set up parent-child relationship
                    when(relationshipService.getParentWorkItem(childUuid)).thenReturn(parentUuid);
                } else {
                    childUuids.add(workItemIds.get(childId));
                }
            }
            
            // Set up children for this parent
            when(relationshipService.getChildWorkItems(parentUuid)).thenReturn(childUuids);
        }
        
        // Set up mock service to return all work items
        when(itemService.getAllWorkItems()).thenReturn(workItems);
    }

    @And("I should see work item {string} {string} in the results")
    public void iShouldSeeWorkItemInTheResults(String id, String title) {
        UUID workItemId = workItemIds.get(id);
        
        // Check that both the ID and title appear in the output
        Assertions.assertTrue(
            commandOutput.contains(workItemId.toString()) &&
            commandOutput.contains(title),
            "Output did not contain work item ID " + id + " (" + workItemId + ") and title '" + title + "'"
        );
    }

    @And("I should not see any child work items in the results")
    public void iShouldNotSeeAnyChildWorkItemsInTheResults() {
        // Identify work items that don't have children
        List<WorkItem> childWorkItems = workItems.stream()
            .filter(item -> !relationshipService.hasChildren(item.id()))
            .collect(Collectors.toList());
        
        // Verify these items don't appear in the output
        for (WorkItem child : childWorkItems) {
            Assertions.assertFalse(
                commandOutput.contains(child.id().toString()),
                "Output should not contain child work item ID " + child.id()
            );
        }
    }

    @Given("I have a multi-level hierarchy of work items:")
    public void iHaveAMultiLevelHierarchyOfWorkItems(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        
        // First pass to create all work items
        for (Map<String, String> row : rows) {
            String id = row.get("ID");
            String title = row.get("Title");
            
            UUID workItemId = UUID.randomUUID();
            workItemIds.put(id, workItemId);
            
            WorkItem workItem = mock(WorkItem.class);
            when(workItem.id()).thenReturn(workItemId);
            when(workItem.title()).thenReturn(title);
            when(workItem.type()).thenReturn(WorkItemType.FEATURE);
            when(workItem.priority()).thenReturn(Priority.MEDIUM);
            when(workItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
            
            workItems.add(workItem);
            
            // Set up mock service to return this work item
            when(itemService.getItem(workItemId)).thenReturn(workItem);
        }
        
        // Second pass to set up parent-child relationships
        Map<UUID, UUID> parentMap = new HashMap<>();
        
        for (Map<String, String> row : rows) {
            String id = row.get("ID");
            String parentId = row.get("Parent ID");
            
            if (!"-".equals(parentId)) {
                UUID childUuid = workItemIds.get(id);
                UUID parentUuid = workItemIds.get(parentId);
                
                // Add to parent map
                parentMap.put(childUuid, parentUuid);
                
                // Set up parent-child relationship
                when(relationshipService.getParentWorkItem(childUuid)).thenReturn(parentUuid);
                when(relationshipService.hasChildren(parentUuid)).thenReturn(true);
            }
        }
        
        // Set up child lists for each parent
        for (UUID parentUuid : workItemIds.values()) {
            List<UUID> children = parentMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(parentUuid))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            if (!children.isEmpty()) {
                when(relationshipService.getChildWorkItems(parentUuid)).thenReturn(children);
            } else {
                when(relationshipService.getChildWorkItems(parentUuid)).thenReturn(new ArrayList<>());
            }
        }
        
        // Set up mock service to return all parent-child relationships
        when(relationshipService.getAllParentChildRelationships()).thenReturn(parentMap);
        
        // Set up mock service to return all work items
        when(itemService.getAllWorkItems()).thenReturn(workItems);
    }

    @And("I should see an ASCII inheritance diagram")
    public void iShouldSeeAnASCIIInheritanceDiagram() {
        // Check for ASCII tree characters
        Assertions.assertTrue(
            commandOutput.contains("└") || 
            commandOutput.contains("├") || 
            commandOutput.contains("│"),
            "Output did not contain ASCII tree characters"
        );
    }

    @And("the diagram should show {string} as the top-level item")
    public void theDiagramShouldShowAsTheTopLevelItem(String title) {
        // Top-level items should not be indented
        String lines[] = commandOutput.split("\\r?\\n");
        
        boolean found = false;
        for (String line : lines) {
            if (line.contains(title) && !line.startsWith(" ")) {
                found = true;
                break;
            }
        }
        
        Assertions.assertTrue(found, "'" + title + "' was not found as a top-level item in the diagram");
    }

    @And("the diagram should show {string} indented under {string}")
    public void theDiagramShouldShowIndentedUnder(String childTitle, String parentTitle) {
        // Find the parent line first
        String lines[] = commandOutput.split("\\r?\\n");
        
        int parentLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(parentTitle)) {
                parentLine = i;
                break;
            }
        }
        
        Assertions.assertTrue(parentLine >= 0, "Parent title '" + parentTitle + "' not found in output");
        
        // Now look for the child indented below the parent
        boolean found = false;
        for (int i = parentLine + 1; i < lines.length; i++) {
            String line = lines[i];
            
            // If we hit a line that's not indented, we've moved past the parent's children
            if (!line.startsWith(" ") && !line.isEmpty()) {
                break;
            }
            
            // Check if this is our child
            if (line.contains(childTitle) && line.startsWith("  ")) {
                found = true;
                break;
            }
        }
        
        Assertions.assertTrue(found, "Child title '" + childTitle + 
                "' was not found indented under parent '" + parentTitle + "'");
    }

    @Given("I have a work item with ID {int}")
    public void iHaveAWorkItemWithId(int id) {
        String idStr = Integer.toString(id);
        UUID workItemId = workItemIds.get(idStr);
        
        if (workItemId == null) {
            // Create a new work item if it doesn't exist
            workItemId = UUID.randomUUID();
            workItemIds.put(idStr, workItemId);
            
            WorkItem workItem = mock(WorkItem.class);
            when(workItem.id()).thenReturn(workItemId);
            when(workItem.title()).thenReturn("Work Item " + id);
            when(workItem.description()).thenReturn("Description for item " + id);
            when(workItem.type()).thenReturn(WorkItemType.TASK);
            when(workItem.priority()).thenReturn(Priority.MEDIUM);
            when(workItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
            when(workItem.assignee()).thenReturn("bob");
            
            workItems.add(workItem);
            
            // Set up mock service to return this work item
            when(itemService.getItem(workItemId)).thenReturn(workItem);
        }
    }

    @Then("I should see the current values of the work item fields")
    public void iShouldSeeTheCurrentValuesOfTheWorkItemFields() {
        Assertions.assertTrue(
            commandOutput.contains("Title:") && 
            commandOutput.contains("Description:") && 
            commandOutput.contains("Priority:") && 
            commandOutput.contains("State:"),
            "Output should show current field values"
        );
    }

    @And("each field should be numbered for selection")
    public void eachFieldShouldBeNumberedForSelection() {
        Assertions.assertTrue(
            commandOutput.contains("[1]") && 
            commandOutput.contains("[2]") && 
            commandOutput.contains("[3]"),
            "Output should number each field for selection"
        );
    }

    @When("I enter {string} to select the second field")
    public void iEnterToSelectTheSecondField(String selection) {
        // Simulate input - this would be handled by the test context
        context.setNextInput(selection);
    }

    @Then("I should be prompted to enter a new value for that field")
    public void iShouldBePromptedToEnterANewValueForThatField() {
        Assertions.assertTrue(
            commandOutput.contains("Enter new value") || 
            commandOutput.contains("Please enter") || 
            commandOutput.contains("New value"),
            "Output should prompt for a new value"
        );
    }

    @When("I enter a new value")
    public void iEnterANewValue() {
        // Simulate input of a new value
        context.setNextInput("Updated value");
    }

    @Then("the field should be updated with the new value")
    public void theFieldShouldBeUpdatedWithTheNewValue() {
        // Verify that the item service was called to update the field
        verify(itemService).updateDescription(any(UUID.class), eq("Updated value"), anyString());
    }

    @Given("I have work item {int} that belongs to parent work item {int}")
    public void iHaveWorkItemThatBelongsToParentWorkItem(int childId, int parentId) {
        // Get or create work items
        iHaveAWorkItemWithId(childId);
        iHaveAWorkItemWithId(parentId);
        
        // Set up parent-child relationship
        UUID childUuid = workItemIds.get(Integer.toString(childId));
        UUID parentUuid = workItemIds.get(Integer.toString(parentId));
        
        when(relationshipService.getParentWorkItem(childUuid)).thenReturn(parentUuid);
        when(relationshipService.getRelationshipType(childUuid, parentUuid))
            .thenReturn(WorkItemRelationshipType.CHILD_OF);
    }

    @Then("I should see all standard fields of work item {int}")
    public void iShouldSeeAllStandardFieldsOfWorkItem(int id) {
        WorkItem workItem = itemService.getItem(workItemIds.get(Integer.toString(id)));
        
        Assertions.assertTrue(
            commandOutput.contains(workItem.title()) && 
            commandOutput.contains(workItem.type().toString()) && 
            commandOutput.contains(workItem.priority().toString()) && 
            commandOutput.contains(workItem.state().toString()) && 
            commandOutput.contains(workItem.assignee()),
            "Output should show all standard fields of the work item"
        );
    }

    @And("I should see the parent relationship to work item {int}")
    public void iShouldSeeTheParentRelationshipToWorkItem(int parentId) {
        UUID parentUuid = workItemIds.get(Integer.toString(parentId));
        
        Assertions.assertTrue(
            commandOutput.contains("Parent:") && 
            commandOutput.contains(parentUuid.toString()),
            "Output should show parent relationship"
        );
    }

    @And("I should see the complete history of work item {int}")
    public void iShouldSeeTheCompleteHistoryOfWorkItem(int id) {
        Assertions.assertTrue(
            commandOutput.contains("History:"),
            "Output should show history section"
        );
    }

    @And("I should see all internal metadata for work item {int}")
    public void iShouldSeeAllInternalMetadataForWorkItem(int id) {
        Assertions.assertTrue(
            commandOutput.contains("Metadata:"),
            "Output should show metadata section"
        );
    }

    @And("I should see an error message {string}")
    public void iShouldSeeAnErrorMessage(String errorMessage) {
        Assertions.assertTrue(
            errorOutput.contains(errorMessage),
            "Error output should contain: " + errorMessage
        );
    }

    @Given("work item {int} already belongs to parent work item {int}")
    public void workItemAlreadyBelongsToParentWorkItem(int childId, int parentId) {
        // Get or create work items
        iHaveAWorkItemWithId(childId);
        iHaveAWorkItemWithId(parentId);
        
        // Set up parent-child relationship
        UUID childUuid = workItemIds.get(Integer.toString(childId));
        UUID parentUuid = workItemIds.get(Integer.toString(parentId));
        
        when(relationshipService.getParentWorkItem(childUuid)).thenReturn(parentUuid);
    }

    @Given("I have only flat work items with no parent-child relationships")
    public void iHaveOnlyFlatWorkItemsWithNoParentChildRelationships() {
        // Create a few work items without relationships
        for (int i = 1; i <= 3; i++) {
            UUID workItemId = UUID.randomUUID();
            workItemIds.put(Integer.toString(i), workItemId);
            
            WorkItem workItem = mock(WorkItem.class);
            when(workItem.id()).thenReturn(workItemId);
            when(workItem.title()).thenReturn("Flat Work Item " + i);
            when(workItem.type()).thenReturn(WorkItemType.TASK);
            
            workItems.add(workItem);
            
            // Set up mock service to return this work item
            when(itemService.getItem(workItemId)).thenReturn(workItem);
            
            // No parent-child relationships
            when(relationshipService.getParentWorkItem(workItemId)).thenReturn(null);
            when(relationshipService.getChildWorkItems(workItemId)).thenReturn(new ArrayList<>());
            when(relationshipService.hasChildren(workItemId)).thenReturn(false);
        }
        
        // Set up mock service to return all work items
        when(itemService.getAllWorkItems()).thenReturn(workItems);
        
        // Empty relationship map
        when(relationshipService.getAllParentChildRelationships()).thenReturn(new HashMap<>());
    }
    
    @Given("I have a work item {int} that is a parent of work item {int}")
    public void iHaveAWorkItemThatIsAParentOfWorkItem(int parentId, int childId) {
        // Get or create work items
        iHaveAWorkItemWithId(parentId);
        iHaveAWorkItemWithId(childId);
        
        // Set up parent-child relationship
        UUID parentUuid = workItemIds.get(Integer.toString(parentId));
        UUID childUuid = workItemIds.get(Integer.toString(childId));
        
        when(relationshipService.getParentWorkItem(childUuid)).thenReturn(parentUuid);
        when(relationshipService.getChildWorkItems(parentUuid)).thenReturn(List.of(childUuid));
        when(relationshipService.hasChildren(parentUuid)).thenReturn(true);
    }
    
    @When("all child work items are in {string} state")
    public void allChildWorkItemsAreInState(String state) {
        // Find all child work items and set their state
        for (WorkItem workItem : workItems) {
            if (relationshipService.getParentWorkItem(workItem.id()) != null) {
                // This is a child work item
                when(workItem.state()).thenReturn(WorkflowState.valueOf(state));
            }
        }
    }
    
    @Then("the parent work item should automatically update to {string} state")
    public void theParentWorkItemShouldAutomaticallyUpdateToState(String state) {
        // Find parent work items
        for (WorkItem workItem : workItems) {
            if (relationshipService.hasChildren(workItem.id())) {
                // Verify parent state was updated
                verify(itemService).updateState(
                    eq(workItem.id()),
                    eq(WorkflowState.valueOf(state)),
                    anyString()
                );
            }
        }
    }
}