/*
 * Domain repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.WorkQueue;

/**
 * Repository interface for work queues.
 * This interface defines the data access operations for work queues.
 */
public interface QueueRepository {
    
    /**
     * Saves a work queue.
     * If the queue already exists, it will be updated; otherwise, it will be created.
     *
     * @param queue the queue to save
     * @return the saved queue
     */
    WorkQueue save(WorkQueue queue);
    
    /**
     * Finds a queue by its ID.
     *
     * @param id the ID of the queue
     * @return an Optional containing the queue, or empty if not found
     */
    Optional<WorkQueue> findById(UUID id);
    
    /**
     * Finds a queue by its name.
     *
     * @param name the name of the queue
     * @return an Optional containing the queue, or empty if not found
     */
    Optional<WorkQueue> findByName(String name);
    
    /**
     * Finds all queues.
     *
     * @return a list of all queues
     */
    List<WorkQueue> findAll();
    
    /**
     * Finds all active queues.
     *
     * @return a list of all active queues
     */
    List<WorkQueue> findByActive(boolean active);
    
    /**
     * Deletes a queue by its ID.
     *
     * @param id the ID of the queue to delete
     * @return true if the queue was deleted, false if it wasn't found
     */
    boolean deleteById(UUID id);
    
    /**
     * Counts the number of queues.
     *
     * @return the number of queues
     */
    long count();
}