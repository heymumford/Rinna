# Rinna Project

A polyglot workflow management system designed for efficient cross-language communication and unified workflow management, with a focus on clean architecture principles and high-performance execution.

> 💡 **Rinna Philosophy**: Rinna makes workflow management work _for_ developers rather than the other way around. It brings workflow management to where developers actually work: the command line.

## Quick Start

```bash
# Install Rinna
./install.sh

# Build all components
./build.sh all

# Start the Rinna server
./rinna-server start

# Create your first work item
rin create feature "My first feature"
```

## Key Features

- **Polyglot Architecture**: Leverages Java, Python, and Go for their respective strengths
- **Workflow Flexibility**: Adapts to any workflow methodology (Kanban, Scrum, Waterfall, etc.)
- **Clean Architecture**: Clear separation of concerns with dependencies pointing inward
- **Terminal-First Interface**: Lives where developers work
- **Quality Gates**: Configurable validation with context-aware rules
- **Universal Management**: From portfolio management to personal task tracking

## Project Structure

This project follows a clean polyglot architecture pattern with clear language boundaries:

```
/
├── java/                     # Java components (core domain models)
├── python/                   # Python components (data processing)
├── go/                       # Go components (high-performance APIs)
├── api-specs/                # API definition files
├── config/                   # Configuration files
├── docs/                     # Documentation
├── scripts/                  # Build and utility scripts
├── build/                    # Build output directory
└── logs/                     # Build and execution logs
```

## Documentation

Rinna uses [Antora](https://antora.org/) to provide comprehensive documentation across all components.

### Viewing Documentation

To build and view the documentation:

```bash
# Build and serve documentation
./build-docs.sh
```

This will open your browser to the documentation site.

### Documentation Content

The documentation includes:

- [Architecture](./docs/architecture/README.md) - System design documents
- [Getting Started Guides](./docs/guides/getting-started/README.md) - Installation and quick start
- [User Guides](./docs/guides/user/README.md) - End-user documentation
- [Developer Guides](./docs/guides/developer/README.md) - Developer documentation
- [API Reference](./docs/implementation/api/README.md) - API documentation
- [CLI Reference](./docs/implementation/cli/README.md) - Command-line interface reference

## Development Quick Reference

### Build Commands

```bash
# Build all components
./build.sh all [dev|test|prod]

# Build specific components
./build.sh java [dev|test|prod]
./build.sh python [dev|test|prod]
./build.sh go [dev|test|prod]
```

### Component Development

```bash
# Java
cd java && mvn clean install

# Python
cd python && poetry install

# Go
cd go && go build ./...
```

## Contributing

Please see [CONTRIBUTING.md](./docs/contributing/CONTRIBUTING.md) for information on how to contribute to the project.

## Community and Support

- Website: [rinnacloud.com](https://rinnacloud.com)
- GitHub: [github.com/heymumford/Rinna](https://github.com/heymumford/Rinna)
- Support: [support@rinnacloud.com](mailto:support@rinnacloud.com)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
