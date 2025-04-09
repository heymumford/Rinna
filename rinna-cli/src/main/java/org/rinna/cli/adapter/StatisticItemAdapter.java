/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.adapter;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.domain.model.WorkflowState;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Adapter class for statistics-related work item operations.
 */
public class StatisticItemAdapter {
    private final WorkItem workItem;
    
    /**
     * Creates a new adapter for a work item.
     *
     * @param workItem the work item to adapt
     */
    public StatisticItemAdapter(WorkItem workItem) {
        this.workItem = workItem;
    }
    
    /**
     * Gets the underlying work item.
     *
     * @return the work item
     */
    public WorkItem getWorkItem() {
        return workItem;
    }
    
    /**
     * Gets the created at timestamp as an Instant.
     *
     * @return the created at timestamp
     */
    public Instant getCreatedAt() {
        return workItem.getCreated() != null
            ? workItem.getCreated().atZone(ZoneId.systemDefault()).toInstant()
            : Instant.now();
    }
    
    /**
     * Gets the updated at timestamp as an Instant.
     *
     * @return the updated at timestamp
     */
    public Instant getUpdatedAt() {
        return workItem.getUpdated() != null
            ? workItem.getUpdated().atZone(ZoneId.systemDefault()).toInstant()
            : Instant.now();
    }
    
    /**
     * Gets the due date as an Instant.
     *
     * @return the due date, or null if not set
     */
    public Instant getDueDate() {
        return workItem.getDueDate() != null
            ? workItem.getDueDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            : null;
    }
    
    /**
     * Gets the workflow state.
     *
     * @return the workflow state
     */
    public WorkflowState getState() {
        return WorkflowState.valueOf(workItem.getState().name());
    }
    
    /**
     * Checks if the work item is completed.
     *
     * @return true if the work item is completed
     */
    public boolean isCompleted() {
        return workItem.getState().name().equals("DONE") || 
               workItem.getState().name().equals("COMPLETED");
    }
    
    /**
     * Gets the project ID for the work item.
     *
     * @return the project ID
     */
    public String getProjectId() {
        return workItem.getProjectId();
    }
}