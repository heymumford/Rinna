/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.repository.BacklogRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBacklogRepositoryTest {

    private BacklogRepository backlogRepository;
    private final String testUser = "testuser";
    private final String anotherUser = "anotheruser";
    private final UUID itemId1 = UUID.randomUUID();
    private final UUID itemId2 = UUID.randomUUID();
    private final UUID itemId3 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        backlogRepository = new InMemoryBacklogRepository();
    }

    @Test
    @DisplayName("Should return empty list for new user")
    void getEmptyBacklog() {
        List<UUID> backlog = backlogRepository.getBacklog(testUser);
        assertTrue(backlog.isEmpty());
    }

    @Test
    @DisplayName("Should add item to backlog")
    void addItemToBacklog() {
        boolean result = backlogRepository.addToBacklog(testUser, itemId1);
        assertTrue(result);
        
        List<UUID> backlog = backlogRepository.getBacklog(testUser);
        assertEquals(1, backlog.size());
        assertEquals(itemId1, backlog.get(0));
    }

    @Test
    @DisplayName("Should not add duplicate item to backlog")
    void addDuplicateItemToBacklog() {
        backlogRepository.addToBacklog(testUser, itemId1);
        boolean result = backlogRepository.addToBacklog(testUser, itemId1);
        
        assertFalse(result);
        List<UUID> backlog = backlogRepository.getBacklog(testUser);
        assertEquals(1, backlog.size());
    }

    @Test
    @DisplayName("Should remove item from backlog")
    void removeItemFromBacklog() {
        backlogRepository.addToBacklog(testUser, itemId1);
        boolean result = backlogRepository.removeFromBacklog(testUser, itemId1);
        
        assertTrue(result);
        List<UUID> backlog = backlogRepository.getBacklog(testUser);
        assertTrue(backlog.isEmpty());
    }

    @Test
    @DisplayName("Should handle removing non-existent item")
    void removeNonExistentItem() {
        boolean result = backlogRepository.removeFromBacklog(testUser, itemId1);
        assertFalse(result);
    }

    @Test
    @DisplayName("Should move item in backlog")
    void moveItemInBacklog() {
        backlogRepository.addToBacklog(testUser, itemId1);
        backlogRepository.addToBacklog(testUser, itemId2);
        backlogRepository.addToBacklog(testUser, itemId3);
        
        boolean result = backlogRepository.moveInBacklog(testUser, itemId3, 0);
        
        assertTrue(result);
        List<UUID> backlog = backlogRepository.getBacklog(testUser);
        assertEquals(3, backlog.size());
        assertEquals(itemId3, backlog.get(0));
        assertEquals(itemId1, backlog.get(1));
        assertEquals(itemId2, backlog.get(2));
    }

    @Test
    @DisplayName("Should handle moving non-existent item")
    void moveNonExistentItem() {
        backlogRepository.addToBacklog(testUser, itemId1);
        
        boolean result = backlogRepository.moveInBacklog(testUser, itemId2, 0);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle moving to invalid position")
    void moveToInvalidPosition() {
        backlogRepository.addToBacklog(testUser, itemId1);
        backlogRepository.addToBacklog(testUser, itemId2);
        
        boolean result = backlogRepository.moveInBacklog(testUser, itemId1, 5);
        
        assertFalse(result);
        List<UUID> backlog = backlogRepository.getBacklog(testUser);
        assertEquals(2, backlog.size());
        assertEquals(itemId1, backlog.get(0));
        assertEquals(itemId2, backlog.get(1));
    }

    @Test
    @DisplayName("Should keep backlogs separate for different users")
    void separateBacklogsForDifferentUsers() {
        backlogRepository.addToBacklog(testUser, itemId1);
        backlogRepository.addToBacklog(testUser, itemId2);
        backlogRepository.addToBacklog(anotherUser, itemId3);
        
        List<UUID> testUserBacklog = backlogRepository.getBacklog(testUser);
        List<UUID> anotherUserBacklog = backlogRepository.getBacklog(anotherUser);
        
        assertEquals(2, testUserBacklog.size());
        assertEquals(1, anotherUserBacklog.size());
        
        assertEquals(itemId1, testUserBacklog.get(0));
        assertEquals(itemId2, testUserBacklog.get(1));
        assertEquals(itemId3, anotherUserBacklog.get(0));
    }

    @Test
    @DisplayName("Should move item to correct position when moving down")
    void moveItemDown() {
        backlogRepository.addToBacklog(testUser, itemId1);
        backlogRepository.addToBacklog(testUser, itemId2);
        backlogRepository.addToBacklog(testUser, itemId3);
        
        boolean result = backlogRepository.moveInBacklog(testUser, itemId1, 1);
        
        assertTrue(result);
        List<UUID> backlog = backlogRepository.getBacklog(testUser);
        assertEquals(3, backlog.size());
        assertEquals(itemId2, backlog.get(0));
        assertEquals(itemId1, backlog.get(1));
        assertEquals(itemId3, backlog.get(2));
    }

    @Test
    @DisplayName("Should move item to correct position when moving up")
    void moveItemUp() {
        backlogRepository.addToBacklog(testUser, itemId1);
        backlogRepository.addToBacklog(testUser, itemId2);
        backlogRepository.addToBacklog(testUser, itemId3);
        
        boolean result = backlogRepository.moveInBacklog(testUser, itemId3, 1);
        
        assertTrue(result);
        List<UUID> backlog = backlogRepository.getBacklog(testUser);
        assertEquals(3, backlog.size());
        assertEquals(itemId1, backlog.get(0));
        assertEquals(itemId3, backlog.get(1));
        assertEquals(itemId2, backlog.get(2));
    }
}