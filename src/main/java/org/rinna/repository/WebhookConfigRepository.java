/*
 * Domain repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import org.rinna.domain.WebhookConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for WebhookConfig entities.
 */
public interface WebhookConfigRepository {
    
    /**
     * Saves a webhook configuration.
     *
     * @param config the configuration to save
     * @return the saved configuration
     */
    WebhookConfig save(WebhookConfig config);
    
    /**
     * Finds a webhook configuration by ID.
     *
     * @param id the configuration ID
     * @return the configuration, if found
     */
    Optional<WebhookConfig> findById(UUID id);
    
    /**
     * Finds webhook configurations by project ID and source.
     *
     * @param projectId the project ID
     * @param source the webhook source
     * @return a list of matching configurations
     */
    List<WebhookConfig> findByProjectIdAndSource(UUID projectId, String source);
    
    /**
     * Returns all webhook configurations for a project.
     *
     * @param projectId the project ID
     * @return a list of configurations for the project
     */
    List<WebhookConfig> findByProjectId(UUID projectId);
    
    /**
     * Returns all webhook configurations.
     *
     * @return a list of all configurations
     */
    List<WebhookConfig> findAll();
    
    /**
     * Returns all active webhook configurations.
     *
     * @return a list of all active configurations
     */
    List<WebhookConfig> findAllActive();
    
    /**
     * Deletes a webhook configuration by ID.
     *
     * @param id the configuration ID
     */
    void deleteById(UUID id);
}