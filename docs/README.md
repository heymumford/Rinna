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

## Documentation Structure

This documentation repository is organized into the following sections:

- [Architecture](architecture/README.md) - System design documents and technical architecture
- [Contributing](contributing/README.md) - Guidelines for contributors
- [Guides](guides/README.md) - User and developer guides
  - [Getting Started](guides/getting-started/README.md) - Installation and quick start
  - [Developer](guides/developer/README.md) - Documentation for developers
  - [User](guides/user/README.md) - End-user documentation
- [Implementation](implementation/README.md) - Implementation details
  - [API](implementation/api/README.md) - API documentation
  - [CLI](implementation/cli/README.md) - Command-line interface documentation
  - [Integration](implementation/integration/README.md) - Integration guides
- [Project](project/README.md) - Project management documentation
  - [Standards](project/standards/README.md) - Project standards and guidelines
  - [Status](project/status/README.md) - Project status reports
  - [Backlog](project/backlog/README.md) - Feature backlog
- [Reference](reference/README.md) - Reference materials
  - [Diagrams](reference/diagrams/README.md) - Technical diagrams
  - [Specifications](reference/specifications/README.md) - Technical specifications
  - [Glossary](reference/glossary.md) - Terminology definitions
- [Security](security/README.md) - Security documentation
- [Testing](testing/README.md) - Testing documentation
- [Archive](archive/README.md) - Archived documentation

## Documentation By User Persona

### For New Developers 🚀
You're new to Rinna and need to get started quickly:

- [Quick Start Guide](guides/getting-started/README.md) - Install and set up your first project in minutes
- [CLI Basics](guides/user/rin-quick-reference.md) - Essential commands for daily use
- [Workflow Overview](implementation/README.md) - Understanding Rinna's streamlined workflow
- [First Work Item](guides/getting-started/first-work-item.md) - Create and manage your first task

### For Experienced Developers 💻
You're familiar with development workflows and want to maximize efficiency:

- [CLI Reference](guides/user/cli-reference.md) - Complete command-line interface documentation
- [TUI Requirements](guides/user/tui-requirements.md) - Text User Interface specifications
- [Work Item Management](guides/user/work-item-relationships.md) - Managing dependencies and relationships
- [Test-Driven Development](guides/developer/README.md#test-driven-development-workflow) - Using Rinna for effective TDD
- [Development Guide](guides/developer/README.md) - Contributing to Rinna

### For Team Leads 👨‍💼👩‍💼
You're managing a team and need tools for coordination and oversight:

- [Release Management](guides/user/releases.md) - Planning and tracking releases
- [Lota Management](guides/user/lota.md) - Managing development cycles
- [Metrics and Reporting](guides/user/metrics/it-workflow-metrics.md) - Team productivity insights
- [Critical Path Analysis](implementation/README.md#critical-path-analysis) - Identifying bottlenecks
- [Migration Guide](guides/user/migration/README.md) - Transitioning from other tools

### For Administrators 🔧
You're responsible for setting up and maintaining Rinna:

- [Admin Guide](guides/user/admin-guide.md) - Complete administration guide
- [Server Configuration](guides/user/admin-server-setup.md) - Deployment and configuration
- [Admin CLI Quick Start](guides/user/admin-cli-quickstart.md) - Essential admin commands
- [Security Setup](security/README.md) - Securing your Rinna installation

### For System Integrators 🔄
You need to connect Rinna with other systems in your environment:

- [API Reference](implementation/api/README.md) - RESTful API documentation
- [Enterprise Integration](implementation/integration/README.md) - Connecting with external systems
- [Migration Guide](guides/user/migration/README.md) - Data migration strategies
- [Webhook Configuration](implementation/api/README.md#webhook-integration) - Event-based integration

### For Architects 🏗️
You're interested in Rinna's technical architecture and design decisions:

- [Architecture Overview](architecture/README.md) - Technical design and principles
- [Clean Architecture Implementation](reference/diagrams/clean_architecture.ascii) - How Rinna implements Clean Architecture
- [Architecture Decisions](architecture/decisions/README.md) - Recorded architecture decisions
- [Engineering Specification](reference/specifications/README.md) - Detailed system design

### For Individual Users 👤
You want to manage both your work and personal life in one unified system:

- [Personal Task Management](guides/user/personal/tasks.md) - Managing personal to-dos and projects
- [Habit Building](guides/user/personal/habits.md) - Creating and tracking personal habits
- [Life Coordination](guides/user/personal/coordination.md) - Family and household management
- [Unified Life Dashboard](guides/user/personal/dashboard.md) - Bringing it all together

## Core Concepts

For a complete reference of all terms and concepts, see the [Glossary](reference/glossary.md).

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

### Ryorin-Do Philosophy and Canonical Modeling

Rinna implements the Ryorin-Do workflow management philosophy through a canonical modeling approach that forms the core of its design:

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

The canonical modeling approach is central to Ryorin-Do implementation. This means:

1. **Universal Underlying Model**: Rinna maintains a consistent internal model based on workflow fundamentals
2. **Bidirectional Mapping**: All external workflow representations map to and from this canonical model
3. **Semantic Preservation**: Workflow meaning is preserved across different methodologies
4. **Unified Metrics**: Consistent measurement across projects using different methodologies
5. **Methodology Flexibility**: Freedom to use any workflow methodology without losing interoperability

Through this canonical approach, Rinna can provide powerful workflow capabilities while adapting to your preferred methodology instead of forcing you to adapt to it.

[Learn more about Ryorin-Do and canonical modeling](project/standards/RYORINDO.md)

### Flexible Workflow Mapping

Rinna provides a universal workflow management model based on the Ryorin-Do methodology, which is compatible with all Software Development Life Cycles (SDLCs):

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

While Rinna has this internal workflow model, it provides extensive mapping capabilities to adapt to any workflow you prefer. Rinna comes with out-of-the-box mappings for:

- **Kanban**: Direct mapping to Kanban columns and WIP limits
- **Scrum**: Sprint and backlog management with story points
- **Waterfall**: Phase-based progression with approvals
- **Jira**: Compatible with Jira's workflow states and transitions
- **Custom**: Define your own workflow with custom states and transitions

This flexibility allows you to maintain your existing processes while gaining Rinna's efficiency advantages. [Learn more about workflow mapping](implementation/README.md)

### Quality Gates and Validation

Rinna provides configurable quality gates for workflow states and actions:

```
                  ┌─────────────────┐
                  │                 │
                  │ Quality Gate    │
                  │                 │
┌─────────────┐   │ ┌─────────────┐ │   ┌─────────────┐
│             │   │ │             │ │   │             │
│  Previous   │   │ │ Validation  │ │   │   Next      │
│   State     │──►│ │  Logic      │ │──►│   State     │
│             │   │ │             │ │   │             │
└─────────────┘   │ └─────────────┘ │   └─────────────┘
                  │                 │
                  │  • Context      │
                  │  • Measures     │
                  │  • Permissions  │
                  │  • Rules        │
                  │                 │
                  └─────────────────┘
```

Quality gates allow you to:
- Define context-specific validation requirements
- Establish measurement criteria for progression
- Set role-based permissions for state transitions
- Create custom rules for quality enforcement
- Connect external tools via webhooks for analysis
- Generate reports to monitor quality metrics

This ensures that work items meet your organization's quality standards before progressing through the workflow.

[Learn more about quality gates](implementation/README.md#quality-gates-system)

### Work Item Types

- **Goal**: High-level objectives that orient development efforts
- **Feature**: Incremental functionality that delivers user value
- **Bug**: Software issues requiring correction
- **Chore**: Non-functional maintenance tasks

### Work Items and Dependencies

Understanding relationships between work items is crucial for effective project management:

- [Dependencies and Relationships](guides/user/work-item-relationships.md) - How work items relate to and affect each other

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

[Learn more about Rinna's API](implementation/api/README.md)

## Comprehensive Management Practice Templates

Rinna provides templates across the full spectrum of management practices, all built on Ryorin-do principles while adapting to your preferred management or organizational paradigm:

```
┌───────────────────────────────────────────────────────────────┐
│                                                               │
│                     MANAGEMENT PRACTICES                      │
│                                                               │
├───────────────┬───────────────┬───────────────┬───────────────┤
│  Portfolio    │   Product     │   Project     │  Engineering  │
│  Management   │   Management  │   Management  │  Development  │
├───────────────┼───────────────┼───────────────┼───────────────┤
│• Investment   │• Roadmapping  │• Traditional  │• Agile Dev    │
│  Tracking     │• Backlog Mgmt │  Waterfall    │• DevOps       │
│• Resource     │• Market Fit   │• Critical     │• CI/CD        │
│  Allocation   │• User Stories │  Path         │• TDD/BDD      │
│• Strategic    │• Competitive  │• Resource     │• Code Review  │
│  Alignment    │  Analysis     │  Management   │• Sprint Mgmt  │
└───────────────┴───────────────┴───────────────┴───────────────┘

┌───────────────────────────────────────────────────────────────┐
│                                                               │
│                    MEASUREMENT & REPORTING                    │
│                                                               │
├───────────────┬───────────────┬───────────────┬───────────────┤
│  Quality      │   Unified     │   KPI         │  Data         │
│  Management   │   Reporting   │   Webhooks    │  Consolidation│
├───────────────┼───────────────┼───────────────┼───────────────┤
│• Test Planning│• Cross-team   │• Real-time    │• Statistics   │
│• Test         │  Dashboards   │  Metrics      │  Generation   │
│  Execution    │• Executive    │• Integration  │• Batch        │
│• Results      │  Summaries    │  with BI      │  Computing    │
│  Tracking     │• Compliance   │  Tools        │• Custom KPI   │
│• Coverage     │  Reports      │• Alerts and   │  Reports      │
│  Analysis     │• Trend        │  Notifications│• Historical   │
│               │  Visualization│               │  Analysis     │
└───────────────┴───────────────┴───────────────┴───────────────┘
```

All templates leverage the canonical modeling approach of Ryorin-do, allowing them to:
- Adapt to any organizational structure or methodology
- Integrate with existing tools and processes
- Provide consistent metrics across different practices
- Support seamless transitions between management approaches
- Enable unified reporting across diverse teams and projects

[Learn more about management practice templates](implementation/README.md#management-practice-templates)

## Industry-Specific Templates

Rinna includes pre-configured templates for industry-specific workflows:

### Scaled Agile Framework (SAFe)
```
                    ┌───────────────┐
                    │   Portfolio   │
                    │    Level      │
                    └───────┬───────┘
                            │
                            ▼
┌───────────────┬───────────────────┬───────────────┐
│   Program     │      Value        │    Solution   │
│   Level       │      Stream       │    Level      │
└───────┬───────┴────────┬──────────┴───────┬───────┘
        │                │                  │
        ▼                ▼                  ▼
┌───────────────┬───────────────────┬───────────────┐
│   Team        │      Team         │    Team       │
│   Level       │      Level        │    Level      │
└───────────────┴───────────────────┴───────────────┘
```

### Good Practice (GxP) Compliance
```
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ Requirements  │  │  Risk         │  │ Design        │
│ Specification │─►│  Assessment   │─►│ Specification │
└───────┬───────┘  └───────────────┘  └───────┬───────┘
        │                                     │
        ▼                                     ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ Traceability  │  │ Validation    │  │ Implementation│
│ Matrix        │◄─┤ Protocol      │◄─┤ Phase         │
└───────┬───────┘  └───────────────┘  └───────┬───────┘
        │                                     │
        ▼                                     ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ Summary       │  │ Change        │  │ Testing       │
│ Report        │◄─┤ Control       │◄─┤ Phase         │
└───────────────┘  └───────────────┘  └───────────────┘
```

[Learn more about industry templates](implementation/README.md#industry-specific-templates)

## Personal Life Management

Rinna extends beyond professional workflow management to help users manage their personal lives using the same powerful approach:

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                PERSONAL LIFE MANAGEMENT                 │
│                                                         │
├─────────────────┬─────────────────┬─────────────────────┤
│ Personal Tasks  │ Habit Building  │ Life Coordination   │
├─────────────────┼─────────────────┼─────────────────────┤
│• Honey-do lists │• Daily routines │• Calendar reminders │
│• Shopping lists │• Streak tracking│• Event planning     │
│• Home projects  │• Goal systems   │• Family coordination│
│• Errands        │• Progress charts│• Shared tasks       │
│• Personal goals │• Accountability │• Travel planning    │
└─────────────────┴─────────────────┴─────────────────────┘
```

Rinna provides a unified repository that consolidates items typically scattered across:
- Calendar apps
- Email reminders
- Note-taking applications
- To-do list apps
- Habit trackers
- Family coordination tools

By bringing your personal and professional workflow management into one system, Rinna creates a seamless experience that helps you maintain clarity and progress across all aspects of life. The same flexible workflow mapping, quality gates, and reporting capabilities that power your work management can be applied to personal goals and habits.

For teams and families, Rinna enables shared coordination with appropriate privacy controls, allowing collaborative management while maintaining separation between personal and professional domains when desired.

## Why Rinna?

Traditional project management tools are designed primarily for reporting to management. Rinna takes the opposite approach:

- **Lives Where Developers Work**: Terminal-first interface in your coding environment
- **Zero-Friction Philosophy**: Never adds more process than absolutely necessary
- **Developer-Owned**: Complete workflow control by the people doing the work
- **Clear Visibility**: Simple, unambiguous work item tracking without the noise
- **Flexible Quality Gates**: Configurable checkpoints with context-aware validation
- **Tool Integration**: Connect with your existing toolchain through webhooks and APIs

## Getting Started

Ready to dive in? Here are the quickest paths to get started with Rinna:

1. [Installation Guide](guides/getting-started/README.md) - Set up Rinna in your environment
2. [CLI Quick Reference](guides/user/rin-quick-reference.md) - Essential commands to get started
3. [First Project Setup](guides/user/admin-cli-quickstart.md) - Create your first Rinna project
4. [Creating Work Items](implementation/cli/rin-cli.md#standard-work-item-commands) - Start tracking your work

## Need Help?

- Join our community forum at [rinnacloud.com](https://rinnacloud.com)
- Submit issues or feature requests on [GitHub](https://github.com/heymumford/Rinna/issues)
- Contact support at support@rinnacloud.com
