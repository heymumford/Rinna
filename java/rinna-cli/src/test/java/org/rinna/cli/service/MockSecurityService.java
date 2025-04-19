package org.rinna.cli.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mock security service for testing.
 */
public class MockSecurityService {
    private String currentUser;
    private boolean isAdmin;
    private Map<String, Set<String>> userRoles;
    
    /**
     * Constructor.
     */
    public MockSecurityService() {
        userRoles = new HashMap<>();
        Set<String> adminRoles = new HashSet<>();
        adminRoles.add("ADMIN");
        userRoles.put("admin", adminRoles);
        
        currentUser = null;
        isAdmin = false;
    }
    
    /**
     * Login a user.
     * 
     * @param username the username
     * @param password the password
     * @return true if login successful
     */
    public boolean login(String username, String password) {
        if (username != null && !username.isEmpty()) {
            currentUser = username;
            isAdmin = "admin".equals(username) || isAdmin;
            return true;
        }
        return false;
    }
    
    /**
     * Logout the current user.
     */
    public void logout() {
        currentUser = null;
        isAdmin = false;
    }
    
    /**
     * Get the current authenticated user.
     * 
     * @return the username of the current user, or null if not authenticated
     */
    public String getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Set the current user (for testing).
     * 
     * @param username the username to set
     */
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }
    
    /**
     * Set the admin status (for testing).
     * 
     * @param isAdmin the admin status to set
     */
    public void setAdminStatus(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    /**
     * Check if the current user is authenticated.
     * 
     * @return true if user is authenticated
     */
    public boolean isAuthenticated() {
        return currentUser != null;
    }
    
    /**
     * Check if the current user has admin role.
     * 
     * @return true if the user is an admin
     */
    public boolean isAdmin() {
        return isAdmin;
    }
    
    /**
     * Check if the specified user has admin role.
     * 
     * @param username the username to check
     * @return true if the user is an admin
     */
    public boolean isAdmin(String username) {
        Set<String> roles = userRoles.get(username);
        return roles != null && roles.contains("ADMIN");
    }
    
    /**
     * Check if the current user has the specified role.
     * 
     * @param role the role to check
     * @return true if the user has the role
     */
    public boolean hasRole(String role) {
        if (currentUser == null) {
            return false;
        }
        
        Set<String> roles = userRoles.get(currentUser);
        return roles != null && roles.contains(role);
    }
    
    /**
     * Check if the current user has the specified permission.
     * 
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    public boolean hasPermission(String permission) {
        return isAdmin || "read".equals(permission);
    }
    
    /**
     * Add a role to a user.
     * 
     * @param username the username
     * @param role the role to add
     */
    public void addRole(String username, String role) {
        userRoles.computeIfAbsent(username, k -> new HashSet<>()).add(role);
    }
    
    /**
     * Remove a role from a user.
     * 
     * @param username the username
     * @param role the role to remove
     */
    public void removeRole(String username, String role) {
        Set<String> roles = userRoles.get(username);
        if (roles != null) {
            roles.remove(role);
        }
    }
    
    /**
     * Grants permission to a user.
     * 
     * @param username the username
     * @param permission the permission to grant
     * @return true if successful
     */
    public boolean grantPermission(String username, String permission) {
        if (username != null && permission != null) {
            // Simulate granting a permission
            return true;
        }
        return false;
    }
    
    /**
     * Revokes permission from a user.
     * 
     * @param username the username
     * @param permission the permission to revoke
     * @return true if successful
     */
    public boolean revokePermission(String username, String permission) {
        if (username != null && permission != null) {
            // Simulate revoking a permission
            return true;
        }
        return false;
    }
    
    /**
     * Grants admin access to a user for a specific area.
     * 
     * @param username the username
     * @param area the admin area
     * @return true if successful
     */
    public boolean grantAdminAccess(String username, String area) {
        if (username != null && area != null) {
            // Simulate granting admin access
            return true;
        }
        return false;
    }
    
    /**
     * Revokes admin access from a user for a specific area.
     * 
     * @param username the username
     * @param area the admin area
     * @return true if successful
     */
    public boolean revokeAdminAccess(String username, String area) {
        if (username != null && area != null) {
            // Simulate revoking admin access
            return true;
        }
        return false;
    }
    
    /**
     * Promotes a user to full admin.
     * 
     * @param username the username
     * @return true if successful
     */
    public boolean promoteToAdmin(String username) {
        if (username != null) {
            addRole(username, "ADMIN");
            return true;
        }
        return false;
    }
    
    /**
     * Checks if the current user has admin access to a specific area.
     * 
     * @param area the admin area
     * @return true if the user has admin access
     */
    public boolean hasAdminAccess(String area) {
        // Admins have access to all areas
        if (isAdmin) {
            return true;
        }
        
        // Non-admins don't have access
        return false;
    }
}