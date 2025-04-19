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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.pui.RinnaPUI;
import org.rinna.pui.cli.ServiceBridge;
import org.rinna.pui.component.*;
import org.rinna.pui.component.BoxLayout.BoxConstraints;
import org.rinna.pui.component.BoxLayout.Orientation;
import org.rinna.pui.examples.model.WorkItemRelationship;
import org.rinna.pui.examples.model.WorkItemRelationship.RelationshipType;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

/**
 * Demo that showcases dependency graph visualization with interactive exploration.
 * This demo demonstrates how the DependencyGraphView component can be used to
 * visualize and navigate through work item relationships.
 */
public class DependencyGraphDemo {
    
    // Core components
    private static ServiceBridge serviceBridge;
    private static List<WorkItem> workItemList;
    private static Label statusLabel;
    private static DependencyGraphView dependencyGraph;
    private static TextArea detailsArea;
    private static Map<String, List<CheckBox>> relationshipFilters = new HashMap<>();
    
    // Work item data
    private static List<WorkItem> allWorkItems = new ArrayList<>();
    private static List<WorkItemRelationship> allRelationships = new ArrayList<>();
    private static WorkItem selectedWorkItem;
    
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
            
            // Create the body with work items and dependency graph
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
                updateDependencyView(allWorkItems.get(0));
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
        
        Label titleLabel = new Label("Rinna Dependency Graph Demo");
        titleLabel.setAlignment(Label.Alignment.CENTER);
        
        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(btn -> {
            loadWorkItems();
            if (!allWorkItems.isEmpty()) {
                workItemList.setSelectedIndex(0);
                updateDependencyView(allWorkItems.get(0));
            }
            statusLabel.setText("Work items refreshed: " + allWorkItems.size() + " items loaded");
        });
        
        Button helpButton = new Button("Help");
        helpButton.addClickListener(btn -> {
            statusLabel.setText("Use arrow keys to navigate, Space to expand/collapse, +/- to adjust depth, Enter to select");
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
        headerLayout.setConstraints(helpButton, buttonConstraints);
        
        header.addComponent(titleLabel);
        header.addComponent(refreshButton);
        header.addComponent(helpButton);
        
        return header;
    }
    
    /**
     * Creates the body container with work items list and dependency graph.
     * 
     * @return the body container
     */
    private static Container createBody() {
        Container body = new Container("body");
        body.setSize(new Dimension(120, 29));
        
        BoxLayout bodyLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        body.setLayout(bodyLayout);
        
        // Create the left panel with work items list and filters
        Container leftPanel = createLeftPanel();
        
        // Create the right panel with dependency graph
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
     * Creates the left panel with work items list, details, and relationship filters.
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
        workItemList = new List<>("work-items-list", 8);
        workItemList.setItemRenderer(item -> {
            String id = item.getId().length() > 8 ? item.getId().substring(0, 8) : item.getId();
            String title = item.getTitle();
            
            return String.format("%-8s %s", 
                id, 
                title.length() > 20 ? title.substring(0, 17) + "..." : title);
        });
        
        // Create a details view for the selected work item
        detailsArea = new TextArea("details-area", 5, 36);
        detailsArea.setReadOnly(true);
        
        Label detailsHeader = new Label("Work Item Details");
        Style detailsHeaderStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        detailsHeader.setStyle(detailsHeaderStyle);
        
        // Create relationship type filters
        Container filtersContainer = createRelationshipFilters();
        
        // Add components to the left panel
        leftPanel.addComponent(listHeader);
        leftPanel.addComponent(workItemList);
        leftPanel.addComponent(detailsHeader);
        leftPanel.addComponent(detailsArea);
        leftPanel.addComponent(filtersContainer);
        
        // Set up selection listener
        workItemList.addSelectionListener((list, selected) -> {
            if (selected != null) {
                updateDependencyView(selected);
                updateDetailsArea(selected);
                selectedWorkItem = selected;
            }
        });
        
        return leftPanel;
    }
    
    /**
     * Creates the relationship filters container.
     * 
     * @return the filters container
     */
    private static Container createRelationshipFilters() {
        Container filtersContainer = new Container("filters-container");
        
        // Create a header for the filters
        Label filtersHeader = new Label("Relationship Filters");
        Style filtersHeaderStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        filtersHeader.setStyle(filtersHeaderStyle);
        
        BoxLayout filtersLayout = new BoxLayout(Orientation.VERTICAL, 0);
        filtersContainer.setLayout(filtersLayout);
        filtersContainer.addComponent(filtersHeader);
        
        // Create a row for inbound relationships
        Container inboundContainer = new Container("inbound-container");
        BoxLayout inboundLayout = new BoxLayout(Orientation.VERTICAL, 0);
        inboundContainer.setLayout(inboundLayout);
        
        Label inboundLabel = new Label("Inbound:");
        inboundContainer.addComponent(inboundLabel);
        
        Container inboundCheckboxes = new Container("inbound-checkboxes");
        BoxLayout inboundCheckboxesLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        inboundCheckboxes.setLayout(inboundCheckboxesLayout);
        
        List<CheckBox> inboundFilters = new ArrayList<>();
        relationshipFilters.put("inbound", inboundFilters);
        
        // Add inbound relationship checkboxes
        addRelationshipCheckbox(inboundCheckboxes, inboundFilters, RelationshipType.CHILD, true);
        addRelationshipCheckbox(inboundCheckboxes, inboundFilters, RelationshipType.BLOCKED_BY, true);
        addRelationshipCheckbox(inboundCheckboxes, inboundFilters, RelationshipType.DUPLICATED_BY, true);
        addRelationshipCheckbox(inboundCheckboxes, inboundFilters, RelationshipType.PRECEDED_BY, true);
        
        inboundContainer.addComponent(inboundCheckboxes);
        
        // Create a row for outbound relationships
        Container outboundContainer = new Container("outbound-container");
        BoxLayout outboundLayout = new BoxLayout(Orientation.VERTICAL, 0);
        outboundContainer.setLayout(outboundLayout);
        
        Label outboundLabel = new Label("Outbound:");
        outboundContainer.addComponent(outboundLabel);
        
        Container outboundCheckboxes = new Container("outbound-checkboxes");
        BoxLayout outboundCheckboxesLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        outboundCheckboxes.setLayout(outboundCheckboxesLayout);
        
        List<CheckBox> outboundFilters = new ArrayList<>();
        relationshipFilters.put("outbound", outboundFilters);
        
        // Add outbound relationship checkboxes
        addRelationshipCheckbox(outboundCheckboxes, outboundFilters, RelationshipType.PARENT, true);
        addRelationshipCheckbox(outboundCheckboxes, outboundFilters, RelationshipType.BLOCKS, true);
        addRelationshipCheckbox(outboundCheckboxes, outboundFilters, RelationshipType.DUPLICATES, true);
        addRelationshipCheckbox(outboundCheckboxes, outboundFilters, RelationshipType.FOLLOWS, true);
        
        outboundContainer.addComponent(outboundCheckboxes);
        
        // Create a row for bidirectional relationships
        Container biContainer = new Container("bi-container");
        BoxLayout biLayout = new BoxLayout(Orientation.VERTICAL, 0);
        biContainer.setLayout(biLayout);
        
        Label biLabel = new Label("Bidirectional:");
        biContainer.addComponent(biLabel);
        
        Container biCheckboxes = new Container("bi-checkboxes");
        BoxLayout biCheckboxesLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        biCheckboxes.setLayout(biCheckboxesLayout);
        
        List<CheckBox> biFilters = new ArrayList<>();
        relationshipFilters.put("bidirectional", biFilters);
        
        // Add bidirectional relationship checkbox
        addRelationshipCheckbox(biCheckboxes, biFilters, RelationshipType.RELATED, true);
        
        biContainer.addComponent(biCheckboxes);
        
        // Add all containers to the filters container
        filtersContainer.addComponent(inboundContainer);
        filtersContainer.addComponent(outboundContainer);
        filtersContainer.addComponent(biContainer);
        
        return filtersContainer;
    }
    
    /**
     * Adds a relationship checkbox to a container.
     * 
     * @param container The container to add the checkbox to
     * @param filtersList The list to add the checkbox to
     * @param type The relationship type
     * @param initialState The initial state (checked or unchecked)
     */
    private static void addRelationshipCheckbox(Container container, List<CheckBox> filtersList, 
                                             RelationshipType type, boolean initialState) {
        CheckBox checkbox = new CheckBox(type.getDisplayName());
        checkbox.setChecked(initialState);
        checkbox.setChangeListener((cb, checked) -> {
            updateRelationshipFilters();
        });
        
        container.addComponent(checkbox);
        filtersList.add(checkbox);
    }
    
    /**
     * Creates the right panel with the dependency graph.
     * 
     * @return the right panel container
     */
    private static Container createRightPanel() {
        Container rightPanel = new Container("right-panel");
        
        Style rightPanelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        rightPanel.setStyle(rightPanelStyle);
        
        BoxLayout rightPanelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        rightPanel.setLayout(rightPanelLayout);
        
        Label graphHeader = new Label("Dependency Graph");
        Style graphHeaderStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        graphHeader.setStyle(graphHeaderStyle);
        
        // Create the dependency graph view
        dependencyGraph = new DependencyGraphView("dependency-graph", 78, 27);
        
        // Configure the node selection handler
        dependencyGraph.setNodeSelectionHandler(item -> {
            if (item != null) {
                // Find the item in the list and select it
                for (int i = 0; i < workItemList.getItemCount(); i++) {
                    if (workItemList.getItem(i).getId().equals(item.getId())) {
                        workItemList.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });
        
        // Configure custom labels
        dependencyGraph.setNodeLabelProvider(item -> {
            return String.format("%s: %s", 
                item.getId().length() > 6 ? item.getId().substring(0, 6) : item.getId(),
                item.getTitle().length() > 12 ? item.getTitle().substring(0, 9) + "..." : item.getTitle());
        });
        
        // Configure edge labels
        dependencyGraph.setEdgeLabelProvider(relationship -> {
            String type = relationship.getRelationshipType().toString();
            return type.substring(0, Math.min(type.length(), 1));
        });
        
        // Add components to the right panel
        rightPanel.addComponent(graphHeader);
        rightPanel.addComponent(dependencyGraph);
        
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
        Label helpLabel = new Label("ESC: Exit  TAB: Navigate  ↑↓←→: Move  Space: Expand  +/-: Depth");
        
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
     * Loads work items and creates relationships.
     */
    private static void loadWorkItems() {
        try {
            allWorkItems = serviceBridge.getAllWorkItems();
            
            if (allWorkItems.isEmpty()) {
                // If no items are returned, create some sample items
                createSampleWorkItems();
            }
            
            // Create relationships between work items
            createWorkItemRelationships();
            
            // Update the list items
            workItemList.setItems(allWorkItems);
            
            statusLabel.setText("Loaded " + allWorkItems.size() + " work items with " + 
                               allRelationships.size() + " relationships");
        } catch (Exception e) {
            statusLabel.setText("Error loading work items: " + e.getMessage());
            // Create sample items on error
            createSampleWorkItems();
            createWorkItemRelationships();
            workItemList.setItems(allWorkItems);
        }
    }
    
    /**
     * Updates the dependency graph view with the selected work item.
     * 
     * @param item the selected work item
     */
    private static void updateDependencyView(WorkItem item) {
        if (item == null) {
            return;
        }
        
        // Update the dependency graph
        dependencyGraph.setData(item, allWorkItems, allRelationships);
        
        // Apply relationship filters
        updateRelationshipFilters();
        
        // Update the status label
        statusLabel.setText("Viewing dependencies for " + item.getId() + " - " + item.getTitle());
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
        details.append("State: ").append(item.getState()).append("\n");
        
        detailsArea.setText(details.toString());
    }
    
    /**
     * Updates the dependency graph with the current relationship filters.
     */
    private static void updateRelationshipFilters() {
        // Collect all checked relationship types
        Set<RelationshipType> visibleTypes = new HashSet<>();
        
        // Process inbound relationship filters
        for (int i = 0; i < relationshipFilters.get("inbound").size(); i++) {
            CheckBox cb = relationshipFilters.get("inbound").get(i);
            if (cb.isChecked()) {
                switch (i) {
                    case 0: visibleTypes.add(RelationshipType.CHILD); break;
                    case 1: visibleTypes.add(RelationshipType.BLOCKED_BY); break;
                    case 2: visibleTypes.add(RelationshipType.DUPLICATED_BY); break;
                    case 3: visibleTypes.add(RelationshipType.PRECEDED_BY); break;
                }
            }
        }
        
        // Process outbound relationship filters
        for (int i = 0; i < relationshipFilters.get("outbound").size(); i++) {
            CheckBox cb = relationshipFilters.get("outbound").get(i);
            if (cb.isChecked()) {
                switch (i) {
                    case 0: visibleTypes.add(RelationshipType.PARENT); break;
                    case 1: visibleTypes.add(RelationshipType.BLOCKS); break;
                    case 2: visibleTypes.add(RelationshipType.DUPLICATES); break;
                    case 3: visibleTypes.add(RelationshipType.FOLLOWS); break;
                }
            }
        }
        
        // Process bidirectional relationship filters
        for (int i = 0; i < relationshipFilters.get("bidirectional").size(); i++) {
            CheckBox cb = relationshipFilters.get("bidirectional").get(i);
            if (cb.isChecked()) {
                switch (i) {
                    case 0: visibleTypes.add(RelationshipType.RELATED); break;
                }
            }
        }
        
        // Update the dependency graph
        dependencyGraph.setVisibleRelationshipTypes(visibleTypes);
    }
    
    /**
     * Creates sample work items for the demo.
     */
    private static void createSampleWorkItems() {
        allWorkItems = new ArrayList<>();
        
        // Create a set of sample work items
        WorkItem epic = createWorkItem("WI-1001", "Epic: User Authentication System", 
                                    WorkItemType.EPIC, Priority.HIGH, WorkflowState.IN_PROGRESS);
                
        WorkItem story1 = createWorkItem("WI-1002", "User Registration", 
                                      WorkItemType.STORY, Priority.HIGH, WorkflowState.IN_PROGRESS);
                                      
        WorkItem story2 = createWorkItem("WI-1003", "User Login", 
                                      WorkItemType.STORY, Priority.HIGH, WorkflowState.READY);
                                      
        WorkItem story3 = createWorkItem("WI-1004", "Password Reset", 
                                      WorkItemType.STORY, Priority.MEDIUM, WorkflowState.CREATED);
                                      
        WorkItem task1 = createWorkItem("WI-1005", "Implement registration form", 
                                     WorkItemType.TASK, Priority.MEDIUM, WorkflowState.DONE);
                                     
        WorkItem task2 = createWorkItem("WI-1006", "Implement email verification", 
                                     WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
                                     
        WorkItem task3 = createWorkItem("WI-1007", "Implement login form", 
                                     WorkItemType.TASK, Priority.MEDIUM, WorkflowState.READY);
                                     
        WorkItem task4 = createWorkItem("WI-1008", "Implement session management", 
                                     WorkItemType.TASK, Priority.MEDIUM, WorkflowState.CREATED);
                                     
        WorkItem bug1 = createWorkItem("WI-1009", "Registration fails with special characters", 
                                    WorkItemType.BUG, Priority.HIGH, WorkflowState.IN_PROGRESS);
                                    
        WorkItem bug2 = createWorkItem("WI-1010", "Login page not mobile responsive", 
                                    WorkItemType.BUG, Priority.LOW, WorkflowState.CREATED);
                                    
        WorkItem spike = createWorkItem("WI-1011", "Research OAuth integration options", 
                                     WorkItemType.SPIKE, Priority.LOW, WorkflowState.READY);
        
        // Second feature branch
        WorkItem feature = createWorkItem("WI-1012", "Search Functionality", 
                                       WorkItemType.FEATURE, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
                                       
        WorkItem task5 = createWorkItem("WI-1013", "Implement basic search", 
                                     WorkItemType.TASK, Priority.MEDIUM, WorkflowState.DONE);
                                     
        WorkItem task6 = createWorkItem("WI-1014", "Add advanced filters", 
                                     WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
                                     
        WorkItem bug3 = createWorkItem("WI-1015", "Search performance issue with large datasets", 
                                    WorkItemType.BUG, Priority.HIGH, WorkflowState.READY);
    }
    
    /**
     * Helper method to create a work item and add it to the list.
     * 
     * @param id The work item ID
     * @param title The work item title
     * @param type The work item type
     * @param priority The work item priority
     * @param state The work item state
     * @return The created work item
     */
    private static WorkItem createWorkItem(String id, String title, WorkItemType type, 
                                       Priority priority, WorkflowState state) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setType(type);
        item.setPriority(priority);
        item.setState(state);
        
        allWorkItems.add(item);
        return item;
    }
    
    /**
     * Creates relationships between work items.
     */
    private static void createWorkItemRelationships() {
        allRelationships = new ArrayList<>();
        
        if (allWorkItems.size() < 5) {
            return; // Not enough work items to create relationships
        }
        
        // Create a relationship map for easy access
        Map<String, WorkItem> itemMap = new HashMap<>();
        for (WorkItem item : allWorkItems) {
            itemMap.put(item.getId(), item);
        }
        
        // Now create relationships
        try {
            // Epic contains stories
            createRelationship(itemMap.get("WI-1001"), itemMap.get("WI-1002"), RelationshipType.PARENT);
            createRelationship(itemMap.get("WI-1001"), itemMap.get("WI-1003"), RelationshipType.PARENT);
            createRelationship(itemMap.get("WI-1001"), itemMap.get("WI-1004"), RelationshipType.PARENT);
            
            // Stories contain tasks
            createRelationship(itemMap.get("WI-1002"), itemMap.get("WI-1005"), RelationshipType.PARENT);
            createRelationship(itemMap.get("WI-1002"), itemMap.get("WI-1006"), RelationshipType.PARENT);
            createRelationship(itemMap.get("WI-1003"), itemMap.get("WI-1007"), RelationshipType.PARENT);
            createRelationship(itemMap.get("WI-1003"), itemMap.get("WI-1008"), RelationshipType.PARENT);
            
            // Bugs block tasks
            createRelationship(itemMap.get("WI-1009"), itemMap.get("WI-1006"), RelationshipType.BLOCKS);
            createRelationship(itemMap.get("WI-1010"), itemMap.get("WI-1007"), RelationshipType.BLOCKS);
            
            // Tasks have dependencies
            createRelationship(itemMap.get("WI-1005"), itemMap.get("WI-1006"), RelationshipType.PRECEDED_BY);
            createRelationship(itemMap.get("WI-1007"), itemMap.get("WI-1008"), RelationshipType.PRECEDED_BY);
            
            // Spike is related to stories
            createRelationship(itemMap.get("WI-1011"), itemMap.get("WI-1003"), RelationshipType.RELATED);
            createRelationship(itemMap.get("WI-1011"), itemMap.get("WI-1004"), RelationshipType.RELATED);
            
            // Second feature branch
            createRelationship(itemMap.get("WI-1012"), itemMap.get("WI-1013"), RelationshipType.PARENT);
            createRelationship(itemMap.get("WI-1012"), itemMap.get("WI-1014"), RelationshipType.PARENT);
            createRelationship(itemMap.get("WI-1013"), itemMap.get("WI-1014"), RelationshipType.PRECEDED_BY);
            createRelationship(itemMap.get("WI-1015"), itemMap.get("WI-1014"), RelationshipType.BLOCKS);
            
            // Connect the two branches
            createRelationship(itemMap.get("WI-1001"), itemMap.get("WI-1012"), RelationshipType.RELATED);
            
        } catch (Exception e) {
            statusLabel.setText("Error creating relationships: " + e.getMessage());
        }
    }
    
    /**
     * Creates a relationship between two work items and adds it to the list.
     * 
     * @param source The source work item
     * @param target The target work item
     * @param type The relationship type
     */
    private static void createRelationship(WorkItem source, WorkItem target, RelationshipType type) {
        if (source == null || target == null) {
            return;
        }
        
        // Create the primary relationship
        WorkItemRelationship relationship = new WorkItemRelationship(source, target, type);
        allRelationships.add(relationship);
        
        // Create the complementary relationship
        WorkItemRelationship complementary = relationship.createComplementaryRelationship();
        allRelationships.add(complementary);
    }
    
    /**
     * Helper class to represent a work item with the required fields for the demo.
     */
    private static class WorkItem {
        private String id;
        private String title;
        private WorkItemType type;
        private Priority priority;
        private WorkflowState state;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public WorkItemType getType() {
            return type;
        }
        
        public void setType(WorkItemType type) {
            this.type = type;
        }
        
        public Priority getPriority() {
            return priority;
        }
        
        public void setPriority(Priority priority) {
            this.priority = priority;
        }
        
        public WorkflowState getState() {
            return state;
        }
        
        public void setState(WorkflowState state) {
            this.state = state;
        }
    }
}