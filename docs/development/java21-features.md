<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Java 21 Features for Rinna

This document outlines the Java 21 features that align with Rinna's architecture and design goals, and how we can leverage them to improve our codebase.

## Key Java 21 Features for Rinna

### 1. Record Patterns (JEP 440)

**Benefit:** Enhanced pattern matching for records to simplify data extraction and transformation.

**Applications in Rinna:**
- Processing work item data across architectural boundaries
- Simplifying data transformation in the adapter layer
- Creating clean, concise DTO structures

**Example:**
```java
// Before Java 21
if (item instanceof DefaultWorkItem) {
    DefaultWorkItem defaultItem = (DefaultWorkItem) item;
    UUID id = defaultItem.getId();
    String title = defaultItem.getTitle();
    // Use id and title
}

// With Java 21 record patterns
if (item instanceof DefaultWorkItem(UUID id, String title, var description, var type, var status, var priority, var assignee, var createdAt, var updatedAt, var parentId)) {
    // Use id and title directly
}
```

### 2. Pattern Matching for Switch (JEP 441)

**Benefit:** More expressive, safer switch statements that work well with class hierarchies and reduce boilerplate.

**Applications in Rinna:**
- Handling different work item types
- Processing workflow state transitions
- Implementing command pattern for CLI operations

**Example:**
```java
// Before Java 21
WorkflowState nextState;
if (currentState == WorkflowState.FOUND) {
    nextState = WorkflowState.TRIAGED;
} else if (currentState == WorkflowState.TRIAGED) {
    nextState = WorkflowState.TODO;
} else if (currentState == WorkflowState.TODO) {
    nextState = WorkflowState.IN_PROGRESS;
} else {
    nextState = currentState;
}

// With Java 21 pattern matching for switch
WorkflowState nextState = switch (currentState) {
    case FOUND -> WorkflowState.TRIAGED;
    case TRIAGED -> WorkflowState.TODO;
    case TODO -> WorkflowState.IN_PROGRESS;
    case IN_PROGRESS -> WorkflowState.IN_TEST;
    case IN_TEST -> WorkflowState.DONE;
    case DONE -> WorkflowState.DONE;
};
```

### 3. Virtual Threads (JEP 444)

**Benefit:** Lightweight concurrency with minimal resource usage, ideal for I/O-bound operations.

**Applications in Rinna:**
- Processing multiple work items concurrently
- Handling parallel database operations
- Improving CLI responsiveness

**Example:**
```java
// With Java 21 virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (WorkItem item : items) {
        executor.submit(() -> {
            processWorkItem(item);
        });
    }
}
```

### 4. Sequenced Collections (JEP 431)

**Benefit:** Unified API for working with collections that have a defined encounter order.

**Applications in Rinna:**
- Managing ordered lists of work items
- Tracking workflow transition history
- Implementing priority queues for work management

**Example:**
```java
// With Java 21 sequenced collections
SequencedCollection<WorkItem> workItems = getWorkItemsInPriorityOrder();
WorkItem highestPriority = workItems.getFirst();
WorkItem lowestPriority = workItems.getLast();
```

### 5. String Templates (JEP 430)

**Benefit:** More readable, maintainable string formatting with integrated expressions.

**Applications in Rinna:**
- CLI output formatting
- Error message generation
- Log message standardization

**Example:**
```java
// Before Java 21
String message = String.format("Transitioned work item %s from %s to %s", 
    item.getId(), currentState, targetState);

// With Java 21 string templates
String message = STR."Transitioned work item \{item.getId()} from \{currentState} to \{targetState}";
```

### 6. Unnamed Patterns and Variables (JEP 443)

**Benefit:** Reduced boilerplate when only some values are needed.

**Applications in Rinna:**
- Simplifying data extraction
- Cleaner pattern matching
- Reduced noise in adapter layer code

**Example:**
```java
// With Java 21 unnamed patterns
if (item instanceof DefaultWorkItem(var id, var title, _, _, var status, _, _, _, _, _)) {
    // Only use id, title, and status
}
```

## Integration Strategy

### 1. Phased Adoption

We'll implement Java 21 features in the following order:

1. **Pattern Matching & Records:** Immediate adoption for cleaner code
2. **Sequenced Collections:** Refactor collection handling
3. **String Templates:** Improve output formatting
4. **Virtual Threads:** Enhance concurrency where applicable

### 2. Updates to Design and Implementation Guidelines

- Document patterns for using these features consistently
- Update code review guidelines
- Create examples in documentation

### 3. Testing Strategy

- Unit tests with specific cases for pattern matching
- Performance benchmarks for virtual threads
- Comparative analysis before/after adoption

## Alignment with Architecture

These Java 21 features align perfectly with our Clean Architecture approach:

- **Domain Layer:** Records for value objects, Sealed classes for clearer modeling
- **Use Cases:** Pattern matching for business logic, Virtual threads for concurrent processing
- **Adapters:** String templates for external interfaces, Records for DTOs
- **Frameworks:** Virtual threads for I/O operations

## Impact on Developer Experience

Adopting these Java 21 features directly supports our developer-centric philosophy:

1. **Less Boilerplate:** More focus on business logic
2. **Clearer Intent:** Pattern matching makes code more readable
3. **Enhanced Productivity:** Simplified syntax for common operations
4. **Modern Codebase:** Attracts and retains talented developers

## Next Steps

1. Update code style guidelines to incorporate Java 21 features
2. Create branch with example refactorings
3. Prepare knowledge-sharing sessions on key features
4. Incrementally adopt features in new code and refactorings
