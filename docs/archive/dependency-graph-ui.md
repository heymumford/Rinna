# Dependency Graph UI Guide

This document provides information about the Pragmatic User Interface (PUI) components developed for dependency graph visualization in the Rinna system, particularly focusing on the DependencyGraphView component and its integration with work item relationships.

## Overview

The Dependency Graph UI allows users to:

1. Visualize relationships between work items in an interactive graph
2. Navigate through complex work item relationship networks
3. Filter relationships by type (parent-child, blocks, etc.)
4. Expand and collapse nodes to explore the graph at different levels
5. Adjust the navigation depth to see more or fewer relationships
6. Select work items directly from the graph

This implementation follows Rinna's PUI (Pragmatic User Interface) design principles, emphasizing efficiency and developer-centric operations.

## Components

### DependencyGraphView

The `DependencyGraphView` component is the central element for visualizing and navigating work item relationships. It provides:

- Visual representation of work items as nodes in a graph
- Display of relationships as edges between nodes
- Interactive navigation through keyboard controls
- Ability to filter relationships by type
- Adjustable navigation depth
- Node expansion/collapse functionality
- Selection and focus capabilities

```java
// Create a dependency graph view
DependencyGraphView dependencyGraph = new DependencyGraphView("graph-id", width, height);

// Set the data for the graph
dependencyGraph.setData(focusItem, allWorkItems, allRelationships);

// Set up node label provider
dependencyGraph.setNodeLabelProvider(item -> {
    return String.format("%s: %s", item.getId(), item.getTitle());
});

// Set up edge label provider
dependencyGraph.setEdgeLabelProvider(relationship -> {
    return relationship.getRelationshipType().toString();
});

// Set up node selection handler
dependencyGraph.setNodeSelectionHandler(item -> {
    // Handle the selection
});

// Set visible relationship types
Set<RelationshipType> visibleTypes = new HashSet<>();
visibleTypes.add(RelationshipType.PARENT);
visibleTypes.add(RelationshipType.BLOCKS);
dependencyGraph.setVisibleRelationshipTypes(visibleTypes);

// Set navigation depth
dependencyGraph.setNavigationDepth(2);
```

### WorkItemRelationship

The `WorkItemRelationship` class models relationships between work items, with support for:

- Various relationship types (parent, child, blocks, blocked by, etc.)
- Bidirectional relationships with complementary creation
- Relationship descriptions
- String representation for display

```java
// Create a relationship
WorkItemRelationship relationship = new WorkItemRelationship(sourceItem, targetItem, RelationshipType.BLOCKS);

// Create its complementary relationship
WorkItemRelationship complementary = relationship.createComplementaryRelationship();
// Result: targetItem BLOCKED_BY sourceItem
```

### Dependency Graph Demo

The `DependencyGraphDemo` provides a comprehensive example of how to use the `DependencyGraphView` component in a complete application:

- Work item selection from a list
- Relationship visualization in an interactive graph
- Relationship filtering by type
- Work item details display
- Interactive navigation through the graph

## User Interaction

Users can interact with the dependency graph UI in the following ways:

1. Navigate through the graph using arrow keys
2. Expand or collapse nodes using the Space key
3. Adjust the navigation depth using + and - keys
4. Filter relationships by type using checkboxes
5. Select a node by pressing Enter
6. View details of the selected work item

## Integration with Services

The dependency graph UI integrates with Rinna's service layer through the `ServiceBridge` class, which provides:

- Access to work items through the `ItemService`
- Creation of work item relationships based on data models
- Visualization and navigation through the dependency graph

## Best Practices

When implementing dependency graph UIs, follow these best practices:

1. **Node Representation**: Use clear, concise labels for nodes to avoid clutter
2. **Edge Types**: Distinguish different relationship types through visual cues
3. **Incremental Exploration**: Allow users to gradually expand the graph to prevent overwhelming displays
4. **Filtering**: Provide filtering options to focus on specific relationship types
5. **Navigation Depth**: Allow users to control how deep they want to explore the graph
6. **Selection Feedback**: Provide clear visual feedback for the currently selected node
7. **Context Preservation**: Ensure users don't lose context when navigating the graph

## Running the Demo

To run the dependency graph demo, use the provided script:

```bash
./run-dependency-graph.sh
```

This will compile and run the `DependencyGraphDemo` class, showcasing the dependency graph UI in action.

## Customization

The dependency graph UI can be customized in several ways:

1. **Node Labels**: Modify the node label provider to change how work items are displayed
2. **Edge Labels**: Customize the edge label provider to change how relationships are shown
3. **Layout Algorithm**: Adjust the graph layout calculation for different visualization styles
4. **Relationship Filters**: Customize the filters for different relationship types
5. **Navigation Options**: Modify the keyboard handling for different navigation patterns

## Integration with Larger Applications

The `DependencyGraphView` component can be integrated into larger applications:

1. Use it as part of a multi-pane work item management UI
2. Combine it with the WorkflowStateView for comprehensive work item visualization
3. Integrate it with reporting tools for dependency analysis
4. Add it to project planning views for visualizing work item hierarchies

## Future Enhancements

Planned enhancements to the dependency graph UI include:

1. **Path Visualization**: Highlighting critical paths through the dependency network
2. **Circular Dependency Detection**: Identifying and highlighting circular dependencies
3. **Clustering**: Grouping related nodes for better visualization of large graphs
4. **Zoom and Pan**: Adding zoom and pan capabilities for larger graphs
5. **Dependency Impact Analysis**: Showing the impact of changing a work item's state on its dependencies
6. **Automatic Layout Optimization**: Improving the layout algorithm for better visualization