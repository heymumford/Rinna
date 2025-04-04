/*
 * Domain entity implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of the WorkItem interface.
 */
public class DefaultWorkItem implements WorkItem {
    private final UUID id;
    private final String title;
    private final String description;
    private final WorkItemType type;
    private WorkflowState status;
    private Priority priority;
    private String assignee;
    private final Instant createdAt;
    private Instant updatedAt;
    private final UUID parentId;
    
    /**
     * Constructs a new DefaultWorkItem from a create request.
     * 
     * @param id the unique identifier
     * @param request the create request
     */
    public DefaultWorkItem(UUID id, WorkItemCreateRequest request) {
        this(
            id,
            request.getTitle(),
            request.getDescription(),
            request.getType(),
            WorkflowState.FOUND, // Initial state
            request.getPriority(),
            request.getAssignee(),
            Instant.now(),
            Instant.now(),
            request.getParentId().orElse(null)
        );
    }
    
    /**
     * Constructs a new DefaultWorkItem with the given parameters.
     * 
     * @param id the unique identifier
     * @param title the title
     * @param description the description
     * @param type the type
     * @param status the workflow state
     * @param priority the priority
     * @param assignee the assignee
     * @param createdAt the creation timestamp
     * @param updatedAt the last update timestamp
     * @param parentId the parent ID
     */
    public DefaultWorkItem(
            UUID id,
            String title,
            String description,
            WorkItemType type,
            WorkflowState status,
            Priority priority,
            String assignee,
            Instant createdAt,
            Instant updatedAt,
            UUID parentId) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.description = description;
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
        this.assignee = assignee;
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated timestamp cannot be null");
        this.parentId = parentId;
    }
    
    @Override
    public UUID getId() {
        return id;
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public WorkItemType getType() {
        return type;
    }
    
    @Override
    public WorkflowState getStatus() {
        return status;
    }
    
    /**
     * Updates the workflow state and updates the last update timestamp.
     * 
     * @param status the new workflow state
     * @return this work item
     */
    public DefaultWorkItem setStatus(WorkflowState status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.updatedAt = Instant.now();
        return this;
    }
    
    @Override
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * Updates the priority and updates the last update timestamp.
     * 
     * @param priority the new priority
     * @return this work item
     */
    public DefaultWorkItem setPriority(Priority priority) {
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
        this.updatedAt = Instant.now();
        return this;
    }
    
    @Override
    public String getAssignee() {
        return assignee;
    }
    
    /**
     * Updates the assignee and updates the last update timestamp.
     * 
     * @param assignee the new assignee
     * @return this work item
     */
    public DefaultWorkItem setAssignee(String assignee) {
        this.assignee = assignee;
        this.updatedAt = Instant.now();
        return this;
    }
    
    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public Optional<UUID> getParentId() {
        return Optional.ofNullable(parentId);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultWorkItem that = (DefaultWorkItem) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "DefaultWorkItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", priority=" + priority +
                '}';
    }
}