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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages configuration settings for the Rinna CLI.
 */
public final class ConfigurationService {
    private static ConfigurationService instance;
    private final Properties properties;
    private final Path configFilePath;
    
    private static final String CONFIG_DIRNAME = ".rinna";
    private static final String CONFIG_FILENAME = "config.properties";
    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";
    
    // Configuration property keys
    private static final String KEY_SERVER_URL = "server.url";
    private static final String KEY_CURRENT_USER = "user.current";
    private static final String KEY_AUTH_TOKEN = "user.auth.token";
    private static final String KEY_DEFAULT_VERSION = "default.version";
    private static final String KEY_DEFAULT_BUG_ASSIGNEE = "default.bug.assignee";
    private static final String KEY_AUTO_ASSIGN_BUGS = "bug.auto.assign";
    private static final String KEY_DEFAULT_PRIORITY = "default.priority";
    private static final String KEY_CURRENT_PROJECT = "project.current";
    private static final String KEY_USE_REMOTE_SERVICES = "service.remote.enabled";
    
    /**
     * Private constructor for singleton pattern.
     */
    private ConfigurationService() {
        properties = new Properties();
        
        // Determine config file path
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, CONFIG_DIRNAME);
        
        // Create config directory if it doesn't exist
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectory(configDir);
            } catch (IOException e) {
                System.err.println("Error creating config directory: " + e.getMessage());
            }
        }
        
        configFilePath = configDir.resolve(CONFIG_FILENAME);
        
        // Load existing configuration if available
        if (Files.exists(configFilePath)) {
            try (java.io.InputStreamReader reader = new java.io.InputStreamReader(
                    java.nio.file.Files.newInputStream(configFilePath), java.nio.charset.StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                System.err.println("Error loading configuration: " + e.getMessage());
            }
        } else {
            // Set default values
            properties.setProperty(KEY_SERVER_URL, DEFAULT_SERVER_URL);
            properties.setProperty(KEY_DEFAULT_VERSION, "1.0");
            properties.setProperty(KEY_AUTO_ASSIGN_BUGS, "true");
        }
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance
     */
    public static synchronized ConfigurationService getInstance() {
        if (instance == null) {
            instance = new ConfigurationService();
        }
        return instance;
    }
    
    /**
     * Saves the current configuration to disk.
     */
    public void saveConfiguration() {
        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                java.nio.file.Files.newOutputStream(configFilePath), java.nio.charset.StandardCharsets.UTF_8)) {
            properties.store(writer, "Rinna CLI Configuration");
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
        }
    }
    
    /**
     * Gets the server URL.
     *
     * @return the server URL
     */
    public String getServerUrl() {
        return properties.getProperty(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }
    
    /**
     * Sets the server URL.
     *
     * @param url the server URL
     */
    public void setServerUrl(String url) {
        properties.setProperty(KEY_SERVER_URL, url);
        saveConfiguration();
    }
    
    /**
     * Gets the current user.
     *
     * @return the current user, or null if not set
     */
    public String getCurrentUser() {
        return properties.getProperty(KEY_CURRENT_USER);
    }
    
    /**
     * Sets the current user.
     *
     * @param username the username
     */
    public void setCurrentUser(String username) {
        properties.setProperty(KEY_CURRENT_USER, username);
        saveConfiguration();
    }
    
    /**
     * Gets the authentication token.
     *
     * @return the authentication token, or null if not set
     */
    public String getAuthToken() {
        return properties.getProperty(KEY_AUTH_TOKEN);
    }
    
    /**
     * Sets the authentication token.
     *
     * @param token the authentication token
     */
    public void setAuthToken(String token) {
        properties.setProperty(KEY_AUTH_TOKEN, token);
        saveConfiguration();
    }
    
    /**
     * Clears authentication data.
     */
    public void clearAuthentication() {
        properties.remove(KEY_CURRENT_USER);
        properties.remove(KEY_AUTH_TOKEN);
        saveConfiguration();
    }
    
    /**
     * Checks if a user is currently authenticated.
     *
     * @return true if user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return properties.containsKey(KEY_CURRENT_USER) && properties.containsKey(KEY_AUTH_TOKEN);
    }
    
    /**
     * Gets the default version for new items.
     *
     * @return the default version
     */
    public String getDefaultVersion() {
        return properties.getProperty(KEY_DEFAULT_VERSION, "1.0");
    }
    
    /**
     * Sets the default version for new items.
     *
     * @param version the default version
     */
    public void setDefaultVersion(String version) {
        properties.setProperty(KEY_DEFAULT_VERSION, version);
        saveConfiguration();
    }
    
    /**
     * Gets the default assignee for bugs.
     *
     * @return the default bug assignee, or null if not set
     */
    public String getDefaultBugAssignee() {
        return properties.getProperty(KEY_DEFAULT_BUG_ASSIGNEE);
    }
    
    /**
     * Sets the default assignee for bugs.
     *
     * @param assignee the default assignee
     */
    public void setDefaultBugAssignee(String assignee) {
        properties.setProperty(KEY_DEFAULT_BUG_ASSIGNEE, assignee);
        saveConfiguration();
    }
    
    /**
     * Checks if bugs should be automatically assigned.
     *
     * @return true if bugs should be automatically assigned
     */
    public boolean getAutoAssignBugs() {
        return Boolean.parseBoolean(properties.getProperty(KEY_AUTO_ASSIGN_BUGS, "true"));
    }
    
    /**
     * Sets whether bugs should be automatically assigned.
     *
     * @param autoAssign true if bugs should be automatically assigned
     */
    public void setAutoAssignBugs(boolean autoAssign) {
        properties.setProperty(KEY_AUTO_ASSIGN_BUGS, String.valueOf(autoAssign));
        saveConfiguration();
    }
    
    /**
     * Gets a configuration property.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found
     * @return the property value, or defaultValue if not found
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Sets a configuration property.
     *
     * @param key the property key
     * @param value the property value
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfiguration();
    }
    
    /**
     * Gets the current project.
     *
     * @return the current project name, or null if not set
     */
    public String getCurrentProject() {
        return properties.getProperty(KEY_CURRENT_PROJECT);
    }
    
    /**
     * Sets the current project.
     *
     * @param projectName the project name
     */
    public void setCurrentProject(String projectName) {
        properties.setProperty(KEY_CURRENT_PROJECT, projectName);
        saveConfiguration();
    }
    
    /**
     * Checks if remote services are available and should be used.
     * This method not only checks the configuration but attempts to verify
     * that the specified remote services are actually reachable.
     *
     * @return true if remote services should be used, false to use local services
     */
    public boolean isRemoteServicesAvailable() {
        // First check if they're enabled in configuration
        boolean enabled = Boolean.parseBoolean(
            properties.getProperty(KEY_USE_REMOTE_SERVICES, "false"));
        
        if (!enabled) {
            return false;
        }
        
        // Then try to reach the server
        String serverUrl = getServerUrl();
        try {
            java.net.URL url = new java.net.URL(serverUrl + "/health");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2000); // 2 seconds timeout
            connection.setReadTimeout(2000);
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            // If any exception occurs, return false
            return false;
        }
    }
    
    /**
     * Static utility version of isRemoteServicesAvailable.
     * This version is usable without having to first get an instance
     * of ConfigurationService.
     *
     * @return true if remote services should be used
     */
    public static boolean areRemoteServicesAvailable() {
        return getInstance().isRemoteServicesAvailable();
    }
    
    /**
     * Sets whether to use remote services.
     *
     * @param useRemoteServices true to use remote services, false to use local
     */
    public void setUseRemoteServices(boolean useRemoteServices) {
        properties.setProperty(KEY_USE_REMOTE_SERVICES, String.valueOf(useRemoteServices));
        saveConfiguration();
    }
}