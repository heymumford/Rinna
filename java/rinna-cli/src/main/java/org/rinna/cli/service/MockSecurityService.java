/*
 * Mock security service for Rinna CLI tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mock implementation of security service for testing user access commands.
 */
public class MockSecurityService {
    private static MockSecurityService instance;
    
    private boolean authenticated = false;
    private boolean isAdmin = false;
    private String currentUser = null;
    
    private final Map<String, Set<String>> userPermissions = new HashMap<>();
    private final Map<String, Set<String>> userAdminAreas = new HashMap<>();
    private final Set<String> adminUsers = new HashSet<>();
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    public MockSecurityService() {
        // Default constructor
    }
    
    /**
     * Get the singleton instance.
     * 
     * @return the singleton instance
     */
    public static synchronized MockSecurityService getInstance() {
        if (instance == null) {
            instance = new MockSecurityService();
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
        // For testing, any non-empty username/password is valid
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            this.authenticated = true;
            this.currentUser = username;
            return true;
        }
        return false;
    }
    
    /**
     * Logout the current user.
     */
    public void logout() {
        this.authenticated = false;
        this.currentUser = null;
        this.isAdmin = false;
    }
    
    /**
     * Check if the current user has admin access to a specific area.
     * 
     * @param area the administrative area
     * @return true if the user has admin access
     */
    public boolean hasAdminAccess(String area) {
        if (!authenticated || currentUser == null) {
            return false;
        }
        
        // Full admins have access to all areas
        if (isAdmin) {
            return true;
        }
        
        // Check area-specific admin access
        Set<String> areas = userAdminAreas.get(currentUser);
        return areas != null && areas.contains(area);
    }
    
    /**
     * Set the authentication status.
     * 
     * @param authenticated true if user is authenticated
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    /**
     * Set the admin status.
     * 
     * @param isAdmin true if user is an admin
     */
    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    /**
     * Set the current user.
     * 
     * @param username the current username
     */
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }
    
    /**
     * Check if the current user is authenticated.
     * 
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    /**
     * Check if the current user is an admin.
     * 
     * @return true if admin
     */
    public boolean isAdmin() {
        return isAdmin;
    }
    
    /**
     * Get the current user.
     * 
     * @return the current username
     */
    public String getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Grant a permission to a user.
     * 
     * @param username the username
     * @param permission the permission to grant
     * @return true if permission was granted
     */
    public boolean grantPermission(String username, String permission) {
        if (!isAdmin) {
            return false;
        }
        
        userPermissions.computeIfAbsent(username, k -> new HashSet<>()).add(permission);
        return true;
    }
    
    /**
     * Revoke a permission from a user.
     * 
     * @param username the username
     * @param permission the permission to revoke
     * @return true if permission was revoked
     */
    public boolean revokePermission(String username, String permission) {
        if (!isAdmin) {
            return false;
        }
        
        Set<String> permissions = userPermissions.get(username);
        if (permissions != null) {
            permissions.remove(permission);
            return true;
        }
        
        return false;
    }
    
    /**
     * Grant admin access to a user for a specific area.
     * 
     * @param username the username
     * @param area the administrative area
     * @return true if access was granted
     */
    public boolean grantAdminAccess(String username, String area) {
        if (!isAdmin) {
            return false;
        }
        
        userAdminAreas.computeIfAbsent(username, k -> new HashSet<>()).add(area);
        return true;
    }
    
    /**
     * Revoke admin access from a user for a specific area.
     * 
     * @param username the username
     * @param area the administrative area
     * @return true if access was revoked
     */
    public boolean revokeAdminAccess(String username, String area) {
        if (!isAdmin) {
            return false;
        }
        
        Set<String> areas = userAdminAreas.get(username);
        if (areas != null) {
            areas.remove(area);
            return true;
        }
        
        return false;
    }
    
    /**
     * Promote a user to admin role.
     * 
     * @param username the username to promote
     * @return true if promotion was successful
     */
    public boolean promoteToAdmin(String username) {
        if (!isAdmin) {
            return false;
        }
        
        adminUsers.add(username);
        return true;
    }
    
    /**
     * Check if a user has a specific permission.
     * 
     * @param username the username
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    public boolean hasPermission(String username, String permission) {
        Set<String> permissions = userPermissions.get(username);
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * Check if a user has admin access for a specific area.
     * 
     * @param username the username
     * @param area the administrative area
     * @return true if the user has admin access
     */
    public boolean hasAdminAccess(String username, String area) {
        Set<String> areas = userAdminAreas.get(username);
        return areas != null && areas.contains(area);
    }
    
    /**
     * Check if a user is an admin.
     * 
     * @param username the username
     * @return true if the user is an admin
     */
    public boolean isUserAdmin(String username) {
        return adminUsers.contains(username);
    }
    
    /**
     * Get all permissions for a specific user.
     * 
     * @param username the username
     * @return set of permissions
     */
    public Set<String> getUserPermissions(String username) {
        return userPermissions.getOrDefault(username, new HashSet<>());
    }
    
    /**
     * Get all admin areas for a specific user.
     * 
     * @param username the username
     * @return set of admin areas
     */
    public Set<String> getUserAdminAreas(String username) {
        return userAdminAreas.getOrDefault(username, new HashSet<>());
    }
    
    /**
     * Reset the mock security service.
     */
    public void reset() {
        authenticated = false;
        isAdmin = false;
        currentUser = null;
        userPermissions.clear();
        userAdminAreas.clear();
        adminUsers.clear();
    }
}