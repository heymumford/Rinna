/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.Workstream;
import org.rinna.domain.model.WorkstreamCreateRequest;

/**
 * Service interface for managing workstreams in the Rinna system.
 * Provides business functionality for creating, updating, and querying workstreams
 * and their relationships with projects and work items.
 */
public interface WorkstreamService {
    
    /**
     * Creates a new workstream from the provided create request.
     *
     * @param request the workstream create request
     * @return the created workstream
     */
    Workstream createWorkstream(WorkstreamCreateRequest request);
    
    /**
     * Updates an existing workstream.
     *
     * @param workstream the workstream with updated properties
     * @return the updated workstream
     * @throws IllegalArgumentException if the workstream does not exist
     */
    Workstream updateWorkstream(Workstream workstream);
    
    /**
     * Finds a workstream by its ID.
     *
     * @param id the workstream ID
     * @return an Optional containing the workstream, or empty if not found
     */
    Optional<Workstream> findWorkstreamById(UUID id);
    
    /**
     * Finds all workstreams.
     *
     * @return a list of all workstreams
     */
    List<Workstream> findAllWorkstreams();
    
    /**
     * Finds workstreams by status.
     *
     * @param status the status to filter by
     * @return a list of workstreams with the given status
     */
    List<Workstream> findWorkstreamsByStatus(String status);
    
    /**
     * Finds workstreams by owner.
     *
     * @param owner the owner to filter by
     * @return a list of workstreams with the given owner
     */
    List<Workstream> findWorkstreamsByOwner(String owner);
    
    /**
     * Finds workstreams by priority.
     *
     * @param priority the priority to filter by
     * @return a list of workstreams with the given priority
     */
    List<Workstream> findWorkstreamsByPriority(Priority priority);
    
    /**
     * Finds workstreams by CYNEFIN domain.
     *
     * @param domain the CYNEFIN domain to filter by
     * @return a list of workstreams with the given CYNEFIN domain
     */
    List<Workstream> findWorkstreamsByCynefinDomain(CynefinDomain domain);
    
    /**
     * Finds cross-project workstreams.
     *
     * @return a list of cross-project workstreams
     */
    List<Workstream> findCrossProjectWorkstreams();
    
    /**
     * Deletes a workstream by ID.
     *
     * @param id the workstream ID to delete
     * @return true if the workstream was deleted, false if it didn't exist
     */
    boolean deleteWorkstream(UUID id);
    
    /**
     * Associates a project with a workstream.
     *
     * @param workstreamId the workstream ID
     * @param projectId the project ID to associate
     * @return true if the association was created, false otherwise
     */
    boolean associateProject(UUID workstreamId, UUID projectId);
    
    /**
     * Associates multiple projects with a workstream.
     *
     * @param workstreamId the workstream ID
     * @param projectIds the project IDs to associate
     * @return the number of successful associations
     */
    int associateProjects(UUID workstreamId, Set<UUID> projectIds);
    
    /**
     * Dissociates a project from a workstream.
     *
     * @param workstreamId the workstream ID
     * @param projectId the project ID to dissociate
     * @return true if the association was removed, false otherwise
     */
    boolean dissociateProject(UUID workstreamId, UUID projectId);
    
    /**
     * Gets all projects associated with a workstream.
     *
     * @param workstreamId the workstream ID
     * @return a list of associated project IDs
     */
    List<UUID> getProjectsForWorkstream(UUID workstreamId);
    
    /**
     * Gets all workstreams associated with a project.
     *
     * @param projectId the project ID
     * @return a list of associated workstreams
     */
    List<Workstream> getWorkstreamsForProject(UUID projectId);
    
    /**
     * Associates a work item with a workstream.
     *
     * @param workstreamId the workstream ID
     * @param workItemId the work item ID to associate
     * @return true if the association was created, false otherwise
     */
    boolean associateWorkItem(UUID workstreamId, UUID workItemId);
    
    /**
     * Associates multiple work items with a workstream.
     *
     * @param workstreamId the workstream ID
     * @param workItemIds the work item IDs to associate
     * @return the number of successful associations
     */
    int associateWorkItems(UUID workstreamId, Set<UUID> workItemIds);
    
    /**
     * Dissociates a work item from a workstream.
     *
     * @param workstreamId the workstream ID
     * @param workItemId the work item ID to dissociate
     * @return true if the association was removed, false otherwise
     */
    boolean dissociateWorkItem(UUID workstreamId, UUID workItemId);
    
    /**
     * Gets all work items associated with a workstream.
     *
     * @param workstreamId the workstream ID
     * @return a list of associated work item IDs
     */
    List<UUID> getWorkItemsForWorkstream(UUID workstreamId);
    
    /**
     * Gets all workstreams associated with a work item.
     *
     * @param workItemId the work item ID
     * @return a list of associated workstreams
     */
    List<Workstream> getWorkstreamsForWorkItem(UUID workItemId);
    
    /**
     * Updates the status of a workstream.
     *
     * @param workstreamId the workstream ID
     * @param status the new status
     * @return the updated workstream
     * @throws IllegalArgumentException if the workstream does not exist
     */
    Workstream updateWorkstreamStatus(UUID workstreamId, String status);
    
    /**
     * Updates the priority of a workstream.
     *
     * @param workstreamId the workstream ID
     * @param priority the new priority
     * @return the updated workstream
     * @throws IllegalArgumentException if the workstream does not exist
     */
    Workstream updateWorkstreamPriority(UUID workstreamId, Priority priority);
    
    /**
     * Updates the CYNEFIN domain of a workstream.
     *
     * @param workstreamId the workstream ID
     * @param domain the new CYNEFIN domain
     * @return the updated workstream
     * @throws IllegalArgumentException if the workstream does not exist
     */
    Workstream updateWorkstreamCynefinDomain(UUID workstreamId, CynefinDomain domain);
    
    /**
     * Updates the cross-project status of a workstream.
     *
     * @param workstreamId the workstream ID
     * @param crossProject whether the workstream is cross-project
     * @return the updated workstream
     * @throws IllegalArgumentException if the workstream does not exist
     */
    Workstream updateWorkstreamCrossProject(UUID workstreamId, boolean crossProject);
    
    /**
     * Analyzes the context of a workstream and suggests an appropriate CYNEFIN domain
     * based on its properties and relationships.
     *
     * @param workstreamId the workstream ID
     * @return the suggested CYNEFIN domain
     * @throws IllegalArgumentException if the workstream does not exist
     */
    CynefinDomain suggestCynefinDomain(UUID workstreamId);
    
    /**
     * Suggests appropriate workstreams for a work item based on its properties
     * and the properties of existing workstreams.
     *
     * @param workItemId the work item ID
     * @return a list of suggested workstreams, sorted by relevance
     */
    List<Workstream> suggestWorkstreamsForWorkItem(UUID workItemId);
}