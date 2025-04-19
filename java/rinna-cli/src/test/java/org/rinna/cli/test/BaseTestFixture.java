/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ProjectContext;
import org.rinna.cli.service.ServiceManager;

/**
 * Base class for test fixtures that provides common functionality like:
 * - Console output capture
 * - Mocking of services
 * - Standard setup/teardown
 */
public abstract class BaseTestFixture {
    
    // Console capture
    protected ByteArrayOutputStream outputStream;
    protected ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    // Common mocks
    @Mock
    protected MockItemService mockItemService;
    
    @Mock
    protected MockWorkflowService mockWorkflowService;
    
    @Mock
    protected MockSearchService mockSearchService;
    
    @Mock
    protected MockHistoryService mockHistoryService;
    
    @Mock
    protected MockCommentService mockCommentService;
    
    @Mock
    protected ConfigurationService mockConfigService;
    
    @Mock
    protected ProjectContext mockProjectContext;
    
    protected AutoCloseable mockAutoCloseable;
    
    /**
     * Sets up the test fixture by initializing mocks and console capture.
     */
    @BeforeEach
    protected void setUp() throws Exception {
        // Initialize mocks
        mockAutoCloseable = MockitoAnnotations.openMocks(this);
        
        // Create fresh streams for each test to avoid cross-test contamination
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
        
        // Initialize any common mock behaviors here
        setupMockBehaviors();
    }
    
    /**
     * Tears down the test fixture by resetting console output and closing mocks.
     */
    @AfterEach
    protected void tearDown() throws Exception {
        // Restore console
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close mocks
        if (mockAutoCloseable != null) {
            mockAutoCloseable.close();
        }
    }
    
    /**
     * Set up default behaviors for mocks. Override in subclasses to customize.
     */
    protected void setupMockBehaviors() {
        // Set up default behaviors for mocks
        // Subclasses can override this to add more specific behaviors
    }
    
    /**
     * Helper method to set up a mock ServiceManager.
     * 
     * @return The mocked ServiceManager with all services configured
     */
    protected MockedStatic<ServiceManager> mockServiceManager() {
        MockedStatic<ServiceManager> serviceManagerMock = MockitoAnnotations.openMocks(ServiceManager.class);
        ServiceManager mockManager = mock(ServiceManager.class);
        serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
        
        // Set up service mocks
        when(mockManager.getItemService()).thenReturn(mockItemService);
        when(mockManager.getWorkflowService()).thenReturn(mockWorkflowService);
        when(mockManager.getSearchService()).thenReturn(mockSearchService);
        when(mockManager.getHistoryService()).thenReturn(mockHistoryService);
        when(mockManager.getCommentService()).thenReturn(mockCommentService);
        when(mockManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockManager.getProjectContext()).thenReturn(mockProjectContext);
        when(mockManager.getMockItemService()).thenReturn(mockItemService);
        when(mockManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        
        return serviceManagerMock;
    }
    
    /**
     * Helper method to create a test timestamp for consistent testing.
     * 
     * @return A LocalDateTime instance for testing
     */
    protected LocalDateTime createTestTimestamp() {
        return LocalDateTime.of(2025, 4, 1, 10, 30, 0);
    }
    
    /**
     * Helper method to get the captured standard output as a string.
     * 
     * @return The captured standard output
     */
    protected String getStandardOutput() {
        return outputStream.toString();
    }
    
    /**
     * Helper method to get the captured error output as a string.
     * 
     * @return The captured error output
     */
    protected String getErrorOutput() {
        return errorStream.toString();
    }
    
    /**
     * Helper method to reset the captured output streams.
     */
    protected void resetCapturedOutput() {
        outputStream.reset();
        errorStream.reset();
    }
}