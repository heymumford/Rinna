# 4. Refactor Package Structure to Align with Clean Architecture

Date: 2025-04-07

## Status

Accepted

## Context

The Rinna project was initially developed with a package structure that did not fully align with Clean Architecture principles. As the codebase evolved, several issues became apparent:

1. Inconsistent package naming that did not clearly reflect architectural layers
2. Business logic and implementations were not cleanly separated
3. Domain entities and service interfaces were scattered across different packages
4. The package structure did not enforce Clean Architecture's dependency rules
5. Different naming conventions were used inconsistently (e.g., "entity" vs "model", "usecase" vs "service")

Clean Architecture emphasizes clear separation of concerns with dependencies pointing inward toward the domain layer. Our previous package structure did not consistently enforce these boundaries, making it harder to maintain and extend the codebase.

Key issues with the old package structure included:
- Domain entities were in `org.rinna.domain.entity` while some were also in `org.rinna.model`
- Service interfaces were in `org.rinna.domain.usecase` with inconsistent naming
- Service implementations were in `org.rinna.service.impl` when they should be adapters
- Repository implementations were in `org.rinna.persistence` instead of clearly marked as adapters

This inconsistency made it difficult for new developers to understand the architecture and led to confusion about where new code should be placed.

## Decision

We will refactor the package structure to properly align with Clean Architecture principles, using a more consistent and intuitive naming convention:

**Old Structure (Being Phased Out)**
```
org.rinna.domain.entity    → Domain entities
org.rinna.domain.usecase   → Service interfaces
org.rinna.service.impl     → Service implementations
org.rinna.persistence      → Repository implementations
```

**New Structure (Being Adopted)**
```
org.rinna.domain.model     → Domain entities
org.rinna.domain.service   → Service interfaces
org.rinna.domain.repository → Repository interfaces
org.rinna.adapter.service  → Service implementations
org.rinna.adapter.repository → Repository implementations
```

The refactoring will be applied progressively across the codebase, starting with the core modules and gradually extending to the API and CLI components. To support the transition, we will:

1. Create migration scripts to automate import statement updates
2. Maintain temporary compatibility classes during the transition
3. Update build tools to recognize both old and new package structures
4. Document the migration process to guide developers

The new package structure clearly separates:
- Domain layer (containing entities, interfaces for services and repositories)
- Adapter layer (containing implementations of domain interfaces)

This structure reinforces the dependency rule of Clean Architecture: inner layers have no knowledge of outer layers, with dependencies pointing only inward.

## Consequences

### Positive Consequences

1. **Improved architectural clarity**: The package structure now clearly reflects Clean Architecture layers
2. **Better separation of concerns**: Clear distinction between domain model, interfaces, and implementations
3. **Consistent naming conventions**: Package and class names follow a uniform pattern
4. **Enhanced maintainability**: New developers can more easily understand the codebase structure
5. **Stronger enforcement of Clean Architecture principles**: Package structure reinforces proper dependency direction
6. **Better testability**: Clean separation makes unit testing domain logic easier

### Challenges

1. **Transition period complexity**: During migration, both old and new structures will coexist
   - Mitigation: Migration scripts to help with import changes and clear documentation
   
2. **Learning curve for existing team members**: Developers need to learn the new structure
   - Mitigation: Documentation, examples, and pair programming sessions

3. **Potential for regression bugs**: Moving classes can break existing functionality
   - Mitigation: Comprehensive test coverage and staged implementation approach

4. **Build and CI integration**: Need to ensure build processes work with changing package structure
   - Mitigation: Update CI configurations and build scripts in parallel with code changes

### Implementation Plan

1. Complete the `rinna-core` module refactoring (✅ Already done)
2. Refactor the main application code in the `src` module (🔄 In progress)
3. Update the API module to align with the new structure
4. Refactor the CLI module
5. Remove legacy compatibility classes after all modules are migrated
6. Update documentation and examples to use the new package structure exclusively

### Migration Tools

Several tools have been developed to aid in the migration:
- `bin/migration/fix-imports.sh`: Updates import statements
- `bin/package-viz.sh`: Visualize package dependencies
- `bin/check-dependencies.sh`: Validates architectural boundaries

These tools will help ensure a smooth transition while maintaining correct architectural dependencies.