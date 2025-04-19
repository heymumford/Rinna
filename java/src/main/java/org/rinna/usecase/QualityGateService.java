/*
 * QualityGateService - Service interface for quality gate operations
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkflowState;

/**
 * Service interface for quality gate operations.
 * Quality gates enforce quality standards at various stages of the development workflow.
 */
public interface QualityGateService {

    /**
     * Creates a quality gate configuration for a project.
     * 
     * @param projectId the ID of the project
     * @param configuration the quality gate configuration
     * @return true if the configuration was created successfully
     */
    boolean createQualityGateConfiguration(String projectId, Map<String, Object> configuration);
    
    /**
     * Updates a quality gate configuration for a project.
     * 
     * @param projectId the ID of the project
     * @param configuration the updated quality gate configuration
     * @return true if the configuration was updated successfully
     * @throws IllegalArgumentException if the project does not exist
     */
    boolean updateQualityGateConfiguration(String projectId, Map<String, Object> configuration);
    
    /**
     * Gets the quality gate configuration for a project.
     * 
     * @param projectId the ID of the project
     * @return the quality gate configuration, or null if the project has no configuration
     */
    Map<String, Object> getQualityGateConfiguration(String projectId);
    
    /**
     * Validates a workflow transition against quality gates.
     * 
     * @param workItemId the ID of the work item
     * @param projectId the ID of the project
     * @param fromState the current state of the work item
     * @param toState the target state of the work item
     * @param checkResults the results of quality checks
     * @return true if the transition is valid, false otherwise
     */
    boolean validateTransition(UUID workItemId, String projectId, 
                               WorkflowState fromState, WorkflowState toState, 
                               Map<String, Boolean> checkResults);
    
    /**
     * Transitions a work item with quality gate validation.
     * 
     * @param workItemId the ID of the work item
     * @param projectId the ID of the project
     * @param toState the target state of the work item
     * @param checkResults the results of quality checks
     * @return the updated work item
     * @throws IllegalStateException if the transition fails quality gate validation
     */
    WorkItem transitionWithQualityGate(UUID workItemId, String projectId, 
                                   WorkflowState toState, Map<String, Boolean> checkResults);
    
    /**
     * Gets the required checks for a transition.
     * 
     * @param projectId the ID of the project
     * @param fromState the current state of the work item
     * @param toState the target state of the work item
     * @return the list of required check names
     */
    List<String> getRequiredChecks(String projectId, WorkflowState fromState, WorkflowState toState);
    
    /**
     * Bypasses quality gates for a transition with a reason.
     * 
     * @param workItemId the ID of the work item
     * @param projectId the ID of the project
     * @param toState the target state of the work item
     * @param reason the reason for bypassing quality gates
     * @param byUser the user who is bypassing the quality gates
     * @return the updated work item
     */
    WorkItem bypassQualityGate(UUID workItemId, String projectId, 
                           WorkflowState toState, String reason, String byUser);
    
    /**
     * Gets the quality gate history for a work item.
     * 
     * @param workItemId the ID of the work item
     * @return the list of quality gate history entries
     */
    List<Map<String, Object>> getQualityGateHistory(UUID workItemId);
    
    /**
     * Runs quality checks for a work item transition.
     * 
     * @param workItemId the ID of the work item
     * @param projectId the ID of the project
     * @param fromState the current state of the work item
     * @param toState the target state of the work item
     * @return the results of the quality checks
     */
    Map<String, Boolean> runQualityChecks(UUID workItemId, String projectId, 
                                      WorkflowState fromState, WorkflowState toState);
}