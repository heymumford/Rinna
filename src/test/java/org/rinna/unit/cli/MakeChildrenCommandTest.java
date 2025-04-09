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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.UnitTest;
import org.rinna.cli.command.MakeChildrenCommand;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the MakeChildrenCommand class.
 */
@DisplayName("MakeChildrenCommand Unit Tests")
public class MakeChildrenCommandTest extends UnitTest {
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private RelationshipService relationshipService;
    
    @Mock
    private WorkItem mockChild1;
    
    @Mock
    private WorkItem mockChild2;
    
    @Mock
    private WorkItem mockChild3;
    
    @Mock
    private WorkItem mockParent;
    
    private MakeChildrenCommand makeChildrenCommand;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private final UUID child1Id = UUID.randomUUID();
    private final UUID child2Id = UUID.randomUUID();
    private final UUID child3Id = UUID.randomUUID();
    private final UUID parentId = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up System.out/err capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Set up default command
        makeChildrenCommand = new MakeChildrenCommand();
        setMockServiceManager();
        
        // Reset captors between tests
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Set up mock children work items
        when(mockChild1.id()).thenReturn(child1Id);
        when(mockChild1.title()).thenReturn("Implement login screen");
        when(mockChild1.priority()).thenReturn(Priority.MEDIUM);
        when(mockChild1.type()).thenReturn(WorkItemType.TASK);
        when(mockChild1.state()).thenReturn(WorkflowState.IN_PROGRESS);
        
        when(mockChild2.id()).thenReturn(child2Id);
        when(mockChild2.title()).thenReturn("Create user registration");
        when(mockChild2.priority()).thenReturn(Priority.LOW);
        when(mockChild2.type()).thenReturn(WorkItemType.TASK);
        when(mockChild2.state()).thenReturn(WorkflowState.READY);
        
        when(mockChild3.id()).thenReturn(child3Id);
        when(mockChild3.title()).thenReturn("Design password reset");
        when(mockChild3.priority()).thenReturn(Priority.HIGH);
        when(mockChild3.type()).thenReturn(WorkItemType.TASK);
        when(mockChild3.state()).thenReturn(WorkflowState.BACKLOG);
        
        // Set up mock parent work item
        when(mockParent.id()).thenReturn(parentId);
        when(mockParent.title()).thenReturn("Parent of children");
        when(mockParent.priority()).thenReturn(Priority.HIGH);
        when(mockParent.type()).thenReturn(WorkItemType.FEATURE);
        when(mockParent.state()).thenReturn(WorkflowState.IN_PROGRESS);
    }
    
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Sets up a mock ServiceManager to be used by the MakeChildrenCommand.
     */
    private void setMockServiceManager() {
        try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(serviceManager);
            
            // Set up mock responses
            when(serviceManager.getItemService()).thenReturn(itemService);
            when(serviceManager.getRelationshipService()).thenReturn(relationshipService);
        }
    }
    
    @Test
    @DisplayName("Should create parent work item for multiple children successfully")
    void shouldCreateParentWorkItemSuccessfully() {
        // Setup
        String childIds = child1Id + "," + child2Id + "," + child3Id;
        makeChildrenCommand.setChildIds(childIds);
        
        // Mock service responses
        when(itemService.getItem(child1Id)).thenReturn(mockChild1);
        when(itemService.getItem(child2Id)).thenReturn(mockChild2);
        when(itemService.getItem(child3Id)).thenReturn(mockChild3);
        when(relationshipService.getParentWorkItem(any(UUID.class))).thenReturn(null);
        when(itemService.createWorkItem(anyString(), any(WorkItemType.class), any(Priority.class), anyString(), any(WorkflowState.class)))
            .thenReturn(mockParent);
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify parent work item creation
        verify(itemService).createWorkItem(
            eq("Parent of " + child1Id.toString().substring(0, 8) + "," 
                + child2Id.toString().substring(0, 8) + "," 
                + child3Id.toString().substring(0, 8)), 
            eq(WorkItemType.FEATURE), 
            eq(Priority.HIGH), 
            anyString(),
            eq(WorkflowState.IN_PROGRESS)
        );
        
        // Verify relationship creation
        verify(relationshipService).createRelationship(
            eq(child1Id), 
            eq(parentId), 
            eq(WorkItemRelationshipType.CHILD_OF)
        );
        verify(relationshipService).createRelationship(
            eq(child2Id), 
            eq(parentId), 
            eq(WorkItemRelationshipType.CHILD_OF)
        );
        verify(relationshipService).createRelationship(
            eq(child3Id), 
            eq(parentId), 
            eq(WorkItemRelationshipType.CHILD_OF)
        );
        
        assertTrue(outputCaptor.toString().contains("Successfully created parent work item"), 
                "Should show success message");
        assertTrue(outputCaptor.toString().contains(parentId.toString()), 
                "Should show the new parent ID");
    }
    
    @Test
    @DisplayName("Should create parent work item with custom title")
    void shouldCreateParentWorkItemWithCustomTitle() {
        // Setup
        String childIds = child1Id + "," + child2Id;
        makeChildrenCommand.setChildIds(childIds);
        makeChildrenCommand.setTitle("User Authentication Feature");
        
        // Mock service responses
        when(itemService.getItem(child1Id)).thenReturn(mockChild1);
        when(itemService.getItem(child2Id)).thenReturn(mockChild2);
        when(relationshipService.getParentWorkItem(any(UUID.class))).thenReturn(null);
        when(itemService.createWorkItem(anyString(), any(WorkItemType.class), any(Priority.class), anyString(), any(WorkflowState.class)))
            .thenReturn(mockParent);
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify parent work item creation with custom title
        verify(itemService).createWorkItem(
            eq("User Authentication Feature"), 
            eq(WorkItemType.FEATURE), 
            eq(Priority.HIGH), 
            anyString(),
            eq(WorkflowState.IN_PROGRESS)
        );
    }
    
    @Test
    @DisplayName("Should inherit highest priority from children")
    void shouldInheritHighestPriorityFromChildren() {
        // Setup
        String childIds = child1Id + "," + child3Id;
        makeChildrenCommand.setChildIds(childIds);
        
        // Child1 has MEDIUM, Child3 has HIGH priority
        
        // Mock service responses
        when(itemService.getItem(child1Id)).thenReturn(mockChild1);
        when(itemService.getItem(child3Id)).thenReturn(mockChild3);
        when(relationshipService.getParentWorkItem(any(UUID.class))).thenReturn(null);
        when(itemService.createWorkItem(anyString(), any(WorkItemType.class), any(Priority.class), anyString(), any(WorkflowState.class)))
            .thenReturn(mockParent);
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify parent is created with HIGH priority (from child3)
        verify(itemService).createWorkItem(
            anyString(), 
            any(WorkItemType.class), 
            eq(Priority.HIGH), 
            anyString(),
            any(WorkflowState.class)
        );
    }
    
    @Test
    @DisplayName("Should fail when child already has a parent")
    void shouldFailWhenChildAlreadyHasParent() {
        // Setup
        String childIds = child1Id + "," + child2Id;
        makeChildrenCommand.setChildIds(childIds);
        
        // Mock service responses
        when(itemService.getItem(child1Id)).thenReturn(mockChild1);
        when(itemService.getItem(child2Id)).thenReturn(mockChild2);
        
        // Child1 already has a parent
        UUID existingParentId = UUID.randomUUID();
        when(relationshipService.getParentWorkItem(child1Id)).thenReturn(existingParentId);
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("already has a parent"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should fail when no child IDs are provided")
    void shouldFailWhenNoChildIdsProvided() {
        // Setup
        makeChildrenCommand.setChildIds("");
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("No work item IDs provided"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should fail when child work item doesn't exist")
    void shouldFailWhenChildWorkItemDoesNotExist() {
        // Setup
        String childIds = child1Id + "," + child2Id + "," + UUID.randomUUID();
        makeChildrenCommand.setChildIds(childIds);
        
        // Mock service responses - last child ID doesn't exist
        when(itemService.getItem(child1Id)).thenReturn(mockChild1);
        when(itemService.getItem(child2Id)).thenReturn(mockChild2);
        when(itemService.getItem(any(UUID.class))).thenReturn(null);
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("not found"), 
                "Should show appropriate error message");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "123,abc", "not-a-uuid"})
    @DisplayName("Should fail with invalid work item ID format")
    void shouldFailWithInvalidWorkItemIdFormat(String invalidIds) {
        // Setup
        makeChildrenCommand.setChildIds(invalidIds);
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid work item ID format"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should set appropriate state for parent based on children")
    void shouldSetAppropriateStateForParent() {
        // Setup
        String childIds = child1Id + "," + child2Id;
        makeChildrenCommand.setChildIds(childIds);
        
        // Child1 is IN_PROGRESS, Child2 is READY
        
        // Mock service responses
        when(itemService.getItem(child1Id)).thenReturn(mockChild1);
        when(itemService.getItem(child2Id)).thenReturn(mockChild2);
        when(relationshipService.getParentWorkItem(any(UUID.class))).thenReturn(null);
        when(itemService.createWorkItem(anyString(), any(WorkItemType.class), any(Priority.class), anyString(), any(WorkflowState.class)))
            .thenReturn(mockParent);
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify parent is created with IN_PROGRESS state
        verify(itemService).createWorkItem(
            anyString(), 
            any(WorkItemType.class), 
            any(Priority.class), 
            anyString(),
            eq(WorkflowState.IN_PROGRESS)
        );
    }
    
    @Test
    @DisplayName("Should detect circular dependency")
    void shouldDetectCircularDependency() {
        // Setup
        String childIds = parentId.toString();
        makeChildrenCommand.setChildIds(childIds);
        makeChildrenCommand.setParentId(child1Id.toString());
        
        // Mock parent and child existence
        when(itemService.getItem(parentId)).thenReturn(mockParent);
        when(itemService.getItem(child1Id)).thenReturn(mockChild1);
        
        // Mock relationships to create a potential circular dependency
        when(relationshipService.getParentWorkItem(parentId)).thenReturn(null);
        when(relationshipService.getChildWorkItems(parentId)).thenReturn(List.of(child1Id));
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Circular dependency detected"), 
                "Should show circular dependency error");
    }
    
    @Test
    @DisplayName("Should handle malicious input safely")
    void shouldHandleMaliciousInputSafely() {
        // Setup
        String maliciousInput = "'; DROP TABLE WORKITEMS; --";
        makeChildrenCommand.setChildIds(maliciousInput);
        
        // Execute
        Integer result = makeChildrenCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid work item ID format"), 
                "Should show appropriate error message");
        
        // Verify no dangerous calls were made
        verify(itemService, never()).createWorkItem(anyString(), any(), any(), anyString(), any());
        verify(relationshipService, never()).createRelationship(any(), any(), any());
    }
}