# API Integration

## Maven Dependency

Add Rinna to your project using Maven:

```xml
<dependency>
    <groupId>org.samstraumr</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Building and Testing

Use the Rinna CLI tool for simplified build and test operations:

```bash
# Clean and build the project
rin clean build

# Run tests with verbose output
rin -v test

# Full workflow with errors-only output
rin -e all
```

## Basic Integration

```java
// Initialize Rinna with SQLite storage
RinnaConfig config = RinnaConfig.builder()
    .withStorageProvider(new SQLiteStorageProvider("path/to/db"))
    .build();

Rinna rinna = Rinna.initialize(config);

// Create a new work item
WorkItem task = rinna.items().create(WorkItemCreateRequest.builder()
    .title("Implement feature X")
    .type(WorkItemType.TASK)
    .priority(Priority.MEDIUM)
    .build());

// Transition the item through workflow
rinna.workflow().transition(task.getId(), WorkflowState.IN_PROGRESS);
```

## Spring Integration

```java
@Configuration
public class RinnaConfig {
    @Bean
    public StorageProvider storageProvider() {
        return new JPAStorageProvider();
    }
    
    @Bean
    public Rinna rinna(StorageProvider storageProvider) {
        return Rinna.initialize(RinnaConfig.builder()
            .withStorageProvider(storageProvider)
            .build());
    }
}
```

## Core Services

### ItemService

```java
// Create new items
WorkItem feature = rinna.items().create(WorkItemCreateRequest.builder()
    .title("Add authentication")
    .type(WorkItemType.FEATURE)
    .build());

// Search for items
List<WorkItem> items = rinna.items().search(WorkItemSearchCriteria.builder()
    .type(WorkItemType.BUG)
    .status(WorkflowState.IN_PROGRESS)
    .build());
```

### WorkflowService

```java
// Check available transitions
List<WorkflowState> availableStates = rinna.workflow()
    .getAvailableTransitions(itemId);

// Progress through workflow
rinna.workflow().transition(itemId, WorkflowState.IN_TEST);
```

### ReleaseService

```java
// Create a new release
Release release = rinna.releases().createRelease(ReleaseCreateRequest.builder()
    .major(1)
    .minor(0)
    .patch(0)
    .build());

// Add items to release
rinna.releases().addItemToRelease(itemId, release.getId());
```
