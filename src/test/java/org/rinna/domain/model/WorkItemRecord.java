/*
 * Domain entity implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable record implementation of the WorkItem interface.
 * This is a core domain entity record that represents a work item.
 */
public record WorkItemRecord(
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
    boolean localOnly
) implements WorkItem {

    /**
     * Constructs a new WorkItemRecord with validation.
     */
    public WorkItemRecord {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(priority, "Priority cannot be null");
        Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        Objects.requireNonNull(updatedAt, "Updated timestamp cannot be null");

        // Default visibility to PUBLIC if not specified
        if (visibility == null) {
            visibility = "PUBLIC";
        }
    }

    /**
     * Factory method to create a WorkItemRecord from a create request.
     */
    public static WorkItemRecord fromRequest(UUID id, WorkItemCreateRequest request) {
        Instant now = Instant.now();
        return new WorkItemRecord(
            id,
            request.title(),
            request.description(),
            request.type(),
            WorkflowState.FOUND, // Initial state
            request.priority(),
            request.assignee(),
            now,
            now,
            request.getParentId().orElse(null),
            request.getProjectId().orElse(null),
            request.visibility(),
            request.localOnly()
        );
    }

    /**
     * Returns a new WorkItemRecord with updated status, keeping all other fields the same.
     */
    public WorkItemRecord withStatus(WorkflowState newStatus) {
        return new WorkItemRecord(
            id, title, description, type, 
            newStatus, priority, assignee, 
            createdAt, Instant.now(), parentId, 
            projectId, visibility, localOnly
        );
    }

    /**
     * Returns a new WorkItemRecord with updated priority, keeping all other fields the same.
     */
    public WorkItemRecord withPriority(Priority newPriority) {
        return new WorkItemRecord(
            id, title, description, type, 
            status, newPriority, assignee, 
            createdAt, Instant.now(), parentId, 
            projectId, visibility, localOnly
        );
    }

    /**
     * Returns a new WorkItemRecord with updated assignee, keeping all other fields the same.
     */
    public WorkItemRecord withAssignee(String newAssignee) {
        return new WorkItemRecord(
            id, title, description, type, 
            status, priority, newAssignee, 
            createdAt, Instant.now(), parentId, 
            projectId, visibility, localOnly
        );
    }

    /**
     * Creates a new record with updated fields. Null values means keeping the existing value.
     */
    public WorkItemRecord with(
            WorkflowState newStatus,
            Priority newPriority,
            String newAssignee) {
        return new WorkItemRecord(
            id, title, description, type,
            newStatus != null ? newStatus : status,
            newPriority != null ? newPriority : priority,
            newAssignee, // Can be null to remove assignee
            createdAt, Instant.now(), parentId,
            projectId, visibility, localOnly
        );
    }

    // Implementation of WorkItem interface
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

    @Override
    public String toString() {
        return "WorkItemRecord{id=" + id + ", title='" + title + "', type=" + type + ", status=" + status + ", priority=" + priority + "}";
    }
}
