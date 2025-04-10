/*
 * Repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.Workstream;
import org.rinna.domain.model.WorkstreamCreateRequest;

/**
 * Repository interface for managing workstreams.
 */
public interface WorkstreamRepository {
    
    /**
     * Creates a new workstream.
     *
     * @param request the create request
     * @return the created workstream
     */
    Workstream create(WorkstreamCreateRequest request);
    
    /**
     * Finds a workstream by its ID.
     *
     * @param id the ID of the workstream
     * @return an Optional containing the workstream, or empty if not found
     */
    Optional<Workstream> findById(UUID id);
    
    /**
     * Finds all workstreams.
     *
     * @return a list of all workstreams
     */
    List<Workstream> findAll();
    
    /**
     * Finds workstreams by status.
     *
     * @param status the status of workstreams to find
     * @return a list of workstreams with the given status
     */
    List<Workstream> findByStatus(String status);
    
    /**
     * Finds workstreams by owner.
     *
     * @param owner the owner of workstreams to find
     * @return a list of workstreams with the given owner
     */
    List<Workstream> findByOwner(String owner);
    
    /**
     * Finds workstreams by priority.
     *
     * @param priority the priority of workstreams to find
     * @return a list of workstreams with the given priority
     */
    List<Workstream> findByPriority(Priority priority);
    
    /**
     * Finds workstreams by organization.
     *
     * @param organizationId the organization ID of workstreams to find
     * @return a list of workstreams with the given organization ID
     */
    List<Workstream> findByOrganization(UUID organizationId);
    
    /**
     * Finds workstreams by CYNEFIN domain.
     *
     * @param domain the CYNEFIN domain of workstreams to find
     * @return a list of workstreams with the given CYNEFIN domain
     */
    List<Workstream> findByCynefinDomain(CynefinDomain domain);
    
    /**
     * Finds workstreams that span multiple projects.
     *
     * @return a list of cross-project workstreams
     */
    List<Workstream> findCrossProjectWorkstreams();
    
    /**
     * Finds workstreams associated with a project.
     *
     * @param projectId the project ID
     * @return a list of workstreams associated with the given project
     */
    List<Workstream> findByProject(UUID projectId);
    
    /**
     * Finds workstreams associated with a work item.
     *
     * @param workItemId the work item ID
     * @return a list of workstreams associated with the given work item
     */
    List<Workstream> findByWorkItem(UUID workItemId);
    
    /**
     * Saves changes to a workstream.
     *
     * @param workstream the workstream to save
     * @return the saved workstream
     */
    Workstream save(Workstream workstream);
    
    /**
     * Deletes a workstream by ID.
     *
     * @param id the ID of the workstream to delete
     */
    void deleteById(UUID id);
    
    /**
     * Associates a work item with a workstream.
     *
     * @param workstreamId the workstream ID
     * @param workItemId the work item ID
     * @return true if successful
     */
    boolean associateWorkItem(UUID workstreamId, UUID workItemId);
    
    /**
     * Dissociates a work item from a workstream.
     *
     * @param workstreamId the workstream ID
     * @param workItemId the work item ID
     * @return true if successful
     */
    boolean dissociateWorkItem(UUID workstreamId, UUID workItemId);
    
    /**
     * Finds all work items in a workstream.
     *
     * @param workstreamId the workstream ID
     * @return a list of work item IDs in the workstream
     */
    List<UUID> findWorkItemIdsByWorkstreamId(UUID workstreamId);
    
    /**
     * Associates a project with a workstream.
     *
     * @param workstreamId the workstream ID
     * @param projectId the project ID
     * @return true if successful
     */
    boolean associateProject(UUID workstreamId, UUID projectId);
    
    /**
     * Dissociates a project from a workstream.
     *
     * @param workstreamId the workstream ID
     * @param projectId the project ID
     * @return true if successful
     */
    boolean dissociateProject(UUID workstreamId, UUID projectId);
    
    /**
     * Finds all projects associated with a workstream.
     *
     * @param workstreamId the workstream ID
     * @return a list of project IDs associated with the workstream
     */
    List<UUID> findProjectIdsByWorkstreamId(UUID workstreamId);
}