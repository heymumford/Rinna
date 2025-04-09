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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Adapter class to adapt WorkItem model to methods needed by the report generator.
 */
public class ReportItemAdapter {
    private final WorkItem workItem;
    
    /**
     * Creates a new adapter for a work item.
     *
     * @param workItem the work item to adapt
     */
    public ReportItemAdapter(WorkItem workItem) {
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
        LocalDateTime created = workItem.getCreated();
        return created != null ? created.atZone(ZoneId.systemDefault()).toInstant() : Instant.now();
    }
    
    /**
     * Gets the updated at timestamp as an Instant.
     *
     * @return the updated at timestamp
     */
    public Instant getUpdatedAt() {
        LocalDateTime updated = workItem.getUpdated();
        return updated != null ? updated.atZone(ZoneId.systemDefault()).toInstant() : Instant.now();
    }
    
    /**
     * Gets the reporter of the work item.
     *
     * @return the reporter
     */
    public String getReporter() {
        // In this mock implementation, we don't have a reporter field
        // so we'll return a default value
        return "system";
    }
    
    /**
     * Gets the created timestamp formatted.
     *
     * @param formatter the formatter to use
     * @return the formatted timestamp
     */
    public String getCreatedFormatted(java.time.format.DateTimeFormatter formatter) {
        return getCreatedAt().atZone(ZoneId.systemDefault()).format(formatter);
    }
    
    /**
     * Gets the updated timestamp formatted.
     *
     * @param formatter the formatter to use
     * @return the formatted timestamp
     */
    public String getUpdatedFormatted(java.time.format.DateTimeFormatter formatter) {
        return getUpdatedAt().atZone(ZoneId.systemDefault()).format(formatter);
    }
}