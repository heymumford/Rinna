# Rinna Package Structure

This document describes the package structure of the Rinna application, following clean architecture principles with a simplified and flattened hierarchy.

## Overview

The Rinna package structure follows the principles of Clean Architecture, which separates code into distinct layers with clear dependencies flowing from the outside in:

```
org.rinna
  ├── adapter       # Framework adapters (outside layer)
  │   ├── repository  # Concrete repository implementations
  │   └── service     # Service implementations
  ├── config        # Application configuration
  ├── domain        # Core domain (inner layer)
  │   ├── model       # Domain model entities
  │   ├── repository  # Repository interfaces
  │   └── service     # Service interfaces
  └── [legacy]      # Legacy packages (for backward compatibility)
```

## Package Responsibilities

### Domain Layer

The domain layer contains the core business logic and entities of the application. It has no dependencies on other layers.

#### `org.rinna.domain.model`

Contains the domain model classes that represent the core business entities. These classes are pure Java and have no dependencies on frameworks or libraries.

Examples:
- `WorkItem`
- `WorkQueue`
- `Release`
- `Priority` (enum)
- `WorkflowState` (enum)

#### `org.rinna.domain.service`

Contains the service interfaces that define the business operations available in the domain. Previously known as `usecase`.

Examples:
- `ItemService`
- `WorkflowService`
- `ReleaseService`
- `QueueService`

#### `org.rinna.domain.repository`

Contains the repository interfaces that define the data access operations available in the domain.

Examples:
- `ItemRepository`
- `ReleaseRepository`
- `QueueRepository`
- `MetadataRepository`

### Adapter Layer

The adapter layer contains implementations of the interfaces defined in the domain layer. It depends on the domain layer but not vice versa.

#### `org.rinna.adapter.service`

Contains implementations of the service interfaces defined in the domain layer. Previously known as `service.impl`.

Examples:
- `DefaultItemService`
- `DefaultWorkflowService`
- `DefaultReleaseService`
- `DefaultQueueService`

#### `org.rinna.adapter.repository`

Contains implementations of the repository interfaces defined in the domain layer. Previously known as `persistence`.

Examples:
- `InMemoryItemRepository`
- `InMemoryReleaseRepository`
- `InMemoryQueueRepository`
- `InMemoryMetadataRepository`

### Configuration

#### `org.rinna.config`

Contains configuration classes for the application.

Example:
- `RinnaConfig`
- `LoggingBridge`

## Legacy Packages

The following packages are maintained for backward compatibility but are deprecated and should not be used in new code:

- `org.rinna.model` → use `org.rinna.domain.model` instead
- `org.rinna.repository` → use `org.rinna.domain.repository` instead  
- `org.rinna.usecase` → use `org.rinna.domain.service` instead

## Migration Guide

If you're working with existing code that uses the old package structure, you should use the following mappings:

| Old Package | New Package |
|-------------|-------------|
| `org.rinna.domain.entity` | `org.rinna.domain.model` |
| `org.rinna.domain.usecase` | `org.rinna.domain.service` |
| `org.rinna.service.impl` | `org.rinna.adapter.service` |
| `org.rinna.persistence` | `org.rinna.adapter.repository` |

You can use the migration scripts in `bin/migration/` to automate the updates of import statements in your code.
