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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Demo that showcases the ServiceBridge integration with PUI.
 * This example shows a real-time operations monitor that displays
 * system operations tracked through the MetadataService.
 */
public class OperationsMonitorDemo {
    
    private static ServiceBridge serviceBridge;
    private static Label statusLabel;
    private static List<Map<String, Object>> operationsList;
    private static Label detailsLabel;
    private static boolean autoRefresh = true;
    private static final int REFRESH_INTERVAL_MS = 2000;
    
    /**
     * Entry point for the demo.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            // Initialize the service bridge
            serviceBridge = ServiceBridge.getInstance();
            
            // Trigger some sample operations to have data to display
            initializeSampleOperations();
            
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
            
            // Create the body
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
            
            // Set up a background thread for auto-refresh
            Thread refreshThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(REFRESH_INTERVAL_MS);
                        if (autoRefresh) {
                            refreshOperationsList();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            refreshThread.setDaemon(true);
            refreshThread.start();
            
            // Refresh initially
            refreshOperationsList();
            
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
        
        Label titleLabel = new Label("Rinna Operations Monitor");
        
        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(btn -> refreshOperationsList());
        
        Button toggleButton = new Button("Auto-Refresh: ON");
        toggleButton.addClickListener(btn -> {
            autoRefresh = !autoRefresh;
            toggleButton.setText("Auto-Refresh: " + (autoRefresh ? "ON" : "OFF"));
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
        headerLayout.setConstraints(toggleButton, buttonConstraints);
        
        header.addComponent(titleLabel);
        header.addComponent(refreshButton);
        header.addComponent(toggleButton);
        
        return header;
    }
    
    /**
     * Creates the body container with split view of operation list and details.
     * 
     * @return the body container
     */
    private static Container createBody() {
        Container body = new Container("body");
        body.setSize(new Dimension(100, 24));
        
        Style bodyStyle = new Style();
        body.setStyle(bodyStyle);
        
        BoxLayout bodyLayout = new BoxLayout(Orientation.HORIZONTAL, 0);
        body.setLayout(bodyLayout);
        
        // Create the operations list panel
        Container listPanel = new Container("list-panel");
        listPanel.setSize(new Dimension(50, 24));
        
        Style listPanelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        listPanel.setStyle(listPanelStyle);
        
        // Create the operation details panel
        Container detailsPanel = new Container("details-panel");
        detailsPanel.setSize(new Dimension(50, 24));
        
        Style detailsPanelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        detailsPanel.setStyle(detailsPanelStyle);
        
        // Set up layouts for the panels
        BoxLayout listPanelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        listPanel.setLayout(listPanelLayout);
        
        BoxLayout detailsPanelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        detailsPanel.setLayout(detailsPanelLayout);
        
        // Create the list header
        Label listHeaderLabel = new Label("Recent Operations");
        Style listHeaderStyle = new Style()
            .setBold(true)
            .setForeground(Color.YELLOW);
        listHeaderLabel.setStyle(listHeaderStyle);
        
        // Create the operations list
        operationsList = new List<>("operations-list", 20);
        operationsList.setItemRenderer(item -> {
            String command = (String) item.getOrDefault("command", "Unknown");
            String status = (String) item.getOrDefault("status", "Unknown");
            String type = (String) item.getOrDefault("type", "Unknown");
            
            // Color-code status
            String statusDisplay;
            if ("COMPLETED".equals(status)) {
                statusDisplay = "✓";
            } else if ("FAILED".equals(status)) {
                statusDisplay = "✗";
            } else {
                statusDisplay = "⧖";
            }
            
            return String.format("%s [%s] %s", statusDisplay, type, command);
        });
        
        // Create the details header
        Label detailsHeaderLabel = new Label("Operation Details");
        Style detailsHeaderStyle = new Style()
            .setBold(true)
            .setForeground(Color.YELLOW);
        detailsHeaderLabel.setStyle(detailsHeaderStyle);
        
        // Create the details content
        detailsLabel = new Label("");
        detailsLabel.setWordWrap(true);
        
        // Add components to panels
        listPanel.addComponent(listHeaderLabel);
        listPanel.addComponent(operationsList);
        
        detailsPanel.addComponent(detailsHeaderLabel);
        detailsPanel.addComponent(detailsLabel);
        
        // Set up selection listener
        operationsList.addSelectionListener((list, selected) -> {
            if (selected != null) {
                updateDetailsPanel(selected);
            } else {
                detailsLabel.setText("");
            }
        });
        
        // Add panels to body
        BoxConstraints listConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        bodyLayout.setConstraints(listPanel, listConstraints);
        
        BoxConstraints detailsConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        bodyLayout.setConstraints(detailsPanel, detailsConstraints);
        
        body.addComponent(listPanel);
        body.addComponent(detailsPanel);
        
        return body;
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
        
        statusLabel = new Label("Status: Loading operations...");
        Label helpLabel = new Label("Press ESC to exit, TAB to navigate, ENTER to select");
        
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
     * Refreshes the operations list with the latest data.
     */
    private static void refreshOperationsList() {
        try {
            java.util.List<Map<String, Object>> operations = serviceBridge.getRecentOperations(20);
            operationsList.setItems(operations);
            
            if (!operations.isEmpty()) {
                operationsList.setSelectedIndex(0);
                statusLabel.setText("Status: Showing " + operations.size() + " operations");
            } else {
                statusLabel.setText("Status: No operations to display");
            }
        } catch (Exception e) {
            statusLabel.setText("Status: Error refreshing operations: " + e.getMessage());
        }
    }
    
    /**
     * Updates the details panel with the selected operation details.
     * 
     * @param operation the selected operation
     */
    private static void updateDetailsPanel(Map<String, Object> operation) {
        StringBuilder details = new StringBuilder();
        
        details.append("ID: ").append(operation.getOrDefault("id", "Unknown")).append("\n\n");
        details.append("Command: ").append(operation.getOrDefault("command", "Unknown")).append("\n");
        details.append("Type: ").append(operation.getOrDefault("type", "Unknown")).append("\n");
        details.append("Status: ").append(operation.getOrDefault("status", "Unknown")).append("\n\n");
        
        details.append("Started: ").append(operation.getOrDefault("startTime", "Unknown")).append("\n");
        if (operation.containsKey("endTime")) {
            details.append("Ended: ").append(operation.get("endTime")).append("\n");
        }
        if (operation.containsKey("durationMs")) {
            details.append("Duration: ").append(operation.get("durationMs")).append(" ms\n");
        }
        
        details.append("\nUser: ").append(operation.getOrDefault("user", "Unknown")).append("\n\n");
        
        if (operation.containsKey("parameters")) {
            details.append("Parameters:\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) operation.get("parameters");
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    details.append("  ").append(param.getKey()).append(": ");
                    details.append(param.getValue() != null ? param.getValue() : "null").append("\n");
                }
            } else {
                details.append("  None\n");
            }
        }
        
        details.append("\n");
        
        if (operation.containsKey("result")) {
            details.append("Result: ").append(operation.get("result")).append("\n");
        } else if (operation.containsKey("error")) {
            details.append("Error: ").append(operation.get("error")).append("\n");
        }
        
        detailsLabel.setText(details.toString());
    }
    
    /**
     * Initializes sample operations by calling various ServiceBridge methods.
     */
    private static void initializeSampleOperations() {
        try {
            // Get all work items
            serviceBridge.getAllWorkItems();
            
            // Get specific work item
            serviceBridge.getWorkItem("WI-1001");
            
            // Search for work items
            serviceBridge.searchWorkItems("bug");
            
            // Try to get a non-existent work item to generate an error
            try {
                serviceBridge.getWorkItem("NON-EXISTENT");
            } catch (Exception e) {
                // Expected exception
            }
        } catch (Exception e) {
            System.err.println("Error initializing sample operations: " + e.getMessage());
        }
    }
}