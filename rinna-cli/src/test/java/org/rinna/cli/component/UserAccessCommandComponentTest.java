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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.command.UserAccessCommand;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component test for the UserAccessCommand class, focusing on:
 * - Proper integration with MetadataService for operation tracking
 * - Integration with SecurityManager for user permissions management
 * - Proper authentication and authorization validation
 * - Proper handling of different actions (grant, revoke, promote)
 * - Proper output formatting based on format options
 * - Error handling and parameter validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccessCommand Component Tests")
public class UserAccessCommandComponentTest {

    private static final String OPERATION_ID = "op-12345";

    @Mock
    private ServiceManager mockServiceManager;

    @Mock
    private MetadataService mockMetadataService;

    @Mock
    private SecurityManager mockSecurityManager;

    private UserAccessCommand userAccessCommand;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        // Configure mock service manager
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);

        // Configure mock metadata service
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap())).thenReturn(OPERATION_ID);

        // Initialize the command with mocked services (we'll mock SecurityManager later)
        userAccessCommand = new UserAccessCommand(mockServiceManager);
        
        // Redirect stdout and stderr for output validation
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should integrate with MetadataService for operation tracking")
        void shouldIntegrateWithMetadataServiceForOperationTracking() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                userAccessCommand.setAction("help");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify operation tracking
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("access"), eq("SECURITY"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("help", params.get("action"));
                
                // Verify operation completion
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("help", result.get("action"));
                assertEquals(true, result.get("success"));
            }
        }
        
        @Test
        @DisplayName("Should integrate with SecurityManager for permissions management")
        void shouldIntegrateWithSecurityManagerForPermissionsManagement() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                when(mockSecurityManager.grantPermission(anyString(), anyString())).thenReturn(true);
                
                userAccessCommand.setAction("grant-permission");
                userAccessCommand.setUsername("testuser");
                userAccessCommand.setPermission("view");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify SecurityManager was called to grant permission
                verify(mockSecurityManager).grantPermission("testuser", "view");
            }
        }
        
        @Test
        @DisplayName("Should track operation failure when user is not authenticated")
        void shouldTrackOperationFailureWhenUserIsNotAuthenticated() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                
                userAccessCommand.setAction("grant-permission");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(SecurityException.class));
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Authentication required"));
            }
        }
        
        @Test
        @DisplayName("Should track operation failure when user is not admin")
        void shouldTrackOperationFailureWhenUserIsNotAdmin() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(false);
                
                userAccessCommand.setAction("grant-permission");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(SecurityException.class));
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Administrative privileges required"));
            }
        }
    }
    
    @Nested
    @DisplayName("Action Handling Tests")
    class ActionHandlingTests {
        
        @Test
        @DisplayName("Should handle grant-permission action")
        void shouldHandleGrantPermissionAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                when(mockSecurityManager.grantPermission(anyString(), anyString())).thenReturn(true);
                
                userAccessCommand.setAction("grant-permission");
                userAccessCommand.setUsername("testuser");
                userAccessCommand.setPermission("view");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify SecurityManager was called
                verify(mockSecurityManager).grantPermission("testuser", "view");
                
                // Verify operation completion with success result
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("grant-permission", result.get("action"));
                assertEquals("testuser", result.get("username"));
                assertEquals("view", result.get("permission"));
                assertEquals(true, result.get("success"));
                
                // Verify output contains success message
                String output = outputStream.toString();
                assertTrue(output.contains("Successfully granted permission"));
            }
        }
        
        @Test
        @DisplayName("Should handle revoke-permission action")
        void shouldHandleRevokePermissionAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                when(mockSecurityManager.revokePermission(anyString(), anyString())).thenReturn(true);
                
                userAccessCommand.setAction("revoke-permission");
                userAccessCommand.setUsername("testuser");
                userAccessCommand.setPermission("edit");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify SecurityManager was called
                verify(mockSecurityManager).revokePermission("testuser", "edit");
                
                // Verify operation completion with success result
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("revoke-permission", result.get("action"));
                assertEquals("testuser", result.get("username"));
                assertEquals("edit", result.get("permission"));
                assertEquals(true, result.get("success"));
                
                // Verify output contains success message
                String output = outputStream.toString();
                assertTrue(output.contains("Successfully revoked permission"));
            }
        }
        
        @Test
        @DisplayName("Should handle grant-admin action")
        void shouldHandleGrantAdminAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                when(mockSecurityManager.grantAdminAccess(anyString(), anyString())).thenReturn(true);
                
                userAccessCommand.setAction("grant-admin");
                userAccessCommand.setUsername("testuser");
                userAccessCommand.setArea("reports");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify SecurityManager was called
                verify(mockSecurityManager).grantAdminAccess("testuser", "reports");
                
                // Verify operation completion with success result
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("grant-admin", result.get("action"));
                assertEquals("testuser", result.get("username"));
                assertEquals("reports", result.get("area"));
                assertEquals(true, result.get("success"));
                
                // Verify output contains success message
                String output = outputStream.toString();
                assertTrue(output.contains("Successfully granted admin access"));
            }
        }
        
        @Test
        @DisplayName("Should handle revoke-admin action")
        void shouldHandleRevokeAdminAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                when(mockSecurityManager.revokeAdminAccess(anyString(), anyString())).thenReturn(true);
                
                userAccessCommand.setAction("revoke-admin");
                userAccessCommand.setUsername("testuser");
                userAccessCommand.setArea("reports");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify SecurityManager was called
                verify(mockSecurityManager).revokeAdminAccess("testuser", "reports");
                
                // Verify operation completion with success result
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("revoke-admin", result.get("action"));
                assertEquals("testuser", result.get("username"));
                assertEquals("reports", result.get("area"));
                assertEquals(true, result.get("success"));
                
                // Verify output contains success message
                String output = outputStream.toString();
                assertTrue(output.contains("Successfully revoked admin access"));
            }
        }
        
        @Test
        @DisplayName("Should handle promote action")
        void shouldHandlePromoteAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                when(mockSecurityManager.promoteToAdmin(anyString())).thenReturn(true);
                
                userAccessCommand.setAction("promote");
                userAccessCommand.setUsername("testuser");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify SecurityManager was called
                verify(mockSecurityManager).promoteToAdmin("testuser");
                
                // Verify operation completion with success result
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("promote", result.get("action"));
                assertEquals("testuser", result.get("username"));
                assertEquals(true, result.get("success"));
                
                // Verify output contains success message
                String output = outputStream.toString();
                assertTrue(output.contains("Successfully promoted user"));
            }
        }
        
        @Test
        @DisplayName("Should handle list action")
        void shouldHandleListAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                Map<String, Object> userData = new HashMap<>();
                userData.put("username", "testuser");
                userData.put("permissions", Arrays.asList("view", "edit"));
                userData.put("adminAreas", Arrays.asList("reports"));
                
                when(mockSecurityManager.getUserData(anyString())).thenReturn(userData);
                when(mockSecurityManager.isAdmin(anyString())).thenReturn(false);
                when(mockSecurityManager.getUserPermissions(anyString())).thenReturn(Arrays.asList("view", "edit"));
                when(mockSecurityManager.getUserAdminAreas(anyString())).thenReturn(Arrays.asList("reports"));
                
                userAccessCommand.setAction("list");
                userAccessCommand.setUsername("testuser");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify SecurityManager was called
                verify(mockSecurityManager).getUserData("testuser");
                verify(mockSecurityManager).isAdmin("testuser");
                verify(mockSecurityManager).getUserPermissions("testuser");
                verify(mockSecurityManager).getUserAdminAreas("testuser");
                
                // Verify operation completion with success result
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("list", result.get("action"));
                assertEquals("testuser", result.get("username"));
                assertEquals("text", result.get("format"));
                assertEquals(false, result.get("is_admin"));
                assertEquals(2, result.get("permission_count"));
                assertEquals(1, result.get("admin_areas_count"));
                
                // Verify output contains user data
                String output = outputStream.toString();
                assertTrue(output.contains("User access for: testuser"));
                assertTrue(output.contains("Admin: No"));
                assertTrue(output.contains("Permissions:"));
                assertTrue(output.contains("Admin Areas:"));
            }
        }
        
        @Test
        @DisplayName("Should handle help action")
        void shouldHandleHelpAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                userAccessCommand.setAction("help");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify operation completion with success result
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("help", result.get("action"));
                assertEquals(true, result.get("success"));
                
                // Verify output contains help information
                String output = outputStream.toString();
                assertTrue(output.contains("Usage: rin access <action> [options]"));
                assertTrue(output.contains("Actions:"));
                assertTrue(output.contains("Options:"));
                assertTrue(output.contains("Examples:"));
            }
        }
    }
    
    @Nested
    @DisplayName("Parameter Validation Tests")
    class ParameterValidationTests {
        
        @Test
        @DisplayName("Should validate username for grant-permission action")
        void shouldValidateUsernameForGrantPermissionAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                userAccessCommand.setAction("grant-permission");
                // No username set
                userAccessCommand.setPermission("view");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Username required"));
            }
        }
        
        @Test
        @DisplayName("Should validate permission for grant-permission action")
        void shouldValidatePermissionForGrantPermissionAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                userAccessCommand.setAction("grant-permission");
                userAccessCommand.setUsername("testuser");
                // No permission set
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Permission required"));
            }
        }
        
        @Test
        @DisplayName("Should validate area for grant-admin action")
        void shouldValidateAreaForGrantAdminAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                userAccessCommand.setAction("grant-admin");
                userAccessCommand.setUsername("testuser");
                // No area set
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Administrative area required"));
            }
        }
    }
    
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should output in text format by default for list action")
        void shouldOutputInTextFormatByDefaultForListAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                Map<String, Object> userData = new HashMap<>();
                userData.put("username", "testuser");
                userData.put("permissions", Arrays.asList("view", "edit"));
                userData.put("adminAreas", Arrays.asList("reports"));
                
                when(mockSecurityManager.getUserData(anyString())).thenReturn(userData);
                when(mockSecurityManager.isAdmin(anyString())).thenReturn(false);
                when(mockSecurityManager.getUserPermissions(anyString())).thenReturn(Arrays.asList("view", "edit"));
                when(mockSecurityManager.getUserAdminAreas(anyString())).thenReturn(Arrays.asList("reports"));
                
                userAccessCommand.setAction("list");
                userAccessCommand.setUsername("testuser");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output format is text
                String output = outputStream.toString();
                assertTrue(output.contains("User access for: testuser"));
                assertTrue(output.contains("Admin: No"));
                assertTrue(output.contains("Permissions:"));
                assertTrue(output.contains("Admin Areas:"));
            }
        }
        
        @Test
        @DisplayName("Should output in JSON format for list action when specified")
        void shouldOutputInJsonFormatForListActionWhenSpecified() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<OutputFormatter> mockedOutputFormatter = Mockito.mockStatic(OutputFormatter.class)) {
                
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                Map<String, Object> userData = new HashMap<>();
                userData.put("username", "testuser");
                userData.put("permissions", Arrays.asList("view", "edit"));
                userData.put("adminAreas", Arrays.asList("reports"));
                
                when(mockSecurityManager.getUserData(anyString())).thenReturn(userData);
                mockedOutputFormatter.when(() -> OutputFormatter.toJson(any(), anyBoolean()))
                    .thenReturn("{\"json\":\"output\"}");
                
                userAccessCommand.setAction("list");
                userAccessCommand.setUsername("testuser");
                userAccessCommand.setFormat("json");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify OutputFormatter was called
                mockedOutputFormatter.verify(() -> OutputFormatter.toJson(eq(userData), eq(false)));
                
                // Verify output contains JSON
                String output = outputStream.toString();
                assertTrue(output.contains("{\"json\":\"output\"}"));
            }
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle failed grant permission")
        void shouldHandleFailedGrantPermission() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                when(mockSecurityManager.grantPermission(anyString(), anyString())).thenReturn(false);
                
                userAccessCommand.setAction("grant-permission");
                userAccessCommand.setUsername("testuser");
                userAccessCommand.setPermission("view");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify SecurityManager was called
                verify(mockSecurityManager).grantPermission("testuser", "view");
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Failed to grant permission"));
            }
        }
        
        @Test
        @DisplayName("Should handle user not found when listing")
        void shouldHandleUserNotFoundWhenListing() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                when(mockSecurityManager.getUserData(anyString())).thenReturn(null);
                
                userAccessCommand.setAction("list");
                userAccessCommand.setUsername("nonexistent");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify SecurityManager was called
                verify(mockSecurityManager).getUserData("nonexistent");
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("User 'nonexistent' not found"));
            }
        }
        
        @Test
        @DisplayName("Should handle unknown action")
        void shouldHandleUnknownAction() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                userAccessCommand.setAction("unknown-action");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Unknown action: unknown-action"));
            }
        }
        
        @Test
        @DisplayName("Should handle exceptions during execution")
        void shouldHandleExceptionsDuringExecution() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                when(mockSecurityManager.grantPermission(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Test exception"));
                
                userAccessCommand.setAction("grant-permission");
                userAccessCommand.setUsername("testuser");
                userAccessCommand.setPermission("view");
                
                // When
                int exitCode = userAccessCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(RuntimeException.class));
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Test exception"));
            }
        }
    }
}