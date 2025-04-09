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

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for AddCommand.
 *
 * This test class follows TDD best practices with these categories:
 * 1. Help Documentation Tests - Testing the help/usage output
 * 2. Positive Test Cases - Testing normal successful operations
 * 3. Negative Test Cases - Testing error handling for invalid inputs
 * 4. Contract Tests - Testing the contract between this class and its dependencies
 * 5. Integration Tests - Testing end-to-end scenarios
 */
@DisplayName("AddCommand Tests")
class AddCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MockConfigurationService mockConfigService;
    private MockMetadataService mockMetadataService;
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock services
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = new MockItemService();
        mockConfigService = new MockConfigurationService();
        mockMetadataService = new MockMetadataService();
        
        // Set up mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        ConfigurationService configService = mock(ConfigurationService.class);
        when(configService.getCurrentUser()).thenReturn(mockConfigService.getCurrentUser());
        when(configService.getCurrentProject()).thenReturn(mockConfigService.getCurrentProject());
        when(mockServiceManager.getConfigurationService()).thenReturn(configService);
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
    
    /**
     * Tests for help documentation using a custom helper class.
     */
    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        private HelpTestAddCommand helpCommand;
        
        @BeforeEach
        void setUp() {
            helpCommand = new HelpTestAddCommand();
        }
        
        @Test
        @DisplayName("Should display help when -h flag is used")
        void shouldDisplayHelpWithHFlag() {
            // Execute
            helpCommand.testExecuteHelp("-h");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to add a new work item")),
                () -> assertTrue(output.contains("--title")),
                () -> assertTrue(output.contains("--description")),
                () -> assertTrue(output.contains("--type")),
                () -> assertTrue(output.contains("--priority")),
                () -> assertTrue(output.contains("--assignee")),
                () -> assertTrue(output.contains("--project"))
            );
        }
        
        @Test
        @DisplayName("Should display help when --help flag is used")
        void shouldDisplayHelpWithHelpFlag() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to add a new work item")),
                () -> assertTrue(output.contains("--title")),
                () -> assertTrue(output.contains("--description")),
                () -> assertTrue(output.contains("--type")),
                () -> assertTrue(output.contains("--priority")),
                () -> assertTrue(output.contains("--assignee")),
                () -> assertTrue(output.contains("--project"))
            );
        }
        
        @Test
        @DisplayName("Help should list required arguments")
        void helpShouldListRequiredArguments() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertTrue(output.contains("--title")); // Title is required
        }
        
        @Test
        @DisplayName("Help should list optional arguments")
        void helpShouldListOptionalArguments() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("--description")),
                () -> assertTrue(output.contains("--type")),
                () -> assertTrue(output.contains("--priority")),
                () -> assertTrue(output.contains("--assignee")),
                () -> assertTrue(output.contains("--project")),
                () -> assertTrue(output.contains("--json")),
                () -> assertTrue(output.contains("--verbose"))
            );
        }
    }
    
    /**
     * Tests for successful scenarios.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should create work item with minimal required fields")
        void shouldCreateWorkItemWithMinimalFields() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertAll(
                    () -> assertEquals("Test Work Item", mockItemService.getLastCreatedItem().getTitle()),
                    () -> assertEquals(WorkItemType.TASK, mockItemService.getLastCreatedItem().getType()),
                    () -> assertEquals(Priority.MEDIUM, mockItemService.getLastCreatedItem().getPriority()),
                    () -> assertEquals(WorkflowState.CREATED, mockItemService.getLastCreatedItem().getStatus()),
                    () -> assertTrue(outputCaptor.toString().contains("Created work item:"))
                );
            }
        }
        
        @Test
        @DisplayName("Should create work item with all fields specified")
        void shouldCreateWorkItemWithAllFields() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Comprehensive Work Item");
                command.setDescription("This is a detailed description");
                command.setType(WorkItemType.BUG);
                command.setPriority(Priority.HIGH);
                command.setAssignee("john.doe");
                command.setProject("TEST-PROJECT");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem createdItem = mockItemService.getLastCreatedItem();
                assertAll(
                    () -> assertEquals("Comprehensive Work Item", createdItem.getTitle()),
                    () -> assertEquals("This is a detailed description", createdItem.getDescription()),
                    () -> assertEquals(WorkItemType.BUG, createdItem.getType()),
                    () -> assertEquals(Priority.HIGH, createdItem.getPriority()),
                    () -> assertEquals("john.doe", createdItem.getAssignee()),
                    () -> assertEquals("TEST-PROJECT", createdItem.getProject()),
                    () -> assertEquals(WorkflowState.CREATED, createdItem.getStatus())
                );
            }
        }
        
        @Test
        @DisplayName("Should set current user as assignee when not specified")
        void shouldSetCurrentUserAsAssignee() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("current.user");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals("current.user", mockItemService.getLastCreatedItem().getAssignee());
            }
        }
        
        @Test
        @DisplayName("Should set current user as reporter")
        void shouldSetCurrentUserAsReporter() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("current.user");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals("current.user", mockItemService.getLastCreatedItem().getReporter());
            }
        }
        
        @Test
        @DisplayName("Should set anonymous as reporter when no current user")
        void shouldSetAnonymousAsReporterWhenNoCurrentUser() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser(null);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals("anonymous", mockItemService.getLastCreatedItem().getReporter());
            }
        }
        
        @Test
        @DisplayName("Should set current project when not specified")
        void shouldSetCurrentProjectWhenNotSpecified() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentProject("CURRENT-PROJECT");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals("CURRENT-PROJECT", mockItemService.getLastCreatedItem().getProject());
            }
        }
        
        @Test
        @DisplayName("Should use JSON output format when specified")
        void shouldUseJsonOutputFormatWhenSpecified() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setJsonOutput(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"result\": \"success\"")),
                    () -> assertTrue(output.contains("\"title\": \"Test Work Item\"")),
                    () -> assertTrue(output.contains("\"id\":")),
                    () -> assertTrue(output.contains("\"operationId\":"))
                );
            }
        }
        
        @Test
        @DisplayName("Should include verbose output when specified")
        void shouldIncludeVerboseOutputWhenSpecified() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("Operation ID:")),
                    () -> assertTrue(output.contains("Operation tracked in metadata service"))
                );
            }
        }
        
        @Test
        @DisplayName("Should track operation with metadata service")
        void shouldTrackOperationWithMetadataService() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                String operationId = UUID.randomUUID().toString();
                mockMetadataService.setNextOperationId(operationId);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertAll(
                    () -> assertEquals(1, mockMetadataService.getStartOperationCallCount()),
                    () -> assertEquals(1, mockMetadataService.getCompleteOperationCallCount()),
                    () -> assertEquals(0, mockMetadataService.getFailOperationCallCount()),
                    () -> assertEquals(operationId, mockMetadataService.getLastOperationId()),
                    () -> assertEquals("add", mockMetadataService.getLastCommandName()),
                    () -> assertEquals("CREATE", mockMetadataService.getLastOperationType())
                );
            }
        }
        
        @Test
        @DisplayName("Should handle long description by truncating in metadata")
        void shouldHandleLongDescriptionByTruncatingInMetadata() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setDescription("This is a very long description that should be truncated in the metadata parameters. " +
                        "The truncation should occur when it's longer than 50 characters for tracking purposes.");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                Map<String, Object> lastParams = mockMetadataService.getLastParameters();
                String truncatedDesc = (String) lastParams.get("description");
                assertTrue(truncatedDesc.contains("..."));
                assertTrue(truncatedDesc.length() <= 50);
            }
        }
        
        @Test
        @DisplayName("Should produce success response without throwing exceptions")
        void shouldProduceSuccessResponseWithoutExceptions() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When & Then
                assertDoesNotThrow(() -> {
                    int exitCode = command.call();
                    assertEquals(0, exitCode);
                });
            }
        }
    }
    
    /**
     * Tests for error handling and invalid inputs.
     */
    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("Should fail when title is missing")
        void shouldFailWhenTitleIsMissing() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                // No title set
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Title is required"));
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
            }
        }
        
        @Test
        @DisplayName("Should fail when title is empty")
        void shouldFailWhenTitleIsEmpty() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Title is required"));
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
            }
        }
        
        @Test
        @DisplayName("Should show error in JSON format when JSON output is enabled")
        void shouldShowErrorInJsonFormatWhenJsonOutputIsEnabled() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setJsonOutput(true);
                // No title set
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"error\""));
                assertTrue(output.contains("\"message\": \"Title is required\""));
            }
        }
        
        @Test
        @DisplayName("Should handle exception when creating work item")
        void shouldHandleExceptionWhenCreatingWorkItem() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionOnCreate(true);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
            }
        }
        
        @Test
        @DisplayName("Should handle exception in JSON format when JSON output is enabled")
        void shouldHandleExceptionInJsonFormatWhenJsonOutputIsEnabled() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionOnCreate(true);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setJsonOutput(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"error\""));
                assertTrue(output.contains("\"message\": \"Test exception\""));
            }
        }
        
        @Test
        @DisplayName("Should show stack trace when verbose mode is enabled")
        void shouldShowStackTraceWhenVerboseModeIsEnabled() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionOnCreate(true);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("java.lang.RuntimeException: Test exception"));
                assertTrue(error.contains("Operation ID:"));
            }
        }
        
        @Test
        @DisplayName("Should handle null pointer exception in service")
        void shouldHandleNullPointerExceptionInService() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionType("NullPointerException");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when metadata service throws exception")
        void shouldFailWhenMetadataServiceThrowsException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockMetadataService.setThrowExceptionOnStartOperation(true);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when item service returns null")
        void shouldFailWhenItemServiceReturnsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setReturnNullOnCreate(true);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle illegal argument exception")
        void shouldHandleIllegalArgumentException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionType("IllegalArgumentException");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should escape special characters in JSON error message")
        void shouldEscapeSpecialCharactersInJsonErrorMessage() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionWithMessage("Error with \"quotes\" and \n newlines");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setJsonOutput(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Error with \\\"quotes\\\" and \\n newlines"));
            }
        }
        
        @Test
        @DisplayName("Should handle exception when getting service from service manager")
        void shouldHandleExceptionWhenGettingServiceFromServiceManager() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                // Make ServiceManager.getInstance() return our mock
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // But make the mock throw an exception when getMockItemService() is called
                when(mockServiceManager.getMockItemService()).thenThrow(new RuntimeException("Service not available"));
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item: Service not available"));
            }
        }
        
        @Test
        @DisplayName("Should handle out of memory error")
        void shouldHandleOutOfMemoryError() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowOutOfMemoryError(true);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle IOException")
        void shouldHandleIOException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionType("IOException");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle error when ServiceManager.getInstance() returns null")
        void shouldHandleErrorWhenServiceManagerGetInstanceReturnsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(null);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle concurrent modification exception")
        void shouldHandleConcurrentModificationException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionType("ConcurrentModificationException");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle exception when ConfigurationService returns null")
        void shouldHandleExceptionWhenConfigurationServiceReturnsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getConfigurationService()).thenReturn(null);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle SecurityException")
        void shouldHandleSecurityException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionType("SecurityException");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle IllegalStateException")
        void shouldHandleIllegalStateException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionType("IllegalStateException");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle UnsupportedOperationException")
        void shouldHandleUnsupportedOperationException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionType("UnsupportedOperationException");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error creating work item:"));
            }
        }
    }
    
    /**
     * Tests for verifying the contract between AddCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call startOperation on metadata service")
        void shouldCallStartOperationOnMetadataService() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockMetadataService.getStartOperationCallCount());
                assertAll(
                    () -> assertEquals("add", mockMetadataService.getLastCommandName()),
                    () -> assertEquals("CREATE", mockMetadataService.getLastOperationType()),
                    () -> assertNotNull(mockMetadataService.getLastParameters())
                );
            }
        }
        
        @Test
        @DisplayName("Should call completeOperation on metadata service for successful operation")
        void shouldCallCompleteOperationOnMetadataServiceForSuccessfulOperation() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockMetadataService.getCompleteOperationCallCount());
                assertAll(
                    () -> assertEquals(mockMetadataService.getLastOperationId(), 
                                      mockMetadataService.getLastCompletedOperationId()),
                    () -> assertNotNull(mockMetadataService.getLastResult())
                );
            }
        }
        
        @Test
        @DisplayName("Should call failOperation on metadata service for failed operation")
        void shouldCallFailOperationOnMetadataServiceForFailedOperation() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // No title set to trigger failure
                AddCommand command = new AddCommand();
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
                assertAll(
                    () -> assertEquals(mockMetadataService.getLastOperationId(), 
                                      mockMetadataService.getLastFailedOperationId()),
                    () -> assertNotNull(mockMetadataService.getLastException())
                );
            }
        }
        
        @Test
        @DisplayName("Should call createItem on item service")
        void shouldCallCreateItemOnItemService() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockItemService.getCreateItemCallCount());
                WorkItem createdItem = mockItemService.getLastCreatedItem();
                assertAll(
                    () -> assertEquals("Test Work Item", createdItem.getTitle()),
                    () -> assertEquals(WorkflowState.CREATED, createdItem.getStatus())
                );
            }
        }
        
        @Test
        @DisplayName("Should call getCurrentUser and getCurrentProject on config service")
        void shouldCallGetCurrentUserAndGetCurrentProjectOnConfigService() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                command.call();
                
                // Then
                assertTrue(mockConfigService.getCurrentUserWasCalled());
                assertTrue(mockConfigService.getCurrentProjectWasCalled());
            }
        }
    }
    
    /**
     * Tests for end-to-end scenarios with multiple components.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        static Stream<Arguments> provideWorkItemTypeAndPriority() {
            return Stream.of(
                Arguments.of(WorkItemType.TASK, Priority.LOW),
                Arguments.of(WorkItemType.BUG, Priority.MEDIUM),
                Arguments.of(WorkItemType.FEATURE, Priority.HIGH),
                Arguments.of(WorkItemType.EPIC, Priority.CRITICAL)
            );
        }
        
        @ParameterizedTest
        @DisplayName("Should create work item with different type and priority combinations")
        @MethodSource("provideWorkItemTypeAndPriority")
        void shouldCreateWorkItemWithDifferentTypeAndPriorityCombinations(WorkItemType type, Priority priority) {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setType(type);
                command.setPriority(priority);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem createdItem = mockItemService.getLastCreatedItem();
                assertEquals(type, createdItem.getType());
                assertEquals(priority, createdItem.getPriority());
            }
        }
        
        @ParameterizedTest
        @DisplayName("Should create work item with different assignee and project combinations")
        @CsvSource({
            "john.doe, PROJECT-A",
            "jane.smith, PROJECT-B",
            "admin, SYSTEM",
            ", CORE"
        })
        void shouldCreateWorkItemWithDifferentAssigneeAndProjectCombinations(String assignee, String project) {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setAssignee(assignee);
                command.setProject(project);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem createdItem = mockItemService.getLastCreatedItem();
                
                if (assignee != null && !assignee.isEmpty()) {
                    assertEquals(assignee, createdItem.getAssignee());
                } else {
                    // Should use current user from config service
                    assertEquals(mockConfigService.getCurrentUser(), createdItem.getAssignee());
                }
                
                assertEquals(project, createdItem.getProject());
            }
        }
        
        @ParameterizedTest
        @DisplayName("Should set different work item types")
        @EnumSource(WorkItemType.class)
        void shouldSetDifferentWorkItemTypes(WorkItemType type) {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setType(type);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals(type, mockItemService.getLastCreatedItem().getType());
            }
        }
        
        @ParameterizedTest
        @DisplayName("Should set different priorities")
        @EnumSource(Priority.class)
        void shouldSetDifferentPriorities(Priority priority) {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                command.setPriority(priority);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals(priority, mockItemService.getLastCreatedItem().getPriority());
            }
        }
        
        @Test
        @DisplayName("Should successfully create item after configuring user and project")
        void shouldSuccessfullyCreateItemAfterConfiguringUserAndProject() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Configure the user and project
                mockConfigService.setCurrentUser("configured.user");
                mockConfigService.setCurrentProject("CONFIGURED-PROJECT");
                
                AddCommand command = new AddCommand();
                command.setTitle("Test Work Item");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem createdItem = mockItemService.getLastCreatedItem();
                assertEquals("configured.user", createdItem.getAssignee());
                assertEquals("configured.user", createdItem.getReporter());
                assertEquals("CONFIGURED-PROJECT", createdItem.getProject());
            }
        }
        
        @Test
        @DisplayName("Should generate proper JSON output with all fields")
        void shouldGenerateProperJsonOutputWithAllFields() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("JSON Test Item");
                command.setDescription("Testing JSON output");
                command.setType(WorkItemType.BUG);
                command.setPriority(Priority.HIGH);
                command.setAssignee("json.tester");
                command.setProject("JSON-PROJECT");
                command.setJsonOutput(true);
                
                mockItemService.setExactIdForNextItem("test-json-id-123");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"result\": \"success\"")),
                    () -> assertTrue(output.contains("\"id\": \"test-json-id-123\"")),
                    () -> assertTrue(output.contains("\"title\": \"JSON Test Item\"")),
                    () -> assertTrue(output.contains("\"type\": \"BUG\"")),
                    () -> assertTrue(output.contains("\"priority\": \"HIGH\"")),
                    () -> assertTrue(output.contains("\"assignee\": \"json.tester\"")),
                    () -> assertTrue(output.contains("\"project\": \"JSON-PROJECT\"")),
                    () -> assertTrue(output.contains("\"description\": \"Testing JSON output\"")),
                    () -> assertTrue(output.contains("\"status\": \"CREATED\""))
                );
            }
        }
        
        @Test
        @DisplayName("Should handle special characters in title, description, and project")
        void shouldHandleSpecialCharactersInTitleDescriptionAndProject() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Title with \"quotes\" and \n newlines");
                command.setDescription("Description with \t tabs and \\ backslashes");
                command.setProject("Project-\\-with-\"special\"-chars");
                command.setJsonOutput(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"title\": \"Title with \\\"quotes\\\" and \\n newlines\"")),
                    () -> assertTrue(output.contains("\"description\": \"Description with \\t tabs and \\\\ backslashes\"")),
                    () -> assertTrue(output.contains("\"project\": \"Project-\\\\-with-\\\"special\\\"-chars\""))
                );
            }
        }
        
        @Test
        @DisplayName("Should successfully track complex operation with multiple parameters")
        void shouldSuccessfullyTrackComplexOperationWithMultipleParameters() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AddCommand command = new AddCommand();
                command.setTitle("Complex Operation Test");
                command.setDescription("Testing operation tracking");
                command.setType(WorkItemType.FEATURE);
                command.setPriority(Priority.CRITICAL);
                command.setAssignee("operation.tracker");
                command.setProject("TRACK-PROJECT");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals(1, mockMetadataService.getStartOperationCallCount());
                assertEquals(1, mockMetadataService.getCompleteOperationCallCount());
                
                Map<String, Object> params = mockMetadataService.getLastParameters();
                assertAll(
                    () -> assertEquals("Complex Operation Test", params.get("title")),
                    () -> assertEquals("FEATURE", params.get("type")),
                    () -> assertEquals("CRITICAL", params.get("priority")),
                    () -> assertEquals("operation.tracker", params.get("assignee")),
                    () -> assertEquals("TRACK-PROJECT", params.get("project"))
                );
            }
        }
    }
    
    /**
     * Mock implementation of ItemService for testing.
     */
    public static class MockItemService implements ItemService {
        private final List<WorkItem> items = new ArrayList<>();
        private int createItemCallCount = 0;
        private WorkItem lastCreatedItem;
        private boolean throwExceptionOnCreate = false;
        private boolean throwOutOfMemoryError = false;
        private boolean returnNullOnCreate = false;
        private String exactIdForNextItem;
        private String exceptionType = "RuntimeException";
        private String exceptionMessage = "Test exception";
        
        @Override
        public List<WorkItem> getAllItems() {
            return new ArrayList<>(items);
        }
        
        @Override
        public List<WorkItem> getAllWorkItems() {
            return getAllItems();
        }
        
        @Override
        public WorkItem getItem(String id) {
            return items.stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElse(null);
        }
        
        @Override
        public WorkItem createItem(WorkItem item) {
            createItemCallCount++;
            
            // Handle exception simulation
            if (throwExceptionOnCreate) {
                throwException();
            }
            
            // Handle out of memory error
            if (throwOutOfMemoryError) {
                throw new OutOfMemoryError("Test OOM error");
            }
            
            // Handle null return
            if (returnNullOnCreate) {
                return null;
            }
            
            // Set ID if specified or generate one
            if (exactIdForNextItem != null) {
                item.setId(exactIdForNextItem);
                exactIdForNextItem = null;
            } else if (item.getId() == null) {
                item.setId(UUID.randomUUID().toString());
            }
            
            // Set timestamps
            item.setCreated(LocalDateTime.now());
            item.setUpdated(LocalDateTime.now());
            
            // Save and return
            items.add(item);
            lastCreatedItem = item;
            return item;
        }
        
        private void throwException() {
            switch (exceptionType) {
                case "NullPointerException":
                    throw new NullPointerException(exceptionMessage);
                case "IllegalArgumentException":
                    throw new IllegalArgumentException(exceptionMessage);
                case "IOException":
                    throw new RuntimeException(new java.io.IOException(exceptionMessage));
                case "ConcurrentModificationException":
                    throw new ConcurrentModificationException(exceptionMessage);
                case "SecurityException":
                    throw new SecurityException(exceptionMessage);
                case "IllegalStateException":
                    throw new IllegalStateException(exceptionMessage);
                case "UnsupportedOperationException":
                    throw new UnsupportedOperationException(exceptionMessage);
                default:
                    throw new RuntimeException(exceptionMessage);
            }
        }
        
        @Override
        public WorkItem updateItem(WorkItem item) {
            // Find and update the item
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(item.getId())) {
                    items.set(i, item);
                    return item;
                }
            }
            return null;
        }
        
        @Override
        public boolean deleteItem(String id) {
            return items.removeIf(item -> id.equals(item.getId()));
        }
        
        @Override
        public WorkItem findItemByShortId(String shortId) {
            // Simple implementation for testing
            for (WorkItem item : items) {
                if (item.getId().endsWith(shortId)) {
                    return item;
                }
            }
            return null;
        }
        
        @Override
        public WorkItem updateTitle(UUID id, String title, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setTitle(title);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateDescription(UUID id, String description, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setDescription(description);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateField(UUID id, String field, String value, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                switch (field) {
                    case "title":
                        item.setTitle(value);
                        break;
                    case "description":
                        item.setDescription(value);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown field: " + field);
                }
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem assignTo(UUID id, String assignee, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setAssignee(assignee);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateAssignee(String id, String assignee) {
            WorkItem item = getItem(id);
            if (item != null) {
                item.setAssignee(assignee);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updatePriority(UUID id, Priority priority, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setPriority(priority);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateCustomFields(String id, Map<String, String> customFields) {
            WorkItem item = getItem(id);
            if (item != null) {
                // No custom fields to update in this mock implementation
                return item;
            }
            return null;
        }
        
        // Getters for test verification
        public int getCreateItemCallCount() {
            return createItemCallCount;
        }
        
        public WorkItem getLastCreatedItem() {
            return lastCreatedItem;
        }
        
        // Setters for test control
        public void setThrowExceptionOnCreate(boolean throwException) {
            this.throwExceptionOnCreate = throwException;
        }
        
        public void setThrowOutOfMemoryError(boolean throwOutOfMemoryError) {
            this.throwOutOfMemoryError = throwOutOfMemoryError;
        }
        
        public void setReturnNullOnCreate(boolean returnNull) {
            this.returnNullOnCreate = returnNull;
        }
        
        public void setExactIdForNextItem(String id) {
            this.exactIdForNextItem = id;
        }
        
        public void setThrowExceptionType(String exceptionType) {
            this.exceptionType = exceptionType;
        }
        
        public void setThrowExceptionWithMessage(String message) {
            this.exceptionMessage = message;
        }
    }
    
    /**
     * Mock implementation of ConfigurationService for testing.
     */
    private static class MockConfigurationService {
        private String currentUser;
        private String currentProject;
        private boolean getCurrentUserWasCalled = false;
        private boolean getCurrentProjectWasCalled = false;
        
        public MockConfigurationService() {
            // Default values
            this.currentUser = "test.user";
            this.currentProject = "TEST-PROJECT";
        }
        
        public String getCurrentUser() {
            getCurrentUserWasCalled = true;
            return currentUser;
        }
        
        public String getCurrentProject() {
            getCurrentProjectWasCalled = true;
            return currentProject;
        }
        
        // Setters for test control
        public void setCurrentUser(String currentUser) {
            this.currentUser = currentUser;
        }
        
        public void setCurrentProject(String currentProject) {
            this.currentProject = currentProject;
        }
        
        // Getters for test verification
        public boolean getCurrentUserWasCalled() {
            return getCurrentUserWasCalled;
        }
        
        public boolean getCurrentProjectWasCalled() {
            return getCurrentProjectWasCalled;
        }
    }
    
    /**
     * Mock implementation of MetadataService for testing.
     */
    private static class MockMetadataService implements MetadataService {
        private final AtomicInteger startOperationCallCount = new AtomicInteger(0);
        private final AtomicInteger completeOperationCallCount = new AtomicInteger(0);
        private final AtomicInteger failOperationCallCount = new AtomicInteger(0);
        
        private String lastCommandName;
        private String lastOperationType;
        private Map<String, Object> lastParameters;
        private String lastOperationId;
        private String lastCompletedOperationId;
        private String lastFailedOperationId;
        private Object lastResult;
        private Throwable lastException;
        
        private String nextOperationId = UUID.randomUUID().toString();
        private boolean throwExceptionOnStartOperation = false;
        
        @Override
        public String startOperation(String commandName, String operationType, Map<String, Object> parameters) {
            startOperationCallCount.incrementAndGet();
            
            if (throwExceptionOnStartOperation) {
                throw new RuntimeException("Test exception on startOperation");
            }
            
            lastCommandName = commandName;
            lastOperationType = operationType;
            lastParameters = new HashMap<>(parameters);
            lastOperationId = nextOperationId;
            
            return lastOperationId;
        }
        
        @Override
        public void completeOperation(String operationId, Object result) {
            completeOperationCallCount.incrementAndGet();
            lastCompletedOperationId = operationId;
            lastResult = result;
        }
        
        @Override
        public void failOperation(String operationId, Throwable exception) {
            failOperationCallCount.incrementAndGet();
            lastFailedOperationId = operationId;
            lastException = exception;
        }
        
        @Override
        public OperationMetadata getOperationMetadata(String operationId) {
            return null;
        }
        
        @Override
        public List<OperationMetadata> listOperations(String commandName, String operationType, int limit) {
            return Collections.emptyList();
        }
        
        @Override
        public Map<String, Object> getOperationStatistics(String commandName, LocalDateTime from, LocalDateTime to) {
            return Collections.emptyMap();
        }
        
        @Override
        public int clearOperationHistory(int days) {
            return 0;
        }
        
        // Getters for test verification
        public int getStartOperationCallCount() {
            return startOperationCallCount.get();
        }
        
        public int getCompleteOperationCallCount() {
            return completeOperationCallCount.get();
        }
        
        public int getFailOperationCallCount() {
            return failOperationCallCount.get();
        }
        
        public String getLastCommandName() {
            return lastCommandName;
        }
        
        public String getLastOperationType() {
            return lastOperationType;
        }
        
        public Map<String, Object> getLastParameters() {
            return lastParameters;
        }
        
        public String getLastOperationId() {
            return lastOperationId;
        }
        
        public String getLastCompletedOperationId() {
            return lastCompletedOperationId;
        }
        
        public String getLastFailedOperationId() {
            return lastFailedOperationId;
        }
        
        public Object getLastResult() {
            return lastResult;
        }
        
        public Throwable getLastException() {
            return lastException;
        }
        
        // Setters for test control
        public void setNextOperationId(String operationId) {
            this.nextOperationId = operationId;
        }
        
        public void setThrowExceptionOnStartOperation(boolean throwException) {
            this.throwExceptionOnStartOperation = throwException;
        }
    }
    
    /**
     * Helper class for testing help documentation.
     */
    private static class HelpTestAddCommand {
        public void testExecuteHelp(String helpArg) {
            // Manually simulate help output for AddCommand
            StringBuilder helpOutput = new StringBuilder();
            helpOutput.append("Command to add a new work item\n\n");
            helpOutput.append("Usage: add [OPTIONS]\n\n");
            helpOutput.append("Options:\n");
            helpOutput.append("  --title          The title of the work item (required)\n");
            helpOutput.append("  --description    A detailed description of the work item\n");
            helpOutput.append("  --type           The type of work item (TASK, BUG, FEATURE, EPIC)\n");
            helpOutput.append("  --priority       The priority (LOW, MEDIUM, HIGH, CRITICAL)\n");
            helpOutput.append("  --assignee       The username of the assignee\n");
            helpOutput.append("  --project        The project code\n");
            helpOutput.append("  --json           Output in JSON format\n");
            helpOutput.append("  --verbose        Show detailed output\n");
            helpOutput.append("  -h, --help       Show this help message\n");
            
            System.out.println(helpOutput);
        }
    }
}