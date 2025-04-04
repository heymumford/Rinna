# Rinna Architecture

## System Overview

Rinna is designed as a lightweight, reusable Maven library for embedding rich workflow management into projects. It focuses on providing essential workflow primitives while remaining unopinionated about UI/UX implementation details.

### Module Structure
```
rinna/
├── rinna-core/            # Core domain model and services
├── rinna-data-sqlite/     # SQLite persistence implementation
├── rinna-data-api/        # Data access interfaces
├── rinna-cli/             # Reference CLI implementation
└── rinna-spring/          # Optional Spring integration
```

## System Architecture Diagram

The architecture emphasizes modularity, allowing projects to use only what they need:

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Host Application                         │  │
│  └───────────────────────────────────────────────────────┘  │
│                           │                                  │
│                           ▼                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                     Rinna Library                     │  │
│  │                                                       │  │
│  │  ┌─────────────┐  ┌────────────┐  ┌───────────────┐  │  │
│  │  │ Workflow    │  │ Item       │  │ Release       │  │  │
│  │  │ Management  │  │ Management │  │ Management    │  │  │
│  │  └─────────────┘  └────────────┘  └───────────────┘  │  │
│  │                                                       │  │
│  │  ┌─────────────────────────────────────────────────┐ │  │
│  │  │            Storage Abstraction Layer            │ │  │
│  │  └─────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────┘  │
│                           │                                  │
│                           ▼                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Pluggable Storage Implementation (SQLite, JPA, etc.)  │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Domain Model

### Item Hierarchy
```
          ┌─────────┐
          │  Goal   │
          └────┬────┘
               │
               ▼
        ┌────────────┐
        │  Feature   │
        └─────┬──────┘
              │
      ┌───────┴───────┐
      │               │
      ▼               ▼
 ┌────────┐     ┌─────────┐
 │  Chore │     │   Bug   │
 └────────┘     └─────────┘
```

## Core Interfaces

### `WorkItem` Interface
```java
public interface WorkItem {
    UUID getId();
    String getTitle();
    String getDescription();
    WorkItemType getType();
    WorkflowState getStatus();
    Priority getPriority();
    String getAssignee();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    Optional<UUID> getParentId();
    
    // Associated collections
    List<Comment> getComments();
    List<WorkItemRelation> getRelations();
    Map<String, String> getMetadata();
}
```

### `WorkflowService` Interface
```java
public interface WorkflowService {
    boolean canTransition(UUID itemId, WorkflowState targetState);
    WorkItem transition(UUID itemId, WorkflowState targetState) throws InvalidTransitionException;
    List<WorkflowState> getAvailableTransitions(UUID itemId);
    WorkflowDefinition getWorkflowDefinition(WorkItemType type);
}
```

### `ItemService` Interface
```java
public interface ItemService {
    WorkItem create(WorkItemCreateRequest request);
    WorkItem update(UUID id, WorkItemUpdateRequest request);
    Optional<WorkItem> findById(UUID id);
    List<WorkItem> search(WorkItemSearchCriteria criteria);
    void addRelation(UUID sourceId, UUID targetId, RelationType type);
    void addComment(UUID itemId, CommentCreateRequest comment);
}
```

### `ReleaseService` Interface
```java
public interface ReleaseService {
    Release createRelease(ReleaseCreateRequest request);
    void addItemToRelease(UUID itemId, UUID releaseId);
    List<WorkItem> getItemsInRelease(UUID releaseId);
    Release getNextVersionNumber(UUID currentReleaseId, ReleaseType type);
}
```

## Extension Points

### `StorageProvider` Interface
```java
public interface StorageProvider {
    Optional<WorkItem> findItemById(UUID id);
    UUID saveItem(WorkItem item);
    List<WorkItem> queryItems(QueryCriteria criteria);
    void saveRelation(Relation relation);
    Release saveRelease(Release release);
    // Other storage operations
}
```

### `WorkflowDefinitionProvider` Interface
```java
public interface WorkflowDefinitionProvider {
    WorkflowDefinition getDefinition(WorkItemType type);
}
```

## Implementation Philosophy

### 1. Minimal Dependencies
- Only standard Java libraries in core module
- Clear separation between API and implementation
- Optional integrations as separate modules

### 2. Extensibility
- Plugin architecture for storage implementations
- Customizable workflow definitions
- Event system for integration with notification systems

### 3. Developer Experience
- Fluent builder APIs
- Consistent exception handling
- Comprehensive Javadoc documentation

### 4. Testing Strategy
- BDD with Cucumber for acceptance tests
- JUnit for unit testing
- Integration tests with TestContainers
- High test coverage requirements (>85%)
