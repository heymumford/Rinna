/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A request to create a new organizational unit.
 */
public record OrganizationalUnitCreateRequest(
    String name,
    String description,
    OrganizationalUnitType type,
    UUID parentId,
    String owner,
    int cognitiveCapacity,
    List<String> members,
    boolean active,
    List<CynefinDomain> domainExpertise,
    List<WorkParadigm> workParadigms,
    List<String> tags
) {
    /**
     * Creates a new OrganizationalUnitCreateRequest with validation.
     */
    public OrganizationalUnitCreateRequest {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(owner, "Owner cannot be null");
        
        // Initialize empty lists for immutability
        members = members != null ? List.copyOf(members) : List.of();
        domainExpertise = domainExpertise != null ? List.copyOf(domainExpertise) : List.of();
        workParadigms = workParadigms != null ? List.copyOf(workParadigms) : List.of();
        tags = tags != null ? List.copyOf(tags) : List.of();
    }
    
    /**
     * Creates a builder for an OrganizationalUnitCreateRequest.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for OrganizationalUnitCreateRequest.
     */
    public static class Builder {
        private String name;
        private String description;
        private OrganizationalUnitType type = OrganizationalUnitType.TEAM;
        private UUID parentId;
        private String owner;
        private int cognitiveCapacity = 0;
        private List<String> members = new ArrayList<>();
        private boolean active = true;
        private List<CynefinDomain> domainExpertise = new ArrayList<>();
        private List<WorkParadigm> workParadigms = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
        
        /**
         * Sets the name.
         *
         * @param name the name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Sets the description.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the type.
         *
         * @param type the type
         * @return this builder
         */
        public Builder type(OrganizationalUnitType type) {
            this.type = type;
            return this;
        }
        
        /**
         * Sets the type using the type's name (case-insensitive).
         *
         * @param typeName the type name
         * @return this builder
         */
        public Builder type(String typeName) {
            this.type = OrganizationalUnitType.fromName(typeName);
            return this;
        }
        
        /**
         * Sets the parent ID.
         *
         * @param parentId the parent ID
         * @return this builder
         */
        public Builder parentId(UUID parentId) {
            this.parentId = parentId;
            return this;
        }
        
        /**
         * Sets the owner.
         *
         * @param owner the owner
         * @return this builder
         */
        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }
        
        /**
         * Sets the cognitive capacity.
         *
         * @param cognitiveCapacity the cognitive capacity
         * @return this builder
         */
        public Builder cognitiveCapacity(int cognitiveCapacity) {
            this.cognitiveCapacity = cognitiveCapacity;
            return this;
        }
        
        /**
         * Sets the members.
         *
         * @param members the members
         * @return this builder
         */
        public Builder members(List<String> members) {
            this.members = new ArrayList<>(members);
            return this;
        }
        
        /**
         * Adds a member.
         *
         * @param member the member to add
         * @return this builder
         */
        public Builder addMember(String member) {
            this.members.add(member);
            return this;
        }
        
        /**
         * Sets whether the organizational unit is active.
         *
         * @param active whether the organizational unit is active
         * @return this builder
         */
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        /**
         * Sets the domain expertise.
         *
         * @param domainExpertise the domain expertise
         * @return this builder
         */
        public Builder domainExpertise(List<CynefinDomain> domainExpertise) {
            this.domainExpertise = new ArrayList<>(domainExpertise);
            return this;
        }
        
        /**
         * Adds a domain expertise.
         *
         * @param domain the domain to add
         * @return this builder
         */
        public Builder addDomainExpertise(CynefinDomain domain) {
            this.domainExpertise.add(domain);
            return this;
        }
        
        /**
         * Sets the work paradigms.
         *
         * @param workParadigms the work paradigms
         * @return this builder
         */
        public Builder workParadigms(List<WorkParadigm> workParadigms) {
            this.workParadigms = new ArrayList<>(workParadigms);
            return this;
        }
        
        /**
         * Adds a work paradigm.
         *
         * @param paradigm the paradigm to add
         * @return this builder
         */
        public Builder addWorkParadigm(WorkParadigm paradigm) {
            this.workParadigms.add(paradigm);
            return this;
        }
        
        /**
         * Sets the tags.
         *
         * @param tags the tags
         * @return this builder
         */
        public Builder tags(List<String> tags) {
            this.tags = new ArrayList<>(tags);
            return this;
        }
        
        /**
         * Adds a tag.
         *
         * @param tag the tag to add
         * @return this builder
         */
        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }
        
        /**
         * Builds a new OrganizationalUnitCreateRequest.
         *
         * @return a new OrganizationalUnitCreateRequest
         */
        public OrganizationalUnitCreateRequest build() {
            return new OrganizationalUnitCreateRequest(
                name,
                description,
                type,
                parentId,
                owner,
                cognitiveCapacity,
                members,
                active,
                domainExpertise,
                workParadigms,
                tags
            );
        }
    }
}