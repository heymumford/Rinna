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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.UnitTest;
import org.rinna.cli.command.LsCommand;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.HistoryEntry;
import org.rinna.domain.HistoryEntryType;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemRelationshipType;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.HistoryService;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.RelationshipService;

/**
 * Unit tests for the LsCommand class.
 */
@DisplayName("LsCommand Unit Tests")
public class LsCommandTest extends UnitTest {
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ContextManager contextManager;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private RelationshipService relationshipService;
    
    @Mock
    private HistoryService historyService;
    
    @Mock
    private WorkItem item1;
    
    @Mock
    private WorkItem item2;
    
    @Mock
    private WorkItem item3;
    
    @Mock
    private HistoryEntry historyEntry1;
    
    @Mock
    private HistoryEntry historyEntry2;
    
    private LsCommand lsCommand;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private final UUID item1Id = UUID.randomUUID();
    private final UUID item2Id = UUID.randomUUID();
    private final UUID item3Id = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up System.out/err capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Set up default command
        lsCommand = new LsCommand();
        setMockServiceManager();
        
        // Reset captors between tests
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Set up mock work items
        setupMockWorkItems();
        
        // Set up mock history entries
        setupMockHistoryEntries();
    }
    
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Sets up a mock ServiceManager to be used by the LsCommand.
     */
    private void setMockServiceManager() {
        try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(serviceManager);
            
            // Set up mock responses
            when(serviceManager.getItemService()).thenReturn(itemService);
            when(serviceManager.getRelationshipService()).thenReturn(relationshipService);
            when(serviceManager.getHistoryService()).thenReturn(historyService);
        }
        
        try (var contextManagerMock = mockStatic(ContextManager.class)) {
            contextManagerMock.when(ContextManager::getInstance).thenReturn(contextManager);
        }
    }
    
    /**
     * Sets up mock work items for testing.
     */
    private void setupMockWorkItems() {
        // Set up mock work item 1
        when(item1.id()).thenReturn(item1Id);
        when(item1.title()).thenReturn("Implement login screen");
        when(item1.description()).thenReturn("Create a login screen with validation");
        when(item1.type()).thenReturn(WorkItemType.TASK);
        when(item1.priority()).thenReturn(Priority.MEDIUM);
        when(item1.state()).thenReturn(WorkflowState.IN_PROGRESS);
        when(item1.assignee()).thenReturn("bob");
        when(item1.reporter()).thenReturn("alice");
        when(item1.createdAt()).thenReturn(Instant.now().minusSeconds(86400)); // 1 day ago
        when(item1.updatedAt()).thenReturn(Instant.now().minusSeconds(3600));  // 1 hour ago
        
        // Set up mock work item 2
        when(item2.id()).thenReturn(item2Id);
        when(item2.title()).thenReturn("Create user registration");
        when(item2.description()).thenReturn("Implement user registration form");
        when(item2.type()).thenReturn(WorkItemType.TASK);
        when(item2.priority()).thenReturn(Priority.LOW);
        when(item2.state()).thenReturn(WorkflowState.READY);
        when(item2.assignee()).thenReturn("alice");
        when(item2.reporter()).thenReturn("bob");
        when(item2.createdAt()).thenReturn(Instant.now().minusSeconds(172800)); // 2 days ago
        when(item2.updatedAt()).thenReturn(Instant.now().minusSeconds(7200));   // 2 hours ago
        
        // Set up mock work item 3
        when(item3.id()).thenReturn(item3Id);
        when(item3.title()).thenReturn("Design password reset");
        when(item3.description()).thenReturn("Create password reset flow");
        when(item3.type()).thenReturn(WorkItemType.TASK);
        when(item3.priority()).thenReturn(Priority.HIGH);
        when(item3.state()).thenReturn(WorkflowState.BACKLOG);
        when(item3.assignee()).thenReturn("bob");
        when(item3.reporter()).thenReturn("charlie");
        when(item3.createdAt()).thenReturn(Instant.now().minusSeconds(259200)); // 3 days ago
        when(item3.updatedAt()).thenReturn(Instant.now().minusSeconds(10800));  // 3 hours ago
        
        // Set up mock service to return these work items
        when(itemService.getItem(item1Id)).thenReturn(item1);
        when(itemService.getItem(item2Id)).thenReturn(item2);
        when(itemService.getItem(item3Id)).thenReturn(item3);
        when(itemService.getAllWorkItems()).thenReturn(Arrays.asList(item1, item2, item3));
        
        // Set up parent-child relationships
        when(relationshipService.getParentWorkItem(item2Id)).thenReturn(item1Id);
        when(relationshipService.getParentWorkItem(item1Id)).thenReturn(null);
        when(relationshipService.getParentWorkItem(item3Id)).thenReturn(null);
        
        when(relationshipService.getChildWorkItems(item1Id)).thenReturn(Collections.singletonList(item2Id));
        when(relationshipService.getChildWorkItems(item2Id)).thenReturn(Collections.emptyList());
        when(relationshipService.getChildWorkItems(item3Id)).thenReturn(Collections.emptyList());
        
        when(relationshipService.getRelationshipType(item2Id, item1Id)).thenReturn(WorkItemRelationshipType.CHILD_OF);
    }
    
    /**
     * Sets up mock history entries for testing.
     */
    private void setupMockHistoryEntries() {
        // Set up history entry 1
        when(historyEntry1.id()).thenReturn(UUID.randomUUID());
        when(historyEntry1.workItemId()).thenReturn(item1Id);
        when(historyEntry1.type()).thenReturn(HistoryEntryType.STATE_CHANGE);
        when(historyEntry1.user()).thenReturn("alice");
        when(historyEntry1.timestamp()).thenReturn(Instant.now().minusSeconds(43200)); // 12 hours ago
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("previousState", "READY");
        metadata1.put("newState", "IN_PROGRESS");
        when(historyEntry1.metadata()).thenReturn(metadata1);
        
        // Set up history entry 2
        when(historyEntry2.id()).thenReturn(UUID.randomUUID());
        when(historyEntry2.workItemId()).thenReturn(item1Id);
        when(historyEntry2.type()).thenReturn(HistoryEntryType.PRIORITY_CHANGE);
        when(historyEntry2.user()).thenReturn("bob");
        when(historyEntry2.timestamp()).thenReturn(Instant.now().minusSeconds(21600)); // 6 hours ago
        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("previousPriority", "LOW");
        metadata2.put("newPriority", "MEDIUM");
        when(historyEntry2.metadata()).thenReturn(metadata2);
        
        // Set up mock service to return these history entries
        when(historyService.getHistory(item1Id)).thenReturn(Arrays.asList(historyEntry1, historyEntry2));
        when(historyService.getHistory(item2Id)).thenReturn(Collections.emptyList());
        when(historyService.getHistory(item3Id)).thenReturn(Collections.emptyList());
    }
    
    @Test
    @DisplayName("Basic ls command should show work items with inheritance")
    void basicLsCommandShouldShowWorkItemsWithInheritance() {
        // Execute
        Integer result = lsCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain work item IDs and titles
        assertTrue(output.contains(item1Id.toString()), "Output should contain item1 ID");
        assertTrue(output.contains("Implement login screen"), "Output should contain item1 title");
        assertTrue(output.contains(item2Id.toString()), "Output should contain item2 ID");
        assertTrue(output.contains("Create user registration"), "Output should contain item2 title");
        assertTrue(output.contains(item3Id.toString()), "Output should contain item3 ID");
        assertTrue(output.contains("Design password reset"), "Output should contain item3 title");
        
        // Should show parent-child relationships
        assertTrue(output.contains("↳"), "Output should show parent-child relationship marker");
        
        // Should not contain detailed information
        assertFalse(output.contains("Description:"), "Output should not contain detailed fields");
        assertFalse(output.contains("Created:"), "Output should not contain creation time");
        assertFalse(output.contains("History:"), "Output should not contain history");
        
        // Verify that context is NOT updated for basic ls without specific item
        verify(contextManager, never()).setLastViewedWorkItem(any());
    }
    
    @Test
    @DisplayName("Long ls command should show detailed work item information")
    void longLsCommandShouldShowDetailedWorkItemInformation() {
        // Setup
        lsCommand.setLongFormat(true);
        
        // Execute
        Integer result = lsCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain work item IDs, titles, and detailed information
        assertTrue(output.contains(item1Id.toString()), "Output should contain item1 ID");
        assertTrue(output.contains("Implement login screen"), "Output should contain item1 title");
        assertTrue(output.contains("Description: Create a login screen with validation"), 
                "Output should contain item1 description");
        assertTrue(output.contains("MEDIUM"), "Output should contain item1 priority");
        assertTrue(output.contains("IN_PROGRESS"), "Output should contain item1 state");
        assertTrue(output.contains("Assignee: bob"), "Output should contain item1 assignee");
        
        // Should show parent-child relationships
        assertTrue(output.contains("Parent:"), "Output should show parent relationship");
        assertTrue(output.contains("Children:"), "Output should show children relationship");
        
        // Should NOT contain history
        assertFalse(output.contains("History:"), "Output should not contain history");
        
        // Verify that context is updated with the last viewed work item (last in list)
        verify(contextManager).setLastViewedWorkItem(item3Id);
    }
    
    @Test
    @DisplayName("Super long ls command should show detailed info with history")
    void superLongLsCommandShouldShowDetailedInfoWithHistory() {
        // Setup
        lsCommand.setLongFormat(true);
        lsCommand.setAllFormat(true);
        
        // Execute
        Integer result = lsCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain work item IDs, titles, detailed information, and history
        assertTrue(output.contains(item1Id.toString()), "Output should contain item1 ID");
        assertTrue(output.contains("Implement login screen"), "Output should contain item1 title");
        assertTrue(output.contains("Description: Create a login screen with validation"), 
                "Output should contain item1 description");
        assertTrue(output.contains("MEDIUM"), "Output should contain item1 priority");
        assertTrue(output.contains("IN_PROGRESS"), "Output should contain item1 state");
        assertTrue(output.contains("Assignee: bob"), "Output should contain item1 assignee");
        
        // Should show parent-child relationships
        assertTrue(output.contains("Parent:"), "Output should show parent relationship");
        assertTrue(output.contains("Children:"), "Output should show children relationship");
        
        // Should contain history
        assertTrue(output.contains("History:"), "Output should contain history section");
        assertTrue(output.contains("STATE_CHANGE"), "Output should contain state change history");
        assertTrue(output.contains("READY → IN_PROGRESS"), "Output should contain state change details");
        assertTrue(output.contains("PRIORITY_CHANGE"), "Output should contain priority change history");
        
        // Verify that context is updated with the last viewed work item (last in list)
        verify(contextManager).setLastViewedWorkItem(item3Id);
    }
    
    @Test
    @DisplayName("Ls command with specific ID should show only that work item")
    void lsCommandWithSpecificIdShouldShowOnlyThatWorkItem() {
        // Setup
        lsCommand.setItemId(item2Id.toString());
        
        // Execute
        Integer result = lsCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain only item2 information
        assertTrue(output.contains(item2Id.toString()), "Output should contain item2 ID");
        assertTrue(output.contains("Create user registration"), "Output should contain item2 title");
        
        // Should show that it's a child of item1
        assertTrue(output.contains("Child of: " + item1Id.toString()), 
                "Output should show parent relationship");
        
        // Should not contain information about other work items
        assertFalse(output.contains("Implement login screen"), "Output should not contain item1 title");
        assertFalse(output.contains("Design password reset"), "Output should not contain item3 title");
        
        // Verify that context is updated
        verify(contextManager).setLastViewedWorkItem(item2Id);
    }
    
    @Test
    @DisplayName("Long ls command with specific ID should show detailed info for that work item")
    void longLsCommandWithSpecificIdShouldShowDetailedInfoForThatWorkItem() {
        // Setup
        lsCommand.setLongFormat(true);
        lsCommand.setItemId(item3Id.toString());
        
        // Execute
        Integer result = lsCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain detailed item3 information
        assertTrue(output.contains(item3Id.toString()), "Output should contain item3 ID");
        assertTrue(output.contains("Design password reset"), "Output should contain item3 title");
        assertTrue(output.contains("Description: Create password reset flow"), 
                "Output should contain item3 description");
        assertTrue(output.contains("HIGH"), "Output should contain item3 priority");
        assertTrue(output.contains("BACKLOG"), "Output should contain item3 state");
        assertTrue(output.contains("Assignee: bob"), "Output should contain item3 assignee");
        
        // Should not contain information about other work items
        assertFalse(output.contains("Implement login screen"), "Output should not contain item1 title");
        assertFalse(output.contains("Create user registration"), "Output should not contain item2 title");
        
        // Verify that context is updated
        verify(contextManager).setLastViewedWorkItem(item3Id);
    }
    
    @Test
    @DisplayName("Super long ls command with specific ID should show detailed info with history")
    void superLongLsCommandWithSpecificIdShouldShowDetailedInfoWithHistory() {
        // Setup
        lsCommand.setLongFormat(true);
        lsCommand.setAllFormat(true);
        lsCommand.setItemId(item1Id.toString());
        
        // Execute
        Integer result = lsCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain detailed item1 information and history
        assertTrue(output.contains(item1Id.toString()), "Output should contain item1 ID");
        assertTrue(output.contains("Implement login screen"), "Output should contain item1 title");
        assertTrue(output.contains("Description: Create a login screen with validation"), 
                "Output should contain item1 description");
        assertTrue(output.contains("MEDIUM"), "Output should contain item1 priority");
        assertTrue(output.contains("IN_PROGRESS"), "Output should contain item1 state");
        assertTrue(output.contains("Assignee: bob"), "Output should contain item1 assignee");
        
        // Should show parent-child relationships
        assertTrue(output.contains("Children: " + item2Id.toString()), 
                "Output should show children relationship");
        
        // Should contain history
        assertTrue(output.contains("History:"), "Output should contain history section");
        assertTrue(output.contains("STATE_CHANGE"), "Output should contain state change history");
        assertTrue(output.contains("READY → IN_PROGRESS"), "Output should contain state change details");
        assertTrue(output.contains("PRIORITY_CHANGE"), "Output should contain priority change history");
        
        // Should not contain information about other work items
        assertFalse(output.contains("Create user registration"), "Output should not contain item2 title");
        assertFalse(output.contains("Design password reset"), "Output should not contain item3 title");
        
        // Verify that context is updated
        verify(contextManager).setLastViewedWorkItem(item1Id);
    }
    
    @Test
    @DisplayName("Ls command should fail with non-existent work item ID")
    void lsCommandShouldFailWithNonExistentWorkItemId() {
        // Setup
        String nonExistentId = UUID.randomUUID().toString();
        lsCommand.setItemId(nonExistentId);
        when(itemService.getItem(UUID.fromString(nonExistentId))).thenReturn(null);
        
        // Execute
        Integer result = lsCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Work item not found"), 
                "Error output should indicate item not found");
        
        // Verify that context is not updated
        verify(contextManager, never()).setLastViewedWorkItem(any());
    }
    
    @Test
    @DisplayName("Ls command should fail with invalid work item ID format")
    void lsCommandShouldFailWithInvalidWorkItemIdFormat() {
        // Setup
        lsCommand.setItemId("invalid-id");
        
        // Execute
        Integer result = lsCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid work item ID format"), 
                "Error output should indicate invalid ID format");
        
        // Verify that context is not updated
        verify(contextManager, never()).setLastViewedWorkItem(any());
    }
    
    @Test
    @DisplayName("Ls command should handle command injection attempts")
    void lsCommandShouldHandleCommandInjectionAttempts() {
        // Setup
        lsCommand.setItemId("5; echo HACKED");
        
        // Execute
        Integer result = lsCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid work item ID format"), 
                "Error output should indicate invalid ID format");
        
        // Verify that context is not updated
        verify(contextManager, never()).setLastViewedWorkItem(any());
    }
}