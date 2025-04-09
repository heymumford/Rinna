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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.UnitTest;
import org.rinna.cli.command.ListCommand;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemRelationship;
import org.rinna.domain.WorkItemRelationshipType;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.RelationshipService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
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
 * Unit tests for the ListCommand class focusing on parent-child hierarchy features.
 */
@DisplayName("ListCommand Parent-Child Features Unit Tests")
public class ListParentCommandTest extends UnitTest {
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private RelationshipService relationshipService;
    
    private ListCommand listCommand;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    // Sample work items
    private WorkItem parent1;
    private WorkItem parent2;
    private WorkItem child1;
    private WorkItem child2;
    private WorkItem child3;
    private WorkItem grandchild1;
    
    private final UUID parent1Id = UUID.randomUUID();
    private final UUID parent2Id = UUID.randomUUID();
    private final UUID child1Id = UUID.randomUUID();
    private final UUID child2Id = UUID.randomUUID();
    private final UUID child3Id = UUID.randomUUID();
    private final UUID grandchild1Id = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up System.out/err capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Set up default command
        listCommand = new ListCommand();
        setMockServiceManager();
        
        // Reset captors between tests
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Create sample work items
        setupSampleWorkItems();
    }
    
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Sets up a mock ServiceManager to be used by the ListCommand.
     */
    private void setMockServiceManager() {
        try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(serviceManager);
            
            // Set up mock responses
            when(serviceManager.getItemService()).thenReturn(itemService);
            when(serviceManager.getRelationshipService()).thenReturn(relationshipService);
        }
    }
    
    /**
     * Sets up sample work items for testing.
     */
    private void setupSampleWorkItems() {
        // Create parent work items
        parent1 = createWorkItem(parent1Id, "User Auth Features", WorkItemType.FEATURE, Priority.HIGH, WorkflowState.IN_PROGRESS);
        parent2 = createWorkItem(parent2Id, "Admin Functions", WorkItemType.FEATURE, Priority.MEDIUM, WorkflowState.READY);
        
        // Create child work items
        child1 = createWorkItem(child1Id, "Implement login screen", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
        child2 = createWorkItem(child2Id, "Create user registration", WorkItemType.TASK, Priority.LOW, WorkflowState.READY);
        child3 = createWorkItem(child3Id, "Design password reset", WorkItemType.TASK, Priority.HIGH, WorkflowState.BACKLOG);
        
        // Create grandchild work item
        grandchild1 = createWorkItem(grandchild1Id, "Login form validation", WorkItemType.TASK, Priority.LOW, WorkflowState.IN_PROGRESS);
        
        // Set up parent-child relationships
        when(relationshipService.getParentWorkItem(child1Id)).thenReturn(parent1Id);
        when(relationshipService.getParentWorkItem(child2Id)).thenReturn(parent1Id);
        when(relationshipService.getParentWorkItem(child3Id)).thenReturn(parent2Id);
        when(relationshipService.getParentWorkItem(grandchild1Id)).thenReturn(child1Id);
        
        when(relationshipService.getChildWorkItems(parent1Id)).thenReturn(Arrays.asList(child1Id, child2Id));
        when(relationshipService.getChildWorkItems(parent2Id)).thenReturn(Collections.singletonList(child3Id));
        when(relationshipService.getChildWorkItems(child1Id)).thenReturn(Collections.singletonList(grandchild1Id));
        when(relationshipService.getChildWorkItems(child2Id)).thenReturn(Collections.emptyList());
        when(relationshipService.getChildWorkItems(child3Id)).thenReturn(Collections.emptyList());
        when(relationshipService.getChildWorkItems(grandchild1Id)).thenReturn(Collections.emptyList());
        
        // Set up relationship types
        when(relationshipService.getRelationshipType(child1Id, parent1Id)).thenReturn(WorkItemRelationshipType.CHILD_OF);
        when(relationshipService.getRelationshipType(child2Id, parent1Id)).thenReturn(WorkItemRelationshipType.CHILD_OF);
        when(relationshipService.getRelationshipType(child3Id, parent2Id)).thenReturn(WorkItemRelationshipType.CHILD_OF);
        when(relationshipService.getRelationshipType(grandchild1Id, child1Id)).thenReturn(WorkItemRelationshipType.CHILD_OF);
    }
    
    /**
     * Helper method to create a work item.
     */
    private WorkItem createWorkItem(UUID id, String title, WorkItemType type, Priority priority, WorkflowState state) {
        WorkItem workItem = mock(WorkItem.class);
        when(workItem.id()).thenReturn(id);
        when(workItem.title()).thenReturn(title);
        when(workItem.type()).thenReturn(type);
        when(workItem.priority()).thenReturn(priority);
        when(workItem.state()).thenReturn(state);
        when(workItem.assignee()).thenReturn("bob");
        
        when(itemService.getItem(id)).thenReturn(workItem);
        
        return workItem;
    }
    
    @Test
    @DisplayName("Should list only parent work items with 'p' option")
    void shouldListOnlyParentWorkItems() {
        // Setup
        listCommand.setViewMode("p");
        
        // Mock service to return all work items
        List<WorkItem> allWorkItems = Arrays.asList(parent1, parent2, child1, child2, child3, grandchild1);
        when(itemService.getAllWorkItems()).thenReturn(allWorkItems);
        
        // Mock relationship service to identify parents
        when(relationshipService.hasChildren(parent1Id)).thenReturn(true);
        when(relationshipService.hasChildren(parent2Id)).thenReturn(true);
        when(relationshipService.hasChildren(child1Id)).thenReturn(true);
        when(relationshipService.hasChildren(child2Id)).thenReturn(false);
        when(relationshipService.hasChildren(child3Id)).thenReturn(false);
        when(relationshipService.hasChildren(grandchild1Id)).thenReturn(false);
        
        // Execute
        Integer result = listCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Should only display parent1, parent2, and child1 (they have children)
        String output = outputCaptor.toString();
        assertTrue(output.contains(parent1Id.toString()), "Parent1 should be in results");
        assertTrue(output.contains(parent2Id.toString()), "Parent2 should be in results");
        assertTrue(output.contains(child1Id.toString()), "Child1 should be in results (it's a parent to grandchild1)");
        
        // Should not display child2, child3, or grandchild1 (they don't have children)
        assertFalse(output.contains(child2Id.toString()), "Child2 should not be in results");
        assertFalse(output.contains(child3Id.toString()), "Child3 should not be in results");
        assertFalse(output.contains(grandchild1Id.toString()), "Grandchild1 should not be in results");
    }
    
    @Test
    @DisplayName("Should display pretty inheritance diagram with 'pretty' option")
    void shouldDisplayPrettyInheritanceDiagram() {
        // Setup
        listCommand.setViewMode("pretty");
        
        // Mock service to return all work items
        List<WorkItem> allWorkItems = Arrays.asList(parent1, parent2, child1, child2, child3, grandchild1);
        when(itemService.getAllWorkItems()).thenReturn(allWorkItems);
        
        // Create a parent map for the hierarchy
        Map<UUID, UUID> parentMap = new HashMap<>();
        parentMap.put(child1Id, parent1Id);
        parentMap.put(child2Id, parent1Id);
        parentMap.put(child3Id, parent2Id);
        parentMap.put(grandchild1Id, child1Id);
        
        when(relationshipService.getAllParentChildRelationships()).thenReturn(parentMap);
        
        // Execute
        Integer result = listCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // The output should contain ASCII art for hierarchy
        String output = outputCaptor.toString();
        
        // Check for hierarchy formatting
        assertTrue(output.contains("User Auth Features"), "Should show parent1 title");
        assertTrue(output.contains("  ├── Implement login screen"), "Should show child1 indented under parent1");
        assertTrue(output.contains("  │   └── Login form validation"), "Should show grandchild indented under child1");
        assertTrue(output.contains("  └── Create user registration"), "Should show child2 indented under parent1");
        assertTrue(output.contains("Admin Functions"), "Should show parent2 title");
        assertTrue(output.contains("  └── Design password reset"), "Should show child3 indented under parent2");
    }
    
    @Test
    @DisplayName("Should handle no parent-child relationships gracefully")
    void shouldHandleNoParentChildRelationshipsGracefully() {
        // Setup
        listCommand.setViewMode("pretty");
        
        // Mock service to return work items with no relationships
        List<WorkItem> workItems = Arrays.asList(child1, child2, child3);
        when(itemService.getAllWorkItems()).thenReturn(workItems);
        
        // Empty parent map
        when(relationshipService.getAllParentChildRelationships()).thenReturn(new HashMap<>());
        
        // Execute
        Integer result = listCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Should show a message about no relationships
        assertTrue(outputCaptor.toString().contains("No parent-child relationships found"), 
                "Should show message about no relationships");
    }
    
    @Test
    @DisplayName("Should list root parents at the top level")
    void shouldListRootParentsAtTopLevel() {
        // Setup
        listCommand.setViewMode("pretty");
        
        // Mock service to return all work items
        List<WorkItem> allWorkItems = Arrays.asList(parent1, parent2, child1, child2, child3, grandchild1);
        when(itemService.getAllWorkItems()).thenReturn(allWorkItems);
        
        // Create a parent map for the hierarchy
        Map<UUID, UUID> parentMap = new HashMap<>();
        parentMap.put(child1Id, parent1Id);
        parentMap.put(child2Id, parent1Id);
        parentMap.put(child3Id, parent2Id);
        parentMap.put(grandchild1Id, child1Id);
        
        when(relationshipService.getAllParentChildRelationships()).thenReturn(parentMap);
        
        // Execute
        Integer result = listCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify parent items are at top level (no indentation)
        String output = outputCaptor.toString();
        String[] lines = output.split("\\r?\\n");
        
        boolean parent1Found = false;
        boolean parent2Found = false;
        
        for (String line : lines) {
            if (line.contains("User Auth Features") && !line.startsWith(" ")) {
                parent1Found = true;
            } else if (line.contains("Admin Functions") && !line.startsWith(" ")) {
                parent2Found = true;
            }
        }
        
        assertTrue(parent1Found, "Parent1 should be at the top level (no indentation)");
        assertTrue(parent2Found, "Parent2 should be at the top level (no indentation)");
    }
    
    @Test
    @DisplayName("Should handle missing work items gracefully")
    void shouldHandleMissingWorkItemsGracefully() {
        // Setup
        listCommand.setViewMode("pretty");
        
        // Mock service to return incomplete set of work items
        List<WorkItem> workItems = Arrays.asList(parent1, parent2, child1);
        when(itemService.getAllWorkItems()).thenReturn(workItems);
        
        // Create a parent map that references non-existent work items
        Map<UUID, UUID> parentMap = new HashMap<>();
        parentMap.put(child1Id, parent1Id);
        parentMap.put(child2Id, parent1Id); // This work item wasn't returned
        parentMap.put(child3Id, parent2Id); // This work item wasn't returned
        parentMap.put(UUID.randomUUID(), parent1Id); // This ID doesn't exist
        
        when(relationshipService.getAllParentChildRelationships()).thenReturn(parentMap);
        
        // Execute
        Integer result = listCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed despite missing work items");
        
        // The output should still show the hierarchy for items that exist
        String output = outputCaptor.toString();
        assertTrue(output.contains("User Auth Features"), "Should show parent1 title");
        assertTrue(output.contains("  └── Implement login screen"), "Should show child1 indented under parent1");
    }
    
    @Test
    @DisplayName("Should handle circular dependencies gracefully")
    void shouldHandleCircularDependenciesGracefully() {
        // Setup
        listCommand.setViewMode("pretty");
        
        // Mock service to return all work items
        List<WorkItem> allWorkItems = Arrays.asList(parent1, child1, child2);
        when(itemService.getAllWorkItems()).thenReturn(allWorkItems);
        
        // Create a circular dependency in the parent map
        Map<UUID, UUID> parentMap = new HashMap<>();
        parentMap.put(child1Id, parent1Id);
        parentMap.put(parent1Id, child1Id); // This creates a circular dependency
        
        when(relationshipService.getAllParentChildRelationships()).thenReturn(parentMap);
        
        // Execute
        Integer result = listCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should still succeed");
        assertTrue(errorCaptor.toString().contains("Circular dependency detected"), 
                "Should warn about circular dependency");
    }
}