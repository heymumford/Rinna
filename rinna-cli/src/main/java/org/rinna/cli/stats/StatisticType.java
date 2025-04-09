/*
 * Statistic type enum for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.stats;

/**
 * Defines different types of statistics that can be tracked.
 */
public enum StatisticType {
    /**
     * Total number of work items.
     */
    TOTAL_ITEMS,
    
    /**
     * Number of work items by type (task, bug, feature, etc.).
     */
    ITEMS_BY_TYPE,
    
    /**
     * Number of work items by state (open, in progress, done, etc.).
     */
    ITEMS_BY_STATE,
    
    /**
     * Number of work items by priority.
     */
    ITEMS_BY_PRIORITY,
    
    /**
     * Number of work items by assignee.
     */
    ITEMS_BY_ASSIGNEE,
    
    /**
     * Completion rate (completed items / total items).
     */
    COMPLETION_RATE,
    
    /**
     * Average time to complete work items.
     */
    AVG_COMPLETION_TIME,
    
    /**
     * Number of items completed in a time period.
     */
    ITEMS_COMPLETED,
    
    /**
     * Number of items created in a time period.
     */
    ITEMS_CREATED,
    
    /**
     * Number of overdue items.
     */
    OVERDUE_ITEMS,
    
    /**
     * Burndown rate (items completed per day/week).
     */
    BURNDOWN_RATE,
    
    /**
     * Lead time (time from creation to completion).
     */
    LEAD_TIME,
    
    /**
     * Cycle time (time from start of work to completion).
     */
    CYCLE_TIME,
    
    /**
     * Throughput (number of items completed per time period).
     */
    THROUGHPUT,
    
    /**
     * Work in progress (current number of items in progress).
     */
    WORK_IN_PROGRESS
}