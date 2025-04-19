# Rinna Architecture

Rinna follows Clean Architecture principles, organizing the system into concentric layers with dependencies pointing inward. This design ensures separation of concerns, testability, and flexibility.

## Clean Architecture Overview

![Rinna Clean Architecture Diagram](../../diagrams/clean_architecture_layers.svg)

Each layer in the Clean Architecture has a specific role:

1. **Domain Layer (Core)**: Contains business entities and enterprise business rules with no external dependencies
2. **Use Case Layer**: Contains application-specific business rules that orchestrate the flow of data and entities
3. **Interface Adapters Layer**: Converts data between the formats used by use cases and external frameworks
4. **Frameworks & Drivers Layer**: Contains frameworks, tools, and external systems

## Polyglot Clean Architecture

Rinna implements a polyglot approach to Clean Architecture, using:
- **Java** for the core domain layer (business logic and entities)
- **Go** for the API layer (high-performance interfaces)
- **Python** for CLI components and utilities

This combines the strengths of each language while maintaining the principles of Clean Architecture.

## Package Structure

The codebase is organized into the following package structure:

### Core Packages

- `org.rinna.domain.model`: Core business entities and business rules
  - These are pure domain objects with no dependencies on other layers
  - Examples: `WorkItem`, `Priority`, `WorkItemType`, `WorkflowState`

- `org.rinna.domain.service`: Application-specific business rules
  - Contains business logic that orchestrates entity interactions
  - Defines interfaces to be implemented by outer layers
  - Examples: `WorkflowService`, `ItemService`, `ReleaseService`

- `org.rinna.domain.repository`: Repository interfaces
  - Defines data access abstractions to be implemented by adapters
  - Examples: `ItemRepository`, `ReleaseRepository`

### Interface Adapters

- `org.rinna.adapter.service`: Service implementations
  - Implements service interfaces defined in the domain layer
  - Examples: `DefaultWorkflowService`, `DefaultItemService`

- `org.rinna.adapter.repository`: Data access implementations
  - Implements repository interfaces defined in the domain layer
  - Examples: `InMemoryItemRepository`, `SQLiteItemRepository`

- `org.rinna.adapter.controller`: Controllers and presenters
  - Converts between domain and external formats
  - Examples: `WorkItemController`, `WorkflowController`

### Frameworks and Drivers

- `org.rinna.framework`: Frameworks and external tools integration
  - Contains code related to external libraries and tools
  - Examples: `SQLiteDatabase`, `GitIntegration`

- `org.rinna.config`: Application configuration
  - Wires together the different components
  - Examples: `RinnaConfig`, `DependencyConfig`

## Module Structure

The module structure supports both Java and Go components:

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
├── rinna-data-sqlite/     # SQLite persistence implementation
├── bin/                   # CLI tools and utilities
└── docs/                  # Documentation
```

## Dependency Rule

The fundamental rule of Clean Architecture is that dependencies always point inward:

1. Entities have no dependencies
2. Use Cases depend only on Entities
3. Interface Adapters depend on Use Cases and Entities
4. Frameworks & Drivers depend on Interface Adapters

## Enterprise Integration

Rinna integrates with various external systems through a flexible adapter-based architecture:

- **Issue Tracking Systems**: Bidirectional integration with Jira, GitHub Issues, and Azure DevOps
- **Version Control**: Git hooks for commit validation and workflow automation
- **Document Systems**: Integration with Confluence and SharePoint
- **CI/CD Systems**: Integration with Jenkins, GitHub Actions, and Azure Pipelines

## Implementation Philosophy

### 1. Polyglot Architecture with Strong Boundaries
- Java for domain model and business logic (JDK 21)
- Go for API layer and CLI integration services
- Well-defined JSON contract between language boundaries

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
