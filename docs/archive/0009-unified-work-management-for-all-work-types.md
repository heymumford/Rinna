# 9. Unified Work Management for All Work Types

Date: April 10, 2025

## Status

Proposed

## Context

Software development involves multiple types of work - business requirements, product features, engineering tasks, and testing activities. Traditionally, these work types are managed in different systems or with different workflows, creating silos and coordination challenges.

Teams struggle with:
1. Inconsistent terminology across departments
2. Difficulty tracking dependencies between work types
3. Inability to produce holistic reports and metrics
4. User friction from constantly switching between different tools and interfaces
5. Cognitive overhead of translating between different work paradigms

Rinna already has a strong foundation with its unified workflow model, but we need to formalize and enhance the approach to explicitly handle diverse work types within a single framework.

## Decision

We will implement a comprehensive unified work management approach with the following principles:

1. **One Work Item Model**: All work, regardless of type (business, product, engineering, test), will be represented through a single, flexible WorkItem interface with:
   - Common core properties shared by all work types
   - Type-specific extensions for specialized attributes
   - Flexible metadata for custom properties

2. **Consistent Workflow**: All work types will follow the same core workflow states (Found, Triaged, To Do, In Progress, In Test, Done, Released), ensuring process consistency while allowing type-specific rules.

3. **Unified Vocabulary Management**: The system will include translation layers to map between different terminology (e.g., "User Story" vs "Feature" vs "Task") without changing the core model.

4. **Cross-cutting Reporting**: We'll implement reporting that can analyze all work types together, showing dependencies, relationships, and progress across traditional boundaries.

5. **Work Type Classification**: We'll enhance the classification system to categorize items by:
   - Origin (business, product, engineering, test)
   - CYNEFIN framework domain (obvious, complicated, complex, chaotic)
   - Work paradigm (task-based, story-based, experiment-based, goal-based)

6. **Container-First Approach**: To minimize installation requirements and ensure cross-platform compatibility, we'll prioritize a container-based deployment that works equally well on Windows, WSL, and native Linux.

## Consequences

### Positive

1. Reduced cognitive load for teams who no longer need to "context switch" between different work paradigms
2. Improved visibility across different work types and departments
3. Better tracking of dependencies between different work categories
4. Simplified onboarding as team members only need to learn one system
5. Enhanced reporting that crosses traditional boundaries
6. More accurate planning by considering all work types together
7. Easier platform adoption through simplified container-based installation

### Negative

1. May require more initial effort to design a flexible yet consistent model
2. Could feel constraining to teams accustomed to specialized tools
3. Will require additional documentation to explain the unified approach
4. May need more complex mapping to external tools that maintain separate models
5. Container orchestration adds complexity to deployments

### Mitigations

1. Implement a robust vocabulary mapping system to respect team terminology
2. Create detailed documentation demonstrating the value of the unified approach
3. Develop view customizations so different roles see relevant information
4. Build flexible attribute extensions for specialized needs
5. Provide automated container health monitoring

## Implementation Plan

1. Design and document the unified work model (40% complete)
2. Enhance WorkItem interface and implementations (30% complete)
3. Implement improved container orchestration for cross-platform support (25% complete)
4. Create comprehensive vocabulary mapping system (15% complete)
5. Develop unified reporting capabilities (20% complete)
6. Enhance user interfaces to support the unified model (10% complete)
7. Create documentation and training materials (5% complete)

Overall POC completion: Approximately 20%

## References

- [Ryorin-do v0.2 Philosophy](../../diagrams/ryorindo-v0.2-philosophy.md)
- [Workflow State Diagram](../../diagrams/workflow/workflow_state_diagram.md)
- [Work Item Relationships](../../user-guide/work-item-relationships.md)
- [Workflow Philosophy](../../user-guide/workflow-philosophy.md)
- [CYNEFIN Framework](https://en.wikipedia.org/wiki/Cynefin_framework)