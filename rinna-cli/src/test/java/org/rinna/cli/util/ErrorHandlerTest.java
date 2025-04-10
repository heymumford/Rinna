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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rinna.cli.service.MetadataService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ErrorHandler class.
 */
public class ErrorHandlerTest {

    private MetadataService mockMetadataService;
    private ErrorHandler errorHandler;
    private List<String> outputMessages;

    @BeforeEach
    public void setUp() {
        // Create a mock MetadataService
        mockMetadataService = Mockito.mock(MetadataService.class);
        
        // Create a list to capture output messages
        outputMessages = new ArrayList<>();
        
        // Create the error handler with the mock service
        errorHandler = new ErrorHandler(mockMetadataService)
            .errorOutputConsumer(outputMessages::add);
    }

    @Test
    public void testHandleError() {
        // Setup
        String operationId = "test-operation-id";
        String commandName = "test-command";
        String errorMessage = "Test error message";
        Exception exception = new RuntimeException(errorMessage);
        
        // Execute
        int result = errorHandler.handleError(operationId, commandName, errorMessage, exception);
        
        // Verify
        assertEquals(1, result);
        assertEquals(1, outputMessages.size());
        assertEquals("Error: " + errorMessage, outputMessages.get(0));
        verify(mockMetadataService).failOperation(operationId, exception);
        verify(mockMetadataService).trackOperationError(operationId, commandName, errorMessage, exception);
    }

    @Test
    public void testHandleErrorWithJsonOutput() {
        // Setup
        String operationId = "test-operation-id";
        String commandName = "test-command";
        String errorMessage = "Test error message";
        Exception exception = new RuntimeException(errorMessage);
        
        // Configure for JSON output
        errorHandler.outputFormat("json");
        
        // Execute
        int result = errorHandler.handleError(operationId, commandName, errorMessage, exception);
        
        // Verify
        assertEquals(1, result);
        assertEquals(1, outputMessages.size());
        assertTrue(outputMessages.get(0).contains("\"result\":\"error\""));
        assertTrue(outputMessages.get(0).contains("\"message\":\"Test error message\""));
        verify(mockMetadataService).failOperation(operationId, exception);
        verify(mockMetadataService).trackOperationError(operationId, commandName, errorMessage, exception);
    }

    @Test
    public void testHandleValidationError() {
        // Setup
        String operationId = "test-operation-id";
        String commandName = "test-command";
        Map<String, String> validationErrors = new HashMap<>();
        validationErrors.put("field1", "Field 1 is required");
        validationErrors.put("field2", "Field 2 must be a number");
        
        // Execute
        int result = errorHandler.handleValidationError(operationId, commandName, validationErrors);
        
        // Verify
        assertEquals(1, result);
        assertEquals(1, outputMessages.size());
        assertTrue(outputMessages.get(0).startsWith("Error: Validation errors:"));
        assertTrue(outputMessages.get(0).contains("field1: Field 1 is required"));
        assertTrue(outputMessages.get(0).contains("field2: Field 2 must be a number"));
        
        verify(mockMetadataService).failOperation(eq(operationId), any(IllegalArgumentException.class));
        verify(mockMetadataService).trackOperationDetail(eq(operationId), eq("validationError_field1"), any(Map.class));
        verify(mockMetadataService).trackOperationDetail(eq(operationId), eq("validationError_field2"), any(Map.class));
    }

    @Test
    public void testHandleUnexpectedError() {
        // Setup
        String operationId = "test-operation-id";
        String commandName = "test-command";
        Exception exception = new RuntimeException("Unexpected error");
        
        // Execute
        int result = errorHandler.handleUnexpectedError(operationId, commandName, exception);
        
        // Verify
        assertEquals(1, result);
        assertEquals(1, outputMessages.size());
        assertEquals("Error: Unexpected error in test-command: Unexpected error", outputMessages.get(0));
        
        verify(mockMetadataService).trackOperationDetail(eq(operationId), eq("unexpectedError"), any(Map.class));
        verify(mockMetadataService).failOperation(operationId, exception);
    }

    @Test
    public void testHandleSuccess() {
        // Setup
        String operationId = "test-operation-id";
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        
        // Execute
        int exitCode = errorHandler.handleSuccess(operationId, result);
        
        // Verify
        assertEquals(0, exitCode);
        verify(mockMetadataService).completeOperation(operationId, result);
    }

    @Test
    public void testCreateSuccessResult() {
        // Setup
        String commandName = "test-command";
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 123);
        
        // Execute
        Map<String, Object> result = errorHandler.createSuccessResult(commandName, data);
        
        // Verify
        assertTrue((Boolean) result.get("success"));
        assertEquals(commandName, result.get("command"));
        assertEquals("value1", result.get("key1"));
        assertEquals(123, result.get("key2"));
    }
}