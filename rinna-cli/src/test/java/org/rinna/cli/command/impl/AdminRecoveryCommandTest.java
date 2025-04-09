package org.rinna.cli.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rinna.cli.service.RecoveryService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AdminRecoveryCommand class.
 */
class AdminRecoveryCommandTest {

    private ServiceManager serviceManager;
    private RecoveryService mockRecoveryService;
    private AdminRecoveryCommand command;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        // Create a mock ServiceManager
        serviceManager = mock(ServiceManager.class);
        
        // Create a mock RecoveryService
        mockRecoveryService = mock(RecoveryService.class);
        
        // Configure the ServiceManager to return our mock RecoveryService
        when(serviceManager.getRecoveryService()).thenReturn(mockRecoveryService);
        
        // Create the command with our mocked ServiceManager
        command = new AdminRecoveryCommand(serviceManager);
        
        // Set up output capturing
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }
    
    @Test
    void testStartOperation() {
        // Mock a successful recovery start
        when(mockRecoveryService.startRecovery("backup-123")).thenReturn(true);
        
        // Simulate user confirmation for recovery
        String userInput = "yes\n";
        System.setIn(new ByteArrayInputStream(userInput.getBytes()));
        
        // Set up the command
        command.setOperation("start");
        command.setArgs(new String[]{"--backup-id=backup-123"});
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result);
        
        // Verify the mock service was called correctly
        verify(mockRecoveryService).startRecovery("backup-123");
        
        // Verify the output
        String output = outputStream.toString();
        assertTrue(output.contains("Recovery completed successfully"));
    }
    
    @Test
    void testStatusOperation() {
        // Mock a recovery status report
        String statusReport = "Recovery Status: All systems operational";
        when(mockRecoveryService.getRecoveryStatus()).thenReturn(statusReport);
        
        // Set up the command
        command.setOperation("status");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result);
        
        // Verify the mock service was called correctly
        verify(mockRecoveryService).getRecoveryStatus();
        
        // Verify the output
        String output = outputStream.toString();
        assertTrue(output.contains(statusReport));
    }
    
    @Test
    void testPlanGenerateOperation() {
        // Mock a recovery plan path
        String planPath = "target/recovery/recovery-plan-123.json";
        when(mockRecoveryService.generateRecoveryPlan()).thenReturn(planPath);
        
        // Set up the command
        command.setOperation("plan");
        command.setArgs(new String[]{"generate"});
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result);
        
        // Verify the mock service was called correctly
        verify(mockRecoveryService).generateRecoveryPlan();
        
        // Verify the output
        String output = outputStream.toString();
        assertTrue(output.contains("Recovery plan generated successfully"));
        assertTrue(output.contains(planPath));
    }
}