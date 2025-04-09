/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    private SecurityConfig securityConfig;
    private String originalUserHome;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() throws Exception {
        // Save original user.home
        originalUserHome = System.getProperty("user.home");
        
        // Set user.home to a temporary directory for testing
        System.setProperty("user.home", tempDir.toString());
        
        // Reset the singleton instance to use our temporary directory
        resetSingleton();
        
        // Get a new instance with the temporary directory
        securityConfig = SecurityConfig.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        // Restore original user.home
        System.setProperty("user.home", originalUserHome);
        
        try {
            // Reset the singleton instance
            resetSingleton();
        } catch (Exception e) {
            System.err.println("Error resetting singleton: " + e.getMessage());
        }
    }
    
    /**
     * Reset the singleton instance using reflection.
     */
    private void resetSingleton() throws Exception {
        Field instanceField = SecurityConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
    
    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {
        @Test
        @DisplayName("Should create configuration directory if it doesn't exist")
        void shouldCreateConfigurationDirectoryIfItDoesntExist() {
            // Given the temp directory from the setup
            
            // Then the .rinna directory should be created
            Path configDir = tempDir.resolve(".rinna");
            assertTrue(Files.exists(configDir), "Configuration directory should be created");
        }
        
        @Test
        @DisplayName("Should load existing properties file if it exists")
        void shouldLoadExistingPropertiesFileIfItExists() throws IOException {
            // Given
            String username = "testuser";
            String token = "test-token";
            
            // Create .rinna directory
            Path configDir = tempDir.resolve(".rinna");
            Files.createDirectories(configDir);
            
            // Create properties file
            Path configFile = configDir.resolve("security.properties");
            Properties props = new Properties();
            props.setProperty("auth.user", username);
            props.setProperty("auth.token", token);
            
            try (java.io.OutputStreamWriter out = new java.io.OutputStreamWriter(
                    Files.newOutputStream(configFile), StandardCharsets.UTF_8)) {
                props.store(out, "Test Properties");
            }
            
            // When - force reload by resetting singleton
            resetSingleton();
            securityConfig = SecurityConfig.getInstance();
            
            // Then
            assertEquals(username, securityConfig.getCurrentUser());
            assertEquals(token, securityConfig.getAuthToken());
        }
        
        @Test
        @DisplayName("Should be singleton")
        void shouldBeSingleton() {
            // When
            SecurityConfig instance1 = SecurityConfig.getInstance();
            SecurityConfig instance2 = SecurityConfig.getInstance();
            
            // Then
            assertSame(instance1, instance2, "Multiple calls to getInstance() should return the same instance");
        }
    }
    
    @Nested
    @DisplayName("Authentication Token Management Tests")
    class AuthenticationTokenManagementTests {
        @Test
        @DisplayName("Should store and retrieve authentication token")
        void shouldStoreAndRetrieveAuthenticationToken() {
            // Given
            String username = "testuser";
            String token = "test-auth-token-123";
            
            // When
            securityConfig.storeAuthToken(username, token);
            
            // Then
            assertEquals(token, securityConfig.getAuthToken());
            assertEquals(username, securityConfig.getCurrentUser());
        }
        
        @Test
        @DisplayName("Should clear authentication token")
        void shouldClearAuthenticationToken() {
            // Given
            securityConfig.storeAuthToken("testuser", "test-token");
            
            // When
            securityConfig.clearAuthToken();
            
            // Then
            assertNull(securityConfig.getAuthToken());
            assertNull(securityConfig.getCurrentUser());
        }
        
        @Test
        @DisplayName("Should persist authentication token between instances")
        void shouldPersistAuthenticationTokenBetweenInstances() throws Exception {
            // Given
            String username = "persistent-user";
            String token = "persistent-token";
            securityConfig.storeAuthToken(username, token);
            
            // When - force reload by resetting and getting new instance
            resetSingleton();
            SecurityConfig newInstance = SecurityConfig.getInstance();
            
            // Then
            assertEquals(token, newInstance.getAuthToken());
            assertEquals(username, newInstance.getCurrentUser());
        }
    }
    
    @Nested
    @DisplayName("Admin Status Tests")
    class AdminStatusTests {
        @Test
        @DisplayName("Should default to non-admin")
        void shouldDefaultToNonAdmin() {
            // Given a new config (from setup)
            
            // Then
            assertFalse(securityConfig.isAdmin());
        }
        
        @Test
        @DisplayName("Should set and get admin status")
        void shouldSetAndGetAdminStatus() {
            // Given a new config (from setup)
            
            // When
            securityConfig.setAdminStatus(true);
            
            // Then
            assertTrue(securityConfig.isAdmin());
            
            // When changed
            securityConfig.setAdminStatus(false);
            
            // Then
            assertFalse(securityConfig.isAdmin());
        }
        
        @Test
        @DisplayName("Should persist admin status between instances")
        void shouldPersistAdminStatusBetweenInstances() throws Exception {
            // Given
            securityConfig.setAdminStatus(true);
            
            // When - force reload by resetting and getting new instance
            resetSingleton();
            SecurityConfig newInstance = SecurityConfig.getInstance();
            
            // Then
            assertTrue(newInstance.isAdmin());
        }
    }
    
    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {
        @Test
        @DisplayName("Should generate unique auth tokens")
        void shouldGenerateUniqueAuthTokens() {
            // When
            String token1 = SecurityConfig.generateAuthToken();
            String token2 = SecurityConfig.generateAuthToken();
            
            // Then
            assertNotNull(token1);
            assertNotNull(token2);
            assertNotEquals(token1, token2);
        }
        
        @Test
        @DisplayName("Should generate valid Base64 encoded auth tokens")
        void shouldGenerateValidBase64EncodedAuthTokens() {
            // When
            String token = SecurityConfig.generateAuthToken();
            
            // Then
            assertDoesNotThrow(() -> {
                byte[] decoded = Base64.getDecoder().decode(token);
                assertNotNull(decoded);
                assertTrue(decoded.length > 0);
            });
        }
    }
    
    @Nested
    @DisplayName("File Persistence Tests")
    class FilePersistenceTests {
        @Test
        @DisplayName("Should save properties to file")
        void shouldSavePropertiesToFile() {
            // Given
            String username = "filetest";
            String token = "file-test-token";
            
            // When
            securityConfig.storeAuthToken(username, token);
            
            // Then
            Path configFile = tempDir.resolve(".rinna/security.properties");
            assertTrue(Files.exists(configFile));
            
            // Verify file contents
            try {
                Properties props = new Properties();
                try (java.io.InputStreamReader in = new java.io.InputStreamReader(
                        Files.newInputStream(configFile), StandardCharsets.UTF_8)) {
                    props.load(in);
                }
                
                assertEquals(token, props.getProperty("auth.token"));
                assertEquals(username, props.getProperty("auth.user"));
                assertNotNull(props.getProperty("auth.lastLogin"));
            } catch (IOException e) {
                fail("Failed to read properties file: " + e.getMessage());
            }
        }
    }
}