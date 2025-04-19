<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Rinna Architecture

## Clean Architecture Overview

Rinna follows the Clean Architecture approach as defined by Robert C. Martin, organizing the system into concentric layers with dependencies pointing inward. This design ensures separation of concerns, testability, and flexibility.

![Rinna Clean Architecture Diagram](../diagrams/clean_architecture_layers.svg)

Each layer in the Clean Architecture has a specific role:

1. **Domain Layer (Core)**: Contains business entities and enterprise business rules with no external dependencies
2. **Use Case Layer**: Contains application-specific business rules that orchestrate the flow of data and entities
3. **Interface Adapters Layer**: Converts data between the formats used by use cases and external frameworks
4. **Frameworks & Drivers Layer**: Contains frameworks, tools, and external systems

For a detailed breakdown of components and relationships, see the [Clean Architecture Diagrams](../diagrams/architecture/clean_architecture_diagram.md).

## Polyglot Clean Architecture

Rinna implements a polyglot approach to Clean Architecture, using Java for the core domain layer, Go for the API layer, and Python for CLI components. This combines the strengths of each language while maintaining the principles of Clean Architecture:

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

The module structure is modular and polyglot, supporting both Java and Go components:

```
rinna/
├── rinna-core/            # Core domain model and services (Java)
├── api/                   # Go API service
│   ├── cmd/               # Command-line executables
│   │   └── rinnasrv/      # API server executable
│   ├── internal/          # Package-private code
│   │   ├── handlers/      # HTTP handlers
│   │   ├── middleware/    # HTTP middleware
│   │   ├── models/        # API data models
│   │   └── client/        # Java service client
│   └── pkg/               # Public packages
│       ├── auth/          # Authentication
│       └── config/        # Configuration
├── rinna-data-sqlite/     # SQLite persistence implementation (planned)
├── bin/                   # CLI tools and utilities
└── docs/                  # Documentation
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

## Enterprise Integration

Rinna integrates with various external systems through a flexible adapter-based architecture that maintains the principles of Clean Architecture.

![Enterprise Integration Diagram](../diagrams/enterprise_integration_diagram.svg)

Key integration points include:

* **Issue Tracking Systems**: Bidirectional integration with Jira, GitHub Issues, and Azure DevOps
* **Version Control**: Git hooks for commit validation and workflow automation
* **Document Systems**: Integration with Confluence and SharePoint for document generation and storage
* **CI/CD Systems**: Integration with Jenkins, GitHub Actions, and Azure Pipelines for build events

For a detailed view of the integration architecture, including data flows, adapter implementations, and security considerations, see the [Enterprise Integration Diagram](../diagrams/integration/enterprise_integration_diagram.md).

## Implementation Philosophy

### 1. Polyglot Architecture with Strong Boundaries
- Java for domain model and business logic (JDK 21)
- Go for API layer and CLI integration services
- Well-defined JSON contract between language boundaries
- Each language used for its strengths

### 2. Minimal Dependencies
- Only standard Java libraries in domain layer
- Standard library Go where possible
- Clear separation between layers 
- Dependencies only point inward

### 3. Extensibility
- Well-defined interfaces in the domain layer
- Pluggable implementations in outer layers
- Dependency injection for flexible component wiring
- API-first design for service integration

### 4. Developer Experience
- Consistent package structure following Clean Architecture
- Clear separation of concerns
- Intuitive organization that maps to architectural concepts
- Modern Java 21 features for domain logic
- Efficient Go implementation for API services

### 4. Modern Java Utilization
- Records for immutable data transfer and value objects
- Pattern matching for cleaner conditional logic
- Virtual threads for efficient concurrency
- Sealed classes for representing closed hierarchies
- String templates for cleaner text formatting

### 5. Testing Strategy
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
