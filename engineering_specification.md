# Rinna Engineering Specification

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

## Agile Alignment and Critical Success Factors

### Potential Alignment Gaps
- **Limited Responsiveness:** Highly structured workflows may limit flexibility in responding to changing requirements.
- **Risk of Over-structuring:** Clearly defined workflow stages could potentially inhibit rapid adaptive decision-making.
- **Reduced Informal Collaboration:** Explicit structure and CLI-based interaction could reduce spontaneous collaboration opportunities.

### Critical Success Factors
- **Adaptability:** Empower teams to customize workflows and adapt constraints responsively.
- **Collaboration:** Foster intentional communication alongside structured workflows, promoting balance between explicit processes and interpersonal interactions.
- **Continuous Improvement:** Consistently conduct retrospectives to explicitly address and enhance flexibility, responsiveness, and usability.

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

## System-level Cucumber BDD Tests

```gherkin
Feature: Workflow management
  To manage software work clearly and transparently
  As a software engineering team
  We need explicit workflow enforcement

  Scenario: Creating a new Bug item
    Given the Rinna software is initialized
    When the developer creates a new Bug with title "Login fails"
    Then the Bug should exist with status "To Do" and priority "medium"

  Scenario: Progressing an item through workflow
    Given a Bug titled "Login fails" exists
    When the developer updates the Bug status to "In Progress"
    Then the Bug's status should be "In Progress"

  Scenario: Validating workflow transitions
    Given a Bug titled "Login fails" exists
    When the developer attempts an invalid status transition to "Released"
    Then the system should explicitly reject the transition

Feature: Release management
  To enforce clear incremental software versioning
  As a software release manager
  I require structured semantic versioning

  Scenario: Incrementing a software release
    Given the current software release is "1.0.003"
    When the release manager increments the release for major 1 and minor 0
    Then the release version should explicitly increment to "1.0.004"

  Scenario: Enforcing patch limit
    Given the current software release is "1.0.999"
    When the release manager attempts another patch release
    Then the system explicitly prevents this and prompts for minor version increment

Feature: User assignment
  To explicitly assign software items
  As a software team member
  I need to assign items clearly to existing team members

  Scenario: Assigning a Feature to a developer
    Given a user "jdoe" exists
    And a Feature titled "Update Documentation" exists
    When the team member assigns the feature to "jdoe"
    Then the feature explicitly shows "jdoe" as the assignee

  Scenario: Preventing assignment to unknown user
    Given no user named "nonuser" exists
    When the team member attempts to assign an item to "nonuser"
    Then the system explicitly rejects the assignment
```

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

---

Rinna aims above all to deliver explicit and intuitive clarity for software teams.


