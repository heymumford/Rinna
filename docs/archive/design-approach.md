<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Rinna Design Approach

This document outlines our approach to designing and implementing Rinna based on the engineering specification v0.3.

## Core Design Principles

1. **Developer-Centric**: Tools and workflows are optimized for the developer experience.
2. **Clarity**: All components, interfaces, and interactions must be explicitly defined and easily understood.
3. **Usability**: The system should be intuitive to use with well-designed APIs.
4. **Immutability**: Work items have unique, immutable identifiers and follow immutable data patterns.
5. **Explicit Flow**: Workflow transitions are clearly defined and strictly enforced.
6. **Polyglot Architecture**: Java 21 for domain logic and Go for high-performance API services.
7. **Modern Language Features**: Leverage Java 21 and Go features for expressive, concise, and maintainable code.

## Implementation Approach

We're following the incremental feature-driven development approach from our engineering spec:

### Phase 1: Core Domain Library (Current Focus)

1. **Define Core Domain Model**
   - Work Item Types (Goal, Feature, Bug, Chore)
   - Workflow States (Found → Triaged → To Do → In Progress → In Test → Done)
   - Priority Levels
   - Developer-centric attributes (assignee, effort estimation, tags)

2. **Implement Core Services**
   - WorkflowService: Manage transitions between states
   - ItemService: CRUD operations for work items
   - QueryService: Developer-focused work item filtering and reporting

3. **Create In-Memory Storage Implementation**
   - Fast development and testing without external dependencies

### Phase 2: API Layer (Completed)

1. **Go API Server**
   - RESTful endpoints for accessing the Java core services
   - Fast and efficient HTTP server
   - JWT-based authentication

2. **Client Integration**
   - HTTP client for Java service communication
   - JSON contract between services

### Phase 3: Persistence Layer

1. **SQLite Implementation**
   - Local storage for standalone usage
   - Schema design following domain model

2. **Storage Abstraction**
   - Interface-based design for pluggable implementations
   - Repository pattern for data access

### Phase 4: CLI Interface

1. **Bash Command Line Interface**
   - Simple commands for managing items and workflow
   - Interactive workflow visualization

### Phase 5: GitHub Integration

1. **Commit Message Hooks**
   - Status updates via special syntax in commit messages
   - Automated item transitions based on commits and PRs

2. **Continuous State Visibility**
   - Real-time status updates on work items
   - Automated reporting

### Phase 6: Containerization and Cloud Deployment

1. **Docker Containerization**
   - Consistent deployment environment
   - Configuration via environment variables

2. **PostgreSQL Adapter**
   - Cloud-ready database implementation
   - Migration path from SQLite

## Testing Strategy

Our testing strategy follows a comprehensive polyglot approach:

1. **BDD Tests (Cucumber for Java)**
   - High-level acceptance tests validating core workflows
   - Common scenarios in natural language
   - Tests the Java domain logic independently

2. **Unit Tests**
   - Java (JUnit/AssertJ): Fine-grained domain component testing
   - Go (Go Test): API server component testing
   - High test coverage for core logic (>90%)

3. **Integration Tests**
   - Java: Validate interactions between domain services
   - Go: Validate API endpoints and middleware
   - Cross-language: Test communication between Go API and Java services
   - End-to-end tests with actual storage implementations

4. **Build System Integration**
   - Unified testing via Makefile
   - Language-specific test runners in respective directories

## Architecture

Rinna follows a polyglot clean architecture approach:

1. **Core Domain Model** (Inner Layer, Java)
   - Domain entities and value objects
   - Business rules and invariants
   - Written in Java 21 with modern language features

2. **Service Layer** (Java)
   - Use case implementations
   - Business logic coordination
   - Strong typing and automated tests

3. **API Layer** (Go)
   - RESTful HTTP API server
   - Middleware for authentication, logging, and request handling
   - High-performance client-server communication

4. **Infrastructure Layer** (Outer Layer, Mixed)
   - Java: Persistence implementations and core services
   - Go: HTTP server, routing, and client integration
   - Bash: CLI scripts and utilities
   - Integration with external systems

## Next Steps

1. Enhance the Go API server with additional endpoints and features
2. Implement cross-service integration testing between Java core and Go API
3. Develop Go client libraries for easier integration by third-party systems
4. Complete the core workflow state machine with all required states
5. Enhance developer-focused query and filtering capabilities
6. Implement SQLite persistence with efficient query support
7. Develop CLI interface with developer-centric commands
8. Add GitHub integration for seamless workflow management

Each step follows language-appropriate testing practices, with tests written before implementation. For Java components, see the [Java 21 Features](java21-features.md) document for details on how we're leveraging modern Java capabilities. For Go components, we follow idiomatic Go practices and standard library patterns.
