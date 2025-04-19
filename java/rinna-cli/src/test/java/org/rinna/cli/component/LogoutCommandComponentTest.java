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
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.LogoutCommand;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;

/**
 * Component integration tests for the LogoutCommand.
 * These tests verify the integration between LogoutCommand, SecurityManager, and MetadataService.
 */
@DisplayName("LogoutCommand Component Integration Tests")
public class LogoutCommandComponentTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private SecurityManager mockSecurityManager;
    
    private MockedStatic<ServiceManager> serviceManagerMock;
    private MockedStatic<SecurityManager> securityManagerMock;
    
    private static final String OPERATION_ID = "test-operation-id";
    private ArgumentCaptor<Map<String, Object>> operationParamsCaptor;
    private ArgumentCaptor<Object> operationResultCaptor;
    private ArgumentCaptor<Throwable> operationExceptionCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Set up ServiceManager mock
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up operation tracking
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn(OPERATION_ID);
        
        // Set up mocked statics
        serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
        serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        securityManagerMock = Mockito.mockStatic(SecurityManager.class);
        securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
        
        // Set up argument captors
        operationParamsCaptor = ArgumentCaptor.forClass(Map.class);
        operationResultCaptor = ArgumentCaptor.forClass(Map.class);
        operationExceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close static mocks
        serviceManagerMock.close();
        securityManagerMock.close();
    }

    @Nested
    @DisplayName("Security Integration Tests")
    class SecurityIntegrationTests {
        
        @Test
        @DisplayName("Should verify the integration between LogoutCommand and SecurityManager for authenticated user")
        void shouldVerifyIntegrationBetweenLogoutCommandAndSecurityManagerForAuthenticatedUser() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LogoutCommand command = new LogoutCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockSecurityManager).isAuthenticated();
            verify(mockSecurityManager).getCurrentUser();
            verify(mockSecurityManager).isAdmin();
            verify(mockSecurityManager).logout();
        }
        
        @Test
        @DisplayName("Should verify the integration between LogoutCommand and SecurityManager for unauthenticated user")
        void shouldVerifyIntegrationBetweenLogoutCommandAndSecurityManagerForUnauthenticatedUser() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            
            LogoutCommand command = new LogoutCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockSecurityManager).isAuthenticated();
            verify(mockSecurityManager, never()).getCurrentUser();
            verify(mockSecurityManager, never()).isAdmin();
            verify(mockSecurityManager, never()).logout();
        }
        
        @Test
        @DisplayName("Should verify interaction with SecurityManager when it throws an exception")
        void shouldVerifyInteractionWithSecurityManagerWhenItThrowsAnException() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenThrow(new RuntimeException("Security service unavailable"));
            
            LogoutCommand command = new LogoutCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            verify(mockSecurityManager).isAuthenticated();
            verify(mockSecurityManager, never()).getCurrentUser();
            verify(mockSecurityManager, never()).isAdmin();
            verify(mockSecurityManager, never()).logout();
        }
    }
    
    @Nested
    @DisplayName("MetadataService Integration Tests")
    class MetadataServiceIntegrationTests {
        
        @Test
        @DisplayName("Should verify operation tracking for successful logout")
        void shouldVerifyOperationTrackingForSuccessfulLogout() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LogoutCommand command = new LogoutCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("logout"), eq("AUTHENTICATION"), operationParamsCaptor.capture());
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), operationResultCaptor.capture());
            
            // Verify operation parameters
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertNotNull(params);
            assertEquals("text", params.get("format"));
            
            // Verify operation result
            Map<String, Object> result = (Map<String, Object>) operationResultCaptor.getValue();
            assertNotNull(result);
            assertEquals("testuser", result.get("username"));
            assertEquals("logged_out", result.get("status"));
            assertEquals(false, result.get("was_admin"));
        }
        
        @Test
        @DisplayName("Should verify operation tracking for not logged in case")
        void shouldVerifyOperationTrackingForNotLoggedInCase() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            
            LogoutCommand command = new LogoutCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("logout"), eq("AUTHENTICATION"), operationParamsCaptor.capture());
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), operationResultCaptor.capture());
            
            // Verify operation parameters
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertNotNull(params);
            assertEquals("text", params.get("format"));
            
            // Verify operation result
            Map<String, Object> result = (Map<String, Object>) operationResultCaptor.getValue();
            assertNotNull(result);
            assertEquals("not_logged_in", result.get("status"));
        }
        
        @Test
        @DisplayName("Should verify operation tracking for exception case")
        void shouldVerifyOperationTrackingForExceptionCase() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenThrow(new RuntimeException("Security service unavailable"));
            
            LogoutCommand command = new LogoutCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("logout"), eq("AUTHENTICATION"), operationParamsCaptor.capture());
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), operationExceptionCaptor.capture());
            
            // Verify operation parameters
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertNotNull(params);
            assertEquals("text", params.get("format"));
            
            // Verify operation exception
            Throwable exception = operationExceptionCaptor.getValue();
            assertNotNull(exception);
            assertTrue(exception instanceof RuntimeException);
            assertEquals("Security service unavailable", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should verify text output format for successful logout")
        void shouldVerifyTextOutputFormatForSuccessfulLogout() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LogoutCommand command = new LogoutCommand();
            command.setFormat("text");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("Successfully logged out user: testuser"));
            assertFalse(output.contains("Previous role")); // Not in verbose mode
        }
        
        @Test
        @DisplayName("Should verify text output format with verbose information")
        void shouldVerifyTextOutputFormatWithVerboseInformation() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
            when(mockSecurityManager.isAdmin()).thenReturn(true);
            
            LogoutCommand command = new LogoutCommand();
            command.setFormat("text");
            command.setVerbose(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("Successfully logged out user: testuser"));
            assertTrue(output.contains("Previous role: Administrator"));
            assertTrue(output.contains("Session terminated at:"));
        }
        
        @Test
        @DisplayName("Should verify JSON output format for successful logout")
        void shouldVerifyJsonOutputFormatForSuccessfulLogout() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LogoutCommand command = new LogoutCommand();
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("\"status\": \"logged_out\""));
            assertTrue(output.contains("\"username\": \"testuser\""));
            assertFalse(output.contains("\"role\":")); // Not in verbose mode
        }
        
        @Test
        @DisplayName("Should verify JSON output format with verbose information")
        void shouldVerifyJsonOutputFormatWithVerboseInformation() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LogoutCommand command = new LogoutCommand();
            command.setFormat("json");
            command.setVerbose(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("\"status\": \"logged_out\""));
            assertTrue(output.contains("\"username\": \"testuser\""));
            assertTrue(output.contains("\"role\": \"User\""));
            assertTrue(output.contains("\"timestamp\":"));
        }
        
        @Test
        @DisplayName("Should verify text output for not logged in case")
        void shouldVerifyTextOutputForNotLoggedInCase() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            
            LogoutCommand command = new LogoutCommand();
            command.setFormat("text");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("You are not currently logged in."));
        }
        
        @Test
        @DisplayName("Should verify JSON output for not logged in case")
        void shouldVerifyJsonOutputForNotLoggedInCase() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            
            LogoutCommand command = new LogoutCommand();
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("\"status\": \"not_logged_in\""));
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle SecurityManager exception gracefully")
        void shouldHandleSecurityManagerExceptionGracefully() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenThrow(new NullPointerException("Authentication check failed"));
            
            LogoutCommand command = new LogoutCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            String error = errorCaptor.toString();
            assertTrue(error.contains("Error during logout: Authentication check failed"));
            
            // Verify operation tracking
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), operationExceptionCaptor.capture());
            
            // Verify operation exception
            Throwable exception = operationExceptionCaptor.getValue();
            assertNotNull(exception);
            assertTrue(exception instanceof NullPointerException);
        }
        
        @Test
        @DisplayName("Should handle exception during logout with verbose information")
        void shouldHandleExceptionDuringLogoutWithVerboseInformation() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
            doThrow(new RuntimeException("Logout failed"))
                .when(mockSecurityManager).logout();
                
            LogoutCommand command = new LogoutCommand();
            command.setVerbose(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            String error = errorCaptor.toString();
            assertTrue(error.contains("Error during logout: Logout failed"));
            
            // Verify operation tracking
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), operationExceptionCaptor.capture());
            
            // Verify operation exception
            Throwable exception = operationExceptionCaptor.getValue();
            assertNotNull(exception);
            assertTrue(exception instanceof RuntimeException);
            assertEquals("Logout failed", exception.getMessage());
        }
    }
}