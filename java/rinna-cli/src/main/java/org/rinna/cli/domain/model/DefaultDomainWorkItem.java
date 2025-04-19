/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of the DomainWorkItem interface.
 */
public class DefaultDomainWorkItem implements DomainWorkItem {
    private UUID id;
    private String title;
    private String description;
    private DomainWorkItemType type;
    private DomainPriority priority;
    private DomainWorkflowState state;
    private String assignee;
    private String reporter;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Creates a new DefaultDomainWorkItem with default values.
     */
    public DefaultDomainWorkItem() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }
    
    /**
     * Creates a DefaultDomainWorkItem with the specified ID.
     *
     * @param id the work item ID
     */
    public DefaultDomainWorkItem(UUID id) {
        this.id = id;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }
    
    @Override
    public UUID getId() {
        return id;
    }
    
    /**
     * Sets the ID of the work item.
     *
     * @param id the work item ID
     */
    public void setId(UUID id) {
        this.id = id;
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    /**
     * Sets the title of the work item.
     *
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the description of the work item.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public DomainWorkItemType getType() {
        return type;
    }
    
    /**
     * Sets the type of the work item.
     *
     * @param type the work item type
     */
    public void setType(DomainWorkItemType type) {
        this.type = type;
    }
    
    @Override
    public DomainPriority getPriority() {
        return priority;
    }
    
    /**
     * Sets the priority of the work item.
     *
     * @param priority the priority
     */
    public void setPriority(DomainPriority priority) {
        this.priority = priority;
    }
    
    @Override
    public DomainWorkflowState getState() {
        return state;
    }
    
    /**
     * Sets the state of the work item.
     *
     * @param state the workflow state
     */
    public void setState(DomainWorkflowState state) {
        this.state = state;
        this.updatedAt = Instant.now();
    }
    
    @Override
    public String getAssignee() {
        return assignee;
    }
    
    /**
     * Sets the assignee of the work item.
     *
     * @param assignee the assignee
     */
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    @Override
    public String getReporter() {
        return reporter;
    }
    
    /**
     * Sets the reporter of the work item.
     *
     * @param reporter the reporter
     */
    public void setReporter(String reporter) {
        this.reporter = reporter;
    }
    
    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the creation timestamp of the work item.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Sets the creation timestamp from a LocalDateTime.
     *
     * @param createdAt the creation timestamp as LocalDateTime
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant();
    }
    
    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Sets the last update timestamp of the work item.
     *
     * @param updatedAt the last update timestamp
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Sets the update timestamp from a LocalDateTime.
     *
     * @param updatedAt the update timestamp as LocalDateTime
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt.atZone(ZoneId.systemDefault()).toInstant();
    }
}