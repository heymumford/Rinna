/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable implementation of the OrganizationalUnit interface.
 */
public record OrganizationalUnitRecord(
    UUID id,
    String name,
    String description,
    OrganizationalUnitType type,
    UUID parentId,
    String owner,
    Instant createdAt,
    Instant updatedAt,
    int cognitiveCapacity,
    int currentCognitiveLoad,
    List<String> members,
    boolean active,
    List<CynefinDomain> domainExpertise,
    List<WorkParadigm> workParadigms,
    List<String> tags
) implements OrganizationalUnit {
    
    /**
     * Creates a new OrganizationalUnitRecord with validation.
     */
    public OrganizationalUnitRecord {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(owner, "Owner cannot be null");
        Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        Objects.requireNonNull(updatedAt, "Updated timestamp cannot be null");
        
        // Initialize empty lists for immutability
        members = members != null ? List.copyOf(members) : List.of();
        domainExpertise = domainExpertise != null ? List.copyOf(domainExpertise) : List.of();
        workParadigms = workParadigms != null ? List.copyOf(workParadigms) : List.of();
        tags = tags != null ? List.copyOf(tags) : List.of();
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
    public OrganizationalUnitType getType() {
        return type;
    }
    
    @Override
    public Optional<UUID> getParentId() {
        return Optional.ofNullable(parentId);
    }
    
    @Override
    public String getOwner() {
        return owner;
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
    public int getCognitiveCapacity() {
        return cognitiveCapacity;
    }
    
    @Override
    public int getCurrentCognitiveLoad() {
        return currentCognitiveLoad;
    }
    
    @Override
    public List<String> getMembers() {
        return members;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public List<CynefinDomain> getDomainExpertise() {
        return domainExpertise;
    }
    
    @Override
    public List<WorkParadigm> getWorkParadigms() {
        return workParadigms;
    }
    
    @Override
    public List<String> getTags() {
        return tags;
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with an updated name.
     *
     * @param name the new name
     * @return a new OrganizationalUnitRecord with the updated name
     */
    public OrganizationalUnitRecord withName(String name) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with an updated description.
     *
     * @param description the new description
     * @return a new OrganizationalUnitRecord with the updated description
     */
    public OrganizationalUnitRecord withDescription(String description) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with an updated type.
     *
     * @param type the new type
     * @return a new OrganizationalUnitRecord with the updated type
     */
    public OrganizationalUnitRecord withType(OrganizationalUnitType type) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with an updated parent ID.
     *
     * @param parentId the new parent ID
     * @return a new OrganizationalUnitRecord with the updated parent ID
     */
    public OrganizationalUnitRecord withParentId(UUID parentId) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with an updated owner.
     *
     * @param owner the new owner
     * @return a new OrganizationalUnitRecord with the updated owner
     */
    public OrganizationalUnitRecord withOwner(String owner) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with an updated cognitive capacity.
     *
     * @param cognitiveCapacity the new cognitive capacity
     * @return a new OrganizationalUnitRecord with the updated cognitive capacity
     */
    public OrganizationalUnitRecord withCognitiveCapacity(int cognitiveCapacity) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with an updated current cognitive load.
     *
     * @param currentCognitiveLoad the new current cognitive load
     * @return a new OrganizationalUnitRecord with the updated current cognitive load
     */
    public OrganizationalUnitRecord withCurrentCognitiveLoad(int currentCognitiveLoad) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with updated members.
     *
     * @param members the new members
     * @return a new OrganizationalUnitRecord with the updated members
     */
    public OrganizationalUnitRecord withMembers(List<String> members) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with a new member added.
     *
     * @param member the new member to add
     * @return a new OrganizationalUnitRecord with the new member added
     */
    public OrganizationalUnitRecord withAddedMember(String member) {
        List<String> newMembers = new ArrayList<>(members);
        newMembers.add(member);
        return withMembers(newMembers);
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with a member removed.
     *
     * @param member the member to remove
     * @return a new OrganizationalUnitRecord with the member removed
     */
    public OrganizationalUnitRecord withRemovedMember(String member) {
        List<String> newMembers = new ArrayList<>(members);
        newMembers.remove(member);
        return withMembers(newMembers);
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with updated active status.
     *
     * @param active the new active status
     * @return a new OrganizationalUnitRecord with the updated active status
     */
    public OrganizationalUnitRecord withActive(boolean active) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with updated domain expertise.
     *
     * @param domainExpertise the new domain expertise
     * @return a new OrganizationalUnitRecord with the updated domain expertise
     */
    public OrganizationalUnitRecord withDomainExpertise(List<CynefinDomain> domainExpertise) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with a new domain expertise added.
     *
     * @param domain the new domain expertise to add
     * @return a new OrganizationalUnitRecord with the new domain expertise added
     */
    public OrganizationalUnitRecord withAddedDomainExpertise(CynefinDomain domain) {
        List<CynefinDomain> newDomains = new ArrayList<>(domainExpertise);
        if (!newDomains.contains(domain)) {
            newDomains.add(domain);
        }
        return withDomainExpertise(newDomains);
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with updated work paradigms.
     *
     * @param workParadigms the new work paradigms
     * @return a new OrganizationalUnitRecord with the updated work paradigms
     */
    public OrganizationalUnitRecord withWorkParadigms(List<WorkParadigm> workParadigms) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with a new work paradigm added.
     *
     * @param paradigm the new work paradigm to add
     * @return a new OrganizationalUnitRecord with the new work paradigm added
     */
    public OrganizationalUnitRecord withAddedWorkParadigm(WorkParadigm paradigm) {
        List<WorkParadigm> newParadigms = new ArrayList<>(workParadigms);
        if (!newParadigms.contains(paradigm)) {
            newParadigms.add(paradigm);
        }
        return withWorkParadigms(newParadigms);
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with updated tags.
     *
     * @param tags the new tags
     * @return a new OrganizationalUnitRecord with the updated tags
     */
    public OrganizationalUnitRecord withTags(List<String> tags) {
        return new OrganizationalUnitRecord(
            id, name, description, type, parentId, owner,
            createdAt, Instant.now(), cognitiveCapacity, currentCognitiveLoad,
            members, active, domainExpertise, workParadigms, tags
        );
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with a new tag added.
     *
     * @param tag the new tag to add
     * @return a new OrganizationalUnitRecord with the new tag added
     */
    public OrganizationalUnitRecord withAddedTag(String tag) {
        List<String> newTags = new ArrayList<>(tags);
        if (!newTags.contains(tag)) {
            newTags.add(tag);
        }
        return withTags(newTags);
    }
    
    /**
     * Creates a new OrganizationalUnitRecord with a tag removed.
     *
     * @param tag the tag to remove
     * @return a new OrganizationalUnitRecord with the tag removed
     */
    public OrganizationalUnitRecord withRemovedTag(String tag) {
        List<String> newTags = new ArrayList<>(tags);
        newTags.remove(tag);
        return withTags(newTags);
    }
}