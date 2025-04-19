package org.rinna.usecase;

import java.util.List;

import org.rinna.domain.WorkItem;

/**
 * Service for managing the backlog of work items.
 */
public interface BacklogService {
    /**
     * Add an item to the backlog.
     *
     * @param workItem the work item to add
     * @return the added work item
     */
    WorkItem addToBacklog(WorkItem workItem);
    
    /**
     * Get all items in the backlog.
     *
     * @return list of backlog items
     */
    List<WorkItem> getBacklogItems();
    
    /**
     * Get the priority order of a backlog item.
     *
     * @param workItemId the ID of the work item
     * @return the priority order (lower number = higher priority)
     */
    int getBacklogPriority(String workItemId);
    
    /**
     * Set the priority order of a backlog item.
     *
     * @param workItemId the ID of the work item
     * @param priority the priority order (lower number = higher priority)
     */
    void setBacklogPriority(String workItemId, int priority);
    
    /**
     * Remove an item from the backlog.
     *
     * @param workItemId the ID of the work item to remove
     * @return true if the item was removed, false otherwise
     */
    boolean removeFromBacklog(String workItemId);
}