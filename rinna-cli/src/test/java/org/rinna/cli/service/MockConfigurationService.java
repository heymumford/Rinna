package org.rinna.cli.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock ConfigurationService for testing.
 * Since we can't extend the final ConfigurationService, this implements the same methods.
 */
public class MockConfigurationService {
    private Map<String, String> configMap = new HashMap<>();
    
    // Configuration property keys - copied from ConfigurationService
    private static final String KEY_SERVER_URL = "server.url";
    private static final String KEY_CURRENT_USER = "user.current";
    private static final String KEY_AUTH_TOKEN = "user.auth.token";
    private static final String KEY_DEFAULT_VERSION = "default.version";
    private static final String KEY_DEFAULT_BUG_ASSIGNEE = "default.bug.assignee";
    private static final String KEY_AUTO_ASSIGN_BUGS = "bug.auto.assign";
    private static final String KEY_DEFAULT_PRIORITY = "default.priority";
    private static final String KEY_CURRENT_PROJECT = "project.current";
    
    /**
     * Gets the current user.
     * 
     * @return the current user
     */
    public String getCurrentUser() {
        return configMap.get(KEY_CURRENT_USER);
    }
    
    /**
     * Sets the current user.
     * 
     * @param currentUser the current user
     */
    public void setCurrentUser(String currentUser) {
        configMap.put(KEY_CURRENT_USER, currentUser);
    }
    
    /**
     * Gets the current project.
     * 
     * @return the current project
     */
    public String getCurrentProject() {
        return configMap.get(KEY_CURRENT_PROJECT);
    }
    
    /**
     * Sets the current project.
     * 
     * @param currentProject the current project
     */
    public void setCurrentProject(String currentProject) {
        configMap.put(KEY_CURRENT_PROJECT, currentProject);
    }
    
    /**
     * Gets a property.
     * 
     * @param key the property key
     * @param defaultValue the default value
     * @return the property value
     */
    public String getProperty(String key, String defaultValue) {
        return configMap.getOrDefault(key, defaultValue);
    }
    
    /**
     * Sets a property.
     * 
     * @param key the property key
     * @param value the property value
     */
    public void setProperty(String key, String value) {
        configMap.put(key, value);
    }
    
    /**
     * Method to get the authentication token.
     *
     * @return the authentication token or null
     */
    public String getAuthToken() {
        return configMap.get(KEY_AUTH_TOKEN);
    }
    
    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated
     */
    public boolean isAuthenticated() {
        return configMap.containsKey(KEY_CURRENT_USER) && configMap.containsKey(KEY_AUTH_TOKEN);
    }
    
    /**
     * Get the server URL.
     *
     * @return the server URL or default
     */
    public String getServerUrl() {
        return configMap.getOrDefault(KEY_SERVER_URL, "http://localhost:8080");
    }
}