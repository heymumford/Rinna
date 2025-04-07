# Rinna Developer Guide

Welcome to the Rinna Developer Guide! This document is intended for developers who want to contribute to Rinna or extend its functionality. If you're looking for user documentation, please check the [User Guide](docs/user-guide/README.md).

## Quick Start for Developers

```bash
# Clone repository
git clone https://github.com/heymumford/Rinna.git
cd Rinna

# Set up development environment
bin/rin-setup-unified --developer

# Build the project
bin/rin build

# Run tests
bin/rin test
```

## Development Environment

Rinna is a polyglot project that uses:
- Java 21+ (core domain logic)
- Go 1.21+ (API and services)
- Python 3.8+ (utilities and scripts)
- Bash (CLI tools and utilities)

See the [Environment Setup](docs/development/environment-setup.md) guide for detailed instructions.

## Architecture

Rinna follows Clean Architecture principles:

```
┌────────────────────────────────────────────────────────────┐
│                     Presentation Layer                     │
│ ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│ │      CLI       │  │      API       │  │      GUI       │ │
│ └────────────────┘  └────────────────┘  └────────────────┘ │
└───────────────────────────┬────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────┐
│                 Application Service Layer                  │
│ ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│ │    Services    │  │   Use Cases    │  │  Interactors   │ │
│ └────────────────┘  └────────────────┘  └────────────────┘ │
└───────────────────────────┬────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────┐
│                      Domain Layer                          │
│ ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│ │    Entities    │  │   Interfaces   │  │     Rules      │ │
│ └────────────────┘  └────────────────┘  └────────────────┘ │
└───────────────────────────┬────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────┐
│                 Infrastructure Layer                       │
│ ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│ │  Repositories  │  │   Adapters     │  │  Persistence   │ │
│ └────────────────┘  └────────────────┘  └────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

See [Architecture Documentation](docs/development/architecture.md) for details.

## Project Structure

```
rinna/
├── api/               # Go API server
├── bin/               # CLI tools and utilities
├── docs/              # Documentation
│   ├── architecture/  # Architectural documentation
│   ├── development/   # Developer guides and reference
│   ├── getting-started/ # Initial setup guides
│   ├── reference/     # Reference documentation
│   ├── testing/       # Testing guides and strategy
│   └── user-guide/    # End-user documentation
├── rinna-cli/         # Java CLI components
├── rinna-core/        # Java core domain model
├── src/               # Legacy source code (being migrated)
└── version-service/   # Version management service
```

See [Project Structure](docs/development/codebase-organization.md) for details.

## Developer Workflow

### 1. Set Up Your Environment

Follow the [Environment Setup](docs/development/environment-setup.md) guide.

### 2. Understanding the Code

1. Start with the [Architecture Guide](docs/development/architecture.md)
2. Review the [Clean Architecture Principles](docs/architecture/decisions/0003-adopt-clean-architecture-for-system-design.md)
3. Understand the [Package Structure](docs/development/package-structure.md)

### 3. Development Process

1. Create a branch for your work: `git checkout -b feature/your-feature`
2. Implement your changes following our [Code Standards](docs/reference/standards/code-review-guidelines.md)
3. Write tests following our [Testing Strategy](docs/testing/TESTING_STRATEGY.md)
4. Submit a pull request

## Key Developer Tools

### Build System

Rinna uses a sophisticated build system to manage the polyglot codebase:

```bash
# Quick iterations during development
bin/rin build fast

# Build with tests
bin/rin build test

# Full verification with coverage
bin/rin build verify
```

See [Build System](docs/development/build-system.md) for details.

### Testing

We follow a comprehensive testing strategy based on the testing pyramid:

```bash
# Run tests by pyramid layer
bin/rin test unit          # Run unit tests
bin/rin test component     # Run component tests
bin/rin test integration   # Run integration tests
bin/rin test acceptance    # Run acceptance tests
bin/rin test performance   # Run performance tests

# Run test combinations
bin/rin test fast          # Run unit and component tests
bin/rin test essential     # Run unit, component, and integration
```

See [Testing Strategy](docs/testing/TESTING_STRATEGY.md) for details.

### Version Management

Rinna uses a centralized version management system:

```bash
bin/rin-version current   # View version information
bin/rin-version patch     # Bump patch version
bin/rin-version minor     # Bump minor version
bin/rin-version verify    # Check consistency
bin/rin-version update    # Sync all files with version.properties
```

See [Version Management](docs/development/version-management.md) for details.

## Specialized Developer Guides

- [Java 21 Features](docs/development/java21-features.md)
- [Cross-Language Logging](docs/development/cross-language-logging.md)
- [Dependency Management](docs/development/dependency-management.md)
- [CI Workflow](docs/development/ci-workflow.md)
- [Test-Driven Development](docs/testing/TDD_FEATURES.md)

## Reference Documentation

- [Coding Standards](docs/reference/standards/code-review-guidelines.md)
- [Documentation Requirements](docs/reference/standards/documentation-requirements.md)
- [Logging Guidelines](docs/reference/standards/logging-guidelines.md)
- [Naming Conventions](docs/reference/standards/naming-conventions.md)
- [TODO Review Process](docs/reference/standards/todo-review-process.md)
- [Version Numbering](docs/reference/standards/version-numbering.md)

## Contributing

Please see our [Contribution Guidelines](CONTRIBUTING.md) for details on how to contribute to Rinna.

## Architecture Decision Records (ADRs)

We document significant architectural decisions in our ADR directory:

- [ADR-0003: Adopt Clean Architecture](docs/architecture/decisions/0003-adopt-clean-architecture-for-system-design.md)
- [ADR-0004: Refactor Package Structure](docs/architecture/decisions/0004-refactor-package-structure-to-align-with-clean-architecture.md)
- [ADR-0005: Multi-Language Approach](docs/architecture/decisions/0005-adopt-multi-language-approach-for-system-components.md)
- [ADR-0006: Testing Pyramid Strategy](docs/architecture/decisions/0006-implement-comprehensive-testing-pyramid-strategy.md)
- [ADR-0007: Security Framework](docs/architecture/decisions/0007-establish-security-compliance-framework.md)

## License

This project is licensed under the [MIT License](LICENSE).