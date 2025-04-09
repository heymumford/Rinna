/*
 * ServerCommandTest for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ServerCommand class.
 * 
 * This test suite follows best practices:
 * 1. Subcommand Tests - Testing different server subcommands (status, start, stop, etc.)
 * 2. Input Validation Tests - Testing input validation and error scenarios
 * 3. Format Tests - Testing different output formats (text, JSON)
 * 4. Help Tests - Testing help display functionality
 */
@DisplayName("ServerCommand Tests")
class ServerCommandTest {
    
    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.err;
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    
    private ServiceManager mockServiceManager;
    private ServiceManager.ServiceStatusInfo mockStatusInfo;
    
    @BeforeEach
    void setUp() {
        // Redirect stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Create mocks
        mockServiceManager = mock(ServiceManager.class);
        mockStatusInfo = mock(ServiceManager.ServiceStatusInfo.class);
        
        // Configure default mock behavior
        when(mockStatusInfo.getState()).thenReturn("RUNNING");
        when(mockStatusInfo.isAvailable()).thenReturn(true);
        when(mockStatusInfo.getMessage()).thenReturn("Service is running");
        when(mockServiceManager.getServiceStatus(anyString())).thenReturn(mockStatusInfo);
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout and stderr
        System.setOut(standardOut);
        System.setErr(standardErr);
    }
    
    @Nested
    @DisplayName("Subcommand Tests")
    class SubcommandTests {
        
        @Test
        @DisplayName("Should show status of all services when no subcommand is provided")
        void shouldShowStatusOfAllServicesWhenNoSubcommandIsProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                // No subcommand or service name provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Rinna Services Status"), "Output should contain status header");
                assertTrue(output.contains("SERVICE"), "Output should contain column headers");
                assertTrue(output.contains("api"), "Output should contain mock service list");
                assertTrue(output.contains("database"), "Output should contain mock service list");
                assertTrue(output.contains("docs"), "Output should contain mock service list");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                verify(mockServiceManager, never()).getServiceStatus(anyString());
            }
        }
        
        @Test
        @DisplayName("Should show status of a specific service when status subcommand and service name are provided")
        void shouldShowStatusOfSpecificServiceWhenStatusSubcommandAndServiceNameAreProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("status");
                command.setServiceName("api");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Service: api"), "Output should contain service name");
                assertTrue(output.contains("Status: RUNNING"), "Output should contain status");
                assertTrue(output.contains("Available: Yes"), "Output should contain availability");
                assertTrue(output.contains("Message: Service is running"), "Output should contain message");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                verify(mockServiceManager).getServiceStatus("api");
            }
        }
        
        @Test
        @DisplayName("Should start a service when start subcommand and service name are provided")
        void shouldStartServiceWhenStartSubcommandAndServiceNameAreProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("start");
                command.setServiceName("api");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Starting service: api"), "Output should indicate service starting");
                assertTrue(output.contains("Service started successfully"), "Output should confirm service started");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                // Note: This command doesn't actually call any methods on ServiceManager for the start action
                // In a real implementation, it would likely call a startService method or similar
            }
        }
        
        @Test
        @DisplayName("Should stop a service when stop subcommand and service name are provided")
        void shouldStopServiceWhenStopSubcommandAndServiceNameAreProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("stop");
                command.setServiceName("api");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Stopping service: api"), "Output should indicate service stopping");
                assertTrue(output.contains("Service stopped successfully"), "Output should confirm service stopped");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                // Note: This command doesn't actually call any methods on ServiceManager for the stop action
                // In a real implementation, it would likely call a stopService method or similar
            }
        }
        
        @Test
        @DisplayName("Should restart a service when restart subcommand and service name are provided")
        void shouldRestartServiceWhenRestartSubcommandAndServiceNameAreProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("restart");
                command.setServiceName("api");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Restarting service: api"), "Output should indicate service restarting");
                assertTrue(output.contains("Service restarted successfully"), "Output should confirm service restarted");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                // Note: This command doesn't actually call any methods on ServiceManager for the restart action
                // In a real implementation, it would likely call a restartService method or similar
            }
        }
        
        @Test
        @DisplayName("Should configure a service when config subcommand, service name, and config path are provided")
        void shouldConfigureServiceWhenConfigSubcommandServiceNameAndConfigPathAreProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up createServiceConfig to return true (success)
                when(mockServiceManager.createServiceConfig(anyString(), anyString())).thenReturn(true);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("config");
                command.setServiceName("api");
                command.setConfigPath("/path/to/config.json");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Created configuration for api at: /path/to/config.json"), 
                    "Output should confirm configuration created");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                verify(mockServiceManager).createServiceConfig("api", "/path/to/config.json");
            }
        }
        
        @Test
        @DisplayName("Should use default config path when config subcommand and service name are provided without config path")
        void shouldUseDefaultConfigPathWhenConfigSubcommandAndServiceNameAreProvidedWithoutConfigPath() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up createServiceConfig to return true (success)
                when(mockServiceManager.createServiceConfig(anyString(), anyString())).thenReturn(true);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("config");
                command.setServiceName("api");
                // No config path provided - should use default
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Created configuration for api at:"), 
                    "Output should confirm configuration created");
                assertTrue(output.contains("/.rinna/services/api.json"), 
                    "Output should contain default config path");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                verify(mockServiceManager).createServiceConfig(eq("api"), anyString());
            }
        }
        
        @Test
        @DisplayName("Should show help information when help subcommand is provided")
        void shouldShowHelpInformationWhenHelpSubcommandIsProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("help");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Server Command Usage"), "Output should contain usage header");
                assertTrue(output.contains("Subcommands:"), "Output should list subcommands");
                assertTrue(output.contains("status"), "Output should include status subcommand");
                assertTrue(output.contains("start"), "Output should include start subcommand");
                assertTrue(output.contains("stop"), "Output should include stop subcommand");
                assertTrue(output.contains("restart"), "Output should include restart subcommand");
                assertTrue(output.contains("config"), "Output should include config subcommand");
                assertTrue(output.contains("Examples:"), "Output should include examples");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
        
        @Test
        @DisplayName("Should handle unknown subcommand gracefully")
        void shouldHandleUnknownSubcommandGracefully() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("invalid");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Unknown server subcommand: invalid"), 
                    "Error should indicate unknown subcommand");
                assertTrue(error.contains("Valid subcommands: status, start, stop, restart, config, help"), 
                    "Error should list valid subcommands");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
    }
    
    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {
        
        @Test
        @DisplayName("Should require service name for start subcommand")
        void shouldRequireServiceNameForStartSubcommand() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("start");
                // No service name provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result); // Note: The command still returns success even with validation errors
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Service name is required to start a service"), 
                    "Error should indicate service name is required");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
        
        @Test
        @DisplayName("Should require service name for stop subcommand")
        void shouldRequireServiceNameForStopSubcommand() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("stop");
                // No service name provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result); // Note: The command still returns success even with validation errors
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Service name is required to stop a service"), 
                    "Error should indicate service name is required");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
        
        @Test
        @DisplayName("Should require service name for restart subcommand")
        void shouldRequireServiceNameForRestartSubcommand() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("restart");
                // No service name provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result); // Note: The command still returns success even with validation errors
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Service name is required to restart a service"), 
                    "Error should indicate service name is required");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
        
        @Test
        @DisplayName("Should require service name for config subcommand")
        void shouldRequireServiceNameForConfigSubcommand() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("config");
                // No service name provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result); // Note: The command still returns success even with validation errors
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Service name is required to configure a service"), 
                    "Error should indicate service name is required");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                verify(mockServiceManager, never()).createServiceConfig(anyString(), anyString());
            }
        }
        
        @Test
        @DisplayName("Should handle service configuration failure gracefully")
        void shouldHandleServiceConfigurationFailureGracefully() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up createServiceConfig to return false (failure)
                when(mockServiceManager.createServiceConfig(anyString(), anyString())).thenReturn(false);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("config");
                command.setServiceName("api");
                command.setConfigPath("/path/to/config.json");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result); // Note: The command still returns success even with configuration errors
                String error = errorCaptor.toString();
                assertTrue(error.contains("Failed to create service configuration"), 
                    "Error should indicate configuration failure");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                verify(mockServiceManager).createServiceConfig("api", "/path/to/config.json");
            }
        }
        
        @Test
        @DisplayName("Should handle exceptions gracefully")
        void shouldHandleExceptionsGracefully() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenThrow(new RuntimeException("Test exception"));
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("status");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Test exception"), 
                    "Error should contain exception message");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
    }
    
    @Nested
    @DisplayName("Format Tests")
    class FormatTests {
        
        @Test
        @DisplayName("Should output service status in JSON format when requested")
        void shouldOutputServiceStatusInJsonFormatWhenRequested() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("status");
                command.setServiceName("api");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""), "Output should contain success result");
                assertTrue(output.contains("\"action\": \"status\""), "Output should contain action type");
                assertTrue(output.contains("\"name\": \"api\""), "Output should contain service name");
                assertTrue(output.contains("\"status\": \"RUNNING\""), "Output should contain service status");
                assertTrue(output.contains("\"available\": true"), "Output should contain availability");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                verify(mockServiceManager).getServiceStatus("api");
            }
        }
        
        @Test
        @DisplayName("Should output all services status in JSON format when requested")
        void shouldOutputAllServicesStatusInJsonFormatWhenRequested() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("status");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""), "Output should contain success result");
                assertTrue(output.contains("\"action\": \"status\""), "Output should contain action type");
                assertTrue(output.contains("\"services\": ["), "Output should contain services array");
                assertTrue(output.contains("\"name\": \"api\""), "Output should contain API service");
                assertTrue(output.contains("\"name\": \"database\""), "Output should contain database service");
                assertTrue(output.contains("\"name\": \"docs\""), "Output should contain docs service");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
                verify(mockServiceManager, never()).getServiceStatus(anyString());
            }
        }
        
        @Test
        @DisplayName("Should output service start result in JSON format when requested")
        void shouldOutputServiceStartResultInJsonFormatWhenRequested() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("start");
                command.setServiceName("api");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""), "Output should contain success result");
                assertTrue(output.contains("\"action\": \"start\""), "Output should contain action type");
                assertTrue(output.contains("\"service\": \"api\""), "Output should contain service name");
                assertTrue(output.contains("\"status\": \"RUNNING\""), "Output should contain service status");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
        
        @Test
        @DisplayName("Should output validation errors in JSON format when requested")
        void shouldOutputValidationErrorsInJsonFormatWhenRequested() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("start");
                // No service name provided
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"error\""), "Output should contain error result");
                assertTrue(output.contains("\"message\": \"Service name is required to start a service\""), 
                    "Output should contain error message");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
        
        @Test
        @DisplayName("Should output exception errors in JSON format when requested")
        void shouldOutputExceptionErrorsInJsonFormatWhenRequested() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenThrow(new RuntimeException("Test exception"));
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("status");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"error\""), "Output should contain error result");
                assertTrue(output.contains("\"message\": \"Test exception\""), 
                    "Output should contain exception message");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
        
        @Test
        @DisplayName("Should output help in JSON format when requested")
        void shouldOutputHelpInJsonFormatWhenRequested() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("help");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""), "Output should contain success result");
                assertTrue(output.contains("\"command\": \"server\""), "Output should contain command name");
                assertTrue(output.contains("\"usage\": \"rin server <subcommand> [options]\""), 
                    "Output should contain usage information");
                assertTrue(output.contains("\"subcommands\": ["), "Output should contain subcommands array");
                assertTrue(output.contains("\"options\": ["), "Output should contain options array");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
        
        @Test
        @DisplayName("Should include verbose output when verbose flag is set")
        void shouldIncludeVerboseOutputWhenVerboseFlagIsSet() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("status");
                // Status of all services
                command.setVerbose(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Total services: 3"), 
                    "Output should contain verbose total services count");
                assertTrue(output.contains("Running services: 2"), 
                    "Output should contain verbose running services count");
                assertTrue(output.contains("Stopped services: 1"), 
                    "Output should contain verbose stopped services count");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
        
        @Test
        @DisplayName("Should include verbose output for start command when verbose flag is set")
        void shouldIncludeVerboseOutputForStartCommandWhenVerboseFlagIsSet() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("start");
                command.setServiceName("api");
                command.setVerbose(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Starting service: api"), "Output should indicate service starting");
                assertTrue(output.contains("Service is now in RUNNING state"), 
                    "Output should contain verbose state information");
                assertTrue(output.contains("Service process ID: 12345"), 
                    "Output should contain verbose process ID information");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
    }
    
    @Nested
    @DisplayName("Help Tests")
    class HelpTests {
        
        @Test
        @DisplayName("Should display help with examples when help subcommand is provided")
        void shouldDisplayHelpWithExamplesWhenHelpSubcommandIsProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ServerCommand command = new ServerCommand();
                command.setSubcommand("help");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Server Command Usage:"), "Output should contain usage heading");
                assertTrue(output.contains("rin server [subcommand] [options]"), "Output should contain command syntax");
                assertTrue(output.contains("Subcommands:"), "Output should list subcommands");
                assertTrue(output.contains("Options:"), "Output should list options");
                assertTrue(output.contains("Examples:"), "Output should include examples");
                assertTrue(output.contains("rin server                        - Show status of all services"), 
                    "Output should include example for showing all services");
                assertTrue(output.contains("rin server status api             - Show status of the API service"), 
                    "Output should include example for showing specific service");
                assertTrue(output.contains("rin server start api              - Start the API service"), 
                    "Output should include example for starting a service");
                assertTrue(output.contains("rin server config api config.json - Configure the API service"), 
                    "Output should include example for configuring a service");
                
                // Verify service manager method calls
                serviceManagerMock.verify(ServiceManager::getInstance);
            }
        }
    }
}