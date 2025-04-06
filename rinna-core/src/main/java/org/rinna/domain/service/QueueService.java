/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkQueue;
import org.rinna.domain.model.WorkflowState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing work queues.
 * This interface defines the application use cases for work queue management.
 */
public interface QueueService {
    
    /**
     * Creates a new work queue with the specified name and description.
     *
     * @param name the name of the queue
     * @param description the description of the queue
     * @return the created queue
     */
    WorkQueue createQueue(String name, String description);
    
    /**
     * Adds a work item to a queue.
     *
     * @param queueId the ID of the queue
     * @param workItemId the ID of the work item
     * @throws IllegalArgumentException if the queue or work item does not exist
     */
    void addWorkItemToQueue(UUID queueId, UUID workItemId);
    
    /**
     * Removes a work item from a queue.
     *
     * @param queueId the ID of the queue
     * @param workItemId the ID of the work item
     * @return true if the item was in the queue and removed, false otherwise
     * @throws IllegalArgumentException if the queue does not exist
     */
    boolean removeWorkItemFromQueue(UUID queueId, UUID workItemId);
    
    /**
     * Gets the next work item to be worked on from a queue.
     *
     * @param queueId the ID of the queue
     * @return an Optional containing the next work item, or empty if the queue is empty
     * @throws IllegalArgumentException if the queue does not exist
     */
    Optional<WorkItem> getNextWorkItem(UUID queueId);
    
    /**
     * Gets all work items in a queue.
     *
     * @param queueId the ID of the queue
     * @return a list of work items in the queue
     * @throws IllegalArgumentException if the queue does not exist
     */
    List<WorkItem> getQueueItems(UUID queueId);
    
    /**
     * Gets work items in a queue filtered by type.
     *
     * @param queueId the ID of the queue
     * @param type the type to filter by
     * @return a list of work items matching the type
     * @throws IllegalArgumentException if the queue does not exist
     */
    List<WorkItem> getQueueItemsByType(UUID queueId, WorkItemType type);
    
    /**
     * Gets work items in a queue filtered by state.
     *
     * @param queueId the ID of the queue
     * @param state the state to filter by
     * @return a list of work items matching the state
     * @throws IllegalArgumentException if the queue does not exist
     */
    List<WorkItem> getQueueItemsByState(UUID queueId, WorkflowState state);
    
    /**
     * Gets work items in a queue filtered by priority.
     *
     * @param queueId the ID of the queue
     * @param priority the priority to filter by
     * @return a list of work items matching the priority
     * @throws IllegalArgumentException if the queue does not exist
     */
    List<WorkItem> getQueueItemsByPriority(UUID queueId, Priority priority);
    
    /**
     * Gets work items in a queue filtered by assignee.
     *
     * @param queueId the ID of the queue
     * @param assignee the assignee to filter by
     * @return a list of work items matching the assignee
     * @throws IllegalArgumentException if the queue does not exist
     */
    List<WorkItem> getQueueItemsByAssignee(UUID queueId, String assignee);
    
    /**
     * Reprioritizes a queue based on the default prioritization algorithm.
     *
     * @param queueId the ID of the queue
     * @throws IllegalArgumentException if the queue does not exist
     */
    void reprioritizeQueue(UUID queueId);
    
    /**
     * Reprioritizes a queue based on a custom prioritization algorithm.
     *
     * @param queueId the ID of the queue
     * @param weights a map of weights for different criteria
     * @throws IllegalArgumentException if the queue does not exist
     */
    void reprioritizeQueueWithWeights(UUID queueId, Map<String, Integer> weights);
    
    /**
     * Reprioritizes a queue based on team capacity.
     *
     * @param queueId the ID of the queue
     * @param teamCapacity the capacity of the team in story points
     * @throws IllegalArgumentException if the queue does not exist
     */
    void reprioritizeQueueByCapacity(UUID queueId, int teamCapacity);
    
    /**
     * Activates a queue.
     *
     * @param queueId the ID of the queue
     * @throws IllegalArgumentException if the queue does not exist
     */
    void activateQueue(UUID queueId);
    
    /**
     * Deactivates a queue.
     *
     * @param queueId the ID of the queue
     * @throws IllegalArgumentException if the queue does not exist
     */
    void deactivateQueue(UUID queueId);
    
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
    List<WorkQueue> findAllQueues();
    
    /**
     * Finds all active queues.
     *
     * @return a list of all active queues
     */
    List<WorkQueue> findActiveQueues();
    
    /**
     * Submits a critical production incident to the system.
     * This is a convenience method that creates a high-priority bug
     * and adds it to the appropriate queue.
     *
     * @param title the title of the incident
     * @param description the description of the incident (can be null)
     * @return the created work item
     */
    WorkItem submitProductionIncident(String title, String description);
    
    /**
     * Submits a feature request to the system.
     * This is a convenience method that creates a feature work item
     * and adds it to the appropriate queue.
     *
     * @param title the title of the feature
     * @param description the description of the feature (can be null)
     * @param priority the priority of the feature (defaults to MEDIUM if null)
     * @return the created work item
     */
    WorkItem submitFeatureRequest(String title, String description, Priority priority);
    
    /**
     * Submits a technical task to the system.
     * This is a convenience method that creates a chore work item
     * and adds it to the appropriate queue.
     *
     * @param title the title of the task
     * @param description the description of the task (can be null)
     * @param priority the priority of the task (defaults to LOW if null)
     * @return the created work item
     */
    WorkItem submitTechnicalTask(String title, String description, Priority priority);
    
    /**
     * Submits a child work item to the system.
     * This is a convenience method that creates a work item with a parent
     * and adds it to the appropriate queue.
     *
     * @param title the title of the work item
     * @param type the type of the work item
     * @param parentId the ID of the parent work item
     * @param description the description of the work item (can be null)
     * @param priority the priority of the work item (can inherit from parent if null)
     * @return the created work item
     * @throws IllegalArgumentException if the parent work item does not exist or if the parent-child relationship is invalid
     */
    WorkItem submitChildWorkItem(String title, WorkItemType type, UUID parentId, String description, Priority priority);
    
    /**
     * Creates a default queue that all work items are added to.
     * This is typically called during system initialization.
     *
     * @return the created default queue
     */
    WorkQueue createDefaultQueue();
    
    /**
     * Gets the default queue that all work items are added to.
     *
     * @return the default queue
     */
    WorkQueue getDefaultQueue();
    
    /**
     * Checks if an urgent flag is set on a work item.
     *
     * @param workItemId the ID of the work item
     * @return true if the work item has an urgent flag, false otherwise
     */
    boolean isUrgent(UUID workItemId);
    
    /**
     * Sets an urgent flag on a work item.
     * This may affect its position in queues.
     *
     * @param workItemId the ID of the work item
     * @param urgent true to mark as urgent, false to unmark
     */
    void setUrgent(UUID workItemId, boolean urgent);
    
    /**
     * Gets all urgent work items across all queues.
     *
     * @return a list of urgent work items
     */
    List<WorkItem> findUrgentItems();
}