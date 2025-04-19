# Java 21 Features in Rinna

Rinna leverages modern Java 21 features to create more concise, readable, and maintainable code. This guide explains how these features are used throughout the codebase.

## Key Java 21 Features

### 1. Record Classes

Records provide a compact syntax for classes that are primarily data carriers:

```java
// Before Java 21
public class WorkItemDTO {
    private final UUID id;
    private final String title;
    private final WorkItemType type;
    private final WorkflowState status;
    
    // Constructor, getters, equals, hashCode, toString...
}

// With Java 21 records
public record WorkItemDTO(
    UUID id,
    String title,
    WorkItemType type,
    WorkflowState status
) {
    // Factory methods and additional behavior only
}
```

Records are used for:
- Data Transfer Objects (DTOs)
- Value Objects
- Command/Request Objects
- Response Objects

### 2. Pattern Matching for Switch

Pattern matching simplifies conditional logic by combining type checking and destructuring:

```java
// Before Java 21
String getWorkItemSummary(Object item) {
    if (item instanceof DefaultWorkItem) {
        DefaultWorkItem workItem = (DefaultWorkItem) item;
        if (workItem.getType() == WorkItemType.BUG && workItem.getPriority() == Priority.HIGH) {
            return "High priority bug: " + workItem.getTitle() + " (" + workItem.getId() + ")";
        } else if (workItem.getType() == WorkItemType.FEATURE) {
            return "Feature: " + workItem.getTitle() + " (" + workItem.getId() + ")";
        } else {
            return "Work item: " + workItem.getTitle() + " (" + workItem.getId() + ")";
        }
    }
    return "Unknown item type";
}

// With Java 21 pattern matching
String getWorkItemSummary(Object item) {
    return switch (item) {
        case DefaultWorkItem(var id, var title, _, WorkItemType.BUG, var status, Priority.HIGH, var assignee, _, _, _) -> 
            STR."High priority bug: \{title} (\{id}, assigned to \{assignee}, status: \{status})";
            
        case DefaultWorkItem(var id, var title, _, WorkItemType.FEATURE, _, _, var assignee, _, _, _) -> 
            STR."Feature: \{title} (\{id}, assigned to \{assignee})";
            
        case DefaultWorkItem(var id, var title, _, _, _, _, _, _, _, _) -> 
            STR."Work item: \{title} (\{id})";
            
        default -> "Unknown item type";
    };
}
```

### 3. Virtual Threads

Virtual threads enable high concurrency with minimal resources:

```java
// Before Java 21 (thread pools)
ExecutorService executor = Executors.newFixedThreadPool(100);
try {
    for (UUID id : itemIds) {
        executor.submit(() -> processWorkItem(id));
    }
} finally {
    executor.shutdown();
}

// With Java 21 virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (UUID id : itemIds) {
        executor.submit(() -> processWorkItem(id));
    }
}
```

Virtual threads are used for:
- Concurrent processing of work items
- Asynchronous database operations
- Parallel API calls
- Background tasks

### 4. String Templates

String templates make text formatting more concise and readable:

```java
// Before Java 21
String report = String.format("Daily Report\nTotal: %d\nCompleted: %d (%.1f%%)",
    totalItems, completedItems, (double) completedItems / totalItems * 100);

// With Java 21 string templates
String report = STR."""
    Daily Report
    Total: \{totalItems}
    Completed: \{completedItems} (\{(double) completedItems / totalItems * 100:.1f}%)
    """;
```

String templates are used for:
- Logging messages
- Report generation
- Error messages
- User-facing text

### 5. Sealed Classes

Sealed classes define closed hierarchies of types:

```java
// With Java 21 sealed classes
public sealed interface WorkflowCommand permits 
    CreateWorkItemCommand, 
    UpdateWorkItemCommand, 
    TransitionWorkItemCommand, 
    DeleteWorkItemCommand {
    
    UUID getItemId();
}

public record CreateWorkItemCommand(
    UUID itemId,
    String title,
    WorkItemType type
) implements WorkflowCommand {}

// Other implementations...
```

Sealed classes are used for:
- Command hierarchies
- Event hierarchies
- Domain-specific type hierarchies
- State representations

### 6. Sequenced Collections

Sequenced collections provide a unified API for ordered collections:

```java
// With Java 21 sequenced collections
SequencedSet<WorkflowTransition> transitions = new LinkedHashSet<>();

// Get first and last elements
WorkflowTransition first = transitions.getFirst();
WorkflowTransition last = transitions.getLast();

// Get reversed view
SequencedSet<WorkflowTransition> reversed = transitions.reversed();
```

## Implementation Strategy

### Phases of Adoption

We're implementing Java 21 features in the following order:
1. Records and Sealed Classes (immediate benefits for domain model)
2. Pattern Matching (cleaner conditional logic)
3. String Templates (improved formatting)
4. Virtual Threads (performance improvements)
5. Sequenced Collections (API enhancements)

### Code Style Guidelines

When using Java 21 features:
1. Use records for immutable data carriers
2. Prefer pattern matching for type-based conditionals
3. Use string templates for complex text formatting
4. Consider virtual threads for I/O-bound operations
5. Use sealed interfaces/classes for closed type hierarchies

## Examples from Rinna Codebase

### Record-Based DTOs

```java
public record WorkItemDTO(
    UUID id,
    String title,
    String description,
    WorkItemType type,
    WorkflowState status,
    Priority priority,
    String assignee,
    Instant createdAt,
    Instant updatedAt,
    Optional<UUID> parentId
) {
    // Factory method to create from domain entity
    public static WorkItemDTO fromEntity(WorkItem item) {
        return new WorkItemDTO(
            item.getId(),
            item.getTitle(),
            item.getDescription(),
            item.getType(),
            item.getStatus(),
            item.getPriority(),
            item.getAssignee(),
            item.getCreatedAt(),
            item.getUpdatedAt(),
            item.getParentId()
        );
    }
    
    // Convert back to domain entity
    public DefaultWorkItem toEntity() {
        return new DefaultWorkItem(
            id, title, description, type, status, 
            priority, assignee, createdAt, updatedAt, parentId.orElse(null)
        );
    }
}
```

### Pattern Matching for Switch

```java
public class WorkflowCommandHandler {
    public WorkItem handleCommand(WorkflowCommand command) {
        return switch (command) {
            case CreateWorkItemCommand c -> 
                itemService.createWorkItem(new WorkItemCreateRequest(
                    c.title(), c.description(), c.type(), c.priority(), c.assignee()));
                    
            case UpdateWorkItemCommand u -> 
                itemService.updateWorkItem(u.itemId(), 
                    u.title().orElse(null), 
                    u.description().orElse(null),
                    u.priority().orElse(null),
                    u.assignee().orElse(null));
                    
            case TransitionWorkItemCommand t -> 
                workflowService.transition(t.itemId(), t.targetState());
                
            case DeleteWorkItemCommand d -> {
                itemService.deleteWorkItem(d.itemId());
                yield null;
            }
        };
    }
}
```

These examples demonstrate how Java 21 features make Rinna's codebase more expressive, concise, and maintainable while adhering to Clean Architecture principles.
