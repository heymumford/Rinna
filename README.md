# Rinna

Streamlined workflow management for self-driven engineering teams.

## Purpose

Rinna delivers explicit workflow tracking, release management, and development cycle management with intentional clarity and minimum overhead.

## Core Elements

- **Work Items**: Goals → Features → Bugs/Chores
- **Workflow**: Found → Triaged → To Do → In Progress → In Test → Done
- **Versioning**: Enforced semantic versioning (major.minor.patch)
- **Lota**: Configurable development cycles with defined ceremonies

## Architecture

Rinna is built using Clean Architecture principles:

- **Domain Layer**: Core business logic and entities
- **Use Cases**: Application-specific business rules
- **Adapters**: Interface with external systems
- **Frameworks**: External dependencies and delivery mechanisms

This architecture ensures:
- Separation of concerns
- Testability
- Flexibility to change frameworks
- Independence of business logic from technical details

## Testing

Rinna uses a comprehensive testing approach:

- **Unit Tests**: Testing individual components
- **BDD Tests**: Behavior-driven tests for verifying system behavior
- **CI/CD**: Automated testing and deployment through GitHub Actions

## Integration

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Documentation

- [Getting Started](docs/getting-started/README.md)
- [User Guide](docs/user-guide/README.md)
- [Technical Specification](docs/technical-specification.md)
- [Development Guide](docs/development/README.md)
- [Testing Strategy](docs/development/testing.md)
- [Architecture](docs/development/architecture.md)

## Requirements

- Java 11+
- Maven wrapper (included)

## Development

Use the Rinna CLI tool for simplified build and test management:

```bash
# Make scripts executable
chmod +x bin/rin bin/rin-version

# Show help and usage information
bin/rin --help

# Clean, build, and test with default output
bin/rin all

# Run tests with verbose output
bin/rin -v test

# Build with errors-only output
bin/rin -e build
```

See the [CLI documentation](docs/user-guide/rin-cli.md) for more options.

## License

MIT