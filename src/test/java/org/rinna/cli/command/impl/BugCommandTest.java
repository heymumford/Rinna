/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemCreateRequest;
import org.rinna.domain.WorkItemType;
import org.rinna.usecase.BacklogService;
import org.rinna.usecase.ItemService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BugCommandTest {

    @Mock
    private ItemService itemService;

    @Mock
    private BacklogService backlogService;

    @Mock
    private WorkItem createdWorkItem;

    private BugCommand bugCommand;

    @BeforeEach
    void setUp() {
        bugCommand = new BugCommand(itemService, backlogService);
    }

    @Test
    @DisplayName("Should create a bug with minimal input")
    void testCreateBugWithMinimalInput() {
        // Arrange
        String[] args = {"A critical bug in production"};
        UUID mockId = UUID.randomUUID();
        
        when(itemService.create(any(WorkItemCreateRequest.class))).thenReturn(createdWorkItem);
        when(createdWorkItem.getId()).thenReturn(mockId);
        when(backlogService.addToBacklog(mockId)).thenReturn(true);

        // Act
        int result = bugCommand.execute(args);

        // Assert
        assertEquals(0, result);
        verify(itemService).create(argThat(request -> {
            assertEquals("A critical bug in production", request.getTitle());
            assertEquals(WorkItemType.BUG, request.getType());
            assertEquals(Priority.MEDIUM, request.getPriority()); // Default priority
            return true;
        }));
        verify(backlogService).addToBacklog(mockId);
    }

    @Test
    @DisplayName("Should create a bug with custom priority")
    void testCreateBugWithCustomPriority() {
        // Arrange
        String[] args = {"A critical bug in production", "--priority=HIGH"};
        UUID mockId = UUID.randomUUID();
        
        when(itemService.create(any(WorkItemCreateRequest.class))).thenReturn(createdWorkItem);
        when(createdWorkItem.getId()).thenReturn(mockId);
        when(backlogService.addToBacklog(mockId)).thenReturn(true);

        // Act
        int result = bugCommand.execute(args);

        // Assert
        assertEquals(0, result);
        verify(itemService).create(argThat(request -> {
            assertEquals("A critical bug in production", request.getTitle());
            assertEquals(WorkItemType.BUG, request.getType());
            assertEquals(Priority.HIGH, request.getPriority());
            return true;
        }));
        verify(backlogService).addToBacklog(mockId);
    }

    @Test
    @DisplayName("Should create a bug with metadata")
    void testCreateBugWithMetadata() {
        // Arrange
        String[] args = {"A critical bug in production", "--meta=component:login", "--meta=browser:firefox"};
        UUID mockId = UUID.randomUUID();
        
        when(itemService.create(any(WorkItemCreateRequest.class))).thenReturn(createdWorkItem);
        when(createdWorkItem.getId()).thenReturn(mockId);
        when(backlogService.addToBacklog(mockId)).thenReturn(true);
        when(createdWorkItem.getMetadata()).thenReturn(Map.of("component", "login", "browser", "firefox"));

        // Act
        int result = bugCommand.execute(args);

        // Assert
        assertEquals(0, result);
        verify(itemService).create(argThat(request -> {
            assertEquals("A critical bug in production", request.getTitle());
            assertEquals(WorkItemType.BUG, request.getType());
            Map<String, String> metadata = request.getMetadata();
            assertNotNull(metadata);
            assertEquals("login", metadata.get("component"));
            assertEquals("firefox", metadata.get("browser"));
            return true;
        }));
        verify(backlogService).addToBacklog(mockId);
    }

    @Test
    @DisplayName("Should handle empty description")
    void testHandleEmptyDescription() {
        // Arrange
        String[] args = {""};

        // Act
        int result = bugCommand.execute(args);

        // Assert
        assertEquals(1, result);
        verify(itemService, never()).create(any());
        verify(backlogService, never()).addToBacklog(any());
    }

    @Test
    @DisplayName("Should handle invalid priority")
    void testHandleInvalidPriority() {
        // Arrange
        String[] args = {"A critical bug", "--priority=INVALID"};

        // Act
        int result = bugCommand.execute(args);

        // Assert
        assertEquals(1, result);
        verify(itemService, never()).create(any());
        verify(backlogService, never()).addToBacklog(any());
    }

    @Test
    @DisplayName("Should handle failure to add to backlog")
    void testHandleBacklogFailure() {
        // Arrange
        String[] args = {"A critical bug"};
        UUID mockId = UUID.randomUUID();
        
        when(itemService.create(any(WorkItemCreateRequest.class))).thenReturn(createdWorkItem);
        when(createdWorkItem.getId()).thenReturn(mockId);
        when(backlogService.addToBacklog(mockId)).thenReturn(false);

        // Act
        int result = bugCommand.execute(args);

        // Assert
        assertNotEquals(0, result);
        verify(itemService).create(any());
        verify(backlogService).addToBacklog(mockId);
    }
}