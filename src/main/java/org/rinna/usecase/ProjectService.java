/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.usecase;

import org.rinna.domain.Project;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing projects.
 */
public interface ProjectService {
    
    /**
     * Creates a new project.
     *
     * @param name the project name
     * @param description the project description
     * @return the created project ID
     */
    UUID createProject(String name, String description);
    
    /**
     * Creates a new project with additional parameters.
     *
     * @param name the project name
     * @param description the project description
     * @param owner the project owner
     * @return the created project ID
     */
    UUID createProject(String name, String description, String owner);
    
    /**
     * Creates a new category in a project.
     *
     * @param projectName the parent project name
     * @param categoryName the category name
     * @param description the category description
     * @return the created category ID
     */
    UUID createCategory(String projectName, String categoryName, String description);
    
    /**
     * Creates a new sub-project in a project.
     *
     * @param parentPath the parent project path
     * @param subProjectName the sub-project name
     * @param description the sub-project description
     * @return the created sub-project ID
     */
    UUID createSubProject(String parentPath, String subProjectName, String description);
    
    /**
     * Gets a project by its ID.
     *
     * @param projectId the project ID
     * @return the project, or null if not found
     */
    Project getProject(UUID projectId);
    
    /**
     * Gets a project by its name.
     *
     * @param projectName the project name
     * @return the project, or null if not found
     */
    Project getProjectByName(String projectName);
    
    /**
     * Gets all projects.
     *
     * @return the list of all projects
     */
    List<Project> getAllProjects();
    
    /**
     * Checks if a project exists by name.
     *
     * @param projectName the project name
     * @return true if the project exists, false otherwise
     */
    boolean projectExists(String projectName);
    
    /**
     * Updates a project.
     *
     * @param projectId the project ID
     * @param name the new name
     * @param description the new description
     * @return true if the project was updated, false otherwise
     */
    boolean updateProject(UUID projectId, String name, String description);
    
    /**
     * Deletes a project.
     *
     * @param projectId the project ID
     * @return true if the project was deleted, false otherwise
     */
    boolean deleteProject(UUID projectId);
}