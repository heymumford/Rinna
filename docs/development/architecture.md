# Rinna Architecture

## Clean Architecture Overview

Rinna has been restructured to follow Robert C. Martin's (Uncle Bob) Clean Architecture principles, organizing the codebase into concentric layers with dependencies pointing inward:

```
┌───────────────────────────────────────────────┐
│ Framework & Drivers                           │
│ ┌───────────────────────────────────────────┐ │
│ │ Interface Adapters                        │ │
│ │ ┌───────────────────────────────────────┐ │ │
│ │ │ Application Use Cases                 │ │ │
│ │ │ ┌───────────────────────────────────┐ │ │ │
│ │ │ │ Enterprise Business Rules         │ │ │ │
│ │ │ │ (Entities)                        │ │ │ │
│ │ │ └───────────────────────────────────┘ │ │ │
│ │ └───────────────────────────────────────┘ │ │
│ └───────────────────────────────────────────┘ │
└───────────────────────────────────────────────┘
```

## New Package Structure

The codebase has been reorganized into the following package structure:

### Core Packages

- `org.rinna.domain.entity`: Core business entities and business rules
  - These are pure domain objects with no dependencies on other layers
  - Examples: `WorkItem`, `Priority`, `WorkItemType`, `WorkflowState`

- `org.rinna.domain.usecase`: Application-specific business rules
  - Contains business logic that orchestrates entity interactions
  - Defines interfaces to be implemented by outer layers (repository interfaces)
  - Examples: `TransitionWorkflow`, `CreateWorkItem`, `ListWorkItems`

### Interface Adapters

- `org.rinna.adapter.controller`: Controllers, presenters, and gateway implementations
  - Converts data between domain and external formats
  - Examples: `CLIController`, `WorkItemController`, `WorkflowController`

- `org.rinna.adapter.persistence`: Data access implementations
  - Implements repository interfaces defined in the domain layer
  - Examples: `InMemoryItemRepository`, `SQLiteItemRepository`

### Frameworks and Drivers

- `org.rinna.framework`: Frameworks and external tools integration
  - Contains code related to external libraries and tools
  - Examples: `SQLiteDatabase`, `CLIApplication`

- `org.rinna.config`: Application configuration and dependency injection
  - Wires together the different components
  - Examples: `RinnaConfig`, `DependencyConfig`

## Module Structure

The module structure remains modular and extensible:

```
rinna/
├── rinna-core/            # Core domain model and services
├── rinna-data-sqlite/     # SQLite persistence implementation (planned)
├── rinna-data-api/        # Data access interfaces (planned)
├── rinna-cli/             # Reference CLI implementation (planned)
└── rinna-spring/          # Optional Spring integration (planned)
```

## Dependency Rule

The fundamental rule of Clean Architecture is that dependencies always point inward:

1. Entities have no dependencies
2. Use Cases depend only on Entities
3. Interface Adapters depend on Use Cases and Entities
4. Frameworks & Drivers depend on Interface Adapters

## Core Interfaces

### Domain Entities and Use Cases

* **WorkItem**: Core business entity for all work that can be tracked
* **WorkflowService**: Application use case for managing workflow transitions
* **ItemService**: Application use case for managing work items

## Extension Points

The architecture maintains clear extension points:

* **Repository interfaces** defined in the domain layer, implemented in the adapter layer
* **Controllers** in the adapter layer that handle external input
* **Framework integration** through the framework layer

## Implementation Philosophy

### 1. Minimal Dependencies
- Only standard Java libraries in domain layer
- Clear separation between layers 
- Dependencies only point inward

### 2. Extensibility
- Well-defined interfaces in the domain layer
- Pluggable implementations in outer layers
- Dependency injection for flexible component wiring

### 3. Developer Experience
- Consistent package structure following Clean Architecture
- Clear separation of concerns
- Intuitive organization that maps to architectural concepts

### 4. Testing Strategy
- Entities and use cases tested in isolation
- Interface adapters tested with mocked use cases
- Framework components tested with integration tests
- BDD with Cucumber for acceptance tests

## Migration Plan

The migration to Clean Architecture will proceed in phases:

1. Create the new package structure
2. Move entities to the domain.entity package
3. Define use case interfaces in domain.usecase
4. Refactor service implementations into appropriate layers
5. Update dependency injection in the config layer

This approach ensures a smooth transition while maintaining compatibility with existing code.