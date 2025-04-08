<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Getting Started with Rinna

*Note: As of version 1.8.0, Rinna requires Java 21+*

## Installation

### Prerequisites

Before installing Rinna, make sure you have the following prerequisites:

- Java 21+ (OpenJDK recommended)
- Go 1.21+ (for API server)
- Maven 3.8+
- `jq` for CLI client
- Python 3.8+ (for advanced features)

### Unified Setup (Recommended)

Rinna provides a unified setup script that automatically configures all components:

```bash
# Full setup with all components
bin/rin-setup-unified  

# For specific components only
bin/rin-setup-unified java go python

# For minimal setup
bin/rin-setup-unified --minimal

# For faster, non-interactive setup
bin/rin-setup-unified --fast
```

### Manual Installation

```bash
# Clone the repository
git clone https://github.com/heymumford/rinna.git

# Build the project
cd rinna
mvn clean install

# Make scripts executable
chmod +x bin/rin bin/rin-version bin/rin-build bin/rin-test
```

### Maven Dependency

To include Rinna core in your Maven project:

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.8.0</version>
</dependency>
```

## Quick Start

### Activating the Environment

```bash
# Activate all environments in one step (recommended)
source activate-rinna.sh

# Or activate individual components
source activate-java.sh      # Activate Java environment
source activate-api.sh       # Activate API service environment
source activate-python.sh    # Activate Python environment
```

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
bin/rin-cli add "Add user authentication" --type=FEATURE

# List all items
bin/rin-cli list

# Update item status
bin/rin-cli update ITEM-1 --status=IN_PROGRESS

# View details of a work item
bin/rin-cli view ITEM-1
```

### Test-Driven Development (TDD)

Rinna provides comprehensive support for Test-Driven Development across all testing layers:

```bash
# Run the TDD feature tests
bin/rin test --tag=tdd

# Run unit tests
bin/rin test unit

# Run only positive TDD scenarios
bin/rin test --tag=tdd --tag=positive

# Run only negative TDD scenarios
bin/rin test --tag=tdd --tag=negative
```

### Build System

The rin build tool supports a range of development workflows:

```bash
# Quick iterations during development
bin/rin build fast

# Build with tests
bin/rin build test

# Run tests at specific levels
bin/rin test unit
bin/rin test component
bin/rin test integration
bin/rin test acceptance
bin/rin test performance

# Run tests in parallel with coverage
bin/rin test --parallel --coverage

# Monitor file changes and run tests automatically
bin/rin test --watch
```

## Using the API

Rinna provides a Go-based API server:

```bash
# Start the API server
bin/rin server start

# Create a work item via API
curl -X POST "http://localhost:8080/api/v1/workitems" \
  -H "Authorization: Bearer ri-dev-token" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement payment gateway",
    "type": "FEATURE",
    "priority": "HIGH"
  }'

# Get all work items
curl -X GET "http://localhost:8080/api/v1/workitems" \
  -H "Authorization: Bearer ri-dev-token"
```

## Java 21 Features

Rinna leverages modern Java 21 features to provide a clean, maintainable API:

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

## Next Steps

1. Read the [User Guide](../user-guide/README.md) for more commands and options
2. Explore the [CLI Documentation](../user-guide/rin-cli.md) for detailed command reference
3. Learn about [Test-Driven Development](../testing/TEST_PYRAMID.md) in Rinna
4. Set up [Document Generation](../user-guide/documents.md) for reports and documentation
5. Understand the [Architecture](../development/architecture.md) for extending Rinna

For developers wanting to contribute to Rinna, see our [Development Guide](../development/README.md) for details on our architecture, build system, and development workflow.