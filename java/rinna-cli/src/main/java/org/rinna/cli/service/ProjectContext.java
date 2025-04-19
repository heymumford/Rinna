/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the current project context for CLI operations.
 */
public final class ProjectContext {
    private static ProjectContext instance;
    private final ConcurrentHashMap<String, String> userProjects;
    private final ConcurrentHashMap<String, String> projectKeys;
    private final ConcurrentHashMap<String, List<String>> projectMembers;
    
    /**
     * Private constructor for singleton pattern.
     */
    private ProjectContext() {
        userProjects = new ConcurrentHashMap<>();
        projectKeys = new ConcurrentHashMap<>();
        projectMembers = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance
     */
    public static synchronized ProjectContext getInstance() {
        if (instance == null) {
            instance = new ProjectContext();
        }
        return instance;
    }
    
    /**
     * Gets the current project for a user.
     *
     * @return the current project name, or null if no project is active
     */
    public String getCurrentProject() {
        String currentUser = ConfigurationService.getInstance().getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        return userProjects.get(currentUser);
    }
    
    /**
     * Sets the current project for the active user.
     *
     * @param projectName the project name
     */
    public void setCurrentProject(String projectName) {
        String currentUser = ConfigurationService.getInstance().getCurrentUser();
        if (currentUser != null) {
            userProjects.put(currentUser, projectName);
        }
    }
    
    /**
     * Gets the key for the current project.
     *
     * @return the project key, or null if no project is active
     */
    public String getCurrentProjectKey() {
        String currentProject = getCurrentProject();
        if (currentProject == null) {
            return null;
        }
        return projectKeys.get(currentProject);
    }
    
    /**
     * Sets the key for the current project.
     *
     * @param projectKey the project key
     */
    public void setCurrentProjectKey(String projectKey) {
        String currentProject = getCurrentProject();
        if (currentProject != null) {
            projectKeys.put(currentProject, projectKey);
        }
    }
    
    /**
     * Gets the key for a specific project.
     *
     * @param projectName the project name
     * @return the project key, or null if not found
     */
    public String getProjectKey(String projectName) {
        return projectKeys.get(projectName);
    }
    
    /**
     * Checks if a project is currently active.
     *
     * @return true if a project is active, false otherwise
     */
    public boolean isProjectActive() {
        return getCurrentProject() != null;
    }
    
    /**
     * Adds a member to a project.
     *
     * @param projectName the project name
     * @param username    the username to add
     */
    public void addProjectMember(String projectName, String username) {
        projectMembers.computeIfAbsent(projectName, k -> new ArrayList<>()).add(username);
    }
    
    /**
     * Gets the members of a project.
     *
     * @param projectName the project name
     * @return the list of project members
     */
    public List<String> getProjectMembers(String projectName) {
        return projectMembers.getOrDefault(projectName, new ArrayList<>());
    }
    
    /**
     * Checks if a user is a member of a project.
     *
     * @param projectName the project name
     * @param username    the username
     * @return true if the user is a member of the project, false otherwise
     */
    public boolean isProjectMember(String projectName, String username) {
        List<String> members = projectMembers.get(projectName);
        return members != null && members.contains(username);
    }
    
    /**
     * Adds a new project with its key.
     *
     * @param projectName the project name
     * @param projectKey  the project key
     */
    public void addProject(String projectName, String projectKey) {
        projectKeys.put(projectName, projectKey);
    }
    
    /**
     * Clears the project context for a user when they log out.
     */
    public void clearContext() {
        String currentUser = ConfigurationService.getInstance().getCurrentUser();
        if (currentUser != null) {
            userProjects.remove(currentUser);
        }
    }
}