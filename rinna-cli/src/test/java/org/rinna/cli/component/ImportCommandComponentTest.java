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

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.ImportCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Component tests for ImportCommand integration with other components.
 */
@DisplayName("ImportCommand Component Tests")
class ImportCommandComponentTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MockWorkflowService mockWorkflowService;
    private MockBacklogService mockBacklogService;
    private ConfigurationService mockConfigService;
    private MetadataService mockMetadataService;
    
    private Path tempFile;
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock services
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = new MockItemService();
        mockWorkflowService = new MockWorkflowService();
        mockBacklogService = new MockBacklogService();
        mockConfigService = mock(ConfigurationService.class);
        mockMetadataService = mock(MetadataService.class);
        
        // Set up mock service manager
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getMockBacklogService()).thenReturn(mockBacklogService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up mock config service
        when(mockConfigService.getCurrentUser()).thenReturn("test.user");
        when(mockConfigService.getCurrentProject()).thenReturn("IMPORT-TEST");
        
        // Set up mock metadata service
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("test-operation-id");
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Clean up temp files
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test
    @DisplayName("Should import tasks with different states and priorities from markdown")
    void shouldImportTasksWithDifferentStatesAndPrioritiesFromMarkdown() throws IOException {
        // Given
        String content = "# Todo\n" +
                        "- Task 1\n" +
                        "- [high] High priority task\n\n" +
                        "# In Progress\n" +
                        "- Working on this\n\n" +
                        "# Done\n" +
                        "- [critical] Completed task\n";
        
        tempFile = Files.createTempFile("test-import-", ".md");
        Files.writeString(tempFile, content);
        
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            ImportCommand command = new ImportCommand();
            command.setFilePath(tempFile.toString());
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Command should succeed");
            
            // Verify items were created
            List<WorkItem> items = mockItemService.getAllItems();
            assertEquals(4, items.size(), "Should import 4 work items");
            
            // Verify item states and priorities
            Map<String, WorkItem> itemsByTitle = items.stream()
                .collect(java.util.stream.Collectors.toMap(WorkItem::getTitle, item -> item));
            
            assertAll(
                () -> assertEquals(WorkflowState.READY, itemsByTitle.get("Task 1").getState()),
                () -> assertEquals(Priority.MEDIUM, itemsByTitle.get("Task 1").getPriority()),
                
                () -> assertEquals(WorkflowState.READY, itemsByTitle.get("High priority task").getState()),
                () -> assertEquals(Priority.HIGH, itemsByTitle.get("High priority task").getPriority()),
                
                () -> assertEquals(WorkflowState.IN_PROGRESS, itemsByTitle.get("Working on this").getState()),
                () -> assertEquals(Priority.MEDIUM, itemsByTitle.get("Working on this").getPriority()),
                
                () -> assertEquals(WorkflowState.DONE, itemsByTitle.get("Completed task").getState()),
                () -> assertEquals(Priority.CRITICAL, itemsByTitle.get("Completed task").getPriority())
            );
            
            // Verify workflow transitions were called
            assertEquals(4, mockWorkflowService.getTransitionCount(),
                       "Should call transition for each work item");
            
            // Verify output format
            String output = outputCaptor.toString();
            assertTrue(output.contains("Successfully imported 4 task(s)"),
                     "Output should indicate successful import");
        }
    }
    
    @Test
    @DisplayName("Should import tasks and add them to backlog when in Backlog section")
    void shouldImportTasksAndAddThemToBacklogWhenInBacklogSection() throws IOException {
        // Given
        String content = "# Backlog\n" +
                        "- Backlog task 1\n" +
                        "- Backlog task 2\n";
        
        tempFile = Files.createTempFile("test-backlog-", ".md");
        Files.writeString(tempFile, content);
        
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            ImportCommand command = new ImportCommand();
            command.setFilePath(tempFile.toString());
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Command should succeed");
            
            // Verify items were created
            List<WorkItem> items = mockItemService.getAllItems();
            assertEquals(2, items.size(), "Should import 2 work items");
            
            // Verify items were added to backlog
            assertEquals(2, mockBacklogService.getAddToBacklogCount(),
                       "Should add 2 items to backlog");
            
            // Verify items have CREATED state
            for (WorkItem item : items) {
                assertEquals(WorkflowState.CREATED, item.getState(),
                           "Backlog items should have CREATED state");
            }
        }
    }
    
    @Test
    @DisplayName("Should produce JSON output when format is set to json")
    void shouldProduceJsonOutputWhenFormatIsSetToJson() throws IOException {
        // Given
        String content = "# Todo\n- Simple task\n";
        
        tempFile = Files.createTempFile("test-json-", ".md");
        Files.writeString(tempFile, content);
        
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            ImportCommand command = new ImportCommand();
            command.setFilePath(tempFile.toString());
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Command should succeed");
            
            // Verify JSON output
            String output = outputCaptor.toString().trim();
            assertTrue(output.startsWith("{"), "Output should start with {");
            assertTrue(output.endsWith("}"), "Output should end with }");
            assertTrue(output.contains("\"status\": \"success\""), "Output should contain success status");
            assertTrue(output.contains("\"imported_count\": 1"), "Output should contain imported count");
        }
    }
    
    @Test
    @DisplayName("Should handle partial import when some content can't be parsed")
    void shouldHandlePartialImportWhenSomeContentCantBeParsed() throws IOException {
        // Given
        String content = "# Todo\n" +
                        "- Valid task\n" +
                        "- Not a proper task format\n" +
                        "Some random text\n";
        
        tempFile = Files.createTempFile("test-partial-", ".md");
        Files.writeString(tempFile, content);
        
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            ImportCommand command = new ImportCommand();
            command.setFilePath(tempFile.toString());
            
            // Mock the file system operations for the report
            File mockReportParent = mock(File.class);
            when(mockReportParent.mkdirs()).thenReturn(true);
            File mockReportFile = mock(File.class);
            when(mockReportFile.getParentFile()).thenReturn(mockReportParent);
            when(mockReportFile.getAbsolutePath()).thenReturn("/mock/path/target/import-report.txt");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Command should succeed even with partial parsing");
            
            // Verify only the valid task was imported
            List<WorkItem> items = mockItemService.getAllItems();
            assertEquals(1, items.size(), "Should import only the valid task");
            assertEquals("Valid task", items.get(0).getTitle(), "Should import the valid task");
            
            // Verify output mentions unparsed content
            String output = outputCaptor.toString();
            assertTrue(output.contains("Warning: Some content couldn't be parsed"),
                     "Output should warn about unparsed content");
        }
    }
    
    @Test
    @DisplayName("Should track import operation in metadata service")
    void shouldTrackImportOperationInMetadataService() throws IOException {
        // Given
        String content = "# Todo\n- Task 1\n";
        
        tempFile = Files.createTempFile("test-track-", ".md");
        Files.writeString(tempFile, content);
        
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            ImportCommand command = new ImportCommand();
            command.setFilePath(tempFile.toString());
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Command should succeed");
            
            // Verify metadata service interactions
            verify(mockMetadataService).startOperation(eq("import"), eq("CREATE"), any());
            verify(mockMetadataService).completeOperation(eq("test-operation-id"), any());
            verify(mockMetadataService, never()).failOperation(anyString(), any());
        }
    }
    
    @Test
    @DisplayName("Should fail and track error when importing non-existent file")
    void shouldFailAndTrackErrorWhenImportingNonExistentFile() {
        // Given
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            
            ImportCommand command = new ImportCommand();
            command.setFilePath("nonexistent-file.md");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Command should fail");
            
            // Verify metadata service interactions
            verify(mockMetadataService).startOperation(eq("import"), eq("CREATE"), any());
            verify(mockMetadataService, never()).completeOperation(anyString(), any());
            verify(mockMetadataService).failOperation(eq("test-operation-id"), any(IllegalArgumentException.class));
            
            // Verify error output
            String error = errorCaptor.toString();
            assertTrue(error.contains("Error: File not found"),
                     "Error output should indicate file not found");
        }
    }
    
    /**
     * Mock implementation of ItemService for testing.
     */
    private static class MockItemService implements ItemService {
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
                .collect(java.util.stream.Collectors.toList());
        }
        
        @Override
        public List<WorkItem> findByAssignee(String assignee) {
            return items.stream()
                .filter(item -> assignee.equals(item.getAssignee()))
                .collect(java.util.stream.Collectors.toList());
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
    }
    
    /**
     * Mock implementation of WorkflowService for testing.
     */
    private static class MockWorkflowService {
        private int transitionCount = 0;
        
        public WorkItem transition(String itemId, String user, WorkflowState targetState, String comment) {
            transitionCount++;
            // Update the item state in the mock ItemService
            ItemService itemService = mockServiceManager.getMockItemService();
            WorkItem item = itemService.getItem(itemId);
            if (item != null) {
                item.setState(targetState);
                itemService.updateItem(item);
            }
            return item;
        }
        
        public int getTransitionCount() {
            return transitionCount;
        }
    }
    
    /**
     * Mock implementation of BacklogService for testing.
     */
    private static class MockBacklogService {
        private int addToBacklogCount = 0;
        
        public void addToBacklog(String user, WorkItem item) {
            addToBacklogCount++;
        }
        
        public int getAddToBacklogCount() {
            return addToBacklogCount;
        }
    }
}