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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.security.SecurityManager;

/**
 * Unit tests for the LoginCommand class.
 * 
 * This test suite follows best practices:
 * 1. Positive test cases - Testing normal successful operations
 * 2. Negative test cases - Testing error handling and failure scenarios
 * 3. Interaction tests - Testing interaction with dependencies
 */
@DisplayName("LoginCommand Tests")
class LoginCommandTest {
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;
    
    private SecurityManager mockSecurityManager;
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock security manager
        mockSecurityManager = mock(SecurityManager.class);
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout, stderr, and stdin
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
        
        // Reset output capture
        outputCaptor.reset();
        errorCaptor.reset();
    }
    
    /**
     * Tests for successful login operations.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should login successfully with provided username and password")
        void shouldLoginSuccessfullyWithProvidedUsernameAndPassword() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                when(mockSecurityManager.login("testuser", "testpass")).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(false);
                
                LoginCommand command = new LoginCommand();
                command.setUsername("testuser");
                command.setPassword("testpass");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully logged in as: testuser"));
                assertTrue(output.contains("Role: User"));
                
                verify(mockSecurityManager).login("testuser", "testpass");
            }
        }
        
        @Test
        @DisplayName("Should report admin role when user is an admin")
        void shouldReportAdminRoleWhenUserIsAdmin() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                when(mockSecurityManager.login("admin", "adminpass")).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                LoginCommand command = new LoginCommand();
                command.setUsername("admin");
                command.setPassword("adminpass");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully logged in as: admin"));
                assertTrue(output.contains("Role: Administrator"));
                
                verify(mockSecurityManager).login("admin", "adminpass");
            }
        }
        
        @Test
        @DisplayName("Should show current user info when already authenticated and no username provided")
        void shouldShowCurrentUserInfoWhenAlreadyAuthenticatedAndNoUsernameProvided() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.getCurrentUser()).thenReturn("currentuser");
                when(mockSecurityManager.isAdmin()).thenReturn(false);
                
                LoginCommand command = new LoginCommand();
                // No username provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("You are already logged in as: currentuser"));
                assertTrue(output.contains("Role: User"));
                
                verify(mockSecurityManager, never()).login(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should do nothing when already logged in as the requested user")
        void shouldDoNothingWhenAlreadyLoggedInAsRequestedUser() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
                
                LoginCommand command = new LoginCommand();
                command.setUsername("testuser");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("You are already logged in as: testuser"));
                
                verify(mockSecurityManager, never()).login(anyString(), anyString());
                verify(mockSecurityManager, never()).logout();
            }
        }
        
        @Test
        @DisplayName("Should logout previous user and login new user when changing users")
        void shouldLogoutPreviousUserAndLoginNewUserWhenChangingUsers() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.getCurrentUser()).thenReturn("olduser");
                when(mockSecurityManager.login("newuser", "newpass")).thenReturn(true);
                
                LoginCommand command = new LoginCommand();
                command.setUsername("newuser");
                command.setPassword("newpass");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Logging out current user: olduser"));
                assertTrue(output.contains("Successfully logged in as: newuser"));
                
                verify(mockSecurityManager).logout();
                verify(mockSecurityManager).login("newuser", "newpass");
            }
        }
    }
    
    /**
     * Tests for error handling and failure scenarios.
     */
    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("Should fail login with invalid credentials")
        void shouldFailLoginWithInvalidCredentials() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                when(mockSecurityManager.login("wronguser", "wrongpass")).thenReturn(false);
                
                LoginCommand command = new LoginCommand();
                command.setUsername("wronguser");
                command.setPassword("wrongpass");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Login failed: Invalid username or password"));
                
                verify(mockSecurityManager).login("wronguser", "wrongpass");
            }
        }
        
        @Test
        @DisplayName("Should handle exception during login")
        void shouldHandleExceptionDuringLogin() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                when(mockSecurityManager.login(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Login service unavailable"));
                
                LoginCommand command = new LoginCommand();
                command.setUsername("testuser");
                command.setPassword("testpass");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Login failed: Login service unavailable"));
                
                verify(mockSecurityManager).login("testuser", "testpass");
            }
        }
        
        @Test
        @DisplayName("Should handle SecurityManager getInstance returning null")
        void shouldHandleSecurityManagerInstanceReturningNull() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(null);
                
                LoginCommand command = new LoginCommand();
                command.setUsername("testuser");
                command.setPassword("testpass");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Login failed: Security service unavailable"));
            }
        }
        
        @Test
        @DisplayName("Should handle null pointer exception during authentication check")
        void shouldHandleNullPointerExceptionDuringAuthenticationCheck() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenThrow(new NullPointerException("NPE during check"));
                
                LoginCommand command = new LoginCommand();
                command.setUsername("testuser");
                command.setPassword("testpass");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Login failed: NPE during check"));
                
                verify(mockSecurityManager, never()).login(anyString(), anyString());
            }
        }
    }
    
    /**
     * Tests for command's interaction with console and input.
     */
    @Nested
    @DisplayName("Interaction Tests")
    class InteractionTests {
        
        @Test
        @DisplayName("Should prompt for username when not provided")
        void shouldPromptForUsernameWhenNotProvided() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                when(mockSecurityManager.login("prompteduser", "promptedpass")).thenReturn(true);
                
                // Simulate console input for username and password
                String simulatedInput = "prompteduser\npromptedpass\n";
                System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
                
                LoginCommand command = new LoginCommand();
                // No username or password provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Username: "));
                assertTrue(output.contains("Successfully logged in as: prompteduser"));
                
                verify(mockSecurityManager).login("prompteduser", "promptedpass");
            }
        }
        
        @Test
        @DisplayName("Should interact correctly when username is provided but password is not")
        void shouldInteractCorrectlyWhenUsernameProvidedButPasswordIsNot() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                when(mockSecurityManager.login("provideduser", "promptedpass")).thenReturn(true);
                
                // Simulate console input for password only
                String simulatedInput = "promptedpass\n";
                System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
                
                LoginCommand command = new LoginCommand();
                command.setUsername("provideduser");
                // No password provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Password: "));
                assertTrue(output.contains("Successfully logged in as: provideduser"));
                
                verify(mockSecurityManager).login("provideduser", "promptedpass");
            }
        }
    }
}