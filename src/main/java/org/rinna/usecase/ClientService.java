/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import org.rinna.domain.Project;
import org.rinna.domain.WorkItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for local client-related operations.
 */
public interface ClientService {
    
    /**
     * Authenticates a client.
     *
     * @param token the API token
     * @return the associated project
     * @throws APIService.AuthenticationException if authentication fails
     */
    Project authenticate(String token) throws APIService.AuthenticationException;
    
    /**
     * Creates a local work item report.
     *
     * @param projectId the project ID
     * @param attributes the work item attributes
     * @return the created work item
     */
    WorkItem createLocalReport(UUID projectId, Map<String, String> attributes);
    
    /**
     * Synchronizes local work items with the central system.
     *
     * @param projectId the project ID
     * @param localItems the local work items
     * @return the synchronized work items
     */
    List<WorkItem> synchronize(UUID projectId, List<WorkItem> localItems);
    
    /**
     * Changes the visibility of a work item.
     *
     * @param workItemId the work item ID
     * @param visibility the new visibility
     * @return the updated work item
     */
    WorkItem changeVisibility(UUID workItemId, String visibility);
    
    /**
     * Returns all local work items for a project.
     *
     * @param projectId the project ID
     * @return a list of local work items
     */
    List<WorkItem> findLocalItems(UUID projectId);
}