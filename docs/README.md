# Rinna Documentation

Welcome to the Rinna documentation. Rinna is a developer-centric workflow management system designed to reduce process overhead while maximizing productivity.

```
+---------------+    +---------------+    +---------------+
|               |    |               |    |               |
| DEVELOPER     |    | PROJECT       |    | ADMIN         |
| DOCUMENTATION |<-->| DOCUMENTATION |<-->| DOCUMENTATION |
|               |    |               |    |               |
+-------+-------+    +-------+-------+    +-------+-------+
        |                    |                    |
        v                    v                    v
+-------+-------+    +-------+-------+    +-------+-------+
|               |    |               |    |               |
| WORKFLOW      |    | ARCHITECTURE  |    | INTEGRATION   |
| DOCUMENTATION |<-->| DOCUMENTATION |<-->| DOCUMENTATION |
|               |    |               |    |               |
+---------------+    +---------------+    +---------------+
```

> 💡 **Rinna Philosophy**: Rinna isn't trying to replace enterprise tools – it exists to make workflow management work _for_ developers rather than the other way around. It brings workflow management to where developers actually work: the command line.

## Documentation By User Persona

### For New Developers 🚀
You're new to Rinna and need to get started quickly:

- [Quick Start Guide](getting-started/README.md) - Install and set up your first project in minutes
- [CLI Basics](user-guide/rin-quick-reference.md) - Essential commands for daily use
- [Workflow Overview](workflow/README.md) - Understanding Rinna's streamlined workflow
- [First Work Item](getting-started/first-work-item.md) - Create and manage your first task

### For Experienced Developers 💻
You're familiar with development workflows and want to maximize efficiency:

- [CLI Reference](user-guide/rin-cli.md) - Complete command-line interface documentation
- [TUI Requirements](user-guide/tui-requirements.md) - Text User Interface specifications
- [Work Item Management](user-guide/work-item-relationships.md) - Managing dependencies and relationships
- [Test-Driven Development](development/README.md#test-driven-development-workflow) - Using Rinna for effective TDD
- [Development Guide](development/README.md) - Contributing to Rinna

### For Team Leads 👨‍💼👩‍💼
You're managing a team and need tools for coordination and oversight:

- [Release Management](user-guide/releases.md) - Planning and tracking releases
- [Lota Management](user-guide/lota.md) - Managing development cycles
- [Metrics and Reporting](user-guide/metrics/it-workflow-metrics.md) - Team productivity insights
- [Critical Path Analysis](workflow/README.md#critical-path-analysis) - Identifying bottlenecks
- [Migration Guide](user-guide/migration/README.md) - Transitioning from other tools

### For Administrators 🔧
You're responsible for setting up and maintaining Rinna:

- [Admin Guide](user-guide/admin-guide.md) - Complete administration guide
- [Server Configuration](user-guide/admin-server-setup.md) - Deployment and configuration
- [Admin CLI Quick Start](user-guide/admin-cli-quickstart.md) - Essential admin commands
- [Security Setup](development/configuration.md) - Securing your Rinna installation

### For System Integrators 🔄
You need to connect Rinna with other systems in your environment:

- [API Reference](user-guide/api/README.md) - RESTful API documentation
- [Enterprise Integration](integration/README.md) - Connecting with external systems
- [Migration Guide](user-guide/migration/README.md) - Data migration strategies
- [Webhook Configuration](user-guide/api/README.md#webhook-integration) - Event-based integration

### For Architects 🏗️
You're interested in Rinna's technical architecture and design decisions:

- [Architecture Overview](architecture/README.md) - Technical design and principles
- [Clean Architecture Implementation](architecture/diagrams/clean_architecture.ascii) - How Rinna implements Clean Architecture
- [Architecture Decisions](architecture/decisions/README.md) - Recorded architecture decisions
- [Engineering Specification](specifications/README.md) - Detailed system design

## Core Concepts

For a complete reference of all terms and concepts, see the [Glossary and Conceptual Index](glossary.md).

### Clean Architecture

Rinna follows the Clean Architecture approach, organizing the system into concentric layers with dependencies pointing inward:

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

[Learn more about Rinna's architecture](architecture/README.md)

### The Native Developer Workflow

Rinna uses a deliberate, opinionated workflow model that represents the smallest set of states needed for effective software development:

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

Can you customize the workflow? No, you can't. [That's the point](workflow/README.md).

### Ryorin-Do Philosophy

Rinna incorporates the Ryorin-Do workflow management philosophy:

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
         |                   +-----------------+      |
         +-------------------|   REFINEMENT    |<-----+
                             |    (Kairyo)     |
                             |                 |
                             +-----------------+
```

[Learn more about Ryorin-Do](ryorindo/RYORINDO.md)

### Work Item Types

- **Goal**: High-level objectives that orient development efforts
- **Feature**: Incremental functionality that delivers user value
- **Bug**: Software issues requiring correction
- **Chore**: Non-functional maintenance tasks

### Work Items and Dependencies

Understanding relationships between work items is crucial for effective project management:

- [Dependencies and Relationships](user-guide/work-item-relationships.md) - How work items relate to and affect each other

### API Architecture

Rinna provides a comprehensive REST API for integration:

```
                   +------------------+
                   |                  |
                   |   External       |
                   |   Systems        |
                   |                  |
                   +--------+---------+
                            |
                            | HTTPS / OAuth
                            v
+---------------------------------------------------+
|                                                   |
|  +-----------------+      +-------------------+   |
|  |                 |      |                   |   |
|  |  API Gateway    +----->+  Rate Limiter     |   |
|  |                 |      |                   |   |
|  +-----------------+      +-------------------+   |
|           |                                       |
|           | Routes requests                       |
|           v                                       |
|  +-----------------+      +-------------------+   |
|  |                 |      |                   |   |
|  |  Authentication +----->+  Authorization    |   |
|  |  Middleware     |      |  (RBAC)           |   |
|  |                 |      |                   |   |
|  +-----------------+      +-------------------+   |
|           |                                       |
|           | Authenticated request                 |
|           v                                       |
|  +--------------------------------------------+   |
|  |                                            |   |
|  |  API Handlers                              |   |
|  |                                            |   |
|  |  +---------------+  +-------------------+  |   |
|  |  | Work Items    |  | Projects          |  |   |
|  |  +---------------+  +-------------------+  |   |
|  |                                            |   |
|  |  +---------------+  +-------------------+  |   |
|  |  | Workflows     |  | Users             |  |   |
|  |  +---------------+  +-------------------+  |   |
|  |                                            |   |
|  |  +---------------+  +-------------------+  |   |
|  |  | Releases      |  | Health            |  |   |
|  |  +---------------+  +-------------------+  |   |
|  |                                            |   |
|  +--------------------------------------------+   |
|                                                   |
+---------------------------------------------------+
```

[Learn more about Rinna's API](user-guide/api/README.md)

## Why Rinna?

Traditional project management tools are designed primarily for reporting to management. Rinna takes the opposite approach:

- **Lives Where Developers Work**: Terminal-first interface in your coding environment
- **Zero-Friction Philosophy**: Never adds more process than absolutely necessary
- **Developer-Owned**: Complete workflow control by the people doing the work
- **Clear Visibility**: Simple, unambiguous work item tracking without the noise

## Getting Started

Ready to dive in? Here are the quickest paths to get started with Rinna:

1. [Installation Guide](getting-started/README.md) - Set up Rinna in your environment
2. [CLI Quick Reference](user-guide/rin-quick-reference.md) - Essential commands to get started
3. [First Project Setup](user-guide/admin-cli-quickstart.md) - Create your first Rinna project
4. [Creating Work Items](user-guide/rin-cli.md#standard-work-item-commands) - Start tracking your work

## Need Help?

- Check the [CLI Commands Reference](CLAUDE.md) for a complete list of available commands
- Join our community forum at [community.rinna.org](https://community.rinna.org)
- Submit issues or feature requests on [GitHub](https://github.com/heymumford/rinna/issues)
- Contact support at support@rinna.org