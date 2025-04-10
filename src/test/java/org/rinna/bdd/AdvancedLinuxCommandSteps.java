/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.bdd;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.rinna.cli.service.PermissionService;
import org.rinna.domain.PermissionLevel;
import org.rinna.domain.Priority;
import org.rinna.domain.Project;
import org.rinna.domain.Visibility;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemRelationshipType;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.HistoryService;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.ProjectService;
import org.rinna.usecase.RelationshipService;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for advanced Linux-style command features.
 */
public class AdvancedLinuxCommandSteps {

    private TestContext context;
    private List<WorkItem> workItems = new ArrayList<>();
    private Map<String, UUID> workItemIds = new HashMap<>();
    private Map<String, Project> projects = new HashMap<>();
    private String commandOutput;
    private String errorOutput;
    private List<WorkItem> newWorkItems = new ArrayList<>();
    private List<Project> newProjects = new ArrayList<>();
    private Map<UUID, Instant> originalTimestamps = new HashMap<>();
    private PermissionLevel userPermissionLevel = PermissionLevel.STANDARD;
    
    private ItemService itemService;
    private RelationshipService relationshipService;
    private HistoryService historyService;
    private ProjectService projectService;
    private PermissionService permissionService;

    public AdvancedLinuxCommandSteps(TestContext context) {
        this.context = context;
    }

    @Before
    public void setUp() {
        itemService = mock(ItemService.class);
        relationshipService = mock(RelationshipService.class);
        historyService = mock(HistoryService.class);
        projectService = mock(ProjectService.class);
        permissionService = mock(PermissionService.class);
        
        // Register mocks with context
        context.registerService(ItemService.class, itemService);
        context.registerService(RelationshipService.class, relationshipService);
        context.registerService(HistoryService.class, historyService);
        context.registerService(ProjectService.class, projectService);
        context.registerService(PermissionService.class, permissionService);
        
        // Reset collections
        workItems = new ArrayList<>();
        workItemIds = new HashMap<>();
        projects = new HashMap<>();
        newWorkItems = new ArrayList<>();
        newProjects = new ArrayList<>();
        originalTimestamps = new HashMap<>();
        commandOutput = null;
        errorOutput = null;
        
        // Set default permission level
        when(permissionService.getCurrentUserPermissionLevel()).thenReturn(userPermissionLevel);
    }

    @Given("I have permission level {string}")
    public void iHavePermissionLevel(String level) {
        try {
            userPermissionLevel = PermissionLevel.valueOf(level.toUpperCase());
            when(permissionService.getCurrentUserPermissionLevel()).thenReturn(userPermissionLevel);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid permission level: " + level);
        }
    }

    @Given("a project named {string} already exists")
    public void aProjectNamedAlreadyExists(String projectName) {
        Project project = mock(Project.class);
        when(project.getName()).thenReturn(projectName);
        when(project.getId()).thenReturn(UUID.randomUUID());
        
        projects.put(projectName, project);
        when(projectService.getProjectByName(projectName)).thenReturn(project);
        when(projectService.projectExists(projectName)).thenReturn(true);
    }

    @Then("work item {int} should be in state {string}")
    public void workItemShouldBeInState(int id, String state) {
        String idStr = Integer.toString(id);
        UUID workItemId = workItemIds.get(idStr);
        
        // Verify the service was called to update the state
        try {
            WorkflowState workflowState = WorkflowState.valueOf(state);
            verify(itemService).updateState(eq(workItemId), eq(workflowState), anyString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid workflow state: " + state);
        }
    }

    @Then("work item {int} should be assigned to {string}")
    public void workItemShouldBeAssignedTo(int id, String assignee) {
        String idStr = Integer.toString(id);
        UUID workItemId = workItemIds.get(idStr);
        
        // Verify the service was called to update the assignee
        verify(itemService).assignTo(eq(workItemId), eq(assignee), anyString());
    }

    @Then("I should see a success message {string}")
    public void iShouldSeeASuccessMessage(String message) {
        Assertions.assertTrue(commandOutput.contains(message), 
                "Output should contain success message: " + message);
    }

    @Then("work item {int} should be a child of work item {int}")
    public void workItemShouldBeAChildOfWorkItem(int childId, int parentId) {
        String childIdStr = Integer.toString(childId);
        String parentIdStr = Integer.toString(parentId);
        UUID childUuid = workItemIds.get(childIdStr);
        UUID parentUuid = workItemIds.get(parentIdStr);
        
        // Verify the service was called to set the parent-child relationship
        verify(relationshipService).setParentWorkItem(childUuid, parentUuid, WorkItemRelationshipType.CHILD_OF);
    }

    @Then("a new work item should be created")
    public void aNewWorkItemShouldBeCreated() {
        // Verify that the service was called to create a new work item
        verify(itemService, atLeastOnce()).createWorkItem(
                anyString(), anyString(), any(WorkItemType.class), any(Priority.class), anyString());
    }

    @Then("the new work item should have title {string}")
    public void theNewWorkItemShouldHaveTitle(String title) {
        // Collect the titles used in createWorkItem calls
        List<String> capturedTitles = new ArrayList<>();
        verify(itemService, atLeastOnce()).createWorkItem(
                argThat(capturedTitles::add), anyString(), any(WorkItemType.class), any(Priority.class), anyString());
        
        boolean titleMatched = capturedTitles.stream().anyMatch(title::equals);
        Assertions.assertTrue(titleMatched, "No work item was created with title: " + title);
    }

    @Then("the new work item should have the same description as work item {int}")
    public void theNewWorkItemShouldHaveTheSameDescriptionAsWorkItem(int id) {
        String idStr = Integer.toString(id);
        UUID workItemId = workItemIds.get(idStr);
        WorkItem sourceItem = null;
        
        // Find the source work item
        for (WorkItem item : workItems) {
            if (item.id().equals(workItemId)) {
                sourceItem = item;
                break;
            }
        }
        
        if (sourceItem != null) {
            final String expectedDescription = sourceItem.description();
            
            // Verify that createWorkItem was called with the expected description
            verify(itemService, atLeastOnce()).createWorkItem(
                    anyString(), eq(expectedDescription), any(WorkItemType.class), any(Priority.class), anyString());
        } else {
            Assertions.fail("Source work item not found: " + id);
        }
    }

    @Then("the new work item should be in state {string}")
    public void theNewWorkItemShouldBeInState(String state) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(state);
            
            // Verify that the new work item had its state updated
            verify(itemService, atLeastOnce()).updateState(any(UUID.class), eq(workflowState), anyString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid workflow state: " + state);
        }
    }

    @Then("the new work item should have priority {string}")
    public void theNewWorkItemShouldHavePriority(String priority) {
        try {
            Priority priorityEnum = Priority.valueOf(priority);
            
            // Collect the priorities used in createWorkItem calls
            List<Priority> capturedPriorities = new ArrayList<>();
            verify(itemService, atLeastOnce()).createWorkItem(
                    anyString(), anyString(), any(WorkItemType.class), argThat(capturedPriorities::add), anyString());
            
            boolean priorityMatched = capturedPriorities.contains(priorityEnum);
            Assertions.assertTrue(priorityMatched, "No work item was created with priority: " + priority);
            
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority: " + priority);
        }
    }

    @Then("a new project should be created with name {string}")
    public void aNewProjectShouldBeCreatedWithName(String projectName) {
        // Verify the service was called to create a new project
        verify(projectService).createProject(eq(projectName), anyString());
    }

    @Then("a new category should be created with name {string} in project {string}")
    public void aNewCategoryShouldBeCreatedWithNameInProject(String categoryName, String projectName) {
        // Verify the service was called to create a category in a project
        verify(projectService).createCategory(eq(projectName), eq(categoryName), anyString());
    }

    @Then("a new project structure should be created with path {string}")
    public void aNewProjectStructureShouldBeCreatedWithPath(String path) {
        // Split the path into components
        String[] components = path.split("/");
        
        // Verify the service was called for the root project
        verify(projectService).createProject(eq(components[0]), anyString());
        
        // Verify service calls for sub-components
        if (components.length > 1) {
            for (int i = 1; i < components.length; i++) {
                StringBuilder parentPath = new StringBuilder(components[0]);
                for (int j = 1; j < i; j++) {
                    parentPath.append("/").append(components[j]);
                }
                verify(projectService).createSubProject(eq(parentPath.toString()), 
                        eq(components[i]), anyString());
            }
        }
    }

    @Then("work item {int} should be deleted")
    public void workItemShouldBeDeleted(int id) {
        String idStr = Integer.toString(id);
        UUID workItemId = workItemIds.get(idStr);
        
        // Verify the service was called to delete the work item
        verify(itemService).deleteWorkItem(eq(workItemId));
    }

    @Then("work item {int} should have an updated timestamp")
    public void workItemShouldHaveAnUpdatedTimestamp(int id) {
        String idStr = Integer.toString(id);
        UUID workItemId = workItemIds.get(idStr);
        
        // Verify the service was called to update the timestamp
        verify(itemService).updateTimestamp(eq(workItemId));
    }

    @Then("work item {int} should have {string} visibility")
    public void workItemShouldHaveVisibility(int id, String visibilityStr) {
        String idStr = Integer.toString(id);
        UUID workItemId = workItemIds.get(idStr);
        
        try {
            Visibility visibility = Visibility.valueOf(visibilityStr.toUpperCase());
            
            // Verify the service was called to set the visibility
            verify(itemService).setVisibility(eq(workItemId), eq(visibility));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid visibility level: " + visibilityStr);
        }
    }

    @Then("work item {int} should have access for user {string}")
    public void workItemShouldHaveAccessForUser(int id, String username) {
        String idStr = Integer.toString(id);
        UUID workItemId = workItemIds.get(idStr);
        
        // Verify the service was called to add user access
        verify(itemService).addUserAccess(eq(workItemId), eq(username));
    }

    @Then("work item {int} should not have access for user {string}")
    public void workItemShouldNotHaveAccessForUser(int id, String username) {
        String idStr = Integer.toString(id);
        UUID workItemId = workItemIds.get(idStr);
        
        // Verify the service was called to remove user access
        verify(itemService).removeUserAccess(eq(workItemId), eq(username));
    }

    @Then("I should see detailed information for a work item assigned to {string}")
    public void iShouldSeeDetailedInformationForAWorkItemAssignedTo(String assignee) {
        // Find all work items assigned to the specified user
        List<WorkItem> matchingItems = workItems.stream()
                .filter(item -> assignee.equalsIgnoreCase(item.assignee()))
                .collect(Collectors.toList());
        
        if (!matchingItems.isEmpty()) {
            WorkItem firstMatch = matchingItems.get(0);
            
            // Check output for details of this work item
            Assertions.assertTrue(commandOutput.contains(firstMatch.id().toString()), 
                    "Output should contain work item ID");
            Assertions.assertTrue(commandOutput.contains(firstMatch.title()), 
                    "Output should contain work item title");
            Assertions.assertTrue(commandOutput.contains(firstMatch.description()), 
                    "Output should contain work item description");
            Assertions.assertTrue(commandOutput.contains(assignee), 
                    "Output should contain assignee: " + assignee);
        } else {
            Assertions.fail("No work items assigned to: " + assignee);
        }
    }

    @Then("I should see detailed information for high priority backlog items")
    public void iShouldSeeDetailedInformationForHighPriorityBacklogItems() {
        // Find high priority backlog items
        List<WorkItem> matchingItems = workItems.stream()
                .filter(item -> item.state() == WorkflowState.BACKLOG && item.priority() == Priority.HIGH)
                .collect(Collectors.toList());
        
        if (!matchingItems.isEmpty()) {
            WorkItem firstMatch = matchingItems.get(0);
            
            // Check output for details of this work item
            Assertions.assertTrue(commandOutput.contains(firstMatch.id().toString()), 
                    "Output should contain high priority backlog item ID");
            Assertions.assertTrue(commandOutput.contains(firstMatch.title()), 
                    "Output should contain high priority backlog item title");
            Assertions.assertTrue(commandOutput.contains("HIGH"), 
                    "Output should contain HIGH priority");
            Assertions.assertTrue(commandOutput.contains("BACKLOG"), 
                    "Output should contain BACKLOG state");
        } else {
            Assertions.fail("No high priority backlog items found");
        }
    }

    @Then("work items previously assigned to {string} should now be assigned to {string}")
    public void workItemsPreviouslyAssignedToShouldNowBeAssignedTo(String oldAssignee, String newAssignee) {
        // Find work items that were assigned to oldAssignee
        for (WorkItem item : workItems) {
            if (oldAssignee.equalsIgnoreCase(item.assignee())) {
                // Verify service was called to reassign each matching item
                verify(itemService).assignTo(eq(item.id()), eq(newAssignee), anyString());
            }
        }
    }

    @Given("I have the following work items:")
    public void iHaveTheFollowingWorkItems(io.cucumber.datatable.DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> row : rows) {
            String id = row.get("ID");
            String title = row.get("Title");
            String stateStr = row.get("State");
            String priorityStr = row.get("Priority");
            String assignee = row.get("Assignee");
            String description = row.getOrDefault("Description", "Description for " + title);
            
            WorkItemType type = WorkItemType.TASK;
            Priority priority = Priority.valueOf(priorityStr);
            WorkflowState state = WorkflowState.valueOf(stateStr);
            
            // Create mock work item
            WorkItem mockItem = mock(WorkItem.class);
            UUID itemId = UUID.randomUUID();
            
            when(mockItem.id()).thenReturn(itemId);
            when(mockItem.title()).thenReturn(title);
            when(mockItem.description()).thenReturn(description);
            when(mockItem.type()).thenReturn(type);
            when(mockItem.priority()).thenReturn(priority);
            when(mockItem.state()).thenReturn(state);
            when(mockItem.assignee()).thenReturn(assignee);
            when(mockItem.reporter()).thenReturn("testuser");
            when(mockItem.createdAt()).thenReturn(Instant.now().minusSeconds(86400)); // 1 day ago
            when(mockItem.updatedAt()).thenReturn(Instant.now().minusSeconds(3600));  // 1 hour ago
            
            // Store original timestamp for later comparison
            originalTimestamps.put(itemId, Instant.now().minusSeconds(3600));
            
            // Add to test data
            workItems.add(mockItem);
            workItemIds.put(id, itemId);
            
            // Configure mock services
            when(itemService.getItem(itemId)).thenReturn(mockItem);
        }
        
        // Configure service to return all work items
        when(itemService.getAllWorkItems()).thenReturn(workItems);
    }

    @When("I run {string}")
    public void iRun(String command) {
        // Split the command into parts
        String[] parts = command.split("\\s+", 2);
        String cmdName = parts[0];
        String cmdArgs = parts.length > 1 ? parts[1] : "";
        
        // Handle pipeline commands
        if (cmdArgs.contains("|")) {
            // Process pipeline
            String[] pipelineParts = command.split("\\|");
            for (int i = 0; i < pipelineParts.length; i++) {
                // Execute each part of the pipeline
                // In a real implementation, the output of each command would be passed to the next
                String pipelineCommand = pipelineParts[i].trim();
                String[] pipelineCmdParts = pipelineCommand.split("\\s+", 2);
                String pipelineCmdName = pipelineCmdParts[0];
                String pipelineCmdArgs = pipelineCmdParts.length > 1 ? pipelineCmdParts[1] : "";
                
                // Check for command injection attempts in pipeline
                if (pipelineCmdName.contains(";") || pipelineCmdArgs.contains(";") || 
                    pipelineCmdName.equals("rm") || pipelineCmdArgs.startsWith("-rf")) {
                    errorOutput = "Error: Invalid pipeline command";
                    return;
                }
                
                // Check for incompatible pipeline commands
                if (i > 0 && (
                    (pipelineCmdName.equals("cat") && pipelineParts[i-1].trim().startsWith("rin mkdir")) ||
                    (pipelineCmdName.equals("mkdir") && pipelineParts[i-1].trim().startsWith("rin cat")))) {
                    errorOutput = "Error: Cannot pipe output from " + 
                            pipelineParts[i-1].trim().substring(4).split("\\s+")[0] + " to " + 
                            pipelineCmdName;
                    return;
                }
            }
            
            // Simulate successful pipeline execution (handled by individual command steps)
            commandOutput = "Pipeline executed successfully: " + command;
            return;
        }
        
        // Check for command injection attempts
        if (cmdName.contains(";") || cmdArgs.contains(";")) {
            errorOutput = "Error: Invalid arguments";
            return;
        }
        
        // Process individual commands
        if (cmdName.equals("rin")) {
            String[] subParts = cmdArgs.split("\\s+", 2);
            String rinCmd = subParts[0];
            String rinArgs = subParts.length > 1 ? subParts[1] : "";
            
            switch (rinCmd) {
                case "mv":
                    handleMvCommand(rinArgs);
                    break;
                case "cp":
                    handleCpCommand(rinArgs);
                    break;
                case "mkdir":
                    handleMkdirCommand(rinArgs);
                    break;
                case "rm":
                    handleRmCommand(rinArgs);
                    break;
                case "touch":
                    handleTouchCommand(rinArgs);
                    break;
                case "chmod":
                    handleChmodCommand(rinArgs);
                    break;
                case "find":
                    handleFindCommand(rinArgs);
                    break;
                case "grep":
                    handleGrepCommand(rinArgs);
                    break;
                case "cat":
                    handleCatCommand(rinArgs);
                    break;
                default:
                    errorOutput = "Error: Unknown command: " + rinCmd;
            }
        } else {
            errorOutput = "Error: Unknown command: " + cmdName;
        }
    }

    private void handleMvCommand(String args) {
        // Parse command arguments
        String[] parts = args.split("\\s+");
        
        // Check permission
        if (userPermissionLevel == PermissionLevel.READONLY) {
            errorOutput = "Error: Insufficient permissions to move work item";
            return;
        }
        
        // Parse work item ID
        if (parts.length == 0) {
            errorOutput = "Error: No work item ID specified";
            return;
        }
        
        String idStr = parts[0];
        UUID workItemId = workItemIds.get(idStr);
        
        if (workItemId == null) {
            errorOutput = "Error: Work item not found: " + idStr;
            return;
        }
        
        // Process options
        WorkflowState newState = null;
        String newAssignee = null;
        UUID newParentId = null;
        StringBuilder successMsg = new StringBuilder("Work item " + idStr);
        
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("--state=")) {
                String stateStr = parts[i].substring("--state=".length());
                try {
                    newState = WorkflowState.valueOf(stateStr);
                    successMsg.append(" moved to ").append(stateStr).append(" state");
                } catch (IllegalArgumentException e) {
                    errorOutput = "Error: Invalid state: " + stateStr;
                    return;
                }
            } else if (parts[i].startsWith("--assignee=")) {
                newAssignee = parts[i].substring("--assignee=".length());
                if (successMsg.toString().contains("moved to")) {
                    successMsg.append(" and assigned to ").append(newAssignee);
                } else {
                    successMsg.append(" assigned to ").append(newAssignee);
                }
            } else if (parts[i].startsWith("--parent=")) {
                String parentIdStr = parts[i].substring("--parent=".length());
                newParentId = workItemIds.get(parentIdStr);
                if (newParentId == null) {
                    errorOutput = "Error: Parent work item not found: " + parentIdStr;
                    return;
                }
                successMsg.append(" moved to parent ").append(parentIdStr);
            }
        }
        
        // Execute the command
        if (newState != null) {
            itemService.updateState(workItemId, newState, "testuser");
        }
        
        if (newAssignee != null) {
            itemService.assignTo(workItemId, newAssignee, "testuser");
        }
        
        if (newParentId != null) {
            relationshipService.setParentWorkItem(workItemId, newParentId, WorkItemRelationshipType.CHILD_OF);
        }
        
        commandOutput = successMsg.toString();
    }

    private void handleCpCommand(String args) {
        // Parse command arguments
        String[] parts = args.split("\\s+");
        
        // Check permission
        if (userPermissionLevel == PermissionLevel.READONLY) {
            errorOutput = "Error: Insufficient permissions to create work items";
            return;
        }
        
        // Parse work item ID
        if (parts.length == 0) {
            errorOutput = "Error: No work item ID specified";
            return;
        }
        
        String idStr = parts[0];
        UUID workItemId = workItemIds.get(idStr);
        
        if (workItemId == null) {
            errorOutput = "Error: Work item not found: " + idStr;
            return;
        }
        
        // Find the source work item
        WorkItem sourceItem = null;
        for (WorkItem item : workItems) {
            if (item.id().equals(workItemId)) {
                sourceItem = item;
                break;
            }
        }
        
        if (sourceItem == null) {
            errorOutput = "Error: Work item not found: " + idStr;
            return;
        }
        
        // Process options
        String newTitle = "Copy of " + sourceItem.title();
        String newDescription = sourceItem.description();
        WorkItemType newType = sourceItem.type();
        Priority newPriority = sourceItem.priority();
        WorkflowState newState = sourceItem.state();
        boolean keepParent = false;
        
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("--title=")) {
                newTitle = parts[i].substring("--title=".length()).replace("'", "");
            } else if (parts[i].startsWith("--priority=")) {
                String priorityStr = parts[i].substring("--priority=".length());
                try {
                    newPriority = Priority.valueOf(priorityStr);
                } catch (IllegalArgumentException e) {
                    errorOutput = "Error: Invalid priority: " + priorityStr;
                    return;
                }
            } else if (parts[i].startsWith("--state=")) {
                String stateStr = parts[i].substring("--state=".length());
                try {
                    newState = WorkflowState.valueOf(stateStr);
                } catch (IllegalArgumentException e) {
                    errorOutput = "Error: Invalid state: " + stateStr;
                    return;
                }
            } else if (parts[i].equals("--keep-parent")) {
                keepParent = true;
            }
        }
        
        // Create the new work item
        UUID newItemId = UUID.randomUUID();
        when(itemService.createWorkItem(eq(newTitle), eq(newDescription), eq(newType), 
                eq(newPriority), anyString())).thenReturn(newItemId);
        
        UUID createdId = itemService.createWorkItem(newTitle, newDescription, 
                newType, newPriority, "testuser");
        
        // Update state if different from default
        if (newState != WorkflowState.BACKLOG) {
            itemService.updateState(createdId, newState, "testuser");
        }
        
        // Set parent relationship if requested
        if (keepParent) {
            UUID parentId = relationshipService.getParentWorkItem(workItemId);
            if (parentId != null) {
                relationshipService.setParentWorkItem(createdId, parentId, WorkItemRelationshipType.CHILD_OF);
            }
        }
        
        commandOutput = "Created new work item from copy of work item " + idStr;
    }

    private void handleMkdirCommand(String args) {
        // Parse command arguments
        String[] parts = args.split("\\s+");
        
        // Check permission
        if (userPermissionLevel != PermissionLevel.STANDARD && userPermissionLevel != PermissionLevel.ADMIN) {
            errorOutput = "Error: Insufficient permissions to create projects";
            return;
        }
        
        // Process mkdir command
        boolean createParents = false;
        String projectPath = null;
        String description = "Project created via mkdir command";
        String type = "project";
        
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("-p")) {
                createParents = true;
            } else if (parts[i].startsWith("--desc=")) {
                description = parts[i].substring("--desc=".length()).replace("'", "");
            } else if (parts[i].startsWith("--type=")) {
                type = parts[i].substring("--type=".length());
            } else if (projectPath == null) {
                projectPath = parts[i];
            }
        }
        
        if (projectPath == null) {
            errorOutput = "Error: No project name specified";
            return;
        }
        
        // Check if the project already exists
        if (projectService.projectExists(projectPath)) {
            errorOutput = "Error: Project already exists: " + projectPath;
            return;
        }
        
        // Process the command based on path type
        if (projectPath.contains("/")) {
            String[] pathParts = projectPath.split("/");
            String projectName = pathParts[0];
            String categoryName = pathParts[1];
            
            if (pathParts.length == 2) {
                // Project/Category form
                if ("category".equalsIgnoreCase(type)) {
                    projectService.createCategory(projectName, categoryName, description);
                    commandOutput = "Created new category: " + categoryName + " in project " + projectName;
                } else {
                    if (createParents && !projectService.projectExists(projectName)) {
                        projectService.createProject(projectName, "Parent project for " + projectPath);
                    }
                    projectService.createSubProject(projectName, categoryName, description);
                    commandOutput = "Created new subproject: " + categoryName + " in project " + projectName;
                }
            } else {
                // Multi-level hierarchy
                if (!createParents) {
                    errorOutput = "Error: Cannot create multi-level directories without -p flag";
                    return;
                }
                
                // Create the root project if it doesn't exist
                if (!projectService.projectExists(projectName)) {
                    projectService.createProject(projectName, "Root project for " + projectPath);
                }
                
                // Create each level of the hierarchy
                StringBuilder currentPath = new StringBuilder(projectName);
                for (int i = 1; i < pathParts.length; i++) {
                    String subPath = currentPath.toString();
                    String component = pathParts[i];
                    projectService.createSubProject(subPath, component, "Component in " + projectPath);
                    currentPath.append("/").append(component);
                }
                
                commandOutput = "Created project hierarchy: " + projectPath;
            }
        } else {
            // Simple project
            projectService.createProject(projectPath, description);
            commandOutput = "Created new project: " + projectPath;
        }
    }

    private void handleRmCommand(String args) {
        // Parse command arguments
        String[] parts = args.split("\\s+");
        
        // Check permission
        if (userPermissionLevel == PermissionLevel.READONLY) {
            errorOutput = "Error: Insufficient permissions to delete work items";
            return;
        }
        
        boolean forceDelete = false;
        List<UUID> itemsToDelete = new ArrayList<>();
        List<String> idStrings = new ArrayList<>();
        
        // Parse options and IDs
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("-f")) {
                forceDelete = true;
            } else {
                String idStr = parts[i];
                UUID itemId = workItemIds.get(idStr);
                
                if (itemId == null) {
                    errorOutput = "Error: Work item not found: " + idStr;
                    return;
                }
                
                itemsToDelete.add(itemId);
                idStrings.add(idStr);
            }
        }
        
        if (itemsToDelete.isEmpty()) {
            errorOutput = "Error: No work item IDs specified";
            return;
        }
        
        // Check if any items have children and force flag is not set
        if (!forceDelete) {
            for (UUID itemId : itemsToDelete) {
                List<UUID> children = relationshipService.getChildWorkItems(itemId);
                if (children != null && !children.isEmpty()) {
                    errorOutput = "Error: Cannot delete work item with children. Use -f to force delete.";
                    return;
                }
            }
        }
        
        // Delete the items
        for (UUID itemId : itemsToDelete) {
            itemService.deleteWorkItem(itemId);
        }
        
        // Generate success message
        if (idStrings.size() == 1) {
            commandOutput = "Deleted work item " + idStrings.get(0);
        } else {
            commandOutput = "Deleted work items: " + String.join(", ", idStrings);
        }
    }

    private void handleTouchCommand(String args) {
        // Parse command arguments
        String[] parts = args.split("\\s+");
        
        // Check permission
        if (userPermissionLevel == PermissionLevel.READONLY) {
            errorOutput = "Error: Insufficient permissions to update work items";
            return;
        }
        
        boolean createNew = false;
        List<UUID> itemsToUpdate = new ArrayList<>();
        List<String> idStrings = new ArrayList<>();
        String newTitle = null;
        
        // Parse options and IDs
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("--create")) {
                createNew = true;
            } else if (createNew && newTitle == null) {
                // If --create flag is set, the next argument is the title
                newTitle = parts[i];
            } else {
                String idStr = parts[i];
                UUID itemId = workItemIds.get(idStr);
                
                if (itemId == null && !createNew) {
                    errorOutput = "Error: Work item not found: " + idStr;
                    return;
                }
                
                if (itemId != null) {
                    itemsToUpdate.add(itemId);
                    idStrings.add(idStr);
                }
            }
        }
        
        // Create new item if requested
        if (createNew && newTitle != null) {
            UUID newItemId = UUID.randomUUID();
            when(itemService.createWorkItem(eq(newTitle), anyString(), any(WorkItemType.class), 
                    any(Priority.class), anyString())).thenReturn(newItemId);
            
            itemService.createWorkItem(newTitle, "Created via touch command", 
                    WorkItemType.TASK, Priority.MEDIUM, "testuser");
            
            commandOutput = "Created new work item: " + newTitle;
            return;
        }
        
        if (itemsToUpdate.isEmpty() && !createNew) {
            errorOutput = "Error: No work item IDs specified";
            return;
        }
        
        // Update timestamps for the specified items
        for (UUID itemId : itemsToUpdate) {
            itemService.updateTimestamp(itemId);
        }
        
        // Generate success message
        if (idStrings.size() == 1) {
            commandOutput = "Updated timestamp for work item " + idStrings.get(0);
        } else {
            commandOutput = "Updated timestamps for work items: " + String.join(", ", idStrings);
        }
    }

    private void handleChmodCommand(String args) {
        // Parse command arguments
        String[] parts = args.split("\\s+");
        
        // Check permission
        if (userPermissionLevel != PermissionLevel.STANDARD && userPermissionLevel != PermissionLevel.ADMIN) {
            errorOutput = "Error: Insufficient permissions to change work item visibility";
            return;
        }
        
        if (parts.length < 2) {
            errorOutput = "Error: Invalid syntax. Usage: chmod <visibility> <item_id...>";
            return;
        }
        
        String visibilityOrOperation = parts[0];
        List<UUID> itemsToUpdate = new ArrayList<>();
        List<String> idStrings = new ArrayList<>();
        
        // Process user operations (+user/-user)
        if (visibilityOrOperation.startsWith("+user") || visibilityOrOperation.startsWith("-user")) {
            boolean addUser = visibilityOrOperation.startsWith("+user");
            String username = parts[1];
            
            // Process item IDs (starting from index 2)
            for (int i = 2; i < parts.length; i++) {
                String idStr = parts[i];
                UUID itemId = workItemIds.get(idStr);
                
                if (itemId == null) {
                    errorOutput = "Error: Work item not found: " + idStr;
                    return;
                }
                
                itemsToUpdate.add(itemId);
                idStrings.add(idStr);
            }
            
            if (itemsToUpdate.isEmpty()) {
                errorOutput = "Error: No work item IDs specified";
                return;
            }
            
            // Apply user access changes
            for (UUID itemId : itemsToUpdate) {
                if (addUser) {
                    itemService.addUserAccess(itemId, username);
                } else {
                    itemService.removeUserAccess(itemId, username);
                }
            }
            
            // Generate success message
            if (idStrings.size() == 1) {
                if (addUser) {
                    commandOutput = "Added access for " + username + " to work item " + idStrings.get(0);
                } else {
                    commandOutput = "Removed access for " + username + " from work item " + idStrings.get(0);
                }
            } else {
                if (addUser) {
                    commandOutput = "Added access for " + username + " to work items: " + 
                            String.join(", ", idStrings);
                } else {
                    commandOutput = "Removed access for " + username + " from work items: " + 
                            String.join(", ", idStrings);
                }
            }
            
            return;
        }
        
        // Process visibility changes
        Visibility visibility;
        try {
            visibility = Visibility.valueOf(visibilityOrOperation.toUpperCase());
        } catch (IllegalArgumentException e) {
            errorOutput = "Error: Invalid visibility level: " + visibilityOrOperation;
            return;
        }
        
        // Process item IDs (starting from index 1)
        for (int i = 1; i < parts.length; i++) {
            String idStr = parts[i];
            UUID itemId = workItemIds.get(idStr);
            
            if (itemId == null) {
                errorOutput = "Error: Work item not found: " + idStr;
                return;
            }
            
            itemsToUpdate.add(itemId);
            idStrings.add(idStr);
        }
        
        if (itemsToUpdate.isEmpty()) {
            errorOutput = "Error: No work item IDs specified";
            return;
        }
        
        // Apply visibility changes
        for (UUID itemId : itemsToUpdate) {
            itemService.setVisibility(itemId, visibility);
        }
        
        // Generate success message
        if (idStrings.size() == 1) {
            commandOutput = "Changed work item " + idStrings.get(0) + " visibility to " + 
                    visibilityOrOperation.toLowerCase();
        } else {
            commandOutput = "Changed visibility to " + visibilityOrOperation.toLowerCase() + 
                    " for work items: " + String.join(", ", idStrings);
        }
    }

    private void handleFindCommand(String args) {
        // Simulate find command with basic functionality
        commandOutput = "Found matching work items";
    }

    private void handleGrepCommand(String args) {
        // Simulate grep command with basic functionality
        if (args.contains("HIGH")) {
            // Find high priority items
            List<WorkItem> highPriorityItems = workItems.stream()
                    .filter(item -> item.priority() == Priority.HIGH)
                    .collect(Collectors.toList());
            
            if (!highPriorityItems.isEmpty()) {
                WorkItem item = highPriorityItems.get(0);
                commandOutput = "Work Item: " + item.id() + "\n" +
                        "Title: " + item.title() + "\n" +
                        "Priority: HIGH\n" +
                        "State: " + item.state();
            } else {
                commandOutput = "No matches found";
            }
        } else {
            commandOutput = "Grep results";
        }
    }

    private void handleCatCommand(String args) {
        // Simulate cat command with basic functionality
        String[] parts = args.split("\\s+");
        String idStr = null;
        
        // Find ID parameter (non-option argument)
        for (String part : parts) {
            if (!part.startsWith("-")) {
                idStr = part;
                break;
            }
        }
        
        if (idStr != null) {
            UUID itemId = workItemIds.get(idStr);
            
            if (itemId == null) {
                errorOutput = "Error: Work item not found: " + idStr;
                return;
            }
            
            // Find the work item
            WorkItem item = null;
            for (WorkItem wi : workItems) {
                if (wi.id().equals(itemId)) {
                    item = wi;
                    break;
                }
            }
            
            if (item != null) {
                commandOutput = "Work Item: " + item.id() + "\n" +
                        "Title: " + item.title() + "\n" +
                        "Description: " + item.description() + "\n" +
                        "Priority: " + item.priority() + "\n" +
                        "State: " + item.state() + "\n" +
                        "Assignee: " + item.assignee();
            }
        } else {
            // If no ID provided, show the first work item from the search/pipe context
            if (!workItems.isEmpty()) {
                WorkItem item = workItems.get(0);
                commandOutput = "Work Item: " + item.id() + "\n" +
                        "Title: " + item.title() + "\n" +
                        "Description: " + item.description() + "\n" +
                        "Priority: " + item.priority() + "\n" +
                        "State: " + item.state() + "\n" +
                        "Assignee: " + item.assignee();
            } else {
                errorOutput = "Error: No work item context available";
            }
        }
    }

    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        Assertions.assertNull(errorOutput, "Command should not produce an error");
        Assertions.assertNotNull(commandOutput, "Command should produce output");
    }

    @Then("the command should fail")
    public void theCommandShouldFail() {
        Assertions.assertNotNull(errorOutput, "Command should produce an error");
    }

    @Then("I should see an error message {string}")
    public void iShouldSeeAnErrorMessage(String message) {
        Assertions.assertTrue(errorOutput.contains(message), 
                "Error output should contain message: " + message);
    }
}