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
 * 
 * @deprecated Use {@link WorkItemRecord} instead for immutability
 */
@Deprecated(since = "1.2.7", forRemoval = true)
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
    private final UUID projectId;
    private final String visibility;
    private final boolean localOnly;
    
    /**
     * Constructs a new DefaultWorkItem from a create request.
     * 
     * @deprecated Use {@link WorkItemRecord#fromRequest(UUID, WorkItemCreateRequest)} instead
     */
    @Deprecated(since = "1.2.7", forRemoval = true)
    public DefaultWorkItem(UUID id, WorkItemCreateRequest request) {
        this(
            id,
            request.title(),
            request.description(),
            request.type(),
            WorkflowState.FOUND, // Initial state
            request.priority(),
            request.assignee(),
            Instant.now(),
            Instant.now(),
            request.getParentId().orElse(null),
            request.getProjectId().orElse(null),
            request.visibility(),
            request.localOnly()
        );
    }
    
    /**
     * Constructs a new DefaultWorkItem with the given parameters.
     * 
     * @deprecated Use {@link WorkItemRecord} instead
     */
    @Deprecated(since = "1.2.7", forRemoval = true)
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
            UUID parentId,
            UUID projectId,
            String visibility,
            boolean localOnly) {
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
        this.projectId = projectId;
        this.visibility = visibility != null ? visibility : "PUBLIC";
        this.localOnly = localOnly;
    }

    /**
     * Creates a copy with modified fields.
     * 
     * @deprecated Use {@link WorkItemRecord#with(WorkflowState, Priority, String)} instead
     */
    @Deprecated(since = "1.2.7", forRemoval = true)
    public DefaultWorkItem with(
            WorkflowState status,
            Priority priority,
            String assignee) {
        return new DefaultWorkItem(
            this.id,
            this.title,
            this.description,
            this.type,
            status != null ? status : this.status,
            priority != null ? priority : this.priority,
            assignee, // Can be null to remove assignee
            this.createdAt,
            Instant.now(),
            this.parentId,
            this.projectId,
            this.visibility,
            this.localOnly
        );
    }
    
    // Convert to WorkItemRecord
    public WorkItemRecord toRecord() {
        return new WorkItemRecord(
            id, title, description, type, status, priority, assignee,
            createdAt, updatedAt, parentId, projectId, visibility, localOnly
        );
    }
    
    @Override public UUID getId() { return id; }
    @Override public String getTitle() { return title; }
    @Override public String getDescription() { return description; }
    @Override public WorkItemType getType() { return type; }
    @Override public WorkflowState getStatus() { return status; }
    @Override public Priority getPriority() { return priority; }
    @Override public String getAssignee() { return assignee; }
    @Override public Instant getCreatedAt() { return createdAt; }
    @Override public Instant getUpdatedAt() { return updatedAt; }
    @Override public Optional<UUID> getParentId() { return Optional.ofNullable(parentId); }
    @Override public Optional<UUID> getProjectId() { return Optional.ofNullable(projectId); }
    @Override public String getVisibility() { return visibility; }
    @Override public boolean isLocalOnly() { return localOnly; }
    
    /**
     * Updates the workflow state and updates the last update timestamp.
     * 
     * @deprecated Use immutable {@link WorkItemRecord#withStatus(WorkflowState)} instead
     */
    @Deprecated(since = "1.2.7", forRemoval = true)
    public DefaultWorkItem setStatus(WorkflowState status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.updatedAt = Instant.now();
        return this;
    }
    
    /**
     * Updates the priority and updates the last update timestamp.
     * 
     * @deprecated Use immutable {@link WorkItemRecord#withPriority(Priority)} instead
     */
    @Deprecated(since = "1.2.7", forRemoval = true)
    public DefaultWorkItem setPriority(Priority priority) {
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
        this.updatedAt = Instant.now();
        return this;
    }
    
    /**
     * Updates the assignee and updates the last update timestamp.
     * 
     * @deprecated Use immutable {@link WorkItemRecord#withAssignee(String)} instead
     */
    @Deprecated(since = "1.2.7", forRemoval = true)
    public DefaultWorkItem setAssignee(String assignee) {
        this.assignee = assignee;
        this.updatedAt = Instant.now();
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkItem that)) return false;
        return Objects.equals(id, that.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return STR."DefaultWorkItem{id=\{id}, title='\{title}', type=\{type}, status=\{status}, priority=\{priority}}";
    }
}