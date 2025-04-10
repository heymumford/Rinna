/*
 * Domain entity record for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of the UnifiedWorkItem interface as an immutable record.
 * This provides a concrete implementation with all the properties needed for unified work management.
 */
public record UnifiedWorkItemRecord(
        UUID id,
        String title,
        String description,
        WorkItemType type,
        WorkflowState status,
        Priority priority,
        String assignee,
        Instant createdAt,
        Instant updatedAt,
        Optional<UUID> parentId,
        String projectKey,
        Optional<UUID> projectId,
        String visibility,
        boolean localOnly,
        OriginCategory category,
        CynefinDomain cynefinDomain,
        WorkParadigm workParadigm,
        Optional<Integer> cognitiveLoad,
        Optional<String> outcome,
        Set<UUID> dependencies,
        Set<UUID> relatedItems,
        Optional<UUID> releaseId,
        Map<String, String> metadata,
        Optional<Instant> dueDate,
        Optional<Double> estimatedEffort,
        Optional<Double> actualEffort
) implements UnifiedWorkItem {

    /**
     * Creates a new UnifiedWorkItemRecord with the required fields.
     * Optional fields will be initialized to empty.
     *
     * @param id The unique identifier
     * @param title The title of the work item
     * @param description The detailed description
     * @param type The work item type
     * @param status The current workflow state
     * @param priority The priority level
     * @param assignee The assigned user
     * @param projectKey The project key
     * @param category The origin category
     * @param cynefinDomain The CYNEFIN domain classification
     * @param workParadigm The work paradigm
     * @return A new UnifiedWorkItemRecord
     */
    public static UnifiedWorkItemRecord create(
            UUID id,
            String title,
            String description,
            WorkItemType type,
            WorkflowState status,
            Priority priority,
            String assignee,
            String projectKey,
            OriginCategory category,
            CynefinDomain cynefinDomain,
            WorkParadigm workParadigm
    ) {
        Instant now = Instant.now();
        
        return new UnifiedWorkItemRecord(
                id,
                title,
                description,
                type,
                status,
                priority,
                assignee,
                now,
                now,
                Optional.empty(),
                projectKey,
                Optional.empty(),
                "PUBLIC",
                true,
                category,
                cynefinDomain,
                workParadigm,
                Optional.empty(),
                Optional.empty(),
                Collections.emptySet(),
                Collections.emptySet(),
                Optional.empty(),
                Collections.emptyMap(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }
    
    /**
     * Creates a builder to construct a UnifiedWorkItemRecord with customized values.
     * 
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for constructing UnifiedWorkItemRecord instances.
     */
    public static class Builder {
        private UUID id;
        private String title;
        private String description;
        private WorkItemType type;
        private WorkflowState status;
        private Priority priority;
        private String assignee;
        private Instant createdAt;
        private Instant updatedAt;
        private Optional<UUID> parentId = Optional.empty();
        private String projectKey;
        private Optional<UUID> projectId = Optional.empty();
        private String visibility = "PUBLIC";
        private boolean localOnly = true;
        private OriginCategory category;
        private CynefinDomain cynefinDomain;
        private WorkParadigm workParadigm;
        private Optional<Integer> cognitiveLoad = Optional.empty();
        private Optional<String> outcome = Optional.empty();
        private Set<UUID> dependencies = Collections.emptySet();
        private Set<UUID> relatedItems = Collections.emptySet();
        private Optional<UUID> releaseId = Optional.empty();
        private Map<String, String> metadata = Collections.emptyMap();
        private Optional<Instant> dueDate = Optional.empty();
        private Optional<Double> estimatedEffort = Optional.empty();
        private Optional<Double> actualEffort = Optional.empty();
        
        /**
         * Sets the ID for the work item.
         */
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        /**
         * Sets the title for the work item.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * Sets the description for the work item.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the type for the work item.
         */
        public Builder type(WorkItemType type) {
            this.type = type;
            return this;
        }
        
        /**
         * Sets the status for the work item.
         */
        public Builder status(WorkflowState status) {
            this.status = status;
            return this;
        }
        
        /**
         * Sets the priority for the work item.
         */
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }
        
        /**
         * Sets the assignee for the work item.
         */
        public Builder assignee(String assignee) {
            this.assignee = assignee;
            return this;
        }
        
        /**
         * Sets the creation timestamp for the work item.
         */
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        /**
         * Sets the last update timestamp for the work item.
         */
        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        /**
         * Sets the parent ID for the work item.
         */
        public Builder parentId(UUID parentId) {
            this.parentId = Optional.ofNullable(parentId);
            return this;
        }
        
        /**
         * Sets the project key for the work item.
         */
        public Builder projectKey(String projectKey) {
            this.projectKey = projectKey;
            return this;
        }
        
        /**
         * Sets the project ID for the work item.
         */
        public Builder projectId(UUID projectId) {
            this.projectId = Optional.ofNullable(projectId);
            return this;
        }
        
        /**
         * Sets the visibility for the work item.
         */
        public Builder visibility(String visibility) {
            this.visibility = visibility;
            return this;
        }
        
        /**
         * Sets whether the work item is local only.
         */
        public Builder localOnly(boolean localOnly) {
            this.localOnly = localOnly;
            return this;
        }
        
        /**
         * Sets the origin category for the work item.
         */
        public Builder category(OriginCategory category) {
            this.category = category;
            return this;
        }
        
        /**
         * Sets the CYNEFIN domain for the work item.
         */
        public Builder cynefinDomain(CynefinDomain cynefinDomain) {
            this.cynefinDomain = cynefinDomain;
            return this;
        }
        
        /**
         * Sets the work paradigm for the work item.
         */
        public Builder workParadigm(WorkParadigm workParadigm) {
            this.workParadigm = workParadigm;
            return this;
        }
        
        /**
         * Sets the cognitive load for the work item.
         */
        public Builder cognitiveLoad(Integer cognitiveLoad) {
            this.cognitiveLoad = Optional.ofNullable(cognitiveLoad);
            return this;
        }
        
        /**
         * Sets the outcome for the work item.
         */
        public Builder outcome(String outcome) {
            this.outcome = Optional.ofNullable(outcome);
            return this;
        }
        
        /**
         * Sets the dependencies for the work item.
         */
        public Builder dependencies(Set<UUID> dependencies) {
            this.dependencies = dependencies != null ? dependencies : Collections.emptySet();
            return this;
        }
        
        /**
         * Sets the related items for the work item.
         */
        public Builder relatedItems(Set<UUID> relatedItems) {
            this.relatedItems = relatedItems != null ? relatedItems : Collections.emptySet();
            return this;
        }
        
        /**
         * Sets the release ID for the work item.
         */
        public Builder releaseId(UUID releaseId) {
            this.releaseId = Optional.ofNullable(releaseId);
            return this;
        }
        
        /**
         * Sets the metadata for the work item.
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata != null ? metadata : Collections.emptyMap();
            return this;
        }
        
        /**
         * Sets the due date for the work item.
         */
        public Builder dueDate(Instant dueDate) {
            this.dueDate = Optional.ofNullable(dueDate);
            return this;
        }
        
        /**
         * Sets the estimated effort for the work item.
         */
        public Builder estimatedEffort(Double estimatedEffort) {
            this.estimatedEffort = Optional.ofNullable(estimatedEffort);
            return this;
        }
        
        /**
         * Sets the actual effort for the work item.
         */
        public Builder actualEffort(Double actualEffort) {
            this.actualEffort = Optional.ofNullable(actualEffort);
            return this;
        }
        
        /**
         * Builds a new UnifiedWorkItemRecord with the configured values.
         * 
         * @return A new UnifiedWorkItemRecord
         */
        public UnifiedWorkItemRecord build() {
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            
            if (updatedAt == null) {
                updatedAt = createdAt;
            }
            
            return new UnifiedWorkItemRecord(
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
                    projectKey,
                    projectId,
                    visibility,
                    localOnly,
                    category,
                    cynefinDomain,
                    workParadigm,
                    cognitiveLoad,
                    outcome,
                    dependencies,
                    relatedItems,
                    releaseId,
                    metadata,
                    dueDate,
                    estimatedEffort,
                    actualEffort
            );
        }
    }
}