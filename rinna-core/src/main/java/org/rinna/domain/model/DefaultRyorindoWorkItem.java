/*
 * Domain entity for the Rinna workflow management system - Ryorin-do v0.2 implementation
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of the RyorindoWorkItemEnhancer interface.
 * Demonstrates the Decorator pattern to add Ryorin-do v0.2 functionality
 * to existing WorkItem objects.
 */
public class DefaultRyorindoWorkItem implements RyorindoWorkItemEnhancer {
    private final WorkItem workItem;
    private final Map<String, Object> metadata;
    private final List<UUID> dependencies;
    private final List<UUID> relatedItems;
    private final List<String> keyResults;
    private final List<String> aiRecommendations;
    private final List<String> knowledgeLinks;
    private final List<String> attachments;
    private final List<UUID> workstreamIds;
    private final List<String> assignees;
    
    /**
     * Constructs a new DefaultRyorindoWorkItem around the given WorkItem.
     * 
     * @param workItem the WorkItem to enhance
     */
    public DefaultRyorindoWorkItem(WorkItem workItem) {
        this.workItem = workItem;
        this.metadata = new HashMap<>();
        this.dependencies = new ArrayList<>();
        this.relatedItems = new ArrayList<>();
        this.keyResults = new ArrayList<>();
        this.aiRecommendations = new ArrayList<>();
        this.knowledgeLinks = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.workstreamIds = new ArrayList<>();
        
        // Initialize assignees with the basic workItem assignee
        this.assignees = new ArrayList<>();
        if (workItem.getAssignee() != null) {
            this.assignees.add(workItem.getAssignee());
        }
    }
    
    @Override
    public WorkItem getWorkItem() {
        return workItem;
    }
    
    /**
     * Sets the CYNEFIN domain for this work item.
     * 
     * @param domain the CYNEFIN domain
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem setCynefinDomain(CynefinDomain domain) {
        metadata.put("cynefinDomain", domain);
        return this;
    }
    
    @Override
    public Optional<CynefinDomain> getCynefinDomain() {
        return Optional.ofNullable((CynefinDomain) metadata.get("cynefinDomain"));
    }
    
    /**
     * Sets the work paradigm for this work item.
     * 
     * @param paradigm the work paradigm
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem setWorkParadigm(WorkParadigm paradigm) {
        metadata.put("workParadigm", paradigm);
        return this;
    }
    
    @Override
    public Optional<WorkParadigm> getWorkParadigm() {
        return Optional.ofNullable((WorkParadigm) metadata.get("workParadigm"));
    }
    
    /**
     * Adds an assignee to this work item.
     * 
     * @param assignee the assignee to add
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem addAssignee(String assignee) {
        if (!assignees.contains(assignee)) {
            assignees.add(assignee);
        }
        return this;
    }
    
    /**
     * Removes an assignee from this work item.
     * 
     * @param assignee the assignee to remove
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem removeAssignee(String assignee) {
        assignees.remove(assignee);
        return this;
    }
    
    @Override
    public List<String> getAssignees() {
        return Collections.unmodifiableList(assignees);
    }
    
    /**
     * Sets the due date for this work item.
     * 
     * @param dueDate the due date
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem setDueDate(Instant dueDate) {
        metadata.put("dueDate", dueDate);
        return this;
    }
    
    @Override
    public Optional<Instant> getDueDate() {
        return Optional.ofNullable((Instant) metadata.get("dueDate"));
    }
    
    /**
     * Sets the estimated effort for this work item.
     * 
     * @param effort the estimated effort
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem setEstimatedEffort(Double effort) {
        metadata.put("estimatedEffort", effort);
        return this;
    }
    
    @Override
    public Optional<Double> getEstimatedEffort() {
        return Optional.ofNullable((Double) metadata.get("estimatedEffort"));
    }
    
    /**
     * Sets the actual effort for this work item.
     * 
     * @param effort the actual effort
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem setActualEffort(Double effort) {
        metadata.put("actualEffort", effort);
        return this;
    }
    
    @Override
    public Optional<Double> getActualEffort() {
        return Optional.ofNullable((Double) metadata.get("actualEffort"));
    }
    
    /**
     * Sets the desired outcome for this work item.
     * 
     * @param outcome the desired outcome
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem setOutcome(String outcome) {
        metadata.put("outcome", outcome);
        return this;
    }
    
    @Override
    public Optional<String> getOutcome() {
        return Optional.ofNullable((String) metadata.get("outcome"));
    }
    
    /**
     * Adds a key result to this work item.
     * 
     * @param keyResult the key result to add
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem addKeyResult(String keyResult) {
        keyResults.add(keyResult);
        return this;
    }
    
    @Override
    public List<String> getKeyResults() {
        return Collections.unmodifiableList(keyResults);
    }
    
    /**
     * Adds a dependency to this work item.
     * 
     * @param dependencyId the ID of the dependency to add
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem addDependency(UUID dependencyId) {
        dependencies.add(dependencyId);
        return this;
    }
    
    @Override
    public List<UUID> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }
    
    /**
     * Adds a related item to this work item.
     * 
     * @param relatedId the ID of the related item to add
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem addRelatedItem(UUID relatedId) {
        relatedItems.add(relatedId);
        return this;
    }
    
    @Override
    public List<UUID> getRelatedItems() {
        return Collections.unmodifiableList(relatedItems);
    }
    
    /**
     * Sets the cognitive load assessment for this work item.
     * 
     * @param load the cognitive load (1-10)
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem setCognitiveLoadAssessment(Integer load) {
        if (load < 1 || load > 10) {
            throw new IllegalArgumentException("Cognitive load must be between 1 and 10");
        }
        metadata.put("cognitiveLoad", load);
        return this;
    }
    
    @Override
    public Optional<Integer> getCognitiveLoadAssessment() {
        return Optional.ofNullable((Integer) metadata.get("cognitiveLoad"));
    }
    
    /**
     * Adds an AI recommendation to this work item.
     * 
     * @param recommendation the recommendation to add
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem addAiRecommendation(String recommendation) {
        aiRecommendations.add(recommendation);
        return this;
    }
    
    @Override
    public List<String> getAiRecommendations() {
        return Collections.unmodifiableList(aiRecommendations);
    }
    
    /**
     * Adds a knowledge link to this work item.
     * 
     * @param link the knowledge link to add
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem addKnowledgeLink(String link) {
        knowledgeLinks.add(link);
        return this;
    }
    
    @Override
    public List<String> getKnowledgeLinks() {
        return Collections.unmodifiableList(knowledgeLinks);
    }
    
    /**
     * Adds an attachment to this work item.
     * 
     * @param attachment the attachment reference to add
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem addAttachment(String attachment) {
        attachments.add(attachment);
        return this;
    }
    
    @Override
    public List<String> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }
    
    /**
     * Adds a workstream ID to this work item.
     * 
     * @param workstreamId the workstream ID to add
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem addWorkstream(UUID workstreamId) {
        workstreamIds.add(workstreamId);
        return this;
    }
    
    @Override
    public List<UUID> getWorkstreamIds() {
        return Collections.unmodifiableList(workstreamIds);
    }
    
    /**
     * Sets the allocation percentage for this work item.
     * 
     * @param allocation the allocation percentage (0-100)
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem setAllocation(Integer allocation) {
        if (allocation < 0 || allocation > 100) {
            throw new IllegalArgumentException("Allocation must be between 0 and 100");
        }
        metadata.put("allocation", allocation);
        return this;
    }
    
    @Override
    public Optional<Integer> getAllocation() {
        return Optional.ofNullable((Integer) metadata.get("allocation"));
    }
    
    /**
     * Sets the category of this work item.
     * 
     * @param category the category
     * @return this DefaultRyorindoWorkItem for method chaining
     */
    public DefaultRyorindoWorkItem setCategory(String category) {
        metadata.put("category", category);
        return this;
    }
    
    @Override
    public Optional<String> getCategory() {
        return Optional.ofNullable((String) metadata.get("category"));
    }
}