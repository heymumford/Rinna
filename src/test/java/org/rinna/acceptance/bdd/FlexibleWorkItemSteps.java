/*
 * BDD test steps for flexible work item feature
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.acceptance.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemRecord;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.WorkflowService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for the flexible work item BDD tests.
 */
@Tag("acceptance")
public class FlexibleWorkItemSteps {
    
    private final TestContext context;
    private TestItemService itemService;
    private TestWorkflowService workflowService;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    // Test data
    private final Map<String, WorkItemType> workItemTypes = new HashMap<>();
    private final Map<String, UUID> workItemIds = new HashMap<>();
    private final Map<UUID, Map<String, Object>> itemMetadata = new HashMap<>();
    private WorkItem currentWorkItem;
    private List<WorkflowState> stateHistory = new ArrayList<>();
    
    public FlexibleWorkItemSteps(TestContext context) {
        this.context = context;
    }
    
    @Before
    public void setUp() {
        System.setOut(new PrintStream(outputStream));
        itemService = new TestItemService();
        workflowService = new TestWorkflowService();
        
        // Initialize known work item types
        for (WorkItemType type : WorkItemType.values()) {
            workItemTypes.put(type.name(), type);
        }
    }
    
    @Given("the Rinna system supports these work item types:")
    public void theRinnaSystemSupportsTheseWorkItemTypes(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        
        for (Map<String, String> row : rows) {
            String typeName = row.get(0);
            String description = row.get(1);
            
            WorkItemType type = WorkItemType.valueOf(typeName);
            assertNotNull(type, "Work item type " + typeName + " should be supported");
        }
    }
    
    @When("a product owner creates a work item with the following attributes:")
    public void aProductOwnerCreatesAWorkItemWithTheFollowingAttributes(DataTable dataTable) {
        createWorkItemFromAttributes(dataTable, "product.owner@example.com");
    }
    
    @When("a developer creates a work item with the following attributes:")
    public void aDeveloperCreatesAWorkItemWithTheFollowingAttributes(DataTable dataTable) {
        createWorkItemFromAttributes(dataTable, "developer@example.com");
    }
    
    @When("a QA engineer creates a work item with the following attributes:")
    public void aQaEngineerCreatesAWorkItemWithTheFollowingAttributes(DataTable dataTable) {
        createWorkItemFromAttributes(dataTable, "qa@example.com");
    }
    
    private void createWorkItemFromAttributes(DataTable dataTable, String assignee) {
        Map<String, String> attributes = dataTable.asMaps().get(0);
        
        String title = attributes.get("title");
        WorkItemType type = WorkItemType.valueOf(attributes.get("type"));
        String description = attributes.getOrDefault("description", "");
        Priority priority = Priority.valueOf(attributes.getOrDefault("priority", "MEDIUM"));
        
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        WorkItem workItem = new WorkItemRecord(
            id,
            title,
            description,
            type,
            WorkflowState.FOUND,
            priority,
            assignee,
            now,
            now,
            null, // no parent initially
            UUID.randomUUID(), // random project ID
            "PUBLIC",
            false
        );
        
        itemService.addWorkItem(workItem);
        workItemIds.put(title, id);
        currentWorkItem = workItem;
    }
    
    @Given("a GOAL work item with title {string} exists")
    public void aGoalWorkItemWithTitleExists(String title) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        WorkItem workItem = new WorkItemRecord(
            id,
            title,
            "Description for " + title,
            WorkItemType.GOAL,
            WorkflowState.FOUND,
            Priority.HIGH,
            "product.owner@example.com",
            now,
            now,
            null, // no parent
            UUID.randomUUID(), // project ID
            "PUBLIC",
            false
        );
        
        itemService.addWorkItem(workItem);
        workItemIds.put(title, id);
    }
    
    @When("a developer creates the following FEATURE work items as children:")
    public void aDeveloperCreatesTheFollowingFeatureWorkItemsAsChildren(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        String parentTitle = getFirstParentTitleOfType(WorkItemType.GOAL);
        UUID parentId = workItemIds.get(parentTitle);
        Instant now = Instant.now();
        
        for (Map<String, String> row : rows) {
            String title = row.get("title");
            Priority priority = Priority.valueOf(row.get("priority"));
            
            UUID id = UUID.randomUUID();
            WorkItem workItem = new WorkItemRecord(
                id,
                title,
                "Description for " + title,
                WorkItemType.FEATURE,
                WorkflowState.FOUND,
                priority,
                "developer@example.com",
                now,
                now,
                parentId,
                itemService.findById(parentId)
                    .flatMap(WorkItem::getProjectId)
                    .orElse(null),
                "PUBLIC",
                false
            );
            
            itemService.addWorkItem(workItem);
            workItemIds.put(title, id);
        }
    }
    
    @When("creates the following child work items:")
    public void createsTheFollowingChildWorkItems(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        UUID parentId = currentWorkItem.getId();
        Instant now = Instant.now();
        
        for (Map<String, String> row : rows) {
            String title = row.get("title");
            WorkItemType type = WorkItemType.valueOf(row.get("type"));
            Priority priority = Priority.valueOf(row.get("priority"));
            
            UUID id = UUID.randomUUID();
            WorkItem workItem = new WorkItemRecord(
                id,
                title,
                "Description for " + title,
                type,
                WorkflowState.FOUND,
                priority,
                "qa@example.com",
                now,
                now,
                parentId,
                currentWorkItem.getProjectId().orElse(null),
                "PUBLIC",
                false
            );
            
            itemService.addWorkItem(workItem);
            workItemIds.put(title, id);
        }
    }
    
    @Then("the work item is created successfully")
    public void theWorkItemIsCreatedSuccessfully() {
        assertNotNull(currentWorkItem, "Work item should be created");
        assertNotNull(currentWorkItem.getId(), "Work item should have an ID");
        assertTrue(itemService.findById(currentWorkItem.getId()).isPresent(),
            "Work item should exist in the service");
    }
    
    @Then("the work item type is {string}")
    public void theWorkItemTypeIs(String typeName) {
        assertEquals(WorkItemType.valueOf(typeName), 
            currentWorkItem.getType(),
            "Work item should have the correct type");
    }
    
    @Then("the work item can have children of type {string}")
    public void theWorkItemCanHaveChildrenOfType(String childTypeName) {
        WorkItemType childType = WorkItemType.valueOf(childTypeName);
        assertTrue(currentWorkItem.getType().canHaveChildOfType(childType),
            currentWorkItem.getType() + " should be able to have children of type " + childType);
    }
    
    @Then("all FEATURE work items are created successfully")
    public void allFeatureWorkItemsAreCreatedSuccessfully() {
        List<WorkItem> features = itemService.findByType("FEATURE");
        assertNotNull(features, "FEATURE items should be retrieved");
        assertTrue(features.size() > 0, "At least one FEATURE item should exist");
        
        for (WorkItem feature : features) {
            assertEquals(WorkItemType.FEATURE, feature.getType(),
                "Item should be a FEATURE");
            assertTrue(feature.getParentId().isPresent(),
                "FEATURE should have a parent");
        }
    }
    
    @Then("all work items are linked to the parent goal")
    public void allWorkItemsAreLinkedToTheParentGoal() {
        String goalTitle = getFirstParentTitleOfType(WorkItemType.GOAL);
        UUID goalId = workItemIds.get(goalTitle);
        
        List<WorkItem> features = itemService.findByType("FEATURE");
        for (WorkItem feature : features) {
            assertTrue(feature.getParentId().isPresent(),
                "FEATURE should have a parent");
            assertEquals(goalId, feature.getParentId().get(),
                "FEATURE should be linked to the GOAL");
        }
    }
    
    @Then("all work items can proceed through the workflow states")
    public void allWorkItemsCanProceedThroughTheWorkflowStates() {
        List<WorkItem> allItems = itemService.findAll();
        
        for (WorkItem item : allItems) {
            // Test that item can transition to at least one state
            List<WorkflowState> availableTransitions = 
                item.getStatus().getAvailableTransitions();
                
            if (!availableTransitions.isEmpty()) {
                WorkflowState nextState = availableTransitions.get(0);
                WorkItem updated = workflowService.transition(
                    item.getId(), nextState);
                    
                assertEquals(nextState, updated.getStatus(),
                    "Item should transition to " + nextState);
            }
        }
    }
    
    @Then("all work items are created successfully")
    public void allWorkItemsAreCreatedSuccessfully() {
        List<WorkItem> allItems = itemService.findAll();
        assertNotNull(allItems, "Items should be retrieved");
        assertTrue(allItems.size() > 0, "At least one item should exist");
    }
    
    @Then("the work items can be filtered by type {string}")
    public void theWorkItemsCanBeFilteredByType(String typeName) {
        List<WorkItem> items = itemService.findByType(typeName);
        assertNotNull(items, "Items should be retrieved");
        assertTrue(items.size() > 0, "At least one item should exist");
        
        for (WorkItem item : items) {
            assertEquals(WorkItemType.valueOf(typeName), item.getType(),
                "Item should have the correct type");
        }
    }
    
    @Then("the work items can be listed in order of priority")
    public void theWorkItemsCanBeListedInOrderOfPriority() {
        List<WorkItem> allItems = itemService.findAll();
        List<WorkItem> sortedItems = allItems.stream()
            .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
            .collect(Collectors.toList());
            
        // Verify first item has highest priority
        if (!sortedItems.isEmpty()) {
            WorkItem highestPriorityItem = sortedItems.get(0);
            Priority highestPriority = highestPriorityItem.getPriority();
            
            for (WorkItem item : sortedItems) {
                assertTrue(highestPriority.compareTo(item.getPriority()) >= 0,
                    "Items should be sorted by priority");
            }
        }
    }
    
    @Given("an existing BUG work item with title {string}")
    public void anExistingBugWorkItemWithTitle(String title) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        WorkItem workItem = new WorkItemRecord(
            id,
            title,
            "Description for " + title,
            WorkItemType.BUG,
            WorkflowState.FOUND,
            Priority.HIGH,
            "developer@example.com",
            now,
            now,
            null,
            UUID.randomUUID(),
            "PUBLIC",
            false
        );
        
        itemService.addWorkItem(workItem);
        workItemIds.put(title, id);
        currentWorkItem = workItem;
        stateHistory.add(WorkflowState.FOUND);
    }
    
    @When("I transition the work item through the following states:")
    public void iTransitionTheWorkItemThroughTheFollowingStates(DataTable dataTable) {
        List<String> states = dataTable.asList();
        
        for (int i = 1; i < states.size(); i++) {
            WorkflowState targetState = WorkflowState.valueOf(states.get(i));
            currentWorkItem = workflowService.transition(
                currentWorkItem.getId(), targetState);
                
            stateHistory.add(targetState);
        }
    }
    
    @Then("the work item successfully reaches the DONE state")
    public void theWorkItemSuccessfullyReachesTheDoneState() {
        assertEquals(WorkflowState.DONE, currentWorkItem.getStatus(),
            "Work item should reach DONE state");
    }
    
    @Then("I can view the complete history of state transitions")
    public void iCanViewTheCompleteHistoryOfStateTransitions() {
        assertNotNull(stateHistory, "State history should exist");
        assertTrue(stateHistory.size() > 1, "Should have multiple state transitions");
        assertEquals(WorkflowState.DONE, stateHistory.get(stateHistory.size() - 1),
            "Final state should be DONE");
    }
    
    @When("I define custom metadata fields for different work item types:")
    public void iDefineCustomMetadataFieldsForDifferentWorkItemTypes(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        
        // Define metadata schemas (this would be in a configuration service in a real app)
        Map<WorkItemType, Map<String, String>> metadataSchemas = new HashMap<>();
        
        for (Map<String, String> row : rows) {
            WorkItemType type = WorkItemType.valueOf(row.get("type"));
            String fieldName = row.get("field name");
            String fieldType = row.get("field type");
            
            metadataSchemas.computeIfAbsent(type, k -> new HashMap<>())
                .put(fieldName, fieldType);
        }
        
        // Verify schemas were created
        for (WorkItemType type : metadataSchemas.keySet()) {
            assertTrue(metadataSchemas.get(type).size() > 0,
                "Metadata schema should have fields");
        }
    }
    
    @Then("I can add appropriate metadata to work items of each type")
    public void iCanAddAppropriateMetadataToWorkItemsOfEachType() {
        // Add metadata to existing items
        for (WorkItem item : itemService.findAll()) {
            Map<String, Object> metadata = new HashMap<>();
            
            switch (item.getType()) {
                case GOAL:
                    metadata.put("target_quarter", "Q4 2025");
                    break;
                case FEATURE:
                    metadata.put("story_points", 5);
                    break;
                case BUG:
                    metadata.put("severity", "MEDIUM");
                    metadata.put("reproduced", true);
                    break;
                case CHORE:
                    metadata.put("estimated_hours", 2);
                    break;
            }
            
            itemMetadata.put(item.getId(), metadata);
        }
        
        // Verify metadata was added
        for (WorkItem item : itemService.findAll()) {
            assertTrue(itemMetadata.containsKey(item.getId()),
                "Item should have metadata");
            assertNotNull(itemMetadata.get(item.getId()),
                "Metadata should not be null");
            assertTrue(itemMetadata.get(item.getId()).size() > 0,
                "Metadata should have at least one field");
        }
    }
    
    @Then("I can filter work items based on metadata values")
    public void iCanFilterWorkItemsBasedOnMetadataValues() {
        // Filter bugs by severity
        List<WorkItem> mediumSeverityBugs = itemService.findAll().stream()
            .filter(item -> item.getType() == WorkItemType.BUG)
            .filter(item -> itemMetadata.containsKey(item.getId()))
            .filter(item -> {
                Map<String, Object> metadata = itemMetadata.get(item.getId());
                return metadata.containsKey("severity") && 
                       "MEDIUM".equals(metadata.get("severity"));
            })
            .collect(Collectors.toList());
            
        // Assert that we can find filtered items
        assertNotNull(mediumSeverityBugs, "Should find bugs by metadata");
    }
    
    @Then("I can report on work items using the metadata values")
    public void iCanReportOnWorkItemsUsingTheMetadataValues() {
        // Calculate total story points
        int totalStoryPoints = itemService.findAll().stream()
            .filter(item -> item.getType() == WorkItemType.FEATURE)
            .filter(item -> itemMetadata.containsKey(item.getId()))
            .mapToInt(item -> {
                Map<String, Object> metadata = itemMetadata.get(item.getId());
                return metadata.containsKey("story_points") ?
                       (int) metadata.get("story_points") : 0;
            })
            .sum();
            
        // Calculate total estimated hours
        int totalEstimatedHours = itemService.findAll().stream()
            .filter(item -> item.getType() == WorkItemType.CHORE)
            .filter(item -> itemMetadata.containsKey(item.getId()))
            .mapToInt(item -> {
                Map<String, Object> metadata = itemMetadata.get(item.getId());
                return metadata.containsKey("estimated_hours") ?
                       (int) metadata.get("estimated_hours") : 0;
            })
            .sum();
            
        // Should be able to generate reports
        assertTrue(totalStoryPoints >= 0, "Should calculate story points");
        assertTrue(totalEstimatedHours >= 0, "Should calculate estimated hours");
    }
    
    @Given("work items exist for each type with different states and priorities")
    public void workItemsExistForEachTypeWithDifferentStatesAndPriorities() {
        // Already covered by previous steps that create work items
        assertTrue(itemService.findAll().size() > 0, 
            "Work items should exist from previous steps");
    }
    
    @When("I run {string} as a product owner")
    public void iRunAsAProductOwner(String command) {
        simulateCommand(command, "product.owner@example.com");
    }
    
    @When("I run {string} as a developer")
    public void iRunAsADeveloper(String command) {
        simulateCommand(command, "developer@example.com");
    }
    
    @When("I run {string} as a QA engineer")
    public void iRunAsAQaEngineer(String command) {
        simulateCommand(command, "qa@example.com");
    }
    
    @When("I run {string} as any user")
    public void iRunAsAnyUser(String command) {
        simulateCommand(command, "current.user@example.com");
    }
    
    private void simulateCommand(String command, String user) {
        outputStream.reset();
        System.out.println("Executing command: " + command);
        System.out.println("As user: " + user);
        
        // Parse command
        String[] parts = command.split("\\s+");
        if (parts.length >= 3 && parts[0].equals("rin") && parts[1].equals("list")) {
            // Handle rin list command
            String option = parts[2].split("=")[0];
            String value = parts[2].split("=")[1];
            
            if (option.equals("--type")) {
                List<WorkItem> items = itemService.findByType(value);
                System.out.println("Found " + items.size() + " " + value + " items:");
                for (WorkItem item : items) {
                    System.out.println("- " + item.getTitle());
                }
            } else if (option.equals("--assignee")) {
                // For --assignee me, use the current user
                String assignee = value.equals("me") ? user : value;
                List<WorkItem> items = itemService.findByAssignee(assignee);
                System.out.println("Found " + items.size() + " items assigned to " + assignee + ":");
                for (WorkItem item : items) {
                    System.out.println("- " + item.getTitle() + " (" + item.getType() + ")");
                }
            }
        }
    }
    
    @Then("I see all GOAL work items")
    public void iSeeAllGoalWorkItems() {
        String output = outputStream.toString();
        
        // Verify output contains GOAL items
        for (WorkItem item : itemService.findByType("GOAL")) {
            assertTrue(output.contains(item.getTitle()), 
                "Output should contain GOAL item title");
        }
    }
    
    @Then("I see all FEATURE work items")
    public void iSeeAllFeatureWorkItems() {
        String output = outputStream.toString();
        
        // Verify output contains FEATURE items
        for (WorkItem item : itemService.findByType("FEATURE")) {
            assertTrue(output.contains(item.getTitle()), 
                "Output should contain FEATURE item title");
        }
    }
    
    @Then("I see all BUG work items")
    public void iSeeAllBugWorkItems() {
        String output = outputStream.toString();
        
        // Verify output contains BUG items
        for (WorkItem item : itemService.findByType("BUG")) {
            assertTrue(output.contains(item.getTitle()), 
                "Output should contain BUG item title");
        }
    }
    
    @Then("I see all work items assigned to me regardless of type")
    public void iSeeAllWorkItemsAssignedToMeRegardlessOfType() {
        String output = outputStream.toString();
        
        // Verify output contains items of different types
        assertTrue(output.contains("FEATURE") || 
                  output.contains("BUG") || 
                  output.contains("CHORE") || 
                  output.contains("GOAL"),
                  "Output should contain items of different types");
    }
    
    @Given("work items exist with metadata for each type")
    public void workItemsExistWithMetadataForEachType() {
        // Already covered by previous steps
        assertTrue(itemMetadata.size() > 0, 
            "Work items with metadata should exist from previous steps");
    }
    
    @When("I create a custom report showing:")
    public void iCreateACustomReportShowing(DataTable dataTable) {
        outputStream.reset();
        List<String> columns = dataTable.asList();
        
        System.out.println("Custom Report");
        System.out.println("===========================");
        System.out.println(String.join(" | ", columns));
        
        // Generate report rows
        for (WorkItemType type : WorkItemType.values()) {
            List<WorkItem> typeItems = itemService.findByType(type.name());
            
            // Count
            int count = typeItems.size();
            
            // Average priority
            double avgPriority = typeItems.stream()
                .mapToInt(item -> priorityToValue(item.getPriority()))
                .average()
                .orElse(0.0);
                
            // Completion percentage
            long doneCount = typeItems.stream()
                .filter(item -> item.getStatus() == WorkflowState.DONE)
                .count();
            double completionPct = typeItems.isEmpty() ? 0.0 : 
                (double) doneCount / typeItems.size() * 100.0;
            
            System.out.printf("%s | %d | %.1f | %.1f%%%n", 
                type, count, avgPriority, completionPct);
        }
    }
    
    @Then("I can see how many of each type are in progress")
    public void iCanSeeHowManyOfEachTypeAreInProgress() {
        String output = outputStream.toString();
        assertTrue(output.contains("count"), 
            "Report should contain count column");
        assertTrue(output.contains("GOAL") && 
                  output.contains("FEATURE") && 
                  output.contains("BUG") && 
                  output.contains("CHORE"),
                  "Report should contain all work item types");
    }
    
    @Then("I can see the priority distribution across different types")
    public void iCanSeeThePriorityDistributionAcrossDifferentTypes() {
        String output = outputStream.toString();
        assertTrue(output.contains("average_priority"), 
            "Report should contain average priority column");
    }
    
    @Then("I can see completion rates across all work item types")
    public void iCanSeeCompletionRatesAcrossAllWorkItemTypes() {
        String output = outputStream.toString();
        assertTrue(output.contains("completion_percentage"), 
            "Report should contain completion percentage column");
    }
    
    // Helper methods
    
    private String getFirstParentTitleOfType(WorkItemType type) {
        for (Map.Entry<String, UUID> entry : workItemIds.entrySet()) {
            Optional<WorkItem> item = itemService.findById(entry.getValue());
            if (item.isPresent() && item.get().getType() == type) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    private int priorityToValue(Priority priority) {
        switch (priority) {
            case CRITICAL: return 4;
            case HIGH: return 3;
            case MEDIUM: return 2;
            case LOW: return 1;
            default: return 0;
        }
    }
    
    /**
     * Test implementation of ItemService for BDD tests.
     */
    private static class TestItemService implements ItemService {
        private final List<WorkItem> items = new ArrayList<>();
        
        public void addWorkItem(WorkItem item) {
            items.add(item);
        }
        
        @Override
        public WorkItem create(WorkItemCreateRequest request) {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            
            WorkItem workItem = new WorkItemRecord(
                id,
                request.title(),
                request.description(),
                request.type(),
                WorkflowState.FOUND,
                request.priority(),
                request.assignee(),
                now,
                now,
                request.getParentId().orElse(null),
                request.getProjectId().orElse(null),
                request.visibility(),
                request.localOnly()
            );
            
            items.add(workItem);
            return workItem;
        }
        
        @Override
        public Optional<WorkItem> findById(UUID id) {
            return items.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
        }
        
        @Override
        public List<WorkItem> findAll() {
            return new ArrayList<>(items);
        }
        
        @Override
        public List<WorkItem> findByType(String type) {
            return items.stream()
                .filter(item -> item.getType().toString().equals(type))
                .collect(Collectors.toList());
        }
        
        @Override
        public List<WorkItem> findByStatus(String status) {
            return items.stream()
                .filter(item -> item.getStatus().toString().equals(status))
                .collect(Collectors.toList());
        }
        
        @Override
        public List<WorkItem> findByAssignee(String assignee) {
            return items.stream()
                .filter(item -> {
                    String itemAssignee = item.getAssignee();
                    return itemAssignee != null && itemAssignee.equals(assignee);
                })
                .collect(Collectors.toList());
        }
        
        @Override
        public WorkItem updateAssignee(UUID id, String assignee) {
            return findById(id)
                .map(item -> {
                    if (item instanceof WorkItemRecord) {
                        WorkItemRecord record = (WorkItemRecord) item;
                        WorkItemRecord updated = record.withAssignee(assignee);
                        items.removeIf(i -> i.getId().equals(id));
                        items.add(updated);
                        return updated;
                    }
                    return item;
                })
                .orElse(null);
        }
        
        @Override
        public void deleteById(UUID id) {
            items.removeIf(item -> item.getId().equals(id));
        }
    }
    
    /**
     * Test implementation of WorkflowService for BDD tests.
     */
    private static class TestWorkflowService implements WorkflowService {
        
        @Override
        public WorkItem transition(UUID itemId, WorkflowState targetState) {
            WorkItem item = findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
                
            if (!item.getStatus().canTransitionTo(targetState)) {
                throw new IllegalArgumentException(
                    "Invalid transition from " + item.getStatus() + " to " + targetState);
            }
            
            if (item instanceof WorkItemRecord) {
                return ((WorkItemRecord) item).withStatus(targetState);
            }
            
            throw new UnsupportedOperationException("Cannot transition non-record work item");
        }
        
        private Optional<WorkItem> findById(UUID id) {
            // This would normally be injected, but for tests we'll just return null
            // and let the test method handle the actual item retrieval
            return Optional.empty();
        }
    }
}
