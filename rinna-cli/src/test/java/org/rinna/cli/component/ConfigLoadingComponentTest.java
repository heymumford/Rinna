/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rinna.cli.command.LoginCommand;
import org.rinna.cli.command.LogoutCommand;
import org.rinna.cli.config.SecurityConfig;

/**
 * Component tests for configuration loading capabilities.
 * Tests the interaction between commands and configuration systems.
 */
@Tag("component")
@DisplayName("Configuration Loading Component Tests")
class ConfigLoadingComponentTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final ByteArrayInputStream originalIn = new ByteArrayInputStream(new byte[0]);
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Create fresh streams for each test to avoid cross-test contamination
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
        
        // Reset the singleton SecurityConfig instance
        resetSecurityConfigSingleton();
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Reset the singleton SecurityConfig instance
        resetSecurityConfigSingleton();
    }
    
    /**
     * Resets the singleton instance of SecurityConfig using reflection.
     * This is necessary to ensure test isolation.
     */
    private void resetSecurityConfigSingleton() {
        try {
            Field instanceField = SecurityConfig.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Error resetting SecurityConfig singleton: " + e.getMessage());
        }
    }
    
    /**
     * Sets up a test security configuration file in the temp directory.
     * 
     * @param configFile the configuration file to create
     * @param username the username to store
     * @param token the authentication token to store
     * @param isAdmin whether the user is an admin
     * @throws IOException if an I/O error occurs
     */
    private void setupSecurityConfig(File configFile, String username, String token, boolean isAdmin) throws IOException {
        // Create .rinna directory if it doesn't exist
        Files.createDirectories(configFile.getParentFile().toPath());
        
        // Create and populate properties
        Properties properties = new Properties();
        properties.setProperty("auth.user", username);
        properties.setProperty("auth.token", token);
        properties.setProperty("auth.lastLogin", Long.toString(System.currentTimeMillis()));
        properties.setProperty("auth.isAdmin", Boolean.toString(isAdmin));
        
        // Save properties to file
        try (var outputStream = Files.newOutputStream(configFile.toPath())) {
            properties.store(outputStream, "Test Security Configuration");
        }
    }
    
    @Nested
    @DisplayName("Login Command Configuration Tests")
    class LoginCommandConfigurationTests {
        
        @Test
        @DisplayName("Should store auth token after successful login")
        void shouldStoreAuthTokenAfterSuccessfulLogin() throws IOException {
            // Setup temporary configuration directory
            Path configDir = tempDir.resolve(".rinna");
            Files.createDirectories(configDir);
            
            // Set system property to use temp directory as user home for this test
            String originalUserHome = System.getProperty("user.home");
            System.setProperty("user.home", tempDir.toString());
            
            try {
                // Setup login command
                LoginCommand loginCmd = new LoginCommand();
                loginCmd.setUsername("testuser");
                loginCmd.setPassword("password123"); // In a real test, we'd mock authentication
                
                // Set up interactive input for the password
                ByteArrayInputStream testInput = new ByteArrayInputStream("password123\n".getBytes(StandardCharsets.UTF_8));
                System.setIn(testInput);
                
                // Execute command
                int exitCode = loginCmd.call();
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Check if configuration file was created
                Path securityConfig = configDir.resolve("security.properties");
                assertTrue(Files.exists(securityConfig), "Security configuration file should be created");
                
                // Load the properties to verify content
                Properties savedProps = new Properties();
                try (var inputStream = Files.newInputStream(securityConfig)) {
                    savedProps.load(inputStream);
                }
                
                // Verify stored properties
                assertEquals("testuser", savedProps.getProperty("auth.user"), "Username should be stored");
                assertNotNull(savedProps.getProperty("auth.token"), "Auth token should be stored");
                assertNotNull(savedProps.getProperty("auth.lastLogin"), "Last login timestamp should be stored");
            } finally {
                // Restore original user home
                System.setProperty("user.home", originalUserHome);
                System.setIn(originalIn);
            }
        }
    }
    
    @Nested
    @DisplayName("Logout Command Configuration Tests")
    class LogoutCommandConfigurationTests {
        
        @Test
        @DisplayName("Should clear auth token on logout")
        void shouldClearAuthTokenOnLogout() throws IOException {
            // Setup temporary configuration directory
            Path configDir = tempDir.resolve(".rinna");
            Files.createDirectories(configDir);
            
            // Create security.properties file with test data
            File configFile = configDir.resolve("security.properties").toFile();
            setupSecurityConfig(configFile, "testuser", "test-token-123", false);
            
            // Set system property to use temp directory as user home for this test
            String originalUserHome = System.getProperty("user.home");
            System.setProperty("user.home", tempDir.toString());
            
            try {
                // Setup logout command
                LogoutCommand logoutCmd = new LogoutCommand();
                
                // Execute command
                int exitCode = logoutCmd.call();
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Load the properties to verify content after logout
                Properties savedProps = new Properties();
                try (var inputStream = Files.newInputStream(configFile.toPath())) {
                    savedProps.load(inputStream);
                }
                
                // Verify properties have been cleared
                assertNull(savedProps.getProperty("auth.user"), "Username should be cleared");
                assertNull(savedProps.getProperty("auth.token"), "Auth token should be cleared");
            } finally {
                // Restore original user home
                System.setProperty("user.home", originalUserHome);
            }
        }
    }
    
    @Nested
    @DisplayName("Security Configuration Component Tests")
    class SecurityConfigurationComponentTests {
        
        @Test
        @DisplayName("Should load existing configuration file")
        void shouldLoadExistingConfigurationFile() throws IOException {
            // Setup temporary configuration directory
            Path configDir = tempDir.resolve(".rinna");
            Files.createDirectories(configDir);
            
            // Create security.properties file with test data
            File configFile = configDir.resolve("security.properties").toFile();
            setupSecurityConfig(configFile, "testuser", "test-token-123", true);
            
            // Set system property to use temp directory as user home for this test
            String originalUserHome = System.getProperty("user.home");
            System.setProperty("user.home", tempDir.toString());
            
            try {
                // Get SecurityConfig instance which should load from the existing file
                SecurityConfig securityConfig = SecurityConfig.getInstance();
                
                // Verify configuration was loaded correctly
                assertEquals("testuser", securityConfig.getCurrentUser(), "Username should be loaded from file");
                assertEquals("test-token-123", securityConfig.getAuthToken(), "Auth token should be loaded from file");
                assertTrue(securityConfig.isAdmin(), "Admin status should be loaded from file");
            } finally {
                // Restore original user home
                System.setProperty("user.home", originalUserHome);
            }
        }
        
        @Test
        @DisplayName("Should create new configuration file when none exists")
        void shouldCreateNewConfigurationFileWhenNoneExists() {
            // Set system property to use temp directory as user home for this test
            String originalUserHome = System.getProperty("user.home");
            System.setProperty("user.home", tempDir.toString());
            
            try {
                // Get SecurityConfig instance which should create new configuration
                SecurityConfig securityConfig = SecurityConfig.getInstance();
                
                // Store test data
                securityConfig.storeAuthToken("newuser", "new-token-456");
                
                // Verify configuration directory and file were created
                Path configDir = tempDir.resolve(".rinna");
                Path configFile = configDir.resolve("security.properties");
                assertTrue(Files.exists(configDir), "Configuration directory should be created");
                assertTrue(Files.exists(configFile), "Configuration file should be created");
                
                // Load the properties to verify content
                Properties savedProps = new Properties();
                try (var inputStream = Files.newInputStream(configFile)) {
                    savedProps.load(inputStream);
                }
                
                // Verify stored properties
                assertEquals("newuser", savedProps.getProperty("auth.user"), "Username should be stored in new file");
                assertEquals("new-token-456", savedProps.getProperty("auth.token"), "Auth token should be stored in new file");
            } catch (IOException e) {
                fail("Failed to verify configuration file: " + e.getMessage());
            } finally {
                // Restore original user home
                System.setProperty("user.home", originalUserHome);
            }
        }
        
        @Test
        @DisplayName("Should update existing configuration properly")
        void shouldUpdateExistingConfigurationProperly() throws IOException {
            // Setup temporary configuration directory
            Path configDir = tempDir.resolve(".rinna");
            Files.createDirectories(configDir);
            
            // Create security.properties file with test data
            File configFile = configDir.resolve("security.properties").toFile();
            setupSecurityConfig(configFile, "olduser", "old-token-789", false);
            
            // Set system property to use temp directory as user home for this test
            String originalUserHome = System.getProperty("user.home");
            System.setProperty("user.home", tempDir.toString());
            
            try {
                // Get SecurityConfig instance which should load from the existing file
                SecurityConfig securityConfig = SecurityConfig.getInstance();
                
                // Update configuration
                securityConfig.storeAuthToken("updateduser", "updated-token-xyz");
                securityConfig.setAdminStatus(true);
                
                // Get a fresh instance to ensure changes were persisted
                resetSecurityConfigSingleton();
                SecurityConfig reloadedConfig = SecurityConfig.getInstance();
                
                // Verify updated configuration was reloaded correctly
                assertEquals("updateduser", reloadedConfig.getCurrentUser(), "Updated username should be loaded");
                assertEquals("updated-token-xyz", reloadedConfig.getAuthToken(), "Updated auth token should be loaded");
                assertTrue(reloadedConfig.isAdmin(), "Updated admin status should be loaded");
            } finally {
                // Restore original user home
                System.setProperty("user.home", originalUserHome);
            }
        }
    }
}