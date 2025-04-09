/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.BacklogService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BacklogCommandTest {

    @Mock
    private BacklogService backlogService;

    @Mock
    private WorkItem workItem1;

    @Mock
    private WorkItem workItem2;

    private BacklogCommand backlogCommand;

    private final UUID itemId1 = UUID.randomUUID();
    private final UUID itemId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        backlogCommand = new BacklogCommand(backlogService);
        
        // Setup mock work items
        when(workItem1.getId()).thenReturn(itemId1);
        when(workItem1.getTitle()).thenReturn("Bug in login page");
        when(workItem1.getType()).thenReturn(WorkItemType.BUG);
        when(workItem1.getPriority()).thenReturn(Priority.HIGH);
        when(workItem1.getStatus()).thenReturn(WorkflowState.FOUND);
        
        when(workItem2.getId()).thenReturn(itemId2);
        when(workItem2.getTitle()).thenReturn("Add filtering feature");
        when(workItem2.getType()).thenReturn(WorkItemType.FEATURE);
        when(workItem2.getPriority()).thenReturn(Priority.MEDIUM);
        when(workItem2.getStatus()).thenReturn(WorkflowState.READY);
    }

    @Test
    @DisplayName("Should display empty backlog")
    void displayEmptyBacklog() {
        // Arrange
        String[] args = {};
        when(backlogService.getBacklog()).thenReturn(new ArrayList<>());
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(0, result);
        verify(backlogService).getBacklog();
    }

    @Test
    @DisplayName("Should display backlog with items")
    void displayBacklogWithItems() {
        // Arrange
        String[] args = {};
        List<WorkItem> backlogItems = Arrays.asList(workItem1, workItem2);
        when(backlogService.getBacklog()).thenReturn(backlogItems);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(0, result);
        verify(backlogService).getBacklog();
    }

    @Test
    @DisplayName("Should move item up in backlog")
    void moveItemUp() {
        // Arrange
        String[] args = {"up", itemId1.toString()};
        when(backlogService.moveUp(itemId1)).thenReturn(true);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(0, result);
        verify(backlogService).moveUp(itemId1);
    }

    @Test
    @DisplayName("Should handle failure to move item up")
    void handleMoveUpFailure() {
        // Arrange
        String[] args = {"up", itemId1.toString()};
        when(backlogService.moveUp(itemId1)).thenReturn(false);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(1, result);
        verify(backlogService).moveUp(itemId1);
    }

    @Test
    @DisplayName("Should move item down in backlog")
    void moveItemDown() {
        // Arrange
        String[] args = {"down", itemId1.toString()};
        when(backlogService.moveDown(itemId1)).thenReturn(true);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(0, result);
        verify(backlogService).moveDown(itemId1);
    }

    @Test
    @DisplayName("Should handle failure to move item down")
    void handleMoveDownFailure() {
        // Arrange
        String[] args = {"down", itemId1.toString()};
        when(backlogService.moveDown(itemId1)).thenReturn(false);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(1, result);
        verify(backlogService).moveDown(itemId1);
    }

    @Test
    @DisplayName("Should move item to top of backlog")
    void moveItemToTop() {
        // Arrange
        String[] args = {"top", itemId1.toString()};
        when(backlogService.moveToTop(itemId1)).thenReturn(true);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(0, result);
        verify(backlogService).moveToTop(itemId1);
    }

    @Test
    @DisplayName("Should handle failure to move item to top")
    void handleMoveToTopFailure() {
        // Arrange
        String[] args = {"top", itemId1.toString()};
        when(backlogService.moveToTop(itemId1)).thenReturn(false);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(1, result);
        verify(backlogService).moveToTop(itemId1);
    }

    @Test
    @DisplayName("Should move item to bottom of backlog")
    void moveItemToBottom() {
        // Arrange
        String[] args = {"bottom", itemId1.toString()};
        when(backlogService.moveToBottom(itemId1)).thenReturn(true);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(0, result);
        verify(backlogService).moveToBottom(itemId1);
    }

    @Test
    @DisplayName("Should handle failure to move item to bottom")
    void handleMoveToBottomFailure() {
        // Arrange
        String[] args = {"bottom", itemId1.toString()};
        when(backlogService.moveToBottom(itemId1)).thenReturn(false);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(1, result);
        verify(backlogService).moveToBottom(itemId1);
    }

    @Test
    @DisplayName("Should handle invalid subcommand")
    void handleInvalidSubcommand() {
        // Arrange
        String[] args = {"invalid", itemId1.toString()};
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(1, result);
        verifyNoInteractions(backlogService);
    }

    @Test
    @DisplayName("Should handle missing item ID")
    void handleMissingItemId() {
        // Arrange
        String[] args = {"up"};
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(1, result);
        verifyNoInteractions(backlogService);
    }

    @Test
    @DisplayName("Should handle invalid item ID format")
    void handleInvalidItemIdFormat() {
        // Arrange
        String[] args = {"up", "not-a-uuid"};
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(1, result);
        verifyNoInteractions(backlogService);
    }

    @Test
    @DisplayName("Should remove item from backlog")
    void removeItemFromBacklog() {
        // Arrange
        String[] args = {"remove", itemId1.toString()};
        when(backlogService.removeFromBacklog(itemId1)).thenReturn(true);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(0, result);
        verify(backlogService).removeFromBacklog(itemId1);
    }

    @Test
    @DisplayName("Should handle failure to remove item from backlog")
    void handleRemoveFailure() {
        // Arrange
        String[] args = {"remove", itemId1.toString()};
        when(backlogService.removeFromBacklog(itemId1)).thenReturn(false);
        
        // Act
        int result = backlogCommand.execute(args);
        
        // Assert
        assertEquals(1, result);
        verify(backlogService).removeFromBacklog(itemId1);
    }
}