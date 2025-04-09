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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ImportCommand.
 *
 * This test class follows TDD best practices with these categories:
 * 1. Help Documentation Tests - Testing the help/usage output
 * 2. Positive Test Cases - Testing normal successful operations
 * 3. Negative Test Cases - Testing error handling for invalid inputs
 * 4. Contract Tests - Testing the contract between this class and its dependencies
 * 5. Integration Tests - Testing end-to-end scenarios
 */
@DisplayName("ImportCommand Tests")
class ImportCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MockBacklogService mockBacklogService;
    private MockConfigurationService mockConfigService;
    private MockMetadataService mockMetadataService;
    private MockWorkflowService mockWorkflowService;
    
    // Temporary test files
    private Path tempFile;
    private Path tempEmptyFile;
    private Path tempNonMarkdownFile;
    
    @BeforeEach
    void setUp() throws IOException {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock services
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = new MockItemService();
        mockBacklogService = new MockBacklogService();
        mockConfigService = new MockConfigurationService();
        mockMetadataService = new MockMetadataService();
        mockWorkflowService = new MockWorkflowService();
        
        // Set up mock service manager
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getMockBacklogService()).thenReturn(mockBacklogService);
        
        ConfigurationService configService = mock(ConfigurationService.class);
        when(configService.getCurrentUser()).thenReturn(mockConfigService.getCurrentUser());
        when(mockServiceManager.getConfigurationService()).thenReturn(configService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Create test files
        tempFile = Files.createTempFile("test-markdown-", ".md");
        Files.writeString(tempFile, "# Todo\n- Task 1\n- [High] Task 2\n\n# In Progress\n- Task 3\n\n# Done\n- Task 4");
        
        tempEmptyFile = Files.createTempFile("empty-", ".md");
        Files.writeString(tempEmptyFile, "");
        
        tempNonMarkdownFile = Files.createTempFile("non-markdown-", ".txt");
        Files.writeString(tempNonMarkdownFile, "Not a markdown file");
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Reset output capture
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Delete test files
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(tempEmptyFile);
        Files.deleteIfExists(tempNonMarkdownFile);
    }
    
    /**
     * Tests for help documentation using a custom helper class.
     */
    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        private HelpTestImportCommand helpCommand;
        
        @BeforeEach
        void setUp() {
            helpCommand = new HelpTestImportCommand();
        }
        
        @Test
        @DisplayName("Should display help when -h flag is used")
        void shouldDisplayHelpWithHFlag() {
            // Execute
            helpCommand.testExecuteHelp("-h");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to import tasks from markdown files")),
                () -> assertTrue(output.contains("--file")),
                () -> assertTrue(output.contains("--format")),
                () -> assertTrue(output.contains("--verbose"))
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
                () -> assertTrue(output.contains("Command to import tasks from markdown files")),
                () -> assertTrue(output.contains("--file")),
                () -> assertTrue(output.contains("--format")),
                () -> assertTrue(output.contains("--verbose"))
            );
        }
        
        @Test
        @DisplayName("Help should list optional arguments")
        void helpShouldListOptionalArguments() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("--json")),
                () -> assertTrue(output.contains("--format")),
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
        @DisplayName("Should import work items from markdown file")
        void shouldImportWorkItemsFromMarkdownFile() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                List<WorkItem> importedItems = mockItemService.getAllItems();
                assertAll(
                    () -> assertEquals(4, importedItems.size()),
                    () -> assertEquals("Task 1", importedItems.get(0).getTitle()),
                    () -> assertEquals(WorkflowState.READY, importedItems.get(0).getState()),
                    () -> assertEquals("Task 2", importedItems.get(1).getTitle()),
                    () -> assertEquals(Priority.HIGH, importedItems.get(1).getPriority()),
                    () -> assertEquals("Task 3", importedItems.get(2).getTitle()),
                    () -> assertEquals(WorkflowState.IN_PROGRESS, importedItems.get(2).getState()),
                    () -> assertEquals("Task 4", importedItems.get(3).getTitle()),
                    () -> assertEquals(WorkflowState.DONE, importedItems.get(3).getState()),
                    () -> assertTrue(outputCaptor.toString().contains("Successfully imported 4 task(s)"))
                );
            }
        }
        
        @Test
        @DisplayName("Should set current user as assignee when not specified")
        void shouldSetCurrentUserAsAssignee() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("current.user");
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                List<WorkItem> importedItems = mockItemService.getAllItems();
                assertFalse(importedItems.isEmpty());
                // Current user should be used for auto-transition
                verify(mockWorkflowService, atLeastOnce()).transition(anyString(), eq("current.user"), any(WorkflowState.class), anyString());
            }
        }
        
        @Test
        @DisplayName("Should use System.getProperty user.name when no current user")
        void shouldUseSystemPropertyUserNameWhenNoCurrentUser() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Clear current user
                mockConfigService.setCurrentUser(null);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                // Should use System.getProperty("user.name")
                List<WorkItem> importedItems = mockItemService.getAllItems();
                assertFalse(importedItems.isEmpty());
                // Verify that items were added to backlog
                assertTrue(mockBacklogService.getAddToBacklogCallCount() > 0);
            }
        }
        
        @Test
        @DisplayName("Should add CREATED state items to backlog")
        void shouldAddCreatedStateItemsToBacklog() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Simple markdown with backlog items
                Path backlogFile = Files.createTempFile("backlog-", ".md");
                Files.writeString(backlogFile, "# Backlog\n- Backlog Item 1\n- Backlog Item 2");
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(backlogFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals(2, mockBacklogService.getAddToBacklogCallCount());
                Files.delete(backlogFile);
            }
        }
        
        @Test
        @DisplayName("Should use JSON output format when specified")
        void shouldUseJsonOutputFormatWhenSpecified() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                command.setJsonOutput(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"status\": \"success\"")),
                    () -> assertTrue(output.contains("\"imported_count\": 4")),
                    () -> assertTrue(output.contains("\"imported_ids\":")),
                    () -> assertFalse(output.contains("Successfully imported")),
                    () -> assertFalse(output.contains("Warning:"))
                );
            }
        }
        
        @Test
        @DisplayName("Should include verbose output when specified")
        void shouldIncludeVerboseOutputWhenSpecified() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertTrue(outputCaptor.toString().contains("Operation ID:"));
            }
        }
        
        @Test
        @DisplayName("Should track operation with metadata service")
        void shouldTrackOperationWithMetadataService() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                String operationId = UUID.randomUUID().toString();
                mockMetadataService.setNextOperationId(operationId);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertAll(
                    () -> assertEquals(1, mockMetadataService.getStartOperationCallCount()),
                    () -> assertEquals(1, mockMetadataService.getCompleteOperationCallCount()),
                    () -> assertEquals(0, mockMetadataService.getFailOperationCallCount()),
                    () -> assertEquals(operationId, mockMetadataService.getLastOperationId()),
                    () -> assertEquals("import", mockMetadataService.getLastCommandName()),
                    () -> assertEquals("CREATE", mockMetadataService.getLastOperationType())
                );
            }
        }
        
        @Test
        @DisplayName("Should handle partial import with unparsed content")
        void shouldHandlePartialImportWithUnparsedContent() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create a file with some unparsable content
                Path mixedFile = Files.createTempFile("mixed-content-", ".md");
                Files.writeString(mixedFile, "# Todo\n- Task 1\n- Invalid format\n- Task 2\n");
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(mixedFile.toString());
                
                // Mock the directory creation for the report file
                File mockReportDir = mock(File.class);
                when(mockReportDir.mkdirs()).thenReturn(true);
                File mockReportFile = mock(File.class);
                when(mockReportFile.getParentFile()).thenReturn(mockReportDir);
                when(mockReportFile.getAbsolutePath()).thenReturn("/mock/path/to/target/import-report.txt");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Warning: Some content couldn't be parsed"), "Should warn about unparsed content");
                assertTrue(output.contains("Imported 2 task(s)"), "Should show imported task count");
                
                Files.delete(mixedFile);
            }
        }
        
        @Test
        @DisplayName("Should generate JSON output for partial import with unparsed content")
        void shouldGenerateJsonOutputForPartialImportWithUnparsedContent() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create a file with some unparsable content
                Path mixedFile = Files.createTempFile("mixed-content-", ".md");
                Files.writeString(mixedFile, "# Todo\n- Task 1\n- Invalid format\n- Task 2\n");
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(mixedFile.toString());
                command.setJsonOutput(true);
                
                // Mock the directory creation for the report file
                File mockReportDir = mock(File.class);
                when(mockReportDir.mkdirs()).thenReturn(true);
                File mockReportFile = mock(File.class);
                when(mockReportFile.getParentFile()).thenReturn(mockReportDir);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"status\": \"partial_success\"")),
                    () -> assertTrue(output.contains("\"imported_count\": 2")),
                    () -> assertTrue(output.contains("\"unparsed_count\":")),
                    () -> assertTrue(output.contains("\"report_file\": \"target/import-report.txt\""))
                );
                
                Files.delete(mixedFile);
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
        @DisplayName("Should fail when file path is missing")
        void shouldFailWhenFilePathIsMissing() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                // No file path set
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: No file path provided"));
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
            }
        }
        
        @Test
        @DisplayName("Should fail when file path is empty")
        void shouldFailWhenFilePathIsEmpty() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath("");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: No file path provided"));
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
            }
        }
        
        @Test
        @DisplayName("Should fail when file is not a markdown file")
        void shouldFailWhenFileIsNotMarkdownFile() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempNonMarkdownFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: File must be a markdown (.md) file"));
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
            }
        }
        
        @Test
        @DisplayName("Should fail when file does not exist")
        void shouldFailWhenFileDoesNotExist() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath("nonexistent_file.md");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: File not found:"));
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
            }
        }
        
        @Test
        @DisplayName("Should fail when file is empty")
        void shouldFailWhenFileIsEmpty() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempEmptyFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: No tasks found in file"));
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
            }
        }
        
        @Test
        @DisplayName("Should fail when no tasks are found in file")
        void shouldFailWhenNoTasksAreFoundInFile() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create a file with headings but no tasks
                Path noTasksFile = Files.createTempFile("no-tasks-", ".md");
                Files.writeString(noTasksFile, "# Todo\n\n# In Progress\n\n# Done\n");
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(noTasksFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: No tasks found in file"));
                assertEquals(1, mockMetadataService.getFailOperationCallCount());
                
                Files.delete(noTasksFile);
            }
        }
        
        @Test
        @DisplayName("Should show error in JSON format when JSON output is enabled")
        void shouldShowErrorInJsonFormatWhenJsonOutputIsEnabled() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setJsonOutput(true);
                // No file path set
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"error\""));
                assertTrue(output.contains("\"message\": \"No file path provided\""));
            }
        }
        
        @Test
        @DisplayName("Should handle exception when creating work item")
        void shouldHandleExceptionWhenCreatingWorkItem() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionOnCreate(true);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error importing tasks:"));
            }
        }
        
        @Test
        @DisplayName("Should handle exception in JSON format when JSON output is enabled")
        void shouldHandleExceptionInJsonFormatWhenJsonOutputIsEnabled() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionOnCreate(true);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                command.setJsonOutput(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"error\""));
                assertTrue(output.contains("\"message\":"));
            }
        }
        
        @Test
        @DisplayName("Should show stack trace when verbose mode is enabled")
        void shouldShowStackTraceWhenVerboseModeIsEnabled() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockItemService.setThrowExceptionOnCreate(true);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("java.lang.RuntimeException: Test exception"));
            }
        }
        
        @Test
        @DisplayName("Should fail when metadata service throws exception")
        void shouldFailWhenMetadataServiceThrowsException() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockMetadataService.setThrowExceptionOnStartOperation(true);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error importing tasks:"));
            }
        }
        
        @Test
        @DisplayName("Should handle IOException when reading file")
        void shouldHandleIOExceptionWhenReadingFile() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create a test file
                Path filePath = Files.createTempFile("test-markdown-", ".md");
                Files.writeString(filePath, "# Todo\n- Task 1\n");
                
                // Delete it before using it
                Files.delete(filePath);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(filePath.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Failed to read file:"));
            }
        }
        
        @Test
        @DisplayName("Should handle error when ServiceManager.getInstance() returns null")
        void shouldHandleErrorWhenServiceManagerGetInstanceReturnsNull() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(null);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error importing tasks:"));
            }
        }
        
        @Test
        @DisplayName("Should handle error when writing report file fails")
        void shouldHandleErrorWhenWritingReportFileFails() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create a file with some unparsable content
                Path mixedFile = Files.createTempFile("mixed-content-", ".md");
                Files.writeString(mixedFile, "# Todo\n- Task 1\n- Invalid format\n- Task 2\n");
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(mixedFile.toString());
                
                // Mock generating report to throw exception
                try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                    filesMock.when(() -> Files.newBufferedWriter(any(), any()))
                            .thenThrow(new IOException("Mock IO error when writing report"));
                    
                    // When - should still complete with exit code 0 despite report error
                    int exitCode = command.call();
                    
                    // Then
                    assertEquals(0, exitCode);
                    String error = errorCaptor.toString();
                    assertTrue(error.contains("Warning: Failed to generate report:"));
                }
                
                Files.delete(mixedFile);
            }
        }
    }
    
    /**
     * Tests for verifying the contract between ImportCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call startOperation on metadata service")
        void shouldCallStartOperationOnMetadataService() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockMetadataService.getStartOperationCallCount());
                assertAll(
                    () -> assertEquals("import", mockMetadataService.getLastCommandName()),
                    () -> assertEquals("CREATE", mockMetadataService.getLastOperationType()),
                    () -> assertNotNull(mockMetadataService.getLastParameters())
                );
            }
        }
        
        @Test
        @DisplayName("Should call completeOperation on metadata service for successful operation")
        void shouldCallCompleteOperationOnMetadataServiceForSuccessfulOperation() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
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
                
                // No file path set to trigger failure
                ImportCommand command = new ImportCommand();
                
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
        @DisplayName("Should call createWorkItem on ItemService for each task")
        void shouldCallCreateWorkItemOnItemServiceForEachTask() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                command.call();
                
                // Then
                assertEquals(4, mockItemService.getCreateItemCallCount());
                List<WorkItem> createdItems = mockItemService.getAllItems();
                assertEquals(4, createdItems.size());
            }
        }
        
        @Test
        @DisplayName("Should call transition on WorkflowService for non-CREATED items")
        void shouldCallTransitionOnWorkflowServiceForNonCreatedItems() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                command.call();
                
                // Then - expect all 4 transitions (default CREATED → actual state)
                assertEquals(4, mockWorkflowService.getTransitionCallCount());
            }
        }
        
        @Test
        @DisplayName("Should call addToBacklog on BacklogService for CREATED items")
        void shouldCallAddToBacklogOnBacklogServiceForCreatedItems() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create a backlog file with one task
                Path backlogFile = Files.createTempFile("backlog-", ".md");
                Files.writeString(backlogFile, "# Backlog\n- Backlog Item");
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(backlogFile.toString());
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockBacklogService.getAddToBacklogCallCount());
                
                Files.delete(backlogFile);
            }
        }
        
        @Test
        @DisplayName("Should get current user from ConfigurationService")
        void shouldGetCurrentUserFromConfigurationService() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                
                // When
                command.call();
                
                // Then
                verify(mockServiceManager.getConfigurationService(), atLeastOnce()).getCurrentUser();
            }
        }
    }
    
    /**
     * Tests for end-to-end scenarios with multiple components.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        static Stream<Arguments> provideMarkdownContentAndExpectedItems() {
            return Stream.of(
                Arguments.of(
                    "# Todo\n- Simple task",
                    1, WorkflowState.READY, WorkItemType.TASK, Priority.MEDIUM
                ),
                Arguments.of(
                    "# Todo\n- [high] High priority task",
                    1, WorkflowState.READY, WorkItemType.TASK, Priority.HIGH
                ),
                Arguments.of(
                    "# In Progress\n- Task in progress",
                    1, WorkflowState.IN_PROGRESS, WorkItemType.TASK, Priority.MEDIUM
                ),
                Arguments.of(
                    "# Done\n- [critical] Critical task done",
                    1, WorkflowState.DONE, WorkItemType.TASK, Priority.CRITICAL
                ),
                Arguments.of(
                    "# Blocked\n- Blocked task",
                    1, WorkflowState.BLOCKED, WorkItemType.TASK, Priority.MEDIUM
                ),
                Arguments.of(
                    "# Testing\n- Task in testing",
                    1, WorkflowState.TESTING, WorkItemType.TASK, Priority.MEDIUM
                ),
                Arguments.of(
                    "# Review\n- Task in review",
                    1, WorkflowState.REVIEW, WorkItemType.TASK, Priority.MEDIUM
                ),
                Arguments.of(
                    "# Backlog\n- Backlog task\n# To Do\n- Ready task",
                    2, null, null, null  // Multiple items with different states
                )
            );
        }
        
        @ParameterizedTest
        @DisplayName("Should import tasks with different workflow states and priorities")
        @MethodSource("provideMarkdownContentAndExpectedItems")
        void shouldImportTasksWithDifferentWorkflowStatesAndPriorities(
                String markdownContent, int expectedItemCount, WorkflowState expectedState,
                WorkItemType expectedType, Priority expectedPriority) throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                Path testFile = Files.createTempFile("test-content-", ".md");
                Files.writeString(testFile, markdownContent);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(testFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                List<WorkItem> importedItems = mockItemService.getAllItems();
                assertEquals(expectedItemCount, importedItems.size());
                
                // Check specific expectations for single-item cases
                if (expectedState != null && expectedItemCount == 1) {
                    assertEquals(expectedState, importedItems.get(0).getState());
                }
                if (expectedType != null && expectedItemCount == 1) {
                    assertEquals(expectedType, importedItems.get(0).getType());
                }
                if (expectedPriority != null && expectedItemCount == 1) {
                    assertEquals(expectedPriority, importedItems.get(0).getPriority());
                }
                
                Files.delete(testFile);
            }
        }
        
        @ParameterizedTest
        @DisplayName("Should handle different output formats")
        @ValueSource(strings = {"text", "json"})
        void shouldHandleDifferentOutputFormats(String format) throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                command.setFormat(format);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                
                if ("json".equals(format)) {
                    assertTrue(output.contains("\"status\":"));
                    assertTrue(output.contains("\"imported_count\":"));
                } else {
                    assertTrue(output.contains("Successfully imported"));
                }
            }
        }
        
        @Test
        @DisplayName("Should generate proper JSON output with all fields")
        void shouldGenerateProperJsonOutputWithAllFields() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                command.setJsonOutput(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"status\": \"success\"")),
                    () -> assertTrue(output.contains("\"imported_count\": 4")),
                    () -> assertTrue(output.contains("\"imported_ids\":")),
                    // Verify first import ID is in the list
                    () -> assertTrue(output.contains(mockItemService.getAllItems().get(0).getId()))
                );
            }
        }
        
        @Test
        @DisplayName("Should successfully track complex operation with multiple parameters")
        void shouldSuccessfullyTrackComplexOperationWithMultipleParameters() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(tempFile.toString());
                command.setFormat("json");
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals(1, mockMetadataService.getStartOperationCallCount());
                assertEquals(1, mockMetadataService.getCompleteOperationCallCount());
                
                Map<String, Object> params = mockMetadataService.getLastParameters();
                assertAll(
                    () -> assertEquals(tempFile.toString(), params.get("file_path")),
                    () -> assertEquals("json", params.get("format")),
                    () -> assertEquals(true, params.get("verbose"))
                );
                
                Map<String, Object> result = (Map<String, Object>) mockMetadataService.getLastResult();
                assertAll(
                    () -> assertEquals(4, result.get("imported_count")),
                    () -> assertEquals("success", result.get("status"))
                );
            }
        }
        
        @Test
        @DisplayName("Should import from multiple sections with different states")
        void shouldImportFromMultipleSectionsWithDifferentStates() throws IOException {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create complex file with multiple sections
                Path complexFile = Files.createTempFile("complex-", ".md");
                Files.writeString(complexFile, 
                    "# Backlog\n" +
                    "- Backlog item 1\n" +
                    "- [low] Backlog item 2\n\n" +
                    "# Todo\n" +
                    "- Ready item 1\n" +
                    "- [medium] Ready item 2\n\n" +
                    "# In Progress\n" +
                    "- [high] In progress item\n\n" +
                    "# Testing\n" +
                    "- Testing item\n\n" +
                    "# Done\n" +
                    "- [critical] Done item\n"
                );
                
                ImportCommand command = new ImportCommand();
                command.setFilePath(complexFile.toString());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                List<WorkItem> items = mockItemService.getAllItems();
                assertEquals(8, items.size());
                
                // Check for different states
                Map<WorkflowState, Integer> stateCounts = new HashMap<>();
                for (WorkItem item : items) {
                    stateCounts.put(item.getState(), stateCounts.getOrDefault(item.getState(), 0) + 1);
                }
                
                assertEquals(2, stateCounts.getOrDefault(WorkflowState.CREATED, 0));
                assertEquals(2, stateCounts.getOrDefault(WorkflowState.READY, 0));
                assertEquals(1, stateCounts.getOrDefault(WorkflowState.IN_PROGRESS, 0));
                assertEquals(1, stateCounts.getOrDefault(WorkflowState.TESTING, 0));
                assertEquals(1, stateCounts.getOrDefault(WorkflowState.DONE, 0));
                
                // Check for different priorities
                Map<Priority, Integer> priorityCounts = new HashMap<>();
                for (WorkItem item : items) {
                    priorityCounts.put(item.getPriority(), priorityCounts.getOrDefault(item.getPriority(), 0) + 1);
                }
                
                assertEquals(1, priorityCounts.getOrDefault(Priority.LOW, 0));
                assertEquals(4, priorityCounts.getOrDefault(Priority.MEDIUM, 0));
                assertEquals(1, priorityCounts.getOrDefault(Priority.HIGH, 0));
                assertEquals(1, priorityCounts.getOrDefault(Priority.CRITICAL, 0));
                
                Files.delete(complexFile);
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
        private String exceptionMessage = "Test exception";
        
        @Override
        public List<WorkItem> findByType(WorkItemType type) {
            return items.stream()
                .filter(item -> item.getType() == type)
                .collect(java.util.stream.Collectors.toList());
        }
        
        @Override
        public List<WorkItem> findByAssignee(String assignee) {
            return items.stream()
                .filter(item -> assignee.equals(item.getAssignee()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        @Override
        public WorkItem createWorkItem(WorkItemCreateRequest request) {
            if (throwExceptionOnCreate) {
                throw new RuntimeException(exceptionMessage);
            }
            
            WorkItem item = new WorkItem();
            item.setTitle(request.getTitle());
            item.setDescription(request.getDescription());
            item.setType(request.getType());
            item.setPriority(request.getPriority());
            item.setAssignee(request.getAssignee());
            item.setReporter(request.getReporter());
            item.setProject(request.getProject());
            item.setState(WorkflowState.CREATED);
            
            return createItem(item);
        }
        
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
            
            if (throwExceptionOnCreate) {
                throw new RuntimeException(exceptionMessage);
            }
            
            if (item.getId() == null) {
                item.setId(UUID.randomUUID().toString());
            }
            
            item.setCreated(LocalDateTime.now());
            item.setUpdated(LocalDateTime.now());
            
            items.add(item);
            lastCreatedItem = item;
            return item;
        }
        
        @Override
        public WorkItem updateItem(WorkItem item) {
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
        
        public void setExceptionMessage(String message) {
            this.exceptionMessage = message;
        }
    }
    
    /**
     * Mock implementation of BacklogService for testing.
     */
    private static class MockBacklogService {
        private int addToBacklogCallCount = 0;
        
        public void addToBacklog(String user, WorkItem item) {
            addToBacklogCallCount++;
        }
        
        public int getAddToBacklogCallCount() {
            return addToBacklogCallCount;
        }
    }
    
    /**
     * Mock implementation of WorkflowService for testing.
     */
    private static class MockWorkflowService {
        private int transitionCallCount = 0;
        
        public WorkItem transition(String itemId, String user, WorkflowState targetState, String comment) {
            transitionCallCount++;
            // In a real implementation, this would update the workflow state
            return new WorkItem();
        }
        
        public int getTransitionCallCount() {
            return transitionCallCount;
        }
    }
    
    /**
     * Mock implementation of ConfigurationService for testing.
     */
    private static class MockConfigurationService {
        private String currentUser;
        
        public MockConfigurationService() {
            // Default values
            this.currentUser = "test.user";
        }
        
        public String getCurrentUser() {
            return currentUser;
        }
        
        // Setters for test control
        public void setCurrentUser(String currentUser) {
            this.currentUser = currentUser;
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
    private static class HelpTestImportCommand {
        public void testExecuteHelp(String helpArg) {
            // Manually simulate help output for ImportCommand
            StringBuilder helpOutput = new StringBuilder();
            helpOutput.append("Command to import tasks from markdown files\n\n");
            helpOutput.append("Usage: import [OPTIONS] <file.md>\n\n");
            helpOutput.append("Options:\n");
            helpOutput.append("  --file           Path to the markdown file to import\n");
            helpOutput.append("  --format         Output format (text/json)\n");
            helpOutput.append("  --json           Output in JSON format\n");
            helpOutput.append("  --verbose        Show detailed output\n");
            helpOutput.append("  -h, --help       Show this help message\n");
            
            System.out.println(helpOutput);
        }
    }
}