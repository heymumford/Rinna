/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rinna.Rinna;
import org.rinna.cli.command.AddCommand;
import org.rinna.cli.command.ListCommand;
import org.rinna.cli.command.UpdateCommand;
import org.rinna.cli.command.ViewCommand;
import org.rinna.cli.command.WorkflowCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.WorkflowService;
import org.rinna.cli.test.OutputAssertions;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.StateMapper;
import org.rinna.domain.model.WorkItemCreateRequest;

/**
 * Integration tests for CLI-to-Core interactions.
 * These tests verify that CLI commands can correctly interact with Core domain services.
 */
@Tag("integration")
@DisplayName("CLI to Core Integration Tests")
class CliToCoreIntegrationTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager serviceManager;
    private ItemService itemService;
    private WorkflowService workflowService;
    private Rinna rinnaCore;
    
    @BeforeEach
    void setUp() {
        // Capture console output
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
        
        // Initialize Rinna core
        rinnaCore = Rinna.initialize();
        
        // Initialize CLI service wrappers for the core services
        serviceManager = ServiceManager.getInstance();
        itemService = createItemServiceWrapper(rinnaCore);
        workflowService = createWorkflowServiceWrapper(rinnaCore);
        
        // Replace service manager's services with our wrappers
        // This would typically be done by a dependency injection framework
        ServiceManager.registerItemService(itemService);
        ServiceManager.registerWorkflowService(workflowService);
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Reset service manager
        ServiceManager.reset();
    }
    
    /**
     * Creates an item service wrapper that delegates to the core ItemService.
     */
    private ItemService createItemServiceWrapper(Rinna rinna) {
        return new ItemService() {
            @Override
            public WorkItem getItem(String id) {
                try {
                    org.rinna.domain.model.WorkItem coreItem = rinna.items().getById(UUID.fromString(id));
                    return ModelMapper.toCliWorkItem(coreItem);
                } catch (Exception e) {
                    return null;
                }
            }
            
            @Override
            public WorkItem createItem(String title, WorkItemType type, Priority priority, String description) {
                WorkItemCreateRequest request = new WorkItemCreateRequest();
                request.setTitle(title);
                request.setDescription(description);
                request.setType(org.rinna.domain.model.WorkItemType.valueOf(type.name()));
                request.setPriority(org.rinna.domain.model.Priority.valueOf(priority.name()));
                
                org.rinna.domain.model.WorkItem coreItem = rinna.items().create(request);
                return ModelMapper.toCliWorkItem(coreItem);
            }
            
            @Override
            public WorkItem updateItem(WorkItem item) {
                org.rinna.domain.model.WorkItem coreItem = ModelMapper.toDomainWorkItem(item);
                org.rinna.domain.model.WorkItem updatedItem = rinna.items().update(coreItem);
                return ModelMapper.toCliWorkItem(updatedItem);
            }
            
            @Override
            public java.util.List<WorkItem> getAllItems() {
                return rinna.items().getAll().stream()
                    .map(ModelMapper::toCliWorkItem)
                    .collect(java.util.stream.Collectors.toList());
            }
            
            @Override
            public java.util.List<WorkItem> getItemsByType(WorkItemType type) {
                return rinna.items().getByType(org.rinna.domain.model.WorkItemType.valueOf(type.name())).stream()
                    .map(ModelMapper::toCliWorkItem)
                    .collect(java.util.stream.Collectors.toList());
            }
            
            @Override
            public java.util.List<WorkItem> getItemsByState(WorkflowState state) {
                String coreState = StateMapper.toCoreState(state);
                return rinna.items().getByState(coreState).stream()
                    .map(ModelMapper::toCliWorkItem)
                    .collect(java.util.stream.Collectors.toList());
            }
        };
    }
    
    /**
     * Creates a workflow service wrapper that delegates to the core WorkflowService.
     */
    private WorkflowService createWorkflowServiceWrapper(Rinna rinna) {
        return new WorkflowService() {
            @Override
            public void transition(String itemId, WorkflowState targetState) {
                String coreState = StateMapper.toCoreState(targetState);
                rinna.workflow().transition(UUID.fromString(itemId), coreState);
            }
            
            @Override
            public boolean canTransition(String itemId, WorkflowState targetState) {
                String coreState = StateMapper.toCoreState(targetState);
                return rinna.workflow().canTransition(UUID.fromString(itemId), coreState);
            }
            
            @Override
            public java.util.List<WorkflowState> getAvailableTransitions(String itemId) {
                return rinna.workflow().getAvailableTransitions(UUID.fromString(itemId)).stream()
                    .map(StateMapper::fromCoreState)
                    .collect(java.util.stream.Collectors.toList());
            }
        };
    }
    
    @Nested
    @DisplayName("Add Command Integration Tests")
    class AddCommandIntegrationTests {
        
        @Test
        @DisplayName("Should create work item in core system")
        void shouldCreateWorkItemInCoreSystem() {
            // Setup AddCommand
            AddCommand addCmd = new AddCommand();
            addCmd.setTitle("Test Integration Item");
            addCmd.setType(WorkItemType.TASK);
            addCmd.setPriority(Priority.HIGH);
            addCmd.setDescription("This is a test item for CLI-Core integration");
            
            // Execute command
            int exitCode = addCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify output contains confirmation
            OutputAssertions.assertSuccessMessage(outputStream.toString(), "created");
            
            // Extract the ID from the output
            String output = outputStream.toString();
            String itemId = extractItemId(output);
            
            // Verify item was created in core system
            assertNotNull(itemId, "Item ID should be extractable from output");
            org.rinna.domain.model.WorkItem coreItem = rinnaCore.items().getById(UUID.fromString(itemId));
            assertNotNull(coreItem, "Item should exist in core system");
            assertEquals("Test Integration Item", coreItem.getTitle(), "Item title should match");
            assertEquals(org.rinna.domain.model.WorkItemType.TASK, coreItem.getType(), "Item type should match");
            assertEquals(org.rinna.domain.model.Priority.HIGH, coreItem.getPriority(), "Item priority should match");
        }
    }
    
    @Nested
    @DisplayName("View Command Integration Tests")
    class ViewCommandIntegrationTests {
        
        private String testItemId;
        
        @BeforeEach
        void createTestItem() {
            // Create a test item in the core system
            WorkItemCreateRequest request = new WorkItemCreateRequest();
            request.setTitle("View Test Item");
            request.setDescription("Item for testing the view command");
            request.setType(org.rinna.domain.model.WorkItemType.BUG);
            request.setPriority(org.rinna.domain.model.Priority.CRITICAL);
            
            org.rinna.domain.model.WorkItem coreItem = rinnaCore.items().create(request);
            testItemId = coreItem.getId().toString();
        }
        
        @Test
        @DisplayName("Should view work item from core system")
        void shouldViewWorkItemFromCoreSystem() {
            // Setup ViewCommand
            ViewCommand viewCmd = new ViewCommand();
            viewCmd.setId(testItemId);
            
            // Execute command
            int exitCode = viewCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify output contains the work item details
            String output = outputStream.toString();
            assertTrue(output.contains(testItemId), "Output should contain the item ID");
            assertTrue(output.contains("View Test Item"), "Output should contain the item title");
            assertTrue(output.contains("BUG"), "Output should contain the item type");
            assertTrue(output.contains("CRITICAL"), "Output should contain the item priority");
        }
    }
    
    @Nested
    @DisplayName("List Command Integration Tests")
    class ListCommandIntegrationTests {
        
        @BeforeEach
        void createTestItems() {
            // Create multiple test items in the core system
            for (int i = 0; i < 3; i++) {
                WorkItemCreateRequest request = new WorkItemCreateRequest();
                request.setTitle("List Test Item " + i);
                request.setDescription("Item for testing the list command");
                request.setType(i % 2 == 0 ? org.rinna.domain.model.WorkItemType.TASK : org.rinna.domain.model.WorkItemType.BUG);
                request.setPriority(org.rinna.domain.model.Priority.MEDIUM);
                
                rinnaCore.items().create(request);
            }
        }
        
        @Test
        @DisplayName("Should list all work items from core system")
        void shouldListAllWorkItemsFromCoreSystem() {
            // Setup ListCommand
            ListCommand listCmd = new ListCommand();
            
            // Execute command
            int exitCode = listCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify output contains all test items
            String output = outputStream.toString();
            assertTrue(output.contains("List Test Item 0"), "Output should contain first test item");
            assertTrue(output.contains("List Test Item 1"), "Output should contain second test item");
            assertTrue(output.contains("List Test Item 2"), "Output should contain third test item");
        }
        
        @Test
        @DisplayName("Should filter work items by type")
        void shouldFilterWorkItemsByType() {
            // Setup ListCommand with type filter
            ListCommand listCmd = new ListCommand();
            listCmd.setType(WorkItemType.BUG);
            
            // Execute command
            int exitCode = listCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify output contains only BUG items
            String output = outputStream.toString();
            assertFalse(output.contains("List Test Item 0"), "Output should not contain TASK items");
            assertTrue(output.contains("List Test Item 1"), "Output should contain BUG items");
        }
    }
    
    @Nested
    @DisplayName("Update Command Integration Tests")
    class UpdateCommandIntegrationTests {
        
        private String testItemId;
        
        @BeforeEach
        void createTestItem() {
            // Create a test item in the core system
            WorkItemCreateRequest request = new WorkItemCreateRequest();
            request.setTitle("Original Title");
            request.setDescription("Original description");
            request.setType(org.rinna.domain.model.WorkItemType.TASK);
            request.setPriority(org.rinna.domain.model.Priority.MEDIUM);
            
            org.rinna.domain.model.WorkItem coreItem = rinnaCore.items().create(request);
            testItemId = coreItem.getId().toString();
        }
        
        @Test
        @DisplayName("Should update work item in core system")
        void shouldUpdateWorkItemInCoreSystem() {
            // Setup UpdateCommand
            UpdateCommand updateCmd = new UpdateCommand();
            updateCmd.setId(testItemId);
            updateCmd.setTitle("Updated Title");
            updateCmd.setPriority(Priority.HIGH);
            
            // Execute command
            int exitCode = updateCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify output contains confirmation
            String output = outputStream.toString();
            assertTrue(output.contains("Updated work item"), "Output should confirm update");
            
            // Verify item was updated in core system
            org.rinna.domain.model.WorkItem updatedItem = rinnaCore.items().getById(UUID.fromString(testItemId));
            assertNotNull(updatedItem, "Item should exist in core system");
            assertEquals("Updated Title", updatedItem.getTitle(), "Item title should be updated");
            assertEquals(org.rinna.domain.model.Priority.HIGH, updatedItem.getPriority(), "Item priority should be updated");
            assertEquals("Original description", updatedItem.getDescription(), "Item description should not change");
        }
    }
    
    @Nested
    @DisplayName("Workflow Command Integration Tests")
    class WorkflowCommandIntegrationTests {
        
        private String testItemId;
        
        @BeforeEach
        void createTestItem() {
            // Create a test item in the core system
            WorkItemCreateRequest request = new WorkItemCreateRequest();
            request.setTitle("Workflow Test Item");
            request.setDescription("Item for testing workflow transitions");
            request.setType(org.rinna.domain.model.WorkItemType.TASK);
            request.setPriority(org.rinna.domain.model.Priority.MEDIUM);
            
            org.rinna.domain.model.WorkItem coreItem = rinnaCore.items().create(request);
            testItemId = coreItem.getId().toString();
            
            // Make sure it's in the READY state
            rinnaCore.workflow().transition(UUID.fromString(testItemId), "TO_DO");
        }
        
        @Test
        @DisplayName("Should transition work item state in core system")
        void shouldTransitionWorkItemStateInCoreSystem() {
            // Setup WorkflowCommand
            WorkflowCommand workflowCmd = new WorkflowCommand();
            workflowCmd.setItemId(testItemId);
            workflowCmd.setTargetState(WorkflowState.IN_PROGRESS);
            
            // Execute command
            int exitCode = workflowCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify output contains confirmation
            String output = outputStream.toString();
            assertTrue(output.contains("Updated work item") && output.contains("IN_PROGRESS"), 
                "Output should confirm state transition");
            
            // Verify item state was updated in core system
            org.rinna.domain.model.WorkItem updatedItem = rinnaCore.items().getById(UUID.fromString(testItemId));
            assertNotNull(updatedItem, "Item should exist in core system");
            assertEquals("IN_PROGRESS", updatedItem.getState(), "Item state should be updated");
        }
    }
    
    /**
     * Helper method to extract an item ID from command output.
     */
    private String extractItemId(String output) {
        // Look for UUIDs in the output using regex
        java.util.regex.Pattern uuidPattern = java.util.regex.Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = uuidPattern.matcher(output);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }
}