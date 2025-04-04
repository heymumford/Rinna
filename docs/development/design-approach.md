<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford], Gemini Deep Research, Claude 3.7. -->

# Rinna Design Approach

This document outlines our approach to designing and implementing Rinna based on the engineering specification v0.3.

## Core Design Principles

1. **Developer-Centric**: Tools and workflows are optimized for the developer experience.
2. **Clarity**: All components, interfaces, and interactions must be explicitly defined and easily understood.
3. **Usability**: The system should be intuitive to use with well-designed APIs.
4. **Immutability**: Work items have unique, immutable identifiers and follow immutable data patterns.
5. **Explicit Flow**: Workflow transitions are clearly defined and strictly enforced.
6. **Modern Java**: Leverage Java 21 features to write more expressive, concise, and maintainable code.

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

### Phase 2: Persistence Layer

1. **SQLite Implementation**
   - Local storage for standalone usage
   - Schema design following domain model

2. **Storage Abstraction**
   - Interface-based design for pluggable implementations
   - Repository pattern for data access

### Phase 3: CLI Interface

1. **Bash Command Line Interface**
   - Simple commands for managing items and workflow
   - Interactive workflow visualization

### Phase 4: GitHub Integration

1. **Commit Message Hooks**
   - Status updates via special syntax in commit messages
   - Automated item transitions based on commits and PRs

2. **Continuous State Visibility**
   - Real-time status updates on work items
   - Automated reporting

### Phase 5: Containerization and Cloud Deployment

1. **Docker Containerization**
   - Consistent deployment environment
   - Configuration via environment variables

2. **PostgreSQL Adapter**
   - Cloud-ready database implementation
   - Migration path from SQLite

## Testing Strategy

Our testing strategy follows a comprehensive approach:

1. **BDD Tests (Cucumber)**
   - High-level acceptance tests validating core workflows
   - Common scenarios in natural language

2. **Unit Tests (JUnit/AssertJ)**
   - Fine-grained component testing
   - High test coverage for core logic (>90%)

3. **Integration Tests**
   - Validate interactions between services
   - Test with actual storage implementations

## Architecture

Rinna follows a clean architecture approach:

1. **Core Domain Model** (Inner Layer)
   - Domain entities and value objects
   - Business rules and invariants

2. **Service Layer**
   - Use case implementations
   - Business logic coordination

3. **Infrastructure Layer** (Outer Layer)
   - Persistence implementations
   - External system integrations
   - User interfaces (CLI, API)

## Next Steps

1. Complete the core workflow state machine
2. Adopt Java 21 features to enhance code readability and maintainability
3. Enhance developer-focused query and filtering capabilities
4. Develop CLI interface with developer-centric commands
5. Add GitHub integration for seamless workflow management
6. Implement SQLite persistence with efficient query support

Each step follows TDD principles, with tests written before implementation. See the [Java 21 Features](java21-features.md) document for details on how we'll leverage modern Java capabilities.
