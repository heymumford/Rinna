# Rinna: Developer-Centric Workflow Management

<div align="center">

*A terminal-based task management tool for developers who code, not managers who report.*

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/heymumford/Rinna/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Go Version](https://img.shields.io/badge/go-1.21-blue.svg)](https://golang.org/doc/go1.21)
[![Version](https://img.shields.io/badge/version-1.2.4-blue.svg)](https://github.com/heymumford/Rinna/releases)
[![GitHub Stars](https://img.shields.io/github/stars/heymumford/Rinna?style=social)](https://github.com/heymumford/Rinna/stargazers)

[📥 Download](https://github.com/heymumford/Rinna/releases) • [📚 Documentation](docs/) • [🚀 Getting Started](docs/getting-started/README.md) • [🤝 Contribute](docs/development/contribution.md) • [📋 Changelog](CHANGELOG.md) • [📁 Folders](FOLDERS.md)

</div>

## What Is Rinna?

Rinna is a workflow management system built for software engineers. It minimizes process overhead and integrates directly into your development environment, providing clear visibility without excessive ceremony.

**Rinna isn't replacing enterprise tools – it exists to make workflow management work _for_ developers, not the other way around.**

### The Problem

Traditional workflow tools:
- Force context-switching away from coding
- Interrupt [flow state](docs/technical-specification.md#core-philosophy)
- Prioritize reporting over productivity
- Add unnecessary complexity

### The Solution

- **Terminal-first interface** integrates with git workflows and IDEs
- **Zero-friction workflow** adds only what's necessary
- **Developer-owned process** puts control in the right hands
- **Clean architecture** with Go API and Java core

## Work Model

- **Work Items**: Goals → Features → Bugs → Chores
- **Workflow**: Found → Triaged → To Do → In Progress → In Test → Done
- **No customization needed**: We've built what works based on experience

## Example Usage

### CLI

```bash
# Create a work item
bin/rin-cli add "Fix auth bypass" --type=BUG --priority=HIGH

# List work items in development
bin/rin-cli list --status=IN_DEV

# Update a work item
bin/rin-cli update WI-123 --status=DONE --assignee=developer1
```

### API

```bash
# Create work item via API
curl -X POST "http://localhost:8080/api/v1/workitems" \
  -H "Authorization: Bearer ri-dev-token" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement payment gateway",
    "type": "FEATURE",
    "priority": "HIGH"
  }'
```

## Installation

```bash
# Clone and build
git clone https://github.com/heymumford/Rinna.git
cd Rinna
chmod +x bin/rin bin/rin-version bin/rin-build bin/run-tests.sh
bin/rin build
```

### Build System

The Rinna build system supports multiple development workflows with a mode-based architecture:

```bash
# Quick iterations during development
bin/rin build fast

# Build with tests
bin/rin build test

# Full verification with coverage
bin/rin build verify

# Domain-specific tests
bin/rin build test domain:workflow

# Prepare for release
bin/rin build prepare-release
```

Advanced options include:
```bash
# Run parallel tests with coverage
bin/rin build test --parallel --coverage

# Monitor file changes and run tests automatically
bin/rin build test --watch

# Stop tests at first failure
bin/rin build test --fail-fast
```

See [Build System](docs/development/build-system.md) for detailed documentation.

### Maven Integration

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.2.4</version>
</dependency>
```

### Version Management

The project uses a centralized version management approach with `version.properties` as the single source of truth:

```bash
bin/rin version current   # View version information
bin/rin version patch     # Bump patch version
bin/rin version minor     # Bump minor version
bin/rin version verify    # Check consistency
bin/rin version update    # Sync all files with version.properties
bin/rin version release   # Create a release with git tag
```

Integration with the build system enables streamlined release workflows:
```bash
bin/rin build prepare-release  # Run tests, package, update version
```

See [Version Management](docs/development/version-management.md) for details on our centralized approach.

## Requirements

- Java 21+
- Go 1.21+ (for API server)
- Maven 3.8+
- `jq` for CLI client

## Comparison

| Feature | Rinna | Jira | GitHub Issues | Linear |
|---------|-------|------|--------------|--------|
| Focus | Developer experience | Management reporting | Issue tracking | Project management |
| Workflow | Fixed, streamlined | Highly customizable | Basic | Customizable |
| Git integration | Native | Plugin | Native | Plugin |
| Terminal-based | Yes | No | No | No |
| CLI | Full-featured | No | Limited | No |
| API | Go-powered | Yes | Yes | Yes |
| Learning curve | Low | High | Medium | Medium |

## FAQ

### Why no workflow customization?
Most customization adds complexity without value. Our workflow represents the essential states needed for effective development.

### Enterprise tool integration?
Rinna provides mapping capabilities to synchronize with tools like Jira while maintaining its clean workflow.

### Suitable for large teams?
Yes. The streamlined model works for teams of all sizes while maintaining consistency.

### Extensibility?
Rinna follows clean architecture principles with well-defined interfaces for extending functionality.

## Documentation

- [Getting Started](docs/getting-started/README.md)
- [User Guide](docs/user-guide/README.md)
- [Document Generation](docs/user-guide/documents.md)
- [Architecture](docs/development/architecture.md)
- [Development Guide](docs/development/README.md)
- [Build System](docs/development/build-system.md)
- [Version Management](docs/development/version-management.md)
- [Changelog](CHANGELOG.md)
- [Folder Structure](FOLDERS.md)

## License

[MIT License](LICENSE)

<div align="center">

*Built for developers who want to get things done, not fiddle with process.*

</div>