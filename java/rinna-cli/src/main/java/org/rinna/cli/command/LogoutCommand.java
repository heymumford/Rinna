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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;

/**
 * Command for logging out users from the Rinna system.
 * This command handles user logout, supporting different output formats.
 * 
 * Usage examples:
 * - rin logout
 * - rin logout --format=json
 * - rin logout --verbose
 */
public class LogoutCommand implements Callable<Integer> {
    
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    private final SecurityManager securityManager;
    
    /**
     * Creates a new LogoutCommand with default services.
     */
    public LogoutCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new LogoutCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public LogoutCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
        this.securityManager = SecurityManager.getInstance();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("logout", "AUTHENTICATION", params);
        
        try {
            // Check if authenticated
            if (securityManager.isAuthenticated()) {
                String currentUser = securityManager.getCurrentUser();
                boolean wasAdmin = securityManager.isAdmin();
                
                // Perform logout
                securityManager.logout();
                
                // Output the result based on format
                if ("json".equalsIgnoreCase(format)) {
                    outputJson(currentUser, wasAdmin, operationId);
                } else {
                    outputText(currentUser, wasAdmin, operationId);
                }
                
                // Record the successful operation
                Map<String, Object> result = new HashMap<>();
                result.put("username", currentUser);
                result.put("status", "logged_out");
                result.put("was_admin", wasAdmin);
                
                metadataService.completeOperation(operationId, result);
                return 0;
            } else {
                // Not logged in
                if ("json".equalsIgnoreCase(format)) {
                    System.out.println("{\n  \"status\": \"not_logged_in\"\n}");
                } else {
                    System.out.println("You are not currently logged in.");
                }
                
                // Record the operation (not a failure, but a no-op)
                Map<String, Object> result = new HashMap<>();
                result.put("status", "not_logged_in");
                
                metadataService.completeOperation(operationId, result);
                return 0;
            }
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error during logout: " + e.getMessage();
            System.err.println(errorMessage);
            
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
     * Outputs logout information in text format.
     * 
     * @param username the username that was logged out
     * @param wasAdmin whether the user was an admin
     * @param operationId the operation ID for tracking
     */
    private void outputText(String username, boolean wasAdmin, String operationId) {
        System.out.println("Successfully logged out user: " + username);
        
        if (verbose) {
            System.out.println("Previous role: " + (wasAdmin ? "Administrator" : "User"));
            System.out.println("Session terminated at: " + java.time.LocalDateTime.now());
        }
    }
    
    /**
     * Outputs logout information in JSON format.
     * 
     * @param username the username that was logged out
     * @param wasAdmin whether the user was an admin
     * @param operationId the operation ID for tracking
     */
    private void outputJson(String username, boolean wasAdmin, String operationId) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"logged_out\",\n");
        json.append("  \"username\": \"").append(username).append("\"");
        
        if (verbose) {
            json.append(",\n  \"role\": \"").append(wasAdmin ? "Administrator" : "User").append("\",\n");
            json.append("  \"timestamp\": \"").append(java.time.LocalDateTime.now()).append("\"");
        }
        
        json.append("\n}");
        System.out.println(json.toString());
    }
    
    /**
     * Gets the output format.
     *
     * @return the output format
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * Sets the output format (text or json).
     *
     * @param format the output format
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Gets whether verbose output is enabled.
     *
     * @return true if verbose output is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }
    
    /**
     * Sets whether verbose output is enabled.
     *
     * @param verbose true to enable verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Sets whether to output in JSON format.
     * This is a convenience method for picocli integration.
     *
     * @param jsonOutput true to output in JSON format
     */
    public void setJsonOutput(boolean jsonOutput) {
        if (jsonOutput) {
            this.format = "json";
        }
    }
    
    /**
     * Gets whether to output in JSON format.
     * This is a convenience method for picocli integration.
     *
     * @return true if output format is JSON
     */
    public boolean isJsonOutput() {
        return "json".equalsIgnoreCase(format);
    }
}