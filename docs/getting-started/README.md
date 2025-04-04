<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford], Gemini Deep Research, Claude 3.7. -->

# Getting Started with Rinna

*Note: As of version 1.1.0, Rinna requires Java 21+*

## Installation

### Maven Dependency

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.1.0</version>
</dependency>
```

### CLI Installation

```bash
# Clone the repository
git clone https://github.com/heymumford/rinna.git

# Build the project
cd rinna
mvn clean install

# Make scripts executable
chmod +x bin/rin bin/rin-version
```

## Quick Start

### Development Commands

```bash
# Clean, build, and test the project
bin/rin all

# Run tests with verbose output
bin/rin -v test

# Build with errors-only output
bin/rin -e build
```

### Workflow Management

```bash
# Create a feature
rin workflow create feature "Add user authentication"

# List all items
rin workflow list

# Update item status
rin workflow update ITEM-1 --status "In Progress"
```

See the [User Guide](../user-guide/README.md) for more commands and options.

## Java 21 Features

Rinna leverages modern Java 21 features to provide a clean, maintainable API. When integrating with Rinna, you can take advantage of:

```java
// Using records for DTOs
record WorkItemDTO(UUID id, String title, WorkItemType type, WorkflowState status) {
    // Factory method using pattern matching
    static WorkItemDTO from(Object item) {
        return switch(item) {
            case WorkItem wi -> new WorkItemDTO(wi.getId(), wi.getTitle(), wi.getType(), wi.getStatus());
            case null -> throw new IllegalArgumentException("Item cannot be null");
            default -> throw new IllegalArgumentException("Unknown item type");
        };
    }
}

// Processing items with pattern matching
String getItemSummary(WorkItem item) {
    return switch(item) {
        case var i when i.getType() == WorkItemType.BUG && i.getPriority() == Priority.HIGH ->
            STR."High priority bug: \{i.getTitle()}";
        case var i when i.getStatus() == WorkflowState.IN_PROGRESS -> 
            STR."In progress: \{i.getTitle()}";
        default -> item.getTitle();
    };
}
```

For more examples of how Rinna uses Java 21 features, see our [Java 21 Code Examples](../development/java21-examples.md).
