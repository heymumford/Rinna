<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford], Gemini Deep Research, Claude 3.7. -->

# API Integration

## Maven Dependency

Add Rinna to your project using Maven:

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.1.0</version>
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
// Initialize Rinna with default configuration
Rinna rinna = Rinna.initialize();

// Create a new work item
WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
    .title("Implement feature X")
    .type(WorkItemType.TASK)
    .priority(Priority.MEDIUM)
    .build();
WorkItem task = rinna.items().create(request);

// Transition the item through workflow
rinna.workflow().transition(task.getId(), WorkflowState.IN_PROGRESS);

// Create a release and add the work item
Release release = rinna.releases().createRelease("1.0.0", "Initial release");
rinna.releases().addWorkItem(release.getId(), task.getId());
```

## Spring Integration

```java
@Configuration
public class RinnaConfiguration {
    @Bean
    public ItemRepository itemRepository() {
        return new JPAItemRepository();
    }
    
    @Bean
    public ReleaseRepository releaseRepository() {
        return new JPAReleaseRepository();
    }
    
    @Bean
    public ItemService itemService(ItemRepository itemRepository) {
        return new DefaultItemService(itemRepository);
    }
    
    @Bean
    public WorkflowService workflowService(ItemRepository itemRepository) {
        return new DefaultWorkflowService(itemRepository);
    }
    
    @Bean
    public ReleaseService releaseService(ReleaseRepository releaseRepository, ItemService itemService) {
        return new DefaultReleaseService(releaseRepository, itemService);
    }
    
    @Bean
    public Rinna rinna(ItemService itemService, WorkflowService workflowService, ReleaseService releaseService) {
        return new Rinna(itemService, workflowService, releaseService);
    }
}
```

## Core Services

### ItemService

```java
// Create new items
WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
    .title("Add authentication")
    .type(WorkItemType.FEATURE)
    .description("Implement user authentication and authorization")
    .priority(Priority.HIGH)
    .build();
WorkItem feature = rinna.items().create(request);

// Find items
Optional<WorkItem> foundItem = rinna.items().findById(itemId);
List<WorkItem> allItems = rinna.items().findAll();
List<WorkItem> bugs = rinna.items().findByType(WorkItemType.BUG.name());
List<WorkItem> inProgress = rinna.items().findByStatus(WorkflowState.IN_PROGRESS.name());
List<WorkItem> assignedItems = rinna.items().findByAssignee("john.doe");
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
Release release = rinna.releases().createRelease("1.0.0", "Initial release");

// Create next version releases
Release minorRelease = rinna.releases().createNextMinorVersion(releaseId, "New features");
Release patchRelease = rinna.releases().createNextPatchVersion(releaseId, "Bug fixes");
Release majorRelease = rinna.releases().createNextMajorVersion(releaseId, "Breaking changes");

// Manage release items
rinna.releases().addWorkItem(releaseId, itemId);
rinna.releases().removeWorkItem(releaseId, itemId);
boolean contains = rinna.releases().containsWorkItem(releaseId, itemId);
List<WorkItem> items = rinna.releases().getWorkItems(releaseId);

// Find releases
Optional<Release> foundRelease = rinna.releases().findById(releaseId);
Optional<Release> versionRelease = rinna.releases().findByVersion("1.0.0");
List<Release> allReleases = rinna.releases().findAll();
```
