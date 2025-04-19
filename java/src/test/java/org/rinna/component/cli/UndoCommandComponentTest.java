/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.component.cli;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.base.ComponentTest;
import org.rinna.cli.RinnaCli;
import org.rinna.cli.command.UndoCommand;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.HistoryEntryType;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkflowState;

/**
 * Component tests for UndoCommand integration with the CLI framework.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UndoCommand Component Tests")
public class UndoCommandComponentTest extends ComponentTest {

    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private MockWorkflowService workflowService;
    
    @Mock
    private MockHistoryService historyService;
    
    @Mock
    private MockItemService itemService;
    
    @InjectMocks
    private UndoCommand undoCommand;
    
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;
    
    private MockedStatic<ServiceManager> serviceManagerMock;
    
    @BeforeEach
    void setUp() {
        // Redirect stdout and stderr for testing
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
        
        // Setup mock ServiceManager
        serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
        serviceManagerMock.when(ServiceManager::getInstance).thenReturn(serviceManager);
        
        // Setup service mocks
        when(serviceManager.getWorkflowService()).thenReturn(workflowService);
        when(serviceManager.getHistoryService()).thenReturn(historyService);
        when(serviceManager.getItemService()).thenReturn(itemService);
    }
    
    @AfterEach
    void tearDown() {
        // Restore original stdout, stderr, stdin
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
        
        // Close the mocked static
        serviceManagerMock.close();
    }
    
    @Test
    @DisplayName("CLI should properly route 'undo' command")
    void cliShouldRouteUndoCommand() {
        // Arrange
        String[] args = {"undo"};
        MockHistoryService.MockHistoryEntry historyEntry = setupMockSuccessfulUndo();
        
        // Simulate user confirmation
        String input = "y\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        // Act
        int exitCode = RinnaCli.main(args);
        
        // Assert
        assertEquals(0, exitCode, "Command should succeed");
        verify(historyService).getHistory(any(UUID.class));
        assertTrue(outputStream.toString().contains("Successfully"), 
                "Should show success message");
    }
    
    @Test
    @DisplayName("CLI should pass --force flag to UndoCommand")
    void cliShouldPassForceFlagToUndoCommand() {
        // Arrange
        String[] args = {"undo", "--force"};
        MockHistoryService.MockHistoryEntry historyEntry = setupMockSuccessfulUndo();
        
        // Act
        int exitCode = RinnaCli.main(args);
        
        // Assert
        assertEquals(0, exitCode, "Command should succeed");
        assertFalse(outputStream.toString().contains("Are you sure"), 
                "Should not show confirmation prompt");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"--item=123-456", "--item", "123-456"})
    @DisplayName("CLI should properly handle item parameters")
    void cliShouldHandleItemParameters(String param) {
        // Arrange
        String[] args;
        if ("--item".equals(param)) {
            args = new String[]{"undo", "--item", "123-456"};
        } else {
            args = new String[]{"undo", param};
        }
        
        // Mock a missing item
        when(workflowService.getItem(any())).thenReturn(null);
        
        // Act
        int exitCode = RinnaCli.main(args);
        
        // Assert
        assertEquals(1, exitCode, "Command should fail when item not found");
        assertTrue(errorStream.toString().contains("Work item not found"), 
                "Should show error message");
    }
    
    @Test
    @DisplayName("CLI should handle combination of parameters")
    void cliShouldHandleCombinationOfParameters() {
        // Arrange
        String[] args = {"undo", "--force", "--item=123-456"};
        
        // Mock a missing item
        when(workflowService.getItem(any())).thenReturn(null);
        
        // Act
        int exitCode = RinnaCli.main(args);
        
        // Assert
        assertEquals(1, exitCode, "Command should fail when item not found");
        assertTrue(errorStream.toString().contains("Work item not found"), 
                "Should show error message");
    }
    
    @Test
    @DisplayName("CLI should handle invalid parameters gracefully")
    void cliShouldHandleInvalidParameters() {
        // Arrange
        String[] args = {"undo", "--invalid-param"};
        MockHistoryService.MockHistoryEntry historyEntry = setupMockSuccessfulUndo();
        
        // Simulate user confirmation
        String input = "y\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        // Act
        int exitCode = RinnaCli.main(args);
        
        // Assert
        assertEquals(0, exitCode, "Command should succeed and ignore invalid parameters");
    }
    
    @Test
    @DisplayName("CLI should handle malformed command execution")
    void cliShouldHandleMalformedCommand() {
        // Arrange - simulate exception during command execution
        String[] args = {"undo"};
        
        doThrow(new RuntimeException("Test exception")).when(historyService).getHistory(any());
        
        // Mock items in progress
        WorkItem mockItem = mock(WorkItem.class);
        when(mockItem.id()).thenReturn(UUID.randomUUID());
        when(mockItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
        when(mockItem.assignee()).thenReturn(System.getProperty("user.name"));
        
        when(workflowService.getItemsInState(any(), any())).thenReturn(java.util.Collections.singletonList(mockItem));
        
        // Act
        int exitCode = RinnaCli.main(args);
        
        // Assert
        assertEquals(1, exitCode, "Command should fail gracefully");
        assertTrue(errorStream.toString().contains("Error"), 
                "Should show error message");
    }
    
    @Test
    @DisplayName("CLI should handle overly long item IDs")
    void cliShouldHandleOverlyLongItemIds() {
        // Arrange - create a very long item ID (potential overflow attack)
        StringBuilder longId = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longId.append("a");
        }
        
        String[] args = {"undo", "--item=" + longId};
        
        // Act
        int exitCode = RinnaCli.main(args);
        
        // Assert
        assertEquals(1, exitCode, "Command should fail gracefully");
        assertTrue(errorStream.toString().contains("Invalid work item ID"), 
                "Should show appropriate error");
    }
    
    @Test
    @DisplayName("CLI should prevent command injection via item parameter")
    void cliShouldPreventCommandInjection() {
        // Arrange - attempt command injection via item parameter
        String maliciousId = "123; rm -rf /";
        String[] args = {"undo", "--item=" + maliciousId};
        
        // Act
        int exitCode = RinnaCli.main(args);
        
        // Assert
        assertEquals(1, exitCode, "Command should fail gracefully");
        assertTrue(errorStream.toString().contains("Invalid work item ID"), 
                "Should reject malicious input");
    }
    
    /**
     * Helper method to set up mocks for a successful undo scenario.
     *
     * @return A mock history entry
     */
    private MockHistoryService.MockHistoryEntry setupMockSuccessfulUndo() {
        // Create a mock work item
        WorkItem mockItem = mock(WorkItem.class);
        UUID itemId = UUID.randomUUID();
        when(mockItem.id()).thenReturn(itemId);
        when(mockItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
        when(mockItem.assignee()).thenReturn(System.getProperty("user.name"));
        
        // Mock items in progress
        when(workflowService.getItemsInState(any(), any()))
            .thenReturn(java.util.Collections.singletonList(mockItem));
        
        // Create a mock history entry
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previousState", "READY");
        metadata.put("newState", "IN_PROGRESS");
        
        MockHistoryService.MockHistoryEntry historyEntry = 
            new MockHistoryService.MockHistoryEntry(
                UUID.randomUUID(),
                itemId,
                HistoryEntryType.STATE_CHANGE,
                System.getProperty("user.name"),
                Instant.now(),
                metadata
            );
        
        // Mock history service
        when(historyService.getHistory(any()))
            .thenReturn(java.util.Collections.singletonList(historyEntry));
            
        return historyEntry;
    }
}