# Rinna: Developer-Centric Workflow Management

Rinna liberates engineers from the tyranny of bloated workflow tools by bringing task management directly into the terminal where code flows. Born from the frustration of context-switching and interrupted flow states, this elegant system speaks the language of developers—terminal commands, git workflows, and clean architecture.

<div align="center">

*A clean, compact solution for product, project, development, and quality management!*

[![Rinna CI](https://github.com/heymumford/Rinna/actions/workflows/rin-ci.yml/badge.svg)](https://github.com/heymumford/Rinna/actions/workflows/rin-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Go Version](https://img.shields.io/badge/go-1.21-blue.svg)](https://golang.org/doc/go1.21)
[![Version](https://img.shields.io/badge/version-1.6.6-blue.svg)](https://github.com/heymumford/Rinna/releases)
[![Build](https://img.shields.io/badge/build-502-green.svg)](https://github.com/heymumford/Rinna/actions)
[![GitHub Stars](https://img.shields.io/github/stars/heymumford/Rinna?style=social)](https://github.com/heymumford/Rinna/stargazers)

</div>

## Beta Release Progress

<div align="center">

```
[██████████████████████████████▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒] 63.4% Complete
```

*Actively developed and on track for beta release. Join us!*

[📥 Download](https://github.com/heymumford/Rinna/releases) • 
[📚 User Guide](docs/user-guide/README.md) • 
[👩‍💻 Developer Guide](docs/development/DEVELOPER.md) • 
[🚀 Getting Started](docs/getting-started/README.md) • 
[🧪 TDD Guide](rinna-cli/TDD_GUIDE.md) • 
[🤝 Contribute](docs/CONTRIBUTING.md) • 
[📋 Changelog](docs/project-docs/CHANGELOG.md)

</div>

> **⚠️ DEVELOPMENT STANDARD**: For all XML manipulation (especially POM files), ALWAYS use the XMLStarlet-based tools in `bin/xml-tools.sh`. NEVER use grep, sed, or other text-based tools for XML files.

## What Is Rinna?

Rinna is a unified workflow management system built for software engineers that treats all types of work—business, product, engineering, and test—as part of a single coherent system. It minimizes process overhead and integrates directly into your development environment, providing clear visibility without excessive ceremony.

**Rinna doesn't replace enterprise tools – it makes workflow management work _for_ developers, not against them.**

### The Problem

Traditional workflow tools:
- Force context-switching away from coding
- Interrupt [flow state](docs/technical-specification.md#core-philosophy)
- Prioritize reporting over productivity
- Add unnecessary complexity
- Separate different types of work into silos
- Require complex setup and platform-specific installation

### The Solution

- **Terminal-first interface** integrates with git workflows and IDEs
- **Zero-friction workflow** adds only what's necessary
- **Developer-owned process** puts control in the right hands
- **Clean architecture** with Go API and Java core
- **Unified work management** treats all work types consistently
- **Cross-platform containers** for simple deployment on Windows, WSL, and Linux
- **Standardized logging** with SLF4J and clearly defined log levels
- **OAuth integration** for secure third-party API access
- **API documentation** with OpenAPI/Swagger for easy integration

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

## Quick Installation

```bash
# Clone and build
git clone https://github.com/heymumford/Rinna.git
cd Rinna
chmod +x bin/rin bin/rin-version bin/rin-build bin/run-tests.sh
bin/rin build

# OR use containers (no build required)
docker-compose up -d
docker exec -it rinna-cli-1 /bin/bash
rin list
```

See [Cross-Platform Container Setup](docs/user-guide/cross-platform-container-setup.md) for detailed instructions.

## Core Features

### Unified Work Model

- **Work Items**: Business → Product → Engineering → Test
- **Work Types**: Goals → Epics → Features → User Stories → Tasks → Bugs → Tests
- **Workflow**: Found → Triaged → To Do → In Progress → In Test → Done → Released
- **No customization needed**: We've built what works based on experience

### Testing as a First-Class Citizen

```
        ▲ Fewer
        │
        │    ┌───────────────┐
        │    │  Performance  │ Slowest, most complex
        │    └───────────────┘
        │    ┌───────────────┐
        │    │  Acceptance   │ End-to-end workflows
        │    └───────────────┘
        │    ┌───────────────┐
        │    │  Integration  │ Tests between modules
        │    └───────────────┘
        │    ┌───────────────┐
        │    │   Component   │ Tests within modules
        │    └───────────────┘
        │    ┌───────────────┐
        │    │     Unit      │ Fastest, most granular
        │    └───────────────┘
        │
        ▼ More
```

Our testing strategy spans multiple languages (Java, Go, Python, and Bash) with:

- **Base Test Classes** - Standardized parent classes for all test types
- **Layered Discovery** - Intelligent categorization of tests by purpose
- **Smart Test Runner** - Optimized execution based on the testing pyramid
- **Test Pyramid Analysis** - Automated monitoring of test pyramid balance

### Version Management

The project uses a centralized version management approach with `version.properties` as the single source of truth:

```bash
bin/rin version current   # View version information
bin/rin version patch     # Bump patch version
bin/rin version verify    # Check consistency
bin/rin version update    # Sync all files
```

## Why Developers Choose Rinna

| Feature | Rinna | Jira | GitHub Issues | Linear |
|---------|-------|------|--------------|--------|
| **Focus** | Developer experience | Management reporting | Issue tracking | Project management |
| **Workflow** | Fixed, streamlined | Highly customizable | Basic | Customizable |
| **Git integration** | Native | Plugin | Native | Plugin |
| **Terminal-based** | Yes | No | No | No |
| **CLI** | Full-featured | No | Limited | No |
| **Cross-language testing** | Comprehensive | No | No | No |
| **Test automation** | Advanced | Plugin-dependent | Basic | Limited |
| **Learning curve** | Low | High | Medium | Medium |

## Requirements

- Java 21+
- Go 1.21+ (for API server)
- Maven 3.8+
- `jq` for CLI client

## Documentation

### For Users
- [📚 User Guide](docs/user-guide/README.md) - Complete guide for using Rinna
- [🚀 Getting Started](docs/getting-started/README.md) - Quick start guide
- [📄 Documentation Generation](docs/user-guide/documents.md) - Generate documentation

### For Developers
- [👩‍💻 Developer Guide](docs/development/DEVELOPER.md) - Complete guide for developing Rinna
- [🤝 Contribution Guidelines](docs/CONTRIBUTING.md) - How to contribute
- [🧪 Testing Strategy](docs/testing/TESTING_STRATEGY.md) - Comprehensive testing approach
- [🏗️ Architecture](docs/development/architecture.md) - System architecture
- [🔧 Build System](docs/development/build-system.md) - Build and development workflow
- [📘 API Reference](api/docs/swagger.json) - OpenAPI/Swagger documentation

### API Documentation
The API is documented using OpenAPI/Swagger. You can:
- View the [API Reference](api/docs/swagger.json) in any Swagger UI compatible viewer
- Run `python3 api/bin/sync-swagger.py` to synchronize YAML and JSON formats
- Access the documentation at `http://localhost:8080/api/docs` when the server is running

#### API Documentation Server
For exploring the API documentation without running the full API server:

```bash
# Start the documentation server
cd api && ./bin/start-docs-server.sh
```

This starts a lightweight server that serves a comprehensive API documentation portal, including:

- **Interactive API Explorer**: Browse and test API endpoints with the Swagger UI
- **Code Examples**: Implementation examples in JavaScript, Python, Go, and curl
- **Security Guide**: Best practices for secure API integration 
- **API Specifications**: Download OpenAPI specifications in YAML or JSON format
- **Themed Interface**: Custom-designed documentation portal for better readability

Access the documentation at `http://localhost:8080/api/docs` to explore all the available resources.

## License and Acknowledgments

This project is licensed under the [MIT License](LICENSE).

### Development Tools

This project was developed with analytical assistance from:
- Claude 3.7 Sonnet LLM by Anthropic
- Claude Code executable
- Google Gemini Deep Research LLM

<div align="center">

*A clean, compact solution for product, project, development, and quality management!*

</div>