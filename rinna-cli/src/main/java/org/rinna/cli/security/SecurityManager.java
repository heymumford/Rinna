/*
 * Security manager for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Security manager that provides a simplified interface for authentication and authorization.
 */
public final class SecurityManager {
    private static SecurityManager instance;
    
    private final AuthenticationService authService;
    private final AuthorizationService authzService;
    
    /**
     * Constructor initializes the security services.
     */
    private SecurityManager() {
        this.authService = new AuthenticationService();
        this.authzService = new AuthorizationService(authService);
        
        // Initialize services
        this.authService.initialize();
        this.authzService.initialize();
    }
    
    /**
     * Get the singleton instance of the security manager.
     * 
     * @return the security manager instance
     */
    public static synchronized SecurityManager getInstance() {
        if (instance == null) {
            instance = new SecurityManager();
        }
        return instance;
    }
    
    /**
     * Login a user with username and password.
     * 
     * @param username the username
     * @param password the password
     * @return true if login successful
     */
    public boolean login(String username, String password) {
        return authService.login(username, password);
    }
    
    /**
     * Logout the current user.
     */
    public void logout() {
        authService.logout();
    }
    
    /**
     * Get the current authenticated user.
     * 
     * @return the username of the current user, or null if not authenticated
     */
    public String getCurrentUser() {
        return authService.getCurrentUser();
    }
    
    /**
     * Check if the current user is authenticated.
     * 
     * @return true if user is authenticated
     */
    public boolean isAuthenticated() {
        return authService.getCurrentUser() != null;
    }
    
    /**
     * Check if the current user has admin role.
     * 
     * @return true if the user is an admin
     */
    public boolean isAdmin() {
        return authService.isCurrentUserAdmin();
    }
    
    /**
     * Check if the specified user has admin role.
     * 
     * @param username the username to check
     * @return true if the user is an admin
     */
    public boolean isAdmin(String username) {
        return authService.isUserAdmin(username);
    }
    
    /**
     * Check if the current user has a specific permission.
     * 
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    public boolean hasPermission(String permission) {
        return authzService.hasPermission(permission);
    }
    
    /**
     * Check if the current user has admin access to a specific area.
     * 
     * @param area the administrative area
     * @return true if the user has admin access
     */
    public boolean hasAdminAccess(String area) {
        return authzService.hasAdminAccess(area);
    }
    
    /**
     * Promote a user to admin role.
     * 
     * @param username the username to promote
     * @return true if promotion was successful
     */
    public boolean promoteToAdmin(String username) {
        // Only admins can promote other users
        if (!isAdmin()) {
            return false;
        }
        
        return authService.promoteToAdmin(username);
    }
    
    /**
     * Grant a permission to a user.
     * 
     * @param username the username
     * @param permission the permission to grant
     * @return true if permission was granted
     */
    public boolean grantPermission(String username, String permission) {
        return authzService.grantPermission(username, permission);
    }
    
    /**
     * Grant admin access to a user for a specific area.
     * 
     * @param username the username
     * @param area the administrative area
     * @return true if access was granted
     */
    public boolean grantAdminAccess(String username, String area) {
        return authzService.grantAdminAccess(username, area);
    }
    
    /**
     * Revoke a permission from a user.
     * 
     * @param username the username
     * @param permission the permission to revoke
     * @return true if permission was revoked
     */
    public boolean revokePermission(String username, String permission) {
        return authzService.revokePermission(username, permission);
    }
    
    /**
     * Revoke admin access from a user for a specific area.
     * 
     * @param username the username
     * @param area the administrative area
     * @return true if access was revoked
     */
    public boolean revokeAdminAccess(String username, String area) {
        return authzService.revokeAdminAccess(username, area);
    }
    
    /**
     * Get the authentication service.
     * 
     * @return the authentication service
     */
    public AuthenticationService getAuthenticationService() {
        return authService;
    }
    
    /**
     * Get the authorization service.
     * 
     * @return the authorization service
     */
    public AuthorizationService getAuthorizationService() {
        return authzService;
    }
    
    /**
     * Get all user data for a specific user.
     * 
     * @param username the username
     * @return map of user data, or null if user not found
     */
    public Map<String, Object> getUserData(String username) {
        if (!authService.userExists(username)) {
            return null;
        }
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("isAdmin", authService.isUserAdmin(username));
        userData.put("permissions", getUserPermissions(username));
        userData.put("adminAreas", getUserAdminAreas(username));
        
        return userData;
    }
    
    /**
     * Get all permissions for a specific user.
     * 
     * @param username the username
     * @return set of permissions
     */
    public Set<String> getUserPermissions(String username) {
        if (!authService.userExists(username)) {
            return Collections.emptySet();
        }
        
        // Get permissions from properties file
        Properties props = authzService.getPermissionsProperties();
        if (props == null) {
            return Collections.emptySet();
        }
        
        String permsStr = props.getProperty("user." + username + ".permissions", "");
        return parsePermissionString(permsStr);
    }
    
    /**
     * Get all admin areas for a specific user.
     * 
     * @param username the username
     * @return set of admin areas
     */
    public Set<String> getUserAdminAreas(String username) {
        if (!authService.userExists(username)) {
            return Collections.emptySet();
        }
        
        // Get admin areas from properties file
        Properties props = authzService.getPermissionsProperties();
        if (props == null) {
            return Collections.emptySet();
        }
        
        String areasStr = props.getProperty("user." + username + ".admin.areas", "");
        return parsePermissionString(areasStr);
    }
    
    /**
     * Parse a comma-separated string into a set of permissions.
     * 
     * @param permStr the comma-separated permission string
     * @return set of permissions
     */
    private Set<String> parsePermissionString(String permStr) {
        Set<String> result = new HashSet<>();
        
        if (permStr != null && !permStr.isEmpty()) {
            String[] parts = permStr.split(",");
            for (String part : parts) {
                result.add(part.trim());
            }
        }
        
        return result;
    }
}