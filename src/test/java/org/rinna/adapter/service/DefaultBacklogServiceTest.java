/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.domain.WorkItem;
import org.rinna.repository.BacklogRepository;
import org.rinna.repository.ItemRepository;
import org.rinna.usecase.BacklogService;

@ExtendWith(MockitoExtension.class)
class DefaultBacklogServiceTest {

    @Mock
    private BacklogRepository backlogRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private DefaultBacklogService.UserContextProvider userContextProvider;

    @Mock
    private WorkItem workItem1;

    @Mock
    private WorkItem workItem2;

    @Mock
    private WorkItem workItem3;

    private BacklogService backlogService;

    private final UUID itemId1 = UUID.randomUUID();
    private final UUID itemId2 = UUID.randomUUID();
    private final UUID itemId3 = UUID.randomUUID();
    private final String testUser = "testuser";

    @BeforeEach
    void setUp() {
        backlogService = new DefaultBacklogService(backlogRepository, itemRepository, userContextProvider);
        
        // Setup basic mocks
        when(userContextProvider.getCurrentUsername()).thenReturn(testUser);
        
        when(workItem1.getId()).thenReturn(itemId1);
        when(workItem2.getId()).thenReturn(itemId2);
        when(workItem3.getId()).thenReturn(itemId3);
    }

    @Test
    @DisplayName("Should get empty backlog when no items exist")
    void getEmptyBacklog() {
        // Arrange
        when(backlogRepository.getBacklog(testUser)).thenReturn(new ArrayList<>());
        
        // Act
        List<WorkItem> backlog = backlogService.getBacklog();
        
        // Assert
        assertTrue(backlog.isEmpty());
        verify(backlogRepository).getBacklog(testUser);
        verify(itemRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should get backlog with all items")
    void getBacklogWithItems() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        
        when(itemRepository.findById(itemId1)).thenReturn(Optional.of(workItem1));
        when(itemRepository.findById(itemId2)).thenReturn(Optional.of(workItem2));
        when(itemRepository.findById(itemId3)).thenReturn(Optional.of(workItem3));
        
        // Act
        List<WorkItem> backlog = backlogService.getBacklog();
        
        // Assert
        assertEquals(3, backlog.size());
        verify(backlogRepository).getBacklog(testUser);
        
        verify(itemRepository).findById(itemId1);
        verify(itemRepository).findById(itemId2);
        verify(itemRepository).findById(itemId3);
    }

    @Test
    @DisplayName("Should filter out items that no longer exist")
    void getBacklogWithMissingItems() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        
        when(itemRepository.findById(itemId1)).thenReturn(Optional.of(workItem1));
        when(itemRepository.findById(itemId2)).thenReturn(Optional.empty()); // Item no longer exists
        when(itemRepository.findById(itemId3)).thenReturn(Optional.of(workItem3));
        
        // Act
        List<WorkItem> backlog = backlogService.getBacklog();
        
        // Assert
        assertEquals(2, backlog.size());
        verify(backlogRepository).getBacklog(testUser);
        
        verify(itemRepository).findById(itemId1);
        verify(itemRepository).findById(itemId2);
        verify(itemRepository).findById(itemId3);
    }

    @Test
    @DisplayName("Should add item to backlog when it exists")
    void addToBacklogSuccess() {
        // Arrange
        when(itemRepository.findById(itemId1)).thenReturn(Optional.of(workItem1));
        when(backlogRepository.addToBacklog(testUser, itemId1)).thenReturn(true);
        
        // Act
        boolean result = backlogService.addToBacklog(itemId1);
        
        // Assert
        assertTrue(result);
        verify(itemRepository).findById(itemId1);
        verify(backlogRepository).addToBacklog(testUser, itemId1);
    }

    @Test
    @DisplayName("Should not add item to backlog when it doesn't exist")
    void addToBacklogNonExistentItem() {
        // Arrange
        when(itemRepository.findById(itemId1)).thenReturn(Optional.empty());
        
        // Act
        boolean result = backlogService.addToBacklog(itemId1);
        
        // Assert
        assertFalse(result);
        verify(itemRepository).findById(itemId1);
        verify(backlogRepository, never()).addToBacklog(any(), any());
    }

    @Test
    @DisplayName("Should remove item from backlog")
    void removeFromBacklog() {
        // Arrange
        when(backlogRepository.removeFromBacklog(testUser, itemId1)).thenReturn(true);
        
        // Act
        boolean result = backlogService.removeFromBacklog(itemId1);
        
        // Assert
        assertTrue(result);
        verify(backlogRepository).removeFromBacklog(testUser, itemId1);
    }

    @Test
    @DisplayName("Should move item up in backlog")
    void moveItemUp() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        when(backlogRepository.moveInBacklog(testUser, itemId3, 1)).thenReturn(true);
        
        // Act
        boolean result = backlogService.moveUp(itemId3);
        
        // Assert
        assertTrue(result);
        verify(backlogRepository).getBacklog(testUser);
        verify(backlogRepository).moveInBacklog(testUser, itemId3, 1);
    }

    @Test
    @DisplayName("Should not move top item up in backlog")
    void moveTopItemUp() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        
        // Act
        boolean result = backlogService.moveUp(itemId1);
        
        // Assert
        assertFalse(result);
        verify(backlogRepository).getBacklog(testUser);
        verify(backlogRepository, never()).moveInBacklog(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should move item down in backlog")
    void moveItemDown() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        when(backlogRepository.moveInBacklog(testUser, itemId1, 1)).thenReturn(true);
        
        // Act
        boolean result = backlogService.moveDown(itemId1);
        
        // Assert
        assertTrue(result);
        verify(backlogRepository).getBacklog(testUser);
        verify(backlogRepository).moveInBacklog(testUser, itemId1, 1);
    }

    @Test
    @DisplayName("Should not move bottom item down in backlog")
    void moveBottomItemDown() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        
        // Act
        boolean result = backlogService.moveDown(itemId3);
        
        // Assert
        assertFalse(result);
        verify(backlogRepository).getBacklog(testUser);
        verify(backlogRepository, never()).moveInBacklog(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should move item to top of backlog")
    void moveItemToTop() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        when(backlogRepository.moveInBacklog(testUser, itemId3, 0)).thenReturn(true);
        
        // Act
        boolean result = backlogService.moveToTop(itemId3);
        
        // Assert
        assertTrue(result);
        verify(backlogRepository).getBacklog(testUser);
        verify(backlogRepository).moveInBacklog(testUser, itemId3, 0);
    }

    @Test
    @DisplayName("Should not move item already at top to top")
    void moveTopItemToTop() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        
        // Act
        boolean result = backlogService.moveToTop(itemId1);
        
        // Assert
        assertFalse(result);
        verify(backlogRepository).getBacklog(testUser);
        verify(backlogRepository, never()).moveInBacklog(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should move item to bottom of backlog")
    void moveItemToBottom() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        when(backlogRepository.moveInBacklog(testUser, itemId1, 2)).thenReturn(true);
        
        // Act
        boolean result = backlogService.moveToBottom(itemId1);
        
        // Assert
        assertTrue(result);
        verify(backlogRepository).getBacklog(testUser);
        verify(backlogRepository).moveInBacklog(testUser, itemId1, 2);
    }

    @Test
    @DisplayName("Should not move item already at bottom to bottom")
    void moveBottomItemToBottom() {
        // Arrange
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        
        // Act
        boolean result = backlogService.moveToBottom(itemId3);
        
        // Assert
        assertFalse(result);
        verify(backlogRepository).getBacklog(testUser);
        verify(backlogRepository, never()).moveInBacklog(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should get another user's backlog")
    void getAnotherUserBacklog() {
        // Arrange
        String otherUser = "otheruser";
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2);
        
        when(backlogRepository.getBacklog(otherUser)).thenReturn(backlogIds);
        when(itemRepository.findById(itemId1)).thenReturn(Optional.of(workItem1));
        when(itemRepository.findById(itemId2)).thenReturn(Optional.of(workItem2));
        
        // Act
        List<WorkItem> backlog = backlogService.getBacklog(otherUser);
        
        // Assert
        assertEquals(2, backlog.size());
        verify(backlogRepository).getBacklog(otherUser);
        verify(itemRepository).findById(itemId1);
        verify(itemRepository).findById(itemId2);
    }

    @Test
    @DisplayName("Should handle item not in backlog for operations")
    void handleItemNotInBacklog() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        List<UUID> backlogIds = Arrays.asList(itemId1, itemId2, itemId3);
        when(backlogRepository.getBacklog(testUser)).thenReturn(backlogIds);
        
        // Act & Assert
        assertFalse(backlogService.moveUp(nonExistentId));
        assertFalse(backlogService.moveDown(nonExistentId));
        assertFalse(backlogService.moveToTop(nonExistentId));
        assertFalse(backlogService.moveToBottom(nonExistentId));
        
        verify(backlogRepository, times(4)).getBacklog(testUser);
        verify(backlogRepository, never()).moveInBacklog(any(), any(), anyInt());
    }
}