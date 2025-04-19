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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;

@ExtendWith(MockitoExtension.class)
public class WorkflowCommandTest {

    private WorkflowCommand command;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private WorkItem workItem;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        
        command = new WorkflowCommand();
        command.setItemId("WI-123");
        command.setTargetState(WorkflowState.IN_PROGRESS);
        
        // Mock serviceManager to return our mocked itemService
        when(serviceManager.getItemService()).thenReturn(itemService);
        
        // Setup basic workItem behavior
        when(workItem.getId()).thenReturn("WI-123");
        when(workItem.getStatus()).thenReturn(WorkflowState.READY);
        when(itemService.getItem("WI-123")).thenReturn(workItem);
    }
    
    @Test
    public void testCall() {
        // Call the command
        Integer result = command.call();
        
        // Verify the result is successful
        assertEquals(0, result);
        
        // Check that the output contains expected information
        String output = outContent.toString();
        assertTrue(output.contains("WI-123"));
        assertTrue(output.contains("IN_PROGRESS"));
    }
    
    @Test
    public void testCallWithComment() {
        // Set a comment
        command.setComment("Moving to in progress");
        
        // Call the command
        Integer result = command.call();
        
        // Verify the result is successful
        assertEquals(0, result);
        
        // Check that the output contains the comment
        String output = outContent.toString();
        assertTrue(output.contains("Moving to in progress"));
    }
    
    @Test
    public void testCallWithMissingItemId() {
        // Set item ID to null
        command.setItemId(null);
        
        // Call the command
        Integer result = command.call();
        
        // Verify the result indicates an error
        assertEquals(1, result);
        
        // Check that the error message is displayed
        String output = outContent.toString();
        assertTrue(output.contains("Error: Work item ID is required"));
    }
    
    @Test
    public void testCallWithMissingTargetState() {
        // Set target state to null
        command.setTargetState(null);
        
        // Call the command
        Integer result = command.call();
        
        // Verify the result indicates an error
        assertEquals(1, result);
        
        // Check that the error message is displayed
        String output = outContent.toString();
        assertTrue(output.contains("Error: Target state is required"));
    }
}