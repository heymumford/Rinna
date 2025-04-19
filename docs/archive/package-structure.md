# Rinna Package Structure

This document describes the package structure for the Rinna project, which follows Clean Architecture principles. We are currently in a transition phase, moving from the old package structure to a new, more consistent structure.

## Clean Architecture Package Structure

The Rinna project follows Clean Architecture principles with the following layers:

![Clean Architecture Diagram](../diagrams/rinna_clean_architecture_diagram.svg)

1. **Domain Layer** (Innermost)
   - Contains business entities and core business logic
   - Defines interfaces that outer layers implement
   - Has no dependencies on other layers

2. **Use Case Layer**
   - Contains application-specific business rules
   - Depends only on the Domain layer
   - Defines interfaces for external dependencies

3. **Interface Adapters Layer**
   - Contains implementations of the interfaces defined in inner layers
   - Converts data between formats suitable for use cases and external frameworks
   - Includes controllers, presenters, gateways, and repositories

4. **Frameworks & Drivers Layer** (Outermost)
   - Contains framework details, UI, databases, etc.
   - Implements interfaces defined by inner layers
   - Connects to external systems

## Package Structure Migration

The codebase is currently being migrated from the old package structure to a new structure that better aligns with Clean Architecture principles.

### Old Structure (Being Phased Out)

```
org.rinna.domain.entity    → Domain entities
org.rinna.domain.usecase   → Service interfaces
org.rinna.service.impl     → Service implementations
org.rinna.persistence      → Repository implementations
org.rinna.model            → Some domain entities
```

### New Structure (Being Adopted)

```
org.rinna.domain.model     → Domain entities
org.rinna.domain.service   → Service interfaces
org.rinna.domain.repository → Repository interfaces
org.rinna.adapter.service  → Service implementations
org.rinna.adapter.repository → Repository implementations
```

### Migration Status

- ✅ Core domain model has been migrated to the new structure
- ✅ Repository interfaces have been moved to `org.rinna.domain.repository`
- ✅ Service interfaces have been moved to `org.rinna.domain.service`
- 🔄 Repository implementations are being migrated to `org.rinna.adapter.repository`
- 🔄 Service implementations are being migrated to `org.rinna.adapter.service`
- ⏳ Compatibility classes for backward compatibility are temporarily maintained
- ⏳ Test classes are being updated to use the new package structure

## Modules Package Structure

### Core Module (`rinna-core`)

The core module contains the domain model and core business logic:

```
org.rinna.domain.model     → Core domain entities
org.rinna.domain.service   → Service interfaces
org.rinna.domain.repository → Repository interfaces
org.rinna.adapter.service  → Service implementations
org.rinna.adapter.repository → Repository implementations
```

### CLI Module (`rinna-cli`)

The CLI module provides command-line interfaces:

```
org.rinna.cli              → Command-line application
org.rinna.cli.command      → Command implementations
org.rinna.cli.model        → CLI-specific models
org.rinna.cli.service      → CLI services
```

### API Module (`api`)

The Go-based API module follows a similar structure:

```
api/internal/models        → API data models
api/internal/handlers      → Request handlers
api/internal/middleware    → API middleware
api/internal/client        → Client code for API consumers
```

## Package Dependencies

The dependency rule of Clean Architecture must be followed:

1. Domain layer has no dependencies on other layers
2. Use Case layer depends only on Domain layer
3. Interface Adapters layer depends on Use Case and Domain layers
4. Frameworks & Drivers layer depends on all inner layers

Any dependency that violates these rules should be considered a technical debt and refactored.

## Guidelines for New Code

When adding new code to the project:

1. Place domain entities in `org.rinna.domain.model`
2. Place service interfaces in `org.rinna.domain.service`
3. Place repository interfaces in `org.rinna.domain.repository`
4. Place service implementations in `org.rinna.adapter.service`
5. Place repository implementations in `org.rinna.adapter.repository`

## Migration Tools

Several tools have been developed to aid in the migration:

- `bin/migration/fix-imports.sh`: Updates import statements
- `bin/package-viz.sh`: Visualize package dependencies
- `bin/check-dependencies.sh`: Validates architectural boundaries

## References

- [ADR-0003: Adopt Clean Architecture for System Design](../architecture/decisions/0003-adopt-clean-architecture-for-system-design.md)
- [ADR-0004: Refactor Package Structure to Align with Clean Architecture](../architecture/decisions/0004-refactor-package-structure-to-align-with-clean-architecture.md)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)