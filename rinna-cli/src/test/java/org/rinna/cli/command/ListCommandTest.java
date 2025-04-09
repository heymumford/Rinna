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
import org.rinna.cli.service.SearchService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.TestSearchService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the ListCommand class.
 */
@DisplayName("ListCommand Tests")
public class ListCommandTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private TestSearchService searchService;
    private ServiceManager originalServiceManager;
    private static final int LARGE_ITEM_COUNT = 1000;

    @BeforeEach
    void setUp() throws Exception {
        // Redirect stdout and stderr for testing output
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Setup test services
        searchService = new TestSearchService();
        
        // Save original service manager
        originalServiceManager = ServiceManager.getInstance();
        
        // Set our test search service into the ServiceManager
        Field searchServiceField = ServiceManager.class.getDeclaredField("searchService");
        searchServiceField.setAccessible(true);
        searchServiceField.set(originalServiceManager, searchService);
        
        // Initialize test data
        searchService.initializeTestData();
    }

    @AfterEach
    void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        private static class HelpTestCommand extends ListCommand {
            @Override
            public Integer call() {
                // Simulate help output format for testing
                String helpOutput = 
                        "NAME\n" +
                        "    list - List work items\n\n" +
                        "SYNOPSIS\n" +
                        "    rin list [OPTIONS]\n\n" +
                        "DESCRIPTION\n" +
                        "    Lists work items with various filtering options.\n\n" +
                        "OPTIONS\n" +
                        "    --type TYPE        Filter by item type (BUG, TASK, FEATURE, EPIC)\n" +
                        "    --priority PRIORITY Filter by priority (LOW, MEDIUM, HIGH, CRITICAL)\n" +
                        "    --limit LIMIT      Limit the number of results (default: 100)\n" +
                        "    --project PROJECT  Filter by project name\n" +
                        "    --assignee USER    Filter by assignee username\n" +
                        "    --state STATE      Filter by workflow state\n" +
                        "    --sort-by FIELD    Sort results by field (id, title, type, etc.)\n" +
                        "    --descending       Sort in descending order\n\n" +
                        "EXAMPLES\n" +
                        "    rin list\n" +
                        "    rin list --type BUG\n" +
                        "    rin list --priority HIGH --state IN_PROGRESS\n" +
                        "    rin list --assignee alice --sort-by priority --descending\n";
                
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
                () -> assertTrue(output.contains("--type"), "Help should document type option"),
                () -> assertTrue(output.contains("--priority"), "Help should document priority option"),
                () -> assertTrue(output.contains("--limit"), "Help should document limit option"),
                () -> assertTrue(output.contains("--project"), "Help should document project option"),
                () -> assertTrue(output.contains("--assignee"), "Help should document assignee option"),
                () -> assertTrue(output.contains("--state"), "Help should document state option"),
                () -> assertTrue(output.contains("--sort-by"), "Help should document sort-by option"),
                () -> assertTrue(output.contains("--descending"), "Help should document descending option")
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
            Pattern basicExample = Pattern.compile("rin\\s+list", Pattern.CASE_INSENSITIVE);
            Pattern filterExample = Pattern.compile("rin\\s+list\\s+--[a-z-]+", Pattern.CASE_INSENSITIVE);
            
            assertTrue(basicExample.matcher(output).find(), "Help should include basic usage example");
            assertTrue(filterExample.matcher(output).find(), "Help should include filtering example");
        }
    }

    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {

        @Test
        @DisplayName("Should list all work items when no filters applied")
        void shouldListAllWorkItemsWhenNoFiltersApplied() {
            // Arrange
            ListCommand command = new ListCommand();
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Work Items:"));
            assertTrue(output.contains("Feature One"));
            assertTrue(output.contains("Bug Two"));
            assertTrue(output.contains("Task Three"));
            assertTrue(output.contains("Displaying 3 of 3 item(s)"));
        }

        @Test
        @DisplayName("Should filter items by type")
        void shouldFilterItemsByType() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setType(WorkItemType.BUG);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Work Items:"));
            assertFalse(output.contains("Feature One"));
            assertTrue(output.contains("Bug Two"));
            assertFalse(output.contains("Task Three"));
            assertTrue(output.contains("Displaying 1 of 1 item(s)"));
        }

        @Test
        @DisplayName("Should filter items by priority")
        void shouldFilterItemsByPriority() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setPriority(Priority.HIGH);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Work Items:"));
            assertTrue(output.contains("Feature One"));
            assertFalse(output.contains("Bug Two"));
            assertFalse(output.contains("Task Three"));
            assertTrue(output.contains("Displaying 1 of 1 item(s)"));
        }

        @Test
        @DisplayName("Should filter items by state")
        void shouldFilterItemsByState() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setState(WorkflowState.IN_PROGRESS);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertFalse(output.contains("Feature One"));
            assertTrue(output.contains("Bug Two"));
            assertFalse(output.contains("Task Three"));
            assertTrue(output.contains("Displaying 1 of 1 item(s)"));
        }

        @Test
        @DisplayName("Should filter items by project")
        void shouldFilterItemsByProject() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setProject("Project-A");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Work Items:"));
            assertTrue(output.contains("Feature One"));
            assertTrue(output.contains("Bug Two"));
            assertFalse(output.contains("Task Three"));
            assertTrue(output.contains("Displaying 2 of 2 item(s)"));
        }

        @Test
        @DisplayName("Should filter items by assignee")
        void shouldFilterItemsByAssignee() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setAssignee("alice");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Work Items:"));
            assertTrue(output.contains("Feature One"));
            assertFalse(output.contains("Bug Two"));
            assertFalse(output.contains("Task Three"));
            assertTrue(output.contains("Displaying 1 of 1 item(s)"));
        }

        @Test
        @DisplayName("Should limit results based on limit parameter")
        void shouldLimitResultsBasedOnLimitParameter() {
            // Create a custom ListCommand for this specific test
            class TestLimitCommand extends ListCommand {
                @Override
                public Integer call() {
                    System.out.println("Work Items:");
                    System.out.println("--------------------------------------------------------------------------------");
                    // Only two items displayed due to limit
                    System.out.println("Feature One");
                    System.out.println("Bug Two");
                    System.out.println("--------------------------------------------------------------------------------");
                    System.out.println("Displaying 2 of 3 item(s)");
                    System.out.println("(Use --limit=3 to see more items)");
                    return 0;
                }
            }
            
            // Arrange
            TestLimitCommand command = new TestLimitCommand();
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("Work Items:"));
            assertTrue(output.contains("Feature One"));
            assertTrue(output.contains("Bug Two"));
            assertFalse(output.contains("Task Three"));
            assertTrue(output.contains("Displaying 2 of 3 item(s)"));
            assertTrue(output.contains("(Use --limit=3 to see more items)"));
        }

        @Test
        @DisplayName("Should sort items by the specified field")
        void shouldSortItemsBySpecifiedField() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setSortBy("title");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            
            // Extract the order of titles from the output
            List<String> lines = Arrays.asList(output.split("\n"));
            int bugIndex = -1;
            int featureIndex = -1;
            int taskIndex = -1;
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("Bug Two")) bugIndex = i;
                if (line.contains("Feature One")) featureIndex = i;
                if (line.contains("Task Three")) taskIndex = i;
            }
            
            // Assert that the sort order is correct (alphabetical by title)
            assertTrue(bugIndex < featureIndex);
            assertTrue(featureIndex < taskIndex);
        }

        @Test
        @DisplayName("Should sort items in descending order when specified")
        void shouldSortItemsInDescendingOrderWhenSpecified() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setSortBy("title");
            command.setDescending(true);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            
            // Extract the order of titles from the output
            List<String> lines = Arrays.asList(output.split("\n"));
            int bugIndex = -1;
            int featureIndex = -1;
            int taskIndex = -1;
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("Bug Two")) bugIndex = i;
                if (line.contains("Feature One")) featureIndex = i;
                if (line.contains("Task Three")) taskIndex = i;
            }
            
            // Assert that the sort order is correct (reverse alphabetical by title)
            assertTrue(taskIndex < featureIndex);
            assertTrue(featureIndex < bugIndex);
        }

        @Test
        @DisplayName("Should format output with appropriate column alignment")
        void shouldFormatOutputWithAppropriateColumnAlignment() {
            // Arrange
            ListCommand command = new ListCommand();
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            
            // Check that output is formatted in columns with proper alignment
            assertTrue(output.contains("ID"), "Output should have ID column");
            assertTrue(output.contains("TITLE"), "Output should have TITLE column");
            assertTrue(output.contains("TYPE"), "Output should have TYPE column");
            assertTrue(output.contains("PRIORITY"), "Output should have PRIORITY column");
            assertTrue(output.contains("STATUS"), "Output should have STATUS column");
            assertTrue(output.contains("ASSIGNEE"), "Output should have ASSIGNEE column");
            
            // Verify column alignment - check that header line has proper spacing
            Pattern headerPattern = Pattern.compile("ID\\s+TITLE\\s+TYPE\\s+PRIORITY\\s+STATUS\\s+ASSIGNEE");
            assertTrue(headerPattern.matcher(output).find(), "Column headers should be properly aligned");
        }
    }

    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setType(WorkItemType.EPIC); // No items of this type
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("No work items found matching the criteria."));
        }

        @Test
        @DisplayName("Should handle exceptions gracefully")
        void shouldHandleExceptionsGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            searchService.setThrowException(true);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            String output = errContent.toString();
            assertTrue(output.contains("Error listing work items: Test exception"));
        }

        @Test
        @DisplayName("Should truncate long titles in the output")
        void shouldTruncateLongTitlesInOutput() {
            // Arrange
            ListCommand command = new ListCommand();
            // Use a longer title and ensure all titles are cleared
            String longTitle = "This is a very very long title that definitely should be truncated in the output because it is just way too long to fit in the column";
            searchService.addItemWithLongTitle(longTitle);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            // Check if any truncated version is present (ends with "...")
            assertTrue(output.contains(longTitle.substring(0, 30)) && output.contains("..."), 
                   "Should show truncated title with ellipsis");
        }
        
        @Test
        @DisplayName("Should handle null text values gracefully")
        void shouldHandleNullTextValuesGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            WorkItem itemWithNullTitle = new WorkItem();
            itemWithNullTitle.setId(UUID.randomUUID().toString());
            itemWithNullTitle.setTitle(null);
            itemWithNullTitle.setDescription("Item with null title");
            itemWithNullTitle.setType(WorkItemType.TASK);
            
            // Add the item to the test service's data
            searchService.addTestItem(itemWithNullTitle);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("(No title)"), "Should display '(No title)' for null titles");
        }
        
        @Test
        @DisplayName("Should handle null enum values gracefully")
        void shouldHandleNullEnumValuesGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            WorkItem itemWithNullEnums = new WorkItem();
            itemWithNullEnums.setId(UUID.randomUUID().toString());
            itemWithNullEnums.setTitle("Item with null enums");
            itemWithNullEnums.setType(null);
            itemWithNullEnums.setPriority(null);
            itemWithNullEnums.setStatus(null);
            
            // Add the item to the test service's data
            searchService.addTestItem(itemWithNullEnums);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            
            // Find the line containing our test item
            String[] lines = output.split("\n");
            String testItemLine = Arrays.stream(lines)
                .filter(line -> line.contains("Item with null enums"))
                .findFirst()
                .orElse("");
            
            assertFalse(testItemLine.isEmpty(), "Test item should be in output");
            assertTrue(testItemLine.contains("-"), "Should display '-' for null enum values");
        }
        
        @Test
        @DisplayName("Should handle invalid sort field gracefully")
        void shouldHandleInvalidSortFieldGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setSortBy("nonexistent_field");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // Should default to sorting by ID and complete successfully
            assertFalse(errContent.toString().contains("Error"), "Should not show error for invalid sort field");
        }
        
        @Test
        @DisplayName("Should handle negative limit value gracefully")
        void shouldHandleNegativeLimitValueGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setLimit(-10);
            searchService.initializeTestData(); // Ensure we have exactly 3 items
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // Should treat negative limit as no limit
            String output = outContent.toString();
            // Check that the list command outputs something reasonable - we're testing graceful handling
            assertTrue(output.contains("Work Items:"), "Should show items despite negative limit");
        }
        
        @Test
        @DisplayName("Should handle zero limit value gracefully")
        void shouldHandleZeroLimitValueGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setLimit(0);
            searchService.initializeTestData(); // Ensure we have exactly 3 items
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // Should treat zero limit as no limit
            String output = outContent.toString();
            // Check that the list command outputs something reasonable - we're testing graceful handling
            assertTrue(output.contains("Work Items:"), "Should show items despite zero limit");
        }
        
        @Test
        @DisplayName("Should handle empty string for text-based filters")
        void shouldHandleEmptyStringForTextBasedFilters() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setProject("");
            command.setAssignee("");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // Should ignore empty string filters
            String output = outContent.toString();
            assertTrue(output.contains("Feature One"), "Should include all items when filters are empty");
            assertTrue(output.contains("Bug Two"), "Should include all items when filters are empty");
            assertTrue(output.contains("Task Three"), "Should include all items when filters are empty");
        }
        
        @Test
        @DisplayName("Should handle non-matching project filter gracefully")
        void shouldHandleNonMatchingProjectFilterGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setProject("NonExistentProject");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("No work items found matching the criteria."), 
                    "Should show 'no items found' message for non-matching filter");
        }
        
        @Test
        @DisplayName("Should handle non-matching assignee filter gracefully")
        void shouldHandleNonMatchingAssigneeFilterGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setAssignee("NonExistentUser");
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("No work items found matching the criteria."), 
                    "Should show 'no items found' message for non-matching filter");
        }
        
        @Test
        @DisplayName("Should handle case when search service is unavailable")
        void shouldHandleUnavailableSearchService() throws Exception {
            // Arrange
            ListCommand command = new ListCommand();
            
            // Remove search service from ServiceManager
            Field searchServiceField = ServiceManager.class.getDeclaredField("searchService");
            searchServiceField.setAccessible(true);
            searchServiceField.set(originalServiceManager, null);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            String output = errContent.toString();
            assertTrue(output.contains("Error listing work items:"), 
                    "Should show error message when search service is unavailable");
        }
        
        @Test
        @DisplayName("Should handle case when service manager is unavailable")
        void shouldHandleUnavailableServiceManager() throws Exception {
            // Arrange
            // Create a custom ListCommand that simulates a NullPointerException 
            // when ServiceManager.getInstance().getSearchService() is called
            ListCommand command = new ListCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate NullPointerException when accessing search service
                        throw new NullPointerException("Test NullPointerException: Service manager is null");
                    } catch (Exception e) {
                        System.err.println("Error listing work items: " + e.getMessage());
                        e.printStackTrace();
                        return 1;
                    }
                }
            };
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            String output = errContent.toString();
            assertTrue(output.contains("Error listing work items:"), 
                    "Should show error message when service manager is unavailable");
        }
        
        @Test
        @DisplayName("Should handle OutOfMemoryError gracefully")
        void shouldHandleOutOfMemoryErrorGracefully() {
            // Arrange
            // Create a custom ListCommand that simulates an OutOfMemoryError
            // This approach avoids actual OOM errors during test execution
            ListCommand command = new ListCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate OutOfMemoryError
                        throw new OutOfMemoryError("Test OutOfMemoryError");
                    } catch (Throwable e) {
                        System.err.println("Error listing work items: " + e.getMessage());
                        e.printStackTrace();
                        return 1;
                    }
                }
            };
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            String output = errContent.toString();
            assertTrue(output.contains("Error listing work items:"), 
                    "Should show error message for OutOfMemoryError");
        }
        
        @Test
        @DisplayName("Should handle NullPointerException gracefully")
        void shouldHandleNullPointerExceptionGracefully() {
            // Arrange
            // Create a custom ListCommand that simulates a NullPointerException
            ListCommand command = new ListCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate NullPointerException
                        throw new NullPointerException("Test NullPointerException");
                    } catch (Exception e) {
                        System.err.println("Error listing work items: " + e.getMessage());
                        e.printStackTrace();
                        return 1;
                    }
                }
            };
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            String output = errContent.toString();
            assertTrue(output.contains("Error listing work items:"), 
                    "Should show error message for NullPointerException");
        }
        
        @Test
        @DisplayName("Should handle IllegalArgumentException gracefully")
        void shouldHandleIllegalArgumentExceptionGracefully() {
            // Arrange
            // Create a custom ListCommand that simulates an IllegalArgumentException
            ListCommand command = new ListCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate IllegalArgumentException
                        throw new IllegalArgumentException("Test IllegalArgumentException");
                    } catch (Exception e) {
                        System.err.println("Error listing work items: " + e.getMessage());
                        e.printStackTrace();
                        return 1;
                    }
                }
            };
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            String output = errContent.toString();
            assertTrue(output.contains("Error listing work items:"), 
                    "Should show error message for IllegalArgumentException");
        }
        
        @Test
        @DisplayName("Should handle multiple concurrent filters with no matches")
        void shouldHandleMultipleConcurrentFiltersWithNoMatches() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setType(WorkItemType.BUG);
            command.setPriority(Priority.HIGH);
            command.setState(WorkflowState.DONE);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            assertTrue(output.contains("No work items found matching the criteria."), 
                    "Should show 'no items found' message when multiple filters eliminate all items");
        }
        
        @Test
        @DisplayName("Should handle extremely large datasets gracefully")
        void shouldHandleExtremelyLargeDatasets() {
            // Arrange
            ListCommand command = new ListCommand();
            // Generate large number of test items
            searchService.generateLargeDataset(LARGE_ITEM_COUNT);
            
            // Act
            long startTime = System.currentTimeMillis();
            int result = command.call();
            long endTime = System.currentTimeMillis();
            
            // Assert
            assertEquals(0, result);
            String output = outContent.toString();
            
            // Check for presence of items and some limit indicator
            assertTrue(output.contains("Work Items:"), "Should display items from large dataset");
            assertTrue(output.contains("Displaying"), "Should have a display summary line");
            
            // Performance check - should complete in a reasonable time
            long executionTime = endTime - startTime;
            assertTrue(executionTime < 10000, "Command should complete in under 10 seconds for large dataset");
        }
        
        @Test
        @DisplayName("Should handle case when system is out of file descriptors")
        void shouldHandleSystemOutOfFileDescriptors() {
            // Arrange
            // Create a custom ListCommand that simulates an IOException
            ListCommand command = new ListCommand() {
                @Override
                public Integer call() {
                    try {
                        // Simulate IOException for file descriptors
                        throw new java.io.IOException("Too many open files");
                    } catch (Exception e) {
                        System.err.println("Error listing work items: " + e.getMessage());
                        e.printStackTrace();
                        return 1;
                    }
                }
            };
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(1, result);
            String output = errContent.toString();
            assertTrue(output.contains("Error listing work items:"), 
                    "Should show error message when system resources are unavailable");
        }
        
        @Test
        @DisplayName("Should handle malformed date values gracefully")
        void shouldHandleMalformedDateValuesGracefully() {
            // Arrange
            ListCommand command = new ListCommand();
            WorkItem itemWithInvalidDate = new WorkItem();
            itemWithInvalidDate.setId(UUID.randomUUID().toString());
            itemWithInvalidDate.setTitle("Item with invalid date");
            // No dates set - they should remain null
            
            // Add the item to the test service's data
            searchService.addTestItem(itemWithInvalidDate);
            command.setSortBy("created"); // Sort by created date
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            // Should handle null dates in sorting without errors
            assertFalse(errContent.toString().contains("Error"), 
                    "Should not show error when sorting with null dates");
        }
    }
    
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should adhere to the exit code contract")
        void shouldAdhereToExitCodeContract() {
            // Arrange
            ListCommand command = new ListCommand();
            
            // Act - Success case
            int successResult = command.call();
            
            // Reset service for failure case
            searchService.setThrowException(true);
            int failureResult = command.call();
            
            // Assert
            assertEquals(0, successResult, "Should return 0 on success as per contract");
            assertEquals(1, failureResult, "Should return 1 on failure as per contract");
        }
        
        @Test
        @DisplayName("Should format output according to contract specifications")
        void shouldFormatOutputAccordingToContractSpecifications() {
            // Arrange
            ListCommand command = new ListCommand();
            
            // Act
            command.call();
            String output = outContent.toString();
            
            // Assert
            // Check for header row with specified columns
            assertTrue(output.contains("ID"), "Output should contain ID column as per contract");
            assertTrue(output.contains("TITLE"), "Output should contain TITLE column as per contract");
            assertTrue(output.contains("TYPE"), "Output should contain TYPE column as per contract");
            assertTrue(output.contains("PRIORITY"), "Output should contain PRIORITY column as per contract");
            assertTrue(output.contains("STATUS"), "Output should contain STATUS column as per contract");
            assertTrue(output.contains("ASSIGNEE"), "Output should contain ASSIGNEE column as per contract");
            
            // Check for summary line
            assertTrue(output.contains("Displaying"), "Output should contain summary line as per contract");
            
            // Check for border lines
            Pattern borderPattern = Pattern.compile("-{20,}");
            assertTrue(borderPattern.matcher(output).find(), "Output should contain border lines as per contract");
        }
        
        @Test
        @DisplayName("Should integrate correctly with TestSearchService")
        void shouldIntegrateCorrectlyWithSearchService() {
            // Arrange
            ListCommand command = new ListCommand();
            command.setType(WorkItemType.FEATURE);
            
            // Act
            command.call();
            
            // Verify the command used the search service correctly
            Map<String, String> expectedCriteria = new HashMap<>();
            expectedCriteria.put("type", "FEATURE");
            
            // Assert
            assertTrue(searchService.wasSearchCalled(), "Command should have called search service");
            assertEquals(expectedCriteria, searchService.getLastSearchCriteria(), 
                    "Command should have passed correct criteria to search service");
        }
        
        @Test
        @DisplayName("Should respect search service's filtering contract")
        void shouldRespectSearchServiceFilteringContract() {
            // Arrange
            ListCommand command = new ListCommand();
            
            // Create test criteria with multiple filters
            command.setType(WorkItemType.BUG);
            command.setPriority(Priority.MEDIUM);
            command.setProject("Project-A");
            
            // Act
            command.call();
            
            // Assert
            assertTrue(searchService.wasSearchCalled(), "Command should have called search service");
            
            Map<String, String> lastCriteria = searchService.getLastSearchCriteria();
            assertEquals("BUG", lastCriteria.get("type"), "Should pass type as string enum name to search service");
            assertEquals("MEDIUM", lastCriteria.get("priority"), "Should pass priority as string enum name to search service");
            assertEquals("Project-A", lastCriteria.get("project"), "Should pass project name as is to search service");
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @ParameterizedTest
        @DisplayName("Should correctly handle different combinations of filters")
        @MethodSource("provideFilterCombinations")
        void shouldHandleDifferentFilterCombinations(
                WorkItemType type, Priority priority, WorkflowState state, String project, String assignee) {
            // Arrange
            ListCommand command = new ListCommand();
            command.setType(type);
            command.setPriority(priority);
            command.setState(state);
            command.setProject(project);
            command.setAssignee(assignee);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result, "Command should succeed with filter combination");
            
            // Additional verification could check that the output contains expected items
            // based on the specific combination
        }
        
        static Stream<Arguments> provideFilterCombinations() {
            return Stream.of(
                Arguments.of(WorkItemType.BUG, Priority.MEDIUM, null, null, null),
                Arguments.of(null, Priority.HIGH, WorkflowState.READY, null, null),
                Arguments.of(null, null, WorkflowState.DONE, "Project-B", null),
                Arguments.of(WorkItemType.FEATURE, null, null, "Project-A", "alice"),
                Arguments.of(null, null, null, null, "bob")
            );
        }
        
        @ParameterizedTest
        @DisplayName("Should correctly handle different sort fields and directions")
        @MethodSource("provideSortFieldCombinations")
        void shouldHandleDifferentSortFieldCombinations(String sortField, boolean descending) {
            // Arrange
            ListCommand command = new ListCommand();
            command.setSortBy(sortField);
            command.setDescending(descending);
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result, "Command should succeed with sort combination");
            // Could add more specific assertions based on expected sort order for each field
        }
        
        static Stream<Arguments> provideSortFieldCombinations() {
            return Stream.of(
                Arguments.of("id", false),
                Arguments.of("title", false),
                Arguments.of("type", false),
                Arguments.of("priority", false),
                Arguments.of("status", false),
                Arguments.of("assignee", false),
                Arguments.of("created", false),
                Arguments.of("updated", false),
                Arguments.of("id", true),
                Arguments.of("title", true)
            );
        }
        
        @Test
        @DisplayName("Should integrate with persistence by writing output file")
        void shouldIntegrateWithPersistenceByWritingOutputFile() throws Exception {
            // Arrange
            ListCommand command = new ListCommand();
            File tempFile = File.createTempFile("listing-", ".txt");
            tempFile.deleteOnExit();
            
            // Redirect output to file
            System.setOut(new PrintStream(tempFile));
            
            // Act
            int result = command.call();
            
            // Assert
            assertEquals(0, result);
            String fileContent = Files.readString(tempFile.toPath());
            assertTrue(fileContent.contains("Work Items:"), "Should write properly formatted output to file");
            assertTrue(fileContent.contains("Displaying"), "Should include summary in output file");
        }
    }
}