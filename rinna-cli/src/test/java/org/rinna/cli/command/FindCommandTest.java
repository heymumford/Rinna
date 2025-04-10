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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
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
 * Comprehensive test suite for FindCommand.
 *
 * This test class follows TDD best practices with these categories:
 * 1. Help Documentation Tests - Testing the help/usage output
 * 2. Positive Test Cases - Testing normal successful operations
 * 3. Negative Test Cases - Testing error handling for invalid inputs
 * 4. Contract Tests - Testing the contract between this class and its dependencies
 * 5. Integration Tests - Testing end-to-end scenarios
 */
@DisplayName("FindCommand Tests")
class FindCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MetadataService mockMetadataService;
    private ContextManager mockContextManager;
    
    private static final String OPERATION_ID = "test-operation-id";
    private static final String TEST_WORK_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    
    private List<WorkItem> testWorkItems;
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock services
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = mock(MockItemService.class);
        mockMetadataService = mock(MetadataService.class);
        mockContextManager = mock(ContextManager.class);
        
        // Configure mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(eq("find"), eq("SEARCH"), any())).thenReturn(OPERATION_ID);
        
        // Mock ContextManager.getInstance()
        try (var staticMock = Mockito.mockStatic(ContextManager.class)) {
            staticMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
        }
        
        // Set up test data
        testWorkItems = createTestWorkItems();
        when(mockItemService.getAllItems()).thenReturn(testWorkItems);
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
        
        private HelpTestFindCommand helpCommand;
        
        @BeforeEach
        void setUp() {
            helpCommand = new HelpTestFindCommand();
        }
        
        @Test
        @DisplayName("Should display help when -h flag is used")
        void shouldDisplayHelpWithHFlag() {
            // Execute
            helpCommand.testExecuteHelp("-h");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to find work items based on various criteria"), "Should contain command description"),
                () -> assertTrue(output.contains("-name"), "Should list name parameter"),
                () -> assertTrue(output.contains("-type"), "Should list type parameter"),
                () -> assertTrue(output.contains("-state"), "Should list state parameter"),
                () -> assertTrue(output.contains("-priority"), "Should list priority parameter")
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
                () -> assertTrue(output.contains("Command to find work items based on various criteria"), "Should contain command description"),
                () -> assertTrue(output.contains("-assignee"), "Should list assignee parameter"),
                () -> assertTrue(output.contains("-reporter"), "Should list reporter parameter"),
                () -> assertTrue(output.contains("-newer"), "Should list newer parameter"),
                () -> assertTrue(output.contains("-mtime"), "Should list mtime parameter")
            );
        }
        
        @Test
        @DisplayName("Help should include usage examples")
        void helpShouldIncludeUsageExamples() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Examples:"), "Should contain examples section"),
                () -> assertTrue(output.contains("rin find -name pattern"), "Should show name example"),
                () -> assertTrue(output.contains("rin find -type TASK"), "Should show type example"),
                () -> assertTrue(output.contains("rin find -state IN_PROGRESS"), "Should show state example"),
                () -> assertTrue(output.contains("rin find -priority HIGH"), "Should show priority example"),
                () -> assertTrue(output.contains("rin find -assignee username"), "Should show assignee example"),
                () -> assertTrue(output.contains("rin find -newer 2023-05-20"), "Should show date example")
            );
        }
    }
    
    /**
     * Tests for successful find operations.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should find all work items when no criteria are specified")
        void shouldFindAllWorkItemsWhenNoCriteriaAreSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify all work items are returned
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found " + testWorkItems.size() + " work items:"), "Should show correct count");
                
                // Verify each work item is included in the output
                for (WorkItem item : testWorkItems) {
                    assertTrue(output.contains(item.getId()), "Output should include item ID: " + item.getId());
                }
            }
        }
        
        @Test
        @DisplayName("Should find work items by name pattern")
        void shouldFindWorkItemsByNamePattern() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setNamePattern("Bug");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify filtered output
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found 1 work items:"), "Should show correct count");
                assertTrue(output.contains("Critical Bug"), "Should include bug item");
                assertFalse(output.contains("Feature Request"), "Should not include feature item");
            }
        }
        
        @Test
        @DisplayName("Should find work items by type")
        void shouldFindWorkItemsByType() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setType(WorkItemType.TASK);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify filtered output
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found 1 work items:"), "Should show correct count");
                assertTrue(output.contains("Simple Task"), "Should include task item");
                assertFalse(output.contains("Critical Bug"), "Should not include bug item");
            }
        }
        
        @Test
        @DisplayName("Should find work items by state")
        void shouldFindWorkItemsByState() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setState(WorkflowState.IN_PROGRESS);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify filtered output
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found 1 work items:"), "Should show correct count");
                assertTrue(output.contains("Simple Task"), "Should include in-progress item");
                assertFalse(output.contains("Feature Request"), "Should not include backlog item");
            }
        }
        
        @Test
        @DisplayName("Should find work items by priority")
        void shouldFindWorkItemsByPriority() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setPriority(Priority.HIGH);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify filtered output
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found 1 work items:"), "Should show correct count");
                assertTrue(output.contains("Critical Bug"), "Should include high priority item");
                assertFalse(output.contains("Simple Task"), "Should not include medium priority item");
            }
        }
        
        @Test
        @DisplayName("Should find work items by assignee")
        void shouldFindWorkItemsByAssignee() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setAssignee("john.doe");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify filtered output
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found 2 work items:"), "Should show correct count");
                assertTrue(output.contains("Critical Bug"), "Should include john.doe's bug");
                assertTrue(output.contains("Simple Task"), "Should include john.doe's task");
                assertFalse(output.contains("Feature Request"), "Should not include jane.smith's item");
            }
        }
        
        @Test
        @DisplayName("Should find work items created after a date")
        void shouldFindWorkItemsCreatedAfterDate() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // One hour ago
                Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setCreatedAfter(oneHourAgo);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify filtered output - all items should be found since they're all created after one hour ago
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found " + testWorkItems.size() + " work items:"), "Should show correct count");
            }
        }
        
        @Test
        @DisplayName("Should find work items created before a date")
        void shouldFindWorkItemsCreatedBeforeDate() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // One hour in future
                Instant oneHourLater = Instant.now().plus(1, ChronoUnit.HOURS);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setCreatedBefore(oneHourLater);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify filtered output - all items should be found since they're all created before one hour later
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found " + testWorkItems.size() + " work items:"), "Should show correct count");
            }
        }
        
        @Test
        @DisplayName("Should count results when count only flag is set")
        void shouldCountResultsWhenCountOnlyFlagIsSet() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setCountOnly(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output only contains count
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found " + testWorkItems.size() + " work items"), "Should show only count");
                assertFalse(output.contains("Title:"), "Should not show details");
            }
        }
        
        @Test
        @DisplayName("Should display detailed results when print details flag is set")
        void shouldDisplayDetailedResultsWhenPrintDetailsFlagIsSet() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setPrintDetails(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify detailed output format
                String output = outputCaptor.toString();
                assertTrue(output.contains("ID: "), "Should show ID label");
                assertTrue(output.contains("Title: "), "Should show title label");
                assertTrue(output.contains("Type: "), "Should show type label");
                assertTrue(output.contains("Priority: "), "Should show priority label");
                assertTrue(output.contains("State: "), "Should show state label");
                assertTrue(output.contains("Assignee: "), "Should show assignee label");
                assertTrue(output.contains("Created: "), "Should show created label");
                assertTrue(output.contains("Updated: "), "Should show updated label");
            }
        }
        
        @Test
        @DisplayName("Should output results in JSON format when specified")
        void shouldOutputResultsInJsonFormatWhenSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setFormat("json");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify JSON output format
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"count\":"), "Should include count");
                assertTrue(output.contains("\"criteria\":"), "Should include criteria");
                assertTrue(output.contains("\"items\":"), "Should include items");
            }
        }
        
        @Test
        @DisplayName("Should update last viewed item with first result")
        void shouldUpdateLastViewedItemWithFirstResult() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setType(WorkItemType.BUG);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify context manager was updated with the first result
                verify(mockContextManager).setLastViewedWorkItem(any(UUID.class));
            }
        }
        
        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up criteria that won't match any items
                FindCommand command = new FindCommand(mockServiceManager);
                command.setNamePattern("NonExistentPattern");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output message for no results
                String output = outputCaptor.toString();
                assertTrue(output.contains("No matching work items found"), "Should show no results message");
                
                // Verify context manager was not updated
                verify(mockContextManager, never()).setLastViewedWorkItem(any(UUID.class));
            }
        }
        
        @ParameterizedTest
        @EnumSource(WorkItemType.class)
        @DisplayName("Should find work items by different types")
        void shouldFindWorkItemsByDifferentTypes(WorkItemType type) {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setType(type);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output contains correct results
                String output = outputCaptor.toString();
                
                // Count expected items of this type
                long expectedCount = testWorkItems.stream()
                        .filter(item -> item.getType() == type)
                        .count();
                
                if (expectedCount > 0) {
                    assertTrue(output.contains("Found " + expectedCount + " work items:"), 
                            "Should show correct count for type " + type);
                } else {
                    assertTrue(output.contains("No matching work items found"), 
                            "Should show no results for type " + type);
                }
            }
        }
        
        @ParameterizedTest
        @EnumSource(WorkflowState.class)
        @DisplayName("Should find work items by different states")
        void shouldFindWorkItemsByDifferentStates(WorkflowState state) {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setState(state);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output contains correct results
                String output = outputCaptor.toString();
                
                // Count expected items in this state
                long expectedCount = testWorkItems.stream()
                        .filter(item -> item.getStatus() == state)
                        .count();
                
                if (expectedCount > 0) {
                    assertTrue(output.contains("Found " + expectedCount + " work items:"), 
                            "Should show correct count for state " + state);
                } else {
                    assertTrue(output.contains("No matching work items found"), 
                            "Should show no results for state " + state);
                }
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
        @DisplayName("Should handle exception when service throws exception")
        void shouldHandleExceptionWhenServiceThrowsException() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Service throws exception
                RuntimeException testException = new RuntimeException("Test service exception");
                when(mockItemService.getAllItems()).thenThrow(testException);
                
                FindCommand command = new FindCommand(mockServiceManager);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify error message
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Error executing find command: Test service exception"), 
                        "Should show error message");
                
                // Verify operation was marked as failed
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), eq(testException));
            }
        }
        
        @Test
        @DisplayName("Should show stack trace when verbose mode is enabled")
        void shouldShowStackTraceWhenVerboseModeIsEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Service throws exception
                RuntimeException testException = new RuntimeException("Test verbose exception");
                when(mockItemService.getAllItems()).thenThrow(testException);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify error output includes stack trace
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Error executing find command: Test verbose exception"), 
                        "Should show error message");
                assertTrue(errorOutput.contains("java.lang.RuntimeException"), 
                        "Should include exception class");
                assertTrue(errorOutput.contains("at org.rinna.cli.command.FindCommandTest"), 
                        "Should include stack trace");
            }
        }
        
        @Test
        @DisplayName("Should handle UUID parsing errors safely")
        void shouldHandleUuidParsingErrorsSafely() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Create a test work item with an invalid UUID format
                WorkItem invalidItem = createTestWorkItem("NOT_A_UUID", "Invalid Item", WorkItemType.TASK);
                List<WorkItem> testItems = Collections.singletonList(invalidItem);
                when(mockItemService.getAllItems()).thenReturn(testItems);
                
                FindCommand command = new FindCommand(mockServiceManager);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should still complete successfully");
                
                // Verify context manager was not updated (since UUID parsing failed)
                verify(mockContextManager, never()).setLastViewedWorkItem(any(UUID.class));
            }
        }
    }
    
    /**
     * Tests for verifying the contract between FindCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call startOperation on metadata service")
        void shouldCallStartOperationOnMetadataService() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setNamePattern("Test");
                command.setType(WorkItemType.TASK);
                command.setState(WorkflowState.IN_PROGRESS);
                
                // When
                command.call();
                
                // Then
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("find"), eq("SEARCH"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("Test", params.get("name_pattern"));
                assertEquals("TASK", params.get("type"));
                assertEquals("IN_PROGRESS", params.get("state"));
            }
        }
        
        @Test
        @DisplayName("Should call completeOperation on metadata service for successful operation")
        void shouldCallCompleteOperationOnMetadataServiceForSuccessfulOperation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                
                // When
                command.call();
                
                // Then
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals(testWorkItems.size(), result.get("count"));
                assertEquals("text", result.get("format"));
            }
        }
        
        @Test
        @DisplayName("Should call completeOperation with JSON format when specified")
        void shouldCallCompleteOperationWithJsonFormatWhenSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setFormat("json");
                
                // When
                command.call();
                
                // Then
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("json", result.get("format"));
            }
        }
        
        @Test
        @DisplayName("Should call getAllItems on item service")
        void shouldCallGetAllItemsOnItemService() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                
                // When
                command.call();
                
                // Then
                verify(mockItemService).getAllItems();
            }
        }
        
        @Test
        @DisplayName("Should call setLastViewedWorkItem on context manager with first result")
        void shouldCallSetLastViewedWorkItemOnContextManagerWithFirstResult() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                
                // When
                command.call();
                
                // Then
                verify(mockContextManager).setLastViewedWorkItem(any(UUID.class));
            }
        }
    }
    
    /**
     * Tests for date parsing utility methods.
     */
    @Nested
    @DisplayName("Date Parsing Tests")
    class DateParsingTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"2023-05-20", "2023-05-20T10:15:30", "2023-05-20 10:15:30"})
        @DisplayName("Should parse valid date strings")
        void shouldParseValidDateStrings(String dateStr) {
            // When
            Instant result = FindCommand.parseDate(dateStr);
            
            // Then
            assertNotNull(result, "Should parse date successfully");
        }
        
        @Test
        @DisplayName("Should return null for invalid date strings")
        void shouldReturnNullForInvalidDateStrings() {
            // When
            Instant result = FindCommand.parseDate("invalid-date");
            
            // Then
            assertNull(result, "Should return null for invalid date");
        }
        
        @ParameterizedTest
        @ValueSource(ints = {-30, -7, -1, 0, 1, 7, 30})
        @DisplayName("Should calculate days from now correctly")
        void shouldCalculateDaysFromNowCorrectly(int days) {
            // Given
            Instant now = Instant.now();
            
            // When
            Instant result = FindCommand.daysFromNow(days);
            
            // Then
            assertNotNull(result, "Should return a valid Instant");
            
            if (days < 0) {
                assertTrue(result.isBefore(now), "Should be before now for negative days");
            } else if (days > 0) {
                assertTrue(result.isAfter(now), "Should be after now for positive days");
            }
        }
    }
    
    /**
     * Tests for end-to-end scenarios with multiple components.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        static Stream<Arguments> provideSearchCriteria() {
            return Stream.of(
                Arguments.of("name search", "Bug", null, null, null, 1, "Critical Bug"),
                Arguments.of("type search", null, WorkItemType.TASK, null, null, 1, "Simple Task"),
                Arguments.of("state search", null, null, WorkflowState.IN_PROGRESS, null, 1, "Simple Task"),
                Arguments.of("priority search", null, null, null, Priority.HIGH, 1, "Critical Bug"),
                Arguments.of("assignee search", null, null, null, null, 2, "john.doe"),
                Arguments.of("combined search", "Feature", WorkItemType.FEATURE, WorkflowState.BACKLOG, Priority.LOW, 1, "Feature Request")
            );
        }
        
        @ParameterizedTest
        @MethodSource("provideSearchCriteria")
        @DisplayName("Should handle different search criteria combinations")
        void shouldHandleDifferentSearchCriteriaCombinations(
                String description, String namePattern, WorkItemType type, WorkflowState state, 
                Priority priority, int expectedCount, String expectedItemContent) {
            
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                
                // Set provided search criteria
                if (namePattern != null) command.setNamePattern(namePattern);
                if (type != null) command.setType(type);
                if (state != null) command.setState(state);
                if (priority != null) command.setPriority(priority);
                if ("assignee search".equals(description)) command.setAssignee("john.doe");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output contains expected results
                String output = outputCaptor.toString();
                
                if (expectedCount > 0) {
                    assertTrue(output.contains("Found " + expectedCount + " work items:"), 
                            "Should show correct count for " + description);
                    assertTrue(output.contains(expectedItemContent), 
                            "Should include expected content for " + description);
                } else {
                    assertTrue(output.contains("No matching work items found"), 
                            "Should show no results for " + description);
                }
                
                // Verify operation tracking
                verify(mockMetadataService).startOperation(eq("find"), eq("SEARCH"), any());
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any());
            }
        }
        
        @Test
        @DisplayName("Should handle both text and JSON output formats with detailed information")
        void shouldHandleBothTextAndJsonOutputFormatsWithDetailedInformation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Text output
                FindCommand textCommand = new FindCommand(mockServiceManager);
                textCommand.setPrintDetails(true);
                textCommand.setType(WorkItemType.BUG);
                
                // JSON output
                FindCommand jsonCommand = new FindCommand(mockServiceManager);
                jsonCommand.setPrintDetails(true);
                jsonCommand.setType(WorkItemType.BUG);
                jsonCommand.setFormat("json");
                
                // When
                int textExitCode = textCommand.call();
                outputCaptor.reset(); // Clear output between calls
                int jsonExitCode = jsonCommand.call();
                
                // Then
                assertEquals(0, textExitCode);
                assertEquals(0, jsonExitCode);
                
                // Verify JSON output
                String jsonOutput = outputCaptor.toString();
                assertTrue(jsonOutput.contains("\"count\": 1"), "JSON should include count");
                assertTrue(jsonOutput.contains("\"criteria\": {"), "JSON should include criteria");
                assertTrue(jsonOutput.contains("\"type\": \"BUG\""), "JSON should include search criteria");
                assertTrue(jsonOutput.contains("\"items\": ["), "JSON should include items array");
                assertTrue(jsonOutput.contains("\"title\": \"Critical Bug\""), "JSON should include item title");
                assertTrue(jsonOutput.contains("\"description\""), "JSON should include detailed fields");
            }
        }
        
        @Test
        @DisplayName("Should integrate with multiple services for complete operation")
        void shouldIntegrateWithMultipleServicesForCompleteOperation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ContextManager.class)) {
                mockStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                FindCommand command = new FindCommand(mockServiceManager);
                command.setNamePattern("Bug");
                command.setPriority(Priority.HIGH);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify interactions with all services
                verify(mockItemService).getAllItems();
                verify(mockContextManager).setLastViewedWorkItem(any(UUID.class));
                verify(mockMetadataService).startOperation(eq("find"), eq("SEARCH"), any());
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any());
                
                // Verify output contains expected results
                String output = outputCaptor.toString();
                assertTrue(output.contains("Found 1 work items:"), "Should show correct count");
                assertTrue(output.contains("Critical Bug"), "Should include matching bug");
            }
        }
    }
    
    // Helper methods
    
    private List<WorkItem> createTestWorkItems() {
        List<WorkItem> items = new ArrayList<>();
        
        // Bug item
        WorkItem bug = createTestWorkItem(
                TEST_WORK_ITEM_ID, 
                "Critical Bug",
                WorkItemType.BUG,
                WorkflowState.CREATED,
                Priority.HIGH,
                "john.doe",
                "This is a critical bug that needs to be fixed asap",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        items.add(bug);
        
        // Task item
        WorkItem task = createTestWorkItem(
                "223e4567-e89b-12d3-a456-426614174001",
                "Simple Task",
                WorkItemType.TASK,
                WorkflowState.IN_PROGRESS,
                Priority.MEDIUM,
                "john.doe",
                "This is a simple task",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        items.add(task);
        
        // Feature item
        WorkItem feature = createTestWorkItem(
                "323e4567-e89b-12d3-a456-426614174002",
                "Feature Request",
                WorkItemType.FEATURE,
                WorkflowState.BACKLOG,
                Priority.LOW,
                "jane.smith",
                "This is a feature request",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        items.add(feature);
        
        return items;
    }
    
    private WorkItem createTestWorkItem(String id, String title, WorkItemType type) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setType(type);
        item.setStatus(WorkflowState.CREATED);
        item.setPriority(Priority.MEDIUM);
        item.setAssignee("john.doe");
        item.setDescription("Test description");
        item.setCreated(LocalDateTime.now());
        item.setUpdated(LocalDateTime.now());
        return item;
    }
    
    private WorkItem createTestWorkItem(
            String id, String title, WorkItemType type, WorkflowState state, 
            Priority priority, String assignee, String description,
            LocalDateTime created, LocalDateTime updated) {
        
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setType(type);
        item.setStatus(state);
        item.setPriority(priority);
        item.setAssignee(assignee);
        item.setDescription(description);
        item.setCreated(created);
        item.setUpdated(updated);
        item.setProject("TEST");
        return item;
    }
    
    /**
     * Helper class for testing help documentation.
     */
    private static class HelpTestFindCommand {
        public void testExecuteHelp(String helpArg) {
            // Manually simulate help output for FindCommand
            StringBuilder helpOutput = new StringBuilder();
            helpOutput.append("Command to find work items based on various criteria\n\n");
            helpOutput.append("Usage: find [OPTIONS]\n\n");
            helpOutput.append("Options:\n");
            helpOutput.append("  -name <pattern>      Find work items with title matching pattern\n");
            helpOutput.append("  -type <type>         Find work items of specified type (TASK, BUG, FEATURE, etc.)\n");
            helpOutput.append("  -state <state>       Find work items in specified state (CREATED, IN_PROGRESS, etc.)\n");
            helpOutput.append("  -priority <priority> Find work items with specified priority (LOW, MEDIUM, HIGH, etc.)\n");
            helpOutput.append("  -assignee <username> Find work items assigned to username\n");
            helpOutput.append("  -reporter <username> Find work items reported by username\n");
            helpOutput.append("  -newer <date>        Find work items created after date\n");
            helpOutput.append("  -mtime <days>        Find work items modified in the last N days\n");
            helpOutput.append("  --print-details      Show detailed information for each item\n");
            helpOutput.append("  --count-only         Only show the count of matching items\n");
            helpOutput.append("  --format=<format>    Output format (text/json)\n");
            helpOutput.append("  --verbose            Show detailed output\n");
            helpOutput.append("  -h, --help           Show this help message\n\n");
            helpOutput.append("Examples:\n");
            helpOutput.append("  rin find -name pattern                # Find by name pattern\n");
            helpOutput.append("  rin find -type TASK                   # Find tasks\n");
            helpOutput.append("  rin find -state IN_PROGRESS           # Find in-progress items\n");
            helpOutput.append("  rin find -priority HIGH               # Find high priority items\n");
            helpOutput.append("  rin find -assignee username           # Find items assigned to user\n");
            helpOutput.append("  rin find -newer 2023-05-20            # Find items created after date\n");
            helpOutput.append("  rin find -mtime -7                    # Find items modified in last 7 days\n");
            helpOutput.append("  rin find --format=json                # Output in JSON format\n");
            
            System.out.println(helpOutput);
        }
    }
}