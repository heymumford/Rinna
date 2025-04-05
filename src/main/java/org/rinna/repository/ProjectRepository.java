/*
 * Domain repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import org.rinna.domain.Project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Project entities.
 */
public interface ProjectRepository {
    
    /**
     * Saves a project.
     *
     * @param project the project to save
     * @return the saved project
     */
    Project save(Project project);
    
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
}