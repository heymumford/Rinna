<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford], Gemini Deep Research, Claude 3.7. -->

<!--
Rinna: A developer-centric workflow management system for software engineers who want less process overhead 
and more productivity. Designed as a lightweight alternative to heavyweight enterprise tools.

Keywords: workflow management, developer tools, task tracking, agile development, software development process, CLI tool, 
Java, project management, workflow automation
-->

# Rinna: Developer-Centric Workflow Management for Software Engineers

<div align="center">

*A streamlined task management CLI (Command-Line Interface) designed for developers who code, not for EPM (Ever-Probing Managers) who report.*

[![Build Status](https://img.shields.io/badge/build-failing-red.svg)](https://github.com/heymumford/Rinna/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Go Version](https://img.shields.io/badge/go-1.21-blue.svg)](https://golang.org/doc/go1.21)
[![Version](https://img.shields.io/badge/version-1.1.4-blue.svg)](https://github.com/heymumford/Rinna/releases)
[![GitHub Stars](https://img.shields.io/github/stars/heymumford/Rinna?style=social)](https://github.com/heymumford/Rinna/stargazers)
[![Twitter Follow](https://img.shields.io/twitter/follow/heymumford?style=social)](https://twitter.com/heymumford)

[📥 Download](https://github.com/heymumford/Rinna/releases) • [📚 Documentation](docs/) • [🚀 Getting Started](docs/getting-started/README.md) • [🤝 Contribute](docs/development/contribution.md)

</div>

<div align="center">

## Quick Navigation

| **Project Overview** | **Using Rinna** | **Reference** | **Community** |
|:-------------------:|:---------------:|:-------------:|:-------------:|
| [What Is Rinna?](#what-is-rinna) | [Example Usage](#example-usage) | [Documentation](#documentation) | [Contributing](#contributing) |
| [The Problem](#the-problem) | [Installation](#installation) | [Requirements](#requirements) | [License](#license) |
| [Rinna's Solution](#rinnas-solution) | [Maven Integration](#maven-integration) | [Alternatives](#comparison-with-alternatives) | [Related Projects](#related-projects) |
| [Work Model](#work-model) | | [FAQ](#frequently-asked-questions) | [Contact](#contact) |
| [Core Features](#core-features) | | | |

</div>

## What Is Rinna?

Rinna is a developer-centric workflow management system explicitly designed for software engineers. Unlike traditional project management tools, Rinna integrates directly into the developer workflow, minimizing process overhead while maximizing visibility.

**Rinna isn't trying to replace enterprise tools – it exists to make workflow management work _for_ developers rather than the other way around.**

> Rinna is like a clean moist sponge being run across a chaos of flour and sugar and dough after a weekend afternoon of making cookies with children. Project planning is inherently messy and the intention is always to produce predictable results -- but you can't leave the kitchen a wreck. Stop forcing your team into new SDLCs and paying for a space shuttle of a software management solution when all you need is a sport compact car to get around.

### The Problem

Traditional project/workflow management tools:
- Are designed primarily for [reporting to management](docs/specifications/engineering_spec.md#negative-outcomes-prevented)
- Force developers to context-switch away from their coding environment
- Introduce excessive ceremony that interrupts [flow state](docs/technical-specification.md#core-philosophy)
- Prioritize process over productivity
- Constrain rather than enable developers

We've [seen the problems](docs/user-guide/README.md) with existing tools and built Rinna specifically to avoid them.

### Rinna's Solution

Rinna embodies a fundamentally different approach:

- **Lives Where Developers Work**: [Terminal-first interface](docs/user-guide/rin-CLI.md) that integrates with [git workflows](docs/user-guide/workflow.md) and IDEs
- **Zero-Friction Philosophy**: Never adds more process than [absolutely necessary](docs/technical-specification.md#core-philosophy)
- **Developer-Owned**: Complete workflow control by the [people doing the work](docs/development/design-approach.md)
- **Clear Visibility**: Simple, unambiguous [work item tracking](docs/user-guide/lota.md) without the noise
- **Polyglot Architecture**: Go API layer for speed with Java core for robust business logic
- **Clean Architecture**: Designed with [hexagonal principles](docs/development/architecture.md) for extensibility

[Learn more about our philosophy](docs/technical-specification.md)

## Work Model

Rinna uses a deliberate, opinionated workflow model built from hard-earned engineering experience:

- **Work Items**: Goals → Features → Bugs → Chores
- **Workflow**: Found → Triaged → To Do → In Progress → In Test → Done
- **Developer-Focused Attributes**: Effort estimates, assignees, blocking status

Can you customize the workflow? No, you can't. [That's the point](docs/user-guide/workflow-philosophy.md).

**The workflow isn't configurable because it doesn't need to be.** Rather than adapting to inefficient processes, Rinna implements a [battle-tested flow](docs/user-guide/workflow.md) that works. You can map its outputs to other systems if needed, but [we won't compromise on what works](docs/specifications/engineering_spec.md#core-philosophy). 😉

[Explore the workflow model](docs/user-guide/workflow.md)

## Core Features

- **Terminal-First Interface**: Work directly from your [development environment](docs/user-guide/rin-CLI.md)
- **Git Integration**: Update work status through [commit messages](docs/user-guide/API-integration.md) and branches
- **Self-Contained**: [Lightweight SQLite storage](docs/development/architecture.md#module-structure) with no external dependencies
- **High-Performance API**: Go-based API layer provides lightning-fast responses
- **Robust Business Logic**: Java core with strong domain model ensures correctness
- **Clean Architecture**: [Modular polyglot design](docs/development/architecture.md) with clear interfaces
- **Language-Agnostic**: [Core concepts](docs/technical-specification.md) apply regardless of programming language

## Example Usage

```bash
# Create a work item
bin/rin-cli add "Fix authentication bypass" --type=BUG --priority=HIGH

# View a work item
bin/rin-cli view WI-123

# List work items
bin/rin-cli list --status=IN_DEV

# Update a work item
bin/rin-cli update WI-123 --status=DONE --assignee=developer1
```

[See full CLI (Command-Line Interface) reference](docs/user-guide/rin-CLI.md)

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
- Go 1.21+ (for API server)
- Maven 3.8+ (system installation)
- `jq` for the CLI client

## Contributing

Contributions are welcome! Please see our [Contributing Guide](docs/development/contribution.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Comparison with Alternatives

| Feature | Rinna | Jira | GitHub Issues | Linear |
|---------|-------|------|--------------|--------|
| Primary focus | Developer experience | Management reporting | Issue tracking | Project management |
| Workflow customization | No (intentionally) | Extensive | Limited | Moderate |
| Integration with Git | Native | Plugin | Native | Plugin |
| CLI (Command-Line Interface) | Yes | No | Limited | No |
| Terminal-based | Yes | No | No | No |
| Enterprise mapping | Yes | N/A | Limited | Limited |
| Ceremony level | Minimal | Extensive | Moderate | Moderate |
| Target user | Developers | Managers | Developers & Managers | Product teams |
| Learning curve | Low | High | Medium | Medium |
| Java support | Native (Java 21) | Web interface | No | No |

## Frequently Asked Questions

### Why doesn't Rinna allow workflow customization?
Rinna implements a battle-tested native developer workflow. Most "different" processes add unnecessary complexity from internal politics and legacy decisions. [Learn more about our workflow philosophy](docs/user-guide/workflow-philosophy.md).

### How does Rinna integrate with enterprise tools like Jira?
Rinna provides mapping capabilities to synchronize with mandatory enterprise tools. Work in Rinna's clean workflow locally while satisfying management reporting needs.

### What makes Rinna different from other task tracking tools?
Rinna is built specifically for developers, not managers. It lives in your terminal, integrates with your coding workflow, and minimizes process overhead.

### Is Rinna suitable for large teams?
Yes. Rinna scales with your team while maintaining a consistent developer experience. The deliberate workflow model works for teams of all sizes.

### Can I extend Rinna with custom functionality?
Absolutely. Rinna follows Clean Architecture principles with well-defined interfaces for extending behavior without modifying core logic.

## Related Projects

- [Spring Boot](https://github.com/spring-projects/spring-boot) - For Java microservices
- [Cucumber JVM](https://github.com/cucumber/cucumber-jvm) - For BDD testing
- [JUnit 5](https://github.com/junit-team/junit5) - For Java testing
- [Picocli](https://github.com/remkop/picocli) - For Java command line interfaces

## Contact

For questions or feedback, please [open an issue](https://github.com/heymumford/Rinna/issues) on GitHub.

<div align="center">

*Loved using Rinna? Give it a star!* ⭐

[![Tweet this project](https://img.shields.io/twitter/url?style=social&url=https%3A%2F%2Fgithub.com%2Fheymumford%2FRinna)](https://twitter.com/intent/tweet?text=Check%20out%20Rinna:%20A%20developer-centric%20workflow%20management%20system%20by%20@heymumford&url=https://github.com/heymumford/Rinna)

</div>

```json
{
  "@context": "https://schema.org",
  "@type": "SoftwareSourceCode",
  "name": "Rinna",
  "alternateName": "Rinna Workflow Management",
  "description": "A developer-centric workflow management system designed for software engineers who want to focus on coding, not on managing tools.",
  "author": {
    "@type": "Person",
    "name": "Eric C. Mumford",
    "url": "https://github.com/heymumford"
  },
  "programmingLanguage": {
    "@type": "ComputerLanguage",
    "name": "Java",
    "version": "21"
  },
  "codeRepository": "https://github.com/heymumford/Rinna",
  "license": "https://opensource.org/licenses/MIT",
  "keywords": [
    "workflow management",
    "developer tools",
    "task tracking",
    "agile development",
    "software development",
    "CLI tool",
    "Java",
    "project management",
    "workflow automation"
  ],
  "applicationCategory": "DeveloperApplication",
  "operatingSystem": ["Linux", "Windows", "macOS"]
}
```
