# Rinna

<div align="center">

![Rinna Logo](https://via.placeholder.com/150x150.png?text=Rinna)

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/heymumford/Rinna/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/heymumford/Rinna/releases)
[![GitHub Issues](https://img.shields.io/github/issues/heymumford/Rinna.svg)](https://github.com/heymumford/Rinna/issues)
[![GitHub PRs](https://img.shields.io/github/issues-pr/heymumford/Rinna.svg)](https://github.com/heymumford/Rinna/pulls)

**Workflow management for developers, by developers.**

</div>

## What Is Rinna?

Rinna is a streamlined, developer-centric workflow management system designed specifically for software engineers who want to focus on coding, not on managing tools. Unlike bloated enterprise solutions, Rinna provides just what developers need: clear visibility, minimal ceremony, and seamless integration with development workflows.

**Rinna isn't trying to replace Jira or other enterprise tools – it's solving a fundamentally different problem: how to make workflow management work _for_ developers rather than making developers work for their tools.**

### The Problem

Traditional workflow management tools:
- Are designed for managers, not developers
- Introduce excessive ceremony and process overhead
- Pull developers out of their coding environment
- Create friction in the development process
- Prioritize reporting over productivity

### How Rinna Solves It

Rinna embodies a radical philosophy: **workflow management should adapt to developers, not the other way around**.

- **Code-Adjacent**: Lives where developers work – in the terminal, IDE, and git workflow
- **Minimal Ceremony**: Lightweight processes that don't interrupt flow state
- **Developer-Owned**: Complete control over workflow by the people doing the work
- **Clean Architecture**: Extensible design that grows with your needs
- **Zero-Friction**: Integrate with existing tools without disrupting workflows

## Key Features

- **Terminal-First Interface**: Manage your workflow without leaving your development environment
- **Git Integration**: Update work status directly through commit messages
- **Self-Contained**: SQLite-based storage for individual or team use (PostgreSQL for scaled deployments)
- **Clean Architecture**: Modular design with clear boundaries and extensibility points
- **Explicit Workflow**: Clear, auditable state transitions for work items
- **Java 21 Powered**: Leveraging modern language features for a clean, maintainable codebase

## Quick Start

```bash
# Clone the repository
git clone https://github.com/heymumford/Rinna.git
cd Rinna

# Make scripts executable
chmod +x bin/rin bin/rin-version

# Build the project
bin/rin build

# Create a new work item
bin/rin item create "Implement authentication" --type feature

# List all work items
bin/rin items list

# Update status
bin/rin workflow move ITEM-123 in-progress
```

## Work Model

Rinna uses a straightforward work model designed for development teams:

- **Work Items**: Goals → Features → Bugs → Chores
- **Workflow**: Found → Triaged → To Do → In Progress → In Test → Done
- **Developer-Centric Attributes**: Effort, assignee, blocked status, and tags
- **Flexible Organization**: Work hierarchies without prescriptive methodologies

## Maven Integration

Include Rinna in your Java projects:

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Why Rinna Is Different

- **By Developers, For Developers**: Built from the ground up with developer experience at the core
- **Zero-Friction Philosophy**: Never adds more process than is absolutely necessary
- **Technology Agnostic**: Works with any stack, any platform, any methodology
- **No Lock-in**: Your data is yours, with simple import/export and standard formats
- **Extensible Architecture**: Add custom workflows, integrations, and visualizations

## Project Status

Rinna is in active development, following these principles:

- **Incremental Releases**: Delivering value early and often
- **Feature Completeness**: Each feature ships fully functional, not partially implemented
- **Community Driven**: Development priorities based on community feedback

## CLI Reference

Use the Rinna CLI for simplified workflow management:

```bash
# Show help and usage information
bin/rin --help

# Create a work item
bin/rin item create "Fix login bug" --type bug --priority high

# Transition a work item
bin/rin workflow move ITEM-123 in-progress

# Show item details
bin/rin item show ITEM-123

# List items by state
bin/rin items list --state in-progress
```

## Development

Build and test Rinna using the included tools:

```bash
# Clean, build, and test
bin/rin all

# Run tests with verbose output
bin/rin -v test

# Build with errors-only output
bin/rin -e build
```

## Documentation

- [Getting Started](docs/getting-started/README.md)
- [User Guide](docs/user-guide/README.md)
- [Technical Specification](docs/technical-specification.md)
- [Development Guide](docs/development/README.md)
- [Architecture](docs/development/architecture.md)
- [Java 21 Features](docs/development/java21-features.md)

## Requirements

- Java 21+
- Maven wrapper (included)

## Contributing

Contributions are welcome! Please see our [Contributing Guide](docs/development/contribution.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

Special thanks to the open-source community and all contributors who have helped shape Rinna's vision.