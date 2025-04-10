/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.rinna.cli.command.CatCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

/**
 * Component test for CatCommand integration with other components.
 */
@DisplayName("CatCommand Component Tests")
public class CatCommandComponentTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private ItemService itemService;
    private MockHistoryService historyService;
    private ContextManager contextManager;
    private MetadataService metadataService;
    
    private static final String TEST_WORK_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    private WorkItem testWorkItem;
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize test work item
        testWorkItem = createTestWorkItem();
        
        // Initialize service components
        mockServiceManager = mock(ServiceManager.class);
        itemService = mock(ItemService.class);
        historyService = mock(MockHistoryService.class);
        contextManager = mock(ContextManager.class);
        metadataService = mock(MetadataService.class);
        
        // Configure mocks
        when(mockServiceManager.getItemService()).thenReturn(itemService);
        when(mockServiceManager.getMockHistoryService()).thenReturn(historyService);
        when(mockServiceManager.getMetadataService()).thenReturn(metadataService);
        when(metadataService.startOperation(anyString(), anyString(), any())).thenReturn("test-operation-id");
        
        // Set up work item
        when(itemService.getItem(TEST_WORK_ITEM_ID)).thenReturn(testWorkItem);
        
        // Set up history
        List<MockHistoryService.HistoryEntryRecord> historyEntries = createHistoryEntries();
        when(historyService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(historyEntries);
        
        // Mock ContextManager.getInstance()
        try (var staticMock = Mockito.mockStatic(ContextManager.class)) {
            staticMock.when(ContextManager::getInstance).thenReturn(contextManager);
        }
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Reset output capture
        outputCaptor.reset();
        errorCaptor.reset();
    }
    
    @Test
    @DisplayName("Should display work item details and integrate with related components")
    void shouldDisplayWorkItemDetailsAndIntegrateWithRelatedComponents() {
        // Given
        try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            CatCommand command = new CatCommand(mockServiceManager);
            command.setItemId(TEST_WORK_ITEM_ID);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            
            // Verify basic content
            assertTrue(output.contains("====== WORK ITEM " + TEST_WORK_ITEM_ID + " ======"));
            assertTrue(output.contains("Title: Component Test Item"));
            
            // Verify component interactions
            verify(itemService).getItem(TEST_WORK_ITEM_ID);
            verify(contextManager).setLastViewedWorkItem(UUID.fromString(TEST_WORK_ITEM_ID));
            verify(metadataService).startOperation(eq("cat"), eq("READ"), any());
            verify(metadataService).completeOperation(eq("test-operation-id"), any());
        }
    }
    
    @Test
    @DisplayName("Should display history and integrate with history service")
    void shouldDisplayHistoryAndIntegrateWithHistoryService() {
        // Given
        try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            CatCommand command = new CatCommand(mockServiceManager);
            command.setItemId(TEST_WORK_ITEM_ID);
            command.setShowHistory(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            
            // Verify history content
            assertTrue(output.contains("History:"));
            assertTrue(output.contains("CREATED by john.doe"));
            assertTrue(output.contains("UPDATED by jane.smith"));
            
            // Verify component interactions
            verify(historyService).getHistory(UUID.fromString(TEST_WORK_ITEM_ID));
        }
    }
    
    @Test
    @DisplayName("Should use context manager to load last viewed item")
    void shouldUseContextManagerToLoadLastViewedItem() {
        // Given
        try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            // Configure context manager to return last viewed item
            when(contextManager.getLastViewedWorkItem()).thenReturn(UUID.fromString(TEST_WORK_ITEM_ID));
            
            CatCommand command = new CatCommand(mockServiceManager);
            // No item ID specified
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify component interactions
            verify(contextManager).getLastViewedWorkItem();
            verify(itemService).getItem(TEST_WORK_ITEM_ID);
            verify(contextManager).setLastViewedWorkItem(UUID.fromString(TEST_WORK_ITEM_ID));
        }
    }
    
    @Test
    @DisplayName("Should track operation status with metadata service")
    void shouldTrackOperationStatusWithMetadataService() {
        // Given
        try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            CatCommand command = new CatCommand(mockServiceManager);
            command.setItemId(TEST_WORK_ITEM_ID);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify metadata service interactions
            verify(metadataService).startOperation(eq("cat"), eq("READ"), any());
            verify(metadataService).completeOperation(eq("test-operation-id"), any());
            verify(metadataService, never()).failOperation(anyString(), any());
        }
    }
    
    @Test
    @DisplayName("Should track operation failure with metadata service")
    void shouldTrackOperationFailureWithMetadataService() {
        // Given
        try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            // Configure item service to throw exception
            RuntimeException testException = new RuntimeException("Component test exception");
            when(itemService.getItem(TEST_WORK_ITEM_ID)).thenThrow(testException);
            
            CatCommand command = new CatCommand(mockServiceManager);
            command.setItemId(TEST_WORK_ITEM_ID);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify metadata service interactions
            verify(metadataService).startOperation(eq("cat"), eq("READ"), any());
            verify(metadataService, never()).completeOperation(anyString(), any());
            verify(metadataService).failOperation(eq("test-operation-id"), eq(testException));
        }
    }
    
    @Test
    @DisplayName("Should generate JSON output that conforms to expected format")
    void shouldGenerateJsonOutputThatConformsToExpectedFormat() {
        // Given
        try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            CatCommand command = new CatCommand(mockServiceManager);
            command.setItemId(TEST_WORK_ITEM_ID);
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            
            // Verify JSON format and structure
            assertTrue(output.contains("\"id\": \"" + TEST_WORK_ITEM_ID + "\""));
            assertTrue(output.contains("\"title\": \"Component Test Item\""));
            assertTrue(output.contains("\"type\": \"TASK\""));
            assertTrue(output.contains("\"priority\": \"MEDIUM\""));
            assertTrue(output.contains("\"status\": \"IN_PROGRESS\""));
            assertTrue(output.contains("\"displayOptions\": {"));
            
            // Verify the output is valid JSON
            assertTrue(output.trim().startsWith("{"));
            assertTrue(output.trim().endsWith("}"));
        }
    }
    
    // Helper methods
    
    private WorkItem createTestWorkItem() {
        WorkItem workItem = new WorkItem();
        workItem.setId(TEST_WORK_ITEM_ID);
        workItem.setTitle("Component Test Item");
        workItem.setDescription("This is a component test work item");
        workItem.setType(WorkItemType.TASK);
        workItem.setPriority(Priority.MEDIUM);
        workItem.setState(WorkflowState.IN_PROGRESS);
        workItem.setAssignee("john.doe");
        workItem.setCreated(LocalDateTime.now().minus(7, ChronoUnit.DAYS));
        workItem.setUpdated(LocalDateTime.now().minus(1, ChronoUnit.DAYS));
        return workItem;
    }
    
    private List<MockHistoryService.HistoryEntryRecord> createHistoryEntries() {
        List<MockHistoryService.HistoryEntryRecord> entries = new ArrayList<>();
        
        // Created entry
        MockHistoryService.HistoryEntryRecord createdEntry = new MockHistoryService.HistoryEntryRecord(
            UUID.randomUUID(),
            UUID.fromString(TEST_WORK_ITEM_ID),
            "CREATED",
            "john.doe",
            "Item created",
            Date.from(LocalDateTime.now().minus(7, ChronoUnit.DAYS)
                .atZone(java.time.ZoneId.systemDefault()).toInstant())
        );
        entries.add(createdEntry);
        
        // Updated entry
        MockHistoryService.HistoryEntryRecord updatedEntry = new MockHistoryService.HistoryEntryRecord(
            UUID.randomUUID(),
            UUID.fromString(TEST_WORK_ITEM_ID),
            "UPDATED",
            "jane.smith",
            "Updated description",
            Date.from(LocalDateTime.now().minus(5, ChronoUnit.DAYS)
                .atZone(java.time.ZoneId.systemDefault()).toInstant())
        );
        entries.add(updatedEntry);
        
        // State change entry
        MockHistoryService.HistoryEntryRecord stateChangeEntry = new MockHistoryService.HistoryEntryRecord(
            UUID.randomUUID(),
            UUID.fromString(TEST_WORK_ITEM_ID),
            "STATE_CHANGE",
            "john.doe",
            "Changed state from CREATED to IN_PROGRESS",
            Date.from(LocalDateTime.now().minus(3, ChronoUnit.DAYS)
                .atZone(java.time.ZoneId.systemDefault()).toInstant())
        );
        entries.add(stateChangeEntry);
        
        return entries;
    }
}