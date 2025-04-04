# Rinna Engineering Specification v0.3

## Vision

Rinna is a streamlined, modular, open-source software system explicitly designed to facilitate clear, usable, and maintainable workflow and release management for software engineering teams. Inspired by the philosophy of Samstraumr, Rinna provides intentionally selected features that transparently manage software development work, with careful consideration toward refined integration and intuitive user experience.

## Core Philosophy

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

Can you customize the workflow? No, you can't. [That's the point](../user-guide/workflow-philosophy.md).

## Release Management

Rinna enforces semantic versioning (`major.minor.patch`), limiting patch releases to a maximum of 999 per minor release to encourage disciplined incremental improvements.

## Lota (Cycle Duration)

A "Lota" represents the specific cycle duration chosen by the software engineering team, typically ranging from one to four weeks based on team needs.

## Recommended Lota Ceremonies

Software development teams are recommended to adopt these explicit ceremonies:

- **Flow Planning:** Concise session at Lota start to establish clear objectives and explicitly assign work items.
- **Daily Flow Check-in:** Brief daily meetings for explicit communication on progress, impediments, and immediate objectives.
- **Flow Review:** End-of-Lota meeting for transparent review and feedback on completed items.
- **Flow Retrospective:** Regular reflections explicitly targeting enhancements in clarity, usability, and workflow efficiency.

## Incremental Implementation Approach

Given the philosophy of iterative refinement and the desire to deliver incremental value, Rinna’s implementation will follow an incremental feature-driven development approach:

1. **Core Java Domain Library:** Establish foundational Java logic packaged as a Maven dependency for immediate integration and usability.
2. **CLI-first Approach (Bash):** Develop incremental Bash-based CLI tools for immediate user feedback and iteration.
3. **Docker Containerization:** Ensure consistent deployment on local Ubuntu and Azure cloud environments.
4. **Database Migration Path:** Start with SQLite for immediate local feedback; gradually introduce PostgreSQL adapters for seamless cloud deployment.
5. **Infrastructure Automation:** Use Terraform and Azure CLI for incremental automation as infrastructure requirements evolve.
6. **Continuous Agile Validation:** Implement and expand upon high-level Cucumber scenarios incrementally to validate progressive delivery and integration.

## Native GitHub Integration

Rinna natively supports GitHub integration to enable seamless, intuitive workflow updates directly from developers' commit messages and automated tests:

- **Commit Message Hooks:** Simple syntax in commit messages to update work item statuses automatically.
- **Automated Status Updates:** Test outcomes automatically reflected in associated work items, providing immediate visibility into project state.
- **Continuous State Visibility:** Real-time state updates remove the need for traditional status meetings by continuously radiating current project state.

## Technical Environment

- **OS:** Ubuntu 24 LTS
- **Languages:** Java (Core Logic), Bash (CLI), optionally Go/Rust (Performance-critical components)
- **Database:** SQLite (Local), PostgreSQL (Azure deployment)
- **Containerization:** Docker (Local and Azure)
- **Infrastructure Automation:** Terraform, Azure CLI

## Interfaces

### Input
- CLI commands (Bash, Java)
- SQLite/PostgreSQL database operations
- GitHub commit message hooks

### Output
- Explicit CLI feedback messages
- Database queries providing current workflow and release status
- Continuous state updates via GitHub integration

## Automation & Feedback
- Automated validation with explicit feedback on workflow actions
- Automatic enforcement of release versioning rules
- Continuous, real-time project state visibility through GitHub integration

## Outcomes

### Positive Outcomes
- Transparent workflow tracking
- Explicit prioritization
- Controlled incremental software releases
- Immediate, continuous visibility of project status

### Negative Outcomes Prevented
- Ambiguity and potential loss of items
- Unmanaged accumulation of technical debt
- Workflow mismanagement
- Inefficient and redundant status meetings

## Java Integration

Rinna software includes a core Java library packaged as a Maven dependency for easy integration:

```xml
<dependency>
    <groupId>org.samstraumr</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Implementation Autonomy

Software implementation teams retain autonomy to select specific architectural details, integration methods, and testing strategies, provided alignment with Rinna’s core philosophy and passing system-level BDD tests.

## Agile Alignment and Critical Success Factors

### Critical Success Factors
- **Adaptability:** Empower teams to customize workflows and adapt constraints responsively.
- **Collaboration:** Foster intentional communication alongside structured workflows, promoting balance between explicit processes and interpersonal interactions.
- **Continuous Improvement:** Consistently conduct retrospectives to explicitly address and enhance flexibility, responsiveness, and usability.

---

Rinna aims above all to deliver explicit and intuitive clarity for software teams.


