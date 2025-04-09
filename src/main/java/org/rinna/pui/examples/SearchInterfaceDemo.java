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
import org.rinna.pui.cli.SearchBridge;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Demo that showcases a context-aware search interface with auto-completion.
 * This demo demonstrates how to use the AutoCompleteTextBox component with
 * the SearchBridge to provide intelligent search suggestions.
 */
public class SearchInterfaceDemo {
    
    // Core components
    private static SearchBridge searchBridge;
    private static ServiceBridge serviceBridge;
    private static Label statusLabel;
    private static AutoCompleteTextBox searchBox;
    private static List<SearchBridge.SearchContext> searchContexts;
    private static SearchBridge.SearchContext selectedContext = SearchBridge.SearchContext.GLOBAL;
    private static Box contextBox;
    private static List<WorkItem> searchResults = new ArrayList<>();
    private static List<String> searchHistory = new ArrayList<>();
    private static List<WorkItem> workItemList;
    private static TextArea detailsArea;
    
    /**
     * Entry point for the demo.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            // Initialize the bridges
            searchBridge = SearchBridge.getInstance();
            serviceBridge = ServiceBridge.getInstance();
            
            // Initialize search contexts
            searchContexts = Arrays.asList(
                SearchBridge.SearchContext.GLOBAL,
                SearchBridge.SearchContext.WORK_ITEM,
                SearchBridge.SearchContext.PERSON,
                SearchBridge.SearchContext.PROJECT,
                SearchBridge.SearchContext.STATE,
                SearchBridge.SearchContext.TYPE,
                SearchBridge.SearchContext.PRIORITY
            );
            
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
            
            // Create the search panel
            Container searchPanel = createSearchPanel();
            
            // Create the body with search results
            Container bodyContainer = createBody();
            
            // Create the footer
            Container footerContainer = createFooter();
            
            // Add components to the main container
            mainContainer.addComponent(headerContainer);
            mainContainer.addComponent(searchPanel);
            mainContainer.addComponent(bodyContainer);
            mainContainer.addComponent(footerContainer);
            
            // Create a custom theme
            Theme theme = Theme.createDefault();
            
            // Initialize the UI
            pui.initialize(mainLayout)
               .addComponent(mainContainer)
               .setTheme(theme);
            
            // Set initial focus to the search box
            pui.setFocusedComponent(searchBox);
            
            // Start the UI
            pui.start();
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates the header container with title.
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
        
        // Create title
        Label titleLabel = new Label("Rinna Context-Aware Search Interface");
        titleLabel.setAlignment(Label.Alignment.CENTER);
        
        // Add constraints to position the components
        BoxConstraints titleConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        headerLayout.setConstraints(titleLabel, titleConstraints);
        
        header.addComponent(titleLabel);
        
        return header;
    }
    
    /**
     * Creates the search panel with context selector and search box.
     * 
     * @return the search panel
     */
    private static Container createSearchPanel() {
        Container panel = new Container("search-panel");
        panel.setSize(new Dimension(100, 3));
        
        Style panelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        panel.setStyle(panelStyle);
        
        BoxLayout panelLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        panel.setLayout(panelLayout);
        
        // Create context selector label
        Label contextLabel = new Label("Search in:");
        
        // Create context selector
        List<String> contextNames = new ArrayList<>();
        for (SearchBridge.SearchContext context : searchContexts) {
            contextNames.add(context.name());
        }
        
        contextBox = new Box("context-selector", contextNames);
        contextBox.setSelectedItem(selectedContext.name());
        contextBox.addSelectionListener((box, selected) -> {
            selectedContext = SearchBridge.SearchContext.valueOf(selected);
            updateSearchBoxSuggestions();
            statusLabel.setText("Search context changed to: " + selected);
        });
        
        // Create search box label
        Label searchLabel = new Label("Search:");
        
        // Create search box with auto-completion
        searchBox = new AutoCompleteTextBox("search-box", 40);
        
        // Set up suggestion provider based on selected context
        updateSearchBoxSuggestions();
        
        // Set up selection handler
        searchBox.setSelectionHandler(suggestion -> {
            executeSearch(suggestion);
        });
        
        // Create search button
        Button searchButton = new Button("Search");
        searchButton.addClickListener(btn -> {
            String searchText = searchBox.getText();
            if (searchText != null && !searchText.isEmpty()) {
                executeSearch(searchText);
            }
        });
        
        // Add components with constraints
        BoxConstraints labelConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillHeight(true);
        panelLayout.setConstraints(contextLabel, labelConstraints);
        panelLayout.setConstraints(contextBox, labelConstraints);
        panelLayout.setConstraints(searchLabel, labelConstraints);
        
        BoxConstraints searchBoxConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        panelLayout.setConstraints(searchBox, searchBoxConstraints);
        
        BoxConstraints buttonConstraints = new BoxConstraints()
            .setWeight(0)
            .setFillHeight(true);
        panelLayout.setConstraints(searchButton, buttonConstraints);
        
        panel.addComponent(contextLabel);
        panel.addComponent(contextBox);
        panel.addComponent(searchLabel);
        panel.addComponent(searchBox);
        panel.addComponent(searchButton);
        
        return panel;
    }
    
    /**
     * Creates the body container with search results.
     * 
     * @return the body container
     */
    private static Container createBody() {
        Container body = new Container("body");
        body.setSize(new Dimension(100, 21));
        
        BoxLayout bodyLayout = new BoxLayout(Orientation.HORIZONTAL, 1);
        body.setLayout(bodyLayout);
        
        // Create the left panel with search results and history
        Container leftPanel = createLeftPanel();
        
        // Create the right panel with details
        Container rightPanel = createRightPanel();
        
        // Add panels to body with constraints
        BoxConstraints leftConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        bodyLayout.setConstraints(leftPanel, leftConstraints);
        
        BoxConstraints rightConstraints = new BoxConstraints()
            .setWeight(1)
            .setFillHeight(true);
        bodyLayout.setConstraints(rightPanel, rightConstraints);
        
        body.addComponent(leftPanel);
        body.addComponent(rightPanel);
        
        return body;
    }
    
    /**
     * Creates the left panel with search results and history.
     * 
     * @return the left panel
     */
    private static Container createLeftPanel() {
        Container panel = new Container("left-panel");
        
        Style panelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        panel.setStyle(panelStyle);
        
        BoxLayout panelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        panel.setLayout(panelLayout);
        
        // Create search results section
        Label resultsHeader = new Label("Search Results");
        Style headerStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        resultsHeader.setStyle(headerStyle);
        
        // Create the results list
        workItemList = new List<>("results-list", 15);
        workItemList.setItemRenderer(item -> {
            if (item == null) return "No results";
            String id = item.getId().length() > 8 ? item.getId().substring(0, 8) : item.getId();
            String title = item.getTitle();
            String type = item.getType() != null ? item.getType().name() : "";
            String state = item.getState() != null ? item.getState().name() : "";
            
            return String.format("%-8s %-8s %-10s %s", 
                id, 
                type,
                state,
                title.length() > 30 ? title.substring(0, 27) + "..." : title);
        });
        
        // Add selection listener
        workItemList.addSelectionListener((list, selected) -> {
            updateDetailsArea(selected);
        });
        
        // Create search history section
        Label historyHeader = new Label("Search History");
        historyHeader.setStyle(headerStyle);
        
        // Create history list
        List<String> historyList = new List<>("history-list", 3);
        
        // Add selection listener to re-run searches from history
        historyList.addSelectionListener((list, selected) -> {
            if (selected != null && !selected.isEmpty()) {
                searchBox.setText(selected);
                executeSearch(selected);
            }
        });
        
        // Add components to panel
        panel.addComponent(resultsHeader);
        panel.addComponent(workItemList);
        panel.addComponent(historyHeader);
        panel.addComponent(historyList);
        
        return panel;
    }
    
    /**
     * Creates the right panel with details view.
     * 
     * @return the right panel
     */
    private static Container createRightPanel() {
        Container panel = new Container("right-panel");
        
        Style panelStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        panel.setStyle(panelStyle);
        
        BoxLayout panelLayout = new BoxLayout(Orientation.VERTICAL, 1);
        panel.setLayout(panelLayout);
        
        // Create details header
        Label detailsHeader = new Label("Item Details");
        Style headerStyle = new Style()
            .setBold(true)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        detailsHeader.setStyle(headerStyle);
        
        // Create details area
        detailsArea = new TextArea("details-area", 18, 45);
        detailsArea.setReadOnly(true);
        
        // Add help text
        detailsArea.setText("Select a search result to view details.\n\n" +
                           "Search Tips:\n" +
                           "- Use different search contexts for targeted results\n" +
                           "- Press TAB to select from suggestions\n" +
                           "- Use arrow keys to navigate suggestions\n" +
                           "- Press Enter to execute the search\n" +
                           "- Click on history items to re-run searches");
        
        // Add components to panel
        panel.addComponent(detailsHeader);
        panel.addComponent(detailsArea);
        
        return panel;
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
        
        statusLabel = new Label("Ready to search. Enter a query above.");
        Label helpLabel = new Label("ESC: Exit  TAB: Navigate  ↑↓: Select Suggestion  ENTER: Search");
        
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
     * Updates the search box suggestions based on the selected context.
     */
    private static void updateSearchBoxSuggestions() {
        // Create new suggestion provider based on selected context
        Function<String, List<String>> provider = searchBridge.createSuggestionProvider(selectedContext);
        searchBox.setSuggestionProvider(provider);
    }
    
    /**
     * Executes a search with the given text.
     * 
     * @param searchText the search text
     */
    private static void executeSearch(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return;
        }
        
        // Update status
        statusLabel.setText("Searching for: " + searchText + " in context: " + selectedContext.name());
        
        // Execute the search
        searchResults = searchBridge.search(searchText, selectedContext);
        
        // Update results list
        workItemList.setItems(searchResults);
        
        // Add to search history
        addToSearchHistory(searchText);
        
        // Update status with results count
        statusLabel.setText("Found " + searchResults.size() + " results for: " + searchText + 
                           " in context: " + selectedContext.name());
        
        // Clear details if no results
        if (searchResults.isEmpty()) {
            detailsArea.setText("No results found for: " + searchText + 
                              " in context: " + selectedContext.name() + "\n\n" +
                              "Try changing the search context or using different search terms.");
        } else if (searchResults.size() == 1) {
            // If only one result, select it automatically
            workItemList.setSelectedIndex(0);
        }
    }
    
    /**
     * Adds a search to the history.
     * 
     * @param searchText the search text
     */
    private static void addToSearchHistory(String searchText) {
        // Remove if already exists
        searchHistory.remove(searchText);
        
        // Add to beginning of list
        searchHistory.add(0, searchText);
        
        // Limit history size
        if (searchHistory.size() > 10) {
            searchHistory.remove(searchHistory.size() - 1);
        }
        
        // Update history list
        List historyComponent = (List) ((Container) ((Container) 
            RinnaPUI.getInstance().getComponent("main").getComponent(2)
            .getComponent(0)).getComponent(3));
        
        historyComponent.setItems(searchHistory);
    }
    
    /**
     * Updates the details area with the selected work item.
     * 
     * @param item the selected work item
     */
    private static void updateDetailsArea(WorkItem item) {
        if (item == null) {
            detailsArea.setText("No item selected");
            return;
        }
        
        StringBuilder details = new StringBuilder();
        
        details.append("ID: ").append(item.getId()).append("\n\n");
        details.append("Title: ").append(item.getTitle()).append("\n\n");
        
        details.append("Type: ").append(item.getType()).append("\n");
        details.append("Priority: ").append(item.getPriority()).append("\n");
        details.append("State: ").append(item.getState()).append("\n\n");
        
        details.append("Assignee: ").append(item.getAssignee() != null ? item.getAssignee() : "Unassigned").append("\n");
        details.append("Reporter: ").append(item.getReporter() != null ? item.getReporter() : "Unknown").append("\n\n");
        
        if (item.getCreated() != null) {
            details.append("Created: ").append(formatDateTime(item.getCreated())).append("\n");
        }
        
        if (item.getUpdated() != null) {
            details.append("Updated: ").append(formatDateTime(item.getUpdated())).append("\n");
        }
        
        details.append("\nDescription:\n").append(item.getDescription() != null ? 
                                               item.getDescription() : "No description provided");
        
        detailsArea.setText(details.toString());
    }
    
    /**
     * Formats a LocalDateTime for display.
     * 
     * @param dateTime the date time to format
     * @return the formatted string
     */
    private static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}