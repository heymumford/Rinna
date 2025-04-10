/*
 * UserAccessCommandTest for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.security.SecurityManager;

/**
 * Unit tests for the UserAccessCommand class.
 * 
 * This test suite follows best practices:
 * 1. Authentication Tests - Testing proper authentication checks
 * 2. Authorization Tests - Testing admin-only access
 * 3. Action Tests - Testing each permission/admin action
 * 4. Input Validation Tests - Testing required fields
 * 5. Help Tests - Testing help display
 */
@DisplayName("UserAccessCommand Tests")
class UserAccessCommandTest {
    
    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.err;
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    
    private SecurityManager mockSecurityManager;
    
    @BeforeEach
    void setUp() {
        // Redirect stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Create mocks
        mockSecurityManager = mock(SecurityManager.class);
        
        // Configure default mock behavior
        when(mockSecurityManager.isAuthenticated()).thenReturn(true);
        when(mockSecurityManager.isAdmin()).thenReturn(true);
        when(mockSecurityManager.getCurrentUser()).thenReturn("admin");
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout and stderr
        System.setOut(standardOut);
        System.setErr(standardErr);
    }
    
    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {
        
        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-permission");
                command.setUsername("testuser");
                command.setPermission("read");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Authentication required."), 
                    "Error should indicate authentication required");
                assertTrue(error.contains("Use 'rin login' to authenticate."), 
                    "Error should suggest using login command");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager, never()).isAdmin();
                verify(mockSecurityManager, never()).grantPermission(anyString(), anyString());
            }
        }
    }
    
    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {
        
        @Test
        @DisplayName("Should require admin privileges")
        void shouldRequireAdminPrivileges() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(false);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-permission");
                command.setUsername("testuser");
                command.setPermission("read");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Administrative privileges required for user access management."), 
                    "Error should indicate admin privileges required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).grantPermission(anyString(), anyString());
            }
        }
    }
    
    @Nested
    @DisplayName("Action Tests")
    class ActionTests {
        
        @Test
        @DisplayName("Should grant permission successfully")
        void shouldGrantPermissionSuccessfully() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.grantPermission("testuser", "read")).thenReturn(true);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-permission");
                command.setUsername("testuser");
                command.setPermission("read");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully granted permission 'read' to user 'testuser'."), 
                    "Output should confirm permission granted");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).grantPermission("testuser", "read");
            }
        }
        
        @Test
        @DisplayName("Should handle failed permission grant")
        void shouldHandleFailedPermissionGrant() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.grantPermission("testuser", "read")).thenReturn(false);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-permission");
                command.setUsername("testuser");
                command.setPermission("read");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Failed to grant permission. User may not exist."), 
                    "Error should indicate permission grant failure");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).grantPermission("testuser", "read");
            }
        }
        
        @Test
        @DisplayName("Should revoke permission successfully")
        void shouldRevokePermissionSuccessfully() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.revokePermission("testuser", "read")).thenReturn(true);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("revoke-permission");
                command.setUsername("testuser");
                command.setPermission("read");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully revoked permission 'read' from user 'testuser'."), 
                    "Output should confirm permission revoked");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).revokePermission("testuser", "read");
            }
        }
        
        @Test
        @DisplayName("Should handle failed permission revocation")
        void shouldHandleFailedPermissionRevocation() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.revokePermission("testuser", "read")).thenReturn(false);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("revoke-permission");
                command.setUsername("testuser");
                command.setPermission("read");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Failed to revoke permission. User may not exist."), 
                    "Error should indicate permission revocation failure");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).revokePermission("testuser", "read");
            }
        }
        
        @Test
        @DisplayName("Should grant admin access successfully")
        void shouldGrantAdminAccessSuccessfully() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.grantAdminAccess("testuser", "reports")).thenReturn(true);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-admin");
                command.setUsername("testuser");
                command.setArea("reports");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully granted admin access for area 'reports' to user 'testuser'."), 
                    "Output should confirm admin access granted");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).grantAdminAccess("testuser", "reports");
            }
        }
        
        @Test
        @DisplayName("Should handle failed admin access grant")
        void shouldHandleFailedAdminAccessGrant() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.grantAdminAccess("testuser", "reports")).thenReturn(false);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-admin");
                command.setUsername("testuser");
                command.setArea("reports");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Failed to grant admin access. User may not exist."), 
                    "Error should indicate admin access grant failure");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).grantAdminAccess("testuser", "reports");
            }
        }
        
        @Test
        @DisplayName("Should revoke admin access successfully")
        void shouldRevokeAdminAccessSuccessfully() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.revokeAdminAccess("testuser", "reports")).thenReturn(true);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("revoke-admin");
                command.setUsername("testuser");
                command.setArea("reports");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully revoked admin access for area 'reports' from user 'testuser'."), 
                    "Output should confirm admin access revoked");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).revokeAdminAccess("testuser", "reports");
            }
        }
        
        @Test
        @DisplayName("Should handle failed admin access revocation")
        void shouldHandleFailedAdminAccessRevocation() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.revokeAdminAccess("testuser", "reports")).thenReturn(false);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("revoke-admin");
                command.setUsername("testuser");
                command.setArea("reports");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Failed to revoke admin access. User may not exist."), 
                    "Error should indicate admin access revocation failure");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).revokeAdminAccess("testuser", "reports");
            }
        }
        
        @Test
        @DisplayName("Should promote user to admin successfully")
        void shouldPromoteUserToAdminSuccessfully() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.promoteToAdmin("testuser")).thenReturn(true);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("promote");
                command.setUsername("testuser");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully promoted user 'testuser' to administrator role."), 
                    "Output should confirm user promotion");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).promoteToAdmin("testuser");
            }
        }
        
        @Test
        @DisplayName("Should handle failed user promotion")
        void shouldHandleFailedUserPromotion() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.promoteToAdmin("testuser")).thenReturn(false);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("promote");
                command.setUsername("testuser");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Failed to promote user. User may not exist."), 
                    "Error should indicate user promotion failure");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).promoteToAdmin("testuser");
            }
        }
    }
    
    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {
        
        @Test
        @DisplayName("Should require username for grant-permission action")
        void shouldRequireUsernameForGrantPermissionAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-permission");
                // No username provided
                command.setPermission("read");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Username required."), 
                    "Error should indicate username is required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).grantPermission(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should require permission for grant-permission action")
        void shouldRequirePermissionForGrantPermissionAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-permission");
                command.setUsername("testuser");
                // No permission provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Permission required."), 
                    "Error should indicate permission is required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).grantPermission(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should require username for revoke-permission action")
        void shouldRequireUsernameForRevokePermissionAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("revoke-permission");
                // No username provided
                command.setPermission("read");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Username required."), 
                    "Error should indicate username is required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).revokePermission(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should require permission for revoke-permission action")
        void shouldRequirePermissionForRevokePermissionAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("revoke-permission");
                command.setUsername("testuser");
                // No permission provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Permission required."), 
                    "Error should indicate permission is required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).revokePermission(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should require username for grant-admin action")
        void shouldRequireUsernameForGrantAdminAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-admin");
                // No username provided
                command.setArea("reports");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Username required."), 
                    "Error should indicate username is required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).grantAdminAccess(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should require area for grant-admin action")
        void shouldRequireAreaForGrantAdminAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("grant-admin");
                command.setUsername("testuser");
                // No area provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Administrative area required."), 
                    "Error should indicate administrative area is required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).grantAdminAccess(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should require username for revoke-admin action")
        void shouldRequireUsernameForRevokeAdminAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("revoke-admin");
                // No username provided
                command.setArea("reports");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Username required."), 
                    "Error should indicate username is required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).revokeAdminAccess(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should require area for revoke-admin action")
        void shouldRequireAreaForRevokeAdminAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("revoke-admin");
                command.setUsername("testuser");
                // No area provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Administrative area required."), 
                    "Error should indicate administrative area is required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).revokeAdminAccess(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should require username for promote action")
        void shouldRequireUsernameForPromoteAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("promote");
                // No username provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Username required."), 
                    "Error should indicate username is required");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).promoteToAdmin(anyString());
            }
        }
        
        @Test
        @DisplayName("Should require a valid action")
        void shouldRequireValidAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("invalid-action");
                command.setUsername("testuser");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Unknown action: invalid-action"), 
                    "Error should indicate unknown action");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).grantPermission(anyString(), anyString());
                verify(mockSecurityManager, never()).revokePermission(anyString(), anyString());
                verify(mockSecurityManager, never()).grantAdminAccess(anyString(), anyString());
                verify(mockSecurityManager, never()).revokeAdminAccess(anyString(), anyString());
                verify(mockSecurityManager, never()).promoteToAdmin(anyString());
            }
        }
        
        @Test
        @DisplayName("Should display help when no action is provided")
        void shouldDisplayHelpWhenNoActionIsProvided() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                // No action provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Usage: rin access <action> [options]"), 
                    "Output should include usage information");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
            }
        }
    }
    
    @Nested
    @DisplayName("Help Tests")
    class HelpTests {
        
        @Test
        @DisplayName("Should display help information for help action")
        void shouldDisplayHelpInformationForHelpAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                UserAccessCommand command = new UserAccessCommand();
                command.setAction("help");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Usage: rin access <action> [options]"), 
                    "Output should include usage information");
                assertTrue(output.contains("Actions:"), 
                    "Output should list available actions");
                assertTrue(output.contains("grant-permission"), 
                    "Output should include grant-permission action");
                assertTrue(output.contains("revoke-permission"), 
                    "Output should include revoke-permission action");
                assertTrue(output.contains("grant-admin"), 
                    "Output should include grant-admin action");
                assertTrue(output.contains("revoke-admin"), 
                    "Output should include revoke-admin action");
                assertTrue(output.contains("promote"), 
                    "Output should include promote action");
                assertTrue(output.contains("Examples:"), 
                    "Output should include examples");
                
                // Verify mock interactions
                verify(mockSecurityManager).isAuthenticated();
                verify(mockSecurityManager).isAdmin();
            }
        }
    }
}