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
| [ADR-0004](0004-refactor-package-structure-to-align-with-clean-architecture.md) | Refactor Package Structure to Align with Clean Architecture | Accepted |
| [ADR-0005](0005-adopt-multi-language-approach-for-system-components.md) | Adopt Multi-Language Approach for System Components | Accepted |
| [ADR-0006](0006-implement-comprehensive-testing-pyramid-strategy.md) | Implement Comprehensive Testing Pyramid Strategy | Accepted |
| [ADR-0007](0007-establish-security-compliance-framework.md) | Establish Security Compliance Framework | Accepted |
| [ADR-0008](0008-establish-task-prioritization-framework-with-tdd-first-approach.md) | Establish Task Prioritization Framework with TDD-First Approach | Proposed |

## Statuses

- **Proposed**: The ADR is proposed and under discussion
- **Accepted**: The ADR has been accepted and the decision is in effect
- **Rejected**: The ADR was rejected, and the decision will not be implemented
- **Deprecated**: The ADR was once accepted but is no longer relevant
- **Superseded**: The ADR was accepted but has been replaced by a newer decision (link to the new ADR)