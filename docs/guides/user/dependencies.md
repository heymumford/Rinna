# Work Item Dependencies and Relationships

This document explains how work items in Rinna can relate to each other through dependencies and relationships, and how these connections affect workflow.

## Overview

Rinna supports two primary ways to connect work items:

1. **Dependencies**: Represent order-based relationships where one work item must be completed before another can proceed. These are crucial for critical path calculation and project scheduling.

2. **Relationships**: Represent semantic connections between work items, such as parent-child hierarchies, duplicates, or related items. These help with organization and navigation.

## Dependencies

Dependencies represent workflow constraints between work items, indicating which items must be completed before others can start or progress.

### Dependency Types

Rinna supports the following dependency types:

| Type | Description | Effect on Workflow | CLI Example |
|------|-------------|-------------------|-------------|
| `BLOCKED_BY` | Cannot proceed until the blocking item is complete | Prevents transitions to IN_PROGRESS | `rin link WI-123 BLOCKED_BY WI-456` |
| `DEPENDS_ON` | Functional dependency without hard blocking | Warning on transition to IN_PROGRESS | `rin link WI-123 DEPENDS_ON WI-456` |
| `FOLLOWS` | Sequential relationship (non-blocking) | Suggests ordering but doesn't enforce | `rin link WI-123 FOLLOWS WI-456` |
| `PRECEDES` | Sequential relationship (opposite of FOLLOWS) | Suggests ordering but doesn't enforce | `rin link WI-123 PRECEDES WI-456` |
| `CONFLICTS_WITH` | Mutually exclusive implementations | Warning when both items IN_PROGRESS | `rin link WI-123 CONFLICTS_WITH WI-456` |

### Working with Dependencies

#### Creating Dependencies

```bash
# Basic dependency creation
rin link WI-123 DEPENDS_ON WI-456

# Creating dependencies with additional context
rin link WI-123 BLOCKED_BY WI-456 --reason "Requires database schema changes"

# Bulk dependency creation
rin link-bulk WI-123 BLOCKED_BY WI-456,WI-457,WI-458
```

#### Viewing Dependencies

```bash
# View dependencies for a specific work item
rin dependencies WI-123

# With transitive dependencies (dependencies of dependencies)
rin dependencies WI-123 --transitive

# Focusing on specific dependency types
rin dependencies WI-123 --type BLOCKED_BY,DEPENDS_ON

# With depth limit for transitive dependencies
rin dependencies WI-123 --transitive --depth 3
```

#### Modifying Dependencies

```bash
# Change dependency type
rin link-update WI-123 DEPENDS_ON WI-456 --new-type BLOCKED_BY

# Update reason/context
rin link-update WI-123 BLOCKED_BY WI-456 --reason "Updated rationale"

# Remove dependencies
rin unlink WI-123 BLOCKED_BY WI-456
```

### Dependency Rules

1. **No Cycles**: Dependencies cannot form cycles. For example, if A depends on B, and B depends on C, then C cannot depend on A.
2. **Transitivity**: Dependencies are transitive. If A depends on B, and B depends on C, then A implicitly depends on C.
3. **Status Constraints**: Some dependencies enforce status constraints. For example, if item A blocks item B, then item B cannot transition to DONE until item A is DONE.

## Relationships

Relationships represent semantic connections between work items that help with organization, navigation, and understanding.

### Relationship Types

| Type | Description | Workflow Impact |
|------|-------------|----------------|
| `PARENT_CHILD` | Hierarchical organization | Parent can't be DONE until children are DONE |
| `SUBTASK_OF` | Item is part of a larger task | Parent cannot be DONE until all subtasks DONE |
| `RELATED_TO` | General association | No direct workflow impact |
| `DUPLICATE` | Item duplication | Original resolution typically resolves duplicate |

### Working with Relationships

#### Creating Parent-Child Relationships

```bash
# Make a work item a child of another
rin link --parent WI-123 --child WI-456

# Create a subtask relationship
rin link WI-123 SUBTASK_OF WI-458
```

#### Viewing Relationships

```bash
# View a work item's children
rin ls --parent WI-123

# View a work item's parent
rin ls --child WI-456

# View all relationships for a work item
rin view --relationships WI-123
```

#### Marking Duplicates

```bash
# Mark a work item as a duplicate of another
rin link --duplicate WI-123 --original WI-456
```

## Critical Path Analysis

The dependencies between work items form the basis for critical path analysis, which helps identify the sequence of work items that determines the minimum time needed to complete a project.

### Using Critical Path Analysis

```bash
# View the project's critical path
rin path

# Identify blockers on the critical path
rin path --blockers

# See how a specific work item fits into the critical path
rin path --item WI-123

# For detailed information including estimated completion dates
rin path --verbose
```

### Impact Analysis

```bash
# Assess the impact of delays
rin path --impact WI-123 --delay 5

# Identify work that can be done in parallel
rin path --parallel
```

### Visualization

```bash
# Generate a visual dependency graph
rin path --graph --output graph.png
```

## Common Patterns and Best Practices

### When to Use Dependencies vs. Relationships

- Use **dependencies** when one work item must be completed before another can proceed
- Use **parent-child relationships** for organizing related work into logical groups
- Use **related relationships** for associating items without enforcing workflow constraints

### Dependency Management Tips

1. **Be Selective**: Only create dependencies when there is a true blocking condition
2. **Keep it Direct**: Avoid creating redundant dependencies covered by transitivity
3. **Review Regularly**: Periodically review dependencies to remove any that are no longer relevant

### Avoiding Common Pitfalls

1. **Circular Dependencies**: Use the dependency detection tool: `rin check-circular`
2. **Dependency Overload**: Too many dependencies can create unnecessary constraints
3. **Incorrect Dependency Types**: Choose the right type (BLOCKED_BY vs. DEPENDS_ON) based on the true relationship

### Special Cases

#### Handling Circular Dependencies

If Rinna detects a circular dependency, you'll need to resolve it:

```bash
# Detect circular dependencies
rin check-circular --release RELEASE-456

# Change relationship type to break the circle
rin link change WI-789 DEPENDS_ON WI-123 RELATED_TO
```

#### Overriding Dependency Constraints

In rare cases, you may need to override a dependency constraint:

```bash
# Document the exception and override the constraint
rin override-dependency-constraint WI-123 \
  --reason "Dependency will be resolved in parallel branch" \
  --approved-by "jane.doe"
```

## Advanced Topics

For more complex scenarios involving dependencies and relationships, see:

- [Advanced Workflows](advanced-workflows.md) - Special workflow patterns for complex situations
- [Multi-Team Usage](multi-team.md) - Coordinating work across multiple teams
