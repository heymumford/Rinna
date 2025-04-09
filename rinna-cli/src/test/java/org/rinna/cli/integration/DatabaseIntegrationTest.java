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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rinna.cli.command.AddCommand;
import org.rinna.cli.command.ListCommand;
import org.rinna.cli.command.UpdateCommand;
import org.rinna.cli.command.ViewCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.test.OutputAssertions;
import org.rinna.cli.util.ModelMapper;
import org.rinna.data.sqlite.SqliteConnectionManager;
import org.rinna.data.sqlite.SqliteRepositoryFactory;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.repository.MetadataRepository;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CLI-to-Database interactions.
 * These tests verify that CLI commands can correctly interact with
 * persistent storage via SQLite repositories.
 */
@Tag("integration")
@DisplayName("CLI to Database Integration Tests")
class DatabaseIntegrationTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private static SqliteRepositoryFactory repositoryFactory;
    private static ItemRepository coreItemRepository;
    private static MetadataRepository coreMetadataRepository;
    private static ItemService cliItemService;
    
    @TempDir
    static Path tempDir;
    
    @BeforeAll
    static void initDatabase() {
        // Initialize SQLite database in temporary directory
        SqliteConnectionManager connectionManager = new SqliteConnectionManager(
                tempDir.toString(), "cli-integration-test.db");
        
        // Create repository factory and core repositories
        repositoryFactory = new SqliteRepositoryFactory(connectionManager);
        coreItemRepository = repositoryFactory.getItemRepository();
        coreMetadataRepository = repositoryFactory.getMetadataRepository();
        
        // Create CLI item service that wraps the core repository
        cliItemService = createItemServiceWrapper(coreItemRepository);
        
        // Register with service manager
        ServiceManager.registerItemService(cliItemService);
    }
    
    @AfterAll
    static void closeDatabase() {
        if (repositoryFactory != null) {
            repositoryFactory.close();
        }
        
        // Reset service manager
        ServiceManager.reset();
    }
    
    @BeforeEach
    void setUp() {
        // Capture console output
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Creates an item service wrapper that delegates to the core ItemRepository.
     */
    private static ItemService createItemServiceWrapper(ItemRepository repository) {
        return new ItemService() {
            @Override
            public WorkItem getItem(String id) {
                try {
                    return repository.findById(UUID.fromString(id))
                        .map(DatabaseIntegrationTest::convertCoreToCliWorkItem)
                        .orElse(null);
                } catch (Exception e) {
                    return null;
                }
            }
            
            @Override
            public WorkItem createItem(String title, WorkItemType type, Priority priority, String description) {
                org.rinna.domain.model.WorkItemType coreType = org.rinna.domain.model.WorkItemType.valueOf(type.name());
                org.rinna.domain.model.Priority corePriority = org.rinna.domain.model.Priority.valueOf(priority.name());
                
                WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                    .title(title)
                    .description(description)
                    .type(coreType)
                    .priority(corePriority)
                    .build();
                
                org.rinna.domain.model.WorkItem coreItem = repository.create(request);
                return convertCoreToCliWorkItem(coreItem);
            }
            
            @Override
            public WorkItem updateItem(WorkItem item) {
                org.rinna.domain.model.WorkItem coreItem = convertCliToCoreWorkItem(item);
                org.rinna.domain.model.WorkItem updatedItem = repository.save(coreItem);
                return convertCoreToCliWorkItem(updatedItem);
            }
            
            @Override
            public List<WorkItem> getAllItems() {
                return repository.findAll().stream()
                    .map(DatabaseIntegrationTest::convertCoreToCliWorkItem)
                    .collect(Collectors.toList());
            }
            
            @Override
            public List<WorkItem> getItemsByType(WorkItemType type) {
                org.rinna.domain.model.WorkItemType coreType = org.rinna.domain.model.WorkItemType.valueOf(type.name());
                return repository.findByType(coreType).stream()
                    .map(DatabaseIntegrationTest::convertCoreToCliWorkItem)
                    .collect(Collectors.toList());
            }
            
            @Override
            public List<WorkItem> getItemsByState(org.rinna.cli.model.WorkflowState state) {
                org.rinna.domain.model.WorkflowState coreState = 
                    org.rinna.domain.model.WorkflowState.valueOf(state.name());
                return repository.findByStatus(coreState).stream()
                    .map(DatabaseIntegrationTest::convertCoreToCliWorkItem)
                    .collect(Collectors.toList());
            }
        };
    }
    
    /**
     * Converts a core domain WorkItem to a CLI WorkItem.
     */
    private static WorkItem convertCoreToCliWorkItem(org.rinna.domain.model.WorkItem coreItem) {
        WorkItem cliItem = new WorkItem();
        cliItem.setId(coreItem.getId().toString());
        cliItem.setTitle(coreItem.getTitle());
        cliItem.setDescription(coreItem.getDescription());
        cliItem.setType(WorkItemType.valueOf(coreItem.getType().name()));
        cliItem.setPriority(Priority.valueOf(coreItem.getPriority().name()));
        cliItem.setState(org.rinna.cli.model.WorkflowState.valueOf(coreItem.getStatus().name()));
        
        if (coreItem.getAssignee() != null) {
            cliItem.setAssignee(coreItem.getAssignee());
        }
        
        if (coreItem.getCreatedAt() != null) {
            cliItem.setCreated(coreItem.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
        
        if (coreItem.getUpdatedAt() != null) {
            cliItem.setUpdated(coreItem.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
        
        return cliItem;
    }
    
    /**
     * Converts a CLI WorkItem to a core domain WorkItem.
     */
    private static org.rinna.domain.model.WorkItem convertCliToCoreWorkItem(WorkItem cliItem) {
        org.rinna.domain.model.WorkItemType type = org.rinna.domain.model.WorkItemType.valueOf(cliItem.getType().name());
        org.rinna.domain.model.Priority priority = org.rinna.domain.model.Priority.valueOf(cliItem.getPriority().name());
        org.rinna.domain.model.WorkflowState status = org.rinna.domain.model.WorkflowState.valueOf(cliItem.getState().name());
        
        java.time.Instant createdAt = cliItem.getCreated() != null
            ? cliItem.getCreated().atZone(java.time.ZoneId.systemDefault()).toInstant()
            : java.time.Instant.now();
            
        java.time.Instant updatedAt = cliItem.getUpdated() != null
            ? cliItem.getUpdated().atZone(java.time.ZoneId.systemDefault()).toInstant()
            : java.time.Instant.now();
        
        return new org.rinna.domain.model.WorkItemRecord(
            UUID.fromString(cliItem.getId()),
            cliItem.getTitle(),
            cliItem.getDescription(),
            type,
            status,
            priority,
            cliItem.getAssignee(),
            createdAt,
            updatedAt,
            null, // parentId
            null, // projectId
            null, // visibility
            false // localOnly
        );
    }
    
    @Nested
    @DisplayName("CLI to Database Command Tests")
    class CliToDatabaseCommandTests {

        @BeforeEach
        void clearDatabase() {
            // Clear the database before each test
            List<org.rinna.domain.model.WorkItem> allItems = coreItemRepository.findAll();
            for (org.rinna.domain.model.WorkItem item : allItems) {
                coreItemRepository.deleteById(item.getId());
            }
        }
        
        @Test
        @DisplayName("Should persist work item with AddCommand and retrieve with ViewCommand")
        void shouldPersistWorkItemWithAddCommandAndRetrieveWithViewCommand() {
            // Setup AddCommand
            AddCommand addCmd = new AddCommand();
            addCmd.setTitle("Database Integration Test Item");
            addCmd.setType(WorkItemType.TASK);
            addCmd.setPriority(Priority.HIGH);
            addCmd.setDescription("Testing persistence with SQLite");
            
            // Execute command
            int exitCode = addCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Add command should execute successfully");
            
            // Extract the ID from the output
            String output = outputStream.toString();
            String itemId = extractItemId(output);
            assertNotNull(itemId, "Item ID should be extractable from output");
            
            // Reset output stream
            outputStream.reset();
            
            // Setup ViewCommand with the extracted ID
            ViewCommand viewCmd = new ViewCommand();
            viewCmd.setId(itemId);
            
            // Execute command
            exitCode = viewCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "View command should execute successfully");
            
            // Verify output contains the work item details
            output = outputStream.toString();
            assertTrue(output.contains(itemId), "Output should contain the item ID");
            assertTrue(output.contains("Database Integration Test Item"), "Output should contain the item title");
            assertTrue(output.contains("TASK"), "Output should contain the item type");
            assertTrue(output.contains("HIGH"), "Output should contain the item priority");
            
            // Verify the item was actually persisted in the database
            org.rinna.domain.model.WorkItem persistedItem = coreItemRepository.findById(UUID.fromString(itemId))
                .orElse(null);
            assertNotNull(persistedItem, "Item should exist in the database");
            assertEquals("Database Integration Test Item", persistedItem.getTitle());
            assertEquals("Testing persistence with SQLite", persistedItem.getDescription());
            assertEquals(org.rinna.domain.model.WorkItemType.TASK, persistedItem.getType());
            assertEquals(org.rinna.domain.model.Priority.HIGH, persistedItem.getPriority());
        }
        
        @Test
        @DisplayName("Should list persisted work items with ListCommand")
        void shouldListPersistedWorkItemsWithListCommand() {
            // Create multiple work items in the database
            for (int i = 0; i < 3; i++) {
                org.rinna.domain.model.WorkItemType type = i % 2 == 0 
                    ? org.rinna.domain.model.WorkItemType.TASK 
                    : org.rinna.domain.model.WorkItemType.BUG;
                
                org.rinna.domain.model.Priority priority = i % 3 == 0 
                    ? org.rinna.domain.model.Priority.HIGH 
                    : org.rinna.domain.model.Priority.MEDIUM;
                
                WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                    .title("Database Test Item " + i)
                    .description("Description for item " + i)
                    .type(type)
                    .priority(priority)
                    .build();
                
                coreItemRepository.create(request);
            }
            
            // Setup ListCommand
            ListCommand listCmd = new ListCommand();
            
            // Execute command
            int exitCode = listCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "List command should execute successfully");
            
            // Verify output contains all work items
            String output = outputStream.toString();
            assertTrue(output.contains("Database Test Item 0"), "Output should contain first item");
            assertTrue(output.contains("Database Test Item 1"), "Output should contain second item");
            assertTrue(output.contains("Database Test Item 2"), "Output should contain third item");
            
            // Test filtering by type
            outputStream.reset();
            ListCommand filterCmd = new ListCommand();
            filterCmd.setType(WorkItemType.BUG);
            
            // Execute filtered command
            exitCode = filterCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Filtered list command should execute successfully");
            
            // Verify output contains only BUG items
            output = outputStream.toString();
            assertFalse(output.contains("Database Test Item 0"), "Output should not contain TASK items");
            assertTrue(output.contains("Database Test Item 1"), "Output should contain BUG items");
        }
        
        @Test
        @DisplayName("Should update persisted work item with UpdateCommand")
        void shouldUpdatePersistedWorkItemWithUpdateCommand() {
            // Create a work item in the database first
            WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Original Title")
                .description("Original description")
                .type(org.rinna.domain.model.WorkItemType.TASK)
                .priority(org.rinna.domain.model.Priority.MEDIUM)
                .build();
            
            org.rinna.domain.model.WorkItem createdItem = coreItemRepository.create(request);
            String itemId = createdItem.getId().toString();
            
            // Setup UpdateCommand
            UpdateCommand updateCmd = new UpdateCommand();
            updateCmd.setId(itemId);
            updateCmd.setTitle("Updated Title");
            updateCmd.setPriority(Priority.HIGH);
            
            // Execute command
            int exitCode = updateCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Update command should execute successfully");
            
            // Verify item was updated in the database
            org.rinna.domain.model.WorkItem updatedItem = coreItemRepository.findById(UUID.fromString(itemId))
                .orElse(null);
            
            assertNotNull(updatedItem, "Item should still exist in the database");
            assertEquals("Updated Title", updatedItem.getTitle(), "Title should be updated");
            assertEquals(org.rinna.domain.model.Priority.HIGH, updatedItem.getPriority(), "Priority should be updated");
            assertEquals("Original description", updatedItem.getDescription(), "Description should not be changed");
        }
    }
    
    /**
     * Helper method to extract an item ID from command output.
     */
    private String extractItemId(String output) {
        // Look for UUIDs in the output using regex
        Pattern uuidPattern = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", 
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = uuidPattern.matcher(output);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }
}