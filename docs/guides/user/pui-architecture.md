# Pragmatic User Interface (PUI) Architecture

The Pragmatic User Interface (PUI) is a terminal-based user interface framework designed for the Rinna application. This document describes the architecture and usage of the PUI system.

## Overview

The PUI is a component-based UI framework that provides a consistent, keyboard-centric interface for users. It is designed to be efficient, functional, and responsive while maintaining compatibility with various terminal environments.

## Core Architecture

### Component Model

The PUI is built around a component-based architecture:

- **Component**: The base interface that all UI elements implement
- **Container**: A component that can contain other components
- **Layout**: Defines how components are arranged within a container

This hierarchical structure allows for complex UI arrangements while maintaining clear organization.

### Rendering System

The PUI uses a buffer-based rendering approach:

1. Components are rendered to an in-memory buffer
2. The buffer is flushed to the terminal in a single operation
3. ANSI escape sequences are used for colors and formatting

This approach minimizes flickering and provides smooth visual updates.

### Input Handling

Input is managed through a dedicated KeyHandler:

- Captures raw input from the terminal
- Interprets escape sequences for special keys (arrows, function keys, etc.)
- Dispatches key events to focused components

### Style System

The PUI includes a comprehensive styling system:

- **Style**: Defines visual properties like colors, bold, italic, etc.
- **Theme**: A collection of styles for different component types
- **BorderStyle**: Defines border characters for components

### Layout Management

Components are arranged using layout managers:

- **BoxLayout**: Arranges components in a single row or column
- Constraints can be applied to control sizing and alignment

## Components

The PUI includes several core components:

### Implemented Components

- **Container**: A component that can contain other components
- **Label**: Displays static or dynamic text
- **Button**: A clickable button with a text label
- **TextBox**: An editable text field
- **List**: A scrollable list of items with selection capabilities
- **MillerColumnsContainer**: A multi-column hierarchical navigation component that displays each level of a hierarchical structure as adjacent columns

### Planned Components

- **ProgressBar**: Shows progress of an operation
- **Menu**: A dropdown or popup menu
- **Dialog**: A modal dialog box
- **Table**: A table with rows and columns for displaying structured data
- **Panel**: A panel with a title and content
- **Spinner**: A component for numeric input
- **CheckBox**: A component for boolean input
- **RadioButton**: A component for selecting one option from a group
- **Tab**: A tabbed interface for organizing content

## Usage

### Basic Setup

```java
// Create the main container
Container mainContainer = new Container("main");
mainContainer.setSize(new Dimension(80, 24));

// Create and initialize the UI
RinnaPUI pui = RinnaPUI.getInstance();
pui.initialize(new BoxLayout(Orientation.VERTICAL))
   .addComponent(mainContainer)
   .start();
```

### Component Creation and Styling

```java
// Create a container with a border
Container panel = new Container("panel");
panel.setSize(new Dimension(40, 10));

Style panelStyle = new Style()
    .setBorderStyle(BorderStyle.SINGLE)
    .setBackground(Color.BLUE)
    .setForeground(Color.WHITE);
panel.setStyle(panelStyle);

// Add to parent container
mainContainer.addComponent(panel);
```

### Layout Management

```java
// Create a box layout with vertical orientation and 1-character gap
BoxLayout layout = new BoxLayout(Orientation.VERTICAL, 1);
container.setLayout(layout);

// Add a component with constraints
BoxLayout.BoxConstraints constraints = new BoxLayout.BoxConstraints()
    .setWeight(1)
    .setFillWidth(true);
layout.setConstraints(component, constraints);
container.addComponent(component);
```

## Implementation Details

### Terminal Integration

The PUI framework uses ANSI escape sequences for terminal control:

- Sets the terminal to raw mode for direct key input
- Uses the alternate screen buffer to preserve the user's terminal state
- Handles terminal size changes and adapts the UI accordingly

### Performance Considerations

To maintain good performance:

- Rendering is done on a separate thread from input handling
- Components only update when their state changes
- Redraw is limited to ~30 FPS to reduce CPU usage

### Accessibility Features

The PUI includes basic accessibility support:

- High contrast themes
- Keyboard navigation for all functions
- Clear focus indicators

## Best Practices

1. **Use containers for logical grouping**: Organize related components in containers
2. **Apply consistent styling**: Use themes to maintain visual consistency
3. **Optimize for keyboard use**: Ensure all functions are accessible via keyboard
4. **Provide clear feedback**: Use visual cues to indicate focus and actions
5. **Be concise**: Use space efficiently to maximize information density

## Service Integration

The PUI system integrates with Rinna CLI services through a dedicated ServiceBridge class:

### ServiceBridge

The `org.rinna.pui.cli.ServiceBridge` provides a simplified interface for PUI components to interact with CLI services:

- Acts as a facade for various service implementations
- Handles operation tracking through the MetadataService
- Provides consistent error handling and formatting
- Converts domain objects to simplified Maps for UI consumption

### Key Features

- **Consistent Operation Tracking**: All service calls are tracked as operations
- **Error Handling**: Standardized error handling across all service calls
- **Data Normalization**: Converts domain objects to UI-friendly data structures
- **Security Filtering**: Automatically filters sensitive data from display

### Usage Example

```java
// Get the ServiceBridge instance
ServiceBridge serviceBridge = ServiceBridge.getInstance();

// Get all work items
List<WorkItem> workItems = serviceBridge.getAllWorkItems();

// Get operation history
List<Map<String, Object>> operations = serviceBridge.getRecentOperations(10);
```

## Example Applications

See the `org.rinna.pui.examples` package for working examples:

- `SimplePUIDemo`: Basic container layout demonstration with header, body, and footer
- `WorkItemViewDemo`: Demonstrates work item viewing and editing with a list view and detail panel
- `OperationsMonitorDemo`: Real-time monitor for system operations that demonstrates the ServiceBridge integration
- `WorkItemListDemo`: Advanced work item list with filtering, sorting, and details view
- `WorkItemDetailDemo`: Interactive work item detail view with Miller columns navigation for hierarchical relationships

You can run these examples using the provided scripts:

```bash
# Run the simple demo
./run-pui-demo.sh simple

# Run the work item demo
./run-pui-demo.sh workitem

# Run the operations monitor demo
./run-operations-monitor.sh

# Run the work item list demo
./run-workitem-list.sh

# Run the work item detail demo with Miller columns
./run-workitem-detail.sh
```

### Miller Columns Navigation

The `WorkItemDetailDemo` showcases hierarchical data navigation:

- **Multi-Column Navigation**: Navigate through hierarchical data using adjacent columns
- **Context Preservation**: Each column shows the children of the selected item in the previous column
- **Custom Item Renderers**: Format different item types appropriately in each column
- **Flexible Grouping**: Group items by type, status, priority, or other attributes
- **Relationship Visualization**: Explore relationships between work items (parent-child, blocks, etc.)
- **Dynamic Detail Panel**: View detailed information about any selected item
- **Keyboard Navigation**: Tab between columns, use arrow keys to select items

This style of navigation, popularized by Miller on the Mac, provides an intuitive way to explore complex hierarchical data structures like work item relationships.

### Advanced Work Item Management

The `WorkItemListDemo` showcases advanced work item management features:

- **Multi-criteria Filtering**: Filter work items by type, priority, status, and text search
- **Flexible Sorting**: Sort by any column in ascending or descending order
- **Detail View**: View and interact with work item details
- **Real-time Filtering**: Filters apply immediately as search criteria change
- **Responsive Layout**: Adapts to terminal size changes

This demonstrates how PUI components can be combined with the ServiceBridge to create
powerful, data-driven interfaces for managing work items in the terminal.

## Future Enhancements

- Mouse support for terminals that support it
- Animation system for smooth transitions
- Improved window management with floating windows
- Enhanced keyboard shortcut system
- Additional layout managers (GridLayout, FlowLayout, etc.)