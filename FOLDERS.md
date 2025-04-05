# Rinna Project Folder Structure

This document proposes changes to the Rinna project folder structure to reduce the maximum folder depth from 13 to 9 levels while maintaining clean architecture principles and code maintainability.

## Current Structure

The current path to Java files follows this pattern:
```
/home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/entity/DefaultWorkItem.java
```

This is 13 levels deep (including the file itself).

## Proposed Structure

We propose flattening the folder structure to a maximum of 9 levels:
```
/home/emumford/NativeLinuxProjects/Rinna/src/main/java/org/rinna/domain/DefaultWorkItem.java
```

## Key Changes

1. **Eliminate the `rinna-core` module nesting**
   - Move core functionality directly into `src/main/java`
   - This eliminates one level of nesting

2. **Flatten the package structure**
   - Move from `org.rinna.domain.entity` to `org.rinna.domain`
   - Move from `org.rinna.domain.repository` to `org.rinna.repository`
   - Move from `org.rinna.domain.usecase` to `org.rinna.usecase`
   - Move from `org.rinna.adapter.service` to `org.rinna.service`
   - Move from `org.rinna.adapter.persistence` to `org.rinna.persistence`

3. **Use more descriptive file names**
   - Since we're flattening the packages, file names must be more descriptive
   - Example: `DefaultWorkItem.java` in the `domain` package

## Java Package Structure

Our refactored package structure adheres to clean architecture but with a flatter hierarchy:

```
org.rinna.*          → Base package for all code
org.rinna.domain     → Domain entities (formerly org.rinna.domain.entity)
org.rinna.repository → Repository interfaces (formerly org.rinna.domain.repository)
org.rinna.usecase    → Service interfaces (formerly org.rinna.domain.usecase)
org.rinna.service    → Service implementations (formerly org.rinna.adapter.service)
org.rinna.persistence → Repository implementations (formerly org.rinna.adapter.persistence)
org.rinna.api        → API-related code
org.rinna.config     → Configuration code
```

## File Naming Conventions

With a flatter structure, file naming becomes more important:

- Domain entities: `WorkItem.java`, `WorkItemImpl.java`
- Repository interfaces: `WorkItemRepository.java`
- Repository implementations: `InMemoryWorkItemRepository.java`
- Service interfaces: `WorkflowService.java`
- Service implementations: `DefaultWorkflowService.java`

## Benefits of This Approach

1. **Reduced complexity** - Fewer folder levels to navigate
2. **Improved build times** - Shorter paths for build tools to traverse
3. **Easier file location** - More intuitive structure for developers
4. **Preserved architecture** - Clean architecture principles are maintained
5. **Better alignment with Java conventions** - Most Java projects don't use deeply nested packages

## Implementation Plan

1. Create the new directory structure
2. Move files with updated package declarations
3. Update import statements across the codebase
4. Update build files to reflect the new structure
5. Run comprehensive tests to ensure functionality is preserved

This approach will result in a maximum folder depth of 9 levels while maintaining or improving code maintainability.