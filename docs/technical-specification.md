# Rinna Technical Specification

## Vision

Rinna is a streamlined, modular, open-source software system explicitly designed to facilitate clear, usable, and maintainable workflow and release management for software engineering teams. Inspired by the philosophy of Samstraumr, Rinna provides intentionally selected features that transparently manage software development work, with careful consideration toward refined integration and intuitive user experience.

## Core Philosophy

- **Developer-Centric:** Optimize every aspect of the system from the developer's perspective.
- **Clarity:** Ensure transparent processes, explicit definitions, and intuitive interactions within the software.
- **Usability:** Prioritize thoughtful, user-centered design to enable seamless operation.
- **Immutability:** Maintain unique, immutable identifiers for managed work items.
- **Explicit Flow:** Clearly define and enforce workflow transitions to ensure predictable and controlled outcomes.

## Supported Work Item Types

Software teams using Rinna manage the following clearly defined item types:

- **Goal**: High-level software development objectives.
- **Feature**: Incremental, deliverable functionality clearly described and actionable.
- **Bug**: Clearly identified unexpected software issues requiring correction.
- **Chore**: Necessary non-functional tasks ensuring system health and stability.

## Workflow Stages

The software items managed by Rinna flow through the following explicitly defined stages:
```
Found → Triaged → To Do → In Progress → In Test → Done
```

Can you customize the workflow? No, you can't. [That's the point](user-guide/workflow-philosophy.md).

## Release Management

While Rinna provides a clean interface for release management, this is not the primary focus. The system prioritizes work item management from the developer's perspective, with release management maintained as a future integration point. When fully implemented, Rinna will enforce semantic versioning (`major.minor.patch`), limiting patch releases to a maximum of 999 per minor release to encourage disciplined incremental improvements.

## Lota (Cycle Duration)

A "Lota" represents the specific cycle duration chosen by the software engineering team, typically ranging from one to four weeks based on team needs.

## Technical Environment

- **OS:** Ubuntu 24 LTS
- **Languages:** Java (Core Logic), Bash (CLI), optionally Go/Rust (Performance-critical components)
- **Database:** SQLite (Local), PostgreSQL (Azure deployment)
- **Containerization:** Docker (Local and Azure)
- **Infrastructure Automation:** Terraform, Azure CLI

## Database Schema (high-level)

- **items**: Immutable IDs, item type, title, description, status, priority, assignee, timestamps.
- **releases**: Semantic versioning (major, minor, patch).
- **users**: User identification and assignment.

## Interfaces

### Input
- CLI commands (Bash, Java)
- SQLite/PostgreSQL database operations

### Output
- Explicit CLI feedback messages
- Database queries providing current workflow and release status

## Automation & Feedback
- Automated validation with explicit feedback on workflow actions
- Automatic enforcement of release versioning rules

## Outcomes

### Positive Outcomes
- Transparent workflow tracking
- Explicit prioritization
- Controlled incremental software releases

### Negative Outcomes Prevented
- Ambiguity and potential loss of items
- Unmanaged accumulation of technical debt
- Workflow mismanagement

## Implementation Autonomy

Software implementation teams retain autonomy to select specific architectural details, integration methods, and testing strategies, provided alignment with Rinna's core philosophy and passing system-level BDD tests.