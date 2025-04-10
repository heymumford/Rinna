/*
 * Security configuration for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

/**
 * Configuration handler for security-related settings and credentials.
 * Manages the storage and retrieval of authentication tokens, user data,
 * and access control settings.
 */
public final class SecurityConfig {
    private static final String CONFIG_FILE = ".rinna/security.properties";
    private static final String TOKEN_KEY = "auth.token";
    private static final String USER_KEY = "auth.user";
    private static final String LAST_LOGIN_KEY = "auth.lastLogin";
    private static final String ADMIN_STATUS_KEY = "auth.isAdmin";
    
    private static SecurityConfig instance;
    private Properties properties;
    private File configFile;
    
    /**
     * Gets the singleton instance of SecurityConfig.
     * 
     * @return the singleton instance
     */
    public static synchronized SecurityConfig getInstance() {
        if (instance == null) {
            instance = new SecurityConfig();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private SecurityConfig() {
        properties = new Properties();
        
        // Get user home directory
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, ".rinna");
        
        // Create directory if it doesn't exist
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                System.err.println("Error creating configuration directory: " + e.getMessage());
            }
        }
        
        // Initialize config file
        configFile = new File(userHome, CONFIG_FILE);
        if (configFile.exists()) {
            try (java.io.InputStreamReader in = new java.io.InputStreamReader(
                    java.nio.file.Files.newInputStream(configFile.toPath()), java.nio.charset.StandardCharsets.UTF_8)) {
                properties.load(in);
            } catch (IOException e) {
                System.err.println("Error loading security configuration: " + e.getMessage());
            }
        }
    }
    
    /**
     * Saves the current properties to the config file.
     */
    private void saveProperties() {
        try (java.io.OutputStreamWriter out = new java.io.OutputStreamWriter(
                java.nio.file.Files.newOutputStream(configFile.toPath()), java.nio.charset.StandardCharsets.UTF_8)) {
            properties.store(out, "Rinna Security Configuration");
        } catch (IOException e) {
            System.err.println("Error saving security configuration: " + e.getMessage());
        }
    }
    
    /**
     * Stores an authentication token for the given user.
     * 
     * @param username the username
     * @param token the authentication token
     */
    public void storeAuthToken(String username, String token) {
        properties.setProperty(TOKEN_KEY, token);
        properties.setProperty(USER_KEY, username);
        properties.setProperty(LAST_LOGIN_KEY, Long.toString(System.currentTimeMillis()));
        saveProperties();
    }
    
    /**
     * Gets the stored authentication token.
     * 
     * @return the authentication token, or null if not found
     */
    public String getAuthToken() {
        return properties.getProperty(TOKEN_KEY);
    }
    
    /**
     * Gets the current authenticated user.
     * 
     * @return the username, or null if not found
     */
    public String getCurrentUser() {
        return properties.getProperty(USER_KEY);
    }
    
    /**
     * Clears the stored authentication token and user.
     */
    public void clearAuthToken() {
        properties.remove(TOKEN_KEY);
        properties.remove(USER_KEY);
        saveProperties();
    }
    
    /**
     * Sets the admin status of the current user.
     * 
     * @param isAdmin true if the user is an admin, false otherwise
     */
    public void setAdminStatus(boolean isAdmin) {
        properties.setProperty(ADMIN_STATUS_KEY, Boolean.toString(isAdmin));
        saveProperties();
    }
    
    /**
     * Gets the admin status of the current user.
     * 
     * @return true if the user is an admin, false otherwise
     */
    public boolean isAdmin() {
        return "true".equals(properties.getProperty(ADMIN_STATUS_KEY, "false"));
    }
    
    /**
     * Generates a secure authentication token.
     * 
     * @return a secure authentication token
     */
    public static String generateAuthToken() {
        UUID uuid = UUID.randomUUID();
        String randomString = uuid.toString() + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(randomString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    /**
     * Hashes a password using a secure one-way hash function with salt.
     * This implementation uses PBKDF2WithHmacSHA512 with a secure salt and 10000 iterations.
     * 
     * @param password the password to hash
     * @return the hashed password with salt encoded in Base64
     */
    public static String hashPassword(String password) {
        try {
            // Generate a random salt
            java.security.SecureRandom random = new java.security.SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            // Hash the password using PBKDF2WithHmacSHA512
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), salt, 10000, 512);
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            
            // Combine salt and hash, then encode in Base64
            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // In a real system, we'd use proper error handling
            // For simplicity, we'll just return a simpler hash
            System.err.println("Warning: Secure password hashing unavailable: " + e.getMessage());
            return fallbackHashPassword(password);
        }
    }
    
    /**
     * Fallback password hashing method for environments where stronger
     * cryptographic functions are unavailable.
     * 
     * @param password the password to hash
     * @return a basic hash of the password
     */
    private static String fallbackHashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            // Last resort, very insecure but ensures the system works
            return Base64.getEncoder().encodeToString(password.getBytes());
        }
    }
    
    /**
     * Verifies a password against a stored hash.
     * 
     * @param password the password to verify
     * @param storedHash the stored hash to verify against
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            // Decode the stored hash
            byte[] combined = Base64.getDecoder().decode(storedHash);
            
            // Check if it's a PBKDF2 hash with salt (should be over 16 bytes for salt)
            if (combined.length > 32) {
                // Extract salt and hash
                byte[] salt = new byte[16];
                byte[] hash = new byte[combined.length - 16];
                System.arraycopy(combined, 0, salt, 0, 16);
                System.arraycopy(combined, 16, hash, 0, hash.length);
                
                // Generate hash from the provided password with the same salt
                javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(), salt, 10000, 8 * hash.length);
                javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                byte[] testHash = factory.generateSecret(spec).getEncoded();
                
                // Compare the hashes using constant-time comparison to prevent timing attacks
                return constantTimeEquals(hash, testHash);
            } else {
                // It's a fallback hash, verify with the fallback method
                return storedHash.equals(fallbackHashPassword(password));
            }
        } catch (Exception e) {
            // In case of any error, fall back to basic comparison
            System.err.println("Warning: Error in secure password verification: " + e.getMessage());
            return storedHash.equals(fallbackHashPassword(password));
        }
    }
    
    /**
     * Performs a constant-time comparison of two byte arrays to prevent timing attacks.
     * 
     * @param a the first byte array
     * @param b the second byte array
     * @return true if the arrays are equal, false otherwise
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i]; // XOR will be 0 for matching bytes, non-zero otherwise
        }
        
        return result == 0;
    }
}