/*
 * Authentication service for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.security;

import org.rinna.cli.config.SecurityConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

/**
 * Service for authenticating CLI users and managing security tokens.
 */
public final class AuthenticationService {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.rinna";
    private static final String AUTH_FILE = CONFIG_DIR + "/auth.properties";
    private static final Duration TOKEN_VALIDITY = Duration.ofDays(30);
    
    private Properties authProperties;
    private boolean initialized = false;
    
    /**
     * Initialize the authentication service.
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        // Create config directory if it doesn't exist
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        // Load auth properties
        authProperties = new Properties();
        File authFile = new File(AUTH_FILE);
        
        if (authFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    Files.newInputStream(authFile.toPath()), StandardCharsets.UTF_8)) {
                authProperties.load(reader);
            } catch (IOException e) {
                System.err.println("Error loading authentication data: " + e.getMessage());
            }
        }
        
        initialized = true;
    }
    
    /**
     * Login a user with username and password.
     * For demonstration purposes, this uses a simplified authentication method.
     * In a real implementation, this would verify credentials against a secure store.
     *
     * @param username the username
     * @param password the password
     * @return true if login successful
     */
    public boolean login(String username, String password) {
        initialize();
        
        // For demonstration purposes, this is a simplified authentication
        // In a real implementation, this would verify against secure credentials
        if (isValidCredentials(username, password)) {
            // Generate and store token
            String token = SecurityConfig.generateAuthToken();
            
            // Store token in both places (for backward compatibility)
            // In the auth properties
            authProperties.setProperty("current.user", username);
            authProperties.setProperty("current.token", token);
            authProperties.setProperty("current.token.expiry", 
                                      Instant.now().plus(TOKEN_VALIDITY).toString());
            authProperties.setProperty("user." + username + ".role", "user");
            saveProperties();
            
            // And in the security config
            SecurityConfig config = SecurityConfig.getInstance();
            config.storeAuthToken(username, token);
            config.setAdminStatus("admin".equals(username) || isCurrentUserAdmin());
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Promote a user to admin role.
     * In a real implementation, this would require proper authorization.
     *
     * @param username the username to promote
     * @return true if promotion successful
     */
    public boolean promoteToAdmin(String username) {
        initialize();
        
        if (!userExists(username)) {
            return false;
        }
        
        authProperties.setProperty("user." + username + ".role", "admin");
        saveProperties();
        return true;
    }
    
    /**
     * Check if a user exists.
     * 
     * @param username the username to check
     * @return true if the user exists
     */
    public boolean userExists(String username) {
        initialize();
        return authProperties.containsKey("user." + username + ".role");
    }
    
    /**
     * Get the current authenticated user.
     *
     * @return the username of the current user, or null if not authenticated
     */
    public String getCurrentUser() {
        initialize();
        
        // First check the security config
        SecurityConfig config = SecurityConfig.getInstance();
        String configUser = config.getCurrentUser();
        if (configUser != null) {
            return configUser;
        }
        
        // Fall back to the auth properties for backward compatibility
        String currentUser = authProperties.getProperty("current.user");
        String tokenExpiry = authProperties.getProperty("current.token.expiry");
        
        if (currentUser == null || tokenExpiry == null) {
            return null;
        }
        
        // Check if token is expired
        try {
            Instant expiry = Instant.parse(tokenExpiry);
            if (Instant.now().isAfter(expiry)) {
                logout(); // Token expired, logout
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        
        return currentUser;
    }
    
    /**
     * Check if the current user has admin role.
     *
     * @return true if the current user is an admin
     */
    public boolean isCurrentUserAdmin() {
        initialize();
        
        // First check the security config
        SecurityConfig config = SecurityConfig.getInstance();
        if (config.getCurrentUser() != null) {
            return config.isAdmin();
        }
        
        // Fall back to auth properties for backward compatibility
        String currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        String role = authProperties.getProperty("user." + currentUser + ".role");
        return "admin".equals(role);
    }
    
    /**
     * Logout the current user.
     */
    public void logout() {
        initialize();
        
        // Clear from auth properties
        authProperties.remove("current.user");
        authProperties.remove("current.token");
        authProperties.remove("current.token.expiry");
        saveProperties();
        
        // Clear from security config
        SecurityConfig config = SecurityConfig.getInstance();
        config.clearAuthToken();
    }
    
    /**
     * Save authentication properties to disk.
     */
    private void saveProperties() {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(AUTH_FILE)), StandardCharsets.UTF_8)) {
            authProperties.store(writer, "Rinna CLI Authentication Data");
        } catch (IOException e) {
            System.err.println("Error saving authentication data: " + e.getMessage());
        }
    }
    
    /**
     * Generate a security token.
     * 
     * @return a new security token
     */
    private String generateToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Validate user credentials.
     * This is a simplified implementation for demonstration purposes.
     * In a real implementation, this would verify against a secure credential store.
     * 
     * @param username the username
     * @param password the password
     * @return true if credentials are valid
     */
    private boolean isValidCredentials(String username, String password) {
        // For demonstration purposes only
        // In a real implementation, this would use secure password verification
        if ("admin".equals(username) && "admin123".equals(password)) {
            return true;
        }
        
        if ("user".equals(username) && "user123".equals(password)) {
            return true;
        }
        
        return false;
    }
}