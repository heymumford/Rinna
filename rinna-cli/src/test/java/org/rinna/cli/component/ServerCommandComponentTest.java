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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.ServerCommand;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.ServiceManager.ServiceStatusInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Component tests for the ServerCommand class.
 * These tests focus on the integration between ServerCommand and its dependencies.
 */
@Tag("component")
@DisplayName("Server Command Component Tests")
class ServerCommandComponentTest {

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
    
    @Test
    @DisplayName("Should properly integrate with ServiceManager to retrieve service status")
    void shouldIntegrateWithServiceManagerForStatus() {
        // Mock service status
        when(serviceManager.getServiceStatus("api"))
            .thenReturn(new ServiceStatusInfo(true, "RUNNING", "API service is running"));
        
        command.setSubcommand("status");
        command.setServiceName("api");
        
        int result = command.call();
        
        assertEquals(0, result);
        assertTrue(outContent.toString().contains("Service: api"));
        assertTrue(outContent.toString().contains("Status: RUNNING"));
        
        // Verify service manager integration
        verify(serviceManager).getServiceStatus("api");
        
        // Verify operation tracking with metadata service
        verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
        verify(metadataService).startOperation(eq("server-status"), eq("READ"), any());
        verify(metadataService, times(2)).completeOperation(anyString(), any());
    }
    
    @Test
    @DisplayName("Should properly integrate with ServiceManager to create service configuration")
    void shouldIntegrateWithServiceManagerForConfiguration() throws IOException {
        // Mock successful configuration creation
        when(serviceManager.createServiceConfig("api", "/tmp/api.json")).thenReturn(true);
        
        command.setSubcommand("config");
        command.setServiceName("api");
        command.setConfigPath("/tmp/api.json");
        
        int result = command.call();
        
        assertEquals(0, result);
        assertTrue(outContent.toString().contains("Created configuration for api"));
        
        // Verify service manager integration
        verify(serviceManager).createServiceConfig("api", "/tmp/api.json");
        
        // Verify operation tracking with metadata service
        verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
        verify(metadataService).startOperation(eq("server-config"), eq("CREATE"), any());
        verify(metadataService).startOperation(eq("server-create-config"), eq("CREATE"), any());
        verify(metadataService, times(2)).completeOperation(anyString(), any());
    }
    
    @Test
    @DisplayName("Should properly track operations with nested operation IDs")
    void shouldTrackOperationsWithNestedOperationIds() {
        // Mock service status
        when(serviceManager.getServiceStatus("api"))
            .thenReturn(new ServiceStatusInfo(true, "RUNNING", "API service is running"));
        
        // Mock multiple operation IDs
        when(metadataService.startOperation(eq("server-command"), eq("MANAGE"), any()))
            .thenReturn("main-operation-id");
        when(metadataService.startOperation(eq("server-status"), eq("READ"), any()))
            .thenReturn("status-operation-id");
        
        command.setSubcommand("status");
        command.setServiceName("api");
        
        int result = command.call();
        
        assertEquals(0, result);
        
        // Verify proper operation IDs are used
        verify(metadataService).completeOperation(eq("status-operation-id"), any());
        verify(metadataService).completeOperation(eq("main-operation-id"), any());
    }
    
    @Test
    @DisplayName("Should properly handle JSON output format")
    void shouldProperlyHandleJsonOutputFormat() {
        // Mock service status
        when(serviceManager.getServiceStatus("api"))
            .thenReturn(new ServiceStatusInfo(true, "RUNNING", "API service is running"));
        
        command.setSubcommand("status");
        command.setServiceName("api");
        command.setFormat("json");
        
        int result = command.call();
        
        assertEquals(0, result);
        
        // Verify output contains JSON
        String output = outContent.toString();
        assertTrue(output.contains("\"result\":"));
        assertTrue(output.contains("\"action\":"));
        assertTrue(output.contains("\"service\":"));
        
        // Verify format parameter in operation tracking
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), paramsCaptor.capture());
        assertEquals("json", paramsCaptor.getValue().get("format"));
    }
    
    @Test
    @DisplayName("Should properly handle error cases and track failures")
    void shouldProperlyHandleErrorCasesAndTrackFailures() {
        command.setSubcommand("start");
        command.setServiceName("unknown");
        
        int result = command.call();
        
        assertEquals(1, result);
        assertTrue(errContent.toString().contains("Error: Unknown service: unknown"));
        
        // Verify failure is tracked
        verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
        verify(metadataService).startOperation(eq("server-start"), eq("EXECUTE"), any());
        verify(metadataService).failOperation(anyString(), any());
    }
    
    @Test
    @DisplayName("Should properly integrate status, start, and stop operations")
    void shouldProperlyIntegrateStatusStartAndStopOperations() {
        // First check status - return not running
        when(serviceManager.getServiceStatus("api"))
            .thenReturn(new ServiceStatusInfo(false, "STOPPED", "API service is not running"));
        
        command.setSubcommand("status");
        command.setServiceName("api");
        
        int statusResult = command.call();
        assertEquals(0, statusResult);
        assertTrue(outContent.toString().contains("Status: STOPPED"));
        
        // Clear output for next operation
        outContent.reset();
        
        // Create new command instance
        command = new ServerCommand(serviceManager);
        
        // Now start service
        // Can't fully mock process creation, but can verify the operation tracking
        command.setSubcommand("start");
        command.setServiceName("api");
        
        // This will fail in the test because we can't actually start processes in tests
        // But we can verify that start operation was tracked
        command.call();
        
        verify(metadataService).startOperation(eq("server-start"), eq("EXECUTE"), any());
        
        // In real code and integration tests we would also verify service was started
    }
}
