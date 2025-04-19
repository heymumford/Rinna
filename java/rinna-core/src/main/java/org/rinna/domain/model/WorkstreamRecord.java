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
 * Immutable implementation of the Workstream interface.
 */
public record WorkstreamRecord(
    UUID id,
    String name,
    String description,
    String owner,
    String status,
    Priority priority,
    Instant createdAt,
    Instant updatedAt,
    UUID organizationId,
    CynefinDomain cynefinDomain,
    boolean crossProject,
    Instant targetDate
) implements Workstream {
    
    /**
     * Constructor with validation.
     */
    public WorkstreamRecord {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(owner, "Owner cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(priority, "Priority cannot be null");
        Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        Objects.requireNonNull(updatedAt, "Updated timestamp cannot be null");
    }
    
    @Override
    public UUID getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getOwner() {
        return owner;
    }
    
    @Override
    public String getStatus() {
        return status;
    }
    
    @Override
    public Priority getPriority() {
        return priority;
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
    public Optional<UUID> getOrganizationId() {
        return Optional.ofNullable(organizationId);
    }
    
    @Override
    public Optional<CynefinDomain> getCynefinDomain() {
        return Optional.ofNullable(cynefinDomain);
    }
    
    @Override
    public boolean isCrossProject() {
        return crossProject;
    }
    
    @Override
    public Optional<Instant> getTargetDate() {
        return Optional.ofNullable(targetDate);
    }
    
    /**
     * Creates a new WorkstreamRecord with an updated name.
     *
     * @param name the new name
     * @return a new WorkstreamRecord with the updated name
     */
    public WorkstreamRecord withName(String name) {
        return new WorkstreamRecord(
            id, name, description, owner, status, priority,
            createdAt, Instant.now(), organizationId, cynefinDomain,
            crossProject, targetDate
        );
    }
    
    /**
     * Creates a new WorkstreamRecord with an updated description.
     *
     * @param description the new description
     * @return a new WorkstreamRecord with the updated description
     */
    public WorkstreamRecord withDescription(String description) {
        return new WorkstreamRecord(
            id, name, description, owner, status, priority,
            createdAt, Instant.now(), organizationId, cynefinDomain,
            crossProject, targetDate
        );
    }
    
    /**
     * Creates a new WorkstreamRecord with an updated owner.
     *
     * @param owner the new owner
     * @return a new WorkstreamRecord with the updated owner
     */
    public WorkstreamRecord withOwner(String owner) {
        return new WorkstreamRecord(
            id, name, description, owner, status, priority,
            createdAt, Instant.now(), organizationId, cynefinDomain,
            crossProject, targetDate
        );
    }
    
    /**
     * Creates a new WorkstreamRecord with an updated status.
     *
     * @param status the new status
     * @return a new WorkstreamRecord with the updated status
     */
    public WorkstreamRecord withStatus(String status) {
        return new WorkstreamRecord(
            id, name, description, owner, status, priority,
            createdAt, Instant.now(), organizationId, cynefinDomain,
            crossProject, targetDate
        );
    }
    
    /**
     * Creates a new WorkstreamRecord with an updated priority.
     *
     * @param priority the new priority
     * @return a new WorkstreamRecord with the updated priority
     */
    public WorkstreamRecord withPriority(Priority priority) {
        return new WorkstreamRecord(
            id, name, description, owner, status, priority,
            createdAt, Instant.now(), organizationId, cynefinDomain,
            crossProject, targetDate
        );
    }
    
    /**
     * Creates a new WorkstreamRecord with an updated organization ID.
     *
     * @param organizationId the new organization ID
     * @return a new WorkstreamRecord with the updated organization ID
     */
    public WorkstreamRecord withOrganizationId(UUID organizationId) {
        return new WorkstreamRecord(
            id, name, description, owner, status, priority,
            createdAt, Instant.now(), organizationId, cynefinDomain,
            crossProject, targetDate
        );
    }
    
    /**
     * Creates a new WorkstreamRecord with an updated CYNEFIN domain.
     *
     * @param cynefinDomain the new CYNEFIN domain
     * @return a new WorkstreamRecord with the updated CYNEFIN domain
     */
    public WorkstreamRecord withCynefinDomain(CynefinDomain cynefinDomain) {
        return new WorkstreamRecord(
            id, name, description, owner, status, priority,
            createdAt, Instant.now(), organizationId, cynefinDomain,
            crossProject, targetDate
        );
    }
    
    /**
     * Creates a new WorkstreamRecord with an updated cross-project flag.
     *
     * @param crossProject the new cross-project flag
     * @return a new WorkstreamRecord with the updated cross-project flag
     */
    public WorkstreamRecord withCrossProject(boolean crossProject) {
        return new WorkstreamRecord(
            id, name, description, owner, status, priority,
            createdAt, Instant.now(), organizationId, cynefinDomain,
            crossProject, targetDate
        );
    }
    
    /**
     * Creates a new WorkstreamRecord with an updated target date.
     *
     * @param targetDate the new target date
     * @return a new WorkstreamRecord with the updated target date
     */
    public WorkstreamRecord withTargetDate(Instant targetDate) {
        return new WorkstreamRecord(
            id, name, description, owner, status, priority,
            createdAt, Instant.now(), organizationId, cynefinDomain,
            crossProject, targetDate
        );
    }
}