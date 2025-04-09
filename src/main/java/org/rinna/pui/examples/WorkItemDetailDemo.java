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
import org.rinna.pui.component.BoxLayout.VerticalAlignment;
import org.rinna.pui.component.BoxLayout.HorizontalAlignment;
import org.rinna.pui.component.Button.ClickListener;
import org.rinna.pui.component.List;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;
import org.rinna.pui.examples.model.WorkItemRelationship;
import org.rinna.pui.examples.model.WorkItemRelationship.RelationshipType;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkflowState;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Demo that showcases an interactive work item detail view with Miller columns navigation.
 * This demo demonstrates how to use the MillerColumnsContainer for navigating
 * hierarchical relationships between work items.
 */
public class WorkItemDetailDemo {
    
    // Core components
    private static ServiceBridge serviceBridge;
    private static Label statusLabel;
    private static MillerColumnsContainer<Object> millerColumns;
    
    // Data
    private static List<WorkItem> allWorkItems = new ArrayList<>();
    private static Map<String, List<WorkItemRelationship>> relationshipsMap = new HashMap<>();
    
    // For grouping in first column
    private static final String GROUP_ALL = "All Items";
    private static final String GROUP_PRIORITY = "By Priority";
    private static final String GROUP_TYPE = "By Type";
    private static final String GROUP_STATUS = "By Status";
    
    /**
     * Entry point for the demo.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            // Initialize the service bridge
            serviceBridge = ServiceBridge.getInstance();
            
            // Load work items and create sample relationships
            loadWorkItems();
            createSampleRelationships();
            
            // Create the UI
            RinnaPUI pui = RinnaPUI.getInstance();
            
            // Create the main container
            Container mainContainer = new Container("main");
            mainContainer.setPosition(new Point(0, 0));
            mainContainer.setSize(new Dimension(120, 30));
            
            // Set up the main layout
            BoxLayout mainLayout = new BoxLayout(Orientation.VERTICAL, 0);
            mainContainer.setLayout(mainLayout);
            
            // Create the header
            Container headerContainer = createHeader();
            
            // Create the body with Miller columns
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
               .setTheme(theme)
               .start();
            
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
        
        Label titleLabel = new Label("Rinna Work Item Detail View");
        
        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(btn -> {
            loadWorkItems();
            createSampleRelationships();
            initializeMillerColumns();
            statusLabel.setText("Work items refreshed");
        });
        
        Button helpButton = new Button("Help");
        helpButton.addClickListener(btn -> {
            statusLabel.setText("Miller columns: Use TAB to navigate between columns, arrow keys to select items");
        });
        
        // Add constraints to position the components
        BoxConstraints titleConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true)
            .setVerticalAlignment(VerticalAlignment.CENTER);
        headerLayout.setConstraints(titleLabel, titleConstraints);
        
        BoxConstraints buttonConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillHeight(true)
            .setVerticalAlignment(VerticalAlignment.CENTER);
        headerLayout.setConstraints(refreshButton, buttonConstraints);
        headerLayout.setConstraints(helpButton, buttonConstraints);
        
        header.addComponent(titleLabel);
        header.addComponent(refreshButton);
        header.addComponent(helpButton);
        
        return header;
    }
    
    /**
     * Creates the body container with Miller columns for work item navigation.
     * 
     * @return the body container
     */
    private static Container createBody() {
        Container body = new Container("body");
        body.setSize(new Dimension(120, 24));
        
        Style bodyStyle = new Style();
        body.setStyle(bodyStyle);
        
        BoxLayout bodyLayout = new BoxLayout(Orientation.VERTICAL, 1);
        body.setLayout(bodyLayout);
        
        // Create Miller columns container with 4 columns
        millerColumns = new MillerColumnsContainer<>("miller-columns", 4, 30, 22);
        
        // Set up the Miller columns with renderers and providers
        setupMillerColumns();
        
        // Initialize the Miller columns with root items
        initializeMillerColumns();
        
        // Add Miller columns to body with constraints
        BoxConstraints millerConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillWidth(true)
            .setFillHeight(true);
        bodyLayout.setConstraints(millerColumns, millerConstraints);
        
        body.addComponent(millerColumns);
        
        return body;
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
        
        statusLabel = new Label("Ready");
        Label helpLabel = new Label("ESC: Exit  TAB: Navigate columns  ↑↓: Select items");
        
        // Add constraints to position the components
        BoxConstraints statusConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true)
            .setVerticalAlignment(VerticalAlignment.CENTER);
        footerLayout.setConstraints(statusLabel, statusConstraints);
        
        BoxConstraints helpConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true)
            .setVerticalAlignment(VerticalAlignment.CENTER)
            .setHorizontalAlignment(HorizontalAlignment.RIGHT);
        footerLayout.setConstraints(helpLabel, helpConstraints);
        
        footer.addComponent(statusLabel);
        footer.addComponent(helpLabel);
        
        return footer;
    }
    
    /**
     * Sets up the Miller columns with renderers and providers.
     */
    private static void setupMillerColumns() {
        // Set item renderer for displaying items in the lists
        millerColumns.setItemRenderer(item -> {
            if (item instanceof String) {
                return (String) item; // Group names
            } else if (item instanceof WorkItem) {
                WorkItem workItem = (WorkItem) item;
                String type = workItem.getType() != null ? workItem.getType().name() : "?";
                String priority = workItem.getPriority() != null ? workItem.getPriority().name() : "?";
                String status = workItem.getStatus() != null ? workItem.getStatus().name() : "?";
                return String.format("%s [%s] %s", workItem.getId(), type, workItem.getTitle());
            } else if (item instanceof Priority) {
                return ((Priority) item).name();
            } else if (item instanceof WorkItemType) {
                return ((WorkItemType) item).name();
            } else if (item instanceof WorkflowState) {
                return ((WorkflowState) item).name();
            } else if (item instanceof WorkItemRelationship) {
                WorkItemRelationship relationship = (WorkItemRelationship) item;
                return relationship.toString();
            }
            return item.toString();
        });
        
        // Set children provider for getting child items
        millerColumns.setChildrenProvider(parent -> {
            if (parent.equals(GROUP_ALL)) {
                return new ArrayList<>(allWorkItems);
            } else if (parent.equals(GROUP_PRIORITY)) {
                return new ArrayList<>(Priority.values());
            } else if (parent.equals(GROUP_TYPE)) {
                return new ArrayList<>(WorkItemType.values());
            } else if (parent.equals(GROUP_STATUS)) {
                return new ArrayList<>(WorkflowState.values());
            } else if (parent instanceof Priority) {
                Priority priority = (Priority) parent;
                return allWorkItems.stream()
                    .filter(item -> item.getPriority() == priority)
                    .collect(Collectors.toList());
            } else if (parent instanceof WorkItemType) {
                WorkItemType type = (WorkItemType) parent;
                return allWorkItems.stream()
                    .filter(item -> item.getType() == type)
                    .collect(Collectors.toList());
            } else if (parent instanceof WorkflowState) {
                WorkflowState state = (WorkflowState) parent;
                return allWorkItems.stream()
                    .filter(item -> item.getStatus() == state)
                    .collect(Collectors.toList());
            } else if (parent instanceof WorkItem) {
                WorkItem workItem = (WorkItem) parent;
                List<WorkItemRelationship> relationships = relationshipsMap.get(workItem.getId());
                if (relationships != null) {
                    return new ArrayList<>(relationships);
                }
            } else if (parent instanceof WorkItemRelationship) {
                WorkItemRelationship relationship = (WorkItemRelationship) parent;
                WorkItem targetItem = relationship.getTargetItem();
                List<WorkItemRelationship> relationships = relationshipsMap.get(targetItem.getId());
                if (relationships != null) {
                    return new ArrayList<>(relationships);
                }
            }
            return new ArrayList<>();
        });
        
        // Set detail provider for showing item details
        millerColumns.setDetailProvider(item -> {
            if (item instanceof WorkItem) {
                WorkItem workItem = (WorkItem) item;
                StringBuilder details = new StringBuilder();
                
                details.append("ID: ").append(workItem.getId()).append("\n\n");
                details.append("Title: ").append(workItem.getTitle()).append("\n\n");
                
                details.append("Type: ").append(workItem.getType()).append("\n");
                details.append("Priority: ").append(workItem.getPriority()).append("\n");
                details.append("Status: ").append(workItem.getStatus()).append("\n\n");
                
                details.append("Assignee: ").append(workItem.getAssignee() != null ? workItem.getAssignee() : "Unassigned").append("\n");
                details.append("Reporter: ").append(workItem.getReporter() != null ? workItem.getReporter() : "Unknown").append("\n\n");
                
                if (workItem.getCreated() != null) {
                    details.append("Created: ").append(workItem.getCreated()).append("\n");
                }
                
                if (workItem.getUpdated() != null) {
                    details.append("Updated: ").append(workItem.getUpdated()).append("\n");
                }
                
                if (workItem.getDueDate() != null) {
                    details.append("Due: ").append(workItem.getDueDate()).append("\n");
                }
                
                details.append("\nDescription: ").append(workItem.getDescription() != null ? workItem.getDescription() : "No description provided.");
                
                // Add relationship count
                List<WorkItemRelationship> relationships = relationshipsMap.get(workItem.getId());
                if (relationships != null && !relationships.isEmpty()) {
                    details.append("\n\nRelationships: ").append(relationships.size());
                }
                
                return details.toString();
            } else if (item instanceof WorkItemRelationship) {
                WorkItemRelationship relationship = (WorkItemRelationship) item;
                WorkItem sourceItem = relationship.getSourceItem();
                WorkItem targetItem = relationship.getTargetItem();
                
                StringBuilder details = new StringBuilder();
                details.append("Relationship: ").append(relationship.getRelationshipType()).append("\n\n");
                details.append("From: ").append(sourceItem.getId()).append(" - ").append(sourceItem.getTitle()).append("\n");
                details.append("To: ").append(targetItem.getId()).append(" - ").append(targetItem.getTitle()).append("\n\n");
                
                if (relationship.getDescription() != null) {
                    details.append("Description: ").append(relationship.getDescription());
                }
                
                return details.toString();
            } else if (item instanceof Priority) {
                Priority priority = (Priority) item;
                long count = allWorkItems.stream()
                    .filter(i -> i.getPriority() == priority)
                    .count();
                
                StringBuilder details = new StringBuilder();
                details.append("Priority: ").append(priority).append("\n\n");
                details.append("Total Work Items: ").append(count).append("\n\n");
                
                // Add distribution information
                details.append("Status Distribution:\n");
                Map<WorkflowState, Long> statusCounts = allWorkItems.stream()
                    .filter(i -> i.getPriority() == priority)
                    .collect(Collectors.groupingBy(WorkItem::getStatus, Collectors.counting()));
                
                for (WorkflowState state : WorkflowState.values()) {
                    long stateCount = statusCounts.getOrDefault(state, 0L);
                    if (stateCount > 0) {
                        details.append("- ").append(state).append(": ").append(stateCount).append("\n");
                    }
                }
                
                return details.toString();
            } else if (item instanceof WorkItemType) {
                WorkItemType type = (WorkItemType) item;
                long count = allWorkItems.stream()
                    .filter(i -> i.getType() == type)
                    .count();
                
                StringBuilder details = new StringBuilder();
                details.append("Type: ").append(type).append("\n\n");
                details.append("Total Work Items: ").append(count).append("\n\n");
                
                // Add distribution information
                details.append("Priority Distribution:\n");
                Map<Priority, Long> priorityCounts = allWorkItems.stream()
                    .filter(i -> i.getType() == type)
                    .collect(Collectors.groupingBy(WorkItem::getPriority, Collectors.counting()));
                
                for (Priority priority : Priority.values()) {
                    long priorityCount = priorityCounts.getOrDefault(priority, 0L);
                    if (priorityCount > 0) {
                        details.append("- ").append(priority).append(": ").append(priorityCount).append("\n");
                    }
                }
                
                return details.toString();
            } else if (item instanceof WorkflowState) {
                WorkflowState state = (WorkflowState) item;
                long count = allWorkItems.stream()
                    .filter(i -> i.getStatus() == state)
                    .count();
                
                StringBuilder details = new StringBuilder();
                details.append("Status: ").append(state).append("\n\n");
                details.append("Total Work Items: ").append(count).append("\n\n");
                
                // Add distribution information
                details.append("Type Distribution:\n");
                Map<WorkItemType, Long> typeCounts = allWorkItems.stream()
                    .filter(i -> i.getStatus() == state)
                    .collect(Collectors.groupingBy(WorkItem::getType, Collectors.counting()));
                
                for (WorkItemType type : WorkItemType.values()) {
                    long typeCount = typeCounts.getOrDefault(type, 0L);
                    if (typeCount > 0) {
                        details.append("- ").append(type).append(": ").append(typeCount).append("\n");
                    }
                }
                
                return details.toString();
            }
            
            return item.toString();
        });
    }
    
    /**
     * Initializes the Miller columns with root items.
     */
    private static void initializeMillerColumns() {
        // Create root items (view options)
        List<Object> rootItems = new ArrayList<>();
        rootItems.add(GROUP_ALL);
        rootItems.add(GROUP_PRIORITY);
        rootItems.add(GROUP_TYPE);
        rootItems.add(GROUP_STATUS);
        
        // Set root items to Miller columns
        millerColumns.setRootItems(rootItems);
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
        } catch (Exception e) {
            statusLabel.setText("Error loading work items: " + e.getMessage());
            // Create sample items on error
            createSampleWorkItems();
        }
    }
    
    /**
     * Creates sample relationships between work items.
     */
    private static void createSampleRelationships() {
        relationshipsMap.clear();
        
        // Ensure we have at least 2 work items
        if (allWorkItems.size() < 2) {
            return;
        }
        
        // Create parent-child relationships
        for (int i = 0; i < allWorkItems.size(); i++) {
            WorkItem item = allWorkItems.get(i);
            List<WorkItemRelationship> relationships = new ArrayList<>();
            
            // Create a parent relationship for Epic work items
            if (item.getType() == WorkItemType.EPIC) {
                // Find stories that can be children of this epic
                for (WorkItem child : allWorkItems) {
                    if (child.getType() == WorkItemType.STORY && !child.getId().equals(item.getId())) {
                        WorkItemRelationship relationship = new WorkItemRelationship(
                            item, child, RelationshipType.CHILD, 
                            "Story implementing Epic functionality"
                        );
                        relationships.add(relationship);
                        
                        // Add complementary relationship to the child
                        addRelationship(child.getId(), relationship.createComplementaryRelationship());
                    }
                }
            }
            
            // Create blocking relationships for bugs
            if (item.getType() == WorkItemType.BUG && item.getPriority() == Priority.CRITICAL) {
                // Find features that are blocked by this critical bug
                for (WorkItem blocked : allWorkItems) {
                    if (blocked.getType() == WorkItemType.FEATURE && 
                        blocked.getStatus() == WorkflowState.READY && 
                        !blocked.getId().equals(item.getId())) {
                        
                        WorkItemRelationship relationship = new WorkItemRelationship(
                            item, blocked, RelationshipType.BLOCKS, 
                            "Critical bug blocking feature implementation"
                        );
                        relationships.add(relationship);
                        
                        // Add complementary relationship to the blocked item
                        addRelationship(blocked.getId(), relationship.createComplementaryRelationship());
                        
                        // Only block one feature per critical bug to avoid too many relationships
                        break;
                    }
                }
            }
            
            // Create related-to relationships
            for (WorkItem related : allWorkItems) {
                if (!related.getId().equals(item.getId()) && 
                    related.getType() == item.getType() && 
                    relationships.size() < 5) { // Limit to 5 relationships per item
                    
                    // Add a related relationship with 20% probability
                    if (Math.random() < 0.2) {
                        WorkItemRelationship relationship = new WorkItemRelationship(
                            item, related, RelationshipType.RELATED, 
                            "Related work items with similar functionality"
                        );
                        relationships.add(relationship);
                        
                        // Add complementary relationship to the related item
                        addRelationship(related.getId(), relationship.createComplementaryRelationship());
                    }
                }
            }
            
            // Add the relationships to the map
            if (!relationships.isEmpty()) {
                relationshipsMap.put(item.getId(), relationships);
            }
        }
    }
    
    /**
     * Adds a relationship to the relationships map.
     * 
     * @param itemId The item ID
     * @param relationship The relationship to add
     */
    private static void addRelationship(String itemId, WorkItemRelationship relationship) {
        List<WorkItemRelationship> relationships = relationshipsMap.computeIfAbsent(itemId, k -> new ArrayList<>());
        relationships.add(relationship);
    }
    
    /**
     * Creates sample work items for the demo.
     */
    private static void createSampleWorkItems() {
        allWorkItems = new ArrayList<>();
        
        // Create sample work items
        WorkItem item1 = new WorkItem("WI-1001", "Implement login functionality", 
                WorkItemType.FEATURE, Priority.HIGH, WorkflowState.IN_PROGRESS);
        item1.setAssignee("john.doe");
        item1.setReporter("sarah.manager");
        item1.setDescription("Add user authentication to the application with support for multiple authentication providers including local database and OAuth.");
        item1.setCreated(LocalDateTime.now().minusDays(10));
        item1.setUpdated(LocalDateTime.now().minusDays(2));
        allWorkItems.add(item1);
        
        WorkItem item2 = new WorkItem("WI-1002", "Fix sorting bug in reports", 
                WorkItemType.BUG, Priority.MEDIUM, WorkflowState.READY);
        item2.setAssignee("jane.smith");
        item2.setReporter("john.qa");
        item2.setDescription("Reports are not sorting by date correctly when the user clicks on the date column header.");
        item2.setCreated(LocalDateTime.now().minusDays(7));
        item2.setUpdated(LocalDateTime.now().minusDays(7));
        allWorkItems.add(item2);
        
        WorkItem item3 = new WorkItem("WI-1003", "Add export to CSV feature", 
                WorkItemType.FEATURE, Priority.LOW, WorkflowState.TESTING);
        item3.setAssignee("alex.dev");
        item3.setReporter("sarah.manager");
        item3.setDescription("Allow exporting report data to CSV format for further analysis in spreadsheet applications.");
        item3.setCreated(LocalDateTime.now().minusDays(14));
        item3.setUpdated(LocalDateTime.now().minusDays(1));
        allWorkItems.add(item3);
        
        WorkItem item4 = new WorkItem("WI-1004", "Improve application performance", 
                WorkItemType.TASK, Priority.HIGH, WorkflowState.IN_PROGRESS);
        item4.setAssignee("jane.smith");
        item4.setReporter("sarah.manager");
        item4.setDescription("Optimize database queries and caching for faster response time, especially for report generation.");
        item4.setCreated(LocalDateTime.now().minusDays(5));
        item4.setUpdated(LocalDateTime.now().minusDays(1));
        allWorkItems.add(item4);
        
        WorkItem item5 = new WorkItem("WI-1005", "Update user documentation", 
                WorkItemType.TASK, Priority.LOW, WorkflowState.DONE);
        item5.setAssignee("mark.writer");
        item5.setReporter("john.qa");
        item5.setDescription("Add sections for new features and improve existing content in the user manual.");
        item5.setCreated(LocalDateTime.now().minusDays(20));
        item5.setUpdated(LocalDateTime.now().minusDays(3));
        allWorkItems.add(item5);
        
        WorkItem item6 = new WorkItem("WI-1006", "Fix memory leak in background process", 
                WorkItemType.BUG, Priority.CRITICAL, WorkflowState.READY);
        item6.setAssignee("alex.dev");
        item6.setReporter("jane.smith");
        item6.setDescription("Application is leaking memory when running for extended periods, causing crashes after about 48 hours of operation.");
        item6.setCreated(LocalDateTime.now().minusDays(2));
        item6.setUpdated(LocalDateTime.now().minusDays(1));
        allWorkItems.add(item6);
        
        WorkItem item7 = new WorkItem("WI-1007", "Implement email notifications", 
                WorkItemType.FEATURE, Priority.MEDIUM, WorkflowState.CREATED);
        item7.setAssignee(null); // Unassigned
        item7.setReporter("sarah.manager");
        item7.setDescription("Send notifications when work items are assigned or status changes to keep team members informed.");
        item7.setCreated(LocalDateTime.now().minusDays(1));
        item7.setUpdated(LocalDateTime.now().minusDays(1));
        allWorkItems.add(item7);
        
        WorkItem item8 = new WorkItem("WI-1008", "Update third-party libraries", 
                WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
        item8.setAssignee("john.doe");
        item8.setReporter("john.doe");
        item8.setDescription("Update all dependencies to latest versions to address security vulnerabilities and improve stability.");
        item8.setCreated(LocalDateTime.now().minusDays(8));
        item8.setUpdated(LocalDateTime.now().minusDays(2));
        allWorkItems.add(item8);
        
        WorkItem item9 = new WorkItem("WI-1009", "Add dark mode support", 
                WorkItemType.FEATURE, Priority.LOW, WorkflowState.REVIEW);
        item9.setAssignee("jane.smith");
        item9.setReporter("mark.writer");
        item9.setDescription("Implement a dark theme for the application to reduce eye strain and save battery on OLED displays.");
        item9.setCreated(LocalDateTime.now().minusDays(15));
        item9.setUpdated(LocalDateTime.now().minusDays(1));
        allWorkItems.add(item9);
        
        WorkItem item10 = new WorkItem("WI-1010", "Implement search functionality", 
                WorkItemType.FEATURE, Priority.HIGH, WorkflowState.TESTING);
        item10.setAssignee("alex.dev");
        item10.setReporter("sarah.manager");
        item10.setDescription("Add ability to search for work items by various criteria including title, description, assignee, and status.");
        item10.setCreated(LocalDateTime.now().minusDays(12));
        item10.setUpdated(LocalDateTime.now().minusDays(1));
        allWorkItems.add(item10);
        
        // Add a spike for research
        WorkItem item11 = new WorkItem("WI-1011", "Research AI integration options", 
                WorkItemType.SPIKE, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
        item11.setAssignee("john.doe");
        item11.setReporter("sarah.manager");
        item11.setDescription("Investigate options for integrating AI-based recommendations into the workflow system. Time-boxed to 2 days.");
        item11.setCreated(LocalDateTime.now().minusDays(3));
        item11.setUpdated(LocalDateTime.now().minusDays(1));
        allWorkItems.add(item11);
        
        // Add an epic
        WorkItem item12 = new WorkItem("WI-1012", "Mobile App Development", 
                WorkItemType.EPIC, Priority.HIGH, WorkflowState.IN_PROGRESS);
        item12.setAssignee("sarah.manager");
        item12.setReporter("sarah.manager");
        item12.setDescription("Develop a mobile application version of the system for iOS and Android platforms. This is a major initiative that will span multiple sprints.");
        item12.setCreated(LocalDateTime.now().minusDays(30));
        item12.setUpdated(LocalDateTime.now().minusDays(5));
        allWorkItems.add(item12);
        
        // Add a story
        WorkItem item13 = new WorkItem("WI-1013", "User can reset their password", 
                WorkItemType.STORY, Priority.MEDIUM, WorkflowState.READY);
        item13.setAssignee("jane.smith");
        item13.setReporter("mark.writer");
        item13.setDescription("As a user, I want to be able to reset my password so that I can regain access to my account if I forget my credentials.");
        item13.setCreated(LocalDateTime.now().minusDays(10));
        item13.setUpdated(LocalDateTime.now().minusDays(7));
        allWorkItems.add(item13);
        
        // Add more bugs
        WorkItem item14 = new WorkItem("WI-1014", "Date format inconsistent across application", 
                WorkItemType.BUG, Priority.LOW, WorkflowState.FOUND);
        item14.setAssignee(null);
        item14.setReporter("john.qa");
        item14.setDescription("Date formats vary between MM/DD/YYYY and DD/MM/YYYY in different sections of the application.");
        item14.setCreated(LocalDateTime.now().minusDays(5));
        item14.setUpdated(LocalDateTime.now().minusDays(5));
        allWorkItems.add(item14);
        
        WorkItem item15 = new WorkItem("WI-1015", "Application crashes when handling large CSV files", 
                WorkItemType.BUG, Priority.HIGH, WorkflowState.TRIAGED);
        item15.setAssignee("alex.dev");
        item15.setReporter("jane.smith");
        item15.setDescription("When importing CSV files larger than 10MB, the application crashes with an out of memory error.");
        item15.setCreated(LocalDateTime.now().minusDays(4));
        item15.setUpdated(LocalDateTime.now().minusDays(1));
        allWorkItems.add(item15);
    }
}