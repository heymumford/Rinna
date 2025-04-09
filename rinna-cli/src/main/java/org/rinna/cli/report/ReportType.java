/*
 * Report type enum for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

/**
 * Defines different types of reports that can be generated.
 */
public enum ReportType {
    /**
     * Summary report of all work items.
     */
    SUMMARY,
    
    /**
     * Detailed report of work items with all fields.
     */
    DETAILED,
    
    /**
     * Status report showing work item state distribution.
     */
    STATUS,
    
    /**
     * Progress report showing completion metrics.
     */
    PROGRESS,
    
    /**
     * Assignee report showing work items by assignee.
     */
    ASSIGNEE,
    
    /**
     * Priority report showing work items by priority.
     */
    PRIORITY,
    
    /**
     * Overdue report showing late or at-risk items.
     */
    OVERDUE,
    
    /**
     * Timeline report showing expected completion times.
     */
    TIMELINE,
    
    /**
     * Burndown report showing progress over time.
     */
    BURNDOWN,
    
    /**
     * Activity report showing recent changes.
     */
    ACTIVITY,
    
    /**
     * Custom report with user-defined filters and fields.
     */
    CUSTOM
}