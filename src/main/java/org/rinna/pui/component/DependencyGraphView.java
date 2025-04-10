/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.component;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import org.rinna.cli.model.WorkItem;
import org.rinna.pui.examples.model.WorkItemRelationship;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.input.KeyHandler;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;

/**
 * A component that visualizes work item dependencies and relationships.
 * This component renders a graph of work items and their relationships,
 * allowing for interactive exploration of dependencies.
 */
public class DependencyGraphView extends Container {
    
    // Core data
    private WorkItem focusedItem;
    private Map<String, WorkItem> workItems = new HashMap<>();
    private Map<String, List<WorkItemRelationship>> relationships = new HashMap<>();
    
    // Node representation
    private Map<String, Label> nodeLabels = new HashMap<>();
    private Map<String, Point> nodePositions = new HashMap<>();
    
    // Edge representation
    private List<Edge> edges = new ArrayList<>();
    
    // Navigation state
    private String selectedNodeId;
    private int navigationDepth = 1;
    private Set<String> expandedNodes = new HashSet<>();
    
    // Layout configurations
    private int nodeWidth = 20;
    private int nodeHeight = 3;
    private int horizontalSpacing = 10;
    private int verticalSpacing = 4;
    
    // Customization
    private Function<WorkItem, String> nodeLabelProvider;
    private Consumer<WorkItem> nodeSelectionHandler;
    private Function<WorkItemRelationship, String> edgeLabelProvider;
    
    // Status
    private Label statusLabel;
    private Container graphContainer;
    private Container legendContainer;
    
    // Relationship type filters
    private Set<WorkItemRelationship.RelationshipType> visibleRelationshipTypes = new HashSet<>();
    
    /**
     * Creates a new dependency graph view.
     *
     * @param id The component ID
     * @param width The component width
     * @param height The component height
     */
    public DependencyGraphView(String id, int width, int height) {
        super(id);
        setSize(new Dimension(width, height));
        
        // Create a border around the component
        Style style = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        setStyle(style);
        
        // Initialize the component with a vertical box layout
        setLayout(new BoxLayout(BoxLayout.Orientation.VERTICAL, 1));
        
        // Create status label
        statusLabel = new Label("Select nodes to explore dependencies");
        statusLabel.setAlignment(Label.Alignment.CENTER);
        addComponent(statusLabel);
        
        // Create a container for the graph visualization
        graphContainer = new Container("graph-container");
        graphContainer.setSize(new Dimension(width - 2, height - 7));
        addComponent(graphContainer);
        
        // Create a container for the legend
        legendContainer = createLegendContainer();
        addComponent(legendContainer);
        
        // Set default customizations
        nodeLabelProvider = this::defaultNodeLabel;
        edgeLabelProvider = this::defaultEdgeLabel;
        
        // Enable all relationship types by default
        for (WorkItemRelationship.RelationshipType type : WorkItemRelationship.RelationshipType.values()) {
            visibleRelationshipTypes.add(type);
        }
    }
    
    /**
     * Sets the focus work item and its relationships.
     *
     * @param focusItem The central work item
     * @param allItems All work items in the system
     * @param allRelationships All relationships in the system
     * @return This component for method chaining
     */
    public DependencyGraphView setData(WorkItem focusItem, List<WorkItem> allItems, List<WorkItemRelationship> allRelationships) {
        this.focusedItem = focusItem;
        this.selectedNodeId = focusItem.getId();
        
        // Clear previous data
        workItems.clear();
        relationships.clear();
        expandedNodes.clear();
        
        // Add the focus item to expanded nodes
        expandedNodes.add(focusItem.getId());
        
        // Load work items
        for (WorkItem item : allItems) {
            workItems.put(item.getId(), item);
        }
        
        // Load relationships
        for (WorkItemRelationship relationship : allRelationships) {
            String sourceId = relationship.getSourceItem().getId();
            if (!relationships.containsKey(sourceId)) {
                relationships.put(sourceId, new ArrayList<>());
            }
            relationships.get(sourceId).add(relationship);
        }
        
        // Update the graph visualization
        updateGraph();
        
        return this;
    }
    
    /**
     * Sets the function for providing node labels.
     *
     * @param provider The function that converts a work item to a label
     * @return This component for method chaining
     */
    public DependencyGraphView setNodeLabelProvider(Function<WorkItem, String> provider) {
        this.nodeLabelProvider = provider;
        return this;
    }
    
    /**
     * Sets the function for providing edge labels.
     *
     * @param provider The function that converts a relationship to a label
     * @return This component for method chaining
     */
    public DependencyGraphView setEdgeLabelProvider(Function<WorkItemRelationship, String> provider) {
        this.edgeLabelProvider = provider;
        return this;
    }
    
    /**
     * Sets the handler for node selection events.
     *
     * @param handler The handler for node selection events
     * @return This component for method chaining
     */
    public DependencyGraphView setNodeSelectionHandler(Consumer<WorkItem> handler) {
        this.nodeSelectionHandler = handler;
        return this;
    }
    
    /**
     * Sets the visible relationship types.
     *
     * @param types The set of visible relationship types
     * @return This component for method chaining
     */
    public DependencyGraphView setVisibleRelationshipTypes(Set<WorkItemRelationship.RelationshipType> types) {
        this.visibleRelationshipTypes = types;
        updateGraph();
        return this;
    }
    
    /**
     * Toggles a relationship type's visibility.
     *
     * @param type The relationship type to toggle
     * @return true if the type is now visible, false otherwise
     */
    public boolean toggleRelationshipType(WorkItemRelationship.RelationshipType type) {
        if (visibleRelationshipTypes.contains(type)) {
            visibleRelationshipTypes.remove(type);
            return false;
        } else {
            visibleRelationshipTypes.add(type);
            return true;
        }
    }
    
    /**
     * Sets the navigation depth for the graph.
     *
     * @param depth The maximum depth to display
     * @return This component for method chaining
     */
    public DependencyGraphView setNavigationDepth(int depth) {
        this.navigationDepth = Math.max(1, depth);
        updateGraph();
        return this;
    }
    
    /**
     * Updates the graph visualization based on the current data and state.
     */
    private void updateGraph() {
        // Clear the graph container
        graphContainer.removeAllComponents();
        nodeLabels.clear();
        nodePositions.clear();
        edges.clear();
        
        // If no focus item, display a message
        if (focusedItem == null) {
            Label noDataLabel = new Label("No work item selected");
            noDataLabel.setAlignment(Label.Alignment.CENTER);
            graphContainer.addComponent(noDataLabel);
            return;
        }
        
        // Calculate the layout
        calculateGraphLayout();
        
        // Create the node labels
        for (Map.Entry<String, Point> entry : nodePositions.entrySet()) {
            String nodeId = entry.getKey();
            Point position = entry.getValue();
            WorkItem item = workItems.get(nodeId);
            
            if (item != null) {
                // Create the node label
                String labelText = nodeLabelProvider.apply(item);
                Label nodeLabel = new Label(labelText);
                
                // Set position
                nodeLabel.setPosition(position);
                
                // Apply style based on selection state
                Style nodeStyle = new Style();
                if (nodeId.equals(selectedNodeId)) {
                    nodeStyle.setBackground(Color.GREEN).setForeground(Color.BLACK).setBold(true);
                } else if (nodeId.equals(focusedItem.getId())) {
                    nodeStyle.setBackground(Color.BLUE).setForeground(Color.WHITE).setBold(true);
                } else if (expandedNodes.contains(nodeId)) {
                    nodeStyle.setBackground(Color.YELLOW).setForeground(Color.BLACK);
                }
                nodeLabel.setStyle(nodeStyle);
                
                // Add to the graph container
                graphContainer.addComponent(nodeLabel);
                nodeLabels.put(nodeId, nodeLabel);
            }
        }
        
        // Create the edge labels
        for (Edge edge : edges) {
            if (edge.relationship != null && isRelationshipVisible(edge.relationship.getRelationshipType())) {
                // Create edge representation (simplified for terminal)
                Label edgeLabel = new Label(edgeLabelProvider.apply(edge.relationship));
                edgeLabel.setPosition(edge.labelPosition);
                
                // Set edge style
                Style edgeStyle = new Style().setForeground(Color.GRAY);
                edgeLabel.setStyle(edgeStyle);
                
                // Add to the graph container
                graphContainer.addComponent(edgeLabel);
            }
        }
        
        // Update status message
        if (selectedNodeId != null && workItems.containsKey(selectedNodeId)) {
            WorkItem selected = workItems.get(selectedNodeId);
            statusLabel.setText("Selected: " + selected.getId() + " - " + selected.getTitle());
        } else {
            statusLabel.setText("Navigate with arrow keys, Space to expand/collapse, +/- to change depth");
        }
    }
    
    /**
     * Calculates the graph layout with positions for all nodes and edges.
     */
    private void calculateGraphLayout() {
        // Start with the focused node in the center
        Set<String> processedNodes = new HashSet<>();
        Map<String, Integer> nodeDepths = new HashMap<>();
        
        // Set the focus node at depth 0
        nodeDepths.put(focusedItem.getId(), 0);
        
        // Calculate depths for all connected nodes (BFS)
        Queue<String> queue = new LinkedList<>();
        queue.add(focusedItem.getId());
        
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            if (processedNodes.contains(currentId)) continue;
            
            int currentDepth = nodeDepths.get(currentId);
            processedNodes.add(currentId);
            
            // Only process further if within navigation depth or node is expanded
            if (currentDepth < navigationDepth || expandedNodes.contains(currentId)) {
                // Add relationships from this node
                if (relationships.containsKey(currentId)) {
                    for (WorkItemRelationship relationship : relationships.get(currentId)) {
                        String targetId = relationship.getTargetItem().getId();
                        
                        // Only consider visible relationship types
                        if (isRelationshipVisible(relationship.getRelationshipType())) {
                            if (!nodeDepths.containsKey(targetId)) {
                                nodeDepths.put(targetId, currentDepth + 1);
                                queue.add(targetId);
                            }
                            
                            // Create an edge
                            edges.add(new Edge(currentId, targetId, relationship));
                        }
                    }
                }
            }
        }
        
        // Position nodes based on their depth and relationships
        Map<Integer, List<String>> nodesByDepth = new HashMap<>();
        
        // Group nodes by depth
        for (Map.Entry<String, Integer> entry : nodeDepths.entrySet()) {
            int depth = entry.getValue();
            if (!nodesByDepth.containsKey(depth)) {
                nodesByDepth.put(depth, new ArrayList<>());
            }
            nodesByDepth.get(depth).add(entry.getKey());
        }
        
        // Calculate positions for each node
        int centerX = graphContainer.getWidth() / 2;
        int centerY = graphContainer.getHeight() / 2;
        
        // Place the focus node
        nodePositions.put(focusedItem.getId(), new Point(centerX - nodeWidth / 2, centerY - nodeHeight / 2));
        
        // Place the other nodes by depth
        for (int depth = 1; depth <= navigationDepth; depth++) {
            List<String> nodesAtDepth = nodesByDepth.getOrDefault(depth, Collections.emptyList());
            int nodeCount = nodesAtDepth.size();
            
            if (nodeCount > 0) {
                // Calculate the layout based on depth
                if (depth == 1) {
                    // First level nodes go in a circle around the focus node
                    placeNodesInCircle(nodesAtDepth, centerX, centerY, nodeWidth * 2);
                } else {
                    // Higher level nodes form wider circles
                    placeNodesInCircle(nodesAtDepth, centerX, centerY, nodeWidth * 2 + (depth - 1) * nodeWidth * 1.5);
                }
            }
        }
        
        // Calculate edge label positions
        for (Edge edge : edges) {
            if (nodePositions.containsKey(edge.sourceId) && nodePositions.containsKey(edge.targetId)) {
                Point sourcePos = nodePositions.get(edge.sourceId);
                Point targetPos = nodePositions.get(edge.targetId);
                
                // Place edge label at the midpoint
                int midX = (sourcePos.getX() + targetPos.getX()) / 2;
                int midY = (sourcePos.getY() + targetPos.getY()) / 2;
                
                edge.labelPosition = new Point(midX, midY);
            }
        }
    }
    
    /**
     * Places nodes in a circle around a center point.
     *
     * @param nodeIds The list of node IDs to place
     * @param centerX The X coordinate of the center
     * @param centerY The Y coordinate of the center
     * @param radius The radius of the circle
     */
    private void placeNodesInCircle(List<String> nodeIds, int centerX, int centerY, double radius) {
        int nodeCount = nodeIds.size();
        
        for (int i = 0; i < nodeCount; i++) {
            // Calculate angle and position
            double angle = 2 * Math.PI * i / nodeCount;
            int x = (int) (centerX + radius * Math.cos(angle) - nodeWidth / 2);
            int y = (int) (centerY + radius * Math.sin(angle) - nodeHeight / 2);
            
            // Set the position
            nodePositions.put(nodeIds.get(i), new Point(x, y));
        }
    }
    
    /**
     * Checks if a relationship type is currently visible.
     *
     * @param type The relationship type to check
     * @return true if the type is visible, false otherwise
     */
    private boolean isRelationshipVisible(WorkItemRelationship.RelationshipType type) {
        return visibleRelationshipTypes.contains(type);
    }
    
    /**
     * Creates a default node label.
     *
     * @param item The work item
     * @return The node label
     */
    private String defaultNodeLabel(WorkItem item) {
        if (item == null) return "Unknown";
        String id = item.getId().length() > 8 ? item.getId().substring(0, 8) : item.getId();
        String title = item.getTitle();
        if (title.length() > nodeWidth - 4) {
            title = title.substring(0, nodeWidth - 7) + "...";
        }
        return String.format("%s:%s", id, title);
    }
    
    /**
     * Creates a default edge label.
     *
     * @param relationship The work item relationship
     * @return The edge label
     */
    private String defaultEdgeLabel(WorkItemRelationship relationship) {
        if (relationship == null) return "—";
        return relationship.getRelationshipType().toString().substring(0, 1); // Just use the first letter for simplicity
    }
    
    /**
     * Creates the legend container with relationship type information.
     *
     * @return The legend container
     */
    private Container createLegendContainer() {
        Container legend = new Container("legend");
        legend.setSize(new Dimension(getWidth() - 2, 3));
        
        BoxLayout legendLayout = new BoxLayout(BoxLayout.Orientation.VERTICAL, 0);
        legend.setLayout(legendLayout);
        
        // Create a header
        Label legendHeader = new Label("Relationship Types");
        Style headerStyle = new Style().setBold(true);
        legendHeader.setStyle(headerStyle);
        legend.addComponent(legendHeader);
        
        // Create a container for the relationship types
        Container typesContainer = new Container("types-container");
        BoxLayout typesLayout = new BoxLayout(BoxLayout.Orientation.HORIZONTAL, 2);
        typesContainer.setLayout(typesLayout);
        
        // Add relationship types
        for (WorkItemRelationship.RelationshipType type : WorkItemRelationship.RelationshipType.values()) {
            String abbreviation = type.toString().substring(0, 1);
            Label typeLabel = new Label(abbreviation + ": " + type.toString());
            typesContainer.addComponent(typeLabel);
        }
        
        legend.addComponent(typesContainer);
        
        // Add help text
        Label helpLabel = new Label("Navigate with arrows, Space to expand/collapse, +/- to change depth");
        legend.addComponent(helpLabel);
        
        return legend;
    }
    
    /**
     * Handles key events for navigation and interaction.
     *
     * @param keyCode The key code
     * @param keyChar The key character
     * @param modifiers Key modifiers
     * @return true if the key was handled, false otherwise
     */
    @Override
    public boolean handleKeyEvent(int keyCode, char keyChar, int modifiers) {
        // Navigation with arrow keys
        if (keyCode == KeyHandler.KEY_UP || keyCode == KeyHandler.KEY_DOWN || 
            keyCode == KeyHandler.KEY_LEFT || keyCode == KeyHandler.KEY_RIGHT) {
            
            navigateToNextNode(keyCode);
            return true;
        }
        
        // Expand/collapse with space
        if (keyCode == KeyHandler.KEY_SPACE) {
            if (selectedNodeId != null) {
                if (expandedNodes.contains(selectedNodeId)) {
                    expandedNodes.remove(selectedNodeId);
                } else {
                    expandedNodes.add(selectedNodeId);
                }
                updateGraph();
            }
            return true;
        }
        
        // Increase depth with +
        if (keyChar == '+' || keyChar == '=') {
            setNavigationDepth(navigationDepth + 1);
            return true;
        }
        
        // Decrease depth with -
        if (keyChar == '-' || keyChar == '_') {
            setNavigationDepth(Math.max(1, navigationDepth - 1));
            return true;
        }
        
        // Call node selection handler when Enter is pressed
        if (keyCode == KeyHandler.KEY_ENTER) {
            if (selectedNodeId != null && workItems.containsKey(selectedNodeId) && nodeSelectionHandler != null) {
                nodeSelectionHandler.accept(workItems.get(selectedNodeId));
            }
            return true;
        }
        
        // Toggle relationship types with number keys
        if (keyChar >= '1' && keyChar <= '9') {
            int index = keyChar - '1';
            WorkItemRelationship.RelationshipType[] types = WorkItemRelationship.RelationshipType.values();
            if (index < types.length) {
                toggleRelationshipType(types[index]);
                updateGraph();
                return true;
            }
        }
        
        return super.handleKeyEvent(keyCode, keyChar, modifiers);
    }
    
    /**
     * Navigates to the next node in the direction of the arrow key.
     *
     * @param keyCode The key code for the arrow key
     */
    private void navigateToNextNode(int keyCode) {
        if (selectedNodeId == null || nodePositions.isEmpty()) {
            selectedNodeId = focusedItem.getId();
            updateGraph();
            return;
        }
        
        Point currentPos = nodePositions.get(selectedNodeId);
        if (currentPos == null) return;
        
        String closestNodeId = null;
        double closestDistance = Double.MAX_VALUE;
        
        // Find the closest node in the direction of the arrow key
        for (Map.Entry<String, Point> entry : nodePositions.entrySet()) {
            String nodeId = entry.getKey();
            Point nodePos = entry.getValue();
            
            if (nodeId.equals(selectedNodeId)) continue;
            
            boolean isInDirection = false;
            
            // Check if the node is in the direction of the arrow key
            switch (keyCode) {
                case KeyHandler.KEY_UP:
                    isInDirection = nodePos.getY() < currentPos.getY();
                    break;
                case KeyHandler.KEY_DOWN:
                    isInDirection = nodePos.getY() > currentPos.getY();
                    break;
                case KeyHandler.KEY_LEFT:
                    isInDirection = nodePos.getX() < currentPos.getX();
                    break;
                case KeyHandler.KEY_RIGHT:
                    isInDirection = nodePos.getX() > currentPos.getX();
                    break;
            }
            
            if (isInDirection) {
                // Calculate the distance
                double dx = nodePos.getX() - currentPos.getX();
                double dy = nodePos.getY() - currentPos.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                // For Up/Down, prioritize vertical movement
                if (keyCode == KeyHandler.KEY_UP || keyCode == KeyHandler.KEY_DOWN) {
                    distance = Math.abs(dy) + Math.abs(dx) * 0.5;
                }
                // For Left/Right, prioritize horizontal movement
                else if (keyCode == KeyHandler.KEY_LEFT || keyCode == KeyHandler.KEY_RIGHT) {
                    distance = Math.abs(dx) + Math.abs(dy) * 0.5;
                }
                
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestNodeId = nodeId;
                }
            }
        }
        
        // Select the closest node in the direction
        if (closestNodeId != null) {
            selectedNodeId = closestNodeId;
            updateGraph();
        }
    }
    
    /**
     * Helper class to represent an edge in the graph.
     */
    private static class Edge {
        String sourceId;
        String targetId;
        WorkItemRelationship relationship;
        Point labelPosition;
        
        public Edge(String sourceId, String targetId, WorkItemRelationship relationship) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.relationship = relationship;
        }
    }
}