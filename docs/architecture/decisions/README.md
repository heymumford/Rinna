# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for the Rinna project.

## What are Architecture Decision Records?

Architecture Decision Records (ADRs) are documents that capture important architectural decisions made along with their context and consequences. They provide a historical record of the technical choices made during the project development.

Each ADR describes:
- The architectural decision that was made
- The context and forces that led to the decision
- The rationale behind the decision
- The consequences and trade-offs of the decision

## How to Create a New ADR

To create a new ADR:

1. Run the ADR creation script:
   ```bash
   ./bin/new-adr "Title of the decision"
   ```

   For example:
   ```bash
   ./bin/new-adr "Use PostgreSQL for persistent storage"
   ```

2. Edit the generated file to fill in the details.
3. Update the status when the decision is accepted or rejected.

## Index of ADRs

Here's a chronological list of all ADRs:

| ID | Title | Status |
|----|-------|--------|
| [ADR-0001](0001-record-architecture-decisions.md) | Record Architecture Decisions | Accepted |
| [ADR-0002](0002-automated-c4-architecture-diagrams.md) | Automated C4 Architecture Diagrams | Accepted |
| [ADR-0003](0003-adopt-clean-architecture-for-system-design.md) | Adopt Clean Architecture for System Design | Accepted |

## Statuses

- **Proposed**: The ADR is proposed and under discussion
- **Accepted**: The ADR has been accepted and the decision is in effect
- **Rejected**: The ADR was rejected, and the decision will not be implemented
- **Deprecated**: The ADR was once accepted but is no longer relevant
- **Superseded**: The ADR was accepted but has been replaced by a newer decision (link to the new ADR)