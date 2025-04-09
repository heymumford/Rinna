# Rinna Glossary and Conceptual Index

This document provides definitions for key terms and concepts used throughout Rinna, organized both alphabetically and by conceptual category.

## Alphabetical Index

### A

**Adapter Pattern**  
A design pattern used in Rinna to convert the interface of a class into another interface clients expect. Heavily used in the implementation of Clean Architecture to separate core domain from external systems.

**ADR (Architecture Decision Record)**  
A document that captures an important architectural decision made along with its context and consequences. Rinna maintains ADRs in the `/docs/architecture/decisions/` directory.

**Admin CLI**  
Command-line interface tools focused on administration tasks for setting up and managing Rinna environments.

**API (Application Programming Interface)**  
Rinna's RESTful service layer implemented in Go that allows external systems to interact with Rinna programmatically.

**Assignee**  
The user assigned responsibility for completing a work item.

### B

**Backlog**  
A prioritized list of work items that have not yet been scheduled for implementation.

**BDD (Behavior-Driven Development)**  
A development approach used in Rinna where tests are written in natural language describing the behavior of the system. Implemented using Cucumber with feature files in `/src/test/resources/features/`.

**Bidirectional Synchronization**  
A feature of Rinna's integration system that keeps work items updated in both Rinna and external systems when changes occur in either system.

**Bug**  
A work item type that represents a defect or unintended behavior in the software.

### C

**C4 Model**  
A hierarchical approach to diagramming software architecture used in Rinna, consisting of Context, Container, Component, and Code diagrams.

**Chore**  
A work item type that represents maintenance tasks that don't directly add user-visible features but improve system health.

**Clean Architecture**  
The architectural approach used by Rinna, organizing code into concentric layers (Domain, Use Cases, Interface Adapters, and Frameworks & Drivers) with dependencies pointing inward.

**CLI (Command-Line Interface)**  
Rinna's primary user interface, implemented as executable commands in the `bin/` directory for developer interaction with the system.

**Comment**  
Textual notes attached to work items to provide additional context, updates, or discussion.

**Component Test**  
Tests that verify the behavior of a specific component in isolation from other components, but may include multiple classes working together.

**Critical Path**  
The sequence of dependent work items that determines the minimum time needed to complete a project. Identified using the `bin/rin path` command.

### D

**Dependency**  
A relationship between work items where one item cannot be completed until another item is completed.

**Domain Layer**  
The innermost layer in Clean Architecture containing business entities and business rules, located in `org.rinna.domain` package.

**Done**  
A workflow state indicating that work on an item has been completed and verified.

### E

**Enterprise Integration**  
Rinna's capabilities for connecting with external enterprise systems like Jira, Azure DevOps, and GitHub.

**Epic**  
A large work item that can be broken down into multiple smaller work items. Used for organizing large initiatives.

### F

**Feature**  
A work item type that represents a new functionality to be added to the system.

**Found**  
The initial workflow state for newly created work items.

**Framework Layer**  
The outermost layer in Clean Architecture containing frameworks, drivers, and external tools. In Rinna, this includes database implementations and UI components.

### G

**Goal**  
A high-level work item type that represents strategic objectives that guide development efforts.

### H

**History Entry**  
A record of a change made to a work item, including status transitions, assignments, and metadata changes.

### I

**In Progress**  
A workflow state indicating that work on an item is actively being done.

**In Test**  
A workflow state indicating that a work item has been implemented and is undergoing testing or review.

**Integration Test**  
Tests that verify the interaction between different components of the system, potentially including external dependencies.

**Issue**  
A general term for any type of work item in Rinna.

**Item Service**  
A core application service in Rinna responsible for managing work items, implementing the `ItemService` interface.

### J-K

**Java 21**  
The primary programming language version used for Rinna's core domain and business logic, featuring modern capabilities like records, pattern matching, and virtual threads.

### L

**Lota**  
A development cycle or iteration in Rinna, similar to a sprint in Scrum. Named after a traditional Japanese water vessel which holds only what is needed, representing Rinna's philosophy of minimalism.

### M

**Metadata**  
Custom fields and values attached to work items to store additional information beyond the standard fields.

**Migration**  
The process of transferring data from another system into Rinna, implemented using the `rin-migrate` command.

**Model Mapper**  
A component that transforms domain entities between different layers of the architecture, particularly between the CLI and core domain models.

**Monitoring Service**  
A service providing health checking and metrics collection for Rinna components.

### N-O

**Output Formatter**  
A component that controls the rendering of command output in different formats (e.g., text, JSON, CSV).

### P

**Polyglot Architecture**  
Rinna's use of multiple programming languages (Java, Go, Python) for different system components, choosing the best language for each task.

**Priority**  
A classification of work items based on their importance: TRIVIAL, LOW, MEDIUM, HIGH, CRITICAL.

**Project**  
A logical grouping of related work items, typically representing a product or initiative.

### Q

**Queue**  
A collection of work items organized for processing, such as a team's current workload or a pipeline of tasks.

**Query Service**  
A service that provides advanced filtering and search capabilities for finding work items based on various criteria.

### R

**Record**  
A Java feature used in Rinna to create immutable data transfer objects, prominently used in `WorkItemRecord`, `HistoryEntryRecord`, etc.

**Release**  
A versioned collection of work items that are delivered together, managed using `ReleaseService`.

**Repository**  
A pattern used to abstract data storage, defined by interfaces in the domain layer and implemented in adapter layers.

**REST API**  
Representational State Transfer API, the architecture style used for Rinna's HTTP-based services implemented in Go.

### S

**Service**  
A component that implements business logic, typically defined as an interface in the `domain.service` package with implementations in the `adapter.service` package.

**Story**  
A work item type representing a user-centric feature or enhancement, typically describing functionality from an end-user perspective.

**Subtask**  
A work item that is part of a larger work item, created to break down complex tasks.

### T

**Tag**  
A label applied to work items for categorization and filtering.

**TDD (Test-Driven Development)**  
A development methodology where tests are written before implementation, supported by Rinna's workflow and commands like `bin/rin tdd`.

**Test Pyramid**  
A testing strategy that emphasizes having more unit tests than integration tests, and more integration tests than acceptance tests.

**To Do**  
A workflow state indicating that a work item is ready to be worked on but has not yet been started.

**Triaged**  
A workflow state indicating that a work item has been reviewed, prioritized, and is waiting to be scheduled.

### U

**Unit Test**  
A test that verifies the behavior of a single unit of code in isolation, typically a class or method.

**Use Case Layer**  
The second layer in Clean Architecture containing application-specific business rules, implemented in the `org.rinna.usecase` package.

### V-W

**Workflow**  
Rinna's opinionated process for moving work items through their lifecycle, consisting of six core states: FOUND, TRIAGED, TO_DO, IN_PROGRESS, IN_TEST, DONE.

**Workflow Service**  
A core service that manages the transitions between workflow states, implementing the `WorkflowService` interface.

**Work Item**  
The fundamental unit of work in Rinna, representing tasks, features, bugs, or other deliverables.

**Work Item Type**  
The classification of a work item: TASK, STORY, BUG, FEATURE, EPIC, CHORE, etc.

### X-Y-Z

**Zero-Friction Philosophy**  
Rinna's design principle of minimizing process overhead and focusing on developer productivity.

## Conceptual Categories

### Core Domain Concepts

- **Work Item**: The fundamental unit of work in Rinna
- **Work Item Type**: Classification of work items (TASK, STORY, BUG, FEATURE, EPIC, CHORE)
- **Priority**: Importance level (TRIVIAL, LOW, MEDIUM, HIGH, CRITICAL)
- **Workflow State**: Status in the workflow process (FOUND, TRIAGED, TO_DO, IN_PROGRESS, IN_TEST, DONE)
- **Assignee**: User responsible for completing a work item
- **Tag**: Label for categorization and filtering
- **Metadata**: Custom fields for additional information
- **Comment**: Textual notes attached to work items
- **History Entry**: Record of changes to a work item

### Organizational Concepts

- **Project**: Logical grouping of related work items
- **Release**: Versioned collection of work items delivered together
- **Lota**: Development cycle or iteration, similar to a sprint
- **Backlog**: Prioritized list of not-yet-scheduled work items
- **Queue**: Collection of work items organized for processing
- **Critical Path**: Sequence of dependent work items defining minimum completion time

### Technical Architecture Concepts

- **Clean Architecture**: Architectural approach with concentric layers
- **Domain Layer**: Innermost layer with business entities and rules
- **Use Case Layer**: Application-specific business rules
- **Interface Adapters Layer**: Converts between inner and outer layers
- **Frameworks & Drivers Layer**: External tools and frameworks
- **Adapter Pattern**: Design pattern for interface conversion
- **Repository**: Pattern for abstract data storage
- **Service**: Component implementing business logic
- **Model Mapper**: Transforms entities between architecture layers

### Integration Concepts

- **Enterprise Integration**: Connecting with external systems
- **REST API**: HTTP-based service interface
- **Webhook**: Event-based integration mechanism
- **Bidirectional Synchronization**: Two-way updates between systems
- **Migration**: Transferring data from another system into Rinna

### Development Methodology Concepts

- **TDD (Test-Driven Development)**: Tests before implementation
- **BDD (Behavior-Driven Development)**: Natural language tests
- **Test Pyramid**: Testing strategy with hierarchy of test types
- **Unit Test**: Tests a single unit in isolation
- **Component Test**: Tests components working together
- **Integration Test**: Tests interaction between components
- **Zero-Friction Philosophy**: Minimizing process overhead

### Tool Concepts

- **CLI (Command-Line Interface)**: Primary user interface
- **Admin CLI**: Tools for administration tasks
- **Output Formatter**: Controls rendering of command output
- **C4 Model**: Hierarchical approach to architecture diagramming
- **ADR (Architecture Decision Record)**: Documents architectural decisions

## Cross-References

### Work Item Workflow Progression

FOUND → TRIAGED → TO_DO → IN_PROGRESS → IN_TEST → DONE

### Work Item Types Hierarchy

EPIC → FEATURE → STORY → TASK
BUG (independent)
CHORE (independent)

### Clean Architecture Layers (Inside to Outside)

Domain Layer → Use Case Layer → Interface Adapters Layer → Frameworks & Drivers Layer

### Testing Pyramid (Bottom to Top)

Unit Tests → Component Tests → Integration Tests → Acceptance Tests → Performance Tests

## Related Documentation

- [Work Item Management](user-guide/README.md#work-item-management)
- [Workflow States](user-guide/workflow.md)
- [Workflow Philosophy](user-guide/workflow-philosophy.md)
- [Clean Architecture](development/architecture.md)
- [Work Item Relationships](user-guide/work-item-relationships.md)
- [Lota (Development Cycle)](user-guide/lota.md)
- [Testing Strategy](development/testing.md)