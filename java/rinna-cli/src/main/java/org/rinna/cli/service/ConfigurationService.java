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


/**
 * Interface for configuration services within the Rinna CLI.
 */
public interface ConfigurationService {
    
    /**
     * Gets the server URL.
     *
     * @return the server URL
     */
    String getServerUrl();
    
    /**
     * Gets the current user.
     *
     * @return the current user, or null if not set
     */
    String getCurrentUser();
    
    /**
     * Gets the authentication token.
     *
     * @return the authentication token, or null if not set
     */
    String getAuthToken();
    
    /**
     * Checks if a user is currently authenticated.
     *
     * @return true if user is authenticated, false otherwise
     */
    boolean isAuthenticated();
    
    /**
     * Gets the default version for new items.
     *
     * @return the default version
     */
    String getDefaultVersion();
    
    /**
     * Gets the default assignee for bugs.
     *
     * @return the default bug assignee, or null if not set
     */
    String getDefaultBugAssignee();
    
    /**
     * Checks if bugs should be automatically assigned.
     *
     * @return true if bugs should be automatically assigned
     */
    boolean getAutoAssignBugs();
    
    /**
     * Gets a configuration property.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found
     * @return the property value, or defaultValue if not found
     */
    String getProperty(String key, String defaultValue);
    
    /**
     * Gets the current project.
     *
     * @return the current project name, or null if not set
     */
    String getCurrentProject();
    
    /**
     * Checks if remote services are available and should be used.
     *
     * @return true if remote services should be used, false to use local services
     */
    boolean isRemoteServicesAvailable();
    
    /**
     * Static utility to get the default instance.
     * 
     * @return the default instance
     */
    static ConfigurationService getInstance() {
        return DefaultConfigurationService.getInstance();
    }
    
    /**
     * Static utility to check if remote services are available.
     * 
     * @return true if remote services should be used
     */
    static boolean areRemoteServicesAvailable() {
        return getInstance().isRemoteServicesAvailable();
    }
}