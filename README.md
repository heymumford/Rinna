# Rinna

[![Build Status](https://img.shields.io/badge/build-failing-red.svg)](https://github.com/heymumford/Rinna/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/heymumford/Rinna/releases)

## What Is Rinna?

Rinna is a developer-centric workflow management system explicitly designed for software engineers. Unlike traditional project management tools, Rinna integrates directly into the developer workflow, minimizing process overhead while maximizing visibility.

**Rinna isn't trying to replace enterprise tools – it exists to make workflow management work _for_ developers rather than the other way around.**

### The Problem

Traditional project/workflow management tools:
- Are designed primarily for reporting to management
- Force developers to context-switch away from their coding environment
- Introduce excessive ceremony that interrupts flow state
- Prioritize process over productivity
- Constrain rather than enable developers

### Rinna's Solution

Rinna embodies a fundamentally different approach:

- **Lives Where Developers Work**: Terminal-first interface that integrates with git workflows and IDEs
- **Zero-Friction Philosophy**: Never adds more process than absolutely necessary
- **Developer-Owned**: Complete workflow control by the people doing the work
- **Clear Visibility**: Simple, unambiguous work item tracking without the noise
- **Clean Architecture**: Designed for extensibility and adapting to your needs, not the other way around

[Learn more about our philosophy](docs/technical-specification.md)

## Work Model

Rinna uses a deliberate, opinionated workflow model built from hard-earned engineering experience:

- **Work Items**: Goals → Features → Bugs → Chores
- **Workflow**: Found → Triaged → To Do → In Progress → In Test → Done
- **Developer-Focused Attributes**: Effort estimates, assignees, blocking status

**The workflow isn't configurable because it doesn't need to be.** Rather than adapting to inefficient processes, Rinna implements a battle-tested flow that works. You can map its outputs to other systems if needed, but we won't compromise on what works.

[Explore the workflow model](docs/user-guide/workflow.md)

## Core Features

- **Terminal-First Interface**: Work directly from your development environment
- **Git Integration**: Update work status through commit messages and branches
- **Self-Contained**: Lightweight SQLite storage with no external dependencies
- **Clean API**: Modular Java design with clear interfaces
- **Language-Agnostic**: Core concepts apply regardless of programming language

## Example Usage

```bash
# Create a work item
bin/rin item create "Fix authentication bypass" --type bug --priority high

# Transition a work item
bin/rin workflow move ITEM-123 in-progress

# List items by state
bin/rin items list --state in-progress --assignee @me
```

[See full CLI reference](docs/user-guide/rin-cli.md)

## Documentation

- [Getting Started](docs/getting-started/README.md)
- [User Guide](docs/user-guide/README.md)
- [Technical Design](docs/technical-specification.md)
- [Architecture](docs/development/architecture.md)
- [Development Guide](docs/development/README.md)

## Installation

```bash
# Clone the repository
git clone https://github.com/heymumford/Rinna.git
cd Rinna

# Make scripts executable
chmod +x bin/rin bin/rin-version

# Build the project
bin/rin build
```

### Maven Integration

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Requirements

- Java 21+
- Maven wrapper (included)

## Contributing

Contributions are welcome! Please see our [Contributing Guide](docs/development/contribution.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

For questions or feedback, please [open an issue](https://github.com/heymumford/Rinna/issues) on GitHub.