# Rinna Conceptual Categories

This diagram provides a visual representation of the key conceptual categories in Rinna and their relationships.

## Core Domain Concepts

```mermaid
mindmap
  root((Rinna<br>Concepts))
    Core Domain
      Work Item
        Work Item Types
          TASK
          STORY
          BUG
          FEATURE
          EPIC
          CHORE
        Priority
          TRIVIAL
          LOW
          MEDIUM
          HIGH
          CRITICAL
        Workflow States
          FOUND
          TRIAGED
          TO_DO
          IN_PROGRESS
          IN_TEST
          DONE
        Relationships
          DEPENDS_ON
          BLOCKED_BY
          RELATED_TO
          SUBTASK_OF
        Metadata
        Comments
        History
      Workflow
        Transitions
        Validation Rules
        Permissions
    Organization
      Project
      Release
      Lota
      Backlog
      Queue
      Critical Path
    Architecture
      Clean Architecture
        Domain Layer
        Use Case Layer
        Interface Adapters
        Frameworks & Drivers
      Polyglot Design
        Java Core
        Go API
        Python CLI
      Adapter Pattern
      Repository Pattern
      Service Pattern
    Integration
      REST API
      Webhooks
      Entity Mapping
      Bidirectional Sync
      Migration Tools
    Development
      TDD
      BDD
      Test Pyramid
      Zero-Friction
      User Personas
```

## Workflow and Relationship Model

```mermaid
flowchart TB
    subgraph "Work Item Types"
        EPIC
        FEATURE
        STORY
        TASK
        BUG
        CHORE
    end
    
    subgraph "Workflow States"
        FOUND
        TRIAGED
        TO_DO
        IN_PROGRESS
        IN_TEST
        DONE
    end
    
    subgraph "Relationships"
        DEPENDS_ON
        BLOCKED_BY
        RELATED_TO
        SUBTASK_OF
    end
    
    EPIC --> FEATURE
    FEATURE --> STORY
    STORY --> TASK
    
    FOUND --> TRIAGED
    TRIAGED --> TO_DO
    TO_DO --> IN_PROGRESS
    IN_PROGRESS --> IN_TEST
    IN_TEST --> DONE
    IN_TEST -.-> IN_PROGRESS
    
    classDef types fill:#f9f,stroke:#333
    classDef states fill:#ccf,stroke:#333
    classDef relations fill:#ffc,stroke:#333
    
    class EPIC,FEATURE,STORY,TASK,BUG,CHORE types
    class FOUND,TRIAGED,TO_DO,IN_PROGRESS,IN_TEST,DONE states
    class DEPENDS_ON,BLOCKED_BY,RELATED_TO,SUBTASK_OF relations
```

## Clean Architecture Layers

```mermaid
flowchart TD
    subgraph "Domain Layer"
        Entities["Entities<br>(WorkItem, Priority, WorkflowState)"]
        UseCases["Use Cases<br>(ItemService, WorkflowService)"]
    end
    
    subgraph "Interface Adapters"
        Controllers["Controllers<br>(CLI Commands)"]
        Repositories["Repository Implementations<br>(InMemoryItemRepository)"]
        Presenters["Presenters<br>(OutputFormatters)"]
    end
    
    subgraph "Frameworks & Drivers"
        DB["Databases<br>(SQLite)"]
        UI["UI Components"]
        ExternalAPIs["External APIs<br>(Jira, GitHub)"]
    end
    
    Entities --- UseCases
    UseCases --> Controllers
    UseCases --> Repositories
    UseCases --> Presenters
    Controllers --> UI
    Repositories --> DB
    Presenters --> ExternalAPIs
    
    classDef domain fill:#f9f,stroke:#333,stroke-width:2px
    classDef adapters fill:#ccf,stroke:#333,stroke-width:1px
    classDef frameworks fill:#cfc,stroke:#333,stroke-width:1px
    
    class Entities,UseCases domain
    class Controllers,Repositories,Presenters adapters
    class DB,UI,ExternalAPIs frameworks
```

## Development Methodology

```mermaid
flowchart TD
    subgraph "Testing Pyramid"
        Unit["Unit Tests"]
        Component["Component Tests"]
        Integration["Integration Tests"]
        Acceptance["Acceptance Tests"]
        Performance["Performance Tests"]
    end
    
    subgraph "Development Methodologies"
        TDD["Test-Driven Development<br>Red-Green-Refactor"]
        BDD["Behavior-Driven Development<br>Feature Files"]
        ZeroFriction["Zero-Friction Philosophy"]
    end
    
    Unit --> Component
    Component --> Integration
    Integration --> Acceptance
    Acceptance --> Performance
    
    TDD --> Unit
    TDD --> Component
    BDD --> Acceptance
    ZeroFriction --- TDD
    ZeroFriction --- BDD
    
    classDef tests fill:#9cf,stroke:#333
    classDef methods fill:#fc9,stroke:#333
    
    class Unit,Component,Integration,Acceptance,Performance tests
    class TDD,BDD,ZeroFriction methods
```