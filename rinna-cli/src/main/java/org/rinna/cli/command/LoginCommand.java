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

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;

/**
 * Command for authenticating users with the Rinna system.
 * This command handles user authentication, supporting both interactive
 * and non-interactive login modes.
 * 
 * Usage examples:
 * - rin login
 * - rin login username
 * - rin login --user=username
 * - rin login --format=json username
 */
public class LoginCommand implements Callable<Integer> {
    
    private String username = null;
    private String password = null;
    private boolean interactive = true;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    private SecurityManager securityManager;
    
    /**
     * Creates a new LoginCommand with default services.
     */
    public LoginCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new LoginCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public LoginCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
        this.securityManager = SecurityManager.getInstance();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("username", username != null ? username : "");
        params.put("interactive", interactive);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("login", "AUTHENTICATION", params);
        
        try {
            // Check if already authenticated
            if (securityManager.isAuthenticated()) {
                String currentUser = securityManager.getCurrentUser();
                
                // If no username provided, show current user
                if (username == null) {
                    outputCurrentUser(currentUser, operationId);
                    return 0;
                }
                
                // If username provided, check if it matches current user
                if (currentUser.equals(username)) {
                    System.out.println("You are already logged in as: " + username);
                    
                    // Record the successful operation
                    Map<String, Object> result = new HashMap<>();
                    result.put("username", username);
                    result.put("status", "already_logged_in");
                    result.put("admin", securityManager.isAdmin());
                    
                    metadataService.completeOperation(operationId, result);
                    return 0;
                }
                
                // If username differs, log out first
                System.out.println("Logging out current user: " + currentUser);
                securityManager.logout();
            }
            
            // If no username provided, prompt for it
            if (username == null) {
                username = promptForUsername();
            }
            
            // If no password provided, prompt for it
            if (password == null) {
                password = promptForPassword();
            }
            
            // Attempt to log in
            boolean success = securityManager.login(username, password);
            if (success) {
                outputSuccessfulLogin(operationId);
                return 0;
            } else {
                String errorMessage = "Login failed: Invalid username or password";
                System.err.println(errorMessage);
                metadataService.failOperation(operationId, new SecurityException(errorMessage));
                return 1;
            }
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error during login: " + e.getMessage();
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
     * Displays information about the currently logged-in user.
     * 
     * @param currentUser the current user
     * @param operationId the operation ID for tracking
     */
    private void outputCurrentUser(String currentUser, String operationId) {
        boolean isAdmin = securityManager.isAdmin();
        
        if ("json".equalsIgnoreCase(format)) {
            // JSON output
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"already_logged_in\",\n");
            json.append("  \"username\": \"").append(currentUser).append("\",\n");
            json.append("  \"role\": \"").append(isAdmin ? "Administrator" : "User").append("\"\n");
            json.append("}");
            System.out.println(json.toString());
        } else {
            // Text output
            System.out.println("You are already logged in as: " + currentUser);
            System.out.println("Role: " + (isAdmin ? "Administrator" : "User"));
        }
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("username", currentUser);
        result.put("status", "already_logged_in");
        result.put("admin", isAdmin);
        
        metadataService.completeOperation(operationId, result);
    }
    
    /**
     * Displays information about a successful login.
     * 
     * @param operationId the operation ID for tracking
     */
    private void outputSuccessfulLogin(String operationId) {
        boolean isAdmin = securityManager.isAdmin();
        
        if ("json".equalsIgnoreCase(format)) {
            // JSON output
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"username\": \"").append(username).append("\",\n");
            json.append("  \"role\": \"").append(isAdmin ? "Administrator" : "User").append("\"\n");
            json.append("}");
            System.out.println(json.toString());
        } else {
            // Text output
            System.out.println("Successfully logged in as: " + username);
            System.out.println("Role: " + (isAdmin ? "Administrator" : "User"));
        }
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("status", "success");
        result.put("admin", isAdmin);
        
        metadataService.completeOperation(operationId, result);
    }
    
    /**
     * Prompt for username on the console.
     * 
     * @return the username
     */
    private String promptForUsername() {
        System.out.print("Username: ");
        Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8.name());
        return scanner.nextLine().trim();
    }
    
    /**
     * Prompt for password on the console.
     * 
     * @return the password
     */
    private String promptForPassword() {
        Console console = System.console();
        if (console != null) {
            // Use console for secure password entry
            char[] passwordChars = console.readPassword("Password: ");
            return new String(passwordChars);
        } else {
            // Fallback for environments without a console
            System.out.print("Password: ");
            Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8.name());
            return scanner.nextLine();
        }
    }
    
    /**
     * Sets the username for login.
     * 
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Gets the username.
     * 
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Sets the password for login.
     * 
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
        this.interactive = false;
    }
    
    /**
     * Gets whether this login is interactive.
     * 
     * @return true if this login is interactive
     */
    public boolean isInteractive() {
        return interactive;
    }
    
    /**
     * Sets whether this login is interactive.
     * 
     * @param interactive true if this login is interactive
     */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
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