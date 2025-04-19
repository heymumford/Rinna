/*
 * TemplateRepository - Repository interface for work item templates
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for work item templates.
 * A template defines the structure and default values for creating work items.
 */
public interface TemplateRepository {

    /**
     * Finds a template by name.
     *
     * @param name the name of the template
     * @return an Optional containing the template definition if found, or empty if not found
     */
    Optional<Map<String, Object>> findByName(String name);
    
    /**
     * Saves a template.
     *
     * @param name the name of the template
     * @param definition the template definition
     * @return true if the template was saved successfully
     */
    boolean save(String name, Map<String, Object> definition);
    
    /**
     * Deletes a template by name.
     *
     * @param name the name of the template to delete
     * @return true if the template was deleted, false if it didn't exist
     */
    boolean delete(String name);
    
    /**
     * Finds all templates.
     *
     * @return a list of all template names
     */
    List<String> findAllNames();
    
    /**
     * Checks if a template with the given name exists.
     *
     * @param name the name to check
     * @return true if a template with the given name exists
     */
    boolean exists(String name);
}