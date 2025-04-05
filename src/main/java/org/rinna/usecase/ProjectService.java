/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import org.rinna.domain.Project;
import org.rinna.domain.WebhookConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for project-related operations.
 */
public interface ProjectService {
    
    /**
     * Creates a new project.
     *
     * @param key the project key
     * @param name the project name
     * @param description the project description
     * @return the created project
     */
    Project create(String key, String name, String description);
    
    /**
     * Updates an existing project.
     *
     * @param id the project ID
     * @param name the updated name
     * @param description the updated description
     * @param active the updated active status
     * @return the updated project
     */
    Project update(UUID id, String name, String description, boolean active);
    
    /**
     * Finds a project by ID.
     *
     * @param id the project ID
     * @return the project, if found
     */
    Optional<Project> findById(UUID id);
    
    /**
     * Finds a project by key.
     *
     * @param key the project key
     * @return the project, if found
     */
    Optional<Project> findByKey(String key);
    
    /**
     * Returns all projects.
     *
     * @return a list of all projects
     */
    List<Project> findAll();
    
    /**
     * Returns all active projects.
     *
     * @return a list of all active projects
     */
    List<Project> findAllActive();
    
    /**
     * Deletes a project by ID.
     *
     * @param id the project ID
     */
    void deleteById(UUID id);
    
    /**
     * Configures a webhook for a project.
     *
     * @param projectId the project ID
     * @param source the webhook source
     * @param secret the webhook secret
     * @param description the webhook description
     * @return the created webhook configuration
     */
    WebhookConfig configureWebhook(UUID projectId, String source, String secret, String description);
    
    /**
     * Returns all webhook configurations for a project.
     *
     * @param projectId the project ID
     * @return a list of webhook configurations
     */
    List<WebhookConfig> findWebhooksByProjectId(UUID projectId);
    
    /**
     * Deletes a webhook configuration.
     *
     * @param id the webhook configuration ID
     */
    void deleteWebhook(UUID id);
}