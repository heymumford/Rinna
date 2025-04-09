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
     * Verifies user credentials and generates an authentication token.
     *
     * @param username the username
     * @param password the password
     * @return true if login successful
     */
    public boolean login(String username, String password) {
        initialize();
        
        // Verify credentials using secure hash comparison
        if (isValidCredentials(username, password)) {
            // Generate cryptographically secure token
            String token = SecurityConfig.generateAuthToken();
            
            // Store token with expiration
            authProperties.setProperty("current.user", username);
            authProperties.setProperty("current.token", token);
            authProperties.setProperty("current.token.expiry", 
                                      Instant.now().plus(TOKEN_VALIDITY).toString());
            
            // Set or update role if not already present
            if (!authProperties.containsKey("user." + username + ".role")) {
                authProperties.setProperty("user." + username + ".role", "user");
            }
            saveProperties();
            
            // Update security configuration
            SecurityConfig config = SecurityConfig.getInstance();
            config.storeAuthToken(username, token);
            config.setAdminStatus("admin".equals(username) || isCurrentUserAdmin());
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Promote a user to admin role.
     * This operation requires the current user to have admin privileges.
     *
     * @param username the username to promote
     * @return true if promotion successful
     */
    public boolean promoteToAdmin(String username) {
        initialize();
        
        // Verify the user exists
        if (!userExists(username)) {
            return false;
        }
        
        // Verify current user has admin rights
        if (!isCurrentUserAdmin()) {
            return false;
        }
        
        // Update role and save
        authProperties.setProperty("user." + username + ".role", "admin");
        saveProperties();
        
        // Also update in security config if it's the current user
        String currentUser = getCurrentUser();
        if (username.equals(currentUser)) {
            SecurityConfig config = SecurityConfig.getInstance();
            config.setAdminStatus(true);
        }
        
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
     * Validate user credentials using secure password verification.
     * 
     * @param username the username
     * @param password the password
     * @return true if credentials are valid
     */
    private boolean isValidCredentials(String username, String password) {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return false;
        }
        
        // Get stored credentials from properties
        String storedRole = authProperties.getProperty("user." + username + ".role");
        String storedHash = authProperties.getProperty("user." + username + ".passwordHash");
        
        // First time setup: Check if this is the first login ever (no users in the system)
        boolean isFirstSetup = isFirstTimeSetup();
        
        // If user doesn't exist in our properties but meets registration criteria, create the user
        if (storedRole == null) {
            // First time setup case: Create the first user as admin
            if (isFirstSetup) {
                // When no users exist, the first user becomes an admin regardless of username
                // This allows bootstrap of the system with a chosen username
                authProperties.setProperty("user." + username + ".role", "admin");
                authProperties.setProperty("user." + username + ".passwordHash", 
                                  SecurityConfig.hashPassword(password));
                authProperties.setProperty("user." + username + ".created", 
                                  java.time.Instant.now().toString());
                authProperties.setProperty("system.setup.completed", "true");
                saveProperties();
                
                // Log the admin creation
                logSecurityEvent("ADMIN_CREATED", username, "First time system setup");
                return true;
            } 
            // Special handling for built-in users (only for development/testing)
            else if (isDevEnvironment()) {
                if ("admin".equals(username) && "admin123".equals(password)) {
                    // Create admin user if it doesn't exist in dev mode
                    authProperties.setProperty("user." + username + ".role", "admin");
                    authProperties.setProperty("user." + username + ".passwordHash", 
                                      SecurityConfig.hashPassword(password));
                    authProperties.setProperty("user." + username + ".created", 
                                      java.time.Instant.now().toString());
                    saveProperties();
                    
                    // Log the admin creation
                    logSecurityEvent("DEV_ADMIN_CREATED", username, "Development environment user setup");
                    return true;
                } else if ("user".equals(username) && "user123".equals(password)) {
                    // Create regular user if it doesn't exist in dev mode
                    authProperties.setProperty("user." + username + ".role", "user");
                    authProperties.setProperty("user." + username + ".passwordHash", 
                                      SecurityConfig.hashPassword(password));
                    authProperties.setProperty("user." + username + ".created", 
                                      java.time.Instant.now().toString());
                    saveProperties();
                    
                    // Log the regular user creation
                    logSecurityEvent("DEV_USER_CREATED", username, "Development environment user setup");
                    return true;
                }
            }
            
            // Handle user creation through proper registration channels
            if (authProperties.getProperty("registration." + username) != null) {
                // This handles the case where a registration was started but not completed
                // The registration property would have been set by the registerUser method
                String pendingToken = authProperties.getProperty("registration." + username + ".token");
                String expiryString = authProperties.getProperty("registration." + username + ".expiry");
                
                if (pendingToken != null && expiryString != null) {
                    try {
                        // Check if registration token is still valid
                        java.time.Instant expiry = java.time.Instant.parse(expiryString);
                        if (java.time.Instant.now().isBefore(expiry)) {
                            // Create the user account with the provided password
                            authProperties.setProperty("user." + username + ".role", "user");
                            authProperties.setProperty("user." + username + ".passwordHash", 
                                              SecurityConfig.hashPassword(password));
                            authProperties.setProperty("user." + username + ".created", 
                                              java.time.Instant.now().toString());
                            
                            // Clear registration data
                            authProperties.remove("registration." + username + ".token");
                            authProperties.remove("registration." + username + ".expiry");
                            authProperties.remove("registration." + username);
                            saveProperties();
                            
                            // Log user creation
                            logSecurityEvent("USER_CREATED", username, "User registration completed");
                            return true;
                        }
                    } catch (Exception e) {
                        // Invalid expiry date format, registration is invalid
                    }
                }
            }
            
            // User doesn't exist and no valid registration
            logSecurityEvent("LOGIN_FAILED", username, "Invalid credentials - user does not exist");
            return false;
        }
        
        // For existing users, verify hash and handle brute force protection
        if (storedHash != null) {
            String lockoutKey = "user." + username + ".lockout";
            String failedAttemptsKey = "user." + username + ".failedAttempts";
            String lastFailureKey = "user." + username + ".lastFailure";
            
            // Check for account lockout
            String lockoutString = authProperties.getProperty(lockoutKey);
            if (lockoutString != null) {
                try {
                    java.time.Instant lockoutExpiry = java.time.Instant.parse(lockoutString);
                    if (java.time.Instant.now().isBefore(lockoutExpiry)) {
                        // Account is locked
                        logSecurityEvent("LOGIN_LOCKED", username, "Account is temporarily locked");
                        return false;
                    } else {
                        // Lockout expired, clear lockout and reset failed attempts
                        authProperties.remove(lockoutKey);
                        authProperties.remove(failedAttemptsKey);
                        saveProperties();
                    }
                } catch (Exception e) {
                    // Invalid lockout timestamp format, proceed with validation
                    authProperties.remove(lockoutKey);
                }
            }
            
            // Validate password
            boolean isValid = SecurityConfig.verifyPassword(password, storedHash);
            
            if (isValid) {
                // Successful login, reset failed attempts counter
                authProperties.remove(failedAttemptsKey);
                authProperties.remove(lastFailureKey);
                
                // Update last login timestamp
                authProperties.setProperty("user." + username + ".lastLogin", 
                                  java.time.Instant.now().toString());
                saveProperties();
                
                // Log successful login
                logSecurityEvent("LOGIN_SUCCESS", username, "User authenticated successfully");
                return true;
            } else {
                // Failed login attempt
                int failedAttempts = 1;
                String failedAttemptsStr = authProperties.getProperty(failedAttemptsKey);
                if (failedAttemptsStr != null) {
                    try {
                        failedAttempts = Integer.parseInt(failedAttemptsStr) + 1;
                    } catch (NumberFormatException e) {
                        // Invalid count, reset to 1
                        failedAttempts = 1;
                    }
                }
                
                // Record failed attempt
                authProperties.setProperty(failedAttemptsKey, String.valueOf(failedAttempts));
                authProperties.setProperty(lastFailureKey, java.time.Instant.now().toString());
                
                // Implement account lockout after multiple failures
                if (failedAttempts >= 5) {
                    // Lock account for 30 minutes after 5 failed attempts
                    java.time.Instant lockoutExpiry = java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.MINUTES);
                    authProperties.setProperty(lockoutKey, lockoutExpiry.toString());
                    logSecurityEvent("ACCOUNT_LOCKED", username, "Account locked after " + failedAttempts + " failed attempts");
                } else {
                    logSecurityEvent("LOGIN_FAILED", username, "Invalid credentials - attempt " + failedAttempts);
                }
                
                saveProperties();
                return false;
            }
        }
        
        // User exists but no password hash (corrupted account)
        logSecurityEvent("LOGIN_ERROR", username, "Account data corrupted (missing password hash)");
        return false;
    }
    
    /**
     * Register a new user in the system.
     * This creates a pending registration that must be completed by the user.
     * 
     * @param username the username to register
     * @param email the user's email address
     * @param inviteCode optional invite code (required if invites are restricted)
     * @return a registration token, or null if registration failed
     */
    public String registerUser(String username, String email, String inviteCode) {
        initialize();
        
        // Validate inputs
        if (username == null || username.isEmpty() || email == null || email.isEmpty()) {
            return null;
        }
        
        // Check if user already exists
        if (userExists(username)) {
            return null;
        }
        
        // Check if registration is restricted to invite only
        boolean inviteRequired = "true".equals(authProperties.getProperty("system.invite.required", "false"));
        if (inviteRequired && (inviteCode == null || !isValidInviteCode(inviteCode))) {
            return null;
        }
        
        // Generate registration token
        String token = generateToken();
        String expiryStr = java.time.Instant.now()
            .plus(24, java.time.temporal.ChronoUnit.HOURS).toString();
        
        // Store pending registration
        authProperties.setProperty("registration." + username, token);
        authProperties.setProperty("registration." + username + ".email", email);
        authProperties.setProperty("registration." + username + ".token", token);
        authProperties.setProperty("registration." + username + ".expiry", expiryStr);
        if (inviteCode != null) {
            authProperties.setProperty("registration." + username + ".invite", inviteCode);
        }
        saveProperties();
        
        // Log registration attempt
        logSecurityEvent("REGISTRATION", username, "Registration initiated for " + email);
        
        return token;
    }
    
    /**
     * Checks if the given invite code is valid.
     * 
     * @param inviteCode the invite code to check
     * @return true if the invite code is valid
     */
    private boolean isValidInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.isEmpty()) {
            return false;
        }
        
        // Check against stored invite codes
        for (String key : authProperties.stringPropertyNames()) {
            if (key.startsWith("invite.code.") && 
                inviteCode.equals(authProperties.getProperty(key))) {
                
                // Check if the invite has been used
                String usedKey = key + ".used";
                if (!"true".equals(authProperties.getProperty(usedKey, "false"))) {
                    // Mark the invite as used
                    authProperties.setProperty(usedKey, "true");
                    authProperties.setProperty(key + ".usedAt", 
                                      java.time.Instant.now().toString());
                    saveProperties();
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Generates a new invite code.
     * This can only be done by an admin.
     * 
     * @param generatedBy the username of the admin generating the invite
     * @return the generated invite code, or null if the user is not an admin
     */
    public String generateInviteCode(String generatedBy) {
        initialize();
        
        // Verify the user is an admin
        if (!isUserAdmin(generatedBy)) {
            return null;
        }
        
        // Generate a unique invite code
        String inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Store the invite code
        String key = "invite.code." + System.currentTimeMillis();
        authProperties.setProperty(key, inviteCode);
        authProperties.setProperty(key + ".createdBy", generatedBy);
        authProperties.setProperty(key + ".createdAt", java.time.Instant.now().toString());
        saveProperties();
        
        // Log invite code generation
        logSecurityEvent("INVITE_GENERATED", generatedBy, "Generated invite code: " + inviteCode);
        
        return inviteCode;
    }
    
    /**
     * Check if a user has admin role.
     * 
     * @param username the username to check
     * @return true if the user is an admin
     */
    public boolean isUserAdmin(String username) {
        initialize();
        
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        String role = authProperties.getProperty("user." + username + ".role");
        return "admin".equals(role);
    }
    
    /**
     * Check if this is the first time setup of the system (no users exist).
     * 
     * @return true if this is the first time setup
     */
    private boolean isFirstTimeSetup() {
        // If system.setup.completed property exists, this is not first time setup
        if ("true".equals(authProperties.getProperty("system.setup.completed"))) {
            return false;
        }
        
        // Check if any users exist
        for (String key : authProperties.stringPropertyNames()) {
            if (key.endsWith(".role")) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if running in development environment.
     * This is used to enable special handling for dev/test user accounts.
     * 
     * @return true if running in development environment
     */
    private boolean isDevEnvironment() {
        // Check for development environment flag
        return "true".equals(System.getProperty("rinna.dev", "false")) ||
               "true".equals(authProperties.getProperty("system.dev.mode", "false"));
    }
    
    /**
     * Log a security event for auditing purposes.
     * 
     * @param eventType the type of security event
     * @param username the username associated with the event
     * @param details additional details about the event
     */
    private void logSecurityEvent(String eventType, String username, String details) {
        String timestamp = java.time.Instant.now().toString();
        String logEntry = timestamp + "|" + eventType + "|" + username + "|" + details;
        
        // Get security log file path
        String logPath = CONFIG_DIR + "/security.log";
        
        // Append to log file
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(CONFIG_DIR));
            java.nio.file.Files.write(
                java.nio.file.Paths.get(logPath),
                (logEntry + System.lineSeparator()).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Error writing to security log: " + e.getMessage());
        }
    }
}