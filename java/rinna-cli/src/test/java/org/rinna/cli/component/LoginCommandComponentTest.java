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
import org.rinna.cli.command.LoginCommand;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;

/**
 * Component integration tests for the LoginCommand.
 * These tests verify the integration between LoginCommand, SecurityManager, and MetadataService.
 */
@DisplayName("LoginCommand Component Integration Tests")
public class LoginCommandComponentTest {

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
        @DisplayName("Should verify the integration between LoginCommand and SecurityManager for successful login")
        void shouldVerifyIntegrationBetweenLoginCommandAndSecurityManagerForSuccessfulLogin() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            when(mockSecurityManager.login("testuser", "testpass")).thenReturn(true);
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LoginCommand command = new LoginCommand();
            command.setUsername("testuser");
            command.setPassword("testpass");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockSecurityManager).login("testuser", "testpass");
            verify(mockSecurityManager).isAdmin();
        }
        
        @Test
        @DisplayName("Should verify the integration between LoginCommand and SecurityManager for failed login")
        void shouldVerifyIntegrationBetweenLoginCommandAndSecurityManagerForFailedLogin() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            when(mockSecurityManager.login("testuser", "wrongpass")).thenReturn(false);
            
            LoginCommand command = new LoginCommand();
            command.setUsername("testuser");
            command.setPassword("wrongpass");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            verify(mockSecurityManager).login("testuser", "wrongpass");
            verify(mockSecurityManager, never()).isAdmin();
        }
        
        @Test
        @DisplayName("Should verify user authentication check integration")
        void shouldVerifyUserAuthenticationCheckIntegration() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("currentuser");
            when(mockSecurityManager.isAdmin()).thenReturn(true);
            
            LoginCommand command = new LoginCommand();
            // No username provided
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockSecurityManager).isAuthenticated();
            verify(mockSecurityManager).getCurrentUser();
            verify(mockSecurityManager).isAdmin();
            verify(mockSecurityManager, never()).login(anyString(), anyString());
        }
        
        @Test
        @DisplayName("Should verify user switching integration")
        void shouldVerifyUserSwitchingIntegration() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("olduser");
            when(mockSecurityManager.login("newuser", "newpass")).thenReturn(true);
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LoginCommand command = new LoginCommand();
            command.setUsername("newuser");
            command.setPassword("newpass");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockSecurityManager).logout();
            verify(mockSecurityManager).login("newuser", "newpass");
        }
    }
    
    @Nested
    @DisplayName("MetadataService Integration Tests")
    class MetadataServiceIntegrationTests {
        
        @Test
        @DisplayName("Should verify operation tracking for successful login")
        void shouldVerifyOperationTrackingForSuccessfulLogin() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            when(mockSecurityManager.login("testuser", "testpass")).thenReturn(true);
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LoginCommand command = new LoginCommand();
            command.setUsername("testuser");
            command.setPassword("testpass");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("login"), eq("AUTHENTICATION"), operationParamsCaptor.capture());
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), operationResultCaptor.capture());
            
            // Verify operation parameters
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertNotNull(params);
            assertEquals("testuser", params.get("username"));
            
            // Verify operation result
            Map<String, Object> result = (Map<String, Object>) operationResultCaptor.getValue();
            assertNotNull(result);
            assertEquals("testuser", result.get("username"));
            assertEquals("success", result.get("status"));
            assertEquals(false, result.get("admin"));
        }
        
        @Test
        @DisplayName("Should verify operation tracking for failed login")
        void shouldVerifyOperationTrackingForFailedLogin() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            when(mockSecurityManager.login("testuser", "wrongpass")).thenReturn(false);
            
            LoginCommand command = new LoginCommand();
            command.setUsername("testuser");
            command.setPassword("wrongpass");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("login"), eq("AUTHENTICATION"), operationParamsCaptor.capture());
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), operationExceptionCaptor.capture());
            
            // Verify operation parameters
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertNotNull(params);
            assertEquals("testuser", params.get("username"));
            
            // Verify operation exception
            Throwable exception = operationExceptionCaptor.getValue();
            assertNotNull(exception);
            assertTrue(exception instanceof SecurityException);
        }
        
        @Test
        @DisplayName("Should verify operation tracking for already logged in user")
        void shouldVerifyOperationTrackingForAlreadyLoggedInUser() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("currentuser");
            when(mockSecurityManager.isAdmin()).thenReturn(true);
            
            LoginCommand command = new LoginCommand();
            // No username provided
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("login"), eq("AUTHENTICATION"), operationParamsCaptor.capture());
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), operationResultCaptor.capture());
            
            // Verify operation parameters
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertNotNull(params);
            assertEquals("", params.get("username"));
            
            // Verify operation result
            Map<String, Object> result = (Map<String, Object>) operationResultCaptor.getValue();
            assertNotNull(result);
            assertEquals("currentuser", result.get("username"));
            assertEquals("already_logged_in", result.get("status"));
            assertEquals(true, result.get("admin"));
        }
        
        @Test
        @DisplayName("Should verify operation tracking for login exception")
        void shouldVerifyOperationTrackingForLoginException() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            when(mockSecurityManager.login(anyString(), anyString())).thenThrow(new RuntimeException("Login service unavailable"));
            
            LoginCommand command = new LoginCommand();
            command.setUsername("testuser");
            command.setPassword("testpass");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("login"), eq("AUTHENTICATION"), operationParamsCaptor.capture());
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), operationExceptionCaptor.capture());
            
            // Verify operation parameters
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertNotNull(params);
            assertEquals("testuser", params.get("username"));
            
            // Verify operation exception
            Throwable exception = operationExceptionCaptor.getValue();
            assertNotNull(exception);
            assertTrue(exception instanceof RuntimeException);
            assertEquals("Login service unavailable", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should verify text output format for successful login")
        void shouldVerifyTextOutputFormatForSuccessfulLogin() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            when(mockSecurityManager.login("testuser", "testpass")).thenReturn(true);
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LoginCommand command = new LoginCommand();
            command.setUsername("testuser");
            command.setPassword("testpass");
            command.setFormat("text");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("Successfully logged in as: testuser"));
            assertTrue(output.contains("Role: User"));
        }
        
        @Test
        @DisplayName("Should verify JSON output format for successful login")
        void shouldVerifyJsonOutputFormatForSuccessfulLogin() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            when(mockSecurityManager.login("testuser", "testpass")).thenReturn(true);
            when(mockSecurityManager.isAdmin()).thenReturn(false);
            
            LoginCommand command = new LoginCommand();
            command.setUsername("testuser");
            command.setPassword("testpass");
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("\"status\": \"success\""));
            assertTrue(output.contains("\"username\": \"testuser\""));
            assertTrue(output.contains("\"role\": \"User\""));
        }
        
        @Test
        @DisplayName("Should verify JSON output format for already logged in user")
        void shouldVerifyJsonOutputFormatForAlreadyLoggedInUser() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(true);
            when(mockSecurityManager.getCurrentUser()).thenReturn("currentuser");
            when(mockSecurityManager.isAdmin()).thenReturn(true);
            
            LoginCommand command = new LoginCommand();
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("\"status\": \"already_logged_in\""));
            assertTrue(output.contains("\"username\": \"currentuser\""));
            assertTrue(output.contains("\"role\": \"Administrator\""));
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
            
            LoginCommand command = new LoginCommand();
            command.setUsername("testuser");
            command.setPassword("testpass");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            String error = errorCaptor.toString();
            assertTrue(error.contains("Error during login: Authentication check failed"));
            
            // Verify operation tracking
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), operationExceptionCaptor.capture());
            
            // Verify operation exception
            Throwable exception = operationExceptionCaptor.getValue();
            assertNotNull(exception);
            assertTrue(exception instanceof NullPointerException);
        }
        
        @Test
        @DisplayName("Should handle invalid credentials correctly")
        void shouldHandleInvalidCredentialsCorrectly() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            when(mockSecurityManager.login("testuser", "wrongpass")).thenReturn(false);
            
            LoginCommand command = new LoginCommand();
            command.setUsername("testuser");
            command.setPassword("wrongpass");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            String error = errorCaptor.toString();
            assertTrue(error.contains("Login failed: Invalid username or password"));
        }
    }
}