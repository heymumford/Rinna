# Package Structure Refactoring

## Optimizing Folder Depth While Preserving Clean Architecture

This document outlines our strategy for reducing the maximum folder depth in the Rinna codebase from 13 to 9 levels while maintaining clean architecture principles and improving code maintainability.

## Current vs. Proposed Package Structure

| Layer | Current Structure | Proposed Structure |
|-------|------------------|-------------------|
| **Domain Entities** | org.rinna.domain.entity | org.rinna.domain |
| **Repository Interfaces** | org.rinna.domain.repository | org.rinna.repository |
| **Service Interfaces** | org.rinna.domain.usecase | org.rinna.usecase |
| **Repository Implementations** | org.rinna.adapter.persistence | org.rinna.persistence |
| **Service Implementations** | org.rinna.adapter.service | org.rinna.service |

## Clean Architecture Layers

Despite the flatter structure, our code will still adhere to clean architecture principles:

1. **Core Domain Layer**
   - Contains entities (`org.rinna.domain`)
   - Contains business rules and logic
   - Has no dependencies on outer layers

2. **Interface Layer**
   - Contains repository interfaces (`org.rinna.repository`)
   - Contains service interfaces (`org.rinna.usecase`)
   - Depends only on the domain layer

3. **Implementation Layer**
   - Contains repository implementations (`org.rinna.persistence`)
   - Contains service implementations (`org.rinna.service`)
   - Implements interfaces from the interface layer
   - Depends on domain and interface layers

4. **Application Layer**
   - Contains application-specific logic
   - Orchestrates use cases
   - Depends on all inner layers

5. **Infrastructure Layer**
   - Contains frameworks, tools, and drivers
   - Depends on all inner layers

## Dependency Flow

The dependency flow remains the same in our refactored structure:

```
Infrastructure → Application → Implementation → Interface → Domain
```

The arrows point toward dependencies, showing that inner layers don't depend on outer layers.

## File Organization

With a flatter structure, files within each package will be organized by feature or concept:

- **Domain Package** (`org.rinna.domain`):
  - Work items: `WorkItem.java`, `DefaultWorkItem.java`
  - Releases: `Release.java`, `DefaultRelease.java` 
  - Projects: `Project.java`, `DefaultProject.java`
  - States: `WorkflowState.java`, `Priority.java`

- **Repository Package** (`org.rinna.repository`):
  - `ItemRepository.java`
  - `ReleaseRepository.java`
  - `ProjectRepository.java`

- **Service Package** (`org.rinna.usecase`):
  - `WorkflowService.java`
  - `ItemService.java`
  - `ReleaseService.java`

## Module Structure

In addition to flattening the package structure, we'll also simplify the module structure:

1. Move core functionality from `rinna-core/src` directly into `src`
2. Maintain Maven's standard directory structure (`src/main/java`, `src/test/java`)

## Migration Strategy

We'll implement this change incrementally:

1. Create the new package structure
2. Move one package at a time, starting with domain entities
3. Update import statements for each package after migration
4. Run tests after each package migration to catch issues early
5. Update build files as needed

## Tooling Support

We'll create migration scripts to help with:

1. Updating package declarations in Java files
2. Updating import statements throughout the codebase
3. Moving files to their new locations
4. Validating the correctness of the migration

## Example Migration

For the `WorkItem` entity:

**Current:**
```java
// File: rinna-core/src/main/java/org/rinna/domain/entity/WorkItem.java
package org.rinna.domain.entity;
// ...
```

**Proposed:**
```java
// File: src/main/java/org/rinna/domain/WorkItem.java
package org.rinna.domain;
// ...
```

## Benefits

This refactoring will:

1. Reduce cognitive load by simplifying the folder structure
2. Make it easier to find and navigate between related files
3. Align better with Java community practices
4. Improve build times with shorter paths
5. Preserve clean architecture principles
6. Make the codebase more approachable for new developers