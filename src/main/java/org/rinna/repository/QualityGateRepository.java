/*
 * QualityGateRepository - Repository interface for quality gate configurations
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for quality gate configurations.
 * Quality gates enforce quality standards at various stages of the development workflow.
 */
public interface QualityGateRepository {

    /**
     * Finds a quality gate configuration by project ID.
     *
     * @param projectId the ID of the project
     * @return an Optional containing the configuration if found, or empty if not found
     */
    Optional<Map<String, Object>> findByProjectId(String projectId);
    
    /**
     * Saves a quality gate configuration for a project.
     *
     * @param projectId the ID of the project
     * @param configuration the quality gate configuration
     * @return true if the configuration was saved successfully
     */
    boolean save(String projectId, Map<String, Object> configuration);
    
    /**
     * Deletes a quality gate configuration.
     *
     * @param projectId the ID of the project
     * @return true if the configuration was deleted, false if it didn't exist
     */
    boolean delete(String projectId);
    
    /**
     * Saves a quality gate history entry.
     *
     * @param workItemId the ID of the work item
     * @param historyEntry the history entry to save
     * @return the ID of the saved history entry
     */
    UUID saveHistoryEntry(UUID workItemId, Map<String, Object> historyEntry);
    
    /**
     * Finds quality gate history entries for a work item.
     *
     * @param workItemId the ID of the work item
     * @return the list of history entries
     */
    List<Map<String, Object>> findHistoryByWorkItemId(UUID workItemId);
}