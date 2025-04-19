# Rinna Project Folder Structure

This document outlines the folder structure of the Rinna project, current status of the migration to a flatter structure, and guidelines for maintaining the organization.

## Current Structure

The Rinna project is currently organized into several core modules:

```
Rinna/
├── api/              # Go API server
│   ├── cmd/          # API command-line tools
│   ├── configs/      # API configuration
│   ├── internal/     # Internal API packages
│   ├── pkg/          # Reusable API packages
│   └── test/         # API test suite
├── bin/              # CLI scripts and tools
│   ├── checks/       # Architecture validation scripts
│   ├── rin*          # Main CLI utilities
├── config/           # Project-wide configuration
├── docs/             # Documentation
│   ├── architecture/ # Architecture diagrams and docs
│   ├── development/  # Developer guidelines
│   ├── getting-started/ # Onboarding docs
│   ├── specifications/ # Technical specifications
│   ├── testing/      # Testing strategies and test pyramid
│   └── user-guide/   # End-user documentation
├── python/           # Python modules
│   └── tests/        # Python tests
├── rinna-cli/        # Java CLI application
│   └── src/          # CLI source code
├── rinna-core/       # Core Java domain model
│   └── src/          # Core source code
├── src/              # Main application source
│   ├── main/         # Main application code
│   ├── test/         # Test code
│   └── test-doc/     # Test documentation
├── utils/            # Utility scripts
└── version-service/  # Version management service
    ├── adapters/     # Language adapters
    ├── cli/          # Version CLI
    └── core/         # Core version service
```

## Package Structure Migration

We are in the process of migrating to a cleaner package structure following Clean Architecture principles:

### Old Structure (Being Phased Out)
```
org.rinna.domain.entity    → Domain entities
org.rinna.domain.usecase   → Service interfaces
org.rinna.service.impl     → Service implementations
org.rinna.persistence      → Repository implementations
```

### New Structure (Being Adopted)
```
org.rinna.domain.model     → Domain entities
org.rinna.domain.service   → Service interfaces
org.rinna.domain.repository → Repository interfaces
org.rinna.adapter.service  → Service implementations
org.rinna.adapter.repository → Repository implementations
```

## Migration Status

| Module | Status | Notes |
|--------|--------|-------|
| rinna-core | ✅ Complete | All tests passing with new structure |
| src (main) | ✅ Complete | Migration finished |
| API | ✅ Complete | Package structure aligned with core |
| CLI | ⚠️ Disabled | Temporarily disabled due to type incompatibilities |

The CLI module is currently disabled in the build process. See [package-refactoring.md](/docs/development/package-refactoring.md) for more details and migration guidance.

## Guidelines for Code Organization

1. **Follow Clean Architecture Principles**
   - Domain layer should have no dependencies on outer layers
   - Service interfaces belong in the domain layer
   - Implementations belong in adapter packages

2. **Maintain Module Separation**
   - Each module should have a clear responsibility
   - Minimize dependencies between modules
   - Use interfaces for cross-module communication

3. **Package by Feature, Not Layer**
   - Group related functionality within modules
   - Avoid generic packages like "util" or "misc"
   - Aim for high cohesion within packages

4. **File Naming Conventions**
   - Domain entities: `WorkItem.java`, `DefaultWorkItem.java`
   - Repository interfaces: `ItemRepository.java`
   - Repository implementations: `InMemoryItemRepository.java`
   - Service interfaces: `WorkflowService.java`
   - Service implementations: `DefaultWorkflowService.java`

## Cross-Language Integration

Rinna uses multiple languages. Here's how they should interact:

1. **Java Core Domain**
   - Central domain model in Java
   - Service interfaces for domain operations

2. **Go API**
   - REST API implemented in Go
   - Communicates with Java services via client

3. **Python Tools**
   - Utilities and automation in Python
   - Documentation generation
   - Testing tools

4. **CLI**
   - Java-based CLI
   - Interfaces with core services

## Tools for Structure Maintenance

Several tools are available to help maintain the directory structure and ensure code quality:

```bash
# View package dependency graph
./bin/package-viz.sh

# Check for package structure violations
./bin/check-dependencies.sh

# Fix imports after moving classes
./bin/fix-imports.sh

# Run architecture validation checks
./bin/run-checks.sh

# Generate test pyramid coverage report
./bin/test-pyramid-coverage.sh
```

For detailed information on package dependencies and architectural rules, see the `/docs/development/package-structure.md` document.