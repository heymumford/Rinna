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
import org.rinna.pui.cli.StatisticsBridge;
import org.rinna.pui.component.*;
import org.rinna.pui.component.BoxLayout.Orientation;
import org.rinna.pui.component.BoxLayout.BoxConstraints;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;
import org.rinna.pui.component.Button.ClickListener;

import org.rinna.cli.stats.StatisticType;
import org.rinna.cli.stats.StatisticValue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Demo that showcases a project dashboard with real-time statistics and charts.
 * This demo demonstrates how to visualize project metrics using PUI components.
 */
public class ProjectDashboardDemo {
    
    // Core components
    private static StatisticsBridge statisticsBridge;
    private static Label statusLabel;
    private static List<String> projects;
    private static String selectedProject = "All";
    private static Box projectBox;
    
    // Dashboard panels
    private static Container keyMetricsPanel;
    private static Container distributionPanel;
    private static Container velocityPanel;
    private static Container detailsPanel;
    
    // Auto-refresh timer
    private static Timer refreshTimer;
    private static final int REFRESH_INTERVAL = 30000; // 30 seconds
    
    /**
     * Entry point for the demo.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            // Initialize the statistics bridge
            statisticsBridge = StatisticsBridge.getInstance();
            
            // Load projects
            loadProjects();
            
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
            
            // Create the body with dashboard panels
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
            
            // Update the dashboard with initial data
            updateDashboard();
            
            // Start the auto-refresh timer
            startRefreshTimer();
            
            // Start the UI
            pui.start();
            
            // Clean up when UI exits
            stopRefreshTimer();
            
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
        
        // Create title with today's date
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ISO_DATE);
        Label titleLabel = new Label("Rinna Project Dashboard - " + dateStr);
        
        // Create project selector
        Label projectLabel = new Label("Project:");
        
        List<String> projectOptions = new ArrayList<>(projects);
        projectOptions.add(0, "All");
        projectBox = new Box("project-selector", projectOptions);
        projectBox.setSelectedItem("All");
        projectBox.addSelectionListener((box, selected) -> {
            selectedProject = selected;
            updateDashboard();
            statusLabel.setText("Viewing dashboard for project: " + selectedProject);
        });
        
        // Create refresh button
        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(btn -> {
            statisticsBridge.refreshStatistics();
            updateDashboard();
            statusLabel.setText("Dashboard refreshed for project: " + selectedProject);
        });
        
        // Add constraints to position the components
        BoxConstraints titleConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        headerLayout.setConstraints(titleLabel, titleConstraints);
        
        BoxConstraints projectLabelConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillHeight(true);
        headerLayout.setConstraints(projectLabel, projectLabelConstraints);
        
        BoxConstraints projectBoxConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillHeight(true);
        headerLayout.setConstraints(projectBox, projectBoxConstraints);
        
        BoxConstraints buttonConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillHeight(true);
        headerLayout.setConstraints(refreshButton, buttonConstraints);
        
        header.addComponent(titleLabel);
        header.addComponent(projectLabel);
        header.addComponent(projectBox);
        header.addComponent(refreshButton);
        
        return header;
    }
    
    /**
     * Creates the body container with dashboard panels.
     * 
     * @return the body container
     */
    private static Container createBody() {
        Container body = new Container("body");
        body.setSize(new Dimension(120, 29));
        
        BoxLayout bodyLayout = new BoxLayout(Orientation.VERTICAL, 1);
        body.setLayout(bodyLayout);
        
        // Create the top row with key metrics and distribution charts
        Container topRow = new Container("top-row");
        BoxLayout topRowLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        topRow.setLayout(topRowLayout);
        
        // Create key metrics panel
        keyMetricsPanel = createKeyMetricsPanel();
        
        // Create distribution panel
        distributionPanel = createDistributionPanel();
        
        // Add panels to top row with constraints
        BoxConstraints keyMetricsConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        topRowLayout.setConstraints(keyMetricsPanel, keyMetricsConstraints);
        
        BoxConstraints distributionConstraints = new BoxConstraints()
            .setWeight(2)
            .setFillHeight(true);
        topRowLayout.setConstraints(distributionPanel, distributionConstraints);
        
        topRow.addComponent(keyMetricsPanel);
        topRow.addComponent(distributionPanel);
        
        // Create the bottom row with velocity chart and details panel
        Container bottomRow = new Container("bottom-row");
        BoxLayout bottomRowLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        bottomRow.setLayout(bottomRowLayout);
        
        // Create velocity panel
        velocityPanel = createVelocityPanel();
        
        // Create details panel
        detailsPanel = createDetailsPanel();
        
        // Add panels to bottom row with constraints
        BoxConstraints velocityConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        bottomRowLayout.setConstraints(velocityPanel, velocityConstraints);
        
        BoxConstraints detailsConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        bottomRowLayout.setConstraints(detailsPanel, detailsConstraints);
        
        bottomRow.addComponent(velocityPanel);
        bottomRow.addComponent(detailsPanel);
        
        // Add rows to body with constraints
        BoxConstraints topRowConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillWidth(true);
        bodyLayout.setConstraints(topRow, topRowConstraints);
        
        BoxConstraints bottomRowConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillWidth(true);
        bodyLayout.setConstraints(bottomRow, bottomRowConstraints);
        
        body.addComponent(topRow);
        body.addComponent(bottomRow);
        
        return body;
    }
    
    /**
     * Creates the key metrics panel.
     * 
     * @return the key metrics panel
     */
    private static Container createKeyMetricsPanel() {
        Container panel = new Container("key-metrics-panel");
        
        Style panelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        panel.setStyle(panelStyle);
        
        BoxLayout panelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        panel.setLayout(panelLayout);
        
        // Add panel header
        Label header = new Label("Key Metrics");
        Style headerStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        header.setStyle(headerStyle);
        panel.addComponent(header);
        
        // Placeholder content - will be filled by updateDashboard()
        Label placeholder = new Label("Loading key metrics...");
        panel.addComponent(placeholder);
        
        return panel;
    }
    
    /**
     * Creates the distribution panel with charts.
     * 
     * @return the distribution panel
     */
    private static Container createDistributionPanel() {
        Container panel = new Container("distribution-panel");
        
        Style panelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        panel.setStyle(panelStyle);
        
        BoxLayout panelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        panel.setLayout(panelLayout);
        
        // Add panel header
        Label header = new Label("Item Distributions");
        Style headerStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        header.setStyle(headerStyle);
        panel.addComponent(header);
        
        // Placeholder content - will be filled by updateDashboard()
        Label placeholder = new Label("Loading distribution charts...");
        panel.addComponent(placeholder);
        
        return panel;
    }
    
    /**
     * Creates the velocity panel with velocity metrics.
     * 
     * @return the velocity panel
     */
    private static Container createVelocityPanel() {
        Container panel = new Container("velocity-panel");
        
        Style panelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        panel.setStyle(panelStyle);
        
        BoxLayout panelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        panel.setLayout(panelLayout);
        
        // Add panel header
        Label header = new Label("Velocity Metrics");
        Style headerStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        header.setStyle(headerStyle);
        panel.addComponent(header);
        
        // Placeholder content - will be filled by updateDashboard()
        Label placeholder = new Label("Loading velocity metrics...");
        panel.addComponent(placeholder);
        
        return panel;
    }
    
    /**
     * Creates the details panel.
     * 
     * @return the details panel
     */
    private static Container createDetailsPanel() {
        Container panel = new Container("details-panel");
        
        Style panelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        panel.setStyle(panelStyle);
        
        BoxLayout panelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        panel.setLayout(panelLayout);
        
        // Add panel header
        Label header = new Label("Project Details");
        Style headerStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        header.setStyle(headerStyle);
        panel.addComponent(header);
        
        // Placeholder content - will be filled by updateDashboard()
        Label placeholder = new Label("Loading project details...");
        panel.addComponent(placeholder);
        
        return panel;
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
        
        statusLabel = new Label("Loading dashboard data...");
        Label helpLabel = new Label("ESC: Exit  TAB: Navigate  F5: Refresh  ENTER: Select");
        
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
     * Loads projects from the statistics bridge.
     */
    private static void loadProjects() {
        projects = statisticsBridge.getAllProjects();
        if (projects.isEmpty()) {
            // Add a default project if none are available
            projects = new ArrayList<>();
            projects.add("RINNA-1");
        }
    }
    
    /**
     * Updates all dashboard panels with current statistics.
     */
    private static void updateDashboard() {
        // Determine which statistics to use based on selected project
        List<StatisticValue> statistics;
        if ("All".equals(selectedProject)) {
            statistics = statisticsBridge.getAllStatistics();
        } else {
            statistics = statisticsBridge.getProjectStatistics(selectedProject);
        }
        
        // Update each panel
        updateKeyMetricsPanel(statistics);
        updateDistributionPanel(statistics);
        updateVelocityPanel(statistics);
        updateDetailsPanel(statistics);
    }
    
    /**
     * Updates the key metrics panel with current statistics.
     * 
     * @param statistics the current statistics
     */
    private static void updateKeyMetricsPanel(List<StatisticValue> statistics) {
        // Clear existing content (except header)
        clearPanelContent(keyMetricsPanel);
        
        // Container for metrics
        Container metricsContainer = new Container("metrics-container");
        BoxLayout metricsLayout = new BoxLayout(Orientation.VERTICAL, 1);
        metricsContainer.setLayout(metricsLayout);
        
        // Extract key metrics
        StatisticValue totalItems = findStatistic(statistics, StatisticType.TOTAL_ITEMS);
        StatisticValue completionRate = findStatistic(statistics, StatisticType.COMPLETION_RATE);
        StatisticValue workInProgress = findStatistic(statistics, StatisticType.WORK_IN_PROGRESS);
        StatisticValue overdueItems = findStatistic(statistics, StatisticType.OVERDUE_ITEMS);
        
        // Add total items
        if (totalItems != null) {
            Label totalLabel = new Label("Total Items: " + (int)totalItems.getValue());
            Style totalStyle = new Style().setBold(true);
            totalLabel.setStyle(totalStyle);
            metricsContainer.addComponent(totalLabel);
        }
        
        // Add work in progress
        if (workInProgress != null) {
            Label wipLabel = new Label("In Progress: " + (int)workInProgress.getValue() + " items");
            metricsContainer.addComponent(wipLabel);
        }
        
        // Add overdue items
        if (overdueItems != null) {
            Label overdueLabel = new Label("Overdue: " + (int)overdueItems.getValue() + " items");
            Style overdueStyle = new Style();
            if (overdueItems.getValue() > 0) {
                overdueStyle.setForeground(Color.RED);
            }
            overdueLabel.setStyle(overdueStyle);
            metricsContainer.addComponent(overdueLabel);
        }
        
        // Add completion rate progress meter
        if (completionRate != null) {
            ProgressMeter progressMeter = new ProgressMeter("completion-meter", 35, 3);
            progressMeter.setValue(completionRate.getValue())
                        .setMaxValue(100)
                        .setTitle("Completion Rate")
                        .setUnit("%")
                        .setShowPercentage(true)
                        .setUseColors(true);
            metricsContainer.addComponent(progressMeter);
        }
        
        // Add metrics container to panel
        keyMetricsPanel.addComponent(metricsContainer);
    }
    
    /**
     * Updates the distribution panel with current statistics.
     * 
     * @param statistics the current statistics
     */
    private static void updateDistributionPanel(List<StatisticValue> statistics) {
        // Clear existing content (except header)
        clearPanelContent(distributionPanel);
        
        // Container for distribution charts
        Container chartsContainer = new Container("charts-container");
        BoxLayout chartsLayout = new BoxLayout(Orientation.VERTICAL, 1);
        chartsContainer.setLayout(chartsLayout);
        
        // State distribution chart
        StatisticValue statesStat = findStatistic(statistics, StatisticType.ITEMS_BY_STATE);
        if (statesStat != null && !statesStat.getBreakdown().isEmpty()) {
            BarChart statesChart = new BarChart("states-chart", 60, 8);
            statesChart.setTitle("Items by State")
                      .setData(statesStat.getBreakdown())
                      .setMaxBars(6)
                      .setSortByValue(true)
                      .setSortOrder(BarChart.SortOrder.DESCENDING)
                      .setShowValues(true)
                      .setShowPercentages(true);
            
            chartsContainer.addComponent(statesChart);
        }
        
        // Type distribution chart
        StatisticValue typesStat = findStatistic(statistics, StatisticType.ITEMS_BY_TYPE);
        if (typesStat != null && !typesStat.getBreakdown().isEmpty()) {
            BarChart typesChart = new BarChart("types-chart", 60, 8);
            typesChart.setTitle("Items by Type")
                     .setData(typesStat.getBreakdown())
                     .setMaxBars(6)
                     .setSortByValue(true)
                     .setSortOrder(BarChart.SortOrder.DESCENDING)
                     .setShowValues(true)
                     .setShowPercentages(true);
            
            List<Color> typeColors = Arrays.asList(
                Color.CYAN, Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.RED
            );
            typesChart.setBarColors(typeColors);
            
            chartsContainer.addComponent(typesChart);
        }
        
        // Add charts container to panel
        distributionPanel.addComponent(chartsContainer);
    }
    
    /**
     * Updates the velocity panel with current statistics.
     * 
     * @param statistics the current statistics
     */
    private static void updateVelocityPanel(List<StatisticValue> statistics) {
        // Clear existing content (except header)
        clearPanelContent(velocityPanel);
        
        // Container for velocity metrics
        Container velocityContainer = new Container("velocity-container");
        BoxLayout velocityLayout = new BoxLayout(Orientation.VERTICAL, 1);
        velocityContainer.setLayout(velocityLayout);
        
        // Extract velocity metrics
        StatisticValue throughput = findStatistic(statistics, StatisticType.THROUGHPUT);
        StatisticValue leadTime = findStatistic(statistics, StatisticType.LEAD_TIME);
        StatisticValue cycleTime = findStatistic(statistics, StatisticType.CYCLE_TIME);
        StatisticValue itemsCompleted = findStatistic(statistics, StatisticType.ITEMS_COMPLETED);
        StatisticValue itemsCreated = findStatistic(statistics, StatisticType.ITEMS_CREATED);
        
        // Add throughput
        if (throughput != null) {
            Label throughputLabel = new Label(String.format("Throughput: %.2f items/day", throughput.getValue()));
            Style throughputStyle = new Style().setBold(true);
            throughputLabel.setStyle(throughputStyle);
            velocityContainer.addComponent(throughputLabel);
        }
        
        // Add lead time
        if (leadTime != null) {
            Label leadTimeLabel = new Label(String.format("Lead Time: %.2f days", leadTime.getValue()));
            velocityContainer.addComponent(leadTimeLabel);
        }
        
        // Add cycle time
        if (cycleTime != null) {
            Label cycleTimeLabel = new Label(String.format("Cycle Time: %.2f days", cycleTime.getValue()));
            velocityContainer.addComponent(cycleTimeLabel);
        }
        
        // Add items completed
        if (itemsCompleted != null) {
            Label completedLabel = new Label("Completed (30 days): " + (int)itemsCompleted.getValue() + " items");
            velocityContainer.addComponent(completedLabel);
        }
        
        // Add items created
        if (itemsCreated != null) {
            Label createdLabel = new Label("Created (30 days): " + (int)itemsCreated.getValue() + " items");
            velocityContainer.addComponent(createdLabel);
        }
        
        // Add velocity container to panel
        velocityPanel.addComponent(velocityContainer);
        
        // Add a placeholder for a future velocity chart
        Label futureChartLabel = new Label("Detailed velocity chart will be added in a future version");
        Style futureStyle = new Style().setItalic(true);
        futureChartLabel.setStyle(futureStyle);
        velocityPanel.addComponent(futureChartLabel);
    }
    
    /**
     * Updates the details panel with current statistics.
     * 
     * @param statistics the current statistics
     */
    private static void updateDetailsPanel(List<StatisticValue> statistics) {
        // Clear existing content (except header)
        clearPanelContent(detailsPanel);
        
        // Container for details
        Container detailsContainer = new Container("details-container");
        BoxLayout detailsLayout = new BoxLayout(Orientation.VERTICAL, 1);
        detailsContainer.setLayout(detailsLayout);
        
        // Add project title
        Label projectLabel = new Label("Project: " + selectedProject);
        Style projectStyle = new Style().setBold(true);
        projectLabel.setStyle(projectStyle);
        detailsContainer.addComponent(projectLabel);
        
        // Add assignee distribution if available
        StatisticValue assigneeStat = findStatistic(statistics, StatisticType.ITEMS_BY_ASSIGNEE);
        if (assigneeStat != null && !assigneeStat.getBreakdown().isEmpty()) {
            Label assigneeHeader = new Label("Work Item Assignment");
            Style headerStyle = new Style().setBold(true).setUnderline(true);
            assigneeHeader.setStyle(headerStyle);
            detailsContainer.addComponent(assigneeHeader);
            
            // Create a small bar chart for assignees
            BarChart assigneeChart = new BarChart("assignee-chart", 35, 10);
            assigneeChart.setData(assigneeStat.getBreakdown())
                        .setMaxBars(5)
                        .setSortByValue(true)
                        .setSortOrder(BarChart.SortOrder.DESCENDING)
                        .setShowValues(true);
            
            detailsContainer.addComponent(assigneeChart);
        }
        
        // Add priority distribution if available
        StatisticValue priorityStat = findStatistic(statistics, StatisticType.ITEMS_BY_PRIORITY);
        if (priorityStat != null && !priorityStat.getBreakdown().isEmpty()) {
            Label priorityHeader = new Label("Items by Priority");
            Style headerStyle = new Style().setBold(true).setUnderline(true);
            priorityHeader.setStyle(headerStyle);
            detailsContainer.addComponent(priorityHeader);
            
            // Create a list of priorities with counts
            List<String> priorities = new ArrayList<>(priorityStat.getBreakdown().keySet());
            priorities.sort((p1, p2) -> {
                // Sort by custom priority order
                String[] order = {"CRITICAL", "HIGH", "MEDIUM", "LOW", "TRIVIAL"};
                int idx1 = Arrays.asList(order).indexOf(p1);
                int idx2 = Arrays.asList(order).indexOf(p2);
                if (idx1 < 0) idx1 = Integer.MAX_VALUE;
                if (idx2 < 0) idx2 = Integer.MAX_VALUE;
                return Integer.compare(idx1, idx2);
            });
            
            for (String priority : priorities) {
                double count = priorityStat.getBreakdown().get(priority);
                Label priorityLabel = new Label(String.format("%s: %.0f", priority, count));
                
                // Set color based on priority
                Style priorityStyle = new Style();
                if ("CRITICAL".equals(priority)) {
                    priorityStyle.setForeground(Color.RED).setBold(true);
                } else if ("HIGH".equals(priority)) {
                    priorityStyle.setForeground(Color.MAGENTA);
                } else if ("MEDIUM".equals(priority)) {
                    priorityStyle.setForeground(Color.YELLOW);
                } else if ("LOW".equals(priority)) {
                    priorityStyle.setForeground(Color.GREEN);
                }
                priorityLabel.setStyle(priorityStyle);
                
                detailsContainer.addComponent(priorityLabel);
            }
        }
        
        // Add details container to panel
        detailsPanel.addComponent(detailsContainer);
    }
    
    /**
     * Clears the content of a panel, keeping only the header.
     * 
     * @param panel the panel to clear
     */
    private static void clearPanelContent(Container panel) {
        if (panel.getComponentCount() <= 1) {
            return; // Only header or empty
        }
        
        // Keep the first component (header) and remove the rest
        Component header = panel.getComponent(0);
        panel.removeAllComponents();
        panel.addComponent(header);
    }
    
    /**
     * Finds a statistic by type.
     * 
     * @param statistics the list of statistics
     * @param type the statistic type to find
     * @return the found statistic, or null if not found
     */
    private static StatisticValue findStatistic(List<StatisticValue> statistics, StatisticType type) {
        return statistics.stream()
            .filter(stat -> stat.getType() == type)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Starts the auto-refresh timer.
     */
    private static void startRefreshTimer() {
        if (refreshTimer != null) {
            stopRefreshTimer();
        }
        
        refreshTimer = new Timer("DashboardRefreshTimer", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Refresh statistics and update dashboard
                statisticsBridge.refreshStatistics();
                updateDashboard();
            }
        }, REFRESH_INTERVAL, REFRESH_INTERVAL);
    }
    
    /**
     * Stops the auto-refresh timer.
     */
    private static void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
}