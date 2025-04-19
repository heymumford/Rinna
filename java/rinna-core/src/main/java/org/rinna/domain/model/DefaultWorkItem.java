/*
 * Model class for the Rinna workflow management system
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
 * Default implementation of the WorkItem interface.
 */
public final class DefaultWorkItem implements WorkItem {
    private final UUID id;
    private final String title;
    private final String description;
    private final WorkItemType type;
    private final WorkflowState status;
    private final Priority priority;
    private final String assignee;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID parentId;

    /**
     * Builder for DefaultWorkItem.
     */
    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String title;
        private String description = "";
        private WorkItemType type;
        private WorkflowState status = WorkflowState.TO_DO;
        private Priority priority = Priority.MEDIUM;
        private String assignee;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private UUID parentId;

        /**
         * Sets the ID for the work item.
         *
         * @param id the ID
         * @return this builder instance
         */
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the title for the work item.
         *
         * @param title the title
         * @return this builder instance
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the description for the work item.
         *
         * @param description the description
         * @return this builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the type for the work item.
         *
         * @param type the type
         * @return this builder instance
         */
        public Builder type(WorkItemType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the status for the work item.
         *
         * @param status the status
         * @return this builder instance
         */
        public Builder status(WorkflowState status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the priority for the work item.
         *
         * @param priority the priority
         * @return this builder instance
         */
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the assignee for the work item.
         *
         * @param assignee the assignee
         * @return this builder instance
         */
        public Builder assignee(String assignee) {
            this.assignee = assignee;
            return this;
        }

        /**
         * Sets the creation timestamp for the work item.
         *
         * @param createdAt the creation timestamp
         * @return this builder instance
         */
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the update timestamp for the work item.
         *
         * @param updatedAt the update timestamp
         * @return this builder instance
         */
        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Sets the parent ID for the work item.
         *
         * @param parentId the parent ID
         * @return this builder instance
         */
        public Builder parentId(UUID parentId) {
            this.parentId = parentId;
            return this;
        }

        /**
         * Builds a new DefaultWorkItem with the configured values.
         *
         * @return a new DefaultWorkItem instance
         */
        public DefaultWorkItem build() {
            if (title == null || title.isBlank()) {
                throw new IllegalStateException("Title is required");
            }
            if (type == null) {
                throw new IllegalStateException("Type is required");
            }
            return new DefaultWorkItem(this);
        }
    }

    /**
     * Returns a new builder for creating a DefaultWorkItem.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new DefaultWorkItem from a WorkItemCreateRequest.
     *
     * @param request the create request
     * @return a new DefaultWorkItem instance
     */
    public static DefaultWorkItem fromCreateRequest(WorkItemCreateRequest request) {
        Builder builder = builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .priority(request.getPriority());

        if (request.getAssignee() != null) {
            builder.assignee(request.getAssignee());
        }

        request.getParentId().ifPresent(builder::parentId);

        return builder.build();
    }

    private DefaultWorkItem(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.type = builder.type;
        this.status = builder.status;
        this.priority = builder.priority;
        this.assignee = builder.assignee;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.parentId = builder.parentId;
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

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    public String getAssignee() {
        return assignee;
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

    /**
     * Creates a new DefaultWorkItem with a new status.
     *
     * @param newStatus the new status
     * @return a new DefaultWorkItem instance with the updated status
     */
    public DefaultWorkItem withStatus(WorkflowState newStatus) {
        return new Builder()
                .id(this.id)
                .title(this.title)
                .description(this.description)
                .type(this.type)
                .status(newStatus)
                .priority(this.priority)
                .assignee(this.assignee)
                .createdAt(this.createdAt)
                .updatedAt(Instant.now())
                .parentId(this.parentId)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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
                "}";
    }

    /**
     * Converts this DefaultWorkItem to a WorkItemRecord.
     *
     * @return a new WorkItemRecord with the same data as this DefaultWorkItem
     */
    public WorkItemRecord toRecord() {
        return new WorkItemRecord(
            id,
            title,
            description,
            type,
            status,
            priority,
            assignee,
            createdAt,
            updatedAt,
            parentId,
            null, // projectId
            "PUBLIC", // visibility
            false // localOnly
        );
    }
}
