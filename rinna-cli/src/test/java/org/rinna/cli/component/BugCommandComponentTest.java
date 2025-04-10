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
import java.time.LocalDateTime;

import org.junit.jupiter.api.*;
import org.rinna.cli.command.BugCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.service.*;

/**
 * Component test for the BugCommand class.
 * 
 * These tests verify the integration between BugCommand and its dependent services.
 */
@DisplayName("BugCommand Component Tests")
public class BugCommandComponentTest {

    private static final String TEST_BUG_ID = "BUG-123";
    private static final String TEST_OPERATION_ID = "test-operation-id";
    private static final String TEST_PROJECT_KEY = "TEST";
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private ConfigurationService mockConfigService;
    private MetadataService mockMetadataService;
    private ProjectContext mockProjectContext;
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock services
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = mock(MockItemService.class);
        mockConfigService = mock(ConfigurationService.class);
        mockMetadataService = mock(MetadataService.class);
        mockProjectContext = mock(ProjectContext.class);
        
        // Configure mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getProjectContext()).thenReturn(mockProjectContext);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(eq("bug"), eq("CREATE"), any())).thenReturn(TEST_OPERATION_ID);
        
        // Set up default project context
        when(mockProjectContext.getCurrentProject()).thenReturn(TEST_PROJECT_KEY);
        
        // Set up default config values
        when(mockConfigService.getDefaultVersion()).thenReturn("1.0.0");
        when(mockConfigService.getAutoAssignBugs()).thenReturn(true);
        when(mockConfigService.getDefaultBugAssignee()).thenReturn(null); // Use current user by default
        
        // Set up item service to return a sample bug when createItem is called
        when(mockItemService.createItem(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(TEST_BUG_ID);
            return item;
        });
        
        // Mock ServiceManager.getInstance
        try (var staticMock = mockStatic(ServiceManager.class)) {
            staticMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        }
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
    @DisplayName("Should successfully create a bug and track metadata operation")
    void shouldSuccessfullyCreateBugAndTrackMetadataOperation() {
        // Given
        try (var mockStatic = mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            BugCommand command = new BugCommand(mockServiceManager);
            command.setTitle("Test Bug");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify correct service interactions
            verify(mockMetadataService).startOperation(eq("bug"), eq("CREATE"), any());
            verify(mockProjectContext).getCurrentProject();
            verify(mockConfigService).getDefaultVersion();
            verify(mockConfigService).getAutoAssignBugs();
            verify(mockConfigService).getDefaultBugAssignee();
            
            verify(mockItemService).createItem(any(WorkItem.class));
            verify(mockMetadataService).completeOperation(eq(TEST_OPERATION_ID), any());
            
            // Verify stdout contains bug details
            String output = outputCaptor.toString();
            assertTrue(output.contains("Created bug:"));
            assertTrue(output.contains("ID: " + TEST_BUG_ID));
            assertTrue(output.contains("Type: BUG"));
            assertTrue(output.contains("Status: CREATED"));
        }
    }
    
    @Test
    @DisplayName("Should handle integration of ConfigurationService defaults into bug creation")
    void shouldHandleIntegrationOfConfigurationServiceDefaultsIntoBugCreation() {
        // Given
        try (var mockStatic = mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            // Set up custom configuration values
            when(mockConfigService.getDefaultVersion()).thenReturn("2.0.0");
            when(mockConfigService.getAutoAssignBugs()).thenReturn(true);
            when(mockConfigService.getDefaultBugAssignee()).thenReturn("qa.team");
            
            BugCommand command = new BugCommand(mockServiceManager);
            command.setTitle("Config Integration Bug");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify the bug was created with configuration values
            verify(mockItemService).createItem(argThat(workItem -> 
                workItem.getVersion().equals("2.0.0") &&
                workItem.getAssignee().equals("qa.team")
            ));
            
            // Verify output contains the configured values
            String output = outputCaptor.toString();
            assertTrue(output.contains("Version: 2.0.0"));
            assertTrue(output.contains("Assignee: qa.team"));
        }
    }
    
    @Test
    @DisplayName("Should handle failed operation with proper tracking")
    void shouldHandleFailedOperationWithProperTracking() {
        // Given
        try (var mockStatic = mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            // Configure service to throw exception
            RuntimeException testException = new RuntimeException("Test component exception");
            when(mockItemService.createItem(any(WorkItem.class))).thenThrow(testException);
            
            BugCommand command = new BugCommand(mockServiceManager);
            command.setTitle("Exception Bug");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("bug"), eq("CREATE"), any());
            verify(mockMetadataService).failOperation(eq(TEST_OPERATION_ID), eq(testException));
            
            // Verify error output
            String errorOutput = errorCaptor.toString();
            assertTrue(errorOutput.contains("Error creating bug: Test component exception"));
        }
    }
    
    @Test
    @DisplayName("Should integrate with OutputFormatter for JSON output")
    void shouldIntegrateWithOutputFormatterForJsonOutput() {
        // Given
        try (var mockStatic = mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            BugCommand command = new BugCommand(mockServiceManager);
            command.setTitle("JSON Output Bug");
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify JSON output contains correct structure
            String output = outputCaptor.toString();
            assertTrue(output.contains("\"id\": \"" + TEST_BUG_ID + "\""));
            assertTrue(output.contains("\"title\": \"JSON Output Bug\""));
            assertTrue(output.contains("\"type\": \"BUG\""));
            assertTrue(output.contains("\"status\": \"CREATED\""));
            assertTrue(output.contains("\"project\": \"" + TEST_PROJECT_KEY + "\""));
        }
    }
    
    @Test
    @DisplayName("Should track rich metadata in the operation context")
    void shouldTrackRichMetadataInTheOperationContext() {
        // Given
        try (var mockStatic = mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            BugCommand command = new BugCommand(mockServiceManager);
            command.setTitle("Metadata Bug");
            command.setPriority(Priority.HIGH);
            command.setDescription("Test description");
            command.setVersion("3.0.0");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation parameters
            verify(mockMetadataService).startOperation(eq("bug"), eq("CREATE"), argThat(params -> 
                params.containsKey("title") && 
                params.containsKey("description") && 
                params.containsKey("priority") && 
                params.containsKey("version") && 
                params.get("title").equals("Metadata Bug") &&
                params.get("description").equals("Test description") &&
                params.get("priority").equals("HIGH") &&
                params.get("version").equals("3.0.0")
            ));
            
            // Verify operation result
            verify(mockMetadataService).completeOperation(eq(TEST_OPERATION_ID), argThat(result -> 
                result.containsKey("bug_id") && 
                result.containsKey("title") &&
                result.containsKey("status") &&
                result.containsKey("priority") &&
                result.get("bug_id").equals(TEST_BUG_ID) &&
                result.get("title").equals("Metadata Bug") &&
                result.get("status").equals("CREATED") &&
                result.get("priority").equals("HIGH")
            ));
        }
    }
    
    @Test
    @DisplayName("Should set appropriate metadata on the work item")
    void shouldSetAppropriateMetadataOnTheWorkItem() {
        // Given
        try (var mockStatic = mockStatic(ServiceManager.class)) {
            mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            BugCommand command = new BugCommand(mockServiceManager);
            command.setTitle("Metadata Work Item Bug");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify work item fields
            verify(mockItemService).createItem(argThat(workItem -> 
                WorkItemType.BUG.equals(workItem.getType()) &&
                workItem.getCreated() != null &&
                workItem.getUpdated() != null &&
                workItem.getReporter() != null &&
                workItem.getReporter().equals(System.getProperty("user.name"))
            ));
            
            // Timestamps should be reasonable
            verify(mockItemService).createItem(argThat(workItem -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime createdTime = workItem.getCreated();
                LocalDateTime updatedTime = workItem.getUpdated();
                
                // Timestamps should be within 5 seconds of now
                return Math.abs(java.time.Duration.between(now, createdTime).getSeconds()) < 5 &&
                       Math.abs(java.time.Duration.between(now, updatedTime).getSeconds()) < 5;
            }));
        }
    }
}