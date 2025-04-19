/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rinna.cli.service.MetadataService;

/**
 * Unit tests for the OperationTracker class.
 */
public class OperationTrackerTest {

    private MetadataService mockMetadataService;
    private OperationTracker tracker;

    @BeforeEach
    public void setUp() {
        // Create a mock MetadataService
        mockMetadataService = Mockito.mock(MetadataService.class);
        
        // Create the tracker with the mock service
        tracker = new OperationTracker(mockMetadataService);
    }

    @Test
    public void testStartOperation() {
        // Setup
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap()))
            .thenReturn("test-operation-id");
        
        // Configure the tracker
        tracker.command("test-command")
               .operationType("TEST")
               .param("key", "value");
        
        // Execute
        String operationId = tracker.start();
        
        // Verify
        assertEquals("test-operation-id", operationId);
        verify(mockMetadataService).startOperation(
            eq("test-command"), 
            eq("TEST"), 
            argThat(map -> map.containsKey("key") && "value".equals(map.get("key")))
        );
    }

    @Test
    public void testExecuteSuccess() {
        // Setup
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap()))
            .thenReturn("test-operation-id");
        
        // Configure the tracker
        tracker.command("test-command")
               .param("key", "value");
        
        // Execute
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("success", true);
        
        Map<String, Object> result = tracker.execute(() -> expectedResult);
        
        // Verify
        assertEquals(expectedResult, result);
        verify(mockMetadataService).startOperation(
            eq("test-command"), 
            eq("EXECUTE"), 
            argThat(map -> map.containsKey("key") && "value".equals(map.get("key")))
        );
        verify(mockMetadataService).completeOperation("test-operation-id", expectedResult);
    }

    @Test
    public void testExecuteFailure() {
        // Setup
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap()))
            .thenReturn("test-operation-id");
        
        // Configure the tracker
        tracker.command("test-command")
               .param("key", "value");
        
        // Create an exception to throw
        RuntimeException expectedException = new RuntimeException("Test exception");
        
        // Execute and verify exception is thrown
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            tracker.execute(() -> {
                throw expectedException;
            });
        });
        
        // Verify same exception is thrown
        assertSame(expectedException, thrownException);
        
        // Verify metadata service interaction
        verify(mockMetadataService).startOperation(
            eq("test-command"), 
            eq("EXECUTE"), 
            argThat(map -> map.containsKey("key") && "value".equals(map.get("key")))
        );
        verify(mockMetadataService).failOperation("test-operation-id", expectedException);
    }

    @Test
    public void testExecuteVoid() {
        // Setup
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap()))
            .thenReturn("test-operation-id");
        
        // Configure the tracker
        tracker.command("test-command")
               .param("key", "value");
        
        // Execute
        tracker.executeVoid(() -> {
            // Do nothing
        });
        
        // Verify
        verify(mockMetadataService).startOperation(
            eq("test-command"), 
            eq("EXECUTE"), 
            argThat(map -> map.containsKey("key") && "value".equals(map.get("key")))
        );
        verify(mockMetadataService).completeOperation(
            eq("test-operation-id"), 
            argThat(map -> map.containsKey("success") && (Boolean)map.get("success"))
        );
    }

    @Test
    public void testExecuteSubOperation() {
        // Setup
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap()))
            .thenReturn("test-operation-id", "sub-operation-id");
        
        // Configure the tracker
        tracker.command("test-command")
               .param("key", "value")
               .parent("parent-id");
        
        // Execute
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("success", true);
        
        Map<String, Object> result = tracker.executeSubOperation("sub-command", () -> expectedResult);
        
        // Verify
        assertEquals(expectedResult, result);
        verify(mockMetadataService).trackOperationDetail("parent-id", "subOperation", "sub-command");
        verify(mockMetadataService).startOperation(eq("sub-command"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService).completeOperation("sub-operation-id", expectedResult);
    }

    @Test
    public void testCreateSubTracker() {
        // Setup
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap()))
            .thenReturn("test-operation-id");
        
        // Configure the tracker
        tracker.command("test-command")
               .param("key", "value");
        
        // Create sub tracker
        OperationTracker subTracker = tracker.createSubTracker("sub-command");
        
        // Verify
        assertNotNull(subTracker);
        verify(mockMetadataService).startOperation(
            eq("test-command"), 
            eq("EXECUTE"), 
            argThat(map -> map.containsKey("key") && "value".equals(map.get("key")))
        );
        
        // Test the sub tracker
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap()))
            .thenReturn("sub-operation-id");
        
        String subOperationId = subTracker.start();
        
        assertEquals("sub-operation-id", subOperationId);
        verify(mockMetadataService).startOperation(eq("sub-command"), eq("EXECUTE"), anyMap());
    }
}