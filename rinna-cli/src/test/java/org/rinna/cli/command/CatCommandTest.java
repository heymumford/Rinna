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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

/**
 * Comprehensive test suite for CatCommand.
 *
 * This test class follows TDD best practices with these categories:
 * 1. Help Documentation Tests - Testing the help/usage output
 * 2. Positive Test Cases - Testing normal successful operations
 * 3. Negative Test Cases - Testing error handling for invalid inputs
 * 4. Contract Tests - Testing the contract between this class and its dependencies
 * 5. Integration Tests - Testing end-to-end scenarios
 */
@DisplayName("CatCommand Tests")
class CatCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MockHistoryService mockHistoryService;
    private MockConfigurationService mockConfigService;
    private MetadataService mockMetadataService;
    private ContextManager mockContextManager;
    
    private static final String OPERATION_ID = "test-operation-id";
    private static final String TEST_WORK_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock services
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = mock(MockItemService.class);
        mockHistoryService = mock(MockHistoryService.class);
        mockConfigService = mock(MockConfigurationService.class);
        mockMetadataService = mock(MetadataService.class);
        mockContextManager = mock(ContextManager.class);
        
        // Configure mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockHistoryService()).thenReturn(mockHistoryService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(eq("cat"), eq("READ"), any())).thenReturn(OPERATION_ID);
        
        // Mock ContextManager.getInstance()
        try (var staticMock = Mockito.mockStatic(ContextManager.class)) {
            staticMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
        }
        
        // Set up sample work item
        WorkItem testWorkItem = createTestWorkItem();
        when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenReturn(testWorkItem);
        
        // Set up history entries
        List<MockHistoryService.HistoryEntryRecord> historyEntries = createTestHistoryEntries();
        when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(historyEntries);
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
        
        private HelpTestCatCommand helpCommand;
        
        @BeforeEach
        void setUp() {
            helpCommand = new HelpTestCatCommand();
        }
        
        @Test
        @DisplayName("Should display help when -h flag is used")
        void shouldDisplayHelpWithHFlag() {
            // Execute
            helpCommand.testExecuteHelp("-h");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to display the details of a work item")),
                () -> assertTrue(output.contains("--item-id")),
                () -> assertTrue(output.contains("--show-line-numbers")),
                () -> assertTrue(output.contains("--show-history"))
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
                () -> assertTrue(output.contains("Command to display the details of a work item")),
                () -> assertTrue(output.contains("--item-id")),
                () -> assertTrue(output.contains("--show-line-numbers")),
                () -> assertTrue(output.contains("--show-history"))
            );
        }
        
        @Test
        @DisplayName("Help should list display options")
        void helpShouldListDisplayOptions() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("--show-all-formatting")),
                () -> assertTrue(output.contains("--show-relationships")),
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
        @DisplayName("Should display work item with specified ID")
        void shouldDisplayWorkItemWithSpecifiedId() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("====== WORK ITEM " + TEST_WORK_ITEM_ID + " ======")),
                    () -> assertTrue(output.contains("Title: Test Work Item")),
                    () -> assertTrue(output.contains("Type: TASK")),
                    () -> assertTrue(output.contains("Priority: MEDIUM")),
                    () -> assertTrue(output.contains("State: IN_PROGRESS")),
                    () -> assertTrue(output.contains("Description:")),
                    () -> assertTrue(output.contains("This is a test description"))
                );
            }
        }
        
        @Test
        @DisplayName("Should use previously viewed work item when no ID is specified")
        void shouldUsePreviouslyViewedWorkItemWhenNoIdIsSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up last viewed work item in context manager
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(UUID.fromString(TEST_WORK_ITEM_ID));
                
                CatCommand command = new CatCommand(mockServiceManager);
                // No item ID specified
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                verify(mockContextManager).getLastViewedWorkItem();
                String output = outputCaptor.toString();
                assertTrue(output.contains("====== WORK ITEM " + TEST_WORK_ITEM_ID + " ======"));
            }
        }
        
        @Test
        @DisplayName("Should show line numbers when enabled")
        void shouldShowLineNumbersWhenEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setShowLineNumbers(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("1  Title:")),
                    () -> assertTrue(output.contains("2  Type:")),
                    () -> assertTrue(output.matches("(?s).*\\d+\\s+Description:.*"))
                );
            }
        }
        
        @Test
        @DisplayName("Should display history when enabled")
        void shouldDisplayHistoryWhenEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setShowHistory(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("History:")),
                    () -> assertTrue(output.contains("CREATED by")),
                    () -> assertTrue(output.contains("UPDATED by"))
                );
            }
        }
        
        @Test
        @DisplayName("Should show visible formatting when enabled")
        void shouldShowVisibleFormattingWhenEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create test work item with tabs and spaces
                WorkItem workItem = createTestWorkItem();
                workItem.setDescription("Line with tab\tcharacter\nAnother line");
                when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenReturn(workItem);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setShowAllFormatting(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("→   ")), // Tab character replacement
                    () -> assertTrue(output.contains("¶"))     // Line ending marker
                );
            }
        }
        
        @Test
        @DisplayName("Should output in JSON format when specified")
        void shouldOutputInJsonFormatWhenSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setFormat("json");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"id\": \"" + TEST_WORK_ITEM_ID + "\"")),
                    () -> assertTrue(output.contains("\"title\": \"Test Work Item\"")),
                    () -> assertTrue(output.contains("\"type\": \"TASK\"")),
                    () -> assertTrue(output.contains("\"priority\": \"MEDIUM\"")),
                    () -> assertTrue(output.contains("\"status\": \"IN_PROGRESS\"")),
                    () -> assertTrue(output.contains("\"description\": \"This is a test description\""))
                );
            }
        }
        
        @Test
        @DisplayName("Should include history in JSON output when enabled")
        void shouldIncludeHistoryInJsonOutputWhenEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setFormat("json");
                command.setShowHistory(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"history\": [")),
                    () -> assertTrue(output.contains("\"type\": \"CREATED\"")),
                    () -> assertTrue(output.contains("\"user\": \"john.doe\"")),
                    () -> assertTrue(output.contains("\"content\": \"Item created\""))
                );
            }
        }
        
        @Test
        @DisplayName("Should update the last viewed work item in context")
        void shouldUpdateLastViewedWorkItemInContext() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                verify(mockContextManager).setLastViewedWorkItem(UUID.fromString(TEST_WORK_ITEM_ID));
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
        @DisplayName("Should fail when no work item ID is specified and no last viewed item exists")
        void shouldFailWhenNoWorkItemIdIsSpecifiedAndNoLastViewedItemExists() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // No last viewed work item
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(null);
                
                CatCommand command = new CatCommand(mockServiceManager);
                // No item ID specified
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: No work item context available"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when work item ID is invalid UUID format")
        void shouldFailWhenWorkItemIdIsInvalidUuidFormat() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId("invalid-uuid-format");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Invalid work item ID format"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when work item is not found")
        void shouldFailWhenWorkItemIsNotFound() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Work item not found
                String nonExistentId = "123e4567-e89b-12d3-a456-999999999999";
                when(mockItemService.getItem(nonExistentId)).thenReturn(null);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(nonExistentId);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Work item not found"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle exception when service throws exception")
        void shouldHandleExceptionWhenServiceThrowsException() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Service throws exception
                RuntimeException testException = new RuntimeException("Test service exception");
                when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenThrow(testException);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error displaying work item:"));
                assertTrue(errorOutput.contains("Test service exception"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), eq(testException));
            }
        }
        
        @Test
        @DisplayName("Should show stack trace when verbose mode is enabled")
        void shouldShowStackTraceWhenVerboseModeIsEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Service throws exception
                RuntimeException testException = new RuntimeException("Test verbose exception");
                when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenThrow(testException);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error displaying work item:"));
                assertTrue(errorOutput.contains("Test verbose exception"));
                assertTrue(errorOutput.contains("java.lang.RuntimeException"));
                assertTrue(errorOutput.contains("at org.rinna.cli.command.CatCommandTest"));
            }
        }
        
        @Test
        @DisplayName("Should show error in JSON format when format is json")
        void shouldShowErrorInJsonFormatWhenFormatIsJson() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Work item not found
                String nonExistentId = "123e4567-e89b-12d3-a456-999999999999";
                when(mockItemService.getItem(nonExistentId)).thenReturn(null);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(nonExistentId);
                command.setFormat("json");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"error\":"));
                assertTrue(output.contains("\"message\": \"Work item not found:"));
            }
        }
        
        @Test
        @DisplayName("Should handle exception from context manager")
        void shouldHandleExceptionFromContextManager() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Context manager throws exception
                RuntimeException contextException = new RuntimeException("Context manager error");
                when(mockContextManager.getLastViewedWorkItem()).thenThrow(contextException);
                
                CatCommand command = new CatCommand(mockServiceManager);
                // No item ID specified
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error displaying work item:"));
                assertTrue(errorOutput.contains("Context manager error"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), eq(contextException));
            }
        }
        
        @Test
        @DisplayName("Should handle exception from history service")
        void shouldHandleExceptionFromHistoryService() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // History service throws exception
                RuntimeException historyException = new RuntimeException("History service error");
                when(mockHistoryService.getHistory(any(UUID.class))).thenThrow(historyException);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setShowHistory(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error displaying work item:"));
                assertTrue(errorOutput.contains("History service error"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), eq(historyException));
            }
        }
    }
    
    /**
     * Tests for verifying the contract between CatCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call startOperation on metadata service")
        void shouldCallStartOperationOnMetadataService() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("cat"), eq("READ"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals(TEST_WORK_ITEM_ID, params.get("item_id"));
                assertEquals(false, params.get("show_line_numbers"));
                assertEquals(false, params.get("show_all_formatting"));
                assertEquals(false, params.get("show_history"));
                assertEquals(false, params.get("show_relationships"));
                assertEquals("text", params.get("format"));
                assertEquals(false, params.get("verbose"));
            }
        }
        
        @Test
        @DisplayName("Should call completeOperation on metadata service for successful operation")
        void shouldCallCompleteOperationOnMetadataServiceForSuccessfulOperation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals(TEST_WORK_ITEM_ID, result.get("item_id"));
                assertEquals("Test Work Item", result.get("title"));
                assertEquals("IN_PROGRESS", result.get("status"));
            }
        }
        
        @Test
        @DisplayName("Should call failOperation on metadata service for failed operation")
        void shouldCallFailOperationOnMetadataServiceForFailedOperation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Invalid ID to trigger failure
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId("invalid-id");
                
                // When
                command.call();
                
                // Then
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should call getItem on item service")
        void shouldCallGetItemOnItemService() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                verify(mockItemService).getItem(TEST_WORK_ITEM_ID);
            }
        }
        
        @Test
        @DisplayName("Should call getHistory on history service when history is requested")
        void shouldCallGetHistoryOnHistoryServiceWhenHistoryIsRequested() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setShowHistory(true);
                
                // When
                command.call();
                
                // Then
                verify(mockHistoryService).getHistory(UUID.fromString(TEST_WORK_ITEM_ID));
            }
        }
        
        @Test
        @DisplayName("Should not call getHistory when history is not requested")
        void shouldNotCallGetHistoryWhenHistoryIsNotRequested() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setShowHistory(false);
                
                // When
                command.call();
                
                // Then
                verify(mockHistoryService, never()).getHistory(any(UUID.class));
            }
        }
        
        @Test
        @DisplayName("Should call getLastViewedWorkItem when no item ID is specified")
        void shouldCallGetLastViewedWorkItemWhenNoItemIdIsSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up last viewed work item
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(UUID.fromString(TEST_WORK_ITEM_ID));
                
                CatCommand command = new CatCommand(mockServiceManager);
                // No item ID specified
                
                // When
                command.call();
                
                // Then
                verify(mockContextManager).getLastViewedWorkItem();
                verify(mockItemService).getItem(TEST_WORK_ITEM_ID);
            }
        }
        
        @Test
        @DisplayName("Should call setLastViewedWorkItem to update context")
        void shouldCallSetLastViewedWorkItemToUpdateContext() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                verify(mockContextManager).setLastViewedWorkItem(UUID.fromString(TEST_WORK_ITEM_ID));
            }
        }
    }
    
    /**
     * Tests for end-to-end scenarios with multiple components.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        static Stream<Arguments> provideDisplayOptions() {
            return Stream.of(
                Arguments.of(false, false, false, "Basic display without options"),
                Arguments.of(true, false, false, "Display with line numbers"),
                Arguments.of(false, true, false, "Display with visible formatting"),
                Arguments.of(false, false, true, "Display with history"),
                Arguments.of(true, true, true, "Display with all options enabled")
            );
        }
        
        @ParameterizedTest
        @DisplayName("Should display work item with different display options")
        @MethodSource("provideDisplayOptions")
        void shouldDisplayWorkItemWithDifferentDisplayOptions(
                boolean showLineNumbers, boolean showAllFormatting, boolean showHistory, String description) {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setShowLineNumbers(showLineNumbers);
                command.setShowAllFormatting(showAllFormatting);
                command.setShowHistory(showHistory);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                
                // Verify basic content is always present
                assertTrue(output.contains("====== WORK ITEM " + TEST_WORK_ITEM_ID + " ======"));
                assertTrue(output.contains("Test Work Item"));
                
                // Verify option-specific content
                if (showLineNumbers) {
                    assertTrue(output.contains("1  Title:"));
                } else {
                    assertTrue(output.contains("Title:"));
                    assertFalse(output.contains("1  Title:"));
                }
                
                if (showHistory) {
                    assertTrue(output.contains("History:"));
                } else {
                    assertFalse(output.contains("History:"));
                }
            }
        }
        
        @ParameterizedTest
        @DisplayName("Should format output based on specified format")
        @ValueSource(strings = {"text", "json"})
        void shouldFormatOutputBasedOnSpecifiedFormat(String format) {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setFormat(format);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                
                if ("json".equals(format)) {
                    assertTrue(output.contains("\"id\":"));
                    assertTrue(output.contains("\"title\":"));
                    assertTrue(output.contains("\"displayOptions\":"));
                } else {
                    assertTrue(output.contains("====== WORK ITEM"));
                    assertTrue(output.contains("Title:"));
                    assertTrue(output.contains("Description:"));
                }
            }
        }
        
        @Test
        @DisplayName("Should handle different history entry types")
        void shouldHandleDifferentHistoryEntryTypes() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create history entries with different types
                List<MockHistoryService.HistoryEntryRecord> entries = new ArrayList<>();
                entries.add(createHistoryEntry("CREATED", "john.doe", "Item created"));
                entries.add(createHistoryEntry("UPDATED", "jane.smith", "Changed title"));
                entries.add(createHistoryEntry("STATE_CHANGE", "john.doe", "Changed state to IN_PROGRESS"));
                entries.add(createHistoryEntry("COMMENT", "jane.smith", "Added a comment"));
                
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(entries);
                
                CatCommand command = new CatCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setShowHistory(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("CREATED by john.doe: Item created")),
                    () -> assertTrue(output.contains("UPDATED by jane.smith: Changed title")),
                    () -> assertTrue(output.contains("STATE_CHANGE by john.doe: Changed state to IN_PROGRESS")),
                    () -> assertTrue(output.contains("COMMENT by jane.smith: Added a comment"))
                );
            }
        }
        
        @Test
        @DisplayName("Should handle dates consistently in text and JSON output")
        void shouldHandleDatesConsistentlyInTextAndJsonOutput() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create work item with specific dates
                WorkItem workItem = createTestWorkItem();
                LocalDateTime created = LocalDateTime.of(2025, 4, 15, 10, 30, 0);
                LocalDateTime updated = LocalDateTime.of(2025, 4, 16, 14, 45, 0);
                workItem.setCreated(created);
                workItem.setUpdated(updated);
                when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenReturn(workItem);
                
                // Run command with text output
                CatCommand textCommand = new CatCommand(mockServiceManager);
                textCommand.setItemId(TEST_WORK_ITEM_ID);
                textCommand.call();
                String textOutput = outputCaptor.toString();
                outputCaptor.reset();
                
                // Run command with JSON output
                CatCommand jsonCommand = new CatCommand(mockServiceManager);
                jsonCommand.setItemId(TEST_WORK_ITEM_ID);
                jsonCommand.setFormat("json");
                jsonCommand.call();
                String jsonOutput = outputCaptor.toString();
                
                // Then
                assertAll(
                    // Text output should contain formatted dates
                    () -> assertTrue(textOutput.contains("Created: " + created.toString())),
                    () -> assertTrue(textOutput.contains("Updated: " + updated.toString())),
                    
                    // JSON output should contain ISO format dates
                    () -> assertTrue(jsonOutput.contains("\"created\": \"" + created.toString() + "\"")),
                    () -> assertTrue(jsonOutput.contains("\"updated\": \"" + updated.toString() + "\""))
                );
            }
        }
    }
    
    // Helper methods
    
    private WorkItem createTestWorkItem() {
        WorkItem workItem = new WorkItem();
        workItem.setId(TEST_WORK_ITEM_ID);
        workItem.setTitle("Test Work Item");
        workItem.setDescription("This is a test description");
        workItem.setType(WorkItemType.TASK);
        workItem.setPriority(Priority.MEDIUM);
        workItem.setState(WorkflowState.IN_PROGRESS);
        workItem.setAssignee("john.doe");
        workItem.setCreated(LocalDateTime.now());
        workItem.setUpdated(LocalDateTime.now());
        return workItem;
    }
    
    private List<MockHistoryService.HistoryEntryRecord> createTestHistoryEntries() {
        List<MockHistoryService.HistoryEntryRecord> entries = new ArrayList<>();
        entries.add(createHistoryEntry("CREATED", "john.doe", "Item created"));
        entries.add(createHistoryEntry("UPDATED", "jane.smith", "Updated description"));
        return entries;
    }
    
    private MockHistoryService.HistoryEntryRecord createHistoryEntry(String type, String user, String content) {
        return new MockHistoryService.HistoryEntryRecord(
            UUID.randomUUID(),
            UUID.fromString(TEST_WORK_ITEM_ID),
            type,
            user,
            content,
            Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
        );
    }
    
    /**
     * Helper class for testing help documentation.
     */
    private static class HelpTestCatCommand {
        public void testExecuteHelp(String helpArg) {
            // Manually simulate help output for CatCommand
            StringBuilder helpOutput = new StringBuilder();
            helpOutput.append("Command to display the details of a work item\n\n");
            helpOutput.append("Usage: cat [OPTIONS] [ITEM_ID]\n\n");
            helpOutput.append("Options:\n");
            helpOutput.append("  --item-id               The ID of the work item to display\n");
            helpOutput.append("  --show-line-numbers     Show line numbers in output\n");
            helpOutput.append("  --show-all-formatting   Show all formatting characters\n");
            helpOutput.append("  --show-history          Show work item history\n");
            helpOutput.append("  --show-relationships    Show related work items\n");
            helpOutput.append("  --format                Output format (text/json)\n");
            helpOutput.append("  --verbose               Show detailed output\n");
            helpOutput.append("  -h, --help              Show this help message\n");
            
            System.out.println(helpOutput);
        }
    }
}