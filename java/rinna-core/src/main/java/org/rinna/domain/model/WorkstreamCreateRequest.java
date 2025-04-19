/*
 * Domain entity for the Rinna workflow management system
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
 * A request to create a new workstream.
 */
public record WorkstreamCreateRequest(
    String name,
    String description,
    String owner,
    String status,
    Priority priority,
    UUID organizationId,
    CynefinDomain cynefinDomain,
    boolean crossProject,
    Instant targetDate
) {
    /**
     * Constructor with validation.
     */
    public WorkstreamCreateRequest {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(owner, "Owner cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(priority, "Priority cannot be null");
    }
    
    /**
     * Returns the organization ID.
     *
     * @return an Optional containing the organization ID
     */
    public Optional<UUID> getOrganizationId() {
        return Optional.ofNullable(organizationId);
    }
    
    /**
     * Returns the CYNEFIN domain.
     *
     * @return an Optional containing the CYNEFIN domain
     */
    public Optional<CynefinDomain> getCynefinDomain() {
        return Optional.ofNullable(cynefinDomain);
    }
    
    /**
     * Returns the target date.
     *
     * @return an Optional containing the target date
     */
    public Optional<Instant> getTargetDate() {
        return Optional.ofNullable(targetDate);
    }
    
    /**
     * A builder for WorkstreamCreateRequest.
     */
    public static class Builder {
        private String name;
        private String description;
        private String owner;
        private String status = WorkstreamStatus.DRAFT.name();
        private Priority priority = Priority.MEDIUM;
        private UUID organizationId;
        private CynefinDomain cynefinDomain;
        private boolean crossProject = false;
        private Instant targetDate;
        
        /**
         * Sets the name.
         *
         * @param name the name
         * @return this Builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Sets the description.
         *
         * @param description the description
         * @return this Builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the owner.
         *
         * @param owner the owner
         * @return this Builder
         */
        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }
        
        /**
         * Sets the status.
         *
         * @param status the status
         * @return this Builder
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        /**
         * Sets the status.
         *
         * @param status the status
         * @return this Builder
         */
        public Builder status(WorkstreamStatus status) {
            this.status = status.name();
            return this;
        }
        
        /**
         * Sets the priority.
         *
         * @param priority the priority
         * @return this Builder
         */
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }
        
        /**
         * Sets the organization ID.
         *
         * @param organizationId the organization ID
         * @return this Builder
         */
        public Builder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }
        
        /**
         * Sets the CYNEFIN domain.
         *
         * @param cynefinDomain the CYNEFIN domain
         * @return this Builder
         */
        public Builder cynefinDomain(CynefinDomain cynefinDomain) {
            this.cynefinDomain = cynefinDomain;
            return this;
        }
        
        /**
         * Sets whether the workstream is cross-project.
         *
         * @param crossProject whether the workstream is cross-project
         * @return this Builder
         */
        public Builder crossProject(boolean crossProject) {
            this.crossProject = crossProject;
            return this;
        }
        
        /**
         * Sets the target date.
         *
         * @param targetDate the target date
         * @return this Builder
         */
        public Builder targetDate(Instant targetDate) {
            this.targetDate = targetDate;
            return this;
        }
        
        /**
         * Builds a new WorkstreamCreateRequest.
         *
         * @return a new WorkstreamCreateRequest
         */
        public WorkstreamCreateRequest build() {
            return new WorkstreamCreateRequest(
                name, description, owner, status, priority,
                organizationId, cynefinDomain, crossProject, targetDate
            );
        }
    }
}