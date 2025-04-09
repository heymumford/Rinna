/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.unit.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.UnitTest;
import org.rinna.cli.command.PrintCommand;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.HistoryEntry;
import org.rinna.domain.HistoryEntryType;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemMetadata;
import org.rinna.domain.WorkItemRelationshipType;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.HistoryService;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.MetadataService;
import org.rinna.usecase.RelationshipService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the PrintCommand class.
 */
@DisplayName("PrintCommand Unit Tests")
public class PrintCommandTest extends UnitTest {
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private RelationshipService relationshipService;
    
    @Mock
    private HistoryService historyService;
    
    @Mock
    private MetadataService metadataService;
    
    @Mock
    private WorkItem mockWorkItem;
    
    @Mock
    private WorkItem mockParentWorkItem;
    
    @Mock
    private WorkItemMetadata mockMetadata;
    
    @Mock
    private HistoryEntry mockHistoryEntry1;
    
    @Mock
    private HistoryEntry mockHistoryEntry2;
    
    private PrintCommand printCommand;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private final UUID workItemId = UUID.randomUUID();
    private final UUID parentId = UUID.randomUUID();
    private final UUID child1Id = UUID.randomUUID();
    private final UUID child2Id = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up System.out/err capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Set up default command
        printCommand = new PrintCommand();
        setMockServiceManager();
        
        // Reset captors between tests
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Set up mock work item
        setupMockWorkItem();
    }
    
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Sets up a mock ServiceManager to be used by the PrintCommand.
     */
    private void setMockServiceManager() {
        try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(serviceManager);
            
            // Set up mock responses
            when(serviceManager.getItemService()).thenReturn(itemService);
            when(serviceManager.getRelationshipService()).thenReturn(relationshipService);
            when(serviceManager.getHistoryService()).thenReturn(historyService);
            when(serviceManager.getMetadataService()).thenReturn(metadataService);
        }
    }
    
    /**
     * Sets up a mock work item and related data for testing.
     */
    private void setupMockWorkItem() {
        // Set up mock work item
        when(mockWorkItem.id()).thenReturn(workItemId);
        when(mockWorkItem.title()).thenReturn("Implement registration form");
        when(mockWorkItem.description()).thenReturn("Create a new registration form with validation");
        when(mockWorkItem.type()).thenReturn(WorkItemType.TASK);
        when(mockWorkItem.priority()).thenReturn(Priority.MEDIUM);
        when(mockWorkItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
        when(mockWorkItem.assignee()).thenReturn("bob");
        when(mockWorkItem.reporter()).thenReturn("alice");
        when(mockWorkItem.createdAt()).thenReturn(Instant.now().minusSeconds(86400)); // 1 day ago
        when(mockWorkItem.updatedAt()).thenReturn(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        // Set up mock parent work item
        when(mockParentWorkItem.id()).thenReturn(parentId);
        when(mockParentWorkItem.title()).thenReturn("User Authentication Feature");
        when(mockParentWorkItem.type()).thenReturn(WorkItemType.FEATURE);
        when(mockParentWorkItem.priority()).thenReturn(Priority.HIGH);
        when(mockParentWorkItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
        
        // Set up mock relationships
        when(relationshipService.getParentWorkItem(workItemId)).thenReturn(parentId);
        when(relationshipService.getRelationshipType(workItemId, parentId)).thenReturn(WorkItemRelationshipType.CHILD_OF);
        when(relationshipService.getChildWorkItems(workItemId)).thenReturn(Arrays.asList(child1Id, child2Id));
        
        when(itemService.getItem(workItemId)).thenReturn(mockWorkItem);
        when(itemService.getItem(parentId)).thenReturn(mockParentWorkItem);
        
        // Set up mock history entries
        when(mockHistoryEntry1.timestamp()).thenReturn(Instant.now().minusSeconds(86400));
        when(mockHistoryEntry1.type()).thenReturn(HistoryEntryType.STATE_CHANGE);
        when(mockHistoryEntry1.user()).thenReturn("alice");
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("previousState", "READY");
        metadata1.put("newState", "IN_PROGRESS");
        when(mockHistoryEntry1.metadata()).thenReturn(metadata1);
        
        when(mockHistoryEntry2.timestamp()).thenReturn(Instant.now().minusSeconds(3600));
        when(mockHistoryEntry2.type()).thenReturn(HistoryEntryType.ASSIGNMENT_CHANGE);
        when(mockHistoryEntry2.user()).thenReturn("alice");
        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("previousAssignee", "alice");
        metadata2.put("newAssignee", "bob");
        when(mockHistoryEntry2.metadata()).thenReturn(metadata2);
        
        when(historyService.getHistory(workItemId)).thenReturn(Arrays.asList(mockHistoryEntry2, mockHistoryEntry1));
        
        // Set up mock metadata
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("estimated_hours", "8");
        metadataMap.put("actual_hours", "6");
        metadataMap.put("sprint", "Sprint 42");
        metadataMap.put("story_points", "5");
        metadataMap.put("test_coverage", "87.5%");
        when(mockMetadata.getAllMetadata()).thenReturn(metadataMap);
        
        when(metadataService.getMetadata(workItemId)).thenReturn(mockMetadata);
    }
    
    @Test
    @DisplayName("Should print all work item information")
    void shouldPrintAllWorkItemInformation() {
        // Setup
        printCommand.setId(workItemId.toString());
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Basic fields
        assertTrue(output.contains("ID:"), "Should show ID field");
        assertTrue(output.contains(workItemId.toString()), "Should show work item ID");
        assertTrue(output.contains("Title:"), "Should show Title field");
        assertTrue(output.contains("Implement registration form"), "Should show work item title");
        assertTrue(output.contains("Description:"), "Should show Description field");
        assertTrue(output.contains("Create a new registration form with validation"), "Should show work item description");
        assertTrue(output.contains("Type:"), "Should show Type field");
        assertTrue(output.contains("TASK"), "Should show work item type");
        assertTrue(output.contains("Priority:"), "Should show Priority field");
        assertTrue(output.contains("MEDIUM"), "Should show work item priority");
        assertTrue(output.contains("State:"), "Should show State field");
        assertTrue(output.contains("IN_PROGRESS"), "Should show work item state");
        assertTrue(output.contains("Assignee:"), "Should show Assignee field");
        assertTrue(output.contains("bob"), "Should show assignee name");
        
        // Relationships
        assertTrue(output.contains("Parent:"), "Should show Parent field");
        assertTrue(output.contains(parentId.toString()), "Should show parent ID");
        assertTrue(output.contains("User Authentication Feature"), "Should show parent title");
        assertTrue(output.contains("Children:"), "Should show Children field");
        assertTrue(output.contains(child1Id.toString()), "Should show child1 ID");
        assertTrue(output.contains(child2Id.toString()), "Should show child2 ID");
        
        // History
        assertTrue(output.contains("History:"), "Should show History section");
        assertTrue(output.contains("STATE_CHANGE"), "Should show state change history entry");
        assertTrue(output.contains("READY → IN_PROGRESS"), "Should show state change details");
        assertTrue(output.contains("ASSIGNMENT_CHANGE"), "Should show assignment change history entry");
        assertTrue(output.contains("alice → bob"), "Should show assignment change details");
        
        // Metadata
        assertTrue(output.contains("Metadata:"), "Should show Metadata section");
        assertTrue(output.contains("estimated_hours: 8"), "Should show estimated hours metadata");
        assertTrue(output.contains("actual_hours: 6"), "Should show actual hours metadata");
        assertTrue(output.contains("sprint: Sprint 42"), "Should show sprint metadata");
        assertTrue(output.contains("story_points: 5"), "Should show story points metadata");
        assertTrue(output.contains("test_coverage: 87.5%"), "Should show test coverage metadata");
    }
    
    @Test
    @DisplayName("Should handle work item with no parent")
    void shouldHandleWorkItemWithNoParent() {
        // Setup
        printCommand.setId(workItemId.toString());
        when(relationshipService.getParentWorkItem(workItemId)).thenReturn(null);
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        assertTrue(output.contains("Parent: None"), "Should show 'None' for parent");
    }
    
    @Test
    @DisplayName("Should handle work item with no children")
    void shouldHandleWorkItemWithNoChildren() {
        // Setup
        printCommand.setId(workItemId.toString());
        when(relationshipService.getChildWorkItems(workItemId)).thenReturn(Collections.emptyList());
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        assertTrue(output.contains("Children: None"), "Should show 'None' for children");
    }
    
    @Test
    @DisplayName("Should handle work item with no history")
    void shouldHandleWorkItemWithNoHistory() {
        // Setup
        printCommand.setId(workItemId.toString());
        when(historyService.getHistory(workItemId)).thenReturn(Collections.emptyList());
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        assertTrue(output.contains("History: None"), "Should show 'None' for history");
    }
    
    @Test
    @DisplayName("Should handle work item with no metadata")
    void shouldHandleWorkItemWithNoMetadata() {
        // Setup
        printCommand.setId(workItemId.toString());
        when(metadataService.getMetadata(workItemId)).thenReturn(null);
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        assertTrue(output.contains("Metadata: None"), "Should show 'None' for metadata");
    }
    
    @Test
    @DisplayName("Should fail when work item not found")
    void shouldFailWhenWorkItemNotFound() {
        // Setup
        printCommand.setId(UUID.randomUUID().toString());
        when(itemService.getItem(any(UUID.class))).thenReturn(null);
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Work item not found"), 
                "Should show error message for not found item");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid-id", "123", "", "null", "undefined"})
    @DisplayName("Should fail with invalid work item ID")
    void shouldFailWithInvalidWorkItemId(String invalidId) {
        // Setup
        printCommand.setId(invalidId);
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid work item ID"), 
                "Should show error message for invalid ID");
    }
    
    @Test
    @DisplayName("Should handle malicious input safely")
    void shouldHandleMaliciousInputSafely() {
        // Setup
        String maliciousInput = "'; DROP TABLE WORKITEMS; --";
        printCommand.setId(maliciousInput);
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid work item ID"), 
                "Should show appropriate error message for malicious input");
    }
    
    @Test
    @DisplayName("Should format history entries chronologically")
    void shouldFormatHistoryEntriesChronologically() {
        // Setup
        printCommand.setId(workItemId.toString());
        
        // Create a list of history entries in non-chronological order
        List<HistoryEntry> historyEntries = Arrays.asList(mockHistoryEntry2, mockHistoryEntry1);
        when(historyService.getHistory(workItemId)).thenReturn(historyEntries);
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify history entries are displayed chronologically (oldest first)
        String output = outputCaptor.toString();
        int stateChangeIndex = output.indexOf("STATE_CHANGE");
        int assignmentChangeIndex = output.indexOf("ASSIGNMENT_CHANGE");
        
        assertTrue(stateChangeIndex < assignmentChangeIndex, 
                "Older state change entry should appear before newer assignment change entry");
    }
    
    @Test
    @DisplayName("Should format metadata in a readable format")
    void shouldFormatMetadataReadably() {
        // Setup
        printCommand.setId(workItemId.toString());
        
        // Execute
        Integer result = printCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify metadata is formatted with key-value pairs
        String output = outputCaptor.toString();
        assertTrue(output.contains("Metadata:"), "Should show Metadata section");
        
        // Verify metadata formatting
        String metadataSection = output.substring(output.indexOf("Metadata:"));
        assertTrue(metadataSection.contains("estimated_hours: 8"), "Should format metadata as key: value");
        assertTrue(metadataSection.contains("actual_hours: 6"), "Should format metadata as key: value");
        assertTrue(metadataSection.contains("sprint: Sprint 42"), "Should format metadata as key: value");
    }
}