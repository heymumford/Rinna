/*
 * DefaultQualityGateService - Default implementation of QualityGateService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkflowState;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.QualityGateRepository;
import org.rinna.usecase.QualityGateService;
import org.rinna.usecase.WorkflowService;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Default implementation of the QualityGateService interface.
 * This implementation enforces quality standards at various stages of the development workflow.
 */
public class DefaultQualityGateService implements QualityGateService {

    private final QualityGateRepository qualityGateRepository;
    private final ItemRepository itemRepository;
    private final WorkflowService workflowService;
    
    /**
     * Creates a new DefaultQualityGateService with the given repositories and services.
     *
     * @param qualityGateRepository the repository for quality gate configurations
     * @param itemRepository the repository for work items
     * @param workflowService the workflow service
     */
    public DefaultQualityGateService(QualityGateRepository qualityGateRepository, 
                                 ItemRepository itemRepository, 
                                 WorkflowService workflowService) {
        this.qualityGateRepository = qualityGateRepository;
        this.itemRepository = itemRepository;
        this.workflowService = workflowService;
    }
    
    @Override
    public boolean createQualityGateConfiguration(String projectId, Map<String, Object> configuration) {
        return qualityGateRepository.save(projectId, configuration);
    }
    
    @Override
    public boolean updateQualityGateConfiguration(String projectId, Map<String, Object> configuration) {
        // Check if the project has a configuration
        if (qualityGateRepository.findByProjectId(projectId).isEmpty()) {
            throw new IllegalArgumentException("Project does not have a quality gate configuration: " + projectId);
        }
        
        return qualityGateRepository.save(projectId, configuration);
    }
    
    @Override
    public Map<String, Object> getQualityGateConfiguration(String projectId) {
        Optional<Map<String, Object>> config = qualityGateRepository.findByProjectId(projectId);
        return config.orElse(null);
    }
    
    @Override
    public boolean validateTransition(UUID workItemId, String projectId, 
                                  WorkflowState fromState, WorkflowState toState, 
                                  Map<String, Boolean> checkResults) {
        // Get the required checks for this transition
        List<String> requiredChecks = getRequiredChecks(projectId, fromState, toState);
        
        // If there are no required checks, the transition is valid
        if (requiredChecks.isEmpty()) {
            return true;
        }
        
        // Validate that all required checks are present and passing
        for (String check : requiredChecks) {
            if (!checkResults.containsKey(check) || !checkResults.get(check)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public WorkItem transitionWithQualityGate(UUID workItemId, String projectId, 
                                          WorkflowState toState, Map<String, Boolean> checkResults) {
        // Get the work item
        Optional<WorkItem> optionalItem = itemRepository.findById(workItemId);
        if (optionalItem.isEmpty()) {
            throw new IllegalArgumentException("Work item not found: " + workItemId);
        }
        
        WorkItem workItem = optionalItem.get();
        WorkflowState fromState = workItem.getStatus();
        
        // Validate the transition
        boolean isValid = validateTransition(workItemId, projectId, fromState, toState, checkResults);
        
        if (!isValid) {
            throw new IllegalStateException("Transition failed quality gate validation");
        }
        
        // Perform the transition
        WorkItem updatedItem = workflowService.transition(workItemId, toState);
        
        // Record the quality gate history
        recordQualityGateHistory(workItemId, fromState, toState, checkResults, true, null, null);
        
        return updatedItem;
    }
    
    @Override
    public List<String> getRequiredChecks(String projectId, WorkflowState fromState, WorkflowState toState) {
        // Get the quality gate configuration for the project
        Optional<Map<String, Object>> optionalConfig = qualityGateRepository.findByProjectId(projectId);
        if (optionalConfig.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, Object> config = optionalConfig.get();
        
        // Get the gate for this transition
        String transitionKey = fromState + "_to_" + toState;
        if (!config.containsKey(transitionKey)) {
            return Collections.emptyList();
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> gate = (Map<String, Object>) config.get(transitionKey);
        
        // Get the required checks
        if (!gate.containsKey("requiredChecks")) {
            return Collections.emptyList();
        }
        
        @SuppressWarnings("unchecked")
        List<String> requiredChecks = (List<String>) gate.get("requiredChecks");
        
        return new ArrayList<>(requiredChecks);
    }
    
    @Override
    public WorkItem bypassQualityGate(UUID workItemId, String projectId, 
                                  WorkflowState toState, String reason, String byUser) {
        // Get the work item
        Optional<WorkItem> optionalItem = itemRepository.findById(workItemId);
        if (optionalItem.isEmpty()) {
            throw new IllegalArgumentException("Work item not found: " + workItemId);
        }
        
        WorkItem workItem = optionalItem.get();
        WorkflowState fromState = workItem.getStatus();
        
        // Perform the transition
        WorkItem updatedItem = workflowService.transition(workItemId, toState);
        
        // Record the bypass in the work item's metadata
        Map<String, String> metadata = new HashMap<>(
                workItem.getMetadata() != null ? workItem.getMetadata() : new HashMap<>());
        metadata.put("quality_gate_bypassed", "true");
        metadata.put("bypass_reason", reason);
        metadata.put("bypassed_by", byUser);
        metadata.put("bypass_date", LocalDateTime.now().toString());
        
        itemRepository.updateMetadata(workItemId, metadata);
        
        // Record the quality gate history with bypass
        recordQualityGateHistory(workItemId, fromState, toState, Collections.emptyMap(), false, reason, byUser);
        
        return updatedItem;
    }
    
    @Override
    public List<Map<String, Object>> getQualityGateHistory(UUID workItemId) {
        return qualityGateRepository.findHistoryByWorkItemId(workItemId);
    }
    
    @Override
    public Map<String, Boolean> runQualityChecks(UUID workItemId, String projectId, 
                                             WorkflowState fromState, WorkflowState toState) {
        List<String> requiredChecks = getRequiredChecks(projectId, fromState, toState);
        Map<String, Boolean> results = new HashMap<>();
        
        // Get the work item
        Optional<WorkItem> workItemOpt = itemRepository.findById(workItemId);
        if (workItemOpt.isEmpty()) {
            throw new IllegalArgumentException("Work item not found: " + workItemId);
        }
        
        WorkItem workItem = workItemOpt.get();
        
        // Run each required check
        for (String check : requiredChecks) {
            switch (check) {
                case "hasDescription":
                    // Check if the work item has a non-empty description
                    results.put(check, workItem.getDescription() != null && 
                                      !workItem.getDescription().trim().isEmpty());
                    break;
                    
                case "hasAssignee":
                    // Check if the work item has an assignee
                    results.put(check, workItem.getAssignee() != null && 
                                      !workItem.getAssignee().trim().isEmpty());
                    break;
                    
                case "hasTags":
                    // Check if the work item has tags in metadata
                    Map<String, String> metadata = workItem.getMetadata();
                    boolean hasTags = metadata != null && 
                                     metadata.containsKey("tags") && 
                                     !metadata.get("tags").trim().isEmpty();
                    results.put(check, hasTags);
                    break;
                    
                case "testsRequired":
                    // For transitions to IN_TEST and beyond, ensure test coverage is captured
                    boolean hasTests = false;
                    if (toState == WorkflowState.IN_TEST || 
                        toState == WorkflowState.DONE || 
                        toState == WorkflowState.VERIFIED) {
                        
                        Map<String, String> md = workItem.getMetadata();
                        hasTests = md != null && 
                                  (md.containsKey("test_coverage") || 
                                   md.containsKey("test_plan") || 
                                   md.containsKey("test_results"));
                    }
                    results.put(check, hasTests);
                    break;
                    
                case "reviewRequired":
                    // For transitions to DONE, ensure review was completed
                    boolean hasReview = false;
                    if (toState == WorkflowState.DONE || toState == WorkflowState.VERIFIED) {
                        Map<String, String> md = workItem.getMetadata();
                        hasReview = md != null && 
                                   (md.containsKey("review_completed") || 
                                    md.containsKey("review_by"));
                    }
                    results.put(check, hasReview);
                    break;
                    
                default:
                    // For any custom checks, delegate to the qualityGateRepository's validation logic
                    results.put(check, qualityGateRepository.validateCheck(workItemId, projectId, check));
                    break;
            }
        }
        
        return results;
    }
    
    /**
     * Records a quality gate history entry.
     *
     * @param workItemId the ID of the work item
     * @param fromState the source state
     * @param toState the target state
     * @param checkResults the check results
     * @param passed whether the quality gate passed
     * @param bypassReason the bypass reason, or null if not bypassed
     * @param bypassedBy the user who bypassed the quality gate, or null if not bypassed
     */
    private void recordQualityGateHistory(UUID workItemId, WorkflowState fromState, WorkflowState toState, 
                                         Map<String, Boolean> checkResults, boolean passed, 
                                         String bypassReason, String bypassedBy) {
        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("timestamp", LocalDateTime.now().toString());
        historyEntry.put("fromState", fromState.toString());
        historyEntry.put("toState", toState.toString());
        historyEntry.put("checkResults", new HashMap<>(checkResults));
        historyEntry.put("passed", passed);
        
        if (bypassReason != null) {
            historyEntry.put("bypassed", true);
            historyEntry.put("bypassReason", bypassReason);
            historyEntry.put("bypassedBy", bypassedBy);
        }
        
        qualityGateRepository.saveHistoryEntry(workItemId, historyEntry);
    }
}