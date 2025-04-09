/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.examples;

import org.rinna.pui.RinnaPUI;
import org.rinna.pui.cli.ServiceBridge;
import org.rinna.pui.component.*;
import org.rinna.pui.component.BoxLayout.Orientation;
import org.rinna.pui.component.BoxLayout.BoxConstraints;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.InvalidTransitionException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Demo that showcases workflow state transitions with single-key operations.
 * This demo demonstrates how the WorkflowStateView component can be used to
 * visualize and transition between workflow states with keyboard shortcuts.
 */
public class WorkflowTransitionDemo {
    
    // Core components
    private static ServiceBridge serviceBridge;
    private static List<WorkItem> workItemList;
    private static Label statusLabel;
    private static WorkflowStateView workflowStateView;
    private static TextArea detailsArea;
    private static TextArea historyArea;
    
    // Work item data
    private static List<WorkItem> allWorkItems = new ArrayList<>();
    private static WorkItem selectedWorkItem;
    private static List<TransitionHistoryEntry> transitionHistory = new ArrayList<>();
    
    /**
     * Entry point for the demo.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            // Initialize the service bridge
            serviceBridge = ServiceBridge.getInstance();
            
            // Load work items
            loadWorkItems();
            
            // Create the UI
            RinnaPUI pui = RinnaPUI.getInstance();
            
            // Create the main container
            Container mainContainer = new Container("main");
            mainContainer.setPosition(new Point(0, 0));
            mainContainer.setSize(new Dimension(120, 35));
            
            // Set up the main layout
            BoxLayout mainLayout = new BoxLayout(Orientation.VERTICAL, 0);
            mainContainer.setLayout(mainLayout);
            
            // Create the header
            Container headerContainer = createHeader();
            
            // Create the body with work items and workflow visualization
            Container bodyContainer = createBody();
            
            // Create the footer
            Container footerContainer = createFooter();
            
            // Add components to the main container
            mainContainer.addComponent(headerContainer);
            mainContainer.addComponent(bodyContainer);
            mainContainer.addComponent(footerContainer);
            
            // Create a custom theme
            Theme theme = Theme.createDefault();
            
            // Initialize the UI
            pui.initialize(mainLayout)
               .addComponent(mainContainer)
               .setTheme(theme);
            
            // Select the first work item by default
            if (!allWorkItems.isEmpty()) {
                workItemList.setSelectedIndex(0);
                updateWorkflowView(allWorkItems.get(0));
            }
            
            // Start the UI
            pui.start();
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates the header container with title and controls.
     * 
     * @return the header container
     */
    private static Container createHeader() {
        Container header = new Container("header");
        header.setSize(new Dimension(120, 3));
        
        Style headerStyle = new Style()
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE)
            .setBold(true);
        header.setStyle(headerStyle);
        
        BoxLayout headerLayout = new BoxLayout(Orientation.HORIZONTAL, 2);
        header.setLayout(headerLayout);
        
        Label titleLabel = new Label("Rinna Workflow Transition Demo");
        titleLabel.setAlignment(Label.Alignment.CENTER);
        
        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(btn -> {
            loadWorkItems();
            if (!allWorkItems.isEmpty()) {
                workItemList.setSelectedIndex(0);
                updateWorkflowView(allWorkItems.get(0));
            }
            statusLabel.setText("Work items refreshed: " + allWorkItems.size() + " items loaded");
        });
        
        Button clearHistoryButton = new Button("Clear History");
        clearHistoryButton.addClickListener(btn -> {
            transitionHistory.clear();
            updateHistoryArea();
            statusLabel.setText("Transition history cleared");
        });
        
        // Add constraints to position the components
        BoxConstraints titleConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        headerLayout.setConstraints(titleLabel, titleConstraints);
        
        BoxConstraints buttonConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillHeight(true);
        headerLayout.setConstraints(refreshButton, buttonConstraints);
        headerLayout.setConstraints(clearHistoryButton, buttonConstraints);
        
        header.addComponent(titleLabel);
        header.addComponent(refreshButton);
        header.addComponent(clearHistoryButton);
        
        return header;
    }
    
    /**
     * Creates the body container with work items list and workflow visualization.
     * 
     * @return the body container
     */
    private static Container createBody() {
        Container body = new Container("body");
        body.setSize(new Dimension(120, 29));
        
        BoxLayout bodyLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        body.setLayout(bodyLayout);
        
        // Create the left panel with work items list
        Container leftPanel = createLeftPanel();
        
        // Create the right panel with workflow visualization and history
        Container rightPanel = createRightPanel();
        
        // Add components to body with constraints
        BoxConstraints leftConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        bodyLayout.setConstraints(leftPanel, leftConstraints);
        
        BoxConstraints rightConstraints = new BoxConstraints()
            .setWeight(2)
            .setFillHeight(true);
        bodyLayout.setConstraints(rightPanel, rightConstraints);
        
        body.addComponent(leftPanel);
        body.addComponent(rightPanel);
        
        return body;
    }
    
    /**
     * Creates the left panel with the work items list and details.
     * 
     * @return the left panel container
     */
    private static Container createLeftPanel() {
        Container leftPanel = new Container("left-panel");
        
        Style leftPanelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        leftPanel.setStyle(leftPanelStyle);
        
        BoxLayout leftPanelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        leftPanel.setLayout(leftPanelLayout);
        
        // Create a header for the work items list
        Label listHeader = new Label("Work Items");
        Style listHeaderStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        listHeader.setStyle(listHeaderStyle);
        
        // Create the work items list
        workItemList = new List<>("work-items-list", 10);
        workItemList.setItemRenderer(item -> {
            String id = item.getId().length() > 8 ? item.getId().substring(0, 8) : item.getId();
            String title = item.getTitle();
            String state = item.getState() != null ? item.getState().name() : "?";
            
            return String.format("%-8s %-12s %s", 
                id, 
                state,
                title.length() > 20 ? title.substring(0, 17) + "..." : title);
        });
        
        // Create a details view for the selected work item
        detailsArea = new TextArea("details-area", 10, 36);
        detailsArea.setReadOnly(true);
        
        Label detailsHeader = new Label("Work Item Details");
        Style detailsHeaderStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        detailsHeader.setStyle(detailsHeaderStyle);
        
        // Add components to the left panel
        leftPanel.addComponent(listHeader);
        leftPanel.addComponent(workItemList);
        leftPanel.addComponent(detailsHeader);
        leftPanel.addComponent(detailsArea);
        
        // Set up selection listener
        workItemList.addSelectionListener((list, selected) -> {
            if (selected != null) {
                updateWorkflowView(selected);
                updateDetailsArea(selected);
                selectedWorkItem = selected;
            }
        });
        
        return leftPanel;
    }
    
    /**
     * Creates the right panel with workflow visualization and transition history.
     * 
     * @return the right panel container
     */
    private static Container createRightPanel() {
        Container rightPanel = new Container("right-panel");
        
        BoxLayout rightPanelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        rightPanel.setLayout(rightPanelLayout);
        
        // Create workflow visualization panel
        Container workflowPanel = new Container("workflow-panel");
        Style workflowPanelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        workflowPanel.setStyle(workflowPanelStyle);
        
        BoxLayout workflowPanelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        workflowPanel.setLayout(workflowPanelLayout);
        
        Label workflowHeader = new Label("Workflow State Transitions");
        Style workflowHeaderStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        workflowHeader.setStyle(workflowHeaderStyle);
        
        // Create the workflow state view component
        workflowStateView = new WorkflowStateView("workflow-state-view", 78, 15);
        
        // Set the transition handler to update the work item state
        workflowStateView.setTransitionHandler(newState -> {
            if (selectedWorkItem != null) {
                try {
                    // Attempt to transition the work item
                    boolean success = serviceBridge.transitionWorkItem(selectedWorkItem.getId(), newState);
                    
                    if (success) {
                        // Update the work item state
                        WorkflowState oldState = selectedWorkItem.getState();
                        selectedWorkItem.setState(newState);
                        
                        // Record the transition in history
                        recordTransition(selectedWorkItem, oldState, newState);
                        
                        // Update the UI
                        updateWorkflowView(selectedWorkItem);
                        updateDetailsArea(selectedWorkItem);
                        workItemList.refreshItems();
                        
                        // Set success message
                        statusLabel.setText("Transitioned work item " + selectedWorkItem.getId() + 
                                           " from " + oldState + " to " + newState);
                    } else {
                        workflowStateView.setStatusMessage("Transition failed!");
                        statusLabel.setText("Failed to transition work item. Invalid transition or permission denied.");
                    }
                } catch (Exception e) {
                    workflowStateView.setStatusMessage("Error: " + e.getMessage());
                    statusLabel.setText("Error: " + e.getMessage());
                }
            }
        });
        
        // Create history panel
        Container historyPanel = new Container("history-panel");
        Style historyPanelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        historyPanel.setStyle(historyPanelStyle);
        
        BoxLayout historyPanelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        historyPanel.setLayout(historyPanelLayout);
        
        Label historyHeader = new Label("Transition History");
        Style historyHeaderStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        historyHeader.setStyle(historyHeaderStyle);
        
        historyArea = new TextArea("history-area", 10, 78);
        historyArea.setReadOnly(true);
        
        // Add components to panels
        workflowPanel.addComponent(workflowHeader);
        workflowPanel.addComponent(workflowStateView);
        
        historyPanel.addComponent(historyHeader);
        historyPanel.addComponent(historyArea);
        
        // Add panels to right panel with constraints
        BoxConstraints workflowConstraints = new BoxConstraints()
            .setWeight(2)
            .setFillHeight(true);
        rightPanelLayout.setConstraints(workflowPanel, workflowConstraints);
        
        BoxConstraints historyConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        rightPanelLayout.setConstraints(historyPanel, historyConstraints);
        
        rightPanel.addComponent(workflowPanel);
        rightPanel.addComponent(historyPanel);
        
        return rightPanel;
    }
    
    /**
     * Creates the footer container with status and help text.
     * 
     * @return the footer container
     */
    private static Container createFooter() {
        Container footer = new Container("footer");
        footer.setSize(new Dimension(120, 3));
        
        Style footerStyle = new Style()
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        footer.setStyle(footerStyle);
        
        BoxLayout footerLayout = new BoxLayout(Orientation.HORIZONTAL, 2);
        footer.setLayout(footerLayout);
        
        statusLabel = new Label("Loading work items...");
        Label helpLabel = new Label("ESC: Exit  TAB: Navigate  1-9: Transition  F5: Refresh");
        
        // Add constraints to position the components
        BoxConstraints statusConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        footerLayout.setConstraints(statusLabel, statusConstraints);
        
        BoxConstraints helpConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true)
            .setHorizontalAlignment(BoxLayout.HorizontalAlignment.RIGHT);
        footerLayout.setConstraints(helpLabel, helpConstraints);
        
        footer.addComponent(statusLabel);
        footer.addComponent(helpLabel);
        
        return footer;
    }
    
    /**
     * Loads work items from the service bridge.
     */
    private static void loadWorkItems() {
        try {
            allWorkItems = serviceBridge.getAllWorkItems();
            if (allWorkItems.isEmpty()) {
                // If no items are returned, create some sample items
                createSampleWorkItems();
            }
            
            // Update the list items
            workItemList.setItems(allWorkItems);
            
            statusLabel.setText("Loaded " + allWorkItems.size() + " work items");
        } catch (Exception e) {
            statusLabel.setText("Error loading work items: " + e.getMessage());
            // Create sample items on error
            createSampleWorkItems();
            workItemList.setItems(allWorkItems);
        }
    }
    
    /**
     * Updates the workflow state view with the selected work item.
     * 
     * @param item the selected work item
     */
    private static void updateWorkflowView(WorkItem item) {
        if (item == null) {
            return;
        }
        
        try {
            WorkflowState currentState = item.getState();
            
            // Get available transitions from the service bridge
            // In a real implementation, this would call the service bridge
            // For this demo, we simulate the available transitions based on current state
            List<WorkflowState> availableTransitions = getAvailableTransitions(item);
            
            // Update the workflow state view
            workflowStateView.setStateAndTransitions(currentState, availableTransitions);
            workflowStateView.setStatusMessage("Select a transition using numbers 1-" + 
                                               Math.min(availableTransitions.size(), 9));
            
        } catch (Exception e) {
            workflowStateView.setStatusMessage("Error: " + e.getMessage());
            statusLabel.setText("Error updating workflow view: " + e.getMessage());
        }
    }
    
    /**
     * Updates the details area with the selected work item details.
     * 
     * @param item the selected work item
     */
    private static void updateDetailsArea(WorkItem item) {
        if (item == null) {
            detailsArea.setText("");
            return;
        }
        
        StringBuilder details = new StringBuilder();
        
        details.append("ID: ").append(item.getId()).append("\n");
        details.append("Title: ").append(item.getTitle()).append("\n");
        details.append("Type: ").append(item.getType()).append("\n");
        details.append("Priority: ").append(item.getPriority()).append("\n");
        details.append("Current State: ").append(item.getState()).append("\n");
        details.append("Assignee: ").append(item.getAssignee() != null ? item.getAssignee() : "Unassigned").append("\n");
        
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            details.append("\nDescription:\n").append(item.getDescription());
        }
        
        detailsArea.setText(details.toString());
    }
    
    /**
     * Records a transition in the history.
     * 
     * @param item the work item
     * @param oldState the old state
     * @param newState the new state
     */
    private static void recordTransition(WorkItem item, WorkflowState oldState, WorkflowState newState) {
        TransitionHistoryEntry entry = new TransitionHistoryEntry(
            item.getId(),
            item.getTitle(),
            oldState,
            newState,
            System.getProperty("user.name"),
            LocalDateTime.now()
        );
        
        transitionHistory.add(0, entry); // Add to the beginning of the list
        updateHistoryArea();
    }
    
    /**
     * Updates the history area with the transition history.
     */
    private static void updateHistoryArea() {
        StringBuilder history = new StringBuilder();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (TransitionHistoryEntry entry : transitionHistory) {
            history.append(formatter.format(entry.timestamp)).append(" | ");
            history.append(entry.itemId).append(" | ");
            history.append(entry.oldState).append(" → ").append(entry.newState).append(" | ");
            history.append(entry.username).append("\n");
        }
        
        historyArea.setText(history.toString());
    }
    
    /**
     * Gets the available transitions for a work item.
     * 
     * @param item the work item
     * @return a list of available transitions
     */
    private static List<WorkflowState> getAvailableTransitions(WorkItem item) {
        if (item == null) {
            return new ArrayList<>();
        }
        
        // In a real implementation, this would call the service bridge
        // For this demo, we simulate the available transitions based on current state
        List<WorkflowState> availableTransitions = new ArrayList<>();
        
        switch (item.getState()) {
            case CREATED:
                availableTransitions.add(WorkflowState.READY);
                availableTransitions.add(WorkflowState.BLOCKED);
                break;
                
            case READY:
                availableTransitions.add(WorkflowState.IN_PROGRESS);
                availableTransitions.add(WorkflowState.BLOCKED);
                break;
                
            case IN_PROGRESS:
                availableTransitions.add(WorkflowState.READY);
                availableTransitions.add(WorkflowState.REVIEW);
                availableTransitions.add(WorkflowState.TESTING);
                availableTransitions.add(WorkflowState.BLOCKED);
                break;
                
            case REVIEW:
                availableTransitions.add(WorkflowState.IN_PROGRESS);
                availableTransitions.add(WorkflowState.TESTING);
                availableTransitions.add(WorkflowState.DONE);
                break;
                
            case TESTING:
                availableTransitions.add(WorkflowState.IN_PROGRESS);
                availableTransitions.add(WorkflowState.DONE);
                break;
                
            case BLOCKED:
                availableTransitions.add(WorkflowState.READY);
                break;
                
            case DONE:
                availableTransitions.add(WorkflowState.READY);
                break;
                
            // Bug workflow
            case FOUND:
                availableTransitions.add(WorkflowState.TRIAGED);
                break;
                
            case TRIAGED:
                availableTransitions.add(WorkflowState.TO_DO);
                break;
                
            case TO_DO:
                availableTransitions.add(WorkflowState.IN_PROGRESS);
                break;
                
            case IN_TEST:
                availableTransitions.add(WorkflowState.DONE);
                availableTransitions.add(WorkflowState.IN_PROGRESS);
                break;
                
            default:
                break;
        }
        
        return availableTransitions;
    }
    
    /**
     * Creates sample work items for the demo.
     */
    private static void createSampleWorkItems() {
        allWorkItems = new ArrayList<>();
        
        // Add sample work items
        allWorkItems.add(new WorkItem("WI-1001", "Implement login functionality", 
                WorkItemType.FEATURE, Priority.HIGH, WorkflowState.IN_PROGRESS)
                .setAssignee("john.doe")
                .setDescription("Add user authentication to the application with support for multiple providers."));
        
        allWorkItems.add(new WorkItem("WI-1002", "Fix sorting bug in reports", 
                WorkItemType.BUG, Priority.MEDIUM, WorkflowState.READY)
                .setAssignee("jane.smith")
                .setDescription("Reports are not sorting by date correctly when the user clicks on the date column header."));
        
        allWorkItems.add(new WorkItem("WI-1003", "Add export to CSV feature", 
                WorkItemType.FEATURE, Priority.LOW, WorkflowState.TESTING)
                .setAssignee("alex.dev")
                .setDescription("Allow exporting report data to CSV format for further analysis."));
        
        allWorkItems.add(new WorkItem("WI-1004", "Improve application performance", 
                WorkItemType.TASK, Priority.HIGH, WorkflowState.REVIEW)
                .setAssignee("jane.smith")
                .setDescription("Optimize database queries and caching for faster response time."));
        
        allWorkItems.add(new WorkItem("WI-1005", "Update user documentation", 
                WorkItemType.TASK, Priority.LOW, WorkflowState.DONE)
                .setAssignee("mark.writer")
                .setDescription("Add sections for new features and improve existing content in the user manual."));
        
        allWorkItems.add(new WorkItem("WI-1006", "Fix memory leak in background process", 
                WorkItemType.BUG, Priority.CRITICAL, WorkflowState.BLOCKED)
                .setAssignee("alex.dev")
                .setDescription("Application is leaking memory when running for extended periods, causing crashes."));
        
        allWorkItems.add(new WorkItem("WI-1007", "Implement email notifications", 
                WorkItemType.FEATURE, Priority.MEDIUM, WorkflowState.CREATED)
                .setDescription("Send notifications when work items are assigned or status changes."));
        
        allWorkItems.add(new WorkItem("WI-1008", "Fix critical security vulnerability", 
                WorkItemType.BUG, Priority.CRITICAL, WorkflowState.FOUND)
                .setAssignee("security.team")
                .setDescription("Address SQL injection vulnerability in the search functionality."));
        
        allWorkItems.add(new WorkItem("WI-1009", "Database index optimization", 
                WorkItemType.TASK, Priority.MEDIUM, WorkflowState.TRIAGED)
                .setAssignee("db.admin")
                .setDescription("Optimize database indexes to improve query performance for reports."));
        
        allWorkItems.add(new WorkItem("WI-1010", "Implement two-factor authentication", 
                WorkItemType.FEATURE, Priority.HIGH, WorkflowState.TO_DO)
                .setAssignee("security.team")
                .setDescription("Add support for two-factor authentication using authenticator apps."));
    }
    
    /**
     * Helper class to represent a work item with the required fields and builder-style setters.
     */
    private static class WorkItem {
        private String id;
        private String title;
        private WorkItemType type;
        private Priority priority;
        private WorkflowState state;
        private String assignee;
        private String description;
        
        public WorkItem(String id, String title, WorkItemType type, Priority priority, WorkflowState state) {
            this.id = id;
            this.title = title;
            this.type = type;
            this.priority = priority;
            this.state = state;
        }
        
        public String getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public WorkItemType getType() {
            return type;
        }
        
        public Priority getPriority() {
            return priority;
        }
        
        public WorkflowState getState() {
            return state;
        }
        
        public void setState(WorkflowState state) {
            this.state = state;
        }
        
        public String getAssignee() {
            return assignee;
        }
        
        public WorkItem setAssignee(String assignee) {
            this.assignee = assignee;
            return this;
        }
        
        public String getDescription() {
            return description;
        }
        
        public WorkItem setDescription(String description) {
            this.description = description;
            return this;
        }
    }
    
    /**
     * Helper class to represent a transition history entry.
     */
    private static class TransitionHistoryEntry {
        private final String itemId;
        private final String itemTitle;
        private final WorkflowState oldState;
        private final WorkflowState newState;
        private final String username;
        private final LocalDateTime timestamp;
        
        public TransitionHistoryEntry(String itemId, String itemTitle, WorkflowState oldState, 
                                     WorkflowState newState, String username, LocalDateTime timestamp) {
            this.itemId = itemId;
            this.itemTitle = itemTitle;
            this.oldState = oldState;
            this.newState = newState;
            this.username = username;
            this.timestamp = timestamp;
        }
    }
}