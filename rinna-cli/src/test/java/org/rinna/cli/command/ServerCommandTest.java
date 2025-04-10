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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.ServiceManager.ServiceStatusInfo;

/**
 * Unit tests for the ServerCommand class.
 * These tests verify that ServerCommand correctly manages server operations
 * and properly tracks these operations with MetadataService.
 */
@ExtendWith(MockitoExtension.class)
class ServerCommandTest {

    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ConfigurationService configService;
    
    @Mock
    private MetadataService metadataService;
    
    @Mock
    private ContextManager contextManager;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServerCommand command;
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        MockitoAnnotations.openMocks(this);
        
        when(serviceManager.getConfigurationService()).thenReturn(configService);
        when(serviceManager.getMetadataService()).thenReturn(metadataService);
        when(configService.getCurrentUser()).thenReturn("testuser");
        when(metadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        
        command = new ServerCommand(serviceManager);
    }
    
    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        @Test
        @DisplayName("Should display help information in text format")
        void shouldDisplayHelpInTextFormat() {
            command.setSubcommand("help");
            
            int result = command.call();
            
            assertEquals(0, result);
            assertTrue(outContent.toString().contains("Server Command Usage:"));
            assertTrue(outContent.toString().contains("rin server [subcommand] [options]"));
            assertTrue(outContent.toString().contains("Subcommands:"));
            assertTrue(outContent.toString().contains("status"));
            assertTrue(outContent.toString().contains("start"));
            assertTrue(outContent.toString().contains("stop"));
            assertTrue(outContent.toString().contains("restart"));
            assertTrue(outContent.toString().contains("config"));
            
            verify(metadataService).startOperation(eq("server-help"), eq("READ"), any());
            verify(metadataService).completeOperation(anyString(), any());
        }
        
        @Test
        @DisplayName("Should display help information in JSON format")
        void shouldDisplayHelpInJsonFormat() {
            command.setSubcommand("help");
            command.setFormat("json");
            
            int result = command.call();
            
            assertEquals(0, result);
            assertTrue(outContent.toString().contains("\"command\":"));
            assertTrue(outContent.toString().contains("\"usage\":"));
            assertTrue(outContent.toString().contains("\"subcommands\":"));
            
            verify(metadataService).startOperation(eq("server-help"), eq("READ"), any());
            verify(metadataService).completeOperation(anyString(), any());
        }
    }
    
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should show status of all services when no subcommand is provided")
        void shouldShowStatusOfAllServicesWhenNoSubcommand() {
            // Mock service status
            when(serviceManager.getServiceStatus("api")).thenReturn(new ServiceStatusInfo(true, "RUNNING", "API service is running"));
            when(serviceManager.getServiceStatus("database")).thenReturn(new ServiceStatusInfo(false, "STOPPED", "Database service is not running"));
            when(serviceManager.getServiceStatus("docs")).thenReturn(new ServiceStatusInfo(true, "RUNNING", "Documentation service is running"));
            
            int result = command.call();
            
            assertEquals(0, result);
            assertTrue(outContent.toString().contains("Rinna Services Status:"));
            assertTrue(outContent.toString().contains("API"));
            assertTrue(outContent.toString().contains("RUNNING"));
            
            // Should track operation with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).startOperation(eq("server-status"), eq("READ"), any());
            verify(metadataService, times(2)).completeOperation(anyString(), any());
        }
        
        @Test
        @DisplayName("Should show status of a specific service")
        void shouldShowStatusOfSpecificService() {
            command.setSubcommand("status");
            command.setServiceName("api");
            
            // Mock service status
            when(serviceManager.getServiceStatus("api")).thenReturn(new ServiceStatusInfo(true, "RUNNING", "API service is running"));
            
            int result = command.call();
            
            assertEquals(0, result);
            assertTrue(outContent.toString().contains("Service: api"));
            assertTrue(outContent.toString().contains("Status: RUNNING"));
            assertTrue(outContent.toString().contains("Available: Yes"));
            
            // Should track operation with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).startOperation(eq("server-status"), eq("READ"), any());
            verify(metadataService, times(2)).completeOperation(anyString(), any());
        }
        
        @Test
        @DisplayName("Should configure a service successfully")
        void shouldConfigureServiceSuccessfully() throws IOException {
            command.setSubcommand("config");
            command.setServiceName("api");
            command.setConfigPath("/tmp/api.json");
            
            // Mock service configuration
            when(serviceManager.createServiceConfig("api", "/tmp/api.json")).thenReturn(true);
            
            int result = command.call();
            
            assertEquals(0, result);
            assertTrue(outContent.toString().contains("Created configuration for api"));
            
            // Should track operation with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).startOperation(eq("server-create-config"), eq("CREATE"), any());
            verify(metadataService).startOperation(eq("server-config"), eq("CREATE"), any());
            verify(metadataService, times(2)).completeOperation(anyString(), any());
        }
        
        @Test
        @DisplayName("Should output in JSON format when requested")
        void shouldOutputInJsonFormatWhenRequested() {
            command.setSubcommand("status");
            command.setServiceName("api");
            command.setFormat("json");
            
            // Mock service status
            when(serviceManager.getServiceStatus("api")).thenReturn(new ServiceStatusInfo(true, "RUNNING", "API service is running"));
            
            int result = command.call();
            
            assertEquals(0, result);
            assertTrue(outContent.toString().contains("\"result\":"));
            assertTrue(outContent.toString().contains("\"action\":"));
            assertTrue(outContent.toString().contains("\"service\":"));
            
            // Should track operation with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).startOperation(eq("server-status"), eq("READ"), any());
            verify(metadataService, times(2)).completeOperation(anyString(), any());
        }
    }
    
    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("Should handle unknown subcommand")
        void shouldHandleUnknownSubcommand() {
            command.setSubcommand("invalid");
            
            int result = command.call();
            
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Unknown server subcommand: invalid"));
            
            // Should track operation failure with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).failOperation(anyString(), any(IllegalArgumentException.class));
        }
        
        @Test
        @DisplayName("Should handle missing service name for start command")
        void shouldHandleMissingServiceNameForStart() {
            command.setSubcommand("start");
            
            int result = command.call();
            
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Service name is required"));
            
            // Should track operation failure with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).startOperation(eq("server-start"), eq("EXECUTE"), any());
            verify(metadataService).failOperation(anyString(), any(IllegalArgumentException.class));
        }
        
        @Test
        @DisplayName("Should handle unknown service name")
        void shouldHandleUnknownServiceName() {
            command.setSubcommand("start");
            command.setServiceName("unknown");
            
            int result = command.call();
            
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Unknown service: unknown"));
            
            // Should track operation failure with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).startOperation(eq("server-start"), eq("EXECUTE"), any());
            verify(metadataService).failOperation(anyString(), any(IllegalArgumentException.class));
        }
        
        @Test
        @DisplayName("Should handle service configuration failure")
        void shouldHandleServiceConfigurationFailure() {
            command.setSubcommand("config");
            command.setServiceName("api");
            
            // Mock service configuration failure
            when(serviceManager.createServiceConfig(anyString(), anyString())).thenReturn(false);
            
            int result = command.call();
            
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Failed to create service configuration"));
            
            // Should track operation failure with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).startOperation(eq("server-config"), eq("CREATE"), any());
            verify(metadataService).startOperation(eq("server-create-config"), eq("CREATE"), any());
            verify(metadataService, times(2)).failOperation(anyString(), any(RuntimeException.class));
        }
    }
    
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should handle chained method calls")
        void shouldHandleChainedMethodCalls() {
            // Test method chaining
            int result = command
                .setUsername("newuser")
                .setFormat("json")
                .setVerbose(true)
                .setSubcommand("help")
                .call();
            
            assertEquals(0, result);
            assertTrue(outContent.toString().contains("\"command\""));
            
            // Should track operation with username from setter
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), paramsCaptor.capture());
            assertEquals("newuser", paramsCaptor.getValue().get("username"));
        }
        
        @Test
        @DisplayName("Should handle exceptions gracefully")
        void shouldHandleExceptionsGracefully() {
            command.setSubcommand("status");
            
            // Mock an exception during status check
            when(serviceManager.getServiceStatus(any())).thenThrow(new RuntimeException("Test exception"));
            
            int result = command.call();
            
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Test exception"));
            
            // Should track operation failure with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).failOperation(anyString(), any(RuntimeException.class));
        }
        
        @Test
        @DisplayName("Should handle verbose output")
        void shouldHandleVerboseOutput() {
            command.setSubcommand("status");
            command.setServiceName("api");
            command.setVerbose(true);
            
            // Mock service status
            when(serviceManager.getServiceStatus("api")).thenReturn(new ServiceStatusInfo(true, "RUNNING", "API service is running"));
            
            int result = command.call();
            
            assertEquals(0, result);
            assertTrue(outContent.toString().contains("Detailed information:"));
            
            // Should track operation with MetadataService
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).startOperation(eq("server-status"), eq("READ"), any());
            verify(metadataService, times(2)).completeOperation(anyString(), any());
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should properly track operations with MetadataService")
        void shouldTrackOperationsWithMetadataService() {
            command.setSubcommand("status");
            command.setServiceName("api");
            
            // Mock service status
            when(serviceManager.getServiceStatus("api")).thenReturn(new ServiceStatusInfo(true, "RUNNING", "API service is running"));
            
            int result = command.call();
            
            assertEquals(0, result);
            
            // Verify operation tracking
            ArgumentCaptor<Map<String, Object>> mainParamsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), mainParamsCaptor.capture());
            Map<String, Object> mainParams = mainParamsCaptor.getValue();
            assertEquals("status", mainParams.get("subcommand"));
            assertEquals("api", mainParams.get("serviceName"));
            
            ArgumentCaptor<Map<String, Object>> statusParamsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(metadataService).startOperation(eq("server-status"), eq("READ"), statusParamsCaptor.capture());
            Map<String, Object> statusParams = statusParamsCaptor.getValue();
            assertEquals("api", statusParams.get("serviceName"));
            
            ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(metadataService, times(2)).completeOperation(anyString(), resultCaptor.capture());
            
            // Second capture should be the main operation completion
            Map<String, Object> result = resultCaptor.getAllValues().get(1);
            assertEquals("status", result.get("action"));
            assertEquals("success", result.get("result"));
            assertEquals("api", result.get("serviceName"));
        }
        
        @Test
        @DisplayName("Should handle service configuration correctly")
        void shouldHandleServiceConfigurationCorrectly() throws IOException {
            command.setSubcommand("config");
            command.setServiceName("api");
            command.setConfigPath("/tmp/api.json");
            
            // Mock service configuration
            when(serviceManager.createServiceConfig("api", "/tmp/api.json")).thenReturn(true);
            
            int result = command.call();
            
            assertEquals(0, result);
            
            // Verify operation tracking
            verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
            verify(metadataService).startOperation(eq("server-config"), eq("CREATE"), any());
            verify(metadataService).startOperation(eq("server-create-config"), eq("CREATE"), any());
            
            ArgumentCaptor<Map<String, Object>> configParamsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(metadataService).startOperation(eq("server-create-config"), eq("CREATE"), configParamsCaptor.capture());
            
            Map<String, Object> configParams = configParamsCaptor.getValue();
            assertEquals("api", configParams.get("serviceName"));
            assertEquals("/tmp/api.json", configParams.get("configPath"));
            
            ArgumentCaptor<Map<String, Object>> createConfigResultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(metadataService).completeOperation(eq("operation-id"), createConfigResultCaptor.capture());
            
            Map<String, Object> createConfigResult = createConfigResultCaptor.getValue();
            assertEquals("config", createConfigResult.get("action"));
            assertEquals(true, ((Map<String, Object>)createConfigResult.get("result")).get("created"));
        }
    }
}
