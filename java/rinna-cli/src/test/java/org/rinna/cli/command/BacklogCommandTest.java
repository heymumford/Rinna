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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.service.MockBacklogService;
import org.rinna.cli.service.MockMetadataService;
import org.rinna.cli.service.ServiceManager;

/**
 * Basic tests for BacklogCommand.
 */
@DisplayName("BacklogCommand Tests")
class BacklogCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockBacklogService mockBacklogService;
    private MockMetadataService mockMetadataService;
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock services
        mockServiceManager = mock(ServiceManager.class);
        mockBacklogService = new MockBacklogService();
        mockMetadataService = MockMetadataService.getInstance();
        
        // Set up mock service manager
        when(mockServiceManager.getBacklogService()).thenReturn(mockBacklogService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
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

    @Test
    @DisplayName("Should list backlog items when no action is specified")
    void shouldListBacklogItemsWhenNoActionIsSpecified() {
        // Given
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            BacklogCommand command = new BacklogCommand();
            // No action specified
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("Backlog Items:"));
            assertTrue(output.contains("ID"));
            assertTrue(output.contains("TITLE"));
            assertTrue(output.contains("TYPE"));
            assertTrue(output.contains("PRIORITY"));
        }
    }
    
    @Test
    @DisplayName("Should fail with unknown action")
    void shouldFailWithUnknownAction() {
        // Given
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            BacklogCommand command = new BacklogCommand();
            command.setAction("unknown");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            String error = errorCaptor.toString();
            assertTrue(error.contains("Error: Unknown backlog action: unknown"));
        }
    }
}