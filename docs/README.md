<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Rinna Documentation

Welcome to the Rinna documentation. Rinna is a developer-centric workflow management system designed to reduce process overhead while maximizing productivity.

> Rinna isn't trying to replace enterprise tools – it exists to make workflow management work _for_ developers rather than the other way around.

## Documentation By Role

### For New Users
- 🚀 [Getting Started](getting-started/README.md) - Quick installation and first steps
- 📚 [User Guide](user-guide/README.md) - Complete guide to using Rinna
- 🧭 [Workflow Philosophy](user-guide/workflow-philosophy.md) - Why Rinna works the way it does

### For Administrators
- 🔑 [Admin Guide](user-guide/admin-guide.md) - Complete administration guide
- ⚡ [Admin CLI Quick Start](user-guide/admin-cli-quickstart.md) - Essential admin commands
- 🖥️ [Server Configuration](user-guide/admin-server-setup.md) - Detailed server setup
- 📄 [Maven POM Sample](user-guide/admin-pom-sample.xml) - Ready-to-use Maven configuration

### For Developers
- 🧩 [CLI Reference](user-guide/rin-cli.md) - Command-Line Interface tool reference
- 🔧 [Development Guide](development/README.md) - Contributing to Rinna
- 🏗️ [Architecture](development/architecture.md) - Technical design and principles

### For System Integrators
- 📋 [Technical Specification](technical-specification.md) - System requirements
- 🔄 [API Integration](user-guide/api-integration.md) - Connecting with other systems
- 📐 [Engineering Specification](specifications/engineering_spec.md) - Detailed system design

## Core Concepts

### The Native Developer Workflow

Rinna uses a deliberate, opinionated workflow model that represents the smallest set of states needed for effective software development:

```
Found → Triaged → To Do → In Progress → In Test → Done
```

Can you customize the workflow? No, you can't. [That's the point](user-guide/workflow-philosophy.md).

### Work Item Types

- **Goal**: High-level objectives that orient development efforts
- **Feature**: Incremental functionality that delivers user value
- **Bug**: Software issues requiring correction
- **Chore**: Non-functional maintenance tasks

### Developer-Focused, Enterprise-Compatible

Rinna is designed primarily for developers, but it can integrate with enterprise requirements:

- Work locally in Rinna's clean, efficient workflow
- Map to enterprise tools when needed for reporting
- Satisfy management requirements without complicating development

## Why Rinna?

Traditional project management tools are designed primarily for reporting to management. Rinna takes the opposite approach:

- **Lives Where Developers Work**: Terminal-first interface in your coding environment
- **Zero-Friction Philosophy**: Never adds more process than absolutely necessary
- **Developer-Owned**: Complete workflow control by the people doing the work
- **Clear Visibility**: Simple, unambiguous work item tracking without the noise

## Documentation Map

- [Getting Started](getting-started/README.md)
  - Installation
  - Quick start guide
  - Java 21 features

- [User Guide](user-guide/README.md)
  - [CLI Tool](user-guide/rin-cli.md)
  - [Service Management](user-guide/service-management.md)
  - [Configuration](user-guide/configuration-reference.md)
  - [Workflow](user-guide/workflow.md)
  - [Workflow Philosophy](user-guide/workflow-philosophy.md)
  - [Lota (Development Cycle)](user-guide/lota.md)
  - [Release Management](user-guide/releases.md)
  
- [Administrator Guide](user-guide/admin-guide.md)
  - [Admin CLI Quick Start](user-guide/admin-cli-quickstart.md)
  - [Server Configuration](user-guide/admin-server-setup.md)
  - [Maven POM Sample](user-guide/admin-pom-sample.xml)

- [Development](development/README.md)
  - [Architecture](development/architecture.md)
  - [Design Approach](development/design-approach.md)
  - [Version Management](development/version-management.md)
  - [Java 21 Features](development/java21-features.md)
  - [Testing Strategy](development/testing.md)
  - [Contributing](development/contribution.md)

- [Specifications](specifications/engineering_spec.md)
  - Technical requirements
  - System design
  - Integration points
