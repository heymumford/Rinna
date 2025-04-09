/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */

package org.rinna.cli.command;

import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command for managing user permissions and admin access.
 * 
 * Usage examples:
 * - rin access help - Show help information
 * - rin access grant-permission --user=alice --permission=view
 * - rin access revoke-permission --user=bob --permission=edit
 * - rin access grant-admin --user=charlie --area=reports
 * - rin access revoke-admin --user=dave --area=security
 * - rin access promote --user=eve
 * - rin access list --user=frank --format=json
 */
public class UserAccessCommand implements Callable<Integer> {
    
    private String action;
    private String username;
    private String permission;
    private String area;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final SecurityManager securityManager;
    private final MetadataService metadataService;
    
    /**
     * Creates a new UserAccessCommand with default services.
     */
    public UserAccessCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new UserAccessCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public UserAccessCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
        this.securityManager = SecurityManager.getInstance();
    }
    
    /**
     * Set the action to perform (grant, revoke, promote).
     * 
     * @param action the action
     */
    public void setAction(String action) {
        this.action = action;
    }
    
    /**
     * Set the target username.
     * 
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Set the permission to grant or revoke.
     * 
     * @param permission the permission
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }
    
    /**
     * Set the administrative area for which to grant or revoke access.
     * 
     * @param area the area
     */
    public void setArea(String area) {
        this.area = area;
    }
    
    /**
     * Set the output format.
     * 
     * @param format the output format (text, json)
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Set verbose output mode.
     * 
     * @param verbose true to enable verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("action", action != null ? action : "help");
        if (username != null) params.put("username", username);
        if (permission != null) params.put("permission", permission);
        if (area != null) params.put("area", area);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("access", "SECURITY", params);
        
        try {
            // Check if authenticated and has admin privileges
            if (!securityManager.isAuthenticated()) {
                String errorMessage = "Authentication required. Please log in first.";
                System.err.println("Error: " + errorMessage);
                System.err.println("Use 'rin login' to authenticate.");
                metadataService.failOperation(operationId, new SecurityException(errorMessage));
                return 1;
            }
            
            if (!securityManager.isAdmin()) {
                String errorMessage = "Administrative privileges required for user access management.";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new SecurityException(errorMessage));
                return 1;
            }
            
            // Handle different actions
            if (action == null || action.isEmpty()) {
                return displayHelp(operationId);
            }
            
            switch (action) {
                case "grant-permission":
                    return handleGrantPermission(operationId);
                case "revoke-permission":
                    return handleRevokePermission(operationId);
                case "grant-admin":
                    return handleGrantAdmin(operationId);
                case "revoke-admin":
                    return handleRevokeAdmin(operationId);
                case "promote":
                    return handlePromote(operationId);
                case "help":
                    return displayHelp(operationId);
                case "list":
                    return handleList(operationId);
                default:
                    String errorMessage = "Unknown action: " + action;
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    displayHelp(operationId);
                    return 1;
            }
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error executing access command: " + e.getMessage();
            System.err.println("Error: " + e.getMessage());
            
            // Record detailed error information if verbose mode is enabled
            if (verbose) {
                e.printStackTrace();
            }
            
            // Record the failed operation with error details
            metadataService.failOperation(operationId, e);
            
            return 1;
        }
    }
    
    /**
     * Handle granting a permission to a user.
     * 
     * @param operationId the operation tracking ID
     * @return exit code
     */
    private int handleGrantPermission(String operationId) {
        if (username == null || username.isEmpty()) {
            String errorMessage = "Username required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        if (permission == null || permission.isEmpty()) {
            String errorMessage = "Permission required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        boolean success = securityManager.grantPermission(username, permission);
        
        if (success) {
            System.out.println("Successfully granted permission '" + permission + "' to user '" + username + "'.");
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("action", "grant-permission");
            result.put("username", username);
            result.put("permission", permission);
            result.put("success", true);
            metadataService.completeOperation(operationId, result);
            
            return 0;
        } else {
            String errorMessage = "Failed to grant permission. User may not exist.";
            System.err.println(errorMessage);
            
            // Record the failed operation
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            
            return 1;
        }
    }
    
    /**
     * Handle revoking a permission from a user.
     * 
     * @param operationId the operation tracking ID
     * @return exit code
     */
    private int handleRevokePermission(String operationId) {
        if (username == null || username.isEmpty()) {
            String errorMessage = "Username required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        if (permission == null || permission.isEmpty()) {
            String errorMessage = "Permission required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        boolean success = securityManager.revokePermission(username, permission);
        
        if (success) {
            System.out.println("Successfully revoked permission '" + permission + "' from user '" + username + "'.");
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("action", "revoke-permission");
            result.put("username", username);
            result.put("permission", permission);
            result.put("success", true);
            metadataService.completeOperation(operationId, result);
            
            return 0;
        } else {
            String errorMessage = "Failed to revoke permission. User may not exist.";
            System.err.println(errorMessage);
            
            // Record the failed operation
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            
            return 1;
        }
    }
    
    /**
     * Handle granting admin access to a user for a specific area.
     * 
     * @param operationId the operation tracking ID
     * @return exit code
     */
    private int handleGrantAdmin(String operationId) {
        if (username == null || username.isEmpty()) {
            String errorMessage = "Username required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        if (area == null || area.isEmpty()) {
            String errorMessage = "Administrative area required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        boolean success = securityManager.grantAdminAccess(username, area);
        
        if (success) {
            System.out.println("Successfully granted admin access for area '" + area + "' to user '" + username + "'.");
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("action", "grant-admin");
            result.put("username", username);
            result.put("area", area);
            result.put("success", true);
            metadataService.completeOperation(operationId, result);
            
            return 0;
        } else {
            String errorMessage = "Failed to grant admin access. User may not exist.";
            System.err.println(errorMessage);
            
            // Record the failed operation
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            
            return 1;
        }
    }
    
    /**
     * Handle revoking admin access from a user for a specific area.
     * 
     * @param operationId the operation tracking ID
     * @return exit code
     */
    private int handleRevokeAdmin(String operationId) {
        if (username == null || username.isEmpty()) {
            String errorMessage = "Username required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        if (area == null || area.isEmpty()) {
            String errorMessage = "Administrative area required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        boolean success = securityManager.revokeAdminAccess(username, area);
        
        if (success) {
            System.out.println("Successfully revoked admin access for area '" + area + "' from user '" + username + "'.");
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("action", "revoke-admin");
            result.put("username", username);
            result.put("area", area);
            result.put("success", true);
            metadataService.completeOperation(operationId, result);
            
            return 0;
        } else {
            String errorMessage = "Failed to revoke admin access. User may not exist.";
            System.err.println(errorMessage);
            
            // Record the failed operation
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            
            return 1;
        }
    }
    
    /**
     * Handle promoting a user to full admin role.
     * 
     * @param operationId the operation tracking ID
     * @return exit code
     */
    private int handlePromote(String operationId) {
        if (username == null || username.isEmpty()) {
            String errorMessage = "Username required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        boolean success = securityManager.promoteToAdmin(username);
        
        if (success) {
            System.out.println("Successfully promoted user '" + username + "' to administrator role.");
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("action", "promote");
            result.put("username", username);
            result.put("success", true);
            metadataService.completeOperation(operationId, result);
            
            return 0;
        } else {
            String errorMessage = "Failed to promote user. User may not exist.";
            System.err.println(errorMessage);
            
            // Record the failed operation
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            
            return 1;
        }
    }
    
    /**
     * Display help information.
     * 
     * @param operationId the operation tracking ID
     * @return exit code
     */
    private int displayHelp(String operationId) {
        System.out.println("Usage: rin access <action> [options]");
        System.out.println();
        System.out.println("Actions:");
        System.out.println("  grant-permission  - Grant a permission to a user");
        System.out.println("  revoke-permission - Revoke a permission from a user");
        System.out.println("  grant-admin       - Grant admin access for a specific area");
        System.out.println("  revoke-admin      - Revoke admin access for a specific area");
        System.out.println("  promote           - Promote a user to full administrator role");
        System.out.println("  list              - List a user's permissions");
        System.out.println("  help              - Show this help information");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --user=<username>     - The username to operate on");
        System.out.println("  --permission=<perm>   - The permission to grant or revoke");
        System.out.println("  --area=<admin-area>   - The administrative area for admin access");
        System.out.println("  --format=<text|json>  - Output format (default: text)");
        System.out.println("  --verbose             - Show detailed information");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  rin access grant-permission --user=alice --permission=view");
        System.out.println("  rin access grant-admin --user=bob --area=audit");
        System.out.println("  rin access promote --user=charlie");
        System.out.println("  rin access list --user=alice --format=json");
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("action", "help");
        result.put("success", true);
        metadataService.completeOperation(operationId, result);
        
        return 0;
    }
    
    /**
     * Handle listing user permissions and admin access.
     *
     * @param operationId the operation tracking ID
     * @return exit code
     */
    private int handleList(String operationId) {
        if (username == null || username.isEmpty()) {
            String errorMessage = "Username required.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        Map<String, Object> userData = securityManager.getUserData(username);
        
        if (userData == null) {
            String errorMessage = "User '" + username + "' not found.";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        if ("json".equalsIgnoreCase(format)) {
            String json = OutputFormatter.toJson(userData, verbose);
            System.out.println(json);
        } else {
            System.out.println("User access for: " + username);
            System.out.println("Admin: " + (securityManager.isAdmin(username) ? "Yes" : "No"));
            System.out.println();
            
            System.out.println("Permissions:");
            for (String perm : securityManager.getUserPermissions(username)) {
                System.out.println("  - " + perm);
            }
            
            System.out.println();
            System.out.println("Admin Areas:");
            for (String area : securityManager.getUserAdminAreas(username)) {
                System.out.println("  - " + area);
            }
        }
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("action", "list");
        result.put("username", username);
        result.put("format", format);
        result.put("is_admin", securityManager.isAdmin(username));
        result.put("permission_count", securityManager.getUserPermissions(username).size());
        result.put("admin_areas_count", securityManager.getUserAdminAreas(username).size());
        metadataService.completeOperation(operationId, result);
        
        return 0;
    }
}