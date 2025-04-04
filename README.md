# Rinna

Streamlined workflow management for self-driven engineering teams.

## Purpose

Rinna delivers explicit workflow tracking, release management, and development cycle management with intentional clarity and minimum overhead.

## Core Elements

- **Work Items**: Goals → Features → Bugs/Chores
- **Workflow**: Found → Triaged → To Do → In Progress → In Test → Done
- **Versioning**: Enforced semantic versioning (major.minor.patch)
- **Lota**: Configurable development cycles with defined ceremonies

## Integration

```xml
<dependency>
    <groupId>org.samstraumr</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Documentation

- [Getting Started](docs/getting-started/README.md)
- [User Guide](docs/user-guide/README.md)
- [Technical Specification](docs/technical-specification.md)
- [Developer Guide](docs/development/README.md)

## Requirements

- Java 11+
- Maven 3.6+

## Development

Use the Rinna CLI tool for simplified build and test management:

```bash
# Show help and usage information
bin/rin --help

# Clean, build, and test with default output
bin/rin all

# Run tests with verbose output
bin/rin -v test

# Build with errors-only output
bin/rin -e build
```

## License

MIT