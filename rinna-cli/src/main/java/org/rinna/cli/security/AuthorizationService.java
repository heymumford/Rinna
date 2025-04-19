/*
 * Authorization service for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Service for checking user permissions for various operations.
 */
public final class AuthorizationService {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.rinna";
    private static final String PERMISSIONS_FILE = CONFIG_DIR + "/permissions.properties";

    private final AuthenticationService authService;
    private Properties permissionsProperties;
    private boolean initialized = false;

    /**
     * Creates a new authorization service with the provided authentication service.
     * 
     * @param authService the authentication service
     */
    public AuthorizationService(AuthenticationService authService) {
        this.authService = authService;
    }

    /**
     * Initialize the authorization service.
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

        // Load permissions properties
        permissionsProperties = new Properties();
        File permissionsFile = new File(PERMISSIONS_FILE);

        if (permissionsFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    Files.newInputStream(permissionsFile.toPath()), StandardCharsets.UTF_8)) {
                permissionsProperties.load(reader);
            } catch (IOException e) {
                System.err.println("Error loading permissions data: " + e.getMessage());
            }
        } else {
            // Initialize with default permissions
            initializeDefaultPermissions();
        }

        initialized = true;
    }

    /**
     * Check if the current user has permission for the specified operation.
     * 
     * @param operation the operation to check
     * @return true if the user has permission
     */
    public boolean hasPermission(String operation) {
        initialize();

        // Get current user
        String currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return false; // Not authenticated
        }

        // Admins have all permissions
        if (authService.isCurrentUserAdmin()) {
            return true;
        }

        // Check user-specific permissions
        String userPermissions = permissionsProperties.getProperty("user." + currentUser + ".permissions", "");
        Set<String> permissions = parsePermissions(userPermissions);

        return permissions.contains(operation) || permissions.contains("*");
    }

    /**
     * Check if current user has admin access for a specific area.
     * 
     * @param area the administrative area (audit, compliance, monitor, etc.)
     * @return true if the user has admin access
     */
    public boolean hasAdminAccess(String area) {
        initialize();

        // Check if user is authenticated
        String currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return false; // Not authenticated
        }

        // Full admins have access to all areas
        if (authService.isCurrentUserAdmin()) {
            return true;
        }

        // Check for area-specific admin access
        String adminAreas = permissionsProperties.getProperty("user." + currentUser + ".admin.areas", "");
        Set<String> areas = parsePermissions(adminAreas);

        return areas.contains(area) || areas.contains("*");
    }

    /**
     * Grant permission to a user for a specific operation.
     * 
     * @param username the username
     * @param operation the operation
     * @return true if the permission was granted
     */
    public boolean grantPermission(String username, String operation) {
        initialize();

        // Check if current user is admin (only admins can grant permissions)
        if (!authService.isCurrentUserAdmin()) {
            return false;
        }

        // Check if target user exists
        if (!authService.userExists(username)) {
            return false;
        }

        // Get current permissions and add the new one
        String currentPermissions = permissionsProperties.getProperty("user." + username + ".permissions", "");
        Set<String> permissions = parsePermissions(currentPermissions);
        permissions.add(operation);

        // Save updated permissions
        permissionsProperties.setProperty("user." + username + ".permissions", formatPermissions(permissions));
        saveProperties();

        return true;
    }

    /**
     * Grant admin access to a user for a specific area.
     * 
     * @param username the username
     * @param area the administrative area
     * @return true if access was granted
     */
    public boolean grantAdminAccess(String username, String area) {
        initialize();

        // Check if current user is admin
        if (!authService.isCurrentUserAdmin()) {
            return false;
        }

        // Check if target user exists
        if (!authService.userExists(username)) {
            return false;
        }

        // Get current admin areas and add the new one
        String currentAreas = permissionsProperties.getProperty("user." + username + ".admin.areas", "");
        Set<String> areas = parsePermissions(currentAreas);
        areas.add(area);

        // Save updated admin areas
        permissionsProperties.setProperty("user." + username + ".admin.areas", formatPermissions(areas));
        saveProperties();

        return true;
    }

    /**
     * Revoke permission from a user for a specific operation.
     * 
     * @param username the username
     * @param operation the operation
     * @return true if the permission was revoked
     */
    public boolean revokePermission(String username, String operation) {
        initialize();

        // Check if current user is admin
        if (!authService.isCurrentUserAdmin()) {
            return false;
        }

        // Check if target user exists
        if (!authService.userExists(username)) {
            return false;
        }

        // Get current permissions and remove the specified one
        String currentPermissions = permissionsProperties.getProperty("user." + username + ".permissions", "");
        Set<String> permissions = parsePermissions(currentPermissions);
        permissions.remove(operation);

        // Save updated permissions
        permissionsProperties.setProperty("user." + username + ".permissions", formatPermissions(permissions));
        saveProperties();

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
        initialize();

        // Check if current user is admin
        if (!authService.isCurrentUserAdmin()) {
            return false;
        }

        // Check if target user exists
        if (!authService.userExists(username)) {
            return false;
        }

        // Get current admin areas and remove the specified one
        String currentAreas = permissionsProperties.getProperty("user." + username + ".admin.areas", "");
        Set<String> areas = parsePermissions(currentAreas);
        areas.remove(area);

        // Save updated admin areas
        permissionsProperties.setProperty("user." + username + ".admin.areas", formatPermissions(areas));
        saveProperties();

        return true;
    }

    /**
     * Initialize default permissions.
     */
    private void initializeDefaultPermissions() {
        // Default permissions for 'admin' user
        permissionsProperties.setProperty("user.admin.permissions", "*");
        permissionsProperties.setProperty("user.admin.admin.areas", "*");

        // Default permissions for 'user' user
        permissionsProperties.setProperty("user.user.permissions", "view,list,add,update");

        saveProperties();
    }

    /**
     * Save permissions properties to disk.
     */
    private void saveProperties() {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(PERMISSIONS_FILE)), StandardCharsets.UTF_8)) {
            permissionsProperties.store(writer, "Rinna CLI Permissions Data");
        } catch (IOException e) {
            System.err.println("Error saving permissions data: " + e.getMessage());
        }
    }

    /**
     * Parse permissions string into a set of permissions.
     * 
     * @param permissionsStr comma-separated list of permissions
     * @return set of permissions
     */
    private Set<String> parsePermissions(String permissionsStr) {
        Set<String> permissions = new HashSet<>();

        if (permissionsStr != null && !permissionsStr.isEmpty()) {
            String[] perms = permissionsStr.split(",");
            for (String perm : perms) {
                permissions.add(perm.trim());
            }
        }

        return permissions;
    }

    /**
     * Format a set of permissions into a comma-separated string.
     * 
     * @param permissions the set of permissions
     * @return comma-separated string of permissions
     */
    private String formatPermissions(Set<String> permissions) {
        return String.join(",", permissions);
    }

    /**
     * Get the permissions properties.
     * This method is used by the SecurityManager to access permissions.
     * 
     * @return the permissions properties
     */
    public Properties getPermissionsProperties() {
        initialize();
        return permissionsProperties;
    }

    /**
     * Check if the current user is authenticated.
     * 
     * @return true if the user is authenticated
     */
    public boolean isAuthenticated() {
        return authService.getCurrentUser() != null;
    }
}
