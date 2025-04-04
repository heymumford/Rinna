/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service;

import org.rinna.model.WorkItem;
import org.rinna.model.WorkItemCreateRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing work items.
 */
public interface ItemService {
    
    /**
     * Creates a new work item.
     *
     * @param request the request containing the work item details
     * @return the created work item
     */
    WorkItem create(WorkItemCreateRequest request);
    
    /**
     * Finds a work item by its ID.
     *
     * @param id the ID of the work item to find
     * @return an Optional containing the work item if found, or empty if not found
     */
    Optional<WorkItem> findById(UUID id);
    
    /**
     * Finds all work items.
     *
     * @return a list of all work items
     */
    List<WorkItem> findAll();
}