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

import org.rinna.pui.RinnaPUI;
import org.rinna.pui.component.*;
import org.rinna.pui.component.BoxLayout.BoxConstraints;
import org.rinna.pui.component.BoxLayout.HorizontalAlignment;
import org.rinna.pui.component.BoxLayout.Orientation;
import org.rinna.pui.component.List;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

/**
 * Example demonstrating a work item view using the PUI components.
 */
public class WorkItemViewDemo {
    
    /**
     * Simple work item class for the demo.
     */
    private static class WorkItem {
        private String id;
        private String title;
        private String description;
        private String status;
        private String assignee;
        
        public WorkItem(String id, String title, String description, String status, String assignee) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.status = status;
            this.assignee = assignee;
        }
        
        public String getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getAssignee() {
            return assignee;
        }
        
        @Override
        public String toString() {
            return id + ": " + title;
        }
    }
    
    /**
     * Entry point for the demo.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            // Create sample work items
            java.util.List<WorkItem> workItems = createSampleWorkItems();
            
            // Create the UI
            RinnaPUI pui = RinnaPUI.getInstance();
            
            // Create the main container
            Container mainContainer = new Container("main");
            mainContainer.setPosition(new Point(0, 0));
            mainContainer.setSize(new Dimension(80, 24));
            
            // Set up the main layout
            BoxLayout mainLayout = new BoxLayout(Orientation.VERTICAL, 0);
            mainContainer.setLayout(mainLayout);
            
            // Create the header
            Container headerContainer = createHeader();
            
            // Create the body with split view
            Container bodyContainer = createBody(workItems);
            
            // Create the footer
            Container footerContainer = createFooter();
            
            // Add components to the main container
            mainContainer.addComponent(headerContainer);
            mainContainer.addComponent(bodyContainer);
            mainContainer.addComponent(footerContainer);
            
            // Create a custom theme
            Theme theme = Theme.createDefault();
            
            // Initialize and start the UI
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
     * Creates the header container with title and info.
     * 
     * @return the header container
     */
    private static Container createHeader() {
        Container header = new Container("header");
        header.setSize(new Dimension(80, 3));
        
        Style headerStyle = new Style()
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE)
            .setBold(true);
        header.setStyle(headerStyle);
        
        BoxLayout headerLayout = new BoxLayout(Orientation.HORIZONTAL, 2);
        header.setLayout(headerLayout);
        
        Label titleLabel = new Label("Rinna Work Item Manager");
        Label userLabel = new Label("User: admin");
        
        // Add constraints to position the components
        BoxConstraints titleConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true)
            .setVerticalAlignment(BoxLayout.VerticalAlignment.CENTER);
        headerLayout.setConstraints(titleLabel, titleConstraints);
        
        BoxConstraints userConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillHeight(true)
            .setVerticalAlignment(BoxLayout.VerticalAlignment.CENTER);
        headerLayout.setConstraints(userLabel, userConstraints);
        
        header.addComponent(titleLabel);
        header.addComponent(userLabel);
        
        return header;
    }
    
    /**
     * Creates the body container with split view of work item list and details.
     * 
     * @param workItems the list of work items to display
     * @return the body container
     */
    private static Container createBody(java.util.List<WorkItem> workItems) {
        Container body = new Container("body");
        body.setSize(new Dimension(80, 18));
        
        Style bodyStyle = new Style();
        body.setStyle(bodyStyle);
        
        BoxLayout bodyLayout = new BoxLayout(Orientation.HORIZONTAL, 0);
        body.setLayout(bodyLayout);
        
        // Create the work item list panel
        Container listPanel = new Container("list-panel");
        listPanel.setSize(new Dimension(25, 18));
        
        Style listPanelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        listPanel.setStyle(listPanelStyle);
        
        // Create the work item details panel
        Container detailsPanel = new Container("details-panel");
        detailsPanel.setSize(new Dimension(55, 18));
        
        Style detailsPanelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        detailsPanel.setStyle(detailsPanelStyle);
        
        // Set up layouts for the panels
        BoxLayout listPanelLayout = new BoxLayout(Orientation.VERTICAL, 0);
        listPanel.setLayout(listPanelLayout);
        
        BoxLayout detailsPanelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        detailsPanel.setLayout(detailsPanelLayout);
        
        // Create the work item list
        List<WorkItem> workItemList = new List<>("work-item-list", 16);
        workItemList.setItems(workItems);
        workItemList.setSelectedIndex(0);
        workItemList.setItemRenderer(WorkItem::toString);
        
        // Create detail components
        Label titleLabel = new Label("Title:");
        TextBox titleField = new TextBox(40);
        
        Label descriptionLabel = new Label("Description:");
        TextBox descriptionField = new TextBox(40);
        
        Label statusLabel = new Label("Status:");
        TextBox statusField = new TextBox(20);
        
        Label assigneeLabel = new Label("Assignee:");
        TextBox assigneeField = new TextBox(20);
        
        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");
        
        // Create container for buttons
        Container buttonContainer = new Container("button-container");
        BoxLayout buttonLayout = new BoxLayout(Orientation.HORIZONTAL, 2);
        buttonContainer.setLayout(buttonLayout);
        buttonContainer.addComponent(saveButton);
        buttonContainer.addComponent(cancelButton);
        
        // Add components to panels
        listPanel.addComponent(workItemList);
        
        detailsPanel.addComponent(titleLabel);
        detailsPanel.addComponent(titleField);
        detailsPanel.addComponent(descriptionLabel);
        detailsPanel.addComponent(descriptionField);
        detailsPanel.addComponent(statusLabel);
        detailsPanel.addComponent(statusField);
        detailsPanel.addComponent(assigneeLabel);
        detailsPanel.addComponent(assigneeField);
        detailsPanel.addComponent(buttonContainer);
        
        // Add panels to body
        BoxConstraints listConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        bodyLayout.setConstraints(listPanel, listConstraints);
        
        BoxConstraints detailsConstraints = new BoxConstraints()
            .setWeight(2)
            .setFillHeight(true);
        bodyLayout.setConstraints(detailsPanel, detailsConstraints);
        
        body.addComponent(listPanel);
        body.addComponent(detailsPanel);
        
        // Set up selection listener to update detail fields
        workItemList.addSelectionListener((list, selected) -> {
            if (selected != null) {
                titleField.setText(selected.getTitle());
                descriptionField.setText(selected.getDescription());
                statusField.setText(selected.getStatus());
                assigneeField.setText(selected.getAssignee());
            } else {
                titleField.setText("");
                descriptionField.setText("");
                statusField.setText("");
                assigneeField.setText("");
            }
        });
        
        // Trigger initial update of detail fields
        workItemList.notifySelectionChange();
        
        return body;
    }
    
    /**
     * Creates the footer container with status and help text.
     * 
     * @return the footer container
     */
    private static Container createFooter() {
        Container footer = new Container("footer");
        footer.setSize(new Dimension(80, 3));
        
        Style footerStyle = new Style()
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        footer.setStyle(footerStyle);
        
        BoxLayout footerLayout = new BoxLayout(Orientation.HORIZONTAL, 2);
        footer.setLayout(footerLayout);
        
        Label statusLabel = new Label("Status: Ready");
        Label helpLabel = new Label("Press ESC to exit");
        
        // Add constraints to position the components
        BoxConstraints statusConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true)
            .setVerticalAlignment(BoxLayout.VerticalAlignment.CENTER);
        footerLayout.setConstraints(statusLabel, statusConstraints);
        
        BoxConstraints helpConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillHeight(true)
            .setVerticalAlignment(BoxLayout.VerticalAlignment.CENTER)
            .setHorizontalAlignment(HorizontalAlignment.RIGHT);
        footerLayout.setConstraints(helpLabel, helpConstraints);
        
        footer.addComponent(statusLabel);
        footer.addComponent(helpLabel);
        
        return footer;
    }
    
    /**
     * Creates sample work items for the demo.
     * 
     * @return the list of sample work items
     */
    private static java.util.List<WorkItem> createSampleWorkItems() {
        java.util.List<WorkItem> items = new ArrayList<>();
        
        items.add(new WorkItem("WI-1001", "Implement login functionality", 
                "Add user authentication to the application", "In Progress", "john.doe"));
        
        items.add(new WorkItem("WI-1002", "Fix sorting bug in reports", 
                "Reports are not sorting by date correctly", "Open", "jane.smith"));
        
        items.add(new WorkItem("WI-1003", "Add export to CSV feature", 
                "Allow exporting report data to CSV format", "Open", "unassigned"));
        
        items.add(new WorkItem("WI-1004", "Improve application performance", 
                "Optimize database queries for faster response time", "In Progress", "alex.wong"));
        
        items.add(new WorkItem("WI-1005", "Update user documentation", 
                "Add sections for new features and improve existing content", "Ready for Review", "sarah.brown"));
        
        items.add(new WorkItem("WI-1006", "Fix memory leak in background process", 
                "Application is leaking memory when running for extended periods", "Open", "john.doe"));
        
        items.add(new WorkItem("WI-1007", "Implement email notifications", 
                "Send notifications when work items are assigned or status changes", "Not Started", "unassigned"));
        
        items.add(new WorkItem("WI-1008", "Update third-party libraries", 
                "Update all dependencies to latest versions", "In Progress", "alex.wong"));
        
        items.add(new WorkItem("WI-1009", "Add dark mode support", 
                "Implement a dark theme for the application", "Open", "jane.smith"));
        
        items.add(new WorkItem("WI-1010", "Implement search functionality", 
                "Add ability to search for work items by various criteria", "Ready for Review", "sarah.brown"));
        
        return items;
    }
}