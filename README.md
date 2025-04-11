# Rinna: Developer-Centric Workflow Management

Rinna liberates engineers from the tyranny of bloated workflow tools by bringing task management directly into the terminal where code flows. Born from the frustration of context-switching and interrupted flow states, this elegant system speaks the language of developers—terminal commands, git workflows, and clean architecture.

<div align="center">

*A clean, compact solution for product, project, development, and quality management!*

[![Rinna CI](https://github.com/heymumford/Rinna/actions/workflows/rin-ci.yml/badge.svg)](https://github.com/heymumford/Rinna/actions/workflows/rin-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Go Version](https://img.shields.io/badge/go-1.21-blue.svg)](https://golang.org/doc/go1.21)
[![Version](https://img.shields.io/badge/version-1.11.0-blue.svg)](https://github.com/heymumford/Rinna/releases)
[![Build](https://img.shields.io/badge/build-573-green.svg)](https://github.com/heymumford/Rinna/actions)
[![GitHub Stars](https://img.shields.io/github/stars/heymumford/Rinna?style=social)](https://github.com/heymumford/Rinna/stargazers)

</div>

## Beta Release Progress

<div align="center">

```
[█████████████████████████████████▒▒▒▒▒▒▒▒▒▒] 76.3% Complete
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

Rinna is a unified workflow management system built on the philosophy of Ryorin-Do (The Way of Universal Work Management). It treats all types of work—business, product, engineering, and test—as part of a single coherent system, transcending artificial boundaries while respecting each domain's unique characteristics. It minimizes process overhead and integrates directly into your development environment, providing clear visibility without excessive ceremony.

**Rinna doesn't replace enterprise tools – it makes workflow management work _for_ developers, not against them, embodying the Ryorin-Do principles of Flow Optimization and Mindful Simplicity.**

```
+-----------------+   +-----------------+   +-----------------+
|                 |   |                 |   |                 |
|    INTENTION    |-->|    EXECUTION    |-->|  VERIFICATION   |
|     (Ishi)      |   |     (Jikko)     |   |    (Kakunin)    |
|                 |   |                 |   |                 |
+-----------------+   +-----------------+   +-----------------+
         ^                                            |
         |                                            |
         |                                            v
         |                +-----------------+         |
         +----------------|   REFINEMENT    |<--------+
                          |    (Kairyo)     |
                          |                 |
                          +-----------------+
```

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
curl -X POST "https://api.example.com/api/v1/workitems" \
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
```

## Key Features

- **Unified Work Management**: Comprehensive system for managing work items, bugs, tasks, and more
- **Terminal-First Design**: CLI-centric workflow designed for where developers already work
- **Clean Architecture**: Domain-driven design with clear separation of concerns
- **Extension Architecture**: Plugin system for extending core functionality with commercial features
- **Cross-Platform Containers**: Run the entire system in Docker, even on Windows
- **Open API Interface Platform**: Seamlessly integrate with external work management systems
- **Feature Flagging System**: Granular control over feature availability with admin-level management
- **Multi-Channel Notifications**: Configurable notifications via in-app, CLI, email, Slack, webhooks
- **Security-First Approach**: RBAC, audit logging, and comprehensive security controls
- **Expertise Rating System**: "Baseball card" approach to skill certification with blockchain verification
- **Multi-Language Support**: Equal focus on English, Spanish, French, Ukrainian, Hindi, Swedish, Norwegian, German, Portuguese, and Classical Latin
- **Universal Test Automation Integration**: Standardized API for test frameworks with special Karate integration
- **Digital Transformation Templates**: Executive-friendly templates for modernizing legacy systems with DevOps best practices
- **Ryorin-Do Philosophy**: The Way of Universal Work Management, integrating all work types across domains
- **AI-Enhanced Smart Fields**: Intelligent prediction of field values based on patterns and user preferences
- **Git-Based History**: Encrypted, Git-powered work item history with fine-grained version control
- **Personal AI Assistant**: Secure integration with your preferred AI services (Claude, ChatGPT, Gemini)
- **Open Source Core**: MIT-licensed foundation with extension points for commercial features

```bash
rin list
```

See [Cross-Platform Container Setup](docs/user-guide/cross-platform-container-setup.md) for detailed instructions.

## Core Features

### Unified Work Model (Ryorin-Do)

```
                             +-----------+
                             |           |
                   +-------->| BACKLOG   +-----+
                   |         |           |     |
                   |         +-----------+     |
                   |                           | Prioritized
                   |                           v
                   |         +-----------+     |
                   |         |           |     |
                   +-------->|  TRIAGE   +-----+
                   |         |           |     |
                   |         +-----------+     |
                   |                           | Accepted
                   |                           v
                   |         +-----------+     |
                   |         |           |     |
                   +-------->|  TO_DO    +-----+
                   |         |           |     |
                   |         +-----------+     |
                   |                           | Started
                   |                           v
  Blocked          |         +-----------+     |
  +----------------+         |           |     |
  |                +-------->| IN_PROGRESS +---+
  |                |         |           |     |
  |                |         +-----------+     |
  |                |                           | Completed
  |                |                           v
  |  Blocked       |         +-----------+     |
  +----------------+         |           |     |
  |                +-------->|  IN_TEST  +-----+
  |                |         |           |     |
  |                |         +-----------+     |
  |                |                           | Verified
  |                |                           v
  v                |         +-----------+     |
+-----------+      |         |           |     |
|           |      +-------->|  DONE     +-----+
| BLOCKED   |                |           |     |
|           |                +-----------+     |
+-----------+                                  | Deployed
     ^                                         v
     |                       +-----------+     |
     |                       |           |     |
     +-----------------------+ RELEASED  +<----+
                             |           |
                             +-----------+
```

- **Work Items**: Business → Product → Engineering → Test, following Unity of Work (Ichi-no-Rodo)
- **Origin Categories**: Product (PROD), Architecture (ARCH), Development (DEV), Testing (TEST), Operations (OPS), Documentation (DOC), Cross-Cutting (CROSS)
- **Work Complexity Domains**: Simple (Tanjun), Complicated (Fukuzatsu), Complex (Fukuheisa), Chaotic (Konton), Disordered (Midare)
- **Work Paradigms**: Project (Purojekuto), Operational (Unten), Exploratory (Tanken), Governance (Tōchi)
- **Workflow**: Found → Triaged → To Do → In Progress → In Test → Done → Released
- **Four Aspects of Work**: Intention (Ishi), Execution (Jikkō), Verification (Kakunin), Refinement (Kairyō)
- **Mindful Simplicity**: Fixed workflow states that capture the essence without needless customization

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

### System Architecture

```
                          +---------------------------------------------+
                          |                                             |
                          |  +-----------------------------------+      |
                          |  |                                   |      |
                          |  |  +---------------------------+    |      |
                          |  |  |                           |    |      |
                          |  |  |  +-------------------+    |    |      |
                          |  |  |  |                   |    |    |      |
                          |  |  |  |    ENTITIES       |    |    |      |
                          |  |  |  |    (Domain)       |    |    |      |
                          |  |  |  |                   |    |    |      |
                          |  |  |  +-------------------+    |    |      |
                          |  |  |                           |    |      |
                          |  |  |      USE CASES            |    |      |
                          |  |  |      (Application)        |    |      |
                          |  |  |                           |    |      |
                          |  |  +---------------------------+    |      |
                          |  |                                   |      |
                          |  |        INTERFACE ADAPTERS         |      |
                          |  |        (Infrastructure)           |      |
                          |  |                                   |      |
                          |  +-----------------------------------+      |
                          |                                             |
                          |           FRAMEWORKS & DRIVERS              |
                          |           (External Interfaces)             |
                          |                                             |
                          +---------------------------------------------+

                              DEPENDENCY RULE: Dependencies point inward
```

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
| **External system integration** | Native | Limited | Limited | Via API |
| **Learning curve** | Low | High | Medium | Medium |
| **Multi-language support** | 10+ languages | Limited | Limited | Limited |
| **Test automation integration** | Universal API | Limited | Via Actions | Limited |
| **Expertise rating system** | Built-in | No | No | No |

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
- [🏗️ Architecture](docs/architecture/README.md) - System architecture
- [🔧 Build System](docs/development/build-system.md) - Build and development workflow
- [📘 API Reference](docs/user-guide/api/README.md) - API documentation

### API Documentation
The API is documented using OpenAPI/Swagger. You can:
- View the [API Reference](docs/user-guide/api/README.md) in any Swagger UI compatible viewer
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