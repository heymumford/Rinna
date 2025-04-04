<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford], Gemini Deep Research, Claude 3.7. -->

# Java 21 Code Examples for Rinna

This document provides practical examples of how Java 21 features can be applied within Rinna's codebase.

## Record-Based DTOs

Records are perfect for DTOs in the adapter layer:

```java
// WorkItemDTO as a record for the adapter layer
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

## Pattern Matching for Work Item Processing

Pattern matching simplifies conditional logic:

```java
public String getWorkItemSummary(Object item) {
    return switch (item) {
        case DefaultWorkItem(var id, var title, _, WorkItemType.BUG, var status, Priority.HIGH, var assignee, _, _, _) -> 
            STR."High priority bug: \{title} (\{id}, assigned to \{assignee}, status: \{status})";
            
        case DefaultWorkItem(var id, var title, _, WorkItemType.FEATURE, _, _, var assignee, _, _, _) -> 
            STR."Feature: \{title} (\{id}, assigned to \{assignee})";
            
        case DefaultWorkItem(var id, var title, _, _, _, _, _, _, _, _) -> 
            STR."Work item: \{title} (\{id})";
            
        case null -> "No work item provided";
        
        default -> "Unknown item type";
    };
}
```

## Virtual Threads for Concurrent Processing

Using virtual threads for concurrent operations:

```java
@Override
public List<WorkItem> findItemsWithDependencies(List<UUID> itemIds) {
    List<WorkItem> result = new ArrayList<>();
    
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<WorkItem>> futures = itemIds.stream()
            .map(id -> executor.submit(() -> {
                // Get the item and its dependencies - potentially blocking I/O
                WorkItem item = itemRepository.findById(id).orElse(null);
                if (item != null) {
                    // Load dependencies concurrently
                    loadDependencies(item);
                }
                return item;
            }))
            .toList();
            
        // Gather results
        for (Future<WorkItem> future : futures) {
            try {
                WorkItem item = future.get();
                if (item != null) {
                    result.add(item);
                }
            } catch (Exception e) {
                logger.error("Error fetching work item", e);
            }
        }
    }
    
    return result;
}
```

## Sequenced Collections

Using sequenced collections for ordered work items:

```java
public class WorkflowHistory {
    private final SequencedSet<WorkflowTransition> transitions = new LinkedHashSet<>();
    
    public void addTransition(WorkflowTransition transition) {
        transitions.add(transition);
    }
    
    public WorkflowTransition getFirstTransition() {
        return transitions.getFirst();
    }
    
    public WorkflowTransition getLastTransition() {
        return transitions.getLast();
    }
    
    public List<WorkflowTransition> getRecentTransitions(int count) {
        return transitions.reversed().stream()
            .limit(count)
            .toList();
    }
    
    // Transition record using Java 21 record feature
    public record WorkflowTransition(
        UUID itemId,
        WorkflowState fromState,
        WorkflowState toState,
        Instant timestamp,
        String username
    ) {}
}
```

## String Templates for Reporting

Using string templates for cleaner text formatting:

```java
public class WorkflowReportGenerator {
    public String generateDailyReport(List<WorkItem> items, int totalWorkItems) {
        int inProgress = countItemsInState(items, WorkflowState.IN_PROGRESS);
        int completed = countItemsInState(items, WorkflowState.DONE);
        
        return STR."""
            # Daily Workflow Report
            
            ## Summary
            Total work items: \{totalWorkItems}
            Items in progress: \{inProgress} (\{calculatePercentage(inProgress, totalWorkItems)}%)
            Items completed: \{completed} (\{calculatePercentage(completed, totalWorkItems)}%)
            
            ## Recently Completed
            \{formatRecentlyCompleted(items)}
            
            ## Highest Priority Items
            \{formatHighPriorityItems(items)}
            """;
    }
    
    private int countItemsInState(List<WorkItem> items, WorkflowState state) {
        return (int) items.stream()
            .filter(item -> item.getStatus() == state)
            .count();
    }
    
    private double calculatePercentage(int count, int total) {
        return total == 0 ? 0 : Math.round((double) count / total * 100 * 10) / 10.0;
    }
    
    private String formatRecentlyCompleted(List<WorkItem> items) {
        return items.stream()
            .filter(item -> item.getStatus() == WorkflowState.DONE)
            .sorted(Comparator.comparing(WorkItem::getUpdatedAt).reversed())
            .limit(5)
            .map(item -> STR."- \{item.getTitle()} [Completed on \{formatDate(item.getUpdatedAt())}]")
            .collect(Collectors.joining("
"));
    }
    
    private String formatHighPriorityItems(List<WorkItem> items) {
        return items.stream()
            .filter(item -> item.getPriority() == Priority.HIGH)
            .filter(item -> item.getStatus() != WorkflowState.DONE)
            .map(item -> STR."- \{item.getTitle()} [\{item.getStatus()}]")
            .collect(Collectors.joining("
"));
    }
    
    private String formatDate(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
            .format(instant);
    }
}
```

## Sealed Classes for Workflow Commands

Using sealed classes for representing the command pattern:

```java
// Sealed hierarchy for workflow commands
public sealed interface WorkflowCommand permits 
    CreateWorkItemCommand, 
    UpdateWorkItemCommand, 
    TransitionWorkItemCommand, 
    DeleteWorkItemCommand {
    
    UUID getItemId();
}

// Record implementations of the commands
public record CreateWorkItemCommand(
    UUID itemId,
    String title,
    String description,
    WorkItemType type,
    Priority priority,
    String assignee
) implements WorkflowCommand {}

public record UpdateWorkItemCommand(
    UUID itemId,
    Optional<String> title,
    Optional<String> description,
    Optional<Priority> priority,
    Optional<String> assignee
) implements WorkflowCommand {}

public record TransitionWorkItemCommand(
    UUID itemId,
    WorkflowState targetState
) implements WorkflowCommand {}

public record DeleteWorkItemCommand(
    UUID itemId
) implements WorkflowCommand {}

// Command handler using pattern matching
public class WorkflowCommandHandler {
    private final ItemService itemService;
    private final WorkflowService workflowService;
    
    public WorkflowCommandHandler(ItemService itemService, WorkflowService workflowService) {
        this.itemService = Objects.requireNonNull(itemService);
        this.workflowService = Objects.requireNonNull(workflowService);
    }
    
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

These examples demonstrate how Java 21 features can make Rinna's codebase more expressive, concise, and maintainable while adhering to Clean Architecture principles.
