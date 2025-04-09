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

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.security.SecurityManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the LogoutCommand class.
 * 
 * This test suite follows best practices:
 * 1. Positive Test Cases - Testing normal successful operations
 * 2. Edge Cases - Testing boundary conditions
 * 3. Error Handling - Testing error conditions
 */
@DisplayName("LogoutCommand Tests")
class LogoutCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
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
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Reset output capture
        outputCaptor.reset();
        errorCaptor.reset();
    }
    
    /**
     * Tests for successful logout operations.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should logout authenticated user successfully")
        void shouldLogoutAuthenticatedUserSuccessfully() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
                
                LogoutCommand command = new LogoutCommand();
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully logged out user: testuser"));
                
                verify(mockSecurityManager).logout();
            }
        }
        
        @Test
        @DisplayName("Should handle not being logged in gracefully")
        void shouldHandleNotBeingLoggedInGracefully() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                
                LogoutCommand command = new LogoutCommand();
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("You are not currently logged in."));
                
                verify(mockSecurityManager, never()).logout();
            }
        }
    }
    
    /**
     * Tests for edge cases.
     */
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("Should handle admin user logout correctly")
        void shouldHandleAdminUserLogoutCorrectly() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.getCurrentUser()).thenReturn("admin");
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                LogoutCommand command = new LogoutCommand();
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                verify(mockSecurityManager).logout();
            }
        }
        
        @Test
        @DisplayName("Should handle user with special characters in username")
        void shouldHandleUserWithSpecialCharactersInUsername() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.getCurrentUser()).thenReturn("user@example.com");
                
                LogoutCommand command = new LogoutCommand();
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully logged out user: user@example.com"));
                
                verify(mockSecurityManager).logout();
            }
        }
    }
    
    /**
     * Tests for error handling conditions.
     */
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {
        
        @Test
        @DisplayName("Should handle SecurityManager instance being null")
        void shouldHandleSecurityManagerInstanceBeingNull() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(null);
                
                LogoutCommand command = new LogoutCommand();
                
                // When & Then
                assertThrows(NullPointerException.class, command::call);
            }
        }
        
        @Test
        @DisplayName("Should handle exception during isAuthenticated check")
        void shouldHandleExceptionDuringIsAuthenticatedCheck() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenThrow(new RuntimeException("Authentication check failed"));
                
                LogoutCommand command = new LogoutCommand();
                
                // When & Then
                assertThrows(RuntimeException.class, command::call);
                verify(mockSecurityManager, never()).logout();
            }
        }
        
        @Test
        @DisplayName("Should handle exception during getCurrentUser")
        void shouldHandleExceptionDuringGetCurrentUser() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.getCurrentUser()).thenThrow(new RuntimeException("Get current user failed"));
                
                LogoutCommand command = new LogoutCommand();
                
                // When & Then
                assertThrows(RuntimeException.class, command::call);
                verify(mockSecurityManager, never()).logout();
            }
        }
        
        @Test
        @DisplayName("Should handle exception during logout")
        void shouldHandleExceptionDuringLogout() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
                doThrow(new RuntimeException("Logout failed")).when(mockSecurityManager).logout();
                
                LogoutCommand command = new LogoutCommand();
                
                // When & Then
                assertThrows(RuntimeException.class, command::call);
                verify(mockSecurityManager).logout();
            }
        }
    }
}