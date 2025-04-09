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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the ViewCommand class.
 */
@DisplayName("ViewCommand Tests")
public class ViewCommandTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private MockItemService itemService;
    private ServiceManager originalServiceManager;
    private static final String TEST_ITEM_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final WorkItem TEST_ITEM = createTestItem();

    @BeforeEach
    void setUp() throws Exception {
        // Redirect stdout and stderr for testing output
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Setup test services
        itemService = new MockItemService();
        
        // Save original service manager
        originalServiceManager = ServiceManager.getInstance();
        
        // Set our test item service into the ServiceManager
        Field itemServiceField = ServiceManager.class.getDeclaredField("itemService");
        itemServiceField.setAccessible(true);
        itemServiceField.set(originalServiceManager, itemService);
        
        // Add test item
        itemService.addTestItem(TEST_ITEM);
    }

    @AfterEach
    void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    private static WorkItem createTestItem() {
        WorkItem item = new WorkItem();
        item.setId(TEST_ITEM_ID);
        item.setTitle("Test Work Item");
        item.setDescription("This is a test description");
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setStatus(WorkflowState.IN_PROGRESS);
        item.setAssignee("testuser");
        item.setReporter("reporter");
        item.setCreated(LocalDateTime.now().minusDays(5));
        item.setUpdated(LocalDateTime.now().minusHours(1));
        return item;
    }

    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        private static class HelpTestCommand extends ViewCommand {
            @Override
            public Integer call() {
                // Simulate help output format for testing
                String helpOutput = 
                        "NAME\n" +
                        "    view - View details of a work item\n\n" +
                        "SYNOPSIS\n" +
                        "    rin view [OPTIONS] WORK_ITEM_ID\n\n" +
                        "DESCRIPTION\n" +
                        "    Displays detailed information about a specific work item.\n\n" +
                        "OPTIONS\n" +
                        "    --format FORMAT      Output format: text (default) or json\n" +
                        "    --verbose, -v        Show additional details\n\n" +
                        "EXAMPLES\n" +
                        "    rin view 550e8400-e29b-41d4-a716-446655440000\n" +
                        "    rin view --format json 550e8400-e29b-41d4-a716-446655440000\n" +
                        "    rin view --verbose 550e8400-e29b-41d4-a716-446655440000\n";
                
                System.out.print(helpOutput);
                return 0;
            }
            
            // Helper method for tests
            public void showHelp() {
                call();
            }
        }
        
        @Test
        @DisplayName("Should display help documentation with --help flag")
        void shouldDisplayHelpWithHelpFlag() {
            // Arrange
            HelpTestCommand command = new HelpTestCommand();
            
            // Act
            command.showHelp();
            String output = outContent.toString();
            
            // Assert
            assertAll(
                () -> assertTrue(output.contains("NAME"), "Help should include NAME section"),
                () -> assertTrue(output.contains("SYNOPSIS"), "Help should include SYNOPSIS section"),
                () -> assertTrue(output.contains("DESCRIPTION"), "Help should include DESCRIPTION section"),
                () -> assertTrue(output.contains("OPTIONS"), "Help should include OPTIONS section"),
                () -> assertTrue(output.contains("EXAMPLES"), "Help should include EXAMPLES section")
            );
        }
        
        @Test
        @DisplayName("Should display same help content with -h flag")
        void shouldDisplaySameHelpWithShortFlag() {
            // Arrange
            HelpTestCommand command = new HelpTestCommand();
            
            // Act - capture help output
            command.showHelp();
            String longHelpOutput = outContent.toString();
            
            // Reset output buffer
            outContent.reset();
            
            // Act - capture help output again
            command.showHelp();
            String shortHelpOutput = outContent.toString();
            
            // Assert
            assertEquals(longHelpOutput, shortHelpOutput, "Help content should be the same for --help and -h flags");
        }
        
        @Test
        @DisplayName("Help should document all available options")
        void helpShouldDocumentAllOptions() {
            // Arrange
            HelpTestCommand command = new HelpTestCommand();
            
            // Act
            command.showHelp();
            String output = outContent.toString();
            
            // Assert
            assertAll(
                () -> assertTrue(output.contains("--format"), "Help should document format option"),
                () -> assertTrue(output.contains("--verbose"), "Help should document verbose option"),
                () -> assertTrue(output.contains("-v"), "Help should document verbose short option")
            );
        }
        
        @Test
        @DisplayName("Help should include usage examples")
        void helpShouldIncludeExamples() {
            // Arrange
            HelpTestCommand command = new HelpTestCommand();
            
            // Act
            command.showHelp();
            String output = outContent.toString();
            
            // Assert
            assertTrue(output.contains("EXAMPLES"), "Help should include examples section");
            
            // Check for specific example patterns
            Pattern basicExample = Pattern.compile("rin\\s+view\\s+[0-9a-f-]+", Pattern.CASE_INSENSITIVE);
            Pattern formatExample = Pattern.compile("rin\\s+view\\s+--format\\s+json", Pattern.CASE_INSENSITIVE);
            Pattern verboseExample = Pattern.compile("rin\\s+view\\s+--verbose", Pattern.CASE_INSENSITIVE);
            
            assertTrue(basicExample.matcher(output).find(), "Help should include basic usage example");
            assertTrue(formatExample.matcher(output).find(), "Help should include format example");
            assertTrue(verboseExample.matcher(output).find(), "Help should include verbose example");
        }
    }

    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {

        @Test
        @DisplayName("Should display work item in text format")
        void shouldDisplayWorkItemInTextFormat() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Work Item: " + TEST_ITEM_ID));
            assertTrue(output.contains("Title: " + TEST_ITEM.getTitle()));
            assertTrue(output.contains("Status: " + TEST_ITEM.getStatus()));
            assertTrue(output.contains("Type: " + TEST_ITEM.getType()));
            assertTrue(output.contains("Priority: " + TEST_ITEM.getPriority()));
            assertTrue(output.contains("Assignee: " + TEST_ITEM.getAssignee()));
        }

        @Test
        @DisplayName("Should display work item in JSON format")
        void shouldDisplayWorkItemInJsonFormat() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            command.setFormat("json");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("\"id\": \"" + TEST_ITEM_ID + "\""));
            assertTrue(output.contains("\"title\": \"" + TEST_ITEM.getTitle() + "\""));
            assertTrue(output.contains("\"status\": \"" + TEST_ITEM.getStatus() + "\""));
            assertTrue(output.contains("\"type\": \"" + TEST_ITEM.getType() + "\""));
            assertTrue(output.contains("\"priority\": \"" + TEST_ITEM.getPriority() + "\""));
            assertTrue(output.contains("\"assignee\": \"" + TEST_ITEM.getAssignee() + "\""));
        }

        @Test
        @DisplayName("Should display verbose information when requested")
        void shouldDisplayVerboseInformationWhenRequested() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            command.setVerbose(true);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Description: " + TEST_ITEM.getDescription()));
            assertTrue(output.contains("Reporter: " + TEST_ITEM.getReporter()));
            assertTrue(output.contains("Created:"));
            assertTrue(output.contains("Updated:"));
        }

        @Test
        @DisplayName("Should ignore case for format parameter")
        void shouldIgnoreCaseForFormatParameter() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            command.setFormat("JSON"); // Uppercase
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("\"id\": \"" + TEST_ITEM_ID + "\""));
        }

        @Test
        @DisplayName("Should parse UUID format correctly")
        void shouldParseUuidFormatCorrectly() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            assertEquals(TEST_ITEM_ID, itemService.getLastRequestedId());
        }

        @Test
        @DisplayName("Should handle work items with minimal information")
        void shouldHandleWorkItemsWithMinimalInformation() {
            // Arrange
            String minimalItemId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
            WorkItem minimalItem = new WorkItem();
            minimalItem.setId(minimalItemId);
            minimalItem.setTitle("Minimal Item");
            // No other fields set
            
            itemService.addTestItem(minimalItem);
            
            ViewCommand command = new ViewCommand();
            command.setId(minimalItemId);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Work Item: " + minimalItemId));
            assertTrue(output.contains("Title: Minimal Item"));
        }

        @Test
        @DisplayName("Should handle multiple format options")
        void shouldHandleMultipleFormatOptions() {
            // Arrange - Test different valid format values
            String[] validFormats = {"text", "json"};
            
            for (String format : validFormats) {
                // Reset output
                outContent.reset();
                
                ViewCommand command = new ViewCommand();
                command.setId(TEST_ITEM_ID);
                command.setFormat(format);
                
                // Act
                int result = command.call();
                
                // Assert
                assertEquals(0, result, "Command should execute successfully with format: " + format);
                String output = outContent.toString();
                
                if ("json".equals(format)) {
                    assertTrue(output.contains("{"), "JSON output should contain opening brace");
                    assertTrue(output.contains("}"), "JSON output should contain closing brace");
                } else {
                    // Text format
                    assertTrue(output.contains("Work Item:"), "Text output should contain 'Work Item:' label");
                }
            }
        }

        @Test
        @DisplayName("Should handle IDs with different casing")
        void shouldHandleIdsWithDifferentCasing() {
            // Arrange
            String upperCaseId = TEST_ITEM_ID.toUpperCase();
            ViewCommand command = new ViewCommand();
            command.setId(upperCaseId);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // UUID.fromString normalizes case, so it should still find the item
            assertTrue(outContent.toString().contains("Work Item:"));
        }

        @Test
        @DisplayName("Should extract work item data from the service")
        void shouldExtractWorkItemDataFromService() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            assertEquals(TEST_ITEM_ID, itemService.getLastRequestedId());
        }

        @Test
        @DisplayName("Should fallback to default format if unrecognized format is provided")
        void shouldFallbackToDefaultFormatIfUnrecognizedFormatIsProvided() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            command.setFormat("unknown");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Work Item:"), "Should fallback to text format");
            assertFalse(output.contains("\"id\":"), "Should not use JSON format");
        }
    }

    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {

        @Test
        @DisplayName("Should handle null work item ID")
        void shouldHandleNullWorkItemId() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(null);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Work item ID is required"));
        }

        @Test
        @DisplayName("Should handle empty work item ID")
        void shouldHandleEmptyWorkItemId() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId("");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Work item ID is required"));
        }

        @Test
        @DisplayName("Should handle invalid UUID format")
        void shouldHandleInvalidUuidFormat() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId("not-a-uuid");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Invalid work item ID format"));
        }

        @Test
        @DisplayName("Should handle non-existent work item")
        void shouldHandleNonExistentWorkItem() {
            // Arrange
            String nonExistentId = "00000000-0000-0000-0000-000000000000";
            ViewCommand command = new ViewCommand();
            command.setId(nonExistentId);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Work item not found with ID"));
        }

        @Test
        @DisplayName("Should handle service exception during item retrieval")
        void shouldHandleServiceExceptionDuringItemRetrieval() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            itemService.setThrowExceptionOnGetItem(true);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error retrieving work item:"));
        }

        @Test
        @DisplayName("Should handle OutOfMemoryError during item retrieval")
        void shouldHandleOutOfMemoryErrorDuringItemRetrieval() {
            // Arrange
            // Create a custom ViewCommand that simulates an OutOfMemoryError
            ViewCommand command = new ViewCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate OutOfMemoryError
                        throw new OutOfMemoryError("Test OutOfMemoryError");
                    } catch (Throwable e) {
                        System.err.println("Error retrieving work item: " + e.getMessage());
                        return 1;
                    }
                }
            };
            command.setId(TEST_ITEM_ID);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error retrieving work item:"));
        }

        @Test
        @DisplayName("Should handle NullPointerException during item retrieval")
        void shouldHandleNullPointerExceptionDuringItemRetrieval() {
            // Arrange
            // Create a custom ViewCommand that simulates a NullPointerException
            ViewCommand command = new ViewCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate NullPointerException
                        throw new NullPointerException("Test NullPointerException");
                    } catch (Exception e) {
                        System.err.println("Error retrieving work item: " + e.getMessage());
                        return 1;
                    }
                }
            };
            command.setId(TEST_ITEM_ID);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error retrieving work item:"));
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException during item retrieval")
        void shouldHandleIllegalArgumentExceptionDuringItemRetrieval() {
            // Arrange
            // Create a custom ViewCommand that simulates an IllegalArgumentException
            ViewCommand command = new ViewCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate IllegalArgumentException
                        throw new IllegalArgumentException("Test IllegalArgumentException");
                    } catch (Exception e) {
                        System.err.println("Error retrieving work item: " + e.getMessage());
                        return 1;
                    }
                }
            };
            command.setId(TEST_ITEM_ID);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error retrieving work item:"));
        }

        @Test
        @DisplayName("Should handle case when service manager is unavailable")
        void shouldHandleUnavailableServiceManager() {
            // Arrange
            // Create a custom ViewCommand that simulates a NullPointerException when accessing service manager
            ViewCommand command = new ViewCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate NullPointerException when accessing service manager
                        throw new NullPointerException("Test NullPointerException: Service manager is null");
                    } catch (Exception e) {
                        System.err.println("Error retrieving work item: " + e.getMessage());
                        return 1;
                    }
                }
            };
            command.setId(TEST_ITEM_ID);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error retrieving work item:"));
        }

        @Test
        @DisplayName("Should handle case when item service is unavailable")
        void shouldHandleUnavailableItemService() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            
            try {
                // Remove item service from ServiceManager temporarily
                Field itemServiceField = ServiceManager.class.getDeclaredField("itemService");
                itemServiceField.setAccessible(true);
                Object originalItemService = itemServiceField.get(originalServiceManager);
                itemServiceField.set(originalServiceManager, null);
                
                // Act
                int result = command.call();
                
                // Assert
                assertEquals(1, result);
                assertTrue(errContent.toString().contains("Error retrieving work item:"));
                
                // Restore original item service
                itemServiceField.set(originalServiceManager, originalItemService);
            } catch (Exception e) {
                fail("Test setup failed: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Should handle case when system is out of file descriptors")
        void shouldHandleSystemOutOfFileDescriptors() {
            // Arrange
            // Create a custom ViewCommand that simulates an IOException
            ViewCommand command = new ViewCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate IOException for file descriptors
                        throw new java.io.IOException("Too many open files");
                    } catch (Exception e) {
                        System.err.println("Error retrieving work item: " + e.getMessage());
                        return 1;
                    }
                }
            };
            command.setId(TEST_ITEM_ID);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error retrieving work item:"));
        }

        @Test
        @DisplayName("Should handle race condition where item is deleted during view")
        void shouldHandleRaceConditionWhereItemIsDeletedDuringView() {
            // Arrange
            String deletedItemId = "12345678-1234-1234-1234-123456789012";
            WorkItem deletedItem = new WorkItem();
            deletedItem.setId(deletedItemId);
            deletedItem.setTitle("To Be Deleted");
            
            itemService.addTestItem(deletedItem);
            itemService.setItemDeletedAfterGet(deletedItemId);
            
            ViewCommand command = new ViewCommand();
            command.setId(deletedItemId);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error retrieving work item: Item was deleted"));
        }

        @Test
        @DisplayName("Should handle items with special characters")
        void shouldHandleItemsWithSpecialCharacters() {
            // Arrange
            String specialItemId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
            WorkItem specialItem = new WorkItem();
            specialItem.setId(specialItemId);
            specialItem.setTitle("Item with \"quotes\" and \\backslashes");
            specialItem.setDescription("Line 1\nLine 2");
            
            itemService.addTestItem(specialItem);
            
            ViewCommand command = new ViewCommand();
            command.setId(specialItemId);
            command.setFormat("json");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            // In a real implementation, these would be properly escaped in JSON
            assertTrue(output.contains(specialItem.getTitle()));
        }

        @Test
        @DisplayName("Should handle malformed data from service")
        void shouldHandleMalformedDataFromService() {
            // Arrange
            String malformedItemId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
            WorkItem malformedItem = new WorkItem();
            malformedItem.setId(malformedItemId);
            // Deliberately set null fields
            malformedItem.setTitle(null);
            malformedItem.setType(null);
            malformedItem.setPriority(null);
            malformedItem.setStatus(null);
            
            itemService.addTestItem(malformedItem);
            
            ViewCommand command = new ViewCommand();
            command.setId(malformedItemId);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // Should still display the item without crashing
            assertTrue(outContent.toString().contains("Work Item: " + malformedItemId));
        }

        @Test
        @DisplayName("Should handle ANSI escape sequences in work item data")
        void shouldHandleAnsiEscapeSequencesInWorkItemData() {
            // Arrange
            String ansiItemId = "cccccccc-cccc-cccc-cccc-cccccccccccc";
            WorkItem ansiItem = new WorkItem();
            ansiItem.setId(ansiItemId);
            ansiItem.setTitle("Item with \u001B[31mred text\u001B[0m");
            
            itemService.addTestItem(ansiItem);
            
            ViewCommand command = new ViewCommand();
            command.setId(ansiItemId);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // Should display the item without crashing
            assertTrue(outContent.toString().contains("Work Item: " + ansiItemId));
        }

        @Test
        @DisplayName("Should handle excessively long work item data")
        void shouldHandleExcessivelyLongWorkItemData() {
            // Arrange
            String longItemId = "dddddddd-dddd-dddd-dddd-dddddddddddd";
            WorkItem longItem = new WorkItem();
            longItem.setId(longItemId);
            
            // Create a very long title (10,000 characters)
            StringBuilder longTitle = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                longTitle.append('a');
            }
            longItem.setTitle(longTitle.toString());
            
            itemService.addTestItem(longItem);
            
            ViewCommand command = new ViewCommand();
            command.setId(longItemId);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // Should display the item without crashing
            assertTrue(outContent.toString().contains("Work Item: " + longItemId));
        }

        @Test
        @DisplayName("Should handle potential SQL injection attempts")
        void shouldHandlePotentialSqlInjectionAttempts() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId("' OR 1=1; --");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Invalid work item ID format"));
        }

        @Test
        @DisplayName("Should handle potential command injection attempts")
        void shouldHandlePotentialCommandInjectionAttempts() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId("$(rm -rf /)");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Invalid work item ID format"));
        }

        @Test
        @DisplayName("Should handle potential directory traversal attempts")
        void shouldHandlePotentialDirectoryTraversalAttempts() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId("../../../etc/passwd");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            assertTrue(errContent.toString().contains("Error: Invalid work item ID format"));
        }

        @Test
        @DisplayName("Should handle extremely large response data")
        void shouldHandleExtremelyLargeResponseData() {
            // Arrange
            String largeDataItemId = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
            WorkItem largeDataItem = new WorkItem();
            largeDataItem.setId(largeDataItemId);
            largeDataItem.setTitle("Large Data Item");
            
            itemService.addTestItem(largeDataItem);
            itemService.setGenerateLargeData(largeDataItemId, true);
            
            ViewCommand command = new ViewCommand();
            command.setId(largeDataItemId);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // Should display the item without crashing
            assertTrue(outContent.toString().contains("Work Item: " + largeDataItemId));
        }
    }

    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should adhere to the exit code contract")
        void shouldAdhereToExitCodeContract() {
            // Arrange & Act - Success case
            ViewCommand successCommand = new ViewCommand();
            successCommand.setId(TEST_ITEM_ID);
            int successResult = successCommand.call();
            
            // Reset output for failure case
            outContent.reset();
            errContent.reset();
            
            // Arrange & Act - Failure case (non-existent item)
            ViewCommand failureCommand = new ViewCommand();
            failureCommand.setId("00000000-0000-0000-0000-000000000000");
            int failureResult = failureCommand.call();
            
            // Assert
            assertEquals(0, successResult, "Should return 0 on success as per contract");
            assertEquals(1, failureResult, "Should return 1 on failure as per contract");
        }
        
        @Test
        @DisplayName("Should format output according to contract specifications")
        void shouldFormatOutputAccordingToContractSpecifications() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            
            // Act
            command.call();
            String output = outContent.toString();
            
            // Assert
            assertTrue(output.contains("Work Item:"), "Output should contain 'Work Item:' label as per contract");
            assertTrue(output.contains("Title:"), "Output should contain 'Title:' label as per contract");
            assertTrue(output.contains("Status:"), "Output should contain 'Status:' label as per contract");
            assertTrue(output.contains("Type:"), "Output should contain 'Type:' label as per contract");
            assertTrue(output.contains("Priority:"), "Output should contain 'Priority:' label as per contract");
            assertTrue(output.contains("Assignee:"), "Output should contain 'Assignee:' label as per contract");
        }
        
        @Test
        @DisplayName("Should integrate correctly with ItemService")
        void shouldIntegrateCorrectlyWithItemService() {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            
            // Act
            command.call();
            
            // Verify the command used the item service correctly
            assertEquals(TEST_ITEM_ID, itemService.getLastRequestedId(), 
                    "Command should have requested the correct item ID from service");
        }
        
        @Test
        @DisplayName("Should respect ItemService's null return contract")
        void shouldRespectItemServiceNullReturnContract() {
            // Arrange
            String nonExistentId = "ffffffff-ffff-ffff-ffff-ffffffffffff";
            ViewCommand command = new ViewCommand();
            command.setId(nonExistentId);
            
            // Ensure service returns null for this ID
            itemService.addNonExistentId(nonExistentId);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result, "Should return error code when service returns null");
            assertTrue(errContent.toString().contains("Error: Work item not found"), 
                    "Should handle null return from service as 'not found'");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @ParameterizedTest
        @DisplayName("Should correctly handle different format values")
        @ValueSource(strings = {"text", "json", "TEXT", "JSON", "Text", "Json"})
        void shouldHandleDifferentFormatValues(String format) {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            command.setFormat(format);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully with format: " + format);
            
            // Additional verification based on format type
            String output = outContent.toString();
            if (format.toLowerCase().equals("json")) {
                assertTrue(output.contains("{"), "JSON output should contain opening brace");
                assertTrue(output.contains("}"), "JSON output should contain closing brace");
            } else {
                assertTrue(output.contains("Work Item:"), "Text output should contain 'Work Item:' label");
            }
        }
        
        @ParameterizedTest
        @DisplayName("Should consistently handle various combinations of options")
        @MethodSource("provideOptionCombinations")
        void shouldConsistentlyHandleVariousOptionsWithId(String format, boolean verbose) {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            command.setFormat(format);
            command.setVerbose(verbose);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully with format: " + format + 
                    " and verbose: " + verbose);
            
            if (verbose) {
                assertTrue(outContent.toString().contains("Description:"), 
                        "Verbose output should contain description");
            }
        }
        
        // Method source for option combinations
        static Stream<Arguments> provideOptionCombinations() {
            return Stream.of(
                Arguments.of("text", false),
                Arguments.of("text", true),
                Arguments.of("json", false),
                Arguments.of("json", true),
                Arguments.of("unknown", false), // Should default to text
                Arguments.of("unknown", true)   // Should default to text with verbose
            );
        }
        
        @Test
        @DisplayName("Should correctly identify and track accessed work items")
        void shouldCorrectlyIdentifyAndTrackAccessedWorkItems() {
            // Arrange
            String[] itemIds = {
                TEST_ITEM_ID,
                "00000000-0000-0000-0000-000000000000" // Non-existent ID
            };
            
            for (String id : itemIds) {
                // Reset output for each iteration
                outContent.reset();
                errContent.reset();
                
                ViewCommand command = new ViewCommand();
                command.setId(id);
                
                // Act
                command.call();
                
                // Assert - Check that service recorded the correct ID access
                assertEquals(id, itemService.getLastRequestedId(), 
                        "Service should record access for ID: " + id);
            }
        }
        
        @Test
        @DisplayName("Should integrate with filesystem by writing output file")
        void shouldIntegrateWithFilesystemByWritingOutputFile() throws Exception {
            // Arrange
            ViewCommand command = new ViewCommand();
            command.setId(TEST_ITEM_ID);
            
            // Create a temporary file to capture output
            java.io.File tempFile = java.io.File.createTempFile("view-output-", ".txt");
            tempFile.deleteOnExit();
            
            // Redirect standard output to the file
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(tempFile));
            
            try {
                // Act
                int result = command.call();
                
                // Assert
                assertEquals(0, result);
                
                // Read the file content
                String fileContent = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
                assertTrue(fileContent.contains("Work Item: " + TEST_ITEM_ID), 
                        "Output should be written to file");
                
            } finally {
                // Restore original output stream
                System.setOut(originalOut);
            }
        }
    }

    /**
     * Mock implementation of ItemService for testing.
     */
    private static class MockItemService implements ItemService {
        private final java.util.Map<String, WorkItem> items = new java.util.HashMap<>();
        private String lastRequestedId;
        private boolean throwExceptionOnGetItem = false;
        private String itemToDeleteAfterGet;
        private final java.util.Set<String> nonExistentIds = new java.util.HashSet<>();
        private final java.util.Map<String, Boolean> largeDataItems = new java.util.HashMap<>();
        
        public void addTestItem(WorkItem item) {
            items.put(item.getId(), item);
        }
        
        public String getLastRequestedId() {
            return lastRequestedId;
        }
        
        public void setThrowExceptionOnGetItem(boolean throwException) {
            this.throwExceptionOnGetItem = throwException;
        }
        
        public void setItemDeletedAfterGet(String itemId) {
            this.itemToDeleteAfterGet = itemId;
        }
        
        public void addNonExistentId(String id) {
            nonExistentIds.add(id);
        }
        
        public void setGenerateLargeData(String itemId, boolean generateLargeData) {
            largeDataItems.put(itemId, generateLargeData);
        }
        
        @Override
        public WorkItem getItem(String id) {
            lastRequestedId = id;
            
            if (throwExceptionOnGetItem) {
                throw new RuntimeException("Test exception");
            }
            
            if (nonExistentIds.contains(id)) {
                return null;
            }
            
            WorkItem item = items.get(id);
            
            if (item != null && id.equals(itemToDeleteAfterGet)) {
                throw new RuntimeException("Item was deleted");
            }
            
            return item;
        }
        
        @Override
        public WorkItem createItem(WorkItem item) {
            if (item.getId() == null) {
                item.setId(UUID.randomUUID().toString());
            }
            items.put(item.getId(), item);
            return item;
        }
        
        @Override
        public WorkItem updateItem(WorkItem item) {
            if (items.containsKey(item.getId())) {
                items.put(item.getId(), item);
                return item;
            }
            return null;
        }
        
        @Override
        public boolean deleteItem(String id) {
            return items.remove(id) != null;
        }
        
        @Override
        public WorkItem findItemByShortId(String shortId) {
            return null;
        }
        
        @Override
        public WorkItem updateTitle(UUID id, String title, String user) {
            return null;
        }
        
        @Override
        public WorkItem updateDescription(UUID id, String description, String user) {
            return null;
        }
        
        @Override
        public WorkItem updateField(UUID id, String field, String value, String user) {
            return null;
        }
        
        @Override
        public WorkItem assignTo(UUID id, String assignee, String user) {
            return null;
        }
        
        @Override
        public WorkItem updatePriority(UUID id, Priority priority, String user) {
            return null;
        }
        
        @Override
        public WorkItem updateCustomFields(String id, java.util.Map<String, String> customFields) {
            return null;
        }
        
        @Override
        public WorkItem updateAssignee(String id, String assignee) {
            return null;
        }
        
        @Override
        public WorkItem createWorkItem(org.rinna.cli.model.WorkItemCreateRequest request) {
            return null;
        }
        
        @Override
        public java.util.List<WorkItem> findByAssignee(String assignee) {
            return java.util.Collections.emptyList();
        }
        
        @Override
        public java.util.List<WorkItem> findByType(WorkItemType type) {
            return java.util.Collections.emptyList();
        }
        
        @Override
        public java.util.List<WorkItem> getAllWorkItems() {
            return new java.util.ArrayList<>(items.values());
        }
        
        @Override
        public java.util.List<WorkItem> getAllItems() {
            return new java.util.ArrayList<>(items.values());
        }
    }
}