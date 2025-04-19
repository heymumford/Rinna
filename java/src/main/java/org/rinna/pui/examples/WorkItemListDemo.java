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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.pui.RinnaPUI;
import org.rinna.pui.cli.ServiceBridge;
import org.rinna.pui.component.*;
import org.rinna.pui.component.BoxLayout.BoxConstraints;
import org.rinna.pui.component.BoxLayout.HorizontalAlignment;
import org.rinna.pui.component.BoxLayout.Orientation;
import org.rinna.pui.component.BoxLayout.VerticalAlignment;
import org.rinna.pui.component.List;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

/**
 * Demo that showcases a work item list with advanced filtering and sorting capabilities.
 * This demo demonstrates the integration of PUI components with the ServiceBridge
 * to display and interact with work items from the CLI services.
 */
public class WorkItemListDemo {
    
    // Core components
    private static ServiceBridge serviceBridge;
    private static List<WorkItem> workItemList;
    private static Label statusLabel;
    private static Label detailsLabel;
    private static Container filtersContainer;
    
    // Filter components
    private static TextBox searchBox;
    private static List<String> typeFilter;
    private static List<String> priorityFilter;
    private static List<String> statusFilter;
    private static List<String> sortByOptions;
    
    // Filter state
    private static String searchTerm = "";
    private static WorkItemType selectedType = null;
    private static Priority selectedPriority = null;
    private static WorkflowState selectedStatus = null;
    private static String sortBy = "ID";
    private static boolean sortAscending = true;
    
    // Data
    private static java.util.List<WorkItem> allWorkItems = new ArrayList<>();
    
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
            mainContainer.setSize(new Dimension(100, 30));
            
            // Set up the main layout
            BoxLayout mainLayout = new BoxLayout(Orientation.VERTICAL, 0);
            mainContainer.setLayout(mainLayout);
            
            // Create the header
            Container headerContainer = createHeader();
            
            // Create the body with filters and work item list
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
            
            // Apply initial filters
            applyFilters();
            
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
        header.setSize(new Dimension(100, 3));
        
        Style headerStyle = new Style()
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE)
            .setBold(true);
        header.setStyle(headerStyle);
        
        BoxLayout headerLayout = new BoxLayout(Orientation.HORIZONTAL, 2);
        header.setLayout(headerLayout);
        
        Label titleLabel = new Label("Rinna Work Item List");
        
        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(btn -> {
            loadWorkItems();
            applyFilters();
            statusLabel.setText("Work items refreshed: " + allWorkItems.size() + " items loaded");
        });
        
        Button clearFiltersButton = new Button("Clear Filters");
        clearFiltersButton.addClickListener(btn -> {
            clearFilters();
            applyFilters();
            statusLabel.setText("Filters cleared");
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
        headerLayout.setConstraints(clearFiltersButton, buttonConstraints);
        
        header.addComponent(titleLabel);
        header.addComponent(refreshButton);
        header.addComponent(clearFiltersButton);
        
        return header;
    }
    
    /**
     * Creates the body container with filters and work item list.
     * 
     * @return the body container
     */
    private static Container createBody() {
        Container body = new Container("body");
        body.setSize(new Dimension(100, 24));
        
        Style bodyStyle = new Style();
        body.setStyle(bodyStyle);
        
        BoxLayout bodyLayout = new BoxLayout(Orientation.VERTICAL, 1);
        body.setLayout(bodyLayout);
        
        // Create the filters panel
        filtersContainer = createFiltersPanel();
        
        // Create the list panel
        Container listContainer = createListPanel();
        
        // Add components to body with constraints
        BoxConstraints filtersConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillWidth(true);
        bodyLayout.setConstraints(filtersContainer, filtersConstraints);
        
        BoxConstraints listConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillWidth(true)
            .setFillHeight(true);
        bodyLayout.setConstraints(listContainer, listConstraints);
        
        body.addComponent(filtersContainer);
        body.addComponent(listContainer);
        
        return body;
    }
    
    /**
     * Creates the filters panel with search, type, priority, and status filters.
     * 
     * @return the filters container
     */
    private static Container createFiltersPanel() {
        Container filters = new Container("filters-panel");
        filters.setSize(new Dimension(100, 6));
        
        Style filtersStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        filters.setStyle(filtersStyle);
        
        // Create a 2-row layout for the filters
        BoxLayout filtersLayout = new BoxLayout(Orientation.VERTICAL, 1);
        filters.setLayout(filtersLayout);
        
        // First row container
        Container row1 = new Container("filters-row1");
        BoxLayout row1Layout = new BoxLayout(Orientation.HORIZONTAL, 1);
        row1.setLayout(row1Layout);
        
        // Second row container
        Container row2 = new Container("filters-row2");
        BoxLayout row2Layout = new BoxLayout(Orientation.HORIZONTAL, 1);
        row2.setLayout(row2Layout);
        
        // Create search box
        Label searchLabel = new Label("Search: ");
        searchBox = new TextBox(20);
        searchBox.addTextChangedListener(text -> {
            searchTerm = text;
            applyFilters();
        });
        
        // Create type filter
        Label typeLabel = new Label("Type: ");
        typeFilter = new List<>("type-filter", 1);
        java.util.List<String> types = new ArrayList<>();
        types.add("All");
        for (WorkItemType type : WorkItemType.values()) {
            types.add(type.name());
        }
        typeFilter.setItems(types);
        typeFilter.setSelectedIndex(0);
        typeFilter.addSelectionListener((list, selected) -> {
            if (selected.equals("All")) {
                selectedType = null;
            } else {
                selectedType = WorkItemType.fromString(selected);
            }
            applyFilters();
        });
        
        // Create priority filter
        Label priorityLabel = new Label("Priority: ");
        priorityFilter = new List<>("priority-filter", 1);
        java.util.List<String> priorities = new ArrayList<>();
        priorities.add("All");
        for (Priority priority : Priority.values()) {
            priorities.add(priority.name());
        }
        priorityFilter.setItems(priorities);
        priorityFilter.setSelectedIndex(0);
        priorityFilter.addSelectionListener((list, selected) -> {
            if (selected.equals("All")) {
                selectedPriority = null;
            } else {
                selectedPriority = Priority.fromString(selected);
            }
            applyFilters();
        });
        
        // Create status filter
        Label statusLabel = new Label("Status: ");
        statusFilter = new List<>("status-filter", 1);
        java.util.List<String> statuses = new ArrayList<>();
        statuses.add("All");
        for (WorkflowState state : WorkflowState.values()) {
            statuses.add(state.name());
        }
        statusFilter.setItems(statuses);
        statusFilter.setSelectedIndex(0);
        statusFilter.addSelectionListener((list, selected) -> {
            if (selected.equals("All")) {
                selectedStatus = null;
            } else {
                selectedStatus = WorkflowState.fromString(selected);
            }
            applyFilters();
        });
        
        // Create sort options
        Label sortByLabel = new Label("Sort By: ");
        sortByOptions = new List<>("sort-by", 1);
        java.util.List<String> sortOptions = new ArrayList<>();
        sortOptions.add("ID");
        sortOptions.add("Title");
        sortOptions.add("Type");
        sortOptions.add("Priority");
        sortOptions.add("Status");
        sortOptions.add("Created");
        sortOptions.add("Updated");
        sortByOptions.setItems(sortOptions);
        sortByOptions.setSelectedIndex(0);
        sortByOptions.addSelectionListener((list, selected) -> {
            sortBy = selected;
            applyFilters();
        });
        
        // Create sort direction toggle
        Button sortDirButton = new Button("↑ Ascending");
        sortDirButton.addClickListener(btn -> {
            sortAscending = !sortAscending;
            sortDirButton.setText(sortAscending ? "↑ Ascending" : "↓ Descending");
            applyFilters();
        });
        
        // Add components to rows
        row1.addComponent(searchLabel);
        row1.addComponent(searchBox);
        row1.addComponent(typeLabel);
        row1.addComponent(typeFilter);
        row1.addComponent(priorityLabel);
        row1.addComponent(priorityFilter);
        
        row2.addComponent(statusLabel);
        row2.addComponent(statusFilter);
        row2.addComponent(sortByLabel);
        row2.addComponent(sortByOptions);
        row2.addComponent(sortDirButton);
        
        // Add rows to filters container
        filters.addComponent(row1);
        filters.addComponent(row2);
        
        return filters;
    }
    
    /**
     * Creates the list panel with work items list and details view.
     * 
     * @return the list container
     */
    private static Container createListPanel() {
        Container listPanel = new Container("list-panel");
        listPanel.setSize(new Dimension(100, 17));
        
        BoxLayout listPanelLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        listPanel.setLayout(listPanelLayout);
        
        // Create the work items list panel
        Container itemsContainer = new Container("items-container");
        itemsContainer.setSize(new Dimension(50, 17));
        
        Style itemsContainerStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        itemsContainer.setStyle(itemsContainerStyle);
        
        BoxLayout itemsContainerLayout = new BoxLayout(Orientation.VERTICAL, 0);
        itemsContainer.setLayout(itemsContainerLayout);
        
        // Create the work items list
        workItemList = new List<>("work-items-list", 15);
        workItemList.setItemRenderer(item -> {
            String id = item.getId();
            String title = item.getTitle();
            String type = item.getType() != null ? item.getType().name() : "?";
            String priority = item.getPriority() != null ? item.getPriority().name() : "?";
            String status = item.getStatus() != null ? item.getStatus().name() : "?";
            
            // Create a formatted string with fixed width columns
            return String.format("%-8s %-10s %-6s %-12s %s", 
                id, 
                type, 
                priority.substring(0, Math.min(6, priority.length())), 
                status,
                title.length() > 20 ? title.substring(0, 17) + "..." : title);
        });
        
        // Create column headers for the list
        Label columnsHeader = new Label("ID       Type       Prior  Status       Title");
        Style headerStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        columnsHeader.setStyle(headerStyle);
        
        // Create the details panel
        Container detailsContainer = new Container("details-container");
        detailsContainer.setSize(new Dimension(50, 17));
        
        Style detailsContainerStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        detailsContainer.setStyle(detailsContainerStyle);
        
        BoxLayout detailsContainerLayout = new BoxLayout(Orientation.VERTICAL, 1);
        detailsContainer.setLayout(detailsContainerLayout);
        
        // Create the details header
        Label detailsHeader = new Label("Work Item Details");
        Style detailsHeaderStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        detailsHeader.setStyle(detailsHeaderStyle);
        
        // Create the details content
        detailsLabel = new Label("");
        detailsLabel.setWordWrap(true);
        
        // Add action buttons
        Button viewButton = new Button("View");
        viewButton.addClickListener(btn -> {
            WorkItem selected = workItemList.getSelectedItem();
            if (selected != null) {
                statusLabel.setText("Viewing work item: " + selected.getId());
                // In a real app, this would open a detailed view
            }
        });
        
        Button editButton = new Button("Edit");
        editButton.addClickListener(btn -> {
            WorkItem selected = workItemList.getSelectedItem();
            if (selected != null) {
                statusLabel.setText("Editing work item: " + selected.getId());
                // In a real app, this would open an edit form
            }
        });
        
        // Create a container for the buttons
        Container buttonContainer = new Container("button-container");
        BoxLayout buttonLayout = new BoxLayout(Orientation.HORIZONTAL, 2);
        buttonContainer.setLayout(buttonLayout);
        buttonContainer.addComponent(viewButton);
        buttonContainer.addComponent(editButton);
        
        // Add components to containers
        itemsContainer.addComponent(columnsHeader);
        itemsContainer.addComponent(workItemList);
        
        detailsContainer.addComponent(detailsHeader);
        detailsContainer.addComponent(detailsLabel);
        detailsContainer.addComponent(buttonContainer);
        
        // Add containers to the panel
        BoxConstraints listConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        listPanelLayout.setConstraints(itemsContainer, listConstraints);
        
        BoxConstraints detailsConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        listPanelLayout.setConstraints(detailsContainer, detailsConstraints);
        
        listPanel.addComponent(itemsContainer);
        listPanel.addComponent(detailsContainer);
        
        // Set up selection listener
        workItemList.addSelectionListener((list, selected) -> {
            if (selected != null) {
                updateDetailsPanel(selected);
            } else {
                detailsLabel.setText("");
            }
        });
        
        return listPanel;
    }
    
    /**
     * Creates the footer container with status and help text.
     * 
     * @return the footer container
     */
    private static Container createFooter() {
        Container footer = new Container("footer");
        footer.setSize(new Dimension(100, 3));
        
        Style footerStyle = new Style()
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        footer.setStyle(footerStyle);
        
        BoxLayout footerLayout = new BoxLayout(Orientation.HORIZONTAL, 2);
        footer.setLayout(footerLayout);
        
        statusLabel = new Label("Loading work items...");
        Label helpLabel = new Label("ESC: Exit  TAB: Navigate  ENTER: Select  F5: Refresh");
        
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
     * Applies filters and sorting to the work items list.
     */
    private static void applyFilters() {
        if (allWorkItems == null || workItemList == null) {
            return;
        }
        
        // Create a predicate for filtering
        Predicate<WorkItem> filter = item -> true;
        
        // Apply search filter
        if (searchTerm != null && !searchTerm.isEmpty()) {
            final String search = searchTerm.toLowerCase();
            filter = filter.and(item -> 
                (item.getId() != null && item.getId().toLowerCase().contains(search)) ||
                (item.getTitle() != null && item.getTitle().toLowerCase().contains(search)) ||
                (item.getDescription() != null && item.getDescription().toLowerCase().contains(search))
            );
        }
        
        // Apply type filter
        if (selectedType != null) {
            filter = filter.and(item -> item.getType() == selectedType);
        }
        
        // Apply priority filter
        if (selectedPriority != null) {
            filter = filter.and(item -> item.getPriority() == selectedPriority);
        }
        
        // Apply status filter
        if (selectedStatus != null) {
            filter = filter.and(item -> item.getStatus() == selectedStatus);
        }
        
        // Filter the items
        java.util.List<WorkItem> filteredItems = allWorkItems.stream()
            .filter(filter)
            .collect(Collectors.toList());
        
        // Create comparator for sorting
        Comparator<WorkItem> comparator;
        switch (sortBy) {
            case "Title":
                comparator = Comparator.comparing(WorkItem::getTitle, 
                    Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "Type":
                comparator = Comparator.comparing(item -> item.getType() != null ? item.getType().name() : "",
                    Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "Priority":
                comparator = Comparator.comparing(item -> item.getPriority() != null ? item.getPriority().ordinal() : Integer.MAX_VALUE);
                break;
            case "Status":
                comparator = Comparator.comparing(item -> item.getStatus() != null ? item.getStatus().name() : "",
                    Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "Created":
                comparator = Comparator.comparing(WorkItem::getCreated, 
                    Comparator.nullsLast(java.time.LocalDateTime::compareTo));
                break;
            case "Updated":
                comparator = Comparator.comparing(WorkItem::getUpdated, 
                    Comparator.nullsLast(java.time.LocalDateTime::compareTo));
                break;
            case "ID":
            default:
                comparator = Comparator.comparing(WorkItem::getId, 
                    Comparator.nullsLast(String::compareToIgnoreCase));
                break;
        }
        
        // Apply sort direction
        if (!sortAscending) {
            comparator = comparator.reversed();
        }
        
        // Sort the items
        Collections.sort(filteredItems, comparator);
        
        // Update the list
        workItemList.setItems(filteredItems);
        
        // Update status
        statusLabel.setText("Showing " + filteredItems.size() + " of " + allWorkItems.size() + " work items");
        
        // Select the first item if available
        if (!filteredItems.isEmpty()) {
            workItemList.setSelectedIndex(0);
        } else {
            detailsLabel.setText("No work items match the current filters.");
        }
    }
    
    /**
     * Clears all filters and resets to default state.
     */
    private static void clearFilters() {
        searchTerm = "";
        searchBox.setText("");
        
        selectedType = null;
        typeFilter.setSelectedIndex(0);
        
        selectedPriority = null;
        priorityFilter.setSelectedIndex(0);
        
        selectedStatus = null;
        statusFilter.setSelectedIndex(0);
        
        sortBy = "ID";
        sortByOptions.setSelectedIndex(0);
        
        sortAscending = true;
    }
    
    /**
     * Updates the details panel with the selected work item details.
     * 
     * @param item the selected work item
     */
    private static void updateDetailsPanel(WorkItem item) {
        if (item == null) {
            detailsLabel.setText("");
            return;
        }
        
        StringBuilder details = new StringBuilder();
        
        details.append("ID: ").append(item.getId()).append("\n\n");
        details.append("Title: ").append(item.getTitle()).append("\n\n");
        
        details.append("Type: ").append(item.getType()).append("\n");
        details.append("Priority: ").append(item.getPriority()).append("\n");
        details.append("Status: ").append(item.getStatus()).append("\n\n");
        
        details.append("Assignee: ").append(item.getAssignee() != null ? item.getAssignee() : "Unassigned").append("\n");
        details.append("Reporter: ").append(item.getReporter() != null ? item.getReporter() : "Unknown").append("\n\n");
        
        if (item.getCreated() != null) {
            details.append("Created: ").append(item.getCreated()).append("\n");
        }
        
        if (item.getUpdated() != null) {
            details.append("Updated: ").append(item.getUpdated()).append("\n");
        }
        
        if (item.getDueDate() != null) {
            details.append("Due: ").append(item.getDueDate()).append("\n");
        }
        
        details.append("\nDescription: ").append(item.getDescription() != null ? item.getDescription() : "No description provided.");
        
        detailsLabel.setText(details.toString());
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
        allWorkItems.add(item1);
        
        WorkItem item2 = new WorkItem("WI-1002", "Fix sorting bug in reports", 
                WorkItemType.BUG, Priority.MEDIUM, WorkflowState.READY);
        item2.setAssignee("jane.smith");
        item2.setReporter("john.qa");
        item2.setDescription("Reports are not sorting by date correctly when the user clicks on the date column header.");
        allWorkItems.add(item2);
        
        WorkItem item3 = new WorkItem("WI-1003", "Add export to CSV feature", 
                WorkItemType.FEATURE, Priority.LOW, WorkflowState.TESTING);
        item3.setAssignee("alex.dev");
        item3.setReporter("sarah.manager");
        item3.setDescription("Allow exporting report data to CSV format for further analysis in spreadsheet applications.");
        allWorkItems.add(item3);
        
        WorkItem item4 = new WorkItem("WI-1004", "Improve application performance", 
                WorkItemType.TASK, Priority.HIGH, WorkflowState.IN_PROGRESS);
        item4.setAssignee("jane.smith");
        item4.setReporter("sarah.manager");
        item4.setDescription("Optimize database queries and caching for faster response time, especially for report generation.");
        allWorkItems.add(item4);
        
        WorkItem item5 = new WorkItem("WI-1005", "Update user documentation", 
                WorkItemType.TASK, Priority.LOW, WorkflowState.DONE);
        item5.setAssignee("mark.writer");
        item5.setReporter("john.qa");
        item5.setDescription("Add sections for new features and improve existing content in the user manual.");
        allWorkItems.add(item5);
        
        WorkItem item6 = new WorkItem("WI-1006", "Fix memory leak in background process", 
                WorkItemType.BUG, Priority.CRITICAL, WorkflowState.READY);
        item6.setAssignee("alex.dev");
        item6.setReporter("jane.smith");
        item6.setDescription("Application is leaking memory when running for extended periods, causing crashes after about 48 hours of operation.");
        allWorkItems.add(item6);
        
        WorkItem item7 = new WorkItem("WI-1007", "Implement email notifications", 
                WorkItemType.FEATURE, Priority.MEDIUM, WorkflowState.CREATED);
        item7.setAssignee(null); // Unassigned
        item7.setReporter("sarah.manager");
        item7.setDescription("Send notifications when work items are assigned or status changes to keep team members informed.");
        allWorkItems.add(item7);
        
        WorkItem item8 = new WorkItem("WI-1008", "Update third-party libraries", 
                WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
        item8.setAssignee("john.doe");
        item8.setReporter("john.doe");
        item8.setDescription("Update all dependencies to latest versions to address security vulnerabilities and improve stability.");
        allWorkItems.add(item8);
        
        WorkItem item9 = new WorkItem("WI-1009", "Add dark mode support", 
                WorkItemType.FEATURE, Priority.LOW, WorkflowState.REVIEW);
        item9.setAssignee("jane.smith");
        item9.setReporter("mark.writer");
        item9.setDescription("Implement a dark theme for the application to reduce eye strain and save battery on OLED displays.");
        allWorkItems.add(item9);
        
        WorkItem item10 = new WorkItem("WI-1010", "Implement search functionality", 
                WorkItemType.FEATURE, Priority.HIGH, WorkflowState.TESTING);
        item10.setAssignee("alex.dev");
        item10.setReporter("sarah.manager");
        item10.setDescription("Add ability to search for work items by various criteria including title, description, assignee, and status.");
        allWorkItems.add(item10);
        
        // Add a spike for research
        WorkItem item11 = new WorkItem("WI-1011", "Research AI integration options", 
                WorkItemType.SPIKE, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
        item11.setAssignee("john.doe");
        item11.setReporter("sarah.manager");
        item11.setDescription("Investigate options for integrating AI-based recommendations into the workflow system. Time-boxed to 2 days.");
        allWorkItems.add(item11);
        
        // Add an epic
        WorkItem item12 = new WorkItem("WI-1012", "Mobile App Development", 
                WorkItemType.EPIC, Priority.HIGH, WorkflowState.IN_PROGRESS);
        item12.setAssignee("sarah.manager");
        item12.setReporter("sarah.manager");
        item12.setDescription("Develop a mobile application version of the system for iOS and Android platforms. This is a major initiative that will span multiple sprints.");
        allWorkItems.add(item12);
        
        // Add a story
        WorkItem item13 = new WorkItem("WI-1013", "User can reset their password", 
                WorkItemType.STORY, Priority.MEDIUM, WorkflowState.READY);
        item13.setAssignee("jane.smith");
        item13.setReporter("mark.writer");
        item13.setDescription("As a user, I want to be able to reset my password so that I can regain access to my account if I forget my credentials.");
        allWorkItems.add(item13);
        
        // Add more bugs
        WorkItem item14 = new WorkItem("WI-1014", "Date format inconsistent across application", 
                WorkItemType.BUG, Priority.LOW, WorkflowState.FOUND);
        item14.setAssignee(null);
        item14.setReporter("john.qa");
        item14.setDescription("Date formats vary between MM/DD/YYYY and DD/MM/YYYY in different sections of the application.");
        allWorkItems.add(item14);
        
        WorkItem item15 = new WorkItem("WI-1015", "Application crashes when handling large CSV files", 
                WorkItemType.BUG, Priority.HIGH, WorkflowState.TRIAGED);
        item15.setAssignee("alex.dev");
        item15.setReporter("jane.smith");
        item15.setDescription("When importing CSV files larger than 10MB, the application crashes with an out of memory error.");
        allWorkItems.add(item15);
    }
}