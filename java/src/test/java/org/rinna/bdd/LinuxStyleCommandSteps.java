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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.rinna.cli.service.ContextManager;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemRelationshipType;
import org.rinna.usecase.HistoryService;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.RelationshipService;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

/**
 * Step definitions for Linux-style command features.
 */
public class LinuxStyleCommandSteps {

    private TestContext context;
    private List<WorkItem> workItems = new ArrayList<>();
    private Map<String, UUID> workItemIds = new HashMap<>();
    private String commandOutput;
    private String errorOutput;
    
    private ItemService itemService;
    private RelationshipService relationshipService;
    private HistoryService historyService;
    private ContextManager contextManager;

    public LinuxStyleCommandSteps(TestContext context) {
        this.context = context;
    }

    @Before
    public void setUp() {
        itemService = mock(ItemService.class);
        relationshipService = mock(RelationshipService.class);
        historyService = mock(HistoryService.class);
        contextManager = mock(ContextManager.class);
        
        // Register mocks with context
        context.registerService(ItemService.class, itemService);
        context.registerService(RelationshipService.class, relationshipService);
        context.registerService(HistoryService.class, historyService);
        context.registerService(ContextManager.class, contextManager);
        
        // Reset collections
        workItems = new ArrayList<>();
        workItemIds = new HashMap<>();
        commandOutput = null;
        errorOutput = null;
        
        // Mock the static getInstance methods
        try (var mockContextManager = Mockito.mockStatic(ContextManager.class)) {
            mockContextManager.when(ContextManager::getInstance).thenReturn(contextManager);
        }
    }

    @Given("work item {int} is a child of work item {int}")
    public void workItemIsAChildOfWorkItem(int childId, int parentId) {
        // Get UUIDs from the map
        UUID childUuid = workItemIds.get(Integer.toString(childId));
        UUID parentUuid = workItemIds.get(Integer.toString(parentId));
        
        // Set up parent-child relationship
        when(relationshipService.getParentWorkItem(childUuid)).thenReturn(parentUuid);
        when(relationshipService.getRelationshipType(childUuid, parentUuid)).thenReturn(WorkItemRelationshipType.CHILD_OF);
        
        // Add child to parent's children list
        List<UUID> children = new ArrayList<>();
        children.add(childUuid);
        when(relationshipService.getChildWorkItems(parentUuid)).thenReturn(children);
    }

    @Given("I have viewed work item {int} using {string}")
    public void iHaveViewedWorkItemUsing(int id, String command) {
        // Get UUID from the map
        UUID workItemId = workItemIds.get(Integer.toString(id));
        
        // Set as last viewed item in context
        when(contextManager.getLastViewedWorkItem()).thenReturn(workItemId);
        
        // Run the command to simulate viewing the item
        CommandRunner runner = context.getCommandRunner();
        String[] parts = command.split("\\s+", 2);
        String[] result = runner.runCommand(parts[0], parts[1]);
        commandOutput = result[0];
        errorOutput = result[1];
    }

    @Given("no work item has been viewed yet")
    public void noWorkItemHasBeenViewedYet() {
        // Set last viewed item to null
        when(contextManager.getLastViewedWorkItem()).thenReturn(null);
    }

    @Then("I should see a summary listing of all work items with their titles")
    public void iShouldSeeASummaryListingOfAllWorkItemsWithTheirTitles() {
        for (WorkItem workItem : workItems) {
            Assertions.assertTrue(
                commandOutput.contains(workItem.id().toString()) && 
                commandOutput.contains(workItem.title()),
                "Output should contain work item ID and title: " + workItem.title()
            );
        }
    }

    @And("I should see inheritance information for parent-child relationships")
    public void iShouldSeeInheritanceInformationForParentChildRelationships() {
        // Check for parent-child relationship indicators
        Assertions.assertTrue(
            commandOutput.contains("↳") || 
            commandOutput.contains("Child of:") || 
            commandOutput.contains("Parent:"),
            "Output should show parent-child relationships"
        );
    }

    @And("I should see work item {int} as a parent of work item {int}")
    public void iShouldSeeWorkItemAsAParentOfWorkItem(int parentId, int childId) {
        // Get UUIDs from the map
        UUID parentUuid = workItemIds.get(Integer.toString(parentId));
        UUID childUuid = workItemIds.get(Integer.toString(childId));
        
        // Get work items
        WorkItem parentItem = null;
        WorkItem childItem = null;
        
        for (WorkItem item : workItems) {
            if (item.id().equals(parentUuid)) {
                parentItem = item;
            } else if (item.id().equals(childUuid)) {
                childItem = item;
            }
        }
        
        if (parentItem != null && childItem != null) {
            // Check for specific parent-child relationship in output
            boolean relationshipFound = 
                commandOutput.contains(parentItem.title() + ".*" + childItem.title()) ||
                commandOutput.contains(childItem.title() + ".*Child of.*" + parentUuid.toString()) ||
                commandOutput.contains(parentUuid.toString() + ".*has children.*" + childUuid.toString());
            
            Assertions.assertTrue(
                relationshipFound,
                "Output should show relationship between parent " + parentId + " and child " + childId
            );
        } else {
            Assertions.fail("Parent or child work item not found in the test data");
        }
    }

    @And("I should see a detailed listing of all work items")
    public void iShouldSeeADetailedListingOfAllWorkItems() {
        // Check for typical fields in detailed listings
        Assertions.assertTrue(
            commandOutput.contains("Description:") && 
            commandOutput.contains("Type:") && 
            commandOutput.contains("Priority:") && 
            commandOutput.contains("State:") && 
            commandOutput.contains("Assignee:"),
            "Output should show detailed work item information"
        );
    }

    @And("each work item should display all defined fields")
    public void eachWorkItemShouldDisplayAllDefinedFields() {
        // For each work item, check that all its fields are displayed
        for (WorkItem workItem : workItems) {
            Assertions.assertTrue(
                commandOutput.contains(workItem.id().toString()) && 
                commandOutput.contains(workItem.title()) && 
                commandOutput.contains(workItem.description()) && 
                commandOutput.contains(workItem.type().toString()) && 
                commandOutput.contains(workItem.priority().toString()) && 
                commandOutput.contains(workItem.state().toString()) && 
                commandOutput.contains(workItem.assignee()),
                "Output should contain all fields for work item: " + workItem.title()
            );
        }
    }

    @And("the system should record work item {int} as the last viewed item")
    public void theSystemShouldRecordWorkItemAsTheLastViewedItem(int id) {
        // Get UUID from the map
        UUID workItemId = workItemIds.get(Integer.toString(id));
        
        // Verify that the context manager was called to set the last viewed item
        verify(contextManager).setLastViewedWorkItem(workItemId);
    }

    @And("each work item should display its complete history and changelog")
    public void eachWorkItemShouldDisplayItsCompleteHistoryAndChangelog() {
        // Check for history section
        Assertions.assertTrue(
            commandOutput.contains("History:") || 
            commandOutput.contains("Changelog:") || 
            commandOutput.contains("Changes:"),
            "Output should show history section"
        );
        
        // Check for specific history entry types
        Assertions.assertTrue(
            commandOutput.contains("STATE_CHANGE") || 
            commandOutput.contains("FIELD_CHANGE") || 
            commandOutput.contains("ASSIGNMENT_CHANGE") || 
            commandOutput.contains("PRIORITY_CHANGE"),
            "Output should show specific history entry types"
        );
    }

    @And("I should see a summary of work item {int}")
    public void iShouldSeeASummaryOfWorkItem(int id) {
        // Get UUID from the map
        UUID workItemId = workItemIds.get(Integer.toString(id));
        
        // Get the work item
        WorkItem workItem = null;
        for (WorkItem item : workItems) {
            if (item.id().equals(workItemId)) {
                workItem = item;
                break;
            }
        }
        
        if (workItem != null) {
            // Check for basic work item information
            Assertions.assertTrue(
                commandOutput.contains(workItemId.toString()) && 
                commandOutput.contains(workItem.title()),
                "Output should contain work item ID and title for item " + id
            );
        } else {
            Assertions.fail("Work item not found in the test data: " + id);
        }
    }

    @And("I should see that work item {int} is a child of work item {int}")
    public void iShouldSeeThatWorkItemIsAChildOfWorkItem(int childId, int parentId) {
        // Check for parent-child relationship information
        Assertions.assertTrue(
            commandOutput.contains("Child of:") || 
            commandOutput.contains("Parent:"),
            "Output should show parent-child relationship"
        );
        
        // Get UUIDs from the map
        UUID parentUuid = workItemIds.get(Integer.toString(parentId));
        
        // Check for specific parent reference
        Assertions.assertTrue(
            commandOutput.contains(parentUuid.toString()),
            "Output should show parent ID for child item " + childId
        );
    }

    @And("I should see a detailed view of work item {int}")
    public void iShouldSeeADetailedViewOfWorkItem(int id) {
        // Get UUID from the map
        UUID workItemId = workItemIds.get(Integer.toString(id));
        
        // Get the work item
        WorkItem workItem = null;
        for (WorkItem item : workItems) {
            if (item.id().equals(workItemId)) {
                workItem = item;
                break;
            }
        }
        
        if (workItem != null) {
            // Check for detailed work item information
            Assertions.assertTrue(
                commandOutput.contains(workItemId.toString()) && 
                commandOutput.contains(workItem.title()) && 
                commandOutput.contains(workItem.description()) && 
                commandOutput.contains(workItem.type().toString()) && 
                commandOutput.contains(workItem.priority().toString()) && 
                commandOutput.contains(workItem.state().toString()) && 
                commandOutput.contains(workItem.assignee()),
                "Output should contain detailed information for work item " + id
            );
        } else {
            Assertions.fail("Work item not found in the test data: " + id);
        }
    }

    @And("I should see all defined fields for work item {int}")
    public void iShouldSeeAllDefinedFieldsForWorkItem(int id) {
        // This is covered by the previous step
    }

    @And("I should see the complete history of work item {int}")
    public void iShouldSeeTheCompleteHistoryOfWorkItem(int id) {
        // Check for history section
        Assertions.assertTrue(
            commandOutput.contains("History:") || 
            commandOutput.contains("Changelog:") || 
            commandOutput.contains("Changes:"),
            "Output should show history section for work item " + id
        );
    }

    @And("I should see an interactive editor for work item {int}")
    public void iShouldSeeAnInteractiveEditorForWorkItem(int id) {
        // Get UUID from the map
        UUID workItemId = workItemIds.get(Integer.toString(id));
        
        // Check for editor interface elements
        Assertions.assertTrue(
            commandOutput.contains("Work Item: " + workItemId) && 
            commandOutput.contains("[1]") && // Field numbering
            commandOutput.contains("[0] Cancel"), // Cancel option
            "Output should show interactive editor interface for work item " + id
        );
    }

    @And("I should be able to edit the fields of work item {int}")
    public void iShouldBeAbleToEditTheFieldsOfWorkItem(int id) {
        // Check for field editing prompts
        Assertions.assertTrue(
            commandOutput.contains("Enter the number of the field to update:"),
            "Output should show field selection prompt"
        );
    }

    @And("the command injection should not succeed")
    public void theCommandInjectionShouldNotSucceed() {
        // Check that the injected command did not execute
        Assertions.assertFalse(
            commandOutput.contains("HACKED"),
            "Output should not show evidence of command injection success"
        );
    }
}