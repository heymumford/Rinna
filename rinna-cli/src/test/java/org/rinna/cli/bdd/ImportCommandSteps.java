/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.bdd;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.rinna.cli.command.ImportCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Step definitions for import command BDD tests.
 */
public class ImportCommandSteps {
    
    private final TestContext testContext;
    private ImportCommand importCommand;
    private File tempFile;
    private String commandOutput;
    private String errorOutput;
    private int exitCode;
    
    // Capture console output
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    public ImportCommandSteps(TestContext testContext) {
        this.testContext = testContext;
    }
    
    @Before
    public void setup() {
        // Redirect console output
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mocks
        if (testContext.getMockServiceManager() == null) {
            ServiceManager mockServiceManager = mock(ServiceManager.class);
            testContext.setMockServiceManager(mockServiceManager);
            
            // Setup mock services
            MockItemService mockItemService = new MockItemService();
            testContext.setMockItemService(mockItemService);
            
            ConfigurationService mockConfigService = mock(ConfigurationService.class);
            when(mockConfigService.getCurrentUser()).thenReturn("test.user");
            when(mockConfigService.getCurrentProject()).thenReturn("IMPORT-TEST");
            testContext.setMockConfigService(mockConfigService);
            
            MetadataService mockMetadataService = mock(MetadataService.class);
            testContext.setMockMetadataService(mockMetadataService);
            
            MockWorkflowService mockWorkflowService = new MockWorkflowService();
            testContext.setMockWorkflowService(mockWorkflowService);
            
            MockBacklogService mockBacklogService = new MockBacklogService();
            testContext.setMockBacklogService(mockBacklogService);
            
            // Configure service manager
            when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
            when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
            when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
            when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
            when(mockServiceManager.getMockBacklogService()).thenReturn(mockBacklogService);
            
            // Mock static ServiceManager.getInstance()
            testContext.mockServiceManagerStatic();
        }
    }
    
    @After
    public void tearDown() throws IOException {
        // Restore console output
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Clean up temp files
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
    
    @Given("the user is logged in")
    public void userIsLoggedIn() {
        when(testContext.getMockConfigService().getCurrentUser()).thenReturn("test.user");
    }
    
    @Given("the current project is {string}")
    public void currentProjectIs(String project) {
        when(testContext.getMockConfigService().getCurrentProject()).thenReturn(project);
    }
    
    @Given("a markdown file {string} with the following content:")
    public void markdownFileWithContent(String fileName, String content) throws IOException {
        tempFile = File.createTempFile("cucumber-test-", ".md");
        Files.writeString(Path.of(tempFile.getPath()), content);
        testContext.setTestFile(tempFile);
    }
    
    @Given("a text file {string} with some content")
    public void textFileWithContent(String fileName) throws IOException {
        tempFile = File.createTempFile("cucumber-test-", ".txt");
        Files.writeString(Path.of(tempFile.getPath()), "This is some text content");
        testContext.setTestFile(tempFile);
    }
    
    @Given("an empty markdown file {string}")
    public void emptyMarkdownFile(String fileName) throws IOException {
        tempFile = File.createTempFile("cucumber-test-", ".md");
        Files.writeString(Path.of(tempFile.getPath()), "");
        testContext.setTestFile(tempFile);
    }
    
    @When("the user runs {string} command")
    public void userRunsCommand(String command) {
        importCommand = new ImportCommand(testContext.getMockServiceManager());
        
        // Parse the command options
        String[] commandParts = command.split("\\s+");
        if (commandParts.length > 1) {
            // First part is "import", rest are arguments
            for (int i = 1; i < commandParts.length; i++) {
                String arg = commandParts[i];
                if (arg.startsWith("--")) {
                    // Handle options
                    String option = arg.substring(2);
                    switch (option) {
                        case "json":
                            importCommand.setJsonOutput(true);
                            break;
                        case "verbose":
                            importCommand.setVerbose(true);
                            break;
                        case "format":
                            if (i + 1 < commandParts.length) {
                                importCommand.setFormat(commandParts[++i]);
                            }
                            break;
                        default:
                            // Ignore unknown options for BDD tests
                            break;
                    }
                } else if (!arg.startsWith("-")) {
                    // Assume it's the file path
                    if (testContext.getTestFile() != null) {
                        importCommand.setFilePath(testContext.getTestFile().getPath());
                    } else {
                        importCommand.setFilePath(arg);
                    }
                }
            }
        }
        
        // Execute the command
        try {
            exitCode = importCommand.call();
            commandOutput = outputCaptor.toString();
            errorOutput = errorCaptor.toString();
        } catch (Exception e) {
            exitCode = 1;
            errorOutput = e.getMessage();
        }
    }
    
    @Then("the command should succeed")
    public void commandShouldSucceed() {
        assertEquals(0, exitCode, "Command should succeed with exit code 0");
    }
    
    @Then("the command should fail")
    public void commandShouldFail() {
        assertEquals(1, exitCode, "Command should fail with exit code 1");
    }
    
    @Then("{int} work items? should be created")
    public void workItemsShouldBeCreated(int count) {
        assertEquals(count, testContext.getMockItemService().getItems().size(), 
                    "Expected " + count + " work items to be created");
    }
    
    @Then("work item with title {string} should have status {string}")
    public void workItemShouldHaveStatus(String title, String statusStr) {
        WorkflowState status = WorkflowState.valueOf(statusStr);
        WorkItem item = findWorkItemByTitle(title);
        assertNotNull(item, "Work item with title '" + title + "' should exist");
        assertEquals(status, item.getState(), 
                    "Work item should have status " + statusStr);
    }
    
    @Then("work item with title {string} should have priority {string}")
    public void workItemShouldHavePriority(String title, String priorityStr) {
        Priority priority = Priority.valueOf(priorityStr);
        WorkItem item = findWorkItemByTitle(title);
        assertNotNull(item, "Work item with title '" + title + "' should exist");
        assertEquals(priority, item.getPriority(), 
                    "Work item should have priority " + priorityStr);
    }
    
    @Then("the output should contain {string}")
    public void outputShouldContain(String text) {
        assertTrue(commandOutput.contains(text), 
                  "Output should contain '" + text + "', but was:\n" + commandOutput);
    }
    
    @Then("the error output should contain {string}")
    public void errorOutputShouldContain(String text) {
        assertTrue(errorOutput.contains(text), 
                  "Error output should contain '" + text + "', but was:\n" + errorOutput);
    }
    
    @Then("the output should be in JSON format")
    public void outputShouldBeInJsonFormat() {
        String trimmedOutput = commandOutput.trim();
        assertTrue(trimmedOutput.startsWith("{") && trimmedOutput.endsWith("}"), 
                  "Output should be in JSON format");
    }
    
    @Then("the JSON output should contain {string} field with value {string}")
    public void jsonOutputShouldContainField(String field, String value) {
        // Simple JSON field check, assumes field format is "field": "value" or "field": value
        String fieldPattern = "\"" + field + "\":\\s*";
        String valuePattern = value.matches("\\d+") ? value : "\"" + value + "\"";
        String pattern = fieldPattern + valuePattern;
        
        assertTrue(commandOutput.contains(pattern), 
                  "JSON output should contain field '" + field + "' with value '" + value + "'");
    }
    
    @Then("an import report should be generated")
    public void importReportShouldBeGenerated() {
        assertTrue(commandOutput.contains("import-report.txt"), 
                  "Output should mention the import report");
    }
    
    @Then("work items with titles {string} should be added to the backlog")
    public void workItemsShouldBeAddedToBacklog(String titles) {
        List<String> titleList = Arrays.asList(titles.split(",\\s*"));
        MockBacklogService backlogService = testContext.getMockBacklogService();
        
        assertEquals(titleList.size(), backlogService.getAddedItems().size(), 
                    "Expected " + titleList.size() + " items added to backlog");
        
        for (String title : titleList) {
            boolean found = backlogService.getAddedItems().stream()
                .anyMatch(item -> item.getTitle().equals(title));
            assertTrue(found, "Work item with title '" + title + "' should be added to backlog");
        }
    }
    
    private WorkItem findWorkItemByTitle(String title) {
        return testContext.getMockItemService().getItems().stream()
            .filter(item -> item.getTitle().equals(title))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Mock implementation of ItemService for testing.
     */
    public static class MockItemService implements ItemService {
        private final List<WorkItem> items = new ArrayList<>();
        
        @Override
        public List<WorkItem> getAllItems() {
            return items;
        }
        
        @Override
        public List<WorkItem> getAllWorkItems() {
            return items;
        }
        
        @Override
        public WorkItem getItem(String id) {
            return items.stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElse(null);
        }
        
        @Override
        public WorkItem createWorkItem(WorkItemCreateRequest request) {
            WorkItem item = new WorkItem();
            item.setId(java.util.UUID.randomUUID().toString());
            item.setTitle(request.getTitle());
            item.setDescription(request.getDescription());
            item.setType(request.getType());
            item.setPriority(request.getPriority());
            item.setAssignee(request.getAssignee());
            item.setReporter(request.getReporter());
            item.setProject(request.getProject());
            item.setState(WorkflowState.CREATED);
            item.setCreated(java.time.LocalDateTime.now());
            item.setUpdated(java.time.LocalDateTime.now());
            
            items.add(item);
            return item;
        }
        
        @Override
        public WorkItem createItem(WorkItem item) {
            if (item.getId() == null) {
                item.setId(java.util.UUID.randomUUID().toString());
            }
            items.add(item);
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
        public List<WorkItem> findByType(WorkItemType type) {
            return items.stream()
                .filter(item -> item.getType() == type)
                .collect(Collectors.toList());
        }
        
        @Override
        public List<WorkItem> findByAssignee(String assignee) {
            return items.stream()
                .filter(item -> assignee.equals(item.getAssignee()))
                .collect(Collectors.toList());
        }
        
        @Override
        public WorkItem findItemByShortId(String shortId) {
            return items.stream()
                .filter(item -> item.getId().endsWith(shortId))
                .findFirst()
                .orElse(null);
        }
        
        @Override
        public WorkItem updateTitle(java.util.UUID id, String title, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setTitle(title);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateDescription(java.util.UUID id, String description, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setDescription(description);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateField(java.util.UUID id, String field, String value, String user) {
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
        public WorkItem assignTo(java.util.UUID id, String assignee, String user) {
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
        public WorkItem updatePriority(java.util.UUID id, Priority priority, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setPriority(priority);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateCustomFields(String id, java.util.Map<String, String> customFields) {
            return getItem(id); // No-op in mock
        }
        
        // Test helper methods
        public List<WorkItem> getItems() {
            return items;
        }
        
        public void clearItems() {
            items.clear();
        }
    }
    
    /**
     * Mock implementation of WorkflowService for testing.
     */
    public static class MockWorkflowService {
        private final List<WorkflowTransition> transitions = new ArrayList<>();
        
        public WorkItem transition(String itemId, String user, WorkflowState targetState, String comment) {
            transitions.add(new WorkflowTransition(itemId, user, targetState, comment));
            
            // Find the item in the mock item service and update its state
            ItemService itemService = TestContext.getInstance().getMockItemService();
            WorkItem item = itemService.getItem(itemId);
            if (item != null) {
                item.setState(targetState);
                itemService.updateItem(item);
            }
            
            return item;
        }
        
        public List<WorkflowTransition> getTransitions() {
            return transitions;
        }
        
        public static class WorkflowTransition {
            private final String itemId;
            private final String user;
            private final WorkflowState targetState;
            private final String comment;
            
            public WorkflowTransition(String itemId, String user, WorkflowState targetState, String comment) {
                this.itemId = itemId;
                this.user = user;
                this.targetState = targetState;
                this.comment = comment;
            }
            
            public String getItemId() {
                return itemId;
            }
            
            public String getUser() {
                return user;
            }
            
            public WorkflowState getTargetState() {
                return targetState;
            }
            
            public String getComment() {
                return comment;
            }
        }
    }
    
    /**
     * Mock implementation of BacklogService for testing.
     */
    public static class MockBacklogService {
        private final List<WorkItem> addedItems = new ArrayList<>();
        
        public void addToBacklog(String user, WorkItem item) {
            addedItems.add(item);
        }
        
        public List<WorkItem> getAddedItems() {
            return addedItems;
        }
    }
}