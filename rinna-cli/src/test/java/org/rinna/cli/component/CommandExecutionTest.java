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
import org.mockito.MockedStatic;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.AddCommand;
import org.rinna.cli.command.BugCommand;
import org.rinna.cli.command.ListCommand;
import org.rinna.cli.command.UpdateCommand;
import org.rinna.cli.command.ViewCommand;
import org.rinna.cli.command.WorkflowCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;

/**
 * Component tests for CLI command execution.
 * These tests verify that commands work correctly with their dependencies within the CLI framework.
 */
@Tag("component")
@DisplayName("CLI Command Execution Component Tests")
class CommandExecutionTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Mock
    private MockItemService mockItemService;

    @Mock
    private org.rinna.cli.service.MockWorkflowService mockWorkflowService;

    @Mock
    private org.rinna.cli.service.ConfigurationService mockConfigService;

    @Mock
    private org.rinna.cli.service.ProjectContext mockProjectContext;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        mocks = MockitoAnnotations.openMocks(this);

        // Create fresh streams for each test to avoid cross-test contamination
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setErr(originalErr);

        // Close mocks
        if (mocks != null) {
            mocks.close();
        }
    }

    @Nested
    @DisplayName("Basic Command Tests")
    class BasicCommandTests {
        @Test
        @DisplayName("Should validate enum parameters")
        void shouldValidateEnumParameters() {
            // This test validates that our model enums are properly defined
            assertNotNull(Priority.values(), "Priority enum should be properly defined");
            assertNotNull(WorkItemType.values(), "WorkItemType enum should be properly defined");
            assertNotNull(WorkflowState.values(), "WorkflowState enum should be properly defined");

            // Verify specific enum values critical for command functionality
            assertEquals(Priority.HIGH, Priority.valueOf("HIGH"), "Priority enum should include HIGH value");
            assertEquals(WorkItemType.BUG, WorkItemType.valueOf("BUG"), "WorkItemType enum should include BUG value");
            assertEquals(WorkflowState.READY, WorkflowState.valueOf("READY"), "WorkflowState enum should include READY value");
        }
    }

    @Nested
    @DisplayName("View Command Tests")
    class ViewCommandTests {
        private List<WorkItem> testItems;

        @BeforeEach
        void setUpViewTests() {
            // Create test work items
            WorkItem item1 = new WorkItem();
            item1.setId(UUID.randomUUID().toString());
            item1.setTitle("Test Work Item 1");
            item1.setDescription("Description for item 1");
            item1.setType(WorkItemType.TASK);
            item1.setPriority(Priority.HIGH);
            item1.setState(WorkflowState.IN_PROGRESS);
            item1.setCreated(LocalDateTime.now().minusDays(1));
            item1.setUpdated(LocalDateTime.now());

            WorkItem item2 = new WorkItem();
            item2.setId(UUID.randomUUID().toString());
            item2.setTitle("Test Work Item 2");
            item2.setDescription("Description for item 2");
            item2.setType(WorkItemType.BUG);
            item2.setPriority(Priority.CRITICAL);
            item2.setState(WorkflowState.READY);
            item2.setCreated(LocalDateTime.now().minusDays(2));
            item2.setUpdated(LocalDateTime.now().minusHours(1));

            testItems = Arrays.asList(item1, item2);

            // Set up mock to return the test items
            when(mockItemService.getItem(anyString())).thenAnswer(invocation -> {
                String id = invocation.getArgument(0);
                return testItems.stream()
                    .filter(item -> item.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            });
        }

        @Test
        @DisplayName("Should execute ViewCommand and display work item")
        void shouldExecuteViewCommandAndDisplayWorkItem() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);

                // Setup ViewCommand with a valid ID
                ViewCommand viewCmd = new ViewCommand();
                String itemId = testItems.get(0).getId();
                viewCmd.setId(itemId);
                viewCmd.setFormat("text");

                // Execute command
                int exitCode = viewCmd.call();

                // Verify service interaction
                verify(mockItemService).getItem(eq(itemId));

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output contains work item information
                String output = outputStream.toString();
                assertFalse(output.isEmpty(), "Output should not be empty");
                assertTrue(output.contains(itemId), "Output should contain the work item ID");
                assertTrue(output.contains("Test Work Item 1"), "Output should contain the work item title");
            }
        }

        @Test
        @DisplayName("Should handle non-existent work item ID properly")
        void shouldHandleNonExistentWorkItemIdProperly() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);

                // Setup ViewCommand with a non-existent ID
                ViewCommand viewCmd = new ViewCommand();
                String nonExistentId = "non-existent-id";
                viewCmd.setId(nonExistentId);

                // Execute command
                int exitCode = viewCmd.call();

                // Verify service interaction
                verify(mockItemService).getItem(eq(nonExistentId));

                // Verify command handles the case appropriately
                assertEquals(1, exitCode, "Command should return error code for non-existent item");

                // Verify error output
                String error = errorStream.toString();
                assertTrue(error.contains("not found") || error.contains("does not exist"),
                    "Error should indicate item not found");
            }
        }
    }

    @Nested
    @DisplayName("List Command Tests")
    class ListCommandTests {
        private List<WorkItem> testItems;

        @BeforeEach
        void setUpListTests() {
            // Create test work items
            WorkItem item1 = new WorkItem();
            item1.setId(UUID.randomUUID().toString());
            item1.setTitle("Test Work Item 1");
            item1.setType(WorkItemType.TASK);
            item1.setPriority(Priority.HIGH);
            item1.setState(WorkflowState.IN_PROGRESS);

            WorkItem item2 = new WorkItem();
            item2.setId(UUID.randomUUID().toString());
            item2.setTitle("Test Work Item 2");
            item2.setType(WorkItemType.BUG);
            item2.setPriority(Priority.CRITICAL);
            item2.setState(WorkflowState.READY);

            testItems = Arrays.asList(item1, item2);

            // Set up mock to return the test items
            when(mockItemService.getAllItems()).thenReturn(testItems);
            when(mockItemService.getItemsByType(any(WorkItemType.class))).thenAnswer(invocation -> {
                WorkItemType type = invocation.getArgument(0);
                return testItems.stream()
                    .filter(item -> item.getType() == type)
                    .collect(java.util.stream.Collectors.toList());
            });
            when(mockItemService.getItemsByState(any(WorkflowState.class))).thenAnswer(invocation -> {
                WorkflowState state = invocation.getArgument(0);
                return testItems.stream()
                    .filter(item -> item.getState() == state)
                    .collect(java.util.stream.Collectors.toList());
            });
        }

        @Test
        @DisplayName("Should list all work items")
        void shouldListAllWorkItems() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);

                // Setup ListCommand
                ListCommand listCmd = new ListCommand();

                // Execute command
                int exitCode = listCmd.call();

                // Verify service interaction
                verify(mockItemService).getAllItems();

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output contains work item information
                String output = outputStream.toString();
                assertTrue(output.contains("Test Work Item 1"), "Output should contain first work item title");
                assertTrue(output.contains("Test Work Item 2"), "Output should contain second work item title");
            }
        }

        @Test
        @DisplayName("Should filter work items by type")
        void shouldFilterWorkItemsByType() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);

                // Setup ListCommand with type filter
                ListCommand listCmd = new ListCommand();
                listCmd.setType(WorkItemType.BUG);

                // Execute command
                int exitCode = listCmd.call();

                // Verify service interaction
                verify(mockItemService).getItemsByType(eq(WorkItemType.BUG));

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output contains only BUG items
                String output = outputStream.toString();
                assertFalse(output.contains("Test Work Item 1"), "Output should not contain TASK item");
                assertTrue(output.contains("Test Work Item 2"), "Output should contain BUG item");
            }
        }

        @Test
        @DisplayName("Should filter work items by state")
        void shouldFilterWorkItemsByState() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);

                // Setup ListCommand with state filter
                ListCommand listCmd = new ListCommand();
                listCmd.setState(WorkflowState.IN_PROGRESS);

                // Execute command
                int exitCode = listCmd.call();

                // Verify service interaction
                verify(mockItemService).getItemsByState(eq(WorkflowState.IN_PROGRESS));

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output contains only IN_PROGRESS items
                String output = outputStream.toString();
                assertTrue(output.contains("Test Work Item 1"), "Output should contain IN_PROGRESS item");
                assertFalse(output.contains("Test Work Item 2"), "Output should not contain READY item");
            }
        }
    }

    @Nested
    @DisplayName("Add Command Tests")
    class AddCommandTests {
        @Test
        @DisplayName("Should add new work item")
        void shouldAddNewWorkItem() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);

                // Mock the createItem method to return a new work item
                WorkItem newItem = new WorkItem();
                String itemId = UUID.randomUUID().toString();
                newItem.setId(itemId);
                newItem.setTitle("New Test Item");
                newItem.setType(WorkItemType.TASK);
                newItem.setPriority(Priority.MEDIUM);
                newItem.setState(WorkflowState.CREATED);
                when(mockItemService.createItem(anyString(), any(WorkItemType.class), any(Priority.class), anyString()))
                    .thenReturn(newItem);

                // Setup AddCommand
                AddCommand addCmd = new AddCommand();
                addCmd.setTitle("New Test Item");
                addCmd.setType(WorkItemType.TASK);
                addCmd.setPriority(Priority.MEDIUM);
                addCmd.setDescription("This is a test item");

                // Execute command
                int exitCode = addCmd.call();

                // Verify service interaction
                verify(mockItemService).createItem(
                    eq("New Test Item"), 
                    eq(WorkItemType.TASK), 
                    eq(Priority.MEDIUM), 
                    eq("This is a test item")
                );

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output contains confirmation and new item ID
                String output = outputStream.toString();
                assertTrue(output.contains("created") || output.contains("added"), 
                    "Output should confirm item creation");
                assertTrue(output.contains(itemId), "Output should contain the new item ID");
            }
        }
    }

    @Nested
    @DisplayName("Update Command Tests")
    class UpdateCommandTests {
        private WorkItem testItem;

        @BeforeEach
        void setUpUpdateTests() {
            // Create a test work item
            testItem = new WorkItem();
            String itemId = UUID.randomUUID().toString();
            testItem.setId(itemId);
            testItem.setTitle("Original Title");
            testItem.setDescription("Original Description");
            testItem.setType(WorkItemType.TASK);
            testItem.setPriority(Priority.MEDIUM);
            testItem.setState(WorkflowState.READY);
            testItem.setAssignee("original-assignee");

            // Configure mock to return the test item
            when(mockItemService.getItem(eq(itemId))).thenReturn(testItem);
            when(mockItemService.updateItem(any(WorkItem.class))).thenReturn(testItem);

            // Configure workflow service
            when(mockWorkflowService.canTransition(eq(itemId), any(WorkflowState.class))).thenReturn(true);
            when(mockWorkflowService.getAvailableTransitions(eq(itemId))).thenReturn(List.of(WorkflowState.IN_PROGRESS, WorkflowState.DONE));
            when(mockWorkflowService.transition(eq(itemId), any(WorkflowState.class))).thenReturn(testItem);
        }

        @Test
        @DisplayName("Should update work item fields")
        void shouldUpdateWorkItemFields() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getMockItemService()).thenReturn(mockItemService);
                when(mockManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
                when(mockManager.getConfigurationService()).thenReturn(mockConfigService);

                // Setup UpdateCommand with field updates
                UpdateCommand updateCmd = new UpdateCommand();
                updateCmd.setId(testItem.getId());
                updateCmd.setTitle("Updated Title");
                updateCmd.setDescription("Updated Description");
                updateCmd.setPriority(Priority.HIGH);

                // Execute command
                int exitCode = updateCmd.call();

                // Verify service interaction
                verify(mockItemService).getItem(eq(testItem.getId()));
                verify(mockItemService).updateItem(argThat(item -> 
                    "Updated Title".equals(item.getTitle()) &&
                    "Updated Description".equals(item.getDescription()) &&
                    Priority.HIGH.equals(item.getPriority())
                ));

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output contains updated information
                String output = outputStream.toString();
                assertTrue(output.contains("Updated work item"), "Output should confirm update");
                assertTrue(output.contains(testItem.getId()), "Output should contain the item ID");
            }
        }

        @Test
        @DisplayName("Should handle state transitions")
        void shouldHandleStateTransitions() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getMockItemService()).thenReturn(mockItemService);
                when(mockManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
                when(mockManager.getConfigurationService()).thenReturn(mockConfigService);

                // Setup UpdateCommand with state change
                UpdateCommand updateCmd = new UpdateCommand();
                updateCmd.setId(testItem.getId());
                updateCmd.setState(WorkflowState.IN_PROGRESS);

                // Execute command
                int exitCode = updateCmd.call();

                // Verify service interaction
                verify(mockItemService).getItem(eq(testItem.getId()));
                verify(mockWorkflowService).canTransition(eq(testItem.getId()), eq(WorkflowState.IN_PROGRESS));
                verify(mockWorkflowService).transition(eq(testItem.getId()), eq(WorkflowState.IN_PROGRESS));

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output contains status information
                String output = outputStream.toString();
                assertTrue(output.contains("Updated work item"), "Output should confirm update");
                assertTrue(output.contains("Status: " + testItem.getStatus()), "Output should contain status information");
            }
        }

        @Test
        @DisplayName("Should handle invalid transitions")
        void shouldHandleInvalidTransitions() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getMockItemService()).thenReturn(mockItemService);
                when(mockManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
                when(mockManager.getConfigurationService()).thenReturn(mockConfigService);

                // Configure workflow service to disallow this transition
                when(mockWorkflowService.canTransition(eq(testItem.getId()), eq(WorkflowState.DONE)))
                    .thenReturn(false);

                // Setup UpdateCommand with invalid state change
                UpdateCommand updateCmd = new UpdateCommand();
                updateCmd.setId(testItem.getId());
                updateCmd.setState(WorkflowState.DONE);

                // Execute command
                int exitCode = updateCmd.call();

                // Verify service interaction
                verify(mockItemService).getItem(eq(testItem.getId()));
                verify(mockWorkflowService).canTransition(eq(testItem.getId()), eq(WorkflowState.DONE));
                verify(mockWorkflowService).getAvailableTransitions(eq(testItem.getId()));

                // Verify command execution
                assertEquals(1, exitCode, "Command should return error for invalid transition");

                // Verify error output
                String error = errorStream.toString();
                assertTrue(error.contains("Cannot transition"), "Error should indicate invalid transition");
                assertTrue(error.contains("Valid transitions"), "Error should show valid transitions");
            }
        }
    }

    @Nested
    @DisplayName("Bug Command Tests")
    class BugCommandTests {

        @BeforeEach
        void setupBugTests() {
            // Configure project context
            when(mockProjectContext.getCurrentProject()).thenReturn("TEST-PROJECT");

            // Configure configuration service
            when(mockConfigService.getDefaultVersion()).thenReturn("1.0.0");
            when(mockConfigService.getAutoAssignBugs()).thenReturn(true);
            when(mockConfigService.getDefaultBugAssignee()).thenReturn("qa-team");

            // Configure item service
            when(mockItemService.createItem(any(WorkItem.class))).thenAnswer(invocation -> {
                WorkItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID().toString());
                return item;
            });
        }

        @Test
        @DisplayName("Should create bug with minimal information")
        void shouldCreateBugWithMinimalInformation() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getConfigurationService()).thenReturn(mockConfigService);
                when(mockManager.getProjectContext()).thenReturn(mockProjectContext);

                // Setup BugCommand with minimal info
                BugCommand bugCmd = new BugCommand();
                bugCmd.setTitle("Application crashes on startup");

                // Execute command
                int exitCode = bugCmd.call();

                // Verify service interaction
                verify(mockItemService).createItem(argThat(item -> 
                    "Application crashes on startup".equals(item.getTitle()) &&
                    WorkItemType.BUG.equals(item.getType()) &&
                    Priority.MEDIUM.equals(item.getPriority()) && // Default priority
                    WorkflowState.CREATED.equals(item.getStatus()) &&
                    "TEST-PROJECT".equals(item.getProject()) &&
                    "1.0.0".equals(item.getVersion()) &&
                    "qa-team".equals(item.getAssignee()) // Default assignee
                ));

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output contains bug information
                String output = outputStream.toString();
                assertTrue(output.contains("Created bug"), "Output should confirm bug creation");
                assertTrue(output.contains("Application crashes on startup"), "Output should contain bug title");
                assertTrue(output.contains("BUG"), "Output should show bug type");
                assertTrue(output.contains("MEDIUM"), "Output should show priority");
                assertTrue(output.contains("qa-team"), "Output should show assignee");
            }
        }

        @Test
        @DisplayName("Should create bug with custom attributes")
        void shouldCreateBugWithCustomAttributes() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getConfigurationService()).thenReturn(mockConfigService);
                when(mockManager.getProjectContext()).thenReturn(mockProjectContext);

                // Setup BugCommand with custom info
                BugCommand bugCmd = new BugCommand();
                bugCmd.setTitle("Data corruption in profile page");
                bugCmd.setDescription("User profile data gets corrupted when saving changes to profile picture.");
                bugCmd.setPriority(Priority.CRITICAL);
                bugCmd.setAssignee("developer1");
                bugCmd.setVersion("2.3.1");
                bugCmd.setVerbose(true);

                // Execute command
                int exitCode = bugCmd.call();

                // Verify service interaction
                verify(mockItemService).createItem(argThat(item -> 
                    "Data corruption in profile page".equals(item.getTitle()) &&
                    "User profile data gets corrupted when saving changes to profile picture.".equals(item.getDescription()) &&
                    WorkItemType.BUG.equals(item.getType()) &&
                    Priority.CRITICAL.equals(item.getPriority()) &&
                    WorkflowState.CREATED.equals(item.getStatus()) &&
                    "TEST-PROJECT".equals(item.getProject()) &&
                    "2.3.1".equals(item.getVersion()) &&
                    "developer1".equals(item.getAssignee())
                ));

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output contains custom bug information
                String output = outputStream.toString();
                assertTrue(output.contains("CRITICAL"), "Output should show priority");
                assertTrue(output.contains("developer1"), "Output should show assignee");
                assertTrue(output.contains("2.3.1"), "Output should show version");
                // Verbose mode should show description
                assertTrue(output.contains("User profile data gets corrupted"), "Output should show description in verbose mode");
            }
        }

        @Test
        @DisplayName("Should output bug in JSON format when requested")
        void shouldOutputBugInJsonFormatWhenRequested() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getConfigurationService()).thenReturn(mockConfigService);
                when(mockManager.getProjectContext()).thenReturn(mockProjectContext);

                // Setup BugCommand with JSON output
                BugCommand bugCmd = new BugCommand();
                bugCmd.setTitle("Login failure on mobile");
                bugCmd.setJsonOutput(true);

                // Execute command
                int exitCode = bugCmd.call();

                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");

                // Verify output is in JSON format
                String output = outputStream.toString();
                assertTrue(output.contains("{"), "Output should start with JSON opening brace");
                assertTrue(output.contains("}"), "Output should end with JSON closing brace");
                assertTrue(output.contains("\"title\": \"Login failure on mobile\""), "Output should contain JSON formatted title");
                assertTrue(output.contains("\"type\": \"BUG\""), "Output should contain JSON formatted type");
                assertTrue(output.contains("\"priority\": \"MEDIUM\""), "Output should contain JSON formatted priority");
            }
        }
    }

    @Nested
    @DisplayName("Workflow Command Tests")
    class WorkflowCommandTests {

        @Test
        @DisplayName("Should execute workflow command to transition state")
        void shouldExecuteWorkflowCommandToTransitionState() {
            // Setup WorkflowCommand
            WorkflowCommand workflowCmd = new WorkflowCommand();
            String itemId = UUID.randomUUID().toString();
            workflowCmd.setItemId(itemId);
            workflowCmd.setTargetState(WorkflowState.IN_PROGRESS);

            // Execute command
            int exitCode = workflowCmd.call();

            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");

            // Verify output
            String output = outputStream.toString();
            assertTrue(output.contains(itemId), "Output should contain item ID");
            assertTrue(output.contains("IN_PROGRESS"), "Output should contain target state");
        }

        @Test
        @DisplayName("Should include comment with state transition")
        void shouldIncludeCommentWithStateTransition() {
            // Setup WorkflowCommand with comment
            WorkflowCommand workflowCmd = new WorkflowCommand();
            String itemId = UUID.randomUUID().toString();
            workflowCmd.setItemId(itemId);
            workflowCmd.setTargetState(WorkflowState.IN_PROGRESS);
            workflowCmd.setComment("Starting work on this task now");

            // Execute command
            int exitCode = workflowCmd.call();

            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");

            // Verify output contains comment
            String output = outputStream.toString();
            assertTrue(output.contains("Comment: Starting work on this task now"), "Output should contain comment");
        }

        @Test
        @DisplayName("Should validate required parameters")
        void shouldValidateRequiredParameters() {
            // Setup WorkflowCommand with missing parameters
            WorkflowCommand cmdMissingId = new WorkflowCommand();
            cmdMissingId.setTargetState(WorkflowState.IN_PROGRESS);

            // Execute command with missing ID
            int exitCodeMissingId = cmdMissingId.call();

            // Verify command fails with appropriate error
            assertEquals(1, exitCodeMissingId, "Command should fail when item ID is missing");
            String errorOutput1 = errorStream.toString();
            assertTrue(errorOutput1.contains("ID is required"), "Error should indicate missing item ID");

            // Reset error stream
            errorStream.reset();

            // Setup WorkflowCommand with missing state
            WorkflowCommand cmdMissingState = new WorkflowCommand();
            cmdMissingState.setItemId(UUID.randomUUID().toString());

            // Execute command with missing state
            int exitCodeMissingState = cmdMissingState.call();

            // Verify command fails with appropriate error
            assertEquals(1, exitCodeMissingState, "Command should fail when target state is missing");
            String errorOutput2 = errorStream.toString();
            assertTrue(errorOutput2.contains("state is required"), "Error should indicate missing target state");
        }
    }
}
