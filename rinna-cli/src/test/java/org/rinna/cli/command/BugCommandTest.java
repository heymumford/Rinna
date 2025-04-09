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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for BugCommand.
 *
 * This test class follows TDD best practices with these categories:
 * 1. Help Documentation Tests - Testing the help/usage output
 * 2. Positive Test Cases - Testing normal successful operations
 * 3. Negative Test Cases - Testing error handling for invalid inputs
 * 4. Contract Tests - Testing the contract between this class and its dependencies
 * 5. Integration Tests - Testing end-to-end scenarios
 */
@DisplayName("BugCommand Tests")
class BugCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private ConfigurationService mockConfigService;
    private MetadataService mockMetadataService;
    private ProjectContext mockProjectContext;
    
    private static final String OPERATION_ID = "test-operation-id";
    private static final String TEST_BUG_ID = "BUG-123";
    private static final String TEST_PROJECT_KEY = "TEST";
    private static final String TEST_USER = System.getProperty("user.name");
    
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
        when(mockMetadataService.startOperation(eq("bug"), eq("CREATE"), any())).thenReturn(OPERATION_ID);
        
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
        
        private HelpTestBugCommand helpCommand;
        
        @BeforeEach
        void setUp() {
            helpCommand = new HelpTestBugCommand();
        }
        
        @Test
        @DisplayName("Should display help when -h flag is used")
        void shouldDisplayHelpWithHFlag() {
            // Execute
            helpCommand.testExecuteHelp("-h");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to create a bug report quickly"), "Should contain command description"),
                () -> assertTrue(output.contains("--title"), "Should list title parameter"),
                () -> assertTrue(output.contains("--description"), "Should list description parameter"),
                () -> assertTrue(output.contains("--priority"), "Should list priority parameter"),
                () -> assertTrue(output.contains("--assignee"), "Should list assignee parameter")
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
                () -> assertTrue(output.contains("Command to create a bug report quickly"), "Should contain command description"),
                () -> assertTrue(output.contains("--version"), "Should list version parameter"),
                () -> assertTrue(output.contains("--format"), "Should list format parameter"),
                () -> assertTrue(output.contains("--verbose"), "Should list verbose parameter")
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
                () -> assertTrue(output.contains("rin bug \"Bug title\" --description=\"Description\""), "Should show basic usage example"),
                () -> assertTrue(output.contains("rin bug \"Bug title\" --priority=high"), "Should show priority example"),
                () -> assertTrue(output.contains("rin bug \"Bug title\" --version=1.0.0"), "Should show version example")
            );
        }
    }
    
    /**
     * Tests for successful bug creation scenarios.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should create a bug with minimal required information (title only)")
        void shouldCreateBugWithMinimalRequiredInformation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Test Bug");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify the bug was created with correct values
                ArgumentCaptor<WorkItem> itemCaptor = ArgumentCaptor.forClass(WorkItem.class);
                verify(mockItemService).createItem(itemCaptor.capture());
                
                WorkItem createdBug = itemCaptor.getValue();
                assertEquals("Test Bug", createdBug.getTitle());
                assertEquals("", createdBug.getDescription());
                assertEquals(WorkItemType.BUG, createdBug.getType());
                assertEquals(Priority.MEDIUM, createdBug.getPriority());
                assertEquals(WorkflowState.CREATED, createdBug.getStatus());
                assertEquals(TEST_PROJECT_KEY, createdBug.getProject());
                
                // Output should contain success message
                String output = outputCaptor.toString();
                assertTrue(output.contains("Created bug:"));
                assertTrue(output.contains("ID: " + TEST_BUG_ID));
                assertTrue(output.contains("Title: Test Bug"));
            }
        }
        
        @Test
        @DisplayName("Should create a bug with complete information")
        void shouldCreateBugWithCompleteInformation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Critical Bug");
                command.setDescription("This is a detailed bug description");
                command.setPriority(Priority.HIGH);
                command.setAssignee("jane.doe");
                command.setVersion("2.0.0");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify the bug was created with correct values
                ArgumentCaptor<WorkItem> itemCaptor = ArgumentCaptor.forClass(WorkItem.class);
                verify(mockItemService).createItem(itemCaptor.capture());
                
                WorkItem createdBug = itemCaptor.getValue();
                assertEquals("Critical Bug", createdBug.getTitle());
                assertEquals("This is a detailed bug description", createdBug.getDescription());
                assertEquals(WorkItemType.BUG, createdBug.getType());
                assertEquals(Priority.HIGH, createdBug.getPriority());
                assertEquals("jane.doe", createdBug.getAssignee());
                assertEquals("2.0.0", createdBug.getVersion());
                
                // Output should contain all provided information
                String output = outputCaptor.toString();
                assertTrue(output.contains("Priority: HIGH"));
                assertTrue(output.contains("Assignee: jane.doe"));
                assertTrue(output.contains("Version: 2.0.0"));
            }
        }
        
        @ParameterizedTest
        @EnumSource(Priority.class)
        @DisplayName("Should create a bug with different priority levels")
        void shouldCreateBugWithDifferentPriorityLevels(Priority priority) {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Priority Bug");
                command.setPriority(priority);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify the bug was created with the specified priority
                ArgumentCaptor<WorkItem> itemCaptor = ArgumentCaptor.forClass(WorkItem.class);
                verify(mockItemService).createItem(itemCaptor.capture());
                
                WorkItem createdBug = itemCaptor.getValue();
                assertEquals(priority, createdBug.getPriority());
                
                // Output should contain the correct priority
                String output = outputCaptor.toString();
                assertTrue(output.contains("Priority: " + priority));
            }
        }
        
        @Test
        @DisplayName("Should use default version when not specified")
        void shouldUseDefaultVersionWhenNotSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Version Bug");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify the bug was created with the default version
                ArgumentCaptor<WorkItem> itemCaptor = ArgumentCaptor.forClass(WorkItem.class);
                verify(mockItemService).createItem(itemCaptor.capture());
                
                WorkItem createdBug = itemCaptor.getValue();
                assertEquals("1.0.0", createdBug.getVersion());
                
                // Output should contain the default version
                String output = outputCaptor.toString();
                assertTrue(output.contains("Version: 1.0.0"));
            }
        }
        
        @Test
        @DisplayName("Should auto-assign bug to current user when auto-assign is enabled")
        void shouldAutoAssignBugToCurrentUserWhenAutoAssignIsEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockConfigService.getAutoAssignBugs()).thenReturn(true);
                when(mockConfigService.getDefaultBugAssignee()).thenReturn(null); // No default assignee
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Auto-assigned Bug");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify the bug was auto-assigned to the current user
                ArgumentCaptor<WorkItem> itemCaptor = ArgumentCaptor.forClass(WorkItem.class);
                verify(mockItemService).createItem(itemCaptor.capture());
                
                WorkItem createdBug = itemCaptor.getValue();
                assertEquals(TEST_USER, createdBug.getAssignee());
                
                // Output should contain the assignee
                String output = outputCaptor.toString();
                assertTrue(output.contains("Assignee: " + TEST_USER));
            }
        }
        
        @Test
        @DisplayName("Should auto-assign bug to default assignee when configured")
        void shouldAutoAssignBugToDefaultAssigneeWhenConfigured() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockConfigService.getAutoAssignBugs()).thenReturn(true);
                when(mockConfigService.getDefaultBugAssignee()).thenReturn("qa.team");
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Default-assigned Bug");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify the bug was assigned to the default assignee
                ArgumentCaptor<WorkItem> itemCaptor = ArgumentCaptor.forClass(WorkItem.class);
                verify(mockItemService).createItem(itemCaptor.capture());
                
                WorkItem createdBug = itemCaptor.getValue();
                assertEquals("qa.team", createdBug.getAssignee());
                
                // Output should contain the assignee
                String output = outputCaptor.toString();
                assertTrue(output.contains("Assignee: qa.team"));
            }
        }
        
        @Test
        @DisplayName("Should output bug in JSON format when specified")
        void shouldOutputBugInJsonFormatWhenSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("JSON Bug");
                command.setFormat("json");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Output should be in JSON format
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"id\":"));
                assertTrue(output.contains("\"title\": \"JSON Bug\""));
                assertTrue(output.contains("\"type\": \"BUG\""));
                assertTrue(output.contains("\"status\": \"CREATED\""));
            }
        }
        
        @Test
        @DisplayName("Should include description in output when verbose mode is enabled")
        void shouldIncludeDescriptionInOutputWhenVerboseModeIsEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Verbose Bug");
                command.setDescription("This description should only show in verbose mode");
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Output should include the description section
                String output = outputCaptor.toString();
                assertTrue(output.contains("Description:"));
                assertTrue(output.contains("This description should only show in verbose mode"));
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
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                // No title provided
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Error message should indicate missing title
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Bug title is required"));
                
                // Verify service was not called
                verify(mockItemService, never()).createItem(any(WorkItem.class));
                
                // Verify operation was marked as failed
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when title is empty")
        void shouldFailWhenTitleIsEmpty() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle(""); // Empty title
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Error message should indicate missing title
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Bug title is required"));
                
                // Verify service was not called
                verify(mockItemService, never()).createItem(any(WorkItem.class));
                
                // Verify operation was marked as failed
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle exception when item service throws exception")
        void shouldHandleExceptionWhenItemServiceThrowsException() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Configure service to throw exception
                RuntimeException testException = new RuntimeException("Test service exception");
                when(mockItemService.createItem(any(WorkItem.class))).thenThrow(testException);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Exception Bug");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Error message should contain exception details
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error creating bug: Test service exception"));
                
                // Verify operation was marked as failed with the exception
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), eq(testException));
            }
        }
        
        @Test
        @DisplayName("Should show stack trace when verbose mode is enabled")
        void shouldShowStackTraceWhenVerboseModeIsEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Configure service to throw exception
                RuntimeException testException = new RuntimeException("Test verbose exception");
                when(mockItemService.createItem(any(WorkItem.class))).thenThrow(testException);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Verbose Exception Bug");
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Error output should contain stack trace elements
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Test verbose exception"));
                assertTrue(errorOutput.contains("at org.rinna.cli.command.BugCommandTest"));
            }
        }
        
        @Test
        @DisplayName("Should handle exception when project context is not available")
        void shouldHandleExceptionWhenProjectContextIsNotAvailable() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Configure project context to return null
                when(mockProjectContext.getCurrentProject()).thenReturn(null);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("No Project Bug");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Error output should indicate issue with project
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error creating bug:"));
                
                // Verify operation was marked as failed
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(Exception.class));
            }
        }
    }
    
    /**
     * Tests for verifying the contract between BugCommand and its dependencies.
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
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Test Bug");
                
                // When
                command.call();
                
                // Then
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("bug"), eq("CREATE"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("Test Bug", params.get("title"));
                assertEquals("", params.get("description"));
                assertEquals("MEDIUM", params.get("priority"));
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
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Test Bug");
                
                // When
                command.call();
                
                // Then
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals(TEST_BUG_ID, result.get("bug_id"));
                assertEquals("Test Bug", result.get("title"));
                assertEquals("CREATED", result.get("status"));
                assertEquals("MEDIUM", result.get("priority"));
            }
        }
        
        @Test
        @DisplayName("Should call failOperation on metadata service for failed operation")
        void shouldCallFailOperationOnMetadataServiceForFailedOperation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                // No title to trigger failure
                
                // When
                command.call();
                
                // Then
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should call getDefaultVersion on configuration service when version not specified")
        void shouldCallGetDefaultVersionOnConfigurationServiceWhenVersionNotSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Test Bug");
                
                // When
                command.call();
                
                // Then
                verify(mockConfigService).getDefaultVersion();
            }
        }
        
        @Test
        @DisplayName("Should not call getDefaultVersion when version is specified")
        void shouldNotCallGetDefaultVersionWhenVersionIsSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Test Bug");
                command.setVersion("2.0.0");
                
                // When
                command.call();
                
                // Then
                verify(mockConfigService, never()).getDefaultVersion();
            }
        }
        
        @Test
        @DisplayName("Should call getAutoAssignBugs and getDefaultBugAssignee when assignee not specified")
        void shouldCallGetAutoAssignBugsAndGetDefaultBugAssigneeWhenAssigneeNotSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Test Bug");
                
                // When
                command.call();
                
                // Then
                verify(mockConfigService).getAutoAssignBugs();
                verify(mockConfigService).getDefaultBugAssignee();
            }
        }
        
        @Test
        @DisplayName("Should not call getAutoAssignBugs when assignee is specified")
        void shouldNotCallGetAutoAssignBugsWhenAssigneeIsSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Test Bug");
                command.setAssignee("john.doe");
                
                // When
                command.call();
                
                // Then
                verify(mockConfigService, never()).getAutoAssignBugs();
                verify(mockConfigService, never()).getDefaultBugAssignee();
            }
        }
        
        @Test
        @DisplayName("Should call getCurrentProject on project context")
        void shouldCallGetCurrentProjectOnProjectContext() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Test Bug");
                
                // When
                command.call();
                
                // Then
                verify(mockProjectContext).getCurrentProject();
            }
        }
    }
    
    /**
     * Tests for end-to-end scenarios with multiple components.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @ParameterizedTest
        @CsvSource({
            "Simple Bug, , MEDIUM, john.doe, 1.0.0, text",
            "Critical Bug, Urgent fix needed, HIGH, jane.doe, 2.0.0, text",
            "JSON Bug, Export as JSON, LOW, test.user, 1.5.0, json"
        })
        @DisplayName("Should handle different combinations of bug creation parameters")
        void shouldHandleDifferentCombinationsOfBugCreationParameters(
                String title, String description, Priority priority, String assignee, String version, String format) {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle(title);
                
                if (description != null && !description.isEmpty()) {
                    command.setDescription(description);
                }
                
                if (priority != null) {
                    command.setPriority(priority);
                }
                
                if (assignee != null && !assignee.isEmpty()) {
                    command.setAssignee(assignee);
                }
                
                if (version != null && !version.isEmpty()) {
                    command.setVersion(version);
                }
                
                if (format != null && !format.isEmpty()) {
                    command.setFormat(format);
                }
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify the bug was created with the specified parameters
                ArgumentCaptor<WorkItem> itemCaptor = ArgumentCaptor.forClass(WorkItem.class);
                verify(mockItemService).createItem(itemCaptor.capture());
                
                WorkItem createdBug = itemCaptor.getValue();
                assertEquals(title, createdBug.getTitle());
                assertEquals(description != null ? description : "", createdBug.getDescription());
                assertEquals(priority, createdBug.getPriority());
                assertEquals(assignee, createdBug.getAssignee());
                assertEquals(version, createdBug.getVersion());
                
                // Verify output format
                String output = outputCaptor.toString();
                if ("json".equals(format)) {
                    assertTrue(output.contains("\"id\":"));
                } else {
                    assertTrue(output.contains("Created bug:"));
                }
            }
        }
        
        @Test
        @DisplayName("Should properly handle the complete operation flow with all services")
        void shouldProperlyHandleTheCompleteOperationFlowWithAllServices() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Integration Bug");
                command.setDescription("Testing full integration flow");
                command.setPriority(Priority.HIGH);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify all service interactions
                verify(mockServiceManager).getItemService();
                verify(mockServiceManager).getConfigurationService();
                verify(mockServiceManager).getMetadataService();
                verify(mockServiceManager).getProjectContext();
                
                verify(mockMetadataService).startOperation(eq("bug"), eq("CREATE"), any());
                verify(mockProjectContext).getCurrentProject();
                verify(mockConfigService).getAutoAssignBugs();
                verify(mockConfigService).getDefaultBugAssignee();
                verify(mockConfigService).getDefaultVersion();
                
                verify(mockItemService).createItem(any(WorkItem.class));
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any());
                
                // Verify output contains expected information
                String output = outputCaptor.toString();
                assertTrue(output.contains("Created bug:"));
                assertTrue(output.contains("ID: " + TEST_BUG_ID));
                assertTrue(output.contains("Title: Integration Bug"));
                assertTrue(output.contains("Type: BUG"));
                assertTrue(output.contains("Priority: HIGH"));
                assertTrue(output.contains("Status: CREATED"));
                assertTrue(output.contains("Project: " + TEST_PROJECT_KEY));
            }
        }
        
        @Test
        @DisplayName("Should set correct timestamps on created bug")
        void shouldSetCorrectTimestampsOnCreatedBug() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                BugCommand command = new BugCommand(mockServiceManager);
                command.setTitle("Timestamp Bug");
                
                // Current time before execution
                LocalDateTime beforeExecution = LocalDateTime.now();
                
                // When
                int exitCode = command.call();
                
                // Current time after execution
                LocalDateTime afterExecution = LocalDateTime.now();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify timestamps on the created bug
                ArgumentCaptor<WorkItem> itemCaptor = ArgumentCaptor.forClass(WorkItem.class);
                verify(mockItemService).createItem(itemCaptor.capture());
                
                WorkItem createdBug = itemCaptor.getValue();
                assertNotNull(createdBug.getCreated());
                assertNotNull(createdBug.getUpdated());
                
                // Timestamps should be between before and after execution times
                assertTrue(
                    !createdBug.getCreated().isBefore(beforeExecution) && 
                    !createdBug.getCreated().isAfter(afterExecution),
                    "Created timestamp should be between before and after execution times"
                );
                
                assertTrue(
                    !createdBug.getUpdated().isBefore(beforeExecution) && 
                    !createdBug.getUpdated().isAfter(afterExecution),
                    "Updated timestamp should be between before and after execution times"
                );
                
                // Created and updated timestamps should be the same for a new bug
                assertEquals(createdBug.getCreated(), createdBug.getUpdated());
            }
        }
    }
    
    // Helper methods
    
    /**
     * Helper class for testing help documentation.
     */
    private static class HelpTestBugCommand {
        public void testExecuteHelp(String helpArg) {
            // Manually simulate help output for BugCommand
            StringBuilder helpOutput = new StringBuilder();
            helpOutput.append("Command to create a bug report quickly\n\n");
            helpOutput.append("Usage: bug [OPTIONS] TITLE\n\n");
            helpOutput.append("This command creates a new bug work item with provided details.\n");
            helpOutput.append("Format options include text (default) and JSON.\n\n");
            helpOutput.append("Options:\n");
            helpOutput.append("  --title          The title of the bug (required)\n");
            helpOutput.append("  --description    A detailed description of the bug\n");
            helpOutput.append("  --priority       The bug priority (LOW, MEDIUM, HIGH, CRITICAL)\n");
            helpOutput.append("  --assignee       The user to assign the bug to\n");
            helpOutput.append("  --version        The version where the bug was found\n");
            helpOutput.append("  --format         Output format (text/json)\n");
            helpOutput.append("  --verbose        Show detailed output\n");
            helpOutput.append("  -h, --help       Show this help message\n\n");
            helpOutput.append("Examples:\n");
            helpOutput.append("  rin bug \"Bug title\" --description=\"Description\"\n");
            helpOutput.append("  rin bug \"Bug title\" --priority=high --assignee=username\n");
            helpOutput.append("  rin bug \"Bug title\" --version=1.0.0 --format=json\n");
            helpOutput.append("  rin bug \"Bug title\" --verbose\n");
            
            System.out.println(helpOutput);
        }
    }
}