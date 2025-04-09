/*
 * Unit test for the InMemoryQueueRepository
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.DefaultWorkItem;
import org.rinna.domain.model.DefaultWorkQueue;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkQueue;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.QueueRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InMemoryQueueRepository}.
 */
class InMemoryQueueRepositoryTest {

    private InMemoryQueueRepository repository;
    private UUID queueId1;
    private UUID queueId2;
    private WorkQueue queue1;
    private WorkQueue queue2;

    @BeforeEach
    void setUp() {
        repository = new InMemoryQueueRepository();
        queueId1 = UUID.randomUUID();
        queueId2 = UUID.randomUUID();

        // Create first queue (active)
        queue1 = new DefaultWorkQueue(queueId1, "Queue 1", "First test queue", true);
        
        // Create second queue (inactive)
        queue2 = new DefaultWorkQueue(queueId2, "Queue 2", "Second test queue", false);
    }

    @Test
    void testSaveAndFindById() {
        // Save a queue
        WorkQueue savedQueue = repository.save(queue1);
        
        // Verify saved queue
        assertNotNull(savedQueue);
        assertEquals(queueId1, savedQueue.getId());
        assertEquals("Queue 1", savedQueue.getName());
        assertEquals("First test queue", savedQueue.getDescription());
        assertTrue(savedQueue.isActive());
        
        // Find by ID
        Optional<WorkQueue> foundQueue = repository.findById(queueId1);
        
        // Verify found queue
        assertTrue(foundQueue.isPresent());
        assertEquals(queueId1, foundQueue.get().getId());
        assertEquals("Queue 1", foundQueue.get().getName());
        assertEquals("First test queue", foundQueue.get().getDescription());
        assertTrue(foundQueue.get().isActive());
    }

    @Test
    void testFindByIdNonExistent() {
        // Find by non-existent ID
        Optional<WorkQueue> foundQueue = repository.findById(UUID.randomUUID());
        
        // Verify not found
        assertFalse(foundQueue.isPresent());
    }

    @Test
    void testFindByName() {
        // Save queues
        repository.save(queue1);
        repository.save(queue2);
        
        // Find by name
        Optional<WorkQueue> foundQueue = repository.findByName("Queue 2");
        
        // Verify found queue
        assertTrue(foundQueue.isPresent());
        assertEquals(queueId2, foundQueue.get().getId());
        assertEquals("Queue 2", foundQueue.get().getName());
        assertEquals("Second test queue", foundQueue.get().getDescription());
        assertFalse(foundQueue.get().isActive());
    }

    @Test
    void testFindByNameNonExistent() {
        // Save a queue
        repository.save(queue1);
        
        // Find by non-existent name
        Optional<WorkQueue> foundQueue = repository.findByName("Non-existent Queue");
        
        // Verify not found
        assertFalse(foundQueue.isPresent());
    }

    @Test
    void testFindAll() {
        // Initially repository should be empty
        List<WorkQueue> allQueues = repository.findAll();
        assertEquals(0, allQueues.size());
        
        // Save queues
        repository.save(queue1);
        repository.save(queue2);
        
        // Find all queues
        allQueues = repository.findAll();
        
        // Verify found queues
        assertEquals(2, allQueues.size());
        
        // Verify contents (order not guaranteed)
        boolean foundQueue1 = false;
        boolean foundQueue2 = false;
        
        for (WorkQueue queue : allQueues) {
            if (queue.getId().equals(queueId1)) {
                foundQueue1 = true;
                assertEquals("Queue 1", queue.getName());
                assertTrue(queue.isActive());
            } else if (queue.getId().equals(queueId2)) {
                foundQueue2 = true;
                assertEquals("Queue 2", queue.getName());
                assertFalse(queue.isActive());
            }
        }
        
        assertTrue(foundQueue1, "Queue 1 should be in the results");
        assertTrue(foundQueue2, "Queue 2 should be in the results");
    }

    @Test
    void testFindByActive() {
        // Save queues
        repository.save(queue1); // Active
        repository.save(queue2); // Inactive
        
        // Find active queues
        List<WorkQueue> activeQueues = repository.findByActive(true);
        
        // Verify only the active queue is returned
        assertEquals(1, activeQueues.size());
        assertEquals(queueId1, activeQueues.get(0).getId());
        assertEquals("Queue 1", activeQueues.get(0).getName());
        
        // Find inactive queues
        List<WorkQueue> inactiveQueues = repository.findByActive(false);
        
        // Verify only the inactive queue is returned
        assertEquals(1, inactiveQueues.size());
        assertEquals(queueId2, inactiveQueues.get(0).getId());
        assertEquals("Queue 2", inactiveQueues.get(0).getName());
    }

    @Test
    void testUpdateExistingQueue() {
        // Save a queue
        repository.save(queue1);
        
        // Create an updated version with the same ID
        WorkQueue updatedQueue = new DefaultWorkQueue(queueId1, "Updated Queue", "Updated description", false);
        
        // Save the updated queue
        repository.save(updatedQueue);
        
        // Find by ID
        Optional<WorkQueue> foundQueue = repository.findById(queueId1);
        
        // Verify the queue was updated
        assertTrue(foundQueue.isPresent());
        assertEquals(queueId1, foundQueue.get().getId());
        assertEquals("Updated Queue", foundQueue.get().getName());
        assertEquals("Updated description", foundQueue.get().getDescription());
        assertFalse(foundQueue.get().isActive());
    }

    @Test
    void testDeleteById() {
        // Save queues
        repository.save(queue1);
        repository.save(queue2);
        
        // Verify both exist
        assertEquals(2, repository.findAll().size());
        assertTrue(repository.findById(queueId1).isPresent());
        assertTrue(repository.findById(queueId2).isPresent());
        
        // Delete one queue
        boolean deleted = repository.deleteById(queueId1);
        
        // Verify deletion was successful
        assertTrue(deleted);
        
        // Verify only one remains
        assertEquals(1, repository.findAll().size());
        assertFalse(repository.findById(queueId1).isPresent());
        assertTrue(repository.findById(queueId2).isPresent());
    }

    @Test
    void testDeleteByIdNonExistent() {
        // Save a queue
        repository.save(queue1);
        
        // Delete non-existent queue
        UUID nonExistentId = UUID.randomUUID();
        boolean deleted = repository.deleteById(nonExistentId);
        
        // Verify deletion was not successful
        assertFalse(deleted);
        
        // Verify existing queue is still there
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(queueId1).isPresent());
    }

    @Test
    void testCount() {
        // Initially, count should be 0
        assertEquals(0, repository.count());
        
        // Save a queue
        repository.save(queue1);
        
        // Count should be 1
        assertEquals(1, repository.count());
        
        // Save another queue
        repository.save(queue2);
        
        // Count should be 2
        assertEquals(2, repository.count());
        
        // Delete a queue
        repository.deleteById(queueId1);
        
        // Count should be 1 again
        assertEquals(1, repository.count());
    }

    @Test
    void testEnsureDefaultQueue() {
        // Initially, no default queue should exist
        Optional<WorkQueue> defaultQueue = repository.findByName("Default Queue");
        assertFalse(defaultQueue.isPresent());
        
        // Ensure default queue
        WorkQueue createdDefaultQueue = repository.ensureDefaultQueue();
        
        // Verify default queue was created
        assertNotNull(createdDefaultQueue);
        assertEquals("Default Queue", createdDefaultQueue.getName());
        assertEquals("The default work queue for all items", createdDefaultQueue.getDescription());
        assertTrue(createdDefaultQueue.isActive());
        
        // Verify it's in the repository
        defaultQueue = repository.findByName("Default Queue");
        assertTrue(defaultQueue.isPresent());
        
        // Call ensure default queue again
        WorkQueue existingDefaultQueue = repository.ensureDefaultQueue();
        
        // Verify the same queue is returned (not a new one)
        assertEquals(createdDefaultQueue.getId(), existingDefaultQueue.getId());
        assertEquals("Default Queue", existingDefaultQueue.getName());
        
        // Verify count is still 1 (not 2)
        assertEquals(1, repository.count());
    }

    @Test
    void testClear() {
        // Save queues
        repository.save(queue1);
        repository.save(queue2);
        
        // Verify both exist
        assertEquals(2, repository.count());
        
        // Clear the repository
        repository.clear();
        
        // Verify all queues are gone
        assertEquals(0, repository.count());
        assertEquals(0, repository.findAll().size());
        assertFalse(repository.findById(queueId1).isPresent());
        assertFalse(repository.findById(queueId2).isPresent());
    }
}